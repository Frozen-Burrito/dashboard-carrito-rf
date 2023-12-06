package com.fernandomendoza.dashboardcarrorf.models

data class ImuData(
    val accelerationMetersPerSecond: Double,
    val pitchDegrees: Int,
    val rollDegrees: Int,
)
