package in.dreadedlama.dndsync;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

public class DNDSyncListenerService extends WearableListenerService {
    private static final String TAG = "DNDSyncListenerService";
    public static final String SAMSUNG = "Samsung";

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "onDataChanged: " + dataEventBuffer);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        for (DataEvent dataEvent : dataEventBuffer) {

            boolean vibrate = prefs.getBoolean("vibrate_key", false);
            if (vibrate) vibrate();

            byte dndStatePhone = getPhoneDndState(dataEvent);
            Log.d(TAG, "dndStatePhone: " + dndStatePhone);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            int filterState = notificationManager.getCurrentInterruptionFilter();
            byte currentDndState = (byte) filterState;

            if (dndStatePhone == 5 || dndStatePhone == 6) {
                setDndState5or6(dndStatePhone, prefs);
            }

            if ((dndStatePhone != currentDndState) && (dndStatePhone != 5 && dndStatePhone != 6)) {
                if (notificationManager.isNotificationPolicyAccessGranted()) {
                    notificationManager.setInterruptionFilter(dndStatePhone);
                    Log.d(TAG, "DND set to " + dndStatePhone);
                } else {
                    Log.d(TAG, "Attempt to set DND failed: Access not granted");
                }
            }
        }
    }

    private void setDndState5or6(byte dndStatePhone, SharedPreferences prefs) {
        boolean useBedtimeMode = prefs.getBoolean("bedtime_key", true);
        int bedTimeModeValue = (dndStatePhone == 5) ? 1 : 0;

        if (!useBedtimeMode) return;

        boolean usePowerSaverMode = prefs.getBoolean("power_saver_key", true);
        if (usePowerSaverMode) {
            setPowerSaveMode(bedTimeModeValue);
        }

        String manufacturer = android.os.Build.MANUFACTURER;
        boolean isSamsung = manufacturer.equalsIgnoreCase(SAMSUNG);

        boolean bedtimeModeSuccess = true;
        boolean zenModeSuccess = false;
        boolean samsungSuccess = true;

        if (isSamsung) {
            // Samsung Watch
            zenModeSuccess = Settings.Global.putInt(getContentResolver(), "zen_mode", bedTimeModeValue);
            samsungSuccess = Settings.Global.putInt(getContentResolver(), "setting_bedtime_mode_running_state", bedTimeModeValue);

        } else {
            // Google / Pixel Watch
            bedtimeModeSuccess = Settings.Global.putInt(getContentResolver(), "bedtime_mode", bedTimeModeValue);
            zenModeSuccess = Settings.Global.putInt(getContentResolver(), "zen_mode", bedTimeModeValue);
        }

        if (bedtimeModeSuccess && zenModeSuccess && samsungSuccess) {
            Log.d(TAG, "Bedtime values written to system settings");

            if (isSamsung) {
                new Handler(Looper.getMainLooper()).postDelayed(this::launchSamsungBedtimeUIWithRetry, 1500);
            }
        } else {
            Log.d(TAG, "Bedtime mode toggle failed");
        }
    }
    private void launchSamsungBedtimeUIWithRetry() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.google.android.apps.wearable.settings",
                "com.samsung.android.clockwork.settings.advanced.bedtimemode.StBedtimeModeReservedActivity"
        ));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
            Log.d(TAG, "Samsung bedtime activity launch requested");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch Samsung bedtime activity", e);
        }
    }

    private void setPowerSaveMode(int value) {
        boolean lowPower = Settings.Global.putInt(getContentResolver(), "low_power", value);
        boolean perfRestricted = Settings.Global.putInt(getContentResolver(), "restricted_device_performance", value);
        boolean backDataOff = Settings.Global.putInt(getContentResolver(), "low_power_back_data_off", value);
        boolean smConnectivity = Settings.Secure.putInt(getContentResolver(), "sm_connectivity_disable", value);

        if (lowPower && perfRestricted && backDataOff && smConnectivity) {
            Log.d(TAG, "Power Saver mode toggled");
        } else {
            Log.d(TAG, "Power Saver mode toggle failed");
        }
    }

    private static byte getPhoneDndState(DataEvent dataEvent) {
         byte[] data = dataEvent.getDataItem().getData();
        return data[0]; // 0 to 6 (including custom modes 5 & 6)
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
