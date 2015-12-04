package de.unima.ar.collector.controller;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.HashMap;
import java.util.Map;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.api.ListenerService;
import de.unima.ar.collector.util.UIUtils;

public class BluetoothController extends BroadcastReceiver
{
    private Map<String, Integer>  connectedDevices;  // -1 = lost device, 0 = neutral, 1 = connected
    private BiMap<String, String> lostDevices; // deviceID, deviceAddress

    private static BluetoothController INSTANCE = null;


    private BluetoothController()
    {
        this.connectedDevices = new HashMap<>();
        this.lostDevices = HashBiMap.create();

        for(BluetoothDevice bd : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            this.connectedDevices.put(bd.getAddress(), 0);
        }
    }


    public static BluetoothController getInstance()
    {
        if(INSTANCE == null) {
            INSTANCE = new BluetoothController();
        }

        return INSTANCE;
    }


    public void register(Context context)
    {
        IntentFilter blueCon = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        context.registerReceiver(this, blueCon);
        IntentFilter blueDis = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        context.registerReceiver(this, blueDis);
        IntentFilter blueState = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(this, blueState);
    }


    public void unregister(Context context)
    {
        context.unregisterReceiver(this);
    }


    public int getConnectedDevices()
    {
        return this.connectedDevices.size();
    }


    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        String deviceAddress = "";
        if(device != null) {
            deviceAddress = device.getAddress();
        }

        switch(action) {
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                if(this.connectedDevices.containsKey(deviceAddress) && this.connectedDevices.get(deviceAddress) == -1) {
                    String deviceID = this.lostDevices.inverse().get(deviceAddress);
                    ListenerService.addDevice(deviceID, deviceAddress);
                    this.connectedDevices.put(deviceAddress, 1);

                    MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");
                    main.refreshMainScreenOverview();
                    UIUtils.makeToast(main, R.string.app_toast_bluetooth_reestablished, Toast.LENGTH_SHORT);
                } else {
                    this.connectedDevices.put(deviceAddress, 0);
                }
                break;

            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                if(ListenerService.getDevicesAddresses().contains(deviceAddress)) { // bluetooth connection lost but app did not say goodbye
                    String deviceID = ListenerService.getDeviceID(deviceAddress);
                    ListenerService.rmDevice(deviceAddress);        // disconnect app connection
                    this.connectedDevices.put(deviceAddress, -1);   // lost
                    this.lostDevices.put(deviceID, deviceAddress);  // store deviceID

                    MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");
                    main.refreshMainScreenOverview();
                    UIUtils.makeToast(main, R.string.app_toast_bluetooth, Toast.LENGTH_SHORT);
                } else {
                    this.connectedDevices.remove(deviceAddress);
                }

                if(this.getConnectedDevices() < ListenerService.getNumberOfDevices() - 1) {
                    BluetoothAdapter.getDefaultAdapter().disable();
                }
                break;

            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if(BluetoothAdapter.STATE_OFF == state) {
                    BluetoothAdapter.getDefaultAdapter().enable();
                } else
                break;
        }
    }
}