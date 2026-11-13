package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.connectsdk.service.DeviceService
import com.example.data.ConnectionState
import com.example.data.ConnectionStatus
import com.example.data.TvCompanionUiState
import com.example.data.TvDevice
import com.example.data.WebOsPairingMode
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningLight
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoseErrorLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun MainScreen(viewModel: TvCompanionViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val pinInput by viewModel.pinState.collectAsState()
    val linkInput by viewModel.linkInputState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Branding Header reactively driven by ConnectionState
                AppHeader(connectionState = connectionState)

                Spacer(modifier = Modifier.height(10.dp))

                // Connection Status Display Badge ('Searching...', 'TV Found', 'Pairing Required', 'Pairing...', 'Connected', 'Disconnected')
                ConnectionStatusBadge(connectionState = connectionState)

                Spacer(modifier = Modifier.height(16.dp))

                // Error Banner if present
                if (uiState.errorMessage != null) {
                    ErrorBanner(
                        title = uiState.errorMessage ?: "",
                        resolution = uiState.errorResolution,
                        onDismiss = { viewModel.clearError() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Wi-Fi Status warning if disconnected
                if (!uiState.isWifiConnected) {
                    WifiWarningBanner()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Screen State Router driven reactively by ConnectionState
                when (connectionState) {
                    is ConnectionState.Connected -> {
                        ConnectedView(
                            device = (connectionState as ConnectionState.Connected).device,
                            uiState = uiState,
                            linkInput = linkInput,
                            onLinkChanged = { viewModel.onLinkInputChanged(it) },
                            onSendLink = { viewModel.sendLinkToTv(it) },
                            onClearLink = { viewModel.clearLinkInput() },
                            onDismissFeedback = { viewModel.clearLinkFeedback() },
                            onDisconnect = { viewModel.disconnectDevice() },
                            onSearchAgain = {
                                viewModel.disconnectDevice()
                                viewModel.startDiscovery()
                            },
                            onUnpair = { viewModel.unpairDevice() }
                        )
                    }
                    else -> {
                        DiscoveryAndPairingView(
                            uiState = uiState,
                            linkInput = linkInput,
                            onLinkChanged = { viewModel.onLinkInputChanged(it) },
                            onSendLink = { viewModel.sendLinkToTv(it) },
                            onClearLink = { viewModel.clearLinkInput() },
                            onDismissFeedback = { viewModel.clearLinkFeedback() },
                            onStartSearch = { viewModel.startDiscovery() },
                            onStopSearch = { viewModel.stopDiscovery() },
                            onConnect = { tv -> viewModel.connectToDevice(tv) },
                            onReconnect = { viewModel.reconnectToPairedDevice() },
                            onUnpair = { viewModel.unpairDevice() }
                        )
                    }
                }
            }

            // Pairing Prompt / PIN Dialog
            if (connectionState is ConnectionState.Pairing) {
                val pairing = connectionState as ConnectionState.Pairing
                PairingDialog(
                    pairingMode = pairing.mode,
                    device = pairing.device ?: uiState.activePairingDevice,
                    pinInput = pinInput,
                    isPairingInProgress = uiState.connectionStatus == ConnectionStatus.PAIRING,
                    onPinChanged = { viewModel.onPinChanged(it) },
                    onSubmitPin = { viewModel.submitPin() },
                    onCancel = { viewModel.cancelPairing() }
                )
            }
        }
    }
}

@Composable
fun AppHeader(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate900,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "mybrowser TV Companion",
                        tint = BrandCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "MYBROWSER",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isConnected) "Connected to LG webOS TV" else "Connect to your LG TV",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AppHeader(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    AppHeader(
        isConnected = connectionState is ConnectionState.Connected,
        modifier = modifier
    )
}

@Composable
fun AppHeader(
    connectionStatus: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    AppHeader(
        isConnected = connectionStatus == ConnectionStatus.CONNECTED,
        modifier = modifier
    )
}

@Composable
fun ConnectionStatusBadge(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (connectionState) {
        is ConnectionState.Searching -> Triple(
            Slate800,
            BrandCyan,
            BrandCyan.copy(alpha = 0.6f)
        )
        is ConnectionState.Discovered -> Triple(
            Slate800,
            EmeraldGreen,
            EmeraldGreen.copy(alpha = 0.6f)
        )
        is ConnectionState.Pairing -> Triple(
            Slate800,
            AmberWarning,
            AmberWarning.copy(alpha = 0.6f)
        )
        is ConnectionState.Connected -> Triple(
            EmeraldGreenLight,
            EmeraldGreenDark,
            EmeraldGreen
        )
        is ConnectionState.Disconnected -> Triple(
            Slate100,
            Slate600,
            Slate200
        )
    }

    val isSpinning = connectionState is ConnectionState.Searching || connectionState is ConnectionState.Pairing

    Surface(
        modifier = modifier.testTag("connection_status_display"),
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSpinning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = textColor
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(textColor)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = connectionState.displayText,
                modifier = Modifier.testTag("connection_status_text"),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = textColor
            )
        }
    }
}

