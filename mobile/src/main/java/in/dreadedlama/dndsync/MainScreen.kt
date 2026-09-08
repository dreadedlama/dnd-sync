package `in`.dreadedlama.dndsync

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(context: Context) {
    val viewModel = viewModel<MainViewModel>()
    val dndAsBedtime by viewModel.dndAsBedtime.collectAsState()
    val bedtimeSync by viewModel.bedtimeSync.collectAsState()
    val bedtimeNoDnd by viewModel.bedtimeNoDnd.collectAsState()
    val powerSaveEnabled by viewModel.powerSaveEnabled.collectAsState()
    val dndPermissionGranted by viewModel.dndPermissionGranted.collectAsState()
    val notificationState by viewModel.notificationState.collectAsState()
    val dndSync by viewModel.dndSync.collectAsState()
    val connectivityState by viewModel.connectivityState.collectAsState()
    val permissionsGranted = dndPermissionGranted && notificationState
    var isDialogOpen by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val state by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(state){
        viewModel.initiateStates()
    }

    if (isDialogOpen) {
        AlertDialog(
            icon = {
                Icon(Icons.Rounded.Notifications, contentDescription = "Notifications")
            },
            title = {
                Text(text = stringResource(R.string.notifications_permission_title), textAlign = TextAlign.Center)
            },
            text = {
                Text(text = stringResource(R.string.about_notification_permission_desc), textAlign = TextAlign.Center)
            },
            onDismissRequest = {
                isDialogOpen = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDialogOpen = false
                        viewModel.requestNotificationPermission()
                    }
                ) {
                    Text(
                        stringResource(
                            android.R.string.ok
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isDialogOpen = false
                    }
                ) {
                    Text(
                        stringResource(
                            android.R.string.cancel
                        )
                    )
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 10.dp),
                    ){
                        Icon(
                            painterResource(R.drawable.github),
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = "Github",
                            modifier = Modifier.clip(CircleShape).clickable {
                                val url = "https://github.com/dreadedlama/dnd-sync"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }.padding(4.dp).size(25.dp)
                        )
                    }
                }
            )
        },
        content = { padding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                ConfigurationGroup(R.string.phone_sync_header){
                    // DND as Bedtime Switch
                    item(
                        leadingText = R.string.sync_dnd_title,
                        //supportingText = R.string.sync_dnd_desc,
                        icon = {
                            Icon(
                                painterResource(R.drawable.do_not_disturb_on),
                                contentDescription = "Do Not Disturb"
                            )
                        },
                        checked = dndSync,
                        onCheckedChange = { viewModel.setDndSync(it) },
                        enabled = permissionsGranted
                    )

                    item(
                        leadingText = R.string.sync_dnd_as_bedtime_title,
                        supportingText = R.string.sync_dnd_as_bedtime_desc,
                        icon = {
                            Icon(
                                painterResource(R.drawable.rule_settings),
                                contentDescription = "Bedtime"
                            )
                        },
                        checked = dndAsBedtime,
                        onCheckedChange = { viewModel.setDndAsBedtime(it) },
                        enabled = dndSync && permissionsGranted,
                    )

                    // Bedtime Sync Switch
                    item(
                        leadingText = R.string.sync_bedtime_title,
                        //supportingText = R.string.sync_bedtime_desc,
                        icon = {
                            Icon(painterResource(R.drawable.bedtime), contentDescription = "Bedtime")
                        },
                        checked = bedtimeSync,
                        onCheckedChange = {
                            viewModel.setBedtimeSync(it)
                        },
                        enabled = permissionsGranted
                    )

                    // Bedtime only (No DND) Switch
                    item(
                        leadingText = R.string.bedtime_no_dnd_title,
                        supportingText = R.string.bedtime_no_dnd_desc,
                        icon = {
                            Icon(painterResource(R.drawable.bedtime), contentDescription = "Bedtime only")
                        },
                        checked = bedtimeNoDnd,
                        onCheckedChange = {
                            viewModel.setBedtimeNoDnd(it)
                        },
                        enabled = bedtimeSync && permissionsGranted
                    )

                    // Power Save Switch
                    item(
                        leadingText = R.string.enable_power_saving_title,
                        //supportingText = R.string.sync_bedtime_desc,
                        icon = {
                            Icon(painterResource(R.drawable.battery_saver), contentDescription = "Power Saving")
                        },
                        checked = powerSaveEnabled,
                        onCheckedChange = {
                            viewModel.setPowerSaveState(it)
                        },
                        enabled = bedtimeSync && permissionsGranted
                    )
                }

                val watchSync = viewModel.watchSync.collectAsState().value
                val watchVibrate by viewModel.watchVibrate.collectAsState()
                ConfigurationGroup(R.string.watch_sync_header) {
                    // Watch Sync Switch
                    item(
                        leadingText = R.string.watch_sync_dnd_title,
                        //supportingText = R.string.watch_sync_dnd_desc,
                        icon = {
                            Icon(
                                painterResource(R.drawable.do_not_disturb_on),
                                contentDescription = "Do Not Disturb"
                            )
                        },
                        checked = watchSync,
                        onCheckedChange = {
                            viewModel.setWatchSync(it)
                        },
                        enabled = permissionsGranted
                    )

                    // Watch Vibrate Switch
                    item(
                        leadingText = R.string.watch_vibrate_title,
                        supportingText = R.string.watch_vibrate_desc,
                        icon = {
                            Icon(
                                painterResource(R.drawable.watch_vibration),
                                contentDescription = "Vibration"
                            )
                        },
                        checked = watchVibrate,
                        onCheckedChange = {
                            viewModel.setWatchVibrate(it)
                        }
                    )
                }

                var loading by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                // DND Permission Setting
                ConfigurationGroup(R.string.permission_header){
                    item(
                        icon = {
                            Icon(Icons.Rounded.Notifications, contentDescription = "Notifications")
                        },
                        leadingText = R.string.notifications_permission_title,
                        supportingText = if (notificationState) R.string.notifications_permission_allowed
                        else R.string.notifications_permission_not_allowed,
                        onCheckedChange = {
                            isDialogOpen = true
                        },
                        enabled = !notificationState,
                        checked = notificationState
                    )
                    item(
                        icon = {
                            if(loading) CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                                else Icon(
                                painterResource(
                                    if (connectivityState) R.drawable.watch_check
                                    else R.drawable.watch
                                ),
                                contentDescription = "Connectivity"
                            )
                        },
                        leadingText = R.string.connectivity_state_title,
                        supportingText = if (connectivityState) R.string.connectivity_state_connected
                        else R.string.connectivity_state_disconnected,
                        enabled = true,
                        onCheckedChange = {
                            coroutineScope.launch {
                                loading = true
                                viewModel.updateConnectivityState()
                                delay(1000)
                                loading = false
                            }
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun ConfigurationGroup(
    title: Int,
    content: ConfigurationGroupScopeImpl.() -> Unit
) {
    val items = mutableListOf<@Composable (Int, Int) -> Unit>()

    CategoryTitle(title)
    ConfigurationGroupScopeImpl(items).content()
    items.forEachIndexed { i, item -> item(i, items.size) }
}

class ConfigurationGroupScopeImpl(
    private val items: MutableList<@Composable (Int, Int) -> Unit>
) {
    fun item(
        checked: Boolean? = null,
        onCheckedChange: (Boolean) -> Unit,
        leadingText: Int,
        supportingText: Int? = null,
        icon: @Composable (() -> Unit)? = null,
        enabled: Boolean = true,
    ) {
        items.add { index, total ->
            ConfigurationItem(
                checked, onCheckedChange, leadingText,
                supportingText, icon, enabled, index, total
            )
        }
    }
}

@Composable
fun ConfigurationItem(
    checked: Boolean? = null,
    onCheckedChange: (Boolean) -> Unit,
    leadingText: Int,
    supportingText: Int? = null,
    icon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    index: Int = 0,
    total: Int = 1
) {
    val large = MaterialTheme.shapes.large.topStart
    val small = MaterialTheme.shapes.small.topStart
    val clip = if (total <= 1) RoundedCornerShape(large) else RoundedCornerShape(
        topStart = if (index == 0) large else small,
        topEnd = if (index == 0) large else small,
        bottomStart = if (index == total - 1) large else small,
        bottomEnd = if (index == total - 1) large else small
    )

    val interactionSource = remember { MutableInteractionSource() }
    val modifier = Modifier
        .padding(vertical = 2.dp, horizontal = 12.dp)
        .clip(clip)
        .clickable(
            enabled = enabled,
            onClick = {
                onCheckedChange(checked != true)
            },
            interactionSource = interactionSource,
            indication = ripple()
        )

    ListItem(
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        headlineContent = { Text(stringResource(leadingText)) },
        supportingContent = {
            if (supportingText != null) {
                Text(stringResource(supportingText))
            }
        },
        trailingContent = {
            if (checked != null) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled,
                    interactionSource = interactionSource
                )
            }
        },
        leadingContent = {
            if (icon != null) {
                icon()
            }
        },
        modifier = modifier
    )
}

@Composable
fun CategoryTitle(title: Int) {
    Text(
        stringResource(title), style = MaterialTheme.typography.labelLarge.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        ),
        textAlign = TextAlign.Start,
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
    )
}
