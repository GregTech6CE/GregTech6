"""Emit readable material catalogues from the GT6 transpiler's intermediate Java.

Legacy field names remain in a generated compatibility facade only. Registry
names, IDs, factory calls and their ordering are preserved.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

from pathlib import Path
import re
from format_definition_tables import align_java_tables

ROOT = Path(__file__).resolve().parents[1]
JAVA = native_path(ROOT / 'src/main/java/com/gregtech/gregtech')
GROUPS = {'Elements': 'ElementMaterials', 'Compounds': 'CompoundMaterials',
          'Ores': 'OreMaterials', 'Stones': 'StoneMaterials', 'Woods': 'WoodMaterials'}
TOKEN = re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/|\b[A-Za-z_$][\w$]*\b')

def replace_identifiers(text, names):
    return TOKEN.sub(lambda m: names.get(m.group(), m.group()), text)

def parse_catalog(source):
    groups = {}
    for old, new in GROUPS.items():
        start = source.index('    public static final class ' + old + ' {')
        end = source.index('\n    }', start)
        rows = []
        for match in re.finditer(r'^\s+(\w+) = (.+)[,;]$', source[start:end], re.M):
            field, expression = match.groups()
            literal = re.match(r'\w+\([^,]+,\s*"([^"]+)"', expression)
            readable = re.sub(r'[^A-Za-z0-9_$]', '', literal.group(1)) if literal else field
            if not readable or readable[0].isdigit(): readable = 'Material' + readable
            rows.append((field, readable, expression))
        if not rows: raise ValueError('Empty material group: ' + old)
        names = [r[1] for r in rows]
        if len(set(names)) != len(names): raise ValueError('Duplicate readable material names in ' + old)
        groups[old] = rows
    return groups

def write_catalog(source, java_root=JAVA, save_intermediate=True):
    groups = parse_catalog(source)
    if save_intermediate:
        intermediate = ROOT / 'tools/generated/materials-source.java.txt'
        intermediate.parent.mkdir(parents=True, exist_ok=True)
        intermediate.write_text(source, encoding='utf-8')
    package = 'com.gregtech.gregtech.content.material.generated'
    imports = ('import com.gregtech.gregtech.api.material.GTMaterial;\n'
               'import com.gregtech.gregtech.api.material.MaterialProperty;\n'
               'import com.gregtech.gregtech.api.material.MaterialTextureSet;\n'
               'import com.gregtech.gregtech.data.GregTechConstants;\n'
               'import static com.gregtech.gregtech.api.material.MaterialFactories.*;\n')
    facade = source[:source.index('    public static final class Elements {')]
    for group, rows in groups.items():
        cls = GROUPS[group]
        lines = [f'package {package};\n', imports,
                 '/** Generated from GT6 material definitions. Regenerate with tools/transpile_gt6_materials.py. */',
                 f'public final class {cls} {{', f'    private {cls}() {{}}', '']
        for old, readable, expression in rows:
            expression = replace_identifiers(expression, {'CS': 'GregTechConstants'})
            # Same-group aliases are the only unqualified material references in field declarations.
            if expression in dict((r[0], r[1]) for r in rows):
                expression = dict((r[0], r[1]) for r in rows)[expression]
            lines.append(f'    public static final GTMaterial {readable} = {expression};')
        lines.extend(['}', ''])
        dest = java_root / f'content/material/generated/{cls}.java'
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(align_java_tables('\n'.join(lines)), encoding='utf-8')
        facade += f'    public static final class {group} {{\n        private {group}() {{}}\n'
        for old, readable, _ in rows:
            facade += f'        public static final GTMaterial {old} = {package}.{cls}.{readable};\n'
        facade += '    }\n\n'
    facade += '}\n'
    facade = replace_identifiers(facade, {'CS': 'GregTechConstants', 'MT': 'ImportedMaterialData'})
    facade = facade.replace('import static com.gregtech.gregtech.data.ImportedMaterialData.*;\n', '')
    facade = facade.replace('/** Auto-transpiled from GregTech 6 {@code gregapi.data.MT}. Do not edit by hand. */',
                            '/** Legacy GT6 symbol facade. New content uses content.material.generated catalogues. */')
    dest = java_root / 'data/generated/GT6Materials.java'
    dest.write_text(align_java_tables(facade), encoding='utf-8')
    write_readable_references(groups, java_root)
    return groups


def write_readable_references(groups, java_root=JAVA):
    """Refresh the friendly references after a GT6 import or legacy alias update."""
    legacy = (java_root / 'data/ImportedMaterialData.java').read_text(encoding='utf-8')
    start = legacy.index('    // <generated-mt-fields>')
    end = legacy.index('    public static final class OREMATS', start)
    targets = {f'GT6Materials.{g}.{old}': f'{GROUPS[g]}.{new}'
               for g, rows in groups.items() for old, new, _ in rows}
    names = {}
    for _, target in re.findall(r'^\s+(\w+) = (GT6Materials\.\w+\.\w+)[,;]', legacy[start:end], re.M):
        if target not in targets: continue
        readable = targets[target]
        field = readable.split('.')[-1]
        if field in names and names[field] != readable: field = readable.replace('.', '')
        names[field] = readable
    source = '''package com.gregtech.gregtech.content.material;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.content.material.generated.*;

/** Readable built-in references. Generated by tools/modular_materials.py; add custom definitions separately. */
public final class Materials {
    private Materials() {}
    static { GTMaterialRegistry.init(); }
    public static final GTMaterial Invalid = com.gregtech.gregtech.api.material.MaterialSentinels.Invalid;
    public static final GTMaterial Empty = com.gregtech.gregtech.api.material.MaterialSentinels.Empty;
    public static final GTMaterial Magic = com.gregtech.gregtech.data.ImportedMaterialData.Ma;
    public static final GTMaterial Photon = ParticleMaterials.Photon;
    public static final GTMaterial Neutrino = ParticleMaterials.Neutrino;
    public static final GTMaterial Neutron = ParticleMaterials.Neutron;
    public static final GTMaterial Proton = ParticleMaterials.Proton;
    public static final GTMaterial Electron = ParticleMaterials.Electron;
'''
    source += ''.join(f'    public static final GTMaterial {field} = {target};\n' for field, target in sorted(names.items()))
    source += '}\n'
    (java_root / 'content/material/Materials.java').write_text(align_java_tables(source), encoding='utf-8')
