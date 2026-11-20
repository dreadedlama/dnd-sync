package `in`.dreadedlama.dndsync.shared

enum class PreferenceKeys(val key: String, val defaultValue: Boolean = true){
    WatchDndSync("watch_dnd_sync_key", false),
    DndAsBedtime("dnd_as_bedtime_key", false),
    BedtimeSync("bedtime_sync_key", true),
    BedtimeNoDnd("bedtime_no_dnd_key", false),
    PowerSave("power_save_key", false),
    DndSync("dnd_sync_key", true),
    WatchVibrate("watch_vibrate_key", false),
    SamsungModeSync("samsung_mode_sync_key", false),
    BedtimeWindow("bedtime_window_key", false),
}

// Bedtime time window for "Enable Bedtime mode on DND sync", stored as minutes of the day
object IntPreferenceKeys {
    const val BEDTIME_WINDOW_START = "bedtime_window_start_key"
    const val BEDTIME_WINDOW_END = "bedtime_window_end_key"

    // defaults: 22:00 - 08:00
    const val BEDTIME_WINDOW_START_DEFAULT = 22 * 60
    const val BEDTIME_WINDOW_END_DEFAULT = 8 * 60
}

object StringPreferenceKeys {
    // Stores the watch manufacturer. Written once and then treated as immutable.
    const val WATCH_MANUFACTURER = "watch_manufacturer_key"
}

//Wearable MessageClient paths.
object MessagePaths {
    // Mobile -> Watch: request the watch to report its manufacturer.
    const val REQUEST_MANUFACTURER = "/request-manufacturer"
    // Watch -> Mobile: reply carrying the watch manufacturer string.
    const val WATCH_MANUFACTURER = "/watch-manufacturer"
}

