"""Check original tab constructors and loop-generated block names independently."""
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'integration'))
from import_identity_localization import original_english
from source_creative_language import creative_name_sources, unused_prefix_names
from source_language_common import counted_blocks


class CreativeLanguageTests(unittest.TestCase):
    def source(self, directory):
        root = Path(directory)
        java = root / 'src/main/java'
        sources = {
            'gregapi/data/MT.java': 'Steel = metal(42, "Source Steel", 0).setLocal("Actual Steel");',
            'gregapi/data/CS.java': 'VN={"LOW","HIGH"};',
            'gregapi/item/CreativeTab.java': 'LH.add("itemGroup." + aName, aLocal);',
            'gregapi/block/multitileentity/MultiTileEntityRegistry.java':
                'new CreativeTab(mNameInternal+"."+aClassContainer.mCreativeTabID, aCategoricalName, Item.getItemFromBlock(mBlock), aClassContainer.mCreativeTabID)',
            'gregapi/item/prefixitem/PrefixItem.java':
                'new CreativeTab(mPrefix.mNameInternal, mPrefix.mNameCategory, this, W)',
            'gregapi/oredict/OreDictPrefix.java': 'mNameCategory = aCategoryName;',
            'gregapi/data/OP.java': '''
private static OreDictPrefix create(String aName, String aCategory) {
 return OreDictPrefix.createPrefix(aName).setCategoryName(aCategory).setLocalPrefixName(aCategory);
}
ingot = create("ingot", "Ingots");''',
            'gregtech/items/MultiItemBooks.java': 'new CreativeTab(getUnlocalizedName(), "Original Books", this, (short)32000);',
            'gregtech/loaders/b/Loader_MultiTileEntities.java': '''
private static void metalset(Object arg) {}
private static void storages() {
 aRegistry.add("Turbine ("+MT.Steel   .getLocal()+")", "Original Turbines", 1512, 1515);
 for (int i = 0; i < 2; i++) {
  aRegistry.add("Battery ("+VN[i]+")", "Original Batteries", 10080+i, 10081);
  aRegistry.add("Literal i { } " + i, "Panels", i+32500, 32500);
  if (present) { use("a nested { brace }"); }
 }
}''',
        }
        for relative, text in sources.items():
            path = java / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding='utf-8')
        return root, java

    def temporary(self):
        work = Path(__file__).resolve().parents[2] / 'work'
        work.mkdir(exist_ok=True)
        return tempfile.TemporaryDirectory(dir=work)

    def test_original_category_titles_have_no_invented_prefix(self):
        with self.temporary() as directory:
            root, _ = self.source(directory)
            values, files = original_english(root)
            self.assertEqual(values['itemGroup.ingot'], 'Ingots')
            self.assertEqual(values['itemGroup.gt.multiitem.books'], 'Original Books')
            self.assertEqual(values['itemGroup.gt.multitileentity.10081'], 'Original Batteries')
            self.assertEqual(values['gt.multitileentity.1512'], 'Turbine (Actual Steel)')
            self.assertEqual(values['gt.multitileentity.10081'], 'Battery (HIGH)')
            self.assertEqual(values['gt.multitileentity.32501'], 'Literal i { } 1')
            self.assertEqual(len(set(creative_name_sources(root)) - set(files)), 0)

    def test_category_formula_change_and_missing_dependency_are_rejected(self):
        with self.temporary() as directory:
            root, java = self.source(directory)
            path = java / 'gregapi/item/CreativeTab.java'
            text = path.read_text(encoding='utf-8')
            path.write_text(text.replace('aLocal', '"Prefix " + aLocal'), encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'creative naming formula'):
                original_english(root)
            path.unlink()
            with self.assertRaisesRegex(ValueError, 'Missing original creative'):
                original_english(root)

    def test_counted_blocks_do_not_cut_at_nested_or_literal_braces(self):
        loops = list(counted_blocks('for (int i = 0; i < 3; i++) { if (x) { name("}"); } finish(); }'))
        self.assertEqual(loops, [(3, ' if (x) { name("}"); } finish(); ')])
        with self.assertRaisesRegex(ValueError, 'Unclosed'):
            list(counted_blocks('for (int i = 0; i < 3; i++) {'))

    def test_wire_tiers_follow_original_constructor_not_material_spelling(self):
        with self.temporary() as directory:
            root, java = self.source(directory)
            blocks = java / 'gregtech/loaders/a/Loader_Blocks.java'
            blocks.parent.mkdir(parents=True)
            blocks.write_text('new BlockLongDistWire("gt.wire", icons, new byte[] {' + ','.join(['0','1']*8) + '});', encoding='utf-8')
            helper = java / 'gregtech/blocks/tool/BlockLongDistWire.java'
            helper.parent.mkdir(parents=True)
            helper.write_text('''mTiers = aTiers;
for (byte i = 0; i < 16; i++) LH.add(aUnlocalised+"."+i, "Long Distance Electric Wire ("+VN[mTiers[i]]+")");''', encoding='utf-8')
            values, files = original_english(root)
            self.assertEqual(values['gt.wire.1'], 'Long Distance Electric Wire (HIGH)')
            self.assertEqual(values['gt.wire.14'], 'Long Distance Electric Wire (LOW)')
            self.assertIn(helper, files)
            helper.write_text(helper.read_text().replace('i < 16', 'i < 15'), encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'wire name formula'):
                original_english(root)

    def test_unused_prefix_display_does_not_take_category_title(self):
        with self.temporary() as directory:
            root, java = self.source(directory)
            helper=java/'gregapi/oredict/OreDictPrefix.java'
            helper.write_text('''String tName = aName.replaceAll(" ", "").replaceAll("-", "");
return rPrefix == null ? new OreDictPrefix(tName, aName) : rPrefix;
mNameCategory = mNameLocal = aNameLocal;
mNameLocal = aLocalName;''',encoding='utf-8')
            (java/'gregapi/GT_API_Proxy_Client.java').write_text(
                'LH.add("oredict.prefix." + tPrefix.mNameInternal, tPrefix.mNameLocal);',encoding='utf-8')
            op='''return OreDictPrefix.createPrefix(aName).add(PREFIX_UNUSED);
coin=unused("coin").setCategoryName("Coins");
other=unused("another-name").setLocalPrefixName("Different Display").setCategoryName("Other Category");'''
            values,files=unused_prefix_names(root,op)
            self.assertEqual(values,{'oredict.prefix.coin':'coin', 'oredict.prefix.anothername':'Different Display'})
            self.assertIn(helper,files)
            with self.assertRaisesRegex(ValueError,'unused prefix override'):
                unused_prefix_names(root,op.replace('"Different Display"','variable'))
            helper.write_text(helper.read_text().replace('mNameLocal = aNameLocal','mNameLocal = "Wrong"'),encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'unused prefix name'):
                unused_prefix_names(root,op)
