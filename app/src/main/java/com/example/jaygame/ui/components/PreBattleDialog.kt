package com.example.jaygame.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.data.DeckManager
import com.example.jaygame.data.StageDef
import com.example.jaygame.data.UnitRace
import com.example.jaygame.engine.AttackRange
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.UnitBlueprint
import com.example.jaygame.engine.UnitGrade
import com.example.jaygame.ui.screens.blueprintIconRes
import com.example.jaygame.ui.theme.*

private val DeckSlotFilledBg = Color(0xFF1A1A30)
private val DeckSlotEmptyBg = Color(0xFF151520)
private val DeckSlotEmptyBorder = Color(0x14FFFFFF)
private val DeckSlotEmptyPlus = Color(0x26FFFFFF)
private val SelectedSlotBorder = Color(0xFF00D4FF)

private data class DifficultyInfo(
    val id: Int, val name: String, val desc: String,
    val enemyMult: String, val rewardMult: String, val color: Color,
)

private val DialogBgBrush = Brush.verticalGradient(listOf(Color(0xFF2A1F15), Color(0xFF1A0F0A)))

private val DIFFICULTIES = listOf(
    DifficultyInfo(0, "일반", "기본 난이도", "×1.0", "×1.0", NeonGreen),
    DifficultyInfo(1, "하드", "강화된 적과 보상", "×1.5", "×1.5", Color(0xFFFF8800)),
    DifficultyInfo(2, "헬", "극한의 도전", "×2.2", "×2.5", NeonRed),
)

