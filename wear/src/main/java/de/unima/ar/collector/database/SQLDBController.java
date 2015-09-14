package de.unima.ar.collector.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import de.unima.ar.collector.sensors.SensorService;
import de.unima.ar.collector.shared.util.DeviceID;


public class SQLDBController
{
    private static SQLDBController INSTANCE = null;
    private static Context         context  = null;

    private DatabaseHelper databaseHelper;

    private final Object databaseLock;


    private SQLDBController()
    {
        this.databaseLock = new Object();
        this.databaseHelper = new DatabaseHelper(context);
    }


    public static void initInstance(Context c)
    {
        context = c;
        if(INSTANCE == null) {
            INSTANCE = new SQLDBController();
        }

        // create tables
        DatabaseHelper.createTables();
    }


    public static SQLDBController getInstance()
    {
        return INSTANCE;
    }


    public List<String[]> query(String sql, String[] selectionArgs, boolean header)
    {
        List<String[]> result;

        synchronized(databaseLock) {
            SQLiteDatabase database = databaseHelper.getReadableDatabase();
            Cursor c = database.rawQuery(sql, selectionArgs);

            result = convertCursorToList(c, header);
            //            database.close();
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


    public void execSQL(String sql)
    {
        synchronized(databaseLock) {
            SQLiteDatabase database = databaseHelper.getWritableDatabase();
            database.execSQL(sql);
            //            database.close();
        }
    }


    public boolean deleteDatabase()
    {
        synchronized(databaseLock) {
            SQLiteDatabase database = databaseHelper.getWritableDatabase();


            if(SensorService.getInstance().getSCM().getEnabledCollectors().size() > 0) {
                return false;
            }

            database.close();
            this.databaseHelper.deleteDatabase();

            // recreate tables
            String deviceID = DeviceID.get(context);
            DatabaseHelper.createTables();

            // local device
            DatabaseHelper.createDeviceDependentTables(deviceID);
            //            SQLDBController.getInstance().registerDevice(deviceID);
        }

        return true;
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