package de.unima.ar.collector.sensors;

import de.unima.ar.collector.util.SensorDataUtil;

/**
 * @author Fabian Kramm
 */
public class CollectorConstants
{
    public static final int[] activatedCustomCollectors = new int[]{ SensorDataUtil.getSensorTypeInt("TYPE_MICROPHONE"), SensorDataUtil.getSensorTypeInt("TYPE_GPS") };
}