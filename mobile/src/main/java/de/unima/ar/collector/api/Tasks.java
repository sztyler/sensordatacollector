package de.unima.ar.collector.api;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.android.gms.wearable.WearableListenerService;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.SensorDataCollectorService;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.sensors.AccelerometerSensorCollector;
import de.unima.ar.collector.sensors.GravitySensorCollector;
import de.unima.ar.collector.sensors.GyroscopeSensorCollector;
import de.unima.ar.collector.sensors.LinearAccelerationSensorCollector;
import de.unima.ar.collector.sensors.MagneticFieldSensorCollector;
import de.unima.ar.collector.sensors.OrientationSensorCollector;
import de.unima.ar.collector.sensors.PressureSensorCollector;
import de.unima.ar.collector.sensors.RotationVectorSensorCollector;
import de.unima.ar.collector.sensors.StepCounterSensorCollector;
import de.unima.ar.collector.sensors.StepDetectorSensorCollector;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.Utils;
import de.unima.ar.collector.util.DBUtils;
import de.unima.ar.collector.util.SensorDataUtil;
import de.unima.ar.collector.util.StringUtils;

class Tasks
{
    static void informThatWearableHasStarted(byte[] rawData, WearableListenerService wls)
    {
        String data = StringUtils.convertByteArrayToString(rawData);
        if(data == null) {
            return;
        }

        // register device
        String[] tmp = data.split(Pattern.quote("~#X*X#~"));
        String deviceID = tmp[0];
        String deviceAddress = tmp[1];

        if(ListenerService.getDevices().contains(deviceID)) {
            return;
        }

        ListenerService.addDevice(deviceID, deviceAddress);

        // send settings
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(wls);
        Handler sendSettings = new Handler(Looper.getMainLooper());
        sendSettings.post(new Runnable()
        {
            @Override
            public void run()
            {
                boolean run = true;

                do {
                    if(BroadcastService.getInstance() != null) {
                        BroadcastService.getInstance().sendMessage("/settings", "[WEARSENSOR, " + pref.getBoolean("watch_collect", true) + "]");
                        BroadcastService.getInstance().sendMessage("/settings", "[WEARTRANSFERDIRECT, " + pref.getBoolean("watch_direct", false) + "]");
                        run = false;
                    } else {
                        Utils.sleep(100);
                    }
                } while(run);
            }
        });

        // main activity started?
        MainActivity activity = (MainActivity) ActivityController.getInstance().get("MainActivity");
        if(activity == null) {
            Intent intent = new Intent(wls, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            wls.startActivity(intent);

            return;
        }

        // main activity already started so we have to create the sql tables "manually"
        boolean known = SQLDBController.getInstance().registerDevice(deviceID);
        if(!known) {
            DatabaseHelper.createDeviceDependentTables(deviceID);
        }

        // refresh UI
        activity.refreshMainScreenOverview();

        // inform mobile device
        Utils.makeToast2(activity, R.string.listener_app_connected, Toast.LENGTH_SHORT);
    }


    static void informThatWearableHasDestroyed(byte[] rawData)
    {
        // unregister device
        String deviceID = StringUtils.convertByteArrayToString(rawData);
        ListenerService.rmDevice(deviceID);

        // refresh overview
        MainActivity activity = (MainActivity) ActivityController.getInstance().get("MainActivity");
        if(activity != null) {
            activity.refreshMainScreenOverview();

            // inform mobile device
            Utils.makeToast2(activity, R.string.listener_app_disconnected, Toast.LENGTH_SHORT);
        }
    }


    static void updatePostureValue(byte[] rawData)
    {
        // parse data
        String posture = StringUtils.convertByteArrayToString(rawData);

        // update database
        boolean result = DBUtils.updateActivity(posture, SQLTableName.POSTUREDATA, SQLTableName.POSTURES);
        if(!result) {
            return;
        }

        // update UI
        MainActivity mc = (MainActivity) ActivityController.getInstance().get("MainActivity");
        if(mc != null) {
            mc.refreshActivityScreenPosture(posture);
        }
    }


    static void updatePositionValue(byte[] rawData)
    {
        // parse data
        String position = StringUtils.convertByteArrayToString(rawData);

        // update database
        boolean result = DBUtils.updateActivity(position, SQLTableName.POSITIONDATA, SQLTableName.POSITIONS);
        if(!result) {
            return;
        }

        // update UI
        MainActivity mc = (MainActivity) ActivityController.getInstance().get("MainActivity");
        if(mc != null) {
            mc.refreshActivityScreenPosition(position);
        }
    }


    static void updateActivityValue(byte[] rawData)
    {
        // parse data
        String data = StringUtils.convertByteArrayToString(rawData);
        if(data == null) {
            return;
        }

        String activity = data;
        String subActivity = null;

        if(data.contains(Settings.ACTIVITY_DELIMITER)) {  //subactivity?
            subActivity = data.substring(data.indexOf(Settings.ACTIVITY_DELIMITER) + Settings.ACTIVITY_DELIMITER.length());
            activity = data.substring(0, data.indexOf(Settings.ACTIVITY_DELIMITER));
        }

        // update database
        String activityId = DatabaseHelper.getStringResultSet("SELECT id FROM " + SQLTableName.ACTIVITIES + " WHERE name = ? ", new String[]{ activity }).get(0);
        String subActivityID = null;
        if(subActivity != null) {
            subActivityID = DatabaseHelper.getStringResultSet("SELECT id FROM " + SQLTableName.SUBACTIVITIES + " WHERE name = ? AND activityid = ? ", new String[]{ subActivity, activityId }).get(0);
        }

        ContentValues newValues = new ContentValues();
        newValues.put("activityid", activityId);
        newValues.put("subactivityid", subActivityID);
        newValues.put("starttime", System.currentTimeMillis());
        newValues.put("endtime", 0);
        SQLDBController.getInstance().insert(SQLTableName.ACTIVITYDATA, null, newValues);

        // update UI
        MainActivity mc = (MainActivity) ActivityController.getInstance().get("MainActivity");
        if(mc != null) {
            mc.refreshActivityScreenActivity(data, true);
        }
    }


    static void deleteActivityValue(byte[] rawData)
    {
        // parse data
        String data = StringUtils.convertByteArrayToString(rawData);
        if(data == null) {
            return;
        }

        String activity = data;
        String subActivity = null;

        if(data.contains(Settings.ACTIVITY_DELIMITER)) {  //subactivity?
            subActivity = data.substring(data.indexOf(Settings.ACTIVITY_DELIMITER) + Settings.ACTIVITY_DELIMITER.length());
            activity = data.substring(0, data.indexOf(Settings.ACTIVITY_DELIMITER));
        }

        // update database
        String activityId = DatabaseHelper.getStringResultSet("SELECT id FROM " + SQLTableName.ACTIVITIES + " WHERE name = ? ", new String[]{ activity }).get(0);
        String subActivityID = null;
        if(subActivity != null) {
            subActivityID = DatabaseHelper.getStringResultSet("SELECT id FROM " + SQLTableName.SUBACTIVITIES + " WHERE name = ? AND activityid = ? ", new String[]{ subActivity, activityId }).get(0);
        }

        // update database
        ContentValues args = new ContentValues();
        args.put("endtime", System.currentTimeMillis());
        SQLDBController.getInstance().update(SQLTableName.ACTIVITYDATA, args, "activityid = ? AND (subactivityid = ? OR subactivityid is NULL) AND endtime = 0", new String[]{ activityId, subActivityID });

        // update UI
        MainActivity mc = (MainActivity) ActivityController.getInstance().get("MainActivity");
        if(mc != null) {
            mc.refreshActivityScreenActivity(data, false);
        }
    }


    static void processDatabaseRequest(String key, byte[] rawData)
    {
        try {
            StringBuilder sb = new StringBuilder();
            key = key.substring(key.lastIndexOf("/") + 1);
            String queryString = StringUtils.convertByteArrayToString(rawData);

            String delimiterColumn = Settings.DATABASE_DELIMITER;

            List<String[]> result = SQLDBController.getInstance().query(queryString, null, false);

            for(String[] row : result) {
                for(String value : row) {
                    sb.append(value);
                    sb.append(delimiterColumn);
                }
                sb = sb.delete(sb.length() - delimiterColumn.length(), sb.length());
                sb.append("\n"); // lineSeparator not supported
            }

            if(sb.length() != 0) {
                sb = sb.delete(sb.length() - ("\n").length(), sb.length());
            }

            BroadcastService.getInstance().sendMessage("/database/response/" + key, sb.toString());
        } catch(Exception e) {
            e.printStackTrace();

            // Check if UI is available
            Context context = ActivityController.getInstance().get("MainActivity");
            if(context != null) {
                Utils.makeToast2(context, R.string.listener_database_failed, Toast.LENGTH_LONG);
            }
        }
    }


    static void processIncomingSensorData(String path, byte[] rawData)
    {
        if(SQLDBController.getInstance() == null) {
            return;
        }
        path = path.substring("/sensor/data/".length());

        String deviceID = path.substring(0, path.indexOf("/"));
        //int type = Integer.valueOf(path.substring(path.indexOf("/") + 1, path.lastIndexOf("/")));
        int type = Integer.valueOf(path.substring(path.indexOf("/") + 1));

        String data = StringUtils.convertByteArrayToString(rawData);
        String[] entries = StringUtils.split(data);
        ContentValues newValues = new ContentValues();
        for(int i = 0; i < entries.length; i += 2) {
            newValues.put(entries[i], entries[i + 1]);
        }

        switch(type) {
            case 1:
                if(Settings.WEARTRANSFERDIRECT && Settings.LIVE_PLOTTER_ENABLED) {
                    AccelerometerSensorCollector.updateLivePlotter(deviceID, new float[]{ Float.valueOf(entries[1]), Float.valueOf(entries[3]), Float.valueOf(entries[5]) });
                }
                AccelerometerSensorCollector.writeDBStorage(deviceID, newValues);
                break;
            case 2:
                if(Settings.WEARTRANSFERDIRECT && Settings.LIVE_PLOTTER_ENABLED) {
                    MagneticFieldSensorCollector.updateLivePlotter(deviceID, new float[]{ Float.valueOf(entries[1]), Float.valueOf(entries[3]), Float.valueOf(entries[5]) });
                }
                MagneticFieldSensorCollector.writeDBStorage(deviceID, newValues);
                break;
            case 3:
                if(Settings.WEARTRANSFERDIRECT && Settings.LIVE_PLOTTER_ENABLED) {
                    OrientationSensorCollector.updateLivePlotter(deviceID, new float[]{ Float.valueOf(entries[1]), Float.valueOf(entries[3]), Float.valueOf(entries[5]) });
                }
                OrientationSensorCollector.writeDBStorage(deviceID, newValues);
                break;
            case 4:
                if(Settings.WEARTRANSFERDIRECT && Settings.LIVE_PLOTTER_ENABLED) {
                    GyroscopeSensorCollector.updateLivePlotter(deviceID, new float[]{ Float.valueOf(entries[1]), Float.valueOf(entries[3]), Float.valueOf(entries[5]) });
                }
                GyroscopeSensorCollector.writeDBStorage(deviceID, newValues);
                break;
            case 6:
                if(Settings.WEARTRANSFERDIRECT && Settings.LIVE_PLOTTER_ENABLED) {
                    PressureSensorCollector.updateLivePlotter(deviceID, new float[]{ Float.valueOf(entries[1]) });
                }
                PressureSensorCollector.writeDBStorage(deviceID, newValues);
                break;
            case 9:
                if(Settings.WEARTRANSFERDIRECT && Settings.LIVE_PLOTTER_ENABLED) {
                    GravitySensorCollector.updateLivePlotter(deviceID, new float[]{ Float.valueOf(entries[1]), Float.valueOf(entries[3]), Float.valueOf(entries[5]) });
                }
                GravitySensorCollector.writeDBStorage(deviceID, newValues);
                break;
            case 10:
                if(Settings.WEARTRANSFERDIRECT && Settings.LIVE_PLOTTER_ENABLED) {
                    LinearAccelerationSensorCollector.updateLivePlotter(deviceID, new float[]{ Float.valueOf(entries[1]), Float.valueOf(entries[3]), Float.valueOf(entries[5]) });
                }
                LinearAccelerationSensorCollector.writeDBStorage(deviceID, newValues);
                break;
            case 11:
                if(Settings.WEARTRANSFERDIRECT && Settings.LIVE_PLOTTER_ENABLED) {
                    RotationVectorSensorCollector.updateLivePlotter(deviceID, new float[]{ Float.valueOf(entries[1]), Float.valueOf(entries[3]), Float.valueOf(entries[5]) });
                }
                RotationVectorSensorCollector.writeDBStorage(deviceID, newValues);
                break;
            case 18:
                if(Settings.WEARTRANSFERDIRECT && Settings.LIVE_PLOTTER_ENABLED) {
                    StepDetectorSensorCollector.updateLivePlotter(deviceID, new float[]{ Float.valueOf(entries[1]) });
                }
                StepDetectorSensorCollector.writeDBStorage(deviceID, newValues);
                break;
            case 19:
                if(Settings.WEARTRANSFERDIRECT && Settings.LIVE_PLOTTER_ENABLED) {
                    StepCounterSensorCollector.updateLivePlotter(deviceID, new float[]{ Float.valueOf(entries[1]) });
                }
                StepCounterSensorCollector.writeDBStorage(deviceID, newValues);
                break;
        }
    }


    static void processIncomingSensorBlob(String path, byte[] rawData)
    {
        BroadcastService.getInstance().sendMessage("/sensor/blob/confirm/" + Arrays.hashCode(rawData), "");

        Object object = Utils.compressedByteArrayToObject(rawData);
        if(object == null || !(object instanceof List<?>) || ((List<?>) object).size() <= 1) {
            return;
        }

        List<String[]> entries = Utils.safeListCast((List<?>) object, String[].class);

        String[] header = entries.get(0);
        for(int i = 1; i < entries.size(); i++) {
            String[] entry = entries.get(i);
            String record = "";

            for(int j = 1; j < entry.length; j++) {
                record += header[j] + ";" + entry[j] + ";";
            }
            record = record.substring(0, record.length() - 1);

            Tasks.processIncomingSensorData(path.replace("blob", "data").substring(0, path.lastIndexOf("/")), record.getBytes());
        }

        String head = path.substring(0, path.lastIndexOf("/"));
        int type = Integer.valueOf(head.substring(head.lastIndexOf("/") + 1));
        String deviceID = path.substring("/sensor/blob/".length(), path.indexOf("/", "/sensor/blob/".length()));
        boolean last = Boolean.valueOf(path.substring(path.lastIndexOf("/") + 1));
        Set<Integer> enabledSensors = SensorDataCollectorService.getInstance().getSCM().getEnabledCollectors();
        if(!enabledSensors.contains(type) && last && !Settings.DATABASE_DIRECT_INSERT) {
            SensorDataUtil.flushSensorDataCache(type, deviceID);
        }
    }
}