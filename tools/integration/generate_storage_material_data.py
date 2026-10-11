"""Resolve adopted metalset storage CR.REV data and registration metadata.

Sources are read-only. Native families include recursively known charging cables
and logistics covers. Missing automatic ore data is retained without guessing.
"""
from pathlib import Path
from collections import Counter
import argparse, hashlib, json, re, sys
REPO=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(REPO/'tools/integration'))
from generate_machine_material_data import masked, norm, calls, recipe, amount, java_material, U, split_top_level

FAMILIES={0:'chest_',500:'reinforced_wood_chest_',2000:'safe_',3000:'key_safe_',
          4000:'drawer_quad_',6000:'mass_storage_',8600:'bottle_crate_',
          5000:'advanced_crafting_table_',5500:'charging_crafting_table_',6200:'logistics_mass_storage_',
          7100:'bookshelf_metal_',8400:'scaffold_'}
GROUPS={'aMetal':('pickaxe',False),'aWooden':('axe',True),'aMachine':('wrench',False),'aUtilMetal':('pickaxe',True)}

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--source',type=Path,required=True)
 ap.add_argument('--out',type=Path,required=True);ap.add_argument('--audit',type=Path,required=True);ns=ap.parse_args()
 relatives=['src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java','src/main/java/gregapi/data/OP.java',
            'src/main/java/gregapi/load/LoaderOreDictReRegistrations.java','src/main/java/gregapi/data/ANY.java',
            'src/main/java/gregapi/data/MT.java','src/main/java/gregtech/items/MultiItemTechnological.java','LICENSE']
 texts=[masked((ns.source/p).read_text(encoding='utf-8')) for p in relatives[:-1]]
 mte,op,automatic,any_source,mt,tech=texts
 weights={m[1]:amount(m[2].split(',')[0]) for m in re.finditer(r'^\s*(\w+)\s*=\s*create\([^\n]*?\.setMaterialStats\(([^)]*)\)',op,re.M)}
 chest=list(calls(automatic,'OreDictManager.INSTANCE.setAutomaticItemData'))
 chest=[a for _,a in chest if norm(a[0])=='OD.craftingChest']
 assert len(chest)==1 and norm(chest[0][1])=='newOreDictItemData(ANY.Wood,U*4)'
 assert re.search(r'Wood\s*\.[^\n]*setAllToTheOutputOf\(\s*MT.Wood\s*\)',any_source)
 aliases={m[1]:norm(m[2]) for m in re.finditer(r'^\s*(\w+)\s*\.[^\n]*?\.setAllToTheOutputOf\(([^)]*)\)',any_source,re.M)}
 arrays={m[1]:split_top_level(m[2]) for m in re.finditer(r'(\w+)\s*=\s*\{([^{}]*)\}',mt)}
 byproducts={}
 for m in re.finditer(r'(\w+)\s*\.mByProducts.add\(OM.stack\(([^,]+),\s*([^)]*)\)\)',op):
  byproducts[m[1]]=(norm(m[2]),amount(re.sub(r'(\w+)\.mAmount',lambda p:str(weights[p[1]]),m[3])))
 def material(value):
  value=norm(value)
  if value=='aMat':return 'metal'
  if value.startswith('ANY.'):return material(aliases[value[4:]])
  return value.removeprefix('MT.')
 direct={'OD.craftingChest':Counter(Wood=4*U)}
 item_recipes={};item_origins={}
 adopted_items={'IL.Cover_Blank','IL.Cover_Logistics_Generic_Storage','IL.Processor_Crystal_Emerald'}
 for pos,a in calls(tech,'CR.shaped'):
  if len(a)<3 or 'REV' not in a[1]:continue
  output=re.fullmatch(r'(IL\.\w+)\.get\((\d+)\)',norm(a[0]))
  if output and output[1] in adopted_items:
   assert output[1] not in item_recipes
   item_recipes[output[1]]=(*recipe(a[2:]),int(output[2]))
   item_origins[output[1]]=tech[:pos].count('\n')+1
 for key in adopted_items:
  for _,a in calls(norm(tech),key+'.set'):
   for _,parts in calls(a[0],'newOreDictItemData'):
    value=Counter()
    for i in range(0,len(parts),2):value[material(parts[i])]+=amount(parts[i+1])
    direct[key]=value
 assert set(item_recipes)=={'IL.Cover_Blank','IL.Cover_Logistics_Generic_Storage'}
 assert 'IL.Processor_Crystal_Emerald' in direct
 # CR uses automatic data for these strings, not the selected tag member's data.
 unknown_keys={'OD.craftingWorkBench','OD_CIRCUITS[4]'}
 automatic_keys={norm(a[0]) for _,a in calls(automatic,'OreDictManager.INSTANCE.setAutomaticItemData')}
 assert not (unknown_keys & automatic_keys) and '"gt:circuit4"' not in automatic_keys
 end=mte.index('\n\t}',mte.index('private static void metalset('))
 method=mte[:end][mte.index('private static void metalset('):]
 registrations={}; family_rows={}
 for pos,a in calls(method,'aRegistry.add'):
  expr=norm(a[2]);offset=0 if expr=='aID' else int(expr.removesuffix('+aID'))
  if offset not in FAMILIES:continue
  patterns,keys=recipe(a[9:]);tool,hand=GROUPS[norm(a[7])]
  registrations[offset]=(patterns,keys)
  family_rows[offset]=dict(source_offset=offset,pattern=patterns,ingredients=keys,harvest=norm(a[5]),
                           group=norm(a[7]),tool=tool,hand=hand)
 assert set(registrations)==set(FAMILIES)
 def inputs(patterns,keys,ancestry=()):
  totals=Counter();missing=Counter()
  for char,count in Counter(''.join(patterns)).items():
   if char not in keys:continue # CR tools are lowercase implicit ingredient keys.
   parts,unknown=resolve(keys[char],ancestry)
   for mat,value in parts.items():totals[mat]+=value*count
   for key,value in unknown.items():missing[key]+=value*count
  return totals,missing
 def resolve(expression,ancestry=()):
  expr=norm(expression)
  assert expr not in ancestry, 'Source material cycle: '+expr
  next_stack=ancestry+(expr,)
  array=re.fullmatch(r'MT.DATA.(\w+)\[(\d+)\]',expr)
  if array:return resolve(arrays[array[1]][int(array[2])],next_stack)
  prefix=re.fullmatch(r'OP\.(\w+)\.dat\((.+)\)',expr)
  if prefix:
   parts=Counter({material(prefix[2]):weights[prefix[1]]})
   if prefix[1] in byproducts:
    mat,value=byproducts[prefix[1]];parts[material(mat)]+=value
   return parts,Counter()
  if expr in direct:return direct[expr].copy(),Counter()
  if expr in item_recipes:
   patterns,keys,count=item_recipes[expr];parts,missing=inputs(patterns,keys,next_stack)
   return Counter({mat:value//count for mat,value in parts.items() if value//count>0}),missing
  previous=re.fullmatch(r'aRegistry.getItem\((?:(\d+)\+)?aID\)',expr)
  if previous:return inputs(*registrations[int(previous[1] or 0)],next_stack)
  if expr in unknown_keys:return Counter(),Counter({expr:1})
  raise ValueError('Unexpected adopted storage input '+expr)
 for offset,row in family_rows.items():
  parts,missing=inputs(*registrations[offset]);row['material_units']=dict(parts);row['unknown_automatic_inputs']=dict(missing)
 catalog=(REPO/'core/src/main/java/com/gregtech/gregtech/registry/GTStorageMetals.java').read_text(encoding='utf-8')
 suffixes=re.findall(r'new Spec\("([^"]+)"',catalog)
 metalsets=[(pos,a) for pos,a in calls(mte,'metalset') if len(a)==10 and norm(a[5]).startswith(('MT.','ANY.'))]
 assert len(metalsets)==len(suffixes)==60
 shelves=json.loads((REPO/'core/src/main/resources/data/gregtech/bookshelf_variants.json').read_text(encoding='utf-8'))['variants']
 shelves={v['gt6_id']:v for v in shelves if v['kind']=='metal'}
 assert len(shelves)==60
 rows=[]
 for suffix,(pos,a) in zip(suffixes,metalsets):
  original=norm(a[5]);target=aliases[original[4:]] if original.startswith('ANY.') else original
  rows.append(dict(suffix=suffix,source_id=int(a[6]),source_material=original,
                   java_material=java_material(target.removeprefix('MT.')),source_line=mte[:pos].count('\n')+1,
                   bookshelf_path=shelves[7100+int(a[6])]['path']))
 java=['''/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Loader_MultiTileEntities.metalset, CR.REV, OP, MT.DATA, technological items and
 * automatic craftingChest data. Unknown automatic inputs retain their source absence.
 * Generated by tools/integration/generate_storage_material_data.py. */
package com.gregtech.gregtech.content.machine;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.data.ImportedMaterialData;
import com.gregtech.gregtech.data.SourceBlockProperties;
import java.util.*;

/** Original storage recipes, source IDs and explicit block-group/metadata choices. */
public final class OriginalStorageMaterialData {
    private OriginalStorageMaterialData() {}
    private static final Map<String,ItemComposition> DATA = new LinkedHashMap<>();
    private static final Map<String,SourceBlockProperties.Params> HARVEST = new LinkedHashMap<>();
    static {
''']
 for row in rows:java.append(f'        metalset("{row["suffix"]}",{row["source_id"]},{row["java_material"]},"{row["bookshelf_path"]}");\n')
 java.append('''    }
    public static Map<String,ItemComposition> blocks() { return Collections.unmodifiableMap(DATA); }
    public static Map<String,SourceBlockProperties.Params> harvest() { return Collections.unmodifiableMap(HARVEST); }
    private static void add(String path,int id,GTMaterial metal,String tool,int explicitLevel,boolean hand,MaterialComponent... parts) {
        if (DATA.put(path,ReversibleCraftingData.perItem(List.of(parts),1,"GT6 original metalset CR.REV storage"+id)) != null)
            throw new IllegalStateException("Duplicate original storage identity: "+path);
        HARVEST.put(path,new SourceBlockProperties.Params(id,tool,metal,explicitLevel,hand));
    }
    private static void metalset(String suffix,int id,GTMaterial metal,String bookshelfPath) {
''')
 for offset,prefix in FAMILIES.items():
  v=family_rows[offset];units=v['material_units'];expr=f'"{prefix}"+suffix'
  if offset==2000:expr='suffix.equals("steel")?"safe":'+expr
  if offset==4000:expr='suffix.equals("stainless_steel")?"drawer_quad":'+expr
  if offset in (5000,5500,8400):expr=f'suffix.equals("steel")?"{prefix[:-1]}":'+expr
  if offset==7100:expr='bookshelfPath'
  level=-1 if v['harvest']=='aMat.mToolQuality' else int(v['harvest'])
  parts=','.join(f'MaterialComponent.of({"metal" if mat=="metal" else java_material(mat)},{value}L)' for mat,value in units.items())
  java.append(f'        add({expr},id+{offset},metal,"{v["tool"]}",{level},{str(v["hand"]).lower()},{parts});\n')
 java.append('    }\n}\n')
 identities=len(rows)*len(FAMILIES)
 audit=dict(families=family_rows,metalsets=rows,identities=identities,item_recipe_lines=item_origins,
             known_item_data={key:dict(resolve(key)[0]) for key in sorted(adopted_items)},
             source_files=[dict(path=str(ns.source/p),sha256=hashlib.sha256((ns.source/p).read_bytes()).hexdigest()) for p in relatives],
             authors=['GregTech-6 Team','Gregorius Techneticies'],license='LGPL-3.0-or-later',
             boundaries='Twelve existing native metalset families; exact original known REV units, cables with insulation and recursive logistics cover/processor data. Unknown workbench/circuit automatic data stays absent. Normal/charging lockers and unrelated legacy IDs excluded; native paths/source IDs and materials verified by shared source contracts.')
 ns.out.parent.mkdir(parents=True,exist_ok=True);ns.out.write_text(''.join(java),encoding='utf-8')
 ns.audit.parent.mkdir(parents=True,exist_ok=True);ns.audit.write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print(json.dumps(dict(identities=identities,families=len(FAMILIES),metalsets=len(rows),units={k:v['material_units'] for k,v in family_rows.items()})))
if __name__=='__main__':main()
