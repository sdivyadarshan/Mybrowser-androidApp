package com.example.data

import com.connectsdk.device.ConnectableDevice

/**
 * Represents an LG webOS TV or compatible screen discovered on the local network.
 */
data class TvDevice(
    val id: String,
    val friendlyName: String,
    val modelName: String = "",
    val modelNumber: String = "",
    val ipAddress: String = "",
    val isWebOS: Boolean = true,
    val isConnected: Boolean = false,
    val connectableDevice: ConnectableDevice? = null
)
