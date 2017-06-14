package de.unima.ar.collector.sensors;

import android.hardware.Sensor;

import de.unima.ar.collector.util.SensorDataUtil;


/**
 * @author Fabian Kramm
 */
class SensorCollectorFactory
{
    /**
     * @param type   Typ des sensors
     * @param sensor Referenz zum Sensor, wenn null wird versucht default sensor zu nehmen
     * @return Collector Klasse die sich um das Sammeln der Daten kümmert
     */
    static SensorCollector getCollector(int type, Sensor sensor)
    {
        String sensorType = SensorDataUtil.getSensorType(type);

        switch(sensorType) {
            case "TYPE_ACCELEROMETER":
                return new AccelerometerSensorCollector(sensor);
            case "TYPE_LIGHT":
                return new LightSensorCollector(sensor);
            case "TYPE_GRAVITY":
                return new GravitySensorCollector(sensor);
            case "TYPE_AMBIENT_TEMPERATURE":
                return new AmbientTemperatureSensorCollector(sensor);
            case "TYPE_MAGNETIC_FIELD":
                return new MagneticFieldSensorCollector(sensor);
            case "TYPE_PRESSURE":
                return new PressureSensorCollector(sensor);
            case "TYPE_PROXIMITY":
                return new ProximitySensorCollector(sensor);
            case "TYPE_GYROSCOPE":
                return new GyroscopeSensorCollector(sensor);
            case "TYPE_LINEAR_ACCELERATION":
                return new LinearAccelerationSensorCollector(sensor);
            case "TYPE_RELATIVE_HUMIDITY":
                return new RelativeHumiditySensorCollector(sensor);
            case "TYPE_ROTATION_VECTOR":
                return new RotationVectorSensorCollector(sensor);
            case "TYPE_ORIENTATION":
                return new OrientationSensorCollector(sensor);
            case "TYPE_STEP_DETECTOR":
                return new StepDetectorSensorCollector(sensor);
            case "TYPE_STEP_COUNTER":
                return new StepCounterSensorCollector(sensor);
        }

        return null;
    }
}