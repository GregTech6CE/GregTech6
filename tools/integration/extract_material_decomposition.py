#!/usr/bin/env python3
"""Import GT6's explicit decomposition permissions, never infer them from composition.

Use tools/native_tool.py for managed generation; --check verifies without writes.
Factory parameters/varargs are substituted before interpreting .put flags; the
OreDictMaterial alloy helpers are verified against source before applying their
known grants. No reference source is modified.
"""
from __future__ import annotations
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import sys

FLAGS = ('DECOMPOSABLE', 'CENTRIFUGE', 'ELECTROLYSER')


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    sys.modules[name] = value
    spec.loader.exec_module(value)
    return value


def parse(source, repo):
    rules = load('decomposition_data_rules', repo / 'tools/integration/extract_material_data_rules.py')
    forms = load('decomposition_forms', repo / 'tools/extract_gt6_form_flags.py')
    mt_file = source / 'src/main/java/gregapi/data/MT.java'
    material_file = source / 'src/main/java/gregapi/oredict/OreDictMaterial.java'
    raw = mt_file.read_text(encoding='utf-8')
    mt = rules.masked(raw)
    material = rules.masked(material_file.read_text(encoding='utf-8'))
    # These methods are the only instance-method grants in both audited versions.
    # Fail rather than silently retain a guessed rule if the reference changes.
    for method, route in [('alloyCentrifuge', 'CENTRIFUGE'), ('alloyElectrolyzer', 'ELECTROLYSER')]:
        bodies = re.findall(r'public OreDictMaterial ' + method + r'\([^)]*\)\s*\{([^}]*)\}', material)
        if len(bodies) != 4 or any('put(TD.Processing.' + route + ').alloySimple(' not in body for body in bodies):
            raise ValueError('unverified helper: ' + method)
    bodies = re.findall(r'public OreDictMaterial alloySimple\([^)]*\)\s*\{([^}]*)\}', material)
    if len(bodies) != 4 or any('TD.Compounds.DECOMPOSABLE' not in body for body in bodies):
        raise ValueError('unverified alloySimple grant')
    for method in ('setAloy', 'uumAloy'):
        bodies = re.findall(r'public OreDictMaterial ' + method + r'\([^)]*\)\s*\{([^}]*)\}', material)
        if len(bodies) != 9 or any('.alloyCentrifuge()' not in body for body in bodies):
            raise ValueError('unverified helper: ' + method)
    if re.search(r'\.remove\([^)]*\b(?:DECOMPOSABLE|CENTRIFUGE|ELECTROLYSER)\b', mt):
        raise ValueError('processing flag removal requires ordered interpretation')

    factories = forms.factory_overloads(mt)
    memo = {}
    def expand(text, depth=0):
        if depth > 18:
            raise ValueError('factory cycle: ' + text[:300])
        out, at = '', 0
        for match in re.finditer(r'(?<![.\w])([a-z]\w*)\s*\(', text):
            if match.start() < at or match[1] not in factories:
                continue
            opening = match.end() - 1
            closing = rules.end(text, opening + 1, ')')
            call_args = rules.args(text[opening + 1:closing])
            key = (match[1], tuple(call_args))
            if key not in memo:
                bodies = []
                for params, varargs, body in factories[match[1]]:
                    if not forms.accepts(params, varargs, len(call_args)):
                        continue
                    fixed = params[:-1] if varargs else params
                    if any(('TextureSet[]' in param and not value.startswith('SET_'))
                           or ('String ' in param and (value.startswith('SET_') or re.match(r'-?\d', value)))
                           or (re.match(r'(?:int|long|double|byte) ', param)
                               and (value.startswith('SET_') or value.startswith('"')))
                           for param, value in zip(fixed, call_args)):
                        continue
                    if match[1] == 'create' and len(params) == 2:
                        bodies.append(text[match.start():closing + 1])
                        continue
                    replacements = {param.split()[-1]: ','.join(call_args[i:])
                                    if varargs and i == len(params) - 1 else call_args[i]
                                    for i, param in enumerate(params)}
                    body = re.sub(r'\ba\w+\b', lambda m: replacements.get(m[0], m[0]), body)
                    bodies.append(expand(body, depth + 1))
                if not bodies:
                    raise ValueError('no matching factory overload: ' + str(key))
                # Any unresolved overload choice must agree on the policy we import.
                if len({tuple(sorted(policy(body))) for body in bodies}) != 1:
                    raise ValueError('ambiguous processing flags: ' + str(key))
                memo[key] = '\n'.join(dict.fromkeys(bodies))
            out += text[at:match.start()] + memo[key]
            at = closing + 1
        return out + text[at:]

    def policy(expression):
        # Mask string literals so an alias/tooltip named CENTRIFUGE cannot grant a flag.
        expression = re.sub(r'"(?:\\.|[^"\\])*"', '""', expression)
        result = set(re.findall(r'\b(?:DECOMPOSABLE|CENTRIFUGE|ELECTROLYSER)\b', expression))
        methods = set(re.findall(r'\.(\w+)\s*\(', expression))
        if methods & {'setAloy', 'uumAloy', 'alloyCentrifuge'}:
            result.update(('DECOMPOSABLE', 'CENTRIFUGE'))
        if 'alloyElectrolyzer' in methods:
            result.update(('DECOMPOSABLE', 'ELECTROLYSER'))
        if 'alloySimple' in methods:
            result.add('DECOMPOSABLE')
        return result

    rows = []
    seen = set()
    for match in re.finditer(r'^\s*(?:public static final OreDictMaterial\s+)?(\w+)\s*=\s*', mt, re.M):
        expr = mt[match.end():rules.end(mt, match.end(), ',;')]
        if not re.match(r'[a-z]\w*\s*\(', expr):
            continue
        expanded = expand(expr)
        identity = re.search(r'\bcreate\s*\(\s*(-?\d+)\s*,', expanded)
        if not identity:
            raise ValueError('no identity: ' + match[1])
        ident = int(identity[1])
        if ident <= 0:  # unused/idless placeholders are not registered built-in identities
            continue
        if ident in seen:
            raise ValueError('duplicate original material ID: ' + str(ident))
        seen.add(ident)
        flags = policy(expanded)
        if not flags:
            continue
        name = re.search(r'\bcreate\s*\(\s*-?\d+\s*,\s*"([^\"]+)"', expanded)
        if not name:
            raise ValueError('policy material name is not literal: ' + match[1])
        rows.append({'id': ident, 'field': match[1], 'name': name[1].replace(' ', '').replace('-', '').replace("'", ''),
                     'line': raw.count('\n', 0, match.start(1)) + 1,
                     'flags': [flag for flag in FLAGS if flag in flags],
                     'declaration': raw[match.start(1):match.end()] + expr})
    return sorted(rows, key=lambda row: row['id'])


