package com.example.jaygame.ui.battle

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.example.jaygame.audio.SfxManager
import com.example.jaygame.audio.SoundEvent
import com.example.jaygame.util.HapticManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import com.example.jaygame.BuildConfig
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.bridge.BattleResultData
import com.example.jaygame.data.STAGES
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.screens.ResultScreen
import com.example.jaygame.ui.theme.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sin

@Composable
fun BattleScreen(
    result: BattleResultData?,
    onGoHome: () -> Unit,
) {
    val stageId by BattleBridge.stageId.collectAsState()
    val stage = remember(stageId) { STAGES.getOrNull(stageId) ?: STAGES[0] }
    val context = LocalContext.current
    val view = LocalView.current

    var showMenuDialog by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var showBulkSellDialog by remember { mutableStateOf(false) }
    var showBuySheet by remember { mutableStateOf(false) }
    var showUpgradeSheet by remember { mutableStateOf(false) }

    // Boss vignette
    val battleState by BattleBridge.state.collectAsState()
    val isBoss = battleState.isBossRound
    val bossVignetteAlpha by animateFloatAsState(
        targetValue = if (isBoss) 1f else 0f,
        animationSpec = tween(600),
        label = "bossVignette",
    )
    val bossPulse = if (isBoss) {
        val transition = rememberInfiniteTransition(label = "bossPulse")
        val pulse by transition.animateFloat(
            initialValue = 0.08f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
            label = "bossPulseAlpha",
        )
        pulse
    } else 0f

    val damageEvents by BattleBridge.damageEvents.collectAsState()
    val shakeScope = rememberCoroutineScope()

    // C3: Skill flash for high-grade skills
    val skillEvents by BattleBridge.skillEvents.collectAsState()
    val skillFlashAlpha = remember { Animatable(0f) }
    val skillFlashFamily = remember { mutableStateOf(0) }
    val prevSkillCount = remember { mutableStateOf(0) }
    val currentSkillCount = skillEvents.size
    if (currentSkillCount > prevSkillCount.value) {
        val newEvents = skillEvents.takeLast(currentSkillCount - prevSkillCount.value)
        val highGradeEvent = newEvents.firstOrNull { it.grade >= 3 }
        if (highGradeEvent != null) {
            skillFlashFamily.value = highGradeEvent.family
            shakeScope.launch {
                skillFlashAlpha.snapTo(0f)
                skillFlashAlpha.animateTo(0.3f, animationSpec = tween(100))
                skillFlashAlpha.animateTo(0f, animationSpec = tween(100))
            }
        }
    }
    prevSkillCount.value = currentSkillCount

    // Load background image from assets
    val bgAssetName = remember(stageId) {
        when (stageId) {
            1 -> "bg_jungle"
            2 -> "bg_desert"
            3 -> "bg_glacier"
            4 -> "bg_volcano"
            5 -> "bg_abyss"
            else -> "bg_plains"
        }
    }
    val bgBitmap = remember(bgAssetName) {
        try {
            context.assets.open("backgrounds/$bgAssetName.png").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }

    BackHandler { showMenuDialog = true }

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 0: Full-screen background image
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // Fallback gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(stage.bgColors)
                    ),
            )
        }

        // Layer 1: Game area — square (720x720), 25dp margin each side
        Box(
            modifier = Modifier
                .padding(horizontal = 25.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .align(Alignment.Center),
        ) {
            MonsterPathOverlay()
            EnemyOverlay()
            BattleField()
            ProjectileOverlay()
            DamageNumberOverlay()
            BattleParticleOverlay()
            SkillEffectOverlay()
            WaveAnnouncementOverlay()
            DebugOverlay()

            // C6: Gold coin particle overlay
            GoldCoinOverlay()
            // C7: Level up effect overlay
            LevelUpOverlay()

            // C3: Skill flash overlay
            val flashAlpha = skillFlashAlpha.value
            if (flashAlpha > 0.01f) {
                val flashColor = when (skillFlashFamily.value) {
                    0 -> FlashFireColor
                    1 -> FlashFrostColor
                    2 -> FlashPoisonColor
                    3 -> FlashLightningColor
                    4 -> FlashSupportColor
                    5 -> FlashWindColor
                    else -> FlashDefaultColor
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = flashColor.copy(alpha = flashAlpha),
                        size = size,
                    )
                }
            }

            // Boss red vignette overlay
            if (bossVignetteAlpha > 0.01f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val vigAlpha = bossVignetteAlpha * bossPulse
                    // Radial vignette: transparent center → red edges
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Red.copy(alpha = vigAlpha * 0.5f),
                                Color.Red.copy(alpha = vigAlpha),
                            ),
                            center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2),
                            radius = size.width * 0.7f,
                        ),
                        size = size,
                    )
                }
            }
        }

        // Layer 2: HUD overlays
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BattleTopHud(onPauseClick = { showMenuDialog = true })
            Spacer(modifier = Modifier.weight(1f))
            BattleBottomHud(
                onBuyClick = { showBuySheet = true },
                onUpgradeClick = { showUpgradeSheet = true },
                onBulkSellClick = { showBulkSellDialog = true },
            )
        }

        // Layer 3: Popups
        UnitDetailPopup()
        MergeEffectOverlay()
        SummonEffectOverlay()

        // Layer 3.5: Feature sheets
        if (showBulkSellDialog) {
            BulkSellDialog(onDismiss = { showBulkSellDialog = false })
        }
        if (showBuySheet) {
            BuyUnitSheet(onDismiss = { showBuySheet = false })
        }
        if (showUpgradeSheet) {
            UpgradeSheet(onDismiss = { showUpgradeSheet = false })
        }

        // Layer 4: Result screen with A3 transition
        result?.let { data ->
            BattleResultTransition(
                victory = data.victory,
            ) {
                ResultScreen(
                    victory = data.victory,
                    waveReached = data.waveReached,
                    goldEarned = data.goldEarned,
                    trophyChange = data.trophyChange,
                    killCount = data.killCount,
                    mergeCount = data.mergeCount,
                    cardsEarned = data.cardsEarned,
                    noHpLost = data.noHpLost,
                    fastClear = data.fastClear,
                    onGoHome = onGoHome,
                    onRetry = onGoHome,
                )
            }
        }

        // Layer 5: Menu dialog
        if (showMenuDialog) {
            BattleMenuDialog(
                onDismiss = { showMenuDialog = false },
                onQuitClick = {
                    showMenuDialog = false
                    showQuitDialog = true
                },
            )
        }

        // Layer 6: Quit confirmation
        if (showQuitDialog) {
            QuitBattleDialog(
                onConfirm = onGoHome,
                onDismiss = { showQuitDialog = false },
            )
        }
    }
}

