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
import android.widget.EditText;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.SensorDataCollectorService;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.DeviceID;
import de.unima.ar.collector.ui.DatePickerListener;
import de.unima.ar.collector.ui.TimePickerListener;


/**
 * @author Fabian Kramm
 */
@SuppressLint("SimpleDateFormat")
public class GPSDatabaseDialog extends DialogFragment
{
    private MainActivity context;


    public void setContext(MainActivity context)
    {
        this.context = context;
    }


    /*
     * (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @SuppressLint("SimpleDateFormat")
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

        view.findViewById(R.id.showActivitiesLabel).setVisibility(View.GONE);
        view.findViewById(R.id.showActivitiesChkBx).setVisibility(View.GONE);
        view.findViewById(R.id.showPositionsLabel).setVisibility(View.GONE);
        view.findViewById(R.id.showPositionsChkBx).setVisibility(View.GONE);
        view.findViewById(R.id.showPosturesLabel).setVisibility(View.GONE);
        view.findViewById(R.id.showPosturesChkBx).setVisibility(View.GONE);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

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
        toDate.setText(dateFormat.format(new Date()));
        toDate.setOnClickListener(new DatePickerListener(getActivity(), toDate));

        final EditText toTime = (EditText) view.findViewById(R.id.databasetotime);
        toTime.setInputType(EditorInfo.TYPE_NULL);
        toTime.setText(timeFormat.format(new Date()));
        toTime.setOnClickListener(new TimePickerListener(getActivity(), toTime));

        builder.setPositiveButton(R.string.dialog_ok, null);

        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {

            }
        });

        final AlertDialog d = builder.create();

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
                        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

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

                        long count = Long.parseLong(DatabaseHelper.getStringResultSet("SELECT COUNT(*) FROM " + SQLTableName.PREFIX + DeviceID.get(SensorDataCollectorService.getInstance()) + SQLTableName.GPS + " WHERE attr_time > ? AND attr_time < ?", new String[]{ lFromTime + "", lToTime + "" }).get(0));

                        if(count > 1000) {
                            Toast.makeText(d.getContext(), getString(R.string.analyze_analyze_database_dialog_notify3), Toast.LENGTH_LONG).show();
                            return;
                        }

                        /*
                         * Handler mainHandler = new Handler(context.getMainLooper());
                         * final String _ft = ""+ lFromTime;
                         * final String _tt = ""+ lToTime;
                         * Runnable myRunnable = new Runnable(){
                         * @Override
                         * public void run() {
                         * context.showAnalyzeDatabaseGPS(_ft + "", _tt + "");
                         * }
                         * };
                         * mainHandler.post(myRunnable);
                         */

                        context.showAnalyzeDatabaseGPS(lFromTime + "", lToTime + "");
                        d.dismiss();
                    }
                });

            }
        });

        return d;
    }
}