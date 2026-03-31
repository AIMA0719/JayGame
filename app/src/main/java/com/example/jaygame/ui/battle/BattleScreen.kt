package com.example.jaygame.ui.battle

import com.example.jaygame.R
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.bridge.BattleResultData
import com.example.jaygame.data.STAGES
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.screens.ResultScreen
import com.example.jaygame.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sin

@Composable
fun BattleScreen(
    result: BattleResultData?,
    onGoHome: () -> Unit,
    bgmEnabled: Boolean = true,
    sfxEnabled: Boolean = true,
    onToggleBgm: () -> Unit = {},
    onToggleSfx: () -> Unit = {},
) {
    val stageId by BattleBridge.stageId.collectAsState()
    val stage = remember(stageId) { STAGES.getOrNull(stageId) ?: STAGES[0] }
    val context = LocalContext.current

    var showMenuDialog by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var savedSpeed by remember { mutableFloatStateOf(2f) }
    var showBulkSellDialog by remember { mutableStateOf(false) }
    var showGambleDialog by remember { mutableStateOf(false) }
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

    // ParticleLOD: commit(이전 프레임 add 누적값으로 LOD 계산) → reset(새 프레임 카운트 시작) → 오버레이 Canvas에서 add
    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { _ ->
                val speed = BattleBridge.battleSpeed.value
                ParticleLOD.commitFrame(speed)
                ParticleLOD.resetFrame()
            }
        }
    }

    BackHandler {
        savedSpeed = BattleBridge.battleSpeed.value
        BattleBridge.setBattleSpeed(0f)
        showMenuDialog = true
    }

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
        // Track field position for the unclipped skill VFX overlay
        var fieldOffset by remember { mutableStateOf(Offset.Zero) }
        var fieldSizePx by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
        Box(
            modifier = Modifier
                .padding(horizontal = 25.dp)
                .fillMaxWidth()
                .aspectRatio(720f / 1280f)
                .align(Alignment.Center)
                .clipToBounds()
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    fieldOffset = Offset(pos.x, pos.y)
                    fieldSizePx = androidx.compose.ui.geometry.Size(
                        coords.size.width.toFloat(), coords.size.height.toFloat()
                    )
                },
        ) {
            MonsterPathOverlay()
            ZoneGroundOverlay()
            EnemyOverlay()
            BattleField()
            ProjectileOverlay()
            MeleeHitOverlay()
            DamageNumberOverlay()
            BattleParticleOverlay()
            WaveAnnouncementOverlay()
            DebugOverlay()

            // C6: Gold coin particle overlay
            GoldCoinOverlay()
            // C7: Level up effect overlay
            LevelUpOverlay()

        }

        // ── 전체 화면 이펙트 (게임 캔버스 밖, 패딩/마진 무시) ──

        // Skill VFX — sibling of the clipped field so effects can extend beyond the field boundary
        SkillEffectOverlay(fieldOffset = fieldOffset, fieldSize = fieldSizePx)

        // Boss red vignette overlay (화면 전체)
        if (bossVignetteAlpha > 0.01f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val vigAlpha = bossVignetteAlpha * bossPulse
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Red.copy(alpha = vigAlpha * 0.5f),
                            Color.Red.copy(alpha = vigAlpha),
                        ),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2),
                        radius = size.maxDimension * 0.7f,
                    ),
                    size = size,
                )
            }
        }

        // Layer 2: HUD overlays
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BattleTopHud(onPauseClick = {
                savedSpeed = BattleBridge.battleSpeed.value
                BattleBridge.setBattleSpeed(0f)
                showMenuDialog = true
            })
            Spacer(modifier = Modifier.weight(1f))
            BattleBottomHud(
                onBulkSellClick = { showBulkSellDialog = true },
                onGambleClick = { showGambleDialog = true },
                onUpgradeClick = { showUpgradeSheet = true },
            )
        }

        // Layer 2.5: Pet overlay (floating pets)
        PetBattleOverlay()

        // Layer 3: Popups
        UnitDetailPopup()
        MergeEffectOverlay()
        SummonEffectOverlay()

        // Layer 3.5: Feature sheets
        if (showBulkSellDialog) {
            BulkSellDialog(onDismiss = { showBulkSellDialog = false })
        }
        if (showGambleDialog) {
            GambleDialog(onDismiss = { showGambleDialog = false })
        }
        if (showUpgradeSheet) {
            UpgradeSheet(onDismiss = { showUpgradeSheet = false })
        }

        // Layer 4: Result screen with A3 transition
        result?.let { data ->
            val dungeonId by BattleBridge.dungeonId.collectAsState()
            BattleResultTransition(
                victory = data.victory,
            ) {
                val dDef = if (dungeonId >= 0) com.example.jaygame.data.ALL_DUNGEONS.getOrNull(dungeonId) else null
                val displayGold = if (dDef != null) (data.goldEarned * dDef.rewardMultiplier).toInt() else data.goldEarned
                val displayTrophy = if (dDef != null) 0 else data.trophyChange

                ResultScreen(
                    victory = data.victory,
                    waveReached = data.waveReached,
                    goldEarned = displayGold,
                    trophyChange = displayTrophy,
                    killCount = data.killCount,
                    mergeCount = data.mergeCount,
                    cardsEarned = data.cardsEarned,
                    noHpLost = data.noHpLost,
                    fastClear = data.fastClear,
                    relicDropId = data.relicDropId,
                    relicDropGrade = data.relicDropGrade,
                    onGoHome = onGoHome,
                    onRetry = onGoHome,
                )
            }
        }

        // Layer 5: Menu dialog (pauses game)
        if (showMenuDialog) {
            BattleMenuDialog(
                onDismiss = {
                    showMenuDialog = false
                    // Resume: if user changed speed in menu, use that; otherwise restore saved
                    val menuSpeed = BattleBridge.battleSpeed.value
                    if (menuSpeed == 0f) {
                        BattleBridge.setBattleSpeed(savedSpeed)
                    }
                    // else user already set a new speed via menu controls
                },
                onQuitClick = {
                    showMenuDialog = false
                    showQuitDialog = true
                },
                bgmEnabled = bgmEnabled,
                sfxEnabled = sfxEnabled,
                onToggleBgm = onToggleBgm,
                onToggleSfx = onToggleSfx,
            )
        }

        // Layer 6: Quit confirmation
        if (showQuitDialog) {
            QuitBattleDialog(
                onConfirm = onGoHome,
                onDismiss = {
                    showQuitDialog = false
                    // 포기 취소 시 일시정지 해제 — 메뉴 onDismiss를 거치지 않았으므로 여기서 속도 복원
                    if (BattleBridge.battleSpeed.value == 0f) {
                        BattleBridge.setBattleSpeed(savedSpeed)
                    }
                },
            )
        }

        // Layer 7: Tutorial hints (first battle only)
        val isTutorial by BattleBridge.tutorialMode.collectAsState()
        if (isTutorial && result == null) {
            TutorialHintOverlay()
        }
    }
}

