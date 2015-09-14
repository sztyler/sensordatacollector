package de.unima.ar.collector.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.unima.ar.collector.R;

public class CorrectionHistoryAdapter extends ArrayAdapter<String[]>
{
    private Context context;


    public CorrectionHistoryAdapter(Context context, int resource, List<String[]> list)
    {
        super(context, resource, list);

        this.context = context;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View rowView = convertView;

        if(rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.activity_correction_historyentry, parent, false);
        }

        String[] entry = this.getItem((this.getCount() - 1) - position);

        TextView invalid = (TextView) rowView.findViewById(R.id.activity_correction_value);
        invalid.setText(context.getString(R.string.activity_correction_history_invalid) + ": " + entry[3]);

        Timestamp start = new Timestamp(Long.valueOf(entry[1]));
        Date startDate = new Date(start.getTime());

        Timestamp stop = new Timestamp(Long.valueOf(entry[2]));
        Date stopDate = new Date(stop.getTime());

        DateFormat formatter = new SimpleDateFormat();
        TextView dateText = (TextView) rowView.findViewById(R.id.activity_correction_date);
        dateText.setText(formatter.format(startDate) + " - " + formatter.format(stopDate));

        return rowView;
    }
}
