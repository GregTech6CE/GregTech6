"""Original15 small steam turbines: read-only names, order, selected source assets and native visuals."""
import argparse,copy,hashlib,itertools,json,re
from pathlib import Path

DEVICES=[(1512,'bronze','Bronze'),(1515,'brass','Brass'),(1518,'invar','Invar'),(1522,'steel','Steel'),
         (1525,'chromium','Chromium'),(1527,'ironwood','Ironwood'),(1528,'steeleaf','Steeleaf'),(1529,'thaumium','Thaumium'),
         (1530,'titanium','Titanium'),(1531,'fiery_steel','Fiery Steel'),(1535,'aluminium','Aluminium'),(1538,'magnalium','Magnalium'),
         (1540,'void_metal','Void Metal'),(1545,'trinitanium','Trinitanium'),(1548,'graphene','Graphene')]
ROTATIONS={'north':{},'east':{'y':90},'south':{'y':180},'west':{'y':270},'up':{'x':270},'down':{'x':90}}
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,value):p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
    ap=argparse.ArgumentParser()
    for name in ('source','comparison','zh-patch','audit'):ap.add_argument('--'+name,type=Path,required=True)
    ap.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2]);ns=ap.parse_args()
    loader=ns.source/'src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java'
    rows=re.findall(r'aRegistry\.add\(([^,\n]+),\s*"([^"]+)"\s*,\s*([^,]+)\s*,\s*(\d+)\s*,\s*([\w.]+)',loader.read_text(encoding='utf-8'))
    chinese=dict(line.lstrip()[2:].split('=',1) for line in ns.zh_patch.read_text(encoding='utf-8').splitlines() if line.lstrip().startswith('S:') and '=' in line)
    langs={k:json.loads((ns.repo/f'core/src/main/resources/assets/gregtech/lang/{k}.json').read_text(encoding='utf-8')) for k in ('en_us','zh_cn')}
    assets,variants,models={},{},{}
    for layer in ('colored','overlay','overlay_active_ls','overlay_active_lf','overlay_active_rs','overlay_active_rf'):
        for face in ('front','back','side'):
            rel=f'assets/gregtech/textures/blocks/machines/turbines/rotation_steam/{layer}/{face}.png'
            canonical=ns.source/'src/main/resources'/rel
            donor=canonical if canonical.is_file() else ns.comparison/'src/main/resources'/rel
            target=ns.repo/f'core/src/main/resources/assets/gregtech/textures/block/machines/turbines/rotation_steam/{layer}/{face}.png'
            for orig,out in [(donor,target),(Path(str(donor)+'.mcmeta'),Path(str(target)+'.mcmeta'))]:
                if not orig.is_file():
                    if orig==donor:raise ValueError('Missing source steam texture '+str(orig))
                    continue
                if not out.is_file() or sha(orig)!=sha(out):raise ValueError('Inspect existing steam asset before replacement '+str(out))
                assets[str(out.relative_to(ns.repo))]={'path':str(orig),'sha256':sha(orig)}
    for source_id,suffix,name in DEVICES:
        id='steam_turbine_'+suffix
        ordinal,row=next((i,r) for i,r in enumerate(rows) if r[2].strip()==str(source_id))
        if row[1]!='Turbines' or not row[0].startswith('"Steam Turbine ("'):raise ValueError(row)
        source_key=f'gt.multitileentity.{source_id}'
        langs['en_us']['block.gregtech.'+id]='Steam Turbine ('+name+')';langs['zh_cn']['block.gregtech.'+id]=chinese[source_key]
        variants[id]={'source_id':source_id,'source_key':source_key,'name':'Steam Turbine ('+name+')','zh_cn':chinese[source_key],'source_order':ordinal}
        for platform in ('src','neoforge/src'):
            root=ns.repo/platform/'main/resources/assets/gregtech'
            base=json.loads((root/'models/block/machine/energy/steam_turbine_bronze.json').read_text(encoding='utf-8'))
            if base['loader']!=('forge:composite' if platform=='src' else 'neoforge:composite'):raise ValueError('platform loader')
            for kind in ('','ls','lf','rs','rf'):
                model=copy.deepcopy(base)
                if kind:
                    tex=model['children']['layer1']['textures']
                    for face,ref in tex.items():tex[face]=ref.replace('/overlay/','/overlay_active_'+kind+'/')
                out=root/f'models/block/machine/energy/{id}{"_"+kind if kind else ""}.json';write(out,model);models[str(out.relative_to(ns.repo))]=sha(out)
            states={}
            for face,rotation in ROTATIONS.items():
                for activity,left,fast in itertools.product(range(3),(False,True),(False,True)):
                    key=f'facing={face},activity={activity},counter_clockwise={str(left).lower()},fast={str(fast).lower()}'
                    ext='_'+('l' if left else 'r')+('f' if fast else 's') if activity else ''
                    states[key]={'model':f'gregtech:block/machine/energy/{id}{ext}',**rotation}
            out=root/f'blockstates/{id}.json';write(out,{'variants':states});models[str(out.relative_to(ns.repo))]=sha(out)
            out=root/f'models/item/{id}.json';write(out,{'parent':f'gregtech:block/machine/energy/{id}'});models[str(out.relative_to(ns.repo))]=sha(out)
    for key,data in langs.items():write(ns.repo/f'core/src/main/resources/assets/gregtech/lang/{key}.json',data)
    p=ns.repo/'core/src/main/java/com/gregtech/gregtech/content/creative/SourceCreativeCatalog.java'
    s=p.read_text(encoding='utf-8')
    if 'initSteamTurbines();' not in s:s=s.replace('initMagnets();','initMagnets();\ninitSteamTurbines();')
    s=re.sub(r'private static void initSteamTurbines\(\)\{[\s\S]*?\}\n','',s)
    method='private static void initSteamTurbines(){\n'+''.join(f'ITEMS.put("{id}",new Entry("turbines",{v["source_order"]}));\n' for id,v in variants.items())+'}\n'
    p.write_text(s.replace('public static Entry entry(',method+'public static Entry entry('),encoding='utf-8')
    ns.audit.parent.mkdir(parents=True,exist_ok=True)
    write(ns.audit,{'variants':variants,'assets':assets,'native_files':models,'selectors_per_platform':1080,'native_states_per_platform':2160,
         'authors':['GregTech-6 Team','Gregorius Techneticies'],'code_license':'LGPL-3.0-or-later','asset_license':'CC0-1.0 per preserved LICENSE.assets',
         'source_files':[{'path':str(p),'sha256':sha(p)} for p in (loader,ns.zh_patch,ns.source/'LICENSE',ns.comparison/'LICENSE.assets')],
         'scope':'Static source import and references; runtime baking / animation / tint not claimed.'})
    print(json.dumps({'variants':15,'existing_assets_checked':len(assets),'native_files':len(models),'native_states_per_platform':2160}))
if __name__=='__main__':main()
