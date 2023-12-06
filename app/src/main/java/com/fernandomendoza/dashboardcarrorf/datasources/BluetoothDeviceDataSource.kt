package com.fernandomendoza.dashboardcarrorf.datasources

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.fernandomendoza.dashboardcarrorf.models.BluetoothDeviceInfo
import com.fernandomendoza.dashboardcarrorf.models.BluetoothDeviceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class BluetoothDeviceDataSource(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val scanIntervalMs: Long = 60_000,
) : DeviceDataSource {
    override val connectedDevice: Flow<BluetoothDeviceInfo?>
        @SuppressLint("MissingPermission")
        get() = _connectedDevice.map {
            if (it != null) {
                BluetoothDeviceInfo(
                    name = it.name,
                    address = it.address,
                    isConnected = true
                )
            } else null
        }

    override val availableDevices: Flow<List<BluetoothDeviceInfo>>
        @SuppressLint("MissingPermission")
        get() = _bondedDevices.combine(deviceDiscoveryEventChannel.receiveAsFlow()) { bondedDevices, discoveredDevice ->
            val isNotConnectedDevice =
                (_connectedDevice.value == null || discoveredDevice.address != _connectedDevice.value!!.address)

            if (isNotConnectedDevice) {
                discoveredDevicesMap[discoveredDevice.address] = discoveredDevice
            } else {
                discoveredDevicesMap.remove(discoveredDevice.address)
            }

            (bondedDevices + discoveredDevicesMap.values.toList()).map {
                BluetoothDeviceInfo(
                    name = it.name ?: "Unknown device",
                    address = it.address,
                    isConnected = isNotConnectedDevice
                )
            }
        }

    override val isConnecting: StateFlow<Boolean>
        get() = _isConnecting.asStateFlow()

    override val isSupported: Boolean
        get() = (bluetoothAdapter != null && bluetoothAdapter.isEnabled)

    override fun addDiscoveredDevice(device: Any) {
        if (device is BluetoothDevice) {
            Log.i(TAG, "Bluetooth device discovered: $device")
            deviceDiscoveryEventChannel.trySend(device)
                .onClosed {
                    throw it
                        ?: ClosedSendChannelException("Scan result event channel closed normally")
                }
        }
    }

    override suspend fun startDiscovery(durationMs: Long): Boolean {
        try {
            withContext(Dispatchers.IO) {
                endDeviceDiscovery()
                val result = bluetoothAdapter?.startDiscovery() ?: false

                Log.i(TAG, "Started bluetooth device discovery: $result")

                delay(durationMs)

                endDeviceDiscovery()

                return@withContext result
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "BLUETOOTH_SCAN permission not granted by user, cannot start discovery")
        }

        return false
    }

    override fun endDeviceDiscovery(): Boolean = try {
        bluetoothAdapter?.cancelDiscovery() ?: false
    } catch (e: SecurityException) {
        Log.e(TAG, "BLUETOOTH_SCAN permission not granted by user, cannot cancel discovery")
        false
    }

    override suspend fun connectToDevice(deviceAddress: String) {
        if (bluetoothAdapter == null) return

        endDeviceDiscovery()

        _isConnecting.value = true

        try {
            val remoteBluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)

            _connectedDevice.value = remoteBluetoothDevice

            val bluetoothSocket =
                remoteBluetoothDevice.createInsecureRfcommSocketToServiceRecord(RF_COMM_UUID)

            withContext(Dispatchers.IO) {
                bluetoothSocket.connect()
                _bluetoothDeviceSocket = bluetoothSocket
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Address is incorrect, or device at $deviceAddress can't be found")
        } catch (e: SecurityException) {
            Log.e(TAG, "Attempted bluetooth connection without required permissions")
        } finally {
            _isConnecting.value = false
        }
    }

    override fun disconnectFromDevice() {
        _bluetoothDeviceSocket?.close()
        _connectedDevice.value = null
    }

    override suspend fun listen(bufferSize: Int, onResponse: (BluetoothDeviceResult) -> Unit) {
        val socket = _bluetoothDeviceSocket
            ?: throw IllegalStateException("Can't listen because no device is connected.")

        val inputStream = socket.inputStream
        val inputBuffer = ByteArray(bufferSize)
        var inputOffset = 0

        withContext(Dispatchers.IO) {
            while (true) {
                val receivedByte = inputStream.read()

                if (inputOffset >= bufferSize) {
                    Log.i(TAG, "Reset input buffer offset, buffer = $inputBuffer")
                    inputOffset = 0
                } else if (inputOffset > 0 && ((0x0Au).toUByte() == receivedByte.toUByte()) && ((0x0Du).toUByte() == inputBuffer[inputOffset - 1].toUByte())) {
                    Log.i(TAG, "Received a message with $inputOffset bytes")

                    onResponse(
                        BluetoothDeviceResult(
                            inputBuffer.clone().sliceArray(0 until inputOffset),
                            (inputOffset - 1)
                        )
                    )

                    inputOffset = 0
                }  else {
                    inputBuffer[inputOffset] = receivedByte.toByte()
                    inputOffset++
                }
            }
        }
    }

    override fun cancel() {
        _bluetoothDeviceSocket?.close()
    }

    private val _connectedDevice: MutableStateFlow<BluetoothDevice?> = MutableStateFlow(null)

    private val _isConnecting: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var _bluetoothDeviceSocket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    private val _bondedDevices: Flow<List<BluetoothDevice>> = flow {
        while (true) {
            val bondedDevices: Set<BluetoothDevice> = bluetoothAdapter?.bondedDevices ?: emptySet()

            emit(bondedDevices.toList())

            delay(scanIntervalMs)
        }
    }

    private val deviceDiscoveryEventChannel = Channel<BluetoothDevice>()

    private val discoveredDevicesMap = mutableMapOf<String, BluetoothDevice>()

    companion object {
        private val TAG = BluetoothDeviceDataSource::class.simpleName

        private val RF_COMM_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}