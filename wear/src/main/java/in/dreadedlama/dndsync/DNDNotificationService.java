package in.dreadedlama.dndsync;

import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import in.dreadedlama.dndsync.shared.WearSignal;

public class DNDNotificationService extends NotificationListenerService {

    private static final String TAG = "DNDNotificationService";
    private static final String DND_SYNC_MESSAGE_PATH = "/wear-dnd-sync";

    @Override
    public void onInterruptionFilterChanged(int interruptionFilter) {

        Log.d(TAG, "Interruption filter changed to " + interruptionFilter);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean syncDnd = prefs.getBoolean("dnd_sync_key", true);

        if (!syncDnd) {
            return;
        }

        /*
         * DND changed on the WATCH.
         *
         * We are not changing Bedtime.
         *
         * Byte 0 = DND
         * Byte 1 = 2 (NO CHANGE)
         */
        WearSignal signal = new WearSignal(interruptionFilter, 2);

        new Thread(() -> sendDNDSync(signal)).start();
    }

    private void sendDNDSync(WearSignal wearSignal) {

        if (wearSignal.dndState == null || wearSignal.bedtimeState == null) {
            Log.d(TAG, "Invalid WearSignal");
            return;
        }

        byte dndState = (byte) wearSignal.dndState.intValue();
        byte bedtimeState = (byte) wearSignal.bedtimeState.intValue();

        Log.d(TAG, "Sending to phone: DND=" + dndState + ", Bedtime=" + bedtimeState);

        Wearable.getDataClient(this).putDataItem(
                PutDataRequest.create(DND_SYNC_MESSAGE_PATH)
                        .setData(new byte[]{dndState, bedtimeState})
                        .setUrgent()
        );
    }
}