# Battle Visual Overhaul — 운빨존많겜급 생동감 구현

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** C++ 엔진의 기존 atlas 애니메이션을 활용하여 유닛/적/배경을 생동감 있게 렌더링하고, 피격/사망/소환 이펙트를 추가한다.

**Architecture:** Compose가 C++ 렌더링을 덮어씌우는 현재 구조를 제거하고, C++ SpriteBatch+SpriteAtlas가 유닛/적/배경을 직접 렌더링하도록 전환. Compose는 UI 오버레이(HUD, 투사체 빔, 데미지 숫자, 소환 카드, 인터랙션)만 담당.

**Tech Stack:** C++ (OpenGL ES, SpriteBatch, SpriteAtlas, ParticleSystem), Kotlin (Compose Canvas)

**핵심 발견:** C++ Unit::render()와 Enemy::render()가 이미 atlas 스프라이트 애니메이션을 구현하고 있으나, Compose BattleField.kt와 EnemyOverlay.kt가 정적 이미지로 위에 덮어씌우고 있어 보이지 않음. 투사체 trail/impact 파티클도 "Compose가 처리한다"며 비활성화됨.

---

## Chunk 1: Compose 오버레이 제거 — C++ 렌더링 노출

### Task 1: BattleField.kt — 유닛 스프라이트 렌더링 제거

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/battle/BattleField.kt`

**목표:** 유닛 스프라이트/페데스탈/레벨배지/합성표시 등 그리기 코드 제거. 인터랙션(탭/드래그)과 ground platform만 유지.

- [ ] **Step 1: BattleField.kt에서 유닛 렌더링 코드 제거**

Canvas 블록 내에서 `// ── Draw unit sprites` 이하 유닛별 for 루프 전체 제거:
- 라인 259-355 영역의 유닛 스프라이트 그리기 루프 제거 (그림자, 페데스탈, 선택링, 공격글로우, 유닛 스프라이트 drawImage, 레벨배지, 합성표시)
- 라인 358-378의 드래그 고스트 렌더링은 유지 (인터랙션 피드백)
- unitBitmaps remember 블록도 제거 (더 이상 사용 안 함)
- smoothXs/smoothYs 보간 코드는 유지 (드래그 감지에서 위치 참조 필요)

결과: Canvas는 ground platform(라인 189-245)만 그리고, 나머지는 C++ 렌더링이 보임.

- [ ] **Step 2: 빌드 확인**

Run: `cd C:\Users\Infocar\AndroidStudioProjects\JayGame && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/jaygame/ui/battle/BattleField.kt
git commit -m "refactor: remove unit sprite rendering from Compose - expose C++ atlas animations"
```

### Task 2: EnemyOverlay.kt — 적 스프라이트 렌더링 제거

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/battle/EnemyOverlay.kt`

**목표:** 적 스프라이트/HP바 렌더링을 제거. C++ Enemy::render()가 이미 walk 애니메이션 + HP바를 그리고 있으므로 중복 제거.

- [ ] **Step 1: EnemyOverlay.kt를 빈 Composable로 변경**

기존 코드를 모두 제거하고 빈 껍데기만 남김:

```kotlin
@Composable
fun EnemyOverlay() {
    // Enemy rendering is handled by C++ SpriteBatch with atlas animations.
    // (walk animation, HP bars, debuff indicators all rendered in C++)
}
```

또는 BattleScreen.kt에서 `EnemyOverlay()` 호출 자체를 제거해도 됨.

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/jaygame/ui/battle/EnemyOverlay.kt
git commit -m "refactor: remove enemy sprite rendering from Compose - C++ handles walk animation + HP bars"
```

### Task 3: C++ 렌더링 순서 — 유닛/적 렌더링 활성화

**Files:**
- Modify: `app/src/main/cpp/engine/BattleScene.cpp` (onRender 함수)

