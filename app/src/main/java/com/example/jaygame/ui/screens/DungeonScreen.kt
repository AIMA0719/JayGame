package com.example.jaygame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.ALL_DUNGEONS
import com.example.jaygame.data.DUNGEON_DAILY_LIMIT
import com.example.jaygame.data.DungeonDef
import com.example.jaygame.data.DungeonType
import com.example.jaygame.data.GameData
import com.example.jaygame.data.GameRepository
import com.example.jaygame.engine.DungeonManager
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.NeonGreen
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark
import com.example.jaygame.ui.theme.StaminaGreen
import com.example.jaygame.ui.theme.SubText

// ── Pre-allocated colors ──
private val BgScreen = Color(0xFF0D0D1F)
private val BgCard = Color(0xFF16213E)
private val BgCardLocked = Color(0xFF111122)
private val BorderUnlocked = Color(0xFF2A4A7A)
private val BorderLocked = Color(0xFF2A2A3A)
private val TextPrimary = Color(0xFFE0E0E0)

private fun dungeonTypeIcon(type: DungeonType): String = when (type) {
    DungeonType.GOLD_RUSH -> "\uD83D\uDCB0"
    DungeonType.RELIC_HUNT -> "\u2728"
    DungeonType.PET_EXPEDITION -> "\uD83D\uDC3E"
    DungeonType.BOSS_RUSH -> "\uD83D\uDC7A"
    DungeonType.SURVIVAL -> "\u2694\uFE0F"
}

@Composable
fun DungeonScreen(
    repository: GameRepository,
    onBack: () -> Unit,
) {
    val data by repository.gameData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgScreen),
    ) {
        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0A18))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeonButton(
                text = "< 뒤로",
                onClick = onBack,
                fontSize = 13.sp,
                accentColor = SubText,
                accentColorDark = DimText,
            )
            Text(
                text = "던전",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.width(72.dp))
        }

        // ── Daily attempts info ──
        val manager = DungeonManager(data)
        val remaining = manager.remainingAttempts()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0A18))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "일일 입장 횟수",
                fontSize = 13.sp,
                color = SubText,
            )
            Text(
                text = "$remaining / $DUNGEON_DAILY_LIMIT",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (remaining > 0) NeonGreen else NeonRed,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Dungeon cards ──
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(ALL_DUNGEONS) { def ->
                DungeonCard(
                    def = def,
                    gameData = data,
                    onEnter = {
                        val mgr = DungeonManager(data)
                        val updated = mgr.enterDungeon(def.id)
                        if (updated != null) {
                            repository.save(updated)
                            // TODO: navigate to battle with dungeon mode
                        }
                    },
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DungeonCard(
    def: DungeonDef,
    gameData: GameData,
    onEnter: () -> Unit,
) {
    val isUnlocked = gameData.trophies >= def.requiredTrophies
    val manager = DungeonManager(gameData)
    val canEnter = manager.canEnter(def.id)
    val bestWave = gameData.dungeonClears[def.id] ?: 0
    val remaining = manager.remainingAttempts()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isUnlocked) BgCard else BgCardLocked)
            .border(
                width = 1.dp,
                color = if (isUnlocked) BorderUnlocked else BorderLocked,
                shape = RoundedCornerShape(12.dp),
            )
            .then(if (!isUnlocked) Modifier.alpha(0.5f) else Modifier),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            // ── Header row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dungeonTypeIcon(def.type),
                    fontSize = 28.sp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = def.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) TextPrimary else DimText,
                    )
                    Text(
                        text = def.description,
                        fontSize = 12.sp,
                        color = if (isUnlocked) NeonGreen else DimText,
                    )
                }
                // Lock badge
                if (!isUnlocked) {
                    Text(
                        text = "\uD83D\uDD12",
                        fontSize = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Stats row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatChip(label = "필요 트로피", value = "${def.requiredTrophies}", color = Gold)
                StatChip(label = "스태미나", value = "${def.staminaCost}", color = StaminaGreen)
                StatChip(label = "웨이브", value = if (def.waveCount >= 999) "무제한" else "${def.waveCount}", color = SubText)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Best record + attempts row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (bestWave > 0) "최고 기록: ${bestWave}웨이브" else "기록 없음",
                    fontSize = 12.sp,
                    color = if (bestWave > 0) Gold else DimText,
                )
                Text(
                    text = "남은 횟수: $remaining",
                    fontSize = 12.sp,
                    color = if (remaining > 0) SubText else NeonRed,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Enter button ──
            NeonButton(
                text = when {
                    !isUnlocked -> "트로피 ${def.requiredTrophies} 필요"
                    remaining <= 0 -> "오늘 입장 완료"
                    gameData.stamina < def.staminaCost -> "스태미나 부족"
                    else -> "입장 (스태미나 ${def.staminaCost})"
                },
                onClick = onEnter,
                enabled = canEnter,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                accentColor = if (canEnter) NeonRed else DimText,
                accentColorDark = if (canEnter) NeonRedDark else DimText.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = DimText,
        )
    }
}
