package com.example.jaygame.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.ui.viewmodel.CollectionViewModel
import com.example.jaygame.ui.viewmodel.CollectionSideEffect
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import com.example.jaygame.data.LEVEL_MULTIPLIER
import com.example.jaygame.data.UPGRADE_COSTS
import com.example.jaygame.data.UnitFamily
import com.example.jaygame.data.UnitProgress
import com.example.jaygame.engine.AttackRange
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.DamageType
import com.example.jaygame.engine.UnitBlueprint

import com.example.jaygame.engine.UnitGrade
import com.example.jaygame.engine.UnitRole
import com.example.jaygame.ui.components.BEHAVIOR_LABELS
import com.example.jaygame.ui.components.FAMILY_ICONS
import com.example.jaygame.ui.components.GameFilterChip
import com.example.jaygame.ui.components.GradeBgCommon
import com.example.jaygame.ui.components.GradeBgHero
import com.example.jaygame.ui.components.GradeBgRare
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.components.ROLE_LABELS
import com.example.jaygame.ui.components.SortMode
import com.example.jaygame.ui.components.UnitStatRow
import com.example.jaygame.ui.theme.DarkSurface
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.NeonGreen
import com.example.jaygame.ui.theme.PositiveGreen
import com.example.jaygame.ui.theme.SubText
import java.text.NumberFormat

// ── Tab bar colors ──
private val CollectionTabBg = Color(0xFF1A1A2E)

// ── Card shared shapes (pre-allocated to avoid per-card allocation) ──
private val CardShape = RoundedCornerShape(12.dp)
private val IconShape = RoundedCornerShape(8.dp)

// ── Dimmed text colors (pre-allocated) ──
private val DimNameColor = DimText.copy(alpha = 0.6f)
private val DimSubText = SubText.copy(alpha = 0.5f)
private val GoldDim = Gold.copy(alpha = 0.8f)

// ── Progress bar track color ──
private val ProgressTrackColor = Color(0xFF2A2A3E)

// ── Detail sheet colors ──
private val SheetHandleColor = Color(0xFF4A4A5E)
private val SheetBg = Color(0xFF1A1520)

// Grade/chip colors, label maps, and family icons imported from UnitUiUtils

private fun gradeBackgroundColor(grade: UnitGrade): Color = when (grade) {
    UnitGrade.COMMON -> GradeBgCommon
    UnitGrade.RARE -> GradeBgRare
    UnitGrade.HERO -> GradeBgHero
    else -> GradeBgCommon
}


@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel,
) {
    val collectionState by viewModel.collectAsState()
    val data = collectionState.gameData
    val context = androidx.compose.ui.platform.LocalContext.current

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is CollectionSideEffect.ShowToast ->
                android.widget.Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val selectedTab by remember { derivedStateOf { pagerState.currentPage } }
    val coroutineScope = rememberCoroutineScope()

    // ── Cached tab counts ──
    val unitCount = remember(data.units) { data.units.count { (_, u) -> u.owned } }
    val relicCount = remember(data.relics) { data.relics.count { it.owned } }
    val petCount = remember(data.pets) { data.pets.count { it.owned } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDark),
    ) {
        // ── Tab row ──
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CollectionTabBg,
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
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                text = {
                    Text(
                        text = "영웅 ($unitCount)",
                        fontSize = 14.sp,
                        color = if (selectedTab == 0) Gold else SubText,
                    )
                },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                text = {
                    Text(
                        text = "유물 ($relicCount)",
                        fontSize = 14.sp,
                        color = if (selectedTab == 1) Gold else SubText,
                    )
                },
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                text = {
                    Text(
                        text = "펫 ($petCount)",
                        fontSize = 14.sp,
                        color = if (selectedTab == 2) Gold else SubText,
                    )
                },
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
        ) { page ->
            when (page) {
                0 -> HeroCollectionTab(viewModel = viewModel, data = data, ownedCount = unitCount)
                1 -> RelicScreen(
                    gameData = data,
                    onUpgrade = { viewModel.upgradeRelic(it) },
                    onEquip = { viewModel.equipRelic(it) },
                    onUnequip = { viewModel.unequipRelic(it) },
                    showTopBar = false,
                )
                2 -> PetScreen(
                    gameData = data,
                    onPull = { viewModel.pullPet() },
                    onPull10 = { viewModel.pullPet10() },
                    onUpgrade = { viewModel.upgradePet(it) },
                    onEquip = { viewModel.equipPet(it) },
                    onUnequip = { viewModel.unequipPet(it) },
                    showTopBar = false,
                )
            }
        }
    }
}

