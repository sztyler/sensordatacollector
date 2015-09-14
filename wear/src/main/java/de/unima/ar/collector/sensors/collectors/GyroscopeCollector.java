package de.unima.ar.collector.sensors.collectors;

import android.content.ContentValues;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.database.SQLDBController;
import de.unima.ar.collector.sensors.SensorService;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.DeviceID;

public class GyroscopeCollector extends Collector
{
    private static final int      type       = 4;
    private static final String[] valueNames = new String[]{ "attr_x", "attr_y", "attr_z", "attr_time" };

    private boolean isRegistered = false;
    private int     sensorRate   = 0;


    @Override
    public void onSensorChanged(SensorEvent event)
    {
        float[] values = event.values.clone();
        long time = System.currentTimeMillis();

        String deviceID = DeviceID.get(SensorService.getInstance());

        if(Settings.WEARDIRECTTRANSFER) {
            String record = valueNames[0] + ";" + values[0] + ";" + valueNames[1] + ";" + values[1] + ";" + valueNames[2] + ";" + values[2] + ";" + valueNames[3] + ";" + time;
            BroadcastService.getInstance().sendMessage("/sensor/data/" + deviceID + "/" + type, record);
        } else {
            ContentValues newValues = new ContentValues();
            newValues.put(valueNames[0], values[0]);
            newValues.put(valueNames[1], values[1]);
            newValues.put(valueNames[2], values[2]);
            newValues.put(valueNames[3], time);

            GyroscopeCollector.writeDBStorage(deviceID, newValues);
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
        String sqlTable = "CREATE TABLE IF NOT EXISTS " + SQLTableName.PREFIX + deviceID + SQLTableName.GYROSCOPE + " (id INTEGER PRIMARY KEY, " + valueNames[3] + " INT, " + valueNames[0] + " REAL, " + valueNames[1] + " REAL, " + valueNames[2] + " REAL)";
        SQLDBController.getInstance().execSQL(sqlTable);
    }


    public static void writeDBStorage(String deviceID, ContentValues newValues)
    {
        SQLDBController.getInstance().insert(SQLTableName.PREFIX + deviceID + SQLTableName.GYROSCOPE, null, newValues);
    }
}
