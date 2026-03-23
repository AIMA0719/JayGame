package com.example.jaygame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.UnitFamily
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.UnitRole
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.SubText

// ── Role icon labels ──
val ROLE_LABELS: Map<UnitRole, String> = mapOf(
    UnitRole.TANK to "🛡탱커",
    UnitRole.MELEE_DPS to "⚔근딜",
    UnitRole.RANGED_DPS to "🏹원딜",
    UnitRole.SUPPORT to "✚서포터",
    UnitRole.CONTROLLER to "⛓컨트롤러",
)

// ── Behavior pattern labels ──
val BEHAVIOR_LABELS: Map<String, String> = mapOf(
    "tank_blocker" to "전방 저지형",
    "assassin_dash" to "돌진 암살형",
    "ranged_mage" to "원거리 마법형",
    "support_aura" to "오라 지원형",
    "controller_cc_ranged" to "원거리 제어형",
)

// ── Family emoji icons ──
val FAMILY_ICONS: Map<UnitFamily, String> = mapOf(
    UnitFamily.FIRE to "\uD83D\uDD25",
    UnitFamily.FROST to "\u2744\uFE0F",
    UnitFamily.POISON to "\uD83D\uDCA8",
    UnitFamily.LIGHTNING to "\u26A1",
    UnitFamily.SUPPORT to "\uD83D\uDE4F",
    UnitFamily.WIND to "\uD83C\uDF00",
)

// ── Role color (consistent across all screens) ──
fun roleColor(role: UnitRole): Color = when (role) {
    UnitRole.TANK -> Color(0xFF607D8B)
    UnitRole.MELEE_DPS -> Color(0xFFE53935)
    UnitRole.RANGED_DPS -> Color(0xFF43A047)
    UnitRole.SUPPORT -> Color(0xFFFFB300)
    UnitRole.CONTROLLER -> Color(0xFF7E57C2)
}

// ── Filter chip colors (pre-allocated) ──
val ChipSelectedBg = Color(0xFF2A2A4E)
val ChipUnselectedBg = Color(0xFF1A1A2E)
val ChipSelectedBorder = Color(0xFFFFD700)
val ChipUnselectedBorder = Color(0xFF333355)

// ── Grade background colors (pre-allocated) ──
val GradeBgCommon = Color(0xFF424242)
val GradeBgRare = Color(0xFF1A237E)
val GradeBgHero = Color(0xFF4A148C)

// ── Sort mode ──
enum class SortMode(val label: String) {
    GRADE("등급순"),
    ATK("공격력순"),
    NAME("이름순"),
}

// ── Shared filter chip composable ──
@Composable
fun GameFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) ChipSelectedBg else ChipUnselectedBg)
            .border(
                width = 1.dp,
                color = if (selected) ChipSelectedBorder else ChipUnselectedBorder,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            color = if (selected) Gold else SubText,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

// ── Shared stat row composable ──
@Composable
fun UnitStatRow(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = SubText,
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

// ── Blueprint name helper ──
fun blueprintDisplayName(blueprintId: String): String? =
    BlueprintRegistry.instance.findById(blueprintId)?.name
