package com.jay.jaygame.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.jaygame.data.ProfileDef
import com.jay.jaygame.data.STAGES
import com.jay.jaygame.ui.components.DailyLoginDialog
import com.jay.jaygame.ui.components.NeonButton
import com.jay.jaygame.ui.components.PreBattleDialog
import com.jay.jaygame.ui.components.NeonProgressBar
import com.jay.jaygame.ui.components.ProfileBanner
import com.jay.jaygame.ui.components.StageCardPager
import com.jay.jaygame.ui.theme.*
import com.jay.jaygame.ui.components.LottieAsset
import com.jay.jaygame.ui.viewmodel.HomeViewModel
import com.jay.jaygame.ui.viewmodel.HomeSideEffect
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import kotlin.math.sin
import kotlin.random.Random

// ── Pre-allocated particle colors (avoid GC) ──
private val ParticleGold1 = Color(0x33D4A847)
private val ParticleGold2 = Color(0x26FFE0A0)
private val ParticleWhite = Color(0x1AFFFFFF)
private val ParticleColors = arrayOf(ParticleGold1, ParticleGold2, ParticleWhite)

private class Particle {
    var x: Float = Random.nextFloat()
    var y: Float = Random.nextFloat()
    var speed: Float = 0.015f + Random.nextFloat() * 0.025f
    var amplitude: Float = 0.005f + Random.nextFloat() * 0.015f
    var phase: Float = Random.nextFloat() * 6.2832f
    var radius: Float = 2f + Random.nextFloat() * 4f
    var colorIndex: Int = Random.nextInt(ParticleColors.size)
    var alphaBase: Float = 0.1f + Random.nextFloat() * 0.2f

    fun reset() {
        x = Random.nextFloat()
        y = 1f + Random.nextFloat() * 0.1f
        speed = 0.015f + Random.nextFloat() * 0.025f
        amplitude = 0.005f + Random.nextFloat() * 0.015f
        phase = Random.nextFloat() * 6.2832f
        radius = 2f + Random.nextFloat() * 4f
        colorIndex = Random.nextInt(ParticleColors.size)
        alphaBase = 0.1f + Random.nextFloat() * 0.2f
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartBattle: () -> Unit,
    onNavigateToDungeon: (() -> Unit)? = null,
) {
    val state by viewModel.collectAsState()
    val data = state.gameData
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkDailyLogin()
        viewModel.checkNewTitles()
    }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is HomeSideEffect.LaunchBattle -> onStartBattle()
            is HomeSideEffect.ShowToast ->
                android.widget.Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    if (state.showDailyLogin) {
        DailyLoginDialog(
            data = data,
            onClaim = { viewModel.claimDailyLogin() },
            onDismiss = { viewModel.dismissDailyLogin() },
        )
    }

