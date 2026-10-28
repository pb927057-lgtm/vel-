package com.vel.secure.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vel.secure.ui.theme.VelAccentCyan
import com.vel.secure.ui.theme.VelPrimaryBlue
import com.vel.secure.ui.theme.VelTextPrimary

@Composable
fun VelLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    showWordmark: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Arco inferior de enlace entre dispositivos (Turquesa de identidad)
            val arcPath = Path().apply {
                moveTo(w * 0.25f, h * 0.65f)
                cubicTo(
                    w * 0.35f, h * 0.85f,
                    w * 0.65f, h * 0.85f,
                    w * 0.75f, h * 0.65f
                )
            }
            drawPath(
                path = arcPath,
                color = VelAccentCyan,
                style = Stroke(width = w * 0.04f, cap = StrokeCap.Round)
            )

            // V tecnológica (Azul principal)
            val vPath = Path().apply {
                moveTo(w * 0.28f, h * 0.30f)
                lineTo(w * 0.50f, h * 0.72f)
                lineTo(w * 0.72f, h * 0.30f)
            }
            drawPath(
                path = vPath,
                color = VelPrimaryBlue,
                style = Stroke(
                    width = w * 0.06f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Nodo de autoridad (CORE - Turquesa)
            drawCircle(
                color = VelAccentCyan,
                radius = w * 0.065f,
                center = Offset(w * 0.28f, h * 0.30f)
            )

            // Nodo móvil (Terminal - Azul)
            drawCircle(
                color = VelPrimaryBlue,
                radius = w * 0.065f,
                center = Offset(w * 0.72f, h * 0.30f)
            )

            // Nodo central de verificación criptográfica
            drawCircle(
                color = VelTextPrimary,
                radius = w * 0.04f,
                center = Offset(w * 0.50f, h * 0.72f)
            )
        }

        if (showWordmark) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "VEL",
                color = VelTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 4.sp
            )
        }
    }
}
