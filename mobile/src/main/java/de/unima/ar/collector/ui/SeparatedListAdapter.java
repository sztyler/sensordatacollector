package de.unima.ar.collector.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

import de.unima.ar.collector.R;


public class SeparatedListAdapter extends BaseAdapter
{
    private final Map<String, Adapter> sections = new LinkedHashMap<>();
    private final ArrayAdapter<String> headers;
    private final static int             TYPE_SECTION_HEADER = 0;
    private              DataSetObserver observer            = new DataSetObserver()
    {
        public void onChanged()
        {
            notifyDataSetChanged();
        }


        public void onInvalidated()
        {
            notifyDataSetInvalidated();
        }
    };

    private int maxPosition = 0;


    public SeparatedListAdapter(Context context, int maxPosition)
    {
        headers = new ArrayAdapter<>(context, R.layout.listheader);
        this.maxPosition = maxPosition;
    }


    public void addSection(String section, Adapter adapter)
    {
        this.headers.add(section);
        this.sections.put(section, adapter);
        adapter.registerDataSetObserver(observer);
    }


    public Object getItem(int position)
    {
        for(String section : this.sections.keySet()) {
            Adapter adapter = sections.get(section);
            int size = adapter.getCount() + 1;

            // check if position inside this section
            if(position == 0)
                return section;
            if(position < size)
                return adapter.getItem(position - 1);

            // otherwise jump into next section
            position -= size;
        }
        return null;
    }


    public int getCount()
    {
        // total together all sections, plus one for each section header
        int total = 0;
        for(Adapter adapter : this.sections.values())
            total += adapter.getCount() + 1;
        return total;
    }


    /*
     * (non-Javadoc)
     * @see android.widget.BaseAdapter#getViewTypeCount()
     */
    @Override
    public int getViewTypeCount()
    {
        // assume that headers count as one, then total all sections
        int total = 1;
        for(Adapter adapter : this.sections.values())
            total += adapter.getViewTypeCount();
        return total;
    }


    /*
     * (non-Javadoc)
     * @see android.widget.BaseAdapter#getItemViewType(int)
     */
    @Override
    public int getItemViewType(int position)
    {
        int type = 1;
        for(String section : this.sections.keySet()) {
            Adapter adapter = sections.get(section);
            int size = adapter.getCount() + 1;

            // check if position inside this section
            if(position == 0)
                return TYPE_SECTION_HEADER;
            if(position < size)
                return type + adapter.getItemViewType(position - 1);

            // otherwise jump into next section
            position -= size;
            type += adapter.getViewTypeCount();
        }
        return -1;
    }


    //    public boolean areAllItemsSelectable()
    //    {
    //        return false;
    //    }


    /*
     * (non-Javadoc)
     * @see android.widget.BaseAdapter#isEnabled(int)
     */
    @Override
    public boolean isEnabled(int position)
    {
        return (getItemViewType(position) != TYPE_SECTION_HEADER) && position >= maxPosition;
    }


    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        int sectionnum = 0;
        for(String section : this.sections.keySet()) {
            Adapter adapter = sections.get(section);
            int size = adapter.getCount() + 1;

            // check if position inside this section
            if(position == 0)
                return headers.getView(sectionnum, convertView, parent);
            if(position < size)
                return adapter.getView(position - 1, convertView, parent);

            // otherwise jump into next section
            position -= size;
            sectionnum++;
        }
        return null;
    }


    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position)
    {
        return position;
    }
}