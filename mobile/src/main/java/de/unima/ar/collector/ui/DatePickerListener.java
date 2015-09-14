package de.unima.ar.collector.ui;

import java.util.Calendar;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;


/**
 * @author Fabian Kramm
 *
 */
public class DatePickerListener implements View.OnClickListener
{
    private EditText                                 edittext;
    private Context                                  context;
    private final DatePickerDialog.OnDateSetListener datePickerListener = new DatePickerDialog.OnDateSetListener() {

                                                                            /*
                                                                             * (non-Javadoc)
                                                                             * @see android.app.DatePickerDialog.OnDateSetListener#onDateSet(android.widget.DatePicker, int, int, int)
                                                                             */
                                                                            @Override
                                                                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
                                                                            {
                                                                                String[] date = new String[3];

                                                                                monthOfYear = monthOfYear + 1;

                                                                                date[0] = (dayOfMonth < 10) ? "0" + dayOfMonth : "" + dayOfMonth;
                                                                                date[1] = (monthOfYear < 10) ? "0" + monthOfYear : "" + monthOfYear;
                                                                                date[2] = (year < 10) ? "0" + year : "" + year;

                                                                                edittext.setText(date[0] + "." + date[1] + "." + date[2]);
                                                                            }

                                                                        };


    public DatePickerListener(Context context, EditText edittext)
    {
        this.context = context;
        this.edittext = edittext;
    }


    public EditText getEditText()
    {
        return edittext;
    }


    /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v)
    {
        if(edittext == null) { return; }

        String[] date;

        if(edittext.getText().toString().equals("")) {
            Calendar c = Calendar.getInstance();
            date = new String[3];
            date[0] = (c.get(Calendar.DAY_OF_MONTH) < 10) ? "0" + c.get(Calendar.DAY_OF_MONTH) : "" + c.get(Calendar.DAY_OF_MONTH);

            int month = c.get(Calendar.MONTH) + 1;
            date[1] = (month < 10) ? "0" + month : "" + month;
            date[2] = (c.get(Calendar.YEAR) < 10) ? "0" + c.get(Calendar.YEAR) : "" + c.get(Calendar.YEAR);
        } else {
            date = edittext.getText().toString().split("\\.");
        }

        DatePickerDialog dialog = new DatePickerDialog(context, datePickerListener, Integer.parseInt(date[2]), Integer.parseInt(date[1]) - 1, Integer.parseInt(date[0]));

        dialog.show();
    }
}