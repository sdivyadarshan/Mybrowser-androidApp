package com.example.data

import com.connectsdk.service.DeviceService

/**
 * Sealed class representing the reactive UI connection statuses for the LG webOS TV companion app:
 * - [Searching]: Network SSDP scan active, looking for available LG TVs
 * - [Discovered]: One or more LG TVs found on the local Wi-Fi network
 * - [Pairing]: Active pairing handshake with an LG TV (Prompt, PIN, or Combined mode)
 * - [Connected]: TV successfully paired and active command session established
 * - [Disconnected]: No active TV connection
 */
sealed class ConnectionState(val displayText: String) {
    data object Searching : ConnectionState("Searching...")
    
    data class Discovered(
        val devices: List<TvDevice> = emptyList()
    ) : ConnectionState("TV Found")
    
    data class Pairing(
        val device: TvDevice? = null,
        val mode: WebOsPairingMode = WebOsPairingMode.PROMPT,
        val pairingType: DeviceService.PairingType? = null
    ) : ConnectionState("Pairing...")
    
    data class Connected(
        val device: TvDevice
    ) : ConnectionState("Connected")
    
    data class Disconnected(
        val previouslyPairedDevice: TvDevice? = null
    ) : ConnectionState("Disconnected")
}
