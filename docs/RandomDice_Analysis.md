# Random Dice (랜덤 다이스 / 운빨존많겜) - Comprehensive Game Analysis

## Overview

- **Official Name**: Random Dice: PvP Defense (랜덤 다이스: PvP 디펜스)
- **Developer**: 111% (111퍼센트), South Korea
- **Initial Release**: August 28, 2019 (originally named "Royal Dice", renamed to "Random Dice" on December 18, 2019)
- **Platform**: iOS / Android
- **Genre**: Tower Defense / Strategy / Card Collection
- **Downloads**: 5+ million installs (Google Play), Google Play Editor's Choice multiple times
- **Nickname**: 운빨존많겜 ("luck-based game with extreme RNG") - a tongue-in-cheek name reflecting the heavy randomness in dice summoning

---

## 1. Core Gameplay Loop

### Fundamental Concept
Random Dice is a tower defense game where your "towers" are dice placed on a 5x3 grid (15 slots). Each die has a unique ability (fire, ice, poison, etc.) and attacks enemies that walk along a fixed path adjacent to the board. The core tension comes from the randomness: when you summon or merge dice, the result is randomly selected from your 5-dice deck.

### Per-Match Loop (Tick-by-Tick)

```
[Start Match]
    |
    v
[Receive starting SP (Summoning Points)]
    |
    v
[Summon dice onto the board] -- costs SP, random dice from your 5-dice deck
    |                           placed in a random empty slot
    v
[Enemies spawn and walk the path] -- dice automatically attack nearby enemies
    |
    v
[Earn SP from killing enemies]
    |
    v
[Decision Point: Summon more dice OR Merge existing dice OR Power Up]
    |
    +---> [Summon] -- Fill empty slots (costs increasing SP)
    +---> [Merge] -- Drag same-type, same-pip dice together
    |                 Result: pip count +1, type changes to random from deck
    +---> [Power Up] -- Spend SP to globally increase dice level for this match
    |
    v
[Boss appears at end of wave] -- has special abilities
    |
    v
[Next wave begins with stronger enemies]
    |
    v
[Repeat until you lose (PvE) or opponent's HP reaches 0 (PvP)]
```

### SP (Summoning Points) Economy
- SP is earned by killing enemies during waves
- Summoning a new die onto an empty slot costs SP (starts at ~100, increases with each summon)
- Filling all 15 slots without merging costs approximately 1,200 SP total
- Power Ups also cost SP -- each power up increases the base level of ALL dice on the board for the current match
- SP management is the central economic decision each match

### The "Random" Factor
- When you summon a die, it is randomly chosen from your pre-selected 5-dice deck
- When you merge two dice, the resulting die is also randomly chosen from your deck
- Placement of newly summoned dice is also random (fills a random empty slot)
- This triple-layered randomness is the defining characteristic of the game and the source of the nickname "운빨존많겜"

---

## 2. UI/UX Design

### 2.1 Main Menu / Lobby Screen

The lobby is the landing page upon opening the game. Layout structure:

```
+----------------------------------------------------------+
|  [Profile/Avatar]   [Currency Display: Gold | Diamonds]  |
|                     [Dice Tokens]                        |
+----------------------------------------------------------+
|                                                          |
|              [Current Deck Display]                      |
|              (5 dice shown in a row)                     |
|                                                          |
|  [Tier/Class Badge]        [Season Info / Event Banner]  |
|                                                          |
+----------------------------------------------------------+
|                                                          |
|  [BATTLE]              [CO-OP]                           |
|  (PvP Mode)            (Cooperative Mode)                |
|                                                          |
+----------------------------------------------------------+
|                    Bottom Nav Bar                         |
| [Shop] [Inventory] [Battle] [Crew] [Quests/Events]      |
+----------------------------------------------------------+
```

**Key UI Elements:**
- **Top Bar**: Player profile icon (left), currency indicators (gold, diamonds, dice tokens) on the right
- **Center Area**: Current deck preview showing the 5 selected dice, player's trophy count and class rank displayed prominently
- **Battle Buttons**: Large "Battle" and "Co-op" buttons as primary CTAs
- **Bottom Navigation**: Tab bar with Shop, Inventory/Dice Collection, Home/Battle, Crew, and Events/Quests
- **Quest Icon**: Top-right area, shows daily and main quest progress
- **Season Pass Banner**: Rotates event information and season pass promotions

