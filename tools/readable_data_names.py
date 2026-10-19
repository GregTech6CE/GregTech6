"""Java identifier migration and table layout shared by import tools.

Only Java identifiers are changed; registry strings and GT6 source comments stay
verbatim. Short names are accepted only as upstream importer input; emitted Java uses full names.
Complete definition rows are then aligned with whitespace-only column padding.
"""
from modular_materials import replace_identifiers
from format_definition_tables import align_java_tables

DATA_NAMES = {
    'MT': 'ImportedMaterialData',
    'CS': 'GregTechConstants', 'FL': 'RegisteredFluids', 'FM': 'FuelRecipeMaps',
    'RM': 'MachineRecipeMaps', 'OP': 'MaterialPrefixes', 'TD': 'GregTechTags',
    'MD': 'ModReferences', 'OD': 'OreDictionaryNames', 'IL': 'ItemReferences',
    'LH': 'TranslationKeys', 'BI': 'BlockIcons', 'TC': 'AspectReferences',
    'AM': 'AntimatterMaterials', 'ANY': 'MaterialGroups',
}

def readable_java(text):
    return align_java_tables(replace_identifiers(text, DATA_NAMES))
