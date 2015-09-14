package de.unima.ar.collector.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

import de.unima.ar.collector.R;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.database.DatabaseExporter;
import de.unima.ar.collector.shared.database.SQLTableName;


/**
 * @author Fabian Kramm
 */
public class SelectExportTables extends DialogFragment
{
    private ArrayList<Integer> mSelectedItems;
    private ArrayList<String>  arrTblNames;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        mSelectedItems = new ArrayList<Integer>(); // Where we track the selected items
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get all tables
        arrTblNames = new ArrayList<String>();
        List<String[]> result = SQLDBController.getInstance().query("SELECT name FROM sqlite_master WHERE type='table'", null, false);

        for(String[] row : result) {
            String tableName = row[0];
            if(!tableName.startsWith(SQLTableName.PREFIX)) {
                continue;
            }
            arrTblNames.add(row[0]);
        }

        boolean checked[] = new boolean[arrTblNames.size()];

        for(int i = 0; i < arrTblNames.size(); i++) {
            checked[i] = true;
            mSelectedItems.add(Integer.valueOf(i));
        }

        // Set the dialog title
        builder.setTitle(getString(R.string.option_export_dialog_title))
                // Specify the list array, the items to be selected by default
                // (null for none),
                // and the listener through which to receive callbacks when
                // items are selected
                .setMultiChoiceItems((CharSequence[]) arrTblNames.toArray(new String[arrTblNames.size()]), checked, new DialogInterface.OnMultiChoiceClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked)
                    {
                        if(isChecked) {
                            // If the user checked the item, add it to
                            // the selected items
                            mSelectedItems.add(which);
                        } else if(mSelectedItems.contains(which)) {
                            // Else, if the item is already in the
                            // array, remove it
                            mSelectedItems.remove(Integer.valueOf(which));
                        }
                    }
                }).setPositiveButton(R.string.option_export_dialog_ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                ArrayList<String> tablesToExport = new ArrayList<>();

                for(int i = 0; i < mSelectedItems.size(); i++) {
                    tablesToExport.add(arrTblNames.get(mSelectedItems.get(i)));
                }

                new DatabaseExporter(SelectExportTables.this.getActivity(), tablesToExport).execute();
            }
        }).setNegativeButton(getString(R.string.option_export_dialog_cancel), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int id)
            {

            }
        });

        return builder.create();
    }
}