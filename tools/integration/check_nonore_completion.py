#!/usr/bin/env python3
"""Strict isolated six-case read-only baseline/fixed checker. No native launches."""
import argparse,json,re
from collections import Counter
from pathlib import Path
import xml.etree.ElementTree as ET
NS='gregtech_ore_completion_batch'
FAILURES={'slurryrecyclingpreservessourcefractionsandwaters':'source slurry recycling mismatches:',
'anvilrockandrawusesourcetargetandwork':'source Anvil target/work mismatches:',
'fractionaltitaniumironusesourcethermalrows':'source fractional Ti/Fe thermal mismatches:'}
CONTROLS={'existingwholedustandcinnabarthermalremainliteral','existingrawbonusandcoppercentrifugeremainliteral','remainingthermalandcentrifugecensusisobservational'}
def check(xml,log,exit_file,phase):
 root=ET.parse(xml).getroot();cases=list(root.iter('testcase'));wanted=set(FAILURES)|CONTROLS
 if len(cases)!=6 or {c.get('name') for c in cases}!=wanted:raise ValueError('exact six case identities required')
 if any(c.get('classname')!=NS+':test_empty' for c in cases):raise ValueError('wrong namespace/template')
 if any(e.tag in ('error','skipped') for e in root.iter()):raise ValueError('error/skipped forbidden')
 for e in root.iter():
  if e.tag in ('testsuite','testsuites') and any(int(e.get(k,'0')) for k in ('errors','skipped')):raise ValueError('error/skipped metadata')
 failed={c.get('name') for c in cases if list(c.iter('failure'))}
 if phase=='baseline':
  if failed!=set(FAILURES) or len(list(root.iter('failure')))!=3:raise ValueError('baseline requires exact three semantic failures')
  for c in cases:
   if c.get('name') in FAILURES:
    fs=list(c.iter('failure'))
    if len(fs)!=1 or FAILURES[c.get('name')] not in fs[0].get('message','')+(fs[0].text or ''):raise ValueError('fixture/unexpected semantic failure '+c.get('name'))
 else:
  if failed or any(int(e.get('failures','0')) for e in root.iter() if e.tag in ('testsuite','testsuites')):raise ValueError('fixed failures')
 if exit_file.read_text().strip()!=('1' if phase=='baseline' else '0'):raise ValueError('native process exit mismatch')
 text=log.read_text(errors='replace')
 if '[whole-dust-thermal-deferred]' in text:raise ValueError('preserved whole thermal control deferred')
 if text.count('[whole-dust-thermal-facts] ')!=2 or text.count('[whole-dust-thermal-census] ')!=4:raise ValueError('preserved whole thermal vectors incomplete')
 for value in ['thermal16+whole4','raw1+centrifuge2']:
  if text.count('[ore-completion-preserved] '+value+' verified=true')!=1:raise ValueError('missing/duplicate preserved control '+value)
 slurry=re.findall(r'\[slurry-recycling-role\] ([^ ]+) accepted=(\d+) selected=(true|false) warm=(true|false) exact=(true|false)',text)
 slurry_keys={f'Mixer/{size}/{water}' for size in ['tiny','whole'] for water in ['Water','MnWtr','DistW','SpDew']}|{'Drying/tiny'}
 if len(slurry)!=9 or {r[0] for r in slurry}!=slurry_keys or any(r[3]!='true' for r in slurry):raise ValueError('exact nine complete stable slurry roles required')
 if phase=='fixed' and any(r[4]!='true' for r in slurry):raise ValueError('fixed slurry literal mismatch')
 if phase=='baseline' and not any(r[4]=='false' for r in slurry):raise ValueError('baseline lacks actual slurry mismatch')
 slurry_details=re.findall(r'\[slurry-recycling-row\] ([^ ]+) selected=(true|false) actualSelected=(true|false) actualWarm=(true|false) ',text)
 if len(slurry_details)!=text.count('[slurry-recycling-row] ') or any(r[3]!='true' for r in slurry_details):raise ValueError('slurry accepted detail warm census incomplete')
 if Counter(r[0] for r in slurry_details)!=Counter({r[0]:int(r[1]) for r in slurry if int(r[1])}):raise ValueError('slurry accepted detail counts disagree')
 if text.count('[slurry-recycling-preserved] Mixer/whole/Water ')!=1:raise ValueError('existing full Water slurry vector changed')
 anvil_keys={'Stone/rockGt','NetherBrick/rockGt','Cinnabar/oreRaw'}
 anvil_facts=re.findall(r'\[anvil-source-facts\] ([^ ]+) canonical=(true|false) inputIdentity=(true|false) ordinary=(true|false) conditions=(true|false)',text)
 if len(anvil_facts)!=3 or {r[0] for r in anvil_facts}!=anvil_keys or any(v!='true' for r in anvil_facts for v in r[1:]):raise ValueError('Anvil native fixture/ownership/conditions changed')
 anvil=re.findall(r'\[anvil-source-census\] ([^ ]+) accepted=(\d+) cold=([^\n]+)',text)
 if len(anvil)!=3 or {r[0] for r in anvil}!=anvil_keys or any('warmSame=true' not in r[2] for r in anvil):raise ValueError('Anvil three complete stable observations required')
 frac_keys={f'{m}/{p}' for m in ['Titanium','Iron'] for p in ['dustSmall','dustTiny']}
 frac_facts=re.findall(r'\[fractional-thermal-facts\] ([^ ]+) canonical=(true|false) ordinary=(true|false) unchanged=(true|false) sourceMb=(\d+) sourceTicks=(\d+)',text)
 if len(frac_facts)!=4 or {r[0] for r in frac_facts}!=frac_keys or any(v!='true' for r in frac_facts for v in r[1:4]):raise ValueError('fractional native fixture altered/missing')
 expected_fractions={'Titanium/dustSmall':('36','129'),'Titanium/dustTiny':('16','57'),'Iron/dustSmall':('36','207'),'Iron/dustTiny':('16','92')}
 if any(tuple(r[4:])!=expected_fractions[r[0]] for r in frac_facts):raise ValueError('literal fractional source oracle changed')
 frac=re.findall(r'\[fractional-thermal-census\] ([^ ]+) accepted=(\d+) cold=([^\n]+)',text)
 if len(frac)!=8 or {r[0] for r in frac}!={k+'/'+m for k in frac_keys for m in ['Melter','Smelter']} or any('warmSame=true' not in r[2] for r in frac):raise ValueError('fractional eight complete stable observations required')
 records=re.findall(r'\[ore-completion-census\] (\d+)/([^/]+)/([^/]+)/([^ ]+) accepted=(\d+) selected=(true|false) dynamic=(true|false) addon=(true|false) quality=(-?\d+) target=([^ ]+) amount=(-?\d+)',text)
 totals=re.findall(r'\[ore-completion-census-total\] materials=(\d+) roles=(\d+) accepted=(\d+) selected=(\d+) sourceParityClaim=false',text)
 if len(totals)!=1 or not records:raise ValueError('observational census incomplete')
 material_count,role_count,accepted_count,selected_count=map(int,totals[0]);keys=[r[:4] for r in records]
 if len(records)!=text.count('[ore-completion-census] ') or len(set(keys))!=len(keys) or role_count!=len(records):raise ValueError('census roles duplicated/incomplete')
 if material_count<len({r[:2] for r in records}) or accepted_count!=sum(int(r[4]) for r in records) or selected_count!=sum(r[5]=='true' for r in records):raise ValueError('census totals inconsistent')
 if accepted_count!=text.count('[ore-completion-detail] '):raise ValueError('accepted row details incomplete')
 for r in records:
  if r[2]=='dust' and r[3] not in ('gt.recipe.melter','gt.recipe.smelter'):raise ValueError('unexpected thermal census map')
  if r[2] in ('crushedPurified','crushedPurifiedTiny') and r[3]!='gt.recipe.centrifuge':raise ValueError('unexpected Centrifuge map')
  if r[2] not in ('dust','crushedPurified','crushedPurifiedTiny'):raise ValueError('unexpected census prefix')
 return {'phase':phase,'cases':6,'semantic_failures':len(failed),'observational_materials':material_count,'observational_roles':role_count,'accepted':accepted_count,'selected':selected_count,'source_parity_for_observational_census':False}
