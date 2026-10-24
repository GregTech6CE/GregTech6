#!/usr/bin/env python3
"""Require one real isolated native case PLUS completed-restoration receipt. No source-only pass."""
import argparse,json,xml.etree.ElementTree as ET
from pathlib import Path
CASE='taggedforeignpvcoreinputskeepcontinuationanddatapackprecedence'
NAMESPACE='gregtech_nonore_tag_audit'
def check(xml,run,exit_file,log):
    if exit_file.read_text().strip()!='0':raise ValueError('native Gradle process did not exit0')
    run=run.resolve(strict=True)
    if not run.name.startswith('gt6-nonore-tag-'):raise ValueError('wrong disposable run directory')
    root=ET.parse(xml).getroot();cases=list(root.iter('testcase'))
    if len(cases)!=1:raise ValueError(f'exactly1 native case required, got{len(cases)}')
    case=cases[0]
    if case.get('name','').lower()!=CASE or case.get('classname','')!=NAMESPACE+':test_empty':raise ValueError('wrong case/namespace/template')
    if any(e.tag in ('failure','error','skipped') for e in case):raise ValueError('failed/errored/skipped native case')
    if any(int(s.get(key,'0')) for s in root.iter() if s.tag in ('testsuite','testsuites') for key in ('failures','errors','skipped')):raise ValueError('nonzero suite failure metadata')
    receipts=list(run.rglob('.nonore-tag-cleanup.json'))
    if len(receipts)!=1:raise ValueError('exactly1 completed-cleanup receipt required')
    r=json.loads(receipts[0].read_text())
    if r.get('restored') is not True or r.get('probePassed') is not True or r.get('verifiedRounds')!=2:raise ValueError('cleanup/probe not successful')
    if r.get('sourceNoPolicyVerified') is not True or r.get('sameInputStartupWinnerVerified') is not True:raise ValueError('source-NO/startup-winner evidence missing')
    world=Path(r['world']).resolve(strict=True)
    if world==run or not world.is_relative_to(run) or receipts[0].parent.resolve()!=world:raise ValueError('receipt outside isolated world')
    if not (world/'.nonore-tag-probe-used').is_file():raise ValueError('fresh-run marker missing')
    if list(run.rglob('gt-nonore-tag-continuation-probe')):raise ValueError('temporary datapack remains')
    if '[nonore-startup-winner] Rubber accepted=true' not in log.read_text(errors='replace'):raise ValueError('actual startup acceptance log missing')
    if '[nonore-tag-cleanup] restored=true verifiedRounds=2 probePassed=true' not in log.read_text(errors='replace'):raise ValueError('native cleanup completion log missing')
    return {'native_cases':1,'verified_reload_phases':2,'restored':True,'world':str(world),'receipt':str(receipts[0])}
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--xml',required=True,type=Path);p.add_argument('--run-directory',required=True,type=Path);p.add_argument('--exit-file',required=True,type=Path);p.add_argument('--log',required=True,type=Path);a=p.parse_args()
    try:print(json.dumps(check(a.xml,a.run_directory,a.exit_file,a.log),indent=2))
    except Exception as e:p.exit(1,'FAIL: '+str(e)+'\n')
