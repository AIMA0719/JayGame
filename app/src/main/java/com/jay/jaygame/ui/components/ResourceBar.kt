package com.jay.jaygame.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.jaygame.R
import com.jay.jaygame.ui.theme.DarkNavy
import com.jay.jaygame.ui.theme.Gold
import com.jay.jaygame.ui.theme.LightText
import com.jay.jaygame.ui.theme.StaminaGreen
import com.jay.jaygame.ui.theme.SubText
import java.text.NumberFormat

@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    color: Color = LightText,
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(500),
        label = "counterAnim",
    )
    Text(
        text = NumberFormat.getNumberInstance().format(animatedValue),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun ResourceItem(
    iconRes: Int,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        AnimatedCounter(
            targetValue = value,
            color = LightText,
        )
    }
}

@Composable
fun ProfileHeader(
    playerName: String,
    level: Int,
    gold: Int,
    diamonds: Int,
    stamina: Int,
    maxStamina: Int,
    modifier: Modifier = Modifier,
) {
    GameCard(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkNavy),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = playerName.take(1),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = LightText,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = playerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = LightText,
                )
                Text(
                    text = "Lv.$level",
                    fontSize = 11.sp,
                    color = Gold,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ResourceItem(iconRes = R.drawable.ic_diamond, value = diamonds)
                ResourceItem(iconRes = R.drawable.ic_gold, value = gold)
                StaminaItem(stamina = stamina, maxStamina = maxStamina)
            }
        }
    }
}

@Composable
fun StaminaItem(
    stamina: Int,
    maxStamina: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_stamina),
            contentDescription = null,
            tint = StaminaGreen,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$stamina/$maxStamina",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = LightText,
        )
    }
}

@Composable
fun CurrencyHeader(
    gold: Int,
    diamonds: Int,
    modifier: Modifier = Modifier,
) {
    GameCard(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
        ) {
            ResourceItem(iconRes = R.drawable.ic_gold, value = gold)
            Spacer(modifier = Modifier.width(12.dp))
            ResourceItem(iconRes = R.drawable.ic_diamond, value = diamonds)
        }
    }
}
