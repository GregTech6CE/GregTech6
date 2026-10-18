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
# Missing utility registrations, including source rows whose harvest table had no material.
for source_id,path,material in [
 (32711,'advanced_button','TinAlloy'), (32701,'coin_mold','Steel'),
 (32065,'plant_pot','Ceramic'), (32704,'dust_funnel','Steel'),
 (32703,'grindstone_block','Steel'), (32702,'sifting_table','Steel'),
 (32709,'ender_garbage_bin','Steel'), (32737,'fluid_thermos','Al'),
 (32758,'greg_lantern','Wood'), (31019,'sensor_tachometer','TinAlloy'),
 (31015,'sensor_electrometer','TinAlloy'), (32111,'crank','Fe'),
 *[(10151+i,'laser_absorber_'+tier,mat) for i,(tier,mat) in enumerate(zip(
   ['lv','mv','hv','ev','iv'], ['SteelGalvanized','Al','StainlessSteel','Cr','Ti']))],
 *[(15001+i,'auto_hammer_'+name,mat) for i,(name,mat) in enumerate(zip(
   ['steel','aluminium','titanium','tungsten'], ['Bronze','Steel','Ti','TungstenSteel']))],
 *[(15010+i,'auto_igniter_'+name,mat) for i,(name,mat) in enumerate(zip(
   ['steel','aluminium','stainless','titanium','tungsten','ultimet'], ['TinAlloy','SteelGalvanized','Al','StainlessSteel','Cr','Ti']))]]:
 ids[source_id]=(path,'com.gregtech.gregtech.data.generated.GT6Materials.Woods.Wood' if material=='Wood' else 'ImportedMaterialData.'+material)
