#!/usr/bin/env python3
"""
Generate a 2048x2048 pixel art sprite atlas for JayGame.
Each cell is 64x64 pixels, arranged in a 32x32 grid.
Layout matches SpriteAtlas.cpp UV mappings exactly.
"""

from PIL import Image, ImageDraw
import random
import math

ATLAS = 2048
CELL = 64
GRID = 32

# Create the atlas image
atlas = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))

def cell_box(col, row):
    """Return (x0, y0, x1, y1) for a cell."""
    x = col * CELL
    y = row * CELL
    return (x, y, x + CELL, y + CELL)

def cell_img(col, row):
    """Return a new 64x64 RGBA image and its draw context."""
    img = Image.new("RGBA", (CELL, CELL), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    return img, draw

def paste_cell(img, col, row):
    """Paste a 64x64 image into the atlas at given cell position."""
    x, y = col * CELL, row * CELL
    atlas.paste(img, (x, y))

def darken(color, factor=0.7):
    return tuple(int(c * factor) for c in color[:3]) + ((color[3],) if len(color) > 3 else ())

def lighten(color, factor=1.3):
    return tuple(min(255, int(c * factor)) for c in color[:3]) + ((color[3],) if len(color) > 3 else ())

def blend(c1, c2, t=0.5):
    return tuple(int(c1[i] * (1 - t) + c2[i] * t) for i in range(3)) + (255,)

# ============================================================
# UNIT DRAWING HELPERS
# ============================================================

def draw_humanoid(draw, cx, cy, body_color, head_color, cape_color=None,
                  weapon=None, weapon_color=None, dy=0, attack=False, attack_frame=0):
    """Draw a pixel art humanoid character centered at (cx, cy).
    dy: vertical shift for idle animation.
    attack: whether in attack pose.
    """
    body_dark = darken(body_color, 0.6)
    body_light = lighten(body_color, 1.2)
    head_dark = darken(head_color, 0.7)

    y_off = dy

    # Cape/cloak behind
    if cape_color:
        cape_dark = darken(cape_color, 0.7)
        draw.rectangle([cx-8, cy-4+y_off, cx+8, cy+14+y_off], fill=cape_color)
        draw.rectangle([cx-9, cy+2+y_off, cx-8, cy+12+y_off], fill=cape_dark)
        draw.rectangle([cx+8, cy+2+y_off, cx+9, cy+12+y_off], fill=cape_dark)

    # Body (torso)
    draw.rectangle([cx-6, cy-2+y_off, cx+5, cy+8+y_off], fill=body_color)
    draw.rectangle([cx-6, cy-2+y_off, cx-4, cy+8+y_off], fill=body_light)
    draw.rectangle([cx+4, cy-2+y_off, cx+5, cy+8+y_off], fill=body_dark)

    # Head
    draw.rectangle([cx-5, cy-12+y_off, cx+4, cy-3+y_off], fill=head_color)
    draw.rectangle([cx-5, cy-12+y_off, cx-3, cy-3+y_off], fill=lighten(head_color, 1.1))
    # Eyes
    draw.rectangle([cx-3, cy-8+y_off, cx-2, cy-7+y_off], fill=(20, 20, 40, 255))
    draw.rectangle([cx+1, cy-8+y_off, cx+2, cy-7+y_off], fill=(20, 20, 40, 255))

    # Arms
    if attack and weapon:
        # Attack pose - arm extended
        if attack_frame == 0:
            # Wind up
            draw.rectangle([cx+6, cy-4+y_off, cx+12, cy-2+y_off], fill=body_color)
            draw.rectangle([cx+6, cy-6+y_off, cx+8, cy-4+y_off], fill=body_color)
        else:
            # Swing
            draw.rectangle([cx+6, cy+0+y_off, cx+14, cy+2+y_off], fill=body_color)
    else:
        # Idle arms at sides
        draw.rectangle([cx-8, cy-1+y_off, cx-6, cy+6+y_off], fill=body_color)
        draw.rectangle([cx+6, cy-1+y_off, cx+8, cy+6+y_off], fill=body_color)

    # Legs
    draw.rectangle([cx-4, cy+9+y_off, cx-1, cy+16+y_off], fill=body_dark)
    draw.rectangle([cx+1, cy+9+y_off, cx+4, cy+16+y_off], fill=body_dark)

    # Feet
    draw.rectangle([cx-5, cy+16+y_off, cx-1, cy+18+y_off], fill=darken(body_dark, 0.8))
    draw.rectangle([cx+1, cy+16+y_off, cx+5, cy+18+y_off], fill=darken(body_dark, 0.8))

    # Weapon
    if weapon == "staff" and weapon_color:
        wx = cx + 10 if attack else cx + 9
        wy_start = cy - 16 + y_off if not attack else cy - 14 + y_off
        wy_end = cy + 10 + y_off
        draw.rectangle([wx, wy_start, wx+1, wy_end], fill=weapon_color)
        # Staff orb
        orb_color = lighten(weapon_color, 1.4)
        draw.ellipse([wx-2, wy_start-3, wx+3, wy_start+2], fill=orb_color)
    elif weapon == "sword" and weapon_color:
        if attack:
            if attack_frame == 0:
                draw.rectangle([cx+9, cy-14+y_off, cx+11, cy-4+y_off], fill=weapon_color)
            else:
                draw.rectangle([cx+8, cy-2+y_off, cx+18, cy+0+y_off], fill=weapon_color)
                draw.rectangle([cx+16, cy-1+y_off, cx+18, cy+1+y_off], fill=lighten(weapon_color))
        else:
            draw.rectangle([cx+8, cy-10+y_off, cx+10, cy+2+y_off], fill=weapon_color)
    elif weapon == "crossbow" and weapon_color:
        if attack:
            draw.rectangle([cx+6, cy-2+y_off, cx+16, cy+0+y_off], fill=weapon_color)
            draw.rectangle([cx+14, cy-4+y_off, cx+16, cy+2+y_off], fill=weapon_color)
        else:
            draw.rectangle([cx+6, cy-4+y_off, cx+16, cy-2+y_off], fill=weapon_color)
            draw.rectangle([cx+14, cy-6+y_off, cx+16, cy+0+y_off], fill=weapon_color)
    elif weapon == "dagger" and weapon_color:
        if attack:
            if attack_frame == 0:
                draw.rectangle([cx+8, cy-8+y_off, cx+10, cy-2+y_off], fill=weapon_color)
                draw.rectangle([cx-10, cy-8+y_off, cx-8, cy-2+y_off], fill=weapon_color)
            else:
                draw.rectangle([cx+8, cy-1+y_off, cx+14, cy+1+y_off], fill=weapon_color)
                draw.rectangle([cx-14, cy-1+y_off, cx-8, cy+1+y_off], fill=weapon_color)
        else:
            draw.rectangle([cx+8, cy-6+y_off, cx+10, cy+0+y_off], fill=weapon_color)
            draw.rectangle([cx-10, cy-6+y_off, cx-8, cy+0+y_off], fill=weapon_color)
    elif weapon == "axe" and weapon_color:
        if attack:
            if attack_frame == 0:
                draw.rectangle([cx+8, cy-14+y_off, cx+10, cy-2+y_off], fill=darken(weapon_color, 0.6))
                draw.rectangle([cx+6, cy-16+y_off, cx+12, cy-12+y_off], fill=weapon_color)
            else:
                draw.rectangle([cx+6, cy-2+y_off, cx+18, cy+0+y_off], fill=darken(weapon_color, 0.6))
                draw.rectangle([cx+14, cy-4+y_off, cx+20, cy+2+y_off], fill=weapon_color)
        else:
            draw.rectangle([cx+8, cy-14+y_off, cx+10, cy+0+y_off], fill=darken(weapon_color, 0.6))
            draw.rectangle([cx+6, cy-16+y_off, cx+12, cy-12+y_off], fill=weapon_color)
    elif weapon == "shield_weapon" and weapon_color:
        # Big shield in front
        draw.rectangle([cx-12, cy-6+y_off, cx-6, cy+8+y_off], fill=weapon_color)
        draw.rectangle([cx-11, cy-5+y_off, cx-7, cy+7+y_off], fill=lighten(weapon_color, 1.2))
        # Cross on shield
        draw.rectangle([cx-10, cy-2+y_off, cx-8, cy+4+y_off], fill=darken(weapon_color, 0.5))
        draw.rectangle([cx-11, cy+0+y_off, cx-7, cy+2+y_off], fill=darken(weapon_color, 0.5))


def draw_magic_effect(draw, cx, cy, color, style="fire", frame=0):
    """Draw a magic effect around position."""
    c_light = lighten(color, 1.4)
    c_dark = darken(color, 0.7)

    if style == "fire":
        # Flickering flames
        for i in range(5):
            fx = cx - 8 + i * 4 + (frame * 2 - 1)
            fy = cy - 14 - random.randint(0, 4) - frame * 2
            h = random.randint(4, 8)
            draw.rectangle([fx, fy, fx+2, fy+h], fill=color)
            draw.rectangle([fx, fy, fx+1, fy+2], fill=c_light)
    elif style == "ice":
        # Ice crystals
        off = frame * 2
        for angle_i in range(4):
            ax = cx + int(10 * math.cos(angle_i * 1.57 + off * 0.3))
            ay = cy - 6 + int(6 * math.sin(angle_i * 1.57 + off * 0.3))
            draw.rectangle([ax-1, ay-2, ax+1, ay+2], fill=color)
            draw.point([ax, ay-3], fill=c_light)
    elif style == "poison":
        # Green bubbles
        for i in range(4):
            bx = cx - 6 + i * 4 + frame
            by = cy - 10 - (i % 2) * 4 - frame
            r = 2 + (i % 2)
            draw.ellipse([bx-r, by-r, bx+r, by+r], fill=color)
            draw.point([bx-1, by-1], fill=c_light)
    elif style == "lightning":
        # Electric sparks
        for i in range(3):
            sx = cx - 4 + i * 4 + frame
            sy = cy - 16
            # Zigzag down
            for j in range(4):
                nx = sx + (1 if j % 2 == 0 else -1) * 2
                ny = sy + j * 3
                draw.line([sx, sy, nx, ny], fill=color, width=1)
                draw.point([nx, ny], fill=c_light)
                sx, sy = nx, ny


def draw_creature(draw, cx, cy, body_color, style="dragon", dy=0, attack=False, attack_frame=0):
    """Draw a non-humanoid creature."""
    dark = darken(body_color, 0.6)
    light = lighten(body_color, 1.3)

    if style == "dragon":
        y = cy + dy
        # Body
        draw.ellipse([cx-10, y-4, cx+8, y+8], fill=body_color)
        draw.ellipse([cx-8, y-3, cx+6, y+6], fill=light)
        # Head
        draw.ellipse([cx+4, y-10, cx+14, y-2], fill=body_color)
        draw.rectangle([cx+12, y-8, cx+16, y-4], fill=body_color)  # snout
        draw.point([cx+9, y-7], fill=(255, 200, 0, 255))  # eye
        # Wings
        wing_up = -4 if not attack else -8 + attack_frame * 4
        draw.polygon([(cx-4, y-2), (cx-16, y+wing_up-6), (cx-10, y+2)], fill=dark)
        draw.polygon([(cx+2, y-2), (cx+14, y+wing_up-8), (cx+8, y+2)], fill=dark)
        # Tail
        draw.line([(cx-10, y+4), (cx-18, y+10), (cx-20, y+8)], fill=dark, width=2)
        # Legs
        draw.rectangle([cx-4, y+8, cx-2, y+14], fill=dark)
        draw.rectangle([cx+2, y+8, cx+4, y+14], fill=dark)
        # Fire breath in attack
        if attack:
            for i in range(4):
                fx = cx + 16 + i * 3
                fy = y - 6 + (i % 2) * 2
                draw.rectangle([fx, fy, fx+2, fy+2], fill=(255, 100+i*30, 0, 200))

    elif style == "phoenix":
        y = cy + dy
        # Body - bird shape
        draw.ellipse([cx-8, y-2, cx+6, y+8], fill=body_color)
        # Head
        draw.ellipse([cx+2, y-8, cx+10, y-1], fill=lighten(body_color, 1.2))
        draw.point([cx+7, y-5], fill=(20, 20, 20, 255))  # eye
        draw.rectangle([cx+9, y-4, cx+12, y-3], fill=(255, 200, 0, 255))  # beak
        # Wings - spread
        wing_angle = 4 if attack else 0
        draw.polygon([(cx-4, y), (cx-18, y-12-wing_angle), (cx-14, y+4)], fill=lighten(body_color))
        draw.polygon([(cx+4, y), (cx+16, y-14-wing_angle), (cx+12, y+2)], fill=lighten(body_color))
        # Tail feathers
        for i in range(3):
            tx = cx - 8 - i * 3
            ty = y + 6 + i * 2
            draw.rectangle([tx, ty, tx+2, ty+4], fill=(255, 180+i*20, 0, 200))
        # Fire aura
        if attack:
            for i in range(6):
                fx = cx - 12 + i * 5
                fy = y - 10 - random.randint(0, 4)
                draw.rectangle([fx, fy, fx+1, fy+3], fill=(255, 200, 50, 150))

    elif style == "ghost":
        y = cy + dy
        alpha = 200
        ghost_color = (*body_color[:3], alpha)
        ghost_light = (*lighten(body_color)[:3], alpha)
        # Ghostly body - wavy bottom
        draw.ellipse([cx-10, y-10, cx+10, y+4], fill=ghost_color)
        # Wavy tail
        for i in range(5):
            wx = cx - 10 + i * 5
            wy = y + 4 + (i % 2) * 4
            draw.rectangle([wx, y+2, wx+4, wy+2], fill=ghost_color)
        # Eyes - glowing
        draw.rectangle([cx-5, y-6, cx-2, y-3], fill=(200, 200, 255, 255))
        draw.rectangle([cx+2, y-6, cx+5, y-3], fill=(200, 200, 255, 255))
        # Inner glow
        draw.ellipse([cx-6, y-6, cx+6, y+0], fill=ghost_light)
        if attack:
            # Soul wisps
            for i in range(3):
                sx = cx - 8 + i * 8 + attack_frame * 2
                sy = y - 14 - i * 3
                draw.ellipse([sx-1, sy-1, sx+2, sy+2], fill=ghost_light)


def draw_golem(draw, cx, cy, body_color, dy=0, attack=False, attack_frame=0):
    """Draw a fortress-like golem."""
    dark = darken(body_color, 0.6)
    light = lighten(body_color, 1.3)
    y = cy + dy

    # Large blocky body
    draw.rectangle([cx-12, y-6, cx+12, y+10], fill=body_color)
    draw.rectangle([cx-11, y-5, cx+11, y+9], fill=light)
    # Battlements on top
    for i in range(5):
        bx = cx - 10 + i * 5
        draw.rectangle([bx, y-12, bx+3, y-6], fill=body_color)
    # Face - slit eyes
    draw.rectangle([cx-6, y-2, cx-2, y+0], fill=(200, 150, 50, 255))
    draw.rectangle([cx+2, y-2, cx+6, y+0], fill=(200, 150, 50, 255))
    # Arms
    ax = 14 if attack else 12
    draw.rectangle([cx-ax-2, y-4, cx-12, y+6], fill=dark)
    draw.rectangle([cx+12, y-4, cx+ax+2, y+6], fill=dark)
    # Legs
    draw.rectangle([cx-8, y+10, cx-3, y+16], fill=dark)
    draw.rectangle([cx+3, y+10, cx+8, y+16], fill=dark)


# ============================================================
# UNIT DEFINITIONS
# ============================================================

def draw_unit(unit_id, frame_type, frame_idx):
    """Draw a specific unit frame. Returns a 64x64 image."""
    img, draw = cell_img(0, 0)
    cx, cy = 32, 30  # center point

    is_attack = frame_type == "attack"
    is_idle = frame_type == "idle"
    dy = frame_idx * 2 if is_idle else 0  # subtle bounce for idle

    if unit_id == 0:  # Fire mage
        draw_humanoid(draw, cx, cy, (140, 40, 30), (220, 180, 140),
                     cape_color=(180, 60, 20), weapon="staff",
                     weapon_color=(200, 80, 20), dy=dy,
                     attack=is_attack, attack_frame=frame_idx)
        if is_attack:
            draw_magic_effect(draw, cx, cy, (255, 120, 30), "fire", frame_idx)

    elif unit_id == 1:  # Frost mage
        draw_humanoid(draw, cx, cy, (60, 80, 140), (200, 200, 230),
                     cape_color=(80, 120, 180), weapon="staff",
                     weapon_color=(100, 180, 255), dy=dy,
                     attack=is_attack, attack_frame=frame_idx)
        if is_attack:
            draw_magic_effect(draw, cx, cy, (130, 200, 255), "ice", frame_idx)

    elif unit_id == 2:  # Poison alchemist
        draw_humanoid(draw, cx, cy, (50, 80, 40), (180, 200, 160),
                     cape_color=(40, 60, 30), weapon="staff",
                     weapon_color=(80, 180, 50), dy=dy,
                     attack=is_attack, attack_frame=frame_idx)
        if is_attack:
            draw_magic_effect(draw, cx, cy, (100, 200, 60), "poison", frame_idx)

    elif unit_id == 3:  # Iron Wall (shield knight)
        draw_humanoid(draw, cx, cy, (140, 140, 160), (180, 180, 200),
                     cape_color=None, weapon="shield_weapon",
                     weapon_color=(160, 160, 180), dy=dy,
                     attack=is_attack, attack_frame=frame_idx)

    elif unit_id == 4:  # Lightning mage
        draw_humanoid(draw, cx, cy, (120, 110, 40), (220, 200, 160),
                     cape_color=(100, 90, 30), weapon="staff",
                     weapon_color=(255, 240, 80), dy=dy,
                     attack=is_attack, attack_frame=frame_idx)
        if is_attack:
            draw_magic_effect(draw, cx, cy, (255, 255, 100), "lightning", frame_idx)

    elif unit_id == 5:  # Sniper (ranger)
        draw_humanoid(draw, cx, cy, (90, 70, 50), (200, 180, 150),
                     cape_color=(70, 60, 40), weapon="crossbow",
                     weapon_color=(120, 80, 40), dy=dy,
                     attack=is_attack, attack_frame=frame_idx)

    elif unit_id == 6:  # Enhance (priest)
        draw_humanoid(draw, cx, cy, (200, 180, 120), (220, 200, 170),
                     cape_color=(220, 200, 100), weapon="staff",
                     weapon_color=(240, 220, 100), dy=dy,
                     attack=is_attack, attack_frame=frame_idx)
        if is_attack:
            # Golden glow
            for i in range(4):
                gx = cx - 6 + i * 4
                gy = cy - 16 - frame_idx * 2
                draw.ellipse([gx-1, gy-1, gx+2, gy+2], fill=(255, 230, 100, 180))

    elif unit_id == 7:  # Storm mage
        draw_humanoid(draw, cx, cy, (40, 40, 100), (180, 180, 220),
                     cape_color=(30, 30, 80), weapon="staff",
                     weapon_color=(60, 60, 160), dy=dy,
                     attack=is_attack, attack_frame=frame_idx)
        if is_attack:
            draw_magic_effect(draw, cx, cy, (100, 100, 255), "lightning", frame_idx)

    elif unit_id == 8:  # Assassin
        draw_humanoid(draw, cx, cy, (50, 30, 30), (180, 160, 150),
                     cape_color=(40, 20, 25), weapon="dagger",
                     weapon_color=(180, 180, 200), dy=dy,
                     attack=is_attack, attack_frame=frame_idx)

    elif unit_id == 9:  # Dragon
        draw_creature(draw, cx, cy, (180, 50, 30), "dragon", dy=dy,
                     attack=is_attack, attack_frame=frame_idx)

    elif unit_id == 10:  # ElectroPoison
        draw_humanoid(draw, cx, cy, (60, 100, 40), (200, 220, 160),
                     cape_color=(50, 80, 30), weapon="staff",
                     weapon_color=(150, 220, 50), dy=dy,
                     attack=is_attack, attack_frame=frame_idx)
        if is_attack:
            # Combined effect
            draw_magic_effect(draw, cx, cy, (100, 200, 60), "poison", frame_idx)
            draw_magic_effect(draw, cx+4, cy+2, (255, 255, 100), "lightning", frame_idx)

    elif unit_id == 11:  # Executioner
        draw_humanoid(draw, cx, cy, (60, 20, 20), (160, 140, 130),
                     cape_color=(50, 15, 15), weapon="axe",
                     weapon_color=(160, 160, 170), dy=dy,
                     attack=is_attack, attack_frame=frame_idx)

    elif unit_id == 12:  # Citadel golem
        draw_golem(draw, cx, cy, (160, 150, 120), dy=dy,
                  attack=is_attack, attack_frame=frame_idx)

    elif unit_id == 13:  # Phoenix
        draw_creature(draw, cx, cy, (220, 100, 30), "phoenix", dy=dy,
                     attack=is_attack, attack_frame=frame_idx)

    elif unit_id == 14:  # Soul (ghost)
        draw_creature(draw, cx, cy, (140, 100, 200), "ghost", dy=dy,
                     attack=is_attack, attack_frame=frame_idx)

    # Draw outline (1px black border around non-transparent pixels)
    img = add_outline(img)
    return img


def add_outline(img, outline_color=(10, 10, 10, 255)):
    """Add a 1px outline around all non-transparent pixels."""
    w, h = img.size
    pixels = img.load()
    outline_img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out_pixels = outline_img.load()

    for y in range(h):
        for x in range(w):
            if pixels[x, y][3] > 0:
                out_pixels[x, y] = pixels[x, y]
            else:
                # Check neighbors
                for dx, dy in [(-1,0),(1,0),(0,-1),(0,1)]:
                    nx, ny = x+dx, y+dy
                    if 0 <= nx < w and 0 <= ny < h and pixels[nx, ny][3] > 50:
                        out_pixels[x, y] = outline_color
                        break
    return outline_img


# ============================================================
# ENEMIES
# ============================================================

def draw_enemy(enemy_id, frame_idx):
    """Draw enemy frame. frame_idx: 0=walk1, 1=walk2, 2=hit"""
    img, draw = cell_img(0, 0)
    cx, cy = 32, 30
    is_hit = frame_idx == 2
    leg_offset = 2 if frame_idx == 1 else 0

    hit_tint = (255, 100, 100, 255) if is_hit else None

    if enemy_id == 0:  # Goblin
        body = (80, 140, 60) if not is_hit else (200, 140, 100)
        dark = darken(body)
        # Body
        draw.ellipse([cx-7, cy-4, cx+7, cy+8], fill=body)
        # Head - large for goblin
        draw.ellipse([cx-8, cy-14, cx+8, cy-2], fill=body)
        draw.ellipse([cx-6, cy-12, cx+6, cy-4], fill=lighten(body))
        # Ears
        draw.polygon([(cx-8, cy-10), (cx-14, cy-14), (cx-8, cy-6)], fill=body)
        draw.polygon([(cx+8, cy-10), (cx+14, cy-14), (cx+8, cy-6)], fill=body)
        # Eyes
        draw.rectangle([cx-4, cy-9, cx-2, cy-7], fill=(255, 200, 0, 255))
        draw.rectangle([cx+2, cy-9, cx+4, cy-7], fill=(255, 200, 0, 255))
        # Legs
        draw.rectangle([cx-4, cy+8, cx-1, cy+16+leg_offset], fill=dark)
        draw.rectangle([cx+1, cy+8, cx+4, cy+16-leg_offset], fill=dark)
        # Club
        draw.rectangle([cx+8, cy-8, cx+10, cy+4], fill=(120, 80, 40, 255))

    elif enemy_id == 1:  # Fast imp
        body = (160, 50, 40) if not is_hit else (220, 100, 100)
        dark = darken(body)
        # Small agile body
        draw.ellipse([cx-5, cy-2, cx+5, cy+6], fill=body)
        # Head
        draw.ellipse([cx-6, cy-12, cx+6, cy-2], fill=body)
        # Horns
        draw.rectangle([cx-6, cy-16, cx-4, cy-10], fill=dark)
        draw.rectangle([cx+4, cy-16, cx+6, cy-10], fill=dark)
        # Eyes
        draw.rectangle([cx-3, cy-8, cx-1, cy-6], fill=(255, 255, 100, 255))
        draw.rectangle([cx+1, cy-8, cx+3, cy-6], fill=(255, 255, 100, 255))
        # Legs - running pose
        draw.rectangle([cx-3, cy+6, cx-1, cy+14+leg_offset], fill=dark)
        draw.rectangle([cx+1, cy+6, cx+3, cy+14-leg_offset], fill=dark)
        # Tail
        draw.line([(cx-5, cy+4), (cx-12, cy+0), (cx-14, cy-2)], fill=dark, width=1)

    elif enemy_id == 2:  # Tank troll
        body = (120, 110, 90) if not is_hit else (180, 130, 130)
        dark = darken(body)
        # Large body
        draw.ellipse([cx-14, cy-6, cx+14, cy+12], fill=body)
        # Head
        draw.ellipse([cx-8, cy-16, cx+8, cy-4], fill=body)
        # Armor plates
        draw.rectangle([cx-12, cy-2, cx+12, cy+4], fill=darken(body, 0.5))
        draw.rectangle([cx-10, cy-1, cx+10, cy+3], fill=(100, 100, 110, 255))
        # Eyes
        draw.rectangle([cx-4, cy-12, cx-2, cy-10], fill=(200, 50, 50, 255))
        draw.rectangle([cx+2, cy-12, cx+4, cy-10], fill=(200, 50, 50, 255))
        # Legs - thick
        draw.rectangle([cx-8, cy+12, cx-3, cy+18+leg_offset//2], fill=dark)
        draw.rectangle([cx+3, cy+12, cx+8, cy+18-leg_offset//2], fill=dark)

    elif enemy_id == 3:  # Flying bat/wyvern
        body = (80, 50, 100) if not is_hit else (150, 100, 150)
        dark = darken(body)
        wing_y = -4 if frame_idx == 0 else 2
        # Body
        draw.ellipse([cx-6, cy-2, cx+6, cy+6], fill=body)
        # Head
        draw.ellipse([cx-4, cy-8, cx+4, cy-1], fill=body)
        draw.rectangle([cx-2, cy-6, cx-1, cy-5], fill=(200, 50, 50, 255))
        draw.rectangle([cx+1, cy-6, cx+2, cy-5], fill=(200, 50, 50, 255))
        # Wings
        draw.polygon([(cx-6, cy), (cx-22, cy+wing_y-6), (cx-16, cy+6)], fill=dark)
        draw.polygon([(cx+6, cy), (cx+22, cy+wing_y-6), (cx+16, cy+6)], fill=dark)
        # Tail
        draw.line([(cx, cy+6), (cx, cy+14)], fill=dark, width=1)

    elif enemy_id == 4:  # Boss demon lord
        body = (120, 30, 20) if not is_hit else (200, 80, 80)
        dark = darken(body)
        # Large imposing body
        draw.rectangle([cx-14, cy-8, cx+14, cy+10], fill=body)
        draw.rectangle([cx-12, cy-6, cx+12, cy+8], fill=lighten(body, 1.1))
        # Head with horns
        draw.ellipse([cx-10, cy-20, cx+10, cy-6], fill=body)
        draw.polygon([(cx-8, cy-18), (cx-14, cy-28), (cx-4, cy-18)], fill=dark)
        draw.polygon([(cx+8, cy-18), (cx+14, cy-28), (cx+4, cy-18)], fill=dark)
        # Glowing eyes
        draw.rectangle([cx-6, cy-16, cx-2, cy-13], fill=(255, 200, 0, 255))
        draw.rectangle([cx+2, cy-16, cx+6, cy-13], fill=(255, 200, 0, 255))
        # Mouth
        draw.rectangle([cx-4, cy-11, cx+4, cy-9], fill=(200, 50, 20, 255))
        # Arms
        draw.rectangle([cx-18, cy-6, cx-14, cy+4], fill=dark)
        draw.rectangle([cx+14, cy-6, cx+18, cy+4], fill=dark)
        # Legs
        draw.rectangle([cx-8, cy+10, cx-3, cy+18+leg_offset//2], fill=dark)
        draw.rectangle([cx+3, cy+10, cx+8, cy+18-leg_offset//2], fill=dark)
        # Lava cracks on body
        draw.line([(cx-6, cy-2), (cx, cy+4), (cx+4, cy-1)], fill=(255, 150, 30, 200), width=1)

    elif enemy_id == 5:  # Miniboss ogre
        body = (60, 90, 50) if not is_hit else (120, 140, 110)
        dark = darken(body)
        # Bulky body
        draw.ellipse([cx-12, cy-6, cx+12, cy+10], fill=body)
        # Armor
        draw.rectangle([cx-10, cy-4, cx+10, cy+2], fill=(140, 120, 50, 255))
        draw.rectangle([cx-8, cy-3, cx+8, cy+1], fill=(160, 140, 60, 255))
        # Head
        draw.ellipse([cx-8, cy-16, cx+8, cy-4], fill=body)
        # Eyes
        draw.rectangle([cx-4, cy-12, cx-2, cy-10], fill=(255, 200, 0, 255))
        draw.rectangle([cx+2, cy-12, cx+4, cy-10], fill=(255, 200, 0, 255))
        # Tusks
        draw.rectangle([cx-4, cy-7, cx-3, cy-4], fill=(240, 230, 200, 255))
        draw.rectangle([cx+3, cy-7, cx+4, cy-4], fill=(240, 230, 200, 255))
        # Weapon - big mace
        draw.rectangle([cx+12, cy-14, cx+14, cy+4], fill=(100, 80, 40, 255))
        draw.ellipse([cx+10, cy-18, cx+16, cy-12], fill=(120, 120, 130, 255))
        # Legs
        draw.rectangle([cx-6, cy+10, cx-2, cy+18+leg_offset//2], fill=dark)
        draw.rectangle([cx+2, cy+10, cx+6, cy+18-leg_offset//2], fill=dark)

    img = add_outline(img)
    return img


# ============================================================
# PROJECTILES
# ============================================================

def draw_projectile(proj_id, frame_idx):
    """Draw projectile frame."""
    img, draw = cell_img(0, 0)
    cx, cy = 32, 32
    pulse = 1 + frame_idx  # size variation

    if proj_id == 0:  # Arrow
        # Wooden arrow pointing right
        draw.rectangle([cx-12, cy-1, cx+10, cy+1], fill=(140, 100, 50, 255))
        # Arrowhead
        draw.polygon([(cx+10, cy-3), (cx+16, cy), (cx+10, cy+3)], fill=(180, 180, 190, 255))
        # Fletching
        draw.polygon([(cx-12, cy-3), (cx-8, cy), (cx-12, cy+3)], fill=(200, 50, 50, 255))
        if frame_idx == 1:
            # Slight rotation - shift pixels
            draw.rectangle([cx-12, cy, cx+10, cy+2], fill=(140, 100, 50, 255))

    elif proj_id == 1:  # Fireball
        r = 6 + pulse
        draw.ellipse([cx-r, cy-r, cx+r, cy+r], fill=(255, 120, 20, 255))
        draw.ellipse([cx-r+2, cy-r+2, cx+r-2, cy+r-2], fill=(255, 200, 50, 255))
        draw.ellipse([cx-2, cy-2, cx+2, cy+2], fill=(255, 255, 200, 255))
        # Trail
        for i in range(3):
            tx = cx - 8 - i * 4
            ty = cy + (i % 2) * 2 - 1
            tr = 3 - i
            draw.ellipse([tx-tr, ty-tr, tx+tr, ty+tr], fill=(255, 100+i*40, 0, 150-i*40))

    elif proj_id == 2:  # Ice shard
        # Crystal shape
        pts = [(cx, cy-8-pulse), (cx+6, cy), (cx, cy+8+pulse), (cx-6, cy)]
        draw.polygon(pts, fill=(150, 220, 255, 255))
        inner = [(cx, cy-4), (cx+3, cy), (cx, cy+4), (cx-3, cy)]
        draw.polygon(inner, fill=(200, 240, 255, 255))
        # Sparkle
        draw.point([cx+2, cy-3], fill=(255, 255, 255, 255))

    elif proj_id == 3:  # Poison bolt
        r = 5 + pulse
        draw.ellipse([cx-r, cy-r, cx+r, cy+r], fill=(80, 180, 40, 255))
        draw.ellipse([cx-r+2, cy-r+2, cx+r-2, cy+r-2], fill=(100, 220, 60, 255))
        # Drips
        for i in range(2):
            dx = cx - 3 + i * 6
            draw.rectangle([dx, cy+r, dx+1, cy+r+3+frame_idx], fill=(60, 160, 30, 200))

    elif proj_id == 4:  # Lightning bolt
        # Zigzag bolt
        color = (255, 255, 100, 255)
        bright = (255, 255, 220, 255)
        points = [(cx-10, cy-2), (cx-4, cy-2), (cx-2, cy-6), (cx+4, cy-1),
                  (cx+2, cy+2), (cx+8, cy+3), (cx+12, cy)]
        draw.line(points, fill=color, width=2+pulse)
        # Core glow
        draw.line(points, fill=bright, width=1)

    elif proj_id == 5:  # Generic energy orb
        r = 5 + pulse
        draw.ellipse([cx-r, cy-r, cx+r, cy+r], fill=(200, 200, 255, 255))
        draw.ellipse([cx-r+2, cy-r+2, cx+r-2, cy+r-2], fill=(230, 230, 255, 255))
        draw.ellipse([cx-2, cy-2, cx+2, cy+2], fill=(255, 255, 255, 255))

    img = add_outline(img)
    return img


# ============================================================
# TILES
# ============================================================

def draw_tile(tile_name):
    """Draw a tile. Returns 64x64 image."""
    img, draw = cell_img(0, 0)

    random.seed(hash(tile_name) % 10000)  # deterministic

    if tile_name == "grass":
        base = (60, 120, 40, 255)
        draw.rectangle([0, 0, 63, 63], fill=base)
        # Grass variation
        for _ in range(80):
            x, y = random.randint(0, 63), random.randint(0, 63)
            v = random.randint(-15, 15)
            c = (60+v, 120+v, 40+v//2, 255)
            draw.point([x, y], fill=c)
        # Grass blades
        for _ in range(12):
            x = random.randint(2, 62)
            y = random.randint(2, 62)
            draw.line([(x, y), (x+1, y-3)], fill=(50, 140, 35, 255))

    elif tile_name == "stone":
        base = (110, 110, 115, 255)
        draw.rectangle([0, 0, 63, 63], fill=base)
        # Stone texture
        for _ in range(60):
            x, y = random.randint(0, 63), random.randint(0, 63)
            v = random.randint(-10, 10)
            draw.point([x, y], fill=(110+v, 110+v, 115+v, 255))
        # Cracks
        for _ in range(3):
            x = random.randint(10, 50)
            y = random.randint(10, 50)
            draw.line([(x, y), (x+random.randint(-8, 8), y+random.randint(-8, 8))],
                     fill=(90, 90, 95, 255))

    elif tile_name in ("path_h", "path_v", "path_turn_tl", "path_turn_tr",
                       "path_turn_bl", "path_turn_br", "path_cross"):
        # Brown dirt path
        bg = (60, 120, 40, 255)  # grass background
        path = (140, 100, 60, 255)
        path_dark = (120, 85, 50, 255)
        path_light = (160, 120, 75, 255)

        draw.rectangle([0, 0, 63, 63], fill=bg)

        if tile_name == "path_h":
            draw.rectangle([0, 16, 63, 47], fill=path)
            draw.rectangle([0, 16, 63, 18], fill=path_dark)
            draw.rectangle([0, 45, 63, 47], fill=path_dark)
            # Texture
            for _ in range(20):
                x, y = random.randint(0, 63), random.randint(18, 44)
                draw.point([x, y], fill=path_light)
        elif tile_name == "path_v":
            draw.rectangle([16, 0, 47, 63], fill=path)
            draw.rectangle([16, 0, 18, 63], fill=path_dark)
            draw.rectangle([45, 0, 47, 63], fill=path_dark)
            for _ in range(20):
                x, y = random.randint(18, 44), random.randint(0, 63)
                draw.point([x, y], fill=path_light)
        elif tile_name == "path_turn_tl":
            draw.rectangle([16, 16, 63, 47], fill=path)
            draw.rectangle([16, 16, 47, 63], fill=path)
            draw.rectangle([16, 16, 18, 63], fill=path_dark)
            draw.rectangle([16, 16, 63, 18], fill=path_dark)
        elif tile_name == "path_turn_tr":
            draw.rectangle([0, 16, 47, 47], fill=path)
            draw.rectangle([16, 16, 47, 63], fill=path)
            draw.rectangle([0, 16, 47, 18], fill=path_dark)
            draw.rectangle([45, 16, 47, 63], fill=path_dark)
        elif tile_name == "path_turn_bl":
            draw.rectangle([16, 16, 63, 47], fill=path)
            draw.rectangle([16, 0, 47, 47], fill=path)
            draw.rectangle([16, 0, 18, 47], fill=path_dark)
            draw.rectangle([16, 45, 63, 47], fill=path_dark)
        elif tile_name == "path_turn_br":
            draw.rectangle([0, 16, 47, 47], fill=path)
            draw.rectangle([16, 0, 47, 47], fill=path)
            draw.rectangle([0, 45, 47, 47], fill=path_dark)
            draw.rectangle([45, 0, 47, 47], fill=path_dark)
        elif tile_name == "path_cross":
            draw.rectangle([0, 16, 63, 47], fill=path)
            draw.rectangle([16, 0, 47, 63], fill=path)
            draw.rectangle([0, 16, 63, 18], fill=path_dark)
            draw.rectangle([0, 45, 63, 47], fill=path_dark)
            draw.rectangle([16, 0, 18, 63], fill=path_dark)
            draw.rectangle([45, 0, 47, 63], fill=path_dark)

    elif tile_name == "water":
        base = (40, 80, 160, 255)
        draw.rectangle([0, 0, 63, 63], fill=base)
        # Wave pattern
        for y in range(0, 64, 8):
            for x in range(0, 64, 2):
                off = int(3 * math.sin(x * 0.2 + y * 0.1))
                wy = y + off
                if 0 <= wy < 64:
                    draw.point([x, wy], fill=(80, 140, 220, 255))
        # Highlights
        for _ in range(8):
            x, y = random.randint(0, 63), random.randint(0, 63)
            draw.point([x, y], fill=(100, 180, 240, 255))

    elif tile_name == "grid_bg":
        draw.rectangle([0, 0, 63, 63], fill=(40, 35, 30, 255))
        # Subtle stone texture
        for _ in range(40):
            x, y = random.randint(0, 63), random.randint(0, 63)
            v = random.randint(-5, 5)
            draw.point([x, y], fill=(40+v, 35+v, 30+v, 255))

    elif tile_name == "grid_cell":
        draw.rectangle([0, 0, 63, 63], fill=(55, 48, 42, 230))
        # Border
        draw.rectangle([0, 0, 63, 1], fill=(80, 70, 60, 255))
        draw.rectangle([0, 62, 63, 63], fill=(80, 70, 60, 255))
        draw.rectangle([0, 0, 1, 63], fill=(80, 70, 60, 255))
        draw.rectangle([62, 0, 63, 63], fill=(80, 70, 60, 255))
        # Corner highlights
        draw.point([1, 1], fill=(100, 90, 75, 255))
        draw.point([62, 1], fill=(100, 90, 75, 255))
        draw.point([1, 62], fill=(100, 90, 75, 255))
        draw.point([62, 62], fill=(100, 90, 75, 255))

    return img


# ============================================================
# HUD ELEMENTS
# ============================================================

def draw_hud(hud_name):
    """Draw HUD element. Returns 64x64 image."""
    img, draw = cell_img(0, 0)

    if hud_name == "btn_normal":
        # Medieval button frame
        draw.rectangle([2, 2, 61, 61], fill=(60, 45, 30, 255))
        draw.rectangle([4, 4, 59, 59], fill=(80, 60, 40, 255))
        # Metal corners
        for (x, y) in [(2,2), (58,2), (2,58), (58,58)]:
            draw.rectangle([x, y, x+3, y+3], fill=(140, 130, 100, 255))
        # Highlight top
        draw.rectangle([4, 4, 59, 6], fill=(100, 80, 55, 255))
        # Shadow bottom
        draw.rectangle([4, 57, 59, 59], fill=(50, 35, 22, 255))

    elif hud_name == "btn_pressed":
        draw.rectangle([2, 2, 61, 61], fill=(50, 35, 22, 255))
        draw.rectangle([4, 4, 59, 59], fill=(65, 48, 32, 255))
        for (x, y) in [(2,2), (58,2), (2,58), (58,58)]:
            draw.rectangle([x, y, x+3, y+3], fill=(110, 100, 80, 255))
        draw.rectangle([4, 4, 59, 6], fill=(50, 35, 22, 255))
        draw.rectangle([4, 57, 59, 59], fill=(75, 55, 38, 255))

    elif hud_name == "btn_disabled":
        draw.rectangle([2, 2, 61, 61], fill=(70, 70, 70, 255))
        draw.rectangle([4, 4, 59, 59], fill=(90, 90, 90, 255))
        for (x, y) in [(2,2), (58,2), (2,58), (58,58)]:
            draw.rectangle([x, y, x+3, y+3], fill=(110, 110, 110, 255))

    elif hud_name == "panel_bg":
        draw.rectangle([0, 0, 63, 63], fill=(45, 35, 25, 240))
        # Wood grain
        for y in range(0, 64, 4):
            draw.rectangle([0, y, 63, y+1], fill=(50, 38, 28, 240))

    elif hud_name == "panel_border":
        # Border frame
        draw.rectangle([0, 0, 63, 3], fill=(120, 100, 60, 255))
        draw.rectangle([0, 60, 63, 63], fill=(120, 100, 60, 255))
        draw.rectangle([0, 0, 3, 63], fill=(120, 100, 60, 255))
        draw.rectangle([60, 0, 63, 63], fill=(120, 100, 60, 255))
        # Inner highlight
        draw.rectangle([1, 1, 62, 2], fill=(150, 130, 80, 255))
        draw.rectangle([1, 1, 2, 62], fill=(150, 130, 80, 255))

    elif hud_name == "bar_border":
        draw.rectangle([0, 20, 63, 43], fill=(80, 70, 50, 255))
        draw.rectangle([2, 22, 61, 41], fill=(30, 25, 20, 255))
        # Metal corners
        draw.rectangle([0, 20, 2, 22], fill=(140, 130, 100, 255))
        draw.rectangle([61, 20, 63, 22], fill=(140, 130, 100, 255))
        draw.rectangle([0, 41, 2, 43], fill=(140, 130, 100, 255))
        draw.rectangle([61, 41, 63, 43], fill=(140, 130, 100, 255))

    elif hud_name == "bar_fill_hp":
        # Red/green gradient bar fill
        draw.rectangle([0, 22, 63, 41], fill=(180, 40, 30, 255))
        draw.rectangle([0, 22, 63, 28], fill=(200, 60, 40, 255))
        draw.rectangle([0, 36, 63, 41], fill=(150, 30, 20, 255))

    elif hud_name == "bar_fill_mp":
        # Blue mana bar
        draw.rectangle([0, 22, 63, 41], fill=(40, 80, 180, 255))
        draw.rectangle([0, 22, 63, 28], fill=(60, 100, 200, 255))
        draw.rectangle([0, 36, 63, 41], fill=(30, 60, 150, 255))

    elif hud_name == "icon_gold":
        # Gold coin
        cx, cy = 32, 32
        draw.ellipse([cx-12, cy-12, cx+12, cy+12], fill=(200, 170, 40, 255))
        draw.ellipse([cx-10, cy-10, cx+10, cy+10], fill=(230, 200, 60, 255))
        draw.ellipse([cx-8, cy-8, cx+8, cy+8], fill=(210, 180, 50, 255))
        # $ symbol
        draw.rectangle([cx-1, cy-6, cx+1, cy+6], fill=(180, 150, 30, 255))
        draw.rectangle([cx-4, cy-4, cx+4, cy-2], fill=(180, 150, 30, 255))
        draw.rectangle([cx-4, cy+2, cx+4, cy+4], fill=(180, 150, 30, 255))

    elif hud_name == "icon_wave":
        # Blue wave/sword icon
        cx, cy = 32, 32
        # Sword
        draw.rectangle([cx-1, cy-14, cx+1, cy+8], fill=(180, 180, 200, 255))
        draw.polygon([(cx-1, cy-14), (cx, cy-18), (cx+1, cy-14)], fill=(200, 200, 220, 255))
        # Crossguard
        draw.rectangle([cx-6, cy+4, cx+6, cy+6], fill=(140, 120, 60, 255))
        # Handle
        draw.rectangle([cx-1, cy+6, cx+1, cy+14], fill=(120, 80, 40, 255))
        # Wave number background
        draw.ellipse([cx-8, cy+8, cx+8, cy+18], fill=(40, 80, 160, 200))

    elif hud_name == "icon_heart":
        # Red heart
        cx, cy = 32, 32
        draw.ellipse([cx-12, cy-8, cx, cy+2], fill=(200, 40, 40, 255))
        draw.ellipse([cx, cy-8, cx+12, cy+2], fill=(200, 40, 40, 255))
        draw.polygon([(cx-12, cy-2), (cx, cy+14), (cx+12, cy-2)], fill=(200, 40, 40, 255))
        # Highlight
        draw.ellipse([cx-8, cy-6, cx-4, cy-2], fill=(230, 80, 80, 255))

    elif hud_name == "icon_attack":
        # Red sword up
        cx, cy = 32, 32
        draw.rectangle([cx-2, cy-16, cx+2, cy+6], fill=(200, 60, 50, 255))
        draw.polygon([(cx-2, cy-16), (cx, cy-20), (cx+2, cy-16)], fill=(220, 80, 60, 255))
        draw.rectangle([cx-8, cy+2, cx+8, cy+6], fill=(200, 60, 50, 255))
        draw.rectangle([cx-2, cy+6, cx+2, cy+14], fill=(160, 40, 30, 255))

    elif hud_name == "icon_speed":
        # Lightning bolt for speed
        cx, cy = 32, 32
        pts = [(cx+4, cy-16), (cx-4, cy-2), (cx+2, cy-2),
               (cx-4, cy+16), (cx+4, cy+2), (cx-2, cy+2)]
        draw.polygon(pts, fill=(255, 220, 50, 255))
        # Outline
        draw.line(pts + [pts[0]], fill=(200, 170, 30, 255), width=1)

    elif hud_name == "icon_range":
        # Target/crosshair
        cx, cy = 32, 32
        draw.ellipse([cx-12, cy-12, cx+12, cy+12], fill=None, outline=(200, 200, 200, 255), width=2)
        draw.ellipse([cx-6, cy-6, cx+6, cy+6], fill=None, outline=(200, 200, 200, 255), width=1)
        draw.line([(cx, cy-14), (cx, cy+14)], fill=(200, 200, 200, 255), width=1)
        draw.line([(cx-14, cy), (cx+14, cy)], fill=(200, 200, 200, 255), width=1)
        draw.ellipse([cx-2, cy-2, cx+2, cy+2], fill=(255, 60, 40, 255))

    elif hud_name == "icon_merge":
        # Two arrows merging into one
        cx, cy = 32, 32
        draw.polygon([(cx-14, cy-8), (cx-4, cy), (cx-14, cy+8)], fill=(100, 200, 100, 255))
        draw.polygon([(cx-14, cy-2), (cx+4, cy-2), (cx+4, cy+2), (cx-14, cy+2)], fill=(100, 200, 100, 255))
        draw.polygon([(cx+4, cy-6), (cx+14, cy), (cx+4, cy+6)], fill=(60, 160, 60, 255))

    elif hud_name == "icon_sell":
        # Gold coin with X
        cx, cy = 32, 32
        draw.ellipse([cx-10, cy-10, cx+10, cy+10], fill=(200, 170, 40, 255))
        draw.ellipse([cx-8, cy-8, cx+8, cy+8], fill=(230, 200, 60, 255))
        # X mark
        draw.line([(cx-5, cy-5), (cx+5, cy+5)], fill=(180, 40, 30, 255), width=2)
        draw.line([(cx+5, cy-5), (cx-5, cy+5)], fill=(180, 40, 30, 255), width=2)

    elif hud_name == "icon_buff":
        # Up arrow with glow
        cx, cy = 32, 32
        draw.polygon([(cx, cy-14), (cx-10, cy+2), (cx+10, cy+2)], fill=(100, 200, 100, 255))
        draw.rectangle([cx-4, cy+2, cx+4, cy+14], fill=(100, 200, 100, 255))
        # Glow
        draw.polygon([(cx, cy-10), (cx-6, cy), (cx+6, cy)], fill=(140, 230, 140, 255))

    return img


# ============================================================
# EFFECTS
# ============================================================

def draw_effect(effect_name, frame_idx):
    """Draw effect frame."""
    img, draw = cell_img(0, 0)
    cx, cy = 32, 32

    if effect_name == "splash":
        # Explosion expanding
        r = 8 + frame_idx * 5
        alpha = max(80, 255 - frame_idx * 50)
        draw.ellipse([cx-r, cy-r, cx+r, cy+r], fill=(255, 120, 20, alpha))
        inner_r = max(2, r - 4)
        draw.ellipse([cx-inner_r, cy-inner_r, cx+inner_r, cy+inner_r],
                    fill=(255, 200, 80, min(255, alpha+40)))
        # Particles
        for i in range(6):
            angle = i * 1.047 + frame_idx * 0.5
            px = cx + int((r+4) * math.cos(angle))
            py = cy + int((r+4) * math.sin(angle))
            draw.rectangle([px-1, py-1, px+1, py+1], fill=(255, 150, 30, alpha))

    elif effect_name == "slow":
        # Ice crystals forming
        alpha = 150 + frame_idx * 50
        for i in range(4 + frame_idx * 2):
            angle = i * (6.28 / (4 + frame_idx * 2))
            r = 8 + frame_idx * 4
            px = cx + int(r * math.cos(angle))
            py = cy + int(r * math.sin(angle))
            # Crystal shape
            draw.polygon([(px, py-3), (px+2, py), (px, py+3), (px-2, py)],
                        fill=(130, 200, 255, min(255, alpha)))

    elif effect_name == "poison":
        # Green cloud pulsing
        r = 10 + frame_idx * 3
        for i in range(5):
            angle = i * 1.257 + frame_idx * 0.3
            bx = cx + int(r * 0.6 * math.cos(angle))
            by = cy + int(r * 0.6 * math.sin(angle))
            br = 5 + (i % 3)
            alpha = 120 + frame_idx * 30
            draw.ellipse([bx-br, by-br, bx+br, by+br],
                        fill=(80, 180, 40, min(255, alpha)))
        # Center
        draw.ellipse([cx-4, cy-4, cx+4, cy+4], fill=(100, 220, 60, 200))

    elif effect_name == "chain":
        # Lightning arc
        for seg in range(3):
            sx = cx - 16 + seg * 12
            sy = cy + random.randint(-4, 4)
            ex = sx + 14
            ey = cy + random.randint(-4, 4)
            mid_x = (sx + ex) // 2
            mid_y = (sy + ey) // 2 + random.randint(-6, 6)
            draw.line([(sx, sy), (mid_x, mid_y), (ex, ey)],
                     fill=(255, 255, 100, 200+frame_idx*20), width=2)
            draw.line([(sx, sy), (mid_x, mid_y), (ex, ey)],
                     fill=(255, 255, 220, 255), width=1)

    elif effect_name == "shield":
        # Blue barrier pulse
        r = 16 + frame_idx * 2
        alpha = 150 - frame_idx * 30
        draw.ellipse([cx-r, cy-r, cx+r, cy+r], fill=None,
                    outline=(100, 160, 255, min(255, alpha+80)), width=2)
        draw.ellipse([cx-r+3, cy-r+3, cx+r-3, cy+r-3], fill=None,
                    outline=(140, 200, 255, min(255, alpha)), width=1)
        # Glow spots
        for i in range(4):
            angle = i * 1.57 + frame_idx * 0.5
            gx = cx + int((r-2) * math.cos(angle))
            gy = cy + int((r-2) * math.sin(angle))
            draw.ellipse([gx-2, gy-2, gx+2, gy+2], fill=(180, 220, 255, min(255, alpha+60)))

    elif effect_name == "buff_aura":
        # Golden glow radiating
        for ring in range(3):
            r = 8 + ring * 6 + frame_idx * 2
            alpha = max(40, 180 - ring * 50 - frame_idx * 20)
            draw.ellipse([cx-r, cy-r, cx+r, cy+r], fill=None,
                        outline=(255, 220, 80, min(255, alpha)), width=1)
        # Sparkles
        for i in range(5):
            angle = i * 1.257 + frame_idx * 0.7
            r = 12 + frame_idx * 3
            sx = cx + int(r * math.cos(angle))
            sy = cy + int(r * math.sin(angle))
            draw.rectangle([sx-1, sy-1, sx+1, sy+1], fill=(255, 240, 120, 200))

    return img


# ============================================================
# MAIN GENERATION
# ============================================================

print("Generating sprite atlas...")

# --- Units (rows 0-1) ---
# 15 units, 4 frames each: idle0, idle1, attack0, attack1
# 8 units per row, 4 cols per unit
print("  Units...")
for unit_id in range(15):
    row = unit_id // 8
    slot = unit_id % 8
    base_col = slot * 4

    random.seed(unit_id * 100)  # Deterministic per unit

    # idle0
    img = draw_unit(unit_id, "idle", 0)
    paste_cell(img, base_col + 0, row)

    # idle1
    random.seed(unit_id * 100 + 1)
    img = draw_unit(unit_id, "idle", 1)
    paste_cell(img, base_col + 1, row)

    # attack0
    random.seed(unit_id * 100 + 2)
    img = draw_unit(unit_id, "attack", 0)
    paste_cell(img, base_col + 2, row)

    # attack1
    random.seed(unit_id * 100 + 3)
    img = draw_unit(unit_id, "attack", 1)
    paste_cell(img, base_col + 3, row)

# --- Enemies (row 4) ---
# 6 enemies, 3 frames each: walk0, walk1, hit
print("  Enemies...")
for enemy_id in range(6):
    base_col = enemy_id * 3
    for frame in range(3):
        random.seed(enemy_id * 100 + frame)
        img = draw_enemy(enemy_id, frame)
        paste_cell(img, base_col + frame, 4)

# --- Projectiles (row 6) ---
print("  Projectiles...")
for proj_id in range(6):
    base_col = proj_id * 2
    for frame in range(2):
        random.seed(proj_id * 100 + frame)
        img = draw_projectile(proj_id, frame)
        paste_cell(img, base_col + frame, 6)

# --- Tiles (row 7) ---
# Matches initTiles() order exactly
print("  Tiles...")
tile_names = [
    "grass", "stone", "path_h", "path_v",
    "path_turn_tl", "path_turn_tr", "path_turn_bl", "path_turn_br",
    "path_cross", "water", "grid_bg", "grid_cell"
]
for i, name in enumerate(tile_names):
    img = draw_tile(name)
    paste_cell(img, i, 7)

# --- HUD (rows 9-10) ---
# Matches initHud() order exactly
print("  HUD...")
hud_names_row9 = [
    "btn_normal", "btn_pressed", "btn_disabled",
    "panel_bg", "panel_border",
    "bar_border", "bar_fill_hp", "bar_fill_mp",
    "icon_gold", "icon_wave", "icon_heart",
    "icon_attack", "icon_speed", "icon_range",
    "icon_merge", "icon_sell"
]
for i, name in enumerate(hud_names_row9):
    img = draw_hud(name)
    paste_cell(img, i, 9)

# icon_buff at row 10 col 0 (next after row 9's 16 items, col stays at 16 but
# the code wraps: col=16 < 32 so it goes to (16, 9). Let me re-check...
# Actually the C++ code: col starts at 0 and increments for each name.
# 16 names in row9Names means col goes 0..15, all in row 9.
# Then the icon_buff check: col=16, 16 < 32, so it stays row 9, col 16.
img = draw_hud("icon_buff")
paste_cell(img, 16, 9)

# --- Effects (rows 12-13) ---
# splash: row 12, cols 0-3 (4 frames)
# slow: row 12, cols 4-5 (2 frames)
# poison: row 12, cols 6-8 (3 frames)
# chain: row 12, cols 9-11 (3 frames)
# shield: row 13, cols 0-1 (2 frames)
# buff_aura: row 13, cols 2-5 (4 frames)
print("  Effects...")
effect_defs = [
    ("splash", 12, 0, 4),
    ("slow", 12, 4, 2),
    ("poison", 12, 6, 3),
    ("chain", 12, 9, 3),
    ("shield", 13, 0, 2),
    ("buff_aura", 13, 2, 4),
]
for name, row, start_col, frame_count in effect_defs:
    for f in range(frame_count):
        random.seed(hash(name) + f)
        img = draw_effect(name, f)
        paste_cell(img, start_col + f, row)

# --- White pixel (cell 31, 31) ---
print("  White pixel...")
white_img, white_draw = cell_img(0, 0)
white_draw.rectangle([0, 0, 63, 63], fill=(255, 255, 255, 255))
paste_cell(white_img, 31, 31)

# --- Save ---
output_path = r"C:\Users\Infocar\AndroidStudioProjects\JayGame\app\src\main\assets\atlas.png"
atlas.save(output_path, "PNG")
print(f"Atlas saved to {output_path}")
print(f"Size: {atlas.size}")
