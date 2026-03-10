package com.example.jaygame.ui.battle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.bridge.BattleBridge
import com.example.jaygame.bridge.GridTileState
import com.example.jaygame.data.UNIT_DEFS_MAP
import com.example.jaygame.ui.theme.DarkSurface
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.SubText

/**
 * 5x3 interactive grid overlay for the battle screen.
 * - Empty tiles: dim border, tap to summon
 * - Occupied tiles: show unit icon + grade color, tap for popup
 * - Tiles with merge candidates: show sparkle badge
 */
@Composable
fun BattleFieldGrid() {
    val gridState by BattleBridge.gridState.collectAsState()
    val selectedTile by BattleBridge.selectedTile.collectAsState()
    val battle by BattleBridge.state.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        val cellHeight = maxWidth / 5 * 0.6f

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    for (col in 0 until 5) {
                        val index = row * 5 + col
                        val tile = gridState.getOrNull(index)
                        val isSelected = selectedTile == index

                        GridTile(
                            tile = tile,
                            isSelected = isSelected,
                            canSummon = battle.sp >= battle.summonCost,
                            onClick = { BattleBridge.requestClickTile(index) },
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridTile(
    tile: GridTileState?,
    isSelected: Boolean,
    canSummon: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradeColor = when (tile?.grade) {
        0 -> Color(0xFF9E9E9E)  // LOW
        1 -> Color(0xFF2196F3)  // MEDIUM
        2 -> Color(0xFFAB47BC)  // HIGH
        3 -> Color(0xFFFF8F00)  // SUPREME
        4 -> Color(0xFFE94560)  // TRANSCENDENT
        else -> DarkSurface
    }

    val isEmpty = tile == null || tile.unitDefId < 0
    val borderColor = when {
        isSelected -> NeonCyan
        !isEmpty -> gradeColor.copy(alpha = 0.7f)
        else -> SubText.copy(alpha = 0.2f)
    }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "tileScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(if (isEmpty) DarkSurface.copy(alpha = 0.4f) else gradeColor.copy(alpha = 0.1f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isEmpty) {
            if (canSummon) {
                Text("+", color = SubText.copy(alpha = 0.4f), fontSize = 20.sp)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val unitDef = UNIT_DEFS_MAP[tile!!.unitDefId]
                if (unitDef != null) {
                    Image(
                        painter = painterResource(id = unitDef.iconRes),
                        contentDescription = unitDef.name,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = unitDef.name,
                        color = Color.White,
                        fontSize = 9.sp,
                        maxLines = 1,
                    )
                }
            }

            // Merge badge
            if (tile?.canMerge == true) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(14.dp)
                        .background(Gold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("\u2605", fontSize = 8.sp, color = DeepDark)
                }
            }
        }
    }
}
