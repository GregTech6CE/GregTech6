#!/usr/bin/env python3
"""Audit PNG chunks in source trees, processed outputs and release jars (stdlib only, read-only).

The build keeps only ``IHDR``, ``PLTE``, ``tRNS``, ``IDAT`` and ``IEND`` in the delivered
textures. This tool proves that property on processed outputs and release jars, and reports
the same facts for a source tree so ``--baseline`` can record the committed files that still
carry editor metadata.

Usage:
  python tools/resources/check_png_metadata.py path/to/mod.jar --strict
  python tools/resources/check_png_metadata.py core/build/resources/main minecraft/versions/<node>/build/resources/main --strict
  python tools/resources/check_png_metadata.py core/src/main/resources --baseline work/png-baseline.txt

Without ``--strict`` or ``--baseline`` the tool only reports: metadata chunks are
listed, structural damage and CRC errors still fail. ``--baseline`` fails on any
finding the allowlist does not cover, which is how a source tree is audited.
``--strict`` fails on any metadata, trailing byte or CRC error regardless of an
allowlist, which is how a packaged output is audited. Exit code is 0 when nothing
failed, 1 otherwise.
"""

from __future__ import annotations

import argparse
import fnmatch
import struct
import sys
import zlib
from pathlib import Path
from zipfile import BadZipFile, ZipFile

SIGNATURE = b"\x89PNG\r\n\x1a\n"
RETAINED = ("IHDR", "PLTE", "tRNS", "IDAT", "IEND")
RETAINED_BYTES = frozenset(name.encode("ascii") for name in RETAINED)
CHUNK_OVERHEAD = 12
ADAM7 = ((0, 0, 8, 8), (4, 0, 8, 8), (0, 4, 4, 8), (2, 0, 4, 4), (0, 2, 2, 4), (1, 0, 2, 2), (0, 1, 1, 2))
CHANNELS = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}


class PngError(ValueError):
    """Raised when bytes cannot be walked as a PNG chunk stream."""


def walk_chunks(data: bytes):
    """Return ``(chunks, trailing)`` for a PNG stream.

    Each chunk is ``(name, payload, checksum_valid)``. Walking stops at ``IEND``; bytes after
    it are trailing garbage. Structural damage raises :class:`PngError`.
    """
    if not data.startswith(SIGNATURE):
        raise PngError("missing PNG signature")
    position = len(SIGNATURE)
    chunks = []
    while position < len(data):
        if len(data) - position < CHUNK_OVERHEAD:
            raise PngError(f"truncated chunk header at byte {position}")
        length = struct.unpack_from(">I", data, position)[0]
        if length > len(data) - position - CHUNK_OVERHEAD:
            raise PngError(f"chunk at byte {position} exceeds the remaining bytes")
        raw_type = data[position + 4 : position + 8]
        if not all(65 <= value <= 90 or 97 <= value <= 122 for value in raw_type):
            raise PngError(f"invalid chunk type at byte {position}")
        name = raw_type.decode("ascii")
        if not chunks and name != "IHDR":
            raise PngError(f"first chunk is {name}, expected IHDR")
        payload = data[position + 8 : position + 8 + length]
        checksum = struct.unpack_from(">I", data, position + 8 + length)[0]
        position += length + CHUNK_OVERHEAD
        chunks.append((name, payload, zlib.crc32(raw_type + payload) & 0xFFFFFFFF == checksum))
        if name == "IEND":
            return chunks, len(data) - position
    raise PngError("missing IEND chunk")


def analyze(data: bytes) -> dict:
    """Collect chunk findings and the header fields the alpha check needs."""
    report = {
        "error": None,
        "metadata": [],
        "crc": [],
        "trailing": 0,
        "chunks": [],
        "width": 0,
        "height": 0,
        "depth": 0,
        "color": 0,
        "interlace": 0,
        "palette": b"",
        "transparency": b"",
        "pixels": b"",
    }
    try:
        chunks, trailing = walk_chunks(data)
    except PngError as error:
        report["error"] = str(error)
        return report
    report["trailing"] = trailing
    for name, payload, valid in chunks:
        report["chunks"].append(name)
        if not valid:
            report["crc"].append(name)
        if name not in RETAINED:
            report["metadata"].append(name)
        elif name == "IHDR" and len(payload) == 13:
            (report["width"], report["height"], report["depth"], report["color"],
             compression, filtering, report["interlace"]) = struct.unpack(">IIBBBBB", payload)
            if compression != 0 or filtering != 0:
                report["error"] = "unsupported compression or filter method"
        elif name == "PLTE":
            report["palette"] = payload
        elif name == "tRNS":
            report["transparency"] = payload
        elif name == "IDAT":
            report["pixels"] += payload
    if not report["width"] or not report["height"]:
        report["error"] = report["error"] or "invalid image header"
    return report


