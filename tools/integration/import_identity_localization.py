"""Import language bindings by adopted registry identity. Dry run unless --write.

Input is LanguageIdentities.java's export from the current compiled core. The
original checkout is read-only; unsupported English expressions remain reported.
"""
import argparse
import ast
from collections import defaultdict
import hashlib
import json
from pathlib import Path
import re
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from localization import ROOT, LANG_PATH, CONFIG_PATH, load_source, read_json, json_text, expected_chinese, check_english
from generate_machine_material_data import calls, masked
from source_language_common import STRING, material_key, counted_blocks
from source_numbered_language import bumble_declarations, bumble_english
from source_creative_language import creative_name_sources, unused_prefix_names
from source_recipe_language import recipe_map_names


def any_material_names(original):
    """ANY.init overrides display names before multi-tile machine registration."""
    path=original/'src/main/java/gregapi/data/ANY.java'
    if not path.exists():return {},{},[]
    raw=masked(path.read_text(encoding='utf-8'))
    names={symbol:json.loads(value) for symbol,value in re.findall(
        r'\b(\w+)\s*=\s*any\(\s*('+STRING+r')',raw)}
    displays=dict(names)
    for line in raw.splitlines():
        symbol=re.match(r'\s*(\w+)\s*\.',line)
        if not symbol or symbol[1] not in names:continue
        local=re.findall(r'\.setLocal\s*\(\s*('+STRING+r')\s*\)',line)
        if len(local)>1 or len(local)!=len(re.findall(r'\.setLocal\s*\(',line)):
            raise ValueError('Unsupported original ANY display name: '+symbol[1])
        if len(local)==1:displays[symbol[1]]=json.loads(local[0])
    return names,displays,[path]


def tool_ids(original):
    cs = original / 'src/main/java/gregapi/data/CS.java'
    if not cs.exists(): return {}
    text = masked(cs.read_text(encoding='utf-8'))
    if 'class ToolsGT' not in text: return {}
    text = text.split('class ToolsGT',1)[1].split('public static final MultiItem',1)[0]
    return dict(re.findall(r'\b([A-Z][A-Z_0-9]*)\s*=\s*(\d+)\b',text))


def material_identities(original):
    """Positive original IDs prove material identity, including old field-name aliases.

    Names use OreDictMaterial's sanitizer; getLocal uses the separate setLocal
    override. ID zero is a validation/compatibility placeholder, never proof.
    """
    path=original/'src/main/java/gregapi/data/MT.java'
    material=original/'src/main/java/gregapi/oredict/OreDictMaterial.java'
    if not path.exists() or not material.exists():return {},{},[],{}
    by_id,english=defaultdict(set),defaultdict(set)
    tables=[path]
    antimatter=original/'src/main/java/gregapi/data/AM.java'
    if antimatter.exists():tables.append(antimatter)
    for table in tables:
        raw=masked(table.read_text(encoding='utf-8'))
        if table==antimatter and 'OreDictMaterial.createMaterial(aID,aNameOreDict,aNameOreDict)' not in re.sub(r'\s+','',raw):
            raise ValueError('Unsupported original antimatter display-name factory')
        table_english=defaultdict(set)
        for line in raw.splitlines():
            # Element declarations use zero-argument factories; a few symbols are
            # chained assignments (Ma, Magic = Ma = create(...)). Both still expose
            # a literal ID and original name at the actual constructor.
            row=re.search(r'(?:=|\breturn\b)\s*(\w+)\s*\(\s*(-?\d+)\s*,\s*('+STRING+')',line)
            if not row:continue
            factory,ident=row[1],int(row[2])
            name=json.loads(row[3]);key=material_key(name)
            if key=='gt.material.':continue
            # Sentinel/validation names still have original English declarations,
            # but ID zero/negative must never prove a native material's identity.
            if ident>0:by_id[ident].add(key)
            local=re.findall(r'\.setLocal\s*\(\s*('+STRING+r')\s*\)',line)
            if len(local)>1 or len(local)!=len(re.findall(r'\.setLocal\s*\(',line)):continue
            if factory=='woodnormal' and not local:
                args=next(calls(line,factory))[1]
                if len(args)<3 or not re.fullmatch(STRING,args[2]):continue
                local=[args[2]]
            table_english[key].add(json.loads(local[0]) if local else name)
        # Element factories may acquire a display override at their field assignment.
        # Keep the factory's internal identity, but use the final mNameLocal value.
        factories={m[1]:material_key(json.loads(m[2])) for m in re.finditer(
            r'static\s+OreDictMaterial\s+(\w+)\s*\(\)\s*\{return\s+\w+\s*\(\s*\d+\s*,\s*('+STRING+')',raw)}
        for line in raw.splitlines():
            assignment=re.match(r'\s*\w+\s*=\s*(\w+)\s*\(\s*\)',line)
            if assignment is None or assignment[1] not in factories:continue
            key=factories[assignment[1]]
            local=re.findall(r'\.setLocal\s*\(\s*('+STRING+r')\s*\)',line)
            if not re.search(r'\.setLocal\s*\(',line):continue
            if len(local)==1 and len(re.findall(r'\.setLocal\s*\(',line))==1:
                table_english[key]={json.loads(local[0])}
            else:table_english.pop(key,None)
        for key,values in table_english.items():english[key].update(values)
    ambiguous={str(k):sorted(v) for k,v in by_id.items() if len(v)!=1}
    return ({k:next(iter(v)) for k,v in by_id.items() if len(v)==1},
            {k:next(iter(v)) for k,v in english.items() if len(v)==1},tables+[material],ambiguous)


def verified_material_proofs(identity_rows, material_ids):
    """Only the combination of positive numeric ID and exact internal name proves identity."""
    proofs=defaultdict(set)
    for key,original,ident in identity_rows:
        if key.startswith('@material-proof.') and ident.isdigit() and int(ident)>0:
            if material_ids.get(int(ident))==original:
                proofs[key.removeprefix('@material-proof.')].add((int(ident),original))
    return proofs


def resolve_material_collision(key, originals, proofs):
    # getTranslationKey() belongs to the actual material's internal name. A
    # same-named field in another category (e.g. Woods.Gold) must not replace it.
    canonical={original for _,original in proofs.get(key,set())
               if original in originals and key=='material.gregtech.'+original.removeprefix('gt.material.').lower()}
    return canonical if len(canonical)==1 else originals


