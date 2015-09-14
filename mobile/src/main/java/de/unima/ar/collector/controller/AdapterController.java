package de.unima.ar.collector.controller;


import android.widget.BaseAdapter;

import java.util.HashMap;
import java.util.Map;

public class AdapterController
{
    private static AdapterController INSTANCE;

    private Map<String, BaseAdapter> adapters;


    private AdapterController()
    {
        this.adapters = new HashMap<>();
    }


    public static AdapterController getInstance()
    {
        if(INSTANCE == null) {
            INSTANCE = new AdapterController();
        }

        return INSTANCE;
    }


    public void register(String key, BaseAdapter adapter)
    {
        this.adapters.put(key, adapter);
    }


    public BaseAdapter get(String key)
    {
        return this.adapters.get(key);
    }
}