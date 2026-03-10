#!/usr/bin/env python3
"""
build_atlas.py -- Atlas builder using free open-source assets

Sources (all CC0 / Public Domain):
  - RPGCharacterSprites32x32.png by Eldiran (OpenGameArt, CC0)
  - DungeonCrawl_ProjectUtumnoTileset.png (OpenGameArt, CC0)

Layout matches SpriteAtlas.cpp exactly:
  2048x2048, 32x32 grid of 64x64 cells
  Rows 0-3 : 25 units  (4 cols each: idle0 idle1 attack0 attack1)
  Row  4   : 6 enemies (3 cols each: walk0 walk1 hit)
  Row  6   : 6 projectiles (2 cols each: fly0 fly1)
  Row  7   : 12 tiles
  Rows 9-10: HUD elements
  Rows 12-13: Effects
  Cell(31,31): solid white pixel
"""

import os, sys, math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------
SCRIPT_DIR  = Path(__file__).resolve().parent
PROJECT_DIR = SCRIPT_DIR.parent
ASSETS_SRC  = SCRIPT_DIR / "assets_src"
ATLAS_OUT   = PROJECT_DIR / "app" / "src" / "main" / "assets" / "atlas.png"
ICON_OUT    = PROJECT_DIR / "app" / "src" / "main" / "res" / "drawable-xxhdpi"

RPG_CHARS   = ASSETS_SRC / "RPGCharacterSprites32x32.png"
DC_TILESET  = ASSETS_SRC / "DungeonCrawl_ProjectUtumnoTileset.png"

ATLAS_SIZE = 2048
CELL = 64
GRID = ATLAS_SIZE // CELL  # 32

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def remove_magenta_bg(img):
    """Remove magenta (255,0,255) background, replacing with transparency."""
    img = img.convert("RGBA")
    pixels = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            # Magenta or near-magenta background
            if r > 200 and g < 80 and b > 200:
                pixels[x, y] = (0, 0, 0, 0)
    return img


def upscale(img32, size=CELL):
    """Upscale 32x32 to 64x64 using nearest neighbor (preserves pixel art)."""
    return img32.resize((size, size), Image.NEAREST)


def tint_image(img, color, strength=0.4):
    """Apply a color tint to an RGBA image."""
    r, g, b = color
    result = img.copy()
    pixels = result.load()
    w, h = result.size
    for y in range(h):
        for x in range(w):
            pr, pg, pb, pa = pixels[x, y]
            if pa > 0:
                nr = int(pr * (1 - strength) + r * strength)
                ng = int(pg * (1 - strength) + g * strength)
                nb = int(pb * (1 - strength) + b * strength)
                pixels[x, y] = (min(255, nr), min(255, ng), min(255, nb), pa)
    return result


def mirror_h(img):
    """Horizontal mirror."""
    return img.transpose(Image.FLIP_LEFT_RIGHT)


def make_hit_version(img):
    """Create a red-tinted 'hit' version of a sprite."""
    return tint_image(img, (255, 60, 60), 0.5)


def extract_rpg_char(rpg_img, char_row, col):
    """Extract a 32x32 sprite from the RPG character sheet, removing magenta bg."""
    x = col * 32
    y = char_row * 32
    tile = rpg_img.crop((x, y, x + 32, y + 32))
    return remove_magenta_bg(tile)


def extract_dc_tile(dc_img, row, col):
    """Extract a 32x32 tile from Dungeon Crawl tileset."""
    x = col * 32
    y = row * 32
    return dc_img.crop((x, y, x + 32, y + 32))


def place_cell(atlas, cell_img, col, row):
    """Place a 64x64 image at grid position (col, row) in the atlas."""
    x = col * CELL
    y = row * CELL
    atlas.paste(cell_img, (x, y), cell_img)


# ---------------------------------------------------------------------------
# Unit mapping: 25 units from RPG character sprite sheet
# RPG sheet: 12 cols x 21 rows of 32x32
#   Cols 0-2: Front (down) - stand, walk1, walk2
#   Cols 3-5: Left - stand, walk1, walk2
#   Cols 6-8: Right - stand, walk1, walk2
#   Cols 9-11: Back - stand, walk1, walk2
# ---------------------------------------------------------------------------

