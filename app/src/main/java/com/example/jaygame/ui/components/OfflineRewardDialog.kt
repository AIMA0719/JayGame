package com.example.jaygame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jaygame.R
import com.example.jaygame.engine.OfflineRewardManager
import com.example.jaygame.ui.theme.DiamondBlue
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.GoldCoin
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonGreen
import com.example.jaygame.ui.theme.SubText

private val DialogBg = Color(0xFF16213E)
private val DialogBorder = Color(0xFF2A4A7A)

@Composable
fun OfflineRewardDialog(
    reward: OfflineRewardManager.OfflineReward,
    onDismiss: () -> Unit,
) {
    val hours = reward.offlineMinutes / 60
    val minutes = reward.offlineMinutes % 60
    val timeText = if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(DialogBg)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "오프라인 보상",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${timeText} 동안 자리를 비웠습니다",
                fontSize = 13.sp,
                color = SubText,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Rewards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Gold
                Icon(
                    painter = painterResource(id = R.drawable.ic_gold),
                    contentDescription = null,
                    tint = GoldCoin,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "+${reward.goldReward}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldCoin,
                )

                Spacer(Modifier.width(24.dp))

                // Season XP
                Text(
                    text = "시즌 XP",
                    fontSize = 12.sp,
                    color = SubText,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "+${reward.seasonXpReward}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            NeonButton(
                text = "수령",
                onClick = onDismiss,
                fontSize = 14.sp,
                accentColor = Gold,
                accentColorDark = Gold.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
