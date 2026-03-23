package com.example.jaygame.data

import androidx.compose.ui.graphics.Color

data class StageDef(
    val id: Int,
    val name: String,
    val description: String,
    val staminaCost: Int,
    val unlockTrophies: Int,
    val maxWaves: Int,
    val bgAsset: String = "backgrounds/bg_plains.png",
    val bgColors: List<Color> = listOf(Color(0xFF1A3A1A), Color(0xFF0D2B0D)),
    val pathColor: Color = Color(0xFF5D4037),
    val fieldColors: List<Color> = listOf(Color(0xFF4CAF50), Color(0xFF388E3C), Color(0xFF2E7D32)),
)

val STAGES = listOf(
    StageDef(0, "초원", "평화로운 시작의 땅", 5, 0, 60,
        bgAsset = "backgrounds/bg_plains.png",
        bgColors = listOf(Color(0xFF1A3A1A), Color(0xFF0D2B0D)),
        pathColor = Color(0xFF5D4037),
        fieldColors = listOf(Color(0xFF4CAF50), Color(0xFF388E3C), Color(0xFF2E7D32))),
    StageDef(1, "정글", "울창한 밀림 속 전투", 5, 200, 60,
        bgAsset = "backgrounds/bg_jungle.png",
        bgColors = listOf(Color(0xFF1B2E1B), Color(0xFF0A1F0A)),
        pathColor = Color(0xFF4E342E),
        fieldColors = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF145214))),
    StageDef(2, "사막", "뜨거운 모래 위의 사투", 6, 500, 60,
        bgAsset = "backgrounds/bg_desert.png",
        bgColors = listOf(Color(0xFF3E2E1A), Color(0xFF2A1F0F)),
        pathColor = Color(0xFF8D6E3C),
        fieldColors = listOf(Color(0xFFD2B48C), Color(0xFFC19A6B), Color(0xFFA67B5B))),
    StageDef(3, "설산", "얼어붙은 봉우리", 6, 1000, 60,
        bgAsset = "backgrounds/bg_glacier.png",
        bgColors = listOf(Color(0xFF1A2A3A), Color(0xFF0F1F2F)),
        pathColor = Color(0xFF607D8B),
        fieldColors = listOf(Color(0xFFB0C4DE), Color(0xFF87CEEB), Color(0xFF6CA6CD))),
    StageDef(4, "화산", "불타는 대지", 7, 2000, 60,
        bgAsset = "backgrounds/bg_volcano.png",
        bgColors = listOf(Color(0xFF3A1A0A), Color(0xFF2A0F05)),
        pathColor = Color(0xFF8B2500),
        fieldColors = listOf(Color(0xFFCC4400), Color(0xFFAA3300), Color(0xFF882200))),
    StageDef(5, "심연", "최후의 시련", 8, 3500, 60,
        bgAsset = "backgrounds/bg_abyss.png",
        bgColors = listOf(Color(0xFF1A0A2A), Color(0xFF0F0520)),
        pathColor = Color(0xFF4A148C),
        fieldColors = listOf(Color(0xFF6A1B9A), Color(0xFF4A148C), Color(0xFF38006B))),
)
