package com.example.jaygame.ui.battle

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.engine.UnitUpgradeSystem
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.*

private val groupColors = arrayOf(
    Color(0xFF42A5F5),  // 일반/희귀
    Color(0xFFFF9800),  // 영웅/전설
    Color(0xFFE040FB),  // 신화
)
private val groupLabels = arrayOf("일반/희귀", "영웅/전설", "신화")
private val groupIcons = arrayOf("⚔️", "🛡️", "👑")

@Composable
fun UpgradeSheet(
    onDismiss: () -> Unit,
) {
    val battle by BattleBridge.state.collectAsState()
    val groupLevels by BattleBridge.groupUpgradeLevels.collectAsState()

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
        GameCard(
            modifier = Modifier
                .width(320.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
            borderColor = Gold.copy(alpha = 0.5f),
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
                        text = "⬆ 통합 강화",
                        color = Gold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "✕",
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
                    text = "같은 등급 그룹의 모든 유닛을 한번에 강화",
                    color = SubText,
                    fontSize = 11.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 3 group upgrade rows
                for (group in 0 until UnitUpgradeSystem.GROUP_COUNT) {
                    val currentLevel = groupLevels.getOrElse(group) { 0 }
                    val isMaxed = currentLevel >= UnitUpgradeSystem.MAX_UPGRADE_LEVEL
                    val nextCost = if (!isMaxed) UnitUpgradeSystem.getGroupUpgradeCost(group, currentLevel) else 0
                    val canAfford = !isMaxed && battle.sp >= nextCost

                    GroupUpgradeRow(
                        group = group,
                        currentLevel = currentLevel,
                        nextCost = nextCost,
                        isMaxed = isMaxed,
                        canAfford = canAfford,
                        onClick = {
                            if (canAfford) {
                                BattleBridge.requestGroupUpgrade(group)
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GroupUpgradeRow(
    group: Int,
    currentLevel: Int,
    nextCost: Int,
    isMaxed: Boolean,
    canAfford: Boolean,
    onClick: () -> Unit,
) {
    val color = groupColors[group]
    val atkPercent = (UnitUpgradeSystem.getTotalAtkBonus(currentLevel) * 100).toInt()
    val spdPercent = (UnitUpgradeSystem.getTotalSpdBonus(currentLevel) * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        color.copy(alpha = 0.15f),
                        Color(0xFF1A1025).copy(alpha = 0.6f),
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // Top row: icon + label + level
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = groupIcons[group], fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = groupLabels[group],
                        color = color,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lv.$currentLevel/${UnitUpgradeSystem.MAX_UPGRADE_LEVEL}",
                        color = SubText,
                        fontSize = 11.sp,
                    )
                }
                // Current bonus stats
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ATK +${atkPercent}%",
                        color = NeonRed.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                    )
                    if (spdPercent > 0) {
                        Text(
                            text = "속도 +${spdPercent}%",
                            color = NeonCyan.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom row: milestone hint + buy button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isMaxed) {
                Text(
                    text = "다음: ${UnitUpgradeSystem.nextMilestoneHint(currentLevel)}",
                    color = SubText.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (isMaxed) {
                Text(
                    text = "MAX",
                    color = Gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            } else {
                NeonButton(
                    text = "🪙 $nextCost",
                    onClick = onClick,
                    modifier = Modifier
                        .width(80.dp)
                        .height(34.dp),
                    fontSize = 12.sp,
                    accentColor = if (canAfford) color else SubText,
                    accentColorDark = if (canAfford) color.copy(alpha = 0.5f) else DimText,
                    enabled = canAfford,
                )
            }
        }
    }
}
