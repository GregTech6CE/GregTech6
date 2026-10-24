#!/usr/bin/env python3
"""Compare actual per-loader pre/post-production dust recipes; no claim that XML passed."""
import argparse,json
from pathlib import Path
PREFIX='[nonore-dust-snapshot] '
def snapshot(path):
    lines=[line.split(PREFIX,1)[1].strip() for line in path.read_text(errors='replace').splitlines() if PREFIX in line]
    if len(lines)!=1:raise ValueError('exactly one actual dust snapshot required: '+str(path))
    rows=json.loads(lines[0])
    if not isinstance(rows,list) or not rows or any(not isinstance(r,str) or not r for r in rows):raise ValueError('nonempty string-row snapshot required')
    return sorted(rows)
def compare(baseline,fixed):
    a,b=snapshot(baseline),snapshot(fixed)
    if a!=b:raise ValueError('startup dust rows/counts/chances/work/fluids changed across production phases')
    return {'matched_dust_rows':len(a),'comparison':'actual baseline versus fixed, per loader','native_test_pass_claimed':False}
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--baseline-log',type=Path,required=True);p.add_argument('--fixed-log',type=Path,required=True);a=p.parse_args()
    try:print(json.dumps(compare(a.baseline_log,a.fixed_log),indent=2))
    except Exception as e:p.exit(1,'FAIL: '+str(e)+'\n')
