package in.dreadedlama.dndsync;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.apache.commons.lang3.SerializationUtils;

import in.dreadedlama.dndsync.shared.WearSignal;

public class DNDSyncListenerService extends WearableListenerService {
    private static final String TAG = "DNDSyncListenerService";
    private static final String DND_SYNC_MESSAGE_PATH = "/wear-dnd-sync";

    @Override
    public void onMessageReceived (@NonNull MessageEvent messageEvent) {

        if (messageEvent.getPath().equalsIgnoreCase(DND_SYNC_MESSAGE_PATH)) {

            Log.d(TAG, "received path: " + DND_SYNC_MESSAGE_PATH);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            byte[] data = messageEvent.getData();
            WearSignal wearSignal = SerializationUtils.deserialize(data);
            int dndStateWear = wearSignal.dndState;

            Log.d(TAG, "dndStateWear: " + dndStateWear);

            // get dnd state
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            int currentDndState = mNotificationManager.getCurrentInterruptionFilter();

            Log.d(TAG, "currentDndState: " + currentDndState);
            if (currentDndState < 0 || currentDndState > 4) {
                Log.d(TAG, "Current DND state it's weird, should be in range [0,4]");
            }

            boolean shouldSync = prefs.getBoolean("watch_dnd_sync_key", false);

            if (currentDndState != dndStateWear && shouldSync) {
                Log.d(TAG, "currentDndState != dndStateWear: " + currentDndState + " != " + dndStateWear);
                if (mNotificationManager.isNotificationPolicyAccessGranted()) {
                    mNotificationManager.setInterruptionFilter(dndStateWear);
                    Log.d(TAG, "DND set to " + dndStateWear);
                } else {
                    Log.d(TAG, "attempting to set DND but access not granted");
                }
            }

        } else {
            super.onMessageReceived(messageEvent);
        }
    }

}
