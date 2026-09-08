package `in`.dreadedlama.dndsync.shared

import android.content.SharedPreferences
import java.io.Serializable

class PhoneSignal(dndState: Int, prefs: SharedPreferences) : Serializable {
    // dndState and bedtimeState will be null if the signal to sent is not related
    // to those two states
    var dndState: Int? = null
    var bedtimeState: Int? = null
    var powersavePref: Boolean = false
    var vibratePref: Boolean = false
    var bedtimeNoDndPref: Boolean = false

    init {
        val dndAsBedtime = prefs.getBoolean(PreferenceKeys.DndAsBedtime.key, PreferenceKeys.DndAsBedtime.defaultValue)
        this.powersavePref = prefs.getBoolean(PreferenceKeys.PowerSave.key, PreferenceKeys.PowerSave.defaultValue)
        this.vibratePref = prefs.getBoolean(PreferenceKeys.WatchVibrate.key, PreferenceKeys.WatchVibrate.defaultValue)
        this.bedtimeNoDndPref = prefs.getBoolean(PreferenceKeys.BedtimeNoDnd.key, PreferenceKeys.BedtimeNoDnd.defaultValue)

        // DnD disabled:
        // 0 = INTERRUPTION_FILTER_UNKNOWN
        // 1 = INTERRUPTION_FILTER_ALL

        // DnD enabled:
        // 2 = INTERRUPTION_FILTER_PRIORITY
        // 3 = INTERRUPTION_FILTER_NONE (no notification passes)
        // 4 = INTERRUPTION_FILTER_ALARMS

        // Custom
        // 5 = BEDTIME ON
        // 6 = BEDTIME OFF
        if (0 <= dndState && dndState <= 4) {
            this.dndState = dndState

            if (dndAsBedtime && dndState > 1) {
                // dndState > 1 means that it's enabled
                this.bedtimeState = 1
            } else if (dndAsBedtime) {
                // in this branch dndState < 1, so it's disabled
                this.bedtimeState = 0
            }
        } else if (dndState == 5 || dndState == 6) {
            // dndState == 5 means bedtime on, dndState == 6 means bedtime off

            this.bedtimeState = if (dndState == 5) 1 else 0
        }
    }
}
