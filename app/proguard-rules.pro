# ── JayGame ProGuard Rules ──

# ── Preserve line numbers for crash reporting ──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Strip debug/verbose/info logs in release ──
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# ── Kotlin ──
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ── Compose ──
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ── Orbit MVI ──
-dontwarn org.orbitmvi.**
-keep class org.orbitmvi.orbit.** { *; }

# ── Coil ──
-dontwarn coil.**
-keep class coil.** { *; }

# ── Lottie ──
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# ── Game data models (JSON serialization) ──
-keep class com.jay.jaygame.data.GameData { *; }
-keep class com.jay.jaygame.data.GameData$* { *; }
-keep class com.jay.jaygame.engine.UnitGrade { *; }
-keep class com.jay.jaygame.engine.UnitFamily { *; }
-keep class com.jay.jaygame.engine.UnitRole { *; }
-keep class com.jay.jaygame.engine.AttackRange { *; }
-keep class com.jay.jaygame.engine.DamageType { *; }
-keep class com.jay.jaygame.data.UnitRace { *; }

# ── SaveKeyStore (HMAC integrity) ──
-keep class com.jay.jaygame.data.SaveKeyStore { *; }

# ── Enums used in JSON ──
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
