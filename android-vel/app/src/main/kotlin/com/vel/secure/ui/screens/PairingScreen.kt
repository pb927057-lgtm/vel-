package com.vel.secure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vel.secure.domain.model.DeviceRole
import com.vel.secure.ui.theme.*

@Composable
fun PairingScreen(
    currentRole: DeviceRole,
    localDeviceId: String,
    onPairTerminal: (targetCoreId: String, pairingCode: String) -> Unit,
    onApproveTerminal: (terminalId: String, deviceName: String, pairingCode: String) -> Unit,
    onCancel: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var pairingCode by remember { mutableStateOf("VEL-7892-AUTH") }
    var targetCoreIdInput by remember { mutableStateOf("") }
    var targetTerminalIdInput by remember { mutableStateOf("") }
    var deviceNameInput by remember { mutableStateOf("Terminal Móvil") }
    var copiedFeedback by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = VelBackgroundMain,
        topBar = {
            TopAppBar(
                title = { Text("Vinculación Criptográfica", color = VelTextPrimary) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera descriptiva según rol
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, VelBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = VelSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (currentRole == DeviceRole.CORE) "CORE: Autorización de Terminal" else "Terminal: Enlace con CORE",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (currentRole == DeviceRole.CORE) VelAccentCyan else VelPrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ambos teléfonos derivan la misma clave de sesión simétrica de 256 bits a partir del secreto compartido. El servidor de relevo nunca tiene acceso a estas claves.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VelTextSecondary
                    )
                }
            }

            // Identificador de este dispositivo local
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, VelBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = VelSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ID DE ESTE DISPOSITIVO (${if (currentRole == DeviceRole.CORE) "CORE" else "TERMINAL"})",
                        style = MaterialTheme.typography.labelSmall,
                        color = VelTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = localDeviceId,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = VelTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(localDeviceId))
                            copiedFeedback = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar ID",
                                tint = if (copiedFeedback) VelStatusSuccess else VelPrimaryBlue
                            )
                        }
                    }
                    if (copiedFeedback) {
                        Text("ID copiado al portapapeles", color = VelStatusSuccess, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Formulario específico para Terminal
            if (currentRole == DeviceRole.TERMINAL) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, VelBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = VelSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Datos del CORE Custodio",
                            style = MaterialTheme.typography.titleMedium,
                            color = VelTextPrimary
                        )

                        OutlinedTextField(
                            value = targetCoreIdInput,
                            onValueChange = { targetCoreIdInput = it.trim() },
                            label = { Text("ID del CORE Custodio") },
                            placeholder = { Text("Pega el ID del teléfono CORE aquí") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VelPrimaryBlue,
                                unfocusedBorderColor = VelBorder,
                                focusedTextColor = VelTextPrimary,
                                unfocusedTextColor = VelTextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = pairingCode,
                            onValueChange = { pairingCode = it },
                            label = { Text("Código de Vinculación Compartido") },
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
                                val target = if (targetCoreIdInput.isNotBlank()) targetCoreIdInput else "core-custodio-01"
                                onPairTerminal(target, pairingCode)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = VelPrimaryBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = VelTextPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vincular y Derivar Clave Simétrica", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Formulario específico para CORE
            if (currentRole == DeviceRole.CORE) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, VelBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = VelSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Aprobar Terminal en Base de Datos",
                            style = MaterialTheme.typography.titleMedium,
                            color = VelTextPrimary
                        )

                        OutlinedTextField(
                            value = targetTerminalIdInput,
                            onValueChange = { targetTerminalIdInput = it.trim() },
                            label = { Text("ID del Terminal a Autorizar") },
                            placeholder = { Text("Pega el ID del teléfono Terminal") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VelAccentCyan,
                                unfocusedBorderColor = VelBorder,
                                focusedTextColor = VelTextPrimary,
                                unfocusedTextColor = VelTextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = deviceNameInput,
                            onValueChange = { deviceNameInput = it },
                            label = { Text("Nombre del Dispositivo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VelAccentCyan,
                                unfocusedBorderColor = VelBorder,
                                focusedTextColor = VelTextPrimary,
                                unfocusedTextColor = VelTextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = pairingCode,
                            onValueChange = { pairingCode = it },
                            label = { Text("Código de Vinculación Compartido") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VelAccentCyan,
                                unfocusedBorderColor = VelBorder,
                                focusedTextColor = VelTextPrimary,
                                unfocusedTextColor = VelTextPrimary
                            )
                        )

                        Button(
                            onClick = {
                                val termId = if (targetTerminalIdInput.isNotBlank()) targetTerminalIdInput else "term-${java.util.UUID.randomUUID().toString().take(8)}"
                                onApproveTerminal(termId, deviceNameInput, pairingCode)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = VelAccentCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VelBackgroundMain)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aprobar Terminal en CORE", fontWeight = FontWeight.SemiBold, color = VelBackgroundMain)
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VelTextSecondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Volver")
            }
        }
    }
}
