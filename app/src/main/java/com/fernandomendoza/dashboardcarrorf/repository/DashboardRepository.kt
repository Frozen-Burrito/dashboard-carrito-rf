package com.fernandomendoza.dashboardcarrorf.repository

import android.util.Log
import com.fernandomendoza.dashboardcarrorf.datasources.DeviceDataSource
import com.fernandomendoza.dashboardcarrorf.models.CardinalPoint
import com.fernandomendoza.dashboardcarrorf.models.Coordinate
import com.fernandomendoza.dashboardcarrorf.models.DashboardState
import com.fernandomendoza.dashboardcarrorf.models.GpsArea
import com.fernandomendoza.dashboardcarrorf.models.GpsData
import com.fernandomendoza.dashboardcarrorf.models.ImuData
import com.mapbox.maps.extension.style.expressions.dsl.generated.pitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.IOException

class DashboardRepository(private val deviceDataSource: DeviceDataSource) {

    val dashboardState: Flow<DashboardState>
        get() = _dashboardState
            .asStateFlow()
            .combine(deviceDataSource.connectedDevice) { dashboardState, connectedDevice ->
                if (connectedDevice == null) DashboardState.Disconnected
                else dashboardState
            }

    /**
     * @brief Starts listening to updates sent by the dashboard.
     *
     * @throws IOException A device is not connected, or data cannot be read from it.
     */
    suspend fun subscribeToState() {
        withContext(Dispatchers.IO) {
            deviceDataSource.listen { result ->
                Log.i(TAG, "Received state: ${result.length}")

                val payload = result.payload.toList().subList(0, result.length).map { it.toUByte() }
                _dashboardState.value = parseDashboardState(payload)
            }
        }
    }

