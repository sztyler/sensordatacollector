package de.unima.ar.collector.sensors;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.extended.Plotter;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableName;


/**
 * @author Fabian Kramm
 */
abstract public class CustomCollector
{
    private int   sensorRate;
    private Timer timer;

    public boolean isRegistered = false;


    public CustomCollector()
    {
        String queryString = "SELECT freq FROM " + SQLTableName.SENSOROPTIONS + " WHERE sensor = ? ";
        String[] queryArgs = new String[]{ "" + getType() };

        List<String[]> result = SQLDBController.getInstance().query(queryString, queryArgs, false);

        //        sensorRate = (result.size() != 0) ? Integer.valueOf(result.get(0)[0]) : 300;
        //
        //        if(sensorRate < 50) {
        //            sensorRate = 50;
        //        }
        sensorRate = 5000;
    }


    public void register()
    {
        this.isRegistered = true;
        onRegistered();
    }


    public void setSensorRate(int hertz)
    {
        this.sensorRate = (int) ((1000.0d / hertz) * 1000.0d); // hertz -> microseconds
    }


    public long getSensorRate()
    {
        return Settings.GPS_DEFAULT_FREQUENCY;
    }


    public boolean isRegistered()
    {
        return isRegistered;
    }


    public void deregister()
    {
//        this.timer.cancel();

        onDeRegistered();
        this.isRegistered = false;
    }


    //    public abstract boolean plottingEnabled();


    public abstract void onRegistered();


    public abstract void onDeRegistered();


    public abstract void doTask();


    public abstract int getType();

    public abstract Plotter getPlotter(String deviceID);


    private class MyTimerTask extends TimerTask
    {
        @Override
        public void run()
        {
            doTask();
        }
    }
}