# Family color tints
FAMILY_TINTS = {
    "fire":      (255, 100, 50),
    "frost":     (100, 180, 255),
    "poison":    (100, 220, 100),
    "lightning": (255, 220, 80),
    "support":   (200, 140, 255),
}

# Unit ID -> (family, grade_index 0-4, rpg_char_row)
# 5 families x 5 grades = 25 units
# IDs: Fire(0,5,10,15,20), Frost(1,6,11,16,21), Poison(2,7,12,17,22),
#      Lightning(3,8,13,18,23), Support(4,9,14,19,24)
UNIT_MAP = {}
families = ["fire", "frost", "poison", "lightning", "support"]
for fam_idx, family in enumerate(families):
    for grade in range(5):
        unit_id = fam_idx + grade * 5
        # Map to RPG character rows (21 available, cycle if needed)
        char_row = (grade * 5 + fam_idx) % 21
        # Skip row 0 which has ghost-like sprites, shift to row 1+
        char_row = 1 + (char_row % 20)
        UNIT_MAP[unit_id] = (family, grade, char_row)


# ---------------------------------------------------------------------------
# Enemy sprites from Dungeon Crawl tileset
# Row 1 has great fantasy monsters
# ---------------------------------------------------------------------------
# (row, col) pairs for 6 enemy types - each needs walk0, walk1(mirror), hit
ENEMY_COORDS = [
    (1, 9),   # Enemy 0: red imp (basic)
    (1, 11),  # Enemy 1: gold knight (medium)
    (2, 1),   # Enemy 2: lion beast (medium)
    (1, 15),  # Enemy 3: purple mage (hard)
    (2, 17),  # Enemy 4: red demon (hard)
    (2, 62),  # Enemy 5: large teal creature (boss)
]


# ---------------------------------------------------------------------------
# Procedural generators for projectiles, tiles, HUD, effects
# ---------------------------------------------------------------------------

