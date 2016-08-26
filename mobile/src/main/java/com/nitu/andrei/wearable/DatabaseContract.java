package com.nitu.andrei.wearable;


import android.provider.BaseColumns;

/**
 * DatabaseContract
 * database schema
 * Created by bogdan on 20/12/15.
 */
public final class DatabaseContract {


    /**
     * Tracks table
     */
    public static abstract class Heartbeats implements BaseColumns {
        public static final String TABLE_NAME = "heartbeats";
        public static final String COLUMN_VALUE = "value";
        public static final String COLUMN_CREATED = "created";
        public static final String COLUMN_SYNCED = "synced";

        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_VALUE + " INTEGER NOT NULL, " +
                        COLUMN_CREATED + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        COLUMN_SYNCED + " INTEGER DEFAULT 0" +
                        ")";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

}