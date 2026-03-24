package com.example.jaygame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.StageDef
import com.example.jaygame.data.STAGES
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.SubText

// Derive stage card colors from StageDef to stay in sync
private val stageColors = STAGES.map { it.bgColors }

@Composable
fun StageCardPager(
    currentStageId: Int,
    unlockedStages: List<Int>,
    stageBestWaves: List<Int>,
    difficulty: Int,
    onStageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = currentStageId.coerceIn(0, STAGES.size - 1),
        pageCount = { STAGES.size },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onStageChanged(page)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        ) { page ->
            val stage = STAGES[page]
            val isUnlocked = page in unlockedStages
            val bestWave = stageBestWaves.getOrElse(page) { 0 }
            val colors = stageColors.getOrElse(page) { stageColors[0] }

            StageCardItem(
                stage = stage,
                isUnlocked = isUnlocked,
                bestWave = bestWave,
                difficulty = difficulty,
                gradientColors = colors,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            STAGES.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) NeonCyan
                            else SubText.copy(alpha = 0.4f)
                        ),
                )
            }
        }
    }
}

@Composable
private fun StageCardItem(
    stage: StageDef,
    isUnlocked: Boolean,
    bestWave: Int,
    difficulty: Int,
    gradientColors: List<Color>,
) {
    val difficultyText = when (difficulty) {
        0 -> "일반"
        1 -> "하드"
        2 -> "헬"
        else -> "일반"
    }

    // ── B2: 3D tilt/parallax ──
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var cardSize by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size: IntSize ->
                cardSize = size.width.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pos = event.changes.firstOrNull()?.position
                        if (pos != null) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            tiltX = ((pos.x - cx) / cx).coerceIn(-1f, 1f)
                            tiltY = ((pos.y - cy) / cy).coerceIn(-1f, 1f)
                        }
                        if (event.changes.all { !it.pressed }) {
                            tiltX = 0f
                            tiltY = 0f
                        }
                    }
                }
            }
            .graphicsLayer {
                rotationY = tiltX * 8f
                rotationX = -tiltY * 8f
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isUnlocked) gradientColors
                    else listOf(DimText, SubText),
                )
            )
            .padding(16.dp),
    ) {
        if (bestWave > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Gold.copy(alpha = 0.9f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "BEST",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = DeepDark,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stage.name,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = LightText,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isUnlocked) {
                    if (bestWave > 0) "ROUND $bestWave" else "미도전"
                } else {
                    "\uD83C\uDFC6 ${stage.unlockTrophies} 필요"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = if (isUnlocked) LightText else LightText.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
            if (isUnlocked) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "난이도: $difficultyText",
                    fontSize = 13.sp,
                    color = LightText.copy(alpha = 0.7f),
                )
            }
        }
    }
}
