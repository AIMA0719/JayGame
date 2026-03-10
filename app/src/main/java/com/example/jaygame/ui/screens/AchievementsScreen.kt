package com.example.jaygame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.data.GameData
import com.example.jaygame.data.GameRepository
import com.example.jaygame.ui.components.CurrencyHeader
import com.example.jaygame.ui.components.GameProgressBar
import com.example.jaygame.ui.components.MedievalButton
import com.example.jaygame.ui.components.MedievalCard
import com.example.jaygame.ui.components.ScreenHeader
import com.example.jaygame.ui.theme.DarkBrown
import com.example.jaygame.ui.theme.DarkGold
import com.example.jaygame.ui.theme.DiamondBlue
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.GoldCoin
import com.example.jaygame.ui.theme.MedievalFont
import com.example.jaygame.ui.theme.MetalGray
import com.example.jaygame.ui.theme.MediumBrown
import com.example.jaygame.ui.theme.Parchment
import com.example.jaygame.ui.theme.PositiveGreen

// ── Achievement definitions ──

enum class AchievementCategory(val label: String) {
    BATTLE("전투"),
    MERGE("합성"),
    COLLECTION("수집"),
    ECONOMY("경제"),
    SPECIAL("특수"),
}

data class AchievementDef(
    val id: Int,
    val name: String,
    val description: String,
    val category: AchievementCategory,
    val threshold: Int,
    val goldReward: Int,
    val diamondReward: Int,
)

val ACHIEVEMENTS = listOf(
    // Battle (0-4)
    AchievementDef(0, "첫 승리", "전투에서 1회 승리", AchievementCategory.BATTLE, 1, 100, 0),
    AchievementDef(1, "전투의 달인", "전투에서 10회 승리", AchievementCategory.BATTLE, 10, 500, 5),
    AchievementDef(2, "전쟁 영웅", "전투에서 50회 승리", AchievementCategory.BATTLE, 50, 2000, 20),
    AchievementDef(3, "웨이브 서퍼", "20 웨이브 돌파", AchievementCategory.BATTLE, 20, 300, 2),
    AchievementDef(4, "학살자", "적 1000명 처치", AchievementCategory.BATTLE, 1000, 1000, 10),
    // Merge (5-8)
    AchievementDef(5, "첫 합성", "유닛 합성 1회", AchievementCategory.MERGE, 1, 100, 0),
    AchievementDef(6, "합성 장인", "유닛 합성 50회", AchievementCategory.MERGE, 50, 500, 5),
    AchievementDef(7, "합성의 신", "유닛 합성 200회", AchievementCategory.MERGE, 200, 1500, 15),
    AchievementDef(8, "레벨 마스터", "유닛 레벨 5 달성", AchievementCategory.MERGE, 5, 800, 8),
    // Collection (9-11)
    AchievementDef(9, "수집가", "유닛 5종 보유", AchievementCategory.COLLECTION, 5, 200, 2),
    AchievementDef(10, "도감 완성자", "유닛 10종 보유", AchievementCategory.COLLECTION, 10, 1000, 10),
    AchievementDef(11, "전설의 소유자", "유닛 15종 보유", AchievementCategory.COLLECTION, 15, 3000, 30),
    // Economy (12-15)
    AchievementDef(12, "부자의 시작", "골드 1000 획득", AchievementCategory.ECONOMY, 1000, 200, 0),
    AchievementDef(13, "골드 러시", "골드 10000 획득", AchievementCategory.ECONOMY, 10000, 1000, 5),
    AchievementDef(14, "재벌", "골드 100000 획득", AchievementCategory.ECONOMY, 100000, 5000, 50),
    AchievementDef(15, "업그레이드 매니아", "유닛 업그레이드 10회", AchievementCategory.ECONOMY, 10, 500, 5),
    // Special (16-19)
    AchievementDef(16, "퍼펙트 승리", "무피해 승리", AchievementCategory.SPECIAL, 1, 500, 5),
    AchievementDef(17, "한길만 파기", "단일 유닛 타입 승리", AchievementCategory.SPECIAL, 1, 500, 5),
    AchievementDef(18, "실버 달성", "실버 랭크 도달", AchievementCategory.SPECIAL, 1000, 1000, 10),
    AchievementDef(19, "골드 달성", "골드 랭크 도달", AchievementCategory.SPECIAL, 2000, 2000, 20),
)

