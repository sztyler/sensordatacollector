package de.unima.ar.collector.shared.util;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class DeviceID
{
    private static String id;


    public static String get(Context context)
    {
        if((id != null && id.length() > 0) || context == null) {
            return id;
        }

        id = getAndroidID(context);   // not unique!!! but should be available on each device
        if(id != null) {
            return id;
        }

        id = getDeviceID(context);     // should be unique but is not available on each device
        if(id != null) {
            return id;
        }

        id = getBuildSerial(context);   // should be unique but is not available on each device
        if(id != null) {
            return id;
        }

        // Further information: http://stackoverflow.com/questions/16078269/android-unique-serial-number/16929647#16929647

        return null;    // this should never happend
    }


    private static String getAndroidID(Context context)
    {
        try {
            return String.valueOf(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        } catch(Exception e) {
            // not available
        }

        return null;
    }


    private static String getDeviceID(Context context)
    {
        try {
            return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        } catch(Exception e) {
            // not available
        }

        return null;
    }


    private static String getBuildSerial(Context context)
    {
        try {
            return android.os.Build.SERIAL;
        } catch(Exception e) {
            // not available
        }

        return null;
    }
}