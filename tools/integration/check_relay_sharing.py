"""Compare shared relay operations with a Git baseline after inlining platform wrappers.

Reads maintained source directly. This is a source-equivalence check, not a native
compiler or a substitute for item/fluid capability runtime tests.
"""
from pathlib import Path
import argparse
import json
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from native_layout import clean_java
from check_stonecutter_emi import signature

SOURCE = 'multiversion/src/main/java/com/gregtech/gregtech/blockentity/CapabilityRelayBlockEntity.java'


def project(source, platform):
    # clean_java expects generated Java. Maintained Forge-active source still wraps
    # NeoForge branches in block comments, so expose those specific bodies first.
    # Stonecutter escapes nested block comments as /^* ... ^/. Restore them within
    # the exposed body before Java tokenization (an apostrophe in a masked comment
    # would otherwise be mistaken for a character literal and hide directives).
    def expose(match):
        body = re.sub(r'/\^\*([\s\S]*?)\^/', r'/*\1*/', match[2])
        return match[1] + body

    exposed = re.sub(r'(//\? if >=1\.21\.1 \{\s*)/\*([\s\S]*?)\*/(?=//\?})',
                     expose, source)
    return clean_java(exposed, platform)


def verify(root, baseline):
    before = subprocess.check_output(['git', 'show', f'{baseline}:{SOURCE}'], cwd=root).decode('utf-8')
    after = (root / SOURCE).read_text(encoding='utf-8')
    results = {}
    for platform, item_cap, fluid_cap in (
            ('forge', 'ForgeCapabilities.ITEM_HANDLER', 'ForgeCapabilities.FLUID_HANDLER'),
            ('neoforge', 'Capabilities.ItemHandler.BLOCK', 'Capabilities.FluidHandler.BLOCK')):
        current = project(after, platform)
        calls = {}
        for method, handler, capability, expected_count in (
                ('forwardItems', 'IItemHandler', item_cap, 6),
                ('forwardFluids', 'IFluidHandler', fluid_cap, 7)):
            wrapper = (r'private <R> R ' + method
                       + r'\(Direction side, R unavailable, Function<' + handler
                       + r', R> operation\) \{\s*return forward\(side, '
                       + re.escape(capability) + r', unavailable, operation\);\s*\}')
            current, count = re.subn(wrapper, '', current)
            if count != 1:
                raise AssertionError(f'{platform}: expected exactly one simple {method} wrapper, got {count}')
            current, count = re.subn(method + r'\(side\s*,\s*', 'forward(side,' + capability + ',', current)
            if count != expected_count:
                raise AssertionError(f'{platform}: expected {expected_count} {method} calls, got {count}')
            calls[method] = count
        if signature(current) != signature(project(before, platform)):
            raise AssertionError(f'{platform}: relay code or literals changed beyond wrapper extraction')
        results[platform] = {'code_and_literals_equal_after_inlining': True, 'shared_operations': calls}
    return results


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline', required=True, help='Git revision before sharing the relay methods')
    args = parser.parse_args()
    print(json.dumps({'baseline': args.baseline, 'platforms': verify(ROOT, args.baseline)}, indent=2))
