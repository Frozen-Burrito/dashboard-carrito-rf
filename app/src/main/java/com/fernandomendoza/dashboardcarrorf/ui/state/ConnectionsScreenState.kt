package com.fernandomendoza.dashboardcarrorf.ui.state

import com.fernandomendoza.dashboardcarrorf.models.BluetoothDeviceInfo

sealed interface ConnectionsScreenState {
    object NotSupported: ConnectionsScreenState

    data class Disconnected(
        val availableDevices: List<BluetoothDeviceInfo>
    ): ConnectionsScreenState

    data class Connecting(
        val connectingDevice: BluetoothDeviceInfo,
        val availableDevices: List<BluetoothDeviceInfo>
    ): ConnectionsScreenState

    data class Connected(
        val connectedDevice: BluetoothDeviceInfo,
        val availableDevices: List<BluetoothDeviceInfo>
    ): ConnectionsScreenState
}