def numbered_item_identities(original):
    """Use the same IL-to-recipe-ID conversion as the adopted recipe importer."""
    # That historical script executes its CLI at import time; only read its literal
    # naming tables. Never import/run a generator with obsolete output paths.
    recipe_importer=ROOT/'tools/transpile_gt6_chem.py'
    tables={}
    for node in ast.parse(recipe_importer.read_text(encoding='utf-8')).body:
        if isinstance(node,ast.Assign) and isinstance(node.targets[0],ast.Name):
            key=node.targets[0].id
            if key in ('IL_ITEM_PREFIXES','IL_SHAPE_PREFIXES','SHAPE_WORDS'): tables[key]=ast.literal_eval(node.value)
    def item_id(field):
        if any(field.startswith(prefix) for prefix in tables['IL_ITEM_PREFIXES']): return None
        # These adopted technological IDs reorder the original IL field, not its display text.
        usb=re.fullmatch(r'USB_(Stick|Cable|HDD)_([1-4])',field)
        if usb:return 'usb'+usb[2]+'_'+usb[1].lower()
        crystal=re.fullmatch(r'(Circuit|Processor)_Crystal_(Diamond|Ruby|Emerald|Sapphire)',field)
        if crystal:return 'crystal_'+crystal[1].lower()+'_'+crystal[2].lower()
        foodmold=re.fullmatch(r'Shape_Foodmold_(Empty|Bun|Bread|Baguette|Cylinder|Toast)',field)
        if foodmold:return 'foodmold_shape_'+foodmold[1].lower()
        if field=='Shape_Slicer_Eigths_Hollow':return 'slicer_shape_eights_hollow'
        for prefix,native in tables['IL_SHAPE_PREFIXES'].items():
            if not field.startswith(prefix): continue
            words=field[len(prefix):].lower().replace('_','')
            # Native shape IDs retained these spellings; the old recipe importer
            # does not normalize their original IL field word order completely.
            if prefix in ('Shape_Extruder_','Shape_SimpleEx_'):
                forms={'pipetiny':'tinypipe','pipesmall':'smallpipe','pipemedium':'mediumpipe',
                       'pipelarge':'largepipe','pipehuge':'hugepipe','ccc':'capsulecellcontainer',
                       'platetiny':'tinyplate','wirefine':'finewire'}
                if words in forms:return native+forms[words]
            for source,target in tables['SHAPE_WORDS'].items():
                if words==source.replace('_',''): return native+target
            return native+words
        return field.lower()
    file = ROOT/'core/src/main/java/com/gregtech/gregtech/content/recipe/TechnologicalItemDefinitions.java'
    aliases = dict(re.findall(r'Map.entry\("([^"]+)",\s*"([^"]+)"\)',masked(file.read_text(encoding='utf-8'))))
    result, files = {}, [file, ROOT/'tools/transpile_gt6_chem.py']
    categories={'MultiItemRandomTools':'randomtools','MultiItemFood':'food','MultiItemBottles':'bottles',
                'MultiItemCans':'cans','MultiItemTechnological':'technological','MultiItemBooks':'books'}
    for stem,category in categories.items():
        path=original/f'src/main/java/gregtech/items/{stem}.java'
        if not path.exists(): continue
        files.append(path)
        for line in masked(path.read_text(encoding='utf-8')).splitlines():
            field=re.search(r'IL\.(\w+)\s*\.set\(\s*addItem\(',line)
            if field is None: continue
            args=next(calls(line,'addItem'))[1]
            if not args[0].isdigit(): continue
            native=item_id(field[1])
            if native is None: continue
            native=aliases.get(native,native)
            if ':' in native: continue
            key='item.gregtech.'+native
            value=f'gt.multiitem.{category}.{args[0]}'
            if key in result and result[key]!=value: raise ValueError('Ambiguous original item: '+key)
            result[key]=value
    return result, files


def pipe_identities(original, symbols):
    """Resolve original helper offsets using adopted material identities, not names."""
    root = original / 'src/main/java'
    loader = root / 'gregtech/loaders/b/Loader_MultiTileEntities.java'
    if not loader.exists(): return {}, []
    files = [loader]
    result = {}
    for line in masked(loader.read_text(encoding='utf-8')).splitlines():
        assigned = re.search(r'aMat\s*=\s*(MT\.\w+)\s*;', line)
        for _, args in calls(line, 'aRegistry.add'):
            if len(args)>4 and args[4]=='MultiTileEntityCell.class' and args[2].isdigit() and assigned:
                if assigned[1] in symbols:
                    key='@cell.'+symbols[assigned[1]]
                    value='gt.multitileentity.'+args[2]
                    if key in result and result[key]!=value: raise ValueError('Ambiguous original capsule: '+key)
                    result[key]=value
    sizes = {'pipeTiny':'TINY','pipeSmall':'SMALL','pipeMedium':'MEDIUM',
             'pipeLarge':'LARGE','pipeHuge':'HUGE','pipeQuadruple':'QUADRUPLE',
             'pipeNonuple':'NONUPLE','pipeRestrictiveMedium':'RESTRICTIVE_MEDIUM',
             'pipeRestrictiveLarge':'RESTRICTIVE_LARGE','pipeRestrictiveHuge':'RESTRICTIVE_HUGE'}
    for kind, helper in [('item','Item'),('fluid','Fluid')]:
        path = root / f'gregapi/tileentity/connectors/MultiTileEntityPipe{helper}.java'
        files.append(path)
        offsets = {}
        for line in masked(path.read_text(encoding='utf-8')).splitlines():
            prefix = re.search(r'setTarget_\(OP\.(\w+)', line)
            rows = list(calls(line, 'aRegistry.add'))
            if prefix is None or not rows: continue
            offset = re.fullmatch(r'aID\s*(?:\+\s*(\d+))?', rows[0][1][2])
            if offset is None or prefix[1] not in sizes: continue
            size = sizes[prefix[1]]
            if size in offsets: raise ValueError('Duplicate original pipe size: ' + size)
            offsets[size] = int(offset[1] or 0)
        if len(offsets) != (6 if kind == 'item' else 7):
            raise ValueError('Incomplete original pipe helper: ' + kind)
        for _, args in calls(masked(loader.read_text(encoding='utf-8')), f'MultiTileEntityPipe{helper}.add{helper}Pipes'):
            if not args[0].isdigit() or args[-1] not in symbols: continue
            for size, offset in offsets.items():
                key = f'@pipe.{kind}.{symbols[args[-1]]}.{size}'
                value = 'gt.multitileentity.' + str(int(args[0]) + offset)
                if key in result and result[key] != value:
                    raise ValueError('Ambiguous original pipe material: ' + key)
                result[key] = value
    return result, files


def electric_wire_rows(original):
    """Read wire sizes/offsets and preserve the helper's cable-only guard."""
    path=original/'src/main/java/gregapi/tileentity/connectors/MultiTileEntityWireElectric.java'
    raw=masked(path.read_text(encoding='utf-8'))
    guard=re.search(r'if\s*\(aCable\)\s*\{',raw)
    if guard is None:raise ValueError('Unsupported original electric cable guard')
    depth=1;guard_end=None
    for token in re.finditer(STRING+r'|[{}]',raw[guard.end():]):
        if token[0]=='{':depth+=1
        elif token[0]=='}':depth-=1
        if depth==0:
            guard_end=guard.end()+token.start();break
    if guard_end is None:raise ValueError('Unclosed original electric cable guard')
    rows={}
    for pos,args in calls(raw,'OreDictManager.INSTANCE.setTarget_'):
        prefix=re.fullmatch(r'OP\.(wire|cable)Gt(\d+)',args[0])
        if prefix is None:continue
        registrations=list(calls(args[2],'aRegistry.add'))
        if len(registrations)!=1:raise ValueError('Unsupported original electric wire registration')
        registration=registrations[0][1]
        offset=re.fullmatch(r'aID\s*(?:\+\s*(\d+))?',registration[2])
        kind,size=prefix[1],int(prefix[2])
        if offset is None or (guard.end()<=pos<guard_end)!=(kind=='cable'):
            raise ValueError('Unsupported original electric wire offset/guard')
        if (kind,size) in rows:raise ValueError('Duplicate original electric wire size')
        rows[kind,size]=(int(offset[1] or 0),registration[0])
    expected={('wire',i) for i in range(1,17)}|{('cable',i) for i in (1,2,4,8,12)}
    if rows.keys()!=expected or len({offset for offset,_ in rows.values()})!=21:
        raise ValueError('Incomplete original electric wire helper')
    return rows,path


