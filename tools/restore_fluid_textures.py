"""Restore original GT6 named fluid PNGs and their animation metadata, byte for byte."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

from pathlib import Path
import hashlib
import json
import shutil

ROOT=Path(__file__).resolve().parents[1]

def restore(original):
    source=native_path(original/'src/main/resources/assets/gregtech/textures/blocks/fluids')
    assets=native_path(ROOT/'src/main/resources/assets/gt6ce')
    ledger_path=ROOT/'tools/gt6_texture_sources.json'
    ledger=json.loads(ledger_path.read_text(encoding='utf-8'))
    changed=0
    for png in source.glob('*.png'):
        for suffix in ['', '.mcmeta']:
            src=Path(str(png)+suffix)
            relative='textures/block/fluids/'+src.name.lower()
            target=assets/relative
            if not src.exists():
                # A stale animation descriptor can break a restored non-animated sprite.
                if suffix and target.exists():target.unlink();changed+=1
                continue
            if not target.exists() or target.read_bytes()!=src.read_bytes():
                target.parent.mkdir(parents=True,exist_ok=True)
                shutil.copy2(src,target);changed+=1
            ledger[relative]={'source':src.relative_to(original).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()}
    ledger_path.write_text(json.dumps(ledger,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(f'{changed} files restored; {len(list(source.glob("*.png")))} original fluid sprites verified')

if __name__=='__main__':
    import sys
    restore(Path(sys.argv[1]))
