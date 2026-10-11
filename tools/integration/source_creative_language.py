"""Verify the original creative-tab naming path before reading its arguments."""
import json
import re

from generate_machine_material_data import masked
from source_language_common import STRING


def creative_name_sources(original):
    snippets = {
        'gregapi/item/CreativeTab.java': (
            'LH.add("itemGroup."+aName,aLocal);',),
        'gregapi/block/multitileentity/MultiTileEntityRegistry.java': (
            'new CreativeTab(mNameInternal+"."+aClassContainer.mCreativeTabID,aCategoricalName,Item.getItemFromBlock(mBlock),aClassContainer.mCreativeTabID)',),
        'gregapi/item/prefixitem/PrefixItem.java': (
            'new CreativeTab(mPrefix.mNameInternal,mPrefix.mNameCategory,this,W)',),
        'gregapi/oredict/OreDictPrefix.java': (
            'mNameCategory=aCategoryName;',),
    }
    files = [original / 'src/main/java' / path for path in snippets]
    if not any(path.exists() for path in files):
        return []  # Minimal fixtures without a creative-tab subsystem.
    for path, required in zip(files, snippets.values()):
        if not path.exists():
            raise ValueError('Missing original creative naming source: ' + str(path))
        raw = ''.join(re.findall(STRING + r'|\S', masked(path.read_text(encoding='utf-8'))))
        if any(''.join(re.findall(STRING + r'|\S', snippet)) not in raw for snippet in required):
            raise ValueError('Unsupported original creative naming formula: ' + str(path))
    return files


def unused_prefix_names(original, op):
    """Unused prefixes retain their factory name, separately from their category."""
    rows = re.findall(r'\b\w+\s*=\s*unused\(\s*(' + STRING + r')\s*\)([^;\n]*)', op)
    if not rows:
        return {}, []
    helper = original / 'src/main/java/gregapi/oredict/OreDictPrefix.java'
    proxy = original / 'src/main/java/gregapi/GT_API_Proxy_Client.java'
    required = {
        helper: ('String tName = aName.replaceAll(" ", "").replaceAll("-", "");',
                 'return rPrefix == null ? new OreDictPrefix(tName, aName) : rPrefix;',
                 'mNameCategory = mNameLocal = aNameLocal;', 'mNameLocal = aLocalName;'),
        proxy: ('LH.add("oredict.prefix." + tPrefix.mNameInternal, tPrefix.mNameLocal);',),
    }
    compact = lambda text: ''.join(re.findall(STRING + r'|\S', text))
    if compact('return OreDictPrefix.createPrefix(aName).add(PREFIX_UNUSED);') not in compact(op):
        raise ValueError('Unsupported original unused prefix factory')
    for path, snippets in required.items():
        raw = compact(masked(path.read_text(encoding='utf-8')))
        if any(compact(snippet) not in raw for snippet in snippets):
            raise ValueError('Unsupported original unused prefix name: ' + str(path))
    result = {}
    for literal, chain in rows:
        name = json.loads(literal)
        overrides = re.findall(r'\.setLocalPrefixName\(\s*(' + STRING + r')\s*\)', chain)
        if len(overrides) != len(re.findall(r'\.setLocalPrefixName\(', chain)) or len(overrides) > 1:
            raise ValueError('Unsupported original unused prefix override: ' + name)
        key = 'oredict.prefix.' + name.replace(' ', '').replace('-', '')
        value = json.loads(overrides[0]) if overrides else name
        if key in result and result[key] != value:
            raise ValueError('Ambiguous original unused prefix name: ' + key)
        result[key] = value
    return result, list(required)
