package de.unima.ar.collector.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.shared.database.SQLTableName;


/**
 * @author Fabian Kramm
 */
public class DeleteSubActivity extends DialogFragment
{
    static class Container
    {
        int    id;
        String name;
    }

    private MainActivity ma;


    public void setMainActivity(MainActivity ma)
    {
        this.ma = ma;
    }


    /*
     * (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle("Delete Activity");

        final View view = inflater.inflate(R.layout.addactivitydialog, null);

        Spinner sp1 = (Spinner) view.findViewById(R.id.addactivity_spinner1);

        String queryString = "SELECT name, id FROM " + SQLTableName.ACTIVITIES;
        List<String[]> result = SQLDBController.getInstance().query(queryString, null, false);

        final ArrayList<Container> activities = new ArrayList<Container>();
        ArrayList<String> sActivities = new ArrayList<String>();

        for(String[] row : result) {
            Container co = new Container();

            co.name = row[0];
            co.id = Integer.valueOf(row[1]);

            activities.add(co);
            sActivities.add(co.name);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        final ArrayAdapter acadapter = new ArrayAdapter(this.getActivity(), android.R.layout.simple_spinner_item, sActivities);
        sp1.setAdapter(acadapter);

        // Get SubActivities
        // Get Subactivities
        Spinner sp2 = (Spinner) view.findViewById(R.id.addactivity_spinner2);
        final ArrayList<String> subactivities;

        // Fill subactivity list if there is data
        if(activities.size() != 0) {
            subactivities = DatabaseHelper.getStringResultSet("SELECT name FROM " + SQLTableName.SUBACTIVITIES + " WHERE activityid = ? ", new String[]{ "" + activities.get(0).id });
        } else {
            subactivities = new ArrayList<String>();
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final ArrayAdapter subacadapter = new ArrayAdapter(this.getActivity(), android.R.layout.simple_spinner_item, subactivities);
        sp2.setAdapter(subacadapter);

        // Handle click on first spinner
        sp1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {

            @SuppressWarnings("unchecked")
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long id)
            {
                subacadapter.clear();

                Container co = activities.get((int) id);

                // Unsere Activity gefunden jetzt holen wir die
                // subactivities
                ArrayList<String> subactivities = DatabaseHelper.getStringResultSet("SELECT name FROM " + SQLTableName.SUBACTIVITIES + " WHERE activityid = ? ", new String[]{ "" + co.id });

                for(int j = 0; j < subactivities.size(); j++) {
                    subacadapter.add(subactivities.get(j));
                }

                subacadapter.notifyDataSetChanged();
            }


            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }

        });

        final Context context = this.getActivity();

        builder.setView(view);
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // Before we can delete the activity we need to delete
                // activity data and all subactivities
                Spinner sp = (Spinner) view.findViewById(R.id.addactivity_spinner1);

                if(sp.getSelectedItemId() < 0) {
                    return;
                }

                Spinner sp2 = (Spinner) view.findViewById(R.id.addactivity_spinner2);

                if(sp2.getSelectedItemId() < 0) {
                    Toast.makeText(context, "No subactivity selected!", Toast.LENGTH_LONG).show();
                    return;
                }

                int aId = activities.get((int) sp.getSelectedItemId()).id;
                int sId = Integer.parseInt(DatabaseHelper.getStringResultSet("SELECT id FROM " + SQLTableName.SUBACTIVITIES + " WHERE activityid = ? AND name = ? ", new String[]{ "" + aId, sp2.getSelectedItem().toString() }).get(0));

                // Delete running and old subactivity data and earse it
                // from the table
                SQLDBController.getInstance().delete(SQLTableName.ACTIVITYDATA, "activityid = ? AND subactivityid = ? ", new String[]{ "" + aId, "" + sId });
                SQLDBController.getInstance().delete(SQLTableName.SUBACTIVITIES, "id = ? ", new String[]{ "" + sId });

                Toast.makeText(context, "Sub-Activity " + sp2.getSelectedItem().toString() + " successfull deleted!", Toast.LENGTH_LONG).show();

                ma.showActivities();
            }
        });

        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {

            }
        });

        return builder.create();
    }
}