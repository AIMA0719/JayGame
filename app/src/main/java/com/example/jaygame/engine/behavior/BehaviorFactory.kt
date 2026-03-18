package com.example.jaygame.engine.behavior

import com.example.jaygame.engine.UnitBehavior

object BehaviorFactory {
    private val registry = mutableMapOf<String, () -> UnitBehavior>()

    fun register(behaviorId: String, factory: () -> UnitBehavior) {
        registry[behaviorId] = factory
    }

    fun create(behaviorId: String): UnitBehavior {
        val factory = registry[behaviorId]
            ?: throw IllegalArgumentException("Unknown behaviorId: $behaviorId")
        return factory()
    }

    fun isRegistered(behaviorId: String): Boolean = behaviorId in registry

    fun clearForTesting() { registry.clear() }
}
