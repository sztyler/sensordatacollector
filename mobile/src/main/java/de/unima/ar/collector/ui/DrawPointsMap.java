package de.unima.ar.collector.ui;


import android.content.Context;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.DeviceID;

public class DrawPointsMap implements OnMapReadyCallback
{
    private ArrayList<Marker> positionMarkers;

    private Context context;

    private String   start;
    private String   end;
    private Polyline polyline;


    public DrawPointsMap(Context context, String start, String end)
    {
        this.positionMarkers = new ArrayList<>();
        this.start = start;
        this.end = end;
        this.context = context;
    }


    public void cleanUp()
    {
        if(polyline != null) {
            polyline.remove();

            polyline = null;
        }

        if(positionMarkers != null) {
            for(int i = 0; i < positionMarkers.size(); i++) {
                positionMarkers.get(i).remove();
            }

            positionMarkers = null;
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        PolylineOptions polyOptions = new PolylineOptions();
        List<String[]> result = SQLDBController.getInstance().query("SELECT attr_time, attr_lat, attr_lng FROM " + SQLTableName.PREFIX + DeviceID.get(this.context) + SQLTableName.GPS + " WHERE attr_time > ? AND attr_time < ?", new String[]{ String.valueOf(this.start), String.valueOf(this.end) }, false);

        LatLng pos = null;
        for(String[] row : result) {
            pos = new LatLng(Float.valueOf(row[1]), Float.valueOf(row[2]));
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

            polyOptions.add(pos);

            positionMarkers.add(googleMap.addMarker(new MarkerOptions().title(sdf.format(new Date(Long.valueOf(row[0])))).position(pos)));
        }

        if(pos == null) {
            return;
        }

        polyline = googleMap.addPolyline(polyOptions);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
    }
}
