package de.unima.ar.collector.sensors;

import android.util.Log;

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

public class SensorDataUtil
{
    public static void flushSensorDataCache(final int type)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if(type == 1 || type == 0) {
                    AccelerometerCollector.flushDBCache();
                }
                if(type == 2 || type == 0) {
                    MagnetometerCollector.flushDBCache();
                }
                if(type == 3 || type == 0) {
                    OrientationCollector.flushDBCache();
                }
                if(type == 4 || type == 0) {
                    GyroscopeCollector.flushDBCache();
                }
                //                if(type == 5 || type == 0) {
                //                    LightSensorCollector.flushDBCache();
                //                }
                if(type == 6 || type == 0) {
                    PressureCollector.flushDBCache();
                }
                //                if(type == 8 || type == 0) {
                //                    ProximityCollector.flushDBCache();
                //                }
                if(type == 9 || type == 0) {
                    GravityCollector.flushDBCache();
                }
                if(type == 10 || type == 0) {
                    LinearAccelerationCollector.flushDBCache();
                }
                if(type == 11 || type == 0) {
                    RotationVectorCollector.flushDBCache();
                }
                //                if(type == 12 || type == 0) {
                //                    RelativeHumiditySensorCollector.flushDBCache();
                //                }
                //                if(type == 13 || type == 0) {
                //                    AmbientTemperatureSensorCollector.flushDBCache();
                //                }
                if(type == 18 || type == 0) {
                    StepDetectorCollector.flushDBCache();
                }
                if(type == 19 || type == 0) {
                    StepCounterCollector.flushDBCache();
                }

                Log.d("TIMOSENSOR", "FLUSH DONE");
                SensorService.getInstance().forceDBObserver();
            }
        }).start();
    }
}