def wire_identities(original,symbols):
    loader=original/'src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java'
    if not loader.exists():return {},[]
    registrations=list(calls(masked(loader.read_text(encoding='utf-8')),'MultiTileEntityWireElectric.addElectricWires'))
    if not registrations:return {},[]
    rows,helper=electric_wire_rows(original)
    result={}
    for _,args in registrations:
        if not args[0].isdigit() or args[-1] not in symbols:continue
        if args[8] not in ('T','F'):raise ValueError('Unsupported original electric cable availability')
        for (kind,size),(offset,_) in rows.items():
            if kind=='cable' and args[8]=='F':continue
            key=f'@wire.{symbols[args[-1]]}.{kind}.{size}'
            value='gt.multitileentity.'+str(int(args[0])+offset)
            if key in result and result[key]!=value:raise ValueError('Ambiguous original electric wire material: '+key)
            result[key]=value
    return result,[loader,helper]


def material_fluid_english(original, identity_rows, material_data=None):
    """Verify names of actual native fluids against the original material/helper formula.

    Do not manufacture fluids for every material or borrow a translated solid's
    name. Only exported native fluid bindings with a positive original material ID
    participate, and explicit FL.create declarations take precedence at the caller.
    This proves the English naming formula, not original fluid availability or a
    Chinese translation; the Chinese patch must not limit English coverage.
    """
    helper=original/'src/main/java/gregapi/data/FL.java'
    loader=original/'src/main/java/gregtech/loaders/a/Loader_Fluids.java'
    if not helper.exists() or not loader.exists():return {},[]
    compact=lambda value: ''.join(re.findall(STRING+r'|\S',masked(value)))
    raw=compact(helper.read_text(encoding='utf-8'))
    formulas={
        '': ['returncreate(aMaterial.mNameInternal.toLowerCase(),aTexture,aMaterial.mNameLocal,aMaterial,'],
        'molten.':['returncreate("molten."+aMaterial.mNameInternal.toLowerCase(),aTexture,"Molten "+aMaterial.mNameLocal,aMaterial,'],
        'plasma.':['returncreate("plasma."+aMaterial.mNameInternal.toLowerCase(),aTexture,aMaterial.mNameLocal+" Plasma",aMaterial,']}
    if not all(any(formula in raw for formula in values) for values in formulas.values()):
        raise ValueError('Unsupported original generated fluid name formula')
    ids,names,files,_=material_identities(original) if material_data is None else material_data
    targets={key:value for key,value,_ in identity_rows if key.startswith('fluid_type.gregtech.')}
    result={}
    for key,_,ident in identity_rows:
        if not key.startswith('@fluid-proof.') or not ident.isdigit() or int(ident)<=0:continue
        source_key=targets.get(key.removeprefix('@fluid-proof.'))
        if source_key is None:continue
        material=ids.get(int(ident))
        if material is None:continue
        name=names.get(material)
        if name is None:continue
        internal=material.removeprefix('gt.material.').lower()
        for phase in formulas:
            if source_key=='fluid.'+phase+internal:
                result[source_key]='Molten '+name if phase=='molten.' else name+' Plasma' if phase=='plasma.' else name
    return result,files+[helper,loader]