// Pre-allocated C3 skill flash colors (avoid GC)
private val FlashFireColor = Color(0xFFFF8C00)      // orange
private val FlashFrostColor = Color(0xFF64B5F6)     // blue
private val FlashPoisonColor = Color(0xFF66BB6A)    // green
private val FlashLightningColor = Color(0xFFFFD54F) // yellow
private val FlashSupportColor = Color(0xFFCE93D8)   // purple
private val FlashWindColor = Color(0xFF80CBC4)       // teal
private val FlashDefaultColor = Color.White

// Pre-allocated colors for A3 transition Canvas (avoid GC)
private val VictoryGoldBright = Color(0xFFFFD700)
private val VictoryGoldDim = Color(0xFFB8860B)
private val DefeatBlack = Color.Black

/**
 * A3: Battle→Result transition.
 * Victory = expanding gold radial burst, Defeat = darken + fade overlay.
 */
@Composable
private fun BattleResultTransition(
    victory: Boolean,
    content: @Composable () -> Unit,
) {
    val overlayAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val burstProgress = remember { Animatable(0f) }

    LaunchedEffect(victory) {
        // Phase 1: Overlay effect
        if (victory) {
            // Gold burst expanding outward (concurrent animations)
            launch {
                burstProgress.animateTo(
                    1f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                )
            }
            overlayAlpha.animateTo(
                0.7f,
                animationSpec = tween(400),
            )
            // Phase 2: Fade overlay down and show content
            launch {
                overlayAlpha.animateTo(0f, animationSpec = tween(400))
            }
            contentAlpha.animateTo(1f, animationSpec = tween(500))
        } else {
            // Defeat: darken screen
            overlayAlpha.animateTo(
                0.75f,
                animationSpec = tween(600),
            )
            // Then show result content over dark backdrop
            contentAlpha.animateTo(1f, animationSpec = tween(500))
            // Reduce overlay but keep it dark
            overlayAlpha.animateTo(0.4f, animationSpec = tween(300))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Transition overlay
        val oAlpha = overlayAlpha.value
        val burst = burstProgress.value
        if (oAlpha > 0.01f || burst > 0.01f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (victory) {
                    // Gold radial burst
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = max(size.width, size.height) * 0.8f
                    val radius = maxR * burst
                    drawCircle(
                        color = VictoryGoldBright.copy(alpha = oAlpha * 0.6f),
                        radius = radius,
                        center = Offset(cx, cy),
                    )
                    drawCircle(
                        color = VictoryGoldDim.copy(alpha = oAlpha * 0.4f),
                        radius = radius * 0.6f,
                        center = Offset(cx, cy),
                    )
                } else {
                    // Defeat: dark overlay
                    drawRect(
                        color = DefeatBlack.copy(alpha = oAlpha),
                        size = size,
                    )
                }
            }
        }

        // Result content fading in
        Box(modifier = Modifier.fillMaxSize().alpha(contentAlpha.value)) {
            content()
        }
    }
}

