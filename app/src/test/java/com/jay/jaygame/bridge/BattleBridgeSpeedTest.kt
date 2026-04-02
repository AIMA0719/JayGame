package com.jay.jaygame.bridge

import org.junit.Assert.assertEquals
import org.junit.Test

class BattleBridgeSpeedTest {
    @Test
    fun setBattleSpeed_allowsX3Speed() {
        val originalSpeed = BattleBridge.battleSpeed.value
        try {
            BattleBridge.setBattleSpeed(6f)

            assertEquals(6f, BattleBridge.battleSpeed.value, 0f)
        } finally {
            BattleBridge.setBattleSpeed(originalSpeed)
        }
    }

    @Test
    fun cycleBattleSpeed_rotatesThroughX1X2X3() {
        val originalSpeed = BattleBridge.battleSpeed.value
        try {
            BattleBridge.setBattleSpeed(2f)
            BattleBridge.cycleBattleSpeed()
            assertEquals(4f, BattleBridge.battleSpeed.value, 0f)

            BattleBridge.cycleBattleSpeed()
            assertEquals(6f, BattleBridge.battleSpeed.value, 0f)

            BattleBridge.cycleBattleSpeed()
            assertEquals(2f, BattleBridge.battleSpeed.value, 0f)
        } finally {
            BattleBridge.setBattleSpeed(originalSpeed)
        }
    }
}
