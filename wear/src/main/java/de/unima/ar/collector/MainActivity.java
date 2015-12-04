package de.unima.ar.collector;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.listener.OnCustomTouchListener;
import de.unima.ar.collector.sensors.SensorManager;
import de.unima.ar.collector.sensors.SensorService;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.DeviceID;
import de.unima.ar.collector.shared.util.Utils;
import de.unima.ar.collector.ui.ActivitySelector;
import de.unima.ar.collector.ui.Chooser;
import de.unima.ar.collector.ui.ScreenListener;

public class MainActivity extends Activity
{
    private GestureDetector detector;
    private ScreenListener  sl;
    private boolean         onDestroyResume;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // init
        super.onCreate(savedInstanceState);
        setContentView(R.layout.round_activity_main);
        this.onDestroyResume = true;
        this.sl = new ScreenListener();
        String deviceID = DeviceID.get(this);

        // start sensor service
        startService(new Intent(MainActivity.this, SensorService.class));

        // observe screen
        startObserveScreen();

        // keep screen on
        //        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // register activity
        ActivityController.getInstance().add("MainActivity", this);

        // inform mobile phone
        BroadcastService.initInstance(this);
        BroadcastService.getInstance().getAPIClient().connect();
        if(BluetoothAdapter.getDefaultAdapter() != null) {
            BroadcastService.getInstance().sendMessage("/activity/started", deviceID + "~#X*X#~" + BluetoothAdapter.getDefaultAdapter().getAddress());
        }

