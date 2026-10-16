"""Repair composite root display/particle inheritance after legacy asset generation.

Child models do not supply their display transforms to the composite root.
Existing explicit parents and display overrides are preserved.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'platform-shared/src/main/resources/assets/gregtech/models/block'


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
