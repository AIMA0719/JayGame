# JayGame Development Phase Plan

**Project**: JayGame - Mobile Random Defense Game for Android
**Engine**: C++ NDK + OpenGL ES 3.0
**Logical Resolution**: 1280x720 (landscape)
**Target**: 60 FPS fixed timestep (1/60s)
**Date**: 2026-03-09

---

## Engine Infrastructure (COMPLETE)

The following engine systems are built and operational. All game phases build on top of these:

| System | File | Status |
|--------|------|--------|
| Game Loop | `GameEngine.h/.cpp` | Fixed 60Hz timestep, interpolated rendering |
| Sprite Batching | `SpriteBatch.h/.cpp` | 4096 sprites/batch, auto texture-swap flush |
| Input System | `InputSystem.h/.cpp` | Touch, drag, tap detection with screen-to-world |
| Scene Stack | `SceneManager.h/.cpp` | Push/pop/replace with deferred operations |
| Scene Base | `Scene.h` | Virtual interface: update, render, input, resize |
| Entity | `Entity.h` | Transform + SpriteComponent + bounds |
| Components | `Components.h` | Transform (interpolated), SpriteComponent (zOrder) |
| Object Pool | `ObjectPool.h` | Growable pool with free-list, forEach iteration |
| Spatial Hash | `SpatialHash.h` | Grid-based broadphase collision queries |
| Math | `MathTypes.h` | Vec2/3/4, Rect, Mat4 (ortho, inverse, transforms) |
| Sprite | `Sprite.h` | TextureRegion with UV rect |
| Graphics | `GraphicsContext.h/.cpp` | EGL surface management |
| Texture | `TextureAsset.h/.cpp` | Texture loading from Android assets |
| Shader | `Shader.h/.cpp` | GLSL compile/link utilities |

**Base path for all new files**: `app/src/main/cpp/engine/`

---

## Dependency Graph

```
Phase 1 ──────────────────────────────────┐
  Core Battle System                      │
  (Weeks 1-4)                             │
                                          v
Phase 2 ──────────────────────────────────┐
  Merge System + Unit Abilities           │
  (Weeks 5-7)                             │
          │                               │
          v                               v
Phase 3 ──────────────────────────────────┐
  UI/UX + Screen Flow                     │
  (Weeks 7-10)                            │
          │                               │
          v                               v
Phase 4 ──────────────────────────────────┐
  Progression + Economy                   │
  (Weeks 10-13)                           │
          │                               │
          v                               v
Phase 5 ──────────────────────────────────┐
  PvP + Advanced Features                 │
  (Weeks 13-17)                           │
                                          │
                                          v
Phase 6 ────────────────────────────────────
  Polish + Production
  (Weeks 17-20)
```

### Overlap Opportunities

```
Phase 1 ████████████████
Phase 2          ███████████████
Phase 3              ░░░░████████████████
Phase 4                       ░░░░████████████████
Phase 5                                ░░░░████████████████
Phase 6                                         ░░░░████████████████

████ = active development
░░░░ = overlap zone (can start early with partial dependency)

Week:  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20
```

**Parallelizable work:**
- Phase 3 UI layout/widget work can start during Phase 2 (no battle dependency)
- Phase 4 SaveSystem and PlayerData can start during Phase 3 (no UI dependency)
- Phase 5 AudioEngine and ParticleSystem can start during Phase 4 (no economy dependency)
- Phase 6 localization strings can be collected from Phase 3 onward

---

## Phase 1: Core Battle System (핵심 전투 시스템)

**Goal**: Playable single-player battle with basic units that auto-attack enemies walking along a path.

**Duration**: 4 weeks
**Dependencies**: Engine infrastructure only (complete)
**Estimated Files**: 8 new files (4 header + 4 implementation pairs)
**Estimated LOC**: ~2,800

### Files to Create

| File | Purpose | Est. LOC |
|------|---------|----------|
| `engine/Grid.h` | 5x4 board grid: cell positions, coord conversion, placement validation | ~120 |
| `engine/Unit.h` | Base unit class: stats, level, attack timer, target acquisition | ~150 |
| `engine/Unit.cpp` | Unit update logic: target selection via SpatialHash, attack cycle, projectile spawning | ~300 |
| `engine/UnitData.h` | Static data table for 40 units: name, baseATK, atkSpeed, range, element, rarity, sprite IDs | ~250 |
| `engine/Wave.h` | Wave definition struct, wave table (40 waves), boss flags | ~80 |
| `engine/Wave.cpp` | Wave spawner: timer-based enemy emission, HP scaling formula, boss spawning | ~200 |
| `engine/Enemy.h` | Enemy class: HP, speed, armor, path index, status effects container | ~120 |
| `engine/Enemy.cpp` | Enemy update: path following (waypoint interpolation), damage processing, death | ~250 |
| `engine/Projectile.h` | Projectile class: position, velocity, damage, source unit ref, homing flag | ~80 |
| `engine/Projectile.cpp` | Projectile update: movement, collision with enemy bounds, damage application | ~180 |
| `engine/BattleScene.h` | Main battle scene: owns Grid, ObjectPools, SpatialHash, game state | ~150 |
| `engine/BattleScene.cpp` | Battle orchestration: summon logic, wave management, win/lose detection, SP economy | ~600 |
| `engine/BattleHUD.h` | HUD overlay: wave counter, SP display, HP bar, summon button | ~80 |
| `engine/BattleHUD.cpp` | HUD rendering using SpriteBatch (number sprites, bar sprites) | ~240 |

### Technical Design

**Grid Layout (5 columns x 4 rows)**:
```
Logical coords (1280x720):
- Grid area: x=140..740, y=60..660 (600x600 region)
- Cell size: 120x150
- Enemy path runs along the right side and bottom

    Col0   Col1   Col2   Col3   Col4
Row0 [    ] [    ] [    ] [    ] [    ]
Row1 [    ] [    ] [    ] [    ] [    ]
Row2 [    ] [    ] [    ] [    ] [    ]
Row3 [    ] [    ] [    ] [    ] [    ]

Path: top-right -> bottom-right -> bottom-left (L-shaped)
```

**SP Economy**:
- Start with 100 SP
- Summon cost: 50 SP (increases by +10 per summon, caps at 200)
- SP gain: 10 SP per enemy kill, 50 SP per wave clear
- SP passive regen: 2 SP/second

**Wave Scaling Formula**:
```cpp
float enemyHP(int wave) {
    return 100.f * pow(1.12f, wave - 1);  // ~31x HP at wave 40
}
float enemySpeed(int wave) {
    return 60.f + wave * 1.5f;  // pixels/sec, 60..120
}
int enemyCount(int wave) {
    return 8 + wave / 2;  // 8..28 enemies per wave
}
bool isBoss(int wave) {
    return wave % 5 == 0;  // boss every 5 waves
}
// Boss: 5x HP, 0.6x speed, 2x size, 3x SP reward
```

