package com.geeksville.mesh.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.twotone.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PermScanWifi
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.geeksville.mesh.ClientOnlyProtos.DeviceProfile
import com.geeksville.mesh.Position
import com.geeksville.mesh.R
import com.geeksville.mesh.android.Logging
import com.geeksville.mesh.config
import com.geeksville.mesh.database.entity.NodeEntity
import com.geeksville.mesh.model.Channel
import com.geeksville.mesh.model.MetricsViewModel
import com.geeksville.mesh.model.RadioConfigViewModel
import com.geeksville.mesh.moduleConfig
import com.geeksville.mesh.service.MeshService.ConnectionState
import com.geeksville.mesh.ui.components.DeviceMetricsScreen
import com.geeksville.mesh.ui.components.EnvironmentMetricsScreen
import com.geeksville.mesh.ui.components.PreferenceCategory
import com.geeksville.mesh.ui.components.SignalMetricsScreen
import com.geeksville.mesh.ui.components.config.AmbientLightingConfigItemList
import com.geeksville.mesh.ui.components.config.AudioConfigItemList
import com.geeksville.mesh.ui.components.config.BluetoothConfigItemList
import com.geeksville.mesh.ui.components.config.CannedMessageConfigItemList
import com.geeksville.mesh.ui.components.config.ChannelSettingsItemList
import com.geeksville.mesh.ui.components.config.DetectionSensorConfigItemList
import com.geeksville.mesh.ui.components.config.DeviceConfigItemList
import com.geeksville.mesh.ui.components.config.DisplayConfigItemList
import com.geeksville.mesh.ui.components.config.EditDeviceProfileDialog
import com.geeksville.mesh.ui.components.config.ExternalNotificationConfigItemList
import com.geeksville.mesh.ui.components.config.LoRaConfigItemList
import com.geeksville.mesh.ui.components.config.MQTTConfigItemList
import com.geeksville.mesh.ui.components.config.NeighborInfoConfigItemList
import com.geeksville.mesh.ui.components.config.NetworkConfigItemList
import com.geeksville.mesh.ui.components.config.PacketResponseStateDialog
import com.geeksville.mesh.ui.components.config.PaxcounterConfigItemList
import com.geeksville.mesh.ui.components.config.PositionConfigItemList
import com.geeksville.mesh.ui.components.config.PowerConfigItemList
import com.geeksville.mesh.ui.components.config.RangeTestConfigItemList
import com.geeksville.mesh.ui.components.config.RemoteHardwareConfigItemList
import com.geeksville.mesh.ui.components.config.SecurityConfigItemList
import com.geeksville.mesh.ui.components.config.SerialConfigItemList
import com.geeksville.mesh.ui.components.config.StoreForwardConfigItemList
import com.geeksville.mesh.ui.components.config.TelemetryConfigItemList
import com.geeksville.mesh.ui.components.config.UserConfigItemList
import com.google.accompanist.themeadapter.appcompat.AppCompatTheme
import dagger.hilt.android.AndroidEntryPoint

internal fun FragmentManager.navigateToRadioConfig(
    destNum: Int? = null,
    startDestination: String = "RadioConfig",
) {
    val radioConfigFragment = DeviceSettingsFragment().apply {
        arguments = bundleOf("destNum" to destNum, "startDestination" to startDestination)
    }
    beginTransaction()
        .replace(R.id.mainActivityLayout, radioConfigFragment)
        .addToBackStack(null)
        .commit()
}

@AndroidEntryPoint
class DeviceSettingsFragment : ScreenFragment("Radio Configuration"), Logging {

