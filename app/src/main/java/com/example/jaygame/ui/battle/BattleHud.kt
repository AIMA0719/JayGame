package com.example.jaygame.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
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

            // Enemy count
            val enemyColor = if (battle.enemyCount > 80) NeonRed else SubText
            Text(
                text = "적 ${battle.enemyCount}/100",
                color = enemyColor,
                fontSize = 13.sp,
                fontWeight = if (battle.enemyCount > 80) FontWeight.Bold else FontWeight.Normal,
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

        // Boss timer
        if (battle.isBossRound) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val bossSeconds = battle.bossTimeRemaining.toInt()
                val bossMin = bossSeconds / 60
                val bossSec = bossSeconds % 60
                Text(
                    text = "BOSS %02d:%02d".format(bossMin, bossSec),
                    color = NeonRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // SP + summon cost row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SP ${battle.sp.toInt()}",
                color = Gold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "소환 비용: ${battle.summonCost}",
                color = if (canSummon) NeonGreen else NeonRed,
                fontSize = 12.sp,
            )
            Text(
                text = "빈 타일을 터치하여 소환",
                color = SubText.copy(alpha = 0.6f),
                fontSize = 10.sp,
            )
        }
    }
}