def original_english(original, material_data=None):
    found, files = defaultdict(set), [original / 'src/main/java/gregapi/data/MT.java']
    declaration_count = 0
    creative_files = creative_name_sources(original)
    files.extend(creative_files)
    tools = tool_ids(original)
    wood_classes = {}
    woods_file = original/'src/main/java/gregtech/loaders/a/Loader_Woods.java'
    if woods_file.exists():
        wood_classes=dict(re.findall(r'new (BlockTree\w+)\s*\(\s*"([^"]+)"',masked(woods_file.read_text(encoding='utf-8'))))
        files.append(woods_file)
    blocks_file = original/'src/main/java/gregtech/loaders/a/Loader_Blocks.java'
    if blocks_file.exists():
        wood_classes.update(re.findall(r'new (BlockFlowers[AB]|BlockBaleGrass|BlockBaleCrop|BlockSands|BlockGrass|BlockDiggable|'
                                      r'BlockAsphalt|BlockConcrete(?:Reinforced)?|BlockCFoam(?:Fresh)?|BlockGlass(?:Clear|Glow)|'
                                      r'BlockBars(?:Brass|Steel|TungstenSteel)|BlockSpike(?:Sharp|Steel|Super|Metal|Fancy)|'
                                      r'BlockRockOres|BlockCrystalOres|BlockVanillaOresA)\s*\(\s*"([^"]+)"',
                                      masked(blocks_file.read_text(encoding='utf-8'))))
        files.append(blocks_file)
    def put(key, text):
        nonlocal declaration_count
        values = found[key]
        if text not in values:
            values.add(text)
            declaration_count += 1
    def put_category(args, offset=None, creative_id=None):
        if not creative_files or len(args)<4 or not re.fullmatch(STRING,args[1]):return
        ident=args[3]
        if ident=='aCreativeTabID' and creative_id is not None:ident=creative_id
        if offset is not None:
            match=re.fullmatch(r'aID\s*(?:\+\s*(\d+))?',ident)
            if match:ident=str(offset+int(match[1] or 0))
        if ident.isdigit():put('itemGroup.gt.multitileentity.'+ident,json.loads(args[1]))
    recipes, recipe_files = recipe_map_names(original)
    files.extend(recipe_files)
    for key, value in recipes.items():put(key, value)
    _,material_english,material_files,_=material_identities(original) if material_data is None else material_data
    files.extend(p for p in material_files if p not in files)
    for key,value in material_english.items():put(key,value)
    material_text = masked((original / 'src/main/java/gregapi/data/MT.java').read_text(encoding='utf-8'))
    material_names = defaultdict(set)
    for m in re.finditer(r'\bwoodnormal\(\s*\d+\s*,\s*(' + STRING + r')\s*,\s*(' + STRING + ')', material_text):
        put('gt.material.'+json.loads(m[1]),json.loads(m[2]))
    for m in re.finditer(r'(\w+)\s*=\s*\w+\s*\(\s*\d+\s*,\s*(' + STRING + ')', material_text):
        material_names[m[1]].add(json.loads(m[2]))
    # Tier placeholders have no positive material ID, but their helper still declares a literal display name.
    if re.search(r'OreDictMaterial\s+tier\s*\(String aNameOreDict\)\s*\{return create\(-1, aNameOreDict\)',material_text):
        for m in re.finditer(r'(\w+)\s*=\s*tier\s*\(\s*('+STRING+r')\s*\)',material_text):
            material_names[m[1]].add(json.loads(m[2]))
            put(material_key(json.loads(m[2])),json.loads(m[2]))
    factories = {m[1]: json.loads(m[2]) for m in re.finditer(
        r'static\s+OreDictMaterial\s+(\w+)\s*\(\)\s*\{return\s+\w+\s*\(\s*\d+\s*,\s*(' + STRING + ')', material_text)}
    for line in material_text.splitlines():
        assignment = re.match(r'\s*(\w+)\s*=\s*(\w+)\s*\(', line)
        if assignment is None: continue
        symbol, factory = assignment.groups()
        if factory in factories: material_names[symbol].add(factories[factory])
        # setLocal is the original registration-time getLocal value, not an alias.
        local = re.findall(r'\.setLocal\(\s*(' + STRING + r')\s*\)', line)
        if factory == 'woodnormal':
            args = next(calls(line, factory))[1]
            if len(args)>2 and re.fullmatch(STRING,args[2]): local=[args[2]]
        if len(local)==1: material_names[symbol]={json.loads(local[0])}
    arrays = {key: [s.strip() for s in values.split(',')] for key,values in
              re.findall(r'\b(\w+_T)\s*=\s*\{([^}]+)\}', material_text)}
    any_internal,any_names,any_files=any_material_names(original)
    files.extend(any_files)
    for symbol,internal in any_internal.items():put(material_key(internal),any_names[symbol])
    # The separate, lazy Unused catalog can supply a name without proving a
    # numeric material identity. Do not let its fallback replace an active name.
    if re.search(r'OreDictMaterial\s+unused\s*\(String aNameOreDict\)\s*\{return create\(-1, aNameOreDict\)',material_text):
        for line in material_text.splitlines():
            row=re.search(r'\b\w+\s*=\s*unused\s*\(\s*('+STRING+r')\s*\)',line)
            if row is None:continue
            key=material_key(json.loads(row[1]))
            if key in found:continue
            local=re.findall(r'\.setLocal\s*\(\s*('+STRING+r')\s*\)',line)
            if len(local)>1 or len(local)!=len(re.findall(r'\.setLocal\s*\(',line)):continue
            put(key,json.loads(local[0] if local else row[1]))
    voltages, dyes, dye_posts, dye_indices = [], [], [], {}
    cs = original / 'src/main/java/gregapi/data/CS.java'
    if cs.exists():
        match = re.search(r'\bVN\s*=\s*\{([^}]+)\}', masked(cs.read_text(encoding='utf-8')))
        if match: voltages = [json.loads(v.strip()) for v in match[1].split(',')]
        match = re.search(r'\bDYE_NAMES\s*=\s*\{([^}]+)\}', masked(cs.read_text(encoding='utf-8')))
        if match: dyes = [json.loads(v.strip()) for v in match[1].split(',')]
        match = re.search(r'\bDYE_OREDICTS_POST\s*=\s*\{([^}]+)\}', masked(cs.read_text(encoding='utf-8')))
        if match: dye_posts = [json.loads(v.strip()) for v in match[1].split(',')]
        dye_indices = {key:int(value) for key,value in re.findall(r'\b(DYE_INDEX_\w+)\s*=\s*(\d+)\b',masked(cs.read_text(encoding='utf-8')))}
        files.append(cs)
    def material(expr):
        any_match = re.fullmatch(r'ANY\.(\w+)', re.sub(r'\s+', '', expr))
        if any_match: return any_names.get(any_match[1])
        array = re.fullmatch(r'MT\.DATA\.(\w+)\[(\d+)\]', re.sub(r'\s+', '', expr))
        if array:
            rows=arrays.get(array[1], [])
            if int(array[2])>=len(rows): return None
            symbol=rows[int(array[2])]
            if symbol.startswith('ANY.'): return any_names.get(symbol[4:])
            expr='MT.'+symbol
        match = re.fullmatch(r'MT\.(?:STONES\.)?(\w+)', re.sub(r'\s+', '', expr))
        names = material_names.get(match[1], set()) if match else set()
        return next(iter(names)) if len(names) == 1 else None
    def name(expr, mat=None):
        # Preserve all literal whitespace/case. No word matching or translated-name inference.
        expr=re.sub(STRING+r'|DYE_NAMES\[(DYE_INDEX_\w+)\s*\]',lambda m:
            f'DYE_NAMES[{dye_indices[m[1]]}]' if m[1] in dye_indices else m[0],expr)
        explicit_material = r'(?:MT\.(?:STONES\.)?|ANY\.)\w+\s*\.\s*(?:getLocal\(\)|mNameLocal)'
        parts = re.findall(STRING + '|' + explicit_material + r'|aMat\.getLocal\(\)|aMat\.mNameLocal|aDefaultLocalised|VN\[\d+\]|DYE_NAMES\[\d+\]|\+', expr)
        if re.sub(r'\s+', '', ''.join(parts)) != re.sub(r'\s+', '', expr):
            return None
        values = []
        for part in parts:
            if part == '+': continue
            if re.fullmatch(explicit_material,part):
                value=material(re.sub(r'\s*\.\s*(?:getLocal\(\)|mNameLocal)$','',part))
                if value is None:return None
                values.append(value)
            elif part in ('aMat.getLocal()', 'aMat.mNameLocal', 'aDefaultLocalised'):
                if mat is None: return None
                values.append(mat)
            elif part.startswith('VN['):
                index=int(part[3:-1])
                if index>=len(voltages): return None
                values.append(voltages[index])
            elif part.startswith('DYE_NAMES['):
                index=int(part[10:-1])
                if index>=len(dyes): return None
                values.append(dyes[index])
            else: values.append(json.loads(part))
        return ''.join(values)
    if blocks_file.exists():
        block_raw=masked(blocks_file.read_text(encoding='utf-8'))
        wire_helper=original/'src/main/java/gregtech/blocks/tool/BlockLongDistWire.java'
        wire_constructors=list(calls(block_raw,'new BlockLongDistWire'))
        if wire_constructors:
            formula=''.join(re.findall(STRING+r'|\S',masked(wire_helper.read_text(encoding='utf-8'))))
            expected='for(bytei=0;i<16;i++)LH.add(aUnlocalised+"."+i,"Long Distance Electric Wire ("+VN[mTiers[i]]+")");'
            if 'mTiers=aTiers;' not in formula or expected not in formula:
                raise ValueError('Unsupported original long-distance wire name formula')
            files.append(wire_helper)
            for _,args in wire_constructors:
                tiers=re.fullmatch(r'new byte\[\]\s*\{([\d,\s]+)\}',args[2]) if len(args)==3 else None
                if tiers is None or not re.fullmatch(STRING,args[0]):
                    raise ValueError('Unsupported original long-distance wire tiers')
                values=[int(v.strip()) for v in tiers[1].split(',')]
                if len(values)!=16 or any(v>=len(voltages) for v in values):
                    raise ValueError('Invalid original long-distance wire tiers')
                for i,tier in enumerate(values):
                    put(json.loads(args[0])+'.'+str(i),'Long Distance Electric Wire ('+voltages[tier]+')')
    rocks_file=original/'src/main/java/gregtech/loaders/a/Loader_Rocks.java'
    if rocks_file.exists():
        stone_file=original/'src/main/java/gregapi/block/metatype/BlockStones.java'
        wrapper=original/'src/main/java/gregtech/blocks/stone/BlockStonesGT.java'
        meta=original/'src/main/java/gregapi/block/metatype/BlockMetaType.java'
        files.extend([rocks_file,stone_file,wrapper,meta])
        wrapper_calls=list(calls(masked(wrapper.read_text(encoding='utf-8')),'super'))
        if not wrapper_calls or wrapper_calls[0][1][3:5]!=['aName','aMaterial.getLocal()']:
            raise ValueError('Unsupported original BlockStonesGT display-name constructor')
        meta_calls=list(calls(masked(meta.read_text(encoding='utf-8')),'super'))
        compact=lambda value: ''.join(re.findall(STRING+r'|\S',value))
        if not any(len(args)>1 and compact(args[1])=='aName+".slab."+aSlabType' for _,args in meta_calls):
            raise ValueError('Unsupported original stone slab name namespace')
        constructors=re.split(r'\b(?:public|protected)\s+BlockStones\s*\(',masked(stone_file.read_text(encoding='utf-8')))[1:]
        if len(constructors)!=2:raise ValueError('Expected original full/slab stone constructors')
        stone_expressions=[]
        for body in constructors:
            expressions={}
            for _,args in calls(body,'LH.add'):
                if len(args)!=2:continue
                match=re.fullmatch(r'getUnlocalizedName\(\)\+"\.(\d+)"',compact(args[0]))
                if not match:continue
                index=int(match[1])
                if index in expressions or name(args[1],'sample') is None:
                    raise ValueError('Ambiguous/unsupported original stone display formula')
                expressions[index]=args[1]
            if set(expressions)!=set(range(16)):
                raise ValueError('Incomplete original 16-variant stone names')
            stone_expressions.append(expressions)
        for _,args in calls(masked(rocks_file.read_text(encoding='utf-8')),'new BlockStonesGT'):
            if len(args)!=6 or not re.fullmatch(STRING,args[0]):
                raise ValueError('Unsupported original rock registration')
            base=json.loads(args[0]);display=material(args[1])
            if display is None:raise ValueError('Unresolved original rock material: '+args[1])
            for kind,expressions in enumerate(stone_expressions):
                bases=[base] if kind==0 else [base+'.slab.'+str(side) for side in range(6)]
                for stem in bases:
                    for index,expr in expressions.items():put(stem+'.'+str(index),name(expr,display))
    for path in sorted((original / 'src/main/java').rglob('*.java')):
        raw = masked(path.read_text(encoding='utf-8-sig'))
        before = declaration_count
        for key, text in re.findall(r'\bLH\.add\(\s*(' + STRING + r')\s*,\s*(' + STRING + ')', raw):
            put(json.loads(key), json.loads(text))
        if path.name == 'LH.java':
            constants = {key:json.loads(value) for key,value in re.findall(
                r'\b(\w+)\s*=\s*(' + STRING + ')',raw)}
            for _,args in calls(raw,'add'):
                if len(args)>1 and args[0] in constants and re.fullmatch(STRING,args[1]):
                    put(constants[args[0]],json.loads(args[1]))
                elif len(args)>1 and all(re.fullmatch(STRING,arg) for arg in args[:2]):
                    put(json.loads(args[0]),json.loads(args[1]))
        if path.name == 'MultiItemBumbles.java':
            for key, value in bumble_english(*bumble_declarations(raw)).items():
                put(key, value)
        multi_category = {'MultiItemBumbles':'bumblebee','MultiItemBooks':'books','MultiItemBottles':'bottles',
                          'MultiItemCans':'cans','MultiItemFood':'food','MultiItemRandomTools':'randomtools',
                          'MultiItemTechnological':'technological'}.get(path.stem)
        if creative_files and multi_category:
            for _,args in calls(raw,'new CreativeTab'):
                if len(args)==4 and args[0]=='getUnlocalizedName()' and re.fullmatch(STRING,args[1]):
                    put('itemGroup.gt.multiitem.'+multi_category,json.loads(args[1]))
        if path.name == 'Loader_Fluids.java':
            helper=original/'src/main/java/gregapi/data/FL.java'
            if helper.exists():
                helper_raw=masked(helper.read_text(encoding='utf-8'))
                compact=lambda value: ''.join(re.findall(STRING+r'|\S',value))
                formula=compact(helper_raw)
                if ('aName=aName.toLowerCase();' not in formula
                        or 'LH.add(rFluid.getUnlocalizedName(),aLocalized);' not in formula):
                    raise ValueError('Unsupported original fluid language registration')
                files.append(helper)
                icon_overload = re.search(r'\bcreate\(String aName,\s*IIconContainer aTexture,\s*String aLocalized,', helper_raw)
                for _,args in calls(raw,'FL.create'):
                    if len(args)>=4 and all(re.fullmatch(STRING,arg) for arg in args[:2]):
                        put('fluid.'+json.loads(args[0]).lower(),json.loads(args[1]))
                    elif len(args)>=4 and re.fullmatch(STRING,args[0]) and args[1]=='null' and args[2].startswith(('MT.','ANY.')):
                        fallback='aLocalized=(aLocalized==null?aMaterial==null||aMaterial==MT.NULL?UT.Code.capitaliseWords(aName):aMaterial.getLocal():aLocalized);'
                        if fallback not in formula:
                            raise ValueError('Unsupported original null fluid display-name fallback')
                        value=material(args[2])
                        if value is None:raise ValueError('Unresolved original fluid material name: '+args[2])
                        put('fluid.'+json.loads(args[0]).lower(),value)
                    elif len(args)>=11 and re.fullmatch(STRING,args[0]) and re.fullmatch(STRING,args[2]):
                        if not icon_overload:raise ValueError('Unsupported original fluid icon overload')
                        put('fluid.'+json.loads(args[0]).lower(),json.loads(args[2]))
                for loop in re.finditer(r'for\s*\((?:byte|int) i = 0; i < (\d+); i\+\+\)\s*\{([^{}]+)\}',raw):
                    for _,args in calls(loop[2],'FL.create'):
                        if not args or 'DYE_OREDICTS_POST[i]' not in args[0]:continue
                        prefix=re.fullmatch(r'('+STRING+r')\s*\+\s*DYE_OREDICTS_POST\[i\]\.toLowerCase\(\)',args[0])
                        if not icon_overload or not prefix or len(args)<11 or int(loop[1])!=16 or len(dyes)!=16 or len(dye_posts)!=16:
                            raise ValueError('Unsupported original colored fluid registration')
                        for i in range(16):
                            value=name(args[2].replace('DYE_NAMES[i]',f'DYE_NAMES[{i}]'))
                            if value is None:raise ValueError('Unsupported original colored fluid display name')
                            put('fluid.'+json.loads(prefix[1])+dye_posts[i].lower(),value)
        if path.name == 'ItemIntegratedCircuit.java':
            constructors=list(calls(raw,'super'))
            if len(constructors)!=1 or len(constructors[0][1])!=4:
                raise ValueError('Unsupported original selector tag constructor')
            args=constructors[0][1]
            if not all(re.fullmatch(STRING,args[i]) for i in (1,2,3)):
                raise ValueError('Unsupported original selector tag language literals')
            key=json.loads(args[1]);put(key,json.loads(args[2]))
            for _,args in calls(raw,'LH.add'):
                suffix=re.fullmatch(r'mName\s*\+\s*('+STRING+')',args[0])
                if suffix and re.fullmatch(STRING,args[1]):put(key+json.loads(suffix[1]),json.loads(args[1]))
        category = {'MultiItemRandomTools':'randomtools','MultiItemFood':'food','MultiItemBottles':'bottles',
                    'MultiItemCans':'cans','MultiItemTechnological':'technological','MultiItemBooks':'books'}.get(path.stem)
        if category:
            for _, args in calls(raw, 'addItem'):
                if len(args)<3 or not args[0].isdigit(): continue
                for suffix, expr in [('',args[1]),('.tooltip',args[2])]:
                    value=name(expr)
                    if value is not None: put(f'gt.multiitem.{category}.{args[0]}{suffix}',value)
            if category=='books':
                for loop in re.finditer(r'for\s*\(int i = 0; i < (\d+); i\+\+\)\s*\{([^{}]+)\}',raw):
                    rows=list(calls(loop[2],'addItem'))
                    if not rows:continue
                    if int(loop[1])!=11:raise ValueError('Unsupported original book color catalog')
                    for _,args in rows:
                        base=re.fullmatch(r'(?:(\d+)\s*\+\s*)?i',args[0])
                        if not base:raise ValueError('Unsupported original book color ID')
                        for i in range(11):
                            for suffix,expr in [('',args[1]),('.tooltip',args[2])]:
                                value=name(expr)
                                if value is None:raise ValueError('Unsupported original book color name')
                                put(f'gt.multiitem.books.{int(base[1] or 0)+i}{suffix}',value)
            if category=='randomtools':
                # Canvas colors are a single-statement loop, unlike paired spray states below.
                for line in raw.splitlines():
                    loop=re.search(r'for\s*\(int i = 0; i < (\d+); i\+\+\)\s*addItem\(',line)
                    if not loop:continue
                    for _,args in calls(line,'addItem'):
                        if len(args)<3 or 'DYE_NAMES[i]' not in args[1]:continue
                        base=re.fullmatch(r'i\s*\+\s*(\d+)',args[0])
                        if not base or int(loop[1])!=16 or len(dyes)!=16:
                            raise ValueError('Unsupported original colored tool registration')
                        for i in range(16):
                            for suffix,expr in [('',args[1]),('.tooltip',args[2])]:
                                value=name(expr.replace('DYE_NAMES[i]',f'DYE_NAMES[{i}]'))
                                if value is None:raise ValueError('Unsupported original colored tool name')
                                put(f'gt.multiitem.randomtools.{int(base[1])+i}{suffix}',value)
                for loop in re.finditer(r'for\s*\(byte i = 0; i < 16; i\+\+\)\s*\{([^}]+)\}',raw):
                    if 'IL.SPRAY_CAN_DYES[i]' not in loop[1]:continue
                    rows=[args for _,args in calls(loop[1],'addItem')]
                    if len(dyes)!=16 or not rows or len(rows)%2:
                        raise ValueError('Unsupported original full/used spray registration')
                    # The same loop also declares normal and owned foam; each pair resets mLastID.
                    for pair in range(0,len(rows),2):
                        base=re.fullmatch(r'(\d+)\+2\*i',re.sub(r'\s+','',rows[pair][0]))
                        if not base or re.sub(r'\s+','',rows[pair+1][0])!='mLastID+1':
                            raise ValueError('Unsupported original full/used spray registration')
                        for i in range(16):
                            for used,row in enumerate(rows[pair:pair+2]):
                                for suffix,expr in [('',row[1]),('.tooltip',row[2])]:
                                    value=name(expr.replace('DYE_NAMES[i]',f'DYE_NAMES[{i}]'))
                                    if value is None:raise ValueError('Unsupported original spray name/description')
                                    put(f'gt.multiitem.randomtools.{int(base[1])+2*i+used}{suffix}',value)
            if category=='technological':
                # Original compact components: one explicit 0..9 voltage loop.
                loop=re.search(r'for\s*\(int i = 0; i < 10; i\+\+\)\s*\{([^}]+)\}',raw)
                if loop:
                    for _,args in calls(loop[1],'addItem'):
                        base=re.fullmatch(r'(\d+)\s*\+\s*i',args[0])
                        if not base: continue
                        for i in range(10):
                            for suffix,expr in [('',args[1]),('.tooltip',args[2])]:
                                value=name(expr.replace('VN[i]',f'VN[{i}]'))
                                if value is not None:put(f'gt.multiitem.technological.{int(base[1])+i}{suffix}',value)
        if path.name == 'Loader_MultiTileEntities.java':
            wire_calls=list(calls(raw,'MultiTileEntityWireElectric.addElectricWires'))
            if wire_calls:
                wire_rows,helper_file=electric_wire_rows(original)
                files.append(helper_file)
                helper_categories=list(calls(masked(helper_file.read_text(encoding='utf-8')),'aRegistry.add'))
                for _,args in wire_calls:
                    if not args[0].isdigit():continue
                    if args[8] not in ('T','F'):raise ValueError('Unsupported original electric cable availability')
                    for _,row in helper_categories:put_category(row,int(args[0]),args[1])
                    for (kind,_),(offset,expression) in wire_rows.items():
                        if kind=='cable' and args[8]=='F':continue
                        value=name(expression,material(args[-1]))
                        if value is not None:put('gt.multitileentity.'+str(int(args[0])+offset),value)
            rows = list(calls(raw, 'aRegistry.add'))
            for pos, args in rows:
                put_category(args)
                if len(args)<3 or not args[2].isdigit(): continue
                prior = list(re.finditer(r'aMat\s*=\s*([^;]+);',raw[:pos]))
                value=name(args[0],material(prior[-1][1]) if prior else None)
                if value is not None: put('gt.multitileentity.'+args[2],value)
            for count,body in counted_blocks(raw):
                if count>300:continue
                for _,args in calls(body,'aRegistry.add'):
                    offset=re.fullmatch(r'(?:i\s*\+\s*(\d+)|(\d+)\s*\+\s*i)',args[2])
                    if not offset:continue
                    for i in range(count):
                        expression=re.sub(STRING+r'|VN\[i\]|\bi\b',lambda m:
                            f'VN[{i}]' if m[0]=='VN[i]' else json.dumps(str(i)) if m[0]=='i' else m[0],args[0])
                        value=name(expression)
                        if value is None:raise ValueError('Unsupported original counted block name: '+args[0])
                        put('gt.multitileentity.'+str(int(offset[1] or offset[2])+i),value)
            for line in raw.splitlines():
                loop=re.search(r'for\s*\(int i = 0; i < (\d+); i\+\+\)',line)
                if not loop or int(loop[1])>300:continue
                for _,args in calls(line,'aRegistry.add'):
                    base=re.fullmatch(r'i\s*\+\s*(\d+)',args[2]);value=name(args[0])
                    if base and value is not None:
                        for i in range(int(loop[1])):put('gt.multitileentity.'+str(int(base[1])+i),value)
            body=raw.split('private static void metalset(',1)[1].split('private static void storages(',1)[0]
            for _, args in calls(raw,'metalset'):
                if len(args)<7 or not args[6].isdigit(): continue
                mat=material(args[5])
                for _, row in calls(body,'aRegistry.add'):
                    put_category(row)
                    offset=re.fullmatch(r'(?:(\d+)\s*\+\s*)?aID',row[2])
                    value=name(row[0],mat)
                    if offset and value is not None:
                        put('gt.multitileentity.'+str(int(args[6])+int(offset[1] or 0)),value)
            for helper in ('Item','Fluid'):
                helper_file = original / f'src/main/java/gregapi/tileentity/connectors/MultiTileEntityPipe{helper}.java'
                if not helper_file.exists(): continue
                files.append(helper_file)
                helper_rows=list(calls(masked(helper_file.read_text(encoding='utf-8')), 'aRegistry.add'))
                for _, args in calls(raw, f'MultiTileEntityPipe{helper}.add{helper}Pipes'):
                    if not args[0].isdigit(): continue
                    mat=material(args[-1])
                    for _, row in helper_rows:
                        put_category(row,int(args[0]),args[1])
                        offset=re.fullmatch(r'aID\s*(?:\+\s*(\d+))?',row[2])
                        value=name(row[0],mat)
                        if offset and value is not None:
                            put('gt.multitileentity.'+str(int(args[0])+int(offset[1] or 0)),value)
        if path.name == 'OP.java':
            unused,unused_files=unused_prefix_names(original,raw)
            files.extend(unused_files)
            for key,text in unused.items():put(key,text)
            for key,text in re.findall(r'\bcreate\(\s*(' + STRING + r')\s*,\s*(' + STRING + ')',raw):
                put('oredict.prefix.'+json.loads(key),json.loads(text))
                if creative_files:
                    if not re.search(r'createPrefix\(aName\)\.setCategoryName\(aCategory\)\.setLocalPrefixName\(aCategory\)',raw):
                        raise ValueError('Unsupported original prefix category factory')
                    put('itemGroup.'+json.loads(key),json.loads(text))
        if path.name == 'Loader_Rails.java':
            for helper in ('new BlockBaseRail','new BlockRailRoad'):
                for _, args in calls(raw, helper):
                    if len(args)>2 and re.fullmatch(STRING,args[1]) and re.fullmatch(STRING,args[2]):
                        put(json.loads(args[1]),json.loads(args[2]))
        if path.name == 'Loader_Tools.java':
            for _, args in calls(raw, 'ToolsGT.sMetaTool.addTool'):
                if len(args)<3: continue
                ident=tools.get(args[0].removeprefix('ToolsGT.'))
                if ident is None: continue
                for suffix,expr in [('',args[1]),('.tooltip',args[2])]:
                    value=name(expr)
                    if value is not None: put('gt.metatool.01.'+ident+suffix,value)
        if path.name == 'BlockGlowtus.java' and len(dyes)==16:
            suffix = re.search(r'LH\.add\(getUnlocalizedName\(\)\+"\."\+i,\s*DYE_NAMES\[i\]\s*\+\s*(' + STRING + ')',raw)
            if suffix:
                for i,dye in enumerate(dyes): put('gt.block.lilypad.glowtus.'+str(i),dye+json.loads(suffix[1]))
        if path.stem in wood_classes:
            if re.search(r'extends\s+BlockColored\b',raw) and len(dyes)==16:
                # Only evaluate the literal constructor and the verified common BlockColored formula.
                colored = original/'src/main/java/gregapi/block/metatype/BlockColored.java'
                formula = ''.join(re.findall(STRING+r'|\S',masked(colored.read_text(encoding='utf-8'))))
                for suffix in ('', '+" Slab"'):
                    expected='for(inti=0;i<16;i++)LH.add(getUnlocalizedName()+"."+i,DYE_NAMES[i]+" "+aDefaultLocalised'+suffix+');'
                    if expected not in formula:
                        raise ValueError('Unsupported original BlockColored name formula')
                if colored not in files:files.append(colored)
                defaults = {json.loads(args[4]) for _,args in calls(raw,'super')
                            if len(args)>4 and re.fullmatch(STRING,args[4])}
                if len(defaults)!=1:raise ValueError('Ambiguous colored block constructor: '+str(path))
                base=wood_classes[path.stem];default=next(iter(defaults))
                for i,dye in enumerate(dyes):
                    put(f'{base}.{i}',dye+' '+default)
                    for side in range(6):put(f'{base}.slab.{side}.{i}',dye+' '+default+' Slab')
            constructors=re.split(r'\b(?:public|protected)\s+'+re.escape(path.stem)+r'\s*\(',raw)[1:]
            for index,body in enumerate(constructors):
                base=wood_classes[path.stem]
                if index>0:
                    # The second planks constructor creates all six oriented slabs.
                    if 'Planks' not in path.stem:continue
                    bases=[base+'.slab.'+str(side) for side in range(6)]
                else:bases=[base]
                for suffix,text in re.findall(r'LH\.add\(getUnlocalizedName\(\)\s*\+\s*(' + STRING + r')\s*,\s*(' + STRING + ')',body):
                    for base in bases:put(base+json.loads(suffix),json.loads(text))
        if declaration_count != before and path not in files: files.append(path)
    return {k:next(iter(v)) for k,v in found.items() if len(v)==1}, files


