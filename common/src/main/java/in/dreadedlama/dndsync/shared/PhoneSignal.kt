package `in`.dreadedlama.dndsync.shared

import android.content.SharedPreferences
import java.io.Serializable
import java.time.LocalTime

class PhoneSignal : Serializable {
    // dndState and bedtimeState will be null if the signal to sent is not related
    // to those two states
    var dndState: Int? = null
    var bedtimeState: Int? = null
    var powersavePref: Boolean = false
    var vibratePref: Boolean = false
    var bedtimeNoDndPref: Boolean = false

    var samsungMode: Int? = null

    constructor(prefs: SharedPreferences, samsungMode: Int) {
        this.powersavePref = prefs.getBoolean(PreferenceKeys.PowerSave.key, PreferenceKeys.PowerSave.defaultValue)
        this.vibratePref = prefs.getBoolean(PreferenceKeys.WatchVibrate.key, PreferenceKeys.WatchVibrate.defaultValue)
        this.samsungMode = samsungMode
    }

    constructor(dndState: Int, prefs: SharedPreferences) {
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

            // only treat DND as bedtime inside the configured time window (minutes of the day)
            val windowEnabled = prefs.getBoolean(PreferenceKeys.BedtimeWindow.key, PreferenceKeys.BedtimeWindow.defaultValue)
            val start = prefs.getInt(IntPreferenceKeys.BEDTIME_WINDOW_START, IntPreferenceKeys.BEDTIME_WINDOW_START_DEFAULT)
            val end = prefs.getInt(IntPreferenceKeys.BEDTIME_WINDOW_END, IntPreferenceKeys.BEDTIME_WINDOW_END_DEFAULT)
            val now = LocalTime.now().let { it.hour * 60 + it.minute }
            val inWindow = when {
                !windowEnabled -> true // no time restriction
                start < end -> now in start until end
                else -> now >= start || now < end // window spans midnight
            }

            if (dndAsBedtime && dndState > 1 && inWindow) {
                // dndState > 1 means that it's enabled
                this.bedtimeState = 1
            } else if (dndAsBedtime) {
                // in this branch dndState <= 1 (disabled) or we are outside the time window
                this.bedtimeState = 0
            }
        } else if (dndState == 5 || dndState == 6) {
            // dndState == 5 means bedtime on, dndState == 6 means bedtime off

            this.bedtimeState = if (dndState == 5) 1 else 0
        }
    }

    companion object {
        const val SAMSUNG_MODE_NORMAL: Int = 0
        const val SAMSUNG_MODE_SLEEP: Int = 100
//        const val SAMSUNG_MODE_THEATRE: Int = 105
    }
}
