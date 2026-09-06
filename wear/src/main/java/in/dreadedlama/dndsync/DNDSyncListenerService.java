package in.dreadedlama.dndsync;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.apache.commons.lang3.SerializationUtils;

import in.dreadedlama.dndsync.shared.PhoneSignal;

public class DNDSyncListenerService extends WearableListenerService {
    private static final String TAG = "DNDSyncListenerService";
    private static final String DND_SYNC_MESSAGE_PATH = "/wear-dnd-sync";
    private static final String SAMSUNG = "Samsung";
    public static final String GOOGLE = "Google";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable samsungBedtimeLauncher = this::launchSamsungBedtimeUIWithRetry;


    @Override
    public void onMessageReceived (@NonNull MessageEvent messageEvent) {

        if (messageEvent.getPath().equalsIgnoreCase(DND_SYNC_MESSAGE_PATH)) {

            Log.d(TAG, "received path: " + DND_SYNC_MESSAGE_PATH);

            // data is now a PhoneSignal object, it must be deserialized
            byte[] data = messageEvent.getData();
            PhoneSignal phoneSignal = SerializationUtils.deserialize(data);

            Log.d(TAG, "dndStatePhone: " + phoneSignal.dndState);

            // get dnd state
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            int currentDndState = mNotificationManager.getCurrentInterruptionFilter();

            Log.d(TAG, "currentDndState: " + currentDndState);
            if (currentDndState < 0 || currentDndState > 4) {
                Log.d(TAG, "Current DND state it's weird, should be in range [0,4]");
            }

            if (phoneSignal.dndState != null && phoneSignal.dndState == currentDndState) {
                // avoid issue that happens due to redundant signal propagation:
                // if dnd_as_bedtime and watch_sync_dnd are activated, when dnd is activated
                // from the watch, dnd is activated to the phone and then bedtime is activated
                // back on the watch. This early return avoids that.
                return;
            } else if (phoneSignal.dndState != null) {

                Log.d(TAG, "dndStatePhone != currentDndState: " + phoneSignal.dndState + " != " + currentDndState);

                changeDndSetting(mNotificationManager, phoneSignal.dndState);

                Log.d(TAG, "vibrate: " + phoneSignal.vibratePref);
                if (phoneSignal.vibratePref) {
                    vibrate();
                }

            }

            String settingBedtimeStr = "setting_bedtime_mode_running_state";
            int currentBedtimeState = Settings.Global.getInt(getApplicationContext().getContentResolver(), settingBedtimeStr, -1);

            if (currentBedtimeState != -1) {
                Log.d(TAG, "watch is the galaxy watch");
            } else {
                Log.d(TAG, "watch is not the galaxy watch");
                settingBedtimeStr = "bedtime_mode";
                currentBedtimeState = Settings.Global.getInt(getApplicationContext().getContentResolver(), settingBedtimeStr, -1);
            }

            Log.d(TAG, "currentBedtimeState: " + currentBedtimeState);

            if (phoneSignal.bedtimeState != null && phoneSignal.bedtimeState != currentBedtimeState) {

                Log.d(TAG, "bedtimeStatePhone != currentBedtimeState: " + phoneSignal.bedtimeState + " != " + currentBedtimeState);

                // activating/disabling bedtime also activates/disables dnd, just like
                // when activating bedtime manually from the watch.
                // dndState = 2 means it's activated, dndState = 1 means it's disabled
                int dndState = phoneSignal.bedtimeState == 1 ? 2 : 1;
                changeDndSetting(mNotificationManager, dndState);

                boolean bedtimeModeSuccess = changeBedtimeSetting(settingBedtimeStr, phoneSignal.bedtimeState);
                if (bedtimeModeSuccess) {
                    Log.d(TAG, "Bedtime mode value toggled");
                } else {
                    Log.d(TAG, "Bedtime mode toggle failed");
                }

                if(phoneSignal.powersavePref) {

                    boolean powerModeSuccess = changePowerModeSetting(phoneSignal.bedtimeState);
                    if(powerModeSuccess) {
                        Log.d(TAG, "Power Saver mode toggled");
                    } else {
                        Log.d(TAG, "Power Saver mode toggle failed");
                    }
                }

                Log.d(TAG, "vibrate: " + phoneSignal.vibratePref);
                if (phoneSignal.vibratePref) {
                    vibrate();
                }
            }

        } else {
            super.onMessageReceived(messageEvent);
        }
    }

    private void changeDndSetting(NotificationManager mNotificationManager, int newSetting) {

        if (mNotificationManager.isNotificationPolicyAccessGranted()) {
            mNotificationManager.setInterruptionFilter(newSetting);
            Log.d(TAG, "DND set to " + newSetting);
        } else {
            Log.d(TAG, "attempting to set DND but access not granted");
        }

    }

    private boolean changeBedtimeSetting(String settingBedtimeStr, int newSetting) {

        String manufacturer = android.os.Build.MANUFACTURER;
        boolean isSamsung = manufacturer.equalsIgnoreCase(SAMSUNG);

        boolean bedtimeModeSuccess = setGlobalSettingIfPresent(settingBedtimeStr, newSetting);
        boolean zenModeSuccess = setGlobalSettingIfPresent("zen_mode", newSetting);

        if (isSamsung) {
            handler.removeCallbacks(samsungBedtimeLauncher);
            handler.postDelayed(samsungBedtimeLauncher, 1000);
        }
        return bedtimeModeSuccess && zenModeSuccess;
    }

    private boolean setGlobalSettingIfPresent(String settingName, int value) {
        try {
            if (Settings.Global.getString(getContentResolver(), settingName) == null) {
                return true;
            }
            return Settings.Global.putInt(getContentResolver(), settingName, value);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean setSecureSettingIfPresent(String settingName, int value) {
        try {
            if (Settings.Secure.getString(getContentResolver(), settingName) == null) {
                return true;
            }
            return Settings.Secure.putInt(getContentResolver(), settingName, value);
        } catch (SecurityException e) {
            return true;
        }
    }

    private boolean changePowerModeSetting(int newSetting) {

        boolean lowPower = setGlobalSettingIfPresent("low_power", newSetting);
        boolean restrictedDevicePerformance = setGlobalSettingIfPresent("restricted_device_performance", newSetting);
        boolean lowPowerBackDataOff = setGlobalSettingIfPresent("low_power_back_data_off", newSetting);
        boolean smConnectivityDisable = setSecureSettingIfPresent("sm_connectivity_disable", newSetting);

        // screen timeout should be set to 10000 also, and ambient_tilt_to_wake should be set to 0
        // but previous variable states in those 2 cases must be stored and they do not seem to stick
        // and they are not so much important tbh (ambient tilt to wake is disabled anyways)

        return lowPower && restrictedDevicePerformance
                && lowPowerBackDataOff && smConnectivityDisable;
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private void launchSamsungBedtimeUIWithRetry() {

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.google.android.apps.wearable.settings",
                "com.samsung.android.clockwork.settings.advanced.bedtimemode.StBedtimeModeReservedActivity"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
            Log.d(TAG, "Samsung bedtime activity launch requested");

        } catch (Exception e) {
            Log.e(TAG, "Failed to launch Samsung bedtime activity", e);
        }
    }

}
