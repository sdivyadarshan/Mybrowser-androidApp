package com.example

import com.connectsdk.service.DeviceService
import com.example.data.ConnectionState
import com.example.data.ConnectionStatus
import com.example.data.TvCompanionUiState
import com.example.data.TvDevice
import com.example.data.WebOsPairingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvCompanionStateTest {

    @Test
    fun connectionState_hasExpectedDisplayText() {
        assertEquals("Searching...", ConnectionState.Searching.displayText)
        assertEquals("TV Found", ConnectionState.Discovered().displayText)
        assertEquals("Pairing...", ConnectionState.Pairing().displayText)
        assertEquals("Connected", ConnectionState.Connected(TvDevice("1", "TV")).displayText)
        assertEquals("Disconnected", ConnectionState.Disconnected().displayText)
    }

    @Test
    fun syncConnectionState_mapsCorrectlyAcrossAllStatuses() {
        val tv = TvDevice(
            id = "192.168.1.105",
            friendlyName = "LG OLED TV",
            modelName = "OLED55C1PUB",
            modelNumber = "OLED55C1PUB",
            ipAddress = "192.168.1.105",
            isWebOS = true
        )

        // Searching
        val searchingState = TvCompanionUiState(
            connectionStatus = ConnectionStatus.SEARCHING,
            isSearching = true
        ).syncConnectionState()
        assertTrue(searchingState.connectionState is ConnectionState.Searching)

        // TV Found / Discovered
        val discoveredState = TvCompanionUiState(
            connectionStatus = ConnectionStatus.TV_FOUND,
            discoveredDevices = listOf(tv)
        ).syncConnectionState()
        assertTrue(discoveredState.connectionState is ConnectionState.Discovered)
        assertEquals(listOf(tv), (discoveredState.connectionState as ConnectionState.Discovered).devices)

        // Pairing Required / Pairing
        val pairingState = TvCompanionUiState(
            connectionStatus = ConnectionStatus.PAIRING_REQUIRED,
            activePairingDevice = tv,
            pairingMode = WebOsPairingMode.PIN,
            activePairingType = DeviceService.PairingType.PIN_CODE
        ).syncConnectionState()
        assertTrue(pairingState.connectionState is ConnectionState.Pairing)
        val pairing = pairingState.connectionState as ConnectionState.Pairing
        assertEquals(tv, pairing.device)
        assertEquals(WebOsPairingMode.PIN, pairing.mode)
        assertEquals(DeviceService.PairingType.PIN_CODE, pairing.pairingType)

        // Connected
        val connectedState = TvCompanionUiState(
            connectionStatus = ConnectionStatus.CONNECTED,
            connectedDevice = tv
        ).syncConnectionState()
        assertTrue(connectedState.connectionState is ConnectionState.Connected)
        assertEquals(tv, (connectedState.connectionState as ConnectionState.Connected).device)

        // Disconnected
        val disconnectedState = TvCompanionUiState(
            connectionStatus = ConnectionStatus.DISCONNECTED,
            previouslyPairedDevice = tv
        ).syncConnectionState()
        assertTrue(disconnectedState.connectionState is ConnectionState.Disconnected)
        assertEquals(tv, (disconnectedState.connectionState as ConnectionState.Disconnected).previouslyPairedDevice)
    }

    @Test
    fun connectionStatus_hasExpectedDisplayText() {
        assertEquals("Searching...", ConnectionStatus.SEARCHING.displayText)
        assertEquals("TV Found", ConnectionStatus.TV_FOUND.displayText)
        assertEquals("Pairing Required", ConnectionStatus.PAIRING_REQUIRED.displayText)
        assertEquals("Pairing...", ConnectionStatus.PAIRING.displayText)
        assertEquals("Connected", ConnectionStatus.CONNECTED.displayText)
        assertEquals("Disconnected", ConnectionStatus.DISCONNECTED.displayText)
    }

    @Test
    fun pairingMode_supportsPromptPinAndCombined() {
        val promptState = TvCompanionUiState(
            connectionStatus = ConnectionStatus.PAIRING_REQUIRED,
            pairingMode = WebOsPairingMode.PROMPT
        )
        assertEquals(WebOsPairingMode.PROMPT, promptState.pairingMode)

        val pinState = TvCompanionUiState(
            connectionStatus = ConnectionStatus.PAIRING_REQUIRED,
            pairingMode = WebOsPairingMode.PIN,
            pinInput = "1234"
        )
        assertEquals(WebOsPairingMode.PIN, pinState.pairingMode)
        assertEquals("1234", pinState.pinInput)

        val combinedState = TvCompanionUiState(
            connectionStatus = ConnectionStatus.PAIRING,
            pairingMode = WebOsPairingMode.COMBINED
        )
        assertEquals(WebOsPairingMode.COMBINED, combinedState.pairingMode)
        assertEquals(ConnectionStatus.PAIRING, combinedState.connectionStatus)
    }

    @Test
    fun initialState_isDisconnected() {
        val state = TvCompanionUiState()
        assertEquals(ConnectionStatus.DISCONNECTED, state.connectionStatus)
        assertFalse(state.isSearching)
        assertTrue(state.discoveredDevices.isEmpty())
        assertNull(state.connectedDevice)
        assertNull(state.previouslyPairedDevice)
    }

    @Test
    fun discoveredDevices_updatesProperly() {
        val tv = TvDevice(
            id = "192.168.1.105",
            friendlyName = "Living Room TV",
            modelName = "32LM636BPTB",
            modelNumber = "32LM636BPTB",
            ipAddress = "192.168.1.105",
            isWebOS = true
        )
        val state = TvCompanionUiState(
            connectionStatus = ConnectionStatus.TV_FOUND,
            discoveredDevices = listOf(tv)
        )

        assertEquals(1, state.discoveredDevices.size)
        assertEquals("Living Room TV", state.discoveredDevices[0].friendlyName)
        assertEquals("32LM636BPTB", state.discoveredDevices[0].modelName)
        assertTrue(state.discoveredDevices[0].isWebOS)
    }

    @Test
    fun connectedDeviceState_isCorrect() {
        val tv = TvDevice(
            id = "192.168.1.105",
            friendlyName = "Living Room TV",
            modelName = "32LM636BPTB",
            modelNumber = "32LM636BPTB",
            ipAddress = "192.168.1.105",
            isWebOS = true,
            isConnected = true
        )
        val state = TvCompanionUiState(
            connectionStatus = ConnectionStatus.CONNECTED,
            connectedDevice = tv
        )

        assertEquals(ConnectionStatus.CONNECTED, state.connectionStatus)
        assertNotNull(state.connectedDevice)
        assertEquals("Living Room TV", state.connectedDevice?.friendlyName)
        assertTrue(state.connectedDevice?.isConnected == true)
    }

    @Test
    fun urlUtils_extractsAndFormatsUrlsAccurately() {
        // Direct URL
        assertEquals("https://www.google.com", com.example.util.UrlUtils.extractUrl("https://www.google.com"))
        
        // Protocol-less web address gets https:// prepended
        assertEquals("https://youtube.com", com.example.util.UrlUtils.extractUrl("youtube.com"))
        assertEquals("https://www.wikipedia.org/wiki/Kotlin", com.example.util.UrlUtils.extractUrl("www.wikipedia.org/wiki/Kotlin"))

        // Chrome share text with description and URL
        val chromeShareText = "Check this out: https://news.ycombinator.com/item?id=12345 shared from Chrome"
        assertEquals("https://news.ycombinator.com/item?id=12345", com.example.util.UrlUtils.extractUrl(chromeShareText))

        // Text without URL
        assertNull(com.example.util.UrlUtils.extractUrl("just random plain text with no link"))
        assertNull(com.example.util.UrlUtils.extractUrl(""))
    }

    @Test
    fun linkState_tracksManualInputAndFeedback() {
        val state = TvCompanionUiState(
            manualLinkInput = "https://youtube.com",
            isSendingLink = true,
            linkSendSuccessMessage = "Sent to TV! Opening link in browser...",
            linkSendErrorMessage = null,
            sharedLinkNotification = "Link received from Chrome: https://youtube.com"
        )

        assertEquals("https://youtube.com", state.manualLinkInput)
        assertTrue(state.isSendingLink)
        assertEquals("Sent to TV! Opening link in browser...", state.linkSendSuccessMessage)
        assertNull(state.linkSendErrorMessage)
        assertEquals("Link received from Chrome: https://youtube.com", state.sharedLinkNotification)
    }
}
