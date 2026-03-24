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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.jaygame.engine.HiddenRecipe
import com.example.jaygame.engine.RecipeSlot
import com.example.jaygame.engine.RecipeSystem
import com.example.jaygame.engine.UnitBlueprint
import com.example.jaygame.engine.UnitGrade
import com.example.jaygame.ui.components.roleColor
import com.example.jaygame.ui.theme.*
import kotlinx.coroutines.delay
import com.example.jaygame.util.HapticManager
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog

// ── Warm medieval theme colors ──
private val DungeonPurple = Color(0xFFAB47BC)
private val WoodBrown = Color(0xFF5C3A1E)
private val WoodBrownDark = Color(0xFF3E2510)
private val PanelBg = Color(0xFF2A1A0C).copy(alpha = 0.88f)
private val BadgeBg = Color(0xFF1E1208).copy(alpha = 0.9f)
private val GoldBright = Color(0xFFFFD700)
private val GoldDark = Color(0xFFB8860B)
private val GreenTeal = Color(0xFF2E8B57)
private val GreenTealDark = Color(0xFF1B5E3A)
private val OrangeBright = Color(0xFFFF8C00)
private val OrangeDark = Color(0xFFCC6600)
private val BossRed = Color(0xFFFF4444)
private val BossOrange = Color(0xFFFF6644)
private val BossWaveBg = Brush.verticalGradient(listOf(Color(0xFF5C1818), Color(0xFF3D0C0C)))
private val BossMainBg = Brush.verticalGradient(listOf(Color(0xFF3D1515), Color(0xFF2E0C0C)))
private val NormalWaveBg = Brush.verticalGradient(listOf(Color(0xFF5C3A1E), Color(0xFF3D2510)))
private val NormalMainBg = Brush.verticalGradient(listOf(Color(0xFF4A3018), Color(0xFF2E1C0C)))

// ── Disabled state colors ──
private val DisabledTop = Color(0xFF3A3A3A)
private val DisabledBot = Color(0xFF2A2A2A)
private val DisabledText = Color(0xFF888888)

// ── Pre-allocated drawBehind resources (GC-free rendering) ──
private val HighlightBrush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
)
private val HighlightBrushBright = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
)
private val BtnOuterRadius = 14f   // dp → converted at use site
private val BtnInnerRadius = 11f
private val BtnInset = 3f          // dp
private val BtnShape = RoundedCornerShape(14.dp)

// Pre-allocated highlight colors for polygon buttons
private val PolyHighlightAlpha = Color.White.copy(alpha = 0.18f)
private val PolyHighlightTransparent = Color.Transparent

// Wide hexagon button shape for 구매/도박
private val WideHexShape = WideHexagonShape(cornerRadiusDp = 5f)

// Button border colors
private val BuyBorderColor = Color(0xFF6B3A10)
private val GambleBorderColor = Color(0xFF1A4A2A)

// Sell/merge row pre-allocated brushes
private val SellBtnBrush = Brush.verticalGradient(listOf(Color(0xFFEF5350), Color(0xFFC62828)))
private val MergeBtnBrush = Brush.verticalGradient(listOf(GoldBright, GoldDark))

// Recipe book colors
private val RecipeBookBorder = Color(0xFF8B6A3A)
private val RecipeBookBg = Color(0xFF1A1208)
private val RecipeCardBg = Color(0xFF2A1E10)
private val RecipeCardBorder = Color(0xFF5C3A1E)
private val RecipeArrowColor = Color(0xFFFFD700)
private val RecipeAvailableGlow = Color(0xFF4CAF50)
private val RecipeUnavailableText = Color(0xFF666666)
private val RecipeIngredientBg = Color(0xFF1E1610)
private val RecipeResultBg = Brush.verticalGradient(listOf(Color(0xFF3A2A10), Color(0xFF2A1E0C)))
private val RecipeBtnTop = Color(0xFF6A4FA0)
private val RecipeBtnBot = Color(0xFF4A3570)
private val RecipeBtnBorder = Color(0xFF3A2860)
private val RecipeBtnBrush = Brush.verticalGradient(listOf(RecipeBtnTop, RecipeBtnBot))
private val RecipeCraftBtnTop = Color(0xFF4CAF50)
private val RecipeCraftBtnBot = Color(0xFF2E7D32)
private val RecipeCraftEnabledBrush = Brush.verticalGradient(listOf(RecipeCraftBtnTop, RecipeCraftBtnBot))
private val RecipeCraftDisabledBrush = Brush.verticalGradient(listOf(DisabledTop, DisabledBot))
private val RecipeCraftEnabledBorder = Color(0xFF1B5E20)
private val RecipeGradeWarningColor = Color(0xFFFFAA33)

