package com.example.jaygame.engine.behavior

/**
 * Registers all built-in behavior factories.
 * Call once at app startup (e.g., from Application.onCreate or GameEngine.init).
 */
object BehaviorRegistration {
    private var registered = false

    fun ensureRegistered() {
        if (registered) return
        registered = true

        BehaviorFactory.register("ranged_shooter") { RangedShooterBehavior() }
        BehaviorFactory.register("ranged_mage") { RangedShooterBehavior(aoe = true) }
    }
}
