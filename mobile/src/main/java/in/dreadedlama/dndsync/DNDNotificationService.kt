package `in`.dreadedlama.dndsync

import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.Tasks.await
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Wearable
import `in`.dreadedlama.dndsync.shared.PhoneSignal
import `in`.dreadedlama.dndsync.shared.PreferenceKeys
import org.apache.commons.lang3.SerializationUtils
import java.util.concurrent.ExecutionException


class DNDNotificationService : NotificationListenerService() {

    private var samsungModeDetector: SamsungModeDetector? = null
    private var lastModeId: Int = -1

    override fun onListenerConnected() {
        super.onListenerConnected()

        if (!Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            Log.d(TAG, "Not a Samsung device, skipping Samsung mode detection")
            return
        }

        if (samsungModeDetector == null) {
            samsungModeDetector = SamsungModeDetector(
                this,
                { modeId ->
                    Log.d("DND_SYNC", "Samsung mode changed: " + modeId)
                    handleSamsungModeChanged(modeId)
                }
            )
            samsungModeDetector!!.start()
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        stopSamsungModeDetector()
    }

    override fun onDestroy() {
        stopSamsungModeDetector()
        super.onDestroy()
    }

    private fun stopSamsungModeDetector() {
        if (samsungModeDetector != null) {
            samsungModeDetector!!.stop()
            samsungModeDetector = null
        }
    }

    private fun handleSamsungModeChanged(modeId: Int) {
        Log.d(TAG, "Samsung mode_id changed: $modeId")

        if (modeId == lastModeId) {
            return
        }
        lastModeId = modeId

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val interruptionFilter = when (modeId) {
            SamsungModeDetector.MODE_SLEEP -> {
                Log.d(TAG, "Samsung Sleep Mode activated -> bedtime ON")
                5 // bedtime ON
            }
//            SamsungModeDetector.MODE_THEATRE -> {
//                Log.d(TAG, "Samsung Theatre Mode activated -> bedtime ON")
//                5 // bedtime ON
//            }
            else -> {
                Log.d(TAG, "No supported Samsung mode active -> bedtime OFF")
                6 // bedtime OFF
            }
        }

        Thread {
            sendDNDSync(PhoneSignal(interruptionFilter, prefs).also { it.samsungMode = modeId })
        }.start()
    }

    private fun isWindDownNotification(sbn: StatusBarNotification): Boolean {
        return sbn.packageName == "com.google.android.apps.wellbeing" &&
                sbn.notification.channelId == "wind_down_notifications"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (isWindDownNotification(sbn)) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val syncBedTime = prefs.getBoolean(PreferenceKeys.BedtimeSync.key, PreferenceKeys.BedtimeSync.defaultValue)

            if (syncBedTime) {
                // depending on the number of actions that can be done, bedtime mode
                // could be in "pause mode" or "on mode":
                // * If it is in "pause" mode, there is only one action ("Restart bedtime")
                // * If it is in "on" mode, there are two actions possible ("Pause it" and "De-activate it")
                val isOn = sbn.notification.actions.size == 2
                val isPaused = sbn.notification.actions.size == 1

                if (isOn) {
                    // 5 means bedtime ON
                    Log.d(TAG, "bedtime mode is on")
                    val interruptionFilter = 5
                    Thread { sendDNDSync(PhoneSignal(interruptionFilter, prefs)) }.start()
                } else if (isPaused) {
                    // 6 means bedtime OFF
                    Log.d(TAG, "bedtime mode is off")
                    val interruptionFilter = 6
                    Thread { sendDNDSync(PhoneSignal(interruptionFilter, prefs)) }.start()
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // if notifications is removed, we want surely to disable bedtime mode
        if (isWindDownNotification(sbn)) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val syncBedTime = prefs.getBoolean(PreferenceKeys.BedtimeSync.key, PreferenceKeys.BedtimeSync.defaultValue)

            if (syncBedTime) {
                // 6 means bedtime OFF
                Log.d(TAG, "bedtime mode is off")
                val interruptionFilter = 6
                Thread { sendDNDSync(PhoneSignal(interruptionFilter, prefs)) }.start()
            }
        }
    }

    override fun onInterruptionFilterChanged(interruptionFilter: Int) {
        Log.d(TAG, "interruption filter changed to " + interruptionFilter)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val syncDnd = prefs.getBoolean(PreferenceKeys.DndSync.key, PreferenceKeys.DndSync.defaultValue)
        Log.d(TAG, "dnd sync is " + syncDnd)

        if (syncDnd) {
            Thread { sendDNDSync(PhoneSignal(interruptionFilter, prefs)) }.start()
        }
    }

    private fun sendDNDSync(phoneSignal: PhoneSignal?) {
        // https://developer.android.com/training/wearables/data/messages

        // search nodes for sync

        val capabilityInfo: CapabilityInfo
        try {
            capabilityInfo = await<CapabilityInfo>(
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
            val data = SerializationUtils.serialize(phoneSignal)
            val messageClient = Wearable.getMessageClient(this)
            for (node in connectedNodes) {
                try {
                    val result = await(messageClient.sendMessage(node.id, DND_SYNC_MESSAGE_PATH, data))
                    Log.d(
                        TAG,
                        "send successful! Receiver node id: ${node.id} (data: ${result.toString()})"
                    )
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                    Log.e(TAG, "execution error while sending message", e)
                    return
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        private const val TAG = "DNDNotificationService"
        private const val DND_SYNC_CAPABILITY_NAME = "dnd_sync"
        private const val DND_SYNC_MESSAGE_PATH = "/wear-dnd-sync"
    }
}
