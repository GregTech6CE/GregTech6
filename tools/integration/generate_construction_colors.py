"""Generate assets for color-bound construction IDs, retaining the original palette as compatibility data."""
from pathlib import Path
import argparse
import json
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from localization import json_text

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'core/src/main/resources/assets/gt6ce'
FAMILIES = ('asphalt', 'concrete', 'concrete_reinforced', 'cfoam', 'cfoam_fresh',
            'cfoam_slab', 'glass_clear', 'glass_glow', 'glass_clear_slab', 'glass_glow_slab')
COLORS = ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
          'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black')


def outputs():
    languages = {locale: json.loads((ASSETS / f'lang/{locale}.json').read_text(encoding='utf-8'))
                 for locale in ('en_us', 'zh_cn')}
    for family in FAMILIES:
        yield ROOT / f'core/src/main/resources/data/gt6ce/tags/items/construction/{family}.json', json_text({
            'replace': False, 'values': [f'gt6ce:{family}'] + [f'gt6ce:{family}_{color}' for color in COLORS]})
        source = json.loads((ASSETS / f'blockstates/{family}.json').read_text(encoding='utf-8'))
        item = json.loads((ASSETS / f'models/item/{family}.json').read_text(encoding='utf-8'))
        for color in COLORS:
            states = {}
            for key, model in source['variants'].items():
                parts = key.split(',') if key else []
                dyes = [part.removeprefix('color=') for part in parts if part.startswith('color=')]
                if dyes and dyes != [color]:
                    continue
                states[','.join(part for part in parts if not part.startswith('color='))] = model
            if not states:
                raise ValueError(f'No source model for {family}/{color}')
            yield ASSETS / f'blockstates/{family}_{color}.json', json_text({'variants': states})
            yield ASSETS / f'models/item/{family}_{color}.json', json_text(item)
            for locale, values in languages.items():
                values[f'block.gt6ce.{family}_{color}'] = values[f'block.gt6ce.{family}.{color}']
    for locale, values in languages.items():
        yield ASSETS / f'lang/{locale}.json', json_text(values)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    changed = []
    for path, value in outputs():
        if not path.exists() or path.read_text(encoding='utf-8') != value:
            changed.append(str(path.relative_to(ROOT)))
            if not args.check:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(value, encoding='utf-8')
    if args.check and changed:
        raise SystemExit('Construction color assets drift: ' + ', '.join(changed[:8]))
    print(f'Construction colors: 160 identities, 320 asset files; {len(changed)} files updated' if not args.check
          else 'Construction color assets and names match all 160 identities')


if __name__ == '__main__':
    main()
