package de.unima.ar.collector.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.unima.ar.collector.R;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.database.DatabaseExportCSV;
import de.unima.ar.collector.shared.database.SQLTableName;


/**
 * @author Timo Sztyler
 */
public class DatabaseExportCSVDialog extends DialogFragment implements DialogInterface.OnMultiChoiceClickListener
{
    private ArrayList<String> tableNames;
    private boolean[]         isChecked;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        super.onCreateDialog(savedInstanceState);

        this.tableNames = new ArrayList<>();

        // query table names
        List<String[]> result = SQLDBController.getInstance().query("SELECT name FROM sqlite_master WHERE type='table'", null, false);

        // store data
        for(String[] row : result) {
            String tableName = row[0];
            if(!tableName.startsWith(SQLTableName.PREFIX)) {
                continue;
            }
            this.tableNames.add(row[0]);
        }

        // checkbox
        this.isChecked = new boolean[this.tableNames.size()];
        Arrays.fill(this.isChecked, Boolean.TRUE);

        // build dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.option_export_dialog_title));
        builder.setMultiChoiceItems(tableNames.toArray(new String[tableNames.size()]), this.isChecked, this);
        builder.setPositiveButton(R.string.option_export_dialog_ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                ArrayList<String> tablesToExport = new ArrayList<>();

                for(int i = 0; i < DatabaseExportCSVDialog.this.isChecked.length; i++) {
                    if(DatabaseExportCSVDialog.this.isChecked[i]) {
                        tablesToExport.add(tableNames.get(i));
                    }
                }

                new DatabaseExportCSV(DatabaseExportCSVDialog.this.getActivity(), tablesToExport).execute();
            }
        }).setNegativeButton(getString(R.string.option_export_dialog_cancel), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                DatabaseExportCSVDialog.this.dismissAllowingStateLoss();
            }
        });

        return builder.create();
    }


    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked)
    {
        if(which > 0 || which < this.isChecked.length) {
            this.isChecked[which] = isChecked;
        }
    }
}