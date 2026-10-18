#!/usr/bin/env python3
"""Give the port's coins their item icons, shared models and translations.

The coins themselves are ordinary material items (``MaterialPrefix.coin``,
``gregtech:coin_<material>``), so they use the port's shared material models
(``models/item/material/<set>/coin.json``, tinted by the material's colour through
``client/MaterialClientModels``). GT6 draws its coins with ``Textures.BlockIcons.COIN``
(``gregapi/old/Textures.java:40``) - the very icon the flat coin renderer puts on a coin's top and
bottom (``MultiTileEntityCoin.java:430-435``) - so every texture set of the port gets a copy of that
icon, exactly as GT6 colours one icon per material instead of shipping one per material.

GT6 has no coin *overlay* art, and the port's shared models carry a second, untinted layer
(``MaterialIcons.sharedModelJson``), so each set also gets a fully transparent ``coin_overlay.png``.

Run from the repository root:  python tools/add_coin_assets.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import hashlib
import json
import shutil
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = native_path(ROOT / "src/main/resources/assets/gregtech")
ORIGINAL = ROOT.parent / "gregtech6-master/gregtech6-master"
MANIFEST = ROOT / "tools/gt6_texture_sources.json"
ICONS = ASSETS / "textures/item/material_icons"
MODELS = ASSETS / "models/item/material"
SOURCE_ICON = str(native_path("src/main/resources/assets/gregtech/textures/blocks/iconsets/COIN.png"))

# (english, chinese) - the two names every registered gregtech item must have in both files
# (see gametest/MaterialCompatibilityTests.registeredNamesHaveTranslations). ``tab_icon_coin`` is the
# creative tab's icon item, which GTCreativeTabIcons registers per prefix and which that guard reads
# through Item.getDescriptionId like any other item.
LANG_KEYS = {
    "item.gregtech.coin": ("%s Coin", "%s硬币"),
    "itemGroup.gregtech.coin": ("GregTech Coins", "格雷科技·硬币"),
    "item.gregtech.tab_icon_coin": ("Tab Icon Coin", "硬币（分类图标）"),
}


def transparent_png(size: int) -> bytes:
    """A fully transparent RGBA PNG - the port's stand-in for GT6's missing coin overlay."""
    raw = b"".join(b"\x00" + b"\x00" * (size * 4) for _ in range(size))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    header = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header)
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def texture_sets() -> list[str]:
    return sorted(entry.name for entry in ICONS.iterdir() if entry.is_dir())


def main() -> None:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8")) if MANIFEST.exists() else {}
    source = ORIGINAL / SOURCE_ICON
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    overlay = transparent_png(16)

    sets = texture_sets()
    for entry in sets:
        coin = ICONS / entry / "coin.png"
        shutil.copyfile(source, coin)
        manifest[coin.relative_to(ASSETS).as_posix()] = {"source": SOURCE_ICON, "sha256": digest}
        (ICONS / entry / "coin_overlay.png").write_bytes(overlay)
        (MODELS / entry / "coin.json").parent.mkdir(parents=True, exist_ok=True)
        (MODELS / entry / "coin.json").write_text(
            '{"parent": "gregtech:item/coin_minted"}\n', encoding="utf-8")

    from generate_minted_coin_assets import generate
    generate()


    for language, index in (("en_us", 0), ("zh_cn", 1)):
        path = ASSETS / f"lang/{language}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        for key, names in LANG_KEYS.items():
            data[key] = names[index]
        path.write_text(json.dumps(dict(sorted(data.items())), indent=2, ensure_ascii=False) + "\n",
                        encoding="utf-8")

    MANIFEST.write_text(json.dumps(dict(sorted(manifest.items())), indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {len(sets) * 2} textures, {len(sets)} models and {len(LANG_KEYS)} x 2 lang keys")


if __name__ == "__main__":
    main()
