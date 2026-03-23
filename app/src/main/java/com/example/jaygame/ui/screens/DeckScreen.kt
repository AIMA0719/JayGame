package com.example.jaygame.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.jaygame.data.UnitFamily
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.UnitBlueprint
import com.example.jaygame.engine.UnitGrade
import com.example.jaygame.ui.components.FAMILY_ICONS
import com.example.jaygame.ui.components.GameFilterChip
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.components.ROLE_LABELS
import com.example.jaygame.ui.theme.*
import com.example.jaygame.ui.viewmodel.DeckViewModel

// ── Pre-allocated colors (GC-safe) ──
private val DeckScreenBg = Brush.verticalGradient(listOf(Color(0xFF0A0A1A), Color(0xFF1A1028)))
private val SlotFilledBg = Color(0xFF1A1A30)
private val SlotEmptyBg = Color(0xFF1A1A2E)
private val SlotEmptyBorder = Color(0xFF3A3A5E)
private val SlotFilledBorder = Color(0xFF5BA4CF)
private val PresetActiveBg = Color(0xFF2A1F40)
private val PresetInactiveBg = Color(0xFF151520)
private val PresetActiveBorder = Color(0xFFD4A847)
private val PresetInactiveBorder = Color(0xFF3A3A50)
private val ChainArrowColor = Color(0xFF6B5D50)
private val NeonCyanDim = NeonCyan.copy(alpha = 0.7f)
private val DeckSlotShape = RoundedCornerShape(12.dp)
private val UnitCardShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeckScreen(
    viewModel: DeckViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.container.stateFlow.collectAsState()
    val gameData = state.gameData

    val deckBlueprints = remember(state.currentDeck) {
        if (!BlueprintRegistry.isReady) List(DeckManager.DECK_SIZE) { null }
        else {
            val reg = BlueprintRegistry.instance
            List(DeckManager.DECK_SIZE) { i -> state.currentDeck.getOrNull(i)?.let { reg.findById(it) } }
        }
    }

    val filteredUnits = remember(state.availableUnits, state.selectedFamily) {
        if (state.selectedFamily != null) {
            state.availableUnits.filter { state.selectedFamily in it.families }
        } else {
            state.availableUnits
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeckScreenBg)
            .padding(horizontal = 16.dp),
    ) {
        // ── Title bar ──
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "←",
                color = LightText,
                fontSize = 24.sp,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp),
            )
            Text(
                text = "덱 편집",
                color = LightText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            NeonButton(
                text = "저장",
                onClick = {
                    viewModel.saveDeck()
                    onBack()
                },
                modifier = Modifier.height(36.dp),
            )
        }
        Spacer(Modifier.height(12.dp))

        // ── Preset tabs ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (i in 0 until DeckManager.MAX_PRESETS) {
                val isActive = state.activePresetIndex == i
                val bgColor by animateColorAsState(
                    if (isActive) PresetActiveBg else PresetInactiveBg,
                    animationSpec = tween(200),
                    label = "presetBg$i",
                )
                val borderColor by animateColorAsState(
                    if (isActive) PresetActiveBorder else PresetInactiveBorder,
                    animationSpec = tween(200),
                    label = "presetBorder$i",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable { viewModel.selectPreset(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "덱 ${i + 1}",
                        color = if (isActive) Gold else SubText,
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── Deck slots (5개) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (i in 0 until DeckManager.DECK_SIZE) {
                val bp = deckBlueprints[i]
                val blueprintId = state.currentDeck.getOrNull(i)

                DeckSlotItem(
                    blueprint = bp,
                    slotIndex = i,
                    onClick = {
                        if (blueprintId != null) viewModel.removeUnit(blueprintId)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // ── 합성 체인 미리보기 (첫 번째 덱 유닛) ──
        if (state.currentDeck.isNotEmpty()) {
            val firstChain = remember(state.currentDeck.firstOrNull()) {
                state.currentDeck.firstOrNull()?.let { DeckManager.getMergeChain(it) } ?: emptyList()
            }
            if (firstChain.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("합성 체인: ", color = SubText, fontSize = 11.sp)
                    firstChain.forEachIndexed { idx, chainBp ->
                        Text(
                            text = chainBp.name,
                            color = chainBp.grade.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (idx < firstChain.size - 1) {
                            Text(" → ", color = ChainArrowColor, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // ── Auto-fill button ──
        NeonButton(
            text = "자동 채우기",
            onClick = { viewModel.autoFill() },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
        )
        Spacer(Modifier.height(12.dp))

        // ── Family filter chips ──
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            UnitFamily.entries.forEach { family ->
                GameFilterChip(
                    label = "${FAMILY_ICONS[family] ?: ""} ${family.name}",
                    selected = state.selectedFamily == family,
                    onClick = { viewModel.toggleFamilyFilter(family) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // ── Unit grid ──
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(filteredUnits, key = { it.id }) { bp ->
                val isInDeck = bp.id in state.currentDeck
                val isOwned = gameData.units[bp.id]?.owned == true
                AvailableUnitCard(
                    blueprint = bp,
                    isInDeck = isInDeck,
                    isOwned = isOwned,
                    onClick = {
                        if (!isInDeck) viewModel.addUnit(bp.id)
                        else viewModel.removeUnit(bp.id)
                    },
                )
            }
        }
    }
}

// ── Deck slot item ──
@Composable
private fun DeckSlotItem(
    blueprint: UnitBlueprint?,
    slotIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(DeckSlotShape)
            .background(if (blueprint != null) SlotFilledBg else SlotEmptyBg)
            .border(
                width = if (blueprint != null) 1.5.dp else 1.dp,
                color = if (blueprint != null) blueprint.grade.color.copy(alpha = 0.6f) else SlotEmptyBorder,
                shape = DeckSlotShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (blueprint != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp),
            ) {
                Image(
                    painter = painterResource(blueprintIconRes(blueprint)),
                    contentDescription = blueprint.name,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = blueprint.name,
                    color = blueprint.grade.color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = ROLE_LABELS[blueprint.role] ?: blueprint.role.name,
                    color = SubText,
                    fontSize = 8.sp,
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("+", color = SlotEmptyBorder, fontSize = 24.sp)
                Text("슬롯 ${slotIndex + 1}", color = DimText, fontSize = 9.sp)
            }
        }
    }
}

// ── Available unit card ──
@Composable
private fun AvailableUnitCard(
    blueprint: UnitBlueprint,
    isInDeck: Boolean,
    isOwned: Boolean,
    onClick: () -> Unit,
) {
    val alpha = when {
        isInDeck -> 0.4f
        !isOwned -> 0.5f
        else -> 1f
    }

    Box(
        modifier = Modifier
            .height(100.dp)
            .clip(UnitCardShape)
            .background(SlotEmptyBg)
            .border(
                width = if (isInDeck) 2.dp else 1.dp,
                color = if (isInDeck) NeonCyanDim else blueprint.grade.color.copy(alpha = 0.3f),
                shape = UnitCardShape,
            )
            .graphicsLayer { this.alpha = alpha }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp),
        ) {
            // Family icon
            Text(
                text = FAMILY_ICONS[blueprint.families.firstOrNull()] ?: "",
                fontSize = 10.sp,
            )
            Image(
                painter = painterResource(blueprintIconRes(blueprint)),
                contentDescription = blueprint.name,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = blueprint.name,
                color = blueprint.grade.color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = ROLE_LABELS[blueprint.role] ?: blueprint.role.name,
                color = SubText,
                fontSize = 8.sp,
            )
            if (isInDeck) {
                Text("덱에 포함", color = NeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
