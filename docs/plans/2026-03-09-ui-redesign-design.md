# JayGame UI Redesign Design

## Date: 2026-03-09

## Architecture
- **Menu screens (7):** Kotlin Jetpack Compose
- **Battle screen (1):** Existing C++ OpenGL ES 3.0
- **Navigation:** Compose Navigation + Intent bridge to C++ GameActivity
- **Data sharing:** Existing SaveSystem (SharedPreferences), read from both Compose and C++

## Visual Theme: Medieval Fantasy
- Colors: Dark Brown (#2C1810), Leather Beige (#D4A574), Gold (#FFD700), Parchment (#F5E6C8)
- UI Style: Wood frame borders, parchment texture backgrounds, metallic accents
- Font: Serif-style medieval feel
- Icons: SVG-based, gold/brown tones

## Screens (9 total)

| # | Screen | Header | Key Elements |
|---|--------|--------|-------------|
| 1 | Lobby (Home) | Full (Lv, Trophy, Gold, Diamond, Stamina) | Battle start button, daily login popup, notices, achievement/season pass entry |
| 2 | Battle | Round/HP/SP only | C++ OpenGL (unchanged) |
| 3 | Result | None | Victory/Defeat banner, rewards list, card rewards, home button |
| 4 | Deck Edit | Currency only | 5-slot deck, unit inventory grid, drag-and-drop |
| 5 | Collection | Currency only | Unit grid, detail panel (stats/level/ability), upgrade button |
| 6 | Shop | Currency only | Tabs (Gold/Diamond/Package), product card list |
| 7 | Settings | Minimal | Sound/Music toggle, account, language, credits |
| 8 | Season Pass | Currency only | Tier progress bar, free/paid reward tracks |
| 9 | Achievements | Minimal | Category tabs, achievement list, claim rewards |

## Navigation Structure
- Bottom tab bar (5 tabs, hidden during battle):
  - Battle | Deck | Home (center) | Collection | Shop
- Home screen sub-entries:
  - Settings (top-right gear icon)
  - Achievements (left button)
  - Season Pass (right button)

## Header Variants
- Full: Level, nickname, trophy, gold, diamond, stamina
- Currency only: Gold, diamond
- Minimal: Screen title + back button
- Battle: Round, HP, SP (C++ rendered)

## Asset Strategy
- Backgrounds: Per-screen SVG/PNG (parchment texture, wood frames)
- Icons: Currency, tabs, unit grade frames - SVG
- Unit icons: 15 types - illustrated PNG
- UI components: Compose Canvas + image for medieval custom buttons/cards/panels
