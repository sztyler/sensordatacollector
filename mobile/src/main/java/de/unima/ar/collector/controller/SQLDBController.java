package de.unima.ar.collector.controller;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.unima.ar.collector.SensorDataCollectorService;
import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.api.ListenerService;
import de.unima.ar.collector.database.BulkInsertService;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.sensors.CustomCollector;
import de.unima.ar.collector.sensors.SensorCollector;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.DeviceID;


public class SQLDBController
{
    private static SQLDBController INSTANCE = null;

    private       DatabaseHelper databaseHelper;
    private final Object         databaseLock;

    private static final String SERVICENAME = "de.unima.ar.sqlDBCon";

    private static Map<String, List<String[]>> cacheToDB = new HashMap<>();
    ;


    private SQLDBController()
    {
        Context context = SensorDataCollectorService.getInstance().getApplicationContext();

        this.databaseLock = new Object();
        this.databaseHelper = new DatabaseHelper(context);
    }


    public static SQLDBController getInstance()
    {
        if(INSTANCE == null) {
            INSTANCE = new SQLDBController();

            // create tables
            DatabaseHelper.createTables();
        }

        return INSTANCE;
    }


    public List<String[]> query(String sql, String[] selectionArgs, boolean header)
    {
        List<String[]> result;

        synchronized(databaseLock) {
            try {
                SQLiteDatabase database = databaseHelper.getReadableDatabase();
                Cursor c = database.rawQuery(sql, selectionArgs);

                result = convertCursorToList(c, header);
            } catch(SQLiteException e) {
                result = new ArrayList<>(); // table does not exist or invalid query -so the result is empty
            }
        }

        return result;
    }


    public int delete(String table, String whereClause, String[] whereArgs)
    {
        int result;

        synchronized(databaseLock) {
            SQLiteDatabase database = databaseHelper.getWritableDatabase();
            result = database.delete(table, whereClause, whereArgs);
        }

        return result;
    }


    public long insert(String table, String nullColumnHack, ContentValues values)
    {
        long result;

        // TODO
        //        synchronized(databaseLock) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        result = database.insert(table, nullColumnHack, values);
        //        }


        return result;
    }


    public void bulkInsert(String table, List<String[]> values)
    {
        String sql = "INSERT OR IGNORE INTO " + table + " (";
        String qMarks = "";
        for(String name : values.get(0)) {
            sql += name + ",";
            qMarks += "?,";
        }

        sql = sql.substring(0, sql.length() - 1);
        sql += ") values (" + qMarks.substring(0, qMarks.length() - 1) + ");";

        String uuid = UUID.randomUUID().toString();
        cacheToDB.put(uuid, values);

        Context context = SensorDataCollectorService.getInstance().getApplicationContext();
        Intent insert = new Intent(context, BulkInsertService.class);
        insert.putExtra("sqlQuery", sql);
        insert.putExtra("uuid", uuid);
        context.startService(insert);
    }


    public void bulkInsertFromIntent(String uuid, String sql)
    {
        List<String[]> values = cacheToDB.get(uuid);
        if(values == null) {
            return;
        }
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        database.beginTransaction();
        SQLiteStatement insert = database.compileStatement(sql);
        for(int i = 1; i < values.size(); i++) {
            String[] value = values.get(i);

            for(int j = 1; j <= value.length; j++) {
                insert.bindString(j, value[j - 1]);
            }

            insert.executeInsert();
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        cacheToDB.remove(uuid);
    }


    public int update(String table, ContentValues values, String whereClause, String[] whereArgs)
    {
        int result;

        synchronized(databaseLock) {
            SQLiteDatabase database = databaseHelper.getWritableDatabase();
            result = database.update(table, values, whereClause, whereArgs);
        }

        return result;
    }


    public void execSQL(String sql)
    {
        synchronized(databaseLock) {
            SQLiteDatabase database = databaseHelper.getWritableDatabase(); // TODO onActivityCreated
            database.execSQL(sql);
        }
    }


    public String getPath()
    {
        String path;

        synchronized(databaseLock) {
            SQLiteDatabase database = databaseHelper.getWritableDatabase();
            path = database.getPath();
        }

        return path;
    }


    public long getSize()
    {
        long size = 0;

        synchronized(databaseLock) {
            SQLiteDatabase database = databaseHelper.getWritableDatabase();

            File databaseFile = new File(database.getPath());
            if(databaseFile.exists()) {
                size = databaseFile.length();
            } else {
                Log.w(SERVICENAME, "DB not null, File does not exist!");
            }
        }

        return size;
    }


    public boolean deleteDatabase()
    {
        synchronized(databaseLock) {
            SQLiteDatabase database = databaseHelper.getWritableDatabase();

            for(SensorCollector entry : SensorDataCollectorService.getInstance().getSCM().getSensorCollectors().values()) {
                if(entry.isRegistered) {
                    return false;
                }
            }

            for(CustomCollector entry : SensorDataCollectorService.getInstance().getSCM().getCustomCollectors().values()) {
                if(entry.isRegistered()) {
                    return false;
                }
            }

            database.close();
            this.databaseHelper.deleteDatabase();

            // recreate tables
            Context context = SensorDataCollectorService.getInstance().getApplicationContext();
            String deviceID = DeviceID.get(context);
            DatabaseHelper.createTables();

            // local device
            DatabaseHelper.createDeviceDependentTables(deviceID);
            SQLDBController.getInstance().registerDevice(deviceID);

            // external devices
            for(String device : ListenerService.getDevices()) {
                DatabaseHelper.createDeviceDependentTables(device);
                SQLDBController.getInstance().registerDevice(device);
            }

            // inform other devices
            BroadcastService.getInstance().sendMessage("/database/delete", "");
        }

        return true;
    }


    public boolean registerDevice(String deviceID)
    {
        boolean known = true;

        List<String[]> result = SQLDBController.getInstance().query("SELECT device FROM " + SQLTableName.DEVICES + " WHERE device=?", new String[]{ deviceID }, false);
        ContentValues newValues = new ContentValues();
        newValues.put("lastseen", System.currentTimeMillis());

        if(result.size() == 0) {
            newValues.put("device", deviceID);
            SQLDBController.getInstance().insert(SQLTableName.DEVICES, null, newValues);
            known = false;
        } else {
            SQLDBController.getInstance().update(SQLTableName.DEVICES, newValues, "device=?", new String[]{ deviceID });
        }

        return known;
    }


    private List<String[]> convertCursorToList(Cursor c, boolean header)
    {
        List<String[]> result = new ArrayList<>();

        if(header) {
            result.add(c.getColumnNames());
        }

        c.moveToFirst();
        while(!c.isAfterLast()) {
            String[] row = new String[c.getColumnCount()];

            for(int i = 0; i < c.getColumnCount(); i++) {
                row[i] = c.getString(i);
            }

            result.add(row);
            c.moveToNext();
        }

        return result;
    }
}