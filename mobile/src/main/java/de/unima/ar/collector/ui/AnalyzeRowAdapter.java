package de.unima.ar.collector.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import de.unima.ar.collector.R;
import de.unima.ar.collector.util.Triple;

/**
 * @author Timo Sztyler
 */
public class AnalyzeRowAdapter extends ArrayAdapter<Triple<String, String, String>>
{
    private Context context;


    public AnalyzeRowAdapter(Context context, int resource, List<Triple<String, String, String>> values)
    {
        super(context, resource, values);

        this.context = context;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;

        if(v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.listitemueberblick, parent, false);

        }

        Triple<String, String, String> entry = this.getItem(position);

        if(entry != null) {
            TextView title = (TextView) v.findViewById(R.id.list_item_ueberblick_title);
            title.setText(entry.getValue()[0].toString());

            TextView value = (TextView) v.findViewById(R.id.list_item_ueberblick_value);
            if(!entry.getValue()[1].toString().equals(context.getString(R.string.analyze_analyzelive_collecting))) {
                value.setText(context.getString(R.string.analyze_analyze_database_values) + ": " + entry.getValue()[1].toString());
            } else {
                value.setText(entry.getValue()[1].toString());
            }

            TextView subTitle = (TextView) v.findViewById(R.id.list_item_ueberblick_title_subtitle);
            subTitle.setText(context.getString(R.string.analyze_analyzelive_device) + ": " + entry.getValue()[2].toString());
        }

        return v;
    }
}
