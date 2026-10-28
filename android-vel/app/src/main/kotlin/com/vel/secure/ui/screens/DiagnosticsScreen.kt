package com.vel.secure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vel.secure.ui.theme.*

@Composable
fun DiagnosticsScreen(
    roleLabel: String,
    deviceId: String,
    webSocketConnected: Boolean,
    currentRelayUrl: String,
    onUpdateRelayUrl: (String) -> Unit,
    protocolVersion: Int = 1,
    lastConfirmedResult: String?,
    recentErrors: List<String>,
    onBack: () -> Unit
) {
    var relayUrlInput by remember { mutableStateOf(currentRelayUrl) }

    Scaffold(
        containerColor = VelBackgroundMain,
        topBar = {
            TopAppBar(
                title = { Text("Diagnóstico y Red", color = VelTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = VelTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VelSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Configuración del Servidor de Relevo",
                    style = MaterialTheme.typography.titleMedium,
                    color = VelAccentCyan
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, VelBorder, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = VelSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Para pruebas con 2 teléfonos en la misma red Wi-Fi, introduce la IP local de la computadora donde corre el servidor Ktor (ejemplo: ws://192.168.1.50:8443/v1/relay).",
                            style = MaterialTheme.typography.bodySmall,
                            color = VelTextSecondary
                        )

                        OutlinedTextField(
                            value = relayUrlInput,
                            onValueChange = { relayUrlInput = it },
                            label = { Text("URL Servidor Ktor") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VelPrimaryBlue,
                                unfocusedBorderColor = VelBorder,
                                focusedTextColor = VelTextPrimary,
                                unfocusedTextColor = VelTextPrimary
                            )
                        )

                        Button(
                            onClick = {
                                if (relayUrlInput.isNotBlank()) {
                                    onUpdateRelayUrl(relayUrlInput.trim())
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = VelPrimaryBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = VelTextPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar y Conectar al Servidor", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Información del Protocolo VEL",
                    style = MaterialTheme.typography.titleMedium,
                    color = VelAccentCyan
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, VelBorder, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = VelSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Rol Actual", color = VelTextSecondary)
                            Text(roleLabel, color = VelTextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ID Dispositivo", color = VelTextSecondary)
                            Text("${deviceId.take(16)}...", color = VelTextPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Versión Protocolo", color = VelTextSecondary)
                            Text("v$protocolVersion (E2EE AES-GCM / ECDH)", color = VelTextPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Canal WebSocket", color = VelTextSecondary)
                            Text(
                                text = if (webSocketConnected) "Conectado" else "Desconectado",
                                color = if (webSocketConnected) VelStatusSuccess else VelStatusWarning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Último Estado Confirmado", color = VelTextSecondary)
                            Text(
                                text = lastConfirmedResult ?: "Ninguno",
                                color = if (lastConfirmedResult != null) VelStatusSuccess else VelTextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Registro de Errores Técnicos (No sensibles)",
                    style = MaterialTheme.typography.titleMedium,
                    color = VelTextPrimary
                )
            }

            if (recentErrors.isEmpty()) {
                item {
                    Text(
                        text = "Sin incidencias técnicas registradas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VelTextSecondary
                    )
                }
            } else {
                items(recentErrors.size) { index ->
                    val error = recentErrors[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VelBorder, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = VelSurfaceElevated)
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = VelStatusError,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
