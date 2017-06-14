package de.unima.ar.collector.database;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;

import de.unima.ar.collector.R;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.shared.util.Utils;


public class BulkInsertService extends IntentService
{
    public BulkInsertService()
    {
        super("DBInsert");
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent)
    {
        SQLDBController instance = SQLDBController.getInstance();

        if(instance == null || intent == null) {
            return;
        }

        Activity main = ActivityController.getInstance().get("MainActivity");
        if(main != null) {
            Utils.makeToast2(main, R.string.sensor_cache_to_database, Toast.LENGTH_SHORT);
        }

        String sql = intent.getStringExtra("sqlQuery");
        Serializable object = intent.getSerializableExtra("values");


        if(sql == null || object == null || !(object instanceof ArrayList<?>)) {
            return;
        }

        ArrayList<String[]> values = new ArrayList<>();
        for(Object o : (ArrayList<?>) object) {
            if(o instanceof String[]) {
                values.add((String[]) o);
            }
        }

        instance.bulkInsertFromIntent(sql, values);

        if(main != null) {
            Utils.makeToast2(main, R.string.sensor_cache_to_database_done, Toast.LENGTH_SHORT);
        }
    }
}
