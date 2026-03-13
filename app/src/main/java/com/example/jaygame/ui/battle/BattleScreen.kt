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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.bridge.BattleResultData
import com.example.jaygame.data.STAGES
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.screens.ResultScreen
import com.example.jaygame.ui.theme.*

@Composable
fun BattleScreen(
    result: BattleResultData?,
    onGoHome: () -> Unit,
) {
    val stageId by BattleBridge.stageId.collectAsState()
    val stage = remember(stageId) { STAGES.getOrNull(stageId) ?: STAGES[0] }
    val context = LocalContext.current

    var showQuitDialog by remember { mutableStateOf(false) }
    var showGambleDialog by remember { mutableStateOf(false) }
    var showBuySheet by remember { mutableStateOf(false) }
    var showUpgradeSheet by remember { mutableStateOf(false) }

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

    BackHandler { showQuitDialog = true }

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

        // Layer 1: Game area matching C++ 1280x720 aspect ratio
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1280f / 720f)
                .align(Alignment.Center),
        ) {
            MonsterPathOverlay()
            EnemyOverlay()
            BattleField()
            ProjectileOverlay()
            DamageNumberOverlay()
            BattleParticleOverlay()
            WaveAnnouncementOverlay()
        }

        // Layer 2: HUD overlays
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BattleTopHud(onPauseClick = { showQuitDialog = true })
            Spacer(modifier = Modifier.weight(1f))
            BattleBottomHud(
                onGambleClick = { showGambleDialog = true },
                onBuyClick = { showBuySheet = true },
                onUpgradeClick = { showUpgradeSheet = true },
            )
        }

        // Layer 3: Popups
        UnitDetailPopup()
        MergeEffectOverlay()
        SummonEffectOverlay()

        // Layer 3.5: Feature sheets
        if (showGambleDialog) {
            GambleDialog(onDismiss = { showGambleDialog = false })
        }
        if (showBuySheet) {
            BuyUnitSheet(onDismiss = { showBuySheet = false })
        }
        if (showUpgradeSheet) {
            UpgradeSheet(onDismiss = { showUpgradeSheet = false })
        }

        // Layer 4: Result screen
        result?.let { data ->
            ResultScreen(
                victory = data.victory,
                waveReached = data.waveReached,
                goldEarned = data.goldEarned,
                trophyChange = data.trophyChange,
                killCount = data.killCount,
                mergeCount = data.mergeCount,
                onGoHome = onGoHome,
            )
        }

        // Layer 5: Quit confirmation
        if (showQuitDialog) {
            QuitBattleDialog(
                onConfirm = onGoHome,
                onDismiss = { showQuitDialog = false },
            )
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
            .background(Color.Black.copy(alpha = 0.6f))
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
