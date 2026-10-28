package com.vel.secure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vel.secure.domain.model.ExecutionStatus
import com.vel.secure.ui.theme.*

@Composable
fun TerminalHomeScreen(
    deviceId: String,
    pairedCoreId: String?,
    isRevoked: Boolean,
    revocationReason: String?,
    channelConnected: Boolean,
    currentOpStatus: ExecutionStatus?,
    onNavigateToPairing: () -> Unit,
    onExecuteOperation: (String) -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    Scaffold(
        containerColor = VelBackgroundMain,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "VEL Terminal",
                            fontWeight = FontWeight.Bold,
                            color = VelPrimaryBlue,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Cliente Móvil • ID: ${deviceId.take(8)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = VelTextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDiagnostics) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Diagnóstico",
                            tint = VelPrimaryBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VelSurface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ALERTA DE REVOCACIÓN (Bloqueo absoluto)
            if (isRevoked) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, VelStatusError, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = VelSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = VelStatusError)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DISPOSITIVO REVOCADO",
                                    color = VelStatusError,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Este Terminal ha sido revocado por la autoridad CORE. Todas las operaciones protegidas y claves de sesión han sido invalidadas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VelTextPrimary
                            )
                            if (revocationReason != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Motivo: $revocationReason",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VelStatusError
                                )
                            }
                        }
                    }
                }
            }

            // Estado de Conexión y Vinculación
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, VelBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = VelSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Estado Criptográfico",
                            style = MaterialTheme.typography.titleMedium,
                            color = VelTextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Autoridad CORE", color = VelTextSecondary)
                            Text(
                                text = pairedCoreId?.let { "${it.take(8)}... (Vinculado)" } ?: "No vinculado",
                                color = if (pairedCoreId != null) VelAccentCyan else VelStatusWarning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Canal Cifrado (WSS)", color = VelTextSecondary)
                            Text(
                                text = if (channelConnected) "Establecido • E2EE" else "Desconectado",
                                color = if (channelConnected) VelStatusSuccess else VelStatusWarning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Autorización", color = VelTextSecondary)
                            Text(
                                text = if (isRevoked) "Revocado" else if (pairedCoreId != null) "Válida" else "Sin credencial",
                                color = if (isRevoked) VelStatusError else if (pairedCoreId != null) VelStatusSuccess else VelStatusWarning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Si no está vinculado
            if (pairedCoreId == null && !isRevoked) {
                item {
                    Button(
                        onClick = onNavigateToPairing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VelPrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vincular con VEL CORE", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Operaciones Protegidas Disponibles (Solo si está vinculado y no revocado)
            if (pairedCoreId != null && !isRevoked) {
                item {
                    Text(
                        text = "Operaciones Protegidas (E2EE)",
                        style = MaterialTheme.typography.titleMedium,
                        color = VelTextPrimary
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onExecuteOperation("LOCK_DEVICE") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = VelSurfaceElevated),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VelBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = VelAccentCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Bloqueo Inmediato de Emergencia", color = VelTextPrimary)
                        }

                        Button(
                            onClick = { onExecuteOperation("SHIELD_DATA") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = VelSurfaceElevated),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VelBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = VelPrimaryBlue)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Activar Escudo Criptográfico", color = VelTextPrimary)
                        }

                        Button(
                            onClick = { onExecuteOperation("VERIFY_INTEGRITY") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = VelSurfaceElevated),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VelBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = VelAccentCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Verificar Integridad de CORE", color = VelTextPrimary)
                        }
                    }
                }

                // Estado de Confirmación de Ejecución de la última operación
                if (currentOpStatus != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, VelBorder, RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = VelSurface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Estado de Solicitud",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VelTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentOpStatus.label,
                                    fontWeight = FontWeight.Bold,
                                    color = when (currentOpStatus) {
                                        ExecutionStatus.EXECUTED_AND_CONFIRMED -> VelStatusSuccess
                                        ExecutionStatus.PENDING_CORE_CONFIRMATION, ExecutionStatus.RECEIVED_BY_SERVER, ExecutionStatus.SENT, ExecutionStatus.PREPARING -> VelStatusWarning
                                        ExecutionStatus.REJECTED, ExecutionStatus.NOT_EXECUTED, ExecutionStatus.COMMUNICATION_ERROR -> VelStatusError
                                        ExecutionStatus.UNCERTAIN -> VelStatusWarning
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
