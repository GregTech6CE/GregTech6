#!/usr/bin/env python3
"""Scan Minecraft Anvil region files and count gregtech block palette usage.

Verifies worldgen wiring: if GT ore veins / small ores / stone layers ran during
chunk generation, the chunk section palettes contain gt6ce:ore_* /
gt6ce:stone_* entries. Counts actual placed blocks by decoding the packed
block-state indices.

Usage: python scan_region_ores.py <world>/region [more region dirs...]
"""

import collections
import os
import struct
import sys
import zlib

TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG = 0, 1, 2, 3, 4
TAG_FLOAT, TAG_DOUBLE, TAG_BYTE_ARRAY, TAG_STRING = 5, 6, 7, 8
TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY, TAG_LONG_ARRAY = 9, 10, 11, 12


class Reader:
    __slots__ = ("buf", "pos")

    def __init__(self, buf):
        self.buf = buf
        self.pos = 0

    def take(self, n):
        b = self.buf[self.pos:self.pos + n]
        self.pos += n
        return b

    def u8(self):
        v = self.buf[self.pos]
        self.pos += 1
        return v

    def u16(self):
        return struct.unpack(">H", self.take(2))[0]

    def i32(self):
        return struct.unpack(">i", self.take(4))[0]

    def string(self):
        return self.take(self.u16()).decode("utf-8", "replace")


def skip_payload(r, tag):
    # NOTE: augmented assignment `r.pos += r.u16()` reads the OLD pos before the
    # call advances it, silently dropping the length-prefix bytes — use temps.
    if tag == TAG_BYTE: r.pos += 1
    elif tag == TAG_SHORT: r.pos += 2
    elif tag in (TAG_INT, TAG_FLOAT): r.pos += 4
    elif tag in (TAG_LONG, TAG_DOUBLE): r.pos += 8
    elif tag == TAG_BYTE_ARRAY:
        n = r.i32(); r.pos += n
    elif tag == TAG_STRING:
        n = r.u16(); r.pos += n
    elif tag == TAG_INT_ARRAY:
        n = r.i32(); r.pos += 4 * n
    elif tag == TAG_LONG_ARRAY:
        n = r.i32(); r.pos += 8 * n
    elif tag == TAG_LIST:
        item, n = r.u8(), r.i32()
        for _ in range(n):
            skip_payload(r, item)
    elif tag == TAG_COMPOUND:
        while True:
            child = r.u8()
            if child == TAG_END:
                return
            r.string()
            skip_payload(r, child)


def parse_compound(r, keep):
    """Parse a compound, keeping only keys in `keep` (dict name->handler)."""
    out = {}
    while True:
        tag = r.u8()
        if tag == TAG_END:
            return out
        name = r.string()
        if name in keep and keep[name][0] == tag:
            out[name] = keep[name][1](r)
        else:
            skip_payload(r, tag)


def parse_palette_entry(r):
    return parse_compound(r, {"Name": (TAG_STRING, lambda rr: rr.string())})


def parse_list(r, item_parser):
    item, n = r.u8(), r.i32()
    return [item_parser(r) for _ in range(n)]


def parse_block_states(r):
    return parse_compound(r, {
        "palette": (TAG_LIST, lambda rr: parse_list(rr, parse_palette_entry)),
        "data": (TAG_LONG_ARRAY, lambda rr: list(struct.unpack(f">{rr.i32()}q", rr.take(0))) if False else read_longs(rr)),
    })


def read_longs(r):
    n = r.i32()
    return struct.unpack(f">{n}q", r.take(8 * n))


def parse_section(r):
    return parse_compound(r, {"block_states": (TAG_COMPOUND, parse_block_states)})


def parse_chunk(r):
    return parse_compound(r, {"sections": (TAG_LIST, lambda rr: parse_list(rr, parse_section))})


def count_blocks(section, counter):
    bs = section.get("block_states")
    if not bs or "palette" not in bs:
        return
    palette = [e.get("Name", "") for e in bs["palette"]]
    gt = {i: name for i, name in enumerate(palette) if name.startswith("gt6ce:")}
    if not gt:
        return
    data = bs.get("data")
    if data is None:
        if 0 in gt:
            counter[gt[0]] += 4096
        return
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    mask = (1 << bits) - 1
    idx = 0
    for value in data:
        v = value & 0xFFFFFFFFFFFFFFFF
        for _ in range(per_long):
            if idx >= 4096:
                break
            p = v & mask
            if p in gt:
                counter[gt[p]] += 1
            v >>= bits
            idx += 1


def scan_region(path, counter, chunk_presence=None):
    with open(path, "rb") as f:
        header = f.read(4096)
        if len(header) < 4096:
            return 0
        chunks = 0
        for i in range(1024):
            off = struct.unpack(">I", b"\0" + header[i * 4:i * 4 + 3])[0]
            sectors = header[i * 4 + 3]
            if off == 0 or sectors == 0:
                continue
            f.seek(off * 4096)
            (length,) = struct.unpack(">I", f.read(4))
            comp = f.read(1)[0]
            raw = f.read(length - 1)
            if comp == 2:
                raw = zlib.decompress(raw)
            elif comp == 1:
                import gzip
                raw = gzip.decompress(raw)
            else:
                continue
            try:
                r = Reader(raw)
                r.u8()      # root compound tag
                r.string()  # root name
                chunk = parse_chunk(r)
                per_chunk = collections.Counter()
                for section in chunk.get("sections", []):
                    count_blocks(section, per_chunk)
                counter.update(per_chunk)
                if chunk_presence is not None:
                    for name in per_chunk:
                        chunk_presence[name] += 1
                chunks += 1
            except Exception:
                global FAILED
                FAILED += 1
        return chunks


FAILED = 0


def main():
    counter = collections.Counter()
    chunk_presence = collections.Counter()  # block -> number of chunks containing it
    total_chunks = 0
    for region_dir in sys.argv[1:]:
        for name in sorted(os.listdir(region_dir)):
            if name.endswith(".mca"):
                total_chunks += scan_region(os.path.join(region_dir, name), counter, chunk_presence)
    print(f"chunks scanned: {total_chunks}, failed to parse: {FAILED}")
    ores = {k: v for k, v in counter.items() if "ore" in k}
    stones = {k: v for k, v in counter.items() if "ore" not in k}
    print(f"distinct gregtech blocks: {len(counter)}  (ores: {len(ores)})")
    print(f"total gregtech ore blocks: {sum(ores.values())}")
    print(f"total gregtech stone/other blocks: {sum(stones.values())}")
    for name, count in counter.most_common(30):
        pct = 100.0 * chunk_presence.get(name, 0) / max(1, total_chunks)
        print(f"  {name}: {count}  (in {pct:.0f}% of chunks)")


if __name__ == "__main__":
    main()
