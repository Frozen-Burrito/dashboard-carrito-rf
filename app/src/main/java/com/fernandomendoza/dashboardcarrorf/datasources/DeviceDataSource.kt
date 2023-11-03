package com.fernandomendoza.dashboardcarrorf.datasources

import com.fernandomendoza.dashboardcarrorf.models.BluetoothDeviceInfo
import com.fernandomendoza.dashboardcarrorf.models.BluetoothDeviceResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface DeviceDataSource {

    val connectedDevice: Flow<BluetoothDeviceInfo?>

    val availableDevices: Flow<List<BluetoothDeviceInfo>>

    val isConnecting: StateFlow<Boolean>

    val isSupported: Boolean

    suspend fun startDiscovery(durationMs: Long = 10_000): Boolean

    fun endDeviceDiscovery(): Boolean

    fun addDiscoveredDevice(device: Any)

    suspend fun connectToDevice(deviceAddress: String)

    fun disconnectFromDevice()

    suspend fun listen(bufferSize: Int = 32, onResponse: (BluetoothDeviceResult) -> Unit)

    fun cancel()
}