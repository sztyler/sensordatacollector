package de.unima.ar.collector.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.SensorDataCollectorService;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.sensors.CollectorConstants;
import de.unima.ar.collector.sensors.CustomCollector;
import de.unima.ar.collector.sensors.SensorCollector;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.util.DBUtils;
import de.unima.ar.collector.util.SensorDataUtil;
import de.unima.ar.collector.util.StringUtils;


/**
 * @author Fabian Kramm
 */
public class SensorenRowAdapter extends ArrayAdapter<String>
{
    private final Activity            context;
    private       Map<String, Sensor> sensors;


    public SensorenRowAdapter(Activity context)
    {
        super(context, 0, new ArrayList<String>());

        this.context = context;
        this.sensors = new TreeMap<>();

        // Add all sensors
        SensorManager mSensorManager = (SensorManager) this.context.getSystemService(Activity.SENSOR_SERVICE);
        List<Sensor> allSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        for(Sensor sensor : allSensors) {
            sensors.put(StringUtils.formatSensorName(SensorDataUtil.getSensorType(sensor.getType())), sensor);
        }

        for(int i : CollectorConstants.activatedCustomCollectors) {
            sensors.put(StringUtils.formatSensorName(SensorDataUtil.getSensorType(i)), null);
        }

        this.addAll(sensors.keySet());
    }


    static class ViewHolder
    {
        public TextView text1;
        public TextView text2;
        public Button   btn;
        public Button   selfTest;
        public CheckBox checkBx;
    }


