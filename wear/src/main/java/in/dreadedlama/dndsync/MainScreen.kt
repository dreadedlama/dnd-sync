package `in`.dreadedlama.dndsync

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun MainScreen(context: Context) {
    var dndPermissionGranted = checkDNDPermission(context)
    var secureSettingsPermissionGranted = checkSecureSettingsPermission(context)
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    var isConnected by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        dndPermissionGranted = checkDNDPermission(context)
        secureSettingsPermissionGranted = checkSecureSettingsPermission(context)
        val capabilityClient = Wearable.getCapabilityClient(context)
        val capabilityInfo = capabilityClient
            .getCapability("dnd_sync", CapabilityClient.FILTER_REACHABLE)
            .await()
        isConnected = capabilityInfo.nodes.isNotEmpty()

        Wearable.getCapabilityClient(context).addListener(
            {
                isConnected = it.nodes.isNotEmpty()
            },
            "dnd_sync"
        )
    }

    Page {
        item {
            Text(
                text = stringResource(R.string.permission_header),
                style = MaterialTheme.typography.caption1
            )
        }
        item {
            Spacer(Modifier.height(1.dp))
        }
        item {
            StatusItem(
                stringResource(R.string.dnd_permission_title),
                dndPermissionGranted,
                context,
                icon = {
                    Icon(
                        painter = painterResource(
                            id = if (dndPermissionGranted) {
                                R.drawable.do_not_disturb_on
                            } else {
                                R.drawable.do_not_disturb_off
                            }
                        ),
                        contentDescription = if (isConnected) {
                            stringResource(R.string.dnd_permission_allowed)
                        } else {
                            stringResource(R.string.dnd_permission_not_allowed)
                        }
                    )
                }
            ) {
                dndPermissionGranted = checkDNDPermission(context)
                secureSettingsPermissionGranted = checkSecureSettingsPermission(context)
            }
        }
        item {
            StatusItem(
                stringResource(R.string.secure_settings_permission_title),
                secureSettingsPermissionGranted,
                context,
                icon = {
                    Icon(
                        painter = painterResource(
                            id = if (secureSettingsPermissionGranted) {
                                R.drawable.lock
                            } else {
                                R.drawable.no_encryption
                            }
                        ),
                        contentDescription = stringResource(R.string.secure_settings_permission_title)
                    )
                }
            ) {
                dndPermissionGranted = checkDNDPermission(context)
                secureSettingsPermissionGranted = checkSecureSettingsPermission(context)
            }
        }
        item {
            StatusItem(
                stringResource(R.string.phone_title),
                isConnected,
                context,
                onPermissionClick = null,
                description = stringResource(
                    if (isConnected) {
                        R.string.phone_connected
                    } else {
                        R.string.phone_not_connected
                    }
                ),
                icon = {
                    Icon(
                        painter = painterResource(
                            id = if (isConnected) {
                                R.drawable.mobile_friendly
                            } else {
                                R.drawable.mobile_off
                            }
                        ),
                        contentDescription = if (isConnected) {
                            stringResource(R.string.phone_connected)
                        } else {
                            stringResource(R.string.phone_not_connected)
                        }
                    )
                }
            )
        }
    }
}

/**
 * Composable displaying the status of a permission.
 */
@Composable
fun StatusItem(
    title: String,
    isAvailable: Boolean,
    context: Context,
    description: String? = null,
    icon: @Composable (() -> Unit)? = null,
    onPermissionClick: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val color = if (isAvailable) {
        MaterialTheme.colors.onSurface
    } else {
        MaterialTheme.colors.error
    }

    Chip(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = {
            onPermissionClick?.invoke()
            if (!isAvailable) {
                sendMessageToPhone(coroutineScope, context)
            }
        },
        label = {
            Text(
                text = title,
                color = MaterialTheme.colors.onSurface
            )
        },
        secondaryLabel = {
            Text(
                text = description ?: stringResource(
                    if (isAvailable) {
                        R.string.granted
                    } else {
                        R.string.denied
                    }
                )
            )
        },
        icon = {
            if(icon != null){
                icon()
            } else {
                Icon(
                    painter = painterResource(
                        id = if (isAvailable) {
                            R.drawable.check
                        } else {
                            R.drawable.error
                        }
                    ),
                    contentDescription = if (isAvailable) {
                        "Available"
                    } else {
                        "Unavailable"
                    },
                    tint = color
                )
            }
        },
        colors = ChipDefaults.secondaryChipColors()
    )
}

fun checkDNDPermission(context: Context): Boolean {
    val mNotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
    val allowed = mNotificationManager.isNotificationPolicyAccessGranted;
    return allowed;
}

fun checkSecureSettingsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_SECURE_SETTINGS
    ) == PackageManager.PERMISSION_GRANTED;
}

/**
 * Sends a link to the phone (using Play Services Message Client) to learn how to enable permissions using ADB.
 */
fun sendMessageToPhone(coroutineScope: CoroutineScope, context: Context) {
    coroutineScope.launch(Dispatchers.IO) {
        try {
            val capabilityClient = Wearable.getCapabilityClient(context)
            Log.d("sendMessageToPhone", "Getting capability")
            val capabilityInfo = capabilityClient
                .getCapability("dnd_sync", CapabilityClient.FILTER_REACHABLE)
                .await()
            if (capabilityInfo.nodes.isEmpty()) {
                throw IllegalStateException("No reachable nodes found")
            }

            val messageClient = Wearable.getMessageClient(context)
            val messagePath = "/open_link"
            val messagePayload = byteArrayOf()

            Log.d("sendMessageToPhone", "Sending message")
            // Send the message and wait for the result
            getNodes(context).map {
                val result = messageClient.sendMessage(it, messagePath, messagePayload)
                    .await()
                Log.d("sendMessageToPhone", "Message sent to ${it}: ${result.toString()}")
            }

            // Show toast on the main thread after successful message sending
            launch(Dispatchers.Main) {
                Toast.makeText(context, "Link opened on phone", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("sendMessageToPhone", "Error sending message", e)
            launch(Dispatchers.Main) {
                Toast.makeText(context, "Error sending message", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun pickBestNodeId(nodes: Set<Node>): String? {
    if (nodes.isEmpty()) return null
    // Find a nearby node or pick one arbitrarily.
    return nodes.firstOrNull { it.isNearby }?.id ?: nodes.firstOrNull()?.id
}

private fun getNodes(context: Context): Collection<String> {
    return Tasks.await(Wearable.getNodeClient(context).connectedNodes).map { it.id }
}