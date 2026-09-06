package `in`.dreadedlama.dndsync

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.core.content.getSystemService
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import `in`.dreadedlama.dndsync.shared.PhoneSignal
import org.apache.commons.lang3.SerializationUtils


class DNDSyncListenerService : WearableListenerService() {
    val SAMSUNG: String = "Samsung"
    val manufacturer: String? = Build.MANUFACTURER
    val isSamsung = manufacturer.equals(SAMSUNG, ignoreCase = true)
    private val handler = Handler(Looper.getMainLooper())
    private val samsungBedtimeLauncher = Runnable { launchSamsungBedtimeUIWithRetry() }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.getPath().equals(DND_SYNC_MESSAGE_PATH, ignoreCase = true)) {
            Log.d(TAG, "received path: $DND_SYNC_MESSAGE_PATH")

            // data is now a PhoneSignal object, it must be deserialized
            val data = messageEvent.data
            val phoneSignal = SerializationUtils.deserialize<PhoneSignal>(data)

            Log.d(TAG, "dndStatePhone: " + phoneSignal.dndState)

            // get dnd state
            val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val currentDndState = mNotificationManager.getCurrentInterruptionFilter()

            Log.d(TAG, "currentDndState: $currentDndState")
            if (currentDndState < 0 || currentDndState > 4) {
                Log.d(TAG, "Current DND state is suspicious, should be in range [0,4]")
            }

            if (phoneSignal.dndState != null && phoneSignal.dndState == currentDndState) {
                // avoid issue that happens due to redundant signal propagation:
                // if dnd_as_bedtime and watch_sync_dnd are activated, when dnd is activated
                // from the watch, dnd is activated to the phone and then bedtime is activated
                // back on the watch. This early return avoids that.
                return
            } else if (phoneSignal.dndState != null) {
                Log.d(
                    TAG,
                    "dndStatePhone != currentDndState: " + phoneSignal.dndState + " != " + currentDndState
                )

                changeDndSetting(mNotificationManager, phoneSignal.dndState!!)

                Log.d(TAG, "vibrate: " + phoneSignal.vibratePref)
                if (phoneSignal.vibratePref) {
                    vibrate()
                }
            }

            val currentBedtimeState = Settings.Global.getInt(
                applicationContext.contentResolver, getBedtimeSettingName(), -1
            )

            if (phoneSignal.bedtimeState != null && phoneSignal.bedtimeState != currentBedtimeState) {
                Log.d(
                    TAG,
                    "bedtimeStatePhone != currentBedtimeState: " + phoneSignal.bedtimeState + " != " + currentBedtimeState
                )

                // activating/disabling bedtime also activates/disables dnd, just like
                // when activating bedtime manually from the watch.
                // dndState = 2 means it's activated, dndState = 1 means it's disabled
                val dndState = if (phoneSignal.bedtimeState == 1) 2 else 1
                changeDndSetting(mNotificationManager, dndState)

                val bedtimeModeSuccess = changeBedtimeSetting(phoneSignal.bedtimeState!!)
                if (bedtimeModeSuccess) {
                    Log.d(TAG, "Bedtime mode value toggled")
                } else {
                    Log.d(TAG, "Bedtime mode toggle failed")
                }

                if (phoneSignal.powersavePref) {
                    val powerModeSuccess = changePowerModeSetting(phoneSignal.bedtimeState!!)
                    if (powerModeSuccess) {
                        Log.d(TAG, "Power Saver mode toggled")
                    } else {
                        Log.d(TAG, "Power Saver mode toggle failed")
                    }
                }

                Log.d(TAG, "vibrate: " + phoneSignal.vibratePref)
                if (phoneSignal.vibratePref) {
                    vibrate()
                }
            }
        } else {
            super.onMessageReceived(messageEvent)
        }
    }

    private fun changeDndSetting(mNotificationManager: NotificationManager, newSetting: Int) {
        if (mNotificationManager.isNotificationPolicyAccessGranted) {
            mNotificationManager.setInterruptionFilter(newSetting)
            Log.d(TAG, "DND set to $newSetting")
        } else {
            Log.d(TAG, "attempting to set DND but access not granted")
        }
    }

    private fun getBedtimeSettingName(): String {
        return if (isSamsung) "setting_bedtime_mode_running_state" else "bedtime_mode"
    }

    private fun changeBedtimeSetting(newSetting: Int): Boolean {

        val bedtimeModeSuccess = setGlobalSettingIfPresent(getBedtimeSettingName(), newSetting);
        val zenModeSuccess = setGlobalSettingIfPresent("zen_mode", newSetting);
        val nightDisplayActivated = setSecureSettingIfPresent("night_display_activated", newSetting);

        // Trigger Samsung bedtime UI launch
        if (isSamsung) {
            handler.removeCallbacks(samsungBedtimeLauncher);
            handler.postDelayed(samsungBedtimeLauncher, 1000);
        }

        return bedtimeModeSuccess && zenModeSuccess && nightDisplayActivated
    }

    private fun setGlobalSettingIfPresent(settingName: String?, value: Int): Boolean {
        try {
            if (Settings.Global.getString(contentResolver, settingName) == null) {
                return true
            }
            return Settings.Global.putInt(contentResolver, settingName, value)
        } catch (e: Exception) {
            return true
        }
    }

    private fun setSecureSettingIfPresent(settingName: String?, value: Int): Boolean {
        try {
            if (Settings.Secure.getString(contentResolver, settingName) == null) {
                return true
            }
            return Settings.Secure.putInt(contentResolver, settingName, value)
        } catch (e: SecurityException) {
            return true
        }
    }

    private fun launchSamsungBedtimeUIWithRetry() {
        val intent = Intent()
        intent.component = ComponentName(
            "com.google.android.apps.wearable.settings",
            "com.samsung.android.clockwork.settings.advanced.bedtimemode.StBedtimeModeReservedActivity"
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            startActivity(intent)
            Log.d(TAG, "Samsung bedtime activity launch requested")
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Failed to launch Samsung bedtime activity", e)
        }
    }

    /**
     * Changes the power mode setting.
     *
     * **NOTE:** does not seem to work on non-samsung watches, like the Pixel Watch.
     */
    private fun changePowerModeSetting(newSetting: Int): Boolean {
        val lowPower = setGlobalSettingIfPresent("low_power", newSetting);
        val restrictedDevicePerformance = setGlobalSettingIfPresent("restricted_device_performance", newSetting);
        val lowPowerBackDataOff = setGlobalSettingIfPresent("low_power_back_data_off", newSetting);
        val smConnectivityDisable = setSecureSettingIfPresent("sm_connectivity_disable", newSetting);

        // screen timeout should be set to 10000 also, and ambient_tilt_to_wake should be set to 0
        // but previous variable states in those 2 cases must be stored and they do not seem to stick
        // and they are not so much important tbh (ambient tilt to wake is disabled anyways)
        return lowPower && restrictedDevicePerformance
                && lowPowerBackDataOff && smConnectivityDisable
    }

    private fun vibrate() {
        val vibrator = getSystemService<Vibrator>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(500)
        }
    }

    companion object {
        private const val TAG = "DNDSyncListenerService"
        private const val DND_SYNC_MESSAGE_PATH = "/wear-dnd-sync"
    }
}
