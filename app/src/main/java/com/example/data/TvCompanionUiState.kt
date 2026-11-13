package com.example.data

import com.connectsdk.service.DeviceService

enum class ConnectionStatus(val displayText: String) {
    SEARCHING("Searching..."),
    TV_FOUND("TV Found"),
    PAIRING_REQUIRED("Pairing Required"),
    PAIRING("Pairing..."),
    CONNECTED("Connected"),
    DISCONNECTED("Disconnected")
}

enum class WebOsPairingMode {
    PROMPT,     // First screen user confirmation prompt on TV
    PIN,        // PIN code entered on mobile app
    COMBINED    // Mixed mode: Accept on-screen prompt or enter TV PIN
}

data class TvCompanionUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val connectionState: ConnectionState = ConnectionState.Disconnected(),
    val isSearching: Boolean = false,
    val isWifiConnected: Boolean = true,
    val wifiSsid: String? = null,
    val discoveredDevices: List<TvDevice> = emptyList(),
    val previouslyPairedDevice: TvDevice? = null,
    val connectedDevice: TvDevice? = null,
    val activePairingDevice: TvDevice? = null,
    val activePairingType: DeviceService.PairingType? = null,
    val pairingMode: WebOsPairingMode = WebOsPairingMode.PROMPT,
    val pinInput: String = "",
    val manualLinkInput: String = "",
    val isSendingLink: Boolean = false,
    val linkSendSuccessMessage: String? = null,
    val linkSendErrorMessage: String? = null,
    val lastLaunchedUrl: String? = null,
    val sharedLinkNotification: String? = null,
    val errorMessage: String? = null,
    val errorResolution: String? = null
) {
    fun syncConnectionState(): TvCompanionUiState {
        val resolved = when (connectionStatus) {
            ConnectionStatus.SEARCHING -> ConnectionState.Searching
            ConnectionStatus.TV_FOUND -> ConnectionState.Discovered(discoveredDevices)
            ConnectionStatus.PAIRING_REQUIRED, ConnectionStatus.PAIRING -> ConnectionState.Pairing(
                device = activePairingDevice,
                mode = pairingMode,
                pairingType = activePairingType
            )
            ConnectionStatus.CONNECTED -> {
                connectedDevice?.let { ConnectionState.Connected(it) }
                    ?: ConnectionState.Disconnected(previouslyPairedDevice)
            }
            ConnectionStatus.DISCONNECTED -> {
                if (isSearching) {
                    ConnectionState.Searching
                } else if (discoveredDevices.isNotEmpty()) {
                    ConnectionState.Discovered(discoveredDevices)
                } else {
                    ConnectionState.Disconnected(previouslyPairedDevice)
                }
            }
        }
        return copy(connectionState = resolved)
    }
}
