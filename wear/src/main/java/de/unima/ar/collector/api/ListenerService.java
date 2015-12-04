package de.unima.ar.collector.api;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class ListenerService extends WearableListenerService
{

    @Override
    public void onMessageReceived(MessageEvent messageEvent)
    {
        String path = messageEvent.getPath();

        if(path.equalsIgnoreCase("/activity/start")) {
            Tasks.startWearableApp(this);
            return;
        }

        if(path.equalsIgnoreCase("/activity/destroy")) {
            Tasks.destroyWearableApp(messageEvent.getData());
            return;
        }

        if(path.startsWith("/database/response")) {
            Tasks.processDatabaseResponse(path, messageEvent.getData());
            return;
        }

        if(path.equalsIgnoreCase("/database/delete")) {
            Tasks.deleteDatabase(this.getBaseContext());
            return;
        }

        if(path.equalsIgnoreCase("/sensor/register")) {
            Tasks.registerSensor(messageEvent.getData());
            return;
        }

        if(path.startsWith("/sensor/unregister")) {
            Tasks.unregisterSensor(messageEvent.getData());
            return;
        }

        if(path.startsWith("/sensor/blob/confirm")) {
            Tasks.confirmBlob(path);
            return;
        }

        if(path.equalsIgnoreCase("/settings")) {
            Tasks.updateSettings(this.getBaseContext(), messageEvent.getData());
            return;
        }

        super.onMessageReceived(messageEvent);
    }
}