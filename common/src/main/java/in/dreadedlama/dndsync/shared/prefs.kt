package `in`.dreadedlama.dndsync.shared

enum class PreferenceKeys(val key: String, val defaultValue: Boolean = true){
    WatchDndSync("watch_dnd_sync_key", false),
    DndAsBedtime("dnd_as_bedtime_key", false),
    BedtimeSync("bedtime_sync_key", true),
    PowerSave("power_save_key", false),
    DndSync("dnd_sync_key", true),
    WatchVibrate("watch_vibrate_key", false),
}
