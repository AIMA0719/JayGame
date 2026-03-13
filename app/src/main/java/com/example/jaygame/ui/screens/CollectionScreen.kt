package com.example.jaygame.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.LEVEL_MULTIPLIER
import com.example.jaygame.data.UNIT_DEFS
import com.example.jaygame.data.UPGRADE_COSTS
import com.example.jaygame.data.UnitDef
import com.example.jaygame.data.UnitFamily
import com.example.jaygame.data.UnitGrade
import com.example.jaygame.data.UnitProgress
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.DarkSurface
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.NeonGreen
import com.example.jaygame.ui.theme.PositiveGreen
import com.example.jaygame.ui.theme.SubText
import java.text.NumberFormat

// ── Grade-based card background colors (pre-allocated) ──
private val GradeBgCommon = Color(0xFF424242)
private val GradeBgRare = Color(0xFF1A237E)
private val GradeBgHero = Color(0xFF4A148C)
private val GradeBorderGold = Color(0xFFFFD700)
private val GradeBorderRed = Color(0xFFEF4444)
private val GradeBorderRainbowStart = Color(0xFFFF6B35)
private val GradeBorderRainbowEnd = Color(0xFFFBBF24)

private fun gradeBackgroundColor(grade: UnitGrade): Color = when (grade) {
    UnitGrade.COMMON -> GradeBgCommon
    UnitGrade.RARE -> GradeBgRare
    UnitGrade.HERO -> GradeBgHero
    UnitGrade.LEGEND -> GradeBgCommon
    UnitGrade.ANCIENT -> GradeBgCommon
    UnitGrade.MYTHIC -> GradeBgCommon
    UnitGrade.IMMORTAL -> GradeBgCommon
}

private fun gradeGlowColor(grade: UnitGrade): Color? = when (grade) {
    UnitGrade.COMMON -> null
    UnitGrade.RARE -> null
    UnitGrade.HERO -> null
    UnitGrade.LEGEND -> GradeBorderGold
    UnitGrade.ANCIENT -> GradeBorderRed
    UnitGrade.MYTHIC -> GradeBorderRainbowEnd
    UnitGrade.IMMORTAL -> GradeBorderRainbowStart
}

private val FAMILY_ICONS = mapOf(
    UnitFamily.FIRE to "\uD83D\uDD25",
    UnitFamily.FROST to "\u2744\uFE0F",
    UnitFamily.POISON to "\uD83D\uDCA8",
    UnitFamily.LIGHTNING to "\u26A1",
    UnitFamily.SUPPORT to "\uD83D\uDE4F",
    UnitFamily.WIND to "\uD83C\uDF00",
)

