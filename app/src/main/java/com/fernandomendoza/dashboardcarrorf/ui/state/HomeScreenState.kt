package com.fernandomendoza.dashboardcarrorf.ui.state

import com.fernandomendoza.dashboardcarrorf.models.GpsData

sealed interface HomeScreenState {

    object Disconnected: HomeScreenState

    object Loading: HomeScreenState

    data class Connected(
        val gps: GpsData?,
        val approximateSpeedMetersPerSecond: Double?,
        val batterySoC: Int?,
        val hoursOfBatteryLeft: Float?,
    ): HomeScreenState
}