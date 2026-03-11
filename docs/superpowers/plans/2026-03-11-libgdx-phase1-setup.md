# Phase 1: libGDX 기반 세팅 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 기존 JayGame 프로젝트에 libGDX를 추가하고, AndroidLauncher + JayGame 클래스로 빈 화면을 띄운다. 기존 C++/Compose 코드는 아직 삭제하지 않는다.

**Architecture:** libGDX Android 백엔드를 기존 Gradle 프로젝트에 의존성으로 추가한다. 새 AndroidLauncher Activity가 libGDX Game을 호스팅하고, 임시 TestScreen이 "Hello libGDX" 텍스트를 렌더링한다. 기존 코드와 충돌 없이 공존하며, AndroidManifest에서 launcher를 새 Activity로 전환한다.

**Tech Stack:** libGDX 1.13.0, libGDX Box2D, Kotlin, Gradle version catalog

**Spec:** `docs/superpowers/specs/2026-03-11-libgdx-migration-design.md`

---

## File Structure

### 생성할 파일
- `app/src/main/java/com/example/jaygame/AndroidLauncher.kt` — libGDX AndroidApplication Activity
- `app/src/main/java/com/example/jaygame/JayGame.kt` — Game 클래스 (Screen 관리)
- `app/src/main/java/com/example/jaygame/screen/TestScreen.kt` — "Hello libGDX" 확인용 임시 Screen

### 수정할 파일
- `gradle/libs.versions.toml` — libGDX 버전 및 라이브러리 추가
- `settings.gradle.kts` — libGDX maven repo 추가 (필요 시)
- `app/build.gradle.kts` — libGDX 의존성 추가, Kotlin 플러그인 확인
- `app/src/main/AndroidManifest.xml` — AndroidLauncher를 MAIN/LAUNCHER로 설정
- `build.gradle.kts` (project level) — Kotlin 플러그인 추가 (필요 시)

---

## Chunk 1: libGDX 기반 세팅

### Task 1: Gradle에 libGDX 의존성 추가

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `build.gradle.kts` (project level)

- [ ] **Step 1: libs.versions.toml에 libGDX 버전 및 라이브러리 추가**

`gradle/libs.versions.toml`의 `[versions]` 섹션 끝에 추가:
```toml
gdx = "1.13.0"
```

`[libraries]` 섹션 끝에 추가:
```toml
gdx = { group = "com.badlogicgames.gdx", name = "gdx", version.ref = "gdx" }
gdx-backend-android = { group = "com.badlogicgames.gdx", name = "gdx-backend-android", version.ref = "gdx" }
gdx-box2d = { group = "com.badlogicgames.gdx", name = "gdx-box2d", version.ref = "gdx" }
gdx-box2d-platform = { group = "com.badlogicgames.gdx", name = "gdx-box2d-platform", version.ref = "gdx" }
```

`[plugins]` 섹션 끝에 추가:
```toml
kotlin-android = { id = "org.jetbrains.kotlin.android", version = "2.1.20" }
```

- [ ] **Step 2: project-level build.gradle.kts에 Kotlin 플러그인 등록**

`build.gradle.kts` (프로젝트 루트) 수정:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
```

- [ ] **Step 3: app/build.gradle.kts에 libGDX 의존성 및 Kotlin 플러그인 추가**

`app/build.gradle.kts`의 `plugins` 블록에 추가:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}
```

`dependencies` 블록에 libGDX 추가:
```kotlin
// libGDX
implementation(libs.gdx)
implementation(libs.gdx.backend.android)
implementation(libs.gdx.box2d)
natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-armeabi-v7a")
natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-arm64-v8a")
natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-x86")
natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-x86_64")
natives("com.badlogicgames.gdx:gdx-box2d-platform:${libs.versions.gdx.get()}:natives-armeabi-v7a")
natives("com.badlogicgames.gdx:gdx-box2d-platform:${libs.versions.gdx.get()}:natives-arm64-v8a")
natives("com.badlogicgames.gdx:gdx-box2d-platform:${libs.versions.gdx.get()}:natives-x86")
natives("com.badlogicgames.gdx:gdx-box2d-platform:${libs.versions.gdx.get()}:natives-x86_64")
```

`android` 블록 밖, `dependencies` 블록 위에 `natives` 설정 추가:
```kotlin
// libGDX native extraction helper
configurations {
    create("natives")
}

// Copy libGDX native .so files into jniLibs
tasks.register("copyNatives") {
    doLast {
        val nativesConfig = configurations["natives"]
        nativesConfig.files.forEach { jar ->
            val outputDir = file("src/main/jniLibs")
            copy {
                from(zipTree(jar))
                into(outputDir)
                include("**/*.so")
            }
        }
    }
}

tasks.matching { it.name.contains("merge") && it.name.contains("JniLibFolders") }.configureEach {
    dependsOn("copyNatives")
}
```

- [ ] **Step 4: Gradle sync 실행해서 의존성 확인**

Run: Android Studio에서 "Sync Project with Gradle Files" 또는:
```bash
cd C:\Users\Infocar\AndroidStudioProjects\JayGame && ./gradlew app:dependencies --configuration implementation | head -50
```
Expected: `com.badlogicgames.gdx:gdx:1.13.0`, `gdx-backend-android`, `gdx-box2d` 표시

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts
git commit -m "build: add libGDX 1.13.0 + Box2D dependencies"
```

---

### Task 2: AndroidLauncher Activity 생성

**Files:**
- Create: `app/src/main/java/com/example/jaygame/AndroidLauncher.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: AndroidLauncher.kt 생성**

