package de.unima.ar.collector.controller;

import android.app.Activity;

import java.util.HashMap;
import java.util.Map;

public class ActivityController
{
    private static ActivityController ACTIVITY = null;

    private Map<String, Activity> activities;


    private ActivityController()
    {
        this.activities = new HashMap<>();
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
    }


    public Activity get(String key)
    {
        return this.activities.get(key);
    }


    public void shutdown()
    {
        this.activities = new HashMap<>();
    }
}