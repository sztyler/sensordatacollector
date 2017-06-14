package de.unima.ar.collector.sensors;

import java.util.List;

import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.extended.Plotter;
import de.unima.ar.collector.shared.database.SQLTableName;


/**
 * @author Timo Sztyler, Fabian Kramm
 */
abstract public class CustomCollector
{
    long sensorRate;  // milliseconds
    private boolean isRegistered = false;


    public CustomCollector()
    {
        String queryString = "SELECT freq FROM " + SQLTableName.SENSOROPTIONS + " WHERE sensor = ? ";
        String[] queryArgs = new String[]{ String.valueOf(getType()) };

        List<String[]> result = SQLDBController.getInstance().query(queryString, queryArgs, false);

        this.sensorRate = (result.size() != 0) ? Long.valueOf(result.get(0)[0]) : -1L; // milliseconds
    }


    void register()
    {
        this.isRegistered = true;
        onRegistered();
    }


    public void setSensorRate(long milliseconds)
    {
        this.sensorRate = milliseconds;
    }


    public long getSensorRate()
    {
        return this.sensorRate; // milliseconds
    }


    public boolean isRegistered()
    {
        return this.isRegistered;
    }


    void deregister()
    {
        this.isRegistered = false;
        onDeRegistered();
    }


    public abstract void onRegistered();

    public abstract void onDeRegistered();

    public abstract int getType();

    public abstract Plotter getPlotter(String deviceID);
}