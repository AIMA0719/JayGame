package com.jay.jaygame.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.jaygame.R
import com.jay.jaygame.data.ALL_PROFILES
import com.jay.jaygame.data.GameRepository
import com.jay.jaygame.data.ProfileDef
import com.jay.jaygame.ui.components.ResourceHeader
import com.jay.jaygame.ui.theme.BorderGlow
import com.jay.jaygame.ui.theme.DeepDark
import com.jay.jaygame.ui.theme.DimText
import com.jay.jaygame.ui.theme.Gold
import com.jay.jaygame.ui.theme.LightText
import com.jay.jaygame.ui.theme.NeonGreen
import com.jay.jaygame.ui.theme.SubText

// Pre-allocated colors to avoid GC
private val LockedBg = Color(0xFF1A1A2E)
private val UnlockedBg = Color(0xFF1E2A1E)
private val SelectedBg = Color(0xFF2A2A10)
private val AnimatedGlow1 = Color(0xFFFFD700)
private val AnimatedGlow2 = Color(0xFFFF8C00)
private val LockedBorder = Color(0xFF333355)

@Composable
fun ProfileScreen(
    repository: GameRepository,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    val data by repository.gameData.collectAsState()

    // Compute which profiles are newly unlocked based on current GameData
    val nowUnlocked = ALL_PROFILES.filter { it.condition(data) }.map { it.id }.toSet()
    val allUnlocked = remember(data.unlockedProfiles, nowUnlocked) { data.unlockedProfiles + nowUnlocked }

    // Auto-save if new profiles were unlocked
    LaunchedEffect(allUnlocked) {
        if (nowUnlocked.any { it !in data.unlockedProfiles }) {
            repository.save(data.copy(unlockedProfiles = allUnlocked))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDark),
    ) {
        if (showTopBar) {
            ResourceHeader(gold = data.gold, diamonds = data.diamonds)

            Spacer(modifier = Modifier.height(8.dp))

            // Title row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "뒤로",
                    tint = LightText,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onBack),
                )
                Text(
                    text = "프로필 칭호",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Gold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.width(24.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Current profile display
        val currentProfile = ALL_PROFILES.find { it.id == data.selectedProfileId }
        if (currentProfile != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(SelectedBg, RoundedCornerShape(8.dp))
                    .border(1.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "\uD83D\uDC51", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "현재 칭호: ${currentProfile.name}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                    )
                    Text(
                        text = currentProfile.description,
                        fontSize = 11.sp,
                        color = SubText,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ALL_PROFILES, key = { it.id }) { profile ->
                val isUnlocked = profile.id in allUnlocked
                val isSelected = profile.id == data.selectedProfileId

                ProfileCard(
                    profile = profile,
                    isUnlocked = isUnlocked,
                    isSelected = isSelected,
                    onClick = {
                        if (isUnlocked && !isSelected) {
                            repository.save(
                                data.copy(
                                    selectedProfileId = profile.id,
                                    unlockedProfiles = allUnlocked,
                                )
                            )
                        }
                    },
                )
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileDef,
    isUnlocked: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "profileGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    val borderColor = when {
        isSelected -> Gold
        isUnlocked && profile.isAnimated -> AnimatedGlow1.copy(alpha = glowAlpha)
        isUnlocked -> NeonGreen.copy(alpha = 0.6f)
        else -> LockedBorder
    }

    val bgColor = when {
        isSelected -> SelectedBg
        isUnlocked -> UnlockedBg
        else -> LockedBg
    }

    Box(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected && profile.isAnimated) {
                    Brush.radialGradient(
                        listOf(SelectedBg, Color(0xFF1A1A00))
                    )
                } else {
                    Brush.verticalGradient(listOf(bgColor, bgColor))
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Shimmer overlay for animated unlocked profiles
        if (isUnlocked && profile.isAnimated) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                AnimatedGlow1.copy(alpha = glowAlpha * 0.1f),
                                Color.Transparent,
                            )
                        )
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp),
        ) {
            // Icon / emoji placeholder
            Text(
                text = when {
                    !isUnlocked -> "\uD83D\uDD12"
                    isSelected -> "\uD83D\uDC51"
                    profile.isAnimated -> "\u2728"
                    else -> "\u2B50"
                },
                fontSize = 26.sp,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = profile.name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isSelected -> Gold
                    isUnlocked -> LightText
                    else -> DimText
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = profile.description,
                fontSize = 9.sp,
                color = if (isUnlocked) SubText else DimText.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (isSelected) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "장착 중",
                    fontSize = 9.sp,
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
