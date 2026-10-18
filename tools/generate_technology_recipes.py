"""Restore embedded GT6 random-tool crafting rows; keep original tier/mirror policies.

Sources are read-only. The source line is retained on every row for review.
Machine rows and crafting-only tool identities live in shared Java catalogs.
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT.parent / 'gregtech6-master/gregtech6-master/src/main/java'
DATA = ROOT / 'core/src/main/resources/data/gt6ce'
RANDOM = (SRC / 'gregtech/items/MultiItemRandomTools.java').read_text(encoding='utf-8').splitlines()
MTE = (SRC / 'gregtech/loaders/b/Loader_MultiTileEntities.java').read_text(encoding='utf-8').splitlines()
ROWS = {}
def item(id): return {'item': id if ':' in id else 'gt6ce:'+id}
def tag(id): return {'tag': id if ':' in id else 'gt6ce:'+id}
def circuits(n): return tag(f'circuits_tier_{n}_plus')
def write(path, data):
    text = json.dumps(data, ensure_ascii=False, indent=2)+'\n'
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists() or path.read_text(encoding='utf-8') != text:
        path.write_text(text, encoding='utf-8')
def row(id, line, pattern, keys, count=1, mirror=False, mte=False):
    source = MTE if mte else RANDOM
    text = source[line-1]
    assert 'CR.shaped' in text and all('"'+s+'"' in text for s in pattern), (id,line)
    assert ("CR.DEF_NCC_MIR" in text) == mirror, (id,'mirror')
    ROWS[id] = {'type':'gt6ce:tool_shaped', '_comment':f"GT6 {'Loader_MultiTileEntities' if mte else 'MultiItemRandomTools'}:{line}",
        'allow_mirror':mirror, 'pattern':pattern, 'key':keys, 'result':{**item(id),'count':count}}

tool = lambda kind: tag('forge:tools/'+kind)
common = {'w':tool('wrench'),'h':tool('hammer'), 'P':item('plate_curved_steelgalvanized'), 'C':circuits(3)}
tips = [
    ('wrench','compact_electric_motor_hv',item('tool_head_wrench_chromium'),['wPh','CMC',' X ']),
    ('screwdriver','compact_electric_motor_hv',item('tool_head_screwdriver_stainlesssteel'),['wPh','CMC',' X ']),
    ('saw','compact_electric_motor_hv',item('tool_head_buzz_saw_cobaltbrass'),['wPh','CMC','DXd']),
    ('hammer','compact_electric_piston_hv',item('tool_head_hammer_tungstencarbide'),['wPh','CMC',' X ']),
    ('cutter','compact_electric_motor_hv',item('plate_stainlesssteel'),['wPh','CMC','XfX']),
    ('chisel','compact_electric_piston_hv',item('tool_head_chisel_tungstensteel'),['wPh','CMC',' X ']),
    ('rubber_hammer','compact_electric_piston_hv',tag('technology/any_rubber_hammer_head'),['wPh','CMC',' X ']),
    ('blade','compact_electric_piston_hv',item('tool_head_sword_bronze'),['wPh','CMC',' X ']),
    ('drill','compact_electric_motor_hv',tag('technology/any_steel_rod'),['wPh','CMC','fX ']),
    ('file','compact_electric_conveyor_hv',tag('technology/any_diamond_dust'),['wPh','CMC',' X '])]
for offset,(tip,middle,head,pattern) in enumerate(tips):
    keys = {**common,'M':item(middle),'X':head}
    if tip=='saw': keys.update(D=tag('technology/any_diamond_dust'),d=tool('screwdriver'))
    if tip in ['cutter','drill']: keys['f']=tool('file')
    row('robot_arm_'+tip+'_tip',481+offset,pattern,keys)
row('fire_starter',395,['S ','GS'],{'S':tag('ammunition/sticks_wood'),'G':item('dry_grass')},mirror=True)
row('fire_starter_2',398,['S ','GS'],{'S':tag('ammunition/sticks_wood'),'G':item('dry_bark')},mirror=True)
row('plastic_lighter_empty',388,['IF','dP','xP'],{'I':tag('screws/any_iron_or_steel'),'F':item('minecraft:flint'),
    'd':tool('screwdriver'),'x':tool('wire_cutter'),'P':tag('technology/any_plastic_curved_plate')})
for line,prefix,count,phosphorus in [(352,'dust_small',1,False),(353,'dust_small',1,True),(354,'dust',4,False),(355,'dust',4,True)]:
    id='match_'+str(line)
    pattern=['P','S'] if count==1 else [' S ','SPS',' S ']
    row(id,line,pattern,{'P':tag('technology/any_phosphorus_'+prefix) if phosphorus else item(prefix+'_phosphor'),
        'S':tag('technology/any_wood_bolt')},count=count)
    ROWS[id]['result']['item']='gt6ce:match'
row('portable_scanner',1075,['EXR','CPU','BXB'],{'B':item('battery_alkaline_hv'),'X':item('plate_chromium'),
    'U':tag('technology/usb_sticks_tier_3_plus'),'C':tag('technology/usb_cables_tier_3_plus'),'E':item('compact_signal_emitter_ev'),'R':item('compact_sensor_ev'),
    'P':item('crystal_circuit_sapphire')},mte=True)
row('portable_cropnalyzer',1076,['EXR','CPU','BXB'],{'B':item('battery_alkaline_mv'),'X':item('plate_aluminium'),
    'U':tag('technology/usb_sticks_tier_1_plus'),'C':tag('technology/usb_cables_tier_1_plus'),'E':item('compact_signal_emitter_mv'),'R':item('compact_sensor_mv'),
    'P':circuits(6)},mte=True)
row('remote_activator',522,['TPE','BCd','xPT'],{'P':item('plate_chromium'),'T':item('screw_chromium'),
    'C':circuits(4),'E':item('compact_signal_emitter_ev'),'B':tag('minecraft:buttons'),
    'd':tool('screwdriver'),'x':tool('wire_cutter')})

def run():
    for id,data in ROWS.items(): write(DATA/f'recipes/technology/{id}.json',data)
    catalog=ROOT/'core/src/main/java/com/gregtech/gregtech/content/recipe/EquipmentCraftingCatalog.java'
    text=catalog.read_text(encoding='utf-8')
    for id in ROWS:
        entry=f'            "technology/{id}.json",\n'
        if entry.strip() not in text: text=text.replace('FILES = java.util.List.of(\n','FILES = java.util.List.of(\n'+entry)
    catalog.write_text(text,encoding='utf-8')
    # Existing cover/USB rows used the reversed prefix of the circuit list. Only repair
    # those exact arrays; leave all other recipe content and later cover changes untouched.
    tiers=['basic','good','advanced','elite','master','ultimate']
    repaired=[]
    for path in (DATA/'recipes').rglob('*.json'):
        data=json.loads(path.read_text(encoding='utf-8')); changed=False
        for letter,value in data.get('key',{}).items():
            for n in range(1,7):
                old=[item('circuit_'+name) for name in tiers[:n]]
                if value==old:
                    data['key'][letter]=circuits(n); changed=True
        if changed: write(path,data); repaired.append(str(path.relative_to(DATA)))
    print(json.dumps({'crafting_rows':len(ROWS),'repaired_circuit_rows':repaired},indent=2))
if __name__=='__main__': run()
