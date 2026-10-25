#!/usr/bin/env python3
"""A native XML pass without exact fresh-JVM startup evidence and synchronous restoration receipt is not success."""
import argparse,json,re
from pathlib import Path
import xml.etree.ElementTree as ET
NS='gregtech_ore_form_startup_audit';CASE='acceptedcoreformandpublicstartupwinnersurviveoverlay'
def unique(pairs):
 d={}
 for k,v in pairs:
  if k in d:raise ValueError('duplicate JSON key')
  d[k]=v
 return d
def check(xml,log,exit_file,run,nonce):
 if not re.fullmatch('[0-9a-f]{32}',nonce):raise ValueError('fresh nonce required')
 if exit_file.read_text().strip()!='0':raise ValueError('native process did not exit0')
 run=run.resolve(strict=True)
 if not run.is_dir() or not run.name.startswith('gt6-ore-form-startup-'):raise ValueError('wrong disposable run')
 root=ET.parse(xml).getroot();cases=list(root.iter('testcase'))
 if len(cases)!=1 or cases[0].get('name')!=CASE or cases[0].get('classname')!=NS+':test_empty':raise ValueError('exact single case/namespace/template required')
 if any(e.tag in ('failure','error','skipped') for e in root.iter()):raise ValueError('native XML not passing')
 if any(int(s.get(k,'0')) for s in root.iter() if s.tag in ('testsuite','testsuites') for k in ('failures','errors','skipped')):raise ValueError('suite failed')
 files=list(run.rglob('.ore-form-startup-cleanup.json'))
 if len(files)!=1:raise ValueError('exact cleanup receipt required')
 receipt=json.loads(files[0].read_text(),object_pairs_hook=unique)
 for k,v in dict(schema=1,namespace=NS,nonce=nonce,restored=True,acceptedCoreForm=True,publicStartupWinner=True,winnerIdentityPreserved=True,rawControlPreserved=True,probePassed=True,verifiedRounds=2,form="addon_sample:Dilithium/crushedPurifiedTiny",winner="Dolamide/crushed/Water900").items():
  if type(receipt.get(k)) is not type(v) or receipt[k]!=v:raise ValueError('invalid receipt '+k)
 world=Path(receipt['world']).resolve(strict=True)
 if world==run or not world.is_relative_to(run) or files[0].parent.resolve()!=world:raise ValueError('receipt world mismatch')
 if (world/'.ore-form-startup-used').read_text()!=nonce:raise ValueError('world marker nonce mismatch')
 if list(run.rglob('.ore-form-startup-cleanup.tmp')):raise ValueError('unfinished cleanup')
 original=receipt.get('originalPacks');restored=receipt.get('restoredPacks')
 if not isinstance(original,list) or any(not isinstance(v,str) or not v for v in original) or len(set(original))!=len(original) or restored!=original:raise ValueError('exact ordered packs not restored')
 text=log.read_text(errors='replace');records=[l.split('[ore-form-startup-cleanup] ',1)[1] for l in text.splitlines() if '[ore-form-startup-cleanup] ' in l]
 if len(records)!=1 or json.loads(records[0],object_pairs_hook=unique)!=receipt:raise ValueError('receipt log mismatch')
 if text.count('[ore-form-startup-winner] accepted=true')!=1 or '[ore-form-startup-winner] accepted=false' in text:raise ValueError('exact real startup acceptance missing')
 return dict(cases=1,verified_rounds=2,accepted_core_form=True,public_startup_winner=True,restored=True,real_resource_reload=False,actual_stop=False)

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--xml',type=Path,required=True);p.add_argument('--log',type=Path,required=True);p.add_argument('--exit-file',type=Path,required=True);p.add_argument('--run-directory',type=Path,required=True);p.add_argument('--nonce',required=True);a=p.parse_args()
 try:print(json.dumps(check(a.xml,a.log,a.exit_file,a.run_directory,a.nonce),indent=2))
 except Exception as e:p.exit(1,'FAIL: '+str(e)+'\n')
