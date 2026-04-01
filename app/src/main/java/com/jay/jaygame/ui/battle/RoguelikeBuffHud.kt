package com.jay.jaygame.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.jaygame.engine.ActiveRoguelikeBuff
import com.jay.jaygame.engine.RoguelikeBuffGrade

private val IconTextColor = Color.White
private val TooltipBg = Color(0xE61A1A2E)
private val TooltipBorder = Color(0xFF555577)

private fun gradeColor(grade: RoguelikeBuffGrade): Color = Color(grade.colorHex)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoguelikeBuffHud(
    activeBuffs: List<ActiveRoguelikeBuff>,
    modifier: Modifier = Modifier,
) {
    var expandedBuffId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.widthIn(max = 140.dp),
        ) {
            activeBuffs.forEach { activeBuff ->
                val bgColor = gradeColor(activeBuff.buff.grade)
                val initial = activeBuff.buff.name.first().toString()
                val isExpanded = expandedBuffId == activeBuff.buff.id

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(bgColor.copy(alpha = if (isExpanded) 1f else 0.8f))
                        .then(
                            if (isExpanded) Modifier.border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                            else Modifier
                        )
                        .clickable {
                            expandedBuffId = if (isExpanded) null else activeBuff.buff.id
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (activeBuff.stacks > 1) {
                        Text(
                            text = "${activeBuff.stacks}",
                            color = IconTextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            text = initial,
                            color = IconTextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Tooltip for selected buff
        val expandedBuff = activeBuffs.find { it.buff.id == expandedBuffId }
        if (expandedBuff != null) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TooltipBg)
                    .border(1.dp, TooltipBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .widthIn(max = 180.dp)
                    .clickable { expandedBuffId = null },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = buildString {
                            append(expandedBuff.buff.name)
                            if (expandedBuff.stacks > 1) append(" x${expandedBuff.stacks}")
                        },
                        color = gradeColor(expandedBuff.buff.grade),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = expandedBuff.buff.description,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}
