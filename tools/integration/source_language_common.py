"""Shared lexical/material spelling rules for read-only original language importers."""
import re

STRING = r'"(?:\\.|[^"\\])*"'


def counted_blocks(raw):
    """Read literal zero-based i++ loops; retain nested blocks and string braces."""
    structure = re.sub(STRING + r"|'(?:\\.|[^'\\])*'", lambda m: ' ' * len(m[0]), raw)
    pattern = r'for\s*\(\s*(?:int|byte)\s+i\s*=\s*0\s*;\s*i\s*<\s*(\d+)\s*;\s*i\+\+\s*\)\s*\{'
    for match in re.finditer(pattern, structure):
        start = match.end()
        depth = 1
        end = start
        while end < len(structure) and depth:
            depth += (structure[end] == '{') - (structure[end] == '}')
            end += 1
        if depth:
            raise ValueError('Unclosed original language loop')
        yield int(match[1]), raw[start:end - 1]


def material_key(name):
    internal = re.sub(r"[ \-'/]", '', name)
    return 'gt.material.' + internal[:1].upper() + internal[1:]
