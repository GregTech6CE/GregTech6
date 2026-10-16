"""GT6 MultiItemTechnological 1003/1005/1006/1009-1013/1015-1019 (12 implemented covers)."""
import hashlib
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / 'src/main/resources/assets/gregtech'
SPECS = [
    ('automatic_machine_switch', 'autoswitch', ['BW', 'CQ'], 1, 'lever', None),
    ('redstone_machine_switch', 'redstoneswitch', ['BW', 'CQ'], 1, 'redstone_torch', None),
    ('auto_redstone_machine_switch', 'autoredstoneswitch', ['BW', 'CQ'], 2, 'lever', None),
    ('auto_reboot_switch_1m', 'autotimerswitch/1200', ['BWd', 'CQ '], 5, 'repeater', None),
    ('auto_reboot_switch_5m', 'autotimerswitch/6000', ['BW ', 'CQd'], 4, 'repeater', None),
    ('auto_reboot_switch_10m', 'autotimerswitch/12000', ['BW ', 'CQ ', '  d'], 3, 'repeater', None),
    ('auto_reboot_switch_20m', 'autotimerswitch/24000', ['BW', 'CQ', ' d'], 2, 'repeater', None),
    ('auto_reboot_switch_30m', 'autotimerswitch/36000', ['BW', 'CQ', 'd '], 1, 'repeater', None),
    ('activity_detector_possible', 'detectorrunningpossible', ['WQW', 'BCB'], 2, 'comparator', None),
    ('activity_detector_running', 'detectorrunningpassively', ['WQW', 'BCB'], 2, 'repeater', None),
    ('activity_detector_processing', 'detectorrunningactively', ['WQW', 'BCX'], 2, 'comparator', 'repeater'),
    ('activity_detector_success', 'detectorrunningsuccessfully', ['WQW', 'BCX'], 2, 'stone_button', 'redstone_torch'),
]
CIRCUITS = ['unused', 'basic', 'good', 'advanced', 'elite', 'master']


def write(path, data):
    text = json.dumps(data, ensure_ascii=False, indent=2) + '\n'
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists() or path.read_text(encoding='utf-8') != text:
        path.write_text(text, encoding='utf-8')


def main(original=None):
    if original:
        ledger_path = ROOT / 'tools/gt6_texture_sources.json'
        ledger = json.loads(ledger_path.read_text(encoding='utf-8'))
        for texture in ['base'] + [s[1] + '/circuit' for s in SPECS]:
            source = original / f'src/main/resources/assets/gregtech/textures/blocks/machines/covers/{texture}.png'
            relative = f'textures/block/machines/covers/{texture}.png'
            target = ASSETS / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)
            ledger[relative] = {'source': source.relative_to(original).as_posix(), 'sha256': hashlib.sha256(source.read_bytes()).hexdigest()}
            if source.with_suffix('.png.mcmeta').exists():
                shutil.copy2(source.with_suffix('.png.mcmeta'), target.with_suffix('.png.mcmeta'))
        write(ledger_path, ledger)
    for name, texture, pattern, tier, component, extra in SPECS:
        foreground = f'gregtech:block/machines/covers/{texture}/circuit'
        write(SHARED_ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated',
              'textures': {'layer0': 'gregtech:block/machines/covers/base', 'layer1': foreground, 'particle': foreground}})
        keys = {'Q': 'gregtech:blank_cover', 'C': 'gregtech:circuit_' + CIRCUITS[tier],
                'W': 'gregtech:cable_01_' + ('copper' if name == 'auto_redstone_machine_switch' else 'tin'),
                'B': 'minecraft:' + component, 'd': 'gregtech:tool_screwdriver'}
        if extra:
            keys['X'] = 'minecraft:' + extra
        used = set(''.join(pattern)) - {' '}
        write(ROOT / f'src/main/resources/data/gregtech/recipes/control_covers/{name}.json', {
            'type': 'gregtech:tool_shaped', 'pattern': pattern,
            'key': {k: {'item': keys[k]} for k in sorted(used)}, 'result': {'item': 'gregtech:' + name}})


if __name__ == '__main__':
    import sys
    main(Path(sys.argv[1]) if len(sys.argv) > 1 else None)
