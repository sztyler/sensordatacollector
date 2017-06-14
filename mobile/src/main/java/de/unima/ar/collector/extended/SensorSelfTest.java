package de.unima.ar.collector.extended;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

import de.unima.ar.collector.R;
import de.unima.ar.collector.util.SensorDataUtil;


public class SensorSelfTest implements SensorEventListener
{
    private static final double nbElements = 30;
    private SensorManager sensorManager;
    private Sensor        sensor;
    private Activity      context;

    private long now  = 0;
    private int  temp = 0;


    public SensorSelfTest(Activity context, Sensor sensor)
    {
        this.context = context;
        this.sensor = sensor;
        this.context.setContentView(R.layout.sensorselftest);

        sensorManager = (SensorManager) this.context.getSystemService(Activity.SENSOR_SERVICE);

        boolean accelSupported = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        if(!accelSupported) {
            sensorManager.unregisterListener(this, this.sensor);
            ((TextView) this.context.findViewById(R.id.acc)).setText(sensor.getName() + " " + context.getString(R.string.sensor_selftest_notdetected));
        }

        TextView acc = (TextView) context.findViewById(R.id.acc);
        acc.setText(context.getString(R.string.sensor_selftest_calc) + " (" + sensor.getName() + ") ...");

        String name = SensorDataUtil.getSensorType(sensor.getType()).substring(5).replace("_", "");
        TextView ssthead = (TextView) context.findViewById(R.id.ssthead);
        ssthead.setText(context.getString(R.string.sensor_selftest_title) + " (" + name + ")");
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // Nothing to do
    }


    @Override
    public void onSensorChanged(SensorEvent event)
    {
        try {
            long tS;
            long time;
            // only if the event is from the accelerometer
            if(event.sensor.getType() == sensor.getType()) {
                float x, y, z;
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                // Get timestamp of the event
                tS = event.timestamp;

                ((TextView) context.findViewById(R.id.axex)).setText(context.getString(R.string.sensor_selftest_x) + " : " + x);
                ((TextView) context.findViewById(R.id.axey)).setText(context.getString(R.string.sensor_selftest_y) + " : " + y);
                ((TextView) context.findViewById(R.id.axez)).setText(context.getString(R.string.sensor_selftest_z) + " : " + z);

                // Get the mean frequency for "nbElements" (=30) elements
                if(now != 0) {
                    temp++;
                    if(temp == nbElements) {
                        time = tS - now;

                        double hzValue = nbElements * 1000000000 / time;

                        ((TextView) context.findViewById(R.id.acc)).setText(context.getString(R.string.sensor_selftest_frequency) + " : " + hzValue + " Hz");
                        temp = 0;
                    }
                }
                // To set up now on the first event and do not change it while we do not have "nbElements" events
                if(temp == 0) {
                    now = tS;
                }
            }
        } catch(Exception e) {
            // do nothing
        }
    }


    public Sensor getSensor()
    {
        return sensor;
    }


    public void resumeTest()
    {
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }


    public void stopTest()
    {
        sensorManager.unregisterListener(this);
    }
}