package com.example.jaygame.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.LEVEL_MULTIPLIER
import com.example.jaygame.data.UNIT_DEFS
import com.example.jaygame.data.UNIT_DEFS_MAP
import com.example.jaygame.data.UPGRADE_COSTS
import com.example.jaygame.data.UnitDef
import com.example.jaygame.data.UnitProgress
import com.example.jaygame.ui.components.CurrencyHeader
import com.example.jaygame.ui.components.MedievalButton
import com.example.jaygame.ui.components.MedievalCard
import com.example.jaygame.ui.components.WoodFrame
import com.example.jaygame.ui.theme.DarkBrown
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.MedievalFont
import com.example.jaygame.ui.theme.MetalGray
import com.example.jaygame.ui.theme.Parchment
import com.example.jaygame.ui.theme.PositiveGreen
import java.text.NumberFormat

@Composable
fun CollectionScreen(repository: GameRepository) {
    val data by repository.gameData.collectAsState()
    var selectedUnitId by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBrown),
    ) {
        // Currency Header
        CurrencyHeader(gold = data.gold, diamonds = data.diamonds)

        Spacer(modifier = Modifier.height(12.dp))

        // Title
        Text(
            text = "컬렉션",
            style = MaterialTheme.typography.headlineLarge,
            color = Gold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Top half: Unit Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(UNIT_DEFS, key = { it.id }) { def ->
                val progress = data.units.getOrNull(def.id)
                val isSelected = selectedUnitId == def.id
                CollectionUnitCard(
                    def = def,
                    progress = progress,
                    isSelected = isSelected,
                    onClick = { selectedUnitId = def.id },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom half: Detail Panel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
        ) {
            val selectedDef = if (selectedUnitId >= 0) UNIT_DEFS_MAP[selectedUnitId] else null
            val selectedProgress = if (selectedUnitId >= 0) data.units.getOrNull(selectedUnitId) else null

            if (selectedDef != null && selectedProgress != null) {
                UnitDetailPanel(
                    def = selectedDef,
                    progress = selectedProgress,
                    gold = data.gold,
                    onUpgrade = {
                        val level = selectedProgress.level
                        if (level < 7) {
                            val cost = UPGRADE_COSTS[level - 1]
                            val newUnits = data.units.toMutableList()
                            newUnits[selectedDef.id] = newUnits[selectedDef.id].copy(
                                level = newUnits[selectedDef.id].level + 1,
                                cards = newUnits[selectedDef.id].cards - cost.first,
                            )
                            val newGold = data.gold - cost.second
                            repository.save(data.copy(units = newUnits, gold = newGold))
                        }
                    },
                )
            } else {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "유닛을 선택하세요",
                        fontFamily = MedievalFont,
                        fontSize = 16.sp,
                        color = MetalGray,
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionUnitCard(
    def: UnitDef,
    progress: UnitProgress?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val owned = progress?.owned == true
    val level = progress?.level ?: 1

    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "cardScale",
    )

    MedievalCard(
        borderColor = if (isSelected) Gold else def.rarity.color,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = selectionScale
                scaleY = selectionScale
            }
            .then(if (!owned) Modifier.alpha(0.5f) else Modifier),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Unit icon with lock overlay if not owned
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBrown),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = def.iconRes),
                    contentDescription = def.name,
                    modifier = Modifier
                        .size(36.dp)
                        .then(if (!owned) Modifier.alpha(0.3f) else Modifier),
                )
                if (!owned) {
                    Text(
                        text = "\uD83D\uDD12",
                        fontSize = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Unit name
            Text(
                text = if (owned) def.name else "미보유",
                fontFamily = MedievalFont,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (owned) Parchment else MetalGray,
                textAlign = TextAlign.Center,
            )

            // Level as stars
            if (owned) {
                Text(
                    text = buildStarString(level),
                    fontSize = 10.sp,
                    color = Gold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun UnitDetailPanel(
    def: UnitDef,
    progress: UnitProgress,
    gold: Int,
    onUpgrade: () -> Unit,
) {
    val level = progress.level
    val calculatedATK = (def.baseATK * LEVEL_MULTIPLIER[level - 1]).toInt()
    val upgradeCost = if (level < 7) UPGRADE_COSTS[level - 1] else null
    val canUpgrade = progress.owned && level < 7
            && upgradeCost != null
            && progress.cards >= upgradeCost.first
            && gold >= upgradeCost.second
    val fmt = NumberFormat.getNumberInstance()

    WoodFrame(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Large icon + name + rarity badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBrown),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = def.iconRes),
                        contentDescription = def.name,
                        modifier = Modifier.size(48.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = def.name,
                        fontFamily = MedievalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Parchment,
                    )
                    // Rarity badge
                    Text(
                        text = def.rarity.label,
                        fontFamily = MedievalFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = def.rarity.color,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StatRow("ATK", fmt.format(calculatedATK), Gold)
                StatRow("Speed", String.format("%.1f", def.baseSpeed), Parchment)
                StatRow("Range", String.format("%.0f", def.range), Parchment)
                StatRow("Element", def.element.label, def.element.color)
                StatRow("Ability", def.abilityName, Parchment)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Level with stars
            Text(
                text = "레벨 $level  ${buildStarString(level)}",
                fontFamily = MedievalFont,
                fontSize = 14.sp,
                color = Gold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Card progress
            if (upgradeCost != null) {
                Text(
                    text = "카드: ${progress.cards} / ${upgradeCost.first}",
                    fontFamily = MedievalFont,
                    fontSize = 13.sp,
                    color = if (progress.cards >= upgradeCost.first) PositiveGreen else Parchment,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Upgrade button
            if (!progress.owned) {
                MedievalButton(
                    text = "미보유",
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (level >= 7) {
                MedievalButton(
                    text = "최대 레벨",
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                val goldCost = upgradeCost?.second ?: 0
                MedievalButton(
                    text = "업그레이드 (골드 ${fmt.format(goldCost)})",
                    onClick = onUpgrade,
                    enabled = canUpgrade,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontFamily = MedievalFont,
            fontSize = 12.sp,
            color = MetalGray,
        )
        Text(
            text = value,
            fontFamily = MedievalFont,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

private fun buildStarString(level: Int): String {
    val filled = level.coerceIn(0, 7)
    val empty = (7 - filled).coerceAtLeast(0)
    return "\u2605".repeat(filled) + "\u2606".repeat(empty)
}
