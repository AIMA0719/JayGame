package com.jay.jaygame.ui.battle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.jaygame.engine.AuctionPhase
import com.jay.jaygame.engine.AuctionState
import com.jay.jaygame.engine.AuctionSystem
import com.jay.jaygame.engine.BlueprintRegistry
import com.jay.jaygame.ui.components.NeonButton
import com.jay.jaygame.ui.components.blueprintIconRes
import com.jay.jaygame.ui.theme.DeepDark
import com.jay.jaygame.ui.theme.DimText
import com.jay.jaygame.ui.theme.Gold
import com.jay.jaygame.ui.theme.LightText
import com.jay.jaygame.ui.theme.NeonCyan
import com.jay.jaygame.ui.theme.NeonRed
import com.jay.jaygame.ui.theme.NeonRedDark
import com.jay.jaygame.ui.theme.SubText

// ── Pre-allocated colors (Canvas GC policy) ──
private val DialogBg = Color(0xFF1A1A2E)
private val DialogBorder = Color(0xFF3D2E20)
private val TitleGold = Color(0xFFFFD54F)
private val CardBg = Color(0xFF12101E)
private val BidGreen = Color(0xFF4CAF50)
private val BidGreenDark = Color(0xFF2E7D32)
private val PassGray = Color(0xFF78909C)
private val PassGrayDark = Color(0xFF455A64)
private val TimerBg = Color(0xFF2A2040)
private val SoldGold = Color(0xFFFFD700)
private val UnsoldGray = Color(0xFF9E9E9E)
private val NpcActiveColor = Color(0xFF81C784)
private val NpcRetiredColor = Color(0xFFEF5350)

