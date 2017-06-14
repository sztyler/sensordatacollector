package de.unima.ar.collector.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import de.unima.ar.collector.sensors.collectors.AccelerometerCollector;
import de.unima.ar.collector.sensors.collectors.GravityCollector;
import de.unima.ar.collector.sensors.collectors.GyroscopeCollector;
import de.unima.ar.collector.sensors.collectors.LinearAccelerationCollector;
import de.unima.ar.collector.sensors.collectors.MagnetometerCollector;
import de.unima.ar.collector.sensors.collectors.OrientationCollector;
import de.unima.ar.collector.sensors.collectors.PressureCollector;
import de.unima.ar.collector.sensors.collectors.RotationVectorCollector;
import de.unima.ar.collector.sensors.collectors.StepCounterCollector;
import de.unima.ar.collector.sensors.collectors.StepDetectorCollector;
import de.unima.ar.collector.shared.database.SQLTableName;

public class DatabaseHelper extends SQLiteOpenHelper
{
    private Context context;

    private static final String sqlTableActivityData       = "CREATE TABLE IF NOT EXISTS " + SQLTableName.ACTIVITYDATA + " (id INTEGER PRIMARY KEY AUTOINCREMENT, activityid INT NOL NULL, subactivityid INT, starttime INT, endtime INT)";
    private static final String sqlTablePostureData        = "CREATE TABLE IF NOT EXISTS " + SQLTableName.POSTUREDATA + " (id INTEGER PRIMARY KEY AUTOINCREMENT, pid INT NOL NULL, starttime INT, endtime INT)";
    private static final String sqlTablePositionData       = "CREATE TABLE IF NOT EXISTS " + SQLTableName.POSITIONDATA + " (id INTEGER PRIMARY KEY AUTOINCREMENT, pid INT NOL NULL, starttime INT, endtime INT)";
    private static final String sqlTableDevicePositionData = "CREATE TABLE IF NOT EXISTS " + SQLTableName.DEVICEPOSITIONDATA + " (id INTEGER PRIMARY KEY AUTOINCREMENT, pid INT NOL NULL, starttime INT, endtime INT)";
    private static final String sqlTableTansferLog         = "CREATE TABLE IF NOT EXISTS " + SQLTableName.TRANSFERLOG + "(id INTEGER PRIMARY KEY AUTOINCREMENT, transfer INT NOT NULL, entries int NOT NULL)";

    private static final String DATABASE_NAME    = "sensordata.db";
    private static final int    DATABASE_VERSION = 3;


    public DatabaseHelper(Context cxt)
    {
        super(cxt, DATABASE_NAME, null, DATABASE_VERSION);

        this.context = cxt;
    }


    @Override
    public void onCreate(SQLiteDatabase db)
    {
        // nothing
    }


    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
    {
        // nothing
    }


    public static void createDeviceDependentTables(String deviceID)
    {
        AccelerometerCollector.createDBStorage(deviceID);
        GravityCollector.createDBStorage(deviceID);
        GyroscopeCollector.createDBStorage(deviceID);
        LinearAccelerationCollector.createDBStorage(deviceID);
        MagnetometerCollector.createDBStorage(deviceID);
        OrientationCollector.createDBStorage(deviceID);
        PressureCollector.createDBStorage(deviceID);
        RotationVectorCollector.createDBStorage(deviceID);
        StepDetectorCollector.createDBStorage(deviceID);
        StepCounterCollector.createDBStorage(deviceID);
    }


    void deleteDatabase()
    {
        this.context.deleteDatabase(DATABASE_NAME);
    }


    static void createTables()
    {
        SQLDBController.getInstance().execSQL(sqlTableActivityData);
        SQLDBController.getInstance().execSQL(sqlTablePostureData);
        SQLDBController.getInstance().execSQL(sqlTablePositionData);
        SQLDBController.getInstance().execSQL(sqlTableDevicePositionData);
        SQLDBController.getInstance().execSQL(sqlTableTansferLog);
    }
}