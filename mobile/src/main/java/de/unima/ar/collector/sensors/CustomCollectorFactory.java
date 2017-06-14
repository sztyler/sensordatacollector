package de.unima.ar.collector.sensors;

import android.content.Context;

import de.unima.ar.collector.util.SensorDataUtil;


/**
 * @author Fabian Kramm
 */
class CustomCollectorFactory
{
    /**
     * @param type Typ des collectors
     * @return Collector Klasse die sich um das Sammeln der Daten kümmert
     */
    static CustomCollector getCollector(int type, Context context)
    {
        String sensorType = SensorDataUtil.getSensorType(type);

        switch(sensorType) {
            case "TYPE_MICROPHONE":
                return new MicrophoneCollector();
            case "TYPE_GPS":
                return new GPSCollector(context);
            case "TYPE_VIDEO":
                return new VideoCollector();
        }

        return null;
    }
}