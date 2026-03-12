package com.example.jaygame.ui.battle

import android.graphics.BitmapFactory
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.ui.theme.*

// ── Warm medieval theme colors ──
private val WoodBrown = Color(0xFF5C3A1E)
private val WoodBrownLight = Color(0xFF7B5230)
private val WoodBrownDark = Color(0xFF3E2510)
private val PanelBg = Color(0xFF2A1A0C).copy(alpha = 0.88f)
private val PanelBgDark = Color(0xFF1A0F06).copy(alpha = 0.92f)
private val BadgeBg = Color(0xFF1E1208).copy(alpha = 0.9f)
private val GoldBright = Color(0xFFFFD700)
private val GoldDark = Color(0xFFB8860B)
private val GreenTeal = Color(0xFF2E8B57)
private val GreenTealDark = Color(0xFF1B5E3A)
private val BlueSky = Color(0xFF4A90D9)
private val BlueSkyDark = Color(0xFF2C5F99)
private val OrangeBright = Color(0xFFFF8C00)
private val OrangeDark = Color(0xFFCC6600)

// ── Top HUD — centered compact badge (WAVE | timer | enemy count) ──

@Composable
fun BattleTopHud(onPauseClick: () -> Unit = {}) {
    val battle by BattleBridge.state.collectAsState()
    val gridState by BattleBridge.gridState.collectAsState()
    val unitCount = gridState.count { it.unitDefId >= 0 }
    val battleSpeed by BattleBridge.battleSpeed.collectAsState()

    val totalSeconds = battle.elapsedTime.toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── WAVE badge (sits on top of main box) ──
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF5C3A1E), Color(0xFF3D2510))
                        )
                    )
                    .border(1.5.dp, Color(0xFFAA7744), RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "WAVE ${battle.currentWave}",
                    color = GoldBright,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(modifier = Modifier.height((-4).dp))

            // ── Main box: timer + difficulty ──
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF4A3018), Color(0xFF2E1C0C))
                        )
                    )
                    .border(1.5.dp, Color(0xFF8B6040), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // Timer
                    Text(
                        text = "%02d:%02d".format(minutes, seconds),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )

                    // Difficulty badge (settings-style)
                    val diffInfo = when (BattleBridge.difficulty.value) {
                        0 -> "\uC26C\uC6C0" to NeonGreen
                        2 -> "\uC5B4\uB824\uC6C0" to NeonRed
                        else -> "\uBCF4\uD1B5" to NeonCyan
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, diffInfo.second.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .background(diffInfo.second.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = diffInfo.first,
                            color = diffInfo.second,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Enemy count bar (skull + count) ──
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "\uD83D\uDC80",
                    fontSize = 14.sp,
                )
                Text(
                    text = "${battle.enemyCount} / ${battle.maxEnemyCount}",
                    color = if (battle.enemyCount > 80) NeonRed else Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Top-right buttons: speed + menu
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Speed button
            val speedLabel = when (battleSpeed) {
                2f -> "x2"
                4f -> "x4"
                8f -> "x8"
                else -> "x1"
            }
            val speedColor = when (battleSpeed) {
                2f -> GoldBright
                4f -> Color(0xFFFF6B6B)
                8f -> Color(0xFFFF3333)
                else -> Color.White
            }
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, speedColor.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = { BattleBridge.cycleBattleSpeed() }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    speedLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = speedColor,
                )
            }

            // Menu button
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, WoodBrown.copy(alpha = 0.4f), CircleShape)
                    .clickable(onClick = onPauseClick),
                contentAlignment = Alignment.Center,
            ) {
                Text("\u2630", fontSize = 21.sp, color = Color.White)
            }
        }
    }

    // Boss timer
    if (battle.isBossRound) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeonRed.copy(alpha = 0.15f))
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\u26A0 BOSS ${battle.bossTimeRemaining.toInt()}s",
                color = NeonRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ── Bottom HUD — matches reference: resource bar → [신화][소환][도박] → [강화] ──

