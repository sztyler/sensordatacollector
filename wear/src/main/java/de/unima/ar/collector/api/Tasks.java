package de.unima.ar.collector.api;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.wearable.WearableListenerService;

import java.io.UnsupportedEncodingException;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.database.SQLDBController;
import de.unima.ar.collector.sensors.SensorDataUtil;
import de.unima.ar.collector.sensors.SensorService;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.util.DeviceID;
import de.unima.ar.collector.shared.util.Utils;
import de.unima.ar.collector.ui.ActivitySelector;
import de.unima.ar.collector.ui.Chooser;

class Tasks
{
    static void startWearableApp(WearableListenerService wls)
    {
        MainActivity ma = (MainActivity) ActivityController.getInstance().get("MainActivity");

        if(ma != null && BluetoothAdapter.getDefaultAdapter() != null) {
            BroadcastService.getInstance().sendMessage("/activity/started", DeviceID.get(ma) + "~#X*X#~" + BluetoothAdapter.getDefaultAdapter().getAddress());
            return;
        }

        Intent intent = new Intent(wls, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        wls.startActivity(intent);
    }


    static void destroyWearableApp(byte[] data)
    {
        boolean value = true;

        try {
            String values = new String(data, "UTF-8");
            value = Boolean.valueOf(values);
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // update local device
        MainActivity ma = (MainActivity) ActivityController.getInstance().get("MainActivity");
        if(ma == null) {
            return;
        }

        ma.updateOnDestroyResumeStatus(value);
        ma.finish();
    }


    static void processDatabaseResponse(String key, byte[] data)
    {
        try {
            key = key.substring(key.lastIndexOf("/") + 1);
            String values = new String(data, "UTF-8");
            refreshUI(key, values);
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    static void registerSensor(byte[] data)
    {
        if(!Settings.WEARSENSOR) {
            return;
        }

        try {
            String[] values = new String(data, "UTF-8").replace("[", "").replace("]", "").replace(" ", "").split(",");
            int type = Integer.valueOf(values[0]);
            int rate = Integer.valueOf(values[1]);

            Log.d("TIMOSENSOR", "AN");
            SensorService.getInstance().enableCollector(type, rate);
            SensorService.getInstance().registerCollectors();
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    static void unregisterSensor(byte[] data)
    {
        try {
            String value = new String(data, "UTF-8");
            int type = Integer.valueOf(value);

            Log.d("TIMOSENSOR", "AUS");
            SensorService.getInstance().getSCM().unregisterCollector(type);
            SensorService.getInstance().getSCM().disableCollector(type);
            SensorDataUtil.flushSensorDataCache(type);
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    static void confirmBlob(String path)
    {
        String id = path.substring(path.lastIndexOf("/") + 1);
        SensorService ss = SensorService.getInstance();
        if(ss != null) {
            ss.informDBObserver(Integer.valueOf(id));
            Log.d("DBObseverTIMO", "Received ID: " + id);
        }
    }


    static void deleteDatabase(Context context)
    {
        SQLDBController sc = SQLDBController.getInstance();
        if(sc == null) {
            return;
        }

        sc.deleteDatabase();
        Utils.makeToast2(context, R.string.database_delete, Toast.LENGTH_SHORT);

        MainActivity activity = (MainActivity) ActivityController.getInstance().get("MainActivity");
        if(activity == null) {
            return;
        }

        activity.updatePositionView("");
        activity.updatePostureView("");
        ActivitySelector.delete();
        activity.updateActivityView("");
    }


    static void updateSettings(final Context context, byte[] data)
    {
        try {
            String[] values = new String(data, "UTF-8").replace("[", "").replace("]", "").replace(" ", "").split(",");
            String parameter = values[0];
            String value = values[1];

            switch(parameter) {
                case "WEARSENSOR":
                    Log.d("TIMOSENSOR", "WEARSENSOR STATE CHANGED");
                    Settings.WEARSENSOR = Boolean.parseBoolean(value);              // Update Settings
                    SensorService.getInstance().getSCM().unregisterCollectors();    // Stop all sensors
                    SensorDataUtil.flushSensorDataCache(0);                         // Write cache to database and force DBObserver
                    if(Settings.WEARSENSOR) {
                        Utils.makeToast2(context, R.string.preferences_sensors_enabled, Toast.LENGTH_SHORT);
                    } else {
                        Utils.makeToast2(context, R.string.preferences_sensors_disabled, Toast.LENGTH_SHORT);
                    }
                    break;
                case "WEARTRANSFERDIRECT":
                    Log.d("TIMOSENSOR", "WEARTRANSFERDIRECT STATE CHANGED");
                    SensorService.getInstance().getSCM().unregisterCollectors();    // Not necessary but just to be sure TODO -NullPointer...
                    SensorDataUtil.flushSensorDataCache(0);                         // there are only data if it is turned off
                    Settings.WEARTRANSFERDIRECT = Boolean.parseBoolean(value);             // change state
                    if(Settings.WEARTRANSFERDIRECT) {
                        Utils.makeToast2(context, R.string.preferences_directtransfer_enabled, Toast.LENGTH_SHORT);
                    } else {
                        Utils.makeToast2(context, R.string.preferences_directtransfer_disabled, Toast.LENGTH_SHORT);
                    }
                    break;
                default:
                    // nothing
            }
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    private static void refreshUI(String key, String data)
    {
        MainActivity main;
        Chooser chooser;
        ActivitySelector activity;

        switch(key) {
            case "posture":
                chooser = (Chooser) ActivityController.getInstance().get("Chooser");
                if(chooser == null) {
                    return;
                }
                chooser.createPostureList(data);
                break;
            case "position":
                chooser = (Chooser) ActivityController.getInstance().get("Chooser");
                if(chooser == null) {
                    return;
                }
                chooser.createPositionList(data);
                break;
            case "activity":
                activity = (ActivitySelector) ActivityController.getInstance().get("ActivitySelector");
                if(activity == null) {
                    return;
                }
                activity.createActivityList(data);
                break;
            case "currentPosture":
                main = (MainActivity) ActivityController.getInstance().get("MainActivity");
                if(main == null) {
                    return;
                }
                main.updatePostureView(data);
                break;
            case "currentPosition":
                main = (MainActivity) ActivityController.getInstance().get("MainActivity");
                if(main == null) {
                    return;
                }
                main.updatePositionView(data);
                break;
            case "currentActivity":
                main = (MainActivity) ActivityController.getInstance().get("MainActivity");
                if(main == null) {
                    return;
                }
                main.updateActivityView(data);
                break;
            case "deleteActivity":
                main = (MainActivity) ActivityController.getInstance().get("MainActivity");
                if(main == null) {
                    return;
                }
                boolean result = ActivitySelector.rm(data);
                if(result) {
                    main.updateActivityView(null);  // null == reload
                }
            default:
                // noting
        }
    }
}