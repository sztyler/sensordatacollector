package de.unima.ar.collector.controller;

import android.app.Activity;

import java.util.HashMap;
import java.util.Map;

public class ActivityController
{
    private static ActivityController ACTIVITY = null;

    private Map<String, Activity> activities;
    private Map<Activity, State>  states;

    public enum State
    {
        onCreate, onPause_AUTO, onPause_MANUAL, onPause_CHOOSER, onPause_ACTIVITYSELECTOR
    }


    private ActivityController()
    {
        this.activities = new HashMap<>();
        this.states = new HashMap<>();
    }


    public static ActivityController getInstance()
    {
        if(ACTIVITY == null) {
            ACTIVITY = new ActivityController();
        }

        return ACTIVITY;
    }


    public void add(String key, Activity activity)
    {
        this.activities.put(key, activity);
        this.states.put(activity, State.onCreate);
    }


    public Activity get(String key)
    {
        return this.activities.get(key);
    }


    public State getState(Activity activity)
    {
        return this.states.get(activity);
    }


    public void setState(Activity activity, State state)
    {
        this.states.put(activity, state);
    }


    public void shutdown()
    {
        for(String key : this.activities.keySet()) {
            if(key.equalsIgnoreCase("MainActivity")) {
                continue;
            }

            this.activities.get(key).finish();
        }

        this.activities = new HashMap<>();
        this.states = new HashMap<>();
    }
}