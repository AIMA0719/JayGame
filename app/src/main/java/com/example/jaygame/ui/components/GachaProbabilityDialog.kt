package com.example.jaygame.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jaygame.engine.UnitGrade
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.SubText

private data class GradeRow(
    val grade: UnitGrade,
    val probabilityText: String,
)

private val gradeRows = listOf(
    GradeRow(UnitGrade.LOW, "80%"),
    GradeRow(UnitGrade.MEDIUM, "15%"),
    GradeRow(UnitGrade.HIGH, "5%"),
    GradeRow(UnitGrade.SUPREME, "조합으로만 획득"),
    GradeRow(UnitGrade.TRANSCENDENT, "조합으로만 획득"),
)

@Composable
fun GachaProbabilityDialog(
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DeepDark,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Text(
                    text = "소환 확률",
                    color = LightText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                GameCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DarkNavy,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        gradeRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = row.grade.label,
                                    color = row.grade.color,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = row.probabilityText,
                                    color = SubText,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                NeonButton(
                    text = "닫기",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 13.sp,
                )
            }
        }
    }
}
