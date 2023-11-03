package com.fernandomendoza.dashboardcarrorf.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernandomendoza.dashboardcarrorf.repository.DeviceRepository
import com.fernandomendoza.dashboardcarrorf.ui.state.ConnectionsScreenState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConnectionsViewModel(private val deviceRepository: DeviceRepository) : ViewModel() {

    val uiState =
        deviceRepository.connectedDevice.combine(deviceRepository.isConnecting) { connectedDevice, isConnecting ->
            if (connectedDevice != null) {
                if (isConnecting) {
                    ConnectionsScreenState.Connecting(
                        connectedDevice,
                        availableDevices = emptyList()
                    )
                } else {
                    ConnectionsScreenState.Connected(
                        connectedDevice,
                        availableDevices = emptyList()
                    )
                }
            } else {
                null
            }
        }.combine(deviceRepository.availableDevices) { screenState, availableDevices ->
            when (screenState) {
                null -> {
                    ConnectionsScreenState.Disconnected(availableDevices)
                }

                is ConnectionsScreenState.Connected -> {
                    screenState.copy(
                        availableDevices = availableDevices
                    )
                }

                is ConnectionsScreenState.Connecting -> {
                    screenState.copy(
                        availableDevices = availableDevices
                    )
                }

                else -> {
                    ConnectionsScreenState.NotSupported
                }
            }
        }
            .stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ConnectionsScreenState.NotSupported
            )

    fun startDeviceDiscovery() {
        viewModelScope.launch {
            deviceRepository.startDiscovery(durationMs = 30_000)
        }
    }

    fun endDeviceDiscovery() = deviceRepository.endDiscovery()

    fun connectToDevice(address: String) {
        viewModelScope.launch {
            deviceRepository.connectToDevice(address)
        }
    }

    fun disconnectFromDevice() = deviceRepository.disconnectFromDevice()

    @SuppressLint("MissingPermission")
    fun onBluetoothBroadcastReceive(intent: Intent?) {
        when (intent?.action) {
            BluetoothDevice.ACTION_FOUND -> {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                } else {
                    // VERSION.SDK_INT < TIRAMISU
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                Log.i(ConnectionsViewModel::class.simpleName, "Found a bluetooth device: ${device?.name}")

                if (device != null) {
                    viewModelScope.launch {
                        deviceRepository.addDiscoveredBluetoothDevice(device)
                    }
                }
            }
        }
    }
}