@Composable
fun PreBattleDialog(
    stage: StageDef,
    bestWave: Int,
    selectedDifficulty: Int,
    staminaCost: Int,
    hasStamina: Boolean,
    activeDeck: List<String> = emptyList(),
    onDifficultySelected: (Int) -> Unit,
    onDeckChanged: (List<String>) -> Unit = {},
    onStartBattle: () -> Unit,
    onDismiss: () -> Unit,
) {
    var editingDeck by remember { mutableStateOf(false) }
    var currentDeck by remember(activeDeck) { mutableStateOf(activeDeck.toMutableList()) }
    var selectedSlot by remember { mutableStateOf(-1) }
    var raceFilter by remember { mutableStateOf<UnitRace?>(null) }
    var rangeFilter by remember { mutableStateOf<AttackRange?>(null) }

    val reg = remember { if (BlueprintRegistry.isReady) BlueprintRegistry.instance else null }
    val deckBlueprints = remember(currentDeck.toList()) {
        List(DeckManager.DECK_SIZE) { i ->
            currentDeck.getOrNull(i)?.let { id -> reg?.findById(id) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DialogBgBrush)
                .border(1.5.dp, BorderGlow, RoundedCornerShape(20.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Stage Info ──
            Text(stage.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
            Text(stage.description, fontSize = 12.sp, color = SubText)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                StatChip("\uD83C\uDF0A ${stage.maxWaves} 웨이브")
                Spacer(modifier = Modifier.width(12.dp))
                StatChip(if (bestWave > 0) "\uD83C\uDFC6 BEST $bestWave" else "미도전")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Deck Slots ──
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("출전 덱", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LightText)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (editingDeck) "완료" else "편집",
                    fontSize = 11.sp,
                    color = if (editingDeck) NeonGreen else NeonCyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable {
                            if (editingDeck) {
                                onDeckChanged(currentDeck.toList())
                                selectedSlot = -1
                            }
                            editingDeck = !editingDeck
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (i in 0 until DeckManager.DECK_SIZE) {
                    val bp = deckBlueprints[i]
                    val isSelected = editingDeck && selectedSlot == i
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (bp != null) DeckSlotFilledBg else DeckSlotEmptyBg)
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                when {
                                    isSelected -> SelectedSlotBorder
                                    bp != null -> bp.grade.color.copy(alpha = 0.5f)
                                    else -> DeckSlotEmptyBorder
                                },
                                RoundedCornerShape(8.dp),
                            )
                            .clickable(enabled = editingDeck) {
                                if (selectedSlot == i && bp != null) {
                                    // 선택된 슬롯 다시 클릭 → 유닛 제거
                                    currentDeck = currentDeck.toMutableList().apply {
                                        if (i < size) removeAt(i)
                                    }
                                    selectedSlot = -1
                                } else {
                                    selectedSlot = i
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (bp != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = painterResource(blueprintIconRes(bp)),
                                    contentDescription = bp.name,
                                    modifier = Modifier.size(22.dp),
                                )
                                Text(bp.name, color = bp.grade.color, fontSize = 7.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = if (bp.attackRange == AttackRange.MELEE) "근" else "원",
                                    color = SubText, fontSize = 7.sp,
                                )
                            }
                        } else {
                            Text("+", color = DeckSlotEmptyPlus, fontSize = 16.sp)
                        }
                    }
                }
            }

            // ── Inline Deck Editor (toggle) ──
            AnimatedVisibility(
                visible = editingDeck,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    // Race + Range filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Race filters
                        UnitRace.entries.forEach { race ->
                            val selected = raceFilter == race
                            val raceIcon = RACE_ICONS[race] ?: ""
                            GameFilterChip(
                                label = "$raceIcon${race.label}",
                                selected = selected,
                                onClick = { raceFilter = if (selected) null else race },
                            )
                        }
                        // Range filters
                        Text("│", color = SubText.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 2.dp))
                        AttackRange.entries.forEach { range ->
                            val selected = rangeFilter == range
                            GameFilterChip(
                                label = if (range == AttackRange.MELEE) "근거리" else "원거리",
                                selected = selected,
                                onClick = { rangeFilter = if (selected) null else range },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Available units grid
                    val availableUnits = remember(raceFilter, rangeFilter, currentDeck.toList()) {
                        val all = reg?.all() ?: emptyList()
                        all.filter { bp ->
                            bp.isSummonable &&
                            bp.grade == UnitGrade.COMMON &&
                            bp.id !in currentDeck &&
                            (raceFilter == null || bp.race == raceFilter) &&
                            (rangeFilter == null || bp.attackRange == rangeFilter)
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(availableUnits, key = { it.id }) { bp ->
                            Box(
                                modifier = Modifier
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bp.race.color.copy(alpha = 0.08f))
                                    .border(1.dp, bp.race.color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        val deck = currentDeck.toMutableList()
                                        if (selectedSlot in 0 until DeckManager.DECK_SIZE) {
                                            // 선택된 슬롯에 배치
                                            while (deck.size <= selectedSlot) deck.add("")
                                            deck[selectedSlot] = bp.id
                                            selectedSlot = (selectedSlot + 1).coerceAtMost(DeckManager.DECK_SIZE - 1)
                                        } else if (deck.size < DeckManager.DECK_SIZE) {
                                            deck.add(bp.id)
                                        }
                                        currentDeck = deck
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Image(
                                        painter = painterResource(blueprintIconRes(bp)),
                                        contentDescription = bp.name,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(bp.name, color = Color.White, fontSize = 7.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = if (bp.attackRange == AttackRange.MELEE) "근·${if (bp.damageType == com.example.jaygame.engine.DamageType.PHYSICAL) "물" else "마"}"
                                        else "원·${if (bp.damageType == com.example.jaygame.engine.DamageType.PHYSICAL) "물" else "마"}",
                                        color = SubText, fontSize = 6.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!editingDeck && currentDeck.isEmpty()) {
                Text("덱을 편집해 주세요 (자동 채워짐)", fontSize = 10.sp, color = SubText)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Difficulty Selection ──
            Text("난이도 선택", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LightText)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(DIFFICULTIES) { _, diff ->
                    DifficultyCard(diff, selectedDifficulty == diff.id) { onDifficultySelected(diff.id) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Buttons ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NeonButton(
                    text = "취소",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    accentColor = SubText,
                    accentColorDark = DimText,
                )
                NeonButton(
                    text = "\u26A1$staminaCost  출전",
                    onClick = onStartBattle,
                    enabled = hasStamina,
                    modifier = Modifier.weight(2f).height(48.dp),
                    fontSize = 16.sp,
                    accentColor = NeonRed,
                    accentColorDark = NeonRedDark,
                    glowPulse = true,
                )
            }
        }
    }
}

@Composable
private fun DifficultyCard(info: DifficultyInfo, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        if (isSelected) 1.05f else 1f, spring(dampingRatio = 0.7f), label = "diffScale")
    Column(
        modifier = Modifier
            .width(110.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(info.color.copy(alpha = if (isSelected) 0.25f else 0.08f))
            .border(if (isSelected) 2.dp else 1.dp,
                if (isSelected) info.color else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(info.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) info.color else LightText)
        Text(info.desc, fontSize = 9.sp, color = SubText, textAlign = TextAlign.Center, lineHeight = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("\u2694 ", fontSize = 10.sp, color = SubText)
            Text(info.enemyMult, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonRed.copy(alpha = 0.9f))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("\uD83C\uDF81 ", fontSize = 10.sp, color = SubText)
            Text(info.rewardMult, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gold)
        }
    }
}

@Composable
private fun StatChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, fontSize = 11.sp, color = LightText, fontWeight = FontWeight.Medium)
    }
}