    fun cancelSubscription() = deviceDataSource.cancel()

    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Loading)

    private fun parseDashboardState(payload: List<UByte>): DashboardState {
        val previousState = if (_dashboardState.value is DashboardState.Loaded) _dashboardState.value as DashboardState.Loaded else null

        var radioIsConnected: Boolean? = previousState?.radioIsConnected
        var gpsData: GpsData? = previousState?.gps
        var speedMetersPerSecond: Double? = previousState?.approximateSpeedMetersPerSecond
        var batterySoC: Int? = previousState?.batterySoC
        var hoursOfBatteryLeft = previousState?.hoursOfBatteryLeft
        var imuData = previousState?.imuData

        if (EXPECTED_PAYLOAD_SIZE == payload.size) {
            radioIsConnected = payload[RADIO_STAT] == RADIO_STATUS_CONNECTED.toUByte()

            val latInt = (payload[LAT_INT_MSB].toInt() shl 8) or payload[LAT_INT_LSB].toInt()
            val latDec = (payload[LAT_DEC_MSB].toInt() shl 16) or (payload[LAT_DEC_XSB].toInt() shl 8) or payload[LAT_DEC_LSB].toInt()
            val latCardinalPoint = when (payload[GPS_STAT_FG].toInt() and NORTH_SOUTH_FLAG) {
                NORTH_SOUTH_FLAG -> CardinalPoint.SOUTH
                else -> CardinalPoint.NORTH
            }

            val lonInt = (payload[LON_INT_MSB].toInt() shl 8) or payload[LON_INT_LSB].toInt()
            val lonDec = (payload[LON_DEC_MSB].toInt() shl 16) or (payload[LON_DEC_XSB].toInt() shl 8) or payload[LON_DEC_LSB].toInt()
            val lonCardinalPoint = when (payload[GPS_STAT_FG].toInt() and EAST_WEST_FLAG) {
                EAST_WEST_FLAG -> CardinalPoint.EAST
                else -> CardinalPoint.WEST
            }

            gpsData = GpsData(
                latitude = Coordinate(
                    degrees = (latInt / 100).toShort(),
                    minutes = (latInt % 100).toShort(),
                    seconds = latDec.toDouble(),
                    latCardinalPoint,
                ),
                longitude = Coordinate(
                    degrees = (lonInt / 100).toShort(),
                    minutes = (lonInt % 100).toShort(),
                    seconds = lonDec.toDouble(),
                    lonCardinalPoint,
                ),
                numberOfSatellites = ((payload[GPS_STAT_FG].toInt() and SATELLITE_NUMBER_MASK) shr 2).toUShort(),
                expectedArea = expectedGpsArea
            )

            val batteryMv = (payload[BATT_MV_MSB].toInt() shl 8) or payload[BATT_MV_LSB].toInt()
            Log.i(TAG, "Battery: $batteryMv mV")

            batterySoC = maxOf(batteryMv - MIN_BATTERY_MV, 0) * 100 / (MAX_BATTERY_MV - MIN_BATTERY_MV)
            hoursOfBatteryLeft = batterySoC * EXPECTED_BATTERY_HOURS / 100.0f

            val msPerRevolution = ((payload[REV_FRA_MSB].toInt() shl 8) or payload[REV_FRA_LSB].toInt()).toDouble() * 10.0
            val speedRPM = if (msPerRevolution > 0) {
                (60000.0 / msPerRevolution)
            } else { 0.0 }

            val speedMs = (speedRPM / 60.0) * (WHEEL_DIAMETER_M * Math.PI)
            speedMetersPerSecond = speedRPM
            Log.i(TAG, "Velocidad: $msPerRevolution  us por revolucion, $speedRPM RPM, $speedMs m/s")

            val unsignedAcceleration = ((payload[ACCEL_X_MSB].toInt() shl 8) or payload[ACCEL_X_LSB].toInt())
            val unsignedPitchDegrees = ((payload[PITCH_MSB].toInt() shl 8) or payload[PITCH_LSB].toInt())
            val unsignedRollDegrees = ((payload[ROLL_MSB].toInt() shl 8) or payload[ROLL_LSB].toInt())

            val newAccelerationMps = if (unsignedAcceleration > 32768) {
                (0 - (65536 - unsignedAcceleration)).toDouble() * 9.81 / 16384.0
            } else {
                unsignedAcceleration.toDouble() * 9.81 / 16384.0
            }

            val newPitchDegrees = if (unsignedPitchDegrees > 32768) {
                (0 - (65536 - unsignedPitchDegrees))
            } else {
                unsignedPitchDegrees
            }

            val newRollDegrees = if (unsignedRollDegrees > 32768) {
                (0 - (65536 - unsignedRollDegrees))
            } else {
                unsignedRollDegrees
            }

            Log.i(TAG, "Acce $newAccelerationMps, pitch $newPitchDegrees, roll $newRollDegrees")

            if ((-200.0 <= newAccelerationMps && newAccelerationMps <= 200.0) && (-360 <= newPitchDegrees && newPitchDegrees <= 360) && (-360 <= newRollDegrees && newRollDegrees <= 360)) {
                imuData = ImuData(
                    accelerationMetersPerSecond = newAccelerationMps,
                    pitchDegrees = newPitchDegrees,
                    rollDegrees = newRollDegrees
                )
            }
        }

        return if (radioIsConnected != null) {
            DashboardState.Loaded(
                radioIsConnected = radioIsConnected,
                gps = gpsData,
                approximateSpeedMetersPerSecond = speedMetersPerSecond,
                batterySoC = batterySoC,
                hoursOfBatteryLeft = hoursOfBatteryLeft,
                imuData = imuData
            )
        } else {
            DashboardState.Loading
        }
    }

    companion object {
        private val TAG = DashboardRepository::class.simpleName

        private val expectedGpsArea = GpsArea(
            startLatitude = Coordinate(20, 35, 29.2344, CardinalPoint.NORTH),
            startLongitude = Coordinate(103, 23, 25.2312, CardinalPoint.WEST),
            endLatitude = Coordinate(20, 37, 22.8828, CardinalPoint.NORTH),
            endLongitude = Coordinate(103, 27, 5.0004, CardinalPoint.WEST),
        )

        private const val EXPECTED_PAYLOAD_SIZE = 22

        private const val RADIO_STATUS_CONNECTED = 0u

        private const val MIN_BATTERY_MV = 3600
        private const val MAX_BATTERY_MV = 6400
        private const val EXPECTED_BATTERY_HOURS = 3.0f

        private const val NORTH_SOUTH_FLAG = 0x01
        private const val EAST_WEST_FLAG = 0x02
        private const val SATELLITE_NUMBER_MASK = 0xFC

        private const val WHEEL_DIAMETER_M = 0.002

        private const val RADIO_STAT  = 0
        private const val LAT_INT_MSB = 1
        private const val LAT_INT_LSB = 2
        private const val LAT_DEC_MSB = 3
        private const val LAT_DEC_XSB = 4
        private const val LAT_DEC_LSB = 5
        private const val LON_INT_MSB = 6
        private const val LON_INT_LSB = 7
        private const val LON_DEC_MSB = 8
        private const val LON_DEC_XSB = 9
        private const val LON_DEC_LSB = 10
        private const val GPS_STAT_FG = 11
        private const val REV_FRA_MSB = 12
        private const val REV_FRA_LSB = 13
        private const val BATT_MV_MSB = 14
        private const val BATT_MV_LSB = 15
        private const val ACCEL_X_MSB = 16
        private const val ACCEL_X_LSB = 17
        private const val PITCH_MSB   = 18
        private const val PITCH_LSB   = 19
        private const val ROLL_MSB    = 20
        private const val ROLL_LSB    = 21
    }
}