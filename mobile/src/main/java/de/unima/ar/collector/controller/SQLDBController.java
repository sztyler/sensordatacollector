package de.unima.ar.collector.controller;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.unima.ar.collector.SensorDataCollectorService;
import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.api.ListenerService;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.sensors.CustomCollector;
import de.unima.ar.collector.sensors.SensorCollector;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.DeviceID;


public class SQLDBController
{
    private static SQLDBController INSTANCE = null;
    private static Context         context  = null;

    private DatabaseHelper databaseHelper;

    private static final String SERVICENAME = "de.unima.ar.sqlDBCon";

    private final Object databaseLock;


    private SQLDBController()
    {
        this.databaseLock = new Object();
        this.databaseHelper = new DatabaseHelper(context);
    }


    public static SQLDBController getInstance()
    {
        if(INSTANCE == null) {
            context = SensorDataCollectorService.getInstance();
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
                //            database.close();
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
            //            database.close();
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
        //            database.close();
        //        }


        return result;
    }


    public long bulkInsert(String table, List<String[]> values)
    {
        long result = -1;

        if(values.size() == 0) {
            return result;
        }

        String sql = "INSERT OR IGNORE INTO " + table + " (";
        String qMarks = "";
        for(String name : values.get(0)) {
            sql += name + ",";
            qMarks += "?,";
        }

        sql = sql.substring(0, sql.length() - 1);
        sql += ") values (" + qMarks.substring(0, qMarks.length() - 1) + ");";

        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        database.beginTransaction();
        SQLiteStatement insert = database.compileStatement(sql);
        for(int i = 1; i < values.size(); i++) {
            String[] value = values.get(i);

            for(int j = 1; j <= value.length; j++) {
                insert.bindString(j, value[j - 1]);
            }
            result = insert.executeInsert();
        }
        database.setTransactionSuccessful();
        database.endTransaction();

        return result;
    }


    public int update(String table, ContentValues values, String whereClause, String[] whereArgs)
    {
        int result;

        synchronized(databaseLock) {
            SQLiteDatabase database = databaseHelper.getWritableDatabase();
            result = database.update(table, values, whereClause, whereArgs);
            //            database.close();
        }

        return result;
    }


    //    public long replace(String table, String nullColumnHack, ContentValues initValues)
    //    {
    //        long result;
    //
    //        synchronized(databaseLock) {
    //            SQLiteDatabase database = databaseHelper.getWritableDatabase();
    //            result = database.replace(table, nullColumnHack, initValues);
    //            //            database.close();
    //        }
    //
    //        return result;
    //    }


    public void execSQL(String sql)
    {
        synchronized(databaseLock) {
            SQLiteDatabase database = databaseHelper.getWritableDatabase(); // TODO onActivityCreated
            database.execSQL(sql);
            //            database.close();
        }
    }


    public String getPath()
    {
        String path;

        synchronized(databaseLock) {
            SQLiteDatabase database = databaseHelper.getWritableDatabase();
            path = database.getPath();
            //            database.close();
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

            //            database.close();
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