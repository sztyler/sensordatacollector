package de.unima.ar.collector.sensors.collectors;

import android.hardware.SensorEventListener;

public abstract class Collector implements SensorEventListener
{
    public abstract int getType();

    public abstract boolean isRegistered();

    public abstract void setRegisteredState(boolean b);

    public abstract int getSensorRate();

    public abstract void setSensorRate(int rate);
}
