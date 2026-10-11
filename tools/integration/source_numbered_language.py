"""Read original numbered bumble declarations for both language import paths."""
import json
import re

from generate_machine_material_data import match_paren, masked, split_top_level
from source_language_common import STRING


def local_calls(raw, name):
    # ST.make/FL.make create recipe stacks, not bumble species. Also ignore Java
    # string contents before looking for a closing parenthesis inside a call.
    literals = [(m.start(), m.end()) for m in re.finditer(STRING, raw)]
    for match in re.finditer(r'(?<![\w$])' + re.escape(name) + r'\s*\(', raw):
        pos = match.start()
        if raw[:pos].rstrip().endswith('.') or any(a <= pos < b for a, b in literals):
            continue
        opening = raw.index('(', pos)
        closing = match_paren(raw, opening)
        yield split_top_level(raw[opening + 1:closing])


def bumble_declarations(text):
    raw = masked(text)
    species, states = {}, {}
    for args in local_calls(raw, 'make'):
        if len(args) == 3 and args[0] == 'int aSpeciesID':
            continue  # The helper declaration, not a registration.
        if (len(args) != 3 or not args[0].isdigit()
                or not all(re.fullmatch(STRING, arg) for arg in args[1:])):
            raise ValueError('Unsupported original bumble species declaration')
        ident = int(args[0])
        if ident in species:
            raise ValueError('Duplicate original bumble species ID: ' + str(ident))
        species[ident] = tuple(json.loads(arg) for arg in args[1:])
    for args in local_calls(raw, 'addItem'):
        ident = re.fullmatch(r'aSpeciesID\s*\+\s*(\d+)', args[0]) if args else None
        suffix = re.fullmatch(r'aName\s*\+\s*(' + STRING + ')', args[1]) if len(args) > 2 else None
        if ident is None or suffix is None or args[2] != 'aTooltip':
            raise ValueError('Unsupported original bumble state declaration')
        offset = int(ident[1])
        if offset in states:
            raise ValueError('Duplicate original bumble state offset: ' + str(offset))
        states[offset] = json.loads(suffix[1])
    if not species or not states:
        raise ValueError('Missing original bumble species or states')
    return species, states


def bumble_english(species, states):
    result = {}
    for ident, (name, tooltip) in species.items():
        for offset, suffix in states.items():
            key = 'gt.multiitem.bumblebee.' + str(ident + offset)
            if key in result:
                raise ValueError('Overlapping original bumble registration: ' + key)
            result[key] = name + suffix
            result[key + '.tooltip'] = tooltip
    return result
