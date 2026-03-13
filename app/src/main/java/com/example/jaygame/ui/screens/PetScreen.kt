package com.example.jaygame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.ALL_PETS
import com.example.jaygame.data.GameData
import com.example.jaygame.data.PET_PULL_10_COST
import com.example.jaygame.data.PET_PULL_COST
import com.example.jaygame.data.PetCategory
import com.example.jaygame.data.PetDef
import com.example.jaygame.data.PetGrade
import com.example.jaygame.data.PetProgress
import com.example.jaygame.data.petCardsRequired
import com.example.jaygame.data.petUpgradeCost
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.DiamondBlue
import com.example.jaygame.ui.theme.Divider
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.GoldCoin
import com.example.jaygame.ui.theme.NeonGreen
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark
import com.example.jaygame.ui.theme.SubText

// ── Pre-allocated colors ──
private val PetBgDark = Color(0xFF1A1A2E)
private val PetBgCard = Color(0xFF16213E)
private val PetBgPanel = Color(0xFF0D0D1F)
private val PetBgSlotEmpty = Color(0xFF2A2A3E)
private val PetTextPrimary = Color(0xFFE0E0E0)
private val PetTextSecondary = Color(0xFFAAAAAA)
private val PetLockColor = Color(0xFF555555)
private val PetTabBg = Color(0xFF0D0D1F)

private fun petGradeColor(grade: PetGrade): Color = Color(grade.colorHex)

private fun petCategoryIcon(category: PetCategory): String = when (category) {
    PetCategory.ATTACK -> "\u2694\uFE0F"
    PetCategory.SUPPORT -> "\uD83D\uDE4F"
    PetCategory.UTILITY -> "\u2699\uFE0F"
}

@Composable
fun PetScreen(
    gameData: GameData,
    onPull: () -> Unit,
    onPull10: () -> Unit,
    onUpgrade: (petId: Int) -> Unit,
    onEquip: (petId: Int) -> Unit,
    onUnequip: (petId: Int) -> Unit,
    onBack: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val pullResults = remember { mutableStateListOf<PetDef>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PetBgDark),
    ) {
        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PetBgPanel)
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
                text = "펫",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(72.dp))
        }

        // ── Tab row ──
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = PetTabBg,
            contentColor = Gold,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Gold,
                    )
                }
            },
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "내 펫",
                        fontSize = 14.sp,
                        color = if (selectedTab == 0) Gold else SubText,
                    )
                },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "뽑기",
                        fontSize = 14.sp,
                        color = if (selectedTab == 1) Gold else SubText,
                    )
                },
            )
        }

        when (selectedTab) {
            0 -> PetCollectionTab(
                gameData = gameData,
                onUpgrade = onUpgrade,
                onEquip = onEquip,
                onUnequip = onUnequip,
            )
            1 -> PetPullTab(
                gameData = gameData,
                pullResults = pullResults,
                onPull = onPull,
                onPull10 = onPull10,
            )
        }
    }
}

// ── Tab 0: 내 펫 ─────────────────────────────────────────────────────────────

