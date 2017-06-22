package de.unima.ar.collector.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import de.unima.ar.collector.R;
import de.unima.ar.collector.SensorDataCollectorService;
import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.database.DatabaseExportSQL;
import de.unima.ar.collector.sensors.CustomCollector;
import de.unima.ar.collector.sensors.SensorCollector;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.ui.dialog.DatabaseDeleteDialog;
import de.unima.ar.collector.ui.dialog.DatabaseExportCSVDialog;

public class SettingActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // init view
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        ScrollView toolbarContainer = (ScrollView) View.inflate(this, R.layout.preferences, null);
        root.removeAllViews();
        root.addView(toolbarContainer);

        // init toolbar
        Toolbar mToolBar = (Toolbar) toolbarContainer.findViewById(R.id.pref_toolbar);
        mToolBar.setTitle(getTitle());
        mToolBar.setNavigationIcon(android.R.drawable.ic_menu_revert); //TODO
        mToolBar.setNavigationOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });

        // start transaction
        getFragmentManager().beginTransaction().replace(R.id.pref_content_frame, new Prefs1Fragment()).commit();
    }


    @Override
    public void onResume()
    {
        super.onResume();

        // register settings listener
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onPause()
    {
        super.onPause();

        // unregister settings listener
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        switch(key) {
            case "watch_collect":
                boolean oldState = Settings.WEARSENSOR;
                Settings.WEARSENSOR = sharedPreferences.getBoolean(key, true);
                BroadcastService.getInstance().sendMessage("/settings", "[WEARSENSOR, " + sharedPreferences.getBoolean(key, true) + "]");
                if(!oldState) {
                    Toast.makeText(this, R.string.preferences_watch_sensorenabled_hint, Toast.LENGTH_LONG).show();
                }
                break;
            case "watch_direct":
                Settings.WEARTRANSFERDIRECT = sharedPreferences.getBoolean(key, false);
                BroadcastService.getInstance().sendMessage("/settings", "[WEARTRANSFERDIRECT, " + sharedPreferences.getBoolean(key, false) + "]");
                break;
            case "sensor_lowpass":
                Settings.ACCLOWPASS = sharedPreferences.getBoolean(key, false);
                break;
            case "sensor_frequency":
                String sensor_frequency = sharedPreferences.getString(key, "50.0f");
                setDefaultFrequency(sensor_frequency);
                break;
            case "live_plotter":
                Settings.LIVE_PLOTTER_ENABLED = sharedPreferences.getBoolean(key, true);
                break;
            case "database_direct_insert":
                // check if sensors are enabled TODO -not implemented (UI)
                Settings.DATABASE_DIRECT_INSERT = sharedPreferences.getBoolean(key, true);
                break;
            case "database_delete":
                deleteDatabase();
                break;
            case "database_save":
                copyDatabase();
                break;
            case "database_csv":
                databaseToCSV();
                break;
            default:
        }
    }


    public static class Prefs1Fragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            // register listener
            CheckBoxPreference watchDirectBox = (CheckBoxPreference) findPreference("watch_direct");
            watchDirectBox.setOnPreferenceChangeListener(this);
        }


        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
            // TODO Check if this is the watch_direct preference

            for(SensorCollector entry : SensorDataCollectorService.getInstance().getSCM().getSensorCollectors().values()) {
                if(entry.isRegistered) {
                    Toast.makeText(getActivity(), R.string.preferences_watch_directenabled_error, Toast.LENGTH_LONG).show();
                    return false;
                }
            }

            for(CustomCollector entry : SensorDataCollectorService.getInstance().getSCM().getCustomCollectors().values()) {
                if(entry.isRegistered()) {
                    Toast.makeText(getActivity(), R.string.preferences_watch_directenabled_error, Toast.LENGTH_LONG).show();
                    return false;
                }
            }

            return true;
        }
    }


    private void setDefaultFrequency(String sensor_frequency)
    {
        try {
            double frequency = Double.parseDouble(sensor_frequency);
            if(frequency < 1 || frequency > 250) {
                throw new NumberFormatException();
            }

            Settings.SENSOR_DEFAULT_FREQUENCY = frequency;
        } catch(NumberFormatException e) {
            Toast.makeText(this, R.string.preferences_frequency_input_invalid, Toast.LENGTH_LONG).show();
        }

        Toast.makeText(this, R.string.preferences_frequency_input_valid, Toast.LENGTH_LONG).show();
    }


    private void deleteDatabase()
    {
        DatabaseDeleteDialog dialog = new DatabaseDeleteDialog();
        try {
            dialog.show(this.getFragmentManager(), "database_delete");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    private void copyDatabase()
    {
        DatabaseExportSQL task = new DatabaseExportSQL(this);
        task.execute();
    }


    private void databaseToCSV()
    {
        DatabaseExportCSVDialog dialog = new DatabaseExportCSVDialog();
        try {
            dialog.show(this.getFragmentManager(), "database_export_csv");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}