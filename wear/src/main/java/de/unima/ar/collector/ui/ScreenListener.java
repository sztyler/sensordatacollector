package de.unima.ar.collector.ui;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.controller.ActivityController;

public class ScreenListener extends WakefulBroadcastReceiver
{
    private static final String TAG = "BroadcastReceiver";


    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            Log.i(TAG, "Screen ON");

            // load activity
            MainActivity mainActivity = (MainActivity) ActivityController.getInstance().get("MainActivity");
            if(mainActivity == null) {
                return;
            }

            // validate state
            ActivityController.State state = ActivityController.getInstance().getState(mainActivity);

            // create intent
            Intent main;
            switch(state) {
                case onPause_AUTO:
                    main = new Intent(context, MainActivity.class);
                    ActivityController.getInstance().setState(mainActivity, ActivityController.State.onCreate);
                    mainActivity.getWindow().setWindowAnimations(0);
                    break;
                case onPause_CHOOSER:
                    main = new Intent(context, Chooser.class);
                    Chooser chooser = (Chooser) ActivityController.getInstance().get("Chooser");
                    chooser.getWindow().setWindowAnimations(0);
                    break;
                case onPause_ACTIVITYSELECTOR:
                    main = new Intent(context, ActivitySelector.class);
                    ActivitySelector as = (ActivitySelector) ActivityController.getInstance().get("ActivitySelector");
                    as.getWindow().setWindowAnimations(0);
                    break;
                default:
                    return;
            }

            // bring to foreground
            context.startActivity(main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION));
            //            ((Activity) context).getWindow().setWindowAnimations(0);
        } else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Log.i(TAG, "Screen OFF");
        }
    }
}