@Composable
private fun PetCollectionTab(
    gameData: GameData,
    onUpgrade: (petId: Int) -> Unit,
    onEquip: (petId: Int) -> Unit,
    onUnequip: (petId: Int) -> Unit,
) {
    var selectedPetId by remember { mutableIntStateOf(-1) }
    val selectedProgress = remember(selectedPetId, gameData.pets) {
        if (selectedPetId >= 0) gameData.pets.getOrNull(selectedPetId) else null
    }
    val selectedDef = remember(selectedPetId) {
        if (selectedPetId >= 0) ALL_PETS.getOrNull(selectedPetId) else null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Equipped pet slots ──
        PetEquippedSlotsBar(
            gameData = gameData,
            selectedPetId = selectedPetId,
            onSlotClick = { id -> selectedPetId = if (selectedPetId == id) -1 else id },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Pet grid + optional detail panel ──
        if (selectedProgress != null && selectedDef != null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(ALL_PETS) { def ->
                    val progress = gameData.pets.getOrNull(def.id)
                    PetGridItem(
                        def = def,
                        progress = progress,
                        isSelected = selectedPetId == def.id,
                        onClick = {
                            selectedPetId = if (selectedPetId == def.id) -1 else def.id
                        },
                    )
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
            }

            PetDetailPanel(
                gameData = gameData,
                petId = selectedPetId,
                progress = selectedProgress,
                def = selectedDef,
                onUpgrade = onUpgrade,
                onEquip = onEquip,
                onUnequip = onUnequip,
                onClose = { selectedPetId = -1 },
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(ALL_PETS) { def ->
                    val progress = gameData.pets.getOrNull(def.id)
                    PetGridItem(
                        def = def,
                        progress = progress,
                        isSelected = false,
                        onClick = { selectedPetId = def.id },
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun PetEquippedSlotsBar(
    gameData: GameData,
    selectedPetId: Int,
    onSlotClick: (Int) -> Unit,
) {
    val slotCount = gameData.equippedPetSlotCount

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PetBgPanel)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = "장착 펫 (${gameData.equippedPets.size}/$slotCount)",
            fontSize = 12.sp,
            color = SubText,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) { slotIndex ->
                val isUnlocked = slotIndex < slotCount
                val equippedPetId = gameData.equippedPets.getOrNull(slotIndex)
                val equippedDef = if (equippedPetId != null) ALL_PETS.getOrNull(equippedPetId) else null
                val isSelected = equippedPetId != null && equippedPetId == selectedPetId

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                !isUnlocked -> Color(0xFF111122)
                                equippedDef != null -> PetBgCard
                                else -> PetBgSlotEmpty
                            },
                        )
                        .then(
                            if (isSelected) Modifier.border(2.dp, Gold, RoundedCornerShape(10.dp))
                            else if (equippedDef != null) {
                                val gc = petGradeColor(equippedDef.grade)
                                Modifier.border(1.5.dp, gc.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            } else Modifier.border(1.dp, Divider, RoundedCornerShape(10.dp)),
                        )
                        .then(
                            if (isUnlocked && equippedPetId != null) Modifier.clickable { onSlotClick(equippedPetId) }
                            else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!isUnlocked) {
                        Text(text = "\uD83D\uDD12", fontSize = 18.sp, color = PetLockColor)
                    } else if (equippedDef != null) {
                        val progress = gameData.pets.getOrNull(equippedDef.id)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = petCategoryIcon(equippedDef.category),
                                fontSize = 20.sp,
                            )
                            Text(
                                text = equippedDef.name,
                                fontSize = 8.sp,
                                color = PetTextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Lv.${progress?.level ?: 1}",
                                fontSize = 8.sp,
                                color = petGradeColor(equippedDef.grade),
                            )
                        }
                    } else {
                        Text(text = "+", fontSize = 22.sp, color = SubText)
                    }
                }
            }
        }
    }
}

