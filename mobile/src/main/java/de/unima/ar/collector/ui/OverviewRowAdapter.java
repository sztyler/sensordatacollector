package de.unima.ar.collector.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Arrays;

import de.unima.ar.collector.R;
import de.unima.ar.collector.SensorDataCollectorService;
import de.unima.ar.collector.api.ListenerService;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.sensors.SensorCollectorManager;
import de.unima.ar.collector.util.SensorDataUtil;


/**
 * Anzeige des Hauptscreens
 *
 * @author Fabian Kramm, Timo Sztyler
 */
public class OverviewRowAdapter extends ArrayAdapter<String>
{
    private final Activity context;

    static class ViewHolder
    {
        public TextView text;
        public TextView text2;
        public TextView text3;
    }


    public OverviewRowAdapter(Activity context)
    {
        super(context, R.layout.listitemueberblick, new String[]{ "Sensors", "Power", "Database", "SmartWatches" });

        this.context = context;
    }


    @Override
    public boolean isEnabled(int position)
    {
        return false;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View rowView = convertView;

        if(rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.listitemueberblick, parent, false);
            ViewHolder viewHolder = new ViewHolder();

            viewHolder.text = (TextView) rowView.findViewById(R.id.list_item_ueberblick_title);
            viewHolder.text2 = (TextView) rowView.findViewById(R.id.list_item_ueberblick_title_subtitle);
            viewHolder.text3 = (TextView) rowView.findViewById(R.id.list_item_ueberblick_value);

            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();
        SensorCollectorManager scm = SensorDataCollectorService.getInstance().getSCM();

        if(position == 0) {
            holder.text.setText(context.getString(R.string.main_overview_sensors));
            holder.text2.setText(context.getString(R.string.main_overview_sensors_description));
            holder.text3.setText(String.valueOf(scm.getRunningSensorAmount()));
        } else if(position == 1) {
            String[] names = SensorDataUtil.getNamesOfEnabledSensors(1);
            String extDesc = "";
            if(names.length > 0) {
                extDesc = Arrays.toString(names);
                extDesc = extDesc.substring(1, extDesc.length() - 1).trim();
                extDesc = System.getProperty("line.separator") + "(" + context.getString(R.string.main_overview_power_description2) + ": " + extDesc + ")";
            }

            holder.text.setText(context.getString(R.string.main_overview_power));
            holder.text2.setText(context.getString(R.string.main_overview_power_description) + extDesc);
            holder.text3.setText(scm.getPowerUsed() + " mA");
        } else if(position == 2) {
            holder.text.setText(context.getString(R.string.main_overview_database));
            holder.text2.setText(context.getString(R.string.main_overview_database_description));
            holder.text3.setText((((int) ((SQLDBController.getInstance().getSize() / 1024) * 10)) / 10) + " KB");
        } else if(position == 3) {
            holder.text.setText(context.getString(R.string.main_overview_smartwatch));
            holder.text2.setText(context.getString(R.string.main_overview_smartwatch_description));
            holder.text3.setText(String.valueOf(ListenerService.getNumberOfDevices()));
        }

        return rowView;
    }
}