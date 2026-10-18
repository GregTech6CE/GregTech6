"""Shared GT6 electrical recipes and cumulative OD_CIRCUITS tags; no invented providers."""
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT/'core/src/main/resources/data/gt6ce'
TIERS = ['ulv','lv','mv','hv','ev','iv','luv','zpm','uv','xv']
CASINGS = ['tinalloy','steelgalvanized','aluminium','stainlesssteel','chromium','titanium','iridium','osmiumelemental','trinitanium','trinaquadalloy']
WIRES = ['lead','tin','copper','gold','aluminium','platinum']+['graphene']*4
def write(path, data):
    text=json.dumps(data,ensure_ascii=False,indent=2)+'\n'
    path.parent.mkdir(parents=True,exist_ok=True)
    if not path.exists() or path.read_text(encoding='utf-8')!=text:path.write_text(text,encoding='utf-8')
def item(name):return {'item':'gt6ce:'+name}
def circuit_tags():
    names = ['primitive','basic','good','advanced','elite','master','ultimate']
    for tier in range(10):
        values = ['gt6ce:circuit_'+names[tier]] if 1 <= tier <= 6 else []
        if tier < 9: values.append(f'#gt6ce:circuits_tier_{tier+1}_plus')
        # Exact original OD_CIRCUITS key; higher providers satisfy lower requirements.
        values.append({'id':f'#gt:circuit{tier}','required':False})
        if tier <= 6: values.append({'id':'#forge:circuits/'+names[tier],'required':False})
        write(DATA/f'tags/items/circuits_tier_{tier}_plus.json',{'replace':False,'values':values})

def run():
    circuit_tags()
    for i in range(9):
        ident='transformer_'+TIERS[i]+'_'+TIERS[i+1]
        metal='copper' if i<3 else 'annealed_copper'
        write(DATA/f'recipes/energy_nodes/{ident}.json',{'type':'gt6ce:tool_shaped','allow_mirror':False,'pattern':['WIW','XMx','WIW'],'key':{'W':({'tag':'gt6ce:wire_01/any_copper'} if i<3 else item('wire_01_'+metal)),'X':({'tag':'gt6ce:wire_04/any_copper'} if i<3 else item('wire_04_'+metal)),'I':{'tag':'gt6ce:plate_double/any_iron_or_steel'},'M':item('casing_machine_'+CASINGS[i]),'x':item('tool_wire_cutter')},'result':item(ident)})
    for i in range(10):
        for large in (False,True):
            # Source 10099 references unregistered transformer 10049; do not invent it.
            if large and i==9:continue
            ident=('energy_storage_' if large else 'battery_box_')+TIERS[i]
            size='04' if large else '01'
            middle='transformer_'+TIERS[i]+'_'+TIERS[i+1] if large else 'casing_machine_'+CASINGS[i]
            write(DATA/f'recipes/energy_nodes/{ident}.json',{'type':'minecraft:crafting_shaped','pattern':['WCW','WCW','XMX'],'key':{'W':({'tag':'gt6ce:wire_'+size+'/any_copper'} if i==2 else item('wire_'+size+'_'+WIRES[i])),'C':({'tag':'gt6ce:cable_'+size+'/any_copper'} if i==2 else item(('wire_' if i>=6 else 'cable_')+size+'_'+WIRES[i])),'X':{'tag':f'gt6ce:circuits_tier_{i}_plus'},'M':item(middle)},'result':item(ident)})
    print('28 recipes: 9 transformers, 10 ordinary and 9 large boxes. Tiers 7-9 require external circuit tags; large tier 9 retains the missing source 10049 dependency.')
if __name__=='__main__':run()
