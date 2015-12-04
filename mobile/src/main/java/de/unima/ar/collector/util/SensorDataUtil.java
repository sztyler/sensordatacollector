package de.unima.ar.collector.util;

import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;

import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Set;
import java.util.TreeSet;

import de.unima.ar.collector.SensorDataCollectorService;
import de.unima.ar.collector.sensors.AccelerometerSensorCollector;
import de.unima.ar.collector.sensors.AmbientTemperatureSensorCollector;
import de.unima.ar.collector.sensors.GravitySensorCollector;
import de.unima.ar.collector.sensors.GyroscopeSensorCollector;
import de.unima.ar.collector.sensors.LightSensorCollector;
import de.unima.ar.collector.sensors.LinearAccelerationSensorCollector;
import de.unima.ar.collector.sensors.MagneticFieldSensorCollector;
import de.unima.ar.collector.sensors.OrientationSensorCollector;
import de.unima.ar.collector.sensors.PressureSensorCollector;
import de.unima.ar.collector.sensors.ProximitySensorCollector;
import de.unima.ar.collector.sensors.RelativeHumiditySensorCollector;
import de.unima.ar.collector.sensors.RotationVectorSensorCollector;
import de.unima.ar.collector.sensors.SensorCollectorManager;
import de.unima.ar.collector.sensors.StepCounterSensorCollector;
import de.unima.ar.collector.sensors.StepDetectorSensorCollector;


/**
 * Util klasse hier werden verschiedene static funktionen
 * bereitgestellt
 *
 * @author Fabian Kramm
 */
public class SensorDataUtil
{
    /**
     * Sets up a plot for the specific plot style
     * which is used for showing activities etc.
     */
    public static void setUpDatabasePlot(XYPlot plot, float displayedSeriesCount)
    {
        if(displayedSeriesCount == 0) {
            displayedSeriesCount = 1;
        }

        plot.getRangeLabelWidget().setVisible(false);
        plot.getRangeLabelWidget().setSize(new SizeMetrics(0, SizeLayoutType.ABSOLUTE, 0, SizeLayoutType.ABSOLUTE));
        plot.getGraphWidget().setWidth(1, SizeLayoutType.RELATIVE);

        plot.getGraphWidget().setRangeLabelWidth(0f);
        plot.getGraphWidget().setTicksPerDomainLabel(2);

        plot.getGraphWidget().setTicksPerRangeLabel(1);
        plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 1);
        plot.setDomainStep(XYStepMode.SUBDIVIDE, 7);
        // Immer nur leere Strings anzeigen
        plot.getGraphWidget().setRangeValueFormat(new Format()
        {
            private static final long serialVersionUID = 1L;


            @Override
            public StringBuffer format(Object arg0, StringBuffer toAppendTo, FieldPosition arg2)
            {
                return toAppendTo.append("");
            }


            @Override
            public Object parseObject(String arg0, ParsePosition arg1)
            {
                return null;
            }
        });
        plot.getGraphWidget().setDomainValueFormat(new DateFormat());

        // Calc height and table model for count of series
        plot.getTitleWidget().position(10, XLayoutStyle.ABSOLUTE_FROM_LEFT, 0f, YLayoutStyle.ABSOLUTE_FROM_TOP, AnchorPosition.LEFT_TOP);
        plot.getLegendWidget().position(0f, XLayoutStyle.RELATIVE_TO_RIGHT, 0f, YLayoutStyle.ABSOLUTE_FROM_TOP, AnchorPosition.RIGHT_TOP);
        plot.getGraphWidget().position(0f, XLayoutStyle.RELATIVE_TO_RIGHT, PixelUtils.dpToPix((int) ((displayedSeriesCount + 1) / 2) * 17), YLayoutStyle.ABSOLUTE_FROM_TOP, AnchorPosition.RIGHT_TOP);

        plot.getGraphWidget().setHeight(PixelUtils.dpToPix((int) ((displayedSeriesCount + 1) / 2) * 17), SizeLayoutType.FILL);

        plot.getLegendWidget().setWidth(0.8f, SizeLayoutType.RELATIVE);
        plot.getLegendWidget().setHeight(PixelUtils.dpToPix((int) ((displayedSeriesCount + 1) / 2) * 17), SizeLayoutType.ABSOLUTE);