# Only the four newly generated fractional Smelter roles may change in broad census.
# Current broad census observes whole dust, so these exclusions are presently empty;
# fractional presence/values are independently required by the native literal eight-role case.
ALLOWED_BROAD={f'{i}/{p}/gt.recipe.smelter' for i in ['220','260'] for p in ['dustSmall','dustTiny']}
def stable_whole_fluid_evidence(text):
 # Old native helper printed only FluidStack object identities. Never infer their
 # contents: require the independent actual accepted-map census's literal ID/amount.
 bindings={}
 for name,ident,ticks in [('Titanium','220',519),('Iron','260',830)]:
  lower=name.lower();fluid=f'gt6ce:molten_{lower}x144'
  expected=f'{ident}/dust/gt.recipe.smelter [gt6ce:dust_{lower}x1]->[];chance=[];work={ticks}/16;fluidInputs=[];fluidOutputs=[{fluid}]'
  details=[line.split('[ore-completion-detail] ',1)[1] for line in text.splitlines() if '[ore-completion-detail] ' in line and line.split('[ore-completion-detail] ',1)[1].startswith(ident+'/dust/gt.recipe.smelter ')]
  if details!=[expected]:raise ValueError('independent stable whole-fluid fingerprint absent/changed '+name)
  prefix=f'[ore-completion-census] {ident}/{name}/dust/gt.recipe.smelter accepted=1 selected=true dynamic=false '
  if sum(prefix in line for line in text.splitlines())!=1:raise ValueError('stable whole-fluid accepted selected census absent '+name)
  bindings[name]=fluid
 return bindings