**Target Acquisition**:
```
Each unit has an attack range (in logical pixels).
Every attack cooldown reset:
  1. Query SpatialHash with circle(unit.pos, unit.range)
  2. Filter to active enemies
  3. Select nearest enemy (or first-in-path for "first" targeting)
  4. Spawn projectile toward target
```

**Object Pool Sizing**:
```
ObjectPool<Unit>       capacity: 20  (5x4 grid max)
ObjectPool<Enemy>      capacity: 64  (max on screen)
ObjectPool<Projectile> capacity: 128 (many in flight)
```

### Acceptance Criteria

- [ ] **AC-1.1**: 5x4 grid renders with visible cell borders. Tapping an empty cell highlights it.
- [ ] **AC-1.2**: Summon button spawns a random unit from a 5-unit deck onto a random empty cell. SP is deducted. Button is disabled when SP is insufficient or grid is full.
- [ ] **AC-1.3**: Enemies spawn from the path start point, follow waypoints smoothly, and exit at the path end. Player loses 1 HP per enemy that exits.
- [ ] **AC-1.4**: Units auto-attack: when an enemy enters range, the unit fires a projectile. Projectile hits enemy, deals damage, enemy HP bar decreases. Enemy dies at 0 HP and grants SP.
- [ ] **AC-1.5**: 40 waves progress automatically. Wave counter displays in HUD. 3-second delay between waves.
- [ ] **AC-1.6**: Boss enemies (every 5 waves) appear larger, have 5x HP, move slower, and grant 3x SP.
- [ ] **AC-1.7**: SP display updates in real-time. Passive SP regen of 2/second works correctly.
- [ ] **AC-1.8**: HUD displays: current wave, total SP, player HP (starts at 20), enemy count remaining.
- [ ] **AC-1.9**: Game ends with "Victory" at wave 40 clear or "Defeat" at 0 HP.
- [ ] **AC-1.10**: Stable 60 FPS with 20 units and 30 enemies on screen simultaneously.

### Test Scenarios

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| T1.1 | Tap summon with 100 SP, cost is 50 | Unit placed, SP = 50, next cost = 60 |
| T1.2 | Tap summon with 40 SP, cost is 50 | Nothing happens, button visually disabled |
| T1.3 | Fill all 20 grid cells, tap summon | Nothing happens, button visually disabled |
| T1.4 | Enemy walks full path, no units | Player HP decreases by 1, enemy despawns |
| T1.5 | Unit attacks enemy, enemy HP = 1 hit | Enemy dies, SP increases, kill count updates |
| T1.6 | Wave 5 boss spawns | Boss is 2x size, 5x HP, 0.6x speed |
| T1.7 | All wave 40 enemies killed | Victory screen (or placeholder text) |
| T1.8 | Player HP reaches 0 | Defeat screen (or placeholder text) |
| T1.9 | 20 units + 30 enemies active | No frame drops below 55 FPS |
| T1.10 | SP passive regen over 10 seconds | SP increases by ~20 (2/sec x 10s) |

### Risk Factors

| Risk | Impact | Mitigation |
|------|--------|------------|
| Path following feels jerky | Medium | Use cubic interpolation between waypoints, not linear |
| SpatialHash query too slow with many enemies | Medium | Cell size tuning (try 128px cells); profile with 64 enemies |
| Projectile-enemy collision misses at high speed | Low | Use swept circle test or reduce projectile speed |
| 40-unit stat table is tedious | Low | Start with 10 representative units, fill remaining in Phase 2 |
| Touch input conflicts (summon vs grid tap) | Medium | Clear UI zones: summon button in bottom-right, grid in center-left |

---

## Phase 2: Merge System + Unit Abilities (합성 + 유닛 능력)

**Goal**: Full drag-to-merge mechanics, diverse unit abilities, and buff/debuff system.

**Duration**: 3 weeks
**Dependencies**: Phase 1 (Unit, Grid, BattleScene must be complete)
**Estimated Files**: 4 new files (4 header/impl pairs)
**Estimated LOC**: ~2,200

### Files to Create

| File | Purpose | Est. LOC |
|------|---------|----------|
| `engine/MergeSystem.h` | Merge validation, drag state machine, merge execution | ~100 |
| `engine/MergeSystem.cpp` | Drag-and-drop: pick unit, show ghost, drop on target, validate merge, execute level-up | ~350 |
| `engine/Ability.h` | Ability type enum, AbilityData struct, AbilityInstance with cooldown/duration | ~180 |
| `engine/Ability.cpp` | Ability execution: splash damage, slow, DoT, chain lightning, buff aura, heal, etc. | ~500 |
| `engine/Buff.h` | Buff/debuff struct: type, magnitude, duration, source | ~80 |
| `engine/Buff.cpp` | Buff container on enemies/units: apply, tick, expire, stack rules | ~250 |
| `engine/CombinationTable.h` | Hidden recipe table: unitA + unitB = unitC (special combinations) | ~120 |

### Files to Modify

| File | Changes |
|------|---------|
| `engine/Unit.h/.cpp` | Add ability slot, merge level tracking, drag state |
| `engine/Enemy.h/.cpp` | Add buff container, apply debuff effects to speed/armor |
| `engine/BattleScene.h/.cpp` | Integrate MergeSystem, process abilities per frame |
| `engine/Grid.h` | Add swap/move unit between cells, cell highlight for merge target |

### Technical Design

**Merge Rules**:
```
Standard Merge:
  IF unit_A.typeID == unit_B.typeID AND unit_A.level == unit_B.level
  THEN unit_A.level += 1, unit_B is destroyed
  Max level: 7 (requires 64 level-1 units theoretically)

Hidden Combination:
  IF (unit_A.typeID, unit_B.typeID) exists in CombinationTable
  AND unit_A.level >= required_level AND unit_B.level >= required_level
  THEN replace both with CombinationTable result unit at level 1

Merge rejects: different type + different level = swap positions instead
```

**Drag State Machine**:
```
IDLE -> [touch on unit] -> DRAGGING
DRAGGING -> [release on same-type unit] -> MERGE_EXECUTE -> IDLE
DRAGGING -> [release on different unit] -> SWAP_EXECUTE -> IDLE
DRAGGING -> [release on empty cell] -> MOVE_EXECUTE -> IDLE
DRAGGING -> [release outside grid] -> CANCEL -> IDLE
```

**Ability Categories (all 40 units)**:

