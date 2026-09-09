package `in`.dreadedlama.dndsync

import android.app.NotificationManager
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import `in`.dreadedlama.dndsync.shared.MessagePaths
import `in`.dreadedlama.dndsync.shared.PreferenceKeys
import `in`.dreadedlama.dndsync.shared.StringPreferenceKeys
import `in`.dreadedlama.dndsync.shared.WearSignal
import org.apache.commons.lang3.SerializationUtils
import androidx.core.net.toUri

class DNDSyncListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived: $messageEvent")

        if (messageEvent.path.equals(URL_OPEN_PATH, ignoreCase = true)) {
            val url = "https://github.com/dreadedlama/dnd-sync#watch"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } else if (messageEvent.path.equals(MessagePaths.WATCH_MANUFACTURER, ignoreCase = true)) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)

            // Write once: only store the manufacturer if it has not been set yet (immutable).
            val existing = prefs.getString(StringPreferenceKeys.WATCH_MANUFACTURER, "") ?: ""
            if (existing.isEmpty()) {
                val manufacturer = String(messageEvent.data, Charsets.UTF_8)
                if (manufacturer.isNotEmpty()) {
                    prefs.edit().putString(StringPreferenceKeys.WATCH_MANUFACTURER, manufacturer).apply()
                    Log.d(TAG, "Stored watch manufacturer: $manufacturer")
                }
            } else {
                Log.d(TAG, "Watch manufacturer already set to $existing, ignoring")
            }
        } else if (messageEvent.path.equals(DND_SYNC_MESSAGE_PATH, ignoreCase = true)) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)

            val data = messageEvent.data
            val wearSignal = SerializationUtils.deserialize<WearSignal>(data)
            val dndStateWear = wearSignal.dndState

            Log.d(TAG, "dndStateWear: $dndStateWear")

            // get dnd state
            val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val currentDndState = mNotificationManager.getCurrentInterruptionFilter()

            Log.d(TAG, "currentDndState: $currentDndState")
            if (currentDndState < 0 || currentDndState > 4) {
                Log.d(TAG, "Current DND state is not valid, should be in range [0,4]")
            }

            val shouldSync = prefs.getBoolean(PreferenceKeys.WatchDndSync.key, PreferenceKeys.WatchDndSync.defaultValue)

            if (currentDndState != dndStateWear && shouldSync) {
                Log.d(
                    TAG,
                    "currentDndState != dndStateWear: $currentDndState != $dndStateWear"
                )
                if (mNotificationManager.isNotificationPolicyAccessGranted()) {
                    mNotificationManager.setInterruptionFilter(dndStateWear)
                    Log.d(TAG, "DND set to $dndStateWear")
                } else {
                    Log.d(TAG, "attempting to set DND but access not granted")
                }
            }
        } else {
            super.onMessageReceived(messageEvent)
        }
    }

    companion object {
        private const val TAG = "DNDSyncListenerService"
        private const val DND_SYNC_MESSAGE_PATH = "/wear-dnd-sync"
        private const val URL_OPEN_PATH = "/open_link"
    }
}
