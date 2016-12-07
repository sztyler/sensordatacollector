package de.unima.ar.collector;

/**
 * University Mannheim
 * Last Modified : 19.02.2015
 * Author : Fabian Kramm, Timo Sztyler
 */

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeriesFormatter;
import com.androidplot.xy.XYStepMode;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.SupportMapFragment;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.api.ListenerService;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.controller.AdapterController;
import de.unima.ar.collector.controller.BluetoothController;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.extended.Plotter;
import de.unima.ar.collector.extended.SensorSelfTest;
import de.unima.ar.collector.sensors.CustomCollector;
import de.unima.ar.collector.sensors.GPSCollector;
import de.unima.ar.collector.sensors.SensorCollector;
import de.unima.ar.collector.sensors.SensorCollectorManager;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.DeviceID;
import de.unima.ar.collector.shared.util.Utils;
import de.unima.ar.collector.ui.ActivityListRowAdapter;
import de.unima.ar.collector.ui.ActivityOnItemClickListener;
import de.unima.ar.collector.ui.AnalyzeRowAdapter;
import de.unima.ar.collector.ui.CorrectionHistoryAdapter;
import de.unima.ar.collector.ui.DataPlotZoomListener;
import de.unima.ar.collector.ui.DrawPointsMap;
import de.unima.ar.collector.ui.OverviewRowAdapter;
import de.unima.ar.collector.ui.SensorenRowAdapter;
import de.unima.ar.collector.ui.SeparatedListAdapter;
import de.unima.ar.collector.ui.SettingActivity;
import de.unima.ar.collector.ui.dialog.CreateCorrectionDialog;
import de.unima.ar.collector.ui.dialog.DatabaseSensorDialog;
import de.unima.ar.collector.ui.dialog.GPSDatabaseDialog;
import de.unima.ar.collector.util.CustomPointFormatter;
import de.unima.ar.collector.util.DBUtils;
import de.unima.ar.collector.util.DateFormat;
import de.unima.ar.collector.util.PlotConfiguration;
import de.unima.ar.collector.util.SensorDataUtil;
import de.unima.ar.collector.util.StringUtils;
import de.unima.ar.collector.util.Triple;

public class MainActivity extends AppCompatActivity
{
    public enum Screens
    {
        SENSOREN, ANALYZE, ANALYZE_LIVE, ANALYZE_DATABASE, OPTIONS, SENSOREN_DETAILS, ACTIVITIES, SENSOREN_SELFTEST, ADDITIONAL_DEVICES, ACTIVITY_CORRECTION
    }

    private class ScreenInfo
    {
        public Screens screen;
        public Object  data[];
    }

    private final static ArrayList<ScreenInfo> lastScreens = new ArrayList<>();

    private View GPSView;

    private SensorSelfTest lastSensorSelfTest = null;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // set default values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // restore settings
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Settings.WEARSENSOR = pref.getBoolean("watch_collect", true);
        Settings.WEARTRANSFERDIRECT = pref.getBoolean("watch_direct", false);
        Settings.ACCLOWPASS = pref.getBoolean("sensor_lowpass", false);
        Settings.SENSOR_DEFAULT_FREQUENCY = Double.parseDouble(pref.getString("sensor_frequency", "50.0f"));
        Settings.LIVE_PLOTTER_ENABLED = pref.getBoolean("live_plotter", true);

        // register
        String deviceID = DeviceID.get(this);

        // bluetooth observation
        if(BluetoothAdapter.getDefaultAdapter() != null) {
            ListenerService.addDevice(deviceID, BluetoothAdapter.getDefaultAdapter().getAddress());
            BluetoothController.getInstance().register(this);
        } else {
            ListenerService.addDevice(deviceID, "");
        }

        // register
        ActivityController.getInstance().add("MainActivity", this);

        // start and inform
        startService(new Intent(MainActivity.this, SensorDataCollectorService.class));

        // start wearable app
        BroadcastService.initInstance(this);
        BroadcastService.getInstance().getAPIClient().connect();
        BroadcastService.getInstance().sendMessage("/activity/start", "");

        // start handheld app
        showSensoren();

