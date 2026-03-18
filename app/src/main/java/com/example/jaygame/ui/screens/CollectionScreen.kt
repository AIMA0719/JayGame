package com.example.jaygame.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.LEVEL_MULTIPLIER
import com.example.jaygame.data.UPGRADE_COSTS
import com.example.jaygame.data.UnitFamily
import com.example.jaygame.data.UnitProgress
import com.example.jaygame.engine.AttackRange
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.DamageType
import com.example.jaygame.engine.UnitBlueprint
import com.example.jaygame.engine.UnitCategory
import com.example.jaygame.engine.UnitGrade
import com.example.jaygame.engine.UnitRole
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
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

// ── Grade-based card background colors (pre-allocated) ──
private val GradeBgCommon = Color(0xFF424242)
private val GradeBgRare = Color(0xFF1A237E)
private val GradeBgHero = Color(0xFF4A148C)
private val GradeBorderGold = Color(0xFFFFD700)
private val GradeBorderRed = Color(0xFFEF4444)
private val GradeBorderRainbowStart = Color(0xFFFF6B35)
private val GradeBorderRainbowEnd = Color(0xFFFBBF24)

// ── Filter chip colors (pre-allocated) ──
private val CollChipSelectedBg = Color(0xFF2A2A4E)
private val CollChipUnselectedBg = Color(0xFF1A1A2E)
private val CollChipSelectedBorder = Color(0xFFFFD700)
private val CollChipUnselectedBorder = Color(0xFF333355)

// ── Damage type indicator colors (pre-allocated) ──
private val CollPhysicalColor = Color(0xFFFF8A65)
private val CollMagicColor = Color(0xFFCE93D8)

// ── Role icon labels (reuse from UnitCollectionScreen concept) ──
private val COLL_ROLE_LABELS = mapOf(
    UnitRole.TANK to "🛡탱커",
    UnitRole.MELEE_DPS to "⚔근딜",
    UnitRole.RANGED_DPS to "🏹원딜",
    UnitRole.SUPPORT to "✚서포터",
    UnitRole.CONTROLLER to "⛓컨트롤러",
)

// ── Behavior pattern labels ──
private val COLL_BEHAVIOR_LABELS = mapOf(
    "tank_blocker" to "전방 저지형",
    "assassin_dash" to "돌진 암살형",
    "ranged_mage" to "원거리 마법형",
    "support_aura" to "오라 지원형",
    "controller_cc_ranged" to "원거리 제어형",
)

private val COLL_FAMILY_ICONS = mapOf(
    UnitFamily.FIRE to "\uD83D\uDD25",
    UnitFamily.FROST to "\u2744\uFE0F",
    UnitFamily.POISON to "\uD83D\uDCA8",
    UnitFamily.LIGHTNING to "\u26A1",
    UnitFamily.SUPPORT to "\uD83D\uDE4F",
    UnitFamily.WIND to "\uD83C\uDF00",
)

private fun gradeBackgroundColor(grade: UnitGrade): Color = when (grade) {
    UnitGrade.COMMON -> GradeBgCommon
    UnitGrade.RARE -> GradeBgRare
    UnitGrade.HERO -> GradeBgHero
    else -> GradeBgCommon
}

private fun gradeGlowColor(grade: UnitGrade): Color? = when (grade) {
    UnitGrade.COMMON -> null
    UnitGrade.RARE -> null
    UnitGrade.HERO -> null
    UnitGrade.LEGEND -> GradeBorderGold
    UnitGrade.ANCIENT -> GradeBorderRed
    UnitGrade.MYTHIC -> GradeBorderRainbowEnd
    UnitGrade.IMMORTAL -> GradeBorderRainbowStart
}

