package de.unima.ar.collector.database;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
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
    private boolean      wait;


    public DBObserver()
    {
        this.lastTransfer = 0;
        this.isRunning = true;
        this.wait = false;
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


    public void forceSending(Context context)
    {
        while(!this.wait && this.isRunning) {
            Log.d("TIMOSENSOR", "WAIT: " + this.wait + " -- " + this.isRunning);
            Utils.makeToast2(context, R.string.sending_force, Toast.LENGTH_SHORT);
            Utils.sleep(2500);  // waits until the other running process is done
        }

        this.lastTransfer = 0;

        synchronized(this) {
            this.wait = false;
            this.notify();
        }

        Log.d("TIMOSENSOR", "DB NOTIFIED!");
    }


    private boolean verify()
    {
        Log.d("TIMOSENSOR", "VERIFY");
        long currentTime = System.currentTimeMillis();

        if(this.lastTransfer + Settings.WEARTRANSFERIDLETIME > currentTime) {
            long idle = Math.abs(currentTime - (this.lastTransfer + Settings.WEARTRANSFERIDLETIME));
            //            Utils.sleep((int) (idle % Integer.MAX_VALUE));
            try {
                Log.d("TIMOSENSOR", "SLEEP");
                synchronized(this) {
                    this.wait = true;
                    this.wait((idle % Integer.MAX_VALUE));
                }
                Log.d("TIMOSENSOR", "WAKEUP");
            } catch(InterruptedException e) {
                e.printStackTrace();
                // no problem, go on
            }
            return false;
        }

        return true;
    }


    private List<String[]> query(String deviceID, int type)
    {
        String table = SQLTableMapper.getName(type);
        //        if(table == null || SQLDBController.getInstance() == null || !SensorService.getInstance().hasWakelock()) { TODO
        if(table == null || SQLDBController.getInstance() == null) {
            return new ArrayList<>();
        }

        return SQLDBController.getInstance().query("SELECT * FROM " + SQLTableName.PREFIX + deviceID + table + " LIMIT " + (Settings.WEARTRANSFERSIZE + 1), null, true);
    }


    private boolean send(String deviceID, int type, List<String[]> entries, boolean last)
    {
        Log.d("TIMOSENSOR", "" + last);

        if(!last) {
            entries.remove(entries.size() - 1);
        }
        byte[] bytes = Utils.objectToCompressedByteArray(entries);
        BroadcastService.getInstance().sendMessage("/sensor/blob/" + deviceID + "/" + type + "/" + last, bytes);

        if(Settings.DATABASE_DIRECT_INSERT && !Settings.WEARTRANSFERDIRECT) {
            Settings.WEARTRANSFERTIMEOUT = 35000;
        } else {
            Settings.WEARTRANSFERTIMEOUT = 5000;
        }

        int code = Arrays.hashCode(bytes);
        Log.d("DBObseverTIMO", "Send ID: " + code);
        int attempts = 0;
        while(!(this.confirmed.contains(code)) && attempts <= Settings.WEARTRANSFERTIMEOUT) {
            Utils.sleep(1000);
            attempts += 1000;

            if(!this.isRunning) {
                break;
            }
        }

        Log.d("DBObseverTIMO", String.valueOf(this.confirmed.contains(code)) + " -- " + this.confirmed.size());

        return this.confirmed.contains(code);
    }


    @Override
    public void run()
    {
        while(this.isRunning) {
            if(!verify()) {
                continue;
            }

            final MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");
            if(main == null) {
                continue;
            }

            String deviceID = DeviceID.get(main);
            Set<Integer> implementedSensors = SensorService.getInstance().getSCM().getImplementedSensors();

            for(int type : implementedSensors) {
                List<String[]> entries;
                boolean success = true;
                do {
                    entries = query(deviceID, type);

                    if(type == 1 || type == 4) {
                        Log.d("TIMOSENSOR", "DB SCAN " + type + " " + entries.size());
                    }

                    if(entries.size() > 1) {
                        Log.d("TIMOSENSOR", "SEND DATA " + type + " " + entries.size());
                        success = send(deviceID, type, entries, (entries.size() != (Settings.WEARTRANSFERSIZE + 2)));

                        if(success) {   // TODO RESEND MAY RESULT IN DUPLICATE DATA!!!
                            String table = SQLTableName.PREFIX + deviceID + SQLTableMapper.getName(type);
                            SQLDBController.getInstance().delete(table, "id in (SELECT id FROM " + table + " LIMIT " + Settings.WEARTRANSFERSIZE + ")", null);
                        } else {
                            main.runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Toast.makeText(main.getBaseContext(), R.string.sending_error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    if(!this.isRunning) {
                        return;
                    }
                } while(entries.size() >= Settings.WEARTRANSFERSIZE || !success);
            }

            this.lastTransfer = System.currentTimeMillis();
        }

        this.isRunning = true; // reset, since DBO is static
    }
}