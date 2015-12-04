package de.unima.ar.collector.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;

import de.unima.ar.collector.R;
import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.ui.dialog.SelectActivityDialog;
import de.unima.ar.collector.util.DBUtils;


/**
 * @author Fabian Kramm, Timo Sztyler
 */
public class ActivityOnItemClickListener implements android.widget.AdapterView.OnItemClickListener
{
    private FragmentActivity     context;
    private ArrayAdapter<String> positionAdapter;
    private ArrayAdapter<String> postureAdapter;
    private ArrayAdapter<String> devicePositionAdapter;
    private ArrayAdapter<String> activityAdapter;


    public ActivityOnItemClickListener(FragmentActivity ctx, ArrayAdapter<String> positionAdapter, ArrayAdapter<String> postureAdapter, ArrayAdapter<String> devicePositionAdapter, ArrayAdapter<String> activityAdapter)
    {
        context = ctx;

        this.positionAdapter = positionAdapter;
        this.postureAdapter = postureAdapter;
        this.devicePositionAdapter = devicePositionAdapter;
        this.activityAdapter = activityAdapter;
    }


    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long id)
    {
        if(id == 1) {    // Select Position
            ArrayList<String> positions = DatabaseHelper.getStringResultSet("SELECT name FROM " + SQLTableName.POSITIONS, null);
            positions.remove(context.getString(R.string.activity_environment_none));
            Collections.sort(positions);
            positions.add(0, context.getString(R.string.activity_environment_none));

            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            builder.setTitle(context.getString(R.string.activity_environment_select));
            DialogActivityListAdapter adapter = new DialogActivityListAdapter(this.context, R.layout.dialog_entry_activity, positions);
            builder.setAdapter(adapter, new ChooseListener(SQLTableName.POSITIONS, SQLTableName.POSITIONDATA, positionAdapter));
            builder.setNegativeButton(R.string.activity_dialog_cancel, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    // User cancelled the dialog
                }
            });
            builder.create().show();
        } else if(id == 3) { // Select Posture
            ArrayList<String> postures = DatabaseHelper.getStringResultSet("SELECT name FROM " + SQLTableName.POSTURES, null);
            postures.remove(context.getString(R.string.activity_posture_none));
            Collections.sort(postures);
            postures.add(0, context.getString(R.string.activity_posture_none));

            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            builder.setTitle(context.getString(R.string.activity_posture_select));
            DialogActivityListAdapter adapter = new DialogActivityListAdapter(this.context, R.layout.dialog_entry_activity, postures);
            builder.setAdapter(adapter, new ChooseListener(SQLTableName.POSTURES, SQLTableName.POSTUREDATA, postureAdapter));
            builder.setNegativeButton(R.string.activity_dialog_cancel, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    // User cancelled the dialog
                }
            });
            builder.create().show();
        } else if(id == 5) {
            ArrayList<String> devicePositions = DatabaseHelper.getStringResultSet("SELECT name FROM " + SQLTableName.DEVICEPOSITION, null);
            devicePositions.remove(context.getString(R.string.activity_devicepositon_none));
            Collections.sort(devicePositions);
            devicePositions.add(0, context.getString(R.string.activity_devicepositon_none));

            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            builder.setTitle(context.getString(R.string.activity_devicepositon_title));
            DialogActivityListAdapter adapter = new DialogActivityListAdapter(this.context, R.layout.dialog_entry_activity, devicePositions);
            builder.setAdapter(adapter, new ChooseListener(SQLTableName.DEVICEPOSITION, SQLTableName.DEVICEPOSITIONDATA, devicePositionAdapter));
            builder.setNegativeButton(R.string.activity_dialog_cancel, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    // User cancelled the dialog
                }
            });
            builder.create().show();
        } else if(id == 7) {         // Add Activity
            SelectActivityDialog dialog = new SelectActivityDialog();
            dialog.setAdapter(activityAdapter);
            dialog.show(context.getSupportFragmentManager(), "ActivityDialogFragment");
        }
    }


    /**
     * Diese Klasse ist der Listener für Position und Posture Dialoge,
     * sie sorgt dafür das der ArrayAdapter angepasst wird und den
     * aktuellen Wert übernimmt Außerdem wird ein neuer Eintrag erstellt
     * in der Datenbank
     *
     * @author Fabian Kramm, Timo Sztyler
     */
    private class ChooseListener implements DialogInterface.OnClickListener
    {
        private String               tableName;
        private String               dataTableName;
        private ArrayAdapter<String> adapter;


        public ChooseListener(String tableName, String dataTableName, ArrayAdapter<String> adapter)
        {
            this.tableName = tableName;
            this.adapter = adapter;
            this.dataTableName = dataTableName;
        }


        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            // collect data
            ListView lw = ((AlertDialog) dialog).getListView();
            String name = lw.getAdapter().getItem(which).toString();

            // update database
            boolean result = DBUtils.updateActivity(name, dataTableName, tableName);
            if(!result) {
                return;
            }

            // inform other devices
            if(tableName.equals(SQLTableName.POSTURES)) {
                BroadcastService.getInstance().sendMessage("/database/response/currentPosture", name);
            }
            if(tableName.equals(SQLTableName.POSITIONS)) {
                BroadcastService.getInstance().sendMessage("/database/response/currentPosition", name);
            }

            // update UI
            adapter.clear();
            adapter.add(name);
            adapter.notifyDataSetChanged();
        }
    }
}