        // set up layout
        final ImageButton button = (ImageButton) findViewById(R.id.welcome_button_continue);
        button.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                button.setOnTouchListener(null);
                createMainView();
                return true;
            }
        });
    }


    @Override
    public void onPause()
    {
        super.onPause();

        // update state
        if(ActivityController.State.onCreate.equals(ActivityController.getInstance().getState(this))) {
            ActivityController.getInstance().setState(this, ActivityController.State.onPause_AUTO);
        }

        // show notification
        this.displayNotification(this.createBasicNotification());
    }


    @Override
    public void onResume()
    {
        super.onResume();

        // update state
        ActivityController.getInstance().setState(this, ActivityController.State.onCreate);

        // hide notifications
        this.hideNotification();
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        // stop all sensors
        SensorManager sm = SensorService.getInstance().getSCM();
        sm.unregisterCollectors();
        sm.getEnabledCollectors().clear();

        // hide notifications
        hideNotification();

        // inform mobile app
        Log.d("onDestroyResume", onDestroyResume + "");
        if(onDestroyResume) {
            BroadcastService.getInstance().sendMessage("/activity/destroyed", DeviceID.get(this));
        }

        // stop observing screen
        stopObservingScreen();

        // destroy all other activities
        ActivityController.getInstance().shutdown();

        // stop service
        //        stopService(new Intent(MainActivity.this, ListenerService.class));
        stopService(new Intent(MainActivity.this, SensorService.class));
        //        BroadcastService.getInstance().getAPIClient().disconnect();

        //        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        //        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
        //            Log.d("Timo-S", service.service.getClassName());
        //        }

        // destroyed
        Toast.makeText(this, R.string.app_toast_destroy, Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        return this.detector == null || this.detector.onTouchEvent(ev) || super.onTouchEvent(ev);
    }


    public void updatePostureView(final String value)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                TextView posture = (TextView) findViewById(R.id.posture_posture);
                if(value.length() != 0 && !value.equalsIgnoreCase(getString(R.string.activity_general_notspecified)) && posture != null) {
                    posture.setText(value);
                    posture.setTextColor(Color.GREEN);
                } else if(posture != null) {
                    posture.setText(R.string.activity_general_notspecified);
                    posture.setTextColor(Color.RED);
                }
            }
        });
    }


    public void updatePositionView(final String value)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                TextView position = (TextView) findViewById(R.id.posture_position);
                if(value.length() != 0 && !value.equalsIgnoreCase(getString(R.string.activity_general_notspecified)) && position != null) {
                    position.setText("(" + value + ")");
                    position.setTextColor(Color.GREEN);
                } else if(position != null) {
                    position.setText("(" + getString(R.string.activity_general_notspecified) + ")");
                    position.setTextColor(Color.RED);
                }
            }
        });
    }


    public void updateActivityView(final String value)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ActivitySelector.add(Utils.split(value, '\n'));
                List<String> activites = ActivitySelector.get();

                TextView position = (TextView) findViewById(R.id.posture_activity);
                if(activites.size() == 1 && position != null) {
                    position.setText("(" + activites.get(0) + ")");
                    position.setTextColor(Color.GREEN);
                } else if(activites.size() > 1 && position != null) {
                    position.setText("(" + activites.size() + " " + getString(R.string.activity_general_activities) + ")");
                    position.setTextColor(Color.GREEN);
                } else if(position != null) {
                    position.setText("(" + getString(R.string.activity_general_notspecified) + ")");
                    position.setTextColor(Color.RED);
                }
            }
        });
    }


    public void updateOnDestroyResumeStatus(boolean value)
    {
        this.onDestroyResume = value;
    }


    private void createMainView()
    {
        setContentView(R.layout.round_posture);

        //  touch listener
        this.detector = new GestureDetector(this, new OnCustomTouchListener(this));

        // positionAndposture
        ImageButton positionAndposture = (ImageButton) findViewById(R.id.posture_setting_button);
        positionAndposture.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    // update state
                    ActivityController.getInstance().setState(MainActivity.this, ActivityController.State.onPause_CHOOSER);

                    // start chooser
                    Intent i = new Intent(getApplicationContext(), Chooser.class);
                    startActivity(i);
                }
                return true;
            }
        });

        // activitySelector
        ImageButton activitySelector = (ImageButton) findViewById(R.id.activity_selector_button);
        activitySelector.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    // update state
                    ActivityController.getInstance().setState(MainActivity.this, ActivityController.State.onPause_ACTIVITYSELECTOR);

                    // start activity selector
                    Intent i = new Intent(getApplicationContext(), ActivitySelector.class);
                    startActivity(i);
                }
                return true;
            }
        });

        // query current values
        String query = "SELECT T2.name FROM " + SQLTableName.POSTUREDATA + " as T1 JOIN " + SQLTableName.POSTURES + " as T2 ON T1.pid = T2.id WHERE T1.endtime = 0";
        BroadcastService.getInstance().sendMessage("/database/request/currentPosture", query);

        String query2 = "SELECT T2.name FROM " + SQLTableName.POSITIONDATA + " as T1 JOIN " + SQLTableName.POSITIONS + " as T2 ON T1.pid = T2.id WHERE T1.endtime = 0";
        BroadcastService.getInstance().sendMessage("/database/request/currentPosition", query2);

        String query3 = "SELECT T2.name, T3.name FROM " + SQLTableName.ACTIVITYDATA + " as T1 JOIN " + SQLTableName.ACTIVITIES + " as T2 ON T1.activityid = T2.id LEFT OUTER JOIN " + SQLTableName.SUBACTIVITIES + " T3 ON T1.subactivityid = T3.id WHERE T1.endtime = 0";
        BroadcastService.getInstance().sendMessage("/database/request/currentActivity", query3);
    }


    private Notification createBasicNotification()
    {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.app_toast_running));
        builder.setSmallIcon(R.drawable.ic_launcher);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        return notification;
    }


    private void displayNotification(Notification notification)
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }


    private void hideNotification()
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }


    private void startObserveScreen()
    {
        try {
            registerReceiver(this.sl, new IntentFilter(Intent.ACTION_SCREEN_ON));
            registerReceiver(this.sl, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        } catch(IllegalArgumentException e) {
            Log.d("[BroadcastReceiver]", "already registered");
        }
    }


    private void stopObservingScreen()
    {
        unregisterReceiver(this.sl);
    }
}