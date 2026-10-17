"""Translate original MultiItemFood crafting calls using exact numbered identities.

Only explicit native ingredients are emitted. Unresolved source rows are reported, never guessed.
"""
from pathlib import Path
import argparse,collections,hashlib,json,re,sys
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from extract_gt6_block_recipes import split_args
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',type=Path,required=True);args=p.parse_args()
def save(path,value):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def calls(raw,pattern):
 for m in re.finditer(pattern,raw):
  start=raw.index('(',m.start());depth=0;quote=False
  for end in range(start,len(raw)):
   char=raw[end]
   if char=='"' and raw[end-1]!='\\':quote=not quote
   if not quote:
    depth+=(char=='(')-(char==')')
    if depth==0:break
  yield m,raw[start:end+1]
aliases=json.loads((ROOT/'tools/localization/aliases.json').read_text(encoding='utf-8'))
identities={v:k.removeprefix('item.gregtech.') for k,v in aliases.items() if re.fullmatch(r'item\.gregtech\.[a-z0-9_]+',k)}
foods=ROOT/'core/src/main/java/com/gregtech/gregtech/data/generated/GTFoodItemsGen.java'
foodmeta={int(n):id for id,n in re.findall(r'new FoodNutritionRows.Entry\("([^"]+)",\s*"[^"]*",\s*(\d+)',foods.read_text(encoding='utf-8'))}
il={};tags=collections.defaultdict(set);sources={}
for family in ['Food','Bottles']:
 path=args.source/f'gregtech/items/MultiItem{family}.java';raw=path.read_text(encoding='utf-8')
 sources[str(path)]=hashlib.sha256(path.read_bytes()).hexdigest()
 raw=re.sub(r'/\*.*?\*/','',raw,flags=re.S);raw='\n'.join(line.split('//')[0] for line in raw.splitlines())
 if family=='Food':foodraw=raw
 meta={}
 for m,call in calls(raw,r'\baddItem\s*\('):
  a=split_args(call)
  if not a[0].isdigit():continue
  number=int(a[0]);id=identities.get(f'gt.multiitem.{family.lower()}.{number}')
  if family=='Food':id=foodmeta.get(number,id)
  if not id:continue
  meta[number]='gregtech:'+id
  for token in a[3:]:
   if re.fullmatch(r'"(?:food|crop|bottle)[A-Za-z0-9]+"',token):tags[json.loads(token)].add(meta[number])
 for name,number in re.findall(r'IL\.(\w+)\s*\.set\(\s*addItem\(\s*(\d+)',raw):
  if int(number) in meta:il[name]=meta[int(number)]
 for name,item in re.findall(r'IL\.(\w+)\s*\.set\(ST.make\(Items\.(\w+)',raw):il[name]='minecraft:'+item
# Original vanilla crop/food ore dictionary equivalences.
for tag,id in {'cropCarrot':'carrot','cropPotato':'potato','cropApple':'apple','foodBread':'bread','foodCookie':'cookie',
               'cropAppleRed':'apple','foodEgg':'egg','listAllegg':'egg','cropMelon':'melon_slice','cropPumpkin':'pumpkin'}.items():tags[tag].add('minecraft:'+id)
