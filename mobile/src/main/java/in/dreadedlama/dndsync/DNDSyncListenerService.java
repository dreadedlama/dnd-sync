package in.dreadedlama.dndsync;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

public class DNDSyncListenerService extends WearableListenerService {

    private static final String TAG = "DNDSyncListenerService";

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {

        Log.d(TAG, "onDataChanged: " + dataEventBuffer);

        for (DataEvent dataEvent : dataEventBuffer) {

            byte[] data = dataEvent.getDataItem().getData();

            if (data.length < 2) {
                Log.d(TAG, "Invalid sync data. Expected 2 bytes, got " + data.length);
                continue;
            }

            /*
             * Byte 0 = DND
             *
             * 0 = UNKNOWN
             * 1 = ALL
             * 2 = PRIORITY
             * 3 = NONE
             * 4 = ALARMS
             */
            byte dndStateWatch = data[0];

            /*
             * Byte 1 = Bedtime
             *
             * 0 = OFF
             * 1 = ON
             * 2 = NO CHANGE
             */
            byte bedtimeStateWatch = data[1];

            Log.d(TAG, "Received from watch: DND=" + dndStateWatch + ", Bedtime=" + bedtimeStateWatch);

            if (dndStateWatch < 0 || dndStateWatch > 4) {
                Log.d(TAG, "Invalid DND state: " + dndStateWatch);
                continue;
            }

            if (bedtimeStateWatch < 0 || bedtimeStateWatch > 2) {
                Log.d(TAG, "Invalid Bedtime state: " + bedtimeStateWatch);
                continue;
            }

            /*
             * Apply DND received from watch
             * to the PHONE.
             */
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            int currentDndState = notificationManager.getCurrentInterruptionFilter();

            if (dndStateWatch != currentDndState) {

                Log.d(TAG, "Changing phone DND from " + currentDndState + " to " + dndStateWatch);

                if (notificationManager.isNotificationPolicyAccessGranted()) {

                    notificationManager.setInterruptionFilter(dndStateWatch);

                    Log.d(TAG, "DND set to " + dndStateWatch);

                } else {
                    Log.d(TAG, "DND access not granted");
                }
            }

            /*
             * Bedtime from watch.
             *
             * 2 means:
             * don't change phone Bedtime.
             */
            if (bedtimeStateWatch == 1) {

                Log.d(TAG, "Watch Bedtime = ON");

                // Handle phone Bedtime ON if required.

            } else if (bedtimeStateWatch == 0) {

                Log.d(TAG, "Watch Bedtime = OFF");

                // Handle phone Bedtime OFF if required.

            } else {

                Log.d(TAG, "Watch Bedtime = NO CHANGE");
            }
        }
    }
}