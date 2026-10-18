"""Extract original MT/OP molecular-data facts, keyed by numeric material identity.

Read-only source access. Factory arguments are substituted before extracting tags;
uumMcfg/uumAloy propagate UUM only when every referenced component has that tag.
"""
from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

import argparse
import hashlib
import importlib.util
import json
import re
from pathlib import Path


def masked(text):
    # Preserve offsets and strings, remove comments (including commented declarations).
    return re.sub(r'"(?:\\.|[^"\\])*"|//[^\n]*|/\*[\s\S]*?\*/',
                  lambda m: m[0] if m[0].startswith('"') else re.sub(r'[^\n]', ' ', m[0]), text)


def end(text, start, terminators):
    depth = 0
    quoted = False
    escaped = False
    for i in range(start, len(text)):
        c = text[i]
        if quoted:
            if escaped: escaped = False
            elif c == '\\': escaped = True
            elif c == '"': quoted = False
            continue
        if c == '"': quoted = True
        elif depth == 0 and c in terminators: return i
        elif c in '([{': depth += 1
        elif c in ')]}': depth -= 1
    raise ValueError('unclosed expression')


def args(text):
    result = []
    start = 0
    while start < len(text):
        stop = end(text + ',', start, ',')
        result.append(text[start:stop].strip())
        start = stop + 1
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[2])
    ns = parser.parse_args()
    root = native_path(ns.source / 'src/main/java/gregapi')
    mt = masked((root/'data/MT.java').read_text(encoding='utf-8'))
    op = masked((root/'data/OP.java').read_text(encoding='utf-8'))
    spec = importlib.util.spec_from_file_location('forms', ns.repo/'tools/extract_gt6_form_flags.py')
    forms = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(forms)
    factories = forms.factory_overloads(mt)
    memo = {}

    def expand(text, depth=0):
        if depth > 18: raise ValueError('factory cycle: '+text[:300])
        out = ''
        at = 0
        for match in re.finditer(r'(?<![.\w])([a-z]\w*)\s*\(', text):
            if match.start() < at or match[1] not in factories: continue
            opening = match.end()-1
            closing = end(text, opening+1, ')')
            call_args = args(text[opening+1:closing])
            key = (match[1], tuple(call_args))
            if key not in memo:
                bodies = []
                for params, varargs, body in factories[match[1]]:
                    if not forms.accepts(params, varargs, len(call_args)): continue
                    fixed = params[:-1] if varargs else params
                    if any(('TextureSet[]' in param and not value.startswith('SET_'))
                           or ('String ' in param and (value.startswith('SET_') or re.match(r'-?\d',value)))
                           or (re.match(r'(?:int|long|double|byte) ',param)
                               and (value.startswith('SET_') or value.startswith('"')))
                           for param,value in zip(fixed,call_args)): continue
                    if match[1] == 'create' and len(params) == 2:
                        bodies.append(text[match.start():closing+1])
                        continue
                    replacements = {}
                    for i, param in enumerate(params):
                        name = param.split()[-1]
                        replacements[name] = ','.join(call_args[i:]) if varargs and i == len(params)-1 else call_args[i]
                    body = re.sub(r'\ba\w+\b', lambda m: replacements.get(m[0], m[0]), body)
                    bodies.append(expand(body, depth+1))
                if not bodies: raise ValueError(f'no overload: {key}')
                # Overloads with equivalent argument counts may differ in generator flags;
                # these facts only depend on the substituted UUM and prefix calls.
                memo[key] = '\n'.join(dict.fromkeys(bodies))
            out += text[at:match.start()] + memo[key]
            at = closing+1
        return out + text[at:]

    rows = {}
    by_name = {}
    for match in re.finditer(r'^\s*(?:public static final OreDictMaterial\s+)?(\w+)\s*=\s*', mt, re.M):
        start = match.end()
        expr = mt[start:end(mt,start,',;')]
        if not re.match(r'[a-z]\w*\s*\(',expr): continue
        expanded = expand(expr)
        identity = re.search(r'\bcreate\s*\(\s*(-?\d+)\s*,', expanded)
        if not identity: raise ValueError(f'no identity: {match[1]}')
        ident = int(identity[1])
        if ident <= 0: continue
        dependencies = []
        for dep in re.finditer(r'\.uum(?:Mcfg|Aloy)\s*\(',expanded):
            opening = dep.end()-1
            dep_args = args(expanded[opening+1:end(expanded,opening+1,')')])
            dependencies.append([x.removeprefix('MT.') for x in dep_args[1::2]])
        priority = re.findall(r'\.setPriorityPrefix\(\s*([0-5])\s*\)',expanded)
        row = {'id':ident,'field':match[1],'direct_uum': bool(re.search(r'\bUUM\b',expanded)),
               'dependencies':dependencies, 'priority':int(priority[-1]) if priority else 0}
        rows[ident] = row
        by_name[match[1]] = row
    # Resolve only original conditional propagation. Unknown references are errors, not
    # an invitation to treat every chemical material as replicatable.
    for row in rows.values(): row['uum'] = row['direct_uum']
    for _ in range(len(rows)):
        changed = False
        for row in rows.values():
            for group in row['dependencies']:
                missing = [name for name in group if name not in by_name]
                if missing: raise ValueError(f"unknown UUM components of {row['field']}: {missing}")
                if not row['uum'] and all(by_name[name]['uum'] for name in group):
                    row['uum'] = True
                    changed = True
        if not changed: break
    prefixes = []
    for match in re.finditer(r'\b(\w+)\s*=\s*create\s*\(',op):
        start = match.end()-1
        expr = op[start:end(op,start,',;')]
        if re.search(r'\bSCANNABLE\b',expr): prefixes.append(match[1])
    assert len(rows)>1000 and len(prefixes)>60
    uum = [str(k) for k,row in sorted(rows.items()) if row['uum']]
    priority_cases = []
    for i,name in enumerate(['','gem','dust','ingot','plate','plateGem']):
        ids = [str(k) for k,row in sorted(rows.items()) if row['priority']==i]
        if i and ids: priority_cases.append('            case '+','.join(ids)+' -> MaterialPrefix.'+name+';')
    code = '''package com.gregtech.gregtech.data.generated;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.Set;

/** Generated from Gregorius Techneticies' LGPL-3.0-or-later MT/OP facts.
 * Regenerate with tools/integration/extract_material_data_rules.py. */
public final class MaterialDataFacts {
    private MaterialDataFacts() {}
    private static final Set<Integer> UUM_IDS = Set.of(
        '''+',\n        '.join(','.join(uum[i:i+24]) for i in range(0,len(uum),24))+''');
    private static final Set<String> SCANNABLE = Set.of(
        '''+',\n        '.join(','.join('"'+p+'"' for p in prefixes[i:i+8]) for i in range(0,len(prefixes),8))+''');
    public static void apply() {
        for (int id : UUM_IDS) {
            GTMaterial material = GTMaterialRegistry.get(id);
            if (material.isValid()) material.put(MaterialProperty.UUM);
        }
    }
    public static boolean scannable(MaterialPrefix prefix) {
        return prefix != null && SCANNABLE.contains(com.gregtech.gregtech.api.prefix.PrefixRegistry.sourceName(prefix.getName()));
    }
    public static MaterialPrefix priority(GTMaterial material) {
        return switch (material.getId()) {
'''+ '\n'.join(priority_cases)+'''
            default -> null;
        };
    }
}
'''
    target = ns.repo/'core/src/main/java/com/gregtech/gregtech/data/generated/MaterialDataFacts.java'
    target.write_text(code,encoding='utf-8')
    evidence = {'generator':'tools/integration/extract_material_data_rules.py','materials':list(rows.values()),
                'scannable_prefixes':prefixes,'sources':{str(root/name):hashlib.sha256((root/name).read_bytes()).hexdigest()
                for name in ['data/MT.java','data/OP.java','oredict/OreDictMaterial.java']}}
    (ns.repo/'docs/integration/verification/material-data-facts-20261006.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'materials':len(rows),'uum':len(uum),'prefixes':len(prefixes)}))


if __name__ == '__main__': main()
