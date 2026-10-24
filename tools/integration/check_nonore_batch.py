#!/usr/bin/env python3
import argparse,json,re,hashlib
from pathlib import Path
import xml.etree.ElementTree as ET
NS='gregtech_ore_alignment_batch';NEG='reviewedrawcohortrequiressourceorexternalinput'
CASES={NEG,'cinnabarrawchanceandhammerremainliteral','acceptedrawrequestunknownandaddonretainbothmaps','blockandvanillacallersremainaccepted','legitimaterockdustmortarandnativefurnaceremain'}
EXPECTED_FAILURES={NEG:'source-NO fabricated raw Crusher/Hammer remain:',
 'reviewedsluicecohortrequiressourceorexternalinput':'source-NO fabricated Sluice remain:',
 'reviewedmagneticclassesuseoppositesourceproducts':'source magnetic class/output mismatches:',
 'wholedustthermalusessourcetargetpermissionandwork':'source Ti/Fe whole-dust thermal mismatches:'}
CASES.update({'reviewedsluicecohortrequiressourceorexternalinput','reviewedmagneticclassesuseoppositesourceproducts',
 'sourcesluicepositivesunknownandoverridesremain','retainedpackingandcinnabarthermalremainliteral',
 'wholedustthermalusessourcetargetpermissionandwork'})