| Category | Count | Examples |
|----------|-------|---------|
| Single Target DPS | 8 | High ATK, fast attack speed, no special |
| Splash/AoE | 6 | Damage in radius around impact point |
| Slow | 5 | Reduce enemy speed by 30-60% for 2-3 seconds |
| DoT (Damage over Time) | 4 | Poison/burn: X damage per second for Y seconds |
| Chain/Bounce | 3 | Projectile bounces to N nearby enemies |
| Buff Aura | 4 | Boost ATK/speed of adjacent units |
| Debuff | 3 | Reduce enemy armor, increase damage taken |
| Summon | 2 | Spawn temporary minion units |
| Heal/Shield | 2 | Restore player HP or grant shield to units |
| Execute | 2 | Bonus damage when enemy HP < 30% |
| Utility | 1 | Earn bonus SP per kill |

**Buff System**:
```cpp
struct Buff {
    enum Type { SLOW, DOT, ARMOR_BREAK, ATK_UP, SPD_UP, SHIELD };
    Type type;
    float magnitude;   // 0.0-1.0 for percentage, or absolute value
    float duration;     // seconds remaining
    float tickTimer;    // for DoT: time until next tick
    float tickInterval; // for DoT: 0.5s typical
    int sourceUnitID;   // for stacking rules
};

// Stacking: same source refreshes duration. Different sources stack up to 3.
```

### Acceptance Criteria

- [ ] **AC-2.1**: Long-press a unit lifts it. Ghost sprite follows finger. Original cell shows faded outline.
- [ ] **AC-2.2**: Drop unit on same-type, same-level unit: both merge into level+1 unit with visual flash effect. New unit keeps the position of the drop target.
- [ ] **AC-2.3**: Drop unit on different-type unit: units swap positions smoothly.
- [ ] **AC-2.4**: Drop unit on empty cell: unit moves to that cell.
- [ ] **AC-2.5**: Drop unit outside grid: unit returns to original cell with snap-back animation.
- [ ] **AC-2.6**: Level 7 unit cannot merge further. Attempting to merge two level-7 same-type units does nothing.
- [ ] **AC-2.7**: All 40 units have a distinct ability that fires during combat. Each ability has a visible effect (color tint, particle placeholder, or area indicator).
- [ ] **AC-2.8**: Slow debuff visibly reduces enemy movement speed. Slow icon appears on enemy.
- [ ] **AC-2.9**: DoT damage ticks correctly (verified with HP bar decreasing over time without projectile hits).
- [ ] **AC-2.10**: Buff aura units boost adjacent allies. Moving a buffed unit out of range removes the buff.
- [ ] **AC-2.11**: At least 5 hidden combinations exist and produce unique units not available from summoning.
- [ ] **AC-2.12**: Unit level is displayed as a number or star indicator on the unit sprite.

### Test Scenarios

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| T2.1 | Merge two level-1 Archer units | Level-2 Archer appears, one cell freed |
| T2.2 | Merge level-2 Archer + level-1 Archer | No merge, units swap positions |
| T2.3 | Drag Archer onto Mage | Units swap cells |
| T2.4 | Drag unit to empty cell | Unit moves to new cell |
| T2.5 | Hidden combo: Fire Mage + Ice Mage | Storm Mage appears (unique unit) |
| T2.6 | Slow unit attacks enemy | Enemy speed reduced, debuff icon visible |
| T2.7 | Buff aura unit placed next to DPS unit | DPS unit ATK stat increases |
| T2.8 | Remove buff aura unit from board | Adjacent units lose ATK bonus |
| T2.9 | Two DoT sources on same enemy | Both tick independently, HP drops faster |
| T2.10 | Merge during active combat | No crash, combat continues seamlessly |

### Risk Factors

| Risk | Impact | Mitigation |
|------|--------|------------|
| Drag-and-drop feels unresponsive | High | Tune touch thresholds; begin drag after 150ms hold or 10px movement |
| Ability balance is wildly off | High | Implement damage logging; tune in Phase 6 |
| Buff stacking causes exponential damage | Medium | Hard cap: max 3 stacks, max 100% buff magnitude |
| Hidden combos are undiscoverable | Low | Add hint system in Phase 3 collection screen |
| 40 unique ability implementations is large | Medium | Use data-driven ability system with shared behaviors |

---

## Phase 3: UI/UX + Screen Flow (화면 구성)

**Goal**: Complete screen flow from app launch to battle and back, with polished UI widgets.

**Duration**: 3 weeks (can start 1 week early, overlapping Phase 2)
**Dependencies**: Phase 1 (SceneManager, Scene), Phase 2 (merge for deck editor)
**Estimated Files**: 9 new files
**Estimated LOC**: ~3,200

### Files to Create

| File | Purpose | Est. LOC |
|------|---------|----------|
| `engine/UISystem.h` | UI widget base: Button, Panel, ScrollView, Label, ProgressBar, DragSlot | ~200 |
| `engine/UISystem.cpp` | Widget rendering, hit testing, touch dispatch to widgets | ~400 |
| `engine/TextRenderer.h` | Bitmap font loading, glyph table, text measurement | ~100 |
| `engine/TextRenderer.cpp` | Text rendering via SpriteBatch: drawText, drawNumber, alignment, wrapping | ~350 |
| `engine/LobbyScene.h` | Lobby layout: profile bar, deck preview, battle button, mode selection | ~80 |
| `engine/LobbyScene.cpp` | Lobby logic: navigate to deck edit, collection, settings, or battle | ~300 |
| `engine/DeckEditScene.h` | Deck builder: unit inventory grid, 5-slot deck bar, drag to assign | ~80 |
| `engine/DeckEditScene.cpp` | Deck editing: scroll through owned units, drag into deck slots, save | ~350 |
| `engine/CollectionScene.h` | Unit collection: grid of all units, tap to view details | ~80 |
| `engine/CollectionScene.cpp` | Collection rendering: unit cards, stats display, locked/unlocked state | ~300 |
| `engine/ResultScene.h` | Battle result: win/lose, rewards earned, continue button | ~60 |
| `engine/ResultScene.cpp` | Result display: wave reached, SP earned, gold earned, unit cards gained | ~200 |
| `engine/SettingsScene.h` | Settings: sound toggle, music toggle, graphics quality, language | ~60 |
| `engine/SettingsScene.cpp` | Settings logic: read/write preferences, apply changes | ~150 |

### Files to Modify

| File | Changes |
|------|---------|
| `engine/BattleScene.cpp` | Add pause button, integrate BattleHUD properly, push ResultScene on end |
| `engine/BattleHUD.cpp` | Use TextRenderer for numbers, UISystem buttons for pause/speed |
| `main.cpp` | Initial scene = LobbyScene instead of TestScene |

### Technical Design

**Screen Flow**:
```
App Launch
    |
    v
LobbyScene ──────> BattleScene ──────> ResultScene
    |                   |                    |
    |                   v                    |
    |              (pause overlay)           |
    |                                        |
    +──> DeckEditScene                       |
    |                                        |
    +──> CollectionScene                     |
    |                                        |
    +──> SettingsScene                       |
    |                                        |
    <────────────────────────────────────────+
```

