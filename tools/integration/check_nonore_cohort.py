#!/usr/bin/env python3
"""Exact isolated three-case native gate; expected RED is restricted to the source-policy assertion."""
import argparse,json,re
from pathlib import Path
import xml.etree.ElementTree as ET
NS='gregtech_nonore_cohort_audit'
CASES={
 'sourcenocohortreportsacceptedrowsandsuppressesonlyeligiblemacerationwashing',
 'acceptedstartuprequestknownoresandunknownandaddonkeeptheirchains',
 'dustrecyclingrockswoodmortarandforcediceformssurviveoverlay'}
NEG='sourcenocohortreportsacceptedrowsandsuppressesonlyeligiblemacerationwashing'
def check(xml,exit_file,log,phase):
 code=exit_file.read_text().strip()
 if code!=('1' if phase=='baseline' else '0'):raise ValueError('unexpected native process exit for phase')
 root=ET.parse(xml).getroot();cases=list(root.iter('testcase'))
 if len(cases)!=3 or {c.get('name') for c in cases}!=CASES:raise ValueError('exact three native cases required')
 if any(c.get('classname')!=NS+':test_empty' for c in cases):raise ValueError('namespace/template mismatch')
 if any(e.tag in ('error','skipped') for e in root.iter()):raise ValueError('error/skipped native evidence')
 if any(int(s.get(k,'0')) for s in root.iter() if s.tag in ('testsuite','testsuites') for k in ('errors','skipped')):raise ValueError('error/skipped suite evidence')
 failing=[c for c in cases if list(c.iter('failure'))]
 if phase=='fixed':
  if failing:raise ValueError('fixed native failures')
  if any(int(s.get(k,'0')) for s in root.iter() if s.tag in ('testsuite','testsuites') for k in ('failures','errors','skipped')):raise ValueError('fixed suite failure metadata')
 else:
  if len(failing)!=1 or failing[0].get('name')!=NEG:raise ValueError('baseline must fail only source-NO policy case')
  fs=list(failing[0].iter('failure'))
  if len(list(root.iter('failure')))!=1:raise ValueError('unexpected baseline failure elements')
  if len(fs)!=1 or 'bounded source-owned input failures=' not in (fs[0].get('message','')+(fs[0].text or '')):raise ValueError('baseline failed fixture or different assertion, not policy')
 text=log.read_text(errors='replace')
 actual=re.findall(r'\[nonore-cohort-classification\] (\d+)/([^ ]+) source=NO overrides=([^\n\r]*)',text)
 profile=json.loads((Path(__file__).resolve().parents[2]/'docs/integration/nonore-input-source-profile.json').read_text())
 expected={(str(r['id']),r['name']) for r in profile['rows'] if r['source_ores']=='NO'}
 if len(actual)!=53 or {(i,n) for i,n,_ in actual}!=expected:raise ValueError('actual53 classification census incomplete/duplicated')
 for _,name,reasons in actual:
  if reasons.strip()!=('[requested:crushed]' if name=='Bakelite' else '[]'):raise ValueError('unexpected startup/external fixture classification: '+name)
 rowlines=[l for l in text.splitlines() if '[nonore-cohort-row] ' in l]
 if len(rowlines)!=53*14:raise ValueError('exact742 scoped role observations required')
 parsed=[re.search(r'\[nonore-cohort-row\] ([^/]+)/([^ ]+) map=([^ ]+) accepted=(\d+) selected=(true|false)',line) for line in rowlines]
 if any(m is None for m in parsed):raise ValueError('malformed role observation')
 six=['crushed','crushedTiny','crushedPurified','crushedPurifiedTiny','crushedCentrifuged','crushedCentrifugedTiny']
 roles=[(p,'gt.recipe.'+m) for p in six for m in ['crusher','shredder']]+[('crushed','gt.recipe.bath'),('crushedPurified','gt.recipe.centrifuge')]
 wanted={(n,p,m) for _,n in expected for p,m in roles}
 if {m.groups()[:3] for m in parsed}!=wanted:raise ValueError('scoped role identities incomplete/duplicated')
 offenders=[m for m in parsed if m.group(1)!='Bakelite' and (int(m.group(4))>0 or m.group(5)=='true')]
 if phase=='fixed' and offenders:raise ValueError('fixed XML contradicts admitted eligible-NO role observations')
 if phase=='baseline' and not offenders:raise ValueError('baseline must observe actual eligible-NO admissions/lookups')
 snapshots=[l.split('[nonore-dust-snapshot] ',1)[1] for l in text.splitlines() if '[nonore-dust-snapshot] ' in l]
 if len(snapshots)!=1 or not isinstance(json.loads(snapshots[0]),list) or not json.loads(snapshots[0]):raise ValueError('actual nonempty dust snapshot missing')
 return {'phase':phase,'native_cases':3,'expected_policy_failure':phase=='baseline','source_NO':53,'eligible_NO':52,'startup_defer':1,'external_defer':0,'observed_roles':742,'whole_chain_alignment_claimed':False}
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--xml',type=Path,required=True);p.add_argument('--exit-file',type=Path,required=True);p.add_argument('--log',type=Path,required=True);p.add_argument('--phase',choices=['baseline','fixed'],required=True);a=p.parse_args()
 try:print(json.dumps(check(a.xml,a.exit_file,a.log,a.phase),indent=2))
 except Exception as e:p.exit(1,'FAIL: '+str(e)+'\n')
