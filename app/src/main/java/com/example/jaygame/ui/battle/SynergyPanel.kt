package com.example.jaygame.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.example.jaygame.data.UnitFamily
import com.example.jaygame.engine.RoleSynergySystem
import com.example.jaygame.engine.SynergySystem
import com.example.jaygame.engine.UnitRole
import com.example.jaygame.ui.components.roleColor

private val GoldBright = Color(0xFFFFD700)

@Composable
fun SynergyPanel(
    familySynergies: Map<UnitFamily, Int>,
    roleSynergies: Map<UnitRole, Int>,
    modifier: Modifier = Modifier,
) {
    val activeRoles: Map<UnitRole, Int> = remember(roleSynergies) { roleSynergies.filter { it.value >= 3 } }
    val activeFamilies: Map<UnitFamily, Int> = remember(familySynergies) { familySynergies.filter { it.value >= 3 } }

    if (activeRoles.isEmpty() && activeFamilies.isEmpty()) return

    var showDetailDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .widthIn(min = 80.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { showDetailDialog = true }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        // Role synergies (3+ only)
        activeRoles.forEach { (role, count) ->
            SynergyChip(
                label = role.label,
                count = count,
                color = roleColor(role),
            )
        }
        // Family synergies (3+ only)
        activeFamilies.forEach { (family, count) ->
            SynergyChip(
                label = family.label,
                count = count,
                color = family.color,
            )
        }
    }

    if (showDetailDialog) {
        SynergyDetailDialog(
            activeFamilies = activeFamilies,
            activeRoles = activeRoles,
            onDismiss = { showDetailDialog = false },
        )
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
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "x$count",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
        )
    }
}

// ── Synergy Detail Dialog ──

@Composable
private fun SynergyDetailDialog(
    activeFamilies: Map<UnitFamily, Int>,
    activeRoles: Map<UnitRole, Int>,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1008))
                .border(2.dp, Color(0xFF8B6040), RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(
                text = "활성 시너지",
                color = GoldBright,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Role synergies
                if (activeRoles.isNotEmpty()) {
                    Text("역할 시너지", color = GoldBright, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    for ((role, count) in activeRoles) {
                        val color = roleColor(role)
                        val desc = describeRoleSynergy(role, count)
                        SynergyDetailRow(label = role.label, count = count, color = color, description = desc)
                    }
                }

                if (activeRoles.isNotEmpty() && activeFamilies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Family synergies
                if (activeFamilies.isNotEmpty()) {
                    Text("가족 시너지", color = GoldBright, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    for ((family, count) in activeFamilies) {
                        val desc = describeFamilySynergy(family, count)
                        SynergyDetailRow(label = family.label, count = count, color = family.color, description = desc)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Close button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF3E2510))
                    .border(1.dp, Color(0xFF8B6040), RoundedCornerShape(10.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("닫기", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SynergyDetailRow(
    label: String,
    count: Int,
    color: Color,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(" x$count", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
            Text(description, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

private fun describeFamilySynergy(family: UnitFamily, count: Int): String {
    val isFull = count >= 4
    return when (family) {
        UnitFamily.FIRE -> if (isFull) "공격력 +8%, DoT 지속시간 +50%" else "공격력 +4%"
        UnitFamily.FROST -> if (isFull) "공격속도 +6%, 둔화 효과 +30%" else "공격속도 +3%"
        UnitFamily.POISON -> if (isFull) "공격력 +7%, 적 사망 시 주변 독 전파" else "공격력 +4%"
        UnitFamily.LIGHTNING -> if (isFull) "공격속도 +8%, 체인 +1 타겟" else "공격속도 +4%"
        UnitFamily.SUPPORT -> if (isFull) "사거리 +6%, 힐/버프 효과 +25%" else "사거리 +3%"
        UnitFamily.WIND -> if (isFull) "공격력 +5%, 사거리 +5%, 넉백 +40%" else "공격력 +3%, 사거리 +2%"
    }
}

private fun describeRoleSynergy(role: UnitRole, count: Int): String {
    return when (role) {
        UnitRole.TANK -> when {
            count >= 4 -> "블록 시간 +20%, 블록 +1, 피격 시 도발"
            count >= 3 -> "블록 시간 +20%, 블록 +1"
            else -> "블록 시간 +20%"
        }
        UnitRole.MELEE_DPS -> when {
            count >= 4 -> "돌진 데미지 +15%, 쿨다운 -20%, 즉시 재돌진"
            count >= 3 -> "돌진 데미지 +15%, 쿨다운 -20%"
            else -> "돌진 데미지 +15%"
        }
        UnitRole.RANGED_DPS -> when {
            count >= 4 -> "사거리 +10%, 크리 +5%, 관통 +2"
            count >= 3 -> "사거리 +10%, 크리 +5%"
            else -> "사거리 +10%"
        }
        UnitRole.SUPPORT -> when {
            count >= 4 -> "버프 범위 +15%, 버프 중첩, 전역 힐"
            count >= 3 -> "버프 범위 +15%, 버프 중첩"
            else -> "버프 범위 +15%"
        }
        UnitRole.CONTROLLER -> when {
            count >= 4 -> "CC 확률 +10%, CC 지속 +25%, 면역 적에게 절반 적용"
            count >= 3 -> "CC 확률 +10%, CC 지속 +25%"
            else -> "CC 확률 +10%"
        }
    }
}
