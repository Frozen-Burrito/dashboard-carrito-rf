package com.fernandomendoza.dashboardcarrorf.ui.state

import com.fernandomendoza.dashboardcarrorf.models.GpsData
import com.fernandomendoza.dashboardcarrorf.models.ImuData

sealed interface HomeScreenState {

    object Disconnected: HomeScreenState

    object Loading: HomeScreenState

    data class Connected(
        val radioIsConnected: Boolean,
        val gps: GpsData?,
        val approximateSpeedMetersPerSecond: Double?,
        val batterySoC: Int?,
        val hoursOfBatteryLeft: Float?,
        val imuData: ImuData?,
    ): HomeScreenState
}