package de.unima.ar.collector.ui;

import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.TimePicker;

import java.util.Calendar;


/**
 * @author Fabian Kramm
 */
public class TimePickerListener implements View.OnClickListener
{
    private EditText edittext;
    private Context  context;
    private final TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener()
    {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minutes)
        {
            String[] time = new String[2];

            time[0] = (hourOfDay < 10) ? "0" + hourOfDay : "" + hourOfDay;
            time[1] = (minutes < 10) ? "0" + minutes : "" + minutes;

            edittext.setText(time[0] + ":" + time[1]);
        }
    };


    public TimePickerListener(Context context, EditText edittext)
    {
        this.context = context;
        this.edittext = edittext;
    }


    //    public EditText getEditText()
    //    {
    //        return edittext;
    //    }

    
    @Override
    public void onClick(View v)
    {
        if(edittext == null) {
            return;
        }

        String[] time;

        if(edittext.getText().toString().equals("")) {
            Calendar c = Calendar.getInstance();
            time = new String[2];
            time[0] = (c.get(Calendar.HOUR_OF_DAY) < 10) ? "0" + c.get(Calendar.HOUR_OF_DAY) : "" + c.get(Calendar.HOUR_OF_DAY);
            time[1] = (c.get(Calendar.MINUTE) < 10) ? "0" + c.get(Calendar.MINUTE) : "" + c.get(Calendar.MINUTE);
        } else {
            time = edittext.getText().toString().split(":");
        }

        TimePickerDialog dialog = new TimePickerDialog(context, timePickerListener, Integer.parseInt(time[0]), Integer.parseInt(time[1]), true);

        dialog.show();
    }
}