package com.vel.secure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
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
fun RevocationScreen(
    targetTerminalId: String,
    onConfirmRevocation: (String) -> Unit,
    onCancel: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var revocationState by remember { mutableStateOf<String?>(null) } // "PENDING", "CONFIRMED", "ERROR"

    Scaffold(
        containerColor = VelBackgroundMain,
        topBar = {
            TopAppBar(
                title = { Text("Revocación de Dispositivo", color = VelTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = VelTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VelSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Advertencia Crítica
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, VelStatusError, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = VelSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = VelStatusError)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Consecuencias de la Revocación",
                                color = VelStatusError,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "1. El Terminal seleccionado (${targetTerminalId.take(12)}...) perderá inmediatamente la autorización criptográfica.\n" +
                                    "2. Su token individual será invalidado y destruido en CORE.\n" +
                                    "3. La revocación se transmitirá al relevo para rechazar futuras conexiones.\n" +
                                    "4. El dispositivo no podrá reactivarse con credenciales antiguas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VelTextPrimary
                        )
                    }
                }

                Text(
                    text = "Autenticación Fuerte del Propietario",
                    style = MaterialTheme.typography.titleMedium,
                    color = VelTextPrimary
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña maestra de CORE") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelAccentCyan,
                        unfocusedBorderColor = VelBorder,
                        focusedTextColor = VelTextPrimary,
                        unfocusedTextColor = VelTextPrimary
                    )
                )

                if (revocationState != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VelBorder, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = VelSurfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Estado de Revocación:",
                                style = MaterialTheme.typography.labelSmall,
                                color = VelTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (revocationState) {
                                    "PENDING" -> "Pendiente de confirmación remota del relevo..."
                                    "CONFIRMED" -> "Revocación confirmada localmente y en el servidor"
                                    else -> "Fallo en el procedimiento"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (revocationState == "CONFIRMED") VelStatusSuccess else VelStatusWarning
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        revocationState = "PENDING"
                        onConfirmRevocation(targetTerminalId)
                        revocationState = "CONFIRMED"
                    },
                    enabled = password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VelStatusError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Cambiar contraseña y revocar dispositivos autorizados",
                        fontWeight = FontWeight.Bold,
                        color = VelTextPrimary,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VelTextSecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar y Regresar")
                }
            }
        }
    }
}
