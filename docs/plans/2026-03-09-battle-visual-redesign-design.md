# Battle Visual Redesign Design

## Date: 2026-03-09

## Approach
- Pixel art sprite sheet, single atlas (2048x2048)
- Reuse existing SpriteBatch + TextureRegion system
- Map UV coordinates per sprite type
- Add frame animation system

## Atlas Layout (2048x2048, 64x64 cells = 32x32 grid)
- Row 0-3: 15 units × 4 frames (idle×2, attack×2)
- Row 4-5: 6 enemies × 3 frames (walk×2, hit×1)
- Row 6: 6 projectile types × 2 frames
- Row 7-8: Tilemap (grass, stone, path, water, grid decoration)
- Row 9-10: HUD (button frames, panels, bar borders, icons)
- Row 11: Bitmap font (8x8 extended ASCII)
- Row 12-15: Effects (splash, slow, poison cloud, chain lightning, shield, buff aura)

## Changes
- New: assets/atlas.png, SpriteAtlas.h/cpp, Animation system
- Modify: BattleScene, Unit, Enemy, Projectile, TextRenderer, LobbyScene
