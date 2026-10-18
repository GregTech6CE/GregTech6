"""Extract GT6 raw clay mold crafting and firing masks from read-only sources.

Original authors: Gregorius Techneticies / GregTech 6 team (LGPL).
No guessed geometric recipes: retain each original row and its line number.
"""
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT.parent / 'gregtech6-master/gregtech6-master/src/main/java'
RANDOM = SRC / 'gregtech/items/MultiItemRandomTools.java'
MTE = SRC / 'gregtech/loaders/b/Loader_MultiTileEntities.java'
MT = SRC / 'gregapi/data/MT.java'
ANY = SRC / 'gregapi/data/ANY.java'
MOLD = SRC / 'gregtech/tileentity/tools/MultiTileEntityMold.java'
LINES = RANDOM.read_text(encoding='utf-8').splitlines()
OUT = ROOT / 'core/src/main/resources/data/gt6ce/recipes/clay_molds'
JAVA = ROOT / 'core/src/main/java/com/gregtech/gregtech/content/recipe'
ROWS = {}
FORMS = {}

def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists() or path.read_text(encoding='utf-8') != text:
        path.write_text(text, encoding='utf-8')

def item(name): return {'item': name if ':' in name else 'gt6ce:' + name}
def tag(name): return {'tag': name if ':' in name else 'gt6ce:' + name}

raw = {}
for line in LINES[79:110] + LINES[112:120]:
    m = re.search(r'IL\.(Ceramic_\w+_Mold_Raw)\s*\.set\(addItem\(\d+, "([^"]+)"', line)
    if m:
        raw[m[1]] = re.sub(r'[^a-z0-9]+', '_', m[2].lower().replace("'", '')).strip('_')
raw['Ceramic_Mold_Raw'] = 'clay_mold'
assert len(raw) == 31, len(raw)

tools = dict(FILE='file', SWORD='sword', PICKAXE='pickaxe', GEM_PICK='gem_tipped_pickaxe',
             CONSTRUCTION_PICK='construction_pick', SPADE='spade', SHOVEL='shovel',
             UNIVERSALSPADE='universal_spade', AXE='axe', DOUBLE_AXE='double_axe',
             SAW='saw', SOFTHAMMER='soft_hammer', HARDHAMMER='hammer',
             SCREWDRIVER='screwdriver', CHISEL='chisel', HOE='hoe', SENSE='sense',
             PLOW='plow', BUILDERWAND='builder_wand')

def form(prefix, material='all'):
    key = prefix + '/' + material
    FORMS[key] = (prefix, material)
    return tag('molds/forms/' + key.lower())

def ingredient(expr, material='all', loop_prefix=None):
    expr = expr.strip()
    if expr == 'IL.Ceramic_Mold_Raw': return item('clay_mold')
    if expr.startswith('Items.'): return item('minecraft:' + expr[6:])
    if expr == 'OD.itemMudBrick': return tag('molds/mud_bricks')
    if expr == 'OD.paneGlass': return tag('forge:glass_panes')
    if expr == 'OD.plankAnyWood': return tag('minecraft:planks')
    if expr == 'OD.itemFlint': return item('minecraft:flint')
    if expr == 'tItem': return form(loop_prefix)
    m = re.fullmatch(r'ToolsGT.sMetaTool.make\(ToolsGT.(\w+)\)', expr)
    if m: return item('tool_' + tools[m[1]])
    m = re.fullmatch(r'OP.(\w+).dat\((MT.\w+|tMat)\)', expr)
    if m: return form(m[1], material if m[2] == 'tMat' else m[2][3:])
    raise ValueError(expr)

def add(name, line, inputs, output, count=1, kind='gt6ce:tool_shapeless'):
    assert name not in ROWS, name
    ROWS[name] = {'type': kind, '_comment': f'GT6 MultiItemRandomTools:{line}',
                  'ingredients': inputs, 'result': {**item(output), 'count': count}}

for n, line in enumerate(LINES, 1):
    m = re.search(r'IL\.(Ceramic_\w+_Mold_Raw|Ceramic_Mold_Raw)\s*\.set\(addItem', line)
    if m:
        assert 'CR.shapeless(ST.make(Items.clay_ball, 5, 0)' in line, n
        name = raw[m[1]]
        add('reclaim_' + name, n, [item(name)], 'minecraft:clay_ball', 5,
            'minecraft:crafting_shapeless')

