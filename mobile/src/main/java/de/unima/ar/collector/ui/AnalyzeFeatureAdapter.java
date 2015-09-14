package de.unima.ar.collector.ui;

import android.content.Context;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.unima.ar.collector.R;
import de.unima.ar.collector.util.StringUtils;

public class AnalyzeFeatureAdapter extends BaseAdapter
{
    private Context context;
    List<String>   titles;
    List<String[]> values;


    public AnalyzeFeatureAdapter(Context context)
    {
        this.context = context;

        this.titles = new ArrayList<>();
        this.values = new ArrayList<>();
    }


    public void addFeature(String title, String[] values)
    {
        int index = this.titles.indexOf(title);
        if(index > -1) {
            this.values.set(index, values);
            return;
        }

        this.titles.add(title);
        this.values.add(values);
    }


    @Override
    public int getCount()
    {
        return titles.size();
    }


    @Override
    public Object getItem(int position)
    {
        return titles.get(position);
    }


    @Override
    public long getItemId(int position)
    {
        return titles.get(position).hashCode();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View rowView = convertView;

        if(rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.analyze_features_element, parent, false);
        }

        TextView ph1 = (TextView) rowView.findViewById(R.id.feature_placeholder1);
        ((LayerDrawable) ph1.getBackground()).setLayerInset(1, 1, 1, 0, 0);
        ph1.setText(titles.get(position)); // invisible

        TextView title = (TextView) rowView.findViewById(R.id.feature_title);
        ((LayerDrawable) title.getBackground()).setLayerInset(1, 1, 0, 0, 0);
        title.setText(titles.get(position));

        TextView ph2 = (TextView) rowView.findViewById(R.id.feature_placeholder2);
        ((LayerDrawable) ph2.getBackground()).setLayerInset(1, 1, 0, 0, 1);
        ph2.setText(titles.get(position)); // invisible

        String x = StringUtils.relatedLabel(titles.get(position), "x") + " = ";
        String y = StringUtils.relatedLabel(titles.get(position), "y") + " = ";
        String z = StringUtils.relatedLabel(titles.get(position), "z") + " = ";

        TextView valueX = (TextView) rowView.findViewById(R.id.feature_value_x);
        ((LayerDrawable) valueX.getBackground()).setLayerInset(1, 0, 1, 1, 0);
        valueX.setText(x + values.get(position)[0]);

        TextView valueY = (TextView) rowView.findViewById(R.id.feature_value_y);
        ((LayerDrawable) valueY.getBackground()).setLayerInset(1, 0, 0, 1, 0);
        valueY.setText(y + values.get(position)[1]);

        TextView valueZ = (TextView) rowView.findViewById(R.id.feature_value_z);
        ((LayerDrawable) valueZ.getBackground()).setLayerInset(1, 0, 0, 1, 1);
        valueZ.setText(z + values.get(position)[2]);

        return rowView;
    }
}