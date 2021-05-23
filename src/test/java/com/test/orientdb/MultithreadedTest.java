package com.test.orientdb;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.db.*;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.executor.OResult;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultithreadedTest {
    private final static String dbName = "testDb";
    private Supplier<ODatabaseSession> sessionProvider;
    private OrientDB dbClient;
    private ExecutorService pool;

    @BeforeAll
    public static void setUpClass() {
        DockerUtils.start();
    }

    @AfterAll
    public static void tearDownClass() {
        DockerUtils.stop();
    }

    @BeforeEach
    public void setUp() {
        pool = Executors.newCachedThreadPool();
        dbClient = new OrientDB("remote:localhost/" + dbName, "root", "root", OrientDBConfig.defaultConfig());
        dbClient.createIfNotExists(dbName, ODatabaseType.MEMORY, OrientDBConfig.defaultConfig());
        sessionProvider = () -> dbClient.open(dbName, "admin", "admin");
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        if (dbClient.exists(dbName)) {
            dbClient.drop(dbName);
        }
        dbClient.close();
        Thread.sleep(1000);
    }

    class Subscription extends AbstractLiveQueryResultListener implements AutoCloseable {
        private final List<String> notifications = new ArrayList<>();
        private final String className;
        private final AtomicReference<OLiveQueryMonitor> monitor = new AtomicReference<>();

        public Subscription(String className) {
            this.className = className;
        }

        public void subscribe() {
            System.out.println("Subscribing for " + className + " from thread: " + Thread.currentThread().getName());
            try (ODatabaseDocument session = sessionProvider.get()) {
                OLiveQueryMonitor monitor = session.live("select from " + className, this);
                this.monitor.set(monitor);
            }
        }

        public void unsubscribe() {
            try (ODatabaseDocument session = sessionProvider.get()) {
                Optional.ofNullable(monitor.get()).ifPresent(OLiveQueryMonitor::unSubscribe);
            }
        }

        public List<String> getNotifications() {
            return notifications;
        }

        @Override
        public void onCreate(ODatabaseDocument database, OResult data) {
            if (validClass(data)) {
                notifications.add("-> " + data.toJSON());
            }
        }

        @Override
        public void onError(ODatabaseDocument database, OException exception) {
            System.err.println("Error notification: " + exception);
        }

        @Override
        public void close() {
            unsubscribe();
        }

        private boolean validClass(OResult data) {
            if (!className.equals(data.getProperty("@class"))) {
                System.out.println("<" + className + "> listener received notification: " + data.toJSON());
                System.err.println("Received incorrect class notification! Expected class: <" + className +
                        "> actual: <" + data.getProperty("@class") + ">");
                return false;
            }
            return true;
        }
    }

    @RepeatedTest(10)
    public void testMultithreadedLiveQuery() throws Exception {
        try (ODatabaseDocument session = sessionProvider.get()) {
            session.createClass("ClassA").createProperty("name", OType.STRING);
            session.createClass("ClassB").createProperty("name", OType.STRING);
        }

        try (Subscription subscriptionA = new Subscription("ClassA");
             Subscription subscriptionB = new Subscription("ClassB")) {

            pool.submit(subscriptionA::subscribe);
            pool.submit(subscriptionB::subscribe);

            sleep(1000);

            try (ODatabaseDocument session = sessionProvider.get()) {
                session.<ODocument>newInstance("ClassA").field("name", "A1").save();
            }

            sleep(1000);
            assertEquals(1, subscriptionA.getNotifications().size());
        }
    }
}
