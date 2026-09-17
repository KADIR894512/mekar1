package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen royal wallpaper inspired by the regal King Emperor crest
 * with golden dragon, majestic white lion, golden circular ring, imperial throne,
 * and the iconic crowned "K" winged crest shield.
 */
@Composable
fun RoyalWallpaperBackground(
    modifier: Modifier = Modifier,
    dimOverlayAlpha: Float = 0.45f,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ember_glow")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070709))
    ) {
        // Deep atmosphere background with radial golden ember glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.38f)
            val radius = size.width * 0.8f * pulse

            // Golden ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD4AF37).copy(alpha = 0.22f),
                        Color(0xFF8C6B1A).copy(alpha = 0.12f),
                        Color(0xFF1B160A).copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                center = center,
                radius = radius
            )

            // Top light beam
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFE082).copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.15f),
                    radius = size.width * 0.6f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.15f),
                radius = size.width * 0.6f
            )

            // Draw floating golden magical sparks / embers
            drawGoldenEmbers(size, pulse)

            // Draw the Royal Crest Circle & Frame
            drawRoyalCrestRings(center, size.width * 0.42f)
        }

        // Center Crest Graphics
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Imperial Crowned Shield with Wings & 'K'
                RoyalKCrestGraphic(pulse = pulse)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MASTER PRINTER",
                    color = Color(0xFFFFD54F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )

                Text(
                    text = "EMPEROR ACCESS & SECURITY",
                    color = Color(0xFFB0BEC5),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            }
        }

        // Translucent overlay so application cards remain sharp and readable
        if (dimOverlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0A0C).copy(alpha = dimOverlayAlpha))
            )
        }

        // Content slot
        content()
    }
}

/**
 * Procedurally draws floating golden embers
 */
private fun DrawScope.drawGoldenEmbers(size: Size, pulse: Float) {
    val emberCoords = listOf(
        Pair(0.2f, 0.25f), Pair(0.82f, 0.22f), Pair(0.15f, 0.45f),
        Pair(0.85f, 0.42f), Pair(0.35f, 0.18f), Pair(0.65f, 0.16f),
        Pair(0.28f, 0.62f), Pair(0.72f, 0.58f), Pair(0.48f, 0.12f),
        Pair(0.12f, 0.75f), Pair(0.88f, 0.72f)
    )

    emberCoords.forEachIndexed { index, (rx, ry) ->
        val x = size.width * rx
        val y = size.height * ry + ((index % 3) * 6 * pulse)
        val r = (3f + (index % 4) * 2f) * (0.8f + (pulse * 0.2f))
        val alpha = (0.35f + (index % 5) * 0.12f).coerceIn(0.1f, 0.85f)

        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = alpha),
            radius = r,
            center = Offset(x, y)
        )
    }
}

/**
 * Draws the celestial golden rings matching the lion & dragon halo
 */
private fun DrawScope.drawRoyalCrestRings(center: Offset, radius: Float) {
    // Outer golden halo ring
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFFFFD700),
                Color(0xFFB8860B),
                Color(0xFFFFF8DC),
                Color(0xFFDAA520),
                Color(0xFFFFD700)
            ),
            center = center
        ),
        center = center,
        radius = radius,
        style = Stroke(width = 3.5f)
    )

    // Inner patterned thin ring
    drawCircle(
        color = Color(0xFFD4AF37).copy(alpha = 0.4f),
        center = center,
        radius = radius - 14f,
        style = Stroke(width = 1.5f)
    )

    // Decorative golden ticks on the circular frame
    val tickCount = 24
    for (i in 0 until tickCount) {
        val angle = (i * 2 * PI / tickCount).toFloat()
        val startX = center.x + (radius - 12f) * cos(angle)
        val startY = center.y + (radius - 12f) * sin(angle)
        val endX = center.x + (radius + 2f) * cos(angle)
        val endY = center.y + (radius + 2f) * sin(angle)

        drawLine(
            color = Color(0xFFFFE082).copy(alpha = 0.5f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Winged Golden Shield with Imperial Crown and "K" Emblem
 */
@Composable
fun RoyalKCrestGraphic(
    modifier: Modifier = Modifier,
    pulse: Float = 1f
) {
    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2

            // Golden Wing left
            val leftWing = Path().apply {
                moveTo(cx - 20f, cy + 10f)
                cubicTo(cx - 75f, cy - 25f, cx - 70f, cy + 30f, cx - 15f, cy + 35f)
                close()
            }
            drawPath(
                path = leftWing,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFD700).copy(alpha = 0.7f), Color(0xFF8B6508).copy(alpha = 0.4f)),
                    start = Offset(cx - 75f, cy),
                    end = Offset(cx, cy)
                )
            )

            // Golden Wing right
            val rightWing = Path().apply {
                moveTo(cx + 20f, cy + 10f)
                cubicTo(cx + 75f, cy - 25f, cx + 70f, cy + 30f, cx + 15f, cy + 35f)
                close()
            }
            drawPath(
                path = rightWing,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFD700).copy(alpha = 0.7f), Color(0xFF8B6508).copy(alpha = 0.4f)),
                    start = Offset(cx + 75f, cy),
                    end = Offset(cx, cy)
                )
            )

            // Imperial Heraldic Shield
            val shieldPath = Path().apply {
                moveTo(cx - 40f, cy - 30f)
                lineTo(cx + 40f, cy - 30f)
                lineTo(cx + 40f, cy + 15f)
                cubicTo(cx + 35f, cy + 45f, cx, cy + 62f, cx, cy + 62f)
                cubicTo(cx, cy + 62f, cx - 35f, cy + 45f, cx - 40f, cy + 15f)
                close()
            }

            // Shield background
            drawPath(
                path = shieldPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E2028), Color(0xFF0C0D12), Color(0xFF050507))
                )
            )

            // Shield Gold Bevel Border
            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFF8DC), Color(0xFFFFD700), Color(0xFFB8860B), Color(0xFFFFF8DC))
                ),
                style = Stroke(width = 4f)
            )

            // Imperial Crown atop the shield
            val crownPath = Path().apply {
                moveTo(cx - 28f, cy - 34f)
                lineTo(cx - 32f, cy - 48f)
                lineTo(cx - 16f, cy - 40f)
                lineTo(cx, cy - 54f)
                lineTo(cx + 16f, cy - 40f)
                lineTo(cx + 32f, cy - 48f)
                lineTo(cx + 28f, cy - 34f)
                close()
            }
            drawPath(
                path = crownPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFEA79), Color(0xFFD4AF37))
                )
            )
            drawPath(
                path = crownPath,
                color = Color(0xFFFFFFFF).copy(alpha = 0.8f),
                style = Stroke(width = 1.5f)
            )
        }

        // Royal Golden "K" in center of shield
        Text(
            text = "K",
            color = Color(0xFFFFD54F),
            fontSize = 44.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
