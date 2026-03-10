package com.example.jaygame.ui.battle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.UNIT_DEFS_MAP
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.components.NeonProgressBar
import com.example.jaygame.ui.theme.*

@Composable
fun BattleTopHud() {
    val battle by BattleBridge.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkNavy.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Wave row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Wave ${battle.currentWave}/${battle.maxWaves}",
                color = NeonCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            NeonProgressBar(
                progress = if (battle.maxWaves > 0) battle.currentWave.toFloat() / battle.maxWaves else 0f,
                modifier = Modifier.weight(1f),
                height = 6.dp,
                barColor = NeonCyan,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // HP / SP / Timer row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // HP
            val hpColor = if (battle.playerHP <= battle.maxHP / 4) NeonRed else NeonGreen
            Text(
                text = "HP ${battle.playerHP}/${battle.maxHP}",
                color = hpColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )

            // SP
            Text(
                text = "SP ${battle.sp.toInt()}",
                color = Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )

            // Timer
            val totalSeconds = battle.elapsedTime.toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            Text(
                text = "%02d:%02d".format(minutes, seconds),
                color = SubText,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
fun BattleBottomHud() {
    val battle by BattleBridge.state.collectAsState()
    val canSummon = battle.sp >= battle.summonCost

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkNavy.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Deck units row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            battle.deckUnits.forEach { unitId ->
                val unitDef = UNIT_DEFS_MAP[unitId]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (unitDef != null) {
                        Image(
                            painter = painterResource(id = unitDef.iconRes),
                            contentDescription = unitDef.name,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            text = unitDef.name,
                            color = SubText,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Summon button
        NeonButton(
            text = "소환 ${battle.summonCost} SP",
            onClick = { BattleBridge.requestSummon() },
            enabled = canSummon,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            fontSize = 16.sp,
            accentColor = Gold,
            accentColorDark = DarkGold,
        )
    }
}
