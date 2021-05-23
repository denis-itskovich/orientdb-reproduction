package com.test.orientdb;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.db.OLiveQueryResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.sql.executor.OResult;

public class AbstractLiveQueryResultListener implements OLiveQueryResultListener {
    @Override
    public void onCreate(ODatabaseDocument database, OResult data) {

    }

    @Override
    public void onUpdate(ODatabaseDocument database, OResult before, OResult after) {

    }

    @Override
    public void onDelete(ODatabaseDocument database, OResult data) {

    }

    @Override
    public void onError(ODatabaseDocument database, OException exception) {

    }

    @Override
    public void onEnd(ODatabaseDocument database) {

    }
}