def material_prefix_templates(original, identity_rows):
    from generate_material_names import prefix_rows, default_templates, empty_templates
    base=original/'src/main/java'
    paths=[base/'gregapi/data/OP.java',base/'gregapi/oredict/OreDictPrefix.java',base/'gregapi/lang/LanguageHandler.java']
    op,helper,language=[masked(p.read_text(encoding='utf-8')) for p in paths]
    templates=default_templates(prefix_rows(op,helper),language)
    empty=empty_templates(language)
    values,bindings,unsupported={},{},{}
    for key,prefix,actual in identity_rows:
        if key.startswith('@empty-template.'):
            native=key.removeprefix('@empty-template.')
            if prefix not in empty or empty[prefix]!=actual:raise ValueError('Unsupported original empty form: '+native)
            original_key='oredict.'+prefix+'Empty'
            values[original_key]=empty[prefix];bindings[native]=original_key
            continue
        if not key.startswith('@prefix-template.'):continue
        native=key.removeprefix('@prefix-template.')
        if prefix not in templates:
            if actual:raise ValueError('Native name rule exists without original prefix: '+prefix)
            unsupported[native]=prefix;continue
        if templates[prefix]!=actual:
            raise ValueError('Shared naming rule differs from original default template: '+native)
        formula='@material-form.default.'+prefix
        if native in bindings and bindings[native]!=formula:raise ValueError('Conflicting native prefix template: '+native)
        values[formula]=templates[prefix];bindings[native]=formula
    return values,bindings,unsupported,paths


