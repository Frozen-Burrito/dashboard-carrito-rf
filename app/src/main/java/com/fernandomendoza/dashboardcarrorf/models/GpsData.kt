package com.fernandomendoza.dashboardcarrorf.models

data class GpsData(
    val latitude: Coordinate,
    val longitude: Coordinate,
    val numberOfSatellites: UShort,
    val expectedArea: GpsArea?,
) {
    val latitudeAsDecimalDegrees: Double
        get() = latitude.asDecimalDegrees

    val longitudeAsDecimalDegrees: Double
        get() = longitude.asDecimalDegrees

    val coordinateIsInExpectedArea: Boolean
        get() = (expectedArea == null || expectedArea.isCoordinateInRange(latitude, longitude))
}

data class GpsArea(
    val startLatitude: Coordinate,
    val startLongitude: Coordinate,
    val endLatitude: Coordinate,
    val endLongitude: Coordinate,
) {
    fun isCoordinateInRange(lat: Coordinate, lon: Coordinate): Boolean {
        val latInRange = (startLatitude.degrees <= lat.degrees || startLatitude.minutes <= lat.minutes || startLatitude.seconds <= lat.seconds) &&
                (endLatitude.degrees >= lat.degrees || endLatitude.minutes <= lat.minutes || endLatitude.seconds <= lat.seconds)

        val lonInRange = (startLongitude.degrees <= lon.degrees || startLongitude.minutes <= lon.minutes || startLongitude.seconds <= lon.seconds) &&
                (endLongitude.degrees >= lon.degrees || endLongitude.minutes <= lon.minutes || endLongitude.seconds <= lon.seconds)

        return (latInRange && lonInRange)
    }
}

data class Coordinate(
    val degrees: Short,
    val minutes: Short,
    val seconds: Double,
    val cardinalPoint: CardinalPoint,
) {
    //20.605823, -103.415979
    // -103 24 9110
    // -103.401518

    // 20 40 49100
    // 20.674850
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