@Composable
private fun QuitBattleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.8f),
                    ),
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        GameCard(
            modifier = Modifier
                .width(280.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
            borderColor = Gold.copy(alpha = 0.5f),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "전투 포기",
                    color = Gold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "전투를 포기하시겠습니까?\n보상을 받을 수 없습니다.",
                    color = LightText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NeonButton(
                        text = "취소",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(40.dp),
                        accentColor = SubText,
                        accentColorDark = DimText,
                    )
                    NeonButton(
                        text = "포기",
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(40.dp),
                        accentColor = NeonRed,
                        accentColorDark = NeonRedDark,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// ── Battle Menu Dialog ──────────────────────────────────────────

private val MenuSpeedX1Color = Color.White
private val MenuSpeedX2Color = Color(0xFFFFD700)
private val MenuSpeedX4Color = Color(0xFFFF6B6B)
private val MenuSpeedX8Color = Color(0xFFFF3333)

@Composable
private fun BattleMenuDialog(
    onDismiss: () -> Unit,
    onQuitClick: () -> Unit,
) {
    val battleSpeed by BattleBridge.battleSpeed.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.8f),
                    ),
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        GameCard(
            modifier = Modifier
                .width(280.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
            borderColor = Gold.copy(alpha = 0.5f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "메뉴",
                    color = Gold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                // ── Speed control ──
                Text(
                    text = "배속",
                    color = LightText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(1f to "x1", 2f to "x2", 4f to "x4", 8f to "x8").forEach { (speed, label) ->
                        val isSelected = battleSpeed == speed
                        val color = when (speed) {
                            2f -> MenuSpeedX2Color
                            4f -> MenuSpeedX4Color
                            8f -> MenuSpeedX8Color
                            else -> MenuSpeedX1Color
                        }
                        NeonButton(
                            text = label,
                            onClick = { BattleBridge.setBattleSpeed(speed) },
                            modifier = Modifier.weight(1f).height(38.dp),
                            accentColor = if (isSelected) color else SubText,
                            accentColorDark = if (isSelected) color.copy(alpha = 0.7f) else DimText,
                        )
                    }
                }

                // ── Debug toggle (debug builds only) ──
                if (BuildConfig.DEBUG) {
                    val isDebugOn by BattleBridge.debugMode.collectAsState()
                    NeonButton(
                        text = if (isDebugOn) "디버그 OFF" else "디버그 ON",
                        onClick = { BattleBridge.toggleDebugMode() },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        accentColor = if (isDebugOn) NeonGreen else SubText,
                        accentColorDark = if (isDebugOn) NeonGreen.copy(alpha = 0.7f) else DimText,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── Quit button ──
                NeonButton(
                    text = "전투 포기",
                    onClick = onQuitClick,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    accentColor = NeonRed,
                    accentColorDark = NeonRedDark,
                )

                // ── Close ──
                NeonButton(
                    text = "닫기",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    accentColor = SubText,
                    accentColorDark = DimText,
                )
            }
        }
    }
}

// ── C6: Gold Coin Particle Overlay ──────────────────────────────

private val GoldCoinColor = Color(0xFFFFD700)
private val GoldCoinHighlight = Color(0xFFFFF176)

private class GoldCoinParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
)

@Composable
private fun GoldCoinOverlay() {
    val goldEvents by BattleBridge.goldPickupEvents.collectAsState()
    val particles = remember { mutableListOf<GoldCoinParticle>() }
    val prevCount = remember { mutableStateOf(0) }

    val currentCount = goldEvents.size
    if (currentCount > prevCount.value) {
        val newEvents = goldEvents.takeLast(currentCount - prevCount.value)
        for (event in newEvents) {
            if (particles.size >= 150) break
            // Spawn 3-5 coin particles per gold event
            val count = (3 + (event.amount.coerceAtMost(3)))
            for (i in 0 until count) {
                if (particles.size >= 150) break
                // Target: top-right (where gold HUD is, ~0.9, 0.05)
                val targetX = 0.9f
                val targetY = 0.05f
                val dx = targetX - event.x
                val dy = targetY - event.y
                val speed = 0.8f + i * 0.15f
                val jitterX = sin(i.toFloat() * 2.1f) * 0.05f
                val jitterY = kotlin.math.cos(i.toFloat() * 1.7f) * 0.03f
                particles.add(GoldCoinParticle(
                    x = event.x + jitterX * 0.5f,
                    y = event.y + jitterY * 0.5f,
                    vx = dx * speed + jitterX,
                    vy = dy * speed + jitterY,
                    life = 0.5f + i * 0.08f,
                    maxLife = 0.5f + i * 0.08f,
                ))
            }
        }
    }
    prevCount.value = currentCount

    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { _ ->
                val dt = 1f / 60f
                val iter = particles.iterator()
                while (iter.hasNext()) {
                    val p = iter.next()
                    p.life -= dt
                    if (p.life <= 0f) { iter.remove(); continue }
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                }
            }
        }
    }

    if (particles.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (p in particles) {
            val lifeFrac = (p.life / p.maxLife).coerceIn(0f, 1f)
            val alpha = lifeFrac.coerceIn(0f, 1f)
            val coinSize = 3f + lifeFrac * 2f
            val screenX = p.x * w
            val screenY = p.y * h
            // Gold coin body
            drawCircle(
                color = GoldCoinColor.copy(alpha = alpha * 0.9f),
                radius = coinSize,
                center = Offset(screenX, screenY),
            )
            // Highlight
            drawCircle(
                color = GoldCoinHighlight.copy(alpha = alpha * 0.5f),
                radius = coinSize * 0.5f,
                center = Offset(screenX - coinSize * 0.2f, screenY - coinSize * 0.2f),
            )
        }
    }
}

