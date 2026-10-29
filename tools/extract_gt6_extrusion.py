#!/usr/bin/env python3
"""Reproduce bounded native extrusion facts from GT6 3703e403 MT.java.

Usage: python tools/extract_gt6_extrusion.py --source /path/to/MT.java --check
Uses the existing source identity projection only for ID/name spelling, not flags.
Factory/direct flags are evaluated independently. Elemental low-melting overloads
are handled separately; steal/qual do not copy processing flags. Unknown source
versions fail closed instead of silently applying this bounded parser to new Java.
"""
from pathlib import Path
import re,json
import argparse, hashlib
ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source',type=Path,required=True)
parser.add_argument('--check',action='store_true')
args=parser.parse_args()
raw=args.source.read_bytes()
if hashlib.sha256(raw).hexdigest()!='12936831b1b5f419dc783c5f32f3cc07a329734ca6c5c11757d52e1be27904b2':
    raise SystemExit('Unreviewed MT.java version; expected GT6 3703e403')
src=raw.decode()
profiles={int(i):n for i,n in re.findall(r'(\d+)\|([^|\n]+)\|[01]{4}',
    (ROOT/'core/src/main/java/gregtech/content/material/SourceLatheMaterialProfiles.java').read_text())}
methods={}
# Factory bodies are not always one expression: MT.stone(...) spans several statements and
# carries the EXTRUDER/EXTRUDER_SIMPLE flags for every stone and brick material.
for m in re.finditer(r'static OreDictMaterial\s+(\w+)\s*\(([^)]*)\)\s*\{',src):
    depth=1; i=m.end()
    while i<len(src) and depth:
        if src[i]=='{':depth+=1
        elif src[i]=='}':depth-=1
        i+=1
    if depth==0: methods.setdefault(m[1],[]).append((m[2],src[m.end():i-1]))

flags=('EXTRUDER','EXTRUDER_SIMPLE','SMITHABLE')
def factoryflags(name,seen=()):
 if name in seen:return set()
 result=set()
 for params,body in methods.get(name,[]):
  # The threshold applies only to elemental overloads and is evaluated at declaration below.
  body=re.sub(r'aMeltingPoint <= 1200.*','',body)
  result|={f for f in flags if re.search(r'\b'+f+r'\b',body)}
  parent=re.search(r'return\s+(\w+)\s*\(',body)
  if parent:result|=factoryflags(parent[1],seen+(name,))
 return result
# Forge targets are independent of smelting. Copy the source target closure in call order,
# then let later explicit setForging(null, U) reset to self (e.g. TungstenSintered).
statements={}
for line in src.splitlines():
    declaration=re.match(r"\s*(\w+)\s*=",line)
    if declaration: statements[declaration[1]]=line

def forging(field, visiting=()):
    if field in visiting: raise SystemExit('Cyclic forging target copy: '+field)
    target,amount=field,'U'
    for call in re.finditer(r"\.(setForging|setAllToTheOutputOf)\(([^()]*)\)", statements.get(field,'')):
        args=[a.strip() for a in call[2].split(',')]
        if call[1]=='setForging':
            if len(args)!=2: raise SystemExit('Unreviewed forging setter')
            target,amount=(field if args[0]=='null' else args[0]),args[1]
        else:
            if len(args)!=1: raise SystemExit('Unreviewed target-copy scaling')
            if args[0]!='null': target,amount=forging(args[0],visiting+(field,))
    return target,amount
assert forging('RefinedGlowstone')==('Glowstone','U')
assert forging('RefinedObsidian')==('Obsidian','U')
assert forging('TungstenSintered')==('TungstenSintered','U')
assert forging('Brick')==('Ceramic','0')
assert forging('WoodTreated')==('Wood','U')
assert forging('WoodPolished')==('Wood','U')
assert forging('RefinedIron')==('Fe','U')
assert forging('MeatRaw')==('MeatCooked','U')
assert forging('IronMagnetic')==('Fe','U')
rows=[]
for line in src.splitlines():
 m=re.match(r'\s*(\w+)\s*=\s*(\w+)\s*\(',line)
 if not m:continue
 field,fac=m.groups(); statement=line
 # zero-argument elemental convenience methods carry the full declaration in their return.
 if re.match(r'\s*\)',line[m.end():]) and fac in methods:
  ret=re.search(r'return\s+(.+?);',methods[fac][0][1],re.S)
  if not ret:continue
  statement=ret.group(1).strip(); fac=re.match(r'(\w+)\s*\(',statement)[1]
 idname=re.search(r'\(\s*(\d+)\s*,\s*"([^"]+)"',statement)
 if not idname:continue
 ident=int(idname[1]);name=profiles.get(ident) or idname[2]

 f=factoryflags(fac)|{f for f in flags if re.search(r'\b'+f+r'\b',statement)}
 # Elemental metal overload: id,name,symbol,Z,N,melting.
 elem=re.search(r'\(\s*\d+\s*,\s*"[^"]+"\s*,\s*"[^"]*"\s*,\s*\d+\s*,\s*\d+\s*,\s*(\d+)',statement)
 if elem and 'EXTRUDER' in f and int(elem[1])<=1200:f.add('EXTRUDER_SIMPLE')
 if 'EXTRUDER' not in f:continue
 target,amount=forging(field)
 if target==field: target=''
 rows.append((ident,name,'EXTRUDER_SIMPLE' in f,'SMITHABLE' in f,target,amount,field,fac))
