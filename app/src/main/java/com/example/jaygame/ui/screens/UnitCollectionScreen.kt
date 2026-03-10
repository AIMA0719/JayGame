package com.example.jaygame.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark

@Composable
fun UnitCollectionScreen(
    onBack: () -> Unit,
) {
    var selectedFamily by remember { mutableStateOf<UnitFamily?>(null) }
    val displayedUnits = if (selectedFamily != null) {
        UNIT_DEFS.filter { it.family == selectedFamily }
    } else {
        UNIT_DEFS
    }

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
                text = "\uD83D\uDCD6 \uC720\uB2DB \uB3C4\uAC10",
                color = LightText,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 12.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${UNIT_DEFS.size}\uC885",
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
                label = "\uC804\uCCB4",
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

        // Unit grid
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
                CodexUnitCard(unit = unit)
            }
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
private fun CodexUnitCard(unit: UnitDef) {
    val gradeColor = unit.grade.color

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF12121F),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = gradeColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Grade indicator bar at top
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

        Spacer(modifier = Modifier.height(2.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ATK",
                color = Color(0xFFFF8A80),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "${unit.baseATK}",
                color = LightText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SPD",
                color = Color(0xFF80D8FF),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "%.1f".format(unit.baseSpeed),
                color = LightText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Ability badge
        Box(
            modifier = Modifier
                .background(
                    unit.family.color.copy(alpha = 0.15f),
                    RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 6.dp, vertical = 1.dp),
        ) {
            Text(
                text = unit.abilityName,
                color = unit.family.color,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