    state.newTitle?.let { title ->
        NewTitleDialog(
            title = title,
            onDismiss = { viewModel.dismissNewTitle() },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A)),
    ) {
        // Background image with crossfade — Coil handles caching
        Crossfade(
            targetState = data.currentStageId,
            animationSpec = tween(durationMillis = 500),
            label = "bg",
        ) { stageId ->
            val stage = STAGES.getOrNull(stageId)
            if (stage != null) {
                coil.compose.AsyncImage(
                    model = "file:///android_asset/${stage.bgAsset}",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.4f,
                )
            }
        }
        // Dark overlay gradient for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xCC0A0A1A),
                            Color(0x800A0A1A),
                            Color(0xCC0A0A1A),
                        ),
                    ),
                ),
        )

        // ── B1: Floating light particles overlay ──
        val particles = remember { Array(20) { Particle() } }
        var frameTime by remember { mutableLongStateOf(0L) }

        LaunchedEffect(Unit) {
            while (true) {
                withFrameNanos { nanos ->
                    val dt = if (frameTime == 0L) 0.016f else ((nanos - frameTime) / 1_000_000_000f).coerceAtMost(0.05f)
                    frameTime = nanos
                    for (p in particles) {
                        p.y -= p.speed * dt
                        p.phase += 2f * dt
                        p.x += sin(p.phase) * p.amplitude * dt
                        if (p.y < -0.05f) p.reset()
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // frameTime을 읽어서 매 프레임 Canvas 재그리기 트리거
            frameTime
            val w = size.width
            val h = size.height
            for (p in particles) {
                val alpha = p.alphaBase * (0.6f + 0.4f * sin(p.phase * 1.5f).coerceIn(0f, 1f))
                drawCircle(
                    color = ParticleColors[p.colorIndex].copy(alpha = alpha.coerceIn(0.05f, 0.35f)),
                    radius = p.radius * (w / 400f),
                    center = Offset(p.x * w, p.y * h),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Top: Profile Banner ──
            ProfileBanner(
                playerLevel = data.playerLevel,
                trophies = data.trophies,
                gold = data.gold,
                diamonds = data.diamonds,
                totalXP = data.totalXP,
                selectedProfileId = data.selectedProfileId,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.weight(0.3f))

            // ── Center: Stage selector (big & prominent) ──
            StageCardPager(
                currentStageId = data.currentStageId,
                unlockedStages = data.unlockedStages,
                stageBestWaves = data.stageBestWaves,
                difficulty = data.difficulty,
                onStageChanged = { stageId -> viewModel.selectStage(stageId) },
            )

            Spacer(modifier = Modifier.weight(0.5f))

            // ── Bottom: Stamina + Battle button ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Stamina bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "\u26A1 스태미나",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StaminaGreen,
                    )
                    Text(
                        text = "${data.stamina} / ${data.maxStamina}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StaminaGreen.copy(alpha = 0.8f),
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                NeonProgressBar(
                    progress = data.stamina.toFloat() / data.maxStamina.coerceAtLeast(1),
                    barColor = StaminaGreen,
                    height = 10.dp,
                )

                // Season tier mini indicator
                if (data.seasonTier > 0 || data.seasonXP > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "시즌 Tier ${data.seasonTier}",
                            fontSize = 10.sp,
                            color = Gold.copy(alpha = 0.7f),
                        )
                        Text(
                            text = "${data.seasonXP % 100}/100 XP",
                            fontSize = 10.sp,
                            color = SubText,
                        )
                    }
                    NeonProgressBar(
                        progress = data.seasonTierProgress,
                        barColor = Gold,
                        height = 5.dp,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Battle + Dungeon side by side
                run {
                    val isUnlocked = data.currentStageId in data.unlockedStages

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NeonButton(
                            text = "전투 준비",
                            onClick = { viewModel.showPreBattle() },
                            enabled = isUnlocked,
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                            fontSize = 19.sp,
                            accentColor = NeonRed,
                            accentColorDark = NeonRedDark,
                            glowPulse = true,
                        )
                        val dungeonMgr = remember(data) { com.jay.jaygame.engine.DungeonManager(data) }
                        val dungeonRemaining = dungeonMgr.remainingAttempts()
                        NeonButton(
                            text = "던전 ($dungeonRemaining)",
                            onClick = { onNavigateToDungeon?.invoke() },
                            enabled = onNavigateToDungeon != null,
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                            fontSize = 19.sp,
                            accentColor = NeonCyan,
                            accentColorDark = NeonCyan.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Pre-battle dialog overlay (inside main Box for correct z-order)
        if (state.showPreBattle) {
            val stage2 = STAGES.getOrNull(data.currentStageId) ?: STAGES[0]
            val bestWave = data.stageBestWaves.getOrElse(data.currentStageId) { 0 }
            PreBattleDialog(
                stage = stage2,
                bestWave = bestWave,
                selectedDifficulty = data.difficulty,
                staminaCost = stage2.staminaCost,
                hasStamina = data.stamina >= stage2.staminaCost,
                onDifficultySelected = { diff -> viewModel.selectDifficulty(diff) },
                onStartBattle = { viewModel.startBattle(stage2.staminaCost) },
                onDismiss = { viewModel.dismissPreBattle() },
            )
        }
    }
}

// ── 칭호 획득 다이얼로그 ──

private val TitleDialogBg = Color(0xFF16213E)

@Composable
private fun NewTitleDialog(
    title: ProfileDef,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TitleDialogBg, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LottieAsset(
                asset = "lottie/title_unlock.json",
                iterations = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
            )

            Text(
                text = "칭호 획득!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (title.isAnimated) NeonCyan else LightText,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title.description,
                fontSize = 13.sp,
                color = SubText,
            )

            Spacer(modifier = Modifier.height(20.dp))

            NeonButton(
                text = "확인",
                onClick = onDismiss,
                fontSize = 14.sp,
                accentColor = Gold,
                accentColorDark = Gold.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
