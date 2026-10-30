"""Validate and preview the same declarative GUI resources consumed by GuiTemplates."""

import argparse
import html
import json
from pathlib import Path
import re
import struct


class Resources:
    def __init__(self, roots):
        self.roots = roots
        self.catalogs = {}
        self.templates = {}
        self.aliases = {}

    @staticmethod
    def key(value):
        result = value if ":" in value else "gt6ce:" + value
        if not re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9/._-]+", result):
            raise ValueError(f"Invalid resource ID: {value}")
        return result

    def path(self, value, prefix="", suffix="", required=True):
        namespace, name = self.key(value).split(":", 1)
        if any(part in ("", "..") for part in (namespace + "/" + name).split("/")):
            raise ValueError(f"Invalid resource path: {value}")
        for root in reversed(self.roots):
            path = root / namespace / (prefix + name + suffix)
            if path.is_file():
                return path
        if required:
            raise ValueError(f"Missing resource: {namespace}:{prefix}{name}{suffix}")
        return None

    def resolve(self, value):
        key = self.key(value)
        visited = set()
        while key not in visited and len(visited) < 16:
            visited.add(key)
            if self.path(key, "gui/templates/", ".json", required=False):
                return key
            namespace, name = key.split(":", 1)
            aliases = self.alias_catalog(namespace)
            if name in aliases:
                key = self.key(aliases[name])
            elif name.startswith("recipes/"):
                key = namespace + ":machines/" + name[8:]
            else:
                return key
        raise ValueError(f"GUI alias cycle/depth: {value}")

    def alias_catalog(self, namespace):
        if namespace not in self.aliases:
            path = self.path(namespace + ":gui/template_aliases.json", required=False)
            self.aliases[namespace] = json.loads(path.read_text()) if path else {}
        return self.aliases[namespace]

    def legacy_keys(self):
        namespaces = {path.parent.parent.name for root in self.roots for path in root.glob("*/gui/template_aliases.json")}
        return sorted(namespace + ":" + key for namespace in namespaces for key in self.alias_catalog(namespace))

    def sprite(self, key):
        namespace, name = self.key(key).split(":", 1)
        if namespace not in self.catalogs:
            self.catalogs[namespace] = json.loads(self.path(namespace + ":gui/sprites.json").read_text())
        entry = self.catalogs[namespace][name]
        w, h = entry["width"], entry["height"]
        if not 0 < w <= 4096 or not 0 < h <= 4096:
            raise ValueError(f"Invalid sprite dimensions: {key}")
        path = self.path(entry["texture"])
        with path.open("rb") as stream:
            header = stream.read(24)
        if header[:8] != b"\x89PNG\r\n\x1a\n":
            raise ValueError(f"Not a PNG: {path}")
        pw, ph = struct.unpack(">II", header[16:24])
        if pw % w or ph % h or pw // w != ph // h:
            raise ValueError(f"PNG must be an integer scale of logical dimensions: {key}")
        return entry

    def load(self, key, chain=()):
        key = self.resolve(key)
        if key in chain or len(chain) >= 16:
            raise ValueError(f"GUI parent cycle/depth: {chain} -> {key}")
        if key in self.templates:
            return self.templates[key]
        data = json.loads(self.path(key, "gui/templates/", ".json").read_text())
        if data.get("format", 1) != 1:
            raise ValueError(f"Unsupported GUI format: {key}")
        result = dict(self.load(data["parent"], chain + (key,))) if "parent" in data else {
            "width": 176, "height": 166, "background": "gt6ce:base/background", "elements": []
        }
        elements = result.get("elements", []) + data.get("elements", [])
        result.update(data)
        result["elements"] = elements
        if not 8 <= result["width"] <= 4096 or not 8 <= result["height"] <= 4096:
            raise ValueError(f"Invalid panel size: {key}")
        bg = self.sprite(result["background"])
        if min(bg["width"], bg["height"]) <= 8:
            raise ValueError(f"Panel sprite too small: {key}")
        total = 0
        for element in elements:
            self.sprite(element["sprite"])
            columns, rows = element.get("columns", 1), element.get("rows", 1)
            total += columns * rows
            if min(columns, rows) <= 0 or total > 4096:
                raise ValueError(f"Invalid GUI repetition: {key}")
            for field in ("x", "y", "step_x", "step_y"):
                if not -4096 <= element.get(field, 0) <= 4096:
                    raise ValueError(f"Invalid coordinate in {key}: {field}")
        if result.get("progress"):
            progress = self.sprite(result["progress"]["sprite"])
            if progress["width"] % 2:
                raise ValueError(f"Progress sprite needs equal halves: {key}")
        self.templates[key] = result
        return result

    def keys(self):
        return sorted({str(path.relative_to(root)).replace("\\", "/").replace("/gui/templates/", ":")[:-5]
                       for root in self.roots for path in root.glob("*/gui/templates/**/*.json")})

    def image(self, sprite):
        from PIL import Image
        entry = self.sprite(sprite)
        with Image.open(self.path(entry["texture"])) as image:
            return image.convert("RGBA").resize((entry["width"], entry["height"]), Image.Resampling.NEAREST)

    def render(self, key, progress=0, machine_slots=None):
        from PIL import Image
        data = self.load(key)
        w, h = data["width"], data["height"]
        image = Image.new("RGBA", (w, h))
        bg = self.image(data["background"])
        sw, sh = bg.size
        for y, dy, dh, sy, bh in [(0, 0, 4, 0, 4), (1, 4, h-8, 4, sh-8), (2, h-4, 4, sh-4, 4)]:
            for x, dx, dw, sx, bw in [(0, 0, 4, 0, 4), (1, 4, w-8, 4, sw-8), (2, w-4, 4, sw-4, 4)]:
                if dw and dh:
                    tile = bg.crop((sx, sy, sx+bw, sy+bh)).resize((dw, dh), Image.Resampling.NEAREST)
                    image.alpha_composite(tile, (dx, dy))
        if data.get("menu_slots") and machine_slots is not None:
            ins, outs, fluid_ins, fluid_outs = machine_slots
            for input_side, count, fluid_count in [(True, ins, fluid_ins), (False, outs, fluid_outs)]:
                for i in range(count):
                    x = 53-i%3*18 if input_side else 107+i%3*18
                    y = (7 if fluid_count > 3 else 25) if count <= 3 else (
                        (7 if fluid_count > 3 else 16)+i//3*18 if count <= 6 else 7+i//3*18)
                    image.alpha_composite(self.image("slot/item"), (x-1, y-1))
                for i in range(fluid_count):
                    x = 53-i%3*18 if input_side else 107+i%3*18
                    image.alpha_composite(self.image("slot/fluid"), (x-1, 62-i//3*18))
            for y in (83, 101, 119, 141):
                for x in range(7, 152, 18):
                    image.alpha_composite(self.image("slot/item"), (x, y))
        for element in data["elements"]:
            sprite = self.image(element["sprite"])
            for row in range(element.get("rows", 1)):
                for column in range(element.get("columns", 1)):
                    image.alpha_composite(sprite, (element.get("x", 0) + column*element.get("step_x", sprite.width),
                                                   element.get("y", 0) + row*element.get("step_y", sprite.height)))
        if data.get("progress"):
            element = data["progress"]
            sprite = self.image(element["sprite"])
            fw = sprite.width // 2
            x, y = element.get("x", 78), element.get("y", 24)
            image.alpha_composite(sprite.crop((0, 0, fw, sprite.height)), (x, y))
            filled = round(fw * max(0, min(1, progress)))
            if filled:
                image.alpha_composite(sprite.crop((fw, 0, fw+filled, sprite.height)), (x, y))
        return image


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--assets", type=Path, action="append", help="assets root; repeat for addon/resource pack overrides")
    parser.add_argument("--template", action="append", help="template ID; omit to validate/render all")
    parser.add_argument("--output", type=Path, help="write previews and index.html; requires Pillow")
    parser.add_argument("--progress", type=float, default=0, help="working fraction, 0..1")
    parser.add_argument("--machine-slots", type=int, nargs=4, metavar=("IN", "OUT", "FLUID_IN", "FLUID_OUT"),
                        help="preview automatic slots using standard recipe-machine Menu coordinates")
    args = parser.parse_args()
    if args.machine_slots and any(n < 0 or n > 9 for n in args.machine_slots):
        parser.error("standard machine slot counts must be 0..9")
    roots = args.assets or [Path(__file__).resolve().parents[2] / "core/src/main/resources/assets"]
    resources = Resources(roots)
    keys = args.template or resources.keys()
    legacy_keys = resources.legacy_keys()
    for key in legacy_keys:
        resources.load(key)
    cards = []
    for key in keys:
        resources.load(key)
        if args.output:
            name = resources.key(key).replace(":", "/") + ".png"
            dest = args.output / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            resources.render(key, args.progress, args.machine_slots).save(dest)
            cards.append(f'<figure><figcaption>{html.escape(key)}</figcaption><img src="{name}"></figure>')
    for namespace, catalog in list(resources.catalogs.items()):
        for sprite in catalog:
            resources.sprite(namespace + ":" + sprite)
    if args.output:
        sprite_cards = []
        for namespace, catalog in resources.catalogs.items():
            for name in sorted(catalog):
                key = namespace + ":" + name
                relative = "sprites/" + namespace + "/" + name + ".png"
                target = args.output / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                sprite = resources.image(key)
                sprite.save(target)
                sprite_cards.append(f'<figure><figcaption>{html.escape(key)} ({sprite.width} × {sprite.height})</figcaption>'
                                    f'<img src="{relative}"></figure>')
        style = ('<style>body{background:#303030;color:white;font:14px sans-serif;display:flex;flex-wrap:wrap;gap:16px}'
                 'figure{margin:0}img{image-rendering:pixelated;zoom:2}figcaption{padding:8px}'
                 'nav{width:100%}a{color:#8fdcff}</style>')
        (args.output / "index.html").write_text('<!doctype html><meta charset="utf-8"><title>GT6CE GUI templates</title>'
            + style + '<nav><a href="sprites.html">Sprite catalog / 碎片图标表</a></nav>' + "\n".join(cards), encoding="utf-8")
        (args.output / "sprites.html").write_text('<!doctype html><meta charset="utf-8"><title>GT6CE GUI sprites</title>'
            + style + '<nav><a href="index.html">Templates / 模板</a></nav>' + "\n".join(sprite_cards), encoding="utf-8")
    print(f"Validated {len(keys)} templates, {len(legacy_keys)} legacy aliases, {len(resources.catalogs)} sprite catalogs.")


if __name__ == "__main__":
    main()