def render(rows):
    lines = '\n'.join('        bind(%d, "%s", %s);' % (row['id'], row['name'], ', '.join(row['flags'])) for row in rows)
    return '''package com.gregtech.gregtech.data.generated;

import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import static com.gregtech.gregtech.api.material.MaterialProperty.*;

/** Generated from Gregorius Techneticies' LGPL-3.0-or-later MT/OreDictMaterial policy.
 * Regenerate with tools/integration/extract_material_decomposition.py. */
public final class MaterialDecompositionData {
    private MaterialDecompositionData() {}

    public static void apply() {
''' + lines + '''
    }

    private static void bind(int originalId, String canonicalName, MaterialProperty... flags) {
        var material = GTMaterialRegistry.get(originalId);
        // Do not transfer original permissions to a different identity or an addon.
        // The built-in snapshot is not available yet during MaterialDefinitions.link().
        if (material.isValid() && material.getId() == originalId
                && material.getName().equals(canonicalName) && !AddonMaterials.owns(material)) {
            material.put(flags);
        }
    }
}
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--compare-source', type=Path, required=True)
    parser.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument('--java-output', type=Path)
    parser.add_argument('--report-output', type=Path)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    sys.path.insert(0, str(args.repo / 'tools'))
    from native_layout import require_managed_writer
    if not args.check:
        require_managed_writer()
    java_output = args.java_output or args.repo / 'core/src/main/java/com/gregtech/gregtech/data/generated/MaterialDecompositionData.java'
    report_output = args.report_output or args.repo / 'docs/integration/verification/material-decomposition-policy-20261023.json'
    rows = parse(args.source, args.repo)
    other = parse(args.compare_source, args.repo)
    identity = lambda data: [(row['id'], row['name'], row['flags']) for row in data]
    if identity(rows) != identity(other):
        raise ValueError('reference versions disagree; review policies before generating')
    sources = []
    for root in (args.source, args.compare_source):
        paths = ['src/main/java/gregapi/data/MT.java', 'src/main/java/gregapi/oredict/OreDictMaterial.java',
                 'src/main/java/gregtech/loaders/c/Loader_Recipes_Decomp.java']
        sources.append({'reference': root.name, 'files': [{'path': path, 'sha256': hashlib.sha256((root/path).read_bytes()).hexdigest()} for path in paths]})
    report = {'sources': sources, 'policy': 'explicit DECOMPOSABLE plus independent machine flags; no composition inference',
              'row_count': len(rows), 'flag_counts': {flag: sum(flag in row['flags'] for row in rows) for flag in FLAGS},
              'rows': rows}
    outputs = {java_output: render(rows), report_output: json.dumps(report, indent=2) + '\n'}
    for path, content in outputs.items():
        if args.check:
            if not path.is_file() or path.read_text(encoding='utf-8') != content:
                raise ValueError('Generated policy is stale: ' + str(path))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding='utf-8')
    print('Both references agree:', report['row_count'], report['flag_counts'])

if __name__ == '__main__':
    main()
