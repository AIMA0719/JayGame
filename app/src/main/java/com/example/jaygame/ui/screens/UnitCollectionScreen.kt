package com.example.jaygame.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.UNIT_DEFS
import com.example.jaygame.data.UnitDef
import com.example.jaygame.data.UnitFamily
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark
import com.example.jaygame.ui.theme.SubText

@Composable
fun UnitCollectionScreen(
    onBack: () -> Unit,
) {
    var selectedFamily by remember { mutableStateOf<UnitFamily?>(null) }
    var selectedUnit by remember { mutableStateOf<UnitDef?>(null) }
    val displayedUnits = if (selectedFamily != null) {
        UNIT_DEFS.filter { it.family == selectedFamily }
    } else {
        UNIT_DEFS
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A0A1A), Color(0xFF1A1028))
                    )
                ),
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepDark.copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeonButton(
                    text = "\u2190",
                    onClick = onBack,
                    modifier = Modifier.height(36.dp),
                    fontSize = 14.sp,
                    accentColor = NeonRed,
                    accentColorDark = NeonRedDark,
                )
                Text(
                    text = "\uD83D\uDCD6 영웅 도감",
                    color = LightText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 12.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${UNIT_DEFS.size}종",
                    color = Gold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Family filter tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CodexFilterChip(
                    label = "전체",
                    color = NeonCyan,
                    selected = selectedFamily == null,
                    onClick = { selectedFamily = null },
                    modifier = Modifier.weight(1f),
                )
                UnitFamily.entries.forEach { family ->
                    CodexFilterChip(
                        label = family.label,
                        color = family.color,
                        selected = selectedFamily == family,
                        onClick = { selectedFamily = family },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Unit grid — click to open detail dialog
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(displayedUnits, key = { it.id }) { unit ->
                    CodexUnitCard(
                        unit = unit,
                        onClick = { selectedUnit = unit },
                    )
                }
            }
        }

        // Detail dialog overlay
        selectedUnit?.let { unit ->
            UnitDetailDialog(
                unit = unit,
                onDismiss = { selectedUnit = null },
            )
        }
    }
}

@Composable
private fun CodexFilterChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) color.copy(alpha = 0.25f)
                else Color(0xFF1A1A2E),
            )
            .border(
                width = if (selected) 1.5.dp else 0.5.dp,
                color = if (selected) color else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            color = if (selected) color else Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun CodexUnitCard(
    unit: UnitDef,
    onClick: () -> Unit,
) {
    val gradeColor = unit.grade.color

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1A2E), Color(0xFF12121F)),
                ),
            )
            .border(1.dp, gradeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Grade indicator bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(gradeColor),
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Unit icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .shadow(4.dp, CircleShape, ambientColor = gradeColor, spotColor = gradeColor)
                .clip(CircleShape)
                .background(gradeColor.copy(alpha = 0.15f))
                .border(1.5.dp, gradeColor.copy(alpha = 0.5f), CircleShape),
        ) {
            Image(
                painter = painterResource(id = unit.iconRes),
                contentDescription = unit.name,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Unit name
        Text(
            text = unit.name,
            color = LightText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // Grade label
        Text(
            text = unit.grade.label,
            color = gradeColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun UnitDetailDialog(
    unit: UnitDef,
    onDismiss: () -> Unit,
) {
    val gradeColor = unit.grade.color

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
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
            borderColor = gradeColor.copy(alpha = 0.6f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Grade bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(gradeColor),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Large icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(8.dp, CircleShape, ambientColor = gradeColor, spotColor = gradeColor)
                        .clip(CircleShape)
                        .background(gradeColor.copy(alpha = 0.15f))
                        .border(2.dp, gradeColor.copy(alpha = 0.6f), CircleShape),
                ) {
                    Image(
                        painter = painterResource(id = unit.iconRes),
                        contentDescription = unit.name,
                        modifier = Modifier.size(56.dp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grade label
                Text(
                    text = unit.grade.label,
                    color = gradeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )

                // Unit name
                Text(
                    text = unit.name,
                    color = LightText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                // Family badge
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(unit.family.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = unit.family.label,
                        color = unit.family.color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem(label = "공격력", value = "${unit.baseATK}", color = Color(0xFFFF8A80))
                    StatItem(label = "공속", value = "%.1f".format(unit.baseSpeed), color = Color(0xFF80D8FF))
                    StatItem(label = "사거리", value = "${unit.range.toInt()}", color = Color(0xFFCE93D8))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ability section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp),
                ) {
                    Text(
                        text = "\u2726 ${unit.abilityName}",
                        color = unit.family.color,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = unit.description,
                        color = SubText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }

                // Merge info
                if (unit.mergeResultId >= 0) {
                    val nextUnit = com.example.jaygame.data.UNIT_DEFS_MAP[unit.mergeResultId]
                    if (nextUnit != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Gold.copy(alpha = 0.08f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "\u2728 조합",
                                color = Gold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "x3 \u2192 ${nextUnit.grade.label} ${nextUnit.name}",
                                color = LightText,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                NeonButton(
                    text = "닫기",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    accentColor = SubText,
                    accentColorDark = DimText,
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = LightText,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