        // style
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setIcon(R.drawable.ic_launcher);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        return super.onPrepareOptionsMenu(menu);
    }


    private ScreenInfo getCurrentScreen()
    {
        return lastScreens.get(lastScreens.size() - 1);
    }


    private void addScreen(Screens screen)
    {
        addScreen(screen, new Object[]{});
    }


    /**
     * @param screen der hinzugefügt werden soll
     * @param data   zusätzliche Daten zum Screen
     */
    private void addScreen(Screens screen, Object[] data)
    {
        ScreenInfo si = new ScreenInfo();

        si.screen = screen;
        si.data = data;

        // disable sensors if selftest was active
        if(lastScreens.size() > 0 && lastScreens.get(lastScreens.size() - 1).screen == Screens.SENSOREN_SELFTEST) {
            this.lastSensorSelfTest.stopTest();
        }

        // disable live plotter if it was active
        if(lastScreens.size() > 0 && lastScreens.get(lastScreens.size() - 1).screen == Screens.ANALYZE_LIVE) {
            ((Plotter) lastScreens.get(lastScreens.size() - 1).data[2]).setPlotting(false);
        }

        // Maximal die letzten 10 Screens erinnern
        if(lastScreens.size() >= 9) {
            lastScreens.remove(0); // Lösche ersten Eintrag
        }

        lastScreens.add(si);
    }


    @Override
    public void onBackPressed()
    {
        if(lastScreens.size() <= 1) {
            return;
        }

        // disable sensors if selftest was active
        if(lastScreens.get(lastScreens.size() - 1).screen == Screens.SENSOREN_SELFTEST) {
            this.lastSensorSelfTest.stopTest();
        }

        // disable live plotter if it was active
        if(lastScreens.get(lastScreens.size() - 1).screen == Screens.ANALYZE_LIVE) {
            ((Plotter) lastScreens.get(lastScreens.size() - 1).data[2]).setPlotting(false);
        }

        ScreenInfo si = lastScreens.get(lastScreens.size() - 2);

        // Aktuellen Screen und alten löschen
        lastScreens.remove(lastScreens.size() - 1);
        lastScreens.remove(lastScreens.size() - 1);

        switch(si.screen) {
            case SENSOREN:
                showSensoren();
                return;
            case SENSOREN_DETAILS:
                showSensorenDetail();
                return;
            case ANALYZE:
                showAnalyze();
                return;
            case ANALYZE_LIVE:
                showAnalyzeLive(String.valueOf(si.data[0].toString()), Integer.valueOf(si.data[1].toString()));
                return;
            case ANALYZE_DATABASE:
                showAnalyzeDatabase();
                return;
            case OPTIONS:
                showOptions();
                return;
            case ACTIVITIES:
                showActivities();
                return;
            case SENSOREN_SELFTEST:
                showSensorSelftest(null);
                return;
            case ADDITIONAL_DEVICES:
                showAdditionalDevices();
                return;
            case ACTIVITY_CORRECTION:
                showActivityCorrection();
                return;
            default:
                showSensoren();
        }
    }


    /**
     * Zeigt Liste mit anzeigbaren Sensorcollectoren an
     */
    public void showAnalyzeDatabase()
    {
        addScreen(Screens.ANALYZE_DATABASE);
        setContentView(R.layout.activity_main);

        Set<PlotConfiguration> configurations = new HashSet<>();
        Collection<SensorCollector> sCollectors = SensorDataCollectorService.getInstance().getSCM().getSensorCollectors().values();
        Collection<CustomCollector> cCollectors = SensorDataCollectorService.getInstance().getSCM().getCustomCollectors().values();
        List<String> devices = DatabaseHelper.getStringResultSet("SELECT device FROM " + SQLTableName.DEVICES, null);

        // load configurations
        for(String device : devices) {
            for(SensorCollector sCollector : sCollectors) {
                Plotter plotter = sCollector.getPlotter(device);
                if(plotter != null) {
                    configurations.add(plotter.getPlotConfiguration());
                }
            }

            for(CustomCollector cCollector : cCollectors) {
                Plotter plotter = cCollector.getPlotter(device);
                if(plotter != null) {
                    configurations.add(plotter.getPlotConfiguration());
                }
            }
        }

        // build query
        final Map<String, PlotConfiguration> showConfigs = new HashMap<>();
        StringBuilder query = new StringBuilder();
        for(PlotConfiguration configuration : configurations) {
            query.append("SELECT * FROM (SELECT '").append(configuration.sensorName).append("' AS Name, '").append(configuration.deviceID).append("' AS Device, id AS Count FROM ").append(SQLTableName.PREFIX).append(configuration.deviceID).append(configuration.tableName).append(" ORDER BY id DESC LIMIT 1) UNION ");
            showConfigs.put(configuration.sensorName + configuration.deviceID, configuration);
        }

        // execute query
        List<Triple<String, String, String>> values = new ArrayList<>();
        if(query.length() > 0) {
            List<String[]> result = SQLDBController.getInstance().query(query.substring(0, query.length() - 7), null, false);
            for(String[] entry : result) {
                if(entry[2] == null) {
                    continue;
                }

                Triple<String, String, String> value = new Triple<>(entry[0], entry[2], entry[1]);
                values.add(value);
            }
        }

        SeparatedListAdapter sadapter = new SeparatedListAdapter(this, 0);
        Collections.sort(values);
        AnalyzeRowAdapter analyze = new AnalyzeRowAdapter(this, R.layout.listitemueberblick, values);
        sadapter.addSection(getString(R.string.analyze_analyze), analyze);

        final ListView lv = (ListView) findViewById(R.id.mainlist);
        lv.setAdapter(sadapter);
        lv.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener()
        {

            @SuppressLint( "InflateParams" )
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if(!(view instanceof RelativeLayout)) {
                    return;
                }

                RelativeLayout layout = (RelativeLayout) view;
                TextView textView = (TextView) layout.findViewById(R.id.list_item_ueberblick_title);
                String sensorName = textView.getText().toString();
                String deviceID = ((TextView) layout.findViewById(R.id.list_item_ueberblick_title_subtitle)).getText().toString();
                deviceID = deviceID.substring(deviceID.indexOf(" ") + 1);

                if(sensorName.equals("GPS")) {
                    if(GPSView == null) {
                        GPSView = MainActivity.this.getLayoutInflater().inflate(R.layout.googlemaplayout, null);
                    }

                    GPSDatabaseDialog dialog = new GPSDatabaseDialog();
                    dialog.setContext(MainActivity.this);

                    dialog.show(getSupportFragmentManager(), "GPSDatabaseDialog");
                } else if(showConfigs.containsKey(sensorName + deviceID)) {
                    DatabaseSensorDialog dialog = new DatabaseSensorDialog();
                    dialog.setContext(MainActivity.this);
                    dialog.setPlotConfig(showConfigs.get(sensorName + deviceID));
                    dialog.show(getSupportFragmentManager(), "DatabaseSensorDialogFragment");
                }
            }

        });

        if(!de.unima.ar.collector.shared.Settings.DATABASE_DIRECT_INSERT && SensorDataCollectorService.getInstance().getSCM().getEnabledCollectors().size() > 0) {
            Toast.makeText(this, R.string.analyze_analyze_database_cache_enabled, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * @param start Anfangszeit für die Anzeige der Daten
     * @param end   Endzeitpunkt
     */
    @SuppressLint( { "SimpleDateFormat", "InflateParams" } )
    public void showAnalyzeDatabaseGPS(String start, String end)
    {
        addScreen(Screens.ANALYZE_DATABASE);

        if(GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext()) != ConnectionResult.SUCCESS) {
            Toast.makeText(getBaseContext(), getString(R.string.analyze_gps_notify), Toast.LENGTH_LONG).show();
            showAnalyze();
            return;
        }

        if(GPSView == null) {
            GPSView = this.getLayoutInflater().inflate(R.layout.googlemaplayout, null);
        }

        setContentView(GPSView);

        DrawPointsMap dpm = new DrawPointsMap(this, start, end);
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(dpm);
    }


    public void showAnalyzeDatabaseData(final PlotConfiguration pc, final String start, final String end, final boolean showActivity, final boolean showPosition, final boolean showPosture)
    {
        addScreen(Screens.ANALYZE_DATABASE);
        setContentView(R.layout.databaseshowplot);

        this.refreshGraphBuilderProgressBar(R.string.analyze_analyzelive_loading1, 1);

        FrameLayout progressBarHolder = (FrameLayout) findViewById(R.id.progressBarHolder);
        progressBarHolder.setVisibility(View.VISIBLE);

        Thread buildGraph = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");

                // Main Plot
                XYPlot mainPlot = (XYPlot) findViewById(R.id.DatabasePlot);
                mainPlot.getGraphWidget().setTicksPerRangeLabel(1);
                mainPlot.getGraphWidget().setTicksPerDomainLabel(2);
                mainPlot.setDomainStep(XYStepMode.SUBDIVIDE, 7);
                mainPlot.getGraphWidget().setRangeValueFormat(new DecimalFormat("#####.#"));
                mainPlot.getGraphWidget().setDomainValueFormat(new DateFormat());
                mainPlot.getGraphWidget().setRangeLabelWidth(25);
                mainPlot.setRangeLabel("");
                mainPlot.setDomainLabel("");

                // Data Plot Listener
                DataPlotZoomListener dpzl = new DataPlotZoomListener(mainPlot, pc, start, end);
                mainPlot.setOnTouchListener(dpzl);

                // Activity Plot
                XYPlot activityPlot = (XYPlot) findViewById(R.id.ActivityPlot);
                dpzl.ActivityPlot = activityPlot;

                // Position Plot
                XYPlot positionPlot = (XYPlot) findViewById(R.id.PositionPlot);
                dpzl.PositionPlot = positionPlot;

                // Posture Plot
                XYPlot posturePlot = (XYPlot) findViewById(R.id.PosturePlot);
                dpzl.PosturePlot = posturePlot;

                // start ploting
                dpzl.start();

                // Activity Plot
                if(showActivity) {
                    // Create database query
                    main.refreshGraphBuilderProgressBar(R.string.analyze_analyzelive_loading6, 85);
                    List<String[]> result = SQLDBController.getInstance().query("SELECT atn.name, satn.name, ad.starttime, ad.endtime FROM " + SQLTableName.ACTIVITYDATA + " as ad " + " LEFT JOIN " + SQLTableName.ACTIVITIES + " as atn ON ad.activityid = atn.id" + " LEFT JOIN " + SQLTableName.SUBACTIVITIES + " as satn ON ad.subactivityid = satn.id" + " WHERE (ad.endtime > ? AND ad.starttime < ?) OR (ad.endtime = 0 AND ad.starttime < ?)", new String[]{ String.valueOf(start), String.valueOf(end), String.valueOf(end) }, false);

                    ArrayList<String> displayNames = new ArrayList<>();
                    float displayedSeriesCount = 0;

                    for(String[] row : result) {
                        String displayString = row[0];
                        int alreadyExists = -1;

                        if(row[1] != null) {
                            displayString += Settings.ACTIVITY_DELIMITER + row[1];
                        }

                        for(int i = 0; i < displayNames.size(); i++) {
                            if(displayNames.get(i).equals(displayString)) {
                                alreadyExists = i;
                                break;
                            }
                        }

                        int yVal;

                        if(alreadyExists != -1) {
                            yVal = alreadyExists + 1;
                            displayString = "";
                        } else {
                            displayedSeriesCount++;
                            yVal = (int) displayedSeriesCount;
                            displayNames.add(displayString);
                        }

                        ArrayList<Number> yVals = new ArrayList<>();
                        yVals.add(yVal);
                        yVals.add(yVal);

                        ArrayList<Number> xVals = new ArrayList<>();

                        // starttime
                        double starttime = Double.valueOf(row[2]);
                        xVals.add(starttime - 1388534400000d);

                        // endtime
                        double endtime = Double.valueOf(row[3]);
                        if(endtime == 0) {
                            endtime = System.currentTimeMillis();
                        }
                        xVals.add(endtime - 1388534400000d);

                        SimpleXYSeries series = new SimpleXYSeries(xVals, yVals, displayString);
                        XYSeriesFormatter<?> sf;

                        if(alreadyExists != -1) {
                            sf = new CustomPointFormatter(SensorDataUtil.getColor(yVal), null, Color.TRANSPARENT, null);
                            ((CustomPointFormatter) sf).getLinePaint().setStrokeWidth(40f);
                        } else {
                            sf = new LineAndPointFormatter(SensorDataUtil.getColor(yVal), null, null, null);
                            ((LineAndPointFormatter) sf).getLinePaint().setStrokeWidth(40f);
                        }

                        activityPlot.addSeries(series, sf);

                        // Display max 12 diffrent activities
                        if(displayNames.size() >= 12) {
                            break;
                        }
                    }

                    SensorDataUtil.setUpDatabasePlot(activityPlot, displayedSeriesCount);
                    activityPlot.setOnTouchListener(dpzl);
                    activityPlot.setDomainBoundaries(dpzl.minXY.x, dpzl.maxXY.x, BoundaryMode.FIXED);
                    activityPlot.redraw();
                } else {
                    activityPlot.setVisibility(View.GONE);
                }

                // Position Plot
                if(showPosition) {
                    // Create database query
                    main.refreshGraphBuilderProgressBar(R.string.analyze_analyzelive_loading7, 90);
                    List<String[]> result = SQLDBController.getInstance().query("SELECT atn.name, ad.starttime, ad.endtime FROM " + SQLTableName.POSITIONDATA + " as ad " + " LEFT JOIN " + SQLTableName.POSITIONS + " as atn ON ad.pid = atn.id" + " WHERE (ad.endtime > ? AND ad.starttime < ?) OR (ad.endtime = 0 AND ad.starttime < ?)", new String[]{ String.valueOf(start), String.valueOf(end), String.valueOf(end) }, false);

                    ArrayList<String> displayNames = new ArrayList<>();
                    float displayedSeriesCount = 0;

                    for(String[] row : result) {
                        String displayString = row[0];
                        int alreadyExists = -1;

                        for(int i = 0; i < displayNames.size(); i++) {
                            if(displayNames.get(i).equals(displayString)) {
                                alreadyExists = i;
                                break;
                            }
                        }

                        int yVal;

                        if(alreadyExists != -1) {
                            yVal = alreadyExists + 1;
                            displayString = "";
                        } else {
                            displayedSeriesCount++;
                            yVal = (int) displayedSeriesCount;
                            displayNames.add(displayString);
                        }

                        ArrayList<Number> yVals = new ArrayList<>();
                        yVals.add(yVal);
                        yVals.add(yVal);

                        ArrayList<Number> xVals = new ArrayList<>();

                        // starttime
                        double starttime = Double.valueOf(row[1]);
                        xVals.add(starttime - 1388534400000d);

                        // endtime
                        double endtime = Double.valueOf(row[2]);
                        if(endtime == 0) {    // still active / running
                            endtime = System.currentTimeMillis();
                        }
                        xVals.add(endtime - 1388534400000d);

                        SimpleXYSeries series = new SimpleXYSeries(xVals, yVals, displayString);
                        XYSeriesFormatter<?> sf;

                        if(alreadyExists != -1) {
                            sf = new CustomPointFormatter(SensorDataUtil.getColor(yVal), null, Color.TRANSPARENT, null);
                            ((CustomPointFormatter) sf).getLinePaint().setStrokeWidth(40f);
                        } else {
                            sf = new LineAndPointFormatter(SensorDataUtil.getColor(yVal), null, null, null);
                            ((LineAndPointFormatter) sf).getLinePaint().setStrokeWidth(40f);
                        }

                        // Display max 12 diffrent activities
                        if(displayNames.size() >= 12) {
                            break;
                        }
                        positionPlot.addSeries(series, sf);
                    }

                    SensorDataUtil.setUpDatabasePlot(positionPlot, displayedSeriesCount);
                    positionPlot.setOnTouchListener(dpzl);
                    positionPlot.setDomainBoundaries(dpzl.minXY.x, dpzl.maxXY.x, BoundaryMode.FIXED);
                    positionPlot.redraw();
                } else {
                    positionPlot.setVisibility(View.GONE);
                }

                // Posture Plot
                if(showPosture) {
                    main.refreshGraphBuilderProgressBar(R.string.analyze_analyzelive_loading8, 95);
                    // Create database query
                    List<String[]> result = SQLDBController.getInstance().query("SELECT atn.name, ad.starttime, ad.endtime FROM " + SQLTableName.POSTUREDATA + " as ad " + " LEFT JOIN " + SQLTableName.POSTURES + " as atn ON ad.pid = atn.id" + " WHERE (ad.endtime > ? AND ad.starttime < ?) OR (ad.endtime = 0 AND ad.starttime < ?)", new String[]{ String.valueOf(start), String.valueOf(end), String.valueOf(end) }, false);

                    ArrayList<String> displayNames = new ArrayList<>();
                    float displayedSeriesCount = 0;

                    for(String[] row : result) {
                        String displayString = row[0];
                        int alreadyExists = -1;

                        for(int i = 0; i < displayNames.size(); i++) {
                            if(displayNames.get(i).equals(displayString)) {
                                alreadyExists = i;
                                break;
                            }
                        }

                        int yVal;

                        if(alreadyExists != -1) {
                            yVal = alreadyExists + 1;
                            displayString = "";
                        } else {
                            displayedSeriesCount++;
                            yVal = (int) displayedSeriesCount;
                            displayNames.add(displayString);
                        }

                        ArrayList<Number> yVals = new ArrayList<>();
                        yVals.add(yVal);
                        yVals.add(yVal);

                        ArrayList<Number> xVals = new ArrayList<>();

                        // starttime
                        double starttime = Double.valueOf(row[1]);
                        xVals.add(starttime - 1388534400000d);

                        // endtime
                        double endtime = Double.valueOf(row[2]);
                        if(endtime == 0) {  // still active/running
                            endtime = System.currentTimeMillis();
                        }
                        xVals.add(endtime - 1388534400000d);

                        SimpleXYSeries series = new SimpleXYSeries(xVals, yVals, displayString);
                        XYSeriesFormatter<?> sf;

                        if(alreadyExists != -1) {
                            sf = new CustomPointFormatter(SensorDataUtil.getColor(yVal), null, Color.TRANSPARENT, null);
                            ((CustomPointFormatter) sf).getLinePaint().setStrokeWidth(40f);
                        } else {
                            sf = new LineAndPointFormatter(SensorDataUtil.getColor(yVal), null, null, null);
                            ((LineAndPointFormatter) sf).getLinePaint().setStrokeWidth(40f);
                        }

                        // Display max 12 diffrent activities
                        if(displayNames.size() >= 12) {
                            break;
                        }
                        posturePlot.addSeries(series, sf);
                    }

                    SensorDataUtil.setUpDatabasePlot(posturePlot, displayedSeriesCount);
                    posturePlot.setOnTouchListener(dpzl);
                    posturePlot.setDomainBoundaries(dpzl.minXY.x, dpzl.maxXY.x, BoundaryMode.FIXED);
                    posturePlot.redraw();
                } else {
                    posturePlot.setVisibility(View.GONE);
                }

                main.refreshGraphBuilderProgressBar(R.string.analyze_analyzelive_loading9, 100);
            }
        });
        buildGraph.start();

    }


    /**
     * Zeigt activity screen an
     */
    public void showActivities()
    {
        addScreen(Screens.ACTIVITIES);

        setContentView(R.layout.activity_main);

        SeparatedListAdapter sadapter = new SeparatedListAdapter(this, 0);

        // Get current Position
        ArrayList<String> positionList = new ArrayList<>();
        String queryString = "SELECT T2.name FROM " + SQLTableName.POSITIONDATA + " as T1 JOIN " + SQLTableName.POSITIONS + " as T2 ON T1.pid = T2.id WHERE T1.endtime = 0";
        List<String[]> result = SQLDBController.getInstance().query(queryString, null, false);

        if(result.size() != 0) {
            positionList.add(result.get(0)[0]);
        } else {
            positionList.add(getString(R.string.activity_environment_none));
        }

        // Get current Posture
        ArrayList<String> postureList = new ArrayList<>();
        queryString = "SELECT T2.name FROM " + SQLTableName.POSTUREDATA + " as T1 JOIN " + SQLTableName.POSTURES + " as T2 ON T1.pid = T2.id WHERE T1.endtime = 0";
        List<String[]> result2 = SQLDBController.getInstance().query(queryString, null, false);

        if(result2.size() != 0) {
            postureList.add(result2.get(0)[0]);
        } else {
            postureList.add(getString(R.string.activity_posture_none));
        }

        // Get current DevicePosition
        ArrayList<String> devicePositionList = new ArrayList<>();
        queryString = "SELECT T2.name FROM " + SQLTableName.DEVICEPOSITIONDATA + " as T1 JOIN " + SQLTableName.DEVICEPOSITION + " as T2 ON T1.pid = T2.id WHERE T1.endtime = 0";
        List<String[]> result3 = SQLDBController.getInstance().query(queryString, null, false);

        if(result3.size() != 0) {
            devicePositionList.add(result3.get(0)[0]);
        } else {
            devicePositionList.add(getString(R.string.activity_devicepositon_none));
        }

        // Adapter
        ArrayAdapter<String> positionAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.listitem, positionList);
        AdapterController.getInstance().register("ActivityScreenPosition", positionAdapter);

        ArrayAdapter<String> postureAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.listitem, postureList);
        AdapterController.getInstance().register("ActivityScreenPosture", postureAdapter);

        ArrayAdapter<String> devicePositionAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.listitem, devicePositionList);
        AdapterController.getInstance().register("ActivityScreenDevicePosition", devicePositionAdapter);

        ArrayList<String> ActivityList = new ArrayList<>();
        ActivityList.add(getString(R.string.activity_activities_add));

        ActivityListRowAdapter activityAdapter = new ActivityListRowAdapter(this, ActivityList);
        AdapterController.getInstance().register("ActivityScreenActivity", activityAdapter);

        sadapter.addSection(getString(R.string.activity_environment_title), positionAdapter);
        sadapter.addSection(getString(R.string.activity_posture_title), postureAdapter);
        sadapter.addSection(getString(R.string.activity_devicepositon_title), devicePositionAdapter);
        sadapter.addSection(getString(R.string.activity_activities_title), activityAdapter);

        ListView lv = (ListView) findViewById(R.id.mainlist);
        lv.setAdapter(sadapter);
        lv.setOnItemClickListener(new ActivityOnItemClickListener(this, positionAdapter, postureAdapter, devicePositionAdapter, activityAdapter));
    }


    public void showActivityCorrection()
    {
        addScreen(Screens.ACTIVITY_CORRECTION);
        setContentView(R.layout.activity_correction);

        ArrayList<String> addEntry = new ArrayList<>();
        addEntry.add(getString(R.string.activity_correction_correction_entry1));
        ArrayAdapter<String> correction = new ArrayAdapter<>(getApplicationContext(), R.layout.listitem, addEntry);

        List<String[]> historyEntries = SQLDBController.getInstance().query("SELECT * FROM " + SQLTableName.ACTIVITYCORRECTION, null, false);
        CorrectionHistoryAdapter history = new CorrectionHistoryAdapter(getApplicationContext(), R.layout.activity_correction_historyentry, historyEntries);
        AdapterController.getInstance().register("ActivityCorrectionHistory", history);

        SeparatedListAdapter mainAdapter = new SeparatedListAdapter(this, 0);
        mainAdapter.addSection(getString(R.string.activity_correction_correction), correction);
        mainAdapter.addSection(getString(R.string.activity_correction_history), history);

        final ListView lv = (ListView) findViewById(R.id.activity_correction_entry);
        lv.setAdapter(mainAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if(position == 1) {
                    CreateCorrectionDialog cc = new CreateCorrectionDialog();
                    cc.show(getSupportFragmentManager(), "CreateCorrectionDialog");
                } else if(position > 2) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(MainActivity.this.getString(R.string.activity_correction_dialog_delete_title));
                    builder.setMessage(MainActivity.this.getString(R.string.activity_correction_dialog_delete_message));

                    CorrectionHistoryAdapter chAdapter = (CorrectionHistoryAdapter) AdapterController.getInstance().get("ActivityCorrectionHistory");
                    int pos = (chAdapter.getCount() - 1) - (position - 3);  // TODO Workaround "-3"
                    final String entryID = chAdapter.getItem(pos)[0];

                    builder.setNegativeButton(R.string.activity_dialog_cancel, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            // User cancelled the dialog
                        }
                    });
                    builder.setPositiveButton(R.string.activity_correction_history_delete, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            SQLDBController.getInstance().delete(SQLTableName.ACTIVITYCORRECTION, "id=?", new String[]{ entryID });
                            MainActivity.this.refreshActivityCorrectionHistoryScreen();
                        }
                    });

                    builder.create().show();
                }
            }
        });
    }


    /**
     * Zeigt GoogleMaps an mit GPS position
     */
    @SuppressLint( "InflateParams" )
    public void showGPSAnalyze()
    {
        addScreen(Screens.ANALYZE);

        if(GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext()) != ConnectionResult.SUCCESS) {
            Toast.makeText(getBaseContext(), getString(R.string.analyze_gps_notify), Toast.LENGTH_LONG).show();
            showAnalyze();
            return;
        }

        if(GPSView == null) {
            GPSView = this.getLayoutInflater().inflate(R.layout.googlemaplayout, null);
        }

        setContentView(GPSView);

        final SupportMapFragment map = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        Collection<CustomCollector> ccs = SensorDataCollectorService.getInstance().getSCM().getCustomCollectors().values();

        for(CustomCollector cc : ccs) {
            if(SensorDataUtil.getSensorType(cc.getType()).equals("TYPE_GPS")) {
                map.getMapAsync((GPSCollector) cc);
                break;
            }
        }
    }


    /**
     * Zeigt analyze screen an
     */
    public void showAnalyze()
    {
        // init
        addScreen(Screens.ANALYZE);
        setContentView(R.layout.activity_main);

        // create adapter
        SeparatedListAdapter sadapter = new SeparatedListAdapter(this, 0);
        sadapter.addSection(getString(R.string.analyze_analyze), new ArrayAdapter<>(getApplicationContext(), R.layout.listitem, new String[]{ getString(R.string.analyze_analyze_database) }));

        List<Triple<String, String, String>> activeSensors = new ArrayList<>();

        // devices
        Set<String> devices = ListenerService.getDevices();

        // select active sensors
        Set<Integer> enabledSensors = SensorDataCollectorService.getInstance().getSCM().getEnabledCollectors();
        for(Integer enabledSensor : enabledSensors) {
            String name = SensorDataUtil.getSensorType(enabledSensor);
            for(String device : devices) {
                if(enabledSensor > 0) {
                    SensorCollector sc = SensorDataCollectorService.getInstance().getSCM().getSensorCollectors().get(enabledSensor);
                    if(sc.isRegistered) {
                        activeSensors.add(new Triple<>(StringUtils.formatSensorName(name), getString(R.string.analyze_analyzelive_collecting), device));
                    }
                } else {
                    CustomCollector cc = SensorDataCollectorService.getInstance().getSCM().getCustomCollectors().get(enabledSensor);
                    if(cc.isRegistered() && cc.getPlotter(device) != null && cc.getPlotter(device).plottingEnabled()) {
                        activeSensors.add(new Triple<>(StringUtils.formatSensorName(name), getString(R.string.analyze_analyzelive_collecting), device));
                    } else if(cc.isRegistered() && cc.getPlotter(device) != null && SensorDataUtil.getSensorType(cc.getType()).equals("TYPE_GPS")) {
                        activeSensors.add(new Triple<>(StringUtils.formatSensorName(name), getString(R.string.analyze_analyzelive_collecting), device));
                    }
                }
            }
        }


        // create list
        Collections.sort(activeSensors);
        sadapter.addSection(getString(R.string.analyze_analyzelive), new AnalyzeRowAdapter(this, R.layout.listitemueberblick, activeSensors));

        ListView lv = (ListView) findViewById(R.id.mainlist);
        lv.setAdapter(sadapter);
        lv.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if(view instanceof TextView) {
                    showAnalyzeDatabase();
                }

                if(!(view instanceof RelativeLayout)) {
                    return;
                }

                RelativeLayout layout = (RelativeLayout) view;
                TextView title = (TextView) layout.findViewById(R.id.list_item_ueberblick_title);
                String sensorName = title.getText().toString();

                TextView value = (TextView) layout.findViewById(R.id.list_item_ueberblick_title_subtitle);
                String deviceID = value.getText().toString().replace(getString(R.string.analyze_analyzelive_device) + ": ", "");

                int sensorId = SensorDataUtil.getSensorTypeInt("TYPE_" + sensorName.toUpperCase(Locale.ENGLISH).replace(" ", "_"));
                showAnalyzeLive(deviceID, sensorId);
            }

        });
    }


    /**
     * @param sensorId SensorId die live angezeigt werden soll
     */
    public void showAnalyzeLive(String deviceID, int sensorId)
    {
        // if sensorId > 0 SensorCollector else its a CustomCollector
        if(sensorId >= 0) {
            Collection<SensorCollector> sensors = SensorDataCollectorService.getInstance().getSCM().getSensorCollectors().values();

            SensorCollector sc = null;
            for(SensorCollector tmp : sensors) {
                if(tmp.getSensor().getType() == sensorId) {
                    sc = tmp;
                    break;
                }
            }

            // Fehler sensor kann nicht plotten bzw. existiert nicht
            if(sc == null || !sc.isRegistered) {
                showAnalyze();
                return;
            }

            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

            setContentView(R.layout.androidplottest);

            XYPlot sensorLevelsPlot = (XYPlot) findViewById(R.id.levelsPlot);
            XYPlot sensorHistoryPlot = (XYPlot) findViewById(R.id.historyPlot);

            Plotter plotter = sc.getPlotter(deviceID);
            addScreen(Screens.ANALYZE_LIVE, new Object[]{ deviceID, sensorId, plotter });
            plotter.startPlotting(sensorLevelsPlot, sensorHistoryPlot);

            if(de.unima.ar.collector.shared.Settings.LIVE_PLOTTER_ENABLED && (de.unima.ar.collector.shared.Settings.WEARTRANSFERDIRECT || DeviceID.get(this).equals(deviceID))) {
                plotter.setPlotting(true);
            } else if(!de.unima.ar.collector.shared.Settings.LIVE_PLOTTER_ENABLED) {
                Toast.makeText(this, R.string.analyze_analyzelive_disabled, Toast.LENGTH_SHORT).show();
            } else if(!de.unima.ar.collector.shared.Settings.WEARTRANSFERDIRECT) {
                Toast.makeText(this, R.string.analyze_analyzelive_wearcache, Toast.LENGTH_SHORT).show();
            }
        } else {
            Collection<CustomCollector> sensors = SensorDataCollectorService.getInstance().getSCM().getCustomCollectors().values();

            for(CustomCollector cc : sensors) {
                if(cc.getType() == sensorId && cc.getPlotter(deviceID).plottingEnabled() && cc.isRegistered()) {
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

                    setContentView(R.layout.androidplottest);

                    XYPlot sensorLevelsPlot = (XYPlot) findViewById(R.id.levelsPlot);
                    XYPlot sensorHistoryPlot = (XYPlot) findViewById(R.id.historyPlot);

                    Plotter plotter = cc.getPlotter(deviceID);
                    addScreen(Screens.ANALYZE_LIVE, new Object[]{ deviceID, sensorId, plotter });
                    plotter.startPlotting(sensorLevelsPlot, sensorHistoryPlot);

                    if(de.unima.ar.collector.shared.Settings.LIVE_PLOTTER_ENABLED && (de.unima.ar.collector.shared.Settings.WEARTRANSFERDIRECT || DeviceID.get(this).equals(deviceID))) {
                        plotter.setPlotting(true);
                    } else if(!de.unima.ar.collector.shared.Settings.LIVE_PLOTTER_ENABLED) {
                        Toast.makeText(this, R.string.analyze_analyzelive_disabled, Toast.LENGTH_SHORT).show();
                    } else if(!de.unima.ar.collector.shared.Settings.WEARTRANSFERDIRECT) {
                        Toast.makeText(this, R.string.analyze_analyzelive_wearcache, Toast.LENGTH_SHORT).show();
                    }
                    break;
                } else if(cc.isRegistered() && cc.getType() == sensorId) {
                    showGPSAnalyze();
                    break;
                }
            }
        }

    }


    public void showOptions()
    {
        Intent i = new Intent(this, SettingActivity.class);
        startActivityForResult(i, 1000);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == 1000) {        // workaround -just want to be sure that the main screen is shown if the preference activity stopped
            showSensoren();
        }
    }


    /**
     * Mainscreen
     */
    public void showSensoren()
    {
        addScreen(Screens.SENSOREN);
        setContentView(R.layout.activity_main);

        // Fülle Liste
        List<String> valueList = new ArrayList<>();

        valueList.add(getString(R.string.main_collectors_sensors));
        valueList.add(getString(R.string.main_collectors_activities));
        valueList.add(getString(R.string.main_collectors_correction));

        ArrayAdapter adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.listitem, valueList);
        SeparatedListAdapter sadapter = new SeparatedListAdapter(this, 4);

        OverviewRowAdapter overview = new OverviewRowAdapter(this);
        AdapterController.getInstance().register("MainScreenOverview", overview);

        sadapter.addSection(getString(R.string.main_overview), overview);
        sadapter.addSection(getString(R.string.main_collectors), adapter);

        final ListView lv = (ListView) findViewById(R.id.mainlist);

        lv.setAdapter(sadapter);

        lv.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if(id == 4) {
                    showAdditionalDevices();
                } else if(id == 6) {
                    showSensorenDetail();
                } else if(id == 7) {
                    showActivities();
                } else if(id == 8) {
                    showActivityCorrection();
                } else {
                    String item = ((TextView) view).getText().toString();
                    Toast.makeText(getBaseContext(), item + " " + id, Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    public void showAdditionalDevices()
    {
        addScreen(Screens.ADDITIONAL_DEVICES);
        setContentView(R.layout.activity_main);

        SeparatedListAdapter sadapter = new SeparatedListAdapter(this, 0);

        ArrayList<String> tmp = new ArrayList<>(ListenerService.getDevices());
        String deviceID = DeviceID.get(this);
        tmp.remove(deviceID);
        tmp.add(deviceID + " (" + getString(R.string.device_localdevice) + ")");
        Collections.sort(tmp);

        ArrayAdapter<String> aa = new ArrayAdapter<>(this, R.layout.listitem, tmp);
        sadapter.addSection(getString(R.string.device_title), aa);

        ListView lv = (ListView) findViewById(R.id.mainlist);
        lv.setAdapter(sadapter);
        lv.setEnabled(false);
    }


    public void showSensorenDetail()
    {
        addScreen(Screens.SENSOREN_DETAILS);
        setContentView(R.layout.activity_main);

        SeparatedListAdapter sadapter = new SeparatedListAdapter(this, 0);

        SensorenRowAdapter sra = new SensorenRowAdapter(this);
        sadapter.addSection(getString(R.string.main_collectors), sra);

        ListView lv = (ListView) findViewById(R.id.mainlist);
        lv.setAdapter(sadapter);

        lv.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                SensorDataCollectorService service = SensorDataCollectorService.getInstance();

                CheckBox chBx = ((CheckBox) view.findViewById(R.id.sensorcheckBox1));
                TextView txt1 = ((TextView) view.findViewById(R.id.sensortextview1));
                TextView txt2 = ((TextView) view.findViewById(R.id.sensortextview2));

                int type = SensorDataUtil.getSensorTypeInt("TYPE_" + txt1.getText().toString().toUpperCase(Locale.ENGLISH).replace(" ", "_"));

                if(SensorDataUtil.getSensorType(type).equals("TYPE_GPS") && !service.getSCM().isCustomCollectorRegistered(SensorDataUtil.getSensorTypeInt("TYPE_GPS"))) {
                    final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                    if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        Toast.makeText(getBaseContext(), R.string.sensor_collector_gps_notify, Toast.LENGTH_LONG).show();
                        return;
                    }

                    if(Build.VERSION.SDK_INT >= 23 &&
                            ContextCompat.checkSelfPermission(getBaseContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(getBaseContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getBaseContext(), R.string.sensor_collector_gps_permission, Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                // Is custom sensor?
                if(type < -1) {
                    CustomCollector cc = service.getSCM().getCustomCollectors().get(type);
                    if(chBx.isChecked()) {
                        if(!service.getSCM().unregisterCustomCollector(type)) {
                            Toast.makeText(getBaseContext(), getString(R.string.sensor_collector_generel_notify1), Toast.LENGTH_LONG).show();
                        } else {
                            service.getSCM().disableCollectors(type);
                            DBUtils.updateSensorStatus(type, (int) cc.getSensorRate(), 0);
                            chBx.setChecked(false);
                        }
                    } else {
                        if(!service.getSCM().enableCollectors(type)) {
                            Toast.makeText(getBaseContext(), getString(R.string.sensor_collector_custom_notify), Toast.LENGTH_LONG).show();
                        } else {
                            service.getSCM().registerCustomCollectors();
                            DBUtils.updateSensorStatus(type, (int) cc.getSensorRate(), 1);
                            chBx.setChecked(true);
                        }
                    }
                } else {
                    String sensorName = txt2.getText().toString();
                    final int sensorID = SensorDataUtil.getSensorTypeInt("TYPE_" + txt1.getText().toString().toUpperCase(Locale.ENGLISH).replace(" ", "_"));
                    SensorCollector sc = service.getSCM().getSensorCollectors().get(sensorID);
                    // Fall 1: Sensor läuft bereits dann removen wir ihn
                    if(chBx.isChecked()) {
                        if(!service.getSCM().removeSensor(sensorName, sensorID)) {
                            Toast.makeText(getBaseContext(), getString(R.string.sensor_collector_generel_notify1), Toast.LENGTH_LONG).show();
                        } else {
                            BroadcastService.getInstance().sendMessage("/sensor/unregister", String.valueOf(sensorID));
                            DBUtils.updateSensorStatus(sensorID, (1000 * 1000) / sc.getSensorRate(), 0); // microseconds -> hertz
                            chBx.setChecked(false);

                            new Thread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Utils.makeToast2(MainActivity.this, R.string.sensor_cache_to_database, Toast.LENGTH_LONG);
                                    SensorDataUtil.flushSensorDataCache(sensorID, null);
                                }
                            }).start();
                        }
                    } else {
                        if(!service.getSCM().enableCollectors(sensorID)) {
                            Toast.makeText(getBaseContext(), getString(R.string.sensor_collector_generel_notify2), Toast.LENGTH_LONG).show();
                        } else {
                            if(Settings.WEARSENSOR) {
                                BroadcastService.getInstance().sendMessage("/sensor/register", "[" + sensorID + ", " + sc.getSensorRate() + "]");
                            }
                            DBUtils.updateSensorStatus(sensorID, (1000 * 1000) / sc.getSensorRate(), 1); // microseconds -> hertz
                            service.getSCM().registerSensorCollector(sensorID);
                            chBx.setChecked(true);
                        }
                    }
                }

            }
        });
    }


    public void showSensorSelftest(Sensor sensor)
    {
        addScreen(Screens.SENSOREN_SELFTEST);

        if(sensor == null) {
            sensor = this.lastSensorSelfTest.getSensor();
        }

        lastSensorSelfTest = new SensorSelfTest(this, sensor);
    }


    public void refreshMainScreenOverview()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                OverviewRowAdapter overview = (OverviewRowAdapter) AdapterController.getInstance().get("MainScreenOverview");
                overview.notifyDataSetChanged();
            }
        });
    }


    public void refreshActivityScreenPosture(final String posture)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                @SuppressWarnings( "unchecked" )  // it is save
                        ArrayAdapter<String> postureAdapter = (ArrayAdapter<String>) AdapterController.getInstance().get("ActivityScreenPosture");
                if(postureAdapter == null) {
                    return;
                }
                postureAdapter.clear();
                postureAdapter.add(posture);
                postureAdapter.notifyDataSetChanged();
            }
        });
    }


    public void refreshActivityScreenPosition(final String position)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                @SuppressWarnings( "unchecked" ) // it is save
                        ArrayAdapter<String> positionAdapter = (ArrayAdapter<String>) AdapterController.getInstance().get("ActivityScreenPosition");
                if(positionAdapter == null) {
                    return;
                }
                positionAdapter.clear();
                positionAdapter.add(position);
                positionAdapter.notifyDataSetChanged();
            }
        });
    }


    public void refreshActivityScreenActivity(final String activity, final boolean add)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ActivityListRowAdapter activityAdapter = (ActivityListRowAdapter) AdapterController.getInstance().get("ActivityScreenActivity");
                if(activityAdapter == null) {
                    return;
                }

                if(add) {
                    activityAdapter.add(activity);
                } else {
                    activityAdapter.remove(activity);
                }
                activityAdapter.notifyDataSetChanged();
            }
        });
    }


    public void refreshActivityCorrectionHistoryScreen()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                CorrectionHistoryAdapter chAdapter = (CorrectionHistoryAdapter) AdapterController.getInstance().get("ActivityCorrectionHistory");
                if(chAdapter == null) {
                    return;
                }

                List<String[]> historyEntries = SQLDBController.getInstance().query("SELECT * FROM " + SQLTableName.ACTIVITYCORRECTION, null, false);

                chAdapter.clear();
                chAdapter.addAll(historyEntries);
                chAdapter.notifyDataSetChanged();
            }
        });
    }


    public void refreshGraphBuilderProgressBar(final int text, final int progress)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ProgressBar gbBar = (ProgressBar) findViewById(R.id.GraphBuilderprogressBar);
                gbBar.setProgress(progress);

                TextView gbtext = (TextView) findViewById(R.id.GraphBuildertextView);
                gbtext.setText(getString(text) + " (" + progress + "%)");

                if(progress == 100) {
                    FrameLayout progressBarHolder = (FrameLayout) findViewById(R.id.progressBarHolder);
                    progressBarHolder.setVisibility(View.GONE);
                }
            }
        });
    }


    private Notification createBasicNotification()
    {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.app_toast_running2));

        SensorCollectorManager scm = SensorDataCollectorService.getInstance().getSCM();
        if(scm != null && scm.getEnabledCollectors().size() > 0) {
            builder.setContentText(getString(R.string.app_toast_running1));
        }

        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentIntent(intent);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        return notification;
    }


    private void displayNotification(Notification notification)
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }


    private void hideNotification()
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }


    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        if(item.getItemId() == R.id.action_analyze) {
            if(getCurrentScreen().screen != Screens.ANALYZE) {
                showAnalyze();
            }
            return true;
        } else if(item.getItemId() == R.id.action_sensoren) {
            if(getCurrentScreen().screen != Screens.SENSOREN) {
                showSensoren();
            }
            return true;
        } else if(item.getItemId() == R.id.action_options) {
            if(getCurrentScreen().screen != Screens.OPTIONS) {
                showOptions();
            }
            return true;
            //        } else if(item.getItemId() == R.id.menu_createactivity) {
            //            CreateActivityDialog cs = new CreateActivityDialog();
            //            cs.show(getSupportFragmentManager(), "CreateActivityDialog");
            //
            //            return true;
            //        } else if(item.getItemId() == R.id.menu_createsubactivity) {
            //            CreateSubActivityDialog cs = new CreateSubActivityDialog();
            //            cs.show(getSupportFragmentManager(), "CreateSubActivityDialog");
            //
            //            return true;
            //        } else if(item.getItemId() == R.id.menu_createposition) {
            //            CreatePostitionDialog cs = new CreatePostitionDialog();
            //            cs.show(getSupportFragmentManager(), "CreatePostitionDialog");
            //
            //            return true;
            //        } else if(item.getItemId() == R.id.menu_createposture) {
            //            CreatePostureDialog cs = new CreatePostureDialog();
            //            cs.show(getSupportFragmentManager(), "CreatePostureDialog");
            //
            //            return true;
            //        } else if(item.getItemId() == R.id.menu_deleteactivity) {
            //            DeleteActivityDialog cs = new DeleteActivityDialog();
            //
            //            cs.setMainActivity(this);
            //            cs.show(getSupportFragmentManager(), "DeleteActivityDialog");
            //
            //            return true;
            //        } else if(item.getItemId() == R.id.menu_deletesubactivity) {
            //            DeleteSubActivity cs = new DeleteSubActivity();
            //
            //            cs.setMainActivity(this);
            //            cs.show(getSupportFragmentManager(), "DeleteSubActivity");
            //
            //            return true;
            //        } else if(item.getItemId() == R.id.menu_deleteposition) {
            //            DeletePositionDialog cs = new DeletePositionDialog();
            //
            //            cs.setMainActivity(this);
            //            cs.show(getSupportFragmentManager(), "DeletePositionDialog");
            //
            //            return true;
            //        } else if(item.getItemId() == R.id.menu_deleteposture) {
            //            DeletePostureDialog cs = new DeletePostureDialog();
            //
            //            cs.setMainActivity(this);
            //            cs.show(getSupportFragmentManager(), "DeletePostureDialog");
            //
            //            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onResume()
    {
        super.onResume();

        // resume sensorselftest (activate sensors)
        if(this.lastSensorSelfTest != null && lastScreens.size() > 0 && lastScreens.get(lastScreens.size() - 1).screen == Screens.SENSOREN_SELFTEST) {
            this.lastSensorSelfTest.resumeTest();
        }

        // resume live plotter
        if(this.lastSensorSelfTest != null && lastScreens.size() > 0 && lastScreens.get(lastScreens.size() - 1).screen == Screens.ANALYZE_LIVE) {
            ((Plotter) lastScreens.get(lastScreens.size() - 1).data[2]).setPlotting(true);
        }

        // hide notifications
        this.hideNotification();
    }


    @Override
    public void onPause()
    {
        super.onPause();

        // stop sensorselftest (disable sensors)
        if(this.lastSensorSelfTest != null) {
            this.lastSensorSelfTest.stopTest();
        }

        // stop live plotter
        if(this.lastSensorSelfTest != null && lastScreens.size() > 0 && lastScreens.get(lastScreens.size() - 1).screen == Screens.ANALYZE_LIVE) {
            ((Plotter) lastScreens.get(lastScreens.size() - 1).data[2]).setPlotting(false);
        }

        // show notification
        this.displayNotification(this.createBasicNotification());
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();

        // stop all sensors
        SensorCollectorManager scManager = SensorDataCollectorService.getInstance().getSCM();
        scManager.unregisterSensorCollectors();
        scManager.getEnabledCollectors().clear();

        // stop sensorselftest (disables sensors)
        if(this.lastSensorSelfTest != null) {
            this.lastSensorSelfTest.stopTest();
        }

        // stop live plotter
        if(this.lastSensorSelfTest != null && lastScreens.size() > 0 && lastScreens.get(lastScreens.size() - 1).screen == Screens.ANALYZE_LIVE) {
            ((Plotter) lastScreens.get(lastScreens.size() - 1).data[2]).setPlotting(false);
        }

        // stop service
        stopService(new Intent(MainActivity.this, SensorDataCollectorService.class));
        BluetoothController.getInstance().unregister(this);

        // hide notifications
        hideNotification();

        // stop wearable app
        BroadcastService.getInstance().sendMessage("/activity/destroy", "false");
        //        if(ListenerService.getDevices().size() > 1) {
        //            Toast.makeText(this, getString(R.string.app_toast_destroy1), Toast.LENGTH_SHORT).show();
        //        }

        // flush cache
        SensorDataUtil.flushSensorDataCache(0, DeviceID.get(this));

        // clean up
        ActivityController.getInstance().shutdown();

        // destroyed
        //        Toast.makeText(SensorDataCollectorService.getInstance(), getString(R.string.app_toast_destroy2), Toast.LENGTH_SHORT).show();
    }
}