**UI Widget System**:
```cpp
class UIWidget {
public:
    Rect bounds;
    bool visible = true;
    bool enabled = true;
    int zOrder = 0;

    virtual void onRender(SpriteBatch& batch, TextRenderer& text) = 0;
    virtual bool onTouch(const InputEvent& event) = 0;
};

class UIButton : public UIWidget {
    enum State { NORMAL, PRESSED, DISABLED };
    TextureRegion normalTex, pressedTex, disabledTex;
    std::string label;
    std::function<void()> onClick;
};

class UIPanel : public UIWidget {
    std::vector<UIWidget*> children;
    Vec4 backgroundColor;
};

class UIScrollView : public UIWidget {
    float scrollOffset;
    float contentHeight;
    std::vector<UIWidget*> children;
};
```

**TextRenderer - Bitmap Font**:
```
Font texture: 16x16 grid of ASCII characters (256 glyphs)
Each glyph: 32x48 pixels in texture
Rendering: calculate UV per character, batch via SpriteBatch
Support: ASCII + Korean Hangul block (U+AC00..U+D7A3) via second texture

drawText(batch, "Wave 15", x, y, scale, color, align)
drawNumber(batch, 12345, x, y, scale, color)  // optimized for frequently changing numbers
```

**Scene Transitions**:
```
Transition types:
  - FADE: alpha 1.0 -> 0.0 (old scene), 0.0 -> 1.0 (new scene), 0.3s each
  - SLIDE_LEFT: old slides left, new slides in from right
  - NONE: instant swap

Implementation: SceneManager handles transition state
  - transitionPhase_: NONE | FADE_OUT | FADE_IN
  - transitionAlpha_: 0.0 .. 1.0
  - Both old and new scenes exist during transition
```

### Acceptance Criteria

- [ ] **AC-3.1**: App launches directly into LobbyScene with profile area (player name, trophy count, level), deck preview (5 unit icons), and "Battle" button.
- [ ] **AC-3.2**: TextRenderer displays ASCII text at variable sizes. Numbers render correctly for HUD counters (SP, wave, HP).
- [ ] **AC-3.3**: Tapping "Battle" transitions to BattleScene with a fade effect (0.3s).
- [ ] **AC-3.4**: Deck Edit screen shows all owned units in a scrollable grid. Dragging a unit onto one of 5 deck slots assigns it. Deck saves on exit.
- [ ] **AC-3.5**: Collection screen shows all 40 units in a grid. Owned units are full color; locked units are silhouette. Tapping a unit shows stats panel.
- [ ] **AC-3.6**: Result screen appears after battle end. Shows wave reached, gold earned, "Continue" button returns to lobby.
- [ ] **AC-3.7**: Settings screen has toggle buttons for sound and music. Changes persist across app restarts.
- [ ] **AC-3.8**: All buttons have visual feedback (pressed state) and audio feedback placeholder.
- [ ] **AC-3.9**: Back navigation works: Android back button pops current scene or shows exit confirmation on lobby.
- [ ] **AC-3.10**: No visual glitches during scene transitions. No input accepted during transition animation.

### Test Scenarios

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| T3.1 | Launch app fresh | Lobby renders with default deck, 0 trophies |
| T3.2 | Tap Battle from lobby | Fade transition, BattleScene starts |
| T3.3 | Win battle, tap Continue | Returns to lobby, gold updated |
| T3.4 | Open Deck Edit, drag unit to slot 3 | Unit appears in slot 3, saved on back |
| T3.5 | Open Collection, tap locked unit | Stats show with "Locked" overlay, no interaction |
| T3.6 | Open Settings, toggle sound off, relaunch app | Sound remains off after relaunch |
| T3.7 | Press Android back on lobby | Exit confirmation dialog appears |
| T3.8 | Rapid scene switching (lobby->deck->lobby->collection) | No crashes, no memory growth |
| T3.9 | TextRenderer: display "Wave 40" and Korean "웨이브 40" | Both render correctly |
| T3.10 | Scroll unit list in DeckEdit with 40 units | Smooth scrolling, no visual tearing |

### Risk Factors

| Risk | Impact | Mitigation |
|------|--------|------------|
| Bitmap font Korean coverage is huge (11,172 Hangul syllables) | High | Use pre-rendered common subset (~2,000 chars) or use FreeType fallback |
| ScrollView touch conflicts with drag-to-deck | Medium | Priority: drag starts on long-press, scroll on swipe |
| Scene transition memory spike (two scenes in memory) | Medium | Lazy-load textures; old scene releases assets in onExit() |
| UI layout doesn't scale across aspect ratios | Medium | Use anchoring system: top-left, center, bottom-right relative to logical viewport |
| Button hit targets too small on small phones | Low | Minimum touch target: 80x80 logical pixels |

---

## Phase 4: Progression + Economy (진행/경제 시스템)

**Goal**: Meta-game loop that keeps players engaged between battles.

**Duration**: 3 weeks (can start 1 week early, overlapping Phase 3)
**Dependencies**: Phase 3 (UI scenes for displaying progression), Phase 1 (BattleScene for rewards)
**Estimated Files**: 7 new files
**Estimated LOC**: ~2,500

### Files to Create

| File | Purpose | Est. LOC |
|------|---------|----------|
| `engine/PlayerData.h` | Player state: owned units, levels, currencies, trophies, stats | ~120 |
| `engine/PlayerData.cpp` | PlayerData singleton: getters, setters, validation | ~200 |
| `engine/Currency.h` | Currency types (Gold, Diamond), transaction logic, overflow protection | ~80 |
| `engine/Currency.cpp` | Add/spend/check currency, transaction log for debugging | ~150 |
| `engine/UnitUpgrade.h` | Upgrade requirements: duplicate cards + gold cost per level | ~80 |
| `engine/UnitUpgrade.cpp` | Upgrade execution: validate requirements, apply stat boost, consume resources | ~200 |
| `engine/Achievement.h` | Achievement definitions: ID, condition type, threshold, reward | ~100 |
| `engine/Achievement.cpp` | Achievement tracker: check conditions per event, unlock, grant rewards | ~250 |
| `engine/DailyLogin.h` | Daily login calendar: day tracking, reward table (7-day cycle) | ~60 |
| `engine/DailyLogin.cpp` | Login check, reward claim, streak tracking, reset logic | ~150 |
| `engine/SeasonPass.h` | Season pass: free track rewards, XP requirements per tier | ~80 |
| `engine/SeasonPass.cpp` | XP gain from battles, tier unlock, reward claim | ~200 |
| `engine/SaveSystem.h` | Save/load interface: serialize PlayerData to JSON, store via JNI SharedPreferences | ~80 |
| `engine/SaveSystem.cpp` | JSON serialization, JNI calls to Android SharedPreferences, integrity check | ~350 |

