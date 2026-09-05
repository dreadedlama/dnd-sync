package in.dreadedlama.dndsync;


import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import androidx.preference.PreferenceManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.apache.commons.lang3.SerializationUtils;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import in.dreadedlama.dndsync.shared.PhoneSignal;

public class DNDNotificationService extends NotificationListenerService {
    private static final String TAG = "DNDNotificationService";
    private static final String DND_SYNC_CAPABILITY_NAME = "dnd_sync";
    private static final String DND_SYNC_MESSAGE_PATH = "/wear-dnd-sync";

    private boolean isWindDownNotification(StatusBarNotification sbn) {
        return sbn.getPackageName().equals("com.google.android.apps.wellbeing") &&
                sbn.getNotification().getChannelId().equals("wind_down_notifications");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn){

        if(isWindDownNotification(sbn)) {

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean syncBedTime = prefs.getBoolean("bedtime_sync_key", true);

            if(syncBedTime) {
                // depending on the number of actions that can be done, bedtime mode
                // could be in "pause mode" or "on mode":
                // * If it is in "pause" mode, there is only one action ("Restart bedtime")
                // * If it is in "on" mode, there are two actions possible ("Pause it" and "De-activate it")
                boolean is_on = sbn.getNotification().actions.length == 2;
                boolean is_paused = sbn.getNotification().actions.length == 1;

                if (is_on) {
                    // 5 means bedtime ON
                    Log.d(TAG, "bedtime mode is on");
                    int interruptionFilter = 5;
                    new Thread(() -> sendDNDSync(new PhoneSignal(interruptionFilter, prefs))).start();
                } else if (is_paused) {
                    // 6 means bedtime OFF
                    Log.d(TAG, "bedtime mode is off");
                    int interruptionFilter = 6;
                    new Thread(() -> sendDNDSync(new PhoneSignal(interruptionFilter, prefs))).start();
                }
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn){
        // if notifications is removed, we want surely to disable bedtime mode
        if(isWindDownNotification(sbn)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean syncBedTime = prefs.getBoolean("bedtime_sync_key", true);

            if (syncBedTime) {
                // 6 means bedtime OFF
                Log.d(TAG, "bedtime mode is off");
                int interruptionFilter = 6;
                new Thread(() -> sendDNDSync(new PhoneSignal(interruptionFilter, prefs))).start();
            }
        }
    }

    @Override
    public void onInterruptionFilterChanged (int interruptionFilter) {
        Log.d(TAG, "interruption filter changed to " + interruptionFilter);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean syncDnd = prefs.getBoolean("dnd_sync_key", true);

        if(syncDnd) {
            new Thread(() -> sendDNDSync(new PhoneSignal(interruptionFilter, prefs))).start();
        }
    }

    private void sendDNDSync(PhoneSignal phoneSignal) {
        // https://developer.android.com/training/wearables/data/messages

        // search nodes for sync
        CapabilityInfo capabilityInfo;
        try {
            capabilityInfo = Tasks.await(
                    Wearable.getCapabilityClient(this).getCapability(
                            DND_SYNC_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE));
        } catch (ExecutionException e) {
            e.printStackTrace();
            Log.e(TAG, "execution error while searching nodes", e);
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, "interruption error while searching nodes", e);
            return;
        }

        // send request to all reachable nodes
        // capabilityInfo has the reachable nodes with the dnd sync capability
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        if (connectedNodes.isEmpty()) {
            // Unable to retrieve node with transcription capability
            Log.d(TAG, "Unable to retrieve node with sync capability!");
        } else {
            for (Node node : connectedNodes) {
                if (node.isNearby()) {

                    byte[] data = SerializationUtils.serialize(phoneSignal);
                    Task<Integer> sendTask =
                            Wearable.getMessageClient(this).sendMessage(node.getId(), DND_SYNC_MESSAGE_PATH, data);

                    sendTask.addOnSuccessListener(integer -> Log.d(TAG, "send successful! Receiver node id: " + node.getId()));

                    sendTask.addOnFailureListener(e -> Log.d(TAG, "send failed! Receiver node id: " + node.getId()));
                }
            }
        }
    }
}