@Composable
fun AuctionDialog(
    state: AuctionState,
    playerSp: Float,
    onBid: () -> Unit,
    onPass: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val gradeColor = GradeColorsByIndex.getOrElse(state.blueprintGrade) { Color.Gray }
    val gradeName = GradeNamesByIndex.getOrElse(state.blueprintGrade) { "?" }
    val bp = remember(state.blueprintId) {
        BlueprintRegistry.instance.findById(state.blueprintId)
    }
    val iconRes = remember(bp) {
        bp?.let { blueprintIconRes(it, context) } ?: 0
    }

    // INTRO 카드 등장 애니메이션
    val cardScale = remember { Animatable(0.3f) }
    var introVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.phase) {
        if (state.phase == AuctionPhase.INTRO) {
            introVisible = false
            cardScale.snapTo(0.3f)
            introVisible = true
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500),
            )
        } else if (!introVisible) {
            introVisible = true
            cardScale.snapTo(1f)
        }
    }

    // 타이머 진행률
    val maxTime = when (state.phase) {
        AuctionPhase.INTRO -> AuctionSystem.INTRO_TIME
        AuctionPhase.BIDDING -> AuctionSystem.ROUND_TIME
        AuctionPhase.NPC_TURN -> AuctionSystem.NPC_DELAY
        AuctionPhase.GOING_ONCE -> AuctionSystem.ROUND_TIME
        else -> 1f
    }
    val timerProgress = if (maxTime > 0f) (state.timer / maxTime).coerceIn(0f, 1f) else 0f

    val isTerminal = state.phase == AuctionPhase.SOLD || state.phase == AuctionPhase.UNSOLD
    val buttonsEnabled = state.phase == AuctionPhase.BIDDING || state.phase == AuctionPhase.GOING_ONCE
    val nextBidCost = state.currentBid + state.bidIncrement
    val canAfford = playerSp >= nextBidCost

    // 배경 (클릭 불가)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.60f),
                        Color.Black.copy(alpha = 0.88f),
                    ),
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { /* 배경 클릭 무시 */ },
        contentAlignment = Alignment.Center,
    ) {
        // 메인 다이얼로그
        AnimatedVisibility(
            visible = introVisible,
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .graphicsLayer {
                        scaleX = cardScale.value
                        scaleY = cardScale.value
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, gradeColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DialogBg, DeepDark),
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── 타이틀 ──
                Text(
                    text = "\u2694 \uACBD\uB9E4\uC7A5 \u2694",
                    color = TitleGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── 유닛 카드 ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, gradeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    CardBg,
                                    gradeColor.copy(alpha = 0.08f),
                                )
                            )
                        )
                        .padding(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 아이콘
                        if (iconRes != 0) {
                            Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = state.blueprintName,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        gradeColor.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    ),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(gradeColor.copy(alpha = 0.15f))
                                    .border(
                                        1.dp,
                                        gradeColor.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "?",
                                    color = gradeColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            // 이름 + 등급
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = state.blueprintName,
                                    color = LightText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "[$gradeName]",
                                    color = gradeColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // 스탯
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text(
                                    text = "ATK: ${state.blueprintAtk.toInt()}",
                                    color = SubText,
                                    fontSize = 13.sp,
                                )
                                Text(
                                    text = "SPD: ${"%.1f".format(state.blueprintSpd)}",
                                    color = SubText,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── 현재가 + 입찰자 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "\uD604\uC7AC\uAC00: ",
                        color = SubText,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = "${state.currentBid}",
                        color = Gold,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (state.currentBidder.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${
                                if (state.currentBidder == AuctionSystem.BIDDER_PLAYER) "\uB098"
                                else state.currentBidder
                            })",
                            color = if (state.currentBidder == AuctionSystem.BIDDER_PLAYER) NeonCyan
                            else SubText,
                            fontSize = 13.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── 타이머 바 ──
                if (!isTerminal) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinearProgressIndicator(
                            progress = { timerProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when (state.phase) {
                                AuctionPhase.GOING_ONCE -> NeonRed
                                AuctionPhase.NPC_TURN -> Gold
                                else -> NeonCyan
                            },
                            trackColor = TimerBg,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${"%.1f".format(state.timer)}\uCD08",
                            color = if (state.phase == AuctionPhase.GOING_ONCE) NeonRed
                            else DimText,
                            fontSize = 12.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }

                // ── Phase 상태 텍스트 ──
                when (state.phase) {
                    AuctionPhase.INTRO -> {
                        Text(
                            text = "\uB9E4\uBB3C \uC18C\uAC1C \uC911...",
                            color = TitleGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    AuctionPhase.NPC_TURN -> {
                        Text(
                            text = "NPC \uC785\uCC30 \uC911...",
                            color = Gold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    AuctionPhase.GOING_ONCE -> {
                        Text(
                            text = "\uB9C8\uC9C0\uB9C9 \uAE30\uD68C!",
                            color = NeonRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    AuctionPhase.SOLD -> {
                        val isPlayerWon = state.currentBidder == AuctionSystem.BIDDER_PLAYER
                        Text(
                            text = if (isPlayerWon) "\uD83C\uDF89 \uB099\uCC30! \uCD95\uD558\uD569\uB2C8\uB2E4!"
                            else "\uD83D\uDE1E ${state.currentBidder}\uC5D0\uAC8C \uB099\uCC30\uB418\uC5C8\uC2B5\uB2C8\uB2E4",
                            color = if (isPlayerWon) SoldGold else UnsoldGray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    AuctionPhase.UNSOLD -> {
                        Text(
                            text = "\uC720\uCC30 \u2014 \uC544\uBB34\uB3C4 \uC785\uCC30\uD558\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4",
                            color = UnsoldGray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    else -> {} // BIDDING — 별도 텍스트 없음
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── NPC 목록 ──
                if (state.npcs.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "NPC: ",
                            color = DimText,
                            fontSize = 12.sp,
                        )
                        state.npcs.forEach { npc ->
                            val npcColor = if (npc.retired) NpcRetiredColor else NpcActiveColor
                            Text(
                                text = "${npc.name}(${
                                    if (npc.retired) "\uD3EC\uAE30" else "\uCC38\uC5EC"
                                })",
                                color = npcColor,
                                fontSize = 12.sp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── 버튼 영역 ──
                if (isTerminal) {
                    // 종료 상태: 확인 버튼
                    NeonButton(
                        text = "\uD655\uC778",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(0.6f),
                        accentColor = Gold,
                        accentColorDark = Color(0xFF8B6914),
                        fontSize = 15.sp,
                    )
                } else {
                    // 입찰 / 포기 버튼
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // 입찰 버튼
                        val bidEnabled = buttonsEnabled && canAfford && !state.playerRetired
                                && state.currentBidder != AuctionSystem.BIDDER_PLAYER
                        NeonButton(
                            text = "\uC785\uCC30 +${state.bidIncrement}",
                            onClick = onBid,
                            modifier = Modifier.weight(1f),
                            enabled = bidEnabled,
                            accentColor = BidGreen,
                            accentColorDark = BidGreenDark,
                            fontSize = 14.sp,
                        )

                        // 포기 버튼
                        NeonButton(
                            text = "\uD3EC\uAE30",
                            onClick = onPass,
                            modifier = Modifier.weight(1f),
                            enabled = buttonsEnabled && !state.playerRetired,
                            accentColor = PassGray,
                            accentColorDark = PassGrayDark,
                            fontSize = 14.sp,
                        )
                    }

                    // 보유 코인 표시
                    if (buttonsEnabled && !canAfford && !state.playerRetired) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\uCF54\uC778 \uBD80\uC871 (\uBCF4\uC720: ${playerSp.toInt()})",
                            color = NeonRed,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}
