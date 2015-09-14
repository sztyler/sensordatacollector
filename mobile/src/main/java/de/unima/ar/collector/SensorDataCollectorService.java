package de.unima.ar.collector;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

import java.util.List;
import java.util.Set;

import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.api.ListenerService;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.sensors.SensorCollectorManager;
import de.unima.ar.collector.shared.database.SQLTableName;


/**
 * Singleton Service Klasse die unsere Background Arbeit erledigt
 *
 * @author Fabian Kramm, Timo Sztyler
 * @version 11.12.2014
 */
public class SensorDataCollectorService extends Service
{
    private static SensorDataCollectorService INSTANCE = null;
    private static boolean                    created  = false;

    private SensorCollectorManager scm;
    private WakeLock               mWakeLock;


    public SensorDataCollectorService() // has to be public because it is a service
    {
        super();

        if(INSTANCE == null) {
            INSTANCE = this;
        }
    }


    public static SensorDataCollectorService getInstance()
    {
        if(INSTANCE == null) {
            new SensorDataCollectorService();
        }

        return INSTANCE;
    }


    public SensorCollectorManager getSCM()
    {
        return this.scm;
    }


    /**
     * observe action screen off - important part! - ensures that the sensors are working correct even if the screen turns off
     */
    public BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                return;
            }
            Runnable runnable = new Runnable()
            {
                public void run()
                {
                    scm.unregisterSensorCollectors();
                    scm.registerSensorCollectors();
                }
            };
            new Handler().postDelayed(runnable, 600);
        }
    };


    @Override
    public void onCreate()
    {
        if(this.scm != null) {
            return;
        }

        SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        SQLDBController.getInstance();
        scm = new SensorCollectorManager(this);
        // scm.addSensorCollector(new TestSensorCollector());
        // scm.addSensorCollector(new AccelerometerSensorCollector());
        // scm.openDatabase();

        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        // refresh/update DB
        Set<String> devices = ListenerService.getDevices();
        for(String device : devices) {
            DatabaseHelper.createDeviceDependentTables(device);
            SQLDBController.getInstance().registerDevice(device);
        }

        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorCollectorManager");
        this.mWakeLock.acquire();

        // reactivate sensors
        List<String[]> sensors = SQLDBController.getInstance().query("SELECT sensor FROM " + SQLTableName.SENSOROPTIONS + " WHERE enabled=1", null, false);
        int cou = 0;
        for(String[] sensor : sensors) {
            try {
                int sensorID = Integer.valueOf(sensor[0]);
                if(!scm.enableCollectors(sensorID)) {
                    continue;
                } else if(sensorID > 0) {
                    BroadcastService.getInstance().sendMessage("/sensor/register", "[" + sensorID + ", " + scm.getSensorCollectors().get(sensorID).getSensorRate() + "]");
                }
                cou++;
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        if(cou > 0) {
            scm.registerSensorCollectors();
            scm.registerCustomCollectors();
            Toast.makeText(this, getString(R.string.sensor_collector_reenable_notify) + " (" + cou + ")", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(!created) {
            created = true;

            // Start registered collectors
            scm.registerSensorCollectors();
        }

        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }


    @Override
    public void onDestroy()
    {
        // Den SensorCollectorManager schließen
        // scm.close();
        created = false;

        mWakeLock.release();

        unregisterReceiver(mReceiver);
    }
}