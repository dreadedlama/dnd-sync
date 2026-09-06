package `in`.dreadedlama.dndsync

import android.service.notification.NotificationListenerService
import android.util.Log
import com.google.android.gms.tasks.Tasks.await
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Wearable
import `in`.dreadedlama.dndsync.shared.WearSignal
import org.apache.commons.lang3.SerializationUtils
import java.util.concurrent.ExecutionException


class DNDNotificationService : NotificationListenerService() {
    override fun onListenerConnected() {
        Log.d(TAG, "listener connected")
        running = true

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
    override fun onListenerDisconnected() {
        Log.d(TAG, "listener disconnected")
        running = false
    }


    override fun onInterruptionFilterChanged(interruptionFilter: Int) {
        Log.d(TAG, "interruption filter changed to $interruptionFilter")

        // preferences are now stored on the mobile app, so we send the signal nonetheless
        // and if the user ticked the relative option then it is synced to the phone
        Thread(Runnable { sendDNDSync(WearSignal(interruptionFilter)) }).start()
    }

    private fun sendDNDSync(wearSignal: WearSignal?) {
        // https://developer.android.com/training/wearables/data/messages

        // search nodes for sync

        var capabilityInfo: CapabilityInfo? = null
        try {
            capabilityInfo = await<CapabilityInfo?>(
                Wearable.getCapabilityClient(this).getCapability(
                    DND_SYNC_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE
                )
            )
        } catch (e: ExecutionException) {
            e.printStackTrace()
            Log.e(TAG, "execution error while searching nodes", e)
            return
        } catch (e: InterruptedException) {
            e.printStackTrace()
            Log.e(TAG, "interruption error while searching nodes", e)
            return
        }

        // send request to all reachable nodes
        // capabilityInfo has the reachable nodes with the dnd sync capability
        val connectedNodes = capabilityInfo.nodes
        if (connectedNodes.isEmpty()) {
            // Unable to retrieve node with transcription capability
            Log.d(TAG, "Unable to retrieve node with sync capability!")
        } else {
            val data = SerializationUtils.serialize(wearSignal)
            val messageClient = Wearable.getMessageClient(this)
            for (node in connectedNodes) {
                try {
                    val result = await(
                        messageClient.sendMessage(node.id, DND_SYNC_MESSAGE_PATH, data)
                    )

                    Log.d(TAG, "message sent to ${node.id}: $result")
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                    Log.e(TAG, "execution error while sending message", e)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    Log.e(TAG, "interruption error while sending message", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "DNDNotificationService"
        private const val DND_SYNC_CAPABILITY_NAME = "dnd_sync"
        private const val DND_SYNC_MESSAGE_PATH = "/wear-dnd-sync"

        var running: Boolean = false
    }
}
