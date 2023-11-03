package com.fernandomendoza.dashboardcarrorf.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.fernandomendoza.dashboardcarrorf.R
import com.fernandomendoza.dashboardcarrorf.models.BluetoothDeviceInfo
import com.fernandomendoza.dashboardcarrorf.repository.DeviceRepository
import com.fernandomendoza.dashboardcarrorf.ui.state.ConnectionsScreenState

internal const val CONNECTIONS_SCREEN_ROUTE = "connections"

@Composable
private fun ConnectionsScreen(
    uiState: ConnectionsScreenState,
    onConnectDevice: (String) -> Unit,
    onDisconnectDevice: () -> Unit,
    onScanPermissionGranted: () -> Unit,
    onStartDiscovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scanPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionStatus ->
        if (permissionStatus.values.all { value -> value }) {
            onScanPermissionGranted()
        }
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {

        val isScanPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            val bluetoothGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED

            val locationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            (bluetoothGranted && locationGranted)
        }

        if (isScanPermissionGranted) {
            onScanPermissionGranted()
        } else {
            Log.i("BluetoothConnectionsScreen", "Scan permission is denied, starting request.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                scanPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                    )
                )
            } else {
                scanPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    )
                )
            }
        }
    }

    when (uiState) {
        ConnectionsScreenState.NotSupported -> BluetoothDisabledScreen()
        is ConnectionsScreenState.Connected -> BluetoothEnabledScreen(
            uiState.connectedDevice,
            uiState.availableDevices,
            onDisconnectDevice,
            onConnectDevice,
            onStartDiscovery,
            modifier
        )

        is ConnectionsScreenState.Connecting -> BluetoothEnabledScreen(
            uiState.connectingDevice,
            uiState.availableDevices,
            onDisconnectDevice,
            onConnectDevice,
            onStartDiscovery,
            modifier,
            isConnecting = true,
        )

        is ConnectionsScreenState.Disconnected -> BluetoothEnabledScreen(
            null,
            uiState.availableDevices,
            onDisconnectDevice,
            onConnectDevice,
            onStartDiscovery,
            modifier,
        )
    }
}

@Composable
private fun BluetoothDisabledScreen() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            stringResource(R.string.bluetooth_disabled_hint),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothEnabledScreen(
    connectedDevice: BluetoothDeviceInfo?,
    availableDevices: List<BluetoothDeviceInfo>,
    onDisconnectDevice: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onStartDiscovery: () -> Unit,
    modifier: Modifier = Modifier,
    isConnecting: Boolean = false,
) {
    LazyColumn(modifier) {
        item { Spacer(modifier = Modifier.height(32.dp)) }

        if (connectedDevice != null) {
            item {
                Text(
                    stringResource(R.string.connectedDeviceHeader),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                ListItem(
                    headlineText = {
                        Text(
                            text = connectedDevice.name
                        )
                    },
                    supportingText = {
                        Text(
                            text = connectedDevice.address
                        )
                    },
                    trailingContent = {
                        ElevatedButton(
                            onClick = onDisconnectDevice,
                            enabled = !isConnecting
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Text(stringResource(R.string.disconnect))
                            }
                        }
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text(
                    stringResource(R.string.available_devices),
                    style = MaterialTheme.typography.headlineSmall,
                )

                IconButton(onClick = onStartDiscovery) {
                    Icon(
                        painter = painterResource(R.drawable.refresh),
                        contentDescription = stringResource(R.string.search_devices),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        items(availableDevices) { availableDevice ->
            ListItem(
                headlineText = { Text(availableDevice.name) },
                supportingText = { Text(text = availableDevice.address) },
                trailingContent = {
                    ElevatedButton(onClick = { onConnectDevice(availableDevice.address) }) {
                        Text(stringResource(R.string.connect))
                    }
                }
            )
        }
    }
}

fun NavGraphBuilder.connectionsScreen(
    deviceRepository: DeviceRepository,
    modifier: Modifier = Modifier
) {
    composable(route = CONNECTIONS_SCREEN_ROUTE) {

        val viewModel by remember { mutableStateOf(ConnectionsViewModel(deviceRepository)) }

        val uiState by viewModel.uiState.collectAsState()

//        DisposableEffect(deviceRepository) {
//            viewModel.startDeviceDiscovery()
//
//            onDispose {
//                viewModel.endDeviceDiscovery()
//            }
//        }

        val context = LocalContext.current

        DisposableEffect(context, BluetoothDevice.ACTION_FOUND) {
            val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) =
                    viewModel.onBluetoothBroadcastReceive(
                        intent
                    )
            }

            context.registerReceiver(receiver, intentFilter)

            onDispose {
                context.unregisterReceiver(receiver)
            }
        }

        ConnectionsScreen(
            uiState,
            onConnectDevice = viewModel::connectToDevice,
            onDisconnectDevice = viewModel::disconnectFromDevice,
            onScanPermissionGranted = viewModel::startDeviceDiscovery,
            onStartDiscovery = viewModel::startDeviceDiscovery
        )
    }
}

fun NavController.navigateToConnectionsScreen() {
    navigate(CONNECTIONS_SCREEN_ROUTE)  { launchSingleTop = true }
}