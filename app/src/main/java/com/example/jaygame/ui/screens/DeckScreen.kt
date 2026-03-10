package com.example.jaygame.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.UNIT_DEFS
import com.example.jaygame.data.UNIT_DEFS_MAP
import com.example.jaygame.data.UnitProgress
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.ResourceHeader
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.DarkSurface
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.PositiveGreen
import com.example.jaygame.ui.theme.SubText

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
            repository.save(currentData.copy(deck = buildSaveDeck(deck.toList(), currentData.units)))
        }
    }

    val ownedUnits = remember(data) {
        UNIT_DEFS.filter { def ->
            data.units.getOrNull(def.id)?.owned == true
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
            color = LightText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Deck Slots
        GameCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            borderColor = NeonRed.copy(alpha = 0.6f),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (slotIndex in 0 until 5) {
                    val unitId = deck[slotIndex]
                    val def = if (unitId >= 0) UNIT_DEFS_MAP[unitId] else null
                    val progress = if (unitId >= 0) data.units.getOrNull(unitId) else null

                    DeckSlotItem(
                        unitId = unitId,
                        iconRes = def?.iconRes,
                        name = def?.name,
                        level = progress?.level,
                        rarityColor = def?.grade?.color,
                        onClick = {
                            if (unitId >= 0) {
                                deck[slotIndex] = -1
                            }
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "보유 유닛",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = SubText,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ownedUnits, key = { it.id }) { def ->
                val progress = data.units.getOrNull(def.id)
                val isInDeck = def.id in deck

                InventoryUnitCard(
                    iconRes = def.iconRes,
                    name = def.name,
                    level = progress?.level ?: 1,
                    rarityColor = def.grade.color,
                    isInDeck = isInDeck,
                    onClick = {
                        if (!isInDeck) {
                            val emptySlot = deck.indexOf(-1)
                            if (emptySlot >= 0) {
                                deck[emptySlot] = def.id
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DeckSlotItem(
    unitId: Int,
    iconRes: Int?,
    name: String?,
    level: Int?,
    rarityColor: Color?,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (unitId >= 0) {
                        Modifier.border(2.dp, rarityColor ?: DimText, RoundedCornerShape(8.dp))
                    } else {
                        Modifier.border(
                            1.dp,
                            DimText,
                            RoundedCornerShape(8.dp),
                        )
                    }
                )
                .background(DarkSurface),
            contentAlignment = Alignment.Center,
        ) {
            if (unitId >= 0 && iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = name,
                    modifier = Modifier.size(36.dp),
                )
            } else {
                Text(
                    text = "+",
                    fontSize = 18.sp,
                    color = DimText,
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = name ?: "빈 슬롯",
            fontSize = 10.sp,
            color = if (unitId >= 0) LightText else DimText,
            textAlign = TextAlign.Center,
        )
        if (unitId >= 0 && level != null) {
            Text(
                text = "Lv.$level",
                fontSize = 9.sp,
                color = Gold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun InventoryUnitCard(
    iconRes: Int,
    name: String,
    level: Int,
    rarityColor: Color,
    isInDeck: Boolean,
    onClick: () -> Unit,
) {
    GameCard(
        borderColor = rarityColor,
        onClick = if (!isInDeck) onClick else null,
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
                    painter = painterResource(id = iconRes),
                    contentDescription = name,
                    modifier = Modifier.size(36.dp),
                )
                // Checkmark overlay if in deck
                if (isInDeck) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DeepDark.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "\u2713",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PositiveGreen,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = LightText,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Lv.$level",
                fontSize = 10.sp,
                color = Gold,
                textAlign = TextAlign.Center,
            )
            if (isInDeck) {
                Text(
                    text = "배치됨",
                    fontSize = 9.sp,
                    color = NeonCyan,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun buildSaveDeck(deck: List<Int>, allUnits: List<UnitProgress>): List<Int> {
    val valid = deck.filter { it >= 0 }
    if (valid.isEmpty()) return listOf(0, 1, 2, 3, 4)
    if (valid.size >= 5) return valid.take(5)
    val result = valid.toMutableList()
    val available = allUnits.indices.filter { allUnits[it].owned && it !in result }
    for (id in available) {
        if (result.size >= 5) break
        result.add(id)
    }
    while (result.size < 5) result.add(result.first())
    return result
}
