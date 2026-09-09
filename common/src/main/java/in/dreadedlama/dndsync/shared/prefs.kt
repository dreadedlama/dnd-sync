package `in`.dreadedlama.dndsync.shared

enum class PreferenceKeys(val key: String, val defaultValue: Boolean = true){
    WatchDndSync("watch_dnd_sync_key", false),
    DndAsBedtime("dnd_as_bedtime_key", false),
    BedtimeSync("bedtime_sync_key", true),
    BedtimeNoDnd("bedtime_no_dnd_key", false),
    PowerSave("power_save_key", false),
    DndSync("dnd_sync_key", true),
    WatchVibrate("watch_vibrate_key", false),
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