@Composable
private fun HeroCollectionTab(
    viewModel: CollectionViewModel,
    data: com.example.jaygame.data.GameData,
    ownedCount: Int,
) {
    var selectedBlueprint by remember { mutableStateOf<UnitBlueprint?>(null) }

    // ── Data source: BlueprintRegistry ──
    val allBlueprints = remember {
        BlueprintRegistry.instance.all()
    }
    val totalCount = remember(allBlueprints) { allBlueprints.size }

    // ── Filter state ──
    var selectedRoles by remember { mutableStateOf(emptySet<UnitRole>()) }
    var selectedFamilies by remember { mutableStateOf(emptySet<UnitFamily>()) }
    var sortMode by remember { mutableStateOf(SortMode.GRADE) }
    var filterExpanded by remember { mutableStateOf(false) }

    // ── Active filter count for collapsed indicator ──
    val activeFilterCount = selectedRoles.size + selectedFamilies.size

    // ── Filtered + sorted list ──
    val filteredBlueprints = remember(allBlueprints, selectedRoles, selectedFamilies, sortMode) {
        allBlueprints
            .filter { bp ->
                (selectedRoles.isEmpty() || bp.role in selectedRoles) &&
                (selectedFamilies.isEmpty() || bp.families.any { it in selectedFamilies })
            }
            .sortedWith(when (sortMode) {
                SortMode.GRADE -> compareByDescending { it.grade.ordinal }
                SortMode.ATK -> compareByDescending { it.stats.baseATK }
                SortMode.NAME -> compareBy { it.name }
            })
    }

    // ── Collection progress ──
    val progress by remember(ownedCount, totalCount) {
        derivedStateOf {
            if (totalCount > 0) ownedCount.toFloat() / totalCount else 0f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepDark),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Compact progress header ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "수집 진행도",
                        fontSize = 12.sp,
                        color = SubText,
                    )
                    Text(
                        text = "$ownedCount / $totalCount  (${(progress * 100).toInt()}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Gold,
                    trackColor = ProgressTrackColor,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Filter toggle + sort row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Filter toggle button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (filterExpanded) Gold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (filterExpanded) Gold.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(16.dp),
                        )
                        .clickable { filterExpanded = !filterExpanded }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (filterExpanded) "필터 접기" else "필터",
                        fontSize = 11.sp,
                        color = if (filterExpanded) Gold else SubText,
                    )
                    if (activeFilterCount > 0 && !filterExpanded) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Gold, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$activeFilterCount",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepDark,
                            )
                        }
                    }
                }

                // Sort chips (always visible)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SortMode.entries.forEach { mode ->
                        GameFilterChip(
                            label = mode.label,
                            selected = sortMode == mode,
                            onClick = { sortMode = mode },
                        )
                    }
                }
            }

            // ── Collapsible filter section ──
            AnimatedVisibility(
                visible = filterExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Role chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "역할",
                            color = SubText,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        UnitRole.entries.forEach { role ->
                            val selected = role in selectedRoles
                            GameFilterChip(
                                label = ROLE_LABELS[role] ?: role.label,
                                selected = selected,
                                onClick = {
                                    selectedRoles = if (selected) selectedRoles - role
                                    else selectedRoles + role
                                },
                            )
                        }
                    }

                    // Family chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "계열",
                            color = SubText,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        UnitFamily.entries.forEach { family ->
                            val icon = FAMILY_ICONS[family] ?: ""
                            val selected = family in selectedFamilies
                            GameFilterChip(
                                label = "$icon${family.label}",
                                selected = selected,
                                onClick = {
                                    selectedFamilies = if (selected) selectedFamilies - family
                                    else selectedFamilies + family
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Grid display ──
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(filteredBlueprints, key = { it.id }, contentType = { "blueprint" }) { bp ->
                    val progress = data.units[bp.id]
                    val owned = progress?.owned == true
                    CollectionBlueprintCard(
                        blueprint = bp,
                        progress = progress,
                        onClick = { if (owned) selectedBlueprint = bp },
                    )
                }
            }
        }

        // Detail bottom sheet overlay
        selectedBlueprint?.let { bp ->
            val bpProgress = data.units[bp.id]
            CollectionBlueprintDetailSheet(
                blueprint = bp,
                progress = bpProgress,
                gold = data.gold,
                onUpgrade = {
                    if (bpProgress != null && bpProgress.level < 7) {
                        val cost = UPGRADE_COSTS.getOrNull(bpProgress.level - 1) ?: return@CollectionBlueprintDetailSheet
                        viewModel.upgradeUnit(bp.id, cost.first, cost.second)
                    }
                },
                onDismiss = { selectedBlueprint = null },
            )
        }
    }
}

