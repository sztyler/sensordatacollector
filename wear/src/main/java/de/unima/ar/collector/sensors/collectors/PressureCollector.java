package de.unima.ar.collector.sensors.collectors;

import android.content.ContentValues;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

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

public class PressureCollector extends Collector
{
    private static final int      type       = 6;
    private static final String[] valueNames = new String[]{ "attr_millibar", "attr_time" };

    private boolean isRegistered = false;
    private int     sensorRate   = 0;

    private static Map<String, List<String[]>> cache = new HashMap<>();


    @Override
    public void onSensorChanged(SensorEvent event)
    {
        float[] values = event.values.clone();
        long time = System.currentTimeMillis();

        String deviceID = DeviceID.get(SensorService.getInstance());

        if(Settings.WEARTRANSFERDIRECT) {
            String record = valueNames[0] + ";" + values[0] + ";" + valueNames[1] + ";" + time;
            BroadcastService.getInstance().sendMessage("/sensor/data/" + deviceID + "/" + type, record);
        } else {
            ContentValues newValues = new ContentValues();
            newValues.put(valueNames[0], values[0]);
            newValues.put(valueNames[1], time);

            PressureCollector.writeDBStorage(deviceID, newValues);
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
    }


    @Override
    public boolean isRegistered()
    {
        return this.isRegistered;
    }


    public static void createDBStorage(String deviceID)
    {
        String sqlTable = "CREATE TABLE IF NOT EXISTS " + SQLTableName.PREFIX + deviceID + SQLTableName.PRESSURE + " (id INTEGER PRIMARY KEY, " + valueNames[1] + " INT, " + valueNames[0] + " REAL)";
        SQLDBController.getInstance().execSQL(sqlTable);
    }


    public static void writeDBStorage(String deviceID, ContentValues newValues)
    {
        String tableName = SQLTableName.PREFIX + deviceID + SQLTableName.PRESSURE;

        if(Settings.DATABASE_DIRECT_INSERT) {
            SQLDBController.getInstance().insert(tableName, null, newValues);
            return;
        }

        List<String[]> clone = DBUtils.manageCache(deviceID, cache, newValues, (Settings.DATABASE_CACHE_SIZE + type * 200));
        if(clone != null) {
            SQLDBController.getInstance().bulkInsert(tableName, clone);
        }
    }


    public static void flushDBCache()
    {
        DBUtils.flushCache(SQLTableName.PRESSURE, cache);
    }
}