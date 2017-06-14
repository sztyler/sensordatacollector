package de.unima.ar.collector.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WearableListView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.adapter.ItemListViewAdapter;
import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.Utils;

public class ActivitySelector extends Activity
{
    private ItemListViewAdapter                   adapterActivity;
    private NavigableMap<String, TreeSet<String>> allActivities;
    private GestureDetector                       detector;
    private WearableListView                      mainView;

    private static List<String> selectedActivites = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // init
        super.onCreate(savedInstanceState);
        setContentView(R.layout.round_chooser);
        detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener()
        {
            public void onLongPress(MotionEvent e)
            {
                DismissOverlayView dismissOverlay = (DismissOverlayView) (ActivitySelector.this).findViewById(R.id.dismiss_overlay);
                dismissOverlay.setIntroText(R.string.welcome_long_press_exit);
                dismissOverlay.showIntroIfNecessary();
                dismissOverlay.show();
            }
        });

        // register activity
        ActivityController.getInstance().add("ActivitySelector", this);

        // adapter
        this.adapterActivity = new ItemListViewAdapter(ActivitySelector.this, false);
        this.adapterActivity.setSelectedElements(selectedActivites);

        // view
        TextView tv = (TextView) findViewById(R.id.posture_posture_headline);
        tv.setText(R.string.activity_headline);

        this.mainView = (WearableListView) findViewById(R.id.posture_posture_list);
        this.mainView.setAdapter(this.adapterActivity);
        this.mainView.setClickListener(new ActivityClickListener());

        // send database request
        String query = "SELECT t1.name, t2.name FROM " + SQLTableName.ACTIVITIES + " t1 LEFT OUTER JOIN " + SQLTableName.SUBACTIVITIES + " t2 ON t1.id == t2.activityid";
        BroadcastService.getInstance().sendMessage("/database/request/activity", query);
    }


    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev)
    {
        return detector.onTouchEvent(ev) || super.dispatchTouchEvent(ev);
    }


    public void createActivityList(final String s)
    {
        // process data
        allActivities = new TreeMap<>();
        Set<String> tmp = new HashSet<>();
        tmp.addAll(Arrays.asList(s.split("\n")));

        // process 's'
        for(String value : tmp) {   // should be fast because tmp is small
            int pos = value.indexOf(Settings.DATABASE_DELIMITER);
            String key = value;
            String object = null;

            if(pos != -1) {
                key = key.substring(0, pos).trim();
                object = value.substring(pos + Settings.DATABASE_DELIMITER.length()).trim();
            }

            if(!allActivities.containsKey(key)) {
                allActivities.put(key, new TreeSet<String>());
            }

            if(object != null && !object.equals("null")) {
                allActivities.get(key).add(object);
            }
        }

        // refresh UI
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                // data
                List<String> activities = new ArrayList<>(allActivities.keySet());

                int pos = 0;
                if(selectedActivites.size() > 0) {
                    String selAct = selectedActivites.get(selectedActivites.size() - 1);
                    if(selAct.contains(Settings.ACTIVITY_DELIMITER)) { // subactivity?
                        selAct = selAct.substring(0, selAct.indexOf(Settings.ACTIVITY_DELIMITER));
                    }

                    pos = Utils.getPosition(selAct, activities);
                }

                // progressbar
                ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar1);
                pb.setVisibility(View.INVISIBLE);

                // update adapter
                ActivitySelector.this.adapterActivity.update(null, activities);
                ActivitySelector.this.adapterActivity.notifyDataSetChanged();

                // view
                ActivitySelector.this.mainView.scrollToPosition(pos);
            }
        });
    }


    private class ActivityClickListener implements WearableListView.ClickListener
    {
        private String lastParent;


        @Override
        public void onClick(WearableListView.ViewHolder viewHolder)
        {
            // parse data
            String activity = ActivitySelector.this.adapterActivity.get(viewHolder.getPosition());

            // subactivities?
            if(allActivities != null && allActivities.containsKey(activity) && allActivities.get(activity).size() > 0) {
                lastParent = activity;

                // update adapter
                NavigableSet<String> subActivities = allActivities.get(activity);
                ActivitySelector.this.adapterActivity.update(activity, new ArrayList<>(subActivities));
                ActivitySelector.this.adapterActivity.notifyDataSetChanged();

                // view
                ActivitySelector.this.mainView.setAdapter(ActivitySelector.this.adapterActivity);

                // determine position - should be fast because there are only less values
                String lastSelectedSubActivity = null;
                ListIterator<String> it = selectedActivites.listIterator(selectedActivites.size());
                while(it.hasPrevious()) {
                    String entry = it.previous();

                    if(entry.startsWith(activity) && entry.contains(Settings.ACTIVITY_DELIMITER)) {
                        lastSelectedSubActivity = entry.substring(entry.indexOf(Settings.ACTIVITY_DELIMITER) + Settings.ACTIVITY_DELIMITER.length());
                        break;
                    }
                }
                int pos = Utils.getPosition(lastSelectedSubActivity, new ArrayList<>(subActivities));
                ActivitySelector.this.mainView.scrollToPosition(pos);

                return;
            }

            if(allActivities != null && !allActivities.containsKey(activity)) {  // subactivity
                activity = lastParent + Settings.ACTIVITY_DELIMITER + activity;
            }

            // update mobile device
            if(!selectedActivites.contains(activity)) {
                BroadcastService.getInstance().sendMessage("/activity/update", activity);
            } else {
                BroadcastService.getInstance().sendMessage("/activity/delete", activity);
            }

            // update local device
            MainActivity mc = (MainActivity) ActivityController.getInstance().get("MainActivity");
            if(!selectedActivites.contains(activity)) {
                mc.updateActivityView(activity);
            } else {
                selectedActivites.remove(activity);
                mc.updateActivityView(null);    // refresh
            }

            // clean up
            ((TextView) findViewById(R.id.posture_posture_headline)).setText("");
            ActivitySelector.this.mainView.setAdapter(null);
            ActivitySelector.this.mainView.setOnClickListener(null);

            // finish
            ActivitySelector.this.finish();
            ActivitySelector.this.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        }


        @Override
        public void onTopEmptyRegionClick()
        {
            // TODO
        }
    }


    public static List<String> get()
    {
        return selectedActivites;
    }


    public static void add(String[] activities)
    {
        if(activities == null || activities.length == 0) {
            return;
        }

        for(String activity : activities) {
            if(activity == null || activity.trim().length() == 0) { // valid value?
                continue;
            }
            String value = activity.trim();

            if(value.contains(Settings.DATABASE_DELIMITER)) {   // subactivity?
                value = value.replace(Settings.DATABASE_DELIMITER, Settings.ACTIVITY_DELIMITER);
            }

            if(value.contains("null")) {    // no subactivity?
                value = value.substring(0, value.indexOf(Settings.ACTIVITY_DELIMITER));
            }

            if(!selectedActivites.contains(value)) {    // already exists?
                selectedActivites.add(value);
            }
        }
    }


    public static boolean rm(String activity)
    {
        return selectedActivites.remove(activity);
    }


    public static void delete()
    {
        selectedActivites.clear();
    }
}