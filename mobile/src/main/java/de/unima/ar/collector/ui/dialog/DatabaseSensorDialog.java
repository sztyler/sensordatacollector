package de.unima.ar.collector.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
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
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.ui.DatePickerListener;
import de.unima.ar.collector.ui.TimePickerListener;
import de.unima.ar.collector.util.PlotConfiguration;


public class DatabaseSensorDialog extends Dialog implements android.view.View.OnClickListener
{
    private Context           context;
    private PlotConfiguration pc;


    public DatabaseSensorDialog(Context context)
    {
        super(context, R.style.MyActivityDialogTheme);

        this.context = context;
    }


    public void setPlotConfig(PlotConfiguration pc)
    {
        this.pc = pc;
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.databasesensordialog);

        Button positive = (Button) findViewById(R.id.btn_yes);
        positive.setOnClickListener(this);

        Button negative = (Button) findViewById(R.id.btn_no);
        negative.setOnClickListener(this);

        setTitle(context.getString(R.string.analyze_analyze_database_dialog_title));

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

        EditText fromDate = (EditText) findViewById(R.id.databasefromdate);
        fromDate.setInputType(EditorInfo.TYPE_NULL);
        fromDate.setText(dateFormat.format(new Date(new Date().getTime() - 86400000)));
        fromDate.setOnClickListener(new DatePickerListener(this.context, fromDate));

        EditText fromTime = (EditText) findViewById(R.id.databasefromtime);
        fromTime.setInputType(EditorInfo.TYPE_NULL);
        fromTime.setText(timeFormat.format(new Date(new Date().getTime() - 86400000)));
        fromTime.setOnClickListener(new TimePickerListener(this.context, fromTime));

        EditText toDate = (EditText) findViewById(R.id.databasetodate);
        toDate.setInputType(EditorInfo.TYPE_NULL);
        toDate.setText(dateFormat.format(new Date(new Date().getTime() + 60000)));
        toDate.setOnClickListener(new DatePickerListener(this.context, toDate));

        EditText toTime = (EditText) findViewById(R.id.databasetotime);
        toTime.setInputType(EditorInfo.TYPE_NULL);
        toTime.setText(timeFormat.format(new Date(new Date().getTime() + 60000)));
        toTime.setOnClickListener(new TimePickerListener(this.context, toTime));

        CheckBox showActivities = (CheckBox) findViewById(R.id.showActivitiesChkBx);
        showActivities.setChecked(true);

        CheckBox showPositions = (CheckBox) findViewById(R.id.showPositionsChkBx);
        showPositions.setChecked(true);

        CheckBox showPostures = (CheckBox) findViewById(R.id.showPosturesChkBx);
        showPostures.setChecked(true);
    }


    @Override
    public void onClick(View v)
    {
        switch(v.getId()) {
            case R.id.btn_yes:
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH);

                String fromString = ((EditText) findViewById(R.id.databasefromdate)).getText().toString() + " " + ((EditText) findViewById(R.id.databasefromtime)).getText().toString();
                String toString = ((EditText) findViewById(R.id.databasetodate)).getText().toString() + " " + ((EditText) findViewById(R.id.databasetotime)).getText().toString();

                long lFromTime, lToTime;

                try {
                    lFromTime = sdf.parse(fromString).getTime();
                    lToTime = sdf.parse(toString).getTime();

                    if(lFromTime == lToTime || lFromTime > lToTime) {
                        Toast.makeText(context, context.getString(R.string.analyze_analyze_database_dialog_notify1), Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch(ParseException e1) {
                    Toast.makeText(context, context.getString(R.string.analyze_analyze_database_dialog_notify2), Toast.LENGTH_LONG).show();
                    return;
                }

                MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");
                main.showAnalyzeDatabaseData(pc, String.valueOf(lFromTime), String.valueOf(lToTime), ((CheckBox) findViewById(R.id.showActivitiesChkBx)).isChecked(), ((CheckBox) findViewById(R.id.showPositionsChkBx)).isChecked(), ((CheckBox) findViewById(R.id.showPosturesChkBx)).isChecked());

                dismiss();
                break;
            case R.id.btn_no:
                dismiss();
                break;
            default:
                break;
        }
        dismiss();
    }
}