# LoaderOreDictReRegistrations:870 explicitly maps bottleCream to foodHeavycream.
tags['foodHeavycream'].update(tags['bottleCream'])
tools={'k':'knife','s':'saw','r':'soft_hammer','h':'hammer','m':'mortar','f':'file','v':'saw'}
usedtags=set()
def ingredient(exp):
 exp=exp.strip()
 m=re.fullmatch(r'IL\.(\w+)(?:\.get\(\s*1\s*\))?',exp)
 if m:
  if m[1] not in il:raise ValueError(exp)
  return {'item':il[m[1]]}
 m=re.fullmatch(r'Items\.(\w+)',exp)
 if m:return {'item':'minecraft:'+m[1]}
 if exp.startswith('OreDictToolNames.'):
  name=exp.split('.')[-1];return {'tag':'forge:tools/'+{'rollingpin':'rolling_pin'}.get(name,name)}
 m=re.fullmatch(r'OP\.(\w+)\.dat\((?:MT|ANY)\.(\w+)\)',exp)
 if m:
  prefix,material=m.groups()
  if prefix=='stick' and material=='Wood':return {'tag':'gregtech:ammunition/sticks_wood'}
  prefix=re.sub(r'(?<=[a-z0-9])(?=[A-Z])','_',prefix).lower()
  material={'Al':'Aluminium'}.get(material,material)
  material=re.sub(r'(?<=[a-z0-9])(?=[A-Z])','_',material).lower()
  return {'tag':f'gregtech:{prefix}/{material}'}
 if exp.startswith('"'):
  tag=json.loads(exp)
  if not tags[tag]:raise ValueError('unresolved food group '+tag)
  usedtags.add(tag);return {'tag':'gregtech:food/'+tag.lower()}
 raise ValueError(exp)
rows=[];skipped=[];files=[];serial=collections.Counter()
for m,call in calls(foodraw,r'CR\.(?:shaped|shapeless)\s*\('):
 a=split_args(call);out=re.fullmatch(r'IL\.(Food_\w+)\s*\.get\(\s*(\d+)\s*\)',a[0])
 cake=a[0]=='ST.make(Items.cake, 1, 0)'
 if not out and not cake:continue
 try:
  output='minecraft:cake' if cake else il[out[1]];count=1 if cake else int(out[2]);result={'item':output,'count':count}
  if foodraw[m.start():].startswith('CR.shapeless'):
   body=re.fullmatch(r'new Object\[\]\s*\{(.*)\}',a[2],re.S)
   if not body:raise ValueError(a[2])
   ingredients=[ingredient(exp) for exp in split_args('('+body[1]+')')]
   row={'type':'gregtech:tool_shapeless','ingredients':ingredients,'result':result}
  else:
   patterns=[];index=2
   while index<len(a) and a[index].startswith('"'):patterns.append(json.loads(a[index]));index+=1
   keys={a[i][1]:ingredient(a[i+1]) for i in range(index,len(a),2)}
   for char in set(''.join(patterns))-set(keys)-{' '}:
    if char not in tools:raise ValueError('tool '+char)
    keys[char]={'tag':'forge:tools/'+tools[char]}
   row={'type':'gregtech:tool_shaped','pattern':patterns,'key':keys,'result':result,'allow_mirror':'MIR' in a[1]}
  row['gregtech_autocraftable']='NCC' not in a[1]
  id=output.split(':')[1];serial[id]+=1;file=f'food/source/{id}_{serial[id]}.json'
  save(ROOT/'core/src/main/resources/data/gregtech/recipes'/file,row);files.append(file)
  rows.append({'file':file,'source_call':call})
 except (ValueError,KeyError) as ex:skipped.append({'call':call,'reason':str(ex)})
for tag in sorted(usedtags):save(ROOT/f'core/src/main/resources/data/gregtech/tags/items/food/{tag.lower()}.json',{'replace':False,'values':sorted(tags[tag])})
catalog=ROOT/'core/src/main/java/com/gregtech/gregtech/content/recipe/EquipmentCraftingCatalog.java';text=catalog.read_text(encoding='utf-8')
for field,values in [('FILES',files),('TAGS',['gregtech:food/'+t.lower() for t in sorted(usedtags)])]:
 anchor=f'    public static final java.util.List<String> {field} = java.util.List.of(\n';assert anchor in text
 text=text.replace(anchor,anchor+''.join('            '+json.dumps(v)+',\n' for v in values if '"'+v+'"' not in text))
catalog.write_text(text,encoding='utf-8')
save(ROOT/'docs/integration/verification/food-crafting-source-20261018.json',{'author':'GregTech-6 Team / Gregorius Techneticies','license':'LGPL-3.0-or-later','source_sha256':sources,'rows':rows,'unresolved':skipped})
print(f'{len(rows)} source food recipes; {len(usedtags)} food tags; {len(skipped)} unresolved calls')
for row in skipped:print(row['reason'])