// Recipe dialog data models (file-level to avoid recomposition overhead)
private data class RecipeDisplayInfo(
    val recipe: HiddenRecipe,
    val resultBlueprint: UnitBlueprint?,
    val fieldMatchCount: Int,     // 필드에서 충족된 재료 수
    val totalIngredients: Int,    // 총 재료 수
) {
    val readyOnField get() = fieldMatchCount >= totalIngredients
}

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
                    if (isBoss) {
                        // 보스 웨이브: 60초 카운트다운
                        val waveSec = battle.waveTimeRemaining.toInt().coerceAtLeast(0)
                        val waveTimeColor = when {
                            waveSec < 15 -> Color(0xFFFF0000)
                            waveSec < 30 -> NeonRed
                            else -> Color(0xFFFF8844)
                        }
                        val timerSize = if (waveSec < 30) 26.sp else 22.sp
                        Text(
                            text = "%d:%02d".format(waveSec / 60, waveSec % 60),
                            color = waveTimeColor,
                            fontSize = timerSize,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    } else {
                        // 일반 웨이브: 경과 시간 카운트업
                        val elapsed = battle.waveElapsed.toInt().coerceAtLeast(0)
                        val timerColor = if (elapsed < 30) NeonGreen else Color.White
                        Text(
                            text = "%02d:%02d".format(elapsed / 60, elapsed % 60),
                            color = timerColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }

                    // Difficulty badge (settings-style)
                    val difficulty by BattleBridge.difficulty.collectAsState()
                    val diffInfo = when (difficulty) {
                        0 -> "일반" to NeonGreen
                        1 -> "하드" to Color(0xFFFF8800)
                        2 -> "헬" to NeonRed
                        else -> "일반" to NeonGreen
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
                4f -> "x2"
                8f -> "x4"
                else -> ""
            }
            if (speedLabel.isNotEmpty()) {
                Text(
                    speedLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when (battleSpeed) {
                        4f -> GoldBright
                        8f -> Color(0xFFFF6B6B)
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

// ── Recipe Book Dialog — shows all craftable recipes for current deck ──

@Composable
private fun RecipeBookDialog(onDismiss: () -> Unit) {
    val allRecipes = remember {
        if (RecipeSystem.isReady) RecipeSystem.instance.allRecipes() else emptyList()
    }

    // 필드 상태는 다이얼로그가 열릴 때만 스냅샷으로 캡처
    val gridState by BattleBridge.gridState.collectAsState()
    val occupiedTiles = remember(gridState) {
        gridState.filter { it.blueprintId.isNotEmpty() }
    }

    val recipeInfos = remember(allRecipes, occupiedTiles) {
        allRecipes.map { recipe ->
            // 필드 유닛 기반 재료 충족 수 카운트
            val used = mutableSetOf<Int>()
            var matchCount = 0
            for (slot in recipe.ingredients) {
                val found = occupiedTiles.indices.any { idx ->
                    if (idx in used) return@any false
                    val tile = occupiedTiles[idx]
                    val match = if (slot.specificUnitId != null) {
                        tile.blueprintId == slot.specificUnitId
                    } else {
                        (slot.family == null || slot.family in tile.families) &&
                            (slot.role == null || slot.role == tile.role) &&
                            tile.grade >= slot.minGrade.ordinal
                    }
                    if (match) { used.add(idx); true } else false
                }
                if (found) matchCount++
            }

            val resultBp = if (BlueprintRegistry.isReady) {
                BlueprintRegistry.instance.findById(recipe.resultId)
            } else null
            RecipeDisplayInfo(recipe, resultBp, matchCount, recipe.ingredients.size)
        }.sortedByDescending { it.fieldMatchCount }
    }

    val hasReadyRecipe = recipeInfos.any { it.readyOnField }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RecipeBookBg)
                .border(2.dp, RecipeBookBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "\uD83D\uDCD6 조합법",
                        color = GoldBright,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("\u2715", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "현재 덱으로 조합 가능한 신화 레시피",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (recipeInfos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "레시피 없음",
                            color = RecipeUnavailableText,
                            fontSize = 14.sp,
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(320.dp),
                    ) {
                        items(recipeInfos) { info ->
                            RecipeCard(info.recipe, info.resultBlueprint, info.fieldMatchCount, info.totalIngredients)
                        }
                    }
                }

                // 조합하기 버튼
                if (recipeInfos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (hasReadyRecipe) RecipeCraftEnabledBrush else RecipeCraftDisabledBrush)
                            .border(
                                2.dp,
                                if (hasReadyRecipe) RecipeCraftEnabledBorder else DisabledBot,
                                RoundedCornerShape(12.dp),
                            )
                            .then(
                                if (hasReadyRecipe) Modifier.clickable {
                                    BattleBridge.requestRecipeCraft()
                                    onDismiss()
                                } else Modifier
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (hasReadyRecipe) "\u2728 조합하기" else "필드에 재료를 배치하세요",
                            color = if (hasReadyRecipe) Color.White else DisabledText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeCard(
    recipe: HiddenRecipe,
    resultBlueprint: UnitBlueprint?,
    fieldMatchCount: Int,
    totalIngredients: Int,
) {
    val readyOnField = fieldMatchCount >= totalIngredients
    val hasPartial = fieldMatchCount > 0
    val borderColor = when {
        readyOnField -> GoldBright.copy(alpha = 0.8f)
        hasPartial -> RecipeAvailableGlow.copy(alpha = 0.4f)
        else -> RecipeCardBorder
    }
    val cardAlpha = if (hasPartial) 1f else 0.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = cardAlpha }
            .clip(RoundedCornerShape(12.dp))
            .background(RecipeCardBg)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Result unit name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val resultName = resultBlueprint?.name ?: recipe.resultId
                val resultGrade = resultBlueprint?.grade
                val gradeColor = resultGrade?.color ?: GoldBright
                Text("\u2728", fontSize = 14.sp)
                Text(
                    text = resultName,
                    color = gradeColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                if (resultGrade != null) {
                    Text(
                        text = resultGrade.label,
                        color = gradeColor.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Ingredients row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                recipe.ingredients.forEachIndexed { idx, slot ->
                    if (idx > 0) {
                        Text(
                            "+",
                            color = GoldBright.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    RecipeSlotChip(slot)
                }

                Text(
                    " \u279C ",
                    color = RecipeArrowColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(RecipeResultBg)
                        .border(1.dp, GoldBright.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("\u2728", fontSize = 14.sp)
                }
            }

            when {
                readyOnField -> Text(
                    text = "\u2728 조합 준비 완료!",
                    color = GoldBright,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                hasPartial -> {
                    val maxGrade = recipe.ingredients.maxOf { it.minGrade.ordinal }
                    val gradeLabel = UnitGrade.entries.getOrNull(maxGrade)?.label ?: ""
                    val gradeHint = if (maxGrade > 0) " ($gradeLabel↑)" else ""
                    Text(
                        text = "\uD83D\uDD27 재료 $fieldMatchCount/$totalIngredients 준비됨$gradeHint",
                        color = RecipeAvailableGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                else -> Text(
                    text = "재료 미배치",
                    color = RecipeUnavailableText,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun RecipeSlotChip(slot: RecipeSlot) {
    val familyColor = slot.family?.color ?: Color.White.copy(alpha = 0.5f)
    val familyLabel = slot.family?.label ?: "아무"
    val roleLabel = slot.role?.label ?: "아무"
    val gradeLabel = if (slot.minGrade.ordinal <= com.example.jaygame.engine.UnitGrade.COMMON.ordinal) {
        ""
    } else {
        "${slot.minGrade.label}+"
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RecipeIngredientBg)
            .border(1.dp, familyColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = familyLabel,
            color = familyColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = roleLabel,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 9.sp,
        )
        if (gradeLabel.isNotEmpty()) {
            Text(
                text = gradeLabel,
                color = GoldBright.copy(alpha = 0.6f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
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
    onBulkSellClick: () -> Unit = {},
    onGambleClick: () -> Unit = {},
    onUpgradeClick: () -> Unit = {},
) {
    val battle by BattleBridge.state.collectAsState()
    val gridState by BattleBridge.gridState.collectAsState()
    val deckBlueprints by BattleBridge.deckBlueprints.collectAsState()
    val isDeckMode = deckBlueprints.isNotEmpty()
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
    var showRecipeBook by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current

    val goldIcon = remember { loadAssetBitmap(context, "raw/ui/icon_gold.png") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-35).dp)
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Row 1: [일괄판매(좌)] [금화(정중앙)] [조합법(우)] ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        ) {
            // 일괄판매 (좌)
            val sellShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(sellShape)
                    .background(if (hasUnits) SellBtnBrush else Brush.verticalGradient(listOf(DisabledTop, DisabledBot)))
                    .border(2.dp, if (hasUnits) Color(0xFF8B1A1A) else DisabledBot, sellShape)
                    .then(if (hasUnits) Modifier.clickable(onClick = onBulkSellClick) else Modifier)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("일괄판매", color = if (hasUnits) Color.White else DisabledText, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }

            // 금화 (정중앙 고정)
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (goldIcon != null) {
                    Image(bitmap = goldIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${battle.sp.toInt()}",
                    color = GoldBright,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            // 조합법 (우)
            val recipeShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(recipeShape)
                    .background(RecipeBtnBrush)
                    .border(2.dp, RecipeBtnBorder, recipeShape)
                    .clickable { showRecipeBook = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("\uD83D\uDCD6 조합법", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        // Recipe book dialog
        if (showRecipeBook) {
            RecipeBookDialog(onDismiss = { showRecipeBook = false })
        }

        // ── Row 2: 조합 가능 알림 ──
        if (canMerge) {
            val inf = rememberInfiniteTransition(label = "mg")
            val glow by inf.animateFloat(0.7f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "mg")
            val mergeShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .graphicsLayer { alpha = glow }
                    .clip(mergeShape)
                    .background(MergeBtnBrush)
                    .border(2.dp, Color(0xFF8B6914), mergeShape)
                    .clickable {
                        val tiles = BattleBridge.gridState.value
                        for (i in tiles.indices) { if (tiles[i].canMerge) BattleBridge.requestMerge(i) }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("조합", color = WoodBrownDark, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Row 3+4: 소환(큰) 위, 구매/도박(중간) 겹침, 강화(아래) ──
        // Box로 겹쳐서 구매/도박이 소환과 강화 사이에 위치
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            // 소환 — 상단에 배치
            SummonButton(
                cost = battle.summonCost,
                enabled = canSummon,
                gridFull = unitCount >= battle.maxUnitSlots,
                onClick = {
                    HapticManager.medium(view)
                    SfxManager.play(SoundEvent.Summon)
                    BattleBridge.requestSummon()
                },
                modifier = Modifier
                    .fillMaxWidth(0.52f)
                    .height(70.dp)
                    .align(Alignment.TopCenter),
                goldIcon = goldIcon,
            )

            // 구매/도박 — 중간 높이에 양쪽 배치
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                WideHexButton(
                    icon = "\uD83D\uDCD6",
                    label = "조합법",
                    enabled = true,
                    gradientTop = RecipeBtnTop, gradientBot = RecipeBtnBot,
                    borderColor = RecipeBtnBorder,
                    onClick = { showRecipeBook = true },
                    modifier = Modifier.size(width = 72.dp, height = 66.dp),
                )
                WideHexButton(
                    icon = "\uD83C\uDFB2",
                    label = "도박",
                    enabled = canGamble,
                    gradientTop = GreenTeal, gradientBot = GreenTealDark,
                    borderColor = GambleBorderColor,
                    onClick = onGambleClick,
                    modifier = Modifier.size(width = 72.dp, height = 66.dp),
                )
            }

            // 강화 — 하단에 배치
            WarmButton(
                topText = "\u25C6 강화",
                bottomText = "",
                enabled = true,
                gradientTop = Color(0xFF4A3E32), gradientBot = Color(0xFF32281C),
                borderColor = Color(0xFF1A1510),
                onClick = onUpgradeClick,
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .align(Alignment.BottomCenter),
                buttonHeight = 50.dp,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 네비게이션 바 안전 영역 — 고정 높이로 레이아웃 점프 방지
        Spacer(modifier = Modifier.height(48.dp))
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

    val bgTop = if (enabled) gradientTop else DisabledTop
    val bgBot = if (enabled) gradientBot else DisabledBot
    val outlineCol = if (enabled) borderColor else DisabledBot
    val fillBrush = remember(bgTop, bgBot) { Brush.verticalGradient(listOf(bgTop, bgBot)) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(buttonHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(BtnShape)
            .drawBehind {
                drawThickOutlineButton(outlineCol, fillBrush, HighlightBrush)
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
                color = if (enabled) Color.White else DisabledText,
                fontSize = if (bottomText.isEmpty()) 13.sp else 18.sp,
                fontWeight = if (bottomText.isEmpty()) FontWeight.ExtraBold else FontWeight.Normal,
            )
            if (bottomText.isNotEmpty()) {
                Text(
                    text = bottomText,
                    color = if (enabled) Color.White else DisabledText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

// ── Summon Button (center, large, gold with coin icon) ──────

@Composable
private fun SummonButton(
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

    val bgTop = if (enabled) GoldBright else DisabledTop
    val bgBot = if (enabled) GoldDark else DisabledBot
    val fillBrush = remember(bgTop, bgBot) { Brush.verticalGradient(listOf(bgTop, bgBot)) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(BtnShape)
            .drawBehind {
                drawThickOutlineButton(Color(0xFF1A1510), fillBrush, HighlightBrushBright)
            }
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "소환",
                color = if (enabled) WoodBrownDark else DisabledText,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (goldIcon != null) {
                    Image(
                        bitmap = goldIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = if (gridFull) "FULL" else "$cost",
                    color = if (gridFull) NeonRed
                    else if (enabled) WoodBrownDark.copy(alpha = 0.85f)
                    else Color(0xFF666666),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
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

// ── Shared drawBehind helper: thick dark outline + inset gradient fill + highlight ──

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawThickOutlineButton(
    outlineColor: Color,
    fillBrush: Brush,
    highlightBrush: Brush,
) {
    val outerR = CornerRadius(BtnOuterRadius.dp.toPx())
    val innerR = CornerRadius(BtnInnerRadius.dp.toPx())
    val inset = BtnInset.dp.toPx()
    val innerW = size.width - inset * 2
    val innerH = size.height - inset * 2
    val innerOffset = androidx.compose.ui.geometry.Offset(inset, inset)

    drawRoundRect(color = outlineColor, cornerRadius = outerR)
    drawRoundRect(
        brush = fillBrush,
        cornerRadius = innerR,
        topLeft = innerOffset,
        size = androidx.compose.ui.geometry.Size(innerW, innerH),
    )
    drawRoundRect(
        brush = highlightBrush,
        cornerRadius = innerR,
        topLeft = innerOffset,
        size = androidx.compose.ui.geometry.Size(innerW, innerH * 0.45f),
    )
}

// ── Wide Hexagon shape for side buttons (구매/도박) ──

private class WideHexagonShape(private val cornerRadiusDp: Float = 5f) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(
        buildWideHexPath(size.width, size.height, cornerRadiusDp * density.density)
    )
}

private fun buildWideHexPath(width: Float, height: Float, cornerPx: Float): Path {
    val r = cornerPx.coerceAtMost(kotlin.math.min(width, height) * 0.2f)
    val cy = height / 2f
    val pointInset = width * 0.12f
    val vx = floatArrayOf(0f, pointInset, width - pointInset, width, width - pointInset, pointInset)
    val vy = floatArrayOf(cy, 0f, 0f, cy, height, height)
    val path = Path()
    val n = vx.size
    for (i in 0 until n) {
        val prev = (i - 1 + n) % n
        val next = (i + 1) % n
        val tpx = vx[prev] - vx[i]; val tpy = vy[prev] - vy[i]
        val tnx = vx[next] - vx[i]; val tny = vy[next] - vy[i]
        val lp = kotlin.math.sqrt(tpx * tpx + tpy * tpy)
        val ln = kotlin.math.sqrt(tnx * tnx + tny * tny)
        val pull = r.coerceAtMost(kotlin.math.min(lp, ln) / 2f)
        val sx = vx[i] + tpx / lp * pull; val sy = vy[i] + tpy / lp * pull
        val ex = vx[i] + tnx / ln * pull; val ey = vy[i] + tny / ln * pull
        if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
        @Suppress("DEPRECATION")
        path.quadraticBezierTo(vx[i], vy[i], ex, ey)
    }
    path.close()
    return path
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWideHexBackground(
    outlineColor: Color,
    fillBrush: Brush,
    outerPath: Path,
    innerPath: Path,
    highlightBrush: Brush,
) {
    val inset = BtnInset.dp.toPx()
    drawPath(path = outerPath, color = outlineColor)
    val innerH = size.height - inset * 2f
    if (innerH <= 0f) return
    drawContext.transform.translate(inset, inset)
    drawPath(path = innerPath, brush = fillBrush)
    clipPath(innerPath) {
        drawRect(
            brush = highlightBrush,
            size = androidx.compose.ui.geometry.Size(size.width - inset * 2f, innerH * 0.45f),
        )
    }
    drawContext.transform.translate(-inset, -inset)
}

private class PolyDrawCache {
    var outer: Path = Path()
    var inner: Path = Path()
    var highlight: Brush = HighlightBrush
    var w: Float = 0f
    var h: Float = 0f
}

@Composable
private fun WideHexButton(
    icon: String,
    label: String,
    enabled: Boolean,
    gradientTop: Color,
    gradientBot: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "hexScale",
    )
    val bgTop = if (enabled) gradientTop else DisabledTop
    val bgBot = if (enabled) gradientBot else DisabledBot
    val outlineCol = if (enabled) borderColor else DisabledBot
    val fillBrush = remember(bgTop, bgBot) { Brush.verticalGradient(listOf(bgTop, bgBot)) }
    val cache = remember { PolyDrawCache() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(WideHexShape)
            .drawBehind {
                if (size.width != cache.w || size.height != cache.h) {
                    cache.w = size.width; cache.h = size.height
                    val insetPx = BtnInset.dp.toPx()
                    val cornerPx = 5.dp.toPx()
                    cache.outer = buildWideHexPath(size.width, size.height, cornerPx)
                    val iw = size.width - insetPx * 2f
                    val ih = size.height - insetPx * 2f
                    cache.inner = buildWideHexPath(iw, ih, (cornerPx - insetPx * 0.5f).coerceAtLeast(0f))
                    cache.highlight = Brush.verticalGradient(
                        listOf(PolyHighlightAlpha, PolyHighlightTransparent),
                        startY = 0f, endY = ih * 0.45f,
                    )
                }
                drawWideHexBackground(outlineCol, fillBrush, cache.outer, cache.inner, cache.highlight)
            }
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = icon, fontSize = 18.sp)
            Text(
                text = label,
                color = if (enabled) Color.White else DisabledText,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

