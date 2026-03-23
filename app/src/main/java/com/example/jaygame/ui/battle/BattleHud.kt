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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.audio.SfxManager
import com.example.jaygame.audio.SoundEvent
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.engine.AttackRange
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.BossModifier
import com.example.jaygame.engine.DamageType
import com.example.jaygame.ui.components.roleColor
import com.example.jaygame.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.jaygame.util.HapticManager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog

// ── Warm medieval theme colors ──
private val DungeonPurple = Color(0xFFAB47BC)
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
private val BossRed = Color(0xFFFF4444)
private val BossOrange = Color(0xFFFF6644)
private val BossWaveBg = Brush.verticalGradient(listOf(Color(0xFF5C1818), Color(0xFF3D0C0C)))
private val BossMainBg = Brush.verticalGradient(listOf(Color(0xFF3D1515), Color(0xFF2E0C0C)))
private val NormalWaveBg = Brush.verticalGradient(listOf(Color(0xFF5C3A1E), Color(0xFF3D2510)))
private val NormalMainBg = Brush.verticalGradient(listOf(Color(0xFF4A3018), Color(0xFF2E1C0C)))

// ── Top HUD — centered compact badge (WAVE | timer | enemy count) ──

@Composable
fun BattleTopHud(onPauseClick: () -> Unit = {}) {
    val battle by BattleBridge.state.collectAsState()
    val battleSpeed by BattleBridge.battleSpeed.collectAsState()
    val isBoss = battle.isBossRound

    // Boss pulse animation for HUD accent
    val bossPulse = if (isBoss) {
        val transition = rememberInfiniteTransition(label = "bossHudPulse")
        val pulse by transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "bossHudPulseAlpha",
        )
        pulse
    } else 0f

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
            val waveBadgeBorder = if (isBoss) BossRed.copy(alpha = bossPulse) else Color(0xFFAA7744)
            val waveBadgeBg = if (isBoss) BossWaveBg else NormalWaveBg
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(waveBadgeBg)
                    .border(1.5.dp, waveBadgeBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isBoss) "\uD83D\uDC80 WAVE ${battle.currentWave}" else "WAVE ${battle.currentWave}",
                    color = if (isBoss) BossOrange else GoldBright,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(modifier = Modifier.height((-4).dp))

            // ── Main box: timer + difficulty ──
            val mainBoxBorder = if (isBoss) BossRed.copy(alpha = bossPulse * 0.8f) else Color(0xFF8B6040)
            val mainBoxBg = if (isBoss) BossMainBg else NormalMainBg
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(mainBoxBg)
                    .border(1.5.dp, mainBoxBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // Wave remaining time — boss waves get bigger, more urgent display
                    val waveMin = (battle.waveTimeRemaining / 60f).toInt()
                    val waveSec = (battle.waveTimeRemaining % 60f).toInt()
                    val isBossWave = battle.isBossRound
                    val waveTimeColor = when {
                        battle.waveTimeRemaining < 15f && isBossWave -> Color(0xFFFF0000) // critical
                        battle.waveTimeRemaining < 30f -> NeonRed
                        isBossWave -> Color(0xFFFF8844)
                        else -> Color.White
                    }
                    val timerSize = if (isBossWave && battle.waveTimeRemaining < 30f) 26.sp else 22.sp
                    Text(
                        text = "%d:%02d".format(waveMin, waveSec),
                        color = waveTimeColor,
                        fontSize = timerSize,
                        fontWeight = FontWeight.ExtraBold,
                    )

                    // Difficulty badge (settings-style)
                    val difficulty by BattleBridge.difficulty.collectAsState()
                    val diffInfo = when (difficulty) {
                        0 -> "초보" to NeonGreen
                        1 -> "숙련자" to NeonCyan
                        2 -> "고인물" to Gold
                        3 -> "썩은물" to NeonRed
                        4 -> "챌린저" to Color(0xFFFF3333)
                        else -> "초보" to NeonGreen
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

                    // Dungeon mode indicator
                    val dungeonId by BattleBridge.dungeonId.collectAsState()
                    if (dungeonId >= 0) {
                        val dDef = com.example.jaygame.data.ALL_DUNGEONS.getOrNull(dungeonId)
                        if (dDef != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, DungeonPurple.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .background(DungeonPurple.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = dDef.name,
                                    color = DungeonPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Enemy count bar (skull + count) ──
            val enemyBarBorder = if (isBoss) BossRed.copy(alpha = bossPulse * 0.5f) else Color.White.copy(alpha = 0.15f)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.dp, enemyBarBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (isBoss) "\u26A0\uFE0F" else "\uD83D\uDC80",
                    fontSize = 14.sp,
                )
                Text(
                    text = "${battle.enemyCount} / ${battle.maxEnemyCount}",
                    color = when {
                        battle.enemyCount > 80 -> NeonRed
                        isBoss -> BossOrange
                        else -> Color.White.copy(alpha = 0.9f)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Top-right: menu button only
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp)
                .size(45.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, WoodBrown.copy(alpha = 0.4f), CircleShape)
                .clickable(onClick = onPauseClick),
            contentAlignment = Alignment.Center,
        ) {
            // Show current speed indicator on menu button
            val speedLabel = when (battleSpeed) {
                2f -> "x2"
                4f -> "x4"
                8f -> "x8"
                else -> ""
            }
            if (speedLabel.isNotEmpty()) {
                Text(
                    speedLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when (battleSpeed) {
                        2f -> GoldBright
                        4f -> Color(0xFFFF6B6B)
                        8f -> Color(0xFFFF3333)
                        else -> Color.White
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 4.dp),
                )
            }
            Text("\u2630", fontSize = 21.sp, color = Color.White)
        }
    }

}

// ── Unit Card Strip — compact summary of all summoned units at top-left ──

@Composable
fun UnitCardStrip(modifier: Modifier = Modifier) {
    val gridState by BattleBridge.gridState.collectAsState()
    val activeUnits = remember(gridState) {
        gridState.filter { it.unitDefId >= 0 || it.blueprintId.isNotEmpty() }
    }
    if (activeUnits.isEmpty()) return

    // Group by grade for compact display
    val gradeGroups = remember(activeUnits) {
        activeUnits.groupBy { it.grade }
            .entries
            .sortedByDescending { it.key }
    }

    var showUnitListDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .widthIn(min = 80.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .clickable { showUnitListDialog = true }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Header: total count
        Text(
            text = "\uC720\uB2DB ${activeUnits.size}",  // 유닛
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )

        // Grade rows: colored dot + count
        for ((grade, units) in gradeGroups) {
            val gradeColor = GradeColorsByIndex.getOrElse(grade) { Color.Gray }
            val gradeName = GradeNamesByIndex.getOrElse(grade) { "?" }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(gradeColor),
                )
                Text(
                    text = "$gradeName ${units.size}",
                    color = gradeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (showUnitListDialog) {
        UnitListDialog(
            activeUnits = activeUnits,
            onDismiss = { showUnitListDialog = false },
        )
    }
}

@Composable
private fun UnitListDialog(
    activeUnits: List<com.example.jaygame.bridge.GridTileState>,
    onDismiss: () -> Unit,
) {
    val registry = remember { BlueprintRegistry.instance }

    // Group by blueprintId, count duplicates
    val unitGroups = remember(activeUnits) {
        activeUnits
            .filter { it.blueprintId.isNotEmpty() }
            .groupBy { it.blueprintId }
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, List<com.example.jaygame.bridge.GridTileState>>> {
                it.value.first().grade
            }.thenBy { it.key })
    }

    // State for detail dialog
    var selectedBlueprintId by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1008))
                .border(2.dp, Color(0xFF8B6040), RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            // Title
            Text(
                text = "\uC18C\uD658\uB41C \uC720\uB2DB (${activeUnits.size})",  // 소환된 유닛
                color = GoldBright,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable unit list
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for ((bpId, tiles) in unitGroups) {
                    val bp = registry.findById(bpId)
                    val tile = tiles.first()
                    val gradeColor = GradeColorsByIndex.getOrElse(tile.grade) { Color.Gray }
                    val gradeName = GradeNamesByIndex.getOrElse(tile.grade) { "?" }
                    val count = tiles.size

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(gradeColor.copy(alpha = 0.1f))
                            .border(1.dp, gradeColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable { selectedBlueprintId = bpId }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Grade dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(gradeColor),
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Unit info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = bp?.name ?: bpId,
                                    color = gradeColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (count > 1) {
                                    Text(
                                        text = " x$count",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                            // Role + Grade
                            Text(
                                text = "$gradeName | ${tile.role.label}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                            )
                            // Stats from blueprint
                            if (bp != null) {
                                val families = bp.families.joinToString("/") { it.label }
                                Text(
                                    text = "$families | 공격력 ${bp.stats.baseATK.toInt()} | 공속 ${String.format(java.util.Locale.US, "%.1f", bp.stats.baseSpeed)} | 사거리 ${bp.stats.range.toInt()}",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 9.sp,
                                )
                                if (bp.stats.hp > 0f) {
                                    Text(
                                        text = "체력 ${bp.stats.hp.toInt()} | 방어력 ${bp.stats.defense.toInt()} | 마법저항 ${bp.stats.magicResist.toInt()}",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 9.sp,
                                    )
                                }
                            }
                        }

                        // Arrow hint
                        Text(
                            text = "\u276F",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 14.sp,
                        )
                    }
                }

                if (unitGroups.isEmpty()) {
                    // Legacy units without blueprintId
                    Text(
                        text = "\uBE14\uB8E8\uD504\uB9B0\uD2B8 \uC815\uBCF4 \uC5C6\uC74C",  // 블루프린트 정보 없음
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Close button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF3E2510))
                    .border(1.dp, Color(0xFF8B6040), RoundedCornerShape(10.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\uB2EB\uAE30",  // 닫기
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    // Blueprint detail dialog
    selectedBlueprintId?.let { bpId ->
        BlueprintDetailDialog(
            blueprintId = bpId,
            onDismiss = { selectedBlueprintId = null },
        )
    }
}

// ── Blueprint Detail Dialog — full spec view from unit list ──

@Composable
private fun BlueprintDetailDialog(
    blueprintId: String,
    onDismiss: () -> Unit,
) {
    val bp = remember(blueprintId) {
        BlueprintRegistry.instance.findById(blueprintId)
    } ?: return

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1008))
                .border(2.dp, bp.grade.color.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header: icon + name + grade + families
            if (bp.iconRes != 0) {
                Image(
                    painter = painterResource(id = bp.iconRes),
                    contentDescription = bp.name,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = bp.name,
                color = bp.grade.color,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            // Grade + families
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = bp.grade.label,
                    color = bp.grade.color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (bp.families.isNotEmpty()) {
                    Text(
                        text = bp.families.joinToString("/") { it.label },
                        color = bp.families.first().color,
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Role / AttackRange / DamageType badges
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BadgePill(bp.role.label, roleColor(bp.role))
                BadgePill(
                    bp.attackRange.label,
                    if (bp.attackRange == AttackRange.MELEE) Color(0xFF64B5F6) else Color(0xFF81C784),
                )
                BadgePill(
                    if (bp.damageType == DamageType.PHYSICAL) "\uBB3C\uB9AC" else "\uB9C8\uBC95",
                    if (bp.damageType == DamageType.PHYSICAL) Color(0xFFEF5350) else Color(0xFF7E57C2),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats grid — 2 rows of 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                DetailStatItem("\uACF5\uACA9\uB825", "${bp.stats.baseATK.toInt()}", Color(0xFFEF5350))
                DetailStatItem("\uACF5\uC18D", String.format(java.util.Locale.US, "%.1f", bp.stats.baseSpeed), Color(0xFF26C6DA))
                DetailStatItem("\uC0AC\uAC70\uB9AC", "${bp.stats.range.toInt()}", GoldBright)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                DetailStatItem("\uCCB4\uB825", "${bp.stats.hp.toInt()}", Color(0xFF4CAF50))
                DetailStatItem("\uBC29\uC5B4\uB825", "${bp.stats.defense.toInt()}", Color(0xFF90A4AE))
                DetailStatItem("\uB9C8\uBC95\uC800\uD56D", "${bp.stats.magicResist.toInt()}", Color(0xFF7E57C2))
            }

            // Extra stats
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                DetailStatItem("\uC774\uB3D9\uC18D\uB3C4", String.format(java.util.Locale.US, "%.0f", bp.stats.moveSpeed), Color(0xFF80CBC4))
                DetailStatItem("\uBE14\uB85D", "${bp.stats.blockCount}", Color(0xFFFFAB91))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            if (bp.description.isNotEmpty()) {
                Text(
                    text = bp.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Ability
            if (bp.ability != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A1A0C))
                        .border(1.dp, Color(0xFF8B6040).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                ) {
                    Text(
                        text = "\u2694 ${bp.ability.name}",
                        color = GoldBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    // Ability details row
                    val triggerLabel = when (bp.ability.type) {
                        com.example.jaygame.engine.AbilityTrigger.PASSIVE -> "\uD328\uC2DC\uBE0C"
                        com.example.jaygame.engine.AbilityTrigger.ACTIVE -> "\uC561\uD2F0\uBE0C"
                        com.example.jaygame.engine.AbilityTrigger.AURA -> "\uC624\uB77C"
                    }
                    val dmgLabel = if (bp.ability.damageType == DamageType.PHYSICAL) "\uBB3C\uB9AC" else "\uB9C8\uBC95"
                    val detailParts = mutableListOf(triggerLabel, dmgLabel)
                    if (bp.ability.cooldown > 0f) detailParts.add("\uCFE8\uD0C0\uC784 ${String.format(java.util.Locale.US, "%.1f", bp.ability.cooldown)}\uCD08")
                    if (bp.ability.value > 0f) detailParts.add("\uC704\uB825 ${String.format(java.util.Locale.US, "%.0f%%", bp.ability.value * 100f)}")
                    if (bp.ability.range > 0f) detailParts.add("\uBC94\uC704 ${bp.ability.range.toInt()}")
                    Text(
                        text = detailParts.joinToString(" | "),
                        color = Color(0xFF26C6DA),
                        fontSize = 9.sp,
                    )
                    Text(
                        text = bp.ability.description,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Unique ability
            if (bp.uniqueAbility != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(bp.grade.color.copy(alpha = 0.1f))
                        .border(1.dp, bp.grade.color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                ) {
                    Text(
                        text = "\u2726 ${bp.uniqueAbility.name}",
                        color = bp.grade.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${bp.uniqueAbility.requiredGrade.label} \uB4F1\uAE09 \uC774\uC0C1 \uD574\uAE08",
                        color = bp.uniqueAbility.requiredGrade.color.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                    )
                    bp.uniqueAbility.passive?.let { passive ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "[\uD328\uC2DC\uBE0C] ${passive.name}",
                            color = Color(0xFF81C784),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        val passiveDetails = mutableListOf<String>()
                        if (passive.value > 0f) passiveDetails.add("\uC704\uB825 ${String.format(java.util.Locale.US, "%.0f%%", passive.value * 100f)}")
                        if (passive.range > 0f) passiveDetails.add("\uBC94\uC704 ${passive.range.toInt()}")
                        if (passiveDetails.isNotEmpty()) {
                            Text(
                                text = passiveDetails.joinToString(" | "),
                                color = Color(0xFF81C784).copy(alpha = 0.7f),
                                fontSize = 9.sp,
                            )
                        }
                        Text(
                            text = passive.description,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    bp.uniqueAbility.active?.let { active ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "[\uC561\uD2F0\uBE0C] ${active.name}",
                            color = Color(0xFF26C6DA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        val activeDetails = mutableListOf<String>()
                        if (active.cooldown > 0f) activeDetails.add("\uCFE8\uD0C0\uC784 ${String.format(java.util.Locale.US, "%.1f", active.cooldown)}\uCD08")
                        if (active.value > 0f) activeDetails.add("\uC704\uB825 ${String.format(java.util.Locale.US, "%.0f%%", active.value * 100f)}")
                        if (active.range > 0f) activeDetails.add("\uBC94\uC704 ${active.range.toInt()}")
                        val dmgLabel = if (active.damageType == DamageType.PHYSICAL) "\uBB3C\uB9AC" else "\uB9C8\uBC95"
                        activeDetails.add(0, dmgLabel)
                        Text(
                            text = activeDetails.joinToString(" | "),
                            color = Color(0xFF26C6DA).copy(alpha = 0.7f),
                            fontSize = 9.sp,
                        )
                        Text(
                            text = active.description,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF3E2510))
                    .border(1.dp, Color(0xFF8B6040), RoundedCornerShape(10.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\uB3CC\uC544\uAC00\uAE30",  // 돌아가기
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun DetailStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Bottom HUD — resource bar → [일괄판매?] [조합?] → [구매|소환|도박] → [강화] ──

@Composable
fun BattleBottomHud(
    onBuyClick: () -> Unit = {},
    onBulkSellClick: () -> Unit = {},
    onGambleClick: () -> Unit = {},
) {
    val battle by BattleBridge.state.collectAsState()
    val gridState by BattleBridge.gridState.collectAsState()
    val unitPullPity by BattleBridge.unitPullPity.collectAsState()
    // Single pass over gridState for unit count, merge, and has-units flags
    var unitCount = 0
    var canMerge = false
    for (tile in gridState) {
        val occupied = tile.unitDefId >= 0 || tile.blueprintId.isNotEmpty()
        if (occupied) unitCount++
        if (tile.canMerge) canMerge = true
    }
    val canSummon = battle.sp >= battle.summonCost && unitCount < battle.maxUnitSlots
    val canGamble = battle.sp > 0f
    val hasUnits = unitCount > 0
    val context = LocalContext.current
    val view = LocalView.current

    val goldIcon = remember { loadAssetBitmap(context, "raw/ui/icon_gold.png") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Floating merge/sell row — fixed height so it doesn't push layout ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp),
        ) {
            // Left: 일괄판매 button (symmetric to merge)
            if (hasUnits) {
                val sellShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .shadow(6.dp, sellShape, ambientColor = Color(0xFFEF4444).copy(alpha = 0.3f), spotColor = Color(0xFFEF4444).copy(alpha = 0.4f))
                        .clip(sellShape)
                        .background(Brush.verticalGradient(listOf(Color(0xFFEF5350), Color(0xFFC62828))))
                        .border(2.dp, Color(0xFFFF8A80).copy(alpha = 0.8f), sellShape)
                        .clickable(onClick = onBulkSellClick)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "\uD83D\uDCB0 일괄판매",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            // Right: 조합 button
            if (canMerge) {
                val inf = rememberInfiniteTransition(label = "mg")
                val glow by inf.animateFloat(0.7f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "mg")
                val mergeShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .graphicsLayer { alpha = glow }
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

        // ── Resource row: SP bar ──
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

        }

        // ── Action buttons ──
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
                    gridFull = unitCount >= battle.maxUnitSlots,
                    pity = unitPullPity,
                    onClick = {
                        HapticManager.medium(view)
                        SfxManager.play(SoundEvent.Summon)
                        BattleBridge.requestSummon()
                    },
                    modifier = Modifier.weight(1.6f),
                    goldIcon = goldIcon,
                )

                // 도박 (Gamble) — opens GambleDialog
                GambleButton(
                    enabled = canGamble,
                    onClick = { onGambleClick() },
                    modifier = Modifier.weight(1f),
                )
            }

            // 강화 버튼 제거됨 — 개별 유닛 탭 → UnitDetailPopup에서 강화
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
    pity: Int = 0,
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
            // 천장 카운터
            Text(
                text = "천장: $pity/100",
                color = when {
                    pity >= 80 -> NeonRed
                    pity >= 30 -> Color(0xFFFFAA44)
                    else -> WoodBrownDark.copy(alpha = 0.6f)
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ── Boss Modifier Alert Banner ─────────────────────────────────

/**
 * Shows an alert banner for 3 seconds when a boss with a modifier spawns.
 * Display: "⚠️ 보스: {label} — {description}"
 * Red/orange text on semi-transparent dark background.
 */
@Composable
fun BossModifierAlert() {
    val bossModifierState by BattleBridge.bossModifier.collectAsState()
    var visibleModifier by remember { mutableStateOf<BossModifier?>(null) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(bossModifierState) {
        if (bossModifierState != null) {
            visibleModifier = bossModifierState
            alpha.snapTo(1f)
            delay(2500)
            alpha.animateTo(0f, tween(500))
            visibleModifier = null
        }
    }

    val mod = visibleModifier ?: return

    Box(
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha.value }
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xCC1A0A00))
            .border(2.dp, Color(0xFFFF4400).copy(alpha = 0.8f), RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "\u26A0\uFE0F 보스: ${mod.label}",
                color = Color(0xFFFF6633),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = mod.description,
                color = Color(0xFFFFBB88),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Gamble Button (coin icon + 10 cost, like summon style) ──────

@Composable
private fun GambleButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "gambleScale",
    )

    val bgTop = if (enabled) GreenTeal else Color(0xFF3A3A3A)
    val bgBot = if (enabled) GreenTealDark else Color(0xFF2A2A2A)
    val border = if (enabled) Color(0xFF66CC88).copy(alpha = 0.7f) else Color(0xFF4A4A4A)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(58.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                if (enabled) 10.dp else 2.dp,
                RoundedCornerShape(12.dp),
                ambientColor = if (enabled) GreenTealDark.copy(alpha = 0.4f) else Color.Black,
                spotColor = if (enabled) GreenTealDark.copy(alpha = 0.5f) else Color.Black,
            )
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(bgTop, bgBot)),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.4f),
                )
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
        Text(
            text = "\uD83C\uDFB2 도박",
            color = if (enabled) Color.White else Color(0xFF888888),
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}
