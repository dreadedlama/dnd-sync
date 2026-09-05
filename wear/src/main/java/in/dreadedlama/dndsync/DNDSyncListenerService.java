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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable samsungBedtimeLauncher = this::launchSamsungBedtimeUIWithRetry;

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {

        Log.d(TAG, "onDataChanged: " + dataEventBuffer);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        for (DataEvent dataEvent : dataEventBuffer) {
            byte[] data = dataEvent.getDataItem().getData();
            if (data.length < 2) {
                Log.d(TAG, "Invalid sync data. Expected 2 bytes, got " + data.length);
                continue;
            }
            /*
             * Byte 0 = DND
             */
            byte dndStatePhone = data[0];

            /*
             * Byte 1 = Bedtime
             *
             * 0 = OFF
             * 1 = ON
             * 2 = NO CHANGE
             */
            byte bedtimeStatePhone = data[1];

            Log.d(TAG, "Received from phone: DND=" + dndStatePhone + ", Bedtime=" + bedtimeStatePhone);

            if (dndStatePhone < 0 || dndStatePhone > 4) {
                Log.d(TAG, "Invalid DND state: " + dndStatePhone);
                continue;
            }

            if (bedtimeStatePhone < 0 || bedtimeStatePhone > 2) {
                Log.d(TAG, "Invalid Bedtime state: " + bedtimeStatePhone);
                continue;
            }

            boolean vibrate = prefs.getBoolean("vibrate_key", false);

            if (vibrate) {
                vibrate();
            }

            /*
             * -------------------------
             * BEDTIME
             * -------------------------
             */

            if (bedtimeStatePhone == 1) {
                Log.d(TAG, "Phone Bedtime = ON");
                setBedtimeState(1, prefs);

            } else if (bedtimeStatePhone == 0) {
                Log.d(TAG, "Phone Bedtime = OFF");
                setBedtimeState(0, prefs);

            } else {
                Log.d(TAG, "Phone Bedtime = NO CHANGE");
            }

            /*
             * -------------------------
             * DND
             * -------------------------
             */

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            int currentDndState = notificationManager.getCurrentInterruptionFilter();
            byte currentDndStateByte = (byte) currentDndState;

            if (dndStatePhone != currentDndStateByte) {

                Log.d(TAG, "Changing watch DND from " + currentDndState + " to " + dndStatePhone);

                if (notificationManager.isNotificationPolicyAccessGranted()) {

                    notificationManager.setInterruptionFilter(dndStatePhone);

                    Log.d(TAG, "DND set to " + dndStatePhone);

                } else {
                    Log.d(TAG, "DND access not granted");
                }
            }
        }
    }

    private void setBedtimeState(int bedTimeModeValue, SharedPreferences prefs) {

        boolean useBedtimeMode = prefs.getBoolean("bedtime_key", true);

        if (!useBedtimeMode) {
            return;
        }

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
            // Pixel / Google Watch
            bedtimeModeSuccess = Settings.Global.putInt(getContentResolver(), "bedtime_mode", bedTimeModeValue);
            zenModeSuccess = Settings.Global.putInt(getContentResolver(), "zen_mode", bedTimeModeValue);
        }

        if (bedtimeModeSuccess && zenModeSuccess && samsungSuccess) {

            Log.d(TAG, "Bedtime values written: " + bedTimeModeValue);

            /*
             * ONLY launch Samsung UI when
             * turning Bedtime ON.
             * Never launch it for OFF.
             */
            if (isSamsung && bedTimeModeValue == 1) {
                handler.removeCallbacks(samsungBedtimeLauncher);
                handler.postDelayed(samsungBedtimeLauncher, 1500);
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

    private void vibrate() {

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}