package com.example.jaygame.ui.battle

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.data.UnitGrade
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark
import com.example.jaygame.ui.theme.SubText
import com.example.jaygame.ui.theme.DimText

private val SELLABLE_GRADES = listOf(
    UnitGrade.COMMON,
    UnitGrade.RARE,
    UnitGrade.HERO,
    UnitGrade.LEGEND,
)

@Composable
fun BulkSellDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.8f),
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
                .width(280.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
            borderColor = NeonRed.copy(alpha = 0.5f),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "\uD83D\uDCB0 일괄 판매",
                        color = NeonRed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    Text(
                        text = "\u2715",
                        color = SubText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable { onDismiss() }
                            .padding(4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "판매할 등급을 선택하세요",
                    color = LightText,
                    fontSize = 13.sp,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SELLABLE_GRADES.forEach { grade ->
                        NeonButton(
                            text = "${grade.label} 전체 판매",
                            onClick = {
                                val sold = BattleBridge.requestBulkSell(grade.ordinal)
                                if (sold > 0) {
                                    Toast.makeText(
                                        context,
                                        "${grade.label} ${sold}개 판매 완료",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "판매할 ${grade.label} 유닛이 없습니다",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            accentColor = grade.color,
                            accentColorDark = grade.color.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                NeonButton(
                    text = "닫기",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    accentColor = SubText,
                    accentColorDark = DimText,
                    fontSize = 13.sp,
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
