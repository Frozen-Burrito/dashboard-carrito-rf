package com.fernandomendoza.dashboardcarrorf.models

sealed interface DashboardState {

    object Disconnected : DashboardState

    object Loading : DashboardState

    data class Loaded(
        val gps: GpsData?,
        val approximateSpeedMetersPerSecond: Double?,
        val batterySoC: Int?,
        val hoursOfBatteryLeft: Float?
    ) : DashboardState
}