line = LINES[126]
assert 'IL.Ceramic_Mold_Raw' in line and '"C C", "CCC", "k R"' in line
ROWS['form_clay_mold'] = {'type': 'gt6ce:tool_shaped', '_comment': 'GT6 MultiItemRandomTools:127',
    'allow_mirror': False, 'pattern': ['C C', 'CCC', 'k R'],
    'key': {'C': tag('forge:clay_balls'), 'k': tag('forge:tools/knife'),
            'R': tag('forge:tools/rolling_pin')}, 'result': item('clay_mold')}

materials = ['WoodPlastic', 'Stone', 'Glass', 'Wax', 'Iron', 'Cu', 'Sn', 'Zn',
             'Pb', 'Bi', 'Brass', 'Bronze', 'ArsenicCopper', 'ArsenicBronze', 'BismuthBronze', 'Au']
groups = {'WoodPlastic', 'Stone', 'Wax', 'Iron', 'Cu'}
for n, line in enumerate(LINES[136:233], 137):
    m = re.search(r'CR.shapeless\(IL\.(Ceramic_\w+_Mold_Raw)\s*\.get\(1\), CR.DEF_NCC, new Object\[\] \{(.+)\}\);', line)
    if not m: continue
    prefix = re.search(r'for \(IPrefixItem tItem : OP\.(\w+)', line)
    exprs = [x.strip() for x in m[2].split(',')]
    for material in materials if 'tMat' in line else ['all']:
        material_key = ('Any' if material in groups else '') + material
        inputs = [ingredient(e, material_key, prefix[1] if prefix else None) for e in exprs]
        add(f'imprint_{n}_{material_key.lower()}', n, inputs, raw[m[1]])

masks = {'clay_mold': 0}
firing_sources = {'clay_mold': 352}
for n, line in enumerate(MTE.read_text(encoding='utf-8').splitlines(), 1):
    m = re.search(r'RM.add_smelting\(IL\.(Ceramic_\w+_Mold_Raw)\s*\.get\(1\).*"gt.mold", (0b[01_]+)', line)
    if m:
        assert 'F, F, T' in line
        masks[raw[m[1]]] = int(m[2].replace('_', ''), 2)
        firing_sources[raw[m[1]]] = n
assert set(masks) == set(raw.values()), (set(raw.values()) - set(masks))

for name, row in ROWS.items():
    write(OUT / (name + '.json'), json.dumps(row, ensure_ascii=False, indent=2) + '\n')
raw_rows = ',\n'.join(f'            new Raw("{name}", {mask})' for name, mask in masks.items())
forms = ',\n'.join(f'            new Form("{prefix}", "{material}")' for prefix, material in sorted(FORMS.values()))
files = ',\n'.join('            "clay_molds/' + name + '.json"' for name in sorted(ROWS))
# MT factories attach members to ANY.Stone / ANY.Wax; the older generic group transpiler omitted these.
# Keep exact source membership for these recipe ingredients without modifying unrelated material aliases.
refs = {}
owner = ''
for line in (ROOT / 'core/src/main/java/com/gregtech/gregtech/data/generated/GT6Materials.java').read_text(encoding='utf-8').splitlines():
    m = re.search(r'public static final class (\w+)', line)
    if m: owner = m[1]
    m = re.search(r'public static final GTMaterial (\w+) =', line)
    if m: refs[m[1]] = 'GT6Materials.' + owner + '.' + m[1]
factory_members = {}
for family, factories in [('STONE', ['stone', 'stonedcmp', 'stonecent', 'stoneelec']), ('WAX', ['wax'])]:
    names = re.findall(r'\b(\w+)\s*=\s*(?:' + '|'.join(factories) + r')\s*\(\s*[-\d]+\s*,\s*"[^"]+"', MT.read_text(encoding='utf-8'))
    assert names and all(name in refs for name in names), family
    factory_members[family] = names
    refs[family] = ',\n                '.join(refs[name] for name in names)
