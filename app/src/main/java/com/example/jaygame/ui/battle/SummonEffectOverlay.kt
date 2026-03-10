package com.example.jaygame.ui.battle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.UNIT_DEFS_MAP
import com.example.jaygame.data.UnitGrade
import com.example.jaygame.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Summon result popup overlay.
 * Shows the summoned unit with grade color and animation.
 * "Lucky!" for HIGH+ grade units.
 * Auto-dismisses after 1.5 seconds.
 */
@Composable
fun SummonEffectOverlay() {
    val summonResult by BattleBridge.summonResult.collectAsState()
    val data = summonResult ?: return

    val unitDef = UNIT_DEFS_MAP[data.unitDefId]

    // Auto-dismiss
    LaunchedEffect(data) {
        delay(1500)
        BattleBridge.clearSummonResult()
    }

    val gradeColor = when (data.grade) {
        0 -> Color(0xFF9E9E9E)
        1 -> Color(0xFF42A5F5)
        2 -> Color(0xFFAB47BC)
        3 -> Color(0xFFFF8F00)
        4 -> Color(0xFFE94560)
        else -> Color.White
    }

    val gradeName = when (data.grade) {
        0 -> "하급"
        1 -> "중급"
        2 -> "상급"
        3 -> "최상급"
        4 -> "초월"
        else -> ""
    }

    val isRare = data.grade >= 2  // HIGH or above

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "summonScale",
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Background dim for rare summons
        if (isRare) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
            )
        }

        // Summon result card
        Column(
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkNavy.copy(alpha = 0.95f),
                            gradeColor.copy(alpha = 0.3f),
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // "Lucky!" text for rare summons
            if (isRare) {
                Text(
                    text = "Lucky",
                    color = Gold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Unit icon
            if (unitDef != null) {
                Image(
                    painter = painterResource(id = unitDef.iconRes),
                    contentDescription = unitDef.name,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Grade name with color
            Text(
                text = "$gradeName 소환 성공!",
                color = gradeColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            // Unit name
            if (unitDef != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = unitDef.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
