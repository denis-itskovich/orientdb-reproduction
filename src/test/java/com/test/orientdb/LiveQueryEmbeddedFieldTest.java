package com.test.orientdb;

import com.orientechnologies.orient.core.db.*;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.executor.OResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/***********************************
 *
 *  This test demonstrates the live query broken behavior since 3.1 (for 3.0.x the test passes)
 *
 *  Schema:
 *    ClassA: {name: String}
 *    ClassB: {embedded: ClassA}
 *
 *  Test case:
 *    1. live query: `select from ClassB`
 *    2. Add object: ClassB: {embedded: {name: test, @class: "ClassA"}, @class: "ClassB"}
 *    3. Delete this object
 *
 *  Expected behavior (behavior in 3.0.37): `on delete` notification should provide the entire deleted object, including embedded field "embedded"
 *  Actual behavior (behavior in 3.1.11): `on delete` notification provides deleted object, without embedded field (embedded: null)
 *
 */

public class LiveQueryEmbeddedFieldTest {
    private final static String dbName = "testDb_" + LiveQueryEmbeddedFieldTest.class.getSimpleName();
    private OrientDB dbClient;
    private Supplier<ODatabaseSession> sessionProvider;

    @BeforeEach
    public void setUp() {
        dbClient = new OrientDB("embedded:" + dbName, "root", "root", OrientDBConfig.defaultConfig());
        dbClient.createIfNotExists(dbName, ODatabaseType.MEMORY);
        sessionProvider = () -> dbClient.open(dbName, "admin", "admin");
    }

    class Subscriber extends AbstractLiveQueryResultListener implements AutoCloseable {
        private final OLiveQueryMonitor monitor;

        Subscriber(String query) {
            try (ODatabaseDocument session = sessionProvider.get()) {
                this.monitor = session.live(query, this);
            }
        }

        @Override
        public void close() {
            monitor.unSubscribe();
        }
    }

    @Test
    public void testLiveQueryWithEmbeddedField() throws InterruptedException {
        try (ODatabaseDocument session = sessionProvider.get()) {
            var classA = session.createClass("ClassA");
            classA.createProperty("name", OType.STRING);
            session.createClass("ClassB").createProperty("embedded", OType.EMBEDDED, classA);
        }

        List<String> createdObjects = new ArrayList<>();
        List<String> deletedObjects = new ArrayList<>();

        try (Subscriber subscriber = new Subscriber("select from ClassB") {
            @Override
            public void onCreate(ODatabaseDocument database, OResult data) {
                createdObjects.add(data.toJSON());
            }

            @Override
            public void onDelete(ODatabaseDocument database, OResult data) {
                deletedObjects.add(data.toJSON());
            }
        }) {
            ORID rowId = null;
            try (ODatabaseSession session = sessionProvider.get()) {
                var doc = new ODocument("ClassB")
                        .field("embedded", new ODocument("ClassA").field("name", "Object1"));
                rowId = session.save(doc).getIdentity();
            }

            Thread.sleep(1000);
            Assertions.assertEquals(1, createdObjects.size());
            Assertions.assertTrue(createdObjects.get(0).contains("\"embedded\": {\"name\": \"Object1\", \"@class\": \"ClassA\"}"));

            try (ODatabaseSession session = sessionProvider.get()) {
                session.delete(rowId);
            }

            Thread.sleep(1000);
            Assertions.assertEquals(1, deletedObjects.size());
            Assertions.assertTrue(deletedObjects.get(0).contains("\"embedded\": {\"name\": \"Object1\", \"@class\": \"ClassA\"}"));
        }
    }
}