### Files to Modify

| File | Changes |
|------|---------|
| `engine/ResultScene.cpp` | Award gold/cards based on battle performance |
| `engine/LobbyScene.cpp` | Show currency, daily login popup, season pass progress |
| `engine/CollectionScene.cpp` | Add upgrade button per unit, show card count/cost |
| `engine/BattleScene.cpp` | Track achievement events (kills, waves, merges) |

### Technical Design

**Currency Design**:
```
Gold:   earned from battles, daily login, achievements
        spent on: unit upgrades, re-rolls
        soft cap: 999,999

Diamond: premium currency (earned slowly from achievements, season pass)
         spent on: cosmetics (future), special summons
         hard cap: 99,999
```

**Unit Upgrade System**:
```
Level 1 -> 2:   2 cards  + 100 gold
Level 2 -> 3:   4 cards  + 200 gold
Level 3 -> 4:   10 cards + 500 gold
Level 4 -> 5:   20 cards + 1000 gold
Level 5 -> 6:   50 cards + 2000 gold
Level 6 -> 7:   100 cards + 5000 gold

Stat boost per level: +10% ATK, +5% attack speed
```

**Trophy System**:
```
Win:  +25 trophies
Lose: -15 trophies (floor at 0)
Ranks:
  Bronze:   0-999
  Silver:   1000-1999
  Gold:     2000-2999
  Diamond:  3000-3999
  Master:   4000+
```

**Achievement Categories (20 achievements)**:
```
Battle:     "Win 10 battles", "Reach wave 40", "Kill 1000 enemies"
Merge:      "Merge 100 times", "Create a level 7 unit"
Collection: "Own 10 unique units", "Own all 40 units"
Economy:    "Earn 10000 gold", "Upgrade a unit to level 5"
Special:    "Win without losing HP", "Win with only 1 unit type"
```

**Save Data Format (JSON)**:
```json
{
  "version": 1,
  "player": {
    "name": "Player",
    "gold": 1500,
    "diamonds": 50,
    "trophies": 1250,
    "xp": 3400,
    "level": 12
  },
  "units": {
    "archer_01": { "owned": true, "cards": 15, "level": 3 },
    "mage_fire": { "owned": true, "cards": 8, "level": 2 }
  },
  "deck": ["archer_01", "mage_fire", "knight_01", "healer_01", "assassin_01"],
  "achievements": { "win_10": true, "kill_1000": false },
  "dailyLogin": { "lastDate": "2026-03-09", "streak": 5 },
  "seasonPass": { "xp": 3400, "claimedTiers": [1, 2, 3] },
  "settings": { "sound": true, "music": true, "language": "ko" },
  "checksum": "a1b2c3d4"
}
```

**JNI Save Bridge**:
```cpp
// C++ side
void SaveSystem::save(const std::string& json) {
    JNIEnv* env = getJNIEnv();
    jclass cls = env->FindClass("com/jaygame/SaveBridge");
    jmethodID mid = env->GetStaticMethodID(cls, "save", "(Ljava/lang/String;)V");
    jstring jstr = env->NewStringUTF(json.c_str());
    env->CallStaticVoidMethod(cls, mid, jstr);
}

// Java side (SaveBridge.java)
public static void save(String json) {
    SharedPreferences prefs = context.getSharedPreferences("jaygame", MODE_PRIVATE);
    prefs.edit().putString("save_data", json).apply();
}
```

### Acceptance Criteria

- [ ] **AC-4.1**: Gold is awarded after each battle (10 gold per wave cleared). Gold displays correctly in lobby.
- [ ] **AC-4.2**: Unit upgrade screen: shows current level, cards owned/required, gold cost. Tapping "Upgrade" consumes resources and increments level.
- [ ] **AC-4.3**: Insufficient resources for upgrade: button is grayed out, shows "Need X more cards" or "Need X more gold".
- [ ] **AC-4.4**: Achievement popup appears when an achievement is unlocked during or after battle.
- [ ] **AC-4.5**: Daily login popup appears on first launch each day. Shows 7-day calendar with today highlighted. Claiming gives correct reward.
- [ ] **AC-4.6**: Season pass shows free track with 30 tiers. XP bar fills based on battles played. Rewards claimable per tier.
- [ ] **AC-4.7**: Game data persists across app restart. Kill the app, relaunch: all currencies, units, levels intact.
- [ ] **AC-4.8**: Trophy count updates after each battle. Rank badge in lobby matches trophy count.
- [ ] **AC-4.9**: Save data integrity: corrupted save file triggers "Data Corrupted" warning and resets to default.
- [ ] **AC-4.10**: Currency overflow protection: gold cannot exceed 999,999.

### Test Scenarios

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| T4.1 | Win battle, clear 20 waves | +200 gold, gold saves correctly |
| T4.2 | Upgrade unit: have 4 cards + 200 gold at level 2 | Level becomes 3, 0 cards remain, 0 gold deducted |
| T4.3 | Upgrade unit: have 3 cards (need 4) | Upgrade button disabled |
| T4.4 | Kill 1000th enemy total | "Kill 1000 enemies" achievement popup |
| T4.5 | Open app on day 5 of streak | Day 5 reward claimable, days 1-4 marked claimed |
| T4.6 | Earn enough XP for season pass tier 10 | Tier 10 reward claimable |
| T4.7 | Force-quit app mid-battle, relaunch | Player data from before battle is intact |
| T4.8 | Win 3 battles in a row | Trophies increase by 75 total |
| T4.9 | Manually corrupt save data in SharedPreferences | App shows warning, resets to default |
| T4.10 | Earn gold to exceed 999,999 cap | Gold stays at 999,999 |

### Risk Factors

| Risk | Impact | Mitigation |
|------|--------|------------|
| JNI save call crashes on certain Android versions | High | Wrap all JNI in try-catch on Java side; test on API 24, 28, 31, 33 |
| Save data corruption from power loss during write | High | Write to temp file first, then rename (atomic write) |
| Economy inflation: players accumulate gold too fast | Medium | Tune gold rewards; add gold sinks (re-roll, cosmetics) |
| Achievement conditions checked every frame = slow | Low | Check only on relevant events, not every frame |
| Season pass feels grindy | Medium | 50 XP per battle, 30 tiers x 100 XP = ~60 battles for completion |

---

## Phase 5: PvP + Advanced Features (PvP + 고급 기능)

**Goal**: Real-time PvP mode and production-quality audio/visual effects.

**Duration**: 4 weeks
**Dependencies**: Phase 4 (PlayerData, trophies for matchmaking), Phase 2 (full battle system)
**Estimated Files**: 6 new files
**Estimated LOC**: ~3,500

