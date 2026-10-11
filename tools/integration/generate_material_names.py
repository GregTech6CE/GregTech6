"""Adopt the complete original material-name function and its prefix metadata.

Only reads the supplied GT6 tree. Dry run unless --write; generated Java is pure
domain code. Unknown source syntax fails rather than silently dropping branches.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re

from source_language_common import STRING, material_key
from generate_machine_material_data import calls, masked

ROOT=Path(__file__).resolve().parents[2]
TARGET=Path('core/src/main/java/com/gregtech/gregtech/api/material/OriginalMaterialNameRules.java')
TAGS={'ORE','ORE_PROCESSING_BASED','DUST_BASED','IS_CONTAINER'}


def block_end(text,start):
    depth=0
    for match in re.finditer(STRING+r'|[{}]',text[start:]):
        if match[0]=='{':depth+=1
        elif match[0]=='}':
            depth-=1
            if depth==0:return start+match.end()
    raise ValueError('Unclosed original Java block')


def material_symbols(text):
    scopes=[]
    for match in re.finditer(r'public static class (OREMATS|STONES|WOODS)\s*\{',text):
        start=text.index('{',match.start());scopes.append((start,block_end(text,start),match[1]+'.'))
    def scope(pos):return next((name for a,b,name in scopes if a<pos<b),'')
    factories={m[1]:(int(m[2]),material_key(json.loads(m[3])).removeprefix('gt.material.'))
               for m in re.finditer(r'static\s+OreDictMaterial\s+(\w+)\s*\(\)\s*\{return\s+\w+\s*\(\s*(\d+)\s*,\s*('+STRING+')',text)}
    symbols={};aliases={};factory_members={}
    for line_match in re.finditer(r'[^\n]+',text):
        line=line_match[0];prefix=scope(line_match.start())
        for m in re.finditer(r'\b(\w+)\s*=\s*(\w+)\s*\(\s*(\d+)\s*,\s*('+STRING+')',line):
            symbol=prefix+m[1];value=(int(m[3]),material_key(json.loads(m[4])).removeprefix('gt.material.'))
            symbols[symbol]=value;factory_members.setdefault(m[2],set()).add(value)
        for m in re.finditer(r'\b(\w+)\s*=\s*(\w+)\s*\(\s*\)',line):
            if m[2] in factories:symbols[prefix+m[1]]=factories[m[2]]
        for m in re.finditer(r'\b(\w+)\s*=\s*((?:MT\.)?(?:(?:OREMATS|STONES|WOODS)\.)?\w+)\s*[,;]',line):
            value=m[2].removeprefix('MT.')
            aliases[prefix+m[1]]=prefix+value if '.' not in value and prefix else value
    for _ in range(len(aliases)):
        before=len(symbols)
        for key,value in aliases.items():
            if key not in symbols and value in symbols:symbols[key]=symbols[value]
        if len(symbols)==before:break
    return symbols,factory_members


def prefix_rows(text,helper):
    if 'add(MATERIAL_BASED, UNIFICATABLE, ORE, TOOLTIP_ENCHANTS);' not in helper:
        raise ValueError('Unsupported source setOreStats tags')
    rows={}
    for line in text.splitlines():
        for _,args in calls(line,'create'):
            if len(args)!=4 or not all(re.fullmatch(STRING,a) for a in args):continue
            name,_,pre,post=map(json.loads,args)
            tags=set()
            for _,added in calls(line,'.add'):
                tags.update(a.removeprefix('TD.Prefix.') for a in added if a.removeprefix('TD.Prefix.') in TAGS)
            if '.setOreStats' in line:tags.add('ORE')
            if name in rows:raise ValueError('Duplicate original prefix: '+name)
            rows[name]=(pre,post,sorted(tags))
    # Later display-name mutations must be handled deliberately, never ignored.
    if '.setLocalItemName(' in text.replace('.setLocalItemName(aPreMaterial, aPostMaterial)',''):
        raise ValueError('Additional source prefix display override')
    return rows


def empty_templates(language):
    result={}
    for match in re.finditer(r'if\s*\(\s*aMaterial\s*==\s*MT\.Empty\s*\)\s*\{', language):
        start=language.index('{', match.start())
        body=language[start+1:block_end(language,start)-1]
        for prefix,literal in re.findall(r'if\s*\(\s*aPrefix\s*==\s*OP\.(\w+)\s*\)\s*return\s+('+STRING+r')\s*;',body):
            value=json.loads(literal)
            if prefix in result and result[prefix]!=value:raise ValueError('Ambiguous empty material name: '+prefix)
            result[prefix]=value
    return result


def default_templates(prefixes, language):
    """Generic (non-special material, no material flags, non-April) original names."""
    if not re.search(r'return\s+aPrefix\.mMaterialPre\s*\+\s*aMaterial\.mNameLocal\s*\+\s*aPrefix\.mMaterialPost\s*;', language):
        raise ValueError('Unsupported original default material name')
    recursions={}
    pattern=r'if\s*\(([^)]+)\)\s*return\s+aPrefix\.mMaterialPre\s*\+\s*getLocalName\(OP\.(\w+)\s*,\s*aMaterial\);'
    matches=list(re.finditer(pattern,language))
    if len(matches)!=len(re.findall(r'getLocalName\(OP\.',language)):
        raise ValueError('Unsupported original recursive material name')
    for match in matches:
        for expression in match[1].split('||'):
            prefix=re.fullmatch(r'\s*aPrefix\s*==\s*OP\.(\w+)\s*',expression)
            if not prefix:raise ValueError('Unsupported original recursive prefix condition')
            recursions[prefix[1]]=match[2]
    def template(name,seen=()):
        if name not in prefixes or name in seen:raise ValueError('Missing or cyclic original recursive prefix: '+name)
        before,after,_=prefixes[name]
        return before+(template(recursions[name],(*seen,name)) if name in recursions else '%s'+after)
    return {name:template(name) for name in prefixes}


def generate(source):
    paths={name:source/'src/main/java'/path for name,path in {
        'language':'gregapi/lang/LanguageHandler.java','mt':'gregapi/data/MT.java',
        'op':'gregapi/data/OP.java','any':'gregapi/data/ANY.java',
        'prefix':'gregapi/oredict/OreDictPrefix.java','constants':'gregapi/data/CS.java'}.items()}
    raw={name:masked(path.read_text(encoding='utf-8')) for name,path in paths.items()}
    symbols,factory_members=material_symbols(raw['mt'])
    prefixes=prefix_rows(raw['op'],raw['prefix'])
    match=re.search(r'public static String getLocalName\(OreDictPrefix aPrefix, OreDictMaterial aMaterial\)\s*\{',raw['language'])
    if not match:raise ValueError('Missing original material naming function')
    start=raw['language'].index('{',match.start());body=raw['language'][start+1:block_end(raw['language'],start)-1]
    references=set(re.findall(r'MT\.((?:(?:STONES|OREMATS|WOODS)\.)?\w+)',body))
    missing=references-symbols.keys()
    if missing:raise ValueError('Unresolved original material symbols: '+','.join(sorted(missing)))
    used_prefixes=set(re.findall(r'OP\.(\w+)',body))
    if used_prefixes-prefixes.keys():raise ValueError('Unresolved original naming prefixes')
    groups={key:set() for key in ('Blaze','Clay','Plastic','Rubber')}
    for line in raw['any'].splitlines():
        match=re.match(r'\s*(Blaze|Clay|Plastic|Rubber)\s*\.',line)
        if not match:continue
        for _,args in calls(line,'.addReRegistrationToThis'):
            for argument in args:
                if not argument.startswith('MT.') or argument[3:] not in symbols:
                    raise ValueError('Unsupported original name-group member: '+argument)
                groups[match[1]].add(symbols[argument[3:]])
    # The other two name groups are registered by the source material factories.
    for group,factory in [('Blaze','blaze'),('Clay','clay')]:
        declaration=next((line for line in raw['mt'].splitlines() if re.search(r'static OreDictMaterial\s+'+factory+r'\s*\(',line)),None)
        if declaration is None or not re.search(r'\.put\(\s*ANY\.'+group+r'\b',declaration):
            raise ValueError('Unsupported source material name-group factory: '+factory)
        groups[group].update(factory_members.get(factory,set()))
    if not re.search(r'public static final long C\s*=\s*273\s*;',raw['constants']):
        raise ValueError('Unsupported original native-ore temperature boundary')
    if not re.search(r'APRIL_FOOLS\s*=\s*\(new Date\(\).getMonth\(\)\+1 ==\s*4 && new Date\(\).getDate\(\) <=\s*2\)',raw['constants']):
        raise ValueError('Unsupported original April naming interval')

    def identity(symbol):
        ident,name=symbols[symbol]
        return f'is(material, {ident}, {json.dumps(name)})'
    body=re.sub(r'aMaterial\s*==\s*MT\.((?:(?:STONES|OREMATS|WOODS)\.)?\w+)',lambda m:identity(m[1]),body)
    body=re.sub(r'getLocalName\(OP\.(\w+)\s*,\s*aMaterial\)',r'name("\1", material, displayName, aprilFools)',body)
    body=re.sub(r'aPrefix\s*==\s*OP\.(\w+)',r'prefix.name().equals("\1")',body)
    body=re.sub(r'ANY\.(\w+)\.mToThis\.contains\(aMaterial\)',r'inGroup("\1", material)',body)
    body=re.sub(r'aPrefix\.containsAny\(([^)]+)\)',lambda m:'('+ ' || '.join('prefix.has("'+s.strip().removeprefix('TD.Prefix.')+'")' for s in m[1].split(','))+')',body)
    body=re.sub(r'aPrefix\.contains\(TD.Prefix\.(\w+)\)',r'prefix.has("\1")',body)
    body=re.sub(r'aMaterial\.contains\(TD\.(?:Properties|Processing)\.(\w+)\)',r'material.has(MaterialProperty.\1)',body)
    body=body.replace('aMaterial.mTargetCrushing.mMaterial == aMaterial','material.getTargetCrushingMaterial().resolve() == material')
    body=re.sub(r'MT\.(\w+)\.mNameLocal',lambda m:'displayName.apply(GTMaterialRegistry.get('+json.dumps(symbols[m[1]][1])+').resolve())',body)
    for old,new in {'aPrefix.mMaterialPre':'prefix.before()','aPrefix.mMaterialPost':'prefix.after()',
                    'aPrefix.mNameInternal':'prefix.name()','aMaterial.mNameLocal':'local',
                    'aMaterial.mID':'material.getId()','aMaterial.mMeltingPoint':'material.getMeltingPoint()',
                    'APRIL_FOOLS':'aprilFools'}.items():body=body.replace(old,new)
    body=re.sub(r'\bC\b','273',body)
    if re.search(r'\baPrefix\b|\baMaterial\b|\b(?:MT|OP|ANY|TD)\.',body):
        raise ValueError('Unadapted source naming expression')
    header=paths['language'].read_text(encoding='utf-8').split('package ',1)[0]
    lines=[header,'package com.gregtech.gregtech.api.material;\n',
        'import com.gregtech.gregtech.api.prefix.PrefixRegistry;\nimport java.time.LocalDate;\nimport java.util.Map;\nimport java.util.Set;\nimport java.util.function.Function;\n',
        '/** Generated by tools/integration/generate_material_names.py from the complete GT6 naming method. */',
        'public final class OriginalMaterialNameRules {', '    private OriginalMaterialNameRules() {}',
        '    private static final boolean APRIL = aprilFools(LocalDate.now());',
        '    public static boolean aprilFools(LocalDate day) { return day.getMonthValue() == 4 && day.getDayOfMonth() <= 2; }',
        '    public static String name(String prefix, GTMaterial material, Function<GTMaterial, String> displayName) {',
        '        return name(prefix, material, displayName, APRIL);', '    }',
        '    /** Null means a port-only prefix with no source naming rule. */',
        '    public static String name(String prefixName, GTMaterial input, Function<GTMaterial, String> displayName, boolean aprilFools) {',
        '        Prefix prefix = PREFIXES.get(PrefixRegistry.sourceName(prefixName));',
        '        if (prefix == null) return null;',
        '        GTMaterial material = input.resolve();', '        String local = displayName.apply(material);',body,'    }',
        '    private static boolean is(GTMaterial material, int id, String name) {',
        '        return material.getId() == id && material.getName().equals(name);', '    }',
        '    private static boolean inGroup(String group, GTMaterial material) {',
        '        return switch (group) {']
    for group,members in sorted(groups.items()):
        checks=' || '.join(f'is(material, {ident}, {json.dumps(name)})' for ident,name in sorted(members)) or 'false'
        lines.append(f'            case "{group}" -> {checks};')
    lines+=['            default -> throw new IllegalArgumentException(group);','        };','    }',
        '    private record Prefix(String name, String before, String after, Set<String> tags) {',
        '        boolean has(String tag) { return tags.contains(tag); }','    }',
        '    private static final Map<String, Prefix> PREFIXES = Map.ofEntries(']
    entries=[]
    for name,(pre,post,tags) in sorted(prefixes.items()):
        q=lambda s:json.dumps(s,ensure_ascii=False)
        entries.append(f'            Map.entry({q(name)}, new Prefix({q(name)}, {q(pre)}, {q(post)}, Set.of('+', '.join(q(t) for t in tags)+')))')
    lines += [',\n'.join(entries),'    );','}','']
    report={'source_files':[{'path':str(p.resolve()),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in paths.values()],
            'original_material_symbols':{k:symbols[k] for k in sorted(references)},'prefixes':prefixes,
            'name_groups':{k:sorted(v) for k,v in groups.items()},
            'policy':'Complete source function, including April branch, same conditions and return order; exact names/positive identity for special materials, current shared material properties for generic wood/stone/native-ore branches.'}
    return '\n'.join(line.rstrip() for line in '\n'.join(lines).expandtabs(4).split('\n')),report


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source',type=Path,required=True)
    parser.add_argument('--audit',type=Path,required=True)
    parser.add_argument('--write',action='store_true')
    args=parser.parse_args();java,report=generate(args.source)
    report['generated_java_sha256']=hashlib.sha256(java.encode('utf-8')).hexdigest()
    args.audit.write_text(json.dumps(report,ensure_ascii=False,indent=2,sort_keys=True)+'\n',encoding='utf-8')
    if args.write:(ROOT/TARGET).write_text(java,encoding='utf-8',newline='\n')
    print(json.dumps({'prefixes':len(report['prefixes']),'special_materials':len(report['original_material_symbols']),
                      'name_groups':{k:len(v) for k,v in report['name_groups'].items()},'written':args.write}))


if __name__=='__main__':main()
