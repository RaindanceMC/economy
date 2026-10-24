"""
The shop's window as a merchant's stall: a striped awning that hangs over the top of the window, a beam with the
title, the wares on oak shelves, the navigation on the counter; and the coin
purse the balance is shown on. One painting per window height, the font that draws them, the purse's model.

Uses Core's menu canvas (RainsCore/art/menus, checked out next to this repository).

    python3 stall.py
"""
import json
import math
import os
import random
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "..", "..", "..", "RainsCore", "art", "menus"))
from canvas import (Canvas, chest_well, inventory_well, hotbar_well, label_y, height, wood, rivet, well, mix,
                    shade, vignette)

PACK = os.path.join(HERE, "..", "..", "src", "main", "resources", "de", "raindancer", "modules", "economy", "pack")
NS = "rainseconomy"

OVER = 20          # how far the awning rises above the window
SIDE = 4           # and overhangs it either side
W = 176 + 2 * SIDE

BRASS, BRASS_D, BRASS_L = (212, 166, 70), (120, 82, 30), (255, 228, 150)
RED, RED_D, RED_L = (176, 38, 42), (104, 18, 24), (222, 82, 76)
CREAM, CREAM_D = (238, 226, 198), (196, 178, 140)
WALNUT, WALNUT_D, WALNUT_L = (92, 56, 32), (44, 25, 14), (146, 98, 58)
OAK, OAK_D, OAK_L = (168, 120, 72), (112, 74, 40), (214, 168, 112)
INK = (26, 14, 7)


def stall(rows):
    h = height(rows) + OVER
    c = Canvas(W, h, seed=rows)
    ox, oy = SIDE, OVER  # the window's corner inside the painting

    # ---- the stall's frame: walnut posts and boards behind everything
    wood(c, ox, oy, ox + 176, h, WALNUT, WALNUT_D, WALNUT_L, seed=7 + rows, plank=None)
    vignette(c, ox, oy, ox + 176, h, WALNUT_D, 0.4, 6)

    # ---- the beam the title is painted on
    for y in range(oy + 2, oy + 16):
        for x in range(ox + 2, ox + 174):
            c.tint(x, y, INK, 0.28 + 0.05 * math.sin(x * 0.3))
    c.hline(ox + 2, ox + 174, oy + 16, WALNUT_L, 120)

    # ---- shelves of wares
    def shelf(y0, y1, seed):
        wood(c, ox + 6, y0, ox + 170, y1, OAK, OAK_D, OAK_L, seed=seed)
        c.outline(ox + 5, y0 - 1, ox + 171, y1 + 1, INK, corner=False)

    chest_bottom = oy + 17 + 18 * rows
    shelf(oy + 16, chest_bottom + 1, 30 + rows)
    for r in range(rows):
        counter = r == rows - 1 and rows > 1
        for col in range(9):
            x, y = chest_well(r, col)
            x, y = x + ox, y + oy
            if counter:
                well(c, x, y, (118, 80, 48), (44, 26, 14), (176, 130, 80), 0.28, show=0.3)
            else:
                well(c, x, y, (196, 160, 112), (104, 68, 38), (236, 210, 166), 0.24, show=0.3)
        if not counter:
            ly = oy + 17 + 18 * r + 17
            c.hline(ox + 6, ox + 170, ly, mix(WALNUT_D, INK, 0.5), 110)

    # ---- the counter: a polished top edge with a brass rail and coins left on it
    if rows > 1:
        cy = oy + 17 + 18 * (rows - 1)
        c.hline(ox + 3, ox + 173, cy - 2, WALNUT_L)
        c.hline(ox + 3, ox + 173, cy - 1, mix(BRASS, BRASS_L, 0.4))
        c.hline(ox + 3, ox + 173, cy, BRASS_D)
        for x in range(ox + 8, ox + 172, 24):
            rivet(c, x, cy - 1, BRASS, BRASS_D, BRASS_L)
    # coins stacked at the counter's ends, outside the slots
    cyb = oy + 17 + 18 * rows - 6
    for (sx, n) in ((ox + 1, 4), (ox + 171, 3)):
        for i in range(n):
            yy = cyb - i * 2
            for dx in range(4):
                c.put(sx + dx, yy, mix(BRASS, BRASS_L, 0.5) if dx in (1, 2) else BRASS)
                c.put(sx + dx, yy + 1, BRASS_D)

    # ---- the player's side: the same shelf, the hotbar on its own board
    inv_top = oy + 30 + 18 * rows
    hot_top = oy + 88 + 18 * rows
    shelf(inv_top - 1, hot_top + 19, 60 + rows)
    for r in range(3):
        for col in range(9):
            x, y = inventory_well(rows, r, col)
            well(c, x + ox, y + oy, (196, 160, 112), (104, 68, 38), (236, 210, 166), 0.24, show=0.3)
    c.hline(ox + 6, ox + 170, hot_top - 2, INK)
    c.hline(ox + 6, ox + 170, hot_top - 1, OAK_L, 120)
    for col in range(9):
        x, y = hotbar_well(rows, col)
        well(c, x + ox, y + oy, (196, 160, 112), (104, 68, 38), (236, 210, 166), 0.24, show=0.3)

    # ---- the chalkboard the game writes "Inventory" on is too dark for its grey: a paper price list instead
    ly = oy + label_y(rows)
    for y in range(ly - 2, ly + 9):
        for x in range(ox + 6, ox + 60):
            c.put(x, y, mix(CREAM, CREAM_D, c.rng.random() * 0.3))
    c.outline(ox + 5, ly - 3, ox + 61, ly + 10, INK)
    # a pin at each end
    for px in (ox + 7, ox + 58):
        c.put(px, ly - 1, RED_L)
        c.put(px, ly, RED_D)

    # ---- the awning, over everything above the beam
    awning(c, rows)

    # posts down both sides
    for x0 in (0, W - 4):
        for y in range(OVER - 2, h - 2):
            for x in range(x0, x0 + 4):
                f = (0.8, 1.05, 1.0, 0.75)[x - x0]
                c.put(x, y, shade(mix(WALNUT, WALNUT_L, 0.25), f))
        c.vline(x0 - 1 if x0 else 4, OVER - 2, h - 2, INK)
    for y in range(h - 3, h):
        for x in range(0, W):
            c.put(x, y, shade(WALNUT_D, 0.9))
    c.hline(0, W, h - 3, WALNUT_L, 100)
    return c


