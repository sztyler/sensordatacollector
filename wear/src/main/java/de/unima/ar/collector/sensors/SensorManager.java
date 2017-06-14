package de.unima.ar.collector.sensors;


import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.unima.ar.collector.sensors.collectors.Collector;

public class SensorManager
{
    private Context                        context;
    private Map<Integer, Collector>        collectors;
    private Set<Integer>                   enabledCollectors;
    private android.hardware.SensorManager sensorManager;


    SensorManager(Context context)
    {
        this.context = context;

        this.collectors = new HashMap<>();
        this.enabledCollectors = new HashSet<>();
        this.sensorManager = (android.hardware.SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);

        initSensors();
    }


    public boolean disableCollector(int type)
    {
        return this.enabledCollectors.remove(type);
    }


    public Set<Integer> getEnabledCollectors()
    {
        return this.enabledCollectors;
    }


    public boolean unregisterCollector(int type)
    {
        for(Collector col : this.collectors.values()) {
            if(!(col.isRegistered() && col.getType() == type && this.enabledCollectors.contains(col.getType()))) {
                continue;
            }

            this.sensorManager.unregisterListener(col);
            col.setRegisteredState(false);
            return true;
        }

        return false;
    }


    public void unregisterCollectors()
    {
        for(Collector col : this.collectors.values()) {
            if(col.isRegistered()) {
                this.sensorManager.unregisterListener(col);
                col.setRegisteredState(false);
            }
        }
    }


    public Set<Integer> getImplementedSensors()
    {
        return this.collectors.keySet();
    }


    boolean enableCollector(int type, int rate)
    {
        if(!this.collectors.containsKey(type)) {
            return false;
        }
        this.enabledCollectors.add(type);
        this.collectors.get(type).setSensorRate(rate);

        return true;
    }


    void registerCollectors()
    {
        for(Collector col : this.collectors.values()) {
            if(col.isRegistered() || !enabledCollectors.contains(col.getType())) {
                continue;
            }

            Sensor sensor = this.sensorManager.getDefaultSensor(col.getType());

            if(sensor == null) {
                continue;
            }

            this.sensorManager.registerListener(col, sensor, col.getSensorRate());
            col.setRegisteredState(true);
        }
    }


    private void initSensors()
    {
        // Add all sensors
        android.hardware.SensorManager mSensorManager = (android.hardware.SensorManager) this.context.getSystemService(Activity.SENSOR_SERVICE);
        List<Sensor> allSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        for(Sensor sensor : allSensors) {
            this.addCollector(sensor.getType());
        }
    }


    private boolean addCollector(int type)
    {
        Collector col = CollectorFactory.getCollector(type);

        if(col == null) {
            return false;
        }

        if(this.collectors.containsKey(col.getType())) {
            return true;
        }

        this.collectors.put(col.getType(), col);

        return true;
    }
}