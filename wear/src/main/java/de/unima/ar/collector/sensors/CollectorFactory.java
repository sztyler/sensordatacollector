package de.unima.ar.collector.sensors;

import de.unima.ar.collector.sensors.collectors.AccelerometerCollector;
import de.unima.ar.collector.sensors.collectors.Collector;
import de.unima.ar.collector.sensors.collectors.GravityCollector;
import de.unima.ar.collector.sensors.collectors.GyroscopeCollector;
import de.unima.ar.collector.sensors.collectors.LinearAccelerationCollector;
import de.unima.ar.collector.sensors.collectors.MagnetometerCollector;
import de.unima.ar.collector.sensors.collectors.OrientationCollector;
import de.unima.ar.collector.sensors.collectors.PressureCollector;
import de.unima.ar.collector.sensors.collectors.RotationVectorCollector;
import de.unima.ar.collector.sensors.collectors.StepCounterCollector;
import de.unima.ar.collector.sensors.collectors.StepDetectorCollector;

class CollectorFactory
{
    static Collector getCollector(int type)
    {
        switch(type) {
            case 1:
                return new AccelerometerCollector();
            case 2:
                return new MagnetometerCollector();
            case 3:
                return new OrientationCollector();
            case 4:
                return new GyroscopeCollector();
            case 6:
                return new PressureCollector();
            case 9:
                return new GravityCollector();
            case 10:
                return new LinearAccelerationCollector();
            case 11:
                return new RotationVectorCollector();
            case 18:
                return new StepDetectorCollector();
            case 19:
                return new StepCounterCollector();
        }

        return null;
    }
}
