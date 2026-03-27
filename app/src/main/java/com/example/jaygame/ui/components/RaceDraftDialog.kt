package com.example.jaygame.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.jaygame.R
import com.example.jaygame.data.UnitRace

private val DarkBg = Color(0xFF1A1A2E)
private val CardBg = Color(0xFF16213E)
/** 배틀 시작 전 선택할 종족 수 — 이 값만 바꾸면 전체 반영 */
private const val MAX_DRAFT_RACES = 2

private val AccentGold = Color(0xFFFFD700)
private val SelectedBorder = Color(0xFFFFD700)
private val UnselectedBorder = Color(0xFF444466)
private val ConfirmEnabled = Color(0xFF4CAF50)
private val ConfirmDisabled = Color(0xFF555555)

private fun raceIconRes(race: UnitRace): Int = when (race) {
    UnitRace.HUMAN -> R.drawable.ic_race_human
    UnitRace.SPIRIT -> R.drawable.ic_race_spirit
    UnitRace.ANIMAL -> R.drawable.ic_race_animal
    UnitRace.ROBOT -> R.drawable.ic_race_robot
    UnitRace.DEMON -> R.drawable.ic_race_demon
}

@Composable
fun RaceDraftDialog(
    onConfirm: (Set<UnitRace>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(emptySet<UnitRace>()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkBg)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "종족 선택",
                color = AccentGold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "전투에 사용할 종족 ${MAX_DRAFT_RACES}개를 선택하세요",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))

            UnitRace.entries.forEach { race ->
                val isSelected = race in selected
                val borderColor = if (isSelected) SelectedBorder else UnselectedBorder
                val bgColor = if (isSelected) race.color.copy(alpha = 0.2f) else CardBg

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable {
                            selected = if (isSelected) {
                                selected - race
                            } else if (selected.size < MAX_DRAFT_RACES) {
                                selected + race
                            } else {
                                selected
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Race icon
                    Image(
                        painter = painterResource(raceIconRes(race)),
                        contentDescription = race.label,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = race.label,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                    Spacer(Modifier.weight(1f))
                    if (isSelected) {
                        Text(
                            text = "\u2713",
                            color = AccentGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Selected count
            Text(
                text = "${selected.size} / $MAX_DRAFT_RACES",
                color = if (selected.size == MAX_DRAFT_RACES) AccentGold else Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
            )

            Spacer(Modifier.height(12.dp))

            // Confirm button
            val canConfirm = selected.size == MAX_DRAFT_RACES
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (canConfirm) Brush.horizontalGradient(
                            listOf(ConfirmEnabled, ConfirmEnabled.copy(alpha = 0.8f))
                        ) else Brush.horizontalGradient(
                            listOf(ConfirmDisabled, ConfirmDisabled)
                        )
                    )
                    .then(
                        if (canConfirm) Modifier.clickable { onConfirm(selected) }
                        else Modifier
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "출전!",
                    color = if (canConfirm) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