@Composable
fun CollectionScreen(
    repository: GameRepository,
    // Relic callbacks
    onRelicUpgrade: ((Int) -> Unit)? = null,
    onRelicEquip: ((Int) -> Unit)? = null,
    onRelicUnequip: ((Int) -> Unit)? = null,
    // Pet callbacks
    onPetPull: (() -> Unit)? = null,
    onPetPull10: (() -> Unit)? = null,
    onPetUpgrade: ((Int) -> Unit)? = null,
    onPetEquip: ((Int) -> Unit)? = null,
    onPetUnequip: ((Int) -> Unit)? = null,
) {
    val data by repository.gameData.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

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
                onClick = { selectedTab = 0 },
                text = {
                    val unitCount = data.units.count { (_, u) -> u.owned }
                    Text(
                        text = "영웅 ($unitCount)",
                        fontSize = 14.sp,
                        color = if (selectedTab == 0) Gold else SubText,
                    )
                },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    val relicCount = data.relics.count { it.owned }
                    Text(
                        text = "유물 ($relicCount)",
                        fontSize = 14.sp,
                        color = if (selectedTab == 1) Gold else SubText,
                    )
                },
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    val petCount = data.pets.count { it.owned }
                    Text(
                        text = "펫 ($petCount)",
                        fontSize = 14.sp,
                        color = if (selectedTab == 2) Gold else SubText,
                    )
                },
            )
        }

        when (selectedTab) {
            0 -> HeroCollectionTab(repository = repository, data = data)
            1 -> RelicScreen(
                gameData = data,
                onUpgrade = onRelicUpgrade ?: {},
                onEquip = onRelicEquip ?: {},
                onUnequip = onRelicUnequip ?: {},
                showTopBar = false,
            )
            2 -> PetScreen(
                gameData = data,
                onPull = onPetPull ?: {},
                onPull10 = onPetPull10 ?: {},
                onUpgrade = onPetUpgrade ?: {},
                onEquip = onPetEquip ?: {},
                onUnequip = onPetUnequip ?: {},
                showTopBar = false,
            )
        }
    }
}

@Composable
private fun HeroCollectionTab(
    repository: GameRepository,
    data: com.example.jaygame.data.GameData,
) {
    var selectedBlueprint by remember { mutableStateOf<UnitBlueprint?>(null) }

    // ── Data source: BlueprintRegistry ──
    val allBlueprints = remember {
        BlueprintRegistry.instance.findByCategory(UnitCategory.NORMAL)
    }

    // ── Filter state ──
    var selectedRoles by remember { mutableStateOf(emptySet<UnitRole>()) }
    var selectedFamilies by remember { mutableStateOf(emptySet<UnitFamily>()) }
    var sortMode by remember { mutableStateOf(SortMode.GRADE) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepDark),
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Description banner
            val ownedCount = data.units.count { (_, u) -> u.owned }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1A2A1A), Color(0xFF2A1A2A))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                Column {
                    Text(
                        text = "수집한 영웅들의 정보를 확인하세요",
                        fontSize = 13.sp,
                        color = LightText.copy(alpha = 0.9f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "\uD83D\uDCD6 보유 $ownedCount / ${allBlueprints.size}",
                            fontSize = 12.sp,
                            color = SubText,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${filteredBlueprints.size}종 표시중",
                            fontSize = 11.sp,
                            color = DimText,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Filter bar ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Row 1: Role chips
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
                        CollFilterChip(
                            label = COLL_ROLE_LABELS[role] ?: role.label,
                            selected = selected,
                            onClick = {
                                selectedRoles = if (selected) selectedRoles - role
                                else selectedRoles + role
                            },
                        )
                    }
                }

                // Row 2: Family chips
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
                        val icon = COLL_FAMILY_ICONS[family] ?: ""
                        val selected = family in selectedFamilies
                        CollFilterChip(
                            label = "$icon${family.label}",
                            selected = selected,
                            onClick = {
                                selectedFamilies = if (selected) selectedFamilies - family
                                else selectedFamilies + family
                            },
                        )
                    }
                }

                // Row 3: Sort chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "정렬",
                        color = SubText,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    SortMode.entries.forEach { mode ->
                        CollFilterChip(
                            label = mode.label,
                            selected = sortMode == mode,
                            onClick = { sortMode = mode },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Grid display ──
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredBlueprints, key = { it.id }) { bp ->
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

        // Detail dialog overlay
        selectedBlueprint?.let { bp ->
            val progress = data.units[bp.id]
            CollectionBlueprintDetailDialog(
                blueprint = bp,
                progress = progress,
                gold = data.gold,
                onUpgrade = {
                    if (progress != null && progress.level < 7) {
                        val cost = UPGRADE_COSTS[progress.level - 1]
                        val newUnits = data.units.toMutableMap()
                        val cur = newUnits[bp.id] ?: return@CollectionBlueprintDetailDialog
                        newUnits[bp.id] = cur.copy(
                            level = cur.level + 1,
                            cards = cur.cards - cost.first,
                        )
                        val newGold = data.gold - cost.second
                        val newMaxLevel = maxOf(data.maxUnitLevel, newUnits[bp.id]!!.level)
                        repository.save(data.copy(units = newUnits, gold = newGold, maxUnitLevel = newMaxLevel))
                    }
                },
                onDismiss = { selectedBlueprint = null },
            )
        }
    }
}

