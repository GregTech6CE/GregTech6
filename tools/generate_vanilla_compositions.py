"""Import GT6 item data and derive modern crafting compositions from the local 1.20.1 assets.
No network dependencies. GT6 source rows always take precedence over modern conventions.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

from pathlib import Path
from fractions import Fraction as F
import re,json,zipfile,ast,operator
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT.parent/'gregtech6-master/gregtech6-master/src/main/java/gregapi/load/LoaderItemData.java'
ARCHIVE=Path.home()/'.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar'
U=648648000
rows={}
def put(name,components,source,recover=True,replace=True):
    if not replace and name in rows:return
    merged={}
    for mat,amount in components:
        amount=F(amount)
        if amount>0:merged[mat]=merged.get(mat,F(0))+amount
    rows[name]={'components':{m:int(v*U) for m,v in merged.items()},'source':source,'recoverable':recover}
def single(name,mat,units=1,source='1.20.1 material family',recover=True,replace=False):
    put(name,[(mat,F(units))],source,recover,replace)
RENAMES={'brick_block':'bricks','nether_brick':'nether_bricks','nether_brick_fence':'nether_brick_fence','hardened_clay':'terracotta',
'noteblock':'note_block','stonebrick':'stone_bricks','stone_brick_stairs':'stone_brick_stairs','stone_stairs':'cobblestone_stairs',
'lit_furnace':'furnace','lit_redstone_lamp':'redstone_lamp','unlit_redstone_torch':'redstone_torch','wooden_button':'oak_button',
'wooden_pressure_plate':'oak_pressure_plate','fence':'oak_fence','fence_gate':'oak_fence_gate','trapdoor':'oak_trapdoor',
'wooden_door':'oak_door','sign':'oak_sign','boat':'oak_boat','cooked_beef':'cooked_beef','fireworks':'firework_rocket',
'firework_charge':'firework_star','speckled_melon':'glistering_melon_slice','snow':'snow_block','snow_layer':'snow','deadbush':'dead_bush'}
ALIASES={'Fe':'Iron','Au':'Gold','Cu':'Copper','Cl':'Chlorine','Dead':'Wood','FishOil':'FishOil','BoneWither':'Bone'}
PREFIX={'toolHeadSword':F(17,9),'toolHeadPickaxe':F(26,9),'toolHeadShovel':F(8,9),'toolHeadAxe':F(26,9),'toolHeadHoe':F(17,9),'ingot':F(1),'nugget':F(1,9),'blockIngot':F(9),'ring':F(1,4)}
COLORS='white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split()
def targets(name,meta,kind):
    if name=='fish':return {0:['cod'],1:['salmon'],2:['tropical_fish'],3:['pufferfish']}.get(meta,[])
    if name=='cooked_fished':return ['cooked_cod','cooked_salmon']
    if name=='skull':return {0:['skeleton_skull'],1:['wither_skeleton_skull']}.get(meta,[])
    if name=='golden_apple':return ['enchanted_golden_apple' if meta==1 else 'golden_apple']
    if name=='anvil':return [{0:'anvil',1:'chipped_anvil',2:'damaged_anvil'}[meta]]
    if name=='sand':return ['red_sand' if meta==1 else 'sand']
    if name=='stone_slab':return [{0:'smooth_stone_slab',1:'sandstone_slab',2:'petrified_oak_slab',3:'cobblestone_slab',4:'brick_slab',5:'stone_brick_slab',6:'nether_brick_slab',7:'quartz_slab'}.get(meta%8,'smooth_stone_slab')]
    if name=='double_stone_slab':return []
    if name=='tallgrass':return ['dead_bush'] if meta==0 else []
    if name=='stained_hardened_clay':return [c+'_terracotta' for c in COLORS]
    if name in ('stained_glass','stained_glass_pane'):return [c+'_'+name for c in COLORS]
    if name=='cobblestone_wall':return ['cobblestone_wall','mossy_cobblestone_wall']
    return [RENAMES.get(name,name)]
def amount(expr):
    expr=expr.strip().replace('U * (MD.EtFu.mLoaded?2:6)','2')
    expr=re.sub(r'OP\.(\w+)\s*\.mAmount',lambda m:str(PREFIX[m[1]].numerator)+'/'+str(PREFIX[m[1]].denominator),expr)
    expr=re.sub(r'\bU(\d*)\b',lambda m:('(1/'+m[1]+')') if m[1] else '1',expr)
    def ev(n):
        if isinstance(n,ast.Constant):return F(n.value)
        if isinstance(n,ast.UnaryOp) and isinstance(n.op,ast.USub):return -ev(n.operand)
        if isinstance(n,ast.BinOp):return {ast.Add:operator.add,ast.Sub:operator.sub,ast.Mult:operator.mul,ast.Div:operator.truediv}[type(n.op)](ev(n.left),ev(n.right))
        raise ValueError(expr)
    return ev(ast.parse(expr,mode='eval').body)
def imported():
    for number,line in enumerate(SOURCE.read_text(encoding='utf-8').splitlines(),1):
        m=re.search(r'OM\.dat[2a]\(ST\.make\((Items|Blocks)\.(\w+)\s*,\s*1,\s*(W|\d+)\),(.+?)\);',line)
        if not m:continue
        kind,name,meta,tail=m.groups();meta=-1 if meta=='W' else int(meta)
        tail=tail.replace('OM.stack(','').replace(')','')
        bits=[v.strip() for v in tail.split(',')]
        try:
            components=[(ALIASES.get(bits[i].split('.')[-1],bits[i].split('.')[-1]),amount(bits[i+1])) for i in range(0,len(bits),2)]
            if any(v<=0 for _,v in components):continue
            for target in targets(name,meta,kind):put(target,components,f'GT6 LoaderItemData.java:{number}')
        except (KeyError,ValueError,SyntaxError,IndexError):continue

def build():
    imported()
    with zipfile.ZipFile(ARCHIVE) as z:
        ids={n.removeprefix('assets/minecraft/models/item/').removesuffix('.json') for n in z.namelist() if n.startswith('assets/minecraft/models/item/') and n.endswith('.json')}
        recipes={n:json.loads(z.read(n)) for n in z.namelist() if n.startswith('data/minecraft/recipes/') and n.endswith('.json')}
        tags={n.removeprefix('data/minecraft/tags/items/').removesuffix('.json'):json.loads(z.read(n))['values'] for n in z.namelist() if n.startswith('data/minecraft/tags/items/') and n.endswith('.json')}
    # Basic source-unified items and modern equivalents. Amounts are per item, never per stack.
    for mat,names in {'Iron':['iron_ingot'],'Gold':['gold_ingot'],'Copper':['copper_ingot'],'Netherite':['netherite_ingot'],
        'Diamond':['diamond'],'Emerald':['emerald'],'NetherQuartz':['quartz'],'Amethyst':['amethyst_shard'],
        'Coal':['coal'],'Charcoal':['charcoal'],'Redstone':['redstone'],'Glowstone':['glowstone_dust'],'Gunpowder':['gunpowder'],
        'Sugar':['sugar'],'Bone':['bone_meal'],'Brick':['brick'],'NetherBrick':['nether_brick'],'Paper':['paper'],
        'AncientDebris':['netherite_scrap'],'Prismarine':['prismarine_shard','prismarine_crystals'],'NetherStar':['nether_star'],
        'EnderPearl':['ender_pearl'],'EnderEye':['ender_eye'],'Lapis':['lapis_lazuli']}.items():
        for name in names:single(name,mat)
    for metal in ['iron','gold','copper']:
        single('raw_'+metal,metal.title(),2);single('raw_'+metal+'_block',metal.title(),18)
        single(metal+'_nugget',metal.title(),F(1,9))
    single('blaze_powder','Blaze',F(1,9));single('blaze_rod','Blaze',F(1,2));single('stick','Wood',F(1,2))
    single('glass_bottle','Glass',1);single('snow','Snow',F(1,8));single('blue_ice','Ice',18)
    for name in sorted(ids):
        for rock in ['stone','granite','diorite','andesite','deepslate','tuff','calcite','basalt','blackstone','dripstone','netherrack','end_stone','obsidian','sand','red_sand','sandstone']:
            if name==rock or name==rock+'_block':single(name,{'end_stone':'Endstone','red_sand':'RedSand','sandstone':'Sand'}.get(rock,rock.title()),9)
        for wood in ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','bamboo','crimson','warped']:
            for suffix,units in [('log',4),('wood',4),('stem',4),('hyphae',4),('planks',1),('slab',F(1,2)),('stairs',F(3,4)),('button',1),('pressure_plate',2),('fence',F(3,2)),('fence_gate',4),('door',2),('trapdoor',3),('boat',5),('raft',5),('sign',2)]:
                if name in (wood+'_'+suffix,'stripped_'+wood+'_'+suffix):single(name,'Wood',units,'GT6 wood family, modern species use generic Wood')
        if name.endswith('_wool'):single(name,'Wool',4, recover=False)
        if name.endswith('_carpet'):single(name,'Wool',F(8,3),recover=False)
        if name.endswith('_terracotta'):single(name,'Ceramic',4)
        if name.endswith('_stained_glass'):single(name,'Glass',9)
        if name.endswith('_stained_glass_pane'):single(name,'Glass',1)
        if name.endswith('_ore'):
            ore=name.removeprefix('deepslate_').removeprefix('nether_').removesuffix('_ore')
            mat={'iron':'Iron','gold':'Gold','copper':'Copper','coal':'Coal','redstone':'Redstone','lapis':'Lapis','emerald':'Emerald','diamond':'Diamond','quartz':'NetherQuartz'}.get(ore)
            if mat:single(name,mat,2)
    # WoodEntry/BeamEntry override LoaderItemData's preliminary/default wood weights.
    for wood in ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry']:
        for suffix in ['log','wood']:
            put(wood+'_'+suffix,[('Wood',8),('Bark',1)],'GT6 WoodEntry: six sawn planks plus two wood units and one bark')
            put('stripped_'+wood+'_'+suffix,[('Wood',8)],'GT6 BeamEntry: seven sawn planks plus one wood unit; modern stripped log')
    # Modern stonecutting is one block per stair: the legacy six-quartz recovery would duplicate material.
    put('quartz_stairs',[('NetherQuartz',4)],'1.20.1 stonecutting adaptation: cap GT6 six-quartz stair at four')
    # Match three-sign / three-ladder crafting batches and GT6 saw-dismantling byproducts.
    for wood in ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','crimson','warped','bamboo']:
        put(wood+'_sign',[('Wood',F(13,6))],'1.20.1 sign: six planks and one half-unit stick divided by three')
    put('ladder',[('Wood',F(7,6))],'1.20.1 ladder: seven half-unit sticks divided by three; matches GT6 sawing')
    # Modern biological/magical matter is explicitly descriptive, not an ore or a recycling yield.
    for name in sorted(ids):
        if name.endswith('_spawn_egg') or name in {'air','barrier','bedrock','command_block','chain_command_block','repeating_command_block','command_block_minecart','structure_block','structure_void','jigsaw','debug_stick','light','knowledge_book','end_portal_frame'}:
            put(name,[],'Minecraft technical/entity item: no recoverable bulk material',False);continue
        if name.endswith('_pottery_sherd'):single(name,'Ceramic',1,recover=False)
        if name.endswith('_dye'):single(name,'DyeMatter',F(1,9),recover=False)
        if name.endswith('_banner_pattern') or name.endswith('_smithing_template'):single(name,'Paper',1,recover=False)
        if name.endswith('_leaves') or name.endswith('_sapling') or name.endswith('_roots') or name.endswith('_fungus') or name.endswith('_seeds'):
            single(name,'PlantMatter',1,recover=False)
        if 'coral' in name:single(name,'CaCO3',9 if name.endswith('_block') else 1,recover=False)
        if name.startswith('sculk'):single(name,'Sculk',9 if name=='sculk' else 1,recover=False)
        if name.endswith('_concrete') or name.endswith('_concrete_powder'):single(name,'Concrete',9,recover=False)
        if name.endswith('_shulker_box') or name=='shulker_box':put(name,[('AnimalShell',2),('Wood',8)],'Minecraft shulker shell and chest',False,False)
        if 'amethyst' in name:single(name,'Amethyst',{'amethyst_block':9,'budding_amethyst':9,'amethyst_cluster':4,'large_amethyst_bud':2,'medium_amethyst_bud':1,'small_amethyst_bud':F(1,2)}.get(name,1),recover=False)
        if name.endswith('_hanging_sign'):put(name,[('Wood',4),('Iron',F(2,3))],'Minecraft hanging sign recipe, generic Wood',False,False)
        if name.endswith('_candle') or name=='candle':put(name,[('WaxBee',1),('Wool',1)],'Minecraft honeycomb and string convention',False,False)
        if name.startswith('music_disc_') or name=='disc_fragment_5':single(name,'Plastic',1,recover=False)
        if name.endswith('_bucket'):
            put(name,[('Iron',3)],'Minecraft filled bucket shell; contents excluded from bulk recovery',False);continue
        if name in {'potion','splash_potion','lingering_potion','experience_bottle','dragon_breath','honey_bottle'}:
            put(name,[('Glass',1)],'Minecraft bottle shell; contents are stack-dependent and not recycled',False)
        for prefix in ('exposed_','weathered_','oxidized_','waxed_','waxed_exposed_','waxed_weathered_','waxed_oxidized_'):
            if name.startswith(prefix) and 'copper' in name:single(name,'Copper',F(9,2) if name.endswith('_slab') else F(27,4) if name.endswith('_stairs') else 9,recover=False)
        if name.startswith('netherite_') and name.removeprefix('netherite_') in {'sword','axe','pickaxe','shovel','hoe','helmet','chestplate','leggings','boots'}:
            base=rows['diamond_'+name.removeprefix('netherite_')]['components']
            put(name,[(m,F(v,U)) for m,v in base.items()]+[('Netherite',1)],'Minecraft smithing: diamond equipment plus one netherite ingot',False)
        if 'deepslate' in name and not name.endswith('_ore'):single(name,'Deepslate',F(9,2) if name.endswith('_slab') else F(27,4) if name.endswith('_stairs') else 9,recover=False)
        if 'blackstone' in name:single(name,'Blackstone',F(9,2) if name.endswith('_slab') else F(27,4) if name.endswith('_stairs') else 9,recover=False)
        if name.startswith('infested_'):single(name,'Stone',9,recover=False)
    for mat,names in {
        'Soil':'dirt coarse_dirt rooted_dirt dirt_path farmland grass_block podzol mycelium mud packed_mud soul_soil crimson_nylium warped_nylium',
        'Wool':'string cobweb', 'Feather':'feather', 'AnimalShell':'egg turtle_egg frogspawn turtle_scute scute shulker_shell nautilus_shell',
        'Prismarine':'heart_of_the_sea', 'Sculk':'echo_shard', 'Bone':'goat_horn creeper_head zombie_head player_head piglin_head dragon_head',
        'Potato':'potato poisonous_potato baked_potato', 'Wheat':'wheat bread', 'Cocoa':'cocoa_beans',
        'MeatRaw':'mutton rabbit spider_eye rabbit_foot', 'MeatCooked':'cooked_mutton cooked_rabbit', 'Leather':'rabbit_hide elytra',
        'DyeMatter':'ink_sac glow_ink_sac', 'Rubber':'slime_ball', 'WaxBee':'honeycomb', 'Honey':'honey_block',
        'Obsidian':'crying_obsidian', 'Stone':'gravel mossy_stone_bricks cracked_stone_bricks',
        'NetherBrick':'cracked_nether_bricks','AncientDebris':'ancient_debris','Wood':'mangrove_roots mushroom_stem bamboo_block stripped_bamboo_block',
        'PlantMatter':'apple beetroot carrot melon_slice glow_berries sweet_berries chorus_fruit popped_chorus_fruit bamboo cactus sugar_cane kelp dried_kelp grass fern large_fern seagrass sea_pickle lily_pad azalea flowering_azalea big_dripleaf small_dripleaf hanging_roots glow_lichen moss_carpet pink_petals spore_blossom nether_sprouts nether_wart mangrove_propagule brown_mushroom red_mushroom crimson_roots warped_roots chorus_flower chorus_plant allium azure_bluet blue_orchid cornflower dandelion lily_of_the_valley lilac orange_tulip oxeye_daisy peony pink_tulip poppy red_tulip rose_bush sunflower torchflower torchflower_seeds pitcher_plant pitcher_pod white_tulip wither_rose pumpkin carved_pumpkin melon brown_mushroom_block red_mushroom_block moss_block nether_wart_block warped_wart_block sponge wet_sponge dried_kelp_block',
        'MagicMatter':'ghast_tear dragon_egg totem_of_undying netherite_upgrade_smithing_template'
    }.items():
        for name in names.split():single(name,mat,1,recover=False)
    for name in ['stone','granite','diorite','andesite','deepslate','tuff','calcite','basalt','blackstone','dripstone_block','netherrack','end_stone','obsidian','gravel']:
        if name in rows and rows[name]['source'].startswith('1.20.1'):rows[name]['components']={m:9*U for m in rows[name]['components']}
    # These source rows are deliberately conservative or contain fluid/biological content.
    for name in ['firework_star','firework_rocket','written_book','filled_map']:
        if name in rows:rows[name]['recoverable']=False
    for mat,names,qty in [
        ('Wood','bee_nest',3),('Gold','bell',4),('Leather','phantom_membrane',1),('Dripstone','pointed_dripstone',F(9,4)),
        ('Basalt','smooth_basalt',9),('NetherQuartz','smooth_quartz',4),('Sand','smooth_sandstone',9),('RedSand','smooth_red_sandstone',9),
        ('Stone','smooth_stone suspicious_gravel',9),('Sand','suspicious_sand',9),('SoulSand','soul_sand',9),
        ('PlantMatter','tall_grass vine twisting_vines weeping_vines shroomlight',1),('AnimalShell','sniffer_egg',1),
        ('MagicMatter','ochre_froglight pearlescent_froglight verdant_froglight spawner',1),('Paper','name_tag',1)]:
        for name in names.split():single(name,mat,qty,recover=False)
    modern = {
        'blast_furnace':[('Iron',5),('Stone',35)], 'brewing_stand':[('Blaze',F(1,2)),('Stone',27)],
        'bundle':[('Leather',6),('Wool',2)], 'cake':[('Wheat',3),('Sugar',2),('Milk',3),('AnimalShell',1)],
        'torch':[('Wood',F(1,8)),('Coal',F(1,4))], 'soul_torch':[('Wood',F(1,8)),('Coal',F(1,4)),('SoulSand',F(9,4))],
        'campfire':[('Wood',F(27,2)),('Coal',1)],'soul_campfire':[('Wood',F(27,2)),('SoulSand',9)],
        'fire_charge':[('Coal',F(1,3)),('Blaze',F(1,27)),('Gunpowder',F(1,3))],
        'tnt':[('Gunpowder',4),('SiliconDioxide',4)], 'trident':[('Prismarine',3),('Wood',1)],
        'suspicious_stew':[('Wood',1),('PlantMatter',3)], 'tipped_arrow':[('Wood',F(1,8)),('Flint',F(1,4)),('Feather',F(1,4))],
        'jack_o_lantern':[('PlantMatter',9),('Wood',F(1,8)),('Coal',F(1,4))]
    }
    for name,components in modern.items():put(name,components,'Minecraft 1.20.1 explicit composition convention',False,False)
    # Inference uses only fully known ingredient alternatives, retaining their common minimum.
    # Explicit GT6 values above are never overwritten. Unknown/catalyst-container recipes are skipped.
    def tagitems(key,seen=None):
        seen=set() if seen is None else seen
        if key in seen:return []
        seen=seen|{key};result=[]
        for value in tags.get(key,[]):
            if not isinstance(value,str):continue
            result.extend(tagitems(value[11:],seen) if value.startswith('#minecraft:') else [value.removeprefix('minecraft:')])
        return result
    def ingredient(value):
        if isinstance(value,list):options=[v['item'].removeprefix('minecraft:') for v in value if 'item' in v]
        elif 'item' in value:options=[value['item'].removeprefix('minecraft:')]
        elif 'tag' in value:options=tagitems(value['tag'].removeprefix('minecraft:'))
        else:return None
        if not options or any(x not in rows or not rows[x]['components'] for x in options):return None
        if any(x.endswith('_bucket') or x in ('potion','honey_bottle') for x in options):return None
        common=set.intersection(*(set(rows[x]['components']) for x in options))
        if not common:return None
        return {m:min(rows[x]['components'][m] for x in options) for m in common}
    for _ in range(20):
        new={}
        for key,r in recipes.items():
            typ=r['type'];out=r.get('result');out={'item':out} if isinstance(out,str) else out
            if not out or not out.get('item','').startswith('minecraft:'):continue
            name=out['item'][10:]
            if name in rows:continue
            if typ=='minecraft:crafting_shaped':values=[r['key'][c] for line in r['pattern'] for c in line if c!=' ']
            elif typ=='minecraft:crafting_shapeless':values=r['ingredients']
            elif typ=='minecraft:stonecutting':values=[r['ingredient']];out['count']=r.get('count',1)
            else:continue
            components={};valid=True
            for value in values:
                found=ingredient(value)
                if found is None:valid=False;break
                for mat,qty in found.items():components[mat]=components.get(mat,0)+qty
            if not valid:continue
            components={m:v//out.get('count',1) for m,v in components.items() if v//out.get('count',1)>0}
            if name not in new or sum(components.values())<sum(new[name]['components'].values()):
                new[name]={'components':components,'source':'Minecraft 1.20.1 recipe: '+key,'recoverable':False}
        if not new:break
        rows.update(new)
    path=native_path(ROOT/'src/main/resources/data/gregtech/materials/vanilla_compositions.json')
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(rows,indent=2,ensure_ascii=False,sort_keys=True)+'\n',encoding='utf-8')
    (ROOT/'docs/vanilla-composition-render-variants.txt').write_text('\n'.join(sorted(ids-rows.keys())),encoding='utf-8')
    print(f'{len(rows)} mapped model/item IDs; {len(ids-rows.keys())} unclassified models (includes render variants)')
if __name__=='__main__':build()
