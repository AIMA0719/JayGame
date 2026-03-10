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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import androidx.compose.runtime.collectAsState
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.UNIT_DEFS
import com.example.jaygame.data.UNIT_DEFS_MAP
import com.example.jaygame.data.UnitProgress
import com.example.jaygame.ui.components.CurrencyHeader
import com.example.jaygame.ui.components.MedievalCard
import com.example.jaygame.ui.components.WoodFrame
import com.example.jaygame.ui.theme.DarkBrown
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.MedievalFont
import com.example.jaygame.ui.theme.MetalGray
import com.example.jaygame.ui.theme.Parchment

@Composable
fun DeckScreen(repository: GameRepository) {
    val data by repository.gameData.collectAsState()

    // Mutable deck state: always 5 slots, -1 means empty
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

    // Owned units for inventory
    val ownedUnits = remember(data) {
        UNIT_DEFS.filter { def ->
            data.units.getOrNull(def.id)?.owned == true
        }
    }

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
            text = "덱 편집",
            style = MaterialTheme.typography.headlineLarge,
            color = Gold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Deck Slots in WoodFrame
        WoodFrame(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
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
                            rarityColor = def?.rarity?.color,
                            onClick = {
                                // Tap deck slot to remove unit
                                if (unitId >= 0) {
                                    deck[slotIndex] = -1
                                }
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Inventory title
        Text(
            text = "보유 유닛",
            style = MaterialTheme.typography.titleLarge,
            color = Gold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Unit Inventory Grid
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
                    rarityColor = def.rarity.color,
                    isInDeck = isInDeck,
                    onClick = {
                        if (!isInDeck) {
                            // Find first empty slot
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
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    2.dp,
                    rarityColor ?: MetalGray,
                    RoundedCornerShape(8.dp),
                )
                .background(DarkBrown),
            contentAlignment = Alignment.Center,
        ) {
            if (unitId >= 0 && iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = name,
                    modifier = Modifier.size(40.dp),
                )
            } else {
                Text(
                    text = "+",
                    fontFamily = MedievalFont,
                    fontSize = 20.sp,
                    color = MetalGray,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name ?: "빈 슬롯",
            fontFamily = MedievalFont,
            fontSize = 11.sp,
            color = if (unitId >= 0) Parchment else MetalGray,
            textAlign = TextAlign.Center,
        )
        if (unitId >= 0 && level != null) {
            Text(
                text = "Lv.$level",
                fontSize = 10.sp,
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
    MedievalCard(
        borderColor = rarityColor,
        onClick = if (!isInDeck) onClick else null,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isInDeck) Modifier.alpha(0.4f) else Modifier),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBrown),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = name,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                fontFamily = MedievalFont,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Parchment,
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
                    text = "덱에 배치됨",
                    fontSize = 9.sp,
                    color = MetalGray,
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
