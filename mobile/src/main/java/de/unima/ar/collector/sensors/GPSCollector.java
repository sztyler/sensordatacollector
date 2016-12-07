package de.unima.ar.collector.sensors;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
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
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.DeviceID;
import de.unima.ar.collector.util.PlotConfiguration;


/**
 * @author Timo Sztyler, Fabian Kramm
 */
public class GPSCollector extends CustomCollector implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback
{
    private static final int      type       = -3;
    private static final String[] valueNames = new String[]{ "attr_lat", "attr_lng", "attr_time" };

    private Context               context;
    private LocationManager       locationManager;
    private Location              lastKnownLocation;
    private Set<LocationListener> locationListeners;
    private GoogleMap             map;
    private Timer                 timer;
    private GoogleApiClient       mGoogleApiClient;


    private Location lastKnownGPSLocation = null; // workaround
    private boolean  CameraMoved          = false;    // TODO?

    private static Marker myPositionMarker;
    private static Map<String, Plotter> plotters = new HashMap<>();


    public GPSCollector(Context context)
    {
        super();

        this.context = context;
        this.locationListeners = new HashSet<>();

        if(this.sensorRate < 0) {
            this.sensorRate = Settings.GPS_DEFAULT_FREQUENCY;
        }

        // create new plotter
        List<String> devices = DatabaseHelper.getStringResultSet("SELECT device FROM " + SQLTableName.DEVICES, null);
        for(String device : devices) {
            GPSCollector.createNewPlotter(device);
        }

        // check if google play services are installed
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int status = googleAPI.isGooglePlayServicesAvailable(this.context);
        if(status == ConnectionResult.SUCCESS) {
            this.mGoogleApiClient = new GoogleApiClient.Builder(this.context).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        }

        // backup
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }


    @Override
    public void onRegistered()
    {
        if(mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        } else {
            searchLocationWithoutGoogleService();
        }

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                Log.d("GPSCollector", "New Data Available?");

                Location location = null;
                if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    if(!(Build.VERSION.SDK_INT >= 23 &&
                            !(ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED))) {

                        location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    }
                }

                if(location == null || (lastKnownLocation != null && location.getTime() <= lastKnownLocation.getTime())) {
                    location = getBestLocation();
                }

                if(location != null && (lastKnownLocation == null || location.getTime() > lastKnownLocation.getTime())) {
                    lastKnownLocation = location;
                    doLocationUpdate(lastKnownLocation);
                }
            }
        }, 0, getSensorRate());
    }


    private void searchLocationWithoutGoogleService()
    {
        if(Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

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
    }


    @Override
    public void onDeRegistered()
    {
        if(this.mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }

        if(Build.VERSION.SDK_INT >= 23 && !(ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            for(LocationListener ll : this.locationListeners) {
                this.locationManager.removeUpdates(ll);
            }
        }
        this.timer.cancel();
        this.map = null;
        this.lastKnownLocation = null;
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
        if(Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

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


    @Override
    public Plotter getPlotter(String deviceID)
    {
        if(!plotters.containsKey(deviceID)) {
            GPSCollector.createNewPlotter(deviceID);
        }

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
        levelPlot.domainValueNames = new String[]{};
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
        String sqlTable = "CREATE TABLE IF NOT EXISTS " + SQLTableName.PREFIX + deviceID + SQLTableName.GPS + " (id INTEGER PRIMARY KEY, " + valueNames[2] + " INTEGER, " + valueNames[0] + " REAL, " + valueNames[1] + " REAL)";
        SQLDBController.getInstance().execSQL(sqlTable);
    }


    public static void writeDBStorage(String deviceID, ContentValues newValues)
    {
        SQLDBController.getInstance().insert(SQLTableName.PREFIX + deviceID + SQLTableName.GPS, null, newValues);
    }


    @Override
    public void onConnected(Bundle bundle)
    {
        // TODO
    }


    @Override
    public void onConnectionSuspended(int i)
    {
        // TODO
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        searchLocationWithoutGoogleService();
    }


    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        this.map = googleMap;

        refreshMap();
    }
}