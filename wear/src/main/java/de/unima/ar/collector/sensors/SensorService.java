package de.unima.ar.collector.sensors;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import de.unima.ar.collector.database.DBObserver;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.database.SQLDBController;

public class SensorService extends Service
{
    private static SensorService INSTANCE = null;
    private static boolean       created  = false;
    private static DBObserver    dbo      = null;

    private SensorManager         scm;
    private PowerManager.WakeLock wakeLock;
    private Thread                dbThread;


    public SensorService() // has to be public because it is a service
    {
        super();

        if(INSTANCE == null) {
            INSTANCE = this;
        }

    }


    public static SensorService getInstance()
    {
        if(INSTANCE == null) {
            new SensorService();
        }

        return INSTANCE;
    }


    public boolean enableCollector(int type, int rate)
    {
        return this.scm.enableCollector(type, rate);
    }


    //    public boolean removeSensor(int type)
    //    {
    //        boolean result = this.scm.unregisterCollector(type);
    //
    //        if(result) {
    //            this.scm.disableCollector(type);
    //        }
    //
    //        return result;
    //    }


    public void registerCollectors()
    {
        this.scm.registerCollectors();
    }


    public SensorManager getSCM()
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
                    scm.unregisterCollectors();
                    scm.registerCollectors();
                }
            };
            new Handler().postDelayed(runnable, 600);
        }
    };


    public void informDBObserver(int hashCode)
    {
        Log.d("DBObseverTIMO", String.valueOf(dbo == null));
        if(dbo == null) {
            return;
        }

        dbo.addConfirmedTransaction(hashCode);
    }


    public void forceDBObserver()
    {
        Log.d("TIMOSENSOR", "DB THREAD1: " + (this.dbThread == null ? "null" : String.valueOf(this.dbThread.isAlive())));
        if(!this.dbThread.isAlive()) {
            if(dbo == null) {
                dbo = new DBObserver();
            }

            this.dbThread = new Thread(dbo);
            this.dbThread.start();
        }
        Log.d("TIMOSENSOR", "DB THREAD2: " + (this.dbThread == null ? "null" : String.valueOf(this.dbThread.isAlive())));

        dbo.forceSending(this.getBaseContext());
    }


    @Override
    public void onCreate()
    {
        if(this.scm != null) {
            return;
        }

        SQLDBController.initInstance(this);
        scm = new SensorManager(this);

        if(dbo == null) {
            dbo = new DBObserver();
            this.dbThread = new Thread(dbo);
            this.dbThread.start();
        }

        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        String deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        DatabaseHelper.createDeviceDependentTables(deviceID);

        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorManager");
        this.wakeLock.acquire();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(!created) {
            created = true;
            scm.registerCollectors();
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
        created = false;
        wakeLock.release();
        unregisterReceiver(mReceiver);
        dbo.shutdown();
    }


    //    public boolean hasWakelock()
    //    {
    //        return this.wakeLock != null && this.wakeLock.isHeld();
    //    }
}