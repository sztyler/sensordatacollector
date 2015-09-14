package de.unima.ar.collector.database;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.sensors.SensorService;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableMapper;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.DeviceID;
import de.unima.ar.collector.shared.util.Utils;

public class DBObserver implements Runnable
{
    private long         lastTransfer;
    private boolean      isRunning;
    private Set<Integer> confirmed;


    public DBObserver()
    {
        this.lastTransfer = 0;
        this.isRunning = true;
        this.confirmed = new HashSet<>();
    }


    public void addConfirmedTransaction(int hashCode)
    {
        this.confirmed.add(hashCode);
    }


    public void shutdown()
    {
        this.isRunning = false;
    }


    private boolean verify()
    {
        long currentTime = System.currentTimeMillis();
        if(this.lastTransfer + Settings.WEARTRANSFERIDLETIME > currentTime) {
            long idle = Math.abs(currentTime - (this.lastTransfer + Settings.WEARTRANSFERIDLETIME));
            Utils.sleep((int) (idle % Integer.MAX_VALUE));
            return false;
        }

        return true;
    }


    private List<String[]> query(String deviceID, int type)
    {
        List<String[]> entries = new ArrayList<>();

        String table = SQLTableMapper.getName(type);
        if(table == null || SQLDBController.getInstance() == null || !SensorService.getInstance().hasWakelock()) {
            return entries;
        }

        List<String[]> tmp = SQLDBController.getInstance().query("SELECT * FROM " + SQLTableName.PREFIX + deviceID + table + " LIMIT " + Settings.WEARTRANSFERSIZE, null, true);
        entries.addAll(tmp);

        return entries;
    }


    private void send(String deviceID, int type, List<String[]> entries)
    {
        byte[] bytes = Utils.objectToCompressedByteArray(entries);
        BroadcastService.getInstance().sendMessage("/sensor/blob/" + deviceID + "/" + type, bytes);

        int code = Arrays.hashCode(bytes);
        Log.d("DBObseverTIMO", "Send ID: " + code);
        int attempts = 0;
        while(!(this.confirmed.contains(code)) && attempts < Settings.WEARTRANSFERTIMEOUT) {
            Utils.sleep(100);
            attempts++;

            if(!this.isRunning) {
                return;
            }
        }
    }


    @Override
    public void run()
    {
        while(this.isRunning) {
            if(!verify()) {
                continue;
            }

            MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");
            if(main == null) {
                continue;
            }

            SensorManager sm = (SensorManager) main.getSystemService(Activity.SENSOR_SERVICE);
            List<Sensor> allSensors = sm.getSensorList(Sensor.TYPE_ALL);
            String deviceID = DeviceID.get(main);

            for(Sensor sensor : allSensors) {
                int type = sensor.getType();
                List<String[]> entries;
                do {
                    entries = query(deviceID, type);

                    if(entries.size() > 1) {
                        send(deviceID, type, query(deviceID, type));

                        String table = SQLTableName.PREFIX + deviceID + SQLTableMapper.getName(type);
                        SQLDBController.getInstance().delete(table, "id in (SELECT id FROM " + table + " LIMIT " + Settings.WEARTRANSFERSIZE + ")", null);
                    }

                    if(!this.isRunning) {
                        return;
                    }
                } while(entries.size() >= Settings.WEARTRANSFERSIZE);
            }

            this.lastTransfer = System.currentTimeMillis();
        }
    }
}