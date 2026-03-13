package com.example.jaygame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.ALL_RELICS
import com.example.jaygame.data.GameData
import com.example.jaygame.data.RelicGrade
import com.example.jaygame.data.RelicProgress
import com.example.jaygame.data.relicUpgradeCost
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.Divider
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.GoldCoin
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonGreen
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark
import com.example.jaygame.ui.theme.SubText

// ── Pre-allocated colors ──
private val BgDark = Color(0xFF1A1A2E)
private val BgCard = Color(0xFF16213E)
private val BgPanel = Color(0xFF0F3460)
private val BgSlotEmpty = Color(0xFF2A2A3E)
private val TextPrimary = Color(0xFFE0E0E0)
private val TextSecondary = Color(0xFFAAAAAA)
private val LockColor = Color(0xFF555555)

private fun gradeColor(gradeOrdinal: Int): Color {
    val hex = RelicGrade.entries.getOrNull(gradeOrdinal)?.colorHex ?: 0xFF9E9E9EL
    return Color(hex)
}

private fun resolveDescription(template: String, level: Int, effectPerLevel: Float): String {
    val value = (level * effectPerLevel).toInt()
    return template
        .replace("{lv}", level.toString())
        .replace("{lv}×${effectPerLevel.toInt()}%", "$value%")
        .replace("{lv}×${effectPerLevel.toInt()}", "$value")
}

@Composable
fun RelicScreen(
    gameData: GameData,
    onUpgrade: (relicId: Int) -> Unit,
    onEquip: (relicId: Int) -> Unit,
    onUnequip: (relicId: Int) -> Unit,
    onBack: () -> Unit = {},
    showTopBar: Boolean = true,
) {
    var selectedRelicId by remember { mutableIntStateOf(-1) }
    val selectedRelic = remember(selectedRelicId) {
        if (selectedRelicId >= 0) gameData.relics.getOrNull(selectedRelicId) else null
    }
    val selectedDef = remember(selectedRelicId) {
        ALL_RELICS.getOrNull(selectedRelicId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark),
    ) {
        // ── Top bar ──
        if (showTopBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0D1F))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeonButton(
                    text = "< 뒤로",
                    onClick = onBack,
                    fontSize = 13.sp,
                    accentColor = SubText,
                    accentColorDark = DimText,
                )
                Text(
                    text = "유물",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(72.dp))
            }
        }

        // ── Gold resource display ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "골드: ${gameData.gold.formatNum()}",
                fontSize = 13.sp,
                color = GoldCoin,
                fontWeight = FontWeight.Bold,
            )
        }

        // ── Equipped slots bar ──
        EquippedSlotsBar(
            gameData = gameData,
            selectedRelicId = selectedRelicId,
            onSlotClick = { id -> selectedRelicId = if (selectedRelicId == id) -1 else id },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Main content ──
        if (selectedRelic != null && selectedDef != null) {
            // Detail panel visible — show grid + detail
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(ALL_RELICS) { def ->
                    val progress = gameData.relics.getOrNull(def.id)
                    RelicGridItem(
                        def = def,
                        progress = progress,
                        isSelected = selectedRelicId == def.id,
                        onClick = {
                            selectedRelicId = if (selectedRelicId == def.id) -1 else def.id
                        },
                    )
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
            }

            // Detail panel at bottom
            RelicDetailPanel(
                gameData = gameData,
                relicId = selectedRelicId,
                progress = selectedRelic,
                onUpgrade = onUpgrade,
                onEquip = onEquip,
                onUnequip = onUnequip,
                onClose = { selectedRelicId = -1 },
            )
        } else {
            // Full grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(ALL_RELICS) { def ->
                    val progress = gameData.relics.getOrNull(def.id)
                    RelicGridItem(
                        def = def,
                        progress = progress,
                        isSelected = false,
                        onClick = {
                            selectedRelicId = def.id
                        },
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun EquippedSlotsBar(
    gameData: GameData,
    selectedRelicId: Int,
    onSlotClick: (Int) -> Unit,
) {
    val slotCount = gameData.equippedSlotCount

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D1F))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = "장착 유물 (${gameData.equippedRelics.size}/${slotCount})",
            fontSize = 12.sp,
            color = SubText,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(4) { slotIndex ->
                val isUnlocked = slotIndex < slotCount
                val equippedRelicId = gameData.equippedRelics.getOrNull(slotIndex)
                val equippedDef = if (equippedRelicId != null) ALL_RELICS.getOrNull(equippedRelicId) else null
                val equippedProgress = if (equippedRelicId != null) gameData.relics.getOrNull(equippedRelicId) else null
                val isSelected = equippedRelicId != null && equippedRelicId == selectedRelicId

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                !isUnlocked -> Color(0xFF111122)
                                equippedDef != null -> BgCard
                                else -> BgSlotEmpty
                            },
                        )
                        .then(
                            if (isSelected) Modifier.border(2.dp, Gold, RoundedCornerShape(10.dp))
                            else if (equippedDef != null && equippedProgress != null) {
                                val gc = gradeColor(equippedProgress.grade)
                                Modifier.border(1.5.dp, gc.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            } else Modifier.border(1.dp, Divider, RoundedCornerShape(10.dp)),
                        )
                        .then(
                            if (isUnlocked && equippedRelicId != null) Modifier.clickable { onSlotClick(equippedRelicId) }
                            else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!isUnlocked) {
                        Text(text = "\uD83D\uDD12", fontSize = 18.sp, color = LockColor)
                    } else if (equippedDef != null && equippedProgress != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = relicTypeIcon(equippedDef.type.name),
                                fontSize = 20.sp,
                            )
                            Text(
                                text = equippedDef.name,
                                fontSize = 8.sp,
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Lv.${equippedProgress.level}",
                                fontSize = 8.sp,
                                color = gradeColor(equippedProgress.grade),
                            )
                        }
                    } else {
                        Text(text = "+", fontSize = 22.sp, color = SubText)
                    }
                }
            }
        }
    }
}