@Composable
fun BattleBottomHud(
    onGambleClick: () -> Unit = {},
    onBuyClick: () -> Unit = {},
    onUpgradeClick: () -> Unit = {},
) {
    val battle by BattleBridge.state.collectAsState()
    val gridState by BattleBridge.gridState.collectAsState()
    val gridFull = gridState.all { it.unitDefId >= 0 }
    val canSummon = battle.sp >= battle.summonCost && !gridFull
    val canGamble = battle.sp >= 10
    val canMerge = gridState.any { it.canMerge }
    val context = LocalContext.current

    val goldIcon = remember { loadAssetBitmap(context, "raw/ui/icon_gold.png") }
    val btnSummonImg = remember { loadAssetBitmap(context, "raw/ui/btn_summon.png") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Resource row: SP bar + merge floating button ──
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (goldIcon != null) {
                    Image(bitmap = goldIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(WoodBrownDark)
                        .border(1.dp, WoodBrown.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((battle.sp / 500f).coerceIn(0f, 1f))
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(listOf(GoldDark, GoldBright, Color(0xFFFFEE88)))),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((battle.sp / 500f).coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("${battle.sp.toInt()}", color = GoldBright, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

            // Floating merge button — above SP bar, right side
            if (canMerge) {
                val inf = rememberInfiniteTransition(label = "mg")
                val glow by inf.animateFloat(0.7f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "mg")
                val mergeShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 12.dp)
                        .graphicsLayer { translationY = -52.dp.toPx(); alpha = glow }
                        .shadow(8.dp, mergeShape, ambientColor = GoldBright.copy(alpha = 0.3f), spotColor = GoldBright.copy(alpha = 0.4f))
                        .clip(mergeShape)
                        .background(Brush.verticalGradient(listOf(GoldBright, GoldDark)))
                        .border(2.dp, Color(0xFFFFEE88).copy(alpha = 0.8f), mergeShape)
                        .clickable {
                            val tiles = BattleBridge.gridState.value
                            for (i in tiles.indices) { if (tiles[i].canMerge) BattleBridge.requestMerge(i) }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "\u2728 조합",
                        color = WoodBrownDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }

        // ── Floating action buttons ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Top row: 구매 | 소환 | 도박
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // 구매 (Buy) — orange
                WarmButton(
                    topText = "\uD83D\uDECD",
                    bottomText = "\uAD6C\uB9E4",
                    enabled = true,
                    gradientTop = OrangeBright, gradientBot = OrangeDark,
                    borderColor = Color(0xFFFFAA44),
                    onClick = onBuyClick,
                    modifier = Modifier.weight(1f),
                    buttonHeight = 58.dp,
                )

                // 소환 (Summon) — BIG gold center
                SummonButton(
                    cost = battle.summonCost,
                    enabled = canSummon,
                    gridFull = gridFull,
                    onClick = { BattleBridge.requestSummon() },
                    modifier = Modifier.weight(1.6f),
                    goldIcon = goldIcon,
                )

                // 도박 (Gamble) — green
                WarmButton(
                    topText = "\uD83C\uDFB2",
                    bottomText = "\uB3C4\uBC15",
                    enabled = canGamble,
                    gradientTop = GreenTeal, gradientBot = GreenTealDark,
                    borderColor = Color(0xFF66CC88),
                    onClick = onGambleClick,
                    modifier = Modifier.weight(1f),
                    buttonHeight = 58.dp,
                )
            }

            // Bottom row: 강화 (same width as 소환)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(modifier = Modifier.weight(1f))
                WarmButton(
                    topText = "\u2B06",
                    bottomText = "\uAC15\uD654",
                    enabled = true,
                    gradientTop = BlueSky, gradientBot = BlueSkyDark,
                    borderColor = Color(0xFF6AB0FF),
                    onClick = onUpgradeClick,
                    modifier = Modifier.weight(1.6f),
                    buttonHeight = 44.dp,
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

/** Load a bitmap from assets, returns null on failure */
private fun loadAssetBitmap(
    context: android.content.Context,
    path: String,
): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    } catch (e: Exception) { null }
}

// ── Warm-themed action button ──────────────────────────────

@Composable
private fun WarmButton(
    topText: String,
    bottomText: String,
    enabled: Boolean,
    gradientTop: Color,
    gradientBot: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 48.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "btnScale",
    )

    val bgTop = if (enabled) gradientTop else Color(0xFF3A3A3A)
    val bgBot = if (enabled) gradientBot else Color(0xFF2A2A2A)
    val border = if (enabled) borderColor.copy(alpha = 0.7f) else Color(0xFF4A4A4A)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(buttonHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                if (enabled) 10.dp else 2.dp,
                RoundedCornerShape(12.dp),
                ambientColor = if (enabled) gradientBot.copy(alpha = 0.4f) else Color.Black,
                spotColor = if (enabled) gradientBot.copy(alpha = 0.5f) else Color.Black,
            )
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(bgTop, bgBot)),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                )
                // Top highlight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.4f),
                )
                // Border
                drawRoundRect(
                    color = border,
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
                text = topText,
                fontSize = 18.sp,
            )
            Text(
                text = bottomText,
                color = if (enabled) Color.White else Color(0xFF888888),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ── Summon Button (center, large, gold with coin icon) ──────

@Composable
fun SummonButton(
    cost: Int,
    enabled: Boolean,
    gridFull: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    goldIcon: androidx.compose.ui.graphics.ImageBitmap? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "summonScale",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "summonGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    val bgTop = if (enabled) GoldBright else Color(0xFF3A3A3A)
    val bgBot = if (enabled) GoldDark else Color(0xFF2A2A2A)
    val borderCol = if (enabled) Color(0xFFFFEE88).copy(alpha = glowAlpha) else Color(0xFF4A4A4A)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(70.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                if (enabled) 12.dp else 2.dp,
                RoundedCornerShape(14.dp),
                ambientColor = if (enabled) GoldBright.copy(alpha = 0.4f) else Color.Black,
                spotColor = if (enabled) GoldBright.copy(alpha = 0.5f) else Color.Black,
            )
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(bgTop, bgBot)),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                )
                // Glossy highlight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.45f),
                )
                // Border
                drawRoundRect(
                    color = borderCol,
                    cornerRadius = CornerRadius(12.dp.toPx()),
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
                text = "소환",
                color = if (enabled) WoodBrownDark else Color(0xFF888888),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (goldIcon != null) {
                    Image(
                        bitmap = goldIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = if (gridFull) "FULL" else "$cost",
                    color = if (gridFull) NeonRed
                    else if (enabled) WoodBrownDark.copy(alpha = 0.8f)
                    else Color(0xFF666666),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
