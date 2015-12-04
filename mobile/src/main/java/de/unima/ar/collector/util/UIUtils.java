package de.unima.ar.collector.util;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.controller.ActivityController;

public class UIUtils
{
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


    public static String[] getString(int... values)
    {
        String[] data = new String[values.length];

        MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");
        if(main == null) {
            return new String[0];
        }

        for(int i = 0; i < values.length; i++) {
            data[i] = main.getString(values[i]);
        }

        return data;
    }


    public static void setEnabled(ViewGroup layout, boolean enabled)
    {
        layout.setEnabled(false);
        for(int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if(child instanceof ViewGroup) {
                setEnabled((ViewGroup) child, enabled);
            } else {
                child.setEnabled(enabled);
            }
        }
    }
}