// ── Progress calculation ──

private fun getProgress(achievement: AchievementDef, data: GameData): Int {
    return when (achievement.id) {
        in 0..2 -> data.totalWins
        3 -> data.highestWave
        4 -> data.totalKills
        in 5..7 -> data.totalMerges
        8 -> data.maxUnitLevel
        in 9..11 -> data.units.count { it.owned }
        in 12..14 -> data.totalGoldEarned
        15 -> {
            // Proxy: sum of all unit levels minus initial 15 (each starts at 1)
            val totalLevels = data.units.sumOf { it.level }
            (totalLevels - data.units.size).coerceAtLeast(0)
        }
        16 -> if (data.wonWithoutDamage) 1 else 0
        17 -> if (data.wonWithSingleType) 1 else 0
        in 18..19 -> data.trophies
        else -> 0
    }
}

// ── Screen composable ──

@Composable
fun AchievementsScreen(
    repository: GameRepository,
    onBack: () -> Unit,
) {
    val data by repository.gameData.collectAsState()
    val categories = AchievementCategory.entries
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val filteredAchievements = remember(selectedTabIndex) {
        ACHIEVEMENTS.filter { it.category == categories[selectedTabIndex] }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBrown),
    ) {
        // Currency Header
        CurrencyHeader(gold = data.gold, diamonds = data.diamonds)

        Spacer(modifier = Modifier.height(8.dp))

        ScreenHeader(title = "\uC5C5\uC801", onBack = onBack)

        Spacer(modifier = Modifier.height(12.dp))

        // Category tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MediumBrown,
            contentColor = Parchment,
            edgePadding = 8.dp,
            divider = {},
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = category.label,
                            fontFamily = MedievalFont,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            color = if (selectedTabIndex == index) Gold else Parchment.copy(alpha = 0.7f),
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Achievement list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filteredAchievements, key = { it.id }) { achievement ->
                val progress = getProgress(achievement, data)
                val isCompleted = progress >= achievement.threshold
                AchievementItem(
                    achievement = achievement,
                    progress = progress,
                    isCompleted = isCompleted,
                )
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AchievementItem(
    achievement: AchievementDef,
    progress: Int,
    isCompleted: Boolean,
) {
    val borderColor = if (isCompleted) PositiveGreen.copy(alpha = 0.8f) else DarkGold

    MedievalCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = borderColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Trophy icon
            Text(
                text = "\uD83C\uDFC6",
                fontSize = 28.sp,
                color = if (isCompleted) Gold else MetalGray,
                modifier = Modifier.padding(end = 10.dp),
            )

            // Center: name, description, progress bar
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = achievement.name,
                    fontFamily = MedievalFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isCompleted) Gold else Parchment,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = achievement.description,
                    fontFamily = MedievalFont,
                    fontSize = 12.sp,
                    color = Parchment.copy(alpha = 0.7f),
                )

                Spacer(modifier = Modifier.height(6.dp))

                val clampedProgress = progress.coerceAtMost(achievement.threshold)
                val fraction = if (achievement.threshold > 0) {
                    clampedProgress.toFloat() / achievement.threshold.toFloat()
                } else {
                    0f
                }

                GameProgressBar(progress = fraction, height = 14.dp)

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "$clampedProgress / ${achievement.threshold}",
                    fontFamily = MedievalFont,
                    fontSize = 11.sp,
                    color = if (isCompleted) PositiveGreen else Parchment.copy(alpha = 0.6f),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: rewards
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (achievement.goldReward > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_gold),
                            contentDescription = null,
                            tint = GoldCoin,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "${achievement.goldReward}",
                            fontFamily = MedievalFont,
                            fontSize = 12.sp,
                            color = Parchment,
                        )
                    }
                }
                if (achievement.diamondReward > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_diamond),
                            contentDescription = null,
                            tint = DiamondBlue,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "${achievement.diamondReward}",
                            fontFamily = MedievalFont,
                            fontSize = 12.sp,
                            color = Parchment,
                        )
                    }
                }
            }
        }
    }
}
