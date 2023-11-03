package com.fernandomendoza.dashboardcarrorf.repository

import android.util.Log
import com.fernandomendoza.dashboardcarrorf.datasources.DeviceDataSource
import com.fernandomendoza.dashboardcarrorf.models.CardinalPoint
import com.fernandomendoza.dashboardcarrorf.models.Coordinate
import com.fernandomendoza.dashboardcarrorf.models.DashboardState
import com.fernandomendoza.dashboardcarrorf.models.GpsData
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

        var gpsData: GpsData? = previousState?.gps
        var speedMetersPerSecond: Double? = previousState?.approximateSpeedMetersPerSecond
        var batterySoC: Int? = previousState?.batterySoC
        var hoursOfBatteryLeft = previousState?.hoursOfBatteryLeft

        when (payload.size) {
            GPS_PAYLOAD_SIZE -> {
                val latInt = (payload[0].toInt() shl 8) or payload[1].toInt()
                val latDec = (payload[2].toInt() shl 16) or (payload[3].toInt() shl 8) or payload[4].toInt()
                val latCardinalPoint = when (payload[10].toInt() and NORTH_SOUTH_FLAG) {
                    NORTH_SOUTH_FLAG -> CardinalPoint.SOUTH
                    else -> CardinalPoint.NORTH
                }

                val lonInt = (payload[5].toInt() shl 8) or payload[6].toInt()
                val lonDec = (payload[7].toInt() shl 16) or (payload[8].toInt() shl 8) or payload[9].toInt()
                val lonCardinalPoint = when (payload[10].toInt() and EAST_WEST_FLAG) {
                    EAST_WEST_FLAG -> CardinalPoint.EAST
                    else -> CardinalPoint.WEST
                }

                gpsData = GpsData(
                    latitude = Coordinate(
                        degrees = (latInt / 100).toShort(),
                        minutes = (latInt % 100).toShort(),
                        seconds = latDec,
                        latCardinalPoint,
                    ),
                    longitude = Coordinate(
                        degrees = (lonInt / 100).toShort(),
                        minutes = (lonInt % 100).toShort(),
                        seconds = lonDec,
                        lonCardinalPoint,
                    ),
                    numberOfSatellites = ((payload[8].toInt() and SATELLITE_NUMBER_MASK) shr 2).toUShort()
                )
            }
            BATTERY_PAYLOAD_SIZE -> {
                val batteryMv = (payload[0].toInt() shl 8) or payload[1].toInt()
                Log.i(TAG, "Battery: $batteryMv mV")

                batterySoC = maxOf(batteryMv - MIN_BATTERY_MV, 0) * 100 / (MAX_BATTERY_MV - MIN_BATTERY_MV)
                hoursOfBatteryLeft = batterySoC * EXPECTED_BATTERY_HOURS / 100.0f
            }
            SPEED_PAYLOAD_SIZE -> {
                val msPerRevolution = ((payload[0].toInt() shl 8) or payload[1].toInt()).toDouble() * 10.0
                val speedRPM = (60000.0 / msPerRevolution)
                // Convertir a m/s
//                speedMetersPerSecond = (speedRPM / 60.0) * (WHEEL_DIAMETER_M * Math.PI)
                speedMetersPerSecond = speedRPM
                Log.i(TAG, "Velocidad: $msPerRevolution  us por revolucion, $speedRPM RPM, $speedMetersPerSecond m/s")
            }
        }

        return if (gpsData != null || speedMetersPerSecond != null || (batterySoC != null && hoursOfBatteryLeft != null)) {
            DashboardState.Loaded(
                gps = gpsData,
                approximateSpeedMetersPerSecond = speedMetersPerSecond,
                batterySoC = batterySoC,
                hoursOfBatteryLeft = hoursOfBatteryLeft
            )
        } else {
            DashboardState.Loading
        }
    }

    companion object {
        private val TAG = DashboardRepository::class.simpleName

        private const val GPS_PAYLOAD_SIZE = 11
        private const val SPEED_PAYLOAD_SIZE = 2
        private const val BATTERY_PAYLOAD_SIZE = 3

        private const val MIN_BATTERY_MV = 3600
        private const val MAX_BATTERY_MV = 6400
        private const val EXPECTED_BATTERY_HOURS = 3.0f

        private const val NORTH_SOUTH_FLAG = 0x01
        private const val EAST_WEST_FLAG = 0x02
        private const val SATELLITE_NUMBER_MASK = 0xFC
    }
}