package de.unima.ar.collector.ui.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import de.unima.ar.collector.R;
import de.unima.ar.collector.database.DatabaseDelete;

public class DatabaseDeleteDialog extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.option_export_delete);
        builder.setMessage(R.string.option_export_delete_message);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                DatabaseDelete task = new DatabaseDelete(DatabaseDeleteDialog.this.getActivity());
                task.execute();
            }
        }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                DatabaseDeleteDialog.this.dismissAllowingStateLoss();
            }
        }).setIcon(android.R.drawable.ic_dialog_alert);

        View checkBoxView = View.inflate(getActivity(), R.layout.checkbox, null);
        final CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        checkBox.setText(R.string.option_export_delete_message_checkbox);
        builder.setView(checkBoxView);

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface dialog)
            {
                final AlertDialog alert = (AlertDialog) dialog;
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                    {
                        boolean currentState = alert.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled();
                        alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!currentState);
                    }
                });
            }
        });

        return dialog;
    }
}