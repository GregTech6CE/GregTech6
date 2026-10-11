"""Original numbered names must not be confused with recipe stack factories."""
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'integration'))
from source_numbered_language import bumble_declarations, bumble_english
from import_identity_localization import original_english


SOURCE = '''
make(100, "Original Bumble", " Exact description! ");
make(200, "Other Bumble", "");
public void make(int aSpeciesID, String aName, String aTooltip) {
    addItem(aSpeciesID + 0, aName + " Drone", aTooltip, EXTRA);
    addItem(aSpeciesID + 9, aName + " (Dead & Scanned)", aTooltip, EXTRA);
}
'''


class NumberedLanguageTests(unittest.TestCase):
    def test_original_null_fluid_name_uses_material_display_and_color_hint_uses_index_constant(self):
        with self.temporary_root() as directory:
            root=Path(directory);java,data=self.make_source(root)
            (data/'MT.java').write_text('Foam=create(42,"Construction Foam").setLocal("C-Foam");',encoding='utf-8')
            cs=data/'CS.java';cs.write_text(cs.read_text()+' DYE_INDEX_Blue=4;',encoding='utf-8')
            helper=data/'FL.java'
            text='''public static Fluid create(String aName, IIconContainer aTexture, String aLocalized, Object rest) {
aName=aName.toLowerCase();
aLocalized = (aLocalized==null?aMaterial==null||aMaterial==MT.NULL?UT.Code.capitaliseWords(aName):aMaterial.getLocal():aLocalized);
LH.add(rFluid.getUnlocalizedName(),aLocalized);
}'''
            helper.write_text(text,encoding='utf-8')
            (java/'Loader_Fluids.java').write_text('FL.create("external.foam",null,MT.Foam,1,100,300);',encoding='utf-8')
            (java/'MultiItemBottles.java').write_text('addItem(32001,"Dye Bottle","Color: "+DYE_NAMES[DYE_INDEX_Blue ],OTHER);',encoding='utf-8')
            values,_=original_english(root)
            self.assertEqual(values['fluid.external.foam'],'C-Foam')
            self.assertEqual(values['gt.multiitem.bottles.32001.tooltip'],'Color: Dye 4')
            helper.write_text(text.replace('aMaterial.getLocal()','aMaterial.mNameInternal'),encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'null fluid display-name fallback'):
                original_english(root)

    def make_source(self, root):
        java = root / 'src/main/java'
        data = java / 'gregapi/data'
        data.mkdir(parents=True)
        (data / 'MT.java').write_text('', encoding='utf-8')
        (data / 'CS.java').write_text('DYE_NAMES={' + ','.join('"Dye '+str(i)+'"' for i in range(16))
                                     + '}; DYE_OREDICTS_POST={' + ','.join('"Key'+str(i)+'"' for i in range(16)) + '};', encoding='utf-8')
        return java, data

    def temporary_root(self):
        path = Path(__file__).resolve().parents[2] / 'work'
        path.mkdir(exist_ok=True)
        return tempfile.TemporaryDirectory(dir=path)

    def test_fluid_texture_overload_uses_third_argument_and_original_dye_key_array(self):
        with self.temporary_root() as directory:
            root = Path(directory)
            java, data = self.make_source(root)
            helper = data / 'FL.java'
            helper.write_text('''
public static Fluid create(String aName, IIconContainer aTexture, String aLocalized, Object other) {
 aName=aName.toLowerCase(); LH.add(rFluid.getUnlocalizedName(), aLocalized);
}''', encoding='utf-8')
            loader = java / 'Loader_Fluids.java'
            text = '''
FL.create("potion.example", tIconPotion, "Exact Brew", null, color, 1, 1000, 300, NI, NI, 0);
for (byte i = 0; i < 16; i++) {
 FL.create("dye.flower."+DYE_OREDICTS_POST[i].toLowerCase(), tIconDye, DYE_NAMES[i]+" Flower Dye", null, DYES[i], 1, 1000, 300, NI, NI, 0);
}
'''
            loader.write_text(text, encoding='utf-8')
            values, files = original_english(root)
            self.assertEqual(values['fluid.potion.example'], 'Exact Brew')
            self.assertEqual(values['fluid.dye.flower.key12'], 'Dye 12 Flower Dye')
            self.assertEqual(len(values), 17)
            self.assertIn(helper, files)
            loader.write_text(text.replace('i < 16', 'i < 8'), encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'Unsupported original colored fluid'):
                original_english(root)
            loader.write_text(text, encoding='utf-8')
            helper.write_text(helper.read_text().replace('IIconContainer aTexture', 'String aOther'), encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'Unsupported original fluid icon overload'):
                original_english(root)

    def test_book_names_have_no_color_prefix_but_canvas_names_do(self):
        with self.temporary_root() as directory:
            root = Path(directory)
            java, _ = self.make_source(root)
            book = java / 'MultiItemBooks.java'
            text = '''for (int i = 0; i < 11; i++) {
 register(addItem(i, "Book", "", OTHER));
 register(addItem(1000+i, "Large Book", "", OTHER));
}'''
            book.write_text(text, encoding='utf-8')
            canvas = java / 'MultiItemRandomTools.java'
            canvas.write_text('for (int i = 0; i < 16; i++) addItem(i+7030, DYE_NAMES[i]+" Canvas", "Exact hint", OTHER);', encoding='utf-8')
            values, files = original_english(root)
            self.assertEqual(values['gt.multiitem.books.10'], 'Book')
            self.assertEqual(values['gt.multiitem.books.1010'], 'Large Book')
            self.assertEqual(values['gt.multiitem.books.1000.tooltip'], '')
            self.assertEqual(values['gt.multiitem.randomtools.7042'], 'Dye 12 Canvas')
            self.assertEqual(values['gt.multiitem.randomtools.7042.tooltip'], 'Exact hint')
            self.assertIn(book, files)
            self.assertIn(canvas, files)
            book.write_text(text.replace('i < 11', 'i < 10'), encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'Unsupported original book color catalog'):
                original_english(root)

    def test_exact_suffixes_empty_tooltips_and_nonregistration_calls(self):
        extra = '''
// make(100, "Wrong Bumble", "wrong");
ST.make(this, 1, meta);
FL.Honeydew . make(10);
String example = "make(";
remake(100, "Not a species", "wrong");
'''
        values = bumble_english(*bumble_declarations(SOURCE + extra))
        self.assertEqual(len(values), 8)
        self.assertEqual(values['gt.multiitem.bumblebee.100'], 'Original Bumble Drone')
        self.assertEqual(values['gt.multiitem.bumblebee.109'], 'Original Bumble (Dead & Scanned)')
        self.assertEqual(values['gt.multiitem.bumblebee.109.tooltip'], ' Exact description! ')
        self.assertEqual(values['gt.multiitem.bumblebee.209.tooltip'], '')

    def test_duplicate_species_or_states_are_rejected_even_when_text_matches(self):
        for extra in ['make(100, "Original Bumble", " Exact description! ");',
                      'addItem(aSpeciesID+0, aName+" Drone", aTooltip);']:
            with self.subTest(extra=extra), self.assertRaisesRegex(ValueError, 'Duplicate original bumble'):
                bumble_declarations(SOURCE + extra)

    def test_changed_formula_or_empty_catalog_cannot_pass_by_omission(self):
        for text in ['', SOURCE.replace('aName + " Drone"', 'aName.toUpperCase() + " Drone"'),
                     SOURCE.replace('"Original Bumble"', 'displayName'),
                     SOURCE.replace('aSpeciesID + 9', 'aSpeciesID + offset')]:
            with self.subTest(text=text), self.assertRaises(ValueError):
                bumble_declarations(text)

    def test_overlapping_numbered_registrations_are_rejected(self):
        with self.assertRaisesRegex(ValueError, 'Overlapping original bumble registration'):
            bumble_english({0: ('First', ''), 10: ('Second', '')}, {0: ' Drone', 10: ' Queen'})

    def test_original_english_includes_numbered_source_file_and_complete_values(self):
        root_dir = Path(__file__).resolve().parents[2] / 'work'
        root_dir.mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir=root_dir) as directory:
            root = Path(directory)
            data = root / 'src/main/java/gregapi/data'
            data.mkdir(parents=True)
            (data / 'MT.java').write_text('', encoding='utf-8')
            bees = root / 'src/main/java/gregtech/items/MultiItemBumbles.java'
            bees.parent.mkdir(parents=True)
            bees.write_text(SOURCE, encoding='utf-8')
            values, files = original_english(root)
            self.assertIn(bees, files)
            self.assertEqual(values, bumble_english(*bumble_declarations(SOURCE)))
