#!/usr/bin/env python3
"""Five isolated source-input cases; a fixture failure is never expected baseline RED."""
import argparse,json,re
from pathlib import Path
import xml.etree.ElementTree as ET
NS='gregtech_nonore_sifting_audit'
NEG={'flintpurifiedinputsrequiresourceorexternalorigin','icepurifiedinputsrequiresourceorexternalorigin','obsidianpurifiedinputsrequiresourceorexternalorigin'}
POS={'cinnabarwholeandtinysiftingkeepliteralsourcerows','legacyinputidsandlegitimateicegemformsremain'}
def check(xml,log,exit_file,phase):
 if exit_file.read_text().strip()!=('1' if phase=='baseline' else '0'):raise ValueError('wrong native exit')
 root=ET.parse(xml).getroot();cases=list(root.iter('testcase'))
 if len(cases)!=5 or {c.get('name') for c in cases}!=NEG|POS:raise ValueError('exact five cases required')
 if any(c.get('classname')!=NS+':test_empty' for c in cases):raise ValueError('wrong namespace/template')
 if any(e.tag in ('error','skipped') for e in root.iter()):raise ValueError('error/skipped evidence')
 if any(int(s.get(k,'0')) for s in root.iter() if s.tag in ('testsuite','testsuites') for k in ('errors','skipped')):raise ValueError('error/skipped suite metadata')
 failed={c.get('name') for c in cases if list(c.iter('failure'))}
 if failed!=(NEG if phase=='baseline' else set()):raise ValueError('wrong phase failure set')
 for c in cases:
  fs=list(c.iter('failure'))
  if fs and (len(fs)!=1 or 'source-NO fabricated Sifting inputs remain:' not in fs[0].get('message','')+(fs[0].text or '')):raise ValueError('fixture or unrelated failure')
 if phase=='fixed' and any(int(s.get('failures','0')) for s in root.iter() if s.tag in ('testsuite','testsuites')):raise ValueError('fixed suite failure metadata')
 text=log.read_text(errors='replace');rows=re.findall(r'\[nonore-sifting-input\] (\d+)/([^/]+)/([^ ]+) accepted=(\d+) selected=(true|false) warm=(true|false)',text)
 if text.count('[nonore-sifting-input] ')!=6:raise ValueError('unexpected/malformed role log count')
 wanted={(str(i),n,p) for i,n in [(8002,'Flint'),(9802,'Ice'),(8214,'Obsidian')] for p in ['crushedPurified','crushedPurifiedTiny']}
 if len(rows)!=6 or {r[:3] for r in rows}!=wanted:raise ValueError('exact six role observations missing')
 if phase=='fixed' and any(int(r[3]) or r[4:]!=('false','false') for r in rows):raise ValueError('fixed admissions contradict XML')
 if phase=='baseline' and any(not any((int(r[3]) or r[4:]!=('false','false')) for r in rows if r[1]==n) for n in ['Flint','Ice','Obsidian']):raise ValueError('no admitted evidence for baseline failure')
 return dict(phase=phase,cases=5,role_observations=6,whole_chain_claim=False)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--xml',type=Path,required=True);p.add_argument('--log',type=Path,required=True);p.add_argument('--exit-file',type=Path,required=True);p.add_argument('--phase',choices=['baseline','fixed'],required=True);a=p.parse_args()
 try:print(json.dumps(check(a.xml,a.log,a.exit_file,a.phase),indent=2))
 except Exception as e:p.exit(1,'FAIL: '+str(e)+'\n')