ROOT=next(p for p in Path(__file__).resolve().parents if (p/'docs/integration/nonore-input-source-profile.json').is_file())
PROFILE=ROOT/'docs/integration/nonore-input-source-profile.json'
def identities():return {(str(r['id']),r['name']) for r in json.loads(PROFILE.read_text())['rows'] if r['source_ores']=='NO'}
def check(xml,log,exit_file,phase):
 if exit_file.read_text().strip()!=('1' if phase=='baseline' else '0'):raise ValueError('phase process exit mismatch')
 root=ET.parse(xml).getroot();cases=list(root.iter('testcase'))
 expected_cases=CASES|({'waterkeysretainfluidoutputscopiesandexactownership'} if phase=='fixed' else set())
 if len(cases)!=len(expected_cases) or {c.get('name') for c in cases}!=expected_cases:raise ValueError('exact phase ten/eleven cases required')
 if any(c.get('classname')!=NS+':test_empty' for c in cases):raise ValueError('wrong namespace/template')
 if any(e.tag in ('error','skipped') for e in root.iter()):raise ValueError('error/skipped evidence')
 for s in root.iter():
  if s.tag in ('testsuite','testsuites') and any(int(s.get(k,'0')) for k in ('errors','skipped')):raise ValueError('bad suite metadata')
 failed=[c for c in cases if list(c.iter('failure'))];fs=list(root.iter('failure'))
 if phase=='baseline':
  if {c.get('name') for c in failed}!=set(EXPECTED_FAILURES) or len(fs)!=4:raise ValueError('baseline exact four semantic failures required')
  for c in failed:
   failures=list(c.iter('failure'))
   if len(failures)!=1 or EXPECTED_FAILURES[c.get('name')] not in failures[0].get('message','')+(failures[0].text or ''):raise ValueError('unexpected fixture/semantic failure '+c.get('name'))
 else:
  if fs or any(int(s.get('failures','0')) for s in root.iter() if s.tag in ('testsuite','testsuites')):raise ValueError('fixed failures')
 text=log.read_text(errors='replace');expected=identities();assert len(expected)==53
 classes=re.findall(r'\[nonore-raw-classification\] (\d+)/([^ ]+) overrides=([^\n\r]+)',text)
 if len(classes)!=53 or {(i,n) for i,n,_ in classes}!=expected:raise ValueError('exact53 classifications required')
 for _,n,reasons in classes:
  actual=set(reasons.strip('[]').split(', ')) if reasons!='[]' else set()
  if actual!=({'requested:crushed','requested:oreRaw'} if n=='Bakelite' else set()):raise ValueError('unexpected override '+n)
 facts=re.findall(r'\[nonore-raw-facts\] ([^ ]+) target=([^ ]+) amount=(\d+) multiplier=(\d+)/(\d+) quality=(\d+)',text)
 if len(facts)!=53 or {v[0] for v in facts}!={n for _,n in expected}:raise ValueError('missing/duplicate actual raw facts')
 rows=re.findall(r'\[nonore-raw-row\] (\d+)/([^ ]+) map=([^ ]+) accepted=(\d+) selected=(true|false) warm=(true|false)',text)
 maps=['gt.recipe.crusher','gt.recipe.hammer'];wanted={(i,n,m) for i,n in expected for m in maps}
 if len(rows)!=106 or {r[:3] for r in rows}!=wanted or text.count('[nonore-raw-row] ')!=106:raise ValueError('exact106 raw roles required')
 if any(r[4]!=r[5] for r in rows):raise ValueError('warm/cold mismatch')
 if phase=='fixed' and any(int(r[3]) or r[4:]!=('false','false') for r in rows if r[1]!='Bakelite'):raise ValueError('fixed own rows retained')
 if phase=='baseline' and not any(int(r[3]) and r[4:] ==('true','true') for r in rows if r[1]!='Bakelite'):raise ValueError('baseline missing actual accepted evidence')
 if any(int(r[3])<1 or r[4:]!=('true','true') for r in rows if r[1]=='Bakelite'):raise ValueError('accepted override not retained')
 literals=re.findall(r'\[nonore-raw-literal\] ([^/]+)/([^ ]+) verified=true',text)
 expected_literals={(n,m) for n in ['Wood','Snow','Ice','PVC'] for m in maps} if phase=='baseline' else set()
 if len(literals)!=len(expected_literals) or set(literals)!=expected_literals:raise ValueError('literal representative evidence incomplete')
 preserved=[l.split('[nonore-raw-preserved] ',1)[1] for l in text.splitlines() if '[nonore-raw-preserved] ' in l]
 if len(preserved)!=20 or len(set(preserved))!=20:raise ValueError('exact20 independent preserved controls required')
 dust=[l.split('[nonore-raw-preserved-dust] ',1)[1] for l in text.splitlines() if '[nonore-raw-preserved-dust] ' in l]
 if not dust:raise ValueError('missing actual dust preservation census')
 sluice=re.findall(r'\[nonore-sluice-row\] ([^/]+)/([^/]+)/([^ ]+) accepted=(\d+) selected=(true|false) noOverride=(true|false)',text)
 wanted={(n,size,w) for _,n in expected for size in ['whole','tiny'] for w in ['Water','MnWtr','DistW','SpDew']}
 if len(sluice)!=424 or {r[:3] for r in sluice}!=wanted:raise ValueError('exact424 Sluice roles required')
 for n,size,w,count,selected,no in sluice:
  if no!=('false' if n=='Bakelite' else 'true'):raise ValueError('Sluice override classification mismatch')
  if n=='Bakelite' and (count!='1' or selected!='true'):raise ValueError('Sluice startup override absent')
  if phase=='fixed' and n!='Bakelite' and (count!='0' or selected!='false'):raise ValueError('Sluice owned row retained')
 preserved_sluice=re.findall(r'\[nonore-sluice-preserved\] ([^ ]+) ',text)
 if len(preserved_sluice)!=24 or len(set(preserved_sluice))!=24:raise ValueError('Sluice24 preservation controls required')
 magnetic=re.findall(r'\[ore-batch-magnetic\] (\d+)/([^/]+)/([^ ]+) expected=([^\n]*?) accepted=(\d+) selected=(true|false) deferred=(true|false)',text)
 if len(magnetic)!=1078 or len({r[:3] for r in magnetic})!=1078 or any(r[-1]!='false' for r in magnetic):raise ValueError('magnetic1078 unique nondeferred controlled roles required')
 if {r[:3] for r in magnetic}!={(i,n,size) for i,n,*_ in magnetic for size in ['whole','tiny']}:raise ValueError('magnetic exact whole/tiny role keys required')
 cohort=sorted({i+'/'+n for i,n,*_ in magnetic})
 if len(cohort)!=539 or hashlib.sha256(('\n'.join(cohort)+'\n').encode()).hexdigest()!='6eeb4f929b95684e8bf1b57fec8e6614d4bb9b590a7a85442b1fe4fee1a7659b':raise ValueError('magnetic exact539 identity cohort mismatch')
 if text.count('[ore-batch-magnetic-cohort] sha256=6eeb4f929b95684e8bf1b57fec8e6614d4bb9b590a7a85442b1fe4fee1a7659b')!=1:raise ValueError('native identity digest missing/duplicate')
 if text.count('[ore-batch-magnetic-summary] materials=539 roles=1078 deferred=0 positives=84')!=1:raise ValueError('magnetic summary missing/duplicate')
 thermal=re.findall(r'\[whole-dust-thermal-census\] ([^ ]+) accepted=',text)
 if len(thermal)!=4 or set(thermal)!={'Titanium/Melter','Titanium/Smelter','Iron/Melter','Iron/Smelter'}:raise ValueError('thermal exactfour observations required')
 if '[whole-dust-thermal-deferred]' in text:raise ValueError('thermal fixture facts deferred; not expected RED')
 facts=re.findall(r'\[whole-dust-thermal-facts\] (Titanium|Iron) canonical=true ordinary=true unchanged=true ',text)
 if len(facts)!=2 or set(facts)!={'Titanium','Iron'}:raise ValueError('thermal independent native facts missing')
 packing=re.findall(r'\[ore-batch-packing\] ([^ ]+) verified=true',text)
 if len(packing)!=24 or len(set(packing))!=24:raise ValueError('packing24 literal controls missing/duplicate')
 return {'phase':phase,'cases':len(expected_cases),'classifications':53,'raw_roles':106,'preserved_controls':20,'dust_rows':len(dust),'sluice_roles':424,'magnetic_roles':1078,'thermal_roles':4,'packing_controls':24,'whole_chain_claim':False}
def compare(baseline,fixed):
 def rows(p):
  lines=p.read_text(errors='replace').splitlines()
  return {tag:sorted(l.split(tag,1)[1] for l in lines if tag in l) for tag in ['[nonore-raw-preserved] ','[nonore-raw-preserved-dust] ','[nonore-sluice-preserved] ']}
 a,b=rows(baseline),rows(fixed)
 if any(not v for v in a.values()) or a!=b:raise ValueError('preserved semantic row multiset changed/missing; duplicates significant')
 return {k:len(v) for k,v in a.items()}
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--xml',type=Path,required=True);p.add_argument('--log',type=Path,required=True);p.add_argument('--exit-file',type=Path,required=True);p.add_argument('--phase',choices=['baseline','fixed'],required=True);p.add_argument('--baseline-log',type=Path);a=p.parse_args()
 try:
  result=check(a.xml,a.log,a.exit_file,a.phase)
  if a.baseline_log:
   if a.phase!='fixed':raise ValueError('baseline comparison only for fixed')
   result['cross_revision_preserved']=compare(a.baseline_log,a.log)
  print(json.dumps(result,indent=2))
 except Exception as e:p.exit(1,'FAIL: '+str(e)+'\n')
