package de.unima.ar.collector.listener;


import android.content.Context;
import android.content.Intent;
import android.support.wearable.view.DismissOverlayView;
import android.view.GestureDetector;
import android.view.MotionEvent;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.controller.ActivityController;


public class OnCustomTouchListener extends GestureDetector.SimpleOnGestureListener
{
    private Context            context;
    private DismissOverlayView dismissOverlay;

    private static final int SWIPE_DISTANCE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;


    public OnCustomTouchListener(Context context)
    {
        this.context = context;

        this.dismissOverlay = (DismissOverlayView) ((MainActivity) context).findViewById(R.id.dismiss_overlay);
        this.dismissOverlay.setIntroText(R.string.welcome_long_press_exit);
        //        this.dismissOverlay.showIntroIfNecessary(); TODO - do I realy need this line?
    }


    @Override
    public boolean onDown(MotionEvent e)
    {
        return false;
    }


    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {
        float distanceX = e2.getX() - e1.getX();
        float distanceY = e2.getY() - e1.getY();

        if(Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
            if(distanceX > 0) {
                onSwipeRight();
            } else {
                onSwipeLeft();
            }
            return true;
        }
        return false;
    }


    @Override
    public void onLongPress(MotionEvent ev)
    {
        dismissOverlay.show();
    }


    private void onSwipeRight()
    {
        MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");
        ActivityController.getInstance().setState(main, ActivityController.State.onPause_MANUAL);

        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.context.startActivity(setIntent);
    }


    private void onSwipeLeft()
    {
        // nothing
    }
}