package com.nitu.andrei.wearable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


/**
 * Database handler
 * Created by bogdan on 20/12/15.
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;

    private static final String DATABASE_NAME = "workout";

    public static final int DELETE_OP = 0;
    public static final int INSERT_OP = 1;
    public static final int UPDATE_OP = 2;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DatabaseContract.Heartbeats.SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL(DatabaseContract.Heartbeats.SQL_DELETE_ENTRIES);

        // Create tables again
        onCreate(db);
    }

    public void reinstall() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(DatabaseContract.Heartbeats.SQL_DELETE_ENTRIES);

        onCreate(db);
    }


    /**
     * Marks as deleted any element
     *
     * @param value
     */
    public Long addHeartbeat(Integer value, Boolean synced) {
        if (synced == null) {
            synced = false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Heartbeats.COLUMN_VALUE, value);
        values.put(DatabaseContract.Heartbeats.COLUMN_SYNCED, synced ? 1 : 0);
        long id = db.insert(DatabaseContract.Heartbeats.TABLE_NAME, null, values);

        db.close();

        return id;
    }

    public long setHeartbeatAsSynced(long hbid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.Heartbeats.COLUMN_SYNCED, true);

        long id = db.update(DatabaseContract.Heartbeats.TABLE_NAME, values, DatabaseContract.Heartbeats._ID + " = ?", new String[] { String.valueOf(hbid) });
        db.close();

        return id;
    }

    public ArrayList<Heartbeat> getUnsyncedHeartbeats() {
        SQLiteDatabase db = this.getWritableDatabase();

        ArrayList<Heartbeat> list = new ArrayList<Heartbeat>();

        String selectQuery = "SELECT _id, value, strftime('%s', created), created FROM " + DatabaseContract.Heartbeats.TABLE_NAME + " WHERE synced IS FALSE ORDER BY created DESC";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                list.add(new Heartbeat(Integer.parseInt(cursor.getString(1)), cursor.getLong(2), cursor.getString(3)));
            } while (cursor.moveToNext());
        }
        db.close();

        return list;
    }

    public ArrayList<Heartbeat> getHeartbeats(Integer limit, String period) {
        if (limit == null) {
            limit = 10;
        }
        SQLiteDatabase db = this.getWritableDatabase();

        ArrayList<Heartbeat> list = new ArrayList<Heartbeat>();

        String where = "";
        if (period != null) {
            Date date;
            String dateString;
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
            Calendar cal = Calendar.getInstance();
            int year;
            switch (period) {
                case "day":
                    date = new Date();
                    where = "WHERE date(created) = '" + ft.format(date) + "'";
                    break;
                case "month":
                    date = new Date();
                    cal.setTime(date);
                    year = cal.get(Calendar.YEAR);
                    int month = cal.get(Calendar.MONTH) + 1;
                    String monthStr = month+"";
                    if (month / 10 == 0) {
                        monthStr = "0" + month;
                    }

                    dateString = year + "-" + monthStr + "-01";
                    where = "WHERE date(created) >= '" + dateString + "'";
                    break;
                case "year":
                    date = new Date();

                    cal.setTime(date);
                    year = cal.get(Calendar.YEAR);

                    dateString = year + "-01-01";
                    where = "WHERE date(created) >= '" + dateString + "'";
                    break;
                default:
                    break;
            }

            where = " " + where + " ";
        }

        String selectQuery = "SELECT _id, value, strftime('%s', created), created FROM " + DatabaseContract.Heartbeats.TABLE_NAME + where + " ORDER BY created DESC LIMIT " + limit;

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                list.add(new Heartbeat(Integer.parseInt(cursor.getString(1)), cursor.getLong(2), cursor.getString(3)));
            } while (cursor.moveToNext());
        }
        db.close();

        return list;
    }
}
