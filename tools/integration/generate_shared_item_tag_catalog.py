"""Keep NeoForge's singular-path adapter in sync with every shared item tag."""
from pathlib import Path
import argparse, re
p=argparse.ArgumentParser(description=__doc__); p.add_argument('--check',action='store_true'); a=p.parse_args()
root=Path(__file__).resolve().parents[2]
data=root/'core/src/main/resources/data'
tags=sorted(str(path.relative_to(data)).replace('\\','/') for path in data.glob('*/tags/items/**/*.json'))
file=root/'core/src/main/java/com/gregtech/gregtech/content/recipe/EquipmentCraftingCatalog.java'
text=file.read_text(encoding='utf-8')
value='    public static final java.util.List<String> TAGS = java.util.List.of(\n'+',\n'.join('            "'+tag.split('/')[0]+':'+tag.split('/tags/items/')[1][:-5]+'"' for tag in tags)+');'
updated=re.sub(r'    public static final java.util.List<String> TAGS = java.util.List.of\(.*?\);',lambda _:value,text,flags=re.S)
if a.check:
    assert text==updated,'Run generate_shared_item_tag_catalog.py after adding shared tags'
else:file.write_text(updated,encoding='utf-8')
print('Shared item tags:',len(tags))
