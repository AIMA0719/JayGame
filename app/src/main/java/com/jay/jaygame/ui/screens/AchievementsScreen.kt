package com.jay.jaygame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.jaygame.R
import com.jay.jaygame.data.GameData
import com.jay.jaygame.data.GameRepository
import com.jay.jaygame.ui.components.GameCard
import com.jay.jaygame.ui.components.NeonButton
import com.jay.jaygame.ui.components.NeonProgressBar
import com.jay.jaygame.ui.components.ResourceHeader
import com.jay.jaygame.ui.theme.BorderGlow
import com.jay.jaygame.ui.theme.DeepDark
import com.jay.jaygame.ui.theme.DiamondBlue
import com.jay.jaygame.ui.theme.DimText
import com.jay.jaygame.ui.theme.Gold
import com.jay.jaygame.ui.theme.GoldCoin
import com.jay.jaygame.ui.theme.LightText
import com.jay.jaygame.ui.theme.NeonCyan
import com.jay.jaygame.ui.theme.NeonGreen
import com.jay.jaygame.ui.theme.NeonRed
import com.jay.jaygame.ui.theme.NeonRedDark
import com.jay.jaygame.ui.theme.SubText

// ── Achievement definitions ──

enum class AchievementCategory(val label: String) {
    BATTLE("전투"),
    MERGE("합성"),
    COLLECTION("수집"),
    ECONOMY("경제"),
    SPECIAL("특수"),
    RELIC("유물"),
    PET("펫"),
    DUNGEON("던전"),
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
    // Relic (20-21)
    AchievementDef(20, "유물 수집가", "유물 3종 획득", AchievementCategory.RELIC, 3, 500, 50),
    AchievementDef(21, "유물 강화 마스터", "유물 레벨 10 달성", AchievementCategory.RELIC, 10, 1000, 100),
    // Pet (22-23)
    AchievementDef(22, "펫 친구", "첫 펫 획득", AchievementCategory.PET, 1, 200, 20),
    AchievementDef(23, "펫 마스터", "펫 3종 레벨 10+", AchievementCategory.PET, 3, 2000, 200),
    // Dungeon (24-25)
    AchievementDef(24, "던전 탐험가", "던전 첫 클리어", AchievementCategory.DUNGEON, 1, 300, 30),
    AchievementDef(25, "던전 정복자", "모든 던전 클리어", AchievementCategory.DUNGEON, 5, 3000, 300),
)

// ── Progress calculation ──

private fun getProgress(achievement: AchievementDef, data: GameData): Int {
    return when (achievement.id) {
        in 0..2 -> data.totalWins
        3 -> data.highestWave
        4 -> data.totalKills
        in 5..7 -> data.totalMerges
        8 -> data.maxUnitLevel
        in 9..11 -> data.units.count { (_, u) -> u.owned }
        in 12..14 -> data.totalGoldEarned
        15 -> {
            val totalLevels = data.units.values.sumOf { it.level }
            (totalLevels - data.units.size).coerceAtLeast(0)
        }
        16 -> if (data.wonWithoutDamage) 1 else 0
        17 -> if (data.wonWithSingleType) 1 else 0
        in 18..19 -> data.trophies
        20 -> data.relics.count { it.owned }
        21 -> data.relics.maxOfOrNull { it.level } ?: 0
        22 -> if (data.pets.any { it.owned }) 1 else 0
        23 -> data.pets.count { it.owned && it.level >= 10 }
        24 -> if (data.dungeonClears.isNotEmpty()) 1 else 0
        25 -> data.dungeonClears.size
        else -> 0
    }
}

// ── Screen composable ──