Create `app/src/main/java/com/example/jaygame/AndroidLauncher.kt`:
```kotlin
package com.example.jaygame

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            numSamples = 2  // anti-aliasing
        }

        initialize(JayGame(), config)
    }
}
```

- [ ] **Step 2: AndroidManifest.xml 수정 — AndroidLauncher를 launcher로 등록**

`app/src/main/AndroidManifest.xml`에 새 Activity 추가하고, 기존 ComposeActivity의 MAIN/LAUNCHER intent-filter는 유지 (나중에 제거):

기존 `<application>` 태그 안, 첫 번째 `<activity>` 앞에 추가:
```xml
<activity
    android:name=".AndroidLauncher"
    android:exported="true"
    android:screenOrientation="portrait"
    android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize|screenLayout|uiMode"
    android:launchMode="singleTask">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

기존 ComposeActivity에서 MAIN/LAUNCHER intent-filter를 **제거**:
```xml
<activity
    android:name=".ComposeActivity"
    android:exported="false"
    android:theme="@style/Theme.JayGame"
    android:launchMode="singleTop"
    android:screenOrientation="portrait">
    <!-- intent-filter 제거됨 -->
</activity>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/jaygame/AndroidLauncher.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add AndroidLauncher activity for libGDX"
```

---

### Task 3: JayGame 클래스 + TestScreen 생성

**Files:**
- Create: `app/src/main/java/com/example/jaygame/JayGame.kt`
- Create: `app/src/main/java/com/example/jaygame/screen/TestScreen.kt`

- [ ] **Step 1: JayGame.kt 생성**

Create `app/src/main/java/com/example/jaygame/JayGame.kt`:
```kotlin
package com.example.jaygame

import com.badlogic.gdx.Game
import com.example.jaygame.screen.TestScreen

class JayGame : Game() {
    override fun create() {
        setScreen(TestScreen(this))
    }
}
```

- [ ] **Step 2: screen 디렉토리 생성**

```bash
mkdir -p app/src/main/java/com/example/jaygame/screen
```

- [ ] **Step 3: TestScreen.kt 생성**

Create `app/src/main/java/com/example/jaygame/screen/TestScreen.kt`:
```kotlin
package com.example.jaygame.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.example.jaygame.JayGame

class TestScreen(private val game: JayGame) : ScreenAdapter() {
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont

    override fun show() {
        batch = SpriteBatch()
        font = BitmapFont().apply {
            data.setScale(3f)
            color = Color.WHITE
        }
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0.1f, 0.05f, 0.15f, 1f)  // dark purple

        batch.begin()
        font.draw(
            batch,
            "JayGame - libGDX Ready!",
            Gdx.graphics.width / 2f - 250f,
            Gdx.graphics.height / 2f + 20f,
        )
        font.draw(
            batch,
            "${Gdx.graphics.width}x${Gdx.graphics.height} @ ${Gdx.graphics.framesPerSecond} FPS",
            Gdx.graphics.width / 2f - 200f,
            Gdx.graphics.height / 2f - 40f,
        )
        batch.end()
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
    }
}
```

- [ ] **Step 4: 빌드 및 실행 확인**

Run: Android Studio에서 앱 실행
Expected: 다크 퍼플 배경에 "JayGame - libGDX Ready!" 텍스트와 해상도/FPS 표시

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/jaygame/JayGame.kt app/src/main/java/com/example/jaygame/screen/TestScreen.kt
git commit -m "feat: add JayGame + TestScreen - libGDX hello world"
```

---

### Task 4: libGDX assets 폴더 구조 생성

**Files:**
- Create: `app/src/main/assets/sprites/.gitkeep`
- Create: `app/src/main/assets/particles/.gitkeep`
- Create: `app/src/main/assets/ui/.gitkeep`
- Create: `app/src/main/assets/fonts/.gitkeep`
- Create: `app/src/main/assets/sounds/.gitkeep`

- [ ] **Step 1: assets 하위 디렉토리 구조 생성**

```bash
cd C:\Users\Infocar\AndroidStudioProjects\JayGame
mkdir -p app/src/main/assets/sprites
mkdir -p app/src/main/assets/particles
mkdir -p app/src/main/assets/ui
mkdir -p app/src/main/assets/fonts
mkdir -p app/src/main/assets/sounds
touch app/src/main/assets/sprites/.gitkeep
touch app/src/main/assets/particles/.gitkeep
touch app/src/main/assets/ui/.gitkeep
touch app/src/main/assets/fonts/.gitkeep
touch app/src/main/assets/sounds/.gitkeep
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/assets/
git commit -m "chore: create libGDX asset folder structure"
```

---

## Phase 1 완료 기준

- [ ] `./gradlew assembleDebug` 성공
- [ ] 앱 실행 시 다크 퍼플 배경에 "JayGame - libGDX Ready!" 텍스트 표시
- [ ] FPS 카운터 60 근처로 표시
- [ ] 세로 모드(Portrait) 동작 확인
- [ ] 기존 Compose/C++ 코드와 충돌 없음 (같은 APK에 공존)

---

## 다음 Phase

Phase 1 완료 후 → `2026-03-11-libgdx-phase2-battle-core.md` 작성
- BattleWorld (유닛/적/투사체/SP/웨이브 로직)
- BattleRenderer (SpriteBatch 렌더링)
- C++ BattleScene 로직을 Kotlin으로 포팅
