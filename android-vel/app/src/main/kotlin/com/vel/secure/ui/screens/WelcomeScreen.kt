package com.vel.secure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vel.secure.ui.components.VelLogo
import com.vel.secure.ui.theme.VelBackgroundMain
import com.vel.secure.ui.theme.VelTextSecondary
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1200) // Transición rápida sin esperas forzadas (Especificación 4)
        onContinue()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelBackgroundMain),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VelLogo(size = 110.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Protección de la información",
                color = VelTextSecondary,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
