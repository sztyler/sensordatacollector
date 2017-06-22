package de.unima.ar.collector.util;

import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.shared.database.SQLTableName;

public class DBUtils
{
    public static boolean updateActivity(String name, String dataTableName, String tableName)
    {
        // query and verify current values
        List<String[]> result = SQLDBController.getInstance().query("SELECT B.name FROM " + dataTableName + " AS A, " + tableName + " AS B WHERE A.id=(SELECT max(id) FROM " + dataTableName + ") AND A.pid=B.id;", null, false);
        if(result.size() > 0 && result.get(0)[0].equalsIgnoreCase(name)) {
            return false;
        }

        List<String[]> result2 = SQLDBController.getInstance().query("SELECT id FROM " + tableName + " WHERE name = ?;", new String[]{ name }, false);
        int id = Integer.valueOf(result2.get(0)[0]);

        // time
        long currentTime = System.currentTimeMillis();

        // update old value
        ContentValues args = new ContentValues();
        args.put("endtime", currentTime);
        SQLDBController.getInstance().update(dataTableName, args, "endtime = 0", null);

        // add new value
        ContentValues newValues = new ContentValues();
        newValues.put("pid", id);
        newValues.put("starttime", currentTime);
        newValues.put("endtime", 0);
        SQLDBController.getInstance().insert(dataTableName, null, newValues);

        return true;
    }


    public static List<String[]> manageCache(String deviceID, Map<String, List<String[]>> cache, ContentValues newValues, int cacheSize)
    {
        if(!cache.containsKey(deviceID)) {
            cache.put(deviceID, new ArrayList<String[]>());

            Set<String> keys = newValues.keySet();
            String[] header = keys.toArray(new String[keys.size()]);
            cache.get(deviceID).add(header);
        }

        String[] keys = cache.get(deviceID).get(0);
        String[] entry = new String[keys.length];
        for(int i = 0; i < keys.length; i++) {
            entry[i] = newValues.getAsString(keys[i]);
        }
        cache.get(deviceID).add(entry);

        if(cache.get(deviceID).size() <= cacheSize) {
            return null;
        }

        List<String[]> clone = cache.get(deviceID).subList(0, cacheSize + 1);

        if(cache.get(deviceID).size() > cacheSize + 1) {
            cache.put(deviceID, cache.get(deviceID).subList(cacheSize + 1, cache.get(deviceID).size()));
        } else {
            cache.put(deviceID, new ArrayList<String[]>());
        }
        cache.get(deviceID).add(0, keys);

        return clone;
    }


    public static void flushCache(String sqlTableName, Map<String, List<String[]>> cache, String deviceID)
    {
        if(cache.keySet().size() == 0) {
            return;
        }

        String[] deviceIDs = deviceID == null ? cache.keySet().toArray(new String[cache.keySet().size()]) : new String[]{ deviceID };

        for(String entry : deviceIDs) {
            List<String[]> values = cache.get(entry);
            if(values.size() <= 1) {
                continue;
            }

            String tableName = SQLTableName.PREFIX + entry + sqlTableName;
            SQLDBController.getInstance().bulkInsert(tableName, values);

            cache.remove(entry);
        }
    }


    public static void updateSensorStatus(int type, int frequency, int enabled)
    {
        ContentValues values = new ContentValues();
        values.put("enabled", enabled);
        values.put("freq", frequency);
        int affectedRows = SQLDBController.getInstance().update(SQLTableName.SENSOROPTIONS, values, "sensor = ?", new String[]{ String.valueOf(type) });

        if(affectedRows == 0 && enabled == 1) {
            SQLDBController.getInstance().execSQL("INSERT OR IGNORE INTO " + SQLTableName.SENSOROPTIONS + " (sensor, freq, enabled) VALUES (" + type + "," + frequency + "," + enabled + ")");
        }
    }
}