def _paeth(left: int, above: int, upper_left: int) -> int:
    estimate = left + above - upper_left
    to_left = abs(estimate - left)
    to_above = abs(estimate - above)
    to_upper_left = abs(estimate - upper_left)
    if to_left <= to_above and to_left <= to_upper_left:
        return left
    if to_above <= to_upper_left:
        return above
    return upper_left


def _unfilter(raw: bytes, height: int, stride: int, bpp: int) -> bytes:
    """Reverse the per-scanline PNG filters for one (sub)image."""
    output = bytearray(stride * height)
    previous = bytearray(stride)
    position = 0
    for row in range(height):
        kind = raw[position]
        position += 1
        line = bytearray(raw[position : position + stride])
        position += stride
        if kind == 1:
            for index in range(bpp, stride):
                line[index] = (line[index] + line[index - bpp]) & 0xFF
        elif kind == 2:
            for index in range(stride):
                line[index] = (line[index] + previous[index]) & 0xFF
        elif kind == 3:
            for index in range(stride):
                left = line[index - bpp] if index >= bpp else 0
                line[index] = (line[index] + ((left + previous[index]) >> 1)) & 0xFF
        elif kind == 4:
            for index in range(stride):
                left = line[index - bpp] if index >= bpp else 0
                upper_left = previous[index - bpp] if index >= bpp else 0
                line[index] = (line[index] + _paeth(left, previous[index], upper_left)) & 0xFF
        elif kind != 0:
            raise PngError(f"unknown scanline filter {kind}")
        output[row * stride : (row + 1) * stride] = line
        previous = line
    return bytes(output)