print('methods=%d profiles=%d simple=%d' % (len(methods),len(rows),sum(r[2] for r in rows)))
assert len(methods)==312 and len(rows)==414 and sum(r[2] for r in rows)==178, 'Source extraction changed'
positive={r[0]:r for r in rows}
content='''package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.recipe.generated.GTMaterialFields;
import java.util.HashMap;
import java.util.Map;

/** Native GT6 MT factory/direct extrusion flags and forging targets, pinned to 3703e403.
 * Generated by tools/extract_gt6_extrusion.py; absence is not inferred from the port's broad METAL flag.
 * Profiles are bound to exact native identities; addons are not assigned original identities by name.
 */
public final class SourceExtrusionRules {
    private SourceExtrusionRules() {}
    public record Profile(boolean extrudable, boolean simple, boolean smithable, String target, long forgingAmount) {}
    private record Row(String name, Profile profile) {}
    private static final Map<Integer, Row> ROWS = new HashMap<>();
    static {
        for (String line : DATA.strip().split("\\n")) {
            String[] f = line.strip().split("\\\\|", -1);
            ROWS.put(Integer.parseInt(f[0]), new Row(f[1], new Profile(f[2].charAt(0)=='1',
                    f[2].charAt(1)=='1', f[2].charAt(2)=='1', f[3], Long.parseLong(f[4]))));
        }
    }
    public static Profile profile(GTMaterial material) {
        if (material == null || !material.isValid() || material.resolve()!=material || AddonMaterials.owns(material)) return null;
        Row row=ROWS.get(material.getId());
        return row != null && sameName(row.name(), material.getName()) && GTMaterialRegistry.get(material.getId())==material
                && GTMaterialRegistry.get(material.getName())==material ? row.profile() : null;
    }
    /** The source list spells e.g. {@code SpaceStone} where the port spells {@code Space Stone}. */
    private static boolean sameName(String source, String port) {
        if (source.equals(port)) return true;
        return source.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase(port.replaceAll("[^A-Za-z0-9]", ""));
    }
    /** Source SMITHABLE where the profile exists; unknown/addon materials keep the port property. */
    public static boolean smithable(GTMaterial material) {
        Profile profile = profile(material);
        return profile != null ? profile.smithable() : material.has(gregtech.api.material.MaterialProperty.SMITHABLE);
    }
    public static GTMaterial target(GTMaterial material, Profile profile) {
        if (profile.target().isEmpty()) return material;
        String name=GTMaterialFields.materialOf(profile.target());
        GTMaterial result=name==null?null:GTMaterialRegistry.get(name);
        return result==null?null:result.resolve();
    }
    /** Original ceil input / floor output ratio, and the two 64-stack output limit. */
    public static long[] amounts(long inputWeight, long outputWeight) {
        if (inputWeight<=0 || outputWeight<=0) return null;
        long input=outputWeight>inputWeight?(outputWeight+inputWeight-1)/inputWeight:1;
        long output=outputWeight>inputWeight?1:inputWeight/outputWeight;
        if (input>64 || output<1) return null;
        return new long[]{input, Math.min(128,output), output};
    }
    public static long normalTicks(GTMaterial input, GTMaterial output, long inputUnits) {
        double kilograms=input.getDensity()*111.111111*inputUnits/GTValues.U;
        return Math.max(16,1+(long)Math.abs((output.getMeltingPoint()-293d)*kilograms/(75*96)));
    }
    public static long simpleTicks(long outputUnits) { return Math.max(1,(outputUnits*64+GTValues.U-1)/GTValues.U); }
    private static final String DATA="""
'''
for ident in sorted(set(profiles) | set(positive)):
 row=positive.get(ident)
 name=profiles.get(ident) or (row[1] if row else None)
 if not name:continue
 if row:flags='1'+str(int(row[2]))+str(int(row[3]));target=row[4];amount=648648000 if row[5]=='U' else int(row[5])
 else:flags='000';target='';amount=648648000
 content+=f'            {ident}|{name}|{flags}|{target}|{amount}\n'
content+='''            """;
}
'''
# DATA is referenced by static initializer: move constant before static block to avoid illegal forward reference.
start=content.index('    private static final String DATA='); end=content.index('    static {')
content=content[:end]+content[start:content.rfind('\n}')]+ '\n'+content[end:start]+'}\n'
output=ROOT/'core/src/main/java/gregtech/content/recipe/SourceExtrusionRules.java'
if args.check:
    # Spotless formats and reorders imports; compare the class body, not layout or import order.
    def body(text): return re.sub(r'\s+','',text[text.index('public final class'):])
    if body(output.read_text())!=body(content):
        raise SystemExit('Extrusion facts differ')
else: output.write_text(content)
print('Verified extrusion facts: %d eligible / %d simple native profiles' % (len(rows),sum(r[2] for r in rows)))
