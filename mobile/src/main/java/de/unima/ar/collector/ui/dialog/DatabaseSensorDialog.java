package de.unima.ar.collector.ui.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.ui.DatePickerListener;
import de.unima.ar.collector.ui.TimePickerListener;
import de.unima.ar.collector.util.PlotConfiguration;


/**
 * @author Fabian Kramm
 */
@SuppressLint("InflateParams")
public class DatabaseSensorDialog extends DialogFragment
{
    private MainActivity      context;
    private PlotConfiguration pc;


    public void setContext(MainActivity context)
    {
        this.context = context;
    }


    public void setPlotConfig(PlotConfiguration pc)
    {
        this.pc = pc;
    }


    /*
     * (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle(getString(R.string.analyze_analyze_database_dialog_title));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.databasesensordialog, null);

        builder.setView(view);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

        final EditText fromDate = (EditText) view.findViewById(R.id.databasefromdate);
        fromDate.setInputType(EditorInfo.TYPE_NULL);
        fromDate.setText(dateFormat.format(new Date(new Date().getTime() - 86400000)));
        fromDate.setOnClickListener(new DatePickerListener(getActivity(), fromDate));

        final EditText fromTime = (EditText) view.findViewById(R.id.databasefromtime);
        fromTime.setInputType(EditorInfo.TYPE_NULL);
        fromTime.setText(timeFormat.format(new Date(new Date().getTime() - 86400000)));
        fromTime.setOnClickListener(new TimePickerListener(getActivity(), fromTime));

        final EditText toDate = (EditText) view.findViewById(R.id.databasetodate);
        toDate.setInputType(EditorInfo.TYPE_NULL);
        toDate.setText(dateFormat.format(new Date(new Date().getTime() + 60000)));
        toDate.setOnClickListener(new DatePickerListener(getActivity(), toDate));

        final EditText toTime = (EditText) view.findViewById(R.id.databasetotime);
        toTime.setInputType(EditorInfo.TYPE_NULL);
        toTime.setText(timeFormat.format(new Date(new Date().getTime() + 60000)));
        toTime.setOnClickListener(new TimePickerListener(getActivity(), toTime));

        builder.setPositiveButton(R.string.dialog_ok, null);

        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {

            }
        });

        final AlertDialog d = builder.create();

        final CheckBox showActivities = (CheckBox) view.findViewById(R.id.showActivitiesChkBx);
        showActivities.setChecked(true);

        final CheckBox showPositions = (CheckBox) view.findViewById(R.id.showPositionsChkBx);
        showPositions.setChecked(true);

        final CheckBox showPostures = (CheckBox) view.findViewById(R.id.showPosturesChkBx);
        showPostures.setChecked(true);

        d.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface arg0)
            {
                Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View arg0)
                    {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH);

                        String fromString = fromDate.getText().toString() + " " + fromTime.getText().toString();
                        String toString = toDate.getText().toString() + " " + toTime.getText().toString();

                        long lFromTime = 0;
                        long lToTime = 0;

                        try {
                            lFromTime = sdf.parse(fromString).getTime();
                            lToTime = sdf.parse(toString).getTime();

                            if(lFromTime == lToTime || lFromTime > lToTime) {
                                Toast.makeText(d.getContext(), getString(R.string.analyze_analyze_database_dialog_notify1), Toast.LENGTH_LONG).show();
                                return;
                            }
                        } catch(ParseException e1) {
                            Toast.makeText(d.getContext(), getString(R.string.analyze_analyze_database_dialog_notify2), Toast.LENGTH_LONG).show();
                            return;
                        }

                        context.showAnalyzeDatabaseData(pc, String.valueOf(lFromTime), String.valueOf(lToTime), showActivities.isChecked(), showPositions.isChecked(), showPostures.isChecked());
                        d.dismiss();
                    }
                });

            }
        });

        return d;
    }
}