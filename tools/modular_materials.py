"""Emit readable material catalogues from the GT6 transpiler's intermediate Java.

Legacy field names remain in a generated compatibility facade only. Registry
names, IDs, factory calls and their ordering are preserved.
"""

from pathlib import Path
import re
from format_definition_tables import align_java_tables

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'core/src/main/java/com/gregtech/gregtech'
GROUPS = {'Elements': 'ElementMaterials', 'Compounds': 'CompoundMaterials',
          'Ores': 'OreMaterials', 'Stones': 'StoneMaterials', 'Woods': 'WoodMaterials'}
# These formerly shadowed MT fields were adopted after the original wood table.
# Preserve the checked-in holder initialization order and public facade order.
LATE_WOOD_FIELDS = ('Magic', 'Cinnamon', 'Lime', 'Ash')
WOOD_FACADE_FIELDS = ('Cinnamon', 'Lime', 'Ash', 'Magic')
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
        group_imports = imports
        if any('new AtomicProperties(' in expression for _, _, expression in rows):
            group_imports = group_imports.replace(
                'import com.gregtech.gregtech.api.material.GTMaterial;\n',
                'import com.gregtech.gregtech.api.material.GTMaterial;\n'
                'import com.gregtech.gregtech.api.material.AtomicProperties;\n')
        field_rows = rows
        if group == 'Woods':
            late = {row[0]: row for row in rows if row[0] in LATE_WOOD_FIELDS}
            field_rows = [row for row in rows if row[0] not in late]
            field_rows += [late[name] for name in LATE_WOOD_FIELDS if name in late]
        lines = [f'package {package};\n', group_imports,
                 '/** Generated from GT6 material definitions. Regenerate with tools/transpile_gt6_materials.py. */',
                 f'public final class {cls} {{', f'    private {cls}() {{}}', '']
        for old, readable, expression in field_rows:
            expression = replace_identifiers(expression, {'CS': 'GregTechConstants'})
            # Same-group aliases are the only unqualified material references in field declarations.
            if expression in dict((r[0], r[1]) for r in rows):
                expression = dict((r[0], r[1]) for r in rows)[expression]
            lines.append(f'    public static final GTMaterial {readable} = {expression};')
        lines.extend(['}', ''])
        dest = java_root / f'content/material/generated/{cls}.java'
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(align_java_tables('\n'.join(lines), material_colors=cls == 'CompoundMaterials'),
                        encoding='utf-8')
        facade += f'    public static final class {group} {{\n'
        leading = {row[0]: row for row in rows if group == 'Woods' and row[0] in WOOD_FACADE_FIELDS}
        for name in WOOD_FACADE_FIELDS:
            if name in leading:
                old, readable, _ = leading[name]
                facade += f'        public static final GTMaterial {old} = {package}.{cls}.{readable};\n'
        facade += f'        private {group}() {{}}\n'
        for old, readable, _ in rows:
            if old in leading:
                continue
            facade += f'        public static final GTMaterial {old} = {package}.{cls}.{readable};\n'
        facade += '    }\n\n'
    facade += '}\n'
    facade = replace_identifiers(facade, {'CS': 'GregTechConstants', 'MT': 'ImportedMaterialData'})
    facade = facade.replace('import static com.gregtech.gregtech.data.ImportedMaterialData.*;\n', '')
    # The facade contains references only; atomic constructors live in the holder.
    facade = facade.replace('import com.gregtech.gregtech.api.material.AtomicProperties;\n', '')
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
