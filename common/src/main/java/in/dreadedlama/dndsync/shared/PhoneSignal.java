package in.dreadedlama.dndsync.shared;

import android.content.SharedPreferences;

import java.io.Serializable;

public class PhoneSignal implements Serializable {

    // 0 = UNKNOWN
    // 1 = ALL
    // 2 = PRIORITY
    // 3 = NONE
    // 4 = ALARMS
    public Integer dndState = null;

    // 0 = Bedtime OFF
    // 1 = Bedtime ON
    // 2 = NO CHANGE
    public Integer bedtimeState = 2;
    public boolean powersavePref = false;
    public boolean vibratePref = false;

    public PhoneSignal(Integer dndState, Integer bedtimeState, SharedPreferences prefs) {

        this.powersavePref = prefs.getBoolean("power_save_key", false);
        this.vibratePref = prefs.getBoolean("watch_vibrate_key", false);

        if (dndState != null && dndState >= 0 && dndState <= 4) {
            this.dndState = dndState;
        }

        if (bedtimeState != null && bedtimeState >= 0 && bedtimeState <= 2) {
            this.bedtimeState = bedtimeState;
        }
    }
}