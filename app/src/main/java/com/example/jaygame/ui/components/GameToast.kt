package com.example.jaygame.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Toast message types with colors.
 */
enum class ToastType(val bgStart: Color, val bgEnd: Color, val textColor: Color) {
    INFO(Color(0xFF1A3A5C), Color(0xFF0D2440), Color(0xFF8BD4FF)),
    SUCCESS(Color(0xFF1A4A2A), Color(0xFF0D3018), Color(0xFF6DBF67)),
    WARNING(Color(0xFF4A3A1A), Color(0xFF332808), Color(0xFFFFD54F)),
    ERROR(Color(0xFF4A1A1A), Color(0xFF330808), Color(0xFFFF6B6B)),
    GOLD(Color(0xFF3A2A0A), Color(0xFF251A05), Color(0xFFFFD700)),
}

data class ToastMessage(
    val text: String,
    val type: ToastType = ToastType.INFO,
    val durationMs: Long = 2000L,
    val id: Long = System.currentTimeMillis(),
)

/**
 * Global toast manager — call GameToastManager.show() from anywhere.
 */
object GameToastManager {
    private val _currentToast = MutableStateFlow<ToastMessage?>(null)
    val currentToast: StateFlow<ToastMessage?> = _currentToast.asStateFlow()

    fun show(text: String, type: ToastType = ToastType.INFO, durationMs: Long = 2000L) {
        _currentToast.value = ToastMessage(text, type, durationMs)
    }

    fun dismiss() {
        _currentToast.value = null
    }
}

/**
 * Renders toast messages at the top of the screen.
 * Place this in your root layout (e.g., BattleScreen or MainScreen).
 */
@Composable
fun GameToastHost(modifier: Modifier = Modifier) {
    val toast by GameToastManager.currentToast.collectAsState()

    LaunchedEffect(toast?.id) {
        val t = toast ?: return@LaunchedEffect
        delay(t.durationMs)
        if (GameToastManager.currentToast.value?.id == t.id) {
            GameToastManager.dismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        ) {
            val t = toast ?: return@AnimatedVisibility
            Box(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(t.type.bgStart, t.type.bgEnd),
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = t.text,
                    color = t.type.textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
