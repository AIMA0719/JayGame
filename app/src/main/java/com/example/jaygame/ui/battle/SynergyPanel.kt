package com.example.jaygame.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.UnitFamily
import com.example.jaygame.engine.UnitRole
import com.example.jaygame.ui.components.roleColor

@Composable
fun SynergyPanel(
    familySynergies: Map<UnitFamily, Int>,
    roleSynergies: Map<UnitRole, Int>,
    modifier: Modifier = Modifier,
) {
    val activeRoles: Map<UnitRole, Int> = remember(roleSynergies) { roleSynergies.filter { it.value >= 2 } }
    val activeFamilies: Map<UnitFamily, Int> = remember(familySynergies) { familySynergies.filter { it.value >= 2 } }

    if (activeRoles.isEmpty() && activeFamilies.isEmpty()) return

    Column(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        // Role synergies (2+ only)
        activeRoles.forEach { (role, count) ->
            SynergyChip(
                label = role.label,
                count = count,
                color = roleColor(role),
            )
        }
        // Family synergies (2+ only)
        activeFamilies.forEach { (family, count) ->
            SynergyChip(
                label = family.label,
                count = count,
                color = family.color,
            )
        }
    }
}

@Composable
private fun SynergyChip(
    label: String,
    count: Int,
    color: Color,
) {
    Row(
        modifier = Modifier.padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "x$count",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 9.sp,
        )
    }
}
