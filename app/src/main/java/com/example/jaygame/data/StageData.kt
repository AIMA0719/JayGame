package com.example.jaygame.data

data class StageDef(
    val id: Int,
    val name: String,
    val description: String,
    val staminaCost: Int,
    val unlockTrophies: Int,
    val maxWaves: Int,
)

val STAGES = listOf(
    StageDef(0, "초원", "평화로운 시작의 땅", 5, 0, 40),
    StageDef(1, "정글", "울창한 밀림 속 전투", 5, 200, 40),
    StageDef(2, "사막", "뜨거운 모래 위의 사투", 6, 500, 45),
    StageDef(3, "설산", "얼어붙은 봉우리", 6, 1000, 45),
    StageDef(4, "화산", "불타는 대지", 7, 2000, 50),
    StageDef(5, "심연", "최후의 시련", 8, 3500, 60),
)
