package de.unima.ar.collector.api;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.Set;

public class ListenerService extends WearableListenerService
{
    private static BiMap<String, String> devices = HashBiMap.create();


    public static int getNumberOfDevices()
    {
        return devices.size();
    }


    public static void addDevice(String deviceID, String deviceMAC)
    {
        devices.put(deviceID, deviceMAC);
    }


    public static void rmDevice(String key)
    {
        devices.remove(key);
        devices.inverse().remove(key);
    }


    public static Set<String> getDevices()
    {
        return devices.keySet();
    }


    public static Set<String> getDevicesAddresses()
    {
        return devices.values();
    }


    public static String getDeviceID(String deviceAddress)
    {
        return devices.inverse().get(deviceAddress);
    }


    @Override
    public void onMessageReceived(MessageEvent messageEvent)
    {
        String path = messageEvent.getPath();

        if(path.equalsIgnoreCase("/activity/started")) {
            Tasks.informThatWearableHasStarted(messageEvent.getData(), this);
            return;
        }

        if(path.equalsIgnoreCase("/activity/destroyed")) {
            Tasks.informThatWearableHasDestroyed(messageEvent.getData());
            return;
        }

        if(path.equalsIgnoreCase("/posture/update")) {
            Tasks.updatePostureValue(messageEvent.getData());
            return;
        }

        if(path.equalsIgnoreCase("/position/update")) {
            Tasks.updatePositionValue(messageEvent.getData());
            return;
        }

        if(path.equalsIgnoreCase("/activity/update")) {
            Tasks.updateActivityValue(messageEvent.getData());
            return;
        }

        if(path.equalsIgnoreCase("/activity/delete")) {
            Tasks.deleteActivityValue(messageEvent.getData());
            return;
        }

        if(path.startsWith("/database/request")) {
            Tasks.processDatabaseRequest(path, messageEvent.getData());
            return;
        }

        if(path.startsWith("/sensor/data")) {
            Tasks.processIncomingSensorData(path, messageEvent.getData());
            return;
        }

        if(path.startsWith("/sensor/blob")) {
            Tasks.processIncomingSensorBlob(path, messageEvent.getData());
            return;
        }

        super.onMessageReceived(messageEvent);
    }
}