// ── C7: Level Up Effect Overlay ─────────────────────────────────

private val LevelUpBeamColor = Color(0xFFFFFFCC)
private val LevelUpBeamEdge = Color(0xFFFFD700)
// D4: Star particle colors (pre-allocated to avoid GC)
private val StarGoldBright = Color(0xFFFFD700)
private val StarGoldLight = Color(0xFFFFF176)
private val StarGoldDim = Color(0xFFFFAB00)
private val StarWhiteCore = Color(0xFFFFFFEE)

// D4: Pre-computed star burst angles (12 stars)
private val StarAngles = FloatArray(12) { i -> (i.toFloat() / 12f) * 2f * kotlin.math.PI.toFloat() }
private val StarSpeedVariations = floatArrayOf(1.0f, 0.8f, 1.2f, 0.9f, 1.1f, 0.75f, 1.15f, 0.85f, 1.05f, 0.95f, 1.25f, 0.7f)

@Composable
private fun LevelUpOverlay() {
    val levelUpEvents by BattleBridge.levelUpEvents.collectAsState()

    if (levelUpEvents.isEmpty()) return

    val now = System.currentTimeMillis()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (event in levelUpEvents) {
            val elapsed = (now - event.timestamp) / 1000f
            if (elapsed > 1.2f) continue

            val cx = event.x * w
            val cy = event.y * h
            val progress = (elapsed / 1.2f).coerceIn(0f, 1f)

            // Phase 1 (0-0.3): beam expands
            // Phase 2 (0.3-0.7): beam steady
            // Phase 3 (0.7-1.2): beam fades out
            val beamAlpha = when {
                progress < 0.25f -> progress / 0.25f * 0.6f
                progress < 0.58f -> 0.6f
                else -> (1f - progress) / 0.42f * 0.6f
            }
            val beamWidth = when {
                progress < 0.25f -> 8f + progress / 0.25f * 16f
                else -> 24f * (1f - (progress - 0.25f) * 0.3f)
            }

            // Light beam (vertical pillar)
            val beamTop = (cy - 120f).coerceAtLeast(0f)
            drawRect(
                color = LevelUpBeamColor.copy(alpha = beamAlpha * 0.4f),
                topLeft = Offset(cx - beamWidth / 2f, beamTop),
                size = androidx.compose.ui.geometry.Size(beamWidth, cy - beamTop),
            )
            // Beam edges
            drawRect(
                color = LevelUpBeamEdge.copy(alpha = beamAlpha * 0.6f),
                topLeft = Offset(cx - beamWidth / 2f, beamTop),
                size = androidx.compose.ui.geometry.Size(2f, cy - beamTop),
            )
            drawRect(
                color = LevelUpBeamEdge.copy(alpha = beamAlpha * 0.6f),
                topLeft = Offset(cx + beamWidth / 2f - 2f, beamTop),
                size = androidx.compose.ui.geometry.Size(2f, cy - beamTop),
            )

            // Base glow
            drawCircle(
                color = LevelUpBeamColor.copy(alpha = beamAlpha * 0.5f),
                radius = beamWidth * 1.2f,
                center = Offset(cx, cy),
            )

            // D4: Golden star particles bursting outward
            val starProgress = ((elapsed - 0.1f) / 0.9f).coerceIn(0f, 1f)
            if (starProgress > 0f) {
                val starAlpha = when {
                    starProgress < 0.2f -> starProgress / 0.2f
                    starProgress < 0.5f -> 1f
                    else -> (1f - starProgress) / 0.5f
                }.coerceIn(0f, 1f)

                for (i in StarAngles.indices) {
                    val angle = StarAngles[i]
                    val speed = StarSpeedVariations[i]
                    val dist = starProgress * 50f * speed
                    val starX = cx + kotlin.math.cos(angle) * dist
                    val starY = cy + kotlin.math.sin(angle) * dist - starProgress * 15f * speed

                    val size = (4f - starProgress * 2.5f).coerceAtLeast(0.5f)

                    // Star core (bright)
                    drawCircle(
                        color = StarWhiteCore.copy(alpha = starAlpha * 0.9f),
                        radius = size * 0.5f,
                        center = Offset(starX, starY),
                    )
                    // Star glow (gold)
                    drawCircle(
                        color = StarGoldBright.copy(alpha = starAlpha * 0.7f),
                        radius = size,
                        center = Offset(starX, starY),
                    )
                    // Star cross lines (4-point star shape)
                    val armLen = size * 1.5f
                    val crossAlpha = starAlpha * 0.5f
                    drawLine(
                        color = StarGoldLight.copy(alpha = crossAlpha),
                        start = Offset(starX - armLen, starY),
                        end = Offset(starX + armLen, starY),
                        strokeWidth = 1f,
                    )
                    drawLine(
                        color = StarGoldLight.copy(alpha = crossAlpha),
                        start = Offset(starX, starY - armLen),
                        end = Offset(starX, starY + armLen),
                        strokeWidth = 1f,
                    )
                }

                // Secondary smaller sparkles (6 more, offset timing)
                val sparkDelay = ((elapsed - 0.2f) / 0.8f).coerceIn(0f, 1f)
                if (sparkDelay > 0f) {
                    val sparkAlpha = when {
                        sparkDelay < 0.3f -> sparkDelay / 0.3f
                        else -> (1f - sparkDelay) / 0.7f
                    }.coerceIn(0f, 1f)

                    for (i in 0 until 6) {
                        val angle = StarAngles[i * 2] + 0.26f // offset from main stars
                        val dist = sparkDelay * 35f * StarSpeedVariations[i]
                        val sx = cx + kotlin.math.cos(angle) * dist
                        val sy = cy + kotlin.math.sin(angle) * dist - sparkDelay * 10f

                        drawCircle(
                            color = StarGoldDim.copy(alpha = sparkAlpha * 0.6f),
                            radius = 1.5f,
                            center = Offset(sx, sy),
                        )
                    }
                }
            }

            // "LV UP!" text using drawContext
            val textAlpha = when {
                progress < 0.15f -> progress / 0.15f
                progress < 0.7f -> 1f
                else -> (1f - progress) / 0.3f
            }.coerceIn(0f, 1f)
            val textY = cy - 30f - progress * 20f
            // Use native text paint
            drawContext.canvas.nativeCanvas.drawText(
                "LV UP!",
                cx,
                textY,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(
                        (textAlpha * 255).toInt(),
                        0xFF, 0xD7, 0x00,
                    )
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                },
            )
        }
    }

    // Force recomposition while events are active
    LaunchedEffect(levelUpEvents) {
        while (true) {
            val hasActive = BattleBridge.levelUpEvents.value.any {
                (System.currentTimeMillis() - it.timestamp) < 1200L
            }
            if (!hasActive) break
            androidx.compose.runtime.withFrameNanos { }
        }
    }
}
