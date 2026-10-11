#!/usr/bin/env python3
"""Restore source BasicMachine face masks; pass the preserved original loader with --source.

GregTech-6 Team / Gregorius Techneticies, LGPL-3.0-or-later.
The generated fixture keeps each original registry ID and the exact mapped port tier.
"""
import argparse,json,re
from pathlib import Path
import gt6_machine_map as mapping
import extract_basic_machine_recipes as parser
ROOT=Path(__file__).resolve().parents[1]
SIDES={'BOTTOM':0,'TOP':1,'LEFT':4,'RIGHT':2,'FRONT':3,'BACK':5,'UNDEFINED':-1}
BITS={'D':1,'U':2,'L':16,'R':4,'F':8,'B':32,'A':64}
KEYS=['NBT_INV_SIDE_IN','NBT_INV_SIDE_OUT','NBT_TANK_SIDE_IN','NBT_TANK_SIDE_OUT','NBT_ENERGY_ACCEPTED_SIDES','NBT_INV_SIDE_AUTO_IN','NBT_INV_SIDE_AUTO_OUT','NBT_TANK_SIDE_AUTO_IN','NBT_TANK_SIDE_AUTO_OUT']
def mask(value):
    result=0
    for token in value.split('|'):result|=BITS[token[5:]] if token.startswith('SBIT_') else int(token)
    return result&63
def main():
    args=argparse.ArgumentParser(description=__doc__);args.add_argument('--source',required=True,type=Path);args=args.parse_args()
    entries=parser.parse_source(args.source);index=mapping.index_entries(entries)
    lines=args.source.read_text(encoding='utf-8').splitlines();rows=[];families={}
    for name,tier,original in mapping.single_block_variants():
        entry=mapping.lookup(index,original,tier);assert entry is not None,(name,tier)
        line=re.sub(r'/\*.*?\*/','',lines[entry['line']-1]);start=line.index('aRegistry.add(')+len('aRegistry.add(')
        values=parser.split_top_level(line[start:parser.match_paren(line,start-1)]);nbt=values[8]
        start=nbt.index('UT.NBT.make(')+len('UT.NBT.make(');values=parser.split_top_level(nbt[start:parser.match_paren(nbt,start-1)])
        p=dict(zip(values[::2],values[1::2]));faces=[mask(p.get(k,'127')) for k in KEYS[:5]]+[SIDES[p.get(k,'SIDE_UNDEFINED')[5:]] for k in KEYS[5:]]
        # Energy output is undefined in the source basic class and has no face mask.
        javafaces=faces[:5]+[0]+faces[5:]
        if name in families:assert families[name]==javafaces,(name,tier,'tier-dependent source faces require separate rows')
        families[name]=javafaces
        rows.append('\t'.join(map(str,[name,tier,entry['registryId'],p.get('NBT_CHEAP_OVERCLOCKING')=='T',p.get('NBT_EFFICIENCY',10000),p.get('NBT_NEEDS_IGNITION')=='T',p.get('NBT_NO_CONSTANT_POWER')=='T',*faces])).lower())
    out=ROOT/'core/src/main/java/com/gregtech/gregtech/content/machine/OriginalBasicMachineFaces.java'
    code='''/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Generated from the original Basic Machines registrations by tools/generate_basic_machine_face_defaults.py. */
package com.gregtech.gregtech.content.machine;
import com.gregtech.gregtech.api.energy.MachineFaceMasks;
public final class OriginalBasicMachineFaces {
    private OriginalBasicMachineFaces() {}
    public static MachineFaceMasks defaults(String name) {return switch(name) {
'''+''.join('        case '+json.dumps(name)+'->new MachineFaceMasks('+','.join(map(str,faces))+');\n' for name,faces in sorted(families.items()))+'''        default->throw new IllegalArgumentException("Original basic machine faces:"+name);
    };}
}
'''
    out.write_text(code,encoding='utf-8',newline='\n')
    fixture=ROOT/'core/src/test/resources/gregtech/basic-machine-source-flags.tsv'
    fixture.write_text('# machine\ttier\tsource registry\tcheap\tefficiency\tignition\tno constant power\titemIn\titemOut\tfluidIn\tfluidOut\tenergyIn\titemAutoIn\titemAutoOut\tfluidAutoIn\tfluidAutoOut\n'+'\n'.join(rows)+'\n',encoding='utf-8')
    print(len(families),'source family faces;',len(rows),'source registry fixtures')
if __name__=='__main__':main()
