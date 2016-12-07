package de.unima.ar.collector.shared.util;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Utils
{
    public static int getPosition(String entry, List<String> values)
    {
        int pos = 0;

        for(int i = 0; i < values.size(); i++) {
            if(values.get(i).equalsIgnoreCase(entry)) {
                pos = i;
                break;
            }
        }

        return pos;
    }


    public static void sleep(int milliseconds)
    {
        try {
            Thread.sleep(milliseconds);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }


    public static void makeToast(final Activity activity, final int message, final int duration)
    {
        activity.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(activity, message, duration).show();
            }
        });
    }


    public static void makeToast2(final Context context, final int message, final int duration)
    {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(context, message, duration).show();
            }
        });
    }


    public static byte[] objectToCompressedByteArray(Object object)
    {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
            ObjectOutputStream objectOut = new ObjectOutputStream(gzipOut);
            objectOut.writeObject(object);
            objectOut.close();

            return baos.toByteArray();
        } catch(Exception e) {
            e.printStackTrace();
        }

        return new byte[0];
    }


    public static Object compressedByteArrayToObject(byte[] bytes)
    {
        Object object = null;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            GZIPInputStream gzipIn = new GZIPInputStream(bais);
            ObjectInputStream objectIn = new ObjectInputStream(gzipIn);
            object = objectIn.readObject();
            objectIn.close();
        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return object;
    }


    public static <T> List<T> safeListCast(List<?> list, Class<T> type)
    {
        List<T> result = new ArrayList<>();
        for(Object object : list) {
            if(type.isInstance(object)) {
                result.add(type.cast(object));
            }
        }

        return result;
    }


    public static String[] split(String s, char c)
    {
        List<String> result = new ArrayList<>();
        int counter = 0;

        if(s == null) {
            return result.toArray(new String[result.size()]);
        }

        while(counter <= s.length()) {
            int pos = s.indexOf(c, counter);

            if(pos == -1) {
                pos = s.length();
            }

            String fragment = s.substring(counter, pos);
            result.add(fragment);

            counter += fragment.length() + 1;
        }

        return result.toArray(new String[result.size()]);
    }
}