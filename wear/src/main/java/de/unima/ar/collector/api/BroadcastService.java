package de.unima.ar.collector.api;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.shared.util.Utils;

public class BroadcastService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, NodeApi.NodeListener
{
    private Context  context;
    private Vibrator vibService;

    private static BroadcastService SERVICE = null;
    private static GoogleApiClient  gac     = null;

    private static final String TAG = "wear.broadcast";


    private BroadcastService(Context context)
    {
        this.context = context;
        this.vibService = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);

        gac = new GoogleApiClient.Builder(this.context).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        Wearable.NodeApi.addListener(gac, this);
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


    //    public static void shutdown()
    //    {
    //        gac.disconnect();
    //    }


    public GoogleApiClient getAPIClient()
    {
        return gac;
    }


    public void sendMessage(final String path, final String text)
    {
        sendMessage(path, text.getBytes());
    }


    public void sendMessage(final String path, final byte[] bytes)
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
                    Wearable.MessageApi.sendMessage(gac, node.getId(), path, bytes).await();
                }
            }
        }).start();
    }


    @Override
    public void onConnected(Bundle bundle)
    {
        Log.d(TAG, "Client connected!");
    }


    @Override
    public void onConnectionSuspended(int cause)
    {
        Log.d(TAG, "Client suspended!");
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.d(TAG, "Client failed!");
    }


    @Override
    public void onPeerConnected(Node node)
    {
        Log.d(TAG, "Peer conntected!");

        this.vibService.cancel();
    }


    @Override
    public void onPeerDisconnected(Node node)
    {
        Log.d(TAG, "Peer disconnected!");

        MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");
        if(main == null) {
            return;
        }

        long[] pattern = { 0, 100, 500 };  // start without a delay, vibrate for 100 milliseconds, sleep for 1000 milliseconds
        this.vibService.vibrate(pattern, 0);  // '0' here means to repeat indefinitely

        Utils.makeToast((Activity) this.context, R.string.broadcast_outofrange, Toast.LENGTH_LONG);
    }
}