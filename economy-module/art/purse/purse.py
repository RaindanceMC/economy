"""
The shop's coin purse, modelled in Blockbench: a squat oiled-leather pouch gathered into a frilled neck, a gold
drawstring knotted at the front with two tasselled ends, a struck coin riveted on as a badge, a coin showing in the
opening and a little stack of them at its foot.

Built with the adventurers' item pipeline (RaindanceMC/dnd, art/items: the painter, the shape helpers and the
Blockbench client), which must be checked out next to this repository; Blockbench must be running with its MCP
plugin on 127.0.0.1:3000.

    python3 purse.py
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "..", "..", "dnd", "dnd-module", "art", "items"))
import build  # noqa: E402
import paint  # noqa: E402
from models import box, round_y  # noqa: E402

ASSETS = os.path.join(HERE, "..", "..", "src", "main", "resources", "de", "raindancer", "modules", "economy", "pack",
                      "assets", "rainseconomy")
NS = "rainseconomy"

DISPLAY = dict(build.HELD)
DISPLAY["gui"] = {"rotation": [20, -30, 0], "translation": [0, 2.6, 0], "scale": [1.22, 1.22, 1.22]}


def coin_flat(name, cx, cz, y, r=1.25):
    return round_y(name, cx, cz, y, y + 0.32, r, "gold", {"up": "gold", "down": "gold_dark"})


PROFILE = [(0.0, 3.3), (0.45, 4.2), (1.0, 4.7), (1.8, 5.0), (3.4, 5.05), (4.3, 4.75), (5.0, 4.2), (5.6, 3.5),
           (6.2, 2.75), (6.8, 2.15), (7.6, 1.95)]


def radius(y):
    for (y0, r0), (y1, r1) in zip(PROFILE, PROFILE[1:]):
        if y0 <= y <= y1:
            return r0 + (r1 - r0) * (y - y0) / (y1 - y0)
    return PROFILE[-1][1]


def leather_at(y, name):
    """A slice's own leather: lit from above, so lighter high on the belly and darker towards the foot."""
    light, dark = (204, 128, 70), (118, 64, 33)
    t = max(0.0, min(1.0, (5.2 - y) / 5.2)) ** 1.3
    colour = tuple(round(light[c] + (dark[c] - light[c]) * t) for c in range(3))
    paint.MATERIALS[name] = {"colour": "#%02x%02x%02x" % colour, "kind": "smooth"}
    return name


def purse():
    parts = []
    # the pouch: thin slices along a soft profile, so the belly reads round rather than stepped
    y, i = 0.0, 0
    while y < 7.59:
        top = min(7.6, y + 0.45)
        material = leather_at(min((y + top) / 2, 6.2), "pouch_leather_%02d" % i)
        parts += round_y("pouch_%02d" % i, 8, 8, y, top, radius((y + top) / 2), material)
        y, i = top, i + 1
    # the drawstring wound round the waist, knotted at the front; its ends hang forward over the belly
    parts += round_y("cord", 8, 8, 6.95, 7.45, 2.3, "gold")
    parts.append(box("knot", (7.4, 6.75, 10.05), (8.6, 7.65, 10.75), "gold_dark"))
    for n, (x, length) in enumerate(((7.5, 3.9), (8.5, 3.4))):
        parts.append(box("tail_%d" % n, (x - 0.18, 6.95, 10.4), (x + 0.18, 7.3, 10.4 + length), "gold", None,
                         {"angle": 40, "axis": "x", "origin": [x, 7.1, 10.45]}))
        end_y = 7.1 - length * 0.643
        end_z = 10.45 + length * 0.766
        parts.append(box("tassel_cap_%d" % n, (x - 0.42, end_y - 0.35, end_z - 0.3), (x + 0.42, end_y + 0.05,
                         end_z + 0.3), "gold_dark"))
        parts.append(box("tassel_%d" % n, (x - 0.36, end_y - 1.45, end_z - 0.25), (x + 0.36, end_y - 0.3,
                         end_z + 0.25), "gold"))
    # the gathered neck flaring into a frill, open at the top
    parts += round_y("neck", 8, 8, 7.6, 8.4, 1.7, "leather")
    parts += round_y("frill", 8, 8, 8.4, 9.0, 2.15, "leather_light", {"up": "leather_dark"})
    parts += round_y("frill_lip", 8, 8, 9.0, 9.6, 2.55, "leather_light", {"up": "leather_dark"})
    # a coin standing in the opening
    parts.append(box("coin_in", (7.1, 8.9, 7.75), (9.3, 11.0, 8.15), "gold", {"north": "gold", "south": "gold"},
                     {"angle": 14, "axis": "z", "origin": [8.2, 10, 7.95]}))
    # a little stack of coins beside it
    parts += coin_flat("stack_0", 14.0, 9.6, 0)
    parts += coin_flat("stack_1", 14.15, 9.45, 0.32)
    parts += coin_flat("stack_2", 13.9, 9.7, 0.64)
    parts += coin_flat("stack_3", 14.05, 9.55, 0.96)
    return parts


def view(name, position, target=(0, 4.5, 0), fov=34):
    if "preview" not in json.dumps(build.bb.call("tools/call", {"name": "list_views", "arguments": {}})):
        build.bb.call("tools/call", {"name": "create_offscreen_view",
                                     "arguments": {"id": "preview", "width": 640, "height": 640, "antialias": True}})
    for result in build.bb.call("tools/call", {"name": "set_camera_angle", "arguments": {
            "view": "preview", "position": list(position), "target": list(target), "projection": "perspective",
            "fov": fov}}):
        for part in result.get("result", {}).get("content", []):
            if part.get("type") == "image":
                import base64
                open(os.path.join(HERE, name + ".png"), "wb").write(base64.b64decode(part["data"]))


def main():
    parts = purse()
    faces = build.faces_of(parts)
    first, spots, size = paint.paint(faces)
    elements = []
    index = 0
    for part in parts:
        uv = {}
        for side in build.SIDES:
            face = faces[index]
            x, y = spots[index]
            uv[side] = [x, y, x + face.w, y + face.h]
            index += 1
        rotation, origin = [0, 0, 0], [8, 8, 8]
        if part["rotation"]:
            rotation["xyz".index(part["rotation"]["axis"])] = part["rotation"]["angle"]
            origin = part["rotation"]["origin"]
        elements.append({"name": part["name"], "from": part["from"], "to": part["to"], "uv": uv,
                         "rotation": rotation, "origin": origin})
    built = build.eval_js(build.BUILD.replace("SPEC", json.dumps({
        "name": "purse", "size": size, "texture": build.data_url(first), "elements": elements, "display": DISPLAY})))
    open(os.path.join(HERE, "purse.bbmodel"), "w").write(built["project"])

    texture = NS + ":item/purse"
    model = built["model"]
    model["textures"] = {"0": texture, "particle": texture}
    for element in model.get("elements", []):
        for face in element.get("faces", {}).values():
            face["texture"] = "#0"
    os.makedirs(os.path.join(ASSETS, "models", "item"), exist_ok=True)
    with open(os.path.join(ASSETS, "models", "item", "purse.json"), "w") as out:
        json.dump(model, out, indent=1)
    first.save(os.path.join(ASSETS, "textures", "item", "purse.png"))

    view("purse", (16, 12, 20))
    view("purse-front", (0, 8, 23))
    print("purse:", len(parts), "parts, texture", size)


if __name__ == "__main__":
    main()