    /*
     * (non-Javadoc)
     * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View rowView = convertView;

        if(rowView == null) {   // TODO - Irgendwas ist in diesem Abschnitt langsam!!!
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.listitemsensoren, parent, false);

            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.text1 = (TextView) rowView.findViewById(R.id.sensortextview1);
            viewHolder.text2 = (TextView) rowView.findViewById(R.id.sensortextview2);
            viewHolder.btn = (Button) rowView.findViewById(R.id.sensorfreqbutton);
            viewHolder.selfTest = (Button) rowView.findViewById(R.id.sensorselftest);
            viewHolder.checkBx = (CheckBox) rowView.findViewById(R.id.sensorcheckBox1);

            viewHolder.checkBx.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    viewHolder.checkBx.setChecked(!viewHolder.checkBx.isChecked());

                    final ListView lv = (ListView) context.findViewById(R.id.mainlist);

                    int pos = lv.getPositionForView((RelativeLayout) viewHolder.checkBx.getParent());
                    int id = ((RelativeLayout) viewHolder.checkBx.getParent()).getId();

                    lv.performItemClick((RelativeLayout) viewHolder.checkBx.getParent(), pos, id);
                }
            });

            final int senPos = position;
            viewHolder.selfTest.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(!(context instanceof MainActivity)) {
                        return;
                    }

                    try {
                        List<String> names = new ArrayList<String>(sensors.keySet());
                        Sensor s = sensors.get(names.get(senPos));

                        if(s == null) {
                            throw new Exception();
                        }

                        if(SensorDataCollectorService.getInstance().getSCM().getEnabledCollectors().contains(s.getType())) {
                            Toast.makeText(context, context.getString(R.string.sensor_selftest_notify1), Toast.LENGTH_LONG).show();
                            return;
                        }

                        MainActivity ma = (MainActivity) context;
                        ma.showSensorSelftest(s);
                    } catch(Exception e) {
                        Toast.makeText(context, context.getString(R.string.sensor_selftest_notify2), Toast.LENGTH_LONG).show();
                    }
                }
            });

            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();

        List<String> names = new ArrayList<String>(sensors.keySet());
        final Sensor sensor = sensors.get(names.get(position));

        // First there are the custom collectors then come the sensor collectors
        if(sensor == null) {
            String nameID = names.get(position).toUpperCase(Locale.ENGLISH).replace(" ", "_");
            final int type = SensorDataUtil.getSensorTypeInt("TYPE_" + nameID);

            holder.text1.setText(names.get(position));
            holder.text2.setText(nameID);

            // Add button click listener
            holder.btn.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    // Check if sensor running
                    if(SensorDataCollectorService.getInstance().getSCM().getEnabledCollectors().contains(type)) {
                        Toast.makeText(context, context.getString(R.string.sensor_frequency_notify), Toast.LENGTH_LONG).show();
                        return;
                    }

                    AlertDialog.Builder alert = new AlertDialog.Builder(context);

                    alert.setTitle(context.getString(R.string.sensor_frequency_dialog_title));
                    alert.setMessage(context.getString(R.string.sensor_frequency_dialog_text));

                    // Set an EditText view to get user input
                    final EditText input = new EditText(context);

                    // Query and get the entry if there is any for the sensor id
                    String queryString = "SELECT freq FROM " + SQLTableName.SENSOROPTIONS + " WHERE sensor = ? ";
                    String[] queryArgs = new String[]{ String.valueOf(type) };
                    List<String[]> result = SQLDBController.getInstance().query(queryString, queryArgs, false);

                    double frequency = (result.size() != 0) ? Double.valueOf(result.get(0)[0]) : Settings.SENSOR_DEFAULT_FREQUENCY;    // default value: 50ms - 1000ms/50ms = 20hz
                    input.setText(String.format(Locale.ENGLISH, "%.2f", frequency));  // 1000ms/20hz=50

                    alert.setView(input);

                    alert.setPositiveButton(context.getString(R.string.sensor_frequency_dialog_button_ok), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                            try {
                                double frequencyNew = Double.parseDouble(input.getText().toString());
                                DBUtils.updateSensorStatus(type, (int) frequencyNew, 0);

                                CustomCollector sc = SensorDataCollectorService.getInstance().getSCM().getCustomCollectors().get(type);
                                sc.setSensorRate((int) frequencyNew);

                                String hertz = String.format(Locale.ENGLISH, "%.2f", (frequencyNew));
                                Toast.makeText(context, context.getString(R.string.sensor_frequency_changed_success) + " (" + hertz + "Hz)", Toast.LENGTH_LONG).show();
                            } catch(NumberFormatException e) {
                                Toast.makeText(context, context.getString(R.string.sensor_frequency_changed_failed), Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                    alert.setNegativeButton(context.getString(R.string.sensor_frequency_dialog_button_cancel), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                        }
                    });

                    alert.show();
                }
            });
            SensorDataCollectorService service = SensorDataCollectorService.getInstance();
            holder.checkBx.setChecked(service.getSCM().getEnabledCollectors().contains(type));
        }
        // sensor collectors
        else {
            // position = position - CollectorConstants.activatedCustomCollectors.length;

            // Skip TYPE_
            String s = SensorDataUtil.getSensorType(sensor.getType());
            holder.text1.setText(StringUtils.formatSensorName(s));
            holder.text2.setText(sensor.getName());

            // Add button click listener
            holder.btn.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    // Check if sensor running
                    if(SensorDataCollectorService.getInstance().getSCM().getEnabledCollectors().contains(sensor.getType())) {
                        Toast.makeText(context, context.getString(R.string.sensor_frequency_notify), Toast.LENGTH_LONG).show();
                        return;
                    }

                    AlertDialog.Builder alert = new AlertDialog.Builder(context);

                    alert.setTitle(context.getString(R.string.sensor_frequency_dialog_title));
                    alert.setMessage(context.getString(R.string.sensor_frequency_dialog_text));

                    // Set an EditText view to get user input
                    final EditText input = new EditText(context);

                    // Query and get the entry if there is any for the sensor id
                    String queryString = "SELECT freq FROM " + SQLTableName.SENSOROPTIONS + " WHERE sensor = ? ";
                    String[] queryArgs = new String[]{ String.valueOf(sensor.getType()) };
                    List<String[]> result = SQLDBController.getInstance().query(queryString, queryArgs, false);

                    double frequency = (result.size() != 0) ? Double.valueOf(result.get(0)[0]) : Settings.SENSOR_DEFAULT_FREQUENCY;

                    input.setText(String.format(Locale.ENGLISH, "%.2f", frequency));
                    alert.setView(input);

                    alert.setPositiveButton(context.getString(R.string.sensor_frequency_dialog_button_ok), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                            try {
                                double frequencyNew = Double.parseDouble(input.getText().toString());
                                DBUtils.updateSensorStatus(sensor.getType(), (int) frequencyNew, 0);

                                SensorCollector sc = SensorDataCollectorService.getInstance().getSCM().getSensorCollectors().get(sensor.getType());
                                sc.setSensorRate(frequencyNew);

                                String hertz = String.format(Locale.ENGLISH, "%.2f", (frequencyNew));
                                Toast.makeText(context, context.getString(R.string.sensor_frequency_changed_success) + " (" + hertz + "Hz)", Toast.LENGTH_LONG).show();
                            } catch(NumberFormatException e) {
                                Toast.makeText(context, context.getString(R.string.sensor_frequency_changed_failed), Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                    alert.setNegativeButton(context.getString(R.string.sensor_frequency_dialog_button_cancel), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                        }
                    });

                    alert.show();
                }
            });

            SensorDataCollectorService service = SensorDataCollectorService.getInstance();
            holder.checkBx.setChecked(service.getSCM().getEnabledCollectors().contains(sensors.get(names.get(position)).getType()));
        }

        return rowView;
    }
}