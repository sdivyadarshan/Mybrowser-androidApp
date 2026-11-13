package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ConnectionState
import com.example.data.ConnectionStatus
import com.example.data.TvCompanionUiState
import com.example.data.TvDevice
import com.example.service.ConnectSdkManager
import com.example.util.UrlUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class TvCompanionViewModel(application: Application) : AndroidViewModel(application) {

    private val connectSdkManager = ConnectSdkManager(application)
    val uiState: StateFlow<TvCompanionUiState> = connectSdkManager.uiState

    // Reactive ConnectionState stream for Compose UI updates
    val connectionState: StateFlow<ConnectionState> = connectSdkManager.uiState
        .map { it.connectionState }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = connectSdkManager.uiState.value.connectionState
        )

    private val _pinState = MutableStateFlow("")
    val pinState: StateFlow<String> = _pinState.asStateFlow()

    private val _linkInputState = MutableStateFlow("")
    val linkInputState: StateFlow<String> = _linkInputState.asStateFlow()

    init {
        // Auto-start discovery on initial launch to find available TVs seamlessly
        startDiscovery()
    }

    fun startDiscovery() {
        connectSdkManager.startDiscovery()
    }

    fun stopDiscovery() {
        connectSdkManager.stopDiscovery()
    }

    fun connectToDevice(device: TvDevice) {
        _pinState.value = ""
        connectSdkManager.connectToDevice(device)
    }

    fun reconnectToPairedDevice() {
        _pinState.value = ""
        connectSdkManager.reconnectToPairedDevice()
    }

    fun onPinChanged(pin: String) {
        _pinState.value = pin
    }

    fun submitPin() {
        val pin = _pinState.value
        if (pin.isNotBlank()) {
            connectSdkManager.submitPin(pin)
        }
    }

    fun cancelPairing() {
        _pinState.value = ""
        connectSdkManager.cancelPairing()
    }

    fun disconnectDevice() {
        connectSdkManager.disconnectDevice()
    }

    fun unpairDevice() {
        _pinState.value = ""
        connectSdkManager.unpairDevice()
    }

    fun clearError() {
        connectSdkManager.clearError()
    }

    // ==========================================
    // Link Sending & Browser Launch Actions
    // ==========================================

    fun onLinkInputChanged(newLink: String) {
        _linkInputState.value = newLink
    }

    fun clearLinkInput() {
        _linkInputState.value = ""
    }

    fun clearLinkFeedback() {
        connectSdkManager.clearLinkFeedback()
    }

    fun sendLinkToTv(url: String? = null) {
        val targetUrl = (url ?: _linkInputState.value).trim()
        if (targetUrl.isBlank()) return
        connectSdkManager.launchUrlOnTv(targetUrl)
    }

    fun onSharedLinkReceived(sharedText: String) {
        val extractedUrl = UrlUtils.extractUrl(sharedText) ?: sharedText.trim()
        if (extractedUrl.isNotBlank()) {
            _linkInputState.value = extractedUrl
            if (uiState.value.connectionStatus == ConnectionStatus.CONNECTED) {
                connectSdkManager.setSharedLinkNotification("Received link from Chrome. Opening on TV...")
                connectSdkManager.launchUrlOnTv(extractedUrl)
            } else {
                connectSdkManager.setSharedLinkNotification("Link received from Chrome! Connect to your LG TV below to open it.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectSdkManager.onDestroy()
    }
}
