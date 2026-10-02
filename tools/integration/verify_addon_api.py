"""Verify the compile-only SDK against current shared classes; this is not addon runtime evidence."""
from pathlib import Path
import argparse,hashlib,json,zipfile
from collections import Counter
from verify_artifacts import compiled_core_hashes

def inspect(path,repo):
    required=compiled_core_hashes(repo/'core/build/classes/java/main')
    with zipfile.ZipFile(path) as archive:
        counts=Counter(archive.namelist())
        if any(n!=1 for n in counts.values()):raise ValueError('Duplicate SDK entries')
        actual={name for name in counts if name.endswith('.class')}
        if actual!=set(required):raise ValueError('SDK classes differ from current core compilation')
        for name,expected in required.items():
            data=archive.read(name)
            if hashlib.sha256(data).hexdigest()!=expected or data[:4]!=b'\xca\xfe\xba\xbe' or int.from_bytes(data[6:8],'big')!=61:
                raise ValueError('Stale or non-Java17 SDK class: '+name)
        if any(name.startswith(('assets/','data/')) or name in ('META-INF/mods.toml','META-INF/neoforge.mods.toml') for name in counts):raise ValueError('SDK contains runtime assets or loader descriptor')
        for name,source in [('LICENSE','LICENSE'),('GPL-3.0.txt','docs/licenses/GPL-3.0.txt'),('NOTICE','NOTICE')]:
            if counts.get('META-INF/gregtech6/'+name)!=1 or archive.read('META-INF/gregtech6/'+name)!=(repo/source).read_bytes():raise ValueError('SDK license/notice differs: '+name)
        if any('META-INF/gregtech6/'+name in counts for name in ['COPYING.LESSER','COPYING','LICENSE.txt']):raise ValueError('SDK contains superseded duplicate licenses')
    return {'path':str(path.resolve()),'bytes':path.stat().st_size,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'shared_classes':len(required),'java_class_version':61,'compile_only':True,'external_addon_runtime_verified':False}

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--jar',type=Path,required=True);parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
    result=inspect(args.jar,Path(__file__).resolve().parents[2]);args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print('Addon SDK packaging passed: '+str(result['shared_classes'])+' current Java17 classes; no runtime assets')
