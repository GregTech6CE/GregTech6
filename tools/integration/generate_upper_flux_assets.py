"""Upper three RF rotary grades use the exact same models as their existing MV grade."""
from pathlib import Path
import json, re
ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'multiversion/src/main/resources/assets/gt6ce'

def main():
    for family in ('flux_motor', 'flux_dynamo'):
        for path in ASSETS.rglob(family + '_mv*.json'):
            for tier in ('hv', 'ev', 'iv'):
                target = path.with_name(path.name.replace('_mv', '_' + tier))
                target.write_text(path.read_text(encoding='utf-8').replace(family + '_mv', family + '_' + tier), encoding='utf-8')
    # Names stay bound to source material names and the original translation patch.
    patch = dict(re.findall(r'^\s*S:([^=]+)=(.*)$', (ROOT.parent/'GregTech_zh_cn.lang').read_text(encoding='utf-8'), re.M))
    for lang in ('en_us','zh_cn'):
        file = ROOT / f'core/src/main/resources/assets/gt6ce/lang/{lang}.json'
        values = json.loads(file.read_text(encoding='utf-8'))
        for index, (tier, material) in enumerate(zip(('hv','ev','iv'),('Electrum','Enderium Base','Enderium')), 3):
            for family, base, title in (('flux_motor',11020,'Flux Motor'),('flux_dynamo',11110,'Flux Dynamo')):
                values['block.gt6ce.' + family + '_' + tier] = patch['gt.multitileentity.' + str(base + index)] if lang == 'zh_cn' else f'{title} ({material})'
        file.write_text(json.dumps(values,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

if __name__ == '__main__': main()