@Composable
private fun PetGridItem(
    def: PetDef,
    progress: PetProgress?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val owned = progress?.owned == true
    val gc = petGradeColor(def.grade)

    Box(
        modifier = Modifier
            .height(90.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (owned) PetBgCard else Color(0xFF111122))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = when {
                    isSelected -> Gold
                    owned -> gc.copy(alpha = 0.6f)
                    else -> Divider
                },
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (!owned) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "\uD83D\uDD12", fontSize = 20.sp, color = PetLockColor)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = def.name,
                    fontSize = 9.sp,
                    color = DimText,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp),
            ) {
                Text(text = petCategoryIcon(def.category), fontSize = 22.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = def.name,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = PetTextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Lv.${progress!!.level}",
                    fontSize = 9.sp,
                    color = gc,
                )
                Text(
                    text = def.grade.label,
                    fontSize = 8.sp,
                    color = gc.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun PetDetailPanel(
    gameData: GameData,
    petId: Int,
    progress: PetProgress,
    def: PetDef,
    onUpgrade: (petId: Int) -> Unit,
    onEquip: (petId: Int) -> Unit,
    onUnequip: (petId: Int) -> Unit,
    onClose: () -> Unit,
) {
    val gc = petGradeColor(def.grade)
    val maxLevel = def.grade.maxLevel
    val isMaxLevel = progress.level >= maxLevel
    val cardsRequired = if (!isMaxLevel) petCardsRequired(progress.level) else 0
    val upgradeCost = if (!isMaxLevel) petUpgradeCost(progress.level) else 0
    val canAfford = gameData.gold >= upgradeCost && progress.cards >= cardsRequired
    val isEquipped = petId in gameData.equippedPets
    val canEquip = !isEquipped && gameData.equippedPets.size < gameData.equippedPetSlotCount

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PetBgPanel)
            .padding(12.dp),
    ) {
        HorizontalDivider(color = gc.copy(alpha = 0.4f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            // Icon + grade badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PetBgCard)
                    .border(2.dp, gc, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = petCategoryIcon(def.category), fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = def.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PetTextPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(gc.copy(alpha = 0.2f))
                            .border(1.dp, gc.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(text = def.grade.label, fontSize = 10.sp, color = gc, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = petCategoryIcon(def.category),
                        fontSize = 12.sp,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "레벨 ${progress.level} / $maxLevel",
                    fontSize = 12.sp,
                    color = SubText,
                )
                if (!isMaxLevel) {
                    Text(
                        text = "카드: ${progress.cards} / $cardsRequired",
                        fontSize = 12.sp,
                        color = if (progress.cards >= cardsRequired) NeonGreen else PetTextSecondary,
                    )
                } else {
                    Text(
                        text = "최대 레벨 달성!",
                        fontSize = 12.sp,
                        color = Gold,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\uD83D\uDCA1 ${def.skillName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                )
                Text(
                    text = def.skillDescription,
                    fontSize = 11.sp,
                    color = PetTextSecondary,
                )
            }

            NeonButton(
                text = "닫기",
                onClick = onClose,
                fontSize = 11.sp,
                accentColor = SubText,
                accentColorDark = DimText,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (progress.owned) {
                NeonButton(
                    text = if (isMaxLevel) "최대 레벨" else "강화 (골드 ${upgradeCost.petFormatNum()})",
                    onClick = { if (!isMaxLevel && canAfford) onUpgrade(petId) },
                    enabled = !isMaxLevel && canAfford,
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    accentColor = if (isMaxLevel) DimText else if (canAfford) Gold else NeonRed,
                    accentColorDark = if (isMaxLevel) DimText.copy(alpha = 0.5f) else if (canAfford) com.example.jaygame.ui.theme.DarkGold else NeonRedDark,
                )
                NeonButton(
                    text = if (isEquipped) "해제" else "장착",
                    onClick = {
                        if (isEquipped) onUnequip(petId) else if (canEquip) onEquip(petId)
                    },
                    enabled = isEquipped || canEquip,
                    modifier = Modifier.width(80.dp),
                    fontSize = 12.sp,
                    accentColor = if (isEquipped) NeonRed else NeonGreen,
                    accentColorDark = if (isEquipped) NeonRedDark else NeonGreen.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// ── Tab 1: 뽑기 ──────────────────────────────────────────────────────────────

@Composable
private fun PetPullTab(
    gameData: GameData,
    pullResults: androidx.compose.runtime.MutableList<PetDef>,
    onPull: () -> Unit,
    onPull10: () -> Unit,
) {
    val canPull = gameData.diamonds >= PET_PULL_COST
    val canPull10 = gameData.diamonds >= PET_PULL_10_COST

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Diamond display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PetBgCard)
                .border(1.dp, DiamondBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\uD83D\uDC8E 다이아",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DiamondBlue,
            )
            Text(
                text = gameData.diamonds.petFormatNum(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DiamondBlue,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pity counter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1E3F))
                .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "천장",
                fontSize = 13.sp,
                color = SubText,
            )
            Text(
                text = "${gameData.petPullPity} / 50",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (gameData.petPullPity >= 40) NeonRed else Gold,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "50회 뽑기 시 전설 등급 이상 확정",
            fontSize = 11.sp,
            color = DimText,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Single pull button
        NeonButton(
            text = "\uD83E\uDD50 1회 뽑기  ($PET_PULL_COST \uD83D\uDC8E)",
            onClick = onPull,
            enabled = canPull,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            fontSize = 15.sp,
            accentColor = if (canPull) DiamondBlue else DimText,
            accentColorDark = if (canPull) DiamondBlue.copy(alpha = 0.6f) else DimText.copy(alpha = 0.5f),
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 10-pull button
        NeonButton(
            text = "\uD83E\uDD50\uD83E\uDD50 10회 뽑기  ($PET_PULL_10_COST \uD83D\uDC8E)",
            onClick = onPull10,
            enabled = canPull10,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            fontSize = 15.sp,
            accentColor = if (canPull10) Gold else DimText,
            accentColorDark = if (canPull10) com.example.jaygame.ui.theme.DarkGold else DimText.copy(alpha = 0.5f),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Pull result display area
        if (pullResults.isNotEmpty()) {
            Text(
                text = "뽑기 결과",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(pullResults) { def ->
                    val gc = petGradeColor(def.grade)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PetBgCard)
                            .border(1.5.dp, gc, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = petCategoryIcon(def.category), fontSize = 18.sp)
                            Text(
                                text = def.name,
                                fontSize = 7.sp,
                                color = PetTextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = def.grade.label,
                                fontSize = 7.sp,
                                color = gc,
                            )
                        }
                    }
                }
            }
        } else {
            // Empty result placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111130))
                    .border(1.dp, Divider, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "뽑기 결과가 여기에 표시됩니다",
                    fontSize = 13.sp,
                    color = DimText,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pull rates info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0D1530))
                .border(1.dp, Divider, RoundedCornerShape(8.dp))
                .padding(12.dp),
        ) {
            Text(text = "확률 안내", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SubText)
            Spacer(modifier = Modifier.height(6.dp))
            PetGrade.entries.forEach { grade ->
                val gc = petGradeColor(grade)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = grade.label, fontSize = 11.sp, color = gc)
                    val totalWeight = PetGrade.entries.sumOf { it.pullWeight }.toFloat()
                    val pct = grade.pullWeight / totalWeight * 100f
                    Text(text = "%.1f%%".format(pct), fontSize = 11.sp, color = gc)
                }
            }
        }
    }
}

private fun Int.petFormatNum(): String {
    return if (this >= 1000) {
        val thousands = this / 1000
        val remainder = this % 1000
        if (remainder == 0) "${thousands},000" else "$thousands,${remainder.toString().padStart(3, '0')}"
    } else this.toString()
}
