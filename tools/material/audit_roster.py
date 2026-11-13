#!/usr/bin/env python3
"""Summarize material forms from a fresh runtime baseline."""

from __future__ import annotations

import argparse
import collections
import csv
import json
import re
from pathlib import Path


def recipe_items(slots: str) -> tuple[set[str], set[str], set[str]]:
    inputs: set[str] = set()
    outputs: set[str] = set()
    input_tags: set[str] = set()
    for slot in slots.split(";"):
        if slot.startswith("in["):
            item_match = re.search(r"=(.+?)\*(\d+):", slot)
            tag_match = re.search(r":tag=(.+?):catalyst=", slot)
            if item_match:
                inputs.add(item_match.group(1))
            if tag_match and tag_match.group(1) != "-":
                input_tags.add(tag_match.group(1))
        elif slot.startswith("out["):
            item_match = re.search(r"=(.+?)\*(\d+):", slot)
            if item_match:
                outputs.add(item_match.group(1))
    return inputs, outputs, input_tags


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("snapshot", type=Path)
    parser.add_argument("--json", type=Path)
    args = parser.parse_args()

    forms: dict[str, dict[str, str]] = {}
    with (args.snapshot / "reload-forms.tsv").open() as source:
        for row in csv.reader(source, delimiter="\t"):
            if len(row) == 5:
                forms[row[0]] = {"prefix": row[1], "material": row[2], "weight": row[3], "valid": row[4]}

    items: dict[str, list[str]] = {}
    with (args.snapshot / "reload-items.tsv").open() as source:
        for row in csv.reader(source, delimiter="\t"):
            items[row[0]] = row

    block_forms: dict[str, dict[str, str]] = {}
    with (args.snapshot / "reload-block-forms.tsv").open() as source:
        for row in csv.reader(source, delimiter="\t"):
            if len(row) == 6:
                block_forms[row[1]] = {
                    "block": row[0],
                    "prefix": row[2],
                    "material": row[3],
                    "weight": row[4],
                    "valid": row[5],
                }

    item_prefixes = {
        item: row[1] for item, row in items.items() if len(row) == 12 and row[1] != "-"
    }

    tags: dict[str, set[str]] = {}
    with (args.snapshot / "reload-tags.tsv").open() as source:
        for row in csv.reader(source, delimiter="\t"):
            if len(row) == 2 and row[0].startswith("item:"):
                tags[row[0].removeprefix("item:")] = set(filter(None, row[1].split(",")))

    source_maps: dict[str, collections.Counter[str]] = collections.defaultdict(collections.Counter)
    input_maps: dict[str, collections.Counter[str]] = collections.defaultdict(collections.Counter)
    exact_sources: dict[str, set[str]] = collections.defaultdict(set)
    exact_inputs: dict[str, set[str]] = collections.defaultdict(set)

    with (args.snapshot / "reload-recipes.tsv").open() as source:
        for row in csv.reader(source, delimiter="\t"):
            if len(row) < 2:
                continue
            recipe_map, slots = row[0], row[1]
            inputs, outputs, input_tags = recipe_items(slots)
            for tag in input_tags:
                inputs.update(tags.get(tag, ()))
            for item in outputs:
                prefix = item_prefixes.get(item)
                if prefix:
                    source_maps[prefix][recipe_map] += 1
                    exact_sources[prefix].add(item)
                if item in block_forms:
                    prefix = block_forms[item]["prefix"]
                    source_maps[prefix][recipe_map] += 1
                    exact_sources[prefix].add(item)
            for item in inputs:
                prefix = item_prefixes.get(item)
                if prefix:
                    input_maps[prefix][recipe_map] += 1
                    exact_inputs[prefix].add(item)
                if item in block_forms:
                    prefix = block_forms[item]["prefix"]
                    input_maps[prefix][recipe_map] += 1
                    exact_inputs[prefix].add(item)

    prefix_items = collections.defaultdict(set)
    for item, prefix in item_prefixes.items():
        prefix_items[prefix].add(item)
    for item, form in forms.items():
        prefix_items[form["prefix"]].add(item)

    prefix_blocks = collections.defaultdict(set)
    for item, form in block_forms.items():
        prefix_blocks[form["prefix"]].add(form["block"])

    result = {}
    for prefix in sorted(prefix_items.keys() | prefix_blocks.keys()):
        members = prefix_items[prefix]
        result[prefix] = {
            "items": len(members),
            "material_form_items": sum(item in forms for item in members),
            "material_components": len(
                {forms[item]["material"] for item in members if item in forms}
            ),
            "composition_items": sum(item in items for item in members),
            "source_items": len(exact_sources[prefix]),
            "input_items": len(exact_inputs[prefix]),
            "source_maps": dict(source_maps[prefix].most_common()),
            "input_maps": dict(input_maps[prefix].most_common()),
            "blocks": len(prefix_blocks[prefix]),
            "source_blocks": len({block for block in prefix_blocks[prefix] if block in exact_sources[prefix]}),
            "input_blocks": len({block for block in prefix_blocks[prefix] if block in exact_inputs[prefix]}),
        }

    rendered = json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if args.json:
        args.json.write_text(rendered)
    else:
        print(rendered, end="")


if __name__ == "__main__":
    main()
