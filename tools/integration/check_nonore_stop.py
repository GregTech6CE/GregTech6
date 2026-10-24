#!/usr/bin/env python3
"""Require exact isolated native test AND a real stopped-event observer receipt/log.
No cleanup/restoration or same-JVM next-world claim. Run only after terminal process.
"""
import argparse,json,re
from pathlib import Path
import xml.etree.ElementTree as ET
NS='gregtech_nonore_stop_audit'
CASE='nonoreactiverowsclearonactualserverstop'
FLAGS=('realEventObserved','rowsAbsent','foreignMetadataAbsent','foreignRoutesAbsent','startupWinnerPreserved','passed')
def unique(pairs):
    out={}
    for key,value in pairs:
        if key in out: raise ValueError('duplicate JSON field: '+key)
        out[key]=value
    return out

def json_load(text): return json.loads(text,object_pairs_hook=unique)
def exactly(obj,key,value):
    if type(obj.get(key)) is not type(value) or obj[key]!=value: raise ValueError('mismatch: '+key)
def log_record(text,prefix):
    rows=[line.split(prefix,1)[1].strip() for line in text.splitlines() if prefix in line]
    if len(rows)!=1: raise ValueError('expected one '+prefix+' log')
    value=json_load(rows[0])
    if not isinstance(value,dict): raise ValueError('log must be object')
    return value

def check(xml,run,exit_file,log,nonce):
    if not re.fullmatch('[0-9a-f]{32}',nonce): raise ValueError('expected fresh 32-hex nonce')
    if exit_file.read_text().strip()!='0': raise ValueError('native process did not exit 0')
    run=run.resolve(strict=True)
    if not run.is_dir() or not run.name.startswith('gt6-nonore-stop-'): raise ValueError('wrong disposable directory')
    root=ET.parse(xml).getroot();cases=list(root.iter('testcase'))
    if len(cases)!=1: raise ValueError('exactly one native test required')
    exactly(cases[0].attrib,'name',CASE);exactly(cases[0].attrib,'classname',NS+':test_empty')
    if any(e.tag in ('failure','error','skipped') for e in root.iter()): raise ValueError('nonpassing native XML')
    if any(int(s.get(k,'0')) for s in root.iter() if s.tag in ('testsuite','testsuites') for k in ('failures','errors','skipped')): raise ValueError('nonpassing suite metadata')
    receipts=list(run.rglob('.nonore-stop-event.json'))
    if len(receipts)!=1: raise ValueError('exactly one stop-event receipt required')
    r=json_load(receipts[0].read_text())
    if not isinstance(r,dict): raise ValueError('receipt must be object')
    for k,v in {'schema':1,'namespace':NS,'test':CASE,'nonce':nonce,'event':'ServerStoppedEvent','priority':'LOWEST','armedActiveRows':2}.items(): exactly(r,k,v)
    world=Path(r['world']).resolve(strict=True)
    if world==run or not world.is_relative_to(run) or receipts[0].resolve().parent!=world: raise ValueError('receipt world outside disposable directory')
    if (world/'.nonore-stop-used').read_text().strip()!=nonce: raise ValueError('fresh marker mismatch')
    for key in FLAGS: exactly(r,key,True)
    if 'error' in r or 'receiptError' in r: raise ValueError('error-bearing receipt')
    original=r.get('originalPacks');selected=r.get('selectedProbePacks')
    if (not isinstance(original,list) or not isinstance(selected,list) or
        any(not isinstance(v,str) or not v for v in original+selected) or
        len(set(original))!=len(original) or len(set(selected))!=len(selected) or
        len(selected)!=len(original)+1 or selected[:-1]!=original or selected[-1]!='file/gt-nonore-stop-probe'): raise ValueError('active probe selection evidence invalid')
    if list(world.glob('.nonore-stop-event.*.tmp')): raise ValueError('unfinished receipt write')
    text=log.read_text(errors='replace');arm=log_record(text,'[nonore-stop-arm] ');event=log_record(text,'[nonore-stop-event] ')
    for key in ('schema','namespace','test','nonce','world','armedActiveRows'): exactly(arm,key,r[key])
    exactly(arm,'passed',True)
    if event!=r: raise ValueError('event log differs from disk receipt')
    if text.index('[nonore-stop-arm] ')>=text.index('[nonore-stop-event] '): raise ValueError('event precedes arm')
    return {'native_cases':1,'actual_stopped_event':True,'active_rows_cleared':2,'world':str(world),
            'receipt':str(receipts[0]),'same_jvm_new_world_verified':False,'live_pack_restoration_claimed':False}
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--xml',type=Path,required=True);p.add_argument('--run-directory',type=Path,required=True);p.add_argument('--exit-file',type=Path,required=True);p.add_argument('--log',type=Path,required=True);p.add_argument('--nonce',required=True);a=p.parse_args()
    try: print(json.dumps(check(a.xml,a.run_directory,a.exit_file,a.log,a.nonce),indent=2))
    except Exception as e:p.exit(1,'FAIL: '+str(e)+'\n')