def awning(c, rows):
    """Red and cream stripes bellying in folds, a deep scalloped hem with gold braid, rolled over a pole."""
    stripe = 10
    top, hem = 4, OVER - 2
    for y in range(top, hem + 6):
        for x in range(0, W):
            band = ((x + 3) // stripe) % 2
            u = ((x + 3) % stripe + 0.5) / stripe
            belly = math.sin(u * math.pi)
            depth = (y - top) / (hem - top)
            base, dark, light = ((RED, RED_D, RED_L), (CREAM, CREAM_D, (255, 250, 236)))[band]
            col = mix(base, dark, 0.1 + depth * 0.3)
            col = mix(col, light, max(0, belly - 0.6) * 0.9 * (1 - depth * 0.5))
            col = mix(col, dark, max(0, 0.35 - belly) * 1.2)
            limit = hem + 4.5 * math.sqrt(max(0.0, 1 - (2 * u - 1) ** 2))
            if y > limit:
                continue
            if y > limit - 1.2:
                col = (226, 176, 64) if (x + y) % 3 else (150, 104, 30)  # braided gold hem
            elif y > limit - 2.4:
                col = shade(col, 0.7)
            c.put(x, y, col)
    # the awning's shadow on the beam and the posts
    for x in range(0, W):
        u = ((x + 3) % stripe + 0.5) / stripe
        lim = int(hem + 4.5 * math.sqrt(max(0.0, 1 - (2 * u - 1) ** 2)))
        for i in range(1, 4):
            c.tint(x, lim + i, INK, 0.45 * (1 - (i - 1) / 3))
    # the rolled cloth over its pole, with brass finials
    for x in range(0, W):
        for y, f in ((0, 0.62), (1, 0.95), (2, 1.15), (3, 1.0), (4, 0.7)):
            c.put(x, y, shade(mix(RED, RED_D, 0.15), f))
        if x % 5 == 0:
            c.put(x, 2, shade(RED, 0.85))
    for x0 in (0, W - 4):
        for y in range(0, 5):
            for x in range(x0, x0 + 4):
                c.put(x, y, shade(BRASS, (1.2, 1.05, 0.9, 0.7)[x - x0] * (0.85 if y in (0, 4) else 1)))
        c.put(x0 + 1, 1, BRASS_L)


def purse():
    """A leather coin purse cinched with a gold cord, the neck flared open on a glint of coins — 32px."""
    S = 32
    c = Canvas(S, S, seed=3)
    LEATHER, LEATHER_D, LEATHER_L = (150, 88, 46), (64, 34, 18), (210, 146, 88)
    GOLD, GOLD_D, GOLD_L = (232, 182, 64), (140, 92, 26), (255, 238, 150)
    cx = 15.5

    def half(y):  # the sack's half-width: narrow at the cinch, full and slightly flattened at the bottom
        t = (y - 12) / 17
        return 4 + 8.5 * math.sin(min(1.0, t * 1.15) * math.pi / 2) ** 0.7 - max(0, t - 0.88) * 18

    for y in range(12, 30):
        w = half(y)
        for x in range(S):
            dx = (x + 0.5 - cx) / max(w, 0.1)
            if abs(dx) > 1:
                continue
            fold = math.sin(dx * math.pi * 2.2 + 0.4) * (1 - (y - 12) / 22)
            light = 0.55 - dx * 0.45 - (y - 12) / 30 + fold * 0.18
            col = mix(LEATHER_D, LEATHER, min(1, max(0, light + 0.45)))
            col = mix(col, LEATHER_L, max(0, light - 0.35))
            if abs(dx) > 0.86 or y >= 29:
                col = mix(col, LEATHER_D, 0.5)
            c.put(x, y, col)
    # a stitched seam down the front
    for y in range(15, 28, 2):
        c.put(int(cx + 3 + (y - 15) * 0.12), y, LEATHER_L)
    # the flared neck above the cord, its top edge ruffled
    for y in range(5, 12):
        w = 3.2 + (11 - y) * 0.75
        for x in range(S):
            dx = (x + 0.5 - cx) / w
            if abs(dx) > 1:
                continue
            col = mix(LEATHER_L, LEATHER_D, (dx + 1) / 2 * 0.8)
            if (x + (y // 2)) % 3 == 0:
                col = shade(col, 0.82)
            c.put(x, y, col)
    for x in range(S):
        if c.px[x, 5][3] and x % 2 == 0:
            c.clear(x, 5)
    # the opening, and the coins in it
    for x in range(int(cx - 4), int(cx + 5)):
        c.put(x, 6, (40, 20, 10))
        c.put(x, 7, (60, 32, 16))
    for x, y in ((13, 6), (15, 6), (17, 6), (14, 5), (16, 5)):
        c.put(x, y, GOLD if (x + y) % 2 else GOLD_L)
    c.put(15, 4, GOLD_L)
    # the gold cord, knotted, with two tassels
    for x in range(int(cx - 5), int(cx + 6)):
        c.put(x, 11, GOLD_L if x < cx else GOLD)
        c.put(x, 12, GOLD_D)
    for (tx, ty, col) in ((10, 12, GOLD), (9, 13, GOLD), (9, 14, GOLD_L), (8, 15, GOLD), (8, 16, GOLD_D),
                          (11, 13, GOLD), (11, 14, GOLD), (11, 15, GOLD_D), (10, 11, GOLD_L)):
        c.put(tx, ty, col)
    src = c.img.copy()
    p = src.load()
    for y in range(S):
        for x in range(S):
            if p[x, y][3] == 0 and any(0 <= x + a < S and 0 <= y + b < S and p[x + a, y + b][3]
                                       for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                c.put(x, y, (36, 18, 8))
    return c


def write():
    files = []

    def out(rel, data=None, image=None):
        path = os.path.join(PACK, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        if image is not None:
            image.save(path)
        else:
            with open(path, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=1)
        files.append(rel)

    providers = []
    previews = []
    for rows in range(1, 7):
        c = stall(rows)
        out(f"assets/{NS}/textures/font/stall/stall_{rows}.png", image=c.img)
        providers.append({"type": "bitmap", "file": f"{NS}:font/stall/stall_{rows}.png", "ascent": 13 + OVER,
                          "height": c.h, "chars": [chr(0xE000 + rows)]})
        previews.append(c.img)
    # from the title's start back to the painting's edge, and from its far end back to the title's start
    providers.append({"type": "space", "advances": {"": -(8 + SIDE), "": -(W + 1 - 8 - SIDE)}})
    out(f"assets/{NS}/font/stall.json", {"providers": providers})

    out(f"assets/{NS}/items/purse.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/purse"}})
    out(f"assets/{NS}/models/item/purse.json", {"parent": "minecraft:item/generated",
                                                "textures": {"layer0": f"{NS}:item/purse"}})
    out(f"assets/{NS}/textures/item/purse.png", image=purse().img)

    with open(os.path.join(HERE, "files.txt"), "w") as f:
        f.write("\n".join(files) + "\n")
    sheet = Image.new("RGBA", (sum(i.width + 8 for i in previews), max(i.height for i in previews)), (32, 32, 36, 255))
    x = 0
    for i in previews:
        sheet.alpha_composite(i, (x, 0))
        x += i.width + 8
    sheet.resize((sheet.width * 2, sheet.height * 2), Image.NEAREST).save(os.path.join(HERE, "stall.png"))
    purse().img.resize((256, 256), Image.NEAREST).save(os.path.join(HERE, "purse.png"))
    print(len(files), "files")


if __name__ == "__main__":
    write()
