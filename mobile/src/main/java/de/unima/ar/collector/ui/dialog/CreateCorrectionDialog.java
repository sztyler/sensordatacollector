package de.unima.ar.collector.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.ui.DatePickerListener;
import de.unima.ar.collector.ui.TimePickerListener;
import de.unima.ar.collector.util.UIUtils;

public class CreateCorrectionDialog extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        View view = getActivity().getLayoutInflater().inflate(R.layout.activity_correction_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setTitle(getString(R.string.activity_correction_dialog_add_title));

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

        long currentTime = new Date().getTime();

        long lastChange = new Date().getTime() - 3600000;

        final EditText fromDate = (EditText) view.findViewById(R.id.correctionfromdate);
        fromDate.setInputType(EditorInfo.TYPE_NULL);
        fromDate.setText(dateFormat.format(new Date(lastChange)));
        fromDate.setOnClickListener(new DatePickerListener(getActivity(), fromDate));

        final EditText fromTime = (EditText) view.findViewById(R.id.correctionfromtime);
        fromTime.setInputType(EditorInfo.TYPE_NULL);
        fromTime.setText(timeFormat.format(new Date(lastChange)));
        fromTime.setOnClickListener(new TimePickerListener(getActivity(), fromTime));

        final EditText toDate = (EditText) view.findViewById(R.id.correctiontodate);
        toDate.setInputType(EditorInfo.TYPE_NULL);
        toDate.setText(dateFormat.format(new Date(currentTime)));
        toDate.setOnClickListener(new DatePickerListener(getActivity(), toDate));

        final EditText toTime = (EditText) view.findViewById(R.id.correctiontotime);
        toTime.setInputType(EditorInfo.TYPE_NULL);
        toTime.setText(timeFormat.format(new Date(currentTime)));
        toTime.setOnClickListener(new TimePickerListener(getActivity(), toTime));

        final CheckBox checkEnvironment = (CheckBox) view.findViewById(R.id.markEnvironmentChkBx);
        final CheckBox checkPosture = (CheckBox) view.findViewById(R.id.markPosturesChkBx);
        final CheckBox checkDevicePosition = (CheckBox) view.findViewById(R.id.markDeviceChkBx);
        final CheckBox checkAcitivity = (CheckBox) view.findViewById(R.id.markActivityChkBx);

        builder.setNegativeButton(R.string.activity_dialog_cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // User cancelled the dialog
            }
        });
        builder.setPositiveButton(R.string.activity_dialog_add, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");
                SimpleDateFormat timeDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH);

                try {
                    long fromTimeNew = timeDateFormat.parse(fromDate.getText().toString() + " " + fromTime.getText().toString()).getTime();
                    long toTimeNew = timeDateFormat.parse(toDate.getText().toString() + " " + toTime.getText().toString()).getTime();

                    StringBuilder sb = new StringBuilder();

                    if(checkEnvironment.isChecked()) {
                        sb.append(getString(R.string.activity_correction_dialog_add_environment) + ", ");
                    }
                    if(checkPosture.isChecked()) {
                        sb.append(getString(R.string.activity_correction_dialog_add_posture) + ", ");
                    }
                    if(checkDevicePosition.isChecked()) {
                        sb.append(getString(R.string.activity_correction_dialog_add_device) + ", ");
                    }
                    if(checkAcitivity.isChecked()) {
                        sb.append(getString(R.string.activity_correction_dialog_add_activity) + ", ");
                    }

                    if(fromTimeNew >= toTimeNew || sb.length() == 0) {
                        UIUtils.makeToast(main, R.string.activity_correction_dialog_add_invalid, Toast.LENGTH_SHORT);
                        return;
                    }

                    sb.setLength(sb.length() - 2);

                    ContentValues cv = new ContentValues();
                    cv.put("starttime", fromTimeNew);
                    cv.put("endtime", toTimeNew);
                    cv.put("log", sb.toString());

                    SQLDBController.getInstance().insert(SQLTableName.ACTIVITYCORRECTION, null, cv);

                    main.refreshActivityCorrectionHistoryScreen();
                } catch(ParseException e) {
                    e.printStackTrace();
                }
            }
        });


        if(Build.VERSION.SDK_INT <= 16) {   // layout workaround
            TextView text = (TextView) view.findViewById(R.id.choosetext2);
            text.setPadding(0, 0, 0, 30);
            text.setText(R.string.activity_correction_dialog_add_text2);
            text.setGravity(Gravity.FILL);
        }

        return builder.create();
    }
}
