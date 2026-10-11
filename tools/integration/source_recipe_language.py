"""Read original recipe-map labels through their constructor forwarding chain."""
import json
import re

from generate_machine_material_data import calls, masked
from source_language_common import STRING


def recipe_map_names(original):
    java = original / 'src/main/java/gregapi'
    catalogs = [p for p in (java / 'data/RM.java', java / 'data/FM.java') if p.exists()]
    if not catalogs:
        return {}, []
    base = java / 'recipes/Recipe.java'
    code = re.sub(r'\s+', '', masked(base.read_text(encoding='utf-8')))
    if not all(part in code for part in ('mNameInternal=aNameInternal;', 'mNameLocal=aNameLocal;',
                                         'LH.add(mNameInternal,mNameLocal);')):
        raise ValueError('Unsupported original recipe-map language registration')
    files = [base, *catalogs]
    suffixes = {'RecipeMap': ''}

    def suffix(name, pending=()):
        if name in suffixes:
            return suffixes[name]
        if name in pending:
            raise ValueError('Cyclic original recipe-map inheritance: ' + name)
        path = java / ('recipes/maps/' + name + '.java')
        raw = masked(path.read_text(encoding='utf-8'))
        parent = re.search(r'\bclass\s+' + re.escape(name) + r'\s+extends\s+(RecipeMap\w*)\b', raw)
        forwarded = [args for _, args in calls(raw, 'super')]
        if (not parent or len(forwarded) != 1 or len(forwarded[0]) < 3
                or forwarded[0][1] != 'aUnlocalizedName'):
            raise ValueError('Unsupported original recipe-map forwarding: ' + name)
        label = re.fullmatch(r'aNameLocal\s*(?:\+\s*(' + STRING + r'))?', forwarded[0][2])
        if label is None:
            raise ValueError('Unsupported original recipe-map name formula: ' + name)
        result = (json.loads(label[1]) if label[1] else '') + suffix(parent[1], (*pending, name))
        suffixes[name] = result
        files.append(path)
        return result

    names = {}
    for catalog in catalogs:
        raw = masked(catalog.read_text(encoding='utf-8'))
        for name in sorted(set(re.findall(r'\bnew\s+(RecipeMap\w*)\s*\(', raw))):
            ending = suffix(name)
            for _, args in calls(raw, 'new ' + name):
                if len(args) < 3 or not all(re.fullmatch(STRING, arg) for arg in args[1:3]):
                    raise ValueError('Nonliteral original recipe-map label: ' + name)
                key, value = json.loads(args[1]), json.loads(args[2]) + ending
                if key in names and names[key] != value:
                    raise ValueError('Conflicting original recipe-map label: ' + key)
                names[key] = value
    return names, files