### Files to Create

| File | Purpose | Est. LOC |
|------|---------|----------|
| `engine/NetworkManager.h` | WebSocket client, message serialization, connection state | ~150 |
| `engine/NetworkManager.cpp` | Connect, send, receive, reconnect, heartbeat, message queue | ~500 |
| `engine/PvPScene.h` | PvP battle: split-screen or mirrored view, opponent state sync | ~120 |
| `engine/PvPScene.cpp` | PvP orchestration: receive opponent actions, simulate both boards, determine winner | ~500 |
| `engine/Matchmaking.h` | Matchmaking request, trophy-based matching, timeout handling | ~80 |
| `engine/Matchmaking.cpp` | Queue management, server communication, match found callback | ~200 |
| `engine/SpeedControl.h` | Game speed multiplier: 1x, 2x, 3x with UI toggle | ~40 |
| `engine/SpeedControl.cpp` | Apply speed multiplier to dt in update loop, visual indicator | ~80 |
| `engine/ParticleSystem.h` | Particle emitter, particle struct, emission patterns | ~150 |
| `engine/ParticleSystem.cpp` | Particle update (position, alpha, scale over lifetime), batch rendering | ~350 |
| `engine/AudioEngine.h` | Audio playback: BGM (streaming), SFX (one-shot), volume control | ~120 |
| `engine/AudioEngine.cpp` | OpenSL ES integration: buffer queues, asset loading, mixing | ~450 |
| `engine/Tutorial.h` | Tutorial step definitions, highlight areas, message boxes | ~80 |
| `engine/Tutorial.cpp` | Tutorial state machine: step progression, forced actions, skip option | ~250 |

### Files to Modify

| File | Changes |
|------|---------|
| `engine/BattleScene.cpp` | Integrate SpeedControl, ParticleSystem, AudioEngine |
| `engine/LobbyScene.cpp` | Add PvP mode button, matchmaking UI |
| `engine/GameEngine.cpp` | Apply speed multiplier to fixed timestep accumulator |
| `engine/Unit.cpp` | Trigger particles on attack, play SFX |
| `engine/Enemy.cpp` | Trigger particles on death |
| `engine/MergeSystem.cpp` | Trigger merge particles and SFX |

### Technical Design

**PvP Architecture (Phase 1: AI Opponent)**:
```
Initial implementation uses AI opponent (no server required):
  - AI makes decisions every 2-3 seconds
  - AI summons units, merges when beneficial
  - AI difficulty scales with player trophy count

Phase 2 (future): Real server-based PvP
  - WebSocket to game server
  - Lockstep or input-delay netcode
  - Server authoritative on wave spawning
```

**PvP AI Decision Logic**:
```
Every 2 seconds:
  1. If SP >= summon_cost AND empty_cells > 0: summon (80% chance)
  2. Scan for mergeable pairs: merge best pair (50% chance)
  3. If wave > 20 AND SP > 200: summon aggressively (100% chance)

Difficulty scaling:
  Bronze:   AI reaction time 3s, suboptimal merges
  Silver:   AI reaction time 2.5s, decent merges
  Gold:     AI reaction time 2s, good merges, uses combos
  Diamond+: AI reaction time 1.5s, optimal play
```

**PvP Battle Rules**:
```
Both players face the same waves simultaneously.
Killing an enemy sends a "ghost" enemy to opponent's board (50% HP).
First player to reach 0 HP loses.
If both survive all waves: player with more remaining HP wins.
Tiebreaker: player with more total damage dealt wins.
```

**Particle System**:
```cpp
struct Particle {
    Vec2 position;
    Vec2 velocity;
    float lifetime;     // total lifetime
    float age;          // current age
    float size;         // current size (can animate)
    float startSize, endSize;
    Vec4 startColor, endColor;
    float rotation;
    float rotationSpeed;
};

class ParticleEmitter {
    ObjectPool<Particle> particles_{256};
    Vec2 position;
    float emissionRate;      // particles per second
    float emissionTimer;
    // Shape: POINT, CIRCLE, RECT
    // Velocity: random in range
    // Lifetime: random in range
};

// Pre-built effects:
enum ParticleEffect {
    EFFECT_ATTACK_HIT,      // small burst at impact point
    EFFECT_ENEMY_DEATH,     // explosion outward
    EFFECT_MERGE_FLASH,     // sparkle ring
    EFFECT_LEVEL_UP,        // upward stars
    EFFECT_BUFF_AURA,       // rotating ring around unit
    EFFECT_SLOW_HIT,        // blue ice crystals
    EFFECT_FIRE_DOT,        // lingering flame
    EFFECT_HEAL,            // green crosses rising
};
```

**Audio Engine (OpenSL ES)**:
```
BGM: 1 streaming track, .ogg format, looping
SFX: up to 8 simultaneous one-shot sounds
Sound categories:
  - UI:     button_tap, merge_success, merge_fail, scene_transition
  - Battle: attack_hit, enemy_death, boss_spawn, wave_start
  - Merge:  merge_sparkle, level_up_fanfare
  - Result: victory_jingle, defeat_jingle

Volume: master, bgm, sfx (each 0.0 - 1.0)
Asset format: .ogg (Vorbis), 44.1kHz, mono for SFX, stereo for BGM
```

**Speed Control**:
```
Multipliers: 1.0x (normal), 2.0x (fast), 3.0x (ultra fast)
Implementation: multiply dt before passing to game logic
  - Does NOT affect rendering interpolation
  - Does NOT affect UI input responsiveness
  - Only affects: enemy movement, unit attack timers, projectile speed, wave timers
Toggle button in BattleHUD, cycles through 1x -> 2x -> 3x -> 1x
```

**Tutorial System**:
```
5-step first-battle tutorial:
  Step 1: "Tap Summon to place your first unit" (highlight summon button)
  Step 2: "Enemies are coming! Your unit will attack automatically" (highlight path)
  Step 3: "Earn SP by killing enemies" (highlight SP counter)
  Step 4: "Summon more units to defend" (highlight summon button again)
  Step 5: "Drag matching units to merge them!" (highlight two same units)

Triggered on first ever battle.
Skip button available.
Tutorial flag saved in PlayerData.
```

### Acceptance Criteria

