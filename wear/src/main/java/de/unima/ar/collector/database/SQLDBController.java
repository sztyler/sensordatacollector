package de.unima.ar.collector.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.unima.ar.collector.sensors.SensorService;
import de.unima.ar.collector.shared.Settings;
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
        Log.d("TIMOSENSOR", "BULK INSERT " + values.size());

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

        Log.d("TIMOSENSOR", "BULK INSERT RESULT " + result);

        List<String[]> abc = query("SELECT * FROM " + table + " LIMIT " + Settings.WEARTRANSFERSIZE, null, true);

        Log.d("TIMOSENSOR", "BULK INSERT TEST " + abc.size());

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