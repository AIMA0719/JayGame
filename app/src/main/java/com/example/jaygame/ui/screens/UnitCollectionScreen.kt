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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.data.UnitFamily
import com.example.jaygame.engine.AttackRange
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.DamageType
import com.example.jaygame.engine.UnitBlueprint
import com.example.jaygame.engine.UnitCategory
import com.example.jaygame.engine.UnitGrade
import com.example.jaygame.engine.UnitRole
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.NeonGreen
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.SubText

// ── Grade-based card background colors (pre-allocated) ──
private val CodexGradeBgCommon = Color(0xFF424242)
private val CodexGradeBgRare = Color(0xFF1A237E)
private val CodexGradeBgHero = Color(0xFF4A148C)
private val CodexGradeBorderGold = Color(0xFFFFD700)
private val CodexGradeBorderRed = Color(0xFFEF4444)
private val CodexGradeBorderRainbowStart = Color(0xFFFF6B35)
private val CodexGradeBorderRainbowEnd = Color(0xFFFBBF24)

// ── Filter chip colors (pre-allocated) ──
private val ChipSelectedBg = Color(0xFF2A2A4E)
private val ChipUnselectedBg = Color(0xFF1A1A2E)
private val ChipSelectedBorder = Color(0xFFFFD700)
private val ChipUnselectedBorder = Color(0xFF333355)

// ── Damage type indicator colors (pre-allocated) ──
private val PhysicalColor = Color(0xFFFF8A65)
private val MagicColor = Color(0xFFCE93D8)

// ── Role icon labels ──
private val ROLE_LABELS = mapOf(
    UnitRole.TANK to "🛡탱커",
    UnitRole.MELEE_DPS to "⚔근딜",
    UnitRole.RANGED_DPS to "🏹원딜",
    UnitRole.SUPPORT to "✚서포터",
    UnitRole.CONTROLLER to "⛓컨트롤러",
)

// ── Behavior pattern labels ──
private val BEHAVIOR_LABELS = mapOf(
    "tank_blocker" to "전방 저지형",
    "assassin_dash" to "돌진 암살형",
    "ranged_mage" to "원거리 마법형",
    "support_aura" to "오라 지원형",
    "controller_cc_ranged" to "원거리 제어형",
)

private val FAMILY_ICONS = mapOf(
    UnitFamily.FIRE to "\uD83D\uDD25",
    UnitFamily.FROST to "\u2744\uFE0F",
    UnitFamily.POISON to "\uD83D\uDCA8",
    UnitFamily.LIGHTNING to "\u26A1",
    UnitFamily.SUPPORT to "\uD83D\uDE4F",
    UnitFamily.WIND to "\uD83C\uDF00",
)

// ── Blueprint ID → icon resource mapping ──
private val BLUEPRINT_ICON_MAP: Map<String, Int> = mapOf(
    "fire_mdps_01" to R.drawable.ic_unit_0,
    "fire_mdps_02" to R.drawable.ic_unit_5,
    "fire_mdps_03" to R.drawable.ic_unit_10,
    "fire_mdps_04" to R.drawable.ic_unit_15,
    "fire_mdps_05" to R.drawable.ic_unit_20,
    "frost_rdps_01" to R.drawable.ic_unit_1,
    "frost_rdps_02" to R.drawable.ic_unit_6,
    "frost_rdps_03" to R.drawable.ic_unit_11,
    "frost_rdps_04" to R.drawable.ic_unit_16,
    "frost_rdps_05" to R.drawable.ic_unit_21,
    "poison_mdps_01" to R.drawable.ic_unit_2,
    "poison_mdps_02" to R.drawable.ic_unit_7,
    "poison_mdps_03" to R.drawable.ic_unit_12,
    "poison_mdps_04" to R.drawable.ic_unit_17,
    "poison_mdps_05" to R.drawable.ic_unit_22,
    "lightning_mdps_01" to R.drawable.ic_unit_3,
    "lightning_mdps_02" to R.drawable.ic_unit_8,
    "lightning_mdps_03" to R.drawable.ic_unit_13,
    "lightning_mdps_04" to R.drawable.ic_unit_18,
    "lightning_mdps_05" to R.drawable.ic_unit_23,
    "support_support_01" to R.drawable.ic_unit_4,
    "support_support_02" to R.drawable.ic_unit_9,
    "support_support_03" to R.drawable.ic_unit_14,
    "support_support_04" to R.drawable.ic_unit_19,
    "support_support_05" to R.drawable.ic_unit_24,
    "wind_mdps_01" to R.drawable.ic_unit_35,
    "wind_mdps_02" to R.drawable.ic_unit_36,
    "wind_mdps_03" to R.drawable.ic_unit_37,
    "wind_mdps_04" to R.drawable.ic_unit_38,
    "wind_mdps_05" to R.drawable.ic_unit_39,
)

