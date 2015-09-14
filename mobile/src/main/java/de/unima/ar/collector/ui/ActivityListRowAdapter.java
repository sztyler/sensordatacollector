package de.unima.ar.collector.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.unima.ar.collector.R;
import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableName;


/**
 * Adapter f�r die ActivityListRow
 *
 * @author Fabian Kramm
 */
public class ActivityListRowAdapter extends ArrayAdapter<String>
{
    private Activity context;
    private Typeface symbol;

    static class Holder
    {
        public int    id;
        public String name;
    }


    public ActivityListRowAdapter(Activity context, ArrayList<String> list)
    {
        super(context, R.layout.activitylistitem, list);
        this.context = context;

        reinitialize();

        this.symbol = Typeface.createFromAsset(context.getAssets(), "fonts/Symbola613.ttf");
    }


    static class ViewHolder
    {
        public TextView text1;
        public Button   btn;
    }


    /*
     * (non-Javadoc)
     * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View rowView = convertView;

        if(rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.activitylistitem, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.text1 = (TextView) rowView.findViewById(R.id.activity_list_item_textview);
            viewHolder.btn = (Button) rowView.findViewById(R.id.activity_list_item_delete);
            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();

        holder.text1.setText(getItem(position));

        if(position == 0) {
            holder.btn.setVisibility(View.GONE);
        } else {
            holder.btn.setVisibility(View.VISIBLE);
            holder.btn.setTypeface(symbol);
            holder.btn.setText("\u2718");

            final ActivityListRowAdapter that = this;
            final int pos = position;
            final String ItemText = getItem(position);

            holder.btn.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    // Set activity to completed in database
                    String Activity;
                    String SubActivity = null;

                    if(ItemText.indexOf('-') == -1) {
                        Activity = ItemText;
                    } else {
                        Activity = ItemText.substring(0, ItemText.indexOf(" -"));
                        SubActivity = ItemText.substring(ItemText.indexOf("- ") + 2);
                    }

                    int aId = Integer.parseInt(DatabaseHelper.getStringResultSet("SELECT id FROM " + SQLTableName.ACTIVITIES + " WHERE name = ? ", new String[]{ Activity }).get(0));
                    int sId = -1;

                    if(SubActivity != null) {
                        sId = Integer.parseInt(DatabaseHelper.getStringResultSet("SELECT id FROM " + SQLTableName.SUBACTIVITIES + " WHERE name = ? AND activityid = ? ", new String[]{ SubActivity, aId + "" }).get(0));
                    }

                    ContentValues args = new ContentValues();
                    args.put("endtime", System.currentTimeMillis());

                    if(sId != -1) {
                        SQLDBController.getInstance().update(SQLTableName.ACTIVITYDATA, args, "activityid = ? AND subactivityid = ? AND endtime = 0", new String[]{ String.valueOf(aId), String.valueOf(sId) });
                    } else {
                        SQLDBController.getInstance().update(SQLTableName.ACTIVITYDATA, args, "activityid = ? AND endtime = 0", new String[]{ String.valueOf(aId) });
                    }

                    // Remove from UI
                    that.remove(that.getItem(pos));
                    that.notifyDataSetChanged();
                    BroadcastService.getInstance().sendMessage("/database/response/deleteActivity", ItemText);
                }
            });
        }

        return rowView;
    }


    public void reinitialize()
    {
        // Add the running activities
        String statement = "SELECT a.name, s.name FROM " + SQLTableName.ACTIVITYDATA + " as d," + SQLTableName.ACTIVITIES + " as a LEFT JOIN " + SQLTableName.SUBACTIVITIES + " as s ON s.activityid = a.id WHERE d.activityid = a.id AND d.endtime = 0 AND (d.subactivityid IS NULL OR d.subactivityid = s.id)";
        List<String[]> result = SQLDBController.getInstance().query(statement, null, false);

        for(String[] row : result) {
            String activityString = row[0];

            if(row[1] != null) {
                activityString += Settings.ACTIVITY_DELIMITER + row[1];
            }

            add(activityString);
        }
    }
}