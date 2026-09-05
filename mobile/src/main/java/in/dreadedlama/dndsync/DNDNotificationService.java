package in.dreadedlama.dndsync;

import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import in.dreadedlama.dndsync.shared.PhoneSignal;

public class DNDNotificationService extends NotificationListenerService {

    private static final String TAG = "DNDNotificationService";

    private static final String DND_SYNC_MESSAGE_PATH = "/wear-dnd-sync";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        if (isDigitalWellBeingWindDownNotification(sbn)) {
            onNotificationAddedCallDNDSync(sbn);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

        if (isDigitalWellBeingWindDownNotification(sbn)) {

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            boolean syncBedTime = prefs.getBoolean("bedtime_sync_key", true);

            if (!syncBedTime) {
                return;
            }

            Log.d(TAG, "Bedtime mode is OFF");

            int dndState = getNotificationManager().getCurrentInterruptionFilter();

            PhoneSignal signal = new PhoneSignal(dndState, 0, prefs);

            new Thread(() -> sendDNDSync(signal)).start();
        }
    }

    private boolean isDigitalWellBeingWindDownNotification(StatusBarNotification sbn) {

        return sbn.getPackageName().equals("com.google.android.apps.wellbeing")
                && sbn.getNotification().getChannelId().equals("wind_down_notifications");
    }

    private void onNotificationAddedCallDNDSync(StatusBarNotification sbn) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean syncBedTime = prefs.getBoolean("bedtime_sync_key", true);

        if (!syncBedTime) {
            return;
        }

        if (sbn.getNotification().actions == null) {
            return;
        }

        boolean bedTimeModeIsOn = sbn.getNotification().actions.length == 2;
        boolean bedTimeModeIsPaused = sbn.getNotification().actions.length == 1;

        int dndState = getNotificationManager().getCurrentInterruptionFilter();

        if (bedTimeModeIsOn) {

            Log.d(TAG, "Bedtime mode is ON");

            PhoneSignal signal = new PhoneSignal(dndState, 1, prefs);

            new Thread(() -> sendDNDSync(signal)).start();

        } else if (bedTimeModeIsPaused) {

            Log.d(TAG, "Bedtime mode is OFF");

            PhoneSignal signal = new PhoneSignal(dndState, 0, prefs);

            new Thread(() -> sendDNDSync(signal)).start();
        }
    }

    @Override
    public void onInterruptionFilterChanged(int interruptionFilter) {

        Log.d(TAG, "Interruption filter changed to " + interruptionFilter);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean syncDnd = prefs.getBoolean("dnd_sync_key", true);

        if (!syncDnd) {
            return;
        }

        /*
         * Normal DND change.
         *
         * Byte 0 = DND
         * Byte 1 = 2 (NO CHANGE to Bedtime)
         */
        PhoneSignal signal = new PhoneSignal(interruptionFilter, 2, prefs);

        new Thread(() -> sendDNDSync(signal)).start();
    }

    private void sendDNDSync(PhoneSignal phoneSignal) {

        if (phoneSignal.dndState == null || phoneSignal.bedtimeState == null) {
            Log.d(TAG, "Invalid PhoneSignal");
            return;
        }

        byte dndState = (byte) phoneSignal.dndState.intValue();
        byte bedtimeState = (byte) phoneSignal.bedtimeState.intValue();

        Log.d(TAG, "Sending to watch: DND=" + dndState + ", Bedtime=" + bedtimeState);

        Wearable.getDataClient(this).putDataItem(
                PutDataRequest.create(DND_SYNC_MESSAGE_PATH)
                        .setData(new byte[]{dndState, bedtimeState})
                        .setUrgent()
        );
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
}