/** Resolve icon resource for a blueprint. Falls back to ic_unit_0. */
internal fun blueprintIconRes(bp: UnitBlueprint): Int =
    if (bp.iconRes != 0) bp.iconRes
    else BLUEPRINT_ICON_MAP[bp.id] ?: R.drawable.ic_unit_0

private fun codexGradeBgColor(grade: UnitGrade): Brush = when (grade) {
    UnitGrade.COMMON -> Brush.verticalGradient(listOf(CodexGradeBgCommon, Color(0xFF303030)))
    UnitGrade.RARE -> Brush.verticalGradient(listOf(CodexGradeBgRare, Color(0xFF0D1642)))
    UnitGrade.HERO -> Brush.verticalGradient(listOf(CodexGradeBgHero, Color(0xFF280A42)))
    else -> Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF12121F)))
}

private fun codexGradeBorderColor(grade: UnitGrade): Color = when (grade) {
    UnitGrade.LEGEND -> CodexGradeBorderGold
    UnitGrade.ANCIENT -> CodexGradeBorderRed
    UnitGrade.MYTHIC -> CodexGradeBorderRainbowEnd
    UnitGrade.IMMORTAL -> CodexGradeBorderRainbowStart
    else -> grade.color.copy(alpha = 0.4f)
}

enum class SortMode(val label: String) {
    GRADE("등급순"),
    ATK("공격력순"),
    NAME("이름순"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnitCollectionScreen(
    onBack: () -> Unit,
    repository: com.example.jaygame.data.GameRepository? = null,
) {
    var selectedBlueprint by remember { mutableStateOf<UnitBlueprint?>(null) }
    val gameData = repository?.gameData?.collectAsState()?.value

    // ── Data source: BlueprintRegistry ──
    val allBlueprints = remember {
        BlueprintRegistry.instance.findByCategory(UnitCategory.NORMAL)
    }

    // ── Filter state ──
    var selectedRoles by remember { mutableStateOf(emptySet<UnitRole>()) }
    var selectedFamilies by remember { mutableStateOf(emptySet<UnitFamily>()) }
    var selectedGrades by remember { mutableStateOf(emptySet<UnitGrade>()) }
    var sortMode by remember { mutableStateOf(SortMode.GRADE) }

    // ── Filtered + sorted list ──
    val filteredBlueprints = remember(allBlueprints, selectedRoles, selectedFamilies, selectedGrades, sortMode) {
        allBlueprints
            .filter { bp ->
                (selectedRoles.isEmpty() || bp.role in selectedRoles) &&
                (selectedFamilies.isEmpty() || bp.families.any { it in selectedFamilies }) &&
                (selectedGrades.isEmpty() || bp.grade in selectedGrades)
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
                androidx.compose.material3.Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "뒤로",
                    tint = LightText,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onBack),
                )
                Text(
                    text = "\uD83D\uDCD6 영웅 도감",
                    color = LightText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 12.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${filteredBlueprints.size}/${allBlueprints.size}종",
                    color = Gold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category tabs: 일반 | 히든 | 특수
            var selectedTab by remember { mutableIntStateOf(0) }
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeepDark.copy(alpha = 0.9f),
                contentColor = Gold,
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("일반", modifier = Modifier.padding(8.dp), color = if (selectedTab == 0) Gold else SubText)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("히든", modifier = Modifier.padding(8.dp), color = if (selectedTab == 1) Gold else SubText)
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("특수", modifier = Modifier.padding(8.dp), color = if (selectedTab == 2) Gold else SubText)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> {
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
                                FilterChip(
                                    label = ROLE_LABELS[role] ?: role.label,
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
                                val icon = FAMILY_ICONS[family] ?: ""
                                val selected = family in selectedFamilies
                                FilterChip(
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
                                FilterChip(
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
                            CodexBlueprintCard(
                                blueprint = bp,
                                onClick = { selectedBlueprint = bp },
                                isOwned = gameData?.units?.get(bp.id)?.owned ?: true,
                            )
                        }
                    }
                }
                1 -> {
                    // Hidden units tab
                    val hiddenBlueprints = remember {
                        BlueprintRegistry.instance.findByCategory(UnitCategory.HIDDEN)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "히든 유닛은 특수 조합으로 발견할 수 있습니다.",
                            color = SubText,
                            fontSize = 13.sp,
                        )
                        if (hiddenBlueprints.isEmpty()) {
                            repeat(18) { index ->
                                HiddenUnitCard(
                                    blueprintId = "hidden_$index",
                                    discovered = false,
                                )
                            }
                        } else {
                            hiddenBlueprints.forEach { bp ->
                                HiddenUnitCard(
                                    blueprintId = bp.id,
                                    discovered = false,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                2 -> {
                    // Special units tab
                    val specialBlueprints = remember {
                        BlueprintRegistry.instance.findByCategory(UnitCategory.SPECIAL)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "특수 유닛은 필드 효과를 가진 유닛입니다.",
                            color = SubText,
                            fontSize = 13.sp,
                        )
                        Text(
                            text = "최대 2기까지 배치 가능합니다.",
                            color = SubText.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                        )
                        if (specialBlueprints.isNotEmpty()) {
                            specialBlueprints.forEach { bp ->
                                HiddenUnitCard(
                                    blueprintId = bp.id,
                                    discovered = true,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // Detail dialog overlay
        selectedBlueprint?.let { bp ->
            BlueprintDetailDialog(
                blueprint = bp,
                onDismiss = { selectedBlueprint = null },
                repository = repository,
            )
        }
    }
}

@Composable
private fun FilterChip(
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

@Composable
private fun CodexBlueprintCard(
    blueprint: UnitBlueprint,
    onClick: () -> Unit,
    isOwned: Boolean = true,
) {
    val gradeColor = blueprint.grade.color
    val borderColor = codexGradeBorderColor(blueprint.grade)
    val iconRes = blueprintIconRes(blueprint)
    val roleLabel = ROLE_LABELS[blueprint.role] ?: blueprint.role.label

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(codexGradeBgColor(blueprint.grade))
            .border(
                width = if (blueprint.grade.ordinal >= UnitGrade.LEGEND.ordinal) 2.dp else 1.dp,
                color = if (isOwned) borderColor else borderColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .then(if (!isOwned) Modifier.graphicsLayer { alpha = 0.4f } else Modifier)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Grade indicator bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(gradeColor),
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Unit icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .shadow(4.dp, CircleShape, ambientColor = gradeColor, spotColor = gradeColor)
                .clip(CircleShape)
                .background(gradeColor.copy(alpha = 0.15f))
                .border(1.5.dp, gradeColor.copy(alpha = 0.5f), CircleShape),
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = blueprint.name,
                modifier = Modifier.size(30.dp),
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Unit name
        Text(
            text = blueprint.name,
            color = LightText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // Role + damage type row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Damage type dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (blueprint.damageType == DamageType.PHYSICAL) PhysicalColor
                        else MagicColor
                    ),
            )
            Spacer(modifier = Modifier.width(3.dp))
            // Attack range
            Text(
                text = if (blueprint.attackRange == AttackRange.MELEE) "근" else "원",
                color = SubText,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        // Grade label
        Text(
            text = blueprint.grade.label,
            color = gradeColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun BlueprintDetailDialog(
    blueprint: UnitBlueprint,
    onDismiss: () -> Unit,
    repository: com.example.jaygame.data.GameRepository? = null,
) {
    val gradeColor = blueprint.grade.color
    val scope = rememberCoroutineScope()
    val iconRes = blueprintIconRes(blueprint)
    val behaviorLabel = BEHAVIOR_LABELS[blueprint.behaviorId] ?: blueprint.behaviorId
    val roleLabel = ROLE_LABELS[blueprint.role] ?: blueprint.role.label

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
                .width(320.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
            borderColor = gradeColor.copy(alpha = 0.6f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Grade bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(gradeColor),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Large icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(8.dp, CircleShape, ambientColor = gradeColor, spotColor = gradeColor)
                        .clip(CircleShape)
                        .background(gradeColor.copy(alpha = 0.15f))
                        .border(2.dp, gradeColor.copy(alpha = 0.6f), CircleShape),
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = blueprint.name,
                        modifier = Modifier.size(56.dp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grade label
                Text(
                    text = blueprint.grade.label,
                    color = gradeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )

                // Unit name
                Text(
                    text = blueprint.name,
                    color = LightText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                // Role badge
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(gradeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = roleLabel,
                        color = gradeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Family badges
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    blueprint.families.forEach { family ->
                        val icon = FAMILY_ICONS[family] ?: ""
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

                Spacer(modifier = Modifier.height(4.dp))

                // Damage type + Attack range + Behavior
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Damage type
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (blueprint.damageType == DamageType.PHYSICAL) PhysicalColor
                                    else MagicColor
                                ),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (blueprint.damageType == DamageType.PHYSICAL) "물리" else "마법",
                            color = if (blueprint.damageType == DamageType.PHYSICAL) PhysicalColor else MagicColor,
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

                Spacer(modifier = Modifier.height(16.dp))

                // Full stats section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DetailStatRow("체력", "${blueprint.stats.hp.toInt()}", Color(0xFF81C784))
                    DetailStatRow("공격력", "${blueprint.stats.baseATK.toInt()}", Color(0xFFFF8A80))
                    DetailStatRow("공속", "%.2f".format(blueprint.stats.baseSpeed), Color(0xFF80D8FF))
                    DetailStatRow("사거리", "${blueprint.stats.range.toInt()}", MagicColor)
                    DetailStatRow("방어력", "${blueprint.stats.defense.toInt()}", Color(0xFFFFCC80))
                    DetailStatRow("마법저항", "${blueprint.stats.magicResist.toInt()}", Color(0xFFB39DDB))
                    DetailStatRow("이동속도", "${blueprint.stats.moveSpeed.toInt()}", Color(0xFF80CBC4))
                    DetailStatRow("블록 수", "${blueprint.stats.blockCount}", Gold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ability section
                if (blueprint.ability != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "\u2726 ${blueprint.ability.name}",
                            color = blueprint.families.firstOrNull()?.color ?: NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = blueprint.ability.description,
                            color = SubText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }

                // Unique Ability
                if (blueprint.uniqueAbility != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(gradeColor.copy(alpha = 0.08f))
                            .padding(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "\u2728 ", fontSize = 14.sp)
                            Text(
                                text = blueprint.uniqueAbility.name,
                                color = gradeColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        // Show passive
                        blueprint.uniqueAbility.passive?.let { passive ->
                            Text(
                                text = "[패시브] ${passive.description}",
                                color = LightText.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        // Show active
                        blueprint.uniqueAbility.active?.let { active ->
                            Text(
                                text = "[액티브] ${active.description}" +
                                    if (active.cooldown > 0) " (쿨타임 ${active.cooldown.toInt()}초)" else "",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }

                // Description
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = blueprint.description,
                    color = SubText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Merge info
                if (blueprint.mergeResultId != null) {
                    val nextBp = remember(blueprint.mergeResultId) {
                        BlueprintRegistry.instance.findById(blueprint.mergeResultId)
                    }
                    if (nextBp != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Gold.copy(alpha = 0.08f))
                                .padding(10.dp),
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

                // Card-based permanent level up
                if (repository != null) {
                    val data = repository.gameData.collectAsState().value
                    val progress = data.units[blueprint.id]
                    if (progress != null && progress.owned) {
                        val cardsNeeded = progress.level * 3
                        val canLevelUp = progress.cards >= cardsNeeded && progress.level < 10

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lv.${progress.level}" + if (progress.level >= 10) " (MAX)" else "",
                                    color = if (progress.level >= 10) Gold else LightText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (progress.level < 10) {
                                    Text(
                                        text = "카드: ${progress.cards} / $cardsNeeded",
                                        color = if (canLevelUp) NeonGreen else SubText,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                            if (progress.level < 10) {
                                NeonButton(
                                    text = "레벨업",
                                    onClick = {
                                        if (canLevelUp) {
                                            val updatedUnits = data.units.toMutableMap()
                                            updatedUnits[blueprint.id] = progress.copy(
                                                level = progress.level + 1,
                                                cards = progress.cards - cardsNeeded,
                                            )
                                            val newMaxLevel = updatedUnits.values.maxOf { it.level }
                                            scope.launch(Dispatchers.IO) {
                                                repository.save(data.copy(
                                                    units = updatedUnits,
                                                    maxUnitLevel = newMaxLevel,
                                                ))
                                            }
                                        }
                                    },
                                    enabled = canLevelUp,
                                    modifier = Modifier
                                        .width(72.dp)
                                        .height(32.dp),
                                    fontSize = 12.sp,
                                    accentColor = if (canLevelUp) NeonGreen else DimText,
                                    accentColorDark = if (canLevelUp) NeonGreen.copy(alpha = 0.5f) else DimText.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                NeonButton(
                    text = "닫기",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    accentColor = SubText,
                    accentColorDark = DimText,
                )
            }
        }
    }
}

@Composable
private fun DetailStatRow(
    label: String,
    value: String,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = SubText,
            fontSize = 12.sp,
        )
        Text(
            text = value,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = LightText,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Hidden unit card — shows full info if discovered, silhouette if not.
 */
@Composable
private fun HiddenUnitCard(blueprintId: String, discovered: Boolean) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (discovered) Color(0xFF1A1A2E) else Color(0xFF0D0D0D)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (discovered) {
                Text(
                    text = blueprintId,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            } else {
                Text(
                    text = "???",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF333333),
                )
                Text(
                    text = "미발견",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF555555),
                )
            }
        }
    }
}
