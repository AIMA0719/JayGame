package com.example.jaygame.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaygame.data.GameData
import com.example.jaygame.data.GameRepository
import com.example.jaygame.data.addRandomCardsToUnits
import com.example.jaygame.ui.components.GameCard
import com.example.jaygame.ui.components.NeonButton
import com.example.jaygame.ui.components.ResourceHeader
import com.example.jaygame.ui.theme.DeepDark
import com.example.jaygame.ui.theme.DimText
import com.example.jaygame.ui.theme.Gold
import com.example.jaygame.ui.theme.DarkGold
import com.example.jaygame.ui.theme.LightText
import com.example.jaygame.ui.theme.NeonCyan
import com.example.jaygame.ui.theme.NeonRed
import com.example.jaygame.ui.theme.NeonRedDark
import com.example.jaygame.ui.theme.SubText

private data class ShopItem(
    val name: String,
    val description: String,
    val priceText: String,
    val onPurchase: ((GameData, GameRepository) -> Boolean)?,
)

@Composable
fun ShopScreen(repository: GameRepository) {
    val data by repository.gameData.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabNames = listOf("골드팩", "다이아팩", "스페셜")

    val goldPackItems = remember {
        listOf(
            ShopItem(
                name = "골드 100개",
                description = "소량의 골드를 획득합니다.",
                priceText = "다이아 10",
                onPurchase = { d, repo ->
                    if (d.diamonds >= 10) {
                        repo.save(d.copy(diamonds = d.diamonds - 10, gold = d.gold + 100))
                        true
                    } else false
                },
            ),
            ShopItem(
                name = "골드 500개",
                description = "적당한 양의 골드를 획득합니다.",
                priceText = "다이아 40",
                onPurchase = { d, repo ->
                    if (d.diamonds >= 40) {
                        repo.save(d.copy(diamonds = d.diamonds - 40, gold = d.gold + 500))
                        true
                    } else false
                },
            ),
            ShopItem(
                name = "골드 2,000개",
                description = "대량의 골드를 획득합니다.",
                priceText = "다이아 150",
                onPurchase = { d, repo ->
                    if (d.diamonds >= 150) {
                        repo.save(d.copy(diamonds = d.diamonds - 150, gold = d.gold + 2000))
                        true
                    } else false
                },
            ),
            ShopItem(
                name = "골드 10,000개",
                description = "엄청난 양의 골드를 획득합니다.",
                priceText = "다이아 700",
                onPurchase = { d, repo ->
                    if (d.diamonds >= 700) {
                        repo.save(d.copy(diamonds = d.diamonds - 700, gold = d.gold + 10000))
                        true
                    } else false
                },
            ),
        )
    }

    val diamondPackItems = remember {
        listOf(
            ShopItem(
                name = "다이아 50개",
                description = "소량의 다이아몬드를 획득합니다.",
                priceText = "준비 중",
                onPurchase = null,
            ),
            ShopItem(
                name = "다이아 200개",
                description = "적당한 양의 다이아몬드를 획득합니다.",
                priceText = "준비 중",
                onPurchase = null,
            ),
            ShopItem(
                name = "다이아 500개",
                description = "대량의 다이아몬드를 획득합니다.",
                priceText = "준비 중",
                onPurchase = null,
            ),
        )
    }

    val specialItems = remember {
        listOf(
            ShopItem(
                name = "랜덤 유닛 카드 5장",
                description = "랜덤한 보유 유닛의 카드 5장을 획득합니다.",
                priceText = "골드 1,000",
                onPurchase = { d, repo ->
                    if (d.gold >= 1000) {
                        val updatedUnits = addRandomCardsToUnits(d.units, 5)
                        repo.save(d.copy(units = updatedUnits, gold = d.gold - 1000))
                        true
                    } else false
                },
            ),
            ShopItem(
                name = "랜덤 유닛 카드 20장",
                description = "랜덤한 보유 유닛의 카드 20장을 획득합니다.",
                priceText = "다이아 50",
                onPurchase = { d, repo ->
                    if (d.diamonds >= 50) {
                        val updatedUnits = addRandomCardsToUnits(d.units, 20)
                        repo.save(d.copy(units = updatedUnits, diamonds = d.diamonds - 50))
                        true
                    } else false
                },
            ),
            ShopItem(
                name = "초보자 패키지",
                description = "골드 5,000 + 랜덤 카드 10장을 획득합니다.",
                priceText = "준비 중",
                onPurchase = null,
            ),
        )
    }

    val currentItems = when (selectedTab) {
        0 -> goldPackItems
        1 -> diamondPackItems
        2 -> specialItems
        else -> goldPackItems
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDark),
    ) {
        // Resource Header
        ResourceHeader(gold = data.gold, diamonds = data.diamonds)

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = "상점",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = NeonRed,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabNames.forEachIndexed { index, name ->
                NeonButton(
                    text = name,
                    onClick = { selectedTab = index },
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    accentColor = if (selectedTab == index) NeonRed else DimText,
                    accentColorDark = if (selectedTab == index) NeonRedDark else DimText.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Item List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(currentItems) { item ->
                ShopItemCard(
                    item = item,
                    onBuy = {
                        val purchase = item.onPurchase
                        if (purchase != null) {
                            val currentData = repository.gameData.value
                            val success = purchase(currentData, repository)
                            if (success) {
                                Toast.makeText(context, "${item.name} 구매 완료!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "재화가 부족합니다.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "준비 중입니다.", Toast.LENGTH_SHORT).show()
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
private fun ShopItemCard(
    item: ShopItem,
    onBuy: () -> Unit,
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = LightText,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = SubText,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val btnAccent = when {
                item.onPurchase == null -> DimText
                item.priceText.contains("다이아") -> NeonCyan
                item.priceText.contains("골드") -> Gold
                else -> SubText
            }
            val btnAccentDark = when {
                item.onPurchase == null -> DimText.copy(alpha = 0.6f)
                item.priceText.contains("다이아") -> NeonCyan.copy(alpha = 0.7f)
                item.priceText.contains("골드") -> DarkGold
                else -> SubText.copy(alpha = 0.7f)
            }

            NeonButton(
                text = item.priceText,
                onClick = onBuy,
                fontSize = 11.sp,
                enabled = item.onPurchase != null,
                accentColor = btnAccent,
                accentColorDark = btnAccentDark,
            )
        }
    }
}
