package de.unima.ar.collector.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import de.unima.ar.collector.R;
import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.ui.TwoColumnAdapter;


/**
 * @author Fabian Kramm, Timo Sztyler
 */
public class SelectActivityDialog extends DialogFragment
{
    private ArrayAdapter<String> activityadapter;


    public void setAdapter(ArrayAdapter<String> activityadapter)
    {
        this.activityadapter = activityadapter;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle(this.getString(R.string.activity_activities_select));

        final View view = inflater.inflate(R.layout.addactivitydialog, null);

        // DONE
        Spinner sp1 = (Spinner) view.findViewById(R.id.addactivity_spinner1);

        String queryString = "SELECT A.name, A.id, S.id FROM " + SQLTableName.ACTIVITIES + " A LEFT OUTER JOIN " + SQLTableName.SUBACTIVITIES + " S ON A.id=S.activityid GROUP BY A.name;";
        List<String[]> result = SQLDBController.getInstance().query(queryString, null, false);

        final NavigableMap<String, String> activities = new TreeMap<>();
        NavigableMap<String, String> hasSub = new TreeMap<>();

        for(String[] row : result) {
            activities.put(row[0], row[1]);
            hasSub.put(row[0], row[2] == null ? "" : "\u2A2E");
        }

        TwoColumnAdapter adapter = new TwoColumnAdapter(this.getActivity(), new ArrayList<>(activities.keySet()), new ArrayList<>(hasSub.values()));
        sp1.setAdapter(adapter);

        // Get Subactivities
        final TextView textView2 = (TextView) view.findViewById(R.id.textView2);
        final Spinner sp2 = (Spinner) view.findViewById(R.id.addactivity_spinner2);
        final ArrayList<String> subactivities;

        // Fill subactivity list if there is data
        if(activities.size() > 0) {
            subactivities = DatabaseHelper.getStringResultSet("SELECT name FROM " + SQLTableName.SUBACTIVITIES + " WHERE activityid = ? ", new String[]{ activities.firstEntry().getValue() });
            Collections.sort(subactivities);
            if(subactivities.contains(getString(R.string.activity_subactivities_general_other))) {
                subactivities.remove(getString(R.string.activity_subactivities_general_other));
                subactivities.add(getString(R.string.activity_subactivities_general_other));        // last position
            }
        } else {
            subactivities = new ArrayList<>();
        }

        if(subactivities.size() > 0) {
            textView2.setVisibility(View.VISIBLE);
            sp2.setVisibility(View.VISIBLE);
        } else {
            textView2.setVisibility(View.GONE);
            sp2.setVisibility(View.GONE);
        }

        final ArrayAdapter<String> subacadapter = new ArrayAdapter<>(this.getActivity(), R.layout.spinner_layout, subactivities);
        sp2.setAdapter(subacadapter);

        // Handle click on first spinner
        sp1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long id)
            {
                subacadapter.clear();

                List<String> activityIDs = new ArrayList<>(activities.values());
                String activityID = activityIDs.get((int) id); // this cast should be safe

                List<String> subactivities = DatabaseHelper.getStringResultSet("SELECT name FROM " + SQLTableName.SUBACTIVITIES + " WHERE activityid = ? ", new String[]{ activityID });

                if(subactivities.contains(getString(R.string.activity_subactivities_general_other))) {
                    subactivities.remove(getString(R.string.activity_subactivities_general_other));
                    subactivities.add(getString(R.string.activity_subactivities_general_other));        // last position
                }

                if(subactivities.size() > 0) {
                    textView2.setVisibility(View.VISIBLE);
                    sp2.setVisibility(View.VISIBLE);
                } else {
                    textView2.setVisibility(View.GONE);
                    sp2.setVisibility(View.GONE);
                }

                Collections.sort(subactivities);
                subacadapter.addAll(subactivities);
                subacadapter.notifyDataSetChanged();
            }


            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }

        });

        builder.setView(view);
        builder.setPositiveButton(R.string.activity_dialog_add, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // Add the activity
                Spinner spinner1 = (Spinner) view.findViewById(R.id.addactivity_spinner1);
                String text1 = spinner1.getSelectedItem().toString();

                Spinner spinner2 = (Spinner) view.findViewById(R.id.addactivity_spinner2);
                if(spinner2.getSelectedItem() != null) {
                    text1 = text1 + Settings.ACTIVITY_DELIMITER + spinner2.getSelectedItem().toString();
                }

                // Check if activity is already there
                for(int i = 1; i < activityadapter.getCount(); i++) {
                    if(activityadapter.getItem(i).equals(text1)) {
                        Toast.makeText(getActivity(), SelectActivityDialog.this.getString(R.string.activity_activities_exist), Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                activityadapter.add(text1);
                activityadapter.notifyDataSetChanged();

                // Insert new Data set into db
                int ActivityId = Integer.parseInt(DatabaseHelper.getStringResultSet("SELECT id FROM " + SQLTableName.ACTIVITIES + " WHERE name = ? ", new String[]{ spinner1.getSelectedItem().toString() }).get(0));

                int SubActivityId = -1;

                if(spinner2.getSelectedItem() != null) {
                    SubActivityId = Integer.parseInt(DatabaseHelper.getStringResultSet("SELECT id FROM " + SQLTableName.SUBACTIVITIES + " WHERE name = ? AND activityid = ? ", new String[]{ spinner2.getSelectedItem().toString(), ActivityId + "" }).get(0));
                }

                // Add new data set
                ContentValues newValues = new ContentValues();
                // Assign values for each row.
                newValues.put("activityid", ActivityId);

                if(SubActivityId >= 0) {
                    newValues.put("subactivityid", SubActivityId);
                }

                newValues.put("starttime", System.currentTimeMillis());
                newValues.put("endtime", 0);

                SQLDBController.getInstance().insert(SQLTableName.ACTIVITYDATA, null, newValues);
                BroadcastService.getInstance().sendMessage("/database/response/currentActivity", text1);
            }
        });
        builder.setNegativeButton(R.string.activity_dialog_cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // User cancelled the dialog
            }
        });

        return builder.create();
    }
}