@Composable
private fun CollectionBlueprintCard(
    blueprint: UnitBlueprint,
    progress: UnitProgress?,
    onClick: () -> Unit,
) {
    val owned = progress?.owned == true
    val level = progress?.level ?: 1
    val iconRes = blueprintIconRes(blueprint)
    val bgColor = gradeBackgroundColor(blueprint.grade)
    val borderColor = blueprint.grade.color
    val dimBorderColor = borderColor.copy(alpha = 0.4f)
    val gradeLabel = blueprint.grade.label
    val isMelee = blueprint.attackRange == AttackRange.MELEE

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .heightIn(min = 140.dp)
            .background(bgColor, CardShape)
            .border(1.dp, if (owned) borderColor else dimBorderColor, CardShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        // Unit icon — no clip, no extra Box layers
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .background(DarkSurface, IconShape)
                .padding(4.dp),
            alpha = if (owned) 1f else 0.15f,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Name
        Text(
            text = blueprint.name,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (owned) LightText else DimNameColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // Grade label
        Text(
            text = gradeLabel,
            fontSize = 9.sp,
            color = if (owned) borderColor else dimBorderColor,
            textAlign = TextAlign.Center,
        )

        // Range + ATK (simplified — no Row, no dot Box)
        Text(
            text = if (owned) {
                "${if (isMelee) "근" else "원"} ${blueprint.stats.baseATK.toInt()}"
            } else {
                if (isMelee) "근" else "원"
            },
            fontSize = 8.sp,
            fontWeight = if (owned) FontWeight.Bold else FontWeight.Normal,
            color = if (owned) GoldDim else DimSubText,
            textAlign = TextAlign.Center,
        )

        // Stars for owned
        if (owned) {
            Text(
                text = buildStarString(level),
                fontSize = 9.sp,
                color = Gold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Bottom-sheet style detail overlay — slides up from the bottom,
 * more immersive than a centered dialog.
 */
@Composable
private fun CollectionBlueprintDetailSheet(
    blueprint: UnitBlueprint,
    progress: UnitProgress?,
    gold: Int,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    val owned = progress?.owned == true
    val level = (progress?.level ?: 1).coerceIn(1, LEVEL_MULTIPLIER.size)
    val calculatedATK = remember(blueprint.stats.baseATK, level) {
        (blueprint.stats.baseATK * LEVEL_MULTIPLIER[level - 1]).toInt()
    }
    val upgradeCost = if (level < 7) UPGRADE_COSTS.getOrNull(level - 1) else null
    val canUpgrade = owned && level < 7
            && upgradeCost != null
            && (progress?.cards ?: 0) >= upgradeCost.first
            && gold >= upgradeCost.second
    val fmt = remember { NumberFormat.getNumberInstance() }
    val iconRes = blueprintIconRes(blueprint)
    val behaviorLabel = remember(blueprint.behaviorId) {
        BEHAVIOR_LABELS[blueprint.behaviorId] ?: blueprint.behaviorId
    }
    val roleLabel = remember(blueprint.role) {
        ROLE_LABELS[blueprint.role] ?: blueprint.role.label
    }
    val baseSpeedStr = remember(blueprint.stats.baseSpeed) {
        String.format("%.2f", blueprint.stats.baseSpeed)
    }
    val rangeStr = remember(blueprint.stats.range) {
        String.format("%.0f", blueprint.stats.range)
    }

    // Scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onDismiss() },
    ) {
        // Bottom sheet container — wrapContentHeight so it sizes to content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(SheetBg)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { /* consume clicks */ }
                .padding(top = 10.dp, bottom = 24.dp),
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SheetHandleColor),
                )
            }

            // Grade color bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(blueprint.grade.color),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Header: icon + name + grade + tags (compact row) ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.5.dp, blueprint.grade.color.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = blueprint.name,
                        modifier = Modifier.size(42.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = blueprint.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = LightText,
                        )
                        if (owned) {
                            Text(
                                text = buildStarString(level),
                                fontSize = 11.sp,
                                color = Gold,
                            )
                        }
                    }
                    // Grade + role + damage type + range in one line
                    Text(
                        text = buildString {
                            append(blueprint.grade.label)
                            append(" · ")
                            append(roleLabel)
                            append(" · ")
                            append(if (blueprint.damageType == DamageType.PHYSICAL) "물리" else "마법")
                            append(" · ")
                            append(if (blueprint.attackRange == AttackRange.MELEE) "근접" else "원거리")
                            append(" · ")
                            append(behaviorLabel)
                        },
                        fontSize = 10.sp,
                        color = SubText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Family badges inline
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 3.dp),
                    ) {
                        blueprint.families.forEach { family ->
                            val icon = FAMILY_ICONS[family] ?: ""
                            Text(
                                text = "$icon${family.label}",
                                color = family.color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(family.color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Stats: 2-column grid layout (compact) ──
            val statSectionBg = Color.White.copy(alpha = 0.04f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(statSectionBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    UnitStatRow("체력", "${blueprint.stats.hp.toInt()}", Color(0xFF81C784), Modifier.weight(1f))
                    UnitStatRow("공격력", fmt.format(calculatedATK), Gold, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    UnitStatRow("공속", baseSpeedStr, LightText, Modifier.weight(1f))
                    UnitStatRow("사거리", rangeStr, LightText, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    UnitStatRow("방어력", "${blueprint.stats.defense.toInt()}", Color(0xFFFFCC80), Modifier.weight(1f))
                    UnitStatRow("마법저항", "${blueprint.stats.magicResist.toInt()}", Color(0xFFB39DDB), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    UnitStatRow("이동속도", "${blueprint.stats.moveSpeed.toInt()}", Color(0xFF80CBC4), Modifier.weight(1f))
                    UnitStatRow("블록 수", "${blueprint.stats.blockCount}", Gold, Modifier.weight(1f))
                }
            }

            // ── Description (compact) ──
            if (blueprint.description.isNotBlank()) {
                Text(
                    text = blueprint.description,
                    fontSize = 11.sp,
                    color = SubText,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            // ── Ability + Unique Ability (merged compact section) ──
            if (blueprint.ability != null || blueprint.uniqueAbility != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (blueprint.uniqueAbility != null) blueprint.grade.color.copy(alpha = 0.06f)
                            else Color.White.copy(alpha = 0.04f)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    blueprint.ability?.let { ability ->
                        Text(
                            text = "능력: ${ability.name} — ${ability.description}",
                            fontSize = 10.sp,
                            color = NeonCyan,
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    blueprint.uniqueAbility?.let { ua ->
                        ua.passive?.let { passive ->
                            Text(
                                text = "\u2726 [패시브] ${passive.description}",
                                fontSize = 10.sp,
                                color = LightText.copy(alpha = 0.9f),
                                lineHeight = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        ua.active?.let { active ->
                            Text(
                                text = "\u2726 [액티브] ${active.description}" +
                                    if (active.cooldown > 0) " (${active.cooldown.toInt()}초)" else "",
                                fontSize = 10.sp,
                                color = NeonCyan,
                                lineHeight = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // ── Merge info (inline) ──
            if (blueprint.mergeResultId != null) {
                val nextBp = remember(blueprint.mergeResultId) {
                    BlueprintRegistry.instance.findById(blueprint.mergeResultId)
                }
                if (nextBp != null) {
                    Text(
                        text = "\u2728 조합: x3 \u2192 ${nextBp.grade.label} ${nextBp.name}",
                        color = Gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Upgrade / Level + Close buttons ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (owned) {
                    // Level info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lv.$level ${buildStarString(level)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold,
                        )
                        if (upgradeCost != null) {
                            Text(
                                text = "카드 ${progress?.cards ?: 0}/${upgradeCost.first}",
                                fontSize = 11.sp,
                                color = if ((progress?.cards ?: 0) >= upgradeCost.first) PositiveGreen else SubText,
                            )
                        }
                    }

                    if (level >= 7) {
                        NeonButton(
                            text = "MAX",
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.width(64.dp),
                            fontSize = 12.sp,
                            accentColor = DimText,
                            accentColorDark = DimText,
                        )
                    } else {
                        val goldCost = upgradeCost?.second ?: 0
                        NeonButton(
                            onClick = onUpgrade,
                            enabled = canUpgrade,
                            modifier = Modifier.width(100.dp),
                            accentColor = NeonGreen,
                            accentColorDark = NeonGreen.copy(alpha = 0.5f),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_gold),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = fmt.format(goldCost),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = LightText,
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                    NeonButton(
                        text = "미보유",
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.width(100.dp),
                        accentColor = DimText,
                        accentColorDark = DimText,
                    )
                }

                NeonButton(
                    text = "닫기",
                    onClick = onDismiss,
                    modifier = Modifier.width(72.dp),
                    accentColor = SubText,
                    accentColorDark = DimText,
                )
            }
        }
    }
}

private fun buildStarString(level: Int): String {
    val filled = level.coerceIn(0, 7)
    val empty = (7 - filled).coerceAtLeast(0)
    return "\u2605".repeat(filled) + "\u2606".repeat(empty)
}