def require_retained_english_checks(previous, current, english):
    lost = sorted(key for key in previous if key in english and key not in current)
    if lost:
        raise ValueError('English source audit lost previously verified declarations: ' + ', '.join(lost[:12]))


def english_bindings(english, declarations, source, aliases, missing):
    """Original keys used directly need the same checks as modern aliases."""
    direct = {key:key for key in english if key in declarations}
    english_only = {key:original for key,original in {**direct, **missing}.items()
                    if key not in aliases and key in english and original in declarations and original not in source}
    return {**direct, **english_only, **aliases}, english_only


def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--source',type=Path,required=True)
    ap.add_argument('--identities',type=Path,required=True)
    ap.add_argument('--audit',type=Path,required=True)
    ap.add_argument('--write',action='store_true')
    ns=ap.parse_args()
    source,meta=load_source()
    aliases=read_json(ROOT/CONFIG_PATH/'aliases.json')
    english=read_json(ROOT/LANG_PATH/'en_us.json')
    initial_english=dict(english)
    material_data=material_identities(ns.source)
    source_en,files=original_english(ns.source,material_data)
    candidates=defaultdict(set)
    fallbacks=defaultdict(set)
    # Shared LH instructions/labels can reuse an identical unique source phrase.
    # Deliberately excludes item/block names: wording is never an item identity.
    shared_phrases=defaultdict(set)
    for key,value in source_en.items():
        if key.startswith('gt.lang.') and key in source and value:
            shared_phrases[value].add(key)
    for key,value in english.items():
        if key.startswith('tooltip.gregtech.') and key not in aliases and len(shared_phrases[value])==1:
            candidates[key].update(shared_phrases[value])
    shelves = ROOT/'core/src/main/resources/data/gregtech/bookshelf_variants.json'
    for shelf in read_json(shelves)['variants']:
        key = 'block.gregtech.'+shelf['path']
        original = 'gt.multitileentity.'+str(shelf['gt6_id'])
        if original in source_en:
            candidates[key].add(original)
            fallbacks[key].add(source_en[original])
    item_candidates,item_files=numbered_item_identities(ns.source)
    for key,original in item_candidates.items():
        if key in english: candidates[key].add(original)
    files.extend(item_files)
    identity_rows = [line.split('\t') for line in ns.identities.read_text(encoding='utf-8-sig').splitlines()]
    template_values,template_bindings,unsupported_prefixes,template_files=material_prefix_templates(ns.source,identity_rows)
    source_en.update(template_values)
    files.extend(template_files)
    for native,formula in template_bindings.items():candidates[native].add(formula)
    generated_fluids,fluid_files=material_fluid_english(ns.source,identity_rows,material_data)
    for key,value in generated_fluids.items():source_en.setdefault(key,value)
    files.extend(fluid_files)
    material_ids,_,_,ambiguous_material_ids=material_data
    material_proofs=verified_material_proofs(identity_rows,material_ids)
    symbols = {key.removeprefix('@symbol.'):original for key, original, _ in identity_rows
               if key.startswith('@symbol.')}
    pipes, pipe_files = pipe_identities(ns.source, symbols)
    wires,wire_files=wire_identities(ns.source,symbols)
    pipes.update(wires)
    pipes.update({'@tool.'+key:'gt.metatool.01.'+value for key,value in tool_ids(ns.source).items()})
    files = sorted(set(files + pipe_files + wire_files))
    for key,original,fallback in identity_rows:
        if key.startswith(('@symbol.','@material-proof.','@fluid-proof.','@prefix-template.','@empty-template.')): continue
        if key.startswith('material.gregtech.') and original.startswith('gt.material.'):
            original=material_key(original.removeprefix('gt.material.'))
        original = pipes.get(original, original)
        candidates[key].add(original)
        if fallback: fallbacks[key].add(fallback)
    # A name already bound by numeric identity also owns that ID's description.
    for key in english:
        if key.endswith('.tooltip') and key[:-8] in aliases:
            original = aliases[key[:-8]] + '.tooltip'
            if original in source: candidates[key].add(original)
    added,conflicts,missing,ambiguous,resolved_collisions={},{},{},{},{}
    runtime_fluid_fallbacks={}
    for key, originals in sorted(candidates.items()):
        if len(originals)>1:
            resolved=resolve_material_collision(key,originals,material_proofs)
            if len(resolved)==1:
                resolved_collisions[key]={'candidates':sorted(originals),'actual_material':next(iter(resolved))}
                originals=resolved
        if len(originals)!=1:
            ambiguous[key]=sorted(originals);continue
        original=next(iter(originals))
        if key not in english:
            if key in template_bindings:continue  # Compatibility-only prefixes need no unused language keys.
            if key.startswith('fluid_type.gregtech.') and original not in source and key not in aliases:
                # Native describe() already localizes the material when no complete
                # fluid name is declared. Adding English here would shadow that
                # Chinese fallback (e.g. Water / C-Foam / Molten Graphene).
                missing[key]=original
                if original in source_en:
                    runtime_fluid_fallbacks[key]={'source_key':original,'expected_english':source_en[original]}
                continue
            if original in source_en:
                english[key]=source_en[original]
            elif len(fallbacks[key]) == 1 and key.startswith(('material.gregtech.','item.gregtech.tab_icon_', 'block.gregtech.bookshelf','itemGroup.gregtech.')):
                english[key]=source_en.get(original,next(iter(fallbacks[key])))
            # Modern fluid paths are sanitized; reuse the existing English declaration.
            elif key.startswith('fluid_type.gregtech.'):
                legacy='fluid_type.gregtech.'+original.removeprefix('fluid.')
                if legacy not in english:continue
                english[key]=english[legacy]
            else:continue
        if original not in source:
            missing[key]=original;continue
        if key in aliases and aliases[key]!=original:
            # Accepted special original internal spellings must not be guessed from modern material names.
            if len(material_proofs[key])==1 and next(iter(material_proofs[key]))[1]==original:
                ident=next(iter(material_proofs[key]))[0]
                conflicts[key]={'before':aliases[key],'after':original,'source_material_id':ident,
                                'reason':'Actual shared material ID matches original MT declaration; old field alias is not the material internal name'}
            elif key=='block.gregtech.mortar_block':
                conflicts[key]={'before':aliases[key],'after':original,'reason':'Original ceramic mortar:32735; sapphire tool variant:32075'}
            elif key=='block.gregtech.wood_barrel' and original=='gt.multitileentity.32733':
                conflicts[key]={'before':aliases[key],'after':original,'reason':'CheapWoodBarrelCatalog retains the original lead-rod cheap barrel, not the normal barrel 32714'}
            elif key.endswith('.tooltip') and aliases.get(key[:-8], '') + '.tooltip' == original:
                conflicts[key]={'before':aliases[key],'after':original,'reason':'Description belongs to the accepted original item number, even when multiple items share its text'}
            else:
                conflicts[key]={'accepted':aliases[key],'candidate':original,'action':'retained for source identity review'};continue
        if aliases.get(key)!=original:
            added[key]=original;aliases[key]=original
    added_tooltips={}
    for key,original in list(aliases.items()):
        if not key.startswith('item.gregtech.') or key.endswith('.tooltip') or not original.startswith('gt.multiitem.'):continue
        native,legacy=key+'.tooltip',original+'.tooltip'
        if key in english and native not in english and legacy in source and legacy in source_en:
            english[native]=source_en[legacy];aliases[native]=legacy;added_tooltips[native]=legacy
    english_changes={}
    # A missing Chinese entry must not prevent checking an adopted original English name.
    # Existing accepted Chinese identities still win over conflicting candidates.
    english_identities,english_only=english_bindings(english,source_en,source,aliases,missing)
    for key, original in sorted(english_identities.items()):
        if key not in english and original in source_en:
            english[key]=source_en[original]
        if key in english and original in source_en and english[key]!=source_en[original]:
            english_changes[key]={'before':english[key],'after':source_en[original],'source_key':original}
            english[key]=source_en[original]
    english_additions={key:{'value':value,'source_key':english_identities.get(key)}
                       for key,value in english.items() if key not in initial_english}
    pinned={key:{'source_key':original,'value':source_en[original],
                 **({'source_formula':'material_default_name'} if original.startswith('@material-form.default.') else {}),
                 **({'chinese_source_missing':True} if key in english_only else {})}
            for key,original in english_identities.items() if key in english and original in source_en}
    require_retained_english_checks(read_json(ROOT/CONFIG_PATH/'english_source.json')['values'],pinned,english)
    report={'source_language_sha256':meta['sha256'],'new_bindings':added,'conflicts':conflicts,
            'verified_english_declarations':len(pinned),
            'default_material_templates':template_bindings,'unsupported_native_prefixes':unsupported_prefixes,
            'templates_without_native_keys':sorted(key for key in template_bindings if key not in initial_english),
            'ambiguous_original_material_ids':ambiguous_material_ids,
            'resolved_material_collisions':resolved_collisions,
            'missing_original':missing,'ambiguous_symbols':ambiguous,'english_changes':english_changes,'english_additions':english_additions,'added_tooltips':added_tooltips,
            'english_only_bindings':english_only,
            'generated_fluid_english':{key:value for key,value in generated_fluids.items()
                                       if source_en.get(key)==value},
            'runtime_fluid_fallbacks':runtime_fluid_fallbacks,
            'source_files':[{'path':str(p.resolve()),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in files],
            'identity_export_sha256':hashlib.sha256(ns.identities.read_bytes()).hexdigest(),
            'authors':'GregTech-6 Team / Gregorius Techneticies; original Java LGPL-3.0-or-later',
            'policy':'Chinese copied verbatim from pinned user file; local work only; conflicting modern symbol spellings are not inferred'}
    translated=expected_chinese(english,source,aliases)
    report['english_preflight']=check_english(ROOT,english,translated,aliases,source,pinned=pinned)
    if ns.write:
        (ROOT/CONFIG_PATH/'aliases.json').write_text(json_text(aliases),encoding='utf-8')
        (ROOT/LANG_PATH/'en_us.json').write_text(json_text(english),encoding='utf-8')
        (ROOT/LANG_PATH/'zh_cn.json').write_text(json_text(translated),encoding='utf-8')
        (ROOT/CONFIG_PATH/'english_source.json').write_text(json_text({
            'policy':'Exact original English for source declarations resolved by adopted identity; remaining port text checked separately',
            'values':pinned,'source_files':report['source_files']}),encoding='utf-8')
    ns.audit.write_text(json_text(report),encoding='utf-8')
    print(json.dumps({k:len(report[k]) for k in ['new_bindings','conflicts','missing_original','ambiguous_symbols','english_changes','english_additions','added_tooltips']}))


if __name__=='__main__':main()