def make_projectile(color, shape="circle"):
    """Generate a 32x32 projectile sprite."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    r, g, b = color
    if shape == "circle":
        draw.ellipse([8, 8, 24, 24], fill=(r, g, b, 255))
        draw.ellipse([10, 10, 22, 22], fill=(min(255, r+80), min(255, g+80), min(255, b+80), 200))
        draw.ellipse([13, 13, 19, 19], fill=(255, 255, 255, 180))
    elif shape == "bolt":
        pts = [(16, 4), (20, 14), (18, 14), (22, 28), (14, 18), (16, 18), (12, 4)]
        draw.polygon(pts, fill=(r, g, b, 255))
    elif shape == "arrow":
        draw.polygon([(16, 4), (20, 12), (12, 12)], fill=(r, g, b, 255))
        draw.rectangle([14, 12, 18, 28], fill=(r, g, b, 200))
    return img


def make_tile(color, pattern="solid"):
    """Generate a 32x32 terrain tile."""
    img = Image.new("RGBA", (32, 32), (*color, 255))
    draw = ImageDraw.Draw(img)
    r, g, b = color
    if pattern == "grass":
        for i in range(20):
            x = (i * 7 + 3) % 32
            y = (i * 11 + 5) % 32
            draw.line([(x, y), (x, y - 3)], fill=(min(255, r + 30), min(255, g + 30), b, 255))
    elif pattern == "stone":
        for i in range(6):
            x = (i * 11 + 2) % 28
            y = (i * 9 + 3) % 28
            draw.rectangle([x, y, x + 6, y + 4], outline=(r - 20, g - 20, b - 20, 255))
    elif pattern == "path":
        draw.rectangle([0, 0, 31, 31], fill=(r, g, b, 255))
        draw.rectangle([2, 2, 29, 29], fill=(min(255, r + 15), min(255, g + 15), min(255, b + 10), 255))
    elif pattern == "water":
        for y in range(0, 32, 4):
            draw.arc([0, y, 32, y + 8], 0, 180, fill=(min(255, r + 40), min(255, g + 40), min(255, b + 40), 255))
    elif pattern == "grid_bg":
        draw.rectangle([0, 0, 31, 31], fill=(40, 40, 50, 200))
    elif pattern == "grid_cell":
        draw.rectangle([0, 0, 31, 31], fill=(50, 50, 65, 220))
        draw.rectangle([1, 1, 30, 30], outline=(80, 80, 100, 150))
    return img


def make_hud_element(name):
    """Generate a 32x32 HUD element."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    if name == "btn_normal":
        draw.rounded_rectangle([1, 1, 30, 30], radius=4, fill=(70, 70, 90, 255), outline=(120, 120, 150, 255))
    elif name == "btn_pressed":
        draw.rounded_rectangle([2, 2, 30, 30], radius=4, fill=(50, 50, 70, 255), outline=(100, 100, 130, 255))
    elif name == "btn_disabled":
        draw.rounded_rectangle([1, 1, 30, 30], radius=4, fill=(40, 40, 50, 200), outline=(60, 60, 70, 200))
    elif name == "panel_bg":
        draw.rectangle([0, 0, 31, 31], fill=(30, 30, 40, 230))
    elif name == "panel_border":
        draw.rectangle([0, 0, 31, 31], outline=(100, 100, 130, 255), width=2)
    elif name == "bar_border":
        draw.rounded_rectangle([1, 10, 30, 22], radius=3, outline=(150, 150, 170, 255))
    elif name == "bar_fill_hp":
        draw.rounded_rectangle([2, 11, 29, 21], radius=2, fill=(220, 50, 50, 255))
    elif name == "bar_fill_mp":
        draw.rounded_rectangle([2, 11, 29, 21], radius=2, fill=(50, 120, 220, 255))
    elif name == "icon_gold":
        draw.ellipse([6, 6, 26, 26], fill=(255, 215, 0, 255))
        draw.text((12, 8), "$", fill=(180, 130, 0, 255))
    elif name == "icon_wave":
        for i in range(3):
            draw.arc([4 + i * 3, 8, 20 + i * 3, 24], 180, 360, fill=(100, 200, 255, 255), width=2)
    elif name == "icon_heart":
        draw.polygon([(16, 26), (4, 14), (8, 6), (16, 12), (24, 6), (28, 14)],
                     fill=(220, 40, 60, 255))
    elif name == "icon_attack":
        draw.polygon([(16, 2), (22, 16), (28, 14), (16, 30), (10, 16), (4, 18)],
                     fill=(255, 160, 40, 255))
    elif name == "icon_speed":
        draw.polygon([(6, 16), (16, 4), (18, 14), (26, 16), (16, 28), (14, 18)],
                     fill=(100, 220, 255, 255))
    elif name == "icon_range":
        draw.ellipse([4, 4, 28, 28], outline=(200, 200, 100, 255), width=2)
        draw.ellipse([10, 10, 22, 22], outline=(200, 200, 100, 255), width=1)
        draw.ellipse([14, 14, 18, 18], fill=(255, 80, 80, 255))
    elif name == "icon_merge":
        draw.polygon([(16, 4), (28, 16), (16, 28), (4, 16)], fill=(180, 100, 255, 255))
        draw.text((11, 10), "+", fill=(255, 255, 255, 255))
    elif name == "icon_sell":
        draw.ellipse([4, 4, 28, 28], fill=(255, 200, 50, 255))
        draw.text((11, 8), "G", fill=(120, 80, 0, 255))
    elif name == "icon_buff":
        draw.polygon([(16, 4), (20, 12), (28, 14), (22, 20), (24, 28), (16, 24),
                      (8, 28), (10, 20), (4, 14), (12, 12)], fill=(100, 255, 100, 200))
    return img


