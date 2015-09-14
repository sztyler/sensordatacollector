package de.unima.ar.collector.adapter;

import android.content.Context;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.unima.ar.collector.R;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.ui.elements.ItemListView;

public class ItemListViewAdapter extends WearableListView.Adapter
{
    private Context      context;
    private String       parent;
    private List<String> elements;
    private boolean      circle;

    private List<String> selectedElements;


    public ItemListViewAdapter(Context context, boolean circle)
    {
        this.context = context;
        this.elements = new ArrayList<>();
        this.selectedElements = null;
        this.parent = null;
        this.circle = circle;
    }


    public void update(String parent, List<String> elements)
    {
        this.parent = parent;
        this.elements = elements;
    }


    public void setSelectedElements(List<String> selectedElements)
    {
        this.selectedElements = selectedElements;
    }


    public String get(int pos)
    {
        if(this.getItemCount() < pos) {
            return null;
        }

        return this.elements.get(pos);
    }


    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i)
    {
        return new WearableListView.ViewHolder(new ItemListView(this.context, this.circle));
    }


    @Override
    public void onBindViewHolder(WearableListView.ViewHolder viewHolder, int i)
    {
        String currentValue = this.elements.get(i);
        ItemListView itemView = (ItemListView) viewHolder.itemView;

        TextView txtView = (TextView) itemView.findViewById(R.id.text);
        txtView.setText(this.elements.get(i));

        CircledImageView imgView = (CircledImageView) itemView.findViewById(R.id.image);

        if(this.selectedElements == null) {
            imgView.setImageResource(android.R.color.transparent);
            return;
        }

        for(String activ : selectedElements) {  // mark activity and subactivity if subactivity is activ
            if(activ.equals(currentValue) || activ.startsWith(currentValue + Settings.ACTIVITY_DELIMITER) || (this.parent != null && activ.equals(this.parent + Settings.ACTIVITY_DELIMITER + currentValue))) {
                imgView.setImageResource(R.drawable.yes);
                return;
            }
        }

        imgView.setImageResource(android.R.color.transparent);
    }


    @Override
    public int getItemCount()
    {
        return this.elements.size();
    }
}