- [ ] **AC-5.1**: PvP button in lobby starts matchmaking screen with "Searching..." animation. After 3 seconds, AI opponent is matched.
- [ ] **AC-5.2**: PvP battle shows both player boards (minimap of opponent's board on right side). Both boards progress through same waves.
- [ ] **AC-5.3**: Killing an enemy in PvP sends a ghost enemy to opponent. Ghost enemy appears on opponent's path with correct HP.
- [ ] **AC-5.4**: PvP ends when one player reaches 0 HP. Winner gains trophies, loser loses trophies.
- [ ] **AC-5.5**: Speed control button cycles 1x -> 2x -> 3x. Game visibly speeds up. UI remains responsive at all speeds.
- [ ] **AC-5.6**: Attack impacts show particle burst. Enemy death shows explosion particles. Merge shows sparkle effect.
- [ ] **AC-5.7**: At least 8 distinct particle effects exist for different ability types.
- [ ] **AC-5.8**: BGM plays on lobby and battle scenes (different tracks). SFX plays for attacks, deaths, merges, UI taps.
- [ ] **AC-5.9**: Sound can be toggled off in settings. Volume changes take effect immediately.
- [ ] **AC-5.10**: Tutorial plays on first battle entry. Guides player through 5 steps. Can be skipped. Does not replay.
- [ ] **AC-5.11**: 3x speed with 20 units, 30 enemies, and particles maintains above 45 FPS.

### Test Scenarios

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| T5.1 | Start PvP match | AI opponent found, dual-board battle begins |
| T5.2 | Kill enemy in PvP | Ghost enemy appears on opponent's board |
| T5.3 | AI opponent reaches 0 HP | Player wins, +25 trophies |
| T5.4 | Toggle speed to 3x mid-battle | Game runs 3x faster, UI still responsive |
| T5.5 | Unit attacks enemy | Particle burst at impact point, SFX plays |
| T5.6 | Enemy dies | Death explosion particles, death SFX |
| T5.7 | Merge two units | Sparkle particles, merge SFX, level-up visual |
| T5.8 | Toggle sound off in settings, return to battle | No audio plays |
| T5.9 | First ever battle entry | Tutorial overlay appears, step 1 shown |
| T5.10 | Complete tutorial, start second battle | No tutorial shown |
| T5.11 | 3x speed, 20 units, 30 enemies, many particles | FPS counter shows >= 45 |

### Risk Factors

| Risk | Impact | Mitigation |
|------|--------|------------|
| OpenSL ES is complex and deprecated (Oboe recommended) | High | Use Oboe library (Google's C++ audio API) instead of raw OpenSL ES |
| PvP AI feels too predictable | Medium | Add randomized decision weights; vary AI personality per match |
| Particle system tanks FPS | High | Cap particles at 512 total; use point sprites for small particles |
| 3x speed causes physics glitches | Medium | Sub-step: at 3x, run 3 fixed updates per frame instead of scaling dt |
| Tutorial blocks players who already know the game | Low | Skip button always visible; tutorial never replays |
| Network code for future real PvP is a major rewrite | High | Design PvP interface abstractly: `IPvPOpponent` with LocalAI and RemotePlayer implementations |

---

## Phase 6: Polish + Production (최적화 + 프로덕션)

**Goal**: Production-ready quality — stable, optimized, localized, store-ready.

**Duration**: 3 weeks
**Dependencies**: All previous phases complete
**Estimated Files**: 3 new files + modifications across entire codebase
**Estimated LOC**: ~1,500 new + ~800 modifications

### Files to Create

| File | Purpose | Est. LOC |
|------|---------|----------|
| `engine/Localization.h` | String table, language switching, format strings with parameters | ~100 |
| `engine/Localization.cpp` | Load string tables from assets, fallback to English, runtime language switch | ~250 |
| `engine/Analytics.h` | Event tracking interface: battle_start, battle_end, merge, upgrade, purchase | ~60 |
| `engine/Analytics.cpp` | JNI bridge to Firebase Analytics (or lightweight local logger) | ~200 |
| `engine/CrashReporter.h` | Signal handler for SIGSEGV/SIGABRT, stack trace capture, JNI report | ~80 |
| `engine/CrashReporter.cpp` | Native crash handling, minidump generation, upload via JNI | ~300 |

### Optimization Tasks

| Task | Target | Approach |
|------|--------|----------|
| Draw call reduction | < 15 draw calls per frame | Texture atlas: pack all game sprites into 2048x2048 atlas. Single texture per batch. |
| Memory pooling audit | Zero runtime allocations during gameplay | Profile with `operator new` override; eliminate `std::vector` resizes in hot path |
| SpriteBatch sorting | Minimize texture swaps | Sort sprites by texture ID before batching; use z-order as secondary sort |
| SpatialHash optimization | < 0.1ms per query | Pre-allocate cell vectors; use flat array instead of `unordered_map` for known grid bounds |
| Enemy path optimization | Zero per-frame allocations | Pre-compute path as fixed array of waypoints; interpolate with index |
| Particle pooling | Reuse particle memory | Particles already use ObjectPool; ensure no allocation during emission |
| Battery optimization | < 5% battery per hour | Reduce GPU overdraw; use `eglSwapInterval(1)` for vsync; sleep when app backgrounded |
| APK size | < 100 MB | Compress textures with ETC2; compress audio with Vorbis at 96kbps; strip debug symbols |

### Balance Pass

**Unit Balance Methodology**:
```
1. Damage Per Second (DPS) formula per unit:
   DPS = (baseDamage * (1 + 0.10 * level)) / attackInterval

2. Efficiency metric:
   Efficiency = DPS / summonCost

3. Target: all units within 20% efficiency spread at same rarity

4. Ability value estimation:
   - Splash: multiply DPS by average targets hit (1.5-3.0)
   - Slow: value = teammate_DPS_increase * slow_duration
   - DoT: add DoT_total_damage to DPS calculation
   - Buff: value = buff_magnitude * affected_units * their_DPS

5. Balance tools:
   - Automated battle simulation: run 1000 random battles, collect win rates
   - Per-unit metrics: average damage dealt, average kills, pick rate in winning decks
   - Adjust until no unit has >60% or <40% inclusion in winning decks
```

### Localization

**String Table Structure**:
```cpp
// strings_en.txt
STR_LOBBY_BATTLE = "Battle"
STR_LOBBY_DECK = "Deck"
STR_LOBBY_COLLECTION = "Collection"
STR_HUD_WAVE = "Wave %d"
STR_HUD_SP = "SP: %d"
STR_RESULT_VICTORY = "Victory!"
STR_RESULT_DEFEAT = "Defeat"
STR_RESULT_WAVES = "Waves Cleared: %d"
STR_UNIT_ARCHER = "Archer"
STR_UPGRADE_NEED_CARDS = "Need %d more cards"
// ... (~200 strings total)

// strings_ko.txt
STR_LOBBY_BATTLE = "전투"
STR_LOBBY_DECK = "덱"
STR_LOBBY_COLLECTION = "컬렉션"
STR_HUD_WAVE = "웨이브 %d"
STR_HUD_SP = "SP: %d"
STR_RESULT_VICTORY = "승리!"
STR_RESULT_DEFEAT = "패배"
STR_RESULT_WAVES = "클리어 웨이브: %d"
STR_UNIT_ARCHER = "궁수"
STR_UPGRADE_NEED_CARDS = "카드 %d장 더 필요"
```

### Stress Testing Protocol

| Test | Condition | Target |
|------|-----------|--------|
| Max entities | 20 units + 64 enemies + 128 projectiles + 512 particles | 60 FPS |
| Extended play | 1 hour continuous play, 10+ battles | No memory growth > 5 MB |
| Rapid scene switching | Switch scenes 100 times in 2 minutes | No crash, no memory leak |
| Low memory device | 2 GB RAM device, other apps in background | No OOM crash |
| Background/foreground | Background app, wait 5 minutes, foreground | State restored correctly |
| Screen rotation | Rotate during battle (should be locked, but test) | No crash, orientation locked |
| Slow storage | Full device storage, attempt save | Graceful error message |
| Large save file | 1 MB save data (extreme case) | Load in < 100ms |

### Store Listing Preparation

| Asset | Specification |
|-------|--------------|
| App icon | 512x512 PNG, rounded corners per Google spec |
| Feature graphic | 1024x500 PNG, game key art |
| Screenshots | 5-8 screenshots at 1920x1080 (phone) and 2560x1600 (tablet) |
| Short description | < 80 characters, Korean + English |
| Full description | < 4000 characters, Korean + English, feature bullets |
| Privacy policy | URL to hosted privacy policy page |
| Content rating | Complete IARC questionnaire |
| Target API | API 33+ (Android 13) minimum, API 34 target |

### Acceptance Criteria

- [ ] **AC-6.1**: 60 FPS stable on mid-range device (e.g., Samsung Galaxy A34) with max entities (20 units + 64 enemies + 128 projectiles + 512 particles).
- [ ] **AC-6.2**: No memory leaks: 1 hour continuous play shows < 5 MB memory growth (verified with Android Profiler).
- [ ] **AC-6.3**: APK size < 100 MB (release build, with all assets).
- [ ] **AC-6.4**: No crashes in 1 hour continuous play across 3 different devices (low/mid/high end).
- [ ] **AC-6.5**: Korean localization complete: all UI text, unit names, ability descriptions, tutorial text.
- [ ] **AC-6.6**: English localization complete: same scope as Korean.
- [ ] **AC-6.7**: Language can be switched in settings without restarting the app. All visible text updates immediately.
- [ ] **AC-6.8**: All 40 units balanced: no unit has > 60% inclusion rate in winning decks in automated simulation.
- [ ] **AC-6.9**: Crash reporter captures native crashes and reports them (verified by triggering intentional null pointer dereference in debug build).
- [ ] **AC-6.10**: Analytics events fire correctly for: battle_start, battle_end, merge, upgrade, daily_login.
- [ ] **AC-6.11**: Draw calls < 15 per frame during normal gameplay (verified with SpriteBatch::getDrawCallCount()).
- [ ] **AC-6.12**: App icon, screenshots, and store description are complete and meet Google Play requirements.
- [ ] **AC-6.13**: App survives background -> foreground cycle without state loss or crash.
- [ ] **AC-6.14**: Battery usage < 5% per hour on mid-range device.

### Test Scenarios

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| T6.1 | Battle with max entities, check FPS counter | FPS >= 58 consistently |
| T6.2 | Play 10 battles, check memory in Android Profiler | Memory delta < 5 MB |
| T6.3 | Build release APK, check size | < 100 MB |
| T6.4 | Continuous 1 hour play session | Zero crashes |
| T6.5 | Switch language to Korean, navigate all screens | All text is Korean, no missing strings |
| T6.6 | Switch language to English, navigate all screens | All text is English, no missing strings |
| T6.7 | Automated balance simulation: 1000 random battles | No unit > 60% win inclusion |
| T6.8 | Trigger native crash in debug build | Crash report generated, stack trace readable |
| T6.9 | Check SpriteBatch draw call count during battle | < 15 draw calls |
| T6.10 | Run on Samsung Galaxy A14 (low-end) | Playable at >= 30 FPS |
| T6.11 | Background app for 5 minutes, foreground | Game resumes, no black screen |
| T6.12 | Run `adb shell dumpsys batterystats` after 1 hour | < 5% battery attributed to app |

### Risk Factors

| Risk | Impact | Mitigation |
|------|--------|------------|
| ETC2 texture compression quality loss | Medium | Compare visual quality; use RGBA8888 for critical UI elements |
| Firebase SDK increases APK size by 5-10 MB | Medium | Use lightweight custom analytics if size is tight |
| Native crash reporter misses some crash types | Medium | Supplement with Google Play Console crash reports (ANR, Java crashes) |
| Balance simulation doesn't reflect real player behavior | Medium | Add manual playtesting round; collect beta tester data |
| Korean font texture is very large | High | Use dynamic font atlas (render only used glyphs) or SDF fonts |
| Store review rejection | Medium | Review Google Play policies early; ensure no policy violations |

---

## Summary Table

| Phase | Duration | New Files | Est. LOC | Key Deliverable |
|-------|----------|-----------|----------|-----------------|
| 1. Core Battle | 4 weeks | 14 | 2,800 | Playable 40-wave battle |
| 2. Merge + Abilities | 3 weeks | 7 | 2,200 | Drag-merge, 40 unique abilities |
| 3. UI/UX + Screens | 3 weeks | 14 | 3,200 | Full screen flow, text rendering |
| 4. Progression | 3 weeks | 14 | 2,500 | Economy, saves, achievements |
| 5. PvP + Advanced | 4 weeks | 14 | 3,500 | PvP, audio, particles, tutorial |
| 6. Polish | 3 weeks | 6 | 2,300 | Optimization, localization, store |
| **Total** | **20 weeks** | **69** | **16,500** | **Production-ready game** |

### Critical Path

```
Phase 1 -> Phase 2 -> Phase 5 (PvP needs full battle + merge)
Phase 1 -> Phase 3 -> Phase 4 (Economy needs UI screens)
All -> Phase 6 (Polish requires all features complete)
```

### Milestone Checkpoints

| Week | Milestone | Gate Criteria |
|------|-----------|---------------|
| 4 | Alpha 1 | Single battle playable, 10 units functional |
| 7 | Alpha 2 | Merge working, 40 units with abilities |
| 10 | Beta 1 | Full screen flow, deck editing, text rendering |
| 13 | Beta 2 | Progression loop, save/load, achievements |
| 17 | RC 1 | PvP, audio, particles, tutorial all functional |
| 20 | Gold | Production-ready, store listing complete |

---

*Document generated for JayGame development team. Each phase can be assigned to independent developers provided dependencies are respected. All file paths are relative to `app/src/main/cpp/`.*