    private val model: RadioConfigViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val destNum = arguments?.getInt("destNum")
        val startDestination = arguments?.getString("startDestination") ?: "RadioConfig"
        model.setDestNum(destNum)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorAdvancedBackground))
            setContent {
                val node by model.destNode.collectAsStateWithLifecycle()

                AppCompatTheme {
                    val navController: NavHostController = rememberNavController()
                    // Get current back stack entry
                    // val backStackEntry by navController.currentBackStackEntryAsState()
                    // Get the name of the current screen
                    // val currentScreen = backStackEntry?.destination?.route?.let { route ->
                    //     NavRoute.entries.find { it.name == route }?.title
                    // }

                    Scaffold(
                        topBar = {
                            MeshAppBar(
                                currentScreen = node?.user?.longName
                                    ?: stringResource(R.string.unknown_username),
                                // canNavigateBack = navController.previousBackStackEntry != null,
                                // navigateUp = { navController.navigateUp() },
                                canNavigateBack = true,
                                navigateUp = {
                                    if (navController.previousBackStackEntry != null) {
                                        navController.navigateUp()
                                    } else {
                                        parentFragmentManager.popBackStack()
                                    }
                                },
                            )
                        }
                    ) { innerPadding ->
                        RadioConfigNavHost(
                            node = node,
                            viewModel = model,
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}

enum class AdminRoute(@StringRes val title: Int) {
    REBOOT(R.string.reboot),
    SHUTDOWN(R.string.shutdown),
    FACTORY_RESET(R.string.factory_reset),
    NODEDB_RESET(R.string.nodedb_reset),
}

// Config (configType = AdminProtos.AdminMessage.ConfigType)
enum class ConfigRoute(val title: String, val icon: ImageVector?, val configType: Int = 0) {
    USER("User", Icons.Default.Person, 0),
    CHANNELS("Channels", Icons.AutoMirrored.Default.List, 0),
    DEVICE("Device", Icons.Default.Router, 0),
    POSITION("Position", Icons.Default.LocationOn, 1),
    POWER("Power", Icons.Default.Power, 2),
    NETWORK("Network", Icons.Default.Wifi, 3),
    DISPLAY("Display", Icons.Default.DisplaySettings, 4),
    LORA("LoRa", Icons.Default.CellTower, 5),
    BLUETOOTH("Bluetooth", Icons.Default.Bluetooth, 6),
    SECURITY("Security", Icons.Default.Security, configType = 7),
}

// ModuleConfig (configType = AdminProtos.AdminMessage.ModuleConfigType)
enum class ModuleRoute(val title: String, val icon: ImageVector?, val configType: Int = 0) {
    MQTT("MQTT", Icons.Default.Cloud, 0),
    SERIAL("Serial", Icons.Default.Usb, 1),
    EXTERNAL_NOTIFICATION("External Notification", Icons.Default.Notifications, 2),
    STORE_FORWARD("Store & Forward", Icons.AutoMirrored.Default.Forward, 3),
    RANGE_TEST("Range Test", Icons.Default.Speed, 4),
    TELEMETRY("Telemetry", Icons.Default.DataUsage, 5),
    CANNED_MESSAGE("Canned Message", Icons.AutoMirrored.Default.Message, 6),
    AUDIO("Audio", Icons.AutoMirrored.Default.VolumeUp, 7),
    REMOTE_HARDWARE("Remote Hardware", Icons.Default.SettingsRemote, 8),
    NEIGHBOR_INFO("Neighbor Info", Icons.Default.People, 9),
    AMBIENT_LIGHTING("Ambient Lighting", Icons.Default.LightMode, 10),
    DETECTION_SENSOR("Detection Sensor", Icons.Default.Sensors, 11),
    PAXCOUNTER("Paxcounter", Icons.Default.PermScanWifi, 12),
}

/**
 * Generic sealed class defines each possible state of a response.
 */
sealed class ResponseState<out T> {
    data object Empty : ResponseState<Nothing>()
    data class Loading(var total: Int = 1, var completed: Int = 0) : ResponseState<Nothing>()
    data class Success<T>(val result: T) : ResponseState<T>()
    data class Error(val error: String) : ResponseState<Nothing>()

    fun isWaiting() = this !is Empty
}

@Composable
private fun MeshAppBar(
    currentScreen: String,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text(currentScreen) },
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        null,
                    )
                }
            }
        }
    )
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun RadioConfigNavHost(
    node: NodeEntity?,
    viewModel: RadioConfigViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connected = connectionState == ConnectionState.CONNECTED && node != null

    val radioConfigState by viewModel.radioConfigState.collectAsStateWithLifecycle()
    val isWaiting = radioConfigState.responseState.isWaiting()

    val metricsViewModel: MetricsViewModel = hiltViewModel()
    val metricsState by metricsViewModel.state.collectAsStateWithLifecycle()

    if (isWaiting) {
        PacketResponseStateDialog(
            state = radioConfigState.responseState,
            onDismiss = {
                viewModel.clearPacketResponse()
            },
            onComplete = {
                val route = radioConfigState.route
                if (ConfigRoute.entries.any { it.name == route } ||
                    ModuleRoute.entries.any { it.name == route }) {
                    navController.navigate(route = route)
                    viewModel.clearPacketResponse()
                }
            },
        )
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable("NodeDetails") {
            NodeDetailsScreen(
                node = node,
                metricsState = metricsState,
                onNavigate = { navController.navigate(route = it) },
                setSelectedNode = metricsViewModel::setSelectedNode,
            )
        }
        composable("DeviceMetrics") {
            DeviceMetricsScreen(metricsState.deviceMetrics)
        }
        composable("EnvironmentMetrics") {
            EnvironmentMetricsScreen(
                metricsState.environmentMetrics,
                metricsState.environmentDisplayFahrenheit,
            )
        }
        composable("SignalMetrics") {
            SignalMetricsScreen(metricsState.signalMetrics)
        }
        composable("RadioConfig") {
            RadioConfigScreen(
                node = node,
                enabled = connected && !isWaiting,
                viewModel = viewModel,
            )
        }
        composable(ConfigRoute.USER.name) {
            UserConfigItemList(
                userConfig = radioConfigState.userConfig,
                enabled = connected,
                onSaveClicked = { userInput ->
                    viewModel.setOwner(userInput)
                }
            )
        }
        composable(ConfigRoute.CHANNELS.name) {
            ChannelSettingsItemList(
                settingsList = radioConfigState.channelList,
                modemPresetName = Channel(loraConfig = radioConfigState.radioConfig.lora).name,
                enabled = connected,
                maxChannels = viewModel.maxChannels,
                onPositiveClicked = { channelListInput ->
                    viewModel.updateChannels(channelListInput, radioConfigState.channelList)
                },
            )
        }
        composable(ConfigRoute.DEVICE.name) {
            DeviceConfigItemList(
                deviceConfig = radioConfigState.radioConfig.device,
                enabled = connected,
                onSaveClicked = { deviceInput ->
                    val config = config { device = deviceInput }
                    viewModel.setConfig(config)
                }
            )
        }
        composable(ConfigRoute.POSITION.name) {
            val currentPosition = Position(
                latitude = node?.latitude ?: 0.0,
                longitude = node?.longitude ?: 0.0,
                altitude = node?.position?.altitude ?: 0,
                time = 1, // ignore time for fixed_position
            )
            PositionConfigItemList(
                location = currentPosition,
                positionConfig = radioConfigState.radioConfig.position,
                enabled = connected,
                onSaveClicked = { locationInput, positionInput ->
                    if (positionInput.fixedPosition) {
                        if (locationInput != currentPosition) {
                            viewModel.setFixedPosition(locationInput)
                        }
                    } else {
                        if (radioConfigState.radioConfig.position.fixedPosition) {
                            // fixed position changed from enabled to disabled
                            viewModel.removeFixedPosition()
                        }
                    }
                    val config = config { position = positionInput }
                    viewModel.setConfig(config)
                }
            )
        }
        composable(ConfigRoute.POWER.name) {
            PowerConfigItemList(
                powerConfig = radioConfigState.radioConfig.power,
                enabled = connected,
                onSaveClicked = { powerInput ->
                    val config = config { power = powerInput }
                    viewModel.setConfig(config)
                }
            )
        }
        composable(ConfigRoute.NETWORK.name) {
            NetworkConfigItemList(
                networkConfig = radioConfigState.radioConfig.network,
                enabled = connected,
                onSaveClicked = { networkInput ->
                    val config = config { network = networkInput }
                    viewModel.setConfig(config)
                }
            )
        }
        composable(ConfigRoute.DISPLAY.name) {
            DisplayConfigItemList(
                displayConfig = radioConfigState.radioConfig.display,
                enabled = connected,
                onSaveClicked = { displayInput ->
                    val config = config { display = displayInput }
                    viewModel.setConfig(config)
                }
            )
        }
        composable(ConfigRoute.LORA.name) {
            LoRaConfigItemList(
                loraConfig = radioConfigState.radioConfig.lora,
                primarySettings = radioConfigState.channelList.getOrNull(0) ?: return@composable,
                enabled = connected,
                onSaveClicked = { loraInput ->
                    val config = config { lora = loraInput }
                    viewModel.setConfig(config)
                },
                hasPaFan = viewModel.hasPaFan,
            )
        }
        composable(ConfigRoute.BLUETOOTH.name) {
            BluetoothConfigItemList(
                bluetoothConfig = radioConfigState.radioConfig.bluetooth,
                enabled = connected,
                onSaveClicked = { bluetoothInput ->
                    val config = config { bluetooth = bluetoothInput }
                    viewModel.setConfig(config)
                }
            )
        }
        composable(ConfigRoute.SECURITY.name) {
            SecurityConfigItemList(
                securityConfig = radioConfigState.radioConfig.security,
                enabled = connected,
                onConfirm = { securityInput ->
                    val config = config { security = securityInput }
                    viewModel.setConfig(config)
                }
            )
        }
        composable(ModuleRoute.MQTT.name) {
            MQTTConfigItemList(
                mqttConfig = radioConfigState.moduleConfig.mqtt,
                enabled = connected,
                onSaveClicked = { mqttInput ->
                    val config = moduleConfig { mqtt = mqttInput }
                    viewModel.setModuleConfig(config)
                }
            )
        }
        composable(ModuleRoute.SERIAL.name) {
            SerialConfigItemList(
                serialConfig = radioConfigState.moduleConfig.serial,
                enabled = connected,
                onSaveClicked = { serialInput ->
                    val config = moduleConfig { serial = serialInput }
                    viewModel.setModuleConfig(config)
                }
            )
        }
        composable(ModuleRoute.EXTERNAL_NOTIFICATION.name) {
            ExternalNotificationConfigItemList(
                ringtone = radioConfigState.ringtone,
                extNotificationConfig = radioConfigState.moduleConfig.externalNotification,
                enabled = connected,
                onSaveClicked = { ringtoneInput, extNotificationInput ->
                    if (ringtoneInput != radioConfigState.ringtone) {
                        viewModel.setRingtone(ringtoneInput)
                    }
                    if (extNotificationInput != radioConfigState.moduleConfig.externalNotification) {
                        val config = moduleConfig { externalNotification = extNotificationInput }
                        viewModel.setModuleConfig(config)
                    }
                }
            )
        }
        composable(ModuleRoute.STORE_FORWARD.name) {
            StoreForwardConfigItemList(
                storeForwardConfig = radioConfigState.moduleConfig.storeForward,
                enabled = connected,
                onSaveClicked = { storeForwardInput ->
                    val config = moduleConfig { storeForward = storeForwardInput }
                    viewModel.setModuleConfig(config)
                }
            )
        }
        composable(ModuleRoute.RANGE_TEST.name) {
            RangeTestConfigItemList(
                rangeTestConfig = radioConfigState.moduleConfig.rangeTest,
                enabled = connected,
                onSaveClicked = { rangeTestInput ->
                    val config = moduleConfig { rangeTest = rangeTestInput }
                    viewModel.setModuleConfig(config)
                }
            )
        }
        composable(ModuleRoute.TELEMETRY.name) {
            TelemetryConfigItemList(
                telemetryConfig = radioConfigState.moduleConfig.telemetry,
                enabled = connected,
                onSaveClicked = { telemetryInput ->
                    val config = moduleConfig { telemetry = telemetryInput }
                    viewModel.setModuleConfig(config)
                }
            )
        }
        composable(ModuleRoute.CANNED_MESSAGE.name) {
            CannedMessageConfigItemList(
                messages = radioConfigState.cannedMessageMessages,
                cannedMessageConfig = radioConfigState.moduleConfig.cannedMessage,
                enabled = connected,
                onSaveClicked = { messagesInput, cannedMessageInput ->
                    if (messagesInput != radioConfigState.cannedMessageMessages) {
                        viewModel.setCannedMessages(messagesInput)
                    }
                    if (cannedMessageInput != radioConfigState.moduleConfig.cannedMessage) {
                        val config = moduleConfig { cannedMessage = cannedMessageInput }
                        viewModel.setModuleConfig(config)
                    }
                }
            )
        }
        composable(ModuleRoute.AUDIO.name) {
            AudioConfigItemList(
                audioConfig = radioConfigState.moduleConfig.audio,
                enabled = connected,
                onSaveClicked = { audioInput ->
                    val config = moduleConfig { audio = audioInput }
                    viewModel.setModuleConfig(config)
                }
            )
        }
        composable(ModuleRoute.REMOTE_HARDWARE.name) {
            RemoteHardwareConfigItemList(
                remoteHardwareConfig = radioConfigState.moduleConfig.remoteHardware,
                enabled = connected,
                onSaveClicked = { remoteHardwareInput ->
                    val config = moduleConfig { remoteHardware = remoteHardwareInput }
                    viewModel.setModuleConfig(config)
                }
            )
        }
        composable(ModuleRoute.NEIGHBOR_INFO.name) {
            NeighborInfoConfigItemList(
                neighborInfoConfig = radioConfigState.moduleConfig.neighborInfo,
                enabled = connected,
                onSaveClicked = { neighborInfoInput ->
                    val config = moduleConfig { neighborInfo = neighborInfoInput }
                    viewModel.setModuleConfig(config)
                }
            )
        }
        composable(ModuleRoute.AMBIENT_LIGHTING.name) {
            AmbientLightingConfigItemList(
                ambientLightingConfig = radioConfigState.moduleConfig.ambientLighting,
                enabled = connected,
                onSaveClicked = { ambientLightingInput ->
                    val config = moduleConfig { ambientLighting = ambientLightingInput }
                    viewModel.setModuleConfig(config)
                }
            )
        }
        composable(ModuleRoute.DETECTION_SENSOR.name) {
            DetectionSensorConfigItemList(
                detectionSensorConfig = radioConfigState.moduleConfig.detectionSensor,
                enabled = connected,
                onSaveClicked = { detectionSensorInput ->
                    val config = moduleConfig { detectionSensor = detectionSensorInput }
                    viewModel.setModuleConfig(config)
                }
            )
        }
        composable(ModuleRoute.PAXCOUNTER.name) {
            PaxcounterConfigItemList(
                paxcounterConfig = radioConfigState.moduleConfig.paxcounter,
                enabled = connected,
                onSaveClicked = { paxcounterConfigInput ->
                    val config = moduleConfig { paxcounter = paxcounterConfigInput }
                    viewModel.setModuleConfig(config)
                }
            )
        }
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun RadioConfigScreen(
    node: NodeEntity?,
    enabled: Boolean,
    viewModel: RadioConfigViewModel,
    modifier: Modifier = Modifier,
) {
    val isLocal = node?.num == viewModel.myNodeNum

    var deviceProfile by remember { mutableStateOf<DeviceProfile?>(null) }
    var showEditDeviceProfileDialog by remember { mutableStateOf(false) }

    val importConfigLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            showEditDeviceProfileDialog = true
            it.data?.data?.let { uri ->
                viewModel.importProfile(uri) { profile -> deviceProfile = profile }
            }
        }
    }

    val exportConfigLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri -> viewModel.exportProfile(uri, deviceProfile!!) }
        }
    }

    if (showEditDeviceProfileDialog) {
        EditDeviceProfileDialog(
            title = if (deviceProfile != null) "Import configuration" else "Export configuration",
            deviceProfile = deviceProfile ?: viewModel.currentDeviceProfile,
            onConfirm = {
                showEditDeviceProfileDialog = false
                if (deviceProfile != null) {
                    viewModel.installProfile(it)
                } else {
                    deviceProfile = it
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/*"
                        putExtra(Intent.EXTRA_TITLE, "${node!!.num.toUInt()}.cfg")
                    }
                    exportConfigLauncher.launch(intent)
                }
            },
            onDismiss = {
                showEditDeviceProfileDialog = false
                deviceProfile = null
            }
        )
    }

    RadioConfigItemList(
        enabled = enabled,
        isLocal = isLocal,
        modifier = modifier,
        onRouteClick = { route ->
            viewModel.setResponseStateLoading(route)
        },
        onImport = {
            viewModel.clearPacketResponse()
            deviceProfile = null
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/*"
            }
            importConfigLauncher.launch(intent)
        },
        onExport = {
            viewModel.clearPacketResponse()
            deviceProfile = null
            showEditDeviceProfileDialog = true
        },
    )
}

@Composable
fun NavCard(
    title: String,
    enabled: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val color = if (enabled) {
        MaterialTheme.colors.onSurface
    } else {
        MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(enabled = enabled) { onClick() },
        elevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = color,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                color = color,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.TwoTone.KeyboardArrowRight, "trailingIcon",
                modifier = Modifier.wrapContentSize(),
                tint = color,
            )
        }
    }
}

@Composable
private fun NavButton(@StringRes title: Int, enabled: Boolean, onClick: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.background,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Warning,
                    contentDescription = "warning",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "${stringResource(title)}?\n")
                Icon(
                    imageVector = Icons.TwoTone.Warning,
                    contentDescription = "warning",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = { showDialog = false },
                ) { Text(stringResource(R.string.cancel)) }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showDialog = false
                        onClick()
                    },
                ) { Text(stringResource(R.string.send)) }
            }
        }
    )

    Column {
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = enabled,
            onClick = { showDialog = true },
        ) { Text(text = stringResource(title)) }
    }
}

@Composable
private fun RadioConfigItemList(
    enabled: Boolean = true,
    isLocal: Boolean = true,
    modifier: Modifier = Modifier,
    onRouteClick: (Enum<*>) -> Unit = {},
    onImport: () -> Unit = {},
    onExport: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        item { PreferenceCategory(stringResource(R.string.device_settings)) }
        items(ConfigRoute.entries) {
            NavCard(title = it.title, icon = it.icon, enabled = enabled) { onRouteClick(it) }
        }

        item { PreferenceCategory(stringResource(R.string.module_settings)) }
        items(ModuleRoute.entries) {
            NavCard(title = it.title, icon = it.icon, enabled = enabled) { onRouteClick(it) }
        }

        if (isLocal) {
            item {
                PreferenceCategory("Backup & Restore")
                NavCard(
                    title = "Import configuration",
                    icon = Icons.Default.Download,
                    enabled = enabled,
                    onClick = onImport,
                )
                NavCard(
                    title = "Export configuration",
                    icon = Icons.Default.Upload,
                    enabled = enabled,
                    onClick = onExport,
                )
            }
        }

        items(AdminRoute.entries) { NavButton(it.title, enabled) { onRouteClick(it) } }
    }
}

@Preview(showBackground = true)
@Composable
private fun RadioSettingsScreenPreview() {
    RadioConfigItemList()
}