private val TabBg = Color(0xFF0D1B2A)

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

    // 현재 탭에서 수령 가능한 업적 개수
    val claimableCount = filteredAchievements.count { a ->
        val progress = getProgress(a, data)
        progress >= a.threshold && a.id !in data.claimedAchievements
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDark),
    ) {
        // Resource Header
        ResourceHeader(gold = data.gold, diamonds = data.diamonds)

        Spacer(modifier = Modifier.height(8.dp))

        // Back button + title row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = com.jay.jaygame.R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = LightText,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onBack),
            )
            Text(
                text = "업적",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Gold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(56.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category tabs — ScrollableTabRow
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = TabBg,
            contentColor = LightText,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Gold,
                    )
                }
            },
            divider = {},
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = category.label,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) Gold else DimText,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 모두 수령 버튼
        if (claimableCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                NeonButton(
                    text = "모두 수령 ($claimableCount)",
                    onClick = {
                        var current = repository.gameData.value
                        var totalGold = 0
                        var totalDiamonds = 0
                        var claimed = current.claimedAchievements
                        for (a in filteredAchievements) {
                            if (a.id in claimed) continue
                            val progress = getProgress(a, current)
                            if (progress >= a.threshold) {
                                totalGold += a.goldReward
                                totalDiamonds += a.diamondReward
                                claimed = claimed + a.id
                            }
                        }
                        if (totalGold > 0 || totalDiamonds > 0) {
                            repository.save(current.copy(
                                gold = current.gold + totalGold,
                                diamonds = current.diamonds + totalDiamonds,
                                claimedAchievements = claimed,
                                seasonXP = current.seasonXP + (claimed.size - current.claimedAchievements.size) * 10,
                            ))
                        }
                    },
                    fontSize = 13.sp,
                    accentColor = NeonGreen,
                    accentColorDark = NeonGreen.copy(alpha = 0.5f),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Achievement list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filteredAchievements, key = { it.id }) { achievement ->
                val progress = getProgress(achievement, data)
                val isCompleted = progress >= achievement.threshold
                val isClaimed = achievement.id in data.claimedAchievements
                AchievementItem(
                    achievement = achievement,
                    progress = progress,
                    isCompleted = isCompleted,
                    isClaimed = isClaimed,
                    onClaim = {
                        val current = repository.gameData.value
                        if (isCompleted && achievement.id !in current.claimedAchievements) {
                            // Achievement claim also grants season XP (10 per achievement)
                            repository.save(current.copy(
                                gold = current.gold + achievement.goldReward,
                                diamonds = current.diamonds + achievement.diamondReward,
                                claimedAchievements = current.claimedAchievements + achievement.id,
                                seasonXP = current.seasonXP + 10,
                            ))
                        }
                    },
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
    isClaimed: Boolean,
    onClaim: () -> Unit,
) {
    val borderColor = when {
        isClaimed -> DimText.copy(alpha = 0.5f)
        isCompleted -> NeonGreen.copy(alpha = 0.7f)
        else -> BorderGlow
    }

    GameCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = borderColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status indicator
            Text(
                text = when {
                    isClaimed -> "\u2714"
                    isCompleted -> "\u2713"
                    else -> "\u25CB"
                },
                fontSize = if (isCompleted || isClaimed) 18.sp else 14.sp,
                color = when {
                    isClaimed -> DimText
                    isCompleted -> NeonGreen
                    else -> SubText
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .width(28.dp),
            )

            // Center: name, description, progress bar
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = achievement.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = when {
                        isClaimed -> DimText
                        isCompleted -> NeonGreen
                        else -> LightText
                    },
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = achievement.description,
                    fontSize = 11.sp,
                    color = SubText,
                )

                Spacer(modifier = Modifier.height(5.dp))

                val clampedProgress = progress.coerceAtMost(achievement.threshold)
                val fraction = if (achievement.threshold > 0) {
                    clampedProgress.toFloat() / achievement.threshold.toFloat()
                } else {
                    0f
                }

                NeonProgressBar(
                    progress = fraction,
                    height = 8.dp,
                    barColor = when {
                        isClaimed -> DimText
                        isCompleted -> NeonGreen
                        else -> NeonCyan
                    },
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "$clampedProgress / ${achievement.threshold}",
                    fontSize = 10.sp,
                    color = if (isCompleted) NeonGreen else SubText,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: rewards + claim button
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                // Always show reward preview (unless already claimed)
                if (!isClaimed) {
                    if (achievement.goldReward > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_gold),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "${achievement.goldReward}",
                                fontSize = 11.sp,
                                color = LightText,
                            )
                        }
                    }
                    if (achievement.diamondReward > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_diamond),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "${achievement.diamondReward}",
                                fontSize = 11.sp,
                                color = LightText,
                            )
                        }
                    }
                }

                // Claim button or status
                if (isCompleted && !isClaimed) {
                    NeonButton(
                        text = "수령",
                        onClick = onClaim,
                        fontSize = 12.sp,
                        accentColor = Gold,
                        accentColorDark = Gold.copy(alpha = 0.5f),
                    )
                } else if (isClaimed) {
                    Text(
                        text = "수령 완료",
                        fontSize = 11.sp,
                        color = DimText,
                    )
                }
            }
        }
    }
}
