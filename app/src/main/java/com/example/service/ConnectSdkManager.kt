package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.device.ConnectableDeviceListener
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.DiscoveryManagerListener
import com.connectsdk.discovery.provider.SSDPDiscoveryProvider
import com.connectsdk.service.DeviceService
import com.connectsdk.service.NetcastTVService
import com.connectsdk.service.WebOSTVService
import com.connectsdk.service.capability.Launcher
import com.connectsdk.service.sessions.LaunchSession
import com.connectsdk.service.command.ServiceCommandError
import com.example.data.ConnectionStatus
import com.example.data.TvCompanionUiState
import com.example.data.TvDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConnectSdkManager(private val context: Context) : DiscoveryManagerListener, ConnectableDeviceListener {

    private val tag = "ConnectSdkManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val prefs: SharedPreferences = context.getSharedPreferences("mybrowser_tv_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(TvCompanionUiState().syncConnectionState())
    val uiState: StateFlow<TvCompanionUiState> = _uiState.asStateFlow()

    private var multicastLock: WifiManager.MulticastLock? = null
    private var discoveryManager: DiscoveryManager? = null
    private var currentConnectingDevice: ConnectableDevice? = null

    private fun updateState(transform: (TvCompanionUiState) -> TvCompanionUiState) {
        _uiState.update { current ->
            transform(current).syncConnectionState()
        }
    }

    init {
        setupWifiMonitoring()
        initConnectSdk()
        loadPreviouslyPairedDevice()
    }

    private fun initConnectSdk() {
        try {
            DiscoveryManager.init(context.applicationContext)
            val dm = DiscoveryManager.getInstance()
            dm.pairingLevel = DiscoveryManager.PairingLevel.ON
            // Explicitly register LG webOS and Netcast TV services via SSDPDiscoveryProvider.
            // Do not call registerDefaultDeviceTypes() as it registers legacy FireTV/WhisperLink
            // services which crash on Android 14+ due to missing RECEIVER_NOT_EXPORTED flags.
            dm.registerDeviceService(WebOSTVService::class.java, SSDPDiscoveryProvider::class.java)
            dm.registerDeviceService(NetcastTVService::class.java, SSDPDiscoveryProvider::class.java)
            dm.addListener(this)
            discoveryManager = dm
            Log.d(tag, "Connect SDK DiscoveryManager initialized successfully for LG webOS TV with pairingLevel=ON")
        } catch (e: Exception) {
            Log.e(tag, "Error initializing Connect SDK", e)
            updateState {
                it.copy(
                    errorMessage = "Failed to initialize TV discovery engine.",
                    errorResolution = "Please restart the application."
                )
            }
        }
    }

    private fun setupWifiMonitoring() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            updateState { it.copy(isWifiConnected = false) }
            return
        }

        val isWifi = isCurrentlyOnWifi(connectivityManager)
        updateState { it.copy(isWifiConnected = isWifi) }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch {
                    updateState { it.copy(isWifiConnected = true, errorMessage = null) }
                }
            }

            override fun onLost(network: Network) {
                scope.launch {
                    val stillOnWifi = isCurrentlyOnWifi(connectivityManager)
                    updateState {
                        it.copy(
                            isWifiConnected = stillOnWifi,
                            errorMessage = if (!stillOnWifi) "Wi-Fi disconnected" else it.errorMessage,
                            errorResolution = if (!stillOnWifi) "Connect your phone to the same Wi-Fi network as your LG TV." else it.errorResolution
                        )
                    }
                }
            }
        })
    }

    private fun isCurrentlyOnWifi(connectivityManager: ConnectivityManager): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun loadPreviouslyPairedDevice() {
        val id = prefs.getString("paired_id", null) ?: return
        val name = prefs.getString("paired_name", "LG TV") ?: "LG TV"
        val model = prefs.getString("paired_model", "") ?: ""
        val modelNum = prefs.getString("paired_model_number", "") ?: ""
        val ip = prefs.getString("paired_ip", "") ?: ""

        val device = TvDevice(
            id = id,
            friendlyName = name,
            modelName = model,
            modelNumber = modelNum,
            ipAddress = ip,
            isWebOS = true
        )
        updateState { it.copy(previouslyPairedDevice = device) }
    }

    private fun savePairedDevice(device: TvDevice) {
        prefs.edit()
            .putString("paired_id", device.id)
            .putString("paired_name", device.friendlyName)
            .putString("paired_model", device.modelName)
            .putString("paired_model_number", device.modelNumber)
            .putString("paired_ip", device.ipAddress)
            .apply()

        updateState { it.copy(previouslyPairedDevice = device) }
    }

    fun unpairDevice() {
        val deviceToUnpair = currentConnectingDevice
        if (deviceToUnpair != null) {
            try {
                deviceToUnpair.removeListener(this)
                deviceToUnpair.disconnect()
                discoveryManager?.connectableDeviceStore?.removeDevice(deviceToUnpair)
            } catch (e: Exception) {
                Log.w(tag, "Error removing device from store", e)
            }
        }
        currentConnectingDevice = null

        prefs.edit().clear().apply()
        updateState {
            it.copy(
                previouslyPairedDevice = null,
                connectedDevice = null,
                connectionStatus = ConnectionStatus.DISCONNECTED,
                activePairingDevice = null,
                activePairingType = null
            )
        }
    }

    fun startDiscovery() {
        if (!_uiState.value.isWifiConnected) {
            updateState {
                it.copy(
                    errorMessage = "Phone is not connected to Wi-Fi",
                    errorResolution = "Please enable Wi-Fi and connect to your home network."
                )
            }
            return
        }

        acquireMulticastLock()

        try {
            val dm = discoveryManager ?: DiscoveryManager.getInstance()
            dm.start()
            updateState {
                it.copy(
                    isSearching = true,
                    connectionStatus = if (it.connectionStatus == ConnectionStatus.CONNECTED) it.connectionStatus else ConnectionStatus.SEARCHING,
                    errorMessage = null,
                    errorResolution = null
                )
            }
            Log.d(tag, "Connect SDK discovery started")
        } catch (e: Exception) {
            Log.e(tag, "Error starting discovery", e)
            updateState {
                it.copy(
                    isSearching = false,
                    errorMessage = "Discovery could not start",
                    errorResolution = "Please check network permissions and retry."
                )
            }
        }
    }

    fun stopDiscovery() {
        try {
            discoveryManager?.stop()
        } catch (e: Exception) {
            Log.w(tag, "Error stopping discovery", e)
        }
        releaseMulticastLock()
        updateState { it.copy(isSearching = false) }
        Log.d(tag, "Connect SDK discovery stopped")
    }

    private fun acquireMulticastLock() {
        if (multicastLock == null) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("mybrowser_tv_multicast")?.apply {
                setReferenceCounted(true)
            }
        }
        try {
            if (multicastLock?.isHeld == false) {
                multicastLock?.acquire()
                Log.d(tag, "MulticastLock acquired for SSDP / mDNS discovery")
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to acquire MulticastLock", e)
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                Log.d(tag, "MulticastLock released")
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to release MulticastLock", e)
        }
    }

    fun connectToDevice(tvDevice: TvDevice) {
        val connectableDevice = tvDevice.connectableDevice
            ?: findConnectableDeviceByIdOrIp(tvDevice.id, tvDevice.ipAddress)

        if (connectableDevice == null) {
            updateState {
                it.copy(
                    errorMessage = "TV not currently visible on network",
                    errorResolution = "Ensure '${tvDevice.friendlyName}' is powered on and connected to the same Wi-Fi, then tap Search."
                )
            }
            startDiscovery()
            return
        }

        // Disconnect previous if any
        currentConnectingDevice?.let {
            if (it != connectableDevice) {
                it.removeListener(this)
                it.disconnect()
            }
        }

        currentConnectingDevice = connectableDevice
        connectableDevice.addListener(this)

        updateState {
            it.copy(
                connectionStatus = ConnectionStatus.PAIRING,
                activePairingDevice = tvDevice,
                errorMessage = null,
                errorResolution = null
            )
        }

        try {
            Log.d(tag, "Connecting to device: ${connectableDevice.friendlyName} (${connectableDevice.ipAddress})")
            connectableDevice.connect()
        } catch (e: Exception) {
            Log.e(tag, "Error during connect()", e)
            updateState {
                it.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    errorMessage = "Connection attempt failed",
                    errorResolution = "Please ensure the TV is powered on and retry."
                )
            }
        }
    }

    fun reconnectToPairedDevice() {
        val paired = _uiState.value.previouslyPairedDevice
        if (paired != null) {
            connectToDevice(paired)
        } else {
            startDiscovery()
        }
    }

    fun submitPin(pin: String) {
        val device = currentConnectingDevice
        if (device != null) {
            updateState {
                it.copy(
                    connectionStatus = ConnectionStatus.PAIRING,
                    pinInput = pin
                )
            }
            try {
                Log.d(tag, "Sending pairing key to device")
                device.sendPairingKey(pin.trim())
            } catch (e: Exception) {
                Log.e(tag, "Error sending pairing key", e)
                updateState {
                    it.copy(
                        errorMessage = "Failed to send PIN to TV",
                        errorResolution = "Please check the PIN on your TV screen and retry."
                    )
                }
            }
        }
    }

    fun cancelPairing() {
        val device = currentConnectingDevice
        try {
            device?.cancelPairing()
            device?.disconnect()
            device?.removeListener(this)
        } catch (e: Exception) {
            Log.w(tag, "Error cancelling pairing", e)
        }
        currentConnectingDevice = null
        updateState {
            it.copy(
                connectionStatus = ConnectionStatus.DISCONNECTED,
                activePairingDevice = null,
                activePairingType = null,
                pinInput = ""
            )
        }
    }

    fun disconnectDevice() {
        val device = currentConnectingDevice
        try {
            device?.removeListener(this)
            device?.disconnect()
        } catch (e: Exception) {
            Log.w(tag, "Error disconnecting device", e)
        }
        currentConnectingDevice = null
        updateState {
            it.copy(
                connectionStatus = ConnectionStatus.DISCONNECTED,
                connectedDevice = null,
                activePairingDevice = null,
                activePairingType = null
            )
        }
    }

    fun clearError() {
        updateState { it.copy(errorMessage = null, errorResolution = null) }
    }

    private fun findConnectableDeviceByIdOrIp(id: String, ip: String): ConnectableDevice? {
        val dm = discoveryManager ?: return null
        val devices = dm.allDevices?.values ?: dm.compatibleDevices?.values ?: return null
        return devices.firstOrNull { it.id == id || it.ipAddress == ip }
    }

    // ==========================================
    // DiscoveryManagerListener Callbacks
    // ==========================================

    override fun onDeviceAdded(manager: DiscoveryManager?, device: ConnectableDevice?) {
        if (device == null) return
        Log.d(tag, "Device added: ${device.friendlyName} (${device.modelName}) at ${device.ipAddress}")
        handleDeviceDiscovered(device)
    }

    override fun onDeviceUpdated(manager: DiscoveryManager?, device: ConnectableDevice?) {
        if (device == null) return
        Log.d(tag, "Device updated: ${device.friendlyName} (${device.modelName}) at ${device.ipAddress}")
        handleDeviceDiscovered(device)
    }

    override fun onDeviceRemoved(manager: DiscoveryManager?, device: ConnectableDevice?) {
        if (device == null) return
        Log.d(tag, "Device removed from network: ${device.friendlyName} at ${device.ipAddress}")
        scope.launch {
            val updated = _uiState.value.discoveredDevices.filterNot { it.id == (device.id ?: device.ipAddress) }
            val wasConnected = _uiState.value.connectedDevice?.id == (device.id ?: device.ipAddress)

            updateState {
                it.copy(
                    discoveredDevices = updated,
                    connectedDevice = if (wasConnected) null else it.connectedDevice,
                    connectionStatus = if (wasConnected) ConnectionStatus.DISCONNECTED else it.connectionStatus,
                    errorMessage = if (wasConnected) "TV '${device.friendlyName}' disconnected from network." else it.errorMessage,
                    errorResolution = if (wasConnected) "Check if the TV was turned off or network changed." else it.errorResolution
                )
            }
        }
    }

    override fun onDiscoveryFailed(manager: DiscoveryManager?, error: ServiceCommandError?) {
        val msg = error?.message ?: "Unknown discovery error"
        Log.e(tag, "Discovery failed: $msg (code: ${error?.code})")
        scope.launch {
            updateState {
                it.copy(
                    isSearching = false,
                    errorMessage = "TV discovery failed ($msg)",
                    errorResolution = "Ensure phone and TV are on the same local Wi-Fi network and retry."
                )
            }
        }
    }

    private fun handleDeviceDiscovered(device: ConnectableDevice) {
        scope.launch {
            val isWebOs = device.getServiceByName(WebOSTVService.ID) != null ||
                    device.services?.any { it is WebOSTVService } == true ||
                    device.modelName?.contains("webOS", ignoreCase = true) == true ||
                    device.friendlyName?.contains("LG", ignoreCase = true) == true

            val devId = device.id ?: device.ipAddress ?: return@launch
            val tvDevice = TvDevice(
                id = devId,
                friendlyName = device.friendlyName ?: "LG Smart TV",
                modelName = device.modelName ?: "",
                modelNumber = device.modelNumber ?: "",
                ipAddress = device.ipAddress ?: "",
                isWebOS = isWebOs,
                isConnected = device.isConnected,
                connectableDevice = device
            )

            val currentList = _uiState.value.discoveredDevices.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.id == devId }
            if (existingIndex >= 0) {
                currentList[existingIndex] = tvDevice
            } else {
                currentList.add(tvDevice)
            }

            val newStatus = if (_uiState.value.connectionStatus == ConnectionStatus.SEARCHING ||
                _uiState.value.connectionStatus == ConnectionStatus.DISCONNECTED) {
                ConnectionStatus.TV_FOUND
            } else {
                _uiState.value.connectionStatus
            }

            updateState {
                it.copy(
                    discoveredDevices = currentList,
                    connectionStatus = newStatus
                )
            }
        }
    }

    // ==========================================
    // ConnectableDeviceListener Callbacks
    // ==========================================

    override fun onDeviceReady(device: ConnectableDevice?) {
        if (device == null) return
        Log.i(tag, "Device ready & connected: ${device.friendlyName} (${device.modelName})")
        scope.launch {
            val devId = device.id ?: device.ipAddress ?: ""
            val tvDevice = TvDevice(
                id = devId,
                friendlyName = device.friendlyName ?: "LG TV",
                modelName = device.modelName ?: "",
                modelNumber = device.modelNumber ?: "",
                ipAddress = device.ipAddress ?: "",
                isWebOS = true,
                isConnected = true,
                connectableDevice = device
            )

            savePairedDevice(tvDevice)
            stopDiscovery()

            updateState {
                it.copy(
                    connectionStatus = ConnectionStatus.CONNECTED,
                    connectedDevice = tvDevice,
                    activePairingDevice = null,
                    activePairingType = null,
                    pinInput = "",
                    errorMessage = null,
                    errorResolution = null
                )
            }
        }
    }

    override fun onDeviceDisconnected(device: ConnectableDevice?) {
        Log.i(tag, "Device disconnected: ${device?.friendlyName}")
        scope.launch {
            updateState {
                it.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    connectedDevice = null,
                    activePairingDevice = null,
                    activePairingType = null
                )
            }
        }
    }

    override fun onPairingRequired(
        device: ConnectableDevice?,
        service: DeviceService?,
        pairingType: DeviceService.PairingType?
    ) {
        Log.i(tag, "Pairing required for ${device?.friendlyName}: type = $pairingType")
        scope.launch {
            val devId = device?.id ?: device?.ipAddress ?: ""
            val tvDevice = TvDevice(
                id = devId,
                friendlyName = device?.friendlyName ?: "LG TV",
                modelName = device?.modelName ?: "",
                modelNumber = device?.modelNumber ?: "",
                ipAddress = device?.ipAddress ?: "",
                isWebOS = true,
                connectableDevice = device
            )

            val resolvedMode = when (pairingType) {
                DeviceService.PairingType.PIN_CODE -> com.example.data.WebOsPairingMode.PIN
                DeviceService.PairingType.MIXED -> com.example.data.WebOsPairingMode.COMBINED
                DeviceService.PairingType.FIRST_SCREEN -> com.example.data.WebOsPairingMode.PROMPT
                else -> com.example.data.WebOsPairingMode.PROMPT
            }

            updateState {
                it.copy(
                    connectionStatus = ConnectionStatus.PAIRING_REQUIRED,
                    activePairingDevice = tvDevice,
                    activePairingType = pairingType,
                    pairingMode = resolvedMode,
                    errorMessage = null,
                    errorResolution = null
                )
            }
        }
    }

    override fun onCapabilityUpdated(
        device: ConnectableDevice?,
        added: MutableList<String>?,
        removed: MutableList<String>?
    ) {
        Log.d(tag, "Capabilities updated for ${device?.friendlyName}: +${added?.size} -${removed?.size}")
    }

    override fun onConnectionFailed(device: ConnectableDevice?, error: ServiceCommandError?) {
        val errorMsg = error?.message ?: "Connection failed"
        Log.e(tag, "Connection failed for ${device?.friendlyName}: $errorMsg")
        scope.launch {
            updateState {
                it.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    activePairingDevice = null,
                    activePairingType = null,
                    errorMessage = "Could not connect to ${device?.friendlyName ?: "TV"}",
                    errorResolution = if (errorMsg.contains("pairing", ignoreCase = true) || errorMsg.contains("reject", ignoreCase = true)) {
                        "Pairing request was declined or timed out. Please try again and accept on the TV."
                    } else {
                        "Ensure the TV is powered on, connected to the same Wi-Fi network, and retry."
                    }
                )
            }
        }
    }

    // ==========================================
    // URL / Browser Casting to LG webOS TV
    // ==========================================

    fun launchUrlOnTv(url: String, onResult: ((Boolean, String?) -> Unit)? = null) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            updateState {
                it.copy(
                    linkSendErrorMessage = "Please enter or paste a valid link.",
                    linkSendSuccessMessage = null,
                    isSendingLink = false
                )
            }
            onResult?.invoke(false, "Empty URL")
            return
        }

        val formattedUrl = if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            "https://$trimmed"
        } else {
            trimmed
        }

        val activeDevice = currentConnectingDevice
            ?: _uiState.value.connectedDevice?.connectableDevice
            ?: findConnectableDeviceByIdOrIp(
                _uiState.value.connectedDevice?.id ?: "",
                _uiState.value.connectedDevice?.ipAddress ?: ""
            )

        if (activeDevice == null || !activeDevice.isConnected) {
            updateState {
                it.copy(
                    linkSendErrorMessage = "TV is not connected. Please connect to your LG TV first.",
                    linkSendSuccessMessage = null,
                    isSendingLink = false
                )
            }
            onResult?.invoke(false, "TV not connected")
            return
        }

        updateState {
            it.copy(
                isSendingLink = true,
                linkSendErrorMessage = null,
                linkSendSuccessMessage = null
            )
        }

        val launcher: Launcher? = activeDevice.getCapability(Launcher::class.java)
            ?: activeDevice.launcher
            ?: (activeDevice.getServiceByName(WebOSTVService.ID) as? Launcher)
            ?: (activeDevice.services?.firstOrNull { it is Launcher } as? Launcher)

        if (launcher == null) {
            Log.w(tag, "Launcher capability not directly found on device, attempting WebOSTVService fallback")
            val webOs = (activeDevice.getServiceByName(WebOSTVService.ID) as? WebOSTVService)
            if (webOs != null) {
                webOs.launchBrowser(formattedUrl, object : Launcher.AppLaunchListener {
                    override fun onSuccess(session: LaunchSession?) {
                        Log.i(tag, "Opened $formattedUrl via WebOSTVService")
                        handleLaunchSuccess(formattedUrl)
                        onResult?.invoke(true, null)
                    }

                    override fun onError(error: ServiceCommandError?) {
                        val errMsg = error?.message ?: "TV could not open browser"
                        Log.e(tag, "Error opening browser via WebOSTVService: $errMsg")
                        handleLaunchError(errMsg)
                        onResult?.invoke(false, errMsg)
                    }
                })
                return
            }

            val errorMsg = "TV does not support remote browser launch"
            handleLaunchError(errorMsg)
            onResult?.invoke(false, errorMsg)
            return
        }

        launcher.launchBrowser(formattedUrl, object : Launcher.AppLaunchListener {
            override fun onSuccess(session: LaunchSession?) {
                Log.i(tag, "Successfully launched browser on TV with URL: $formattedUrl")
                handleLaunchSuccess(formattedUrl)
                onResult?.invoke(true, null)
            }

            override fun onError(error: ServiceCommandError?) {
                val errMsg = error?.message ?: "TV failed to launch browser"
                Log.e(tag, "Failed to launch browser: $errMsg")
                handleLaunchError(errMsg)
                onResult?.invoke(false, errMsg)
            }
        })
    }

    private fun handleLaunchSuccess(url: String) {
        scope.launch {
            updateState {
                it.copy(
                    isSendingLink = false,
                    lastLaunchedUrl = url,
                    linkSendSuccessMessage = "Sent to TV! Opening link in browser...",
                    linkSendErrorMessage = null
                )
            }
        }
    }

    private fun handleLaunchError(errorMsg: String) {
        scope.launch {
            updateState {
                it.copy(
                    isSendingLink = false,
                    linkSendErrorMessage = errorMsg,
                    linkSendSuccessMessage = null
                )
            }
        }
    }

    fun clearLinkFeedback() {
        updateState {
            it.copy(
                linkSendSuccessMessage = null,
                linkSendErrorMessage = null,
                sharedLinkNotification = null
            )
        }
    }

    fun setSharedLinkNotification(message: String?) {
        updateState {
            it.copy(sharedLinkNotification = message)
        }
    }

    fun onDestroy() {
        stopDiscovery()
        currentConnectingDevice?.removeListener(this)
        discoveryManager?.removeListener(this)
    }
}
