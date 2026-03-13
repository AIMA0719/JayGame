package com.example.jaygame.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark
import kotlin.math.max

// Pre-allocated color for ripple wave
private val RippleWhite = Color.White

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: TextUnit = 16.sp,
    accentColor: Color = NeonRed,
    accentColorDark: Color = NeonRedDark,
    glowPulse: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "btnScale",
    )

    // Glow pulse animation for highlighted buttons
    val glowAlpha = if (glowPulse && enabled) {
        val transition = rememberInfiniteTransition(label = "glowPulse")
        val pulse by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "glowAlpha",
        )
        pulse
    } else 0f

    // Ripple wave animation state
    var rippleCenter by remember { mutableStateOf(Offset.Zero) }
    val rippleProgress = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0f) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                rippleCenter = interaction.pressPosition
                rippleProgress.snapTo(0f)
                rippleAlpha.snapTo(0.4f)
                rippleProgress.animateTo(1f, animationSpec = tween(400))
                rippleAlpha.animateTo(0f, animationSpec = tween(300))
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                clip = true
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            }
            .alpha(if (enabled) 1f else 0.4f)
            .drawBehind {
                val cr = CornerRadius(12.dp.toPx())

                // Outer glow layer for pulse effect
                if (glowPulse && enabled) {
                    val glowCr = CornerRadius(14.dp.toPx())
                    drawRoundRect(
                        color = accentColor.copy(alpha = glowAlpha * 0.4f),
                        cornerRadius = glowCr,
                        style = Stroke(width = (4f + glowAlpha * 4f).dp.toPx()),
                    )
                }

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (isPressed) listOf(accentColorDark, accentColor.copy(alpha = 0.8f))
                        else listOf(accentColor, accentColorDark),
                    ),
                    cornerRadius = cr,
                )
                drawRoundRect(
                    color = accentColor.copy(alpha = if (glowPulse) 0.5f + glowAlpha * 0.3f else 0.5f),
                    cornerRadius = cr,
                    style = Stroke(width = 1.5f.dp.toPx()),
                )

                // Ripple wave expanding from press point
                if (rippleAlpha.value > 0f) {
                    val maxRadius = max(size.width, size.height) * 1.2f
                    drawCircle(
                        color = RippleWhite.copy(alpha = rippleAlpha.value * 0.5f),
                        radius = maxRadius * rippleProgress.value,
                        center = rippleCenter,
                    )
                    // Bright ring at the edge of the wave
                    if (rippleProgress.value > 0.05f) {
                        drawCircle(
                            color = RippleWhite.copy(alpha = rippleAlpha.value),
                            radius = maxRadius * rippleProgress.value,
                            center = rippleCenter,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
            }
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ) else Modifier,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = LightText,
        )
    }
}
