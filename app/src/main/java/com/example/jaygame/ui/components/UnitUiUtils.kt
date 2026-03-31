package com.example.jaygame.ui.components

import androidx.annotation.DrawableRes
import com.example.jaygame.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import androidx.compose.ui.draw.drawWithContent
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.jaygame.data.UnitFamily
import com.example.jaygame.data.UnitRace
import com.example.jaygame.engine.BlueprintRegistry
import com.example.jaygame.engine.UnitRole
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.SubText

// ── Race labels ──
val RACE_LABELS: Map<UnitRace, String> = mapOf(
    UnitRace.HUMAN to "인간",
    UnitRace.ANIMAL to "동물",
    UnitRace.DEMON to "악마",
    UnitRace.SPIRIT to "정령",
    UnitRace.ROBOT to "로봇",
)

// ── Race icons (drawable resource) ──
val RACE_ICON_RES: Map<UnitRace, Int> = mapOf(
    UnitRace.HUMAN to R.drawable.ic_race_human,
    UnitRace.SPIRIT to R.drawable.ic_race_spirit,
    UnitRace.ANIMAL to R.drawable.ic_race_animal,
    UnitRace.ROBOT to R.drawable.ic_race_robot,
    UnitRace.DEMON to R.drawable.ic_race_demon,
)

/** 종족 아이콘(PNG) + 라벨 텍스트 Row */
@Composable
fun RaceIconLabel(
    race: UnitRace,
    iconSize: Dp = 14.dp,
    fontSize: TextUnit = 10.sp,
    color: Color = race.color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RACE_ICON_RES[race]?.let { resId ->
            Image(
                painter = androidx.compose.ui.res.painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(
            text = race.label,
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Role icon labels (deprecated — kept for compatibility) ──
@Deprecated("Use RACE_LABELS instead")
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

// ── Family emoji icons (deprecated — kept for compatibility) ──
@Deprecated("Use RACE_ICON_RES instead")
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
val GradeBgAncient = Color(0xFF4A148C)

// ── Sort mode ──
enum class SortMode(val label: String) {
    GRADE("등급순"),
    ATK("공격력순"),
    NAME("이름순"),
}

// ── Shared filter chip composable ──
@Composable
fun GameFilterChip(
    label: String = "",
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable (() -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
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
        if (content != null) {
            content()
        } else {
            Text(
                text = label,
                color = if (selected) Gold else SubText,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
            )
        }
    }
}

// ── Shared stat row composable ──
@Composable
fun UnitStatRow(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = SubText,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

// ── Blueprint name helper ──
fun blueprintDisplayName(blueprintId: String): String? =
    BlueprintRegistry.instance.findById(blueprintId)?.name

/**
 * painterResource 기반 아이콘 — Android 프레임워크 리소스 캐시 사용.
 */
@Composable
fun CachedIcon(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 30.dp,
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier.size(iconSize),
    )
}

/**
 * Lottie 에셋 애니메이션 — 반복되는 3줄 패턴을 1줄로 축약.
 */
@Composable
fun LottieAsset(
    asset: String,
    modifier: Modifier = Modifier,
    iterations: Int = 1,
) {
    val result = rememberLottieComposition(LottieCompositionSpec.Asset(asset))
    val composition = result.value
    if (result.isFailure || composition == null) return
    val progress by animateLottieCompositionAsState(composition, iterations = iterations)
    LottieAnimation(
        composition,
        progress = { progress },
        modifier = modifier.drawWithContent {
            try {
                drawContent()
            } catch (e: Exception) {
                Log.w("LottieAsset", "Draw failed for $asset: ${e.message}")
            }
        },
    )
}
