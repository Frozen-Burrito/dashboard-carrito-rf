package com.fernandomendoza.dashboardcarrorf.repository

import com.fernandomendoza.dashboardcarrorf.datasources.DeviceDataSource
import com.fernandomendoza.dashboardcarrorf.models.BluetoothDeviceInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class DeviceRepository(private val deviceDataSource: DeviceDataSource) {
    val connectedDevice: Flow<BluetoothDeviceInfo?> = deviceDataSource.connectedDevice

    val availableDevices: Flow<List<BluetoothDeviceInfo>> = deviceDataSource.availableDevices

    val isConnecting: StateFlow<Boolean> = deviceDataSource.isConnecting

    val isBluetoothSupported: Boolean = deviceDataSource.isSupported

    fun addDiscoveredBluetoothDevice(device: Any) = deviceDataSource.addDiscoveredDevice(device)

    suspend fun connectToDevice(address: String) = deviceDataSource.connectToDevice(address)

    fun disconnectFromDevice() = deviceDataSource.disconnectFromDevice()

    suspend fun startDiscovery(durationMs: Long = 10_000) = deviceDataSource.startDiscovery(durationMs)

    fun endDiscovery() = deviceDataSource.endDeviceDiscovery()
}