extra_ids={p for n,(p,m) in ids.items() if n in {32711,32701,32065,32704,32703,32702,32709,32737,32758,31019,31015,10050,10051}}
wanted=lambda p:p in extra_ids or p.startswith(('mold_','crucible_crossing_','crucible_faucet_','burning_box_gas_','spike_','laser_absorber_','auto_hammer_','auto_igniter_')) or p in {'crank','storage_quadcore_processor_unit','bedrock_mining_drill_head','lightning_rod','coke_oven_wall','tank_plastic'}
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
   exp=a[i+1];key=v[1]
   if key not in ''.join(patterns):continue
   m=re.fullmatch(r'OP\.(\w+)\.dat\(([^)]+)\)',exp)
   if m:
    form,material=(x.strip() for x in m.groups());prefix='block' if form.startswith('casingMachine') else 'wire' if form.startswith('wireGt') else 'cable' if form.startswith('cableGt') else 'pipe' if form.startswith('pipe') else 'mat'
    if prefix in {'wire','cable'}:form=str(int(form[-2:]))
    if prefix=='pipe':form=form.removeprefix('pipe').lower()
    if material=='aMat':spec=prefix+':'+form
    else:
     names={'ANY.Cu':'Copper','ANY.Diamond':'Diamond','MT.Ir':'Iridium','MT.NiobiumTitanium':'NiobiumTitanium','MT.Brick':'Brick','ANY.Clay':'Clay','MT.Graphene':'Graphene','ANY.Quartz':'NetherQuartz','MT.Ceramic':'Ceramic','ANY.Plastic':'Plastic','ANY.Iron':'Iron','ANY.Si':'Silicon','MT.Ge':'Germanium','MT.AnnealedCopper':'AnnealedCopper','MT.TinAlloy':'TinAlloy','MT.RedAlloy':'RedAlloy','MT.Lumium':'Lumium','MT.Brass':'Brass','MT.Ag':'Silver','ANY.Stone':'Stone','MT.Bone':'Bone','MT.Endstone':'Endstone','MT.SoulSand':'SoulSand','MT.Sand':'Sand','MT.Obsidian':'Obsidian','MT.EnderEye':'EnderEye'}
     names['ANY.Steel']='Steel'
     if material not in names:bad.append(exp);continue
     spec=prefix+':'+form+'@'+names[material]
    if form=='stone':
     stone=mat.rsplit('.',1)[-1];slugs={'Basalt':'basalt','GraniteBlack':'granite_black','GraniteRed':'granite_red','Umber':'umber','Livingrock':'livingrock','Holystone':'holystone','Betweenstone':'betweenstone','NetherBrick':'nether_brick'}
     spec='item:minecraft:nether_bricks' if stone=='NetherBrick' else 'item:gt6ce:stone_'+slugs.get(stone,stone.lower())+'_stone'
    if material=='ANY.Steel' and form=='bolt':spec='tag:gt6ce:technology/any_steel_bolt'
    if material=='ANY.Iron' and form in {'gearGt','stick','stickLong'}:
     spec='tag:gt6ce:technology/any_iron_'+{'gearGt':'gear','stick':'rod','stickLong':'long_rod'}[form]
    if material=='ANY.Si' and form=='plateGem':spec='tag:gt6ce:technology/any_silicon_gem_plate'
    keys[key]=spec
   elif exp in {'Blocks.stone','Blocks.brick_block'}:keys[key]='item:minecraft:'+{'Blocks.stone':'stone','Blocks.brick_block':'bricks'}[exp]
   elif re.fullmatch(r'OD_CIRCUITS\[\d+\]',exp):keys[key]='circuit:'+re.search(r'\d+',exp)[0]
   elif re.fullmatch(r'MT.DATA.CABLES_01\[\d+\]',exp):keys[key]='cabletier:'+re.search(r'\[(\d+)\]',exp)[1]
   elif exp=='IL.Processor_Crystal_Sapphire':keys[key]='item:gt6ce:crystal_processor_sapphire'
   elif exp=='IL.Processor_Crystal_Emerald':keys[key]='item:gt6ce:crystal_processor_emerald'
   elif exp=='OD.container1000water':keys[key]='item:minecraft:water_bucket'
   elif exp in {'Blocks.hopper','Blocks.pumpkin'}:keys[key]='item:minecraft:'+exp.removeprefix('Blocks.')
   elif exp=='OD.blockTorch':keys[key]='item:minecraft:torch'
   elif exp=='OD.itemMud':keys[key]='tag:gt6ce:plant_pot_mud'
   elif exp=='OD.listAllmushroom':keys[key]='tag:gt6ce:mushrooms'
   elif exp=='OD.blockGlassColorless':keys[key]='item:minecraft:glass'
   elif exp=='IL.Ceramic_Basin':keys[key]='item:gt6ce:mold_basin_ceramic'
   elif exp=='IL.Tacho_Meter':keys[key]='item:gt6ce:tachometer'
   elif exp=='IL.Electro_Meter':keys[key]='item:gt6ce:electrometer'
   else:bad.append(exp)
  for ch in set(''.join(patterns))-{' '}-set(keys):
   if ch in tools:keys[ch]='item:gt6ce:tool_'+tools[ch]
   else:bad.append(ch)
  if bad:missing.append([path,bad]);continue
  if path=='coke_oven_wall':path='fire_bricks'
  keysrc=', '.join("'%s', \"%s\""%(k,v) for k,v in sorted(keys.items()))
  rows.append(f'        new Row("{path}", 1, {mat}, "'+ '|'.join(patterns)+f'", Map.of({keysrc}))')
  evidence.append({'id':path,'line':line,'source_id':int(a[2])})
# Key crafting is identical in all ten original MultiItemRandomTools registrations.
for material in ['Iron','Gold','Copper','Tin','Bronze','Brass','Silver','Platinum','Lead','Plastic']:
 rows.append(f'        new Row("{material.lower()}_key", 3, com.gregtech.gregtech.api.material.GTMaterialRegistry.get("{material}"), "fPx", Map.of(\'f\', "item:gt6ce:tool_file", \'P\', "mat:plate", \'x\', "item:gt6ce:tool_wire_cutter"))')
for size,form in [('small','Double'),('medium','Triple'),('large','Quadruple')]:
 rows.append(f'        new Row("press_bullet_casing_shape_{size}", 1, com.gregtech.gregtech.content.material.Materials.Steel, "TPT|dyh|SPS", Map.of(\'T\', "mat:screw", \'P\', "mat:plate{form}", \'S\', "mat:stick", \'d\', "item:gt6ce:tool_screwdriver", \'y\', "item:gt6ce:tool_chisel", \'h\', "item:gt6ce:tool_hammer"))')
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
