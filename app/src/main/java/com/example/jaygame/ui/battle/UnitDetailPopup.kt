package com.example.jaygame.ui.battle

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.UnitGrade
import com.example.jaygame.engine.AttackRange
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.DamageType
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.engine.BattleEngine
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.SubText

private val SellAccentDark = NeonRed.copy(alpha = 0.5f)

@Composable
fun UnitDetailPopup() {
    val popupData by BattleBridge.unitPopup.collectAsState()
    val data = popupData ?: return

    val blueprint = remember(data.blueprintId) {
        if (data.blueprintId.isNotEmpty() && BlueprintRegistry.isReady)
            BlueprintRegistry.instance.findById(data.blueprintId) else null
    }
    val displayGrade = UnitGrade.entries.getOrNull(data.grade)

    // 3D card flip animation
    val targetRotation = remember(data.tileIndex) { mutableFloatStateOf(90f) }
    LaunchedEffect(data.tileIndex) { targetRotation.floatValue = 0f }
    val rotation by animateFloatAsState(
        targetValue = targetRotation.floatValue,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "popupFlip",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { BattleBridge.dismissPopup() },
        contentAlignment = Alignment.Center,
    ) {
        GameCard(
            modifier = Modifier
                .width(300.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
            borderColor = Gold.copy(alpha = 0.4f),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // X close
                Text(
                    text = "\u2715",
                    color = SubText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable { BattleBridge.dismissPopup() }
                        .padding(4.dp),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Unit name + grade
                    val unitName = blueprint?.name ?: data.blueprintId.ifEmpty { "???" }
                    Text(
                        text = unitName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row {
                        Text(
                            text = displayGrade?.label ?: "???",
                            color = displayGrade?.color ?: Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (blueprint?.race != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = blueprint.race.label,
                                color = blueprint.race.color,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    // Range + DamageType badges
                    if (data.blueprintId.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (blueprint?.race != null) {
                                BadgeChip(blueprint.race.label, blueprint.race.color.copy(alpha = 0.2f), blueprint.race.color)
                            }
                            BadgeChip(
                                if (data.attackRange == AttackRange.MELEE) "근거리·물리" else "원거리·마법",
                                DarkNavy,
                                if (data.attackRange == AttackRange.MELEE) NeonRed else Color(0xFF7E57C2),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats (ATK + Speed only)
                    if (blueprint != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            StatItem("공격력", "${blueprint.stats.baseATK.toInt()}", NeonRed)
                            StatItem("공속", "%.1f".format(blueprint.stats.baseSpeed), NeonCyan)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Description
                        Text(
                            text = blueprint.description,
                            color = SubText,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Ability
                        if (blueprint.ability != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NeonCyan.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                            ) {
                                Text(
                                    text = blueprint.ability.name,
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = blueprint.ability.description,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }

                        // Unique ability
                        if (blueprint.uniqueAbility != null) {
                            val gradeColor = displayGrade?.color ?: Gold
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(gradeColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                            ) {
                                Text(
                                    text = "\u2726 ${blueprint.uniqueAbility.name}",
                                    color = gradeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (blueprint.uniqueAbility.passive != null) {
                                    Text(
                                        text = "[패시브] ${blueprint.uniqueAbility.passive.description}",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                    )
                                }
                                if (blueprint.uniqueAbility.active != null) {
                                    Text(
                                        text = "[액티브] ${blueprint.uniqueAbility.active.description}",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                    )
                                }
                            }
                        }
                    }

                    val sellPrice = (BattleEngine.SELL_BASE + data.grade * BattleEngine.SELL_PER_GRADE).toInt()
                    Text(
                        text = if (data.stackCount > 1)
                            "슬롯: #${data.tileIndex + 1}  (${data.stackCount}마리)"
                        else
                            "슬롯: #${data.tileIndex + 1}",
                        color = SubText.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (data.stackCount >= 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            NeonButton(
                                text = "1개 판매 (${sellPrice}G)",
                                onClick = {
                                    BattleBridge.requestSell(data.tileIndex)
                                    BattleBridge.dismissPopup()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                fontSize = 11.sp,
                                accentColor = NeonRed,
                                accentColorDark = SellAccentDark,
                            )
                            NeonButton(
                                text = "모두 판매 (${sellPrice * data.stackCount}G)",
                                onClick = {
                                    BattleBridge.requestSellAllSlot(data.tileIndex)
                                    BattleBridge.dismissPopup()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                fontSize = 11.sp,
                                accentColor = NeonRed,
                                accentColorDark = SellAccentDark,
                            )
                        }
                    } else {
                        NeonButton(
                            text = "판매 (${sellPrice}G)",
                            onClick = {
                                BattleBridge.requestSell(data.tileIndex)
                                BattleBridge.dismissPopup()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            fontSize = 13.sp,
                            accentColor = NeonRed,
                            accentColorDark = SellAccentDark,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(text: String, bgColor: Color, textColor: Color = Color.White) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = SubText, fontSize = 10.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
