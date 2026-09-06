package `in`.dreadedlama.dndsync

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import `in`.dreadedlama.dndsync.shared.PreferenceKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ViewModel to manage states and logic
class MainViewModel(val app: Application) : AndroidViewModel(app) {
    val preferencesHelper = PreferencesHelper(app)

    // State management using MutableStateFlow
    private val _dndAsBedtime = MutableStateFlow(false)
    val dndAsBedtime: StateFlow<Boolean> = _dndAsBedtime

    private val _bedtimeSync = MutableStateFlow(false)
    val bedtimeSync: StateFlow<Boolean> = _bedtimeSync

    private val _powerSaveEnabled = MutableStateFlow(false)
    val powerSaveEnabled: StateFlow<Boolean> = _powerSaveEnabled

    private val _dndPermissionGranted = MutableStateFlow(checkDNDPermission())
    val dndPermissionGranted: StateFlow<Boolean> = _dndPermissionGranted

    // Notification State
    private val _notificationState = MutableStateFlow(false)
    val notificationState: StateFlow<Boolean> = _notificationState

    // Dnd Sync State
    private val _dndSync = MutableStateFlow(false)
    val dndSync: StateFlow<Boolean> = _dndSync

    // Watch Sync State
    private val _watchSync = MutableStateFlow(false)
    val watchSync: StateFlow<Boolean> = _watchSync

    // Watch Vibrate State
    private val _watchVibrate = MutableStateFlow(false)
    val watchVibrate: StateFlow<Boolean> = _watchVibrate

    // Connectivity state
    private val _connectivityState = MutableStateFlow(false)
    val connectivityState: StateFlow<Boolean> = _connectivityState

    init {
        // Update power save state based on dndAsBedtime or bedtimeSync
        viewModelScope.launch {
            initiateStates()
        }
    }

    fun initiateStates() {
        _dndAsBedtime.value = preferencesHelper.getValue(PreferenceKeys.DndAsBedtime)
        _bedtimeSync.value = preferencesHelper.getValue(PreferenceKeys.BedtimeSync)
        _powerSaveEnabled.value = preferencesHelper.getValue(PreferenceKeys.PowerSave)
        _dndSync.value = preferencesHelper.getValue(PreferenceKeys.DndSync)
        _watchSync.value = preferencesHelper.getValue(PreferenceKeys.WatchDndSync)
        _watchVibrate.value = preferencesHelper.getValue(PreferenceKeys.WatchVibrate)

        _dndPermissionGranted.value = checkDNDPermission()
        _notificationState.value = isNotificationListenerEnabled(app)
        updateConnectivityState()
    }

    fun updateConnectivityState() {
        viewModelScope.launch {
            _connectivityState.value = getConnectivityState()
            Wearable.getCapabilityClient(app).addListener(
                {
                    viewModelScope.launch {
                        _connectivityState.value = getConnectivityState()
                    }
                },
                "dnd_sync"
            )
        }
    }

    suspend fun getConnectivityState(): Boolean {
        val capabilityInfo = Wearable.getCapabilityClient(app).getCapability(
            "dnd_sync",
            CapabilityClient.FILTER_REACHABLE
        ).await()

        return capabilityInfo.nodes.isNotEmpty()
    }


//
//    private suspend fun combineStates() {
//        _dndAsBedtime.collect { updatePowerSaveState() }
//        _bedtimeSync.collect { updatePowerSaveState() }
//    }

    fun setPowerSaveState(value: Boolean) {
        _powerSaveEnabled.value = value
        preferencesHelper.setValue(PreferenceKeys.PowerSave, value)
    }

    fun setDndAsBedtime(value: Boolean) {
        _dndAsBedtime.value = value
        preferencesHelper.setValue(PreferenceKeys.DndAsBedtime, value)
    }

    fun setBedtimeSync(value: Boolean) {
        _bedtimeSync.value = value
        preferencesHelper.setValue(PreferenceKeys.BedtimeSync, value)
    }

    fun setDndSync(value: Boolean) {
        _dndSync.value = value
        preferencesHelper.setValue(PreferenceKeys.DndSync, value)
    }

    fun setWatchSync(value: Boolean) {
        _watchSync.value = value
        preferencesHelper.setValue(PreferenceKeys.WatchDndSync, value)
    }

    fun setWatchVibrate(value: Boolean) {
        _watchVibrate.value = value
        preferencesHelper.setValue(PreferenceKeys.WatchVibrate, value)
    }

    fun requestDNDPermission() {
        _dndPermissionGranted.value = checkDNDPermission()
        if (!_dndPermissionGranted.value) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            app.startActivity(intent)
        }
    }

    fun requestNotificationPermission() {
        _notificationState.value = isNotificationListenerEnabled(app)
        if (_notificationState.value == false) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            app.startActivity(intent)
        }
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val enabledListeners =
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabledListeners != null && enabledListeners.contains(packageName)
    }

    private fun checkDNDPermission(): Boolean {
        val notificationManager =
            app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }
}