        plot.setRangeBoundaries(0, displayedSeriesCount + 1, BoundaryMode.FIXED);

        plot.getLegendWidget().setDrawIconBackgroundEnabled(false);
        plot.getLegendWidget().setDrawIconBorderEnabled(false);
        plot.getLegendWidget().setTableModel(new DynamicTableModel(2, (int) ((displayedSeriesCount + 1) / 2)));
        plot.getLegendWidget().setPadding(5, 5, 5, 5);

        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAlpha(40);

        plot.getLegendWidget().setBackgroundPaint(bgPaint);
    }


    /**
     * Returns a color from
     * a predefined color array
     */
    public static int getColor(int id)
    {
        int Colors[] = new int[]{ Color.rgb(100, 100, 250), Color.rgb(250, 100, 100), Color.rgb(100, 250, 100), Color.rgb(250, 100, 250), Color.rgb(250, 250, 100), Color.rgb(100, 250, 250), Color.rgb(50, 150, 250), Color.rgb(250, 100, 50), Color.rgb(10, 250, 10), Color.rgb(200, 250, 50), Color.rgb(50, 100, 250), Color.rgb(30, 60, 180), Color.rgb(200, 50, 140), Color.rgb(100, 40, 150), Color.rgb(40, 140, 40), };

        if(id >= Colors.length) {
            return Color.rgb(50, 200, 50);
        } else {
            return Colors[id];
        }
    }


    /**
     * Checks if new location is
     * remarkably diffrent from
     * the new location
     */
    public static boolean isRemarkableChange(Location oldLocation, Location newLocation)
    {
        if(oldLocation == null || newLocation == null) {
            return true;
        }

        long m = 10000;

        double diffLong = (oldLocation.getLatitude() - newLocation.getLatitude()) * m;
        double diffLat = (oldLocation.getLatitude() - newLocation.getLatitude()) * m;

        double diff = Math.sqrt(diffLong * diffLong + diffLat * diffLat);

        return diff > 7;
    }


    /**
     * @param name String der �berpr�ft werden soll
     * @return Fehler meldung, falls alles okay dann null
     */
    public static String checkName(String name)
    {
        // Zu lang?
        if(name.length() > 32) {
            return "Name too long";
        }

        if(name.length() == 0) {
            return "Empty name!";
        }

        // Sonderzeichen
        int spaces = 0;

        for(int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            // Erlaubte Zeichen
            if(c >= 'a' && c <= 'z') {
                continue;
            }
            if(c >= 'A' && c <= 'Z') {
                continue;
            }
            if(c >= '0' && c <= '9') {
                continue;
            }
            if(c == ' ') {
                spaces++;
                continue;
            }

            return "Character " + c + " not permitted!";
        }

        if(spaces == name.length()) {
            return "Only spaces are not allowed!";
        }

        return null;
    }


    /**
     * Returns the type in string from int value
     */
    public static String getSensorType(int type)
    {
        switch(type) {
            case 1:
                return "TYPE_ACCELEROMETER";
            case 13:
                return "TYPE_AMBIENT_TEMPERATURE";
            case 15:
                return "TYPE_GAME_ROTATION_VECTOR";
            case 20:
                return "TYPE_GEOMAGNETIC_ROTATION_VECTOR";
            case 9:
                return "TYPE_GRAVITY";
            case 4:
                return "TYPE_GYROSCOPE";
            case 16:
                return "TYPE_GYROSCOPE_UNCALIBRATED";
            case 5:
                return "TYPE_LIGHT";
            case 10:
                return "TYPE_LINEAR_ACCELERATION";
            case 2:
                return "TYPE_MAGNETIC_FIELD";
            case 14:
                return "TYPE_MAGNETIC_FIELD_UNCALIBRATED";
            case 3:
                return "TYPE_ORIENTATION";
            case 6:
                return "TYPE_PRESSURE";
            case 8:
                return "TYPE_PROXIMITY";
            case 12:
                return "TYPE_RELATIVE_HUMIDITY";
            case 11:
                return "TYPE_ROTATION_VECTOR";
            case 17:
                return "TYPE_SIGNIFICANT_MOTION";
            case 19:
                return "TYPE_STEP_COUNTER";
            case 18:
                return "TYPE_STEP_DETECTOR";
            case 7:
                return "TYPE_TEMPERATURE";
            case -2:
                return "TYPE_MICROPHONE";
            case -3:
                return "TYPE_GPS";
            default:
                return "TYPE_" + type;
        }
    }


    /**
     * Returns the int from an string type
     */
    public static int getSensorTypeInt(String type)
    {
        switch(type) {
            case "TYPE_ACCELEROMETER":
                return 1;
            case "TYPE_AMBIENT_TEMPERATURE":
                return 13;
            case "TYPE_GAME_ROTATION_VECTOR":
                return 15;
            case "TYPE_GEOMAGNETIC_ROTATION_VECTOR":
                return 20;
            case "TYPE_GRAVITY":
                return 9;
            case "TYPE_GYROSCOPE":
                return 4;
            case "TYPE_GYROSCOPE_UNCALIBRATED":
                return 16;
            case "TYPE_LIGHT":
                return 5;
            case "TYPE_LINEAR_ACCELERATION":
                return 10;
            case "TYPE_MAGNETIC_FIELD":
                return 2;
            case "TYPE_MAGNETIC_FIELD_UNCALIBRATED":
                return 14;
            case "TYPE_ORIENTATION":
                return 3;
            case "TYPE_PRESSURE":
                return 6;
            case "TYPE_PROXIMITY":
                return 8;
            case "TYPE_RELATIVE_HUMIDITY":
                return 12;
            case "TYPE_ROTATION_VECTOR":
                return 11;
            case "TYPE_SIGNIFICANT_MOTION":
                return 17;
            case "TYPE_STEP_COUNTER":
                return 19;
            case "TYPE_STEP_DETECTOR":
                return 18;
            case "TYPE_TEMPERATURE":
                return 7;
            case "TYPE_MICROPHONE":
                return -2;
            case "TYPE_GPS":
                return -3;
            default:
                return -1;
        }
    }


    public static void flushSensorDataCache(final int type, final String deviceID)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if(type == 1 || type == 0) {
                    AccelerometerSensorCollector.flushDBCache(deviceID);
                }
                if(type == 2 || type == 0) {
                    MagneticFieldSensorCollector.flushDBCache(deviceID);
                }
                if(type == 3 || type == 0) {
                    OrientationSensorCollector.flushDBCache(deviceID);
                }
                if(type == 4 || type == 0) {
                    GyroscopeSensorCollector.flushDBCache(deviceID);
                }
                if(type == 5 || type == 0) {
                    LightSensorCollector.flushDBCache(deviceID);
                }
                if(type == 6 || type == 0) {
                    PressureSensorCollector.flushDBCache(deviceID);
                }
                if(type == 8 || type == 0) {
                    ProximitySensorCollector.flushDBCache(deviceID);
                }
                if(type == 9 || type == 0) {
                    GravitySensorCollector.flushDBCache(deviceID);
                }
                if(type == 10 || type == 0) {
                    LinearAccelerationSensorCollector.flushDBCache(deviceID);
                }
                if(type == 11 || type == 0) {
                    RotationVectorSensorCollector.flushDBCache(deviceID);
                }
                if(type == 12 || type == 0) {
                    RelativeHumiditySensorCollector.flushDBCache(deviceID);
                }
                if(type == 13 || type == 0) {
                    AmbientTemperatureSensorCollector.flushDBCache(deviceID);
                }
                if(type == 18 || type == 0) {
                    StepDetectorSensorCollector.flushDBCache(deviceID);
                }
                if(type == 19 || type == 0) {
                    StepCounterSensorCollector.flushDBCache(deviceID);
                }
            }
        }).start();
    }


    public static String[] getNamesOfEnabledSensors(int type)   // 0 = SensorCollector, 1 = CustomCollector, 2 = beide
    {
        SensorCollectorManager scm = SensorDataCollectorService.getInstance().getSCM();

        if(scm == null) {
            return new String[]{};
        }

        Set<Integer> enabledCollectors = scm.getEnabledCollectors();
        Set<String> names = new TreeSet<>();
        for(Integer collector : enabledCollectors) {
            String sensorName = StringUtils.formatSensorName(SensorDataUtil.getSensorType(collector));

            if((type >= 1) && collector < 0) {
                names.add(sensorName);
            }

            if(type != 1 && collector > 0) {
                names.add(sensorName);
            }
        }

        return names.toArray(new String[names.size()]);
    }
}