### 2.2 In-Game HUD (Battle Screen)

```
+----------------------------------------------------------+
|  [Wave #]    [Timer: 90s countdown]    [Enemy HP Bar]    |
+----------------------------------------------------------+
|                                                          |
|           [Enemy Path / Track]                           |
|     Enemies walk along a winding path                    |
|     from spawn point to exit point                       |
|                                                          |
|  +-------+-------+-------+-------+-------+               |
|  | Slot1 | Slot2 | Slot3 | Slot4 | Slot5 |  <- Your     |
|  +-------+-------+-------+-------+-------+     Board     |
|  | Slot6 | Slot7 | Slot8 | Slot9 | Slot10|  (5x3 Grid)  |
|  +-------+-------+-------+-------+-------+               |
|  | Slot11| Slot12| Slot13| Slot14| Slot15|               |
|  +-------+-------+-------+-------+-------+               |
|                                                          |
+----------------------------------------------------------+
|  [SP: 1234]  [Summon Button]  [Power Up: Lv.3 / Cost]   |
+----------------------------------------------------------+
```

**HUD Breakdown:**
- **Top Area**: Wave number (left), countdown timer per wave (center), and opponent's board/HP info (in PvP mode the screen is split-view showing both boards)
- **Center**: The game board -- a 5x3 grid of 15 slots where dice are placed. Adjacent to the board is the enemy path/track where monsters walk
- **Bottom Bar**: Current SP amount, a large "Summon" button to spawn a random die, and a "Power Up" button showing current level and SP cost
- **Dice on Board**: Each die shows its type (icon/color), pip count (1-7 dots), and its attack animations are visible in real-time
- **Drag-to-Merge**: Players drag one die onto another of the same type and pip count to merge them

### 2.3 PvP Split Screen
In PvP mode, the screen is divided:
- **Top half**: Opponent's board and path (smaller, view-only)
- **Bottom half**: Your board and path (interactive)
- Each player has an HP bar; when enemies reach the end of the path, they are "sent" to the opponent or damage is dealt

### 2.4 Inventory / Dice Collection Screen
- Grid display of all owned dice
- Each die card shows: icon, name, rarity border color, current class level, number of copies owned
- Tap a die to see: detailed stats, upgrade cost, class-up requirements
- Filter/sort by rarity tier

### 2.5 Deck Builder
- 5 slots for deck composition
- Drag dice from collection into deck slots
- Multiple deck presets can be saved
- Recommended/meta decks shown