catalog = '''package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialGroups;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.GT6Materials;
import java.util.List;

/** Generated from original GT6 MultiItemRandomTools and MTE firing rows; LGPL, original authors retained. */
public final class ClayMoldCatalog {
    private ClayMoldCatalog() {}
    public static final String SHAPE_KEY = "gt.mold.shape";
    public record Raw(String id, int shape) {}
    public record Form(String prefix, String family) {
        public String tag() { return ("molds/forms/" + prefix + "/" + family).toLowerCase(java.util.Locale.ROOT); }
        public boolean accepts(MaterialPrefix actual, GTMaterial material) {
            // OP.casingSmall is the original spelling of the port's itemCasing prefix.
            String name = prefix.equals("casingSmall") ? "itemCasing" : prefix;
            if (actual != PrefixRegistry.byName(name)) return false;
            if (family.equals("all")) return true;
            GTMaterial group = switch (family) {
                case "AnyWoodPlastic" -> MaterialGroups.WoodPlastic;
                case "AnyStone" -> MaterialGroups.Stone;
                case "AnyWax" -> MaterialGroups.Wax;
                case "AnyIron" -> MaterialGroups.Iron;
                case "AnyCu" -> MaterialGroups.Cu;
                default -> null;
            };
            if (group != null) {
                if (group.getReRegistrations().stream().anyMatch(m -> m.resolve() == material.resolve())) return true;
                return switch (family) {
                    case "AnyStone" -> FactoryFamilies.STONE.contains(material.resolve());
                    case "AnyWax" -> FactoryFamilies.WAX.contains(material.resolve());
                    // ANY:145 nests ANY.Plastic; original re-registration propagates that family.
                    case "AnyWoodPlastic" -> MaterialGroups.Plastic.getReRegistrations().stream()
                            .anyMatch(m -> m.resolve() == material.resolve());
                    default -> false;
                };
            }
            String concrete = switch (family) {
                case "Cu" -> "Copper"; case "Sn" -> "Tin"; case "Zn" -> "Zinc";
                case "Pb" -> "Lead"; case "Bi" -> "Bismuth"; case "Au" -> "Gold";
                default -> family;
            };
            return GTMaterialRegistry.get(concrete).resolve() == material.resolve();
        }
    }
    private static final class FactoryFamilies {
        private static java.util.Set<GTMaterial> members(GTMaterial... values) {
            return java.util.Arrays.stream(values).filter(GTMaterial::isValid).map(GTMaterial::resolve)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        static final java.util.Set<GTMaterial> STONE=members(
                %STONE%
        );
        static final java.util.Set<GTMaterial> WAX=members(
                %WAX%
        );
    }
    public static final List<Raw> RAW = List.of(
%RAW%
    );
    public static final List<Form> FORMS = List.of(
%FORMS%
    );
    public static final List<String> FILES = List.of(
%FILES%
    );
}
'''.replace('%RAW%', raw_rows).replace('%FORMS%', forms).replace('%FILES%', files).replace('%STONE%', refs['STONE']).replace('%WAX%', refs['WAX'])
write(JAVA / 'ClayMoldCatalog.java', catalog)
report = {'scope': 'source extraction only; not Minecraft registration or gameplay',
    'author': 'Gregorius Techneticies / GregTech 6 team', 'license': 'LGPL; repository NOTICE retained',
    'sources': [{'path': str(p), 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()} for p in [RANDOM, MTE, MT, ANY, MOLD]],
    'factory_families': factory_members,
    'crafting_rows': len(ROWS), 'material_forms': len(FORMS),
    'firing': [{'id': name, 'shape': mask, 'source_line': firing_sources[name]} for name, mask in masks.items()],
    'recipes': {name: {'source': row['_comment'], 'sha256': hashlib.sha256((OUT/(name+'.json')).read_bytes()).hexdigest()}
                for name, row in ROWS.items()},
    'limitations': ['itemMudBrick has no current native item binding; external tag slot retained',
                    'Original ore dictionary registration callbacks are represented by native form tags']}
write(ROOT / 'work/clay-molds-source-20261004.json', json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print(f'Extracted {len(ROWS)} crafting rows, {len(FORMS)} material form tags, {len(masks)} firing masks.')