def make_effect_frame(name, frame_idx, total_frames):
    """Generate a 32x32 effect animation frame."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    progress = frame_idx / max(1, total_frames - 1)

    if name == "splash":
        radius = int(4 + progress * 12)
        alpha = int(255 * (1 - progress * 0.7))
        draw.ellipse([16 - radius, 16 - radius, 16 + radius, 16 + radius],
                     fill=(255, 120, 30, alpha))
    elif name == "slow":
        r = int(8 + progress * 6)
        draw.ellipse([16 - r, 16 - r, 16 + r, 16 + r],
                     fill=(100, 180, 255, int(200 * (1 - progress * 0.5))))
    elif name == "poison":
        for i in range(5):
            angle = (i / 5 + progress * 0.3) * math.pi * 2
            x = int(16 + math.cos(angle) * (6 + progress * 4))
            y = int(16 + math.sin(angle) * (6 + progress * 4))
            draw.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(80, 220, 80, 200))
    elif name == "chain":
        x1 = int(4 + progress * 24)
        draw.line([(4, 16), (x1, 8 + int(progress * 8))], fill=(255, 255, 100, 255), width=2)
        draw.line([(x1, 8 + int(progress * 8)), (28, 16)], fill=(255, 255, 100, 200), width=1)
    elif name == "shield":
        alpha = int(200 - progress * 80)
        draw.arc([4, 4, 28, 28], 0, 360, fill=(100, 200, 255, alpha), width=3)
    elif name == "buff_aura":
        for i in range(4):
            angle = (i / 4 + progress * 0.5) * math.pi * 2
            x = int(16 + math.cos(angle) * 10)
            y = int(16 + math.sin(angle) * 10)
            draw.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(200, 150, 255, 200))
    return img


# ---------------------------------------------------------------------------
# Main build
# ---------------------------------------------------------------------------

def build():
    print("Loading source assets...")
    rpg = Image.open(RPG_CHARS).convert("RGBA")
    dc = Image.open(DC_TILESET).convert("RGBA")

    atlas = Image.new("RGBA", (ATLAS_SIZE, ATLAS_SIZE), (0, 0, 0, 0))
    unit_icons = {}  # unit_id -> 96x96 PNG for Compose

    # -----------------------------------------------------------------------
    # UNITS (rows 0-3)
    # -----------------------------------------------------------------------
    print("Building units...")
    for unit_id, (family, grade, char_row) in UNIT_MAP.items():
        atlas_row = unit_id // 8
        slot = unit_id % 8
        base_col = slot * 4

        tint_color = FAMILY_TINTS[family]
        # Stronger tint for higher grades
        tint_strength = 0.2 + grade * 0.1

        # Extract 4 frames from RPG sheet
        idle0_32 = extract_rpg_char(rpg, char_row, 0)   # front stand
        idle1_32 = extract_rpg_char(rpg, char_row, 1)   # front walk1
        atk0_32  = extract_rpg_char(rpg, char_row, 6)   # right stand
        atk1_32  = extract_rpg_char(rpg, char_row, 7)   # right walk1

        # Apply family tint
        idle0_32 = tint_image(idle0_32, tint_color, tint_strength)
        idle1_32 = tint_image(idle1_32, tint_color, tint_strength)
        atk0_32  = tint_image(atk0_32, tint_color, tint_strength)
        atk1_32  = tint_image(atk1_32, tint_color, tint_strength)

        # Upscale to 64x64 and place
        place_cell(atlas, upscale(idle0_32), base_col + 0, atlas_row)
        place_cell(atlas, upscale(idle1_32), base_col + 1, atlas_row)
        place_cell(atlas, upscale(atk0_32),  base_col + 2, atlas_row)
        place_cell(atlas, upscale(atk1_32),  base_col + 3, atlas_row)

        # Save Compose icon (96x96, front standing)
        icon = idle0_32.resize((96, 96), Image.NEAREST)
        unit_icons[unit_id] = icon

    # -----------------------------------------------------------------------
    # ENEMIES (row 4)
    # -----------------------------------------------------------------------
    print("Building enemies...")
    for i, (erow, ecol) in enumerate(ENEMY_COORDS):
        base_tile = extract_dc_tile(dc, erow, ecol)
        walk0 = upscale(base_tile)
        walk1 = upscale(mirror_h(base_tile))
        hit   = upscale(make_hit_version(base_tile))

        base_col = i * 3
        place_cell(atlas, walk0, base_col + 0, 4)
        place_cell(atlas, walk1, base_col + 1, 4)
        place_cell(atlas, hit,   base_col + 2, 4)

    # -----------------------------------------------------------------------
    # PROJECTILES (row 6)
    # -----------------------------------------------------------------------
    print("Building projectiles...")
    proj_defs = [
        ((255, 100, 30), "circle"),   # fire
        ((100, 180, 255), "circle"),  # frost
        ((80, 220, 80), "circle"),    # poison
        ((255, 220, 80), "bolt"),     # lightning
        ((200, 140, 255), "circle"),  # support
        ((255, 255, 255), "arrow"),   # generic
    ]
    for i, (color, shape) in enumerate(proj_defs):
        p0 = make_projectile(color, shape)
        p1 = make_projectile(color, shape)
        # Slight variation for frame 2
        p1 = p1.rotate(15, expand=False, fillcolor=(0, 0, 0, 0))

        place_cell(atlas, upscale(p0), i * 2 + 0, 6)
        place_cell(atlas, upscale(p1), i * 2 + 1, 6)

    # -----------------------------------------------------------------------
    # TILES (row 7)
    # -----------------------------------------------------------------------
    print("Building tiles...")
    tile_defs = [
        ((60, 120, 40), "grass"),
        ((120, 110, 100), "stone"),
        ((160, 140, 100), "path"),    # path_h
        ((160, 140, 100), "path"),    # path_v
        ((160, 140, 100), "path"),    # path_turn_tl
        ((160, 140, 100), "path"),    # path_turn_tr
        ((160, 140, 100), "path"),    # path_turn_bl
        ((160, 140, 100), "path"),    # path_turn_br
        ((160, 140, 100), "path"),    # path_cross
        ((40, 80, 160), "water"),
        ((40, 40, 50), "grid_bg"),
        ((50, 50, 65), "grid_cell"),
    ]
    for i, (color, pattern) in enumerate(tile_defs):
        tile = make_tile(color, pattern)
        place_cell(atlas, upscale(tile), i, 7)

    # -----------------------------------------------------------------------
    # HUD (rows 9-10)
    # -----------------------------------------------------------------------
    print("Building HUD...")
    hud_names = [
        "btn_normal", "btn_pressed", "btn_disabled",
        "panel_bg", "panel_border",
        "bar_border", "bar_fill_hp", "bar_fill_mp",
        "icon_gold", "icon_wave", "icon_heart",
        "icon_attack", "icon_speed", "icon_range",
        "icon_merge", "icon_sell", "icon_buff",
    ]
    col = 0
    row = 9
    for name in hud_names:
        if col >= 32:
            col = 0
            row += 1
        hud_img = make_hud_element(name)
        place_cell(atlas, upscale(hud_img), col, row)
        col += 1

    # -----------------------------------------------------------------------
    # EFFECTS (rows 12-13)
    # -----------------------------------------------------------------------
    print("Building effects...")
    effect_defs = [
        ("splash", 12, 0, 4),
        ("slow",   12, 4, 2),
        ("poison", 12, 6, 3),
        ("chain",  12, 9, 3),
        ("shield", 13, 0, 2),
        ("buff_aura", 13, 2, 4),
    ]
    for name, erow, start_col, frame_count in effect_defs:
        for f in range(frame_count):
            frame = make_effect_frame(name, f, frame_count)
            place_cell(atlas, upscale(frame), start_col + f, erow)

    # -----------------------------------------------------------------------
    # WHITE PIXEL (cell 31,31)
    # -----------------------------------------------------------------------
    white = Image.new("RGBA", (CELL, CELL), (255, 255, 255, 255))
    place_cell(atlas, white, 31, 31)

    # -----------------------------------------------------------------------
    # Save atlas
    # -----------------------------------------------------------------------
    print(f"Saving atlas to {ATLAS_OUT}...")
    ATLAS_OUT.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(str(ATLAS_OUT), "PNG")
    print(f"  Atlas size: {atlas.size}")

    # -----------------------------------------------------------------------
    # Save Compose unit icons
    # -----------------------------------------------------------------------
    print(f"Saving {len(unit_icons)} unit icons to {ICON_OUT}...")
    ICON_OUT.mkdir(parents=True, exist_ok=True)
    for uid, icon in sorted(unit_icons.items()):
        path = ICON_OUT / f"ic_unit_{uid}.png"
        icon.save(str(path), "PNG")

    print("Done!")


if __name__ == "__main__":
    build()