**목표:** onRender()에서 유닛과 적을 실제로 렌더링하도록 코드 추가. 현재는 주석으로 "Compose가 처리"라고 되어있는 부분을 활성화.

- [ ] **Step 1: BattleScene::onRender()에 유닛/적 렌더링 추가**

현재 onRender() (라인 631-671):
```cpp
// 2. Units: sprites rendered by Compose overlay
// 3. Enemies: sprites + HP bars rendered by Compose EnemyOverlay
```

이 부분을 실제 렌더링 코드로 교체:

```cpp
// 2. Render grid (cell backgrounds + grid lines)
grid_.render(batch, atlas_);

// 3. Render units (atlas sprite animations)
unitPool_.forEach([&](const Unit& unit) {
    if (unit.active) unit.render(alpha, batch, atlas_);
});

// 4. Render enemies (atlas walk animation + HP bars)
enemyPool_.forEach([&](const Enemy& enemy) {
    if (enemy.active) enemy.render(alpha, batch, atlas_);
});
```

**주의:** 렌더링 순서 = 배경 → 경로 → 그리드 → 유닛 → 적 → 파티클 → (Compose UI 위에)

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: enable C++ unit/enemy rendering with atlas sprite animations"
```

---

## Chunk 2: 유닛 생동감 — Idle Bounce + Attack Motion

### Task 4: Unit idle bounce 애니메이션

**Files:**
- Modify: `app/src/main/cpp/engine/Unit.cpp` (render 함수)

**목표:** 유닛이 가만히 있을 때 sin 파형으로 위아래 호버. 공격할 때 앞으로 돌진하는 모션 추가.

- [ ] **Step 1: Unit::render()에 idle bounce 추가**

현재 render() (라인 172-219)에서 `float cy = position.y;` 부분을 수정:

```cpp
void Unit::render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const {
    if (!active) return;

    const auto& tex = *atlas.getTexture();
    float cx = position.x;
    float cy = position.y;
    float spriteSize = 56.f;
    int grade = unitDefId / 5;

    // ── Idle bounce: gentle hover animation ──
    float bounceOffset = 0.f;
    if (!attacking_) {
        // sin wave: 3px amplitude, unique phase per unit based on unitDefId
        bounceOffset = std::sin(animTime_ * 2.5f + unitDefId * 0.7f) * 3.f;
    } else {
        // Attack: lunge forward toward last target direction (4px)
        float lunge = std::sin(attackAnimTimer_ * 10.f) * 4.f;
        if (lastTarget) {
            Vec2 dir = (lastTarget->position - position).normalized();
            cx += dir.x * lunge;
            cy += dir.y * lunge;
        }
    }
    cy += bounceOffset;

    // 1. Pedestal glow (flat ellipse under unit, grade-colored)
    // pedestal stays at original position (no bounce)
    drawPedestalGlow(batch, atlas, {position.x, position.y}, grade);

    // ... rest of render unchanged (using cx, cy for sprite position) ...
```

- [ ] **Step 2: 빌드 및 동작 확인**

Run: `./gradlew assembleDebug`
Expected: 유닛이 부드럽게 위아래로 호버, 공격 시 대상 방향으로 살짝 돌진

- [ ] **Step 3: Commit**

```bash
git add app/src/main/cpp/engine/Unit.cpp
git commit -m "feat: add unit idle bounce and attack lunge animation"
```

---

## Chunk 3: 적 피격/사망 이펙트

### Task 5: Enemy hit flash (하얀색 깜빡 + 넉백)

**Files:**
- Modify: `app/src/main/cpp/engine/Enemy.h`
- Modify: `app/src/main/cpp/engine/Enemy.cpp`

**목표:** 적이 피격당하면 0.1초간 하얀 플래시 + 살짝 뒤로 밀림.

- [ ] **Step 1: Enemy.h에 hitFlash 필드 추가**

```cpp
// Hit flash state
float hitFlashTimer_ = 0.f;
static constexpr float HIT_FLASH_DURATION = 0.12f;

// Knockback state
Vec2 knockbackOffset_{0.f, 0.f};
```

- [ ] **Step 2: Enemy::takeDamage()에서 hitFlash 트리거**

```cpp
void Enemy::takeDamage(float damage, bool isMagic) {
    // ... existing damage calculation ...
    hp -= finalDamage;
    if (hp < 0.f) hp = 0.f;

    // Trigger hit flash
    hitFlashTimer_ = HIT_FLASH_DURATION;

    // Knockback: push away from path direction
    if (pathIndex + 1 < static_cast<int>(waypoints.size())) {
        // No waypoints access here — use velocity-based knockback
    }
    // Simple knockback: offset in opposite direction of travel
    Vec2 moveDir = (position - prevPosition);
    float len = moveDir.length();
    if (len > 0.1f) {
        moveDir = moveDir * (1.f / len);
        knockbackOffset_ = moveDir * (-3.f);  // push back 3px
    }
}
```

실제로 takeDamage에는 waypoints가 없으므로 prevPosition 기반으로 넉백 방향 계산.

- [ ] **Step 3: Enemy::update()에서 hitFlash 타이머 감소**

```cpp
void Enemy::update(float dt, const std::vector<Vec2>& waypoints) {
    // ... existing code ...
    animTime_ += dt;

    // Hit flash countdown
    if (hitFlashTimer_ > 0.f) hitFlashTimer_ -= dt;

    // Knockback decay
    knockbackOffset_ = knockbackOffset_ * std::max(0.f, 1.f - dt * 15.f);

    // ... rest unchanged ...
```

- [ ] **Step 4: Enemy::render()에서 hit flash 렌더링**

```cpp
void Enemy::render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const {
    if (!active) return;

    const auto& tex = *atlas.getTexture();
    Vec2 pos = Vec2::lerp(prevPosition, position, alpha) + knockbackOffset_;

    // Select sprite frame
    const auto& sprite = atlas.getEnemySprite(enemyType);
    const SpriteFrame* frame;
    if (hitFlashTimer_ > 0.f) {
        // Use hit frame during flash
        frame = &sprite.hit;
    } else {
        frame = &sprite.walk.getFrame(animTime_);
    }

    // Hit flash: white tint overlay
    Vec4 tint;
    if (hitFlashTimer_ > 0.f) {
        float flashIntensity = hitFlashTimer_ / HIT_FLASH_DURATION;
        tint = {1.f, 1.f, 1.f, 1.f};
        // Brighten toward white
        tint.x = std::min(1.f, 1.f + flashIntensity * 0.5f);
        tint.y = std::min(1.f, 0.85f + flashIntensity * 0.5f);
        tint.z = std::min(1.f, 0.85f + flashIntensity * 0.5f);
    } else {
        tint = isBoss ? Vec4{1.f, 0.85f, 0.85f, 1.f} : Vec4{1.f, 1.f, 1.f, 1.f};
    }

    batch.draw(tex, pos, {size, size}, frame->uvRect, tint, 0.f, {0.5f, 0.5f});

    // ... HP bar + debuff icons unchanged ...
```

- [ ] **Step 5: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/cpp/engine/Enemy.h app/src/main/cpp/engine/Enemy.cpp
git commit -m "feat: add enemy hit flash (white blink) and knockback on damage"
```

### Task 6: Enemy death particles (사망 파편 이펙트)

**Files:**
- Modify: `app/src/main/cpp/engine/BattleScene.cpp` (updateEnemies)

**목표:** 적이 죽을 때 파편이 흩어지는 burst 파티클 생성.

- [ ] **Step 1: updateEnemies()에 사망 파티클 추가**

현재 (라인 360-377):
```cpp
if (enemy.isDead()) {
    sp_ += static_cast<float>(enemy.spReward);
    waveManager_.onEnemyDefeated();
    killCount_++;
    enemy.active = false;
    enemyPool_.release(&enemy);
}
```

파티클 추가:
```cpp
if (enemy.isDead()) {
    // Death burst particles
    {
        Vec2 deathPos = enemy.position;
        int burstCount = enemy.isBoss ? 20 : 10;
        float burstSpeed = enemy.isBoss ? 120.f : 80.f;

        // Main fragment burst (enemy type color)
        Vec4 fragColor;
        switch (enemy.enemyType) {
            case 0: fragColor = {0.8f, 0.4f, 0.2f, 1.f}; break;
            case 1: fragColor = {0.3f, 0.7f, 0.9f, 1.f}; break;
            case 2: fragColor = {0.6f, 0.8f, 0.3f, 1.f}; break;
            case 3: fragColor = {0.9f, 0.9f, 0.5f, 1.f}; break;
            case 4: fragColor = {0.7f, 0.3f, 0.8f, 1.f}; break;
            default: fragColor = {0.8f, 0.4f, 0.2f, 1.f}; break;
        }
        Vec4 fragEnd = fragColor;
        fragEnd.w = 0.f;

        particles_.burst(deathPos, burstCount, burstSpeed, burstSpeed * 0.5f,
                        fragColor, fragEnd, 0.5f, 5.f, 1.f, BlendMode::Normal);

        // White flash burst (additive)
        particles_.burst(deathPos, burstCount / 2, burstSpeed * 0.7f, 20.f,
                        {1.f, 1.f, 1.f, 0.8f}, {1.f, 1.f, 1.f, 0.f},
                        0.2f, 8.f, 0.f, BlendMode::Additive);

        // Upward sparkles
        for (int i = 0; i < 4; i++) {
            particles_.spawn(
                {deathPos.x + ParticleSystem::randRange(-8.f, 8.f), deathPos.y},
                {ParticleSystem::randRange(-15.f, 15.f), ParticleSystem::randRange(-80.f, -40.f)},
                fragColor, fragEnd,
                ParticleSystem::randRange(0.3f, 0.6f), 3.f, 0.f,
                20.f, BlendMode::Normal
            );
        }
    }

    sp_ += static_cast<float>(enemy.spReward);
    // ... rest unchanged
```

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: 적 사망 시 색상별 파편 + 흰색 플래시 + 상승 스파클

- [ ] **Step 3: Commit**

```bash
git add app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: add enemy death burst particles (fragments + flash + sparkles)"
```

---

## Chunk 4: C++ 투사체 파티클 재활성화

### Task 7: Trail/Impact 파티클 재활성화

**Files:**
- Modify: `app/src/main/cpp/engine/BattleScene.cpp` (updateProjectiles)

**목표:** 현재 "Compose가 처리하니까" 비활성화된 trail/impact 파티클을 재활성화. Compose ProjectileOverlay의 빔 이펙트 위에 C++ 파티클이 추가됨.

- [ ] **Step 1: updateProjectiles()에서 trail 파티클 활성화**

현재 (라인 405-406):
```cpp
// Trail particles disabled — Compose ProjectileOverlay renders family-themed beams.
// C++ particles aren't visible under opaque Compose overlay.
```

교체:
```cpp
// Spawn trail particles along projectile path
spawnProjectileTrail(particles_, proj.position(), proj.velocity(),
                     proj.sourceUnitId);
```

- [ ] **Step 2: impact 파티클도 활성화**

현재 (라인 414):
```cpp
// Impact particles disabled — Compose ProjectileOverlay handles impact visuals.
```

교체:
```cpp
// Spawn impact burst at hit position
spawnProjectileImpact(particles_, hitEnemy->position, proj.sourceUnitId);
```

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: 투사체가 트레일 파티클을 남기고, 적 피격 시 임팩트 버스트 발생

- [ ] **Step 4: Commit**

```bash
git add app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: re-enable C++ projectile trail and impact particles"
```

---

## Chunk 5: 배경/그리드 시각 품질 향상

### Task 8: Grid 렌더링 개선 — 색칠 사각형 제거

**Files:**
- Modify: `app/src/main/cpp/engine/Grid.h` (render 함수)

**목표:** 격자 셀을 색칠된 사각형 대신 반투명하고 세련된 형태로 변경. 유닛이 있는 셀은 미묘한 글로우만.

- [ ] **Step 1: Grid::render()를 개선된 버전으로 교체**

기존 render() (라인 132-185) 전체 교체:

```cpp
void render(SpriteBatch& batch, const SpriteAtlas& atlas) const {
    const auto& tex = *atlas.getTexture();
    const auto& wp = atlas.getWhitePixel();

    // Draw subtle cell indicators (occupied cells only — soft glow)
    for (int r = 0; r < ROWS; r++) {
        for (int c = 0; c < COLS; c++) {
            Unit* u = cells_[r * COLS + c];
            if (!u) continue;

            float x = GRID_X + c * CELL_W;
            float y = GRID_Y + r * CELL_H;

            // Soft grade-colored fill (very low alpha)
            int grade = u->unitDefId / 5;
            Vec4 gradeCol = getGradeColor(grade);
            batch.draw(tex, x + 2.f, y + 2.f, CELL_W - 4.f, CELL_H - 4.f,
                       wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
                       gradeCol.x, gradeCol.y, gradeCol.z, 0.12f);
        }
    }

    // Draw thin grid lines (very subtle)
    constexpr float LINE_W = 0.5f;
    for (int c = 0; c <= COLS; c++) {
        float x = GRID_X + c * CELL_W - LINE_W * 0.5f;
        batch.draw(tex, x, GRID_Y, LINE_W, GRID_H,
                   wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
                   0.3f, 0.5f, 0.7f, 0.15f);
    }
    for (int r = 0; r <= ROWS; r++) {
        float y = GRID_Y + r * CELL_H - LINE_W * 0.5f;
        batch.draw(tex, GRID_X, y, GRID_W, LINE_W,
                   wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
                   0.3f, 0.5f, 0.7f, 0.15f);
    }
}
```

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: 격자가 미묘한 글로우 + 얇은 라인으로 변경

- [ ] **Step 3: Commit**

```bash
git add app/src/main/cpp/engine/Grid.h
git commit -m "feat: improve grid rendering - subtle glow cells + thin lines"
```

### Task 9: 배경 타일맵 밝기 개선

**Files:**
- Modify: `app/src/main/cpp/engine/BattleScene.cpp` (onRender)

**목표:** 배경 잔디 타일의 틴트를 더 밝고 생동감 있게 변경.

- [ ] **Step 1: 배경 렌더링 틴트 조정**

현재 (라인 637-648):
```cpp
batch.draw(tex, x, y, 64.f, 64.f,
           grassTile.uvRect.x, grassTile.uvRect.y,
           grassTile.uvRect.w, grassTile.uvRect.h,
           0.08f, 0.08f, 0.15f, 0.9f);  // 너무 어두움
```

변경:
```cpp
// Slightly varied tint for visual depth
float variation = ((int)(x + y * 7) % 3) * 0.02f;
batch.draw(tex, x, y, 64.f, 64.f,
           grassTile.uvRect.x, grassTile.uvRect.y,
           grassTile.uvRect.w, grassTile.uvRect.h,
           0.15f + variation, 0.18f + variation, 0.25f, 0.95f);
```

- [ ] **Step 2: 경로 렌더링도 밝기 조정**

현재 renderPath() 틴트:
```cpp
0.12f, 0.15f, 0.3f, 0.5f  // 너무 어둡고 반투명
```

변경:
```cpp
0.2f, 0.25f, 0.4f, 0.7f  // 더 밝고 가시적
```

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: 배경과 경로가 더 밝고 시각적으로 풍성

- [ ] **Step 4: Commit**

```bash
git add app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: improve background and path tile brightness"
```

---

## Chunk 6: 소환 연출 강화

### Task 10: SummonEffectOverlay 강화

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/battle/SummonEffectOverlay.kt`

**목표:** 소환 시 더 화려한 연출 — 스프라이트 확대/축소, 빛 줄기, 등급별 더 극적인 차이.

- [ ] **Step 1: 소환 카드에 진입 애니메이션 강화**

```kotlin
// 기존 spring 애니메이션에 회전 + 페이드인 추가
val rotation by animateFloatAsState(
    targetValue = 0f,
    animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
    label = "summonRotation",
)

// 카드에 graphicsLayer 수정
.graphicsLayer {
    scaleX = scale
    scaleY = scale
    rotationZ = (1f - scale) * 15f  // 살짝 회전하며 등장
    alpha = scale.coerceIn(0f, 1f)
}
```

- [ ] **Step 2: 등급 5+ 전체화면 광선 이펙트 추가**

Canvas에서 grade 5+ 시 화면 중앙에서 방사형 광선:
```kotlin
if (grade >= 5) {
    val rayCount = 12
    for (i in 0 until rayCount) {
        val angle = (i.toFloat() / rayCount) * 2f * PI.toFloat() + fxTime * 1.5f
        val rayLen = size.minDimension * 0.8f * bp
        drawLine(
            color = gradeColor.copy(alpha = (1f - bp) * 0.3f),
            start = Offset(cx, cy),
            end = Offset(cx + cos(angle) * rayLen, cy + sin(angle) * rayLen),
            strokeWidth = 6f,
            cap = StrokeCap.Round,
        )
    }
}
```

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: 소환 카드가 회전하며 등장, 고등급은 광선 추가

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/jaygame/ui/battle/SummonEffectOverlay.kt
git commit -m "feat: enhance summon effect with rotation entry and grade 5+ light rays"
```

---

## Chunk 7: BattleField.kt 인터랙션 레이어 정리

### Task 11: BattleField 인터랙션 유지 + 드래그 고스트 정리

**Files:**
- Modify: `app/src/main/java/com/example/jaygame/ui/battle/BattleField.kt`

**목표:** 유닛 드래그 시 고스트 이미지가 여전히 보이도록 유지. unitBitmaps는 드래그용으로만 사용하도록 축소.

- [ ] **Step 1: 드래그 고스트용 비트맵만 유지**

unitBitmaps를 드래그 기능에만 사용하도록 정리. 필요 시 비트맵 로딩은 유지하되, 일반 유닛 렌더링에는 사용하지 않음.

Canvas 안에서 ground platform + 드래그 고스트만 그리도록 정리.

- [ ] **Step 2: Compose ground platform이 C++ 그리드 위에 제대로 위치하는지 확인**

ground platform (Compose)은 반투명이므로 C++ 그리드 렌더링과 조화되어야 함. 필요 시 alpha 조정.

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: 유닛 드래그 시 고스트 이미지 정상 작동

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/jaygame/ui/battle/BattleField.kt
git commit -m "refactor: clean up BattleField - keep interaction + drag ghost only"
```

---

## 파일 변경 요약

| 파일 | 변경 유형 | 내용 |
|------|-----------|------|
| `BattleField.kt` | Modify | 유닛 스프라이트 렌더링 제거, 인터랙션만 유지 |
| `EnemyOverlay.kt` | Modify | 적 렌더링 제거 (빈 Composable) |
| `BattleScene.cpp` | Modify | 유닛/적 렌더링 활성화, 사망 파티클 추가, trail/impact 재활성화, 배경 밝기 |
| `Unit.cpp` | Modify | idle bounce + attack lunge |
| `Enemy.h` | Modify | hitFlash + knockback 필드 |
| `Enemy.cpp` | Modify | hit flash 렌더링, knockback 로직 |
| `Grid.h` | Modify | 격자 렌더링 개선 |
| `SummonEffectOverlay.kt` | Modify | 소환 연출 강화 |
