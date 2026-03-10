package com.example.jaygame.ui.battle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.UNIT_DEFS_MAP
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

    val unitDef = UNIT_DEFS_MAP[data.unitDefId] ?: return

    // Semi-transparent backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { BattleBridge.dismissPopup() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Bottom card popup - prevent click-through
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false, onClick = {})
                .background(DarkNavy, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Unit header: icon + name + grade badge
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

            Spacer(modifier = Modifier.height(12.dp))

            // Stats grid: ATK, SPD, Range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem("ATK", "${unitDef.baseATK}", NeonRed)
                StatItem("SPD", "%.1f".format(unitDef.baseSpeed), NeonCyan)
                StatItem("Range", "${unitDef.range.toInt()}", Gold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Ability description
            Text(
                text = "${unitDef.abilityName}: ${unitDef.description}",
                color = SubText,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
            )

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

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = SubText, fontSize = 10.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
