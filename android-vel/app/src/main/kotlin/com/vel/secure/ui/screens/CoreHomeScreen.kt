package com.vel.secure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.vel.secure.data.local.AuthorizedDeviceEntity
import com.vel.secure.ui.theme.*

@Composable
fun CoreHomeScreen(
    deviceId: String,
    serverConnected: Boolean,
    authorizedDevices: List<AuthorizedDeviceEntity>,
    onNavigateToPairing: () -> Unit,
    onNavigateToRevocation: (String) -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    Scaffold(
        containerColor = VelBackgroundMain,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "VEL CORE",
                            fontWeight = FontWeight.Bold,
                            color = VelAccentCyan,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Autoridad de Seguridad • ID: ${deviceId.take(8)}...",
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
                            tint = VelAccentCyan
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
            // Estado General del Sistema
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
                            text = "Estado del Sistema",
                            style = MaterialTheme.typography.titleMedium,
                            color = VelTextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Autoridad Local", color = VelTextSecondary)
                            Text("Activa • Keystore Protegido", color = VelStatusSuccess, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Conexión con Relevo", color = VelTextSecondary)
                            Text(
                                text = if (serverConnected) "Conectado WSS" else "Desconectado",
                                color = if (serverConnected) VelStatusSuccess else VelStatusWarning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Dispositivos Autorizados", color = VelTextSecondary)
                            Text(
                                text = "${authorizedDevices.count { it.authorizationStatus == "AUTHORIZED" }} activos",
                                color = VelAccentCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Acciones Rápidas
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateToPairing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = VelPrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = VelTextPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vincular Terminal", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Lista de Terminales Registrados
            item {
                Text(
                    text = "Terminales Registrados",
                    style = MaterialTheme.typography.titleMedium,
                    color = VelTextPrimary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (authorizedDevices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay ningún Terminal vinculado a este CORE.",
                            color = VelTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(authorizedDevices) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, VelBorder, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = VelSurfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = device.deviceName,
                                        fontWeight = FontWeight.Bold,
                                        color = VelTextPrimary
                                    )
                                    Text(
                                        text = "ID: ${device.deviceId.take(12)}...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VelTextSecondary
                                    )
                                }
                                val isRevoked = device.authorizationStatus == "REVOKED"
                                Text(
                                    text = if (isRevoked) "REVOCADO" else "AUTORIZADO",
                                    color = if (isRevoked) VelStatusError else VelStatusSuccess,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (device.authorizationStatus != "REVOKED") {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { onNavigateToRevocation(device.deviceId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VelStatusError),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, VelStatusError)
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, tint = VelStatusError)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cambiar contraseña y revocar dispositivos autorizados",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
