package com.example.jaygame.ui.battle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.NeonCyan
import kotlinx.coroutines.delay

private val GradeColors = GradeColorsByIndex
private val GradeNames = GradeNamesByIndex

/**
 * Shows merge result with unit info:
 * - Normal merge: shows result unit name + grade
 * - Lucky merge (jackpot): golden explosion + "JACKPOT!" + unit info
 * Auto-dismiss after ~1.5 seconds.
 */
@Composable
fun MergeEffectOverlay() {
    val effect by BattleBridge.mergeEffect.collectAsState()
    val data = effect ?: return

    val unitDef = UNIT_DEFS_MAP[data.resultUnitId]
    val grade = if (data.resultUnitId >= 0) com.example.jaygame.data.unitGradeOf(data.resultUnitId) else -1
    val gradeColor = GradeColors.getOrElse(grade) { Color.White }
    val gradeName = GradeNames.getOrElse(grade) { "" }

    LaunchedEffect(data) {
        delay(1500)
        BattleBridge.clearMergeEffect()
    }

    val scale by animateFloatAsState(
        targetValue = if (data.isLucky) 1.3f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "mergeScale",
    )

    val alpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 1500),
        label = "mergeAlpha",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (data.isLucky) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Gold.copy(alpha = alpha * 0.3f)),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DarkNavy.copy(alpha = 0.95f),
                            gradeColor.copy(alpha = 0.3f),
                        )
                    )
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Lucky header
            if (data.isLucky) {
                Text(
                    text = "\u2605 JACKPOT! \u2605",
                    color = Gold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                Text(
                    text = "\u2726 조합 성공!",
                    color = NeonCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Unit icon
            if (unitDef != null) {
                Image(
                    painter = painterResource(id = unitDef.iconRes),
                    contentDescription = unitDef.name,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Grade + unit name
            Text(
                text = "$gradeName ${unitDef?.name ?: ""}",
                color = gradeColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}
