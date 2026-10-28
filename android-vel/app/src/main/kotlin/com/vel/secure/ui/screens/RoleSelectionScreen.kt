package com.vel.secure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vel.secure.domain.model.DeviceRole
import com.vel.secure.ui.components.VelLogo
import com.vel.secure.ui.theme.*

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (DeviceRole) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VelBackgroundMain)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            VelLogo(size = 64.dp, showWordmark = true)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Configuración del Dispositivo",
                style = MaterialTheme.typography.titleLarge,
                color = VelTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Selecciona el rol criptográfico para esta instalación.",
                style = MaterialTheme.typography.bodyMedium,
                color = VelTextSecondary
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta VEL CORE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, VelBorder, RoundedCornerShape(12.dp))
                    .clickable { onRoleSelected(DeviceRole.CORE) },
                colors = CardDefaults.cardColors(containerColor = VelSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VEL CORE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelAccentCyan
                        )
                        Text(
                            text = "Autoridad",
                            fontSize = 12.sp,
                            color = VelAccentCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dispositivo custodio y autoridad de seguridad.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VelTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onRoleSelected(DeviceRole.CORE) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VelPrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Configurar como CORE",
                            fontWeight = FontWeight.SemiBold,
                            color = VelTextPrimary
                        )
                    }
                }
            }

            // Tarjeta VEL Terminal
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, VelBorder, RoundedCornerShape(12.dp))
                    .clickable { onRoleSelected(DeviceRole.TERMINAL) },
                colors = CardDefaults.cardColors(containerColor = VelSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VEL Terminal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelPrimaryBlue
                        )
                        Text(
                            text = "Cliente Móvil",
                            fontSize = 12.sp,
                            color = VelPrimaryBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dispositivo móvil para conexión segura con CORE.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VelTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onRoleSelected(DeviceRole.TERMINAL) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VelPrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Configurar como Terminal",
                            fontWeight = FontWeight.SemiBold,
                            color = VelTextPrimary
                        )
                    }
                }
            }
        }

        Text(
            text = "El rol seleccionado queda vinculado a la identidad criptográfica de hardware.",
            style = MaterialTheme.typography.labelSmall,
            color = VelTextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}