def normalize_whole_display(marker,row,bindings):
 name=row.split('/',1)[0] if marker=='[whole-dust-thermal-row] ' else row.split(' ',1)[0]
 if name not in bindings:raise ValueError('unreviewed whole-fluid display '+name)
 fluid=bindings[name]
 opaque=r'(?:net\.minecraftforge|net\.neoforged\.neoforge)\.fluids\.FluidStack@[0-9a-fA-F]+'
 if marker=='[whole-dust-thermal-row] ':
  pattern=r'fluids=\[('+opaque+r')\]'
  stable='fluids=['+fluid+']'
 else:
  pattern=r'fluid=('+opaque+r')(?= sourceSmelterTicks=)'
  stable='fluid='+fluid
 # NeoForge already prints amount+ID, still require exact independently bound values.
 explicit='144 '+fluid.rsplit('x',1)[0]
 pattern=pattern.replace('('+opaque+')','(?:'+opaque+'|'+re.escape(explicit)+')')
 if re.search(pattern,row):return re.sub(pattern,lambda _:stable,row,count=1)
 if stable not in row:raise ValueError('unknown whole-fluid display format '+name)
 return row

def semantic_diff(baseline, fixed):
 def observations(path):
  text=path.read_text(errors='replace');result={};bindings=stable_whole_fluid_evidence(text)
  for marker in ['[ore-completion-detail] ','[ore-completion-census] ',
   '[slurry-recycling-preserved] ','[whole-dust-thermal-row] ','[whole-dust-thermal-facts] ',
   '[ore-completion-preserved] ']:
   rows=[]
   for line in text.splitlines():
    if marker not in line:continue
    row=line.split(marker,1)[1]
    if marker in ('[whole-dust-thermal-row] ','[whole-dust-thermal-facts] '):row=normalize_whole_display(marker,row,bindings)
    if marker=='[ore-completion-detail] ' and row.split(' ',1)[0] in ALLOWED_BROAD:continue
    if marker=='[ore-completion-census] ':
     parts=row.split(' ',1)[0].split('/')
     if len(parts)==4 and '/'.join([parts[0],parts[2],parts[3]]) in ALLOWED_BROAD:continue
    rows.append(row)
   if not rows:raise ValueError('semantic comparison missing '+marker)
   result[marker]=Counter(rows)
  return result
 before=observations(baseline);after=observations(fixed)
 for marker in before:
  if before[marker]!=after[marker]:raise ValueError('unapproved cross-revision semantic change '+marker+' removed='+str(before[marker]-after[marker])+' added='+str(after[marker]-before[marker]))
 return {'same_loader_semantic_comparison':True,'preserved_multisets':{k:sum(v.values()) for k,v in before.items()},'allowed_new_fractional_smelter_roles':sorted(ALLOWED_BROAD)}
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--xml',type=Path,required=True);p.add_argument('--log',type=Path,required=True);p.add_argument('--exit-file',type=Path,required=True);p.add_argument('--phase',choices=['baseline','fixed'],required=True);p.add_argument('--baseline-log',type=Path);a=p.parse_args();result=check(a.xml,a.log,a.exit_file,a.phase)
 if a.baseline_log:
  if a.phase!='fixed':raise ValueError('baseline-log is for fixed comparison only')
  result.update(semantic_diff(a.baseline_log,a.log))
 print(json.dumps(result,sort_keys=True))
