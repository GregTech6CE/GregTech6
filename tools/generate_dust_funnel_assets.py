"""GT6 MultiTileEntityDustFunnel six render passes, empty top texture and metal tint."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
SIDES=("up","down","north","south","west","east")
def model(loader):
    children={}
    boxes=[([0,8,0],[16,16,16]),([2,0,2],[14,8,14]),
           ([0,0,0],[2,8,2]),([0,0,14],[2,8,16]),([14,0,0],[16,8,2]),([14,0,14],[16,8,16])]
    for layer in ("colored","overlay"):
        elements=[]
        for i,(low,high) in enumerate(boxes):
            faces={side:{"texture":"#"+("top" if side=="up" else "bottom" if side=="down" else "sides")} for side in SIDES if i==0 or side!="up"}
            if layer=="colored":
                for face in faces.values():face["tintindex"]=0
            elements.append({"from":low,"to":high,"faces":faces})
        textures={key:f"gregtech:block/machines/tools/dust_funnel/{layer}/{key}" for key in ("top","bottom","sides","hole")}
        children[layer]={"textures":textures,"elements":elements,"render_type":"minecraft:cutout"}
        # The original input face overlays its hole on top, never on the bottom nozzle.
        face={"texture":"#hole","uv":[0,0,16,16]}
        if layer=="colored":face["tintindex"]=0
        children[layer+"_hole"]={"textures":textures,"elements":[{"from":[0,16.001,0],"to":[16,16.001,16],"faces":{"up":face}}],"render_type":"minecraft:cutout"}
    return {"parent":"minecraft:block/block","loader":loader+":composite","children":children,
            "textures":{"particle":"gregtech:block/machines/tools/dust_funnel/colored/top"},
            "display":{"gui":{"rotation":[30,225,0],"translation":[0,0,0],"scale":[.625,.625,.625]}}}
if __name__=="__main__":
    for base,loader in (("src","forge"),("neoforge/src","neoforge")):
        path=ROOT/base/"main/resources/assets/gregtech/models/block/machine/tool/dust_funnel.json"
        path.write_text(json.dumps(model(loader),indent=2)+"\n",encoding="utf-8")
