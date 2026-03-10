package com.example.jaygame.ui.battle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.ui.theme.*

/** Compact top bar: Pause | WAVE | Timer | Enemy count | HP bar */
@Composable
fun BattleTopHud(onPauseClick: () -> Unit = {}) {
    val battle by BattleBridge.state.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Compact dark panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepDark.copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Pause button
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurface)
                    .border(1.dp, BorderGlow, RoundedCornerShape(6.dp))
                    .clickable(onClick = onPauseClick),
                contentAlignment = Alignment.Center,
            ) {
                Text("☰", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Wave pill badge
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(NeonCyan.copy(alpha = 0.25f), NeonCyan.copy(alpha = 0.10f))
                        ),
                        RoundedCornerShape(12.dp),
                    )
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "WAVE ${battle.currentWave}",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            // Timer
            val totalSeconds = battle.elapsedTime.toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            Text(
                text = "%02d:%02d".format(minutes, seconds),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )

            // Enemy count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (battle.enemyCount > 80) NeonRed else NeonGreen,
                            CircleShape,
                        ),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${battle.enemyCount}/${battle.maxEnemyCount}",
                    color = if (battle.enemyCount > 80) NeonRed else Color.White.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // HP bar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(DarkSurface),
                ) {
                    val hpFraction = if (battle.maxHP > 0)
                        (battle.playerHP.toFloat() / battle.maxHP).coerceIn(0f, 1f) else 1f
                    val hpColor = when {
                        battle.playerHP <= battle.maxHP / 4 -> NeonRed
                        battle.playerHP <= battle.maxHP / 2 -> WarningYellow
                        else -> NeonGreen
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(hpFraction)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(hpColor),
                    )
                }
                Text(
                    text = "HP ${battle.playerHP}",
                    color = if (battle.playerHP <= battle.maxHP / 4) NeonRed else Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Thin separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            NeonCyan.copy(alpha = 0.3f),
                            NeonCyan.copy(alpha = 0.3f),
                            Color.Transparent,
                        )
                    )
                ),
        )
    }
}

/** Bottom control bar: [조합] [소환] [강화] */
@Composable
fun BattleBottomHud() {
    val battle by BattleBridge.state.collectAsState()
    val gridState by BattleBridge.gridState.collectAsState()
    val selectedTile by BattleBridge.selectedTile.collectAsState()
    val gridFull = gridState.all { it.unitDefId >= 0 }
    val canSummon = battle.sp >= battle.summonCost && !gridFull

    // Check if any merge is possible
    val canMerge = gridState.any { it.canMerge }

    // Check if upgrade is possible for selected unit
    val selectedUnit = if (selectedTile >= 0) gridState.getOrNull(selectedTile) else null
    val canUpgrade = selectedUnit != null && selectedUnit.unitDefId >= 0 &&
        selectedUnit.level in 1..6 &&
        battle.sp >= BattleBridge.getUpgradeCost(selectedUnit.level)
    val upgradeCost = if (selectedUnit != null && selectedUnit.level in 1..6)
        BattleBridge.getUpgradeCost(selectedUnit.level) else 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Subtle divider line above bottom HUD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Gold.copy(alpha = 0.3f),
                            Gold.copy(alpha = 0.3f),
                            Color.Transparent,
                        )
                    )
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepDark.copy(alpha = 0.85f))
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // SP bar - taller with shimmer label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(Gold.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("SP", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurface),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((battle.sp / 500f).coerceIn(0f, 1f))
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFFAA00),
                                        Gold,
                                        Color(0xFFFFDD44),
                                    )
                                )
                            ),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${battle.sp.toInt()}",
                    color = Gold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Three buttons row: [조합] [소환] [강화]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Merge button
                ActionButton(
                    text = "\uC870\uD569",
                    subText = if (canMerge) "\uAC00\uB2A5!" else "",
                    enabled = canMerge,
                    colors = listOf(Color(0xFFFF8F00), Color(0xFFE65100)),
                    borderColor = Color(0xFFFFB74D),
                    onClick = {
                        // Find first mergeable tile and merge it
                        val mergeIdx = gridState.indexOfFirst { it.canMerge }
                        if (mergeIdx >= 0) BattleBridge.requestMerge(mergeIdx)
                    },
                    modifier = Modifier.weight(1f),
                )

                // Summon button (center, taller, dominant)
                SummonButton(
                    cost = battle.summonCost,
                    enabled = canSummon,
                    gridFull = gridFull,
                    onClick = { BattleBridge.requestSummon() },
                    modifier = Modifier.weight(1.5f),
                )

                // Upgrade button
                ActionButton(
                    text = "\uAC15\uD654",
                    subText = if (canUpgrade) "${upgradeCost} SP" else if (selectedTile < 0) "\uC120\uD0DD" else "",
                    enabled = canUpgrade,
                    colors = listOf(Color(0xFF1565C0), Color(0xFF0D47A1)),
                    borderColor = Color(0xFF42A5F5),
                    onClick = {
                        if (selectedTile >= 0) BattleBridge.requestUpgrade(selectedTile)
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            // Boss timer — fixed height slot to prevent layout jump
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.height(16.dp)) {
                if (battle.isBossRound) {
                    Text(
                        text = "BOSS ${battle.bossTimeRemaining.toInt()}s",
                        color = NeonRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    subText: String,
    enabled: Boolean,
    colors: List<Color>,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "actionScale",
    )

    val bgColors = if (enabled) colors else listOf(Color(0xFF2A2A3A), Color(0xFF1A1A28))
    val actualBorderColor = if (enabled) borderColor.copy(alpha = 0.6f) else Color(0xFF3A3A4A)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (enabled) 4.dp else 1.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(bgColors),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                )
                // Inner shadow effect for depth
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                )
                drawRoundRect(
                    color = actualBorderColor,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f.dp.toPx()),
                )
            }
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                color = if (enabled) Color.White else Color(0xFF666680),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            if (subText.isNotEmpty()) {
                Text(
                    text = subText,
                    color = if (enabled) Color.White.copy(alpha = 0.8f) else Color(0xFF555570),
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
fun SummonButton(
    cost: Int,
    enabled: Boolean,
    gridFull: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "summonScale",
    )

    // Pulsing glow animation when affordable
    val infiniteTransition = rememberInfiniteTransition(label = "summonGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    val bgColors = if (enabled) {
        if (isPressed) listOf(Color(0xFF1B5E20), Color(0xFF388E3C))
        else listOf(Color(0xFF43A047), Color(0xFF2E7D32))
    } else {
        listOf(Color(0xFF2A2A3A), Color(0xFF1A1A28))
    }

    val glowBorderColor = if (enabled) NeonGreen.copy(alpha = glowAlpha) else Color(0xFF3A3A4A)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (enabled) 8.dp else 2.dp, RoundedCornerShape(14.dp),
                ambientColor = if (enabled) NeonGreen.copy(alpha = 0.3f) else Color.Black,
                spotColor = if (enabled) NeonGreen.copy(alpha = 0.4f) else Color.Black,
            )
            .clip(RoundedCornerShape(14.dp))
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(bgColors),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                )
                // Top highlight for 3D effect
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                    ),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                )
                // Border glow
                drawRoundRect(
                    color = glowBorderColor,
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
            }
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "\u2694\uFE0F \uC18C\uD658",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = if (gridFull) "FULL" else "$cost SP",
                color = if (gridFull) NeonRed else if (enabled) Color(0xFFE8F5E9) else Color(0xFF555570),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