@Composable
fun ConnectionStatusBadge(
    status: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    val state = when (status) {
        ConnectionStatus.SEARCHING -> ConnectionState.Searching
        ConnectionStatus.TV_FOUND -> ConnectionState.Discovered()
        ConnectionStatus.PAIRING_REQUIRED, ConnectionStatus.PAIRING -> ConnectionState.Pairing()
        ConnectionStatus.CONNECTED -> ConnectionState.Connected(TvDevice("", "LG TV"))
        ConnectionStatus.DISCONNECTED -> ConnectionState.Disconnected()
    }
    ConnectionStatusBadge(connectionState = state, modifier = modifier)
}

@Composable
fun WifiWarningBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wifi_warning_banner"),
        shape = RoundedCornerShape(12.dp),
        color = AmberWarningLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = "Wi-Fi Disconnected",
                tint = AmberWarning,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Wi-Fi Not Connected",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Slate900
                )
                Text(
                    text = "Connect your phone to the same Wi-Fi network as your LG webOS TV to discover it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700
                )
            }
        }
    }
}

@Composable
fun ErrorBanner(title: String, resolution: String?, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("error_banner"),
        shape = RoundedCornerShape(12.dp),
        color = RoseErrorLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, RoseError)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = RoseError,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Slate900
                )
                if (resolution != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = resolution,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss error",
                    tint = Slate600,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun DiscoveryAndPairingView(
    uiState: TvCompanionUiState,
    linkInput: String,
    onLinkChanged: (String) -> Unit,
    onSendLink: (String) -> Unit,
    onClearLink: () -> Unit,
    onDismissFeedback: () -> Unit,
    onStartSearch: () -> Unit,
    onStopSearch: () -> Unit,
    onConnect: (TvDevice) -> Unit,
    onReconnect: () -> Unit,
    onUnpair: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discovery_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Web Link / Chrome Shared Link Section
        item {
            SendWebLinkCard(
                linkInput = linkInput,
                isSending = uiState.isSendingLink,
                successMessage = uiState.linkSendSuccessMessage,
                errorMessage = uiState.linkSendErrorMessage,
                notificationMessage = uiState.sharedLinkNotification,
                isConnected = false,
                onLinkChanged = onLinkChanged,
                onSendLink = onSendLink,
                onClearLink = onClearLink,
                onDismissFeedback = onDismissFeedback
            )
        }

        // Search Control Button
        item {
            Button(
                onClick = {
                    if (uiState.isSearching) onStopSearch() else onStartSearch()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("search_tvs_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isSearching) Slate700 else BrandBlue
                )
            ) {
                if (uiState.isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Searching...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Search for TVs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        // Previously connected section (if available)
        if (uiState.previouslyPairedDevice != null) {
            item {
                Text(
                    text = "Previously connected",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )

                PairedTvCard(
                    device = uiState.previouslyPairedDevice,
                    onReconnect = onReconnect,
                    onUnpair = onUnpair
                )
            }
        }

        // Available TVs Section
        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = Slate200
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Available TVs",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                if (uiState.isSearching) {
                    Text(
                        text = "Scanning local Wi-Fi...",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandCyan
                    )
                }
            }
        }

        // Discovered Devices List
        if (uiState.discoveredDevices.isEmpty()) {
            item {
                EmptyStateCard(
                    isSearching = uiState.isSearching,
                    onRetrySearch = onStartSearch
                )
            }
        } else {
            items(
                items = uiState.discoveredDevices,
                key = { it.id }
            ) { tv ->
                DiscoveredTvCard(
                    device = tv,
                    isConnecting = uiState.connectionStatus == ConnectionStatus.PAIRING &&
                            uiState.activePairingDevice?.id == tv.id,
                    onConnect = { onConnect(tv) }
                )
            }
        }

        // Bottom padding spacer
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PairedTvCard(
    device: TvDevice,
    onReconnect: () -> Unit,
    onUnpair: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("paired_tv_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate100,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "TV",
                            tint = Slate800,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.friendlyName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (device.modelName.isNotEmpty()) {
                        Text(
                            text = if (device.modelNumber.isNotEmpty()) "${device.modelName} (${device.modelNumber})" else device.modelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                    if (device.ipAddress.isNotEmpty()) {
                        Text(
                            text = "IP: ${device.ipAddress}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate600
                        )
                    }
                }

                IconButton(
                    onClick = onUnpair,
                    modifier = Modifier.testTag("unpair_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Unpair TV",
                        tint = Slate600
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onReconnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("reconnect_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RECONNECT",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun DiscoveredTvCard(
    device: TvDevice,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tv_card_${device.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate100,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "TV",
                        tint = BrandBlue,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.friendlyName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (device.modelName.isNotEmpty()) device.modelName else "LG webOS TV",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
                if (device.ipAddress.isNotEmpty()) {
                    Text(
                        text = device.ipAddress,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onConnect,
                enabled = !isConnecting,
                modifier = Modifier
                    .height(42.dp)
                    .testTag("connect_button_${device.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "CONNECT",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(isSearching: Boolean, onRetrySearch: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_state_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                    color = BrandBlue
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Searching for TVs...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Make sure your LG TV is turned on and connected to the same Wi-Fi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600,
                    textAlign = TextAlign.Center
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = Slate600,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "TV not found",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Make sure your phone and TV are connected to the same Wi-Fi network.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onRetrySearch,
                    modifier = Modifier.testTag("search_again_empty_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "SEARCH AGAIN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuickLinkChip(label: String, url: String, onSelect: (String) -> Unit) {
    Surface(
        onClick = { onSelect(url) },
        shape = RoundedCornerShape(16.dp),
        color = Slate100,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.testTag("quick_link_${label.lowercase()}")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = Slate800,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SendWebLinkCard(
    linkInput: String,
    isSending: Boolean,
    successMessage: String?,
    errorMessage: String?,
    notificationMessage: String?,
    isConnected: Boolean,
    onLinkChanged: (String) -> Unit,
    onSendLink: (String) -> Unit,
    onClearLink: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("send_link_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isConnected) BrandBlue.copy(alpha = 0.4f) else Slate200
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandBlue.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Web Link",
                            tint = BrandBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Send Web Link to TV",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isConnected) "Launch any website on your LG TV browser" else "Connect TV below to open web link on screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notification from Chrome Share Sheet
            if (notificationMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandBlue.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().testTag("shared_link_notification_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = notificationMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Slate900,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissFeedback, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp), tint = Slate600)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Success Banner
            if (successMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldGreenLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
                    modifier = Modifier.fillMaxWidth().testTag("link_send_success_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = EmeraldGreenDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = successMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = EmeraldGreenDark,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissFeedback, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp), tint = Slate600)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Error Banner
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RoseErrorLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoseError),
                    modifier = Modifier.fillMaxWidth().testTag("link_send_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = RoseError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = RoseError,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissFeedback, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp), tint = Slate600)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Manual Link Input TextField
            OutlinedTextField(
                value = linkInput,
                onValueChange = onLinkChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_link_input_field"),
                placeholder = {
                    Text(
                        text = "e.g. https://youtube.com or paste link",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Link",
                        tint = if (linkInput.isNotBlank()) BrandBlue else Slate600
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (linkInput.isNotEmpty()) {
                            IconButton(
                                onClick = onClearLink,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("clear_link_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear link",
                                    tint = Slate600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    val extracted = com.example.util.UrlUtils.extractUrl(clip) ?: clip.trim()
                                    onLinkChanged(extracted)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("paste_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste from clipboard",
                                tint = BrandBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = if (isConnected) ImeAction.Send else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (isConnected && linkInput.isNotBlank()) {
                            onSendLink(linkInput)
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Paste button
                OutlinedButton(
                    onClick = {
                        val clip = clipboardManager.getText()?.text
                        if (!clip.isNullOrBlank()) {
                            val extracted = com.example.util.UrlUtils.extractUrl(clip) ?: clip.trim()
                            onLinkChanged(extracted)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("paste_clipboard_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Paste Link",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                // Send to TV button
                Button(
                    onClick = { onSendLink(linkInput) },
                    enabled = isConnected && linkInput.isNotBlank() && !isSending,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("send_link_to_tv_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        disabledContainerColor = Slate200
                    )
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sending...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isConnected) "Send to TV" else "Connect First",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Quick Shortcut Chips
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Popular Websites:",
                style = MaterialTheme.typography.labelSmall,
                color = Slate600
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickLinkChip(label = "YouTube", url = "https://youtube.com", onSelect = onLinkChanged)
                QuickLinkChip(label = "Google", url = "https://google.com", onSelect = onLinkChanged)
                QuickLinkChip(label = "Twitch", url = "https://twitch.tv", onSelect = onLinkChanged)
                QuickLinkChip(label = "Wikipedia", url = "https://wikipedia.org", onSelect = onLinkChanged)
            }
        }
    }
}

@Composable
fun ConnectedView(
    device: TvDevice?,
    uiState: TvCompanionUiState,
    linkInput: String,
    onLinkChanged: (String) -> Unit,
    onSendLink: (String) -> Unit,
    onClearLink: () -> Unit,
    onDismissFeedback: () -> Unit,
    onDisconnect: () -> Unit,
    onSearchAgain: () -> Unit,
    onUnpair: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("connected_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Large Status Badge
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = EmeraldGreenLight,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen),
            modifier = Modifier.testTag("connected_status_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreen)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Connected",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreenDark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Prominent Send Web Link Card
        SendWebLinkCard(
            linkInput = linkInput,
            isSending = uiState.isSendingLink,
            successMessage = uiState.linkSendSuccessMessage,
            errorMessage = uiState.linkSendErrorMessage,
            notificationMessage = uiState.sharedLinkNotification,
            isConnected = true,
            onLinkChanged = onLinkChanged,
            onSendLink = onSendLink,
            onClearLink = onClearLink,
            onDismissFeedback = onDismissFeedback
        )

        Spacer(modifier = Modifier.height(20.dp))

        // TV Information Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connected_tv_details_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Slate200)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Slate100,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "LG TV",
                            tint = BrandBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = device?.friendlyName ?: "LG Smart TV",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (device?.modelName?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = device.modelName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = Slate600
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Slate200)
                Spacer(modifier = Modifier.height(20.dp))

                // Connection Metadata Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Connection",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate600
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Local Wi-Fi",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    if (device?.ipAddress?.isNotEmpty() == true) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "IP Address",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate600
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = device.ipAddress,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons
        Button(
            onClick = onDisconnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("disconnect_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Slate800)
        ) {
            Text(
                text = "DISCONNECT",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSearchAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("search_again_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "SEARCH AGAIN",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onUnpair,
            modifier = Modifier.testTag("unpair_tv_button")
        ) {
            Text(
                text = "Forget & Unpair this TV",
                style = MaterialTheme.typography.bodySmall.copy(color = RoseError)
            )
        }
    }
}

@Composable
fun PairingDialog(
    pairingMode: WebOsPairingMode,
    device: TvDevice?,
    pinInput: String,
    isPairingInProgress: Boolean,
    onPinChanged: (String) -> Unit,
    onSubmitPin: () -> Unit,
    onCancel: () -> Unit
) {
    val dialogTitle = when (pairingMode) {
        WebOsPairingMode.PIN -> "Enter TV PIN"
        WebOsPairingMode.COMBINED -> "Enter TV PIN or Accept Prompt"
        WebOsPairingMode.PROMPT -> "Pairing Required"
    }

    val isPinMode = pairingMode == WebOsPairingMode.PIN || pairingMode == WebOsPairingMode.COMBINED

    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.testTag("pairing_dialog"),
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dialogTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPairingInProgress) "Status: Pairing..." else "Status: Pairing Required",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isPairingInProgress) BrandBlue else AmberWarning
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (pairingMode) {
                    WebOsPairingMode.PIN -> {
                        Text(
                            text = "A PIN code has appeared on your LG TV screen. Enter the TV PIN below to complete pairing:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate700,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = onPinChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tv_pin_input"),
                            placeholder = { Text("TV PIN") },
                            singleLine = true,
                            enabled = !isPairingInProgress,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { if (pinInput.isNotBlank()) onSubmitPin() }
                            ),
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                textAlign = TextAlign.Center,
                                letterSpacing = 6.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        if (isPairingInProgress) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Pairing...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = BrandBlue
                                )
                            }
                        }
                    }

                    WebOsPairingMode.COMBINED -> {
                        Text(
                            text = "Select 'Accept' on your TV with the remote, or enter the TV PIN displayed on the TV screen below:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate700,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = onPinChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tv_pin_input"),
                            placeholder = { Text("TV PIN") },
                            singleLine = true,
                            enabled = !isPairingInProgress,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { if (pinInput.isNotBlank()) onSubmitPin() }
                            ),
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                textAlign = TextAlign.Center,
                                letterSpacing = 6.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BrandBlue
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isPairingInProgress) "Pairing..." else "Waiting for TV approval...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isPairingInProgress) BrandBlue else Slate600
                            )
                        }
                    }

                    WebOsPairingMode.PROMPT -> {
                        Text(
                            text = "A pairing request has been sent to your LG TV screen.",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Slate900,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Please follow the on-screen prompt on your TV and select 'Yes' or 'Accept' using your TV remote.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate700,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp,
                            color = BrandBlue
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Pairing... Waiting for approval on TV",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = BrandBlue
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isPinMode) {
                Button(
                    onClick = onSubmitPin,
                    enabled = pinInput.isNotBlank() && !isPairingInProgress,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier.testTag("submit_pin_button")
                ) {
                    Text("CONNECT")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("cancel_pairing_button")
            ) {
                Text("CANCEL")
            }
        }
    )
}
