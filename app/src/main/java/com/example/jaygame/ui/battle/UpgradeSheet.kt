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
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.*

/** Upgrade types for in-battle buffs */
enum class BattleUpgradeType(
    val label: String,
    val icon: String,
    val description: String,
    val effectPerLevel: String,
    val maxLevel: Int,
    val costs: List<Int>,
    val color: Color,
) {
    ATK(
        label = "공격력",
        icon = "\u2694\uFE0F",
        description = "전체 유닛 공격력 증가",
        effectPerLevel = "+10%",
        maxLevel = 5,
        costs = listOf(50, 100, 180, 280, 400),
        color = Color(0xFFFF6B35),
    ),
    ATTACK_SPEED(
        label = "공격속도",
        icon = "\u26A1",
        description = "전체 유닛 공격속도 증가",
        effectPerLevel = "+8%",
        maxLevel = 5,
        costs = listOf(60, 120, 200, 300, 450),
        color = Color(0xFFFFD54F),
    ),
    CRIT_RATE(
        label = "치명타",
        icon = "\uD83D\uDCA5",
        description = "전체 유닛 치명타 확률 증가",
        effectPerLevel = "+5%",
        maxLevel = 5,
        costs = listOf(80, 150, 250, 380, 550),
        color = Color(0xFFEF4444),
    ),
    RANGE(
        label = "사거리",
        icon = "\uD83C\uDFAF",
        description = "전체 유닛 공격 사거리 증가",
        effectPerLevel = "+10%",
        maxLevel = 3,
        costs = listOf(100, 200, 350),
        color = Color(0xFF42A5F5),
    ),
    SP_REGEN(
        label = "SP 회복",
        icon = "\uD83D\uDC8E",
        description = "초당 SP 회복량 증가",
        effectPerLevel = "+0.7/초",
        maxLevel = 4,
        costs = listOf(120, 250, 420, 650),
        color = Color(0xFFCE93D8),
    ),
}

@Composable
fun UpgradeSheet(
    onDismiss: () -> Unit,
) {
    val battle by BattleBridge.state.collectAsState()
    val upgradeLevels by BattleBridge.battleUpgradeLevels.collectAsState()

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
            borderColor = Color(0xFF42A5F5).copy(alpha = 0.5f),
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
                        text = "\u2B06 전투 강화",
                        color = NeonCyan,
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
                    text = "배틀 내내 적용되는 영구 버프",
                    color = SubText,
                    fontSize = 11.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Upgrade items
                BattleUpgradeType.entries.forEach { type ->
                    val currentLevel = upgradeLevels.getOrElse(type.ordinal) { 0 }
                    val isMaxed = currentLevel >= type.maxLevel
                    val nextCost = if (!isMaxed) type.costs[currentLevel] else 0
                    val canAfford = !isMaxed && battle.sp >= nextCost

                    UpgradeRow(
                        type = type,
                        currentLevel = currentLevel,
                        nextCost = nextCost,
                        isMaxed = isMaxed,
                        canAfford = canAfford,
                        onClick = {
                            if (canAfford) {
                                BattleBridge.requestBattleUpgrade(type.ordinal, nextCost)
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun UpgradeRow(
    type: BattleUpgradeType,
    currentLevel: Int,
    nextCost: Int,
    isMaxed: Boolean,
    canAfford: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        type.color.copy(alpha = 0.1f),
                        Color(0xFF1A1025).copy(alpha = 0.5f),
                    )
                )
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon + info
        Text(text = type.icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = type.label,
                    color = type.color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Level dots
                Text(
                    text = "Lv.$currentLevel/${type.maxLevel}",
                    color = SubText,
                    fontSize = 10.sp,
                )
            }
            Text(
                text = "${type.description} (${type.effectPerLevel}/Lv)",
                color = SubText,
                fontSize = 9.sp,
            )
        }

        // Buy button
        if (isMaxed) {
            Text(
                text = "MAX",
                color = Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        } else {
            NeonButton(
                text = "$nextCost SP",
                onClick = onClick,
                modifier = Modifier
                    .width(72.dp)
                    .height(32.dp),
                fontSize = 11.sp,
                accentColor = if (canAfford) type.color else SubText,
                accentColorDark = if (canAfford) type.color.copy(alpha = 0.5f) else DimText,
                enabled = canAfford,
            )
        }
    }
}

