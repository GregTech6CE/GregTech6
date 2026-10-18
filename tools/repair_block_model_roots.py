"""Repair composite root display/particle inheritance after legacy asset generation.

Child models do not supply their display transforms to the composite root.
Existing explicit parents and display overrides are preserved.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import json
from pathlib import Path

ROOT = native_path(Path(__file__).resolve().parents[1] / 'src/main/resources/assets/gt6ce/models/block')


def repair(model):
    if model.get('loader') != 'forge:composite' or 'parent' in model:
        return False
    model['parent'] = 'minecraft:block/block'
    if 'particle' not in model.get('textures', {}):
        for child in model.get('children', {}).values():
            candidates = [value for value in child.get('textures', {}).values()
                          if isinstance(value, str) and not value.startswith('#')]
            if candidates:
                model.setdefault('textures', {})['particle'] = candidates[0]
                break
    return True


def main():
    changed = 0
    for path in ROOT.rglob('*.json'):
        model = json.loads(path.read_text(encoding='utf-8'))
        if repair(model):
            path.write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')
            changed += 1
    print(f'{changed} composite roots repaired')


if __name__ == '__main__':
    main()
