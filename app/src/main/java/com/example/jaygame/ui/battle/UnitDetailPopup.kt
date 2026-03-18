package com.example.jaygame.ui.battle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.UNIT_DEFS_MAP
import com.example.jaygame.data.UnitGrade
import com.example.jaygame.engine.AttackRange
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.DamageType
import com.example.jaygame.engine.UnitRole
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.DarkGold
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.SubText

/**
 * Overlay popup shown when a unit tile is clicked.
 * Shows: unit icon, name, grade, family, stats (ATK, SPD, Range, Ability).
 * Buttons: [합성] (if canMerge, with "5% 잭팟!" tooltip), [판매] (refund SP).
 * Dismiss on backdrop click.
 */
@Composable
fun UnitDetailPopup() {
    val popupData by BattleBridge.unitPopup.collectAsState()
    val data = popupData ?: return

    val unitDef = UNIT_DEFS_MAP[data.unitDefId]
    if (unitDef == null && data.blueprintId.isEmpty()) return

    // G4: 3D card rotation animation (90 -> 0 degrees)
    val targetRotation = remember(data.tileIndex, data.unitDefId) { mutableFloatStateOf(90f) }
    LaunchedEffect(data.tileIndex, data.unitDefId) {
        targetRotation.floatValue = 0f
    }
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation.floatValue,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "cardRotationY",
    )

    // Semi-transparent backdrop — dismiss on tap
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
        // Center dialog card — prevent click-through
        GameCard(
            modifier = Modifier
                .width(300.dp)
                .graphicsLayer {
                    rotationY = animatedRotation
                    cameraDistance = 12f * density
                }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
            borderColor = Gold.copy(alpha = 0.4f),
        ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // X close button (top-right)
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
            // Unit header: icon + name + grade badge
            val displayGrade = UnitGrade.entries.getOrNull(data.grade)
            if (unitDef != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = unitDef.iconRes),
                        contentDescription = unitDef.name,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = unitDef.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Row {
                            Text(
                                text = unitDef.grade.label,
                                color = unitDef.grade.color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = unitDef.family.label,
                                color = unitDef.family.color,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            } else {
                // Blueprint unit — look up name from BlueprintRegistry
                val blueprint = remember(data.blueprintId) {
                    BlueprintRegistry.instance.findById(data.blueprintId)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = blueprint?.name ?: data.blueprintId,
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
                        if (data.families.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = data.families.joinToString("/") { it.label },
                                color = data.families.first().color,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }

            // Role / AttackRange / DamageType badges (Task 18: activated)
            if (data.blueprintId.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Role badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = getRoleColor(data.role)
                    ) {
                        Text(
                            text = data.role.label,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    // Attack Range badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = DarkNavy,
                    ) {
                        Text(
                            text = data.attackRange.label,
                            color = NeonCyan,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    // Damage Type badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = DarkNavy,
                    ) {
                        Text(
                            text = if (data.damageType == DamageType.PHYSICAL) "물리" else "마법",
                            color = if (data.damageType == DamageType.PHYSICAL) NeonRed else Color(0xFF7E57C2),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            // HP bar for melee/tank units (Task 18: activated)
            if (data.attackRange == AttackRange.MELEE && data.maxHp > 0f) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (data.hp / data.maxHp).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFF333333),
                )
                Text(
                    text = "HP: ${data.hp.toInt()} / ${data.maxHp.toInt()}",
                    color = SubText,
                    fontSize = 10.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats grid
            if (unitDef != null) {
                // Legacy unit stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem("공격력", "${unitDef.baseATK}", NeonRed)
                    StatItem("공속", "%.1f".format(unitDef.baseSpeed), NeonCyan)
                    StatItem("사거리", "${unitDef.range.toInt()}", Gold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${unitDef.abilityName}: ${unitDef.description}",
                    color = SubText,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (unitDef.uniqueAbility != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                unitDef.grade.color.copy(alpha = 0.1f),
                                RoundedCornerShape(6.dp),
                            )
                            .padding(8.dp),
                    ) {
                        Text(
                            text = "\u2726 ${unitDef.uniqueAbility.name}",
                            color = unitDef.grade.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (unitDef.uniqueAbility.cooldown > 0) {
                            Text(
                                text = "쿨타임: ${unitDef.uniqueAbility.cooldown}초",
                                color = NeonCyan,
                                fontSize = 9.sp,
                            )
                        }
                        Text(
                            text = unitDef.uniqueAbility.description,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            } else if (data.blueprintId.isNotEmpty()) {
                // Blueprint unit stats
                val bp = remember(data.blueprintId) {
                    BlueprintRegistry.instance.findById(data.blueprintId)
                }
                if (bp != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatItem("공격력", "${bp.stats.baseATK.toInt()}", NeonRed)
                        StatItem("공속", "%.1f".format(bp.stats.baseSpeed), NeonCyan)
                        StatItem("사거리", "${bp.stats.range.toInt()}", Gold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatItem("체력", "${bp.stats.hp.toInt()}", Color(0xFF4CAF50))
                        StatItem("방어력", "${bp.stats.defense.toInt()}", Color(0xFF90A4AE))
                        StatItem("마법저항", "${bp.stats.magicResist.toInt()}", Color(0xFF7E57C2))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = bp.description,
                        color = SubText,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Slot info
            val slotRow = data.tileIndex / BattleBridge.GRID_COLS + 1
            val slotCol = data.tileIndex % BattleBridge.GRID_COLS + 1
            Text(
                text = "\uC2AC\uB86F: ${slotRow}-${slotCol}",  // 슬롯:
                color = SubText.copy(alpha = 0.7f),
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Merge button
                if (data.canMerge) {
                    NeonButton(
                        text = "\uD569\uC131 (5% \uC7AD\uD31F!)",  // 합성 (5% 잭팟!)
                        onClick = { BattleBridge.requestMerge(data.tileIndex) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        fontSize = 14.sp,
                        accentColor = Gold,
                        accentColorDark = DarkGold,
                    )
                }

                // Sell button
                NeonButton(
                    text = "\uD310\uB9E4",  // 판매
                    onClick = { BattleBridge.requestSell(data.tileIndex) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    fontSize = 14.sp,
                    accentColor = NeonRed,
                    accentColorDark = NeonRed.copy(alpha = 0.5f),
                )
            }
        }
        }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = SubText, fontSize = 10.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** Role-specific badge color. Used once bridge exposes role data (Task 18). */
private fun getRoleColor(role: UnitRole): Color = when (role) {
    UnitRole.TANK -> Color(0xFF607D8B)       // Blue Grey
    UnitRole.MELEE_DPS -> Color(0xFFE53935)  // Red
    UnitRole.RANGED_DPS -> Color(0xFF43A047) // Green
    UnitRole.SUPPORT -> Color(0xFFFFB300)    // Amber
    UnitRole.CONTROLLER -> Color(0xFF7E57C2) // Purple
}