@Composable
private fun CollFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) CollChipSelectedBg else CollChipUnselectedBg)
            .border(
                width = 1.dp,
                color = if (selected) CollChipSelectedBorder else CollChipUnselectedBorder,
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

@Composable
private fun CollectionBlueprintCard(
    blueprint: UnitBlueprint,
    progress: UnitProgress?,
    onClick: () -> Unit,
) {
    val owned = progress?.owned == true
    val level = progress?.level ?: 1
    val iconRes = blueprintIconRes(blueprint)

    GameCard(
        borderColor = blueprint.grade.color,
        backgroundColor = gradeBackgroundColor(blueprint.grade),
        glowColor = gradeGlowColor(blueprint.grade),
        onClick = onClick,
        modifier = Modifier
            .then(if (!owned) Modifier.alpha(0.5f) else Modifier),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = blueprint.name,
                    modifier = Modifier
                        .size(36.dp)
                        .then(if (!owned) Modifier.alpha(0.3f) else Modifier),
                )
                if (!owned) {
                    Text(
                        text = "\uD83D\uDD12",
                        fontSize = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (owned) blueprint.name else "???",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (owned) LightText else DimText,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Role + damage type indicators
            if (owned) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (blueprint.damageType == DamageType.PHYSICAL) CollPhysicalColor
                                else CollMagicColor
                            ),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (blueprint.attackRange == AttackRange.MELEE) "근" else "원",
                        fontSize = 8.sp,
                        color = SubText,
                    )
                }
            }

            Text(
                text = blueprint.grade.label,
                fontSize = 9.sp,
                color = blueprint.grade.color.copy(alpha = if (owned) 1f else 0.5f),
                textAlign = TextAlign.Center,
            )

            if (owned) {
                Text(
                    text = buildStarString(level),
                    fontSize = 10.sp,
                    color = Gold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CollectionBlueprintDetailDialog(
    blueprint: UnitBlueprint,
    progress: UnitProgress?,
    gold: Int,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    val owned = progress?.owned == true
    val level = progress?.level ?: 1
    val calculatedATK = (blueprint.stats.baseATK * LEVEL_MULTIPLIER[level - 1]).toInt()
    val upgradeCost = if (level < 7) UPGRADE_COSTS[level - 1] else null
    val canUpgrade = owned && level < 7
            && upgradeCost != null
            && (progress?.cards ?: 0) >= upgradeCost.first
            && gold >= upgradeCost.second
    val fmt = NumberFormat.getNumberInstance()
    val iconRes = blueprintIconRes(blueprint)
    val behaviorLabel = COLL_BEHAVIOR_LABELS[blueprint.behaviorId] ?: blueprint.behaviorId
    val roleLabel = COLL_ROLE_LABELS[blueprint.role] ?: blueprint.role.label

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
                .width(300.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
            borderColor = blueprint.grade.color,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Large icon + name + rarity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = blueprint.name,
                            modifier = Modifier.size(42.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = blueprint.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = LightText,
                        )
                        Text(
                            text = blueprint.grade.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = blueprint.grade.color,
                        )
                        Text(
                            text = roleLabel,
                            fontSize = 11.sp,
                            color = blueprint.grade.color.copy(alpha = 0.8f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Damage type + Attack range + Behavior
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (blueprint.damageType == DamageType.PHYSICAL) CollPhysicalColor
                                    else CollMagicColor
                                ),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (blueprint.damageType == DamageType.PHYSICAL) "물리" else "마법",
                            color = if (blueprint.damageType == DamageType.PHYSICAL) CollPhysicalColor else CollMagicColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(text = "|", color = SubText, fontSize = 10.sp)
                    Text(
                        text = if (blueprint.attackRange == AttackRange.MELEE) "근접" else "원거리",
                        color = SubText,
                        fontSize = 10.sp,
                    )
                    Text(text = "|", color = SubText, fontSize = 10.sp)
                    Text(
                        text = behaviorLabel,
                        color = SubText,
                        fontSize = 10.sp,
                    )
                }

                // Family badges
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    blueprint.families.forEach { family ->
                        val icon = COLL_FAMILY_ICONS[family] ?: ""
                        Box(
                            modifier = Modifier
                                .background(family.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "$icon ${family.label}",
                                color = family.color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Full stats
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    StatRow("체력", "${blueprint.stats.hp.toInt()}", Color(0xFF81C784))
                    StatRow("공격력", fmt.format(calculatedATK), Gold)
                    StatRow("공속", String.format("%.2f", blueprint.stats.baseSpeed), LightText)
                    StatRow("사거리", String.format("%.0f", blueprint.stats.range), LightText)
                    StatRow("방어력", "${blueprint.stats.defense.toInt()}", Color(0xFFFFCC80))
                    StatRow("마법저항", "${blueprint.stats.magicResist.toInt()}", Color(0xFFB39DDB))
                    StatRow("이동속도", "${blueprint.stats.moveSpeed.toInt()}", Color(0xFF80CBC4))
                    StatRow("블록 수", "${blueprint.stats.blockCount}", Gold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Description
                Text(
                    text = blueprint.description,
                    fontSize = 12.sp,
                    color = SubText,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Ability section
                if (blueprint.ability != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    StatRow("능력", blueprint.ability.name, NeonCyan)
                    Text(
                        text = blueprint.ability.description,
                        fontSize = 11.sp,
                        color = SubText,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                    )
                }

                // Unique Ability section
                if (blueprint.uniqueAbility != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                blueprint.grade.color.copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "\u2726 ",
                                fontSize = 13.sp,
                                color = blueprint.grade.color,
                            )
                            Text(
                                text = blueprint.uniqueAbility.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = blueprint.grade.color,
                            )
                        }
                        blueprint.uniqueAbility.passive?.let { passive ->
                            Text(
                                text = "[패시브] ${passive.description}",
                                fontSize = 11.sp,
                                color = LightText.copy(alpha = 0.9f),
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        blueprint.uniqueAbility.active?.let { active ->
                            Text(
                                text = "[액티브] ${active.description}" +
                                    if (active.cooldown > 0) " (쿨타임 ${active.cooldown.toInt()}초)" else "",
                                fontSize = 11.sp,
                                color = NeonCyan,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }

                // Merge info
                if (blueprint.mergeResultId != null) {
                    val nextBp = remember(blueprint.mergeResultId) {
                        BlueprintRegistry.instance.findById(blueprint.mergeResultId)
                    }
                    if (nextBp != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Gold.copy(alpha = 0.08f))
                                .padding(8.dp),
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
                                text = "x3 \u2192 ${nextBp.grade.label} ${nextBp.name}",
                                color = LightText,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Level with stars
                if (owned) {
                    Text(
                        text = "레벨 $level  ${buildStarString(level)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Card progress
                    if (upgradeCost != null) {
                        Text(
                            text = "카드: ${progress?.cards ?: 0} / ${upgradeCost.first}",
                            fontSize = 12.sp,
                            color = if ((progress?.cards ?: 0) >= upgradeCost.first) PositiveGreen else SubText,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Upgrade button
                    if (level >= 7) {
                        NeonButton(
                            text = "최대 레벨",
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = DimText,
                            accentColorDark = DimText,
                        )
                    } else {
                        val goldCost = upgradeCost?.second ?: 0
                        NeonButton(
                            text = "업그레이드 (골드 ${fmt.format(goldCost)})",
                            onClick = onUpgrade,
                            enabled = canUpgrade,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp,
                            accentColor = NeonGreen,
                            accentColorDark = NeonGreen.copy(alpha = 0.5f),
                        )
                    }
                } else {
                    NeonButton(
                        text = "미보유",
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = DimText,
                        accentColorDark = DimText,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                NeonButton(
                    text = "닫기",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = SubText,
                    accentColorDark = DimText,
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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

private fun buildStarString(level: Int): String {
    val filled = level.coerceIn(0, 7)
    val empty = (7 - filled).coerceAtLeast(0)
    return "\u2605".repeat(filled) + "\u2606".repeat(empty)
}
