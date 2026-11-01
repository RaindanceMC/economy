"""
The coin: a chunky voxel gold piece, a raised stamp on both faces and a reeded edge. Writes the model, its item
definition and its texture into the economy pack; `--render` also photographs it in the running Blockbench.
"""
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HERE, "..", "..", "economy-module", "src", "main", "resources", "de", "raindancer", "modules",
                    "economy", "pack", "assets", "rainseconomy")

DARK, MID, LIGHT, SHINE, DEEP = (150, 98, 22), (226, 172, 44), (250, 214, 96), (255, 244, 186), (110, 70, 14)
THICK = (7.25, 8.75)
STAMP = 0.4

# The disc's outline as rows that do not overlap (overlapping faces would flicker): (y0, y1, x0, x1).
DISC = [(2, 3, 5, 11), (3, 5, 3, 13), (5, 11, 2, 14), (11, 13, 3, 13), (13, 14, 5, 11)]
BOSS = [(5, 6, 7, 9), (6, 7, 6, 10), (7, 9, 5, 11), (9, 10, 6, 10), (10, 11, 7, 9)]


def inside(rows, x, y):
    return any(y0 <= y < y1 and x0 <= x < x1 for y0, y1, x0, x1 in rows)


def texture():
    """32×32: the face at 0,0, the stamp at 16,0, the reeded edge along the bottom half; one pixel per unit."""
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = im.load()
    for y in range(16):
        for x in range(16):
            # texture rows run top down, model y bottom up
            my = 15 - y
            if inside(DISC, x, my):
                rim = any(not inside(DISC, x + dx, my + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                second = any(not inside(DISC, x + dx, my + dy) for dx, dy in ((2, 0), (-2, 0), (0, 2), (0, -2)))
                if rim:
                    px[x, y] = LIGHT if x + y < 16 else DARK
                elif second:
                    px[x, y] = DARK if x + y < 16 else LIGHT
                else:
                    px[x, y] = SHINE if (x - y) in (-1, 0) and x < 8 else MID
            if inside(BOSS, x, my):
                edge = any(not inside(BOSS, x + dx, my + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                centre = 7 <= x <= 8 and 7 <= my <= 8
                px[16 + x, y] = DEEP if centre else (LIGHT if not edge else (SHINE if x + y < 16 else DARK))
    for y in range(16, 32):
        for x in range(32):
            px[x, y] = LIGHT if x % 2 == 0 else DARK
    return im


def face_uv(x0, y0, x1, y1, offset=0):
    return [offset + x0 / 2, (16 - y1) / 2, offset + x1 / 2, (16 - y0) / 2]


def slab(name, row, z0, z1, offset):
    y0, y1, x0, x1 = row
    edge_v = [x0 / 2, 8, x1 / 2, 9]
    edge_h = [0, 8, (y1 - y0) / 2, 9]
    return {
        "name": name, "from": [x0, y0, z0], "to": [x1, y1, z1],
        "faces": {
            "north": {"uv": face_uv(x0, y0, x1, y1, offset), "texture": "#0"},
            "south": {"uv": face_uv(16 - x1, y0, 16 - x0, y1, offset), "texture": "#0"},
            "east": {"uv": edge_h, "texture": "#0"},
            "west": {"uv": edge_h, "texture": "#0"},
            "up": {"uv": edge_v, "texture": "#0"},
            "down": {"uv": edge_v, "texture": "#0"},
        },
    }


def model():
    elements = [slab("disc_%d" % n, row, THICK[0], THICK[1], 0) for n, row in enumerate(DISC)]
    elements += [slab("stamp_front_%d" % n, row, THICK[0] - STAMP, THICK[0], 8) for n, row in enumerate(BOSS)]
    elements += [slab("stamp_back_%d" % n, row, THICK[1], THICK[1] + STAMP, 8) for n, row in enumerate(BOSS)]
    return {
        "credit": "RainsEconomy",
        "texture_size": [32, 32],
        "textures": {"0": "rainseconomy:item/coin", "particle": "rainseconomy:item/coin"},
        "elements": elements,
        "display": {
            "gui": {"rotation": [18, -32, 0], "scale": [1.0, 1.0, 1.0]},
            "ground": {"translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.9, 0.9, 0.9]},
            "head": {"translation": [0, 13, 0]},
            "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 2.5, 0.5], "scale": [0.45] * 3},
            "thirdperson_lefthand": {"rotation": [0, -90, 0], "translation": [0, 2.5, 0.5], "scale": [0.45] * 3},
            "firstperson_righthand": {"rotation": [0, -30, 8], "translation": [1.2, 3.2, 1], "scale": [0.42] * 3},
            "firstperson_lefthand": {"rotation": [0, 30, -8], "translation": [1.2, 3.2, 1], "scale": [0.42] * 3},
        },
    }


def write():
    os.makedirs(os.path.join(PACK, "textures", "item"), exist_ok=True)
    texture().save(os.path.join(PACK, "textures", "item", "coin.png"))
    with open(os.path.join(PACK, "models", "item", "coin.json"), "w") as out:
        json.dump(model(), out, indent=1)
    with open(os.path.join(PACK, "items", "coin.json"), "w") as out:
        json.dump({"model": {"type": "minecraft:model", "model": "rainseconomy:item/coin"}}, out, indent=1)


if __name__ == "__main__":
    write()
    print("written")


def render(path, position=(14, 14, -22), target=(8, 8, 8)):
    """A photograph from the running Blockbench (the shared client in ~/.claude/tools/bbmcp)."""
    import base64
    import io
    sys.path.insert(0, os.path.expanduser("~/.claude/tools/bbmcp"))
    import bb
    buffer = io.BytesIO()
    texture().save(buffer, "PNG")
    url = "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode()
    bb.eval_js("""(() => {
      newProject(Formats.java_block);
      Codecs.java_block.parse(MODEL, '');
      const tex = new Texture({name: 'coin'}).fromDataURL('PICTURE').add(false);
      Cube.all.forEach(c => { for (const f in c.faces) c.faces[f].texture = tex.uuid; });
      Canvas.updateAll();
      return Cube.all.length;
    })()""".replace("MODEL", json.dumps(model())).replace("PICTURE", url))
    views = json.dumps(bb.call("tools/call", {"name": "list_views", "arguments": {}}))
    if "coin" not in views:
        bb.call("tools/call", {"name": "create_offscreen_view",
                               "arguments": {"id": "coin", "width": 480, "height": 480, "antialias": True}})
    for message in bb.call("tools/call", {"name": "set_camera_angle", "arguments": {
            "view": "coin", "position": list(position), "target": list(target), "projection": "perspective",
            "fov": 40}}):
        for part in message.get("result", {}).get("content", []):
            if part.get("type") == "image":
                with open(path, "wb") as out:
                    out.write(base64.b64decode(part["data"]))
                return path
    raise RuntimeError("no image from Blockbench")
