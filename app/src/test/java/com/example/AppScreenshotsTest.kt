package com.example

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConnectionState
import com.example.data.ConnectionStatus
import com.example.data.TvCompanionUiState
import com.example.data.TvDevice
import com.example.data.WebOsPairingMode
import com.example.ui.AppHeader
import com.example.ui.ConnectedView
import com.example.ui.ConnectionStatusBadge
import com.example.ui.DiscoveryAndPairingView
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate700
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AppScreenshotsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val livingRoomTv = TvDevice(
        id = "192.168.1.105",
        friendlyName = "Living Room LG OLED",
        modelName = "OLED55C1PUB",
        modelNumber = "OLED55C1PUB",
        ipAddress = "192.168.1.105",
        isWebOS = true,
        isConnected = true
    )

    private val bedroomTv = TvDevice(
        id = "192.168.1.112",
        friendlyName = "Bedroom LG Smart TV",
        modelName = "43UP75006LF",
        modelNumber = "43UP75006LF",
        ipAddress = "192.168.1.112",
        isWebOS = true,
        isConnected = false
    )

    @Test
    fun capture_discovery_screen() {
        val uiState = TvCompanionUiState(
            connectionStatus = ConnectionStatus.TV_FOUND,
            isSearching = false,
            isWifiConnected = true,
            discoveredDevices = listOf(livingRoomTv, bedroomTv),
            previouslyPairedDevice = livingRoomTv.copy(isConnected = false)
        ).syncConnectionState()

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ScreenShell(connectionState = uiState.connectionState) {
                    DiscoveryAndPairingView(
                        uiState = uiState,
                        linkInput = "",
                        onLinkChanged = {},
                        onSendLink = {},
                        onClearLink = {},
                        onDismissFeedback = {},
                        onStartSearch = {},
                        onStopSearch = {},
                        onConnect = {},
                        onReconnect = {},
                        onUnpair = {}
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "../docs/images/screen_discovery.png")
    }

    @Test
    fun capture_connected_screen() {
        val uiState = TvCompanionUiState(
            connectionStatus = ConnectionStatus.CONNECTED,
            isWifiConnected = true,
            connectedDevice = livingRoomTv,
            manualLinkInput = "https://youtube.com",
            linkSendSuccessMessage = "Sent to TV! Opening link in browser...",
            sharedLinkNotification = "Received link from Chrome. Opening on TV..."
        ).syncConnectionState()

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ScreenShell(connectionState = uiState.connectionState) {
                    ConnectedView(
                        device = livingRoomTv,
                        uiState = uiState,
                        linkInput = "https://youtube.com",
                        onLinkChanged = {},
                        onSendLink = {},
                        onClearLink = {},
                        onDismissFeedback = {},
                        onDisconnect = {},
                        onSearchAgain = {},
                        onUnpair = {}
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "../docs/images/screen_connected.png")
    }

    @Test
    fun capture_pairing_dialog() {
        val uiState = TvCompanionUiState(
            connectionStatus = ConnectionStatus.PAIRING_REQUIRED,
            isSearching = false,
            isWifiConnected = true,
            discoveredDevices = listOf(livingRoomTv, bedroomTv),
            activePairingDevice = livingRoomTv,
            pairingMode = WebOsPairingMode.PIN
        ).syncConnectionState()

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ScreenShell(connectionState = uiState.connectionState) {
                        DiscoveryAndPairingView(
                            uiState = uiState,
                            linkInput = "",
                            onLinkChanged = {},
                            onSendLink = {},
                            onClearLink = {},
                            onDismissFeedback = {},
                            onStartSearch = {},
                            onStopSearch = {},
                            onConnect = {},
                            onReconnect = {},
                            onUnpair = {}
                        )
                    }
                    // Scrim + Pairing Dialog card matching PairingDialog in MainScreen.kt
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
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
                                        text = "Enter TV PIN",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Status: Pairing Required",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = AmberWarning
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "A PIN code has appeared on your LG TV screen. Enter the TV PIN below to complete pairing:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate700,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                OutlinedTextField(
                                    value = "482910",
                                    onValueChange = {},
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 6.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(22.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {},
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("CANCEL")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {},
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BrandBlue
                                        )
                                    ) {
                                        Text("CONNECT")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "../docs/images/screen_pairing.png")
    }

    @Composable
    private fun ScreenShell(
        connectionState: ConnectionState,
        content: @Composable () -> Unit
    ) {
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
                    AppHeader(connectionState = connectionState)
                    Spacer(modifier = Modifier.height(10.dp))
                    ConnectionStatusBadge(connectionState = connectionState)
                    Spacer(modifier = Modifier.height(16.dp))
                    content()
                }
            }
        }
    }
}