### 2.6 Shop Screen
- Daily shop with rotating items (dice, gold, diamonds)
- Chest section (Free/Gold/Platinum/Diamond/King's Legacy chests)
- Ad-refresh button to reload daily shop offerings
- IAP packages displayed with price tags

---

## 3. Unit (Dice) System

### 3.1 Rarity Tiers

| Tier | Korean | Color | Count (approx.) | Drop Rate |
|------|--------|-------|------------------|-----------|
| Normal (일반) | 일반 | Gray/White | 9 | Highest |
| Rare (희귀) | 희귀 | Blue | ~20 | Medium |
| Hero (영웅) | 영웅 | Purple | ~40 | Low |
| Legendary (전설) | 전설 | Gold/Orange | ~30 | Very Low |

**Total**: Approximately 100 dice as of late 2024.

### 3.2 Notable Dice by Rarity

**Normal (일반) - 9 dice:**
- Fire (불), Electric (전기), Wind (바람), Poison (독), Ice (얼음)
- Iron (철), Broken (부서진), Gambling (도박), Lock (자물쇠)

**Rare (희귀):**
- Mine (지뢰), Light (빛), Thorn (가시), Crack (균열)
- Critical (치명), Energy (에너지), Sacrifice (희생), Bow (활)
- Gambling Growth, Slingshot, Sword, Strike

**Hero (영웅):**
- Death (죽음), Teleport (텔레포트), Laser (레이저)
- Adaptation (적응), Infection (감염), and many more

**Legendary (전설):**
- Approximately 30 legendary dice, representing the largest tier by count
- These are the most powerful and form the core of endgame meta decks

### 3.3 Dice Stats & Mechanics
Each die has:
- **Base Damage**: Increases with power ups during a match
- **Attack Speed**: Fixed per die type
- **Special Ability**: Unique to each die (e.g., Fire does AoE, Ice slows, Poison does DoT)
- **Critical Chance**: Increased by pip count (dots) and power ups
- **Critical Damage**: Calculated as `Base Damage + (Base Damage * Critical Damage% / 100)`

### 3.4 Pip Count (Dot System)
- Each die on the board has a pip count from 1 to 7 (shown as dots on the die face)
- Higher pip count = stronger (more damage, stronger ability effect)
- Pip count increases by merging two dice of the same type AND same pip count
- The resulting merged die has pip count + 1 but changes to a RANDOM type from your deck

### 3.5 Special Dice Interactions
- **Summoner Dice**: When merged with another Summoner or Mimic, spawns an additional die on the board with random pips (1 less than the merged pip count)
- **Critical Dice**: Does not attack; instead provides critical chance bonus to adjacent dice
- **Sacrifice Dice**: When merged, provides bonus SP instead of creating a new die

---

## 4. Merge/Combine Mechanics (Deep Dive)

### 4.1 Basic Merge Rules
1. Two dice can merge only if they have the **same pip count** AND the **same type**
2. Drag one die onto the other (drag-and-drop gesture)
3. Result: One die with **pip count + 1**, but the **type changes randomly** to any die from your 5-dice deck
4. The merged die occupies the slot of the target die; the source slot becomes empty

### 4.2 Merge Strategy
- **Early Game**: Fill the board first, then merge to consolidate
- **Mid Game**: Merge strategically to build high-pip dice of your desired type
- **Late Game**: Target 5-7 pip dice of your damage-dealing type
- **Risk**: Merging is inherently risky because the type changes randomly -- you might merge two useful damage dice and get a support die you didn't want

### 4.3 Merge Exceptions
- Some dice have special merge behaviors (e.g., Summoner spawns extra dice)
- Mimic dice can merge with any die regardless of type matching
- Certain legendary dice have unique merge interactions

### 4.4 Board Management
- 15 slots maximum
- When all slots are full, you cannot summon new dice -- must merge first to free slots
- Strategic balance: too many dice = weaker individual strength; too few = not enough coverage
- Optimal play involves maintaining 10-12 dice while building toward high-pip key dice

---

## 5. Wave/Stage System

### 5.1 Wave Structure
- Waves are **semi-endless** -- they continue until the player loses (PvE/Co-op) or the opponent falls (PvP)
- Each wave lasts a countdown timer starting at **90 seconds**
- Timer decreases by 10 seconds per subsequent wave, with a minimum of **60 seconds**
- At the end of each wave timer, a **Boss** appears

### 5.2 Monster Types

| Type | HP | Speed | Notes |
|------|----|-------|-------|
| Normal | Base (100 initial) | Normal | Basic enemy |
| Speed | 50% of Normal | 150% faster | Less affected by slow effects |
| Big | 10x Normal | ~88.8% of Normal speed | Tank-type, slow but massive HP |
| Boss | Very High | Varies | Unique abilities per boss type |

### 5.3 Boss System
- A random boss appears at the end of each wave
- Boss type is determined partly by the lower-class player in PvP matchmaking
- Each boss has unique mechanics (e.g., shield, healing, spawning minions, silencing dice, swapping dice positions)
- **Death Wave**: Starting from Wave 7, the game enters "Death Wave" mode:
  - Enemy movement speed increases
  - Boss skill cooldowns decrease
  - BGM tempo increases and pitch rises (audio feedback for tension)
- **Berserk Mode**: From Wave 11 onward:
  - Bosses that normally don't appear at the current class level start appearing
  - Boss abilities are significantly strengthened
  - Boss cooldowns continue to decrease further

### 5.4 Co-op Wave Progression
- In Co-op mode, waves are fully endless
- Two players share responsibility for defending against increasingly difficult waves
- The goal is to survive as many waves as possible
- Rewards scale with waves survived

---

## 6. Game Modes

### 6.1 PvP Battle (대전)
- **1v1 real-time** matches against other players
- Split-screen view showing both boards
- Enemies that pass through your defenses are sent to the opponent (or deal damage)
- Winner determined when one player's HP reaches 0
- **Trophy system**: Win = gain trophies, Lose = lose trophies
- Trophies determine your **Class rank**

### 6.2 Co-op Boss Raid (협동전)
- **2-player cooperative** mode
- Both players defend against shared waves
- Endless waves -- survive as long as possible
- Specialized decks exist for Co-op (support + DPS roles)
- Rewards based on wave reached

### 6.3 Solo Mode (솔로모드)
- Single-player puzzle-like challenges
- Limited resources and constraints
- Tests strategic thinking more than RNG
- Functions as a "brain teaser" mode

### 6.4 Crew Battle (크루 대전)
- Guild/crew-based competitive mode
- Crew vs. Crew PvP battles
- Crew honor/prestige at stake
- Requires joining a Crew (guild system)

### 6.5 Random Mirror Match (랜덤 미러전)
- Both players use the **same randomly assigned deck**
- Eliminates deck advantage -- pure skill/RNG test
- Event/special mode

### 6.6 Random Arena (랜덤 투기장)
- Continuous battles for special rewards
- Win-streak based reward system
- Higher risk/reward structure

---

## 7. Progression System

### 7.1 Trophy & Class System
- **Trophies** are earned/lost through PvP wins/losses
- Trophy count determines your **Class** (rank tier)
- Higher class unlocks:
  - New boss types in matches
  - Access to higher-tier rewards
  - Matchmaking against stronger opponents
- Class serves as the primary vertical progression metric

### 7.2 Dice Class-Up (Permanent Upgrades)
- Outside of matches, dice can be permanently upgraded via **Class-Up**
- Requires: duplicate dice copies + gold
- Class-Up increases a die's base stats (damage, ability strength)
- Higher rarity dice require more copies and gold to upgrade
- This is the primary "collection and upgrade" loop between matches

### 7.3 Dice Power-Up (In-Match Temporary)
- During a match, spending SP on "Power Up" increases ALL dice on your board
- This is temporary and resets each match
- Optimal timing: most decks benefit from powering up at 3-star (level 3) or higher
- Strategic choice: power up early for immediate strength vs. save SP for more summons

### 7.4 Critical Damage Progression
- Critical Damage % increases as you collect and upgrade more dice overall
- Acts as a universal damage multiplier across all matches
- Long-term progression metric that rewards collection completeness

### 7.5 Trait System (특성)
- Additional passive bonuses that can be equipped
- Provides strategic customization beyond dice selection
- Documented extensively in its own system

### 7.6 Field System (필드)
- Different battlefield layouts/maps
- Each field may have different path configurations
- Unlocked through progression

### 7.7 Season Pass
- Costs approximately 5,900 KRW (~$4.50 USD)
- Rewards include: ore stones, legendary dice, diamond boxes, hundreds of diamonds
- Limited-edition emblems exclusive to the pass
- Premium pass removes all in-game ads

---

## 8. Monetization Model

### 8.1 Currency System

| Currency | Korean | How to Obtain | Primary Use |
|----------|--------|---------------|-------------|
| Gold (골드) | 골드 | Match rewards, quests | Dice upgrades, shop purchases |
| Diamonds (다이아) | 다이아몬드 | IAP, events, rewards | Premium purchases, chest opening |
| Dice Tokens (다이스 토큰) | 다이스 토큰 | Daily play, pass | Gacha/summoning |

### 8.2 Chest/Gacha System
Chests are the primary randomized reward mechanism:
- **Free Chest**: Available periodically at no cost
- **Gold Chest**: Purchased with gold
- **Platinum Chest**: Higher quality, costs more
- **Diamond Chest**: Premium, purchased with diamonds
- **King's Legacy Chest**: Highest tier, guarantees legendary dice

Each chest contains:
- Random dice (chance of higher rarity based on chest tier)
- Gold
- Occasionally diamonds or dice tokens

### 8.3 In-App Purchases (IAP)

**Monthly Subscription (월정액) - 25,000 KRW (~$19 USD):**
- 200 Dice Tokens
- 1x Random Legendary Dice
- Ad removal
- +150 Dice Token earning limit increase
- 30 Dice Tokens daily

**Diamond Packages**: Direct diamond purchases at various price points

**Season Pass**: ~5,900 KRW per season

**Special Event Packages**: Time-limited bundles with dice + resources

### 8.4 Advertising Revenue
- **Rewarded Ads**: Watch ads to receive bonus rewards (e.g., burning/booster effects, extra resources)
- **Shop Refresh**: Watch an ad to refresh the daily shop inventory
- **Ad Removal**: Purchasing the premium pass or monthly subscription removes ads
- Players are never forced to watch ads -- all ad viewing is optional/rewarded

### 8.5 Revenue Strategy
- F2P-friendly with competitive viability for non-paying players
- Monetization primarily through convenience (faster progression) and cosmetics
- 111% reported a 14% revenue increase through Moloco's ML-powered ad targeting
- Focus on acquiring high-retention users due to the game's depth requiring long-term engagement
- ROAS efficiency improved by 2.5x through targeted D1 action models

---

## 9. Key Success Factors

### 9.1 Unique Core Mechanic
The use of dice as both the "tower" units and the source of randomness is a novel combination. No other tower defense game uses the dice-merge-random-result mechanic as its central pillar. This creates:
- **Emergent strategy**: Every match plays out differently due to RNG
- **Skill expression through RNG management**: Good players minimize bad luck through smart merging decisions
- **"One more game" factor**: The randomness means each match has the potential for a perfect run

### 9.2 Accessibility + Depth
- **Easy to learn**: Summon dice, merge same ones, kill enemies
- **Hard to master**: Deck building, merge timing, SP management, board positioning, power-up timing
- **Low session time**: A single PvP match is 3-7 minutes, perfect for mobile

### 9.3 Strong PvP Loop
- Real-time competitive play creates emotional investment
- Trophy/class system provides clear progression goals
- Matchmaking keeps games competitive and engaging
- "Sending" enemies to opponents creates direct interaction

### 9.4 Generous F2P Model
- All dice can eventually be obtained without paying
- Skill and strategy matter more than spending
- Daily rewards, quests, and free chests keep players engaged
- No hard paywalls for content

### 9.5 Social Features
- Crew system creates community
- Co-op mode provides positive social experiences
- Competitive leaderboards drive aspirational play
- Mirror mode eliminates pay-to-win concerns for purists

### 9.6 Content Cadence
- Regular updates with new dice, balance patches
- Seasonal content and events
- New game modes added over time (Solo, Crew Battle, Arena)
- Active community (randomdice.gg, Namu Wiki, Discord)

### 9.7 The "운빨" Factor (Luck Factor)
- The heavy RNG is simultaneously the game's biggest draw and criticism
- Creates dramatic moments: pulling the exact die you need at a critical moment
- Generates shareable/streamable content ("look what happened!")
- The nickname "운빨존많겜" (luck-dependent game) has become a badge of identity for the community

### 9.8 Korean Market Fit
- Developed by Korean studio 111%
- Strong cultural fit with Korean mobile gaming preferences:
  - Short session competitive gameplay
  - Collection/upgrade loops
  - Social/crew features
  - Regular events and updates
- Google Play Editor's Choice recognition in Korea

---

## 10. Competitive Analysis: What Makes Random Dice Different

| Feature | Random Dice | Traditional TD | Other Merge Games |
|---------|-------------|----------------|-------------------|
| Unit Selection | Random from deck | Player chooses | Usually fixed |
| Merge Result | Random type change | N/A | Deterministic upgrade |
| PvP | Real-time split screen | Rare | Very rare |
| Session Length | 3-7 min | 15-30 min | Varies |
| Skill vs Luck | 60/40 | 90/10 | Varies |
| Collection Depth | 100+ dice | Fixed towers | Limited |

---

## 11. Takeaways for Game Development

1. **Controlled randomness is compelling**: Players accept RNG when they feel they have agency (deck building, merge decisions)
2. **Short sessions + competitive loop = retention**: The 3-7 minute PvP match length is ideal for mobile
3. **Simple inputs, complex outcomes**: Drag-to-merge is intuitive but creates deep strategic decisions
4. **Dual economy (in-match SP + meta-game currencies)**: Separating match economy from progression economy keeps both loops satisfying
5. **Social features enhance longevity**: Crews, co-op, and leaderboards keep players beyond the initial novelty
6. **Fair F2P monetization builds trust**: Revenue comes from convenience and speed, not power gating
7. **The board as a constraint**: 15 slots force meaningful decisions about when to summon vs merge vs power up
8. **Audio/visual feedback for tension**: Death Wave music changes create palpable tension without any UI changes
9. **Multiple game modes serve different moods**: PvP for competition, Co-op for relaxation, Solo for puzzle-solving

---

*Document compiled: March 2026*
*Based on web research from official sources, community wikis, and developer information*

## Sources
- [Random Dice: PvP Defense - Namu Wiki (Main)](https://namu.wiki/w/%EB%9E%9C%EB%8D%A4%20%EB%8B%A4%EC%9D%B4%EC%8A%A4(Random%20Dice):%20PvP%20%EB%94%94%ED%8E%9C%EC%8A%A4)
- [Random Dice: PvP Defense/Dice - Namu Wiki](https://namu.wiki/w/%EB%9E%9C%EB%8D%A4%20%EB%8B%A4%EC%9D%B4%EC%8A%A4(Random%20Dice):%20PvP%20%EB%94%94%ED%8E%9C%EC%8A%A4/%EC%A3%BC%EC%82%AC%EC%9C%84)
- [Random Dice: PvP Defense/Monster - Namu Wiki](https://namu.wiki/w/%EB%9E%9C%EB%8D%A4%20%EB%8B%A4%EC%9D%B4%EC%8A%A4(Random%20Dice):%20PvP%20%EB%94%94%ED%8E%9C%EC%8A%A4/%EB%AA%AC%EC%8A%A4%ED%84%B0)
- [Dice Merging Guide - Random Dice Wiki (Fandom)](https://random-dice.fandom.com/wiki/Dice_Merging_Guide)
- [Dice Mechanics - Random Dice Wiki (Fandom)](https://random-dice.fandom.com/wiki/Dice_Mechanics)
- [Random Dice Community Website - Dice Mechanics](https://randomdice.gg/wiki/dice_mechanics)
- [Random Dice Community Website - PvP](https://randomdice.gg/wiki/pvp)
- [Dice Class Upgrade - Random Dice Wiki (Fandom)](https://random-dice.fandom.com/wiki/Dice_Class_Upgrade)
- [111Percent Help Center - All Chests](https://111percent.helpshift.com/hc/ko/3-random-dice-pvp-defense/faq/750-all-chests-in-random-dice-25-02-20/)
- [111Percent Help Center - Gameplays & Dice](https://111percent.helpshift.com/hc/en/3-random-dice-pvp-defense/section/32-gameplays-dice/)
- [PocketGamer - 111 Percent Random Dice Success](https://www.pocketgamer.biz/comment-and-opinion/73897/unique-gameplay-intelligent-marketing-fuels-the-growth-of-111-percents-random-dice/)
- [Moloco Case Study - 111Percent Revenue](https://www.moloco.com/case-studies/111-percent)
- [Random Dice Guide - Pocket Tactics](https://www.pockettactics.com/random-dice/guide)
- [Random Dice - Google Play Store](https://play.google.com/store/apps/details?id=com.percent.royaldice&hl=en_US)
- [Random Dice - Apple App Store (KR)](https://apps.apple.com/kr/app/%EB%9E%9C%EB%8D%A4-%EB%8B%A4%EC%9D%B4%EC%8A%A4-random-dice/id1462877149)
