"""Import unmodified GT6 panel textures and generate the twelve functional panel recipes/models."""
import hashlib
import json
import shutil
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS=ROOT/'src/main/resources/assets/gregtech'
BUTTON_STYLES=['underlay','underlay_0_to_15','underlay_0_to_F','underlay_1_to_16','underlay_16_1_to_15','underlay_keypad_1_to_9','underlay_keypad_9_to_1','underlay_bits']
MODE_FOLDERS=['manualselector','redstoneselector','buttonselector','redstoneemitter']
TEXTURES=['base','progressredstone/circuit','energyredstone/circuit','redstoneconductor/in','redstoneconductor/out','coverswitch/base','coverswitch/circuit','shutter/normal','shutter/inverted']
TEXTURES += [f'{folder}/{n}' for folder in MODE_FOLDERS for n in range(16)]
TEXTURES += [f'{folder}/underlay' for folder in MODE_FOLDERS if folder!='buttonselector']
TEXTURES += ['buttonselector/'+style for style in BUTTON_STYLES]
TEXTURES += ['energydisplay/underlay']+[f'energydisplay/{n}' for n in range(11)]
TEXTURES += [f'statusdisplay/{style}/{image}' for style in ['bottom','top'] for image in ['base']+[f'{n}_{state}' for n in range(1,5) for state in ['off','on']]]
MODELS={
    'manual_selector':['base','manualselector/underlay','manualselector/0'],
    'redstone_selector':['base','redstoneselector/underlay','redstoneselector/0'],
    'button_panel_selector':['buttonselector/underlay','buttonselector/0'],
    'redstone_emitter':['base','redstoneemitter/underlay','redstoneemitter/0'],
    'progress_sensor':['base','progressredstone/circuit'],
    'energy_sensor':['base','energyredstone/circuit'],
    'energy_display_cover':['base','energydisplay/underlay','energydisplay/0'],
    'machine_status_display_cover':['base','statusdisplay/bottom/base','statusdisplay/bottom/4_on'],
    'redstone_conductor_cover_accept':['base','redstoneconductor/in'],
    'redstone_conductor_cover_emit':['base','redstoneconductor/out'],
    'cover_controller':['coverswitch/base','coverswitch/circuit'],
    'shutter_cover':['base','shutter/normal'],
}
# Source patterns; the signal wire blocks provide the original wireGt01 ingredients.
COMMON={'Q':'gregtech:blank_cover','C':'gregtech:circuit_basic','W':'gregtech:cable_01_tin',
        'X':'gregtech:integrated_circuit_0','B':'minecraft:stone_button','L':'gregtech:wire_01_lumium',
        'R':'gregtech:wire_01_redalloy','P':'gregtech:plate_stainlesssteel','T':'gregtech:screw_stainlesssteel',
        'w':'gregtech:tool_wrench','d':'gregtech:tool_screwdriver'}
RECIPES={
    'manual_selector':([' C ','WQX',' B '],{'W':'gregtech:cable_01_copper'}),
    'redstone_selector':([' C ','WQX',' B '],{'W':'gregtech:cable_01_copper','B':'minecraft:comparator'}),
    'button_panel_selector':(['BXB','BQB','BCB'],{}),
    'redstone_emitter':(['BQB','WXW'],{'X':'minecraft:comparator'}),
    'progress_sensor':(['WQW','BCB'],{'C':'gregtech:circuit_good','B':'gregtech:gear_gt_small_brass'}),
    'energy_sensor':(['WQW','BCB'],{'C':'gregtech:circuit_good','B':'gregtech:wire_01_tin'}),
    'energy_display_cover':(['CLB','WQW'],{'B':'gregtech:wire_01_tin'}),
    'machine_status_display_cover':(['LLB','CQW'],{'B':'minecraft:lever'}),
    'redstone_conductor_cover_accept':(['R','Q'],{}),
    'redstone_conductor_cover_emit':(['Q','R'],{}),
    'cover_controller':(['BW','CQ'],{'C':'gregtech:circuit_good','B':'minecraft:comparator'}),
    'shutter_cover':(['TwT','PQP','TdT'],{}),
}
def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    text=json.dumps(data,ensure_ascii=False,indent=2)+'\n'
    if not path.exists() or path.read_text(encoding='utf-8')!=text:path.write_text(text,encoding='utf-8')
def main(original):
    ledger_path=ROOT/'tools/gt6_texture_sources.json';ledger=json.loads(ledger_path.read_text(encoding='utf-8'))
    for texture in TEXTURES:
        source=original/f'src/main/resources/assets/gregtech/textures/blocks/machines/covers/{texture}.png'
        relative=f'textures/block/machines/covers/{texture}.png';target=ASSETS/relative
        target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
        ledger[relative]={'source':source.relative_to(original).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()}
        if source.with_suffix('.png.mcmeta').exists():shutil.copy2(source.with_suffix('.png.mcmeta'),target.with_suffix('.png.mcmeta'))
    write(ledger_path,ledger)
    for name,layers in MODELS.items():
        textures={f'layer{i}':'gregtech:block/machines/covers/'+value for i,value in enumerate(layers)}
        textures['particle']=textures['layer0']
        write(SHARED_ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':textures})
    for name,(pattern,overrides) in RECIPES.items():
        keys=COMMON|overrides;used=set(''.join(pattern))-{' '}
        write(ROOT/f'src/main/resources/data/gregtech/recipes/panel_covers/{name}.json',{
            'type':'gregtech:tool_shaped','pattern':pattern,'key':{k:{'item':keys[k]} for k in sorted(used)},'result':{'item':'gregtech:'+name}})
if __name__=='__main__':
    import sys
    main(Path(sys.argv[1]))