@Composable
private fun RelicGridItem(
    def: com.example.jaygame.data.RelicDef,
    progress: RelicProgress?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val owned = progress?.owned == true
    val gradeOrdinal = progress?.grade ?: def.minGrade.ordinal
    val gc = gradeColor(gradeOrdinal)

    Box(
        modifier = Modifier
            .height(90.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (owned) BgCard else Color(0xFF111122))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = when {
                    isSelected -> Gold
                    owned -> gc.copy(alpha = 0.6f)
                    else -> Divider
                },
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (!owned) {
            // Grayed out locked state
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "\uD83D\uDD12", fontSize = 20.sp, color = LockColor)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = def.name,
                    fontSize = 9.sp,
                    color = DimText,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp),
            ) {
                Text(
                    text = relicTypeIcon(def.type.name),
                    fontSize = 22.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = def.name,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Lv.${progress!!.level}",
                    fontSize = 9.sp,
                    color = gc,
                )
                Text(
                    text = RelicGrade.entries.getOrNull(gradeOrdinal)?.label ?: "",
                    fontSize = 8.sp,
                    color = gc.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun RelicDetailPanel(
    gameData: GameData,
    relicId: Int,
    progress: RelicProgress,
    onUpgrade: (relicId: Int) -> Unit,
    onEquip: (relicId: Int) -> Unit,
    onUnequip: (relicId: Int) -> Unit,
    onClose: () -> Unit,
) {
    val def = ALL_RELICS.getOrNull(relicId) ?: return
    val grade = RelicGrade.entries.getOrNull(progress.grade) ?: RelicGrade.COMMON
    val gc = gradeColor(progress.grade)
    val maxLevel = grade.maxLevel
    val isMaxLevel = progress.level >= maxLevel
    val upgradeCost = if (!isMaxLevel) relicUpgradeCost(progress.level) else 0
    val canAfford = gameData.gold >= upgradeCost
    val isEquipped = relicId in gameData.equippedRelics
    val canEquip = !isEquipped && gameData.equippedRelics.size < gameData.equippedSlotCount

    val effectDesc = resolveDescription(def.description, progress.level, def.effectPerLevel)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D1F))
            .padding(12.dp),
    ) {
        HorizontalDivider(color = gc.copy(alpha = 0.4f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            // Icon + grade badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgCard)
                    .border(2.dp, gc, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = relicTypeIcon(def.type.name), fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = def.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(gc.copy(alpha = 0.2f))
                            .border(1.dp, gc.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(text = grade.label, fontSize = 10.sp, color = gc, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "레벨 ${progress.level} / $maxLevel",
                    fontSize = 12.sp,
                    color = SubText,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = effectDesc,
                    fontSize = 12.sp,
                    color = NeonGreen,
                )
            }

            // Close button
            NeonButton(
                text = "닫기",
                onClick = onClose,
                fontSize = 11.sp,
                accentColor = SubText,
                accentColorDark = DimText,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Upgrade button
            if (progress.owned) {
                NeonButton(
                    text = if (isMaxLevel) "최대 레벨" else "강화 (골드 ${upgradeCost.formatNum()})",
                    onClick = { if (!isMaxLevel && canAfford) onUpgrade(relicId) },
                    enabled = !isMaxLevel && canAfford,
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    accentColor = if (isMaxLevel) DimText else if (canAfford) Gold else NeonRed,
                    accentColorDark = if (isMaxLevel) DimText.copy(alpha = 0.5f) else if (canAfford) com.example.jaygame.ui.theme.DarkGold else NeonRedDark,
                )
                // Equip / Unequip button
                NeonButton(
                    text = if (isEquipped) "해제" else "장착",
                    onClick = {
                        if (isEquipped) onUnequip(relicId) else if (canEquip) onEquip(relicId)
                    },
                    enabled = isEquipped || canEquip,
                    modifier = Modifier.width(80.dp),
                    fontSize = 12.sp,
                    accentColor = if (isEquipped) NeonRed else NeonGreen,
                    accentColorDark = if (isEquipped) NeonRedDark else NeonGreen.copy(alpha = 0.6f),
                )
            }
        }
    }
}

private fun relicTypeIcon(typeName: String): String = when (typeName) {
    "ECONOMY" -> "\uD83D\uDCB0"
    "COMBAT" -> "\u2694\uFE0F"
    "UTILITY" -> "\u2699\uFE0F"
    else -> "\u2B50"
}

private fun Int.formatNum(): String {
    return if (this >= 1000) {
        val thousands = this / 1000
        val remainder = this % 1000
        if (remainder == 0) "${thousands},000" else "$thousands,${remainder.toString().padStart(3, '0')}"
    } else this.toString()
}
