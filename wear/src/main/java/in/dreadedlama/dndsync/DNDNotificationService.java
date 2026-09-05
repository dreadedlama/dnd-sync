package in.dreadedlama.dndsync;


import android.service.notification.NotificationListenerService;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.apache.commons.lang3.SerializationUtils;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import in.dreadedlama.dndsync.shared.WearSignal;

public class DNDNotificationService extends NotificationListenerService {
    private static final String TAG = "DNDNotificationService";
    private static final String DND_SYNC_CAPABILITY_NAME = "dnd_sync";
    private static final String DND_SYNC_MESSAGE_PATH = "/wear-dnd-sync";

    public static boolean running = false;

    @Override
    public void onListenerConnected() {
        Log.d(TAG, "listener connected");
        running = true;

        //TODO enable/disable service based on app setting to save battery
//        // We don't want to run a background service so disable and stop it
//        // to avoid running this service in the background
//        disableServiceComponent();
//        Log.i(TAG, "Disabling service");
//
//        try {
//            stopSelf();
//        } catch(SecurityException e) {
//            Log.e(TAG, "Failed to stop service");
//        }
    }
//    private void disableServiceComponent() {
//        PackageManager p = getPackageManager();
//        ComponentName componentName = new ComponentName(this, DNDNotificationService.class);
//        p.setComponentEnabledSetting(componentName,PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
//    }

    @Override
    public void onListenerDisconnected() {
        Log.d(TAG, "listener disconnected");
        running = false;
    }


    @Override
    public void onInterruptionFilterChanged (int interruptionFilter) {
        Log.d(TAG, "interruption filter changed to " + interruptionFilter);

        // preferences are now stored on the mobile app, so we send the signal nonetheless
        // and if the user ticked the relative option then it is synced to the phone
        new Thread(() -> sendDNDSync(new WearSignal(interruptionFilter))).start();
    }

    private void sendDNDSync(WearSignal wearSignal) {
        // https://developer.android.com/training/wearables/data/messages

        // search nodes for sync
        CapabilityInfo capabilityInfo = null;
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
                    byte[] data = SerializationUtils.serialize(wearSignal);
                    Task<Integer> sendTask =
                            Wearable.getMessageClient(this).sendMessage(node.getId(), DND_SYNC_MESSAGE_PATH, data);

                    sendTask.addOnSuccessListener(integer ->
                            Log.d(TAG, "send successful! Receiver node id: " + node.getId())
                    );

                    sendTask.addOnFailureListener(e ->
                            Log.d(TAG, "send failed! Receiver node id: " + node.getId())
                    );
                }
            }
        }
    }
}
