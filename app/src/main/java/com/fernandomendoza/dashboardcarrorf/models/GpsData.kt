package com.fernandomendoza.dashboardcarrorf.models

data class GpsData(
    val latitude: Coordinate,
    val longitude: Coordinate,
    val numberOfSatellites: UShort,
) {
    val latitudeAsDecimalDegrees: Double
        get() = latitude.asDecimalDegrees

    val longitudeAsDecimalDegrees: Double
        get() = longitude.asDecimalDegrees
}

data class Coordinate(
    val degrees: Short,
    val minutes: Short,
    val seconds: Int,
    val cardinalPoint: CardinalPoint,
) {
    val asDecimalDegrees: Double
        get() {
            val ddResult = (degrees + (minutes / 60.0) + (seconds / (60.0 * SECONDS_DIVIDE_FACTOR)))

            return if (cardinalPoint == CardinalPoint.SOUTH || cardinalPoint == CardinalPoint.WEST) {
                ddResult * -1.0
            } else {
                ddResult
            }
        }
    companion object {
        private const val SECONDS_DIVIDE_FACTOR = 100000
    }
}
