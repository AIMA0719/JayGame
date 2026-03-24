package com.example.jaygame.ui.battle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.UnitBlueprint
import com.example.jaygame.engine.UnitGrade
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.screens.blueprintIconRes
import com.example.jaygame.ui.theme.*

private const val MYTHIC_BUY_PRICE = 800

private fun getBuyableBlueprints(): List<UnitBlueprint> {
    if (!BlueprintRegistry.isReady) return emptyList()
    return BlueprintRegistry.instance.all().filter { it.grade == UnitGrade.MYTHIC }
}

@Composable
fun BuyUnitSheet(
    onDismiss: () -> Unit,
) {
    val battle by BattleBridge.state.collectAsState()
    val gridState by BattleBridge.gridState.collectAsState()
    val gridFull = gridState.all { it.unitDefId >= 0 || it.blueprintId.isNotEmpty() }

    var confirmUnit by remember { mutableStateOf<UnitBlueprint?>(null) }
    val buyableUnits = remember { getBuyableBlueprints() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Black.copy(alpha = 0.85f),
                    ),
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        if (confirmUnit != null) {
            // ── Purchase confirmation dialog ──
            val bp = confirmUnit ?: return@Box
            val price = MYTHIC_BUY_PRICE
            val canAfford = battle.sp >= price && !gridFull
            val displayIcon = blueprintIconRes(bp)

            GameCard(
                modifier = Modifier
                    .width(280.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {},
                borderColor = bp.grade.color.copy(alpha = 0.5f),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "구매 확인",
                        color = Gold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        painter = painterResource(id = displayIcon),
                        contentDescription = bp.name,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bp.name,
                        color = bp.grade.color,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = bp.grade.label,
                        color = bp.grade.color.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${price} SP",
                        color = Gold,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    if (gridFull) {
                        Text(
                            text = "그리드가 가득 찼습니다!",
                            color = NeonRed,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        NeonButton(
                            text = "취소",
                            onClick = { confirmUnit = null },
                            modifier = Modifier.weight(1f).height(40.dp),
                            accentColor = SubText,
                            accentColorDark = DimText,
                        )
                        NeonButton(
                            text = "구매",
                            onClick = {
                                if (canAfford) {
                                    BattleBridge.requestBuyBlueprint(bp.id, price)
                                    confirmUnit = null
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            accentColor = NeonGreen,
                            accentColorDark = Color(0xFF1B5E20),
                            enabled = canAfford,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            // ── Unit shop grid ──
            GameCard(
                modifier = Modifier
                    .width(320.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {},
                borderColor = Color(0xFFFFAB40).copy(alpha = 0.5f),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "\uD83D\uDED2 유닛 구매",
                            color = Gold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "\u2715",
                            color = SubText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onDismiss() }
                                .padding(4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "신화 이상 유닛을 SP로 구매",
                        color = SubText,
                        fontSize = 11.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Unit grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(buyableUnits) { bp ->
                            BuyBlueprintCard(
                                bp = bp,
                                price = MYTHIC_BUY_PRICE,
                                canAfford = battle.sp >= MYTHIC_BUY_PRICE,
                                onClick = { confirmUnit = bp },
                            )
                        }
                        if (buyableUnits.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    "구매 가능한 유닛이 없습니다.",
                                    color = Color.Gray,
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BuyBlueprintCard(
    bp: UnitBlueprint,
    price: Int,
    canAfford: Boolean,
    onClick: () -> Unit,
) {
    val displayIcon = remember(bp.id) { blueprintIconRes(bp) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        bp.grade.color.copy(alpha = 0.15f),
                        Color(0xFF1A1025).copy(alpha = 0.8f),
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(id = displayIcon),
                contentDescription = bp.name,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bp.name,
                color = bp.grade.color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = bp.grade.label,
                color = bp.grade.color.copy(alpha = 0.7f),
                fontSize = 9.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$price SP",
                color = if (canAfford) Gold else NeonRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
