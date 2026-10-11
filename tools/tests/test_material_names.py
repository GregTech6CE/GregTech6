"""Small source-parser contracts; runtime naming is checked separately in shared Java."""
from pathlib import Path
import sys
import unittest
import tempfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'integration'))
from generate_material_names import block_end, material_symbols, prefix_rows, default_templates, empty_templates
from import_identity_localization import material_prefix_templates


class MaterialNameGeneratorTests(unittest.TestCase):
    def test_default_crates_use_child_name_instead_of_plural_postfix(self):
        prefixes={'dust':('', ' Dust', []), 'gem':('', '', []),
                  'blockDust':('Block of ', ' Dusts', []), 'blockGem':('Block of ', ' Gems', [])}
        source='''if (aPrefix == OP.blockDust) return aPrefix.mMaterialPre + getLocalName(OP.dust, aMaterial);
if (aPrefix == OP.blockGem) return aPrefix.mMaterialPre + getLocalName(OP.gem, aMaterial);
return aPrefix.mMaterialPre + aMaterial.mNameLocal + aPrefix.mMaterialPost;'''
        self.assertEqual(default_templates(prefixes,source), {'dust':'%s Dust','gem':'%s',
                         'blockDust':'Block of %s Dust','blockGem':'Block of %s'})
        with self.assertRaisesRegex(ValueError,'cyclic'):
            default_templates(prefixes,source.replace('OP.dust,','OP.blockDust,'))
        with self.assertRaisesRegex(ValueError,'recursive'):
            default_templates(prefixes,source.replace('aPrefix.mMaterialPre + getLocalName','getLocalName'))
        with self.assertRaisesRegex(ValueError,'default'):
            default_templates(prefixes,source.replace('aMaterial.mNameLocal','"changed"'))

    def test_empty_ammunition_literal_does_not_take_april_concatenation(self):
        source='''if (APRIL_FOOLS) {if (aMaterial == MT.Empty) {
if (aPrefix == OP.bulletGtSmall) return aPrefix.mMaterialPre + "Bolt Shaft";
}}
if (aMaterial == MT.Empty) {if (aPrefix == OP.bulletGtSmall) return "Small Bullet Casing";}'''
        self.assertEqual(empty_templates(source), {'bulletGtSmall':'Small Bullet Casing'})
        with self.assertRaisesRegex(ValueError,'Ambiguous'):
            empty_templates(source+source.replace('Small Bullet Casing','Changed'))

    def test_exported_native_template_must_match_original_source(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);base=root/'src/main/java/gregapi'
            for name in ('data','oredict','lang'):(base/name).mkdir(parents=True)
            (base/'data/OP.java').write_text('dust = create("dust", "Dusts", "", " Dust");')
            (base/'oredict/OreDictPrefix.java').write_text('add(MATERIAL_BASED, UNIFICATABLE, ORE, TOOLTIP_ENCHANTS);')
            (base/'lang/LanguageHandler.java').write_text('return aPrefix.mMaterialPre + aMaterial.mNameLocal + aPrefix.mMaterialPost;')
            rows=[('@prefix-template.item.gregtech.dust','dust','%s Dust'),
                  ('@prefix-template.item.gregtech.unit','unit','')]
            values,bindings,unsupported,_=material_prefix_templates(root,rows)
            self.assertEqual(values,{'@material-form.default.dust':'%s Dust'})
            self.assertEqual(bindings,{'item.gregtech.dust':'@material-form.default.dust'})
            self.assertEqual(unsupported,{'item.gregtech.unit':'unit'})
            for wrong in [[('@prefix-template.item.gregtech.dust','dust','Dust %s')],
                          [('@prefix-template.item.gregtech.unit','unit','%s')],
                          [('@empty-template.item.gregtech.dust_empty','dust','Empty')]]:
                with self.assertRaises(ValueError):material_prefix_templates(root,wrong)

    def test_braces_inside_java_strings_do_not_close_method(self):
        sample = '{ if (true) { return "} escaped \\\" {"; } } trailing'
        self.assertEqual(sample[block_end(sample, 0):], ' trailing')
        with self.assertRaises(ValueError):
            block_end('{return "}";', 0)

    def test_material_identity_uses_scopes_factories_and_aliases(self):
        symbols, groups = material_symbols('''
static OreDictMaterial gold() {return metal(790, "Gold", 0);}
Au = gold();
Alias = Au;
public static class WOODS {
Gold = wood(9500, "Golden Wood", 0);
Again = Gold;
}
Clay = clay(8215, "Clay", 0);
''')
        self.assertEqual(symbols['Alias'], (790, 'Gold'))
        self.assertEqual(symbols['WOODS.Again'], (9500, 'GoldenWood'))
        self.assertEqual(groups['clay'], {(8215, 'Clay')})

    def test_only_naming_tags_and_implicit_ore_tag_are_carried_over(self):
        helper = 'add(MATERIAL_BASED, UNIFICATABLE, ORE, TOOLTIP_ENCHANTS);'
        source = '''ore = create("ore", "Category", "Small ", " Ore").setOreStats(U).add(UNIFICATABLE);
dust = create("dust", "Dusts", "", " Dust").add(DUST_BASED, TD.Prefix.IS_CONTAINER, TOOLTIP_MATERIAL);'''
        self.assertEqual(prefix_rows(source, helper), {
            'ore': ('Small ', ' Ore', ['ORE']), 'dust': ('', ' Dust', ['DUST_BASED', 'IS_CONTAINER'])})
        with self.assertRaisesRegex(ValueError, 'setOreStats'):
            prefix_rows(source, '')
        with self.assertRaisesRegex(ValueError, 'display override'):
            prefix_rows(source + '\ndust.setLocalItemName("changed", "name");', helper)
        with self.assertRaisesRegex(ValueError, 'Duplicate'):
            prefix_rows(source + '\n' + source, helper)


if __name__ == '__main__':
    unittest.main()
