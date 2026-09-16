package `in`.dreadedlama.dndsync

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import `in`.dreadedlama.dndsync.shared.PhoneSignal

//Detects Samsung "Modes and Routines" changes by observing the `mode_id` setting.
class SamsungModeDetector(context: Context, private val listener: ModeChangeListener) {

    private val context: Context = context.applicationContext

    private var modeObserver: ContentObserver? = null

    fun interface ModeChangeListener {
        fun onSamsungModeChanged(modeId: Int)
    }

    fun start() {
        if (modeObserver != null) {
            return
        }

        modeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                val modeId = getCurrentMode()
                Log.d(TAG, "Samsung mode changed: $modeId")
                listener.onSamsungModeChanged(modeId)
            }
        }

        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(MODE_ID),
            true,
            modeObserver!!
        )

        // Read the current state once so the initial mode is synchronized.
        val currentMode = getCurrentMode()
        Log.d(TAG, "Initial Samsung mode: $currentMode")
        listener.onSamsungModeChanged(currentMode)
    }

    fun stop() {
        modeObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
            modeObserver = null
        }
    }

    fun getCurrentMode(): Int {
        val value = Settings.Global.getString(context.contentResolver, MODE_ID)
        if (value == null) {
            Log.w(TAG, "mode_id not found in Settings.Global, defaulting to normal")
            return MODE_NORMAL
        }
        Log.d(TAG, "mode_id found in Settings.Global = $value")
        return value.toIntOrNull() ?: MODE_NORMAL
    }

    companion object {
        private const val TAG: String = "SamsungModeDetector"
        private const val MODE_ID: String = "mode_id"

        const val MODE_NORMAL: Int = PhoneSignal.SAMSUNG_MODE_NORMAL
        const val MODE_SLEEP: Int = PhoneSignal.SAMSUNG_MODE_SLEEP
//        const val MODE_THEATRE: Int = PhoneSignal.SAMSUNG_MODE_THEATRE
    }
}
