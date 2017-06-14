package de.unima.ar.collector.database;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.widget.Toast;

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
        String uuid = intent.getStringExtra("uuid");


        if(sql == null || uuid == null) {
            return;
        }

        instance.bulkInsertFromIntent(uuid, sql);

        if(main != null) {
            Utils.makeToast2(main, R.string.sensor_cache_to_database_done, Toast.LENGTH_SHORT);
        }
    }
}