// ─── Zone Ground Overlay ─────────────────────────────────────

private val ZoneFireColor = Color(0xFFFF6B35)
private val ZoneFrostColor = Color(0xFF64B5F6)
private val ZonePoisonColor = Color(0xFF81C784)
private val ZoneLightningColor = Color(0xFFFFD54F)
private val ZoneSupportColor = Color(0xFFCE93D8)
private val ZoneWindColor = Color(0xFF80CBC4)

private fun zoneColor(family: Int): Color = when (family) {
    0 -> ZoneFireColor
    1 -> ZoneFrostColor
    2 -> ZonePoisonColor
    3 -> ZoneLightningColor
    4 -> ZoneSupportColor
    5 -> ZoneWindColor
    else -> Color.White
}

@Composable
private fun ZoneGroundOverlay() {
    val zones by BattleBridge.zoneData.collectAsState()
    if (zones.count == 0) return

    val infiniteTransition = rememberInfiniteTransition(label = "zoneFx")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2832f, // 2π
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "zoneTick",
    )

    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
        val w = size.width
        val h = size.height
        val tickDeg = tick * 57.2958f // radians to degrees, cached once
        for (i in 0 until zones.count) {
            val cx = zones.xs[i] * w
            val cy = zones.ys[i] * h
            val radius = zones.radii[i] * w
            val color = zoneColor(zones.families[i])
            val progress = if (i < zones.progresses.size) zones.progresses[i] else 1f

            val fadeAlpha = if (progress < 0.2f) progress / 0.2f else 1f

            // Pulsing inner glow
            val pulseScale = 1f + kotlin.math.sin(tick * 2f) * 0.04f
            drawCircle(
                color = color,
                alpha = 0.12f * fadeAlpha,
                radius = radius * pulseScale,
                center = Offset(cx, cy),
            )

            // Secondary pulse layer (offset phase)
            drawCircle(
                color = color,
                alpha = 0.07f * fadeAlpha,
                radius = radius * (0.85f + kotlin.math.sin(tick * 2f + 2f) * 0.05f),
                center = Offset(cx, cy),
            )

            // Animated border ring — dashed effect via rotating arc segments
            val borderAlpha = 0.4f * fadeAlpha
            val segmentSweep = 37f // (360 - 8*8) / 8
            for (s in 0 until 8) {
                drawArc(
                    color = color,
                    alpha = borderAlpha,
                    startAngle = s * 45f + tickDeg * 0.3f,
                    sweepAngle = segmentSweep,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 2.5f),
                )
            }

            // Inner ring (rotating opposite direction)
            val innerRadius = radius * 0.7f
            for (s in 0 until 6) {
                drawArc(
                    color = color,
                    alpha = 0.2f * fadeAlpha,
                    startAngle = s * 60f - tickDeg * 0.5f,
                    sweepAngle = 40f,
                    useCenter = false,
                    topLeft = Offset(cx - innerRadius, cy - innerRadius),
                    size = Size(innerRadius * 2, innerRadius * 2),
                    style = Stroke(width = 1.5f),
                )
            }

            // Floating particles orbiting the zone edge
            for (p in 0 until 6) {
                val angle = tick + p * 1.0472f // 2π/6
                val orbitRadius = radius * (0.9f + kotlin.math.sin(tick * 3f + p * 1.5f) * 0.08f)
                val px = cx + kotlin.math.cos(angle) * orbitRadius
                val py = cy + kotlin.math.sin(angle) * orbitRadius
                val pAlpha = (0.5f + kotlin.math.sin(tick * 4f + p * 2f) * 0.3f) * fadeAlpha
                drawCircle(
                    color = color,
                    alpha = pAlpha.toFloat(),
                    radius = 3f,
                    center = Offset(px.toFloat(), py.toFloat()),
                )
            }

            // Center symbol glow
            val centerAlpha = (0.3f + kotlin.math.sin(tick * 1.5f) * 0.15f) * fadeAlpha
            drawCircle(
                color = color,
                alpha = centerAlpha,
                radius = 6f,
                center = Offset(cx, cy),
            )
        }
    }
}

