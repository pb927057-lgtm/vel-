package com.vel.secure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vel.secure.domain.model.DeviceRole
import com.vel.secure.transport.RelayConnectionState
import com.vel.secure.ui.VelViewModel
import com.vel.secure.ui.screens.*
import com.vel.secure.ui.theme.VelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VelTheme {
                val viewModel: VelViewModel = viewModel()

                var currentScreen by remember { mutableStateOf("WELCOME") }
                val deviceRole by viewModel.deviceRole.collectAsState()
                val deviceId by viewModel.deviceId.collectAsState()
                val pairedCoreId by viewModel.pairedCoreId.collectAsState()
                val isRevoked by viewModel.isRevoked.collectAsState()
                val currentOpStatus by viewModel.currentOpStatus.collectAsState()
                val connectionState by viewModel.connectionState.collectAsState()
                val authorizedDevices by viewModel.authorizedDevices.collectAsState()
                val relayUrl by viewModel.relayUrl.collectAsState()

                var targetRevocationId by remember { mutableStateOf("") }
                val isConnected = connectionState is RelayConnectionState.Connected

                when (currentScreen) {
                    "WELCOME" -> {
                        WelcomeScreen(
                            onContinue = {
                                currentScreen = if (deviceRole == DeviceRole.UNCONFIGURED) {
                                    "ROLE_SELECTION"
                                } else if (deviceRole == DeviceRole.CORE) {
                                    "CORE_HOME"
                                } else {
                                    "TERMINAL_HOME"
                                }
                            }
                        )
                    }

                    "ROLE_SELECTION" -> {
                        RoleSelectionScreen(
                            onRoleSelected = { selected ->
                                viewModel.setRole(selected)
                                currentScreen = if (selected == DeviceRole.CORE) "CORE_HOME" else "TERMINAL_HOME"
                            }
                        )
                    }

                    "CORE_HOME" -> {
                        CoreHomeScreen(
                            deviceId = deviceId,
                            serverConnected = isConnected,
                            authorizedDevices = authorizedDevices,
                            onNavigateToPairing = { currentScreen = "PAIRING" },
                            onNavigateToRevocation = { targetId ->
                                targetRevocationId = targetId
                                currentScreen = "REVOCATION"
                            },
                            onNavigateToDiagnostics = { currentScreen = "DIAGNOSTICS" }
                        )
                    }

                    "TERMINAL_HOME" -> {
                        TerminalHomeScreen(
                            deviceId = deviceId,
                            pairedCoreId = pairedCoreId,
                            isRevoked = isRevoked,
                            revocationReason = if (isRevoked) "Revocado por CORE tras cambio de credencial maestra" else null,
                            channelConnected = isConnected,
                            currentOpStatus = currentOpStatus,
                            onNavigateToPairing = { currentScreen = "PAIRING" },
                            onExecuteOperation = { opType ->
                                // Ejecuta la canalización E2EE real con AES-256-GCM, sobre estructurado y WebSocket
                                viewModel.executeProtectedOperation(opType)
                            },
                            onNavigateToDiagnostics = { currentScreen = "DIAGNOSTICS" }
                        )
                    }

                    "PAIRING" -> {
                        PairingScreen(
                            currentRole = deviceRole,
                            localDeviceId = deviceId,
                            onPairTerminal = { targetCoreId, pairingCode ->
                                viewModel.applyPairing(pairingCode, targetCoreId)
                                currentScreen = "TERMINAL_HOME"
                            },
                            onApproveTerminal = { terminalId, deviceName, pairingCode ->
                                viewModel.approveTerminalAtCore(terminalId, deviceName, pairingCode)
                                currentScreen = "CORE_HOME"
                            },
                            onCancel = {
                                currentScreen = if (deviceRole == DeviceRole.CORE) "CORE_HOME" else "TERMINAL_HOME"
                            }
                        )
                    }

                    "REVOCATION" -> {
                        RevocationScreen(
                            targetTerminalId = targetRevocationId,
                            onConfirmRevocation = { targetId ->
                                viewModel.revokeDeviceAtCore(targetId)
                                currentScreen = "CORE_HOME"
                            },
                            onCancel = { currentScreen = "CORE_HOME" }
                        )
                    }

                    "DIAGNOSTICS" -> {
                        DiagnosticsScreen(
                            roleLabel = deviceRole.label,
                            deviceId = deviceId,
                            webSocketConnected = isConnected,
                            currentRelayUrl = relayUrl,
                            onUpdateRelayUrl = { newUrl ->
                                viewModel.updateRelayUrl(newUrl)
                            },
                            lastConfirmedResult = if (currentOpStatus != null) currentOpStatus?.label else "Sin operaciones",
                            recentErrors = emptyList(),
                            onBack = {
                                currentScreen = if (deviceRole == DeviceRole.CORE) "CORE_HOME" else "TERMINAL_HOME"
                            }
                        )
                    }
                }
            }
        }
    }
}
