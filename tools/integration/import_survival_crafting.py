"""Import the missing survival crafting rows from a read-only original GT6 tree.

Original LGPL-3.0-or-later attribution is retained in the generated catalog. Source
locations and unresolved expressions are emitted for review; never guess a key.
"""
from pathlib import Path
import argparse,sys,re,json
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT / 'tools'))
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source',type=Path,required=True,help='Original GT6 src/main/java directory')
parser.add_argument('--report',type=Path)
parser.add_argument('--check',action='store_true')
args=parser.parse_args()
from extract_gt6_block_recipes import add_calls,split_args
source=(args.source / 'gregtech/loaders/b/Loader_MultiTileEntities.java').read_text(encoding='utf-8')
props=(ROOT / 'core/src/main/java/com/gregtech/gregtech/data/SourceBlockProperties.java').read_text(encoding='utf-8')
ids={int(n):(p,m) for p,n,m in re.findall(r'BLOCKS.put\("([^"]+)", new Params\((\d+),"[^"]+",(ImportedMaterialData\.[\w.]+)',props)}
smelt=(ROOT / 'core/src/main/java/com/gregtech/gregtech/api/machine/OriginalSmelteryDefinitions.java').read_text(encoding='utf-8')
ids.update({int(n):(p,m) for p,n,m in re.findall(r'add\("([^"]+)", (\d+), (ImportedMaterialData\.[\w.]+)',smelt)})
wanted=lambda p:p.startswith(('mold_','crucible_crossing_','crucible_faucet_','burning_box_gas_','spike_')) or p in {'storage_quadcore_processor_unit','bedrock_mining_drill_head','lightning_rod','coke_oven_wall','tank_plastic'}
tools={'h':'hammer','w':'wrench','y':'chisel','s':'saw','f':'file','x':'wire_cutter','d':'screwdriver','k':'knife'}
rows=[];missing=[];evidence=[]
for line,txt in enumerate(source.splitlines(),1):
 if txt.lstrip().startswith('//'):continue
 for call in add_calls(txt):
  a=split_args(call)
  if len(a)<9 or not a[2].isdigit() or int(a[2]) not in ids:continue
  path,mat=ids[int(a[2])]
  if not wanted(path):continue
  patterns=[v[1:-1] for v in a[9:] if re.fullmatch(r'"[a-zA-Z0-9 ]{1,3}"',v)]
  if not patterns:continue
  keys={};bad=[]
  for i,v in enumerate(a):
   if not re.fullmatch("'.'",v):continue
   exp=a[i+1];key=v[1];m=re.fullmatch(r'OP\.(\w+)\.dat\(([^)]+)\)',exp)
   if m:
    form,material=m.groups();prefix='block' if form.startswith('casingMachine') else 'wire' if form.startswith('wireGt') else 'mat'
    if prefix=='wire':form=str(int(form[-2:]))
    if material=='aMat':spec=prefix+':'+form
    else:
     names={'ANY.Cu':'Copper','ANY.Diamond':'Diamond','MT.Ir':'Iridium','MT.NiobiumTitanium':'NiobiumTitanium','MT.Brick':'Brick','ANY.Clay':'Clay','MT.Graphene':'Graphene','ANY.Quartz':'NetherQuartz','MT.Ceramic':'Ceramic','ANY.Plastic':'Plastic'}
     if material not in names:bad.append(exp);continue
     spec=prefix+':'+form+'@'+names[material]
    if form=='stone':
     stone=mat.rsplit('.',1)[-1];slugs={'Basalt':'basalt','GraniteBlack':'granite_black','GraniteRed':'granite_red','Umber':'umber','Livingrock':'livingrock','Holystone':'holystone','Betweenstone':'betweenstone','NetherBrick':'nether_brick'}
     spec='item:minecraft:nether_bricks' if stone=='NetherBrick' else 'item:gregtech:stone_'+slugs.get(stone,stone.lower())+'_stone'
    keys[key]=spec
   elif exp in {'Blocks.stone','Blocks.brick_block'}:keys[key]='item:minecraft:'+{'Blocks.stone':'stone','Blocks.brick_block':'bricks'}[exp]
   elif exp=='OD_CIRCUITS[6]':keys[key]='circuit:6'
   elif exp=='IL.Processor_Crystal_Emerald':keys[key]='item:gregtech:crystal_processor_emerald'
   elif exp=='OD.container1000water':keys[key]='item:minecraft:water_bucket'
   else:bad.append(exp)
  for ch in set(''.join(patterns))-{' '}-set(keys):
   if ch in tools:keys[ch]='item:gregtech:tool_'+tools[ch]
   else:bad.append(ch)
  if bad:missing.append([path,bad]);continue
  if path=='coke_oven_wall':path='fire_bricks'
  keysrc=', '.join("'%s', \"%s\""%(k,v) for k,v in sorted(keys.items()))
  rows.append(f'        new Row("{path}", 1, {mat}, "'+ '|'.join(patterns)+f'", Map.of({keysrc}))')
  evidence.append({'id':path,'line':line,'source_id':int(a[2])})
# Key crafting is identical in all ten original MultiItemRandomTools registrations.
for material in ['Iron','Gold','Copper','Tin','Bronze','Brass','Silver','Platinum','Lead','Plastic']:
 rows.append(f'        new Row("{material.lower()}_key", 3, com.gregtech.gregtech.api.material.GTMaterialRegistry.get("{material}"), "fPx", Map.of(\'f\', "item:gregtech:tool_file", \'P\', "mat:plate", \'x\', "item:gregtech:tool_wire_cutter"))')
for size,form in [('small','Double'),('medium','Triple'),('large','Quadruple')]:
 rows.append(f'        new Row("press_bullet_casing_shape_{size}", 1, com.gregtech.gregtech.content.material.Materials.Steel, "TPT|dyh|SPS", Map.of(\'T\', "mat:screw", \'P\', "mat:plate{form}", \'S\', "mat:stick", \'d\', "item:gregtech:tool_screwdriver", \'y\', "item:gregtech:tool_chisel", \'h\', "item:gregtech:tool_hammer"))')
out='''/* Original GT6 Loader_MultiTileEntities and MultiItemRandomTools; LGPL-3.0-or-later. */
package com.gregtech.gregtech.data;
import com.gregtech.gregtech.api.material.GTMaterial;
import java.util.List;
import java.util.Map;
/** Source crafting rows omitted by the earlier recipe import. */
public final class OriginalSurvivalRecipes {
    private OriginalSurvivalRecipes() {}
    public record Row(String id, int count, GTMaterial material, String pattern, Map<Character,String> keys) {
        public MultiblockCraftingRecipes.Entry entry() { return new MultiblockCraftingRecipes.Entry(id, id, pattern, keys); }
    }
    public static final List<Row> ROWS = List.of(
'''+',\n'.join(rows)+'\n    );\n}\n'
destination=ROOT / 'core/src/main/java/com/gregtech/gregtech/data/OriginalSurvivalRecipes.java'
if missing:
 raise ValueError(f'Unresolved original ingredients: {missing}')
if args.check:
 assert destination.read_text(encoding='utf-8')==out, 'Regenerate original survival catalog'
else:
 destination.write_text(out,encoding='utf-8')
if args.report:
 args.report.parent.mkdir(parents=True,exist_ok=True)
 args.report.write_text(json.dumps({'multiTileRows':evidence,'randomToolKeys':10,'technologicalBulletMolds':3,'total':len(rows),'unresolved':missing},indent=2)+'\n',encoding='utf-8')
print('recipes',len(rows),'unresolved',missing)
