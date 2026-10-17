from pathlib import Path
SHARED_ASSETS = Path(__file__).resolve().parents[1] / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
"""GT6 redstone wire resources. Dynamic models reuse the existing original wire/rubber sprites."""
try:
    from tools.rebuild_panel_covers import ROOT, ASSETS, write
except ModuleNotFoundError:
    from rebuild_panel_covers import ROOT, ASSETS, write
import json

MATERIALS={'redalloy':('Red Alloy','红合金'), 'signalum':('Signalum','信素'), 'lumium':('Lumium','流明')}
IDS=[prefix+name for name in MATERIALS for prefix in ['wire_01_','cable_01_']]

def main():
    for name in IDS:
        cable=name.startswith('cable'); a,b=(6,10) if cable else (7,9)
        texture='gregtech:block/iconsets/insulation_full' if cable else 'gregtech:block/material_icons/metallic/wire'
        # JSON declares atlas dependencies and a usable south/north item fallback. Baking replaces
        # every state with PipeWireBakedModel, the same geometry path as the existing EU wires.
        faces={side:{'texture':'#body','tintindex':1 if cable else 0} for side in ['down','up','north','south','west','east']}
        write(SHARED_ASSETS/f'models/block/signal_wire/{name}.json',{
            'parent':'minecraft:block/block','textures':{'body':texture,'particle':texture,
                'core':'gregtech:block/material_icons/metallic/wire'},
            'elements':[{'from':[a,a,0],'to':[b,b,16],'faces':faces}]})
        write(SHARED_ASSETS/f'blockstates/{name}.json',{'variants':{'':{'model':'gregtech:block/signal_wire/'+name}}})
        write(SHARED_ASSETS/f'models/item/{name}.json',{'parent':'gregtech:block/signal_wire/'+name})
    for locale,index in [('en_us',0),('zh_cn',1)]:
        path=ASSETS/f'lang/{locale}.json'; data=json.loads(path.read_text(encoding='utf-8'))
        for material,names in MATERIALS.items():
            for prefix in ['wire_01_','cable_01_']:
                suffix=(' Insulated Redstone Wire' if prefix.startswith('cable') else ' Redstone Wire') if index==0 else ('绝缘红石线' if prefix.startswith('cable') else '红石线')
                data['block.gregtech.'+prefix+material]=names[index]+suffix
        data['message.gregtech.signal_wire.mode']=['Internal redstone source: %s','内置信号源强度：%s'][index]
        data['tooltip.gregtech.signal_wire.range']=['Redstone: approximately %s blocks per signal level','红石传输：每约 %s 格衰减一级'][index]
        data['tooltip.gregtech.signal_wire.controls']=['Wire cutter: connect ports. Screwdriver: source 0–15; sneak to decrease.','剪线钳调整连接；螺丝刀设置信号源 0–15，潜行时递减。'][index]
        write(path,data)

if __name__=='__main__':main()
