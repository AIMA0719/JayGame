package com.example.jaygame.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.R
import com.example.jaygame.ui.theme.DarkNavy
import com.example.jaygame.ui.theme.Divider
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.SubText

enum class NavTab(val label: String, val iconRes: Int) {
    BATTLE("전투", R.drawable.ic_nav_battle),
    DECK("덱", R.drawable.ic_nav_deck),
    HOME("홈", R.drawable.ic_nav_home),
    COLLECTION("컬렉션", R.drawable.ic_nav_collection),
    SHOP("상점", R.drawable.ic_nav_shop),
}

@Composable
fun GameBottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .drawBehind {
                drawRect(color = DarkNavy)
                drawLine(
                    color = Divider,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val color = if (isSelected) NeonRed else SubText

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(tab) },
                    ),
            ) {
                Icon(
                    painter = painterResource(id = tab.iconRes),
                    contentDescription = tab.label,
                    tint = color,
                    modifier = Modifier.size(if (tab == NavTab.HOME) 26.dp else 22.dp),
                )
                Text(
                    text = tab.label,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp,
                    color = color,
                )
            }
        }
    }
}
