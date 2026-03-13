package com.example.jaygame.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.UnitFamily
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.ResourceHeader
import com.example.jaygame.ui.theme.DarkSurface
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.SubText

// ── Pre-allocated glow colors ──
private val EmptySlotGlowDim = Color(0xFF555555)
private val EmptySlotGlowBright = Color(0xFFAAAAAA)
private val SynergyHighlightAlpha = 0.2f

private data class FamilyInfo(
    val family: UnitFamily,
    val ordinal: Int,
    val icon: String,
    val desc: String,
)

private val FAMILY_INFOS = listOf(
    FamilyInfo(UnitFamily.FIRE, 0, "\uD83D\uDD25", "범위 피해"),
    FamilyInfo(UnitFamily.FROST, 1, "\u2744\uFE0F", "둔화/빙결"),
    FamilyInfo(UnitFamily.POISON, 2, "\uD83D\uDCA8", "지속 피해"),
    FamilyInfo(UnitFamily.LIGHTNING, 3, "\u26A1", "연쇄 공격"),
    FamilyInfo(UnitFamily.SUPPORT, 4, "\uD83D\uDE4F", "아군 버프"),
    FamilyInfo(UnitFamily.WIND, 5, "\uD83C\uDF00", "넉백/회피"),
)

@Composable
fun DeckScreen(repository: GameRepository) {
    val data by repository.gameData.collectAsState()

    val deck = remember(data) {
        mutableStateListOf<Int>().apply {
            val source = data.deck
            for (i in 0 until 5) {
                add(source.getOrElse(i) { -1 })
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val currentData = repository.gameData.value
            repository.save(currentData.copy(deck = buildSaveDeck(deck.toList())))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDark),
    ) {
        ResourceHeader(gold = data.gold, diamonds = data.diamonds)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "덱 편집",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Gold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "소환할 계열을 5개 선택하세요 (중복 가능)",
            fontSize = 12.sp,
            color = SubText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Deck Slots ──
        GameCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            borderColor = Gold.copy(alpha = 0.6f),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "배틀 덱",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (slotIndex in 0 until 5) {
                        val familyOrdinal = deck[slotIndex]
                        val info = FAMILY_INFOS.getOrNull(familyOrdinal)

                        DeckSlotChip(
                            info = info,
                            onClick = {
                                if (familyOrdinal >= 0) {
                                    deck[slotIndex] = -1
                                }
                            },
                        )
                    }
                }
            }
        }

        // ── Synergy Preview ──
        SynergyPreview(deck = deck)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "계열 선택",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = SubText,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Family Cards Grid ──
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(FAMILY_INFOS, key = { it.ordinal }) { info ->
                val countInDeck = deck.count { it == info.ordinal }
                FamilyCard(
                    info = info,
                    countInDeck = countInDeck,
                    onClick = {
                        val emptySlot = deck.indexOf(-1)
                        if (emptySlot >= 0) {
                            deck[emptySlot] = info.ordinal
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DeckSlotChip(
    info: FamilyInfo?,
    onClick: () -> Unit,
) {
    val color = info?.family?.color ?: DimText

    // Pulsing glow for empty slots
    val glowBorderColor = if (info == null) {
        val transition = rememberInfiniteTransition(label = "slotGlow")
        val pulse by transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "slotGlowPulse",
        )
        Gold.copy(alpha = pulse)
    } else {
        color.copy(alpha = 0.8f)
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (info != null) 2.dp else 1.5.dp,
                color = glowBorderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .background(
                if (info != null) color.copy(alpha = 0.15f) else DarkSurface,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (info != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = info.icon,
                    fontSize = 20.sp,
                )
                Text(
                    text = info.family.label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
        } else {
            Text(
                text = "+",
                fontSize = 22.sp,
                color = DimText.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun FamilyCard(
    info: FamilyInfo,
    countInDeck: Int,
    onClick: () -> Unit,
) {
    val color = info.family.color
    val isDeckFull = false // Will be checked by caller

    GameCard(
        borderColor = if (countInDeck > 0) color else color.copy(alpha = 0.3f),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.08f)),
                        ),
                    )
                    .border(1.5.dp, color.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = info.icon,
                    fontSize = 20.sp,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.family.label,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = color,
                )
                Text(
                    text = info.desc,
                    fontSize = 11.sp,
                    color = SubText,
                )
            }

            // Count badge
            if (countInDeck > 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f))
                        .border(1.dp, color.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$countInDeck",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SynergyPreview(deck: List<Int>) {
    // Count families in deck
    val familyCounts = remember(deck.toList()) {
        val counts = mutableMapOf<Int, Int>()
        for (ordinal in deck) {
            if (ordinal >= 0) {
                counts[ordinal] = (counts[ordinal] ?: 0) + 1
            }
        }
        counts
    }

    val activeSynergies = familyCounts.filter { it.value >= 2 }

    if (activeSynergies.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "시너지 활성",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                activeSynergies.forEach { (ordinal, count) ->
                    val info = FAMILY_INFOS.getOrNull(ordinal) ?: return@forEach
                    val familyColor = info.family.color
                    val isHighlighted = count >= 2

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isHighlighted) familyColor.copy(alpha = SynergyHighlightAlpha)
                                else Color.Transparent,
                            )
                            .border(
                                width = 1.dp,
                                color = if (isHighlighted) familyColor.copy(alpha = 0.6f)
                                else DimText.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "${info.icon} ${info.family.label} x$count",
                            fontSize = 11.sp,
                            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                            color = if (isHighlighted) familyColor else SubText,
                        )
                    }
                }
            }
        }
    }
}

private fun buildSaveDeck(deck: List<Int>): List<Int> {
    val valid = deck.filter { it >= 0 }
    if (valid.isEmpty()) return listOf(0, 1, 2, 3, 4)
    if (valid.size >= 5) return valid.take(5)
    val result = valid.toMutableList()
    // Fill remaining slots with existing families
    val families = UnitFamily.entries.map { it.ordinal }
    for (f in families) {
        if (result.size >= 5) break
        result.add(f)
    }
    while (result.size < 5) result.add(result.first())
    return result
}
