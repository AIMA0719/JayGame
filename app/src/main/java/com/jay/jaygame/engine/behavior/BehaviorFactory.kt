package com.jay.jaygame.engine.behavior

import com.jay.jaygame.engine.UnitBehavior

object BehaviorFactory {
    private val registry = mutableMapOf<String, () -> UnitBehavior>()

    fun register(behaviorId: String, factory: () -> UnitBehavior) {
        registry[behaviorId] = factory
    }

    fun create(behaviorId: String): UnitBehavior? {
        return registry[behaviorId]?.invoke()
    }

    fun isRegistered(behaviorId: String): Boolean = behaviorId in registry

    fun clearForTesting() { registry.clear() }
}
