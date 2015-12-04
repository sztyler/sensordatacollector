package de.unima.ar.collector.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import de.unima.ar.collector.R;

public class DialogActivityListAdapter extends ArrayAdapter<String>
{
    private Context context;


    public DialogActivityListAdapter(Context context, int resource, List<String> objects)
    {
        super(context, resource, objects);

        this.context = context;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View rowView = convertView;

        if(rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.dialog_entry_activity, parent, false);
        }

        TextView label = (TextView) rowView.findViewById(R.id.dialog_entry_activity);
        label.setText(this.getItem(position));
        if(position == 0) {
            label.setTypeface(null, Typeface.BOLD | Typeface.ITALIC);
            label.setBackground(ContextCompat.getDrawable(context, R.drawable.border));
        }

        return rowView;
    }
}