/** Tutorial hint overlay — shows step-by-step tips on first battle */
@Composable
private fun TutorialHintOverlay() {
    val battle by BattleBridge.state.collectAsState()
    val unitCount by remember { derivedStateOf { BattleBridge.unitPositions.value.count } }

    // Boss hint: show for 3 seconds then hide
    var showBossHint by remember { mutableStateOf(false) }
    var lastBossWave by remember { mutableStateOf(-1) }
    LaunchedEffect(battle.isBossRound, battle.currentWave) {
        if (battle.isBossRound && battle.currentWave != lastBossWave) {
            lastBossWave = battle.currentWave
            showBossHint = true
            kotlinx.coroutines.delay(3000)
            showBossHint = false
        } else if (!battle.isBossRound) {
            showBossHint = false
        }
    }

    // Determine which hint to show based on game state
    val hintText = when {
        battle.currentWave == 0 && unitCount == 0 -> "하단의 소환 버튼을 탭하여 유닛을 배치하세요!"
        unitCount in 1..3 -> "유닛을 더 소환하세요. 같은 등급 4개를 합성할 수 있습니다!"
        battle.currentWave == 0 && unitCount >= 3 -> "적이 곧 나타납니다. 유닛이 자동으로 공격합니다."
        battle.currentWave in 1..2 && battle.sp > 80 -> "SP가 충분합니다! 유닛을 더 소환하세요."
        showBossHint -> "보스 웨이브! 제한 시간 내에 처치하세요!"
        else -> null
    }

    if (hintText != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 120.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = hintText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

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
                        color = VictoryGoldBright,
                        alpha = oAlpha * 0.6f,
                        radius = radius,
                        center = Offset(cx, cy),
                    )
                    drawCircle(
                        color = VictoryGoldDim,
                        alpha = oAlpha * 0.4f,
                        radius = radius * 0.6f,
                        center = Offset(cx, cy),
                    )
                } else {
                    // Defeat: dark overlay
                    drawRect(
                        color = DefeatBlack,
                        alpha = oAlpha,
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

private val MenuSpeedX1Color = Color(0xFF4FC3F7)  // 밝은 파랑
private val MenuSpeedX2Color = Color(0xFFFFD700)   // 골드

@Composable
private fun BattleMenuDialog(
    onDismiss: () -> Unit,
    onQuitClick: () -> Unit,
    bgmEnabled: Boolean,
    sfxEnabled: Boolean,
    onToggleBgm: () -> Unit,
    onToggleSfx: () -> Unit,
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
                    listOf(2f to "x1", 4f to "x2").forEach { (speed, label) ->
                        val isSelected = battleSpeed == speed
                        val color = when (speed) {
                            4f -> MenuSpeedX2Color
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

                // ── Sound controls ──
                HorizontalDivider(color = Divider, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("배경음", fontSize = 14.sp, color = LightText)
                    Spacer(Modifier.weight(1f))
                    NeonButton(
                        text = if (bgmEnabled) "ON" else "OFF",
                        onClick = onToggleBgm,
                        modifier = Modifier.width(64.dp).height(34.dp),
                        fontSize = 13.sp,
                        accentColor = if (bgmEnabled) NeonGreen else NeonRed,
                        accentColorDark = if (bgmEnabled) NeonGreen.copy(alpha = 0.6f) else NeonRedDark,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("효과음", fontSize = 14.sp, color = LightText)
                    Spacer(Modifier.weight(1f))
                    NeonButton(
                        text = if (sfxEnabled) "ON" else "OFF",
                        onClick = onToggleSfx,
                        modifier = Modifier.width(64.dp).height(34.dp),
                        fontSize = 13.sp,
                        accentColor = if (sfxEnabled) NeonGreen else NeonRed,
                        accentColorDark = if (sfxEnabled) NeonGreen.copy(alpha = 0.6f) else NeonRedDark,
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
        var lastFrameNanos = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { nanos ->
                val dt = if (lastFrameNanos == 0L) 1f / 60f
                         else ((nanos - lastFrameNanos) / 1_000_000_000f).coerceAtMost(0.05f)
                lastFrameNanos = nanos
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
                color = GoldCoinColor,
                alpha = alpha * 0.9f,
                radius = coinSize,
                center = Offset(screenX, screenY),
            )
            // Highlight
            drawCircle(
                color = GoldCoinHighlight,
                alpha = alpha * 0.5f,
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

// Pre-allocated Paint for LV UP text (avoids per-frame allocation)
private val lvUpPaint = android.graphics.Paint().apply {
    textSize = 24f
    textAlign = android.graphics.Paint.Align.CENTER
    isFakeBoldText = true
    setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
}

@Composable
private fun LevelUpOverlay() {
    val levelUpEvents by BattleBridge.levelUpEvents.collectAsState()

    if (levelUpEvents.isEmpty()) return

    val context = androidx.compose.ui.platform.LocalContext.current
    val levelUpImgBitmap = remember { decodeScaledBitmap(context, R.drawable.vfx_levelup, 96)!! }

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
                color = LevelUpBeamColor,
                alpha = beamAlpha * 0.4f,
                topLeft = Offset(cx - beamWidth / 2f, beamTop),
                size = androidx.compose.ui.geometry.Size(beamWidth, cy - beamTop),
            )
            // Beam edges
            drawRect(
                color = LevelUpBeamEdge,
                alpha = beamAlpha * 0.6f,
                topLeft = Offset(cx - beamWidth / 2f, beamTop),
                size = androidx.compose.ui.geometry.Size(2f, cy - beamTop),
            )
            drawRect(
                color = LevelUpBeamEdge,
                alpha = beamAlpha * 0.6f,
                topLeft = Offset(cx + beamWidth / 2f - 2f, beamTop),
                size = androidx.compose.ui.geometry.Size(2f, cy - beamTop),
            )

            // Base glow
            drawCircle(
                color = LevelUpBeamColor,
                alpha = beamAlpha * 0.5f,
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
                        color = StarWhiteCore,
                        alpha = starAlpha * 0.9f,
                        radius = size * 0.5f,
                        center = Offset(starX, starY),
                    )
                    // Star glow (gold)
                    drawCircle(
                        color = StarGoldBright,
                        alpha = starAlpha * 0.7f,
                        radius = size,
                        center = Offset(starX, starY),
                    )
                    // Star cross lines (4-point star shape)
                    val armLen = size * 1.5f
                    val crossAlpha = starAlpha * 0.5f
                    drawLine(
                        color = StarGoldLight,
                        alpha = crossAlpha,
                        start = Offset(starX - armLen, starY),
                        end = Offset(starX + armLen, starY),
                        strokeWidth = 1f,
                    )
                    drawLine(
                        color = StarGoldLight,
                        alpha = crossAlpha,
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
                            color = StarGoldDim,
                            alpha = sparkAlpha * 0.6f,
                            radius = 1.5f,
                            center = Offset(sx, sy),
                        )
                    }
                }
            }

            // 레벨업 이미지 스프라이트 (비컨 + 금색 기둥)
            val imgAlpha = beamAlpha * 1.2f
            val imgSize = (60f + progress * 20f).toInt().coerceAtLeast(1)
            val imgHalf = imgSize / 2f
            drawImage(
                image = levelUpImgBitmap,
                dstOffset = IntOffset(
                    (cx - imgHalf).toInt(),
                    (cy - imgHalf - 10f).toInt(),
                ),
                dstSize = IntSize(imgSize, imgSize),
                alpha = imgAlpha.coerceIn(0f, 1f),
                blendMode = BlendMode.Screen,
            )

            // "LV UP!" text using drawContext
            val textAlpha = when {
                progress < 0.15f -> progress / 0.15f
                progress < 0.7f -> 1f
                else -> (1f - progress) / 0.3f
            }.coerceIn(0f, 1f)
            val textY = cy - 30f - progress * 20f
            // Use native text paint
            lvUpPaint.color = android.graphics.Color.argb(
                (textAlpha * 255).toInt(),
                0xFF, 0xD7, 0x00,
            )
            drawContext.canvas.nativeCanvas.drawText(
                "LV UP!",
                cx,
                textY,
                lvUpPaint,
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

// ── Pet Battle Overlay: 장착된 펫 표시 (둥실둥실 + 클릭 다이얼로그) ──

@Composable
fun PetBattleOverlay() {
    val equippedPetIds by BattleBridge.equippedPetIds.collectAsState()
    if (equippedPetIds.isEmpty()) return

    val context = LocalContext.current
    var selectedPetId by remember { mutableStateOf<Int?>(null) }

    // 둥실둥실 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "petFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "petFloatOffset",
    )

    val petBitmaps = remember(equippedPetIds) {
        equippedPetIds.associateWith { petId ->
            try {
                ContextCompat.getDrawable(context, com.example.jaygame.ui.screens.petIconRes(petId))
                    ?.toBitmap(96, 96)
                    ?.asImageBitmap()
            } catch (_: Exception) { null }
        }
    }

    // 화면 우측 상단에 세로로 배치
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp, end = 8.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            equippedPetIds.forEachIndexed { index, petId ->
                val pet = com.example.jaygame.data.ALL_PETS.find { it.id == petId } ?: return@forEachIndexed
                val petFloat = floatOffset * if (index % 2 == 0) 1f else -1f
                val gradeColor = Color(pet.grade.colorHex)

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .offset { IntOffset(0, petFloat.toInt()) }
                        .background(gradeColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { selectedPetId = petId },
                    contentAlignment = Alignment.Center,
                ) {
                    val bitmap = petBitmaps[petId]
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = pet.name,
                            modifier = Modifier.size(42.dp),
                        )
                    } else {
                        Text(
                            text = pet.name.take(1),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    // 펫 스킬 다이얼로그
    selectedPetId?.let { petId ->
        PetSkillDialog(petId = petId, onDismiss = { selectedPetId = null })
    }
}

@Composable
private fun PetSkillDialog(petId: Int, onDismiss: () -> Unit) {
    val pet = remember(petId) { com.example.jaygame.data.ALL_PETS.find { it.id == petId } } ?: return
    val gradeColor = Color(pet.grade.colorHex)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A2E))
                .padding(20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 펫 이름 + 등급
                Text(
                    text = pet.name,
                    color = gradeColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = pet.grade.label,
                    color = gradeColor.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                )

                HorizontalDivider(color = gradeColor.copy(alpha = 0.3f))

                // 스킬 이름
                Text(
                    text = pet.skillName,
                    color = Color(0xFFFFD700),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                // 스킬 설명
                Text(
                    text = pet.skillDescription,
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )

                // 타입 + 쿨다운
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val typeText = when (pet.category) {
                        com.example.jaygame.data.PetCategory.ATTACK -> "공격"
                        com.example.jaygame.data.PetCategory.SUPPORT -> "지원"
                        com.example.jaygame.data.PetCategory.UTILITY -> "유틸"
                    }
                    Text(text = typeText, color = Color(0xFF90A4AE), fontSize = 12.sp)
                    if (pet.isPassive) {
                        Text(text = "패시브", color = Color(0xFF81C784), fontSize = 12.sp)
                    } else {
                        Text(text = "쿨타임 ${pet.cooldown.toInt()}초", color = Color(0xFF90A4AE), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                NeonButton(text = "닫기", onClick = onDismiss)
            }
        }
    }
}
