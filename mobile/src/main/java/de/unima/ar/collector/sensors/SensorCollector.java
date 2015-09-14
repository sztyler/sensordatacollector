package de.unima.ar.collector.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.util.Arrays;
import java.util.List;

import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.extended.Plotter;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableName;


/**
 * To make new sensor u have to add sensor collector in:
 * <p/>
 * - SensorCollectorFactory.java
 * - SensorDataDatabaseHelper.java
 * - PlotConfiguration.java
 *
 * @author Fabian Kramm
 */
abstract public class SensorCollector implements SensorEventListener
{
    //
    public boolean writeOnSensorChanged = false;
    // Sagt ob Collector im SensorManager registriert werden soll
    public boolean registerCollector    = true;
    // Wurde Collector registriert?
    public boolean isRegistered         = false;

    // Referenz zum eigentlichen Sensor, wenn null dann wird default Sensor für diesen Type genommen
    protected Sensor sensor;
    private   double sensorRate;

    // Rate mit der der Sensor aktualisiert werden soll
    // private int sensorRate = SensorManager.SENSOR_DELAY_NORMAL;
    // private int sensorRateFastest = SensorManager.SENSOR_DELAY_FASTEST;
    private long LastUpdate = 0;


    public SensorCollector(Sensor sensor)
    {
        this.sensor = sensor;

        String queryString = "SELECT freq FROM " + SQLTableName.SENSOROPTIONS + " WHERE sensor = ? ";
        String[] queryArgs = new String[]{ String.valueOf(this.getType()) };

        List<String[]> result = SQLDBController.getInstance().query(queryString, queryArgs, false);
        double hertz = (result.size() != 0) ? Double.valueOf(result.get(0)[0]) : Settings.SENSOR_DEFAULT_FREQUENCY;
        sensorRate = (1000.0d / hertz) * 1000.0d; // hertz -> microseconds
    }


    @Override
    public synchronized void onSensorChanged(SensorEvent se)
    {
        float[] clone = se.values.clone();
        long time = System.currentTimeMillis();
        SensorChanged(clone, time);
    }


    public void setSensorRate(double hertz)
    {
        sensorRate = (1000.0d / hertz) * 1000.0d; // hertz -> microseconds
    }


    public int getSensorRate()
    {
        return (int) sensorRate;    // microseconds
    }


    /**
     * @return Wenn es einen Sensor gibt dann gebe diesen zurück
     */
    public Sensor getSensor()
    {
        return (sensor == null) ? null : sensor;
    }


    // Gibt den TYPE des Sensors zurück
    public abstract int getType();

    public abstract void SensorChanged(float[] values, long time);

    public abstract Plotter getPlotter(String deviceID);


    public static void updateLivePlotter(String deviceID, float[] values)
    {
        throw new IllegalStateException("Method hasn't been set up in the subclass (" + deviceID + ", " + Arrays.toString(values) + ")");
    }


    public static void createDBStorage()
    {
        throw new IllegalStateException("Method hasn't been set up in the subclass");
    }


    public static void writeDBStorage()
    {
        throw new IllegalStateException("Method hasn't been set up in the subclass");
    }


    public static void flushDBCache()
    {
        throw new IllegalStateException("Method hasn't been set up in the subclass");
    }
}