@Composable
fun CollectionScreen(repository: GameRepository) {
    val data by repository.gameData.collectAsState()
    var selectedUnit by remember { mutableStateOf<UnitDef?>(null) }

    // Group units by family
    val unitsByFamily = remember {
        UnitFamily.entries.map { family ->
            family to UNIT_DEFS.filter { it.family == family }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepDark),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "영웅 도감",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = LightText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Owned count
            val ownedCount = data.units.count { it.owned }
            Text(
                text = "보유 $ownedCount / ${UNIT_DEFS.size}",
                fontSize = 12.sp,
                color = SubText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Family rows with horizontal scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                unitsByFamily.forEach { (family, units) ->
                    FamilyUnitRow(
                        family = family,
                        units = units,
                        unitProgress = data.units,
                        onUnitClick = { selectedUnit = it },
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Detail dialog overlay
        selectedUnit?.let { def ->
            val progress = data.units.getOrNull(def.id)
            CollectionUnitDetailDialog(
                def = def,
                progress = progress,
                gold = data.gold,
                onUpgrade = {
                    if (progress != null && progress.level < 7) {
                        val cost = UPGRADE_COSTS[progress.level - 1]
                        val newUnits = data.units.toMutableList()
                        newUnits[def.id] = newUnits[def.id].copy(
                            level = newUnits[def.id].level + 1,
                            cards = newUnits[def.id].cards - cost.first,
                        )
                        val newGold = data.gold - cost.second
                        val newMaxLevel = maxOf(data.maxUnitLevel, newUnits[def.id].level)
                        repository.save(data.copy(units = newUnits, gold = newGold, maxUnitLevel = newMaxLevel))
                    }
                },
                onDismiss = { selectedUnit = null },
            )
        }
    }
}

@Composable
private fun FamilyUnitRow(
    family: UnitFamily,
    units: List<UnitDef>,
    unitProgress: List<UnitProgress>,
    onUnitClick: (UnitDef) -> Unit,
) {
    val icon = FAMILY_ICONS[family] ?: ""
    val ownedInFamily = units.count { def ->
        unitProgress.getOrNull(def.id)?.owned == true
    }

    Column(modifier = Modifier.padding(start = 16.dp)) {
        // Family header
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = family.label,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = family.color,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$ownedInFamily/${units.size}",
                fontSize = 12.sp,
                color = SubText,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal scroll of unit cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            units.forEach { def ->
                val progress = unitProgress.getOrNull(def.id)
                CollectionUnitCard(
                    def = def,
                    progress = progress,
                    onClick = { onUnitClick(def) },
                )
            }
            Spacer(modifier = Modifier.width(8.dp)) // end padding
        }
    }
}

@Composable
private fun CollectionUnitCard(
    def: UnitDef,
    progress: UnitProgress?,
    onClick: () -> Unit,
) {
    val owned = progress?.owned == true
    val level = progress?.level ?: 1

    GameCard(
        borderColor = def.grade.color,
        backgroundColor = gradeBackgroundColor(def.grade),
        glowColor = gradeGlowColor(def.grade),
        onClick = onClick,
        modifier = Modifier
            .width(80.dp)
            .then(if (!owned) Modifier.alpha(0.5f) else Modifier),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface),
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

            Text(
                text = if (owned) def.name else "???",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (owned) LightText else DimText,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Text(
                text = def.grade.label,
                fontSize = 9.sp,
                color = def.grade.color.copy(alpha = if (owned) 1f else 0.5f),
                textAlign = TextAlign.Center,
            )

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
private fun CollectionUnitDetailDialog(
    def: UnitDef,
    progress: UnitProgress?,
    gold: Int,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    val owned = progress?.owned == true
    val level = progress?.level ?: 1
    val calculatedATK = (def.baseATK * LEVEL_MULTIPLIER[level - 1]).toInt()
    val upgradeCost = if (level < 7) UPGRADE_COSTS[level - 1] else null
    val canUpgrade = owned && level < 7
            && upgradeCost != null
            && (progress?.cards ?: 0) >= upgradeCost.first
            && gold >= upgradeCost.second
    val fmt = NumberFormat.getNumberInstance()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        GameCard(
            modifier = Modifier
                .width(300.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
            borderColor = def.grade.color,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Large icon + name + rarity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = def.iconRes),
                            contentDescription = def.name,
                            modifier = Modifier.size(42.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = def.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = LightText,
                        )
                        Text(
                            text = def.grade.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = def.grade.color,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    StatRow("공격력", fmt.format(calculatedATK), Gold)
                    StatRow("공속", String.format("%.1f", def.baseSpeed), LightText)
                    StatRow("사거리", String.format("%.0f", def.range), LightText)
                    StatRow("계열", def.family.label, def.family.color)
                    StatRow("능력", def.abilityName, NeonCyan)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Description
                Text(
                    text = def.description,
                    fontSize = 12.sp,
                    color = SubText,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Unique Ability section (Hero grade and above)
                if (def.uniqueAbility != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                def.grade.color.copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "\u2726 ",
                                fontSize = 13.sp,
                                color = def.grade.color,
                            )
                            Text(
                                text = def.uniqueAbility.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = def.grade.color,
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (def.uniqueAbility.cooldown > 0)
                                "[${def.uniqueAbility.type}] 쿨타임: ${def.uniqueAbility.cooldown}초"
                            else
                                "[${def.uniqueAbility.type}]",
                            fontSize = 10.sp,
                            color = SubText,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = def.uniqueAbility.description,
                            fontSize = 11.sp,
                            color = LightText.copy(alpha = 0.9f),
                            lineHeight = 16.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Level with stars
                if (owned) {
                    Text(
                        text = "레벨 $level  ${buildStarString(level)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Card progress
                    if (upgradeCost != null) {
                        Text(
                            text = "카드: ${progress?.cards ?: 0} / ${upgradeCost.first}",
                            fontSize = 12.sp,
                            color = if ((progress?.cards ?: 0) >= upgradeCost.first) PositiveGreen else SubText,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Upgrade button
                    if (level >= 7) {
                        NeonButton(
                            text = "최대 레벨",
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = DimText,
                            accentColorDark = DimText,
                        )
                    } else {
                        val goldCost = upgradeCost?.second ?: 0
                        NeonButton(
                            text = "업그레이드 (골드 ${fmt.format(goldCost)})",
                            onClick = onUpgrade,
                            enabled = canUpgrade,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp,
                            accentColor = NeonGreen,
                            accentColorDark = NeonGreen.copy(alpha = 0.5f),
                        )
                    }
                } else {
                    NeonButton(
                        text = "미보유",
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = DimText,
                        accentColorDark = DimText,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                NeonButton(
                    text = "닫기",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = SubText,
                    accentColorDark = DimText,
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
            fontSize = 12.sp,
            color = SubText,
        )
        Text(
            text = value,
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
