package de.unima.ar.collector.sensors;

import android.content.ContentValues;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import de.unima.ar.collector.SensorDataCollectorService;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.extended.Plotter;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.DeviceID;
import de.unima.ar.collector.util.PlotConfiguration;


/**
 * @author Fabian Kramm, Timo Sztyler
 */
public class GPSCollector extends CustomCollector
{
    private static final int      type       = -3;
    private static final String[] valueNames = new String[]{ "attr_lat", "attr_lng", "attr_time" };

    private Context               context;
    private LocationManager       locationManager;
    private Location              lastKnownLocation;
    private Set<LocationListener> locationListeners;
    private GoogleMap             map;
    private Timer                 timer;

    private Location lastKnownGPSLocation = null; // workaround
    private boolean  CameraMoved          = false;    // TODO?

    private static Marker myPositionMarker;
    private static Map<String, Plotter> plotters = new HashMap<>();


    public GPSCollector(Context context)
    {
        this.context = context;
        this.locationListeners = new HashSet<>();

        // create new plotter
        List<String> devices = DatabaseHelper.getStringResultSet("SELECT device FROM " + SQLTableName.DEVICES, null);
        for(String device : devices) {
            GPSCollector.createNewPlotter(device);
        }
    }


    @Override
    public void onRegistered()
    {
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        for(String provider : locationManager.getAllProviders()) {
            LocationListener ll = new LocationListener()
            {
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras)
                {
                }


                @Override
                public void onProviderEnabled(String provider)
                {
                }


                @Override
                public void onProviderDisabled(String provider)
                {
                }


                @Override
                public void onLocationChanged(Location location)
                {
                    if(location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                        Log.d("GPSCollector", "+++DATA: " + location.getLatitude() + " - " + location.getLongitude() + " - " + location.getTime() + " - " + location.getProvider() + " - " + System.currentTimeMillis());
                        lastKnownGPSLocation = location;
                    }
                }
            };
            this.locationManager.requestLocationUpdates(provider, getSensorRate(), 0, ll);
            this.locationListeners.add(ll);
        }

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                Log.d("GPSCollector", "New Data Available?");
                Location location = getBestLocation();
                if(location != null && (lastKnownLocation == null || location.getTime() > lastKnownLocation.getTime())) {
                    lastKnownLocation = location;
                    doLocationUpdate(lastKnownLocation);
                }
            }
        }, 0, getSensorRate());
    }


    @Override
    public void onDeRegistered()
    {
        for(LocationListener ll : this.locationListeners) {
            this.locationManager.removeUpdates(ll);
        }
        this.timer.cancel();
        this.map = null;
    }


    @Override
    public void doTask()
    {

    }


    public void doLocationUpdate(final Location location)
    {
        Log.d("GPSCollector", "DATA: " + location.getLatitude() + " - " + location.getLongitude() + " - " + location.getTime() + " - " + location.getProvider() + " - " + System.currentTimeMillis());
        ContentValues newValues = new ContentValues();
        newValues.put(valueNames[0], location.getLatitude());
        newValues.put(valueNames[1], location.getLongitude());
        newValues.put(valueNames[2], location.getTime());

        // upload/store your location here
        String deviceID = DeviceID.get(SensorDataCollectorService.getInstance());
        GPSCollector.writeDBStorage(deviceID, newValues);

        Log.d("GPSCollector", "MAP: " + map);
        if(map != null) {
            refreshMap();
        }
    }


    private Location getBestLocation()
    {
        Location gpsLocation = getLocationByProvider(LocationManager.GPS_PROVIDER);
        Location networkLocation = getLocationByProvider(LocationManager.NETWORK_PROVIDER);

        if(gpsLocation == null) {
            gpsLocation = lastKnownGPSLocation; // workaround - some devices doesn't support getLastKnownLocation
        }

        if(gpsLocation == null && networkLocation == null) {
            return this.lastKnownLocation;
        } else if(this.lastKnownLocation == null && gpsLocation != null && networkLocation != null) {
            return gpsLocation.getTime() > networkLocation.getTime() ? gpsLocation : networkLocation;
        } else if(this.lastKnownLocation == null && gpsLocation == null && networkLocation != null) {
            return networkLocation;
        } else if(this.lastKnownLocation == null && gpsLocation != null && networkLocation == null) {
            return gpsLocation;
        }

        if(gpsLocation != null && gpsLocation.getTime() > this.lastKnownLocation.getTime()) {
            return gpsLocation;
        } else if(networkLocation != null && networkLocation.getTime() > this.lastKnownLocation.getTime()) {
            return networkLocation;
        }

        return this.lastKnownLocation;
    }


    private Location getLocationByProvider(String provider)
    {
        try {
            if(this.locationManager.isProviderEnabled(provider)) {
                return this.locationManager.getLastKnownLocation(provider);
            }
        } catch(IllegalArgumentException e) {
            Log.d("GPSCollector", "Cannot access Provider " + provider);
        }
        return null;
    }


    private void refreshMap()
    {
        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(context.getMainLooper());
        Runnable myRunnable = new Runnable()
        {

            @Override
            public void run()
            {
                if(lastKnownLocation == null) {
                    return;
                }

                LatLng myPos = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                if(myPositionMarker == null) {
                    myPositionMarker = map.addMarker(new MarkerOptions().title("Current Position").position(myPos));
                } else {
                    myPositionMarker.setPosition(myPos);
                }

                if(!CameraMoved) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 15));
                    CameraMoved = true;
                }
            }

        };
        mainHandler.post(myRunnable);
    }


    public void setMap(GoogleMap map)
    {
        this.map = map;

        refreshMap();
    }


    @Override
    public Plotter getPlotter(String deviceID)
    {
        return plotters.get(deviceID);
    }


    @Override
    public int getType()
    {
        return type;
    }


    public static void createNewPlotter(String deviceID)
    {
        PlotConfiguration levelPlot = new PlotConfiguration();
        levelPlot.domainValueNames = new String[]{ };
        levelPlot.tableName = SQLTableName.GPS;
        levelPlot.sensorName = "GPS";

        PlotConfiguration historyPlot = new PlotConfiguration();
        levelPlot.tableName = SQLTableName.GPS;
        levelPlot.sensorName = "GPS";


        Plotter plotter = new Plotter(deviceID, levelPlot, historyPlot);
        plotters.put(deviceID, plotter);
    }


    public static void updateLivePlotter(String deviceID, float[] values)
    {
        Plotter plotter = plotters.get(deviceID);
        if(plotter == null) {
            GPSCollector.createNewPlotter(deviceID);
            plotter = plotters.get(deviceID);
        }

        plotter.setDynamicPlotData(values);
    }


    public static void createDBStorage(String deviceID)
    {
        String sqlTable = "CREATE TABLE IF NOT EXISTS " + SQLTableName.PREFIX + deviceID + SQLTableName.GPS + " (id INTEGER PRIMARY KEY, " + valueNames[2] + " INTEGER UNIQUE, " + valueNames[0] + " REAL, " + valueNames[1] + " REAL)";
        SQLDBController.getInstance().execSQL(sqlTable);
    }


    public static void writeDBStorage(String deviceID, ContentValues newValues)
    {
        SQLDBController.getInstance().insert(SQLTableName.PREFIX + deviceID + SQLTableName.GPS, null, newValues);
    }
}