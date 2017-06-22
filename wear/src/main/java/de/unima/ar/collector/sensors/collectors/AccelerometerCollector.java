package de.unima.ar.collector.sensors.collectors;


import android.content.ContentValues;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.database.DBUtils;
import de.unima.ar.collector.database.SQLDBController;
import de.unima.ar.collector.sensors.SensorService;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.DeviceID;

public class AccelerometerCollector extends Collector
{
    private static final int      type       = 1;
    private static final String[] valueNames = new String[]{ "attr_x", "attr_y", "attr_z", "attr_time" };

    private boolean isRegistered = false;
    private int     sensorRate   = 0;
    private float[] gravity      = new float[]{ 0, 0, 0 };

    private static Map<String, List<String[]>> cache = new HashMap<>();

    private long startTimer = -1;
    private long counter    = -1;


    @Override
    public void onSensorChanged(SensorEvent event)
    {
        float[] values = event.values.clone();
        long time = System.currentTimeMillis();

        if(!(1 + (int) ((time - startTimer) / (this.sensorRate / 1000)) > counter)) {
            return;
        }
        counter++;

        float x = values[0];
        float y = values[1];
        float z = values[2];

        if(Settings.ACCLOWPASS) { // low pass filter
            final float alpha = (float) 0.8;
            gravity[0] = alpha * gravity[0] + (1 - alpha) * values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * values[2];

            x = values[0] - gravity[0];
            y = values[1] - gravity[1];
            z = values[2] - gravity[2];
        }

        String deviceID = DeviceID.get(SensorService.getInstance());

        if(Settings.WEARTRANSFERDIRECT) {
            String record = valueNames[0] + ";" + x + ";" + valueNames[1] + ";" + y + ";" + valueNames[2] + ";" + z + ";" + valueNames[3] + ";" + time;
            BroadcastService.getInstance().sendMessage("/sensor/data/" + deviceID + "/" + type, record);
        } else {
            ContentValues newValues = new ContentValues();
            newValues.put(valueNames[0], x);
            newValues.put(valueNames[1], y);
            newValues.put(valueNames[2], z);
            newValues.put(valueNames[3], time);

            AccelerometerCollector.writeDBStorage(deviceID, newValues);
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // TODO
    }


    @Override
    public int getType()
    {
        return type;
    }


    @Override
    public void setSensorRate(int rate)
    {
        this.sensorRate = rate;
    }


    @Override
    public int getSensorRate()
    {
        return sensorRate;
    }


    @Override
    public void setRegisteredState(boolean b)
    {
        this.isRegistered = b;

        if(this.isRegistered) {
            this.startTimer = System.currentTimeMillis();
            this.counter = 0;
        } else {
            this.startTimer = -1;
            this.counter = -1;
        }
    }


    @Override
    public boolean isRegistered()
    {
        return this.isRegistered;
    }


    public static void createDBStorage(String deviceID)
    {
        String sqlTable = "CREATE TABLE IF NOT EXISTS " + SQLTableName.PREFIX + deviceID + SQLTableName.ACCELEROMETER + " (id INTEGER PRIMARY KEY, " + valueNames[3] + " INT, " + valueNames[0] + " REAL, " + valueNames[1] + " REAL, " + valueNames[2] + " REAL)";
        SQLDBController.getInstance().execSQL(sqlTable);
    }


    public static void writeDBStorage(String deviceID, ContentValues newValues)
    {
        String tableName = SQLTableName.PREFIX + deviceID + SQLTableName.ACCELEROMETER;

        if(Settings.DATABASE_DIRECT_INSERT) {
            SQLDBController.getInstance().insert(tableName, null, newValues);
            return;
        }

        List<String[]> clone = DBUtils.manageCache(deviceID, cache, newValues, (Settings.DATABASE_CACHE_SIZE + type * 200));
        if(clone != null) {
            Log.d("TIMOSENSOR", "INSERT ACC INTO DB");
            SQLDBController.getInstance().bulkInsert(tableName, clone);
        }
    }


    public static void flushDBCache()
    {
        if(cache.keySet().size() != 0) {
            Log.d("TIMOSENSOR", "FLUSH ACC INTO DB - " + cache.values().iterator().next().size());
        } else {
            Log.d("TIMOSENSOR", "FLUSH ACC INTO DB - CACHE EMPTY");
        }
        DBUtils.flushCache(SQLTableName.ACCELEROMETER, cache);
    }
}