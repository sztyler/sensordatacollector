package de.unima.ar.collector.api;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import de.unima.ar.collector.shared.util.Utils;

public class BroadcastService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    private static BroadcastService SERVICE = null;
    private static GoogleApiClient  gac     = null;

    private static final String LOG_TAG = "BROADCASTMOBILE";


    private BroadcastService(Context context)
    {
        gac = new GoogleApiClient.Builder(context).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
    }


    public static void initInstance(Context context)
    {
        if(SERVICE == null) {
            SERVICE = new BroadcastService(context);
        }
    }


    public static BroadcastService getInstance()
    {
        return SERVICE;
    }


    public static void shutdown()
    {
        gac.disconnect();
    }


    public GoogleApiClient getAPIClient()
    {
        return gac;
    }


    public void sendMessage(final String path, final String text)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(gac).await();
                while(nodes.getNodes().size() == 0) {
                    Utils.sleep(500);
                    nodes = Wearable.NodeApi.getConnectedNodes(gac).await();
                }

                for(Node node : nodes.getNodes()) {
                    Wearable.MessageApi.sendMessage(gac, node.getId(), path, text.getBytes()).await();
                }
            }
        }).start();
    }


    @Override
    public void onConnected(Bundle bundle)
    {
        Log.d(LOG_TAG, "Connected!");
    }


    @Override
    public void onConnectionSuspended(int cause)
    {
        Log.d(LOG_TAG, "Suspended!");
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.d(LOG_TAG, "Failed!");
    }
}