def _samples(scanline: bytes, count: int, depth: int):
    """Yield ``count`` sample values from a scanline of the given bit depth."""
    if depth == 8:
        yield from scanline[:count]
    elif depth == 16:
        for index in range(count):
            yield (scanline[index * 2] << 8) | scanline[index * 2 + 1]
    else:
        per_byte = 8 // depth
        mask = (1 << depth) - 1
        for index in range(count):
            shift = 8 - depth * (index % per_byte + 1)
            yield (scanline[index // per_byte] >> shift) & mask


def _alphas(scanline: bytes, width: int, report: dict):
    """Yield one alpha value per pixel of a scanline."""
    color, depth = report["color"], report["depth"]
    transparency, palette = report["transparency"], report["palette"]
    unit = depth // 8
    if color == 6:
        for index in range(width):
            yield scanline[index * 4 * unit + 3 * unit]
    elif color == 4:
        for index in range(width):
            yield scanline[index * 2 * unit + unit]
    elif color == 3:
        for index in _samples(scanline, width, depth):
            if index < len(transparency):
                yield transparency[index]
            elif index < len(palette) // 3:
                yield 255
            else:
                yield 0
    elif color == 0:
        key = struct.unpack(">H", transparency[:2])[0] if len(transparency) >= 2 else None
        for sample in _samples(scanline, width, depth):
            yield 0 if key is not None and sample == key else 255
    elif color == 2:
        key = struct.unpack(">HHH", transparency[:6]) if len(transparency) >= 6 else None
        values = list(_samples(scanline, width * 3, depth))
        for index in range(width):
            yield 0 if key is not None and tuple(values[index * 3 : index * 3 + 3]) == key else 255


def is_fully_transparent(report: dict) -> bool | None:
    """True when every pixel has zero alpha, False when any pixel is visible, None if unknown."""
    color, depth = report["color"], report["depth"]
    if report["error"] or color not in CHANNELS or not report["pixels"]:
        return None
    if depth not in (1, 2, 4, 8, 16) or (depth < 8 and color not in (0, 3)):
        return None
    try:
        raw = zlib.decompress(report["pixels"])
    except zlib.error:
        return None
    bits = CHANNELS[color] * depth
    bpp = max(1, bits // 8)
    passes = ADAM7 if report["interlace"] else ((0, 0, 1, 1),)
    position = 0
    for x_offset, y_offset, x_step, y_step in passes:
        width = (report["width"] - x_offset + x_step - 1) // x_step
        height = (report["height"] - y_offset + y_step - 1) // y_step
        if width <= 0 or height <= 0:
            continue
        stride = (width * bits + 7) // 8
        span = (stride + 1) * height
        if position + span > len(raw):
            return None
        pixels = _unfilter(raw[position : position + span], height, stride, bpp)
        position += span
        for row in range(height):
            for alpha in _alphas(pixels[row * stride : (row + 1) * stride], width, report):
                if alpha:
                    return False
    return True


def iter_pngs(target: Path):
    """Yield ``(label, bytes)`` for every PNG a directory, file or jar contains."""
    if target.is_dir():
        for file in sorted(target.rglob("*")):
            if file.is_file() and file.suffix.lower() == ".png":
                yield file.relative_to(target).as_posix(), file.read_bytes()
        return
    if target.suffix.lower() == ".jar" or target.suffix.lower() == ".zip":
        try:
            with ZipFile(target) as archive:
                for name in sorted(archive.namelist()):
                    if name.lower().endswith(".png"):
                        yield name, archive.read(name)
        except BadZipFile as error:
            raise SystemExit(f"{target}: {error}") from None
        return
    yield target.name, target.read_bytes()


def load_baseline(path: Path) -> list[str]:
    """Read allowlist patterns, one per line; blank lines and ``#`` comments are ignored."""
    if not path.is_file():
        raise SystemExit(f"缺少基线文件: {path}")
    patterns = []
    for line in path.read_text(encoding="utf-8").splitlines():
        entry = line.split("#", 1)[0].strip()
        if entry:
            patterns.append(entry)
    return patterns


def allowed(label: str, patterns: list[str]) -> bool:
    """Match a path against shell-style patterns; a bare name also matches its last segment."""
    name = label.rsplit("/", 1)[-1]
    return any(fnmatch.fnmatchcase(label, pattern) or fnmatch.fnmatchcase(name, pattern)
               for pattern in patterns)


def audit(target: Path, patterns: list[str] | None, strict: bool, transparent: bool) -> tuple[list[str], list[str], int]:
    """Report the findings of one root; returns ``(failures, blank, tolerated)``."""
    failures = []
    blank = []
    tolerated = 0
    checked = 0
    for label, data in iter_pngs(target):
        checked += 1
        report = analyze(data)
        # A damaged retained chunk cannot be decoded at all, so it always fails: metadata is
        # dropped by the build, but image data, header and palette are shipped as they are.
        broken = []
        if report["error"]:
            broken.append(report["error"])
        retained_crc = [name for name in report["crc"] if name in RETAINED]
        if retained_crc:
            broken.append("bad CRC: " + ", ".join(retained_crc))
        # Dropped chunks and trailing bytes are readable but must be declared for a source tree
        # and must be absent from a packaged output.
        content = []
        if report["metadata"]:
            dropped = [name for name in report["crc"] if name not in RETAINED]
            detail = "metadata chunks: " + ", ".join(report["metadata"])
            if dropped:
                detail += " (bad CRC: " + ", ".join(dropped) + ")"
            content.append(detail)
        if report["trailing"]:
            content.append(f"{report['trailing']} trailing bytes after IEND")
        if transparent and is_fully_transparent(report):
            blank.append(label)
            content.append("fully transparent (no visible pixel)")
        if broken:
            failures.append(f"{label}: {'; '.join(broken)}")
        elif content:
            if strict or (patterns is not None and not allowed(label, patterns)):
                failures.append(f"{label}: {'; '.join(content)}")
            else:
                tolerated += 1
    print(f"{target}: {checked} PNG checked, {len(failures)} finding(s), {tolerated} reported")
    return failures, blank, tolerated


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("roots", nargs="+", type=Path, help="jar, processed resource directory or source directory")
    parser.add_argument("--baseline", type=Path, help="allowlist of path patterns, needed when auditing sources")
    parser.add_argument("--transparent", action="store_true", help="also report images with no visible pixel")
    parser.add_argument("--strict", action="store_true",
                        help="require zero metadata, zero trailing bytes and zero CRC errors; ignores the allowlist")
    args = parser.parse_args()

    if args.strict and args.baseline:
        raise SystemExit("--strict 与 --baseline 互斥：打包产物不允许保留元数据，源码审计才需要基线")
    patterns = load_baseline(args.baseline) if args.baseline else None
    failures = []
    blank = []
    for root in args.roots:
        if not root.exists():
            raise SystemExit(f"路径不存在: {root}")
        root_failures, root_blank, _ = audit(root, patterns, args.strict, args.transparent)
        failures.extend(root_failures)
        blank.extend(root_blank)
    if blank:
        print(f"全透明贴图: {len(blank)}")
        for label in blank[:20]:
            print(f"  {label}")
        if len(blank) > 20:
            print(f"  ... 另有 {len(blank) - 20} 个")
    if failures:
        for failure in failures:
            print(f"错误: {failure}", file=sys.stderr)
        print(f"共 {len(failures)} 项问题", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
