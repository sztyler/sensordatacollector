package de.unima.ar.collector.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import de.unima.ar.collector.R;

public class TwoColumnAdapter extends BaseAdapter
{
    private Context      context;
    private List<String> leftValues;
    private List<String> rightValues;
    private Typeface     symbol;


    public TwoColumnAdapter(Context context, List<String> leftValues, List<String> rightValues)
    {
        super();

        this.context = context;
        this.leftValues = leftValues;
        this.rightValues = rightValues;

        this.symbol = Typeface.createFromAsset(context.getAssets(), "fonts/Symbola613.ttf");
    }


    @Override
    public int getCount()
    {
        return leftValues.size();
    }


    @Override
    public Object getItem(int position)
    {
        return leftValues.get(position);
    }


    @Override
    public long getItemId(int position)
    {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.twocolumn, parent, false);
        }

        TextView left = (TextView) convertView.findViewById(R.id.left);
        left.setText(leftValues.get(position));

        TextView right = (TextView) convertView.findViewById(R.id.right);
        right.setTypeface(symbol);
        right.setText(rightValues.get(position));

        return convertView;
    }
}