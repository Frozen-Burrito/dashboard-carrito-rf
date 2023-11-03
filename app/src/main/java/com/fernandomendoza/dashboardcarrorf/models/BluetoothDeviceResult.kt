package com.fernandomendoza.dashboardcarrorf.models

data class BluetoothDeviceResult(
    val payload: ByteArray,
    val length: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BluetoothDeviceResult

        if (!payload.contentEquals(other.payload)) return false
        if (length != other.length) return false

        return true
    }

    override fun hashCode(): Int {
        var result = payload.contentHashCode()
        result = 31 * result + length
        return result
    }
}
