"""Source identity parser must preserve English literals and refuse ambiguous IDs."""
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import copy

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'integration'))
from import_identity_localization import (original_english, pipe_identities, wire_identities, numbered_item_identities,
                                          material_identities, material_fluid_english, verified_material_proofs, resolve_material_collision,
                                          require_retained_english_checks, english_bindings)
from source_recipe_language import recipe_map_names


class IdentityLanguageTests(unittest.TestCase):
    def test_recipe_names_follow_inheritance_and_reject_changed_forwarding(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java/gregapi'
            (java/'data').mkdir(parents=True);(java/'recipes/maps').mkdir(parents=True)
            (java/'recipes/Recipe.java').write_text('''mNameInternal=aNameInternal;
mNameLocal=aNameLocal; LH.add(mNameInternal, mNameLocal);''',encoding='utf-8')
            (java/'data/RM.java').write_text('''new RecipeMap(null, "gt.recipe.direct", "Original");
// new RecipeMap(null, "gt.recipe.ignored", "Comment");
new RecipeMapChild(null, "gt.recipe.child", "Label");''',encoding='utf-8')
            parent=java/'recipes/maps/RecipeMapParent.java'
            parent.write_text('''class RecipeMapParent extends RecipeMap {
super(aRecipes, aUnlocalizedName, aNameLocal + " (suffix)");}''',encoding='utf-8')
            child=java/'recipes/maps/RecipeMapChild.java'
            child.write_text('''class RecipeMapChild extends RecipeMapParent {
super(aRecipes, aUnlocalizedName, aNameLocal);}''',encoding='utf-8')
            values,files=recipe_map_names(root)
            self.assertEqual(values,{'gt.recipe.direct':'Original','gt.recipe.child':'Label (suffix)'})
            self.assertEqual(len(files),4)
            for expression in ('aNameLocal.toUpperCase()', 'aUnlocalizedName'):
                parent.write_text('class RecipeMapParent extends RecipeMap {super(aRecipes, aUnlocalizedName, '+expression+');}',encoding='utf-8')
                with self.assertRaisesRegex(ValueError,'name formula'):
                    recipe_map_names(root)

    def test_direct_original_keys_are_checked_without_a_redundant_chinese_alias(self):
        english={'gt.lang.face.top':'Top', 'gt.untranslated':'Source text',
                 'block.native':'Modern', 'port.only':'Port label'}
        declarations={'gt.lang.face.top':'Top','gt.untranslated':'Source text','gt.block.42':'Original'}
        bindings,missing=english_bindings(english,declarations,{'gt.lang.face.top':'顶面','gt.block.42':'原块'},
                                         {'block.native':'gt.block.42'},{})
        self.assertEqual(bindings,{'gt.lang.face.top':'gt.lang.face.top','gt.untranslated':'gt.untranslated',
                                   'block.native':'gt.block.42'})
        self.assertEqual(missing,{'gt.untranslated':'gt.untranslated'})
        self.assertNotIn('port.only',bindings)

    def setUp(self):
        (Path(__file__).resolve().parents[2]/'work').mkdir(exist_ok=True)

    def test_unparsed_original_must_not_remove_an_existing_english_check(self):
        with self.assertRaisesRegex(ValueError,'previously verified.*item.active'):
            require_retained_english_checks({'item.active':{},'item.removed':{}},{},{'item.active':'Name'})
        require_retained_english_checks({'item.active':{},'item.removed':{}},
                                       {'item.active':{},'item.added':{}},{'item.active':'Name'})

    def test_generated_fluid_english_requires_identity_but_not_a_chinese_translation(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java';data=java/'gregapi/data';data.mkdir(parents=True)
            (data/'MT.java').write_text('Gas = create(42, "Example Gas").setLocal("Original Gas");',encoding='utf-8')
            material=java/'gregapi/oredict/OreDictMaterial.java';material.parent.mkdir(parents=True)
            material.write_text('',encoding='utf-8')
            loader=java/'gregtech/loaders/a/Loader_Fluids.java';loader.parent.mkdir(parents=True)
            loader.write_text('',encoding='utf-8')
            helper=data/'FL.java'
            helper.write_text('''aName=aName.toLowerCase(); LH.add(rFluid.getUnlocalizedName(),aLocalized);
return create(aMaterial.mNameInternal.toLowerCase(), aTexture, aMaterial.mNameLocal, aMaterial, 0);
return create("molten."+aMaterial.mNameInternal.toLowerCase(), aTexture, "Molten "+aMaterial.mNameLocal, aMaterial, 1);
return create("plasma."+aMaterial.mNameInternal.toLowerCase(), aTexture, aMaterial.mNameLocal+" Plasma", aMaterial, 3);''',encoding='utf-8')
            rows=[]
            for native,source,ident in [('gas','examplegas','42'),('molten','molten.examplegas','42'),
                    ('undocumented','plasma.examplegas','42'),('wrong','othergas','42'),
                    ('zero','zero','0'),('wrongphase','unknown.examplegas','42')]:
                rows.extend([('fluid_type.gregtech.'+native,'fluid.'+source,''),
                             ('@fluid-proof.fluid_type.gregtech.'+native,'ExampleGas',ident)])
            values,_=material_fluid_english(root,rows)
            self.assertEqual(values,{'fluid.examplegas':'Original Gas','fluid.molten.examplegas':'Molten Original Gas',
                                     'fluid.plasma.examplegas':'Original Gas Plasma'})
            data=material_identities(root);before=copy.deepcopy(data)
            with patch('import_identity_localization.material_identities',side_effect=AssertionError('Duplicate material parse')):
                self.assertEqual(material_fluid_english(root,rows,data)[0],values)
                self.assertEqual(original_english(root,data)[0]['gt.material.ExampleGas'],'Original Gas')
            self.assertEqual(data,before)
            helper.write_text(helper.read_text().replace('mNameInternal.toLowerCase()','mNameInternal.toUpperCase()'),encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'Unsupported original generated fluid name formula'):
                material_fluid_english(root,rows)

    def test_numeric_material_identity_preserves_case_and_local_overrides(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java';data=java/'gregapi/data'
            data.mkdir(parents=True)
            material=java/'gregapi/oredict/OreDictMaterial.java';material.parent.mkdir(parents=True)
            material.write_text('// Original sanitizer and getLocal are provenance inputs.',encoding='utf-8')
            (data/'MT.java').write_text('''
MoonRock = stone(8513, "Moon Stone", 0).setLocal("Moon");
Moonstone = gem(8452, "Moonstone", 0);
Placeholder = valgem(00000, "Moonstone", 0);
CrudeSteel = compound(8806, "Clay Compound", 0);
static OreDictMaterial gold() {return metal(790, "Gold", 0);}
static OreDictMaterial osmium() {return metal(760, "OsmiumElemental", 0);}
Os = osmium().qual(3, 16, 1280, 4).setLocal("Osmium");
Ma, Magic = Ma = create(4000, "Magic").setLocal("Magic");
RenamedWood = woodnormal(9300, "Cinnamonwood", "Cinnawood", 0);
ExplicitWood = woodnormal(9301, "Example Wood", "Factory name", 0).setLocal("Override");
Sanitized = metal(42, "a-b/c'd e", 0);
Dynamic = metal(43, "Dynamic", 0).setLocal(VN[1]);
Duplicate = metal(99, "First", 0);
Duplicate = metal(99, "Second", 0);
// Wrong = stone(8513, "Commented Out", 0);
''',encoding='utf-8')
            ids,english,files,ambiguous=material_identities(root)
            self.assertEqual(ids[8513],'gt.material.MoonStone')
            self.assertEqual(ids[8452],'gt.material.Moonstone')
            self.assertEqual(ids[8806],'gt.material.ClayCompound')
            self.assertEqual(ids[790],'gt.material.Gold')
            self.assertEqual(ids[4000],'gt.material.Magic')
            self.assertNotIn(0,ids)
            self.assertNotIn(99,ids)
            self.assertEqual(ambiguous,{'99':['gt.material.First','gt.material.Second']})
            self.assertEqual(ids[42],'gt.material.Abcde')
            self.assertEqual(english['gt.material.MoonStone'],'Moon')
            self.assertEqual(english['gt.material.OsmiumElemental'],'Osmium')
            self.assertEqual(english['gt.material.Cinnamonwood'],'Cinnawood')
            self.assertEqual(english['gt.material.ExampleWood'],'Override')
            self.assertNotIn('gt.material.Dynamic',english)
            self.assertIn(material,files)
            proofs=verified_material_proofs([
                ('@material-proof.material.gregtech.moonstone','gt.material.MoonStone','8513'),
                ('@material-proof.material.gregtech.wrong_case','gt.material.Moonstone','8513'),
                ('@material-proof.material.gregtech.zero','gt.material.Moonstone','0'),
                ('@material-proof.material.gregtech.ambiguous','gt.material.First','99'),
                ('@material-proof.material.gregtech.crudesteel','gt.material.ClayCompound','8806'),
                ('material.gregtech.gem','gt.material.Moonstone','8452'),
            ],ids)
            self.assertEqual(dict(proofs),{
                'material.gregtech.moonstone':{(8513,'gt.material.MoonStone')},
                'material.gregtech.crudesteel':{(8806,'gt.material.ClayCompound')},
            })

    def test_antimatter_ids_and_placeholder_names_keep_different_proof_rules(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java';data=java/'gregapi/data';data.mkdir(parents=True)
            material=java/'gregapi/oredict/OreDictMaterial.java';material.parent.mkdir(parents=True)
            material.write_text('',encoding='utf-8')
            (data/'MT.java').write_text('''Empty=create(0,"Empty");
NULL=create(-1,"NULL");
Placeholder=valgem(0,"Example Gem");
Clash=create(4020,"Ordinary Test");''',encoding='utf-8')
            anti=data/'AM.java'
            declaration='''static OreDictMaterial create(int aID,String aNameOreDict) {
 return OreDictMaterial.createMaterial(aID,aNameOreDict,aNameOreDict);
}
H,Hydrogen=H=create(4010,"Anti-Hydrogen");
e,Positron=e=create(4005,"Positron");
Clash=create(4020,"Antimatter Test");'''
            anti.write_text(declaration,encoding='utf-8')
            ids,names,files,ambiguous=material_identities(root)
            self.assertEqual(ids[4010],'gt.material.AntiHydrogen')
            self.assertEqual(ids[4005],'gt.material.Positron')
            self.assertEqual(names['gt.material.AntiHydrogen'],'Anti-Hydrogen')
            self.assertEqual(names['gt.material.ExampleGem'],'Example Gem')
            self.assertEqual(names['gt.material.NULL'],'NULL')
            self.assertNotIn(0,ids);self.assertNotIn(-1,ids);self.assertNotIn(4020,ids)
            self.assertEqual(ambiguous['4020'],['gt.material.AntimatterTest','gt.material.OrdinaryTest'])
            self.assertIn(anti,files)
            self.assertEqual(dict(verified_material_proofs([
                ('@material-proof.material.gregtech.placeholder','gt.material.ExampleGem','0')],ids)),{})
            anti.write_text(declaration.replace('aID,aNameOreDict,aNameOreDict','aID,aNameOreDict,"Changed"'),encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'antimatter display-name factory'):
                material_identities(root)

    def test_actual_material_key_takes_precedence_over_another_categories_field_alias(self):
        candidates={'gt.material.Gold','gt.material.Goldwood'}
        proofs={'material.gregtech.gold':{(790,'gt.material.Gold'),(9369,'gt.material.Goldwood')}}
        self.assertEqual(resolve_material_collision('material.gregtech.gold',candidates,proofs),{'gt.material.Gold'})
        self.assertEqual(resolve_material_collision('material.gregtech.gold',candidates,{}),candidates)
        candidates={'gt.material.Cobalt','gt.material.CarbonMonoxide'}
        proofs={'material.gregtech.co':{(270,'gt.material.Cobalt'),(9838,'gt.material.CarbonMonoxide')}}
        # Neither abbreviation is an actual registered material translation key.
        self.assertEqual(resolve_material_collision('material.gregtech.co',candidates,proofs),candidates)

    def test_lazy_unused_names_do_not_replace_active_names_or_prove_ids(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java';data=java/'gregapi/data';data.mkdir(parents=True)
            material=java/'gregapi/oredict/OreDictMaterial.java';material.parent.mkdir(parents=True)
            material.write_text('',encoding='utf-8')
            (data/'MT.java').write_text('''static OreDictMaterial unused(String aNameOreDict) {return create(-1, aNameOreDict);}
Active=metal(42,"Same Name").setLocal("Active display");
Lazy=unused("Same Name");
Unnumbered=unused("Compatibility Gem").setLocal("Original Display");
// Disabled=valgem(0,"Compatibility Gem").setLocal("Wrong old display");''',encoding='utf-8')
            values,_=original_english(root)
            self.assertEqual(values['gt.material.SameName'],'Active display')
            self.assertEqual(values['gt.material.CompatibilityGem'],'Original Display')
            self.assertNotIn('gt.material.CompatibilityGem',material_identities(root)[0].values())

    def test_original_literals_material_names_and_numeric_offsets(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder); java=root/'src/main/java'
            (java/'gregapi/data').mkdir(parents=True)
            (java/'gregapi/data/MT.java').write_text('Steel = metal(26, "Source Steel", 0);',encoding='utf-8')
            (java/'Loader_MultiTileEntities.java').write_text('''
private static void metalset(Object aRegistry, Object aMat, int aID) {
 aRegistry.add("Bookshelf ("+aMat.getLocal()+")", "Storage", 7100+aID, 0);
}
private static void storages() {
 metalset(aRegistry, aMetal, aUtilMetal, aMachine, aWooden, MT.Steel, 10, 1);
 aMat = MT.Steel; aRegistry.add("Machine ("+aMat.getLocal()+")", "Machines", 20000, 0);
 // aRegistry.add("wrong", "Machines", 20000, 0);
 aRegistry.add("Unsupported "+VN[1], "Machines", 20001, 0);
 LH.add("gt.tooltip.sample", " Exact source! ");
}
''',encoding='utf-8')
            result,_=original_english(root)
            self.assertEqual(result['gt.multitileentity.7110'],'Bookshelf (Source Steel)')
            self.assertEqual(result['gt.multitileentity.20000'],'Machine (Source Steel)')
            self.assertEqual(result['gt.tooltip.sample'],' Exact source! ')
            self.assertNotIn('gt.multitileentity.20001',result)

    def test_conflicting_declarations_are_not_silently_overwritten(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java'
            (java/'gregapi/data').mkdir(parents=True)
            (java/'gregapi/data/MT.java').write_text('',encoding='utf-8')
            (java/'MultiItemBooks.java').write_text('''
addItem(1, "Book", "");
addItem(2, "First", "Original description");
addItem(2, "Conflicting", "Original description");
''',encoding='utf-8')
            result,_=original_english(root)
            self.assertEqual(result['gt.multiitem.books.1'],'Book')
            self.assertEqual(result['gt.multiitem.books.1.tooltip'],'')
            self.assertNotIn('gt.multiitem.books.2',result)
            self.assertEqual(result['gt.multiitem.books.2.tooltip'],'Original description')

    def test_original_local_overrides_voltage_and_any_material_are_distinct(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java';data=java/'gregapi/data'
            data.mkdir(parents=True)
            (data/'MT.java').write_text('''
static OreDictMaterial iron() {return metal(260, "Iron", 0);}
Fe = iron();
Galvanized = metal(1, "SteelGalvanized", 0).setLocal("Galvanized Steel");
Kinetic_T = {ANY.Steel, Fe};
''',encoding='utf-8')
            (data/'ANY.java').write_text('''
Steel = any("Any Iron-Steel");
Coal = any("Any Coal/Carbon");
Steel.steal(MT.Steel).setLocal ("Steel");
Coal.steal(MT.C).setLocal("Carbon");
// Steel.setLocal("Wrong");
''',encoding='utf-8')
            (data/'CS.java').write_text('String[] VN = {"ULV", "LV"};',encoding='utf-8')
            (java/'Loader_MultiTileEntities.java').write_text('''
private static void metalset() {}
private static void storages() {
aMat = MT.Fe; aRegistry.add("Machine ("+aMat.getLocal()+")", "Machines", 1, 0);
aMat = MT.Galvanized; aRegistry.add("Machine ("+aMat.getLocal()+")", "Machines", 2, 0);
aMat = MT.DATA.Kinetic_T[0]; aRegistry.add("Hammer ("+aMat.getLocal()+")", "Machines", 3, 0);
aRegistry.add("Igniter ("+VN[1]+")", "Machines", 4, 0);
aRegistry.add("Unsupported ("+VN[9]+")", "Machines", 5, 0);
aMat = ANY.Steel; aRegistry.add("Any machine ("+aMat.getLocal()+")", "Machines", 6, 0);
aMat = MT.Galvanized; aRegistry.add(aMat.mNameLocal+" Anvil", "Misc Tool Blocks", 7, 0);
}''',encoding='utf-8')
            result,_=original_english(root)
            self.assertEqual(result['gt.multitileentity.1'],'Machine (Iron)')
            self.assertEqual(result['gt.multitileentity.2'],'Machine (Galvanized Steel)')
            self.assertEqual(result['gt.multitileentity.3'],'Hammer (Steel)')
            self.assertEqual(result['gt.multitileentity.4'],'Igniter (LV)')
            self.assertNotIn('gt.multitileentity.5',result)
            self.assertEqual(result['gt.multitileentity.6'],'Any machine (Steel)')
            self.assertEqual(result['gt.multitileentity.7'],'Galvanized Steel Anvil')
            self.assertEqual(result['gt.material.AnyIronSteel'],'Steel')
            self.assertEqual(result['gt.material.AnyCoalCarbon'],'Carbon')
            (data/'ANY.java').write_text('Steel = any("Any Iron-Steel");\nSteel.setLocal(VN[1]);',encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'Unsupported original ANY display name'):
                original_english(root)

    def test_flower_metadata_comes_from_registered_class_and_not_comments(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java';data=java/'gregapi/data'
            data.mkdir(parents=True)
            (data/'MT.java').write_text('',encoding='utf-8')
            loader=java/'gregtech/loaders/a/Loader_Blocks.java'
            loader.parent.mkdir(parents=True)
            loader.write_text('new BlockFlowersA("gt.block.flower.a");',encoding='utf-8')
            (java/'BlockFlowersA.java').write_text('''
public BlockFlowersA(String id) {
  LH.add(getUnlocalizedName()+".0", "Original Flower");
  // LH.add(getUnlocalizedName()+".0", "Wrong Flower");
}''',encoding='utf-8')
            result,_=original_english(root)
            self.assertEqual(result['gt.block.flower.a.0'],'Original Flower')

    def test_shared_lh_constant_declarations_keep_exact_source_phrase(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);data=root/'src/main/java/gregapi/data'
            data.mkdir(parents=True)
            (data/'MT.java').write_text('',encoding='utf-8')
            (data/'LH.java').write_text('''
TOOL_HINT="gt.lang.tool.hint";
add(TOOL_HINT, " Exact source hint ");
// add(TOOL_HINT, "Wrong");
''',encoding='utf-8')
            result,_=original_english(root)
            self.assertEqual(result['gt.lang.tool.hint'],' Exact source hint ')

    def test_colored_constructor_inherits_exact_full_and_slab_names(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java';data=java/'gregapi/data'
            data.mkdir(parents=True)
            (data/'MT.java').write_text('',encoding='utf-8')
            (data/'CS.java').write_text('DYE_NAMES={'+','.join('"Dye '+str(i)+'"' for i in range(16))+'};',encoding='utf-8')
            loader=java/'gregtech/loaders/a/Loader_Blocks.java';loader.parent.mkdir(parents=True)
            loader.write_text('new BlockConcrete("gt.block.concrete");',encoding='utf-8')
            (java/'BlockConcrete.java').write_text('''class BlockConcrete extends BlockColored {
public BlockConcrete(String id) {super(Item.class, Material.rock, sound, id, "Concrete", null);}
}''',encoding='utf-8')
            shared=java/'gregapi/block/metatype/BlockColored.java';shared.parent.mkdir(parents=True)
            formula='''for (int i = 0; i < 16; i++) LH.add(getUnlocalizedName()+"."+i, DYE_NAMES[i] + " " + aDefaultLocalised);
for (int i = 0; i < 16; i++) LH.add(getUnlocalizedName()+"."+i, DYE_NAMES[i] + " " + aDefaultLocalised + " Slab");'''
            shared.write_text(formula,encoding='utf-8')
            result,files=original_english(root)
            self.assertEqual(result['gt.block.concrete.15'],'Dye 15 Concrete')
            self.assertEqual(result['gt.block.concrete.slab.5.0'],'Dye 0 Concrete Slab')
            self.assertIn(shared,files)
            # A new separator changes the source text: reject, never silently normalize it.
            shared.write_text(formula.replace('" "','"  "'),encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'Unsupported original BlockColored name formula'):
                original_english(root)
            shared.write_text(formula.replace('i < 16','i < 8'),encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'Unsupported original BlockColored name formula'):
                original_english(root)

    def test_wire_helpers_preserve_sparse_offsets_and_cable_availability(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java';helpers=java/'gregapi/tileentity/connectors'
            loader=java/'gregtech/loaders/b/Loader_MultiTileEntities.java'
            helpers.mkdir(parents=True);loader.parent.mkdir(parents=True)
            loader.write_text('''
MultiTileEntityWireElectric.addElectricWires(28350,0,256,1,2,1,T,F,T,registry,block,cls,MT.Cu);
MultiTileEntityWireElectric.addElectricWires(29800,0,65536,1,2,2,F,F,F,registry,block,cls,MT.Graphene);
private static void metalset() {} private static void storages() {}
''',encoding='utf-8')
            def row(kind,size,offset):
                return f'OreDictManager.INSTANCE.setTarget_(OP.{kind}Gt{size:02}, aMat, aRegistry.add("{size}x " + aMat.getLocal() + " {kind.title()}", "Wires", aID+{offset}, 0));'
            source='\n'.join(row('wire',i,i-1) for i in range(1,17))+'\nif (aCable) {\n'+'\n'.join(row('cable',i,i+15) for i in (1,2,4,8,12))+'\n}'
            helper=helpers/'MultiTileEntityWireElectric.java';helper.write_text(source,encoding='utf-8')
            result,_=wire_identities(root,{'MT.Cu':'Copper','MT.Graphene':'Graphene'})
            self.assertEqual(len(result),37)
            self.assertEqual(result['@wire.Copper.cable.12'],'gt.multitileentity.28377')
            self.assertEqual(result['@wire.Graphene.wire.16'],'gt.multitileentity.29815')
            self.assertNotIn('@wire.Graphene.cable.1',result)
            mt=java/'gregapi/data/MT.java';mt.parent.mkdir(parents=True)
            mt.write_text('Cu = create(1,"Copper"); Graphene = tier("Graphene");\n'
                          'static OreDictMaterial tier(String aNameOreDict) {return create(-1, aNameOreDict);}',encoding='utf-8')
            english,_=original_english(root)
            self.assertEqual(english['gt.multitileentity.28377'],'12x Copper Cable')
            self.assertEqual(english['gt.multitileentity.29815'],'16x Graphene Wire')
            self.assertNotIn('gt.multitileentity.29816',english)
            helper.write_text(source.replace(row('wire',16,15),''),encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'Incomplete original electric wire helper'):
                wire_identities(root,{'MT.Cu':'Copper'})
            helper.write_text(source.replace('if (aCable)','if (aWrongCondition)'),encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'Unsupported original electric cable guard'):
                wire_identities(root,{'MT.Cu':'Copper'})

    def test_pipe_helpers_resolve_numeric_offsets_and_refuse_incomplete_tables(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java';helpers=java/'gregapi/tileentity/connectors'
            loader=java/'gregtech/loaders/b/Loader_MultiTileEntities.java'
            helpers.mkdir(parents=True);loader.parent.mkdir(parents=True)
            loader.write_text('''
MultiTileEntityPipeItem.addItemPipes(25000, 0, MT.Brass);
MultiTileEntityPipeFluid.addFluidPipes(26000, 0, MT.Wood);
// MultiTileEntityPipeItem.addItemPipes(99999, 0, MT.Brass);
''',encoding='utf-8')
            for helper,sizes,start in [('Item',['Medium','Large','Huge','RestrictiveMedium','RestrictiveLarge','RestrictiveHuge'],2),
                                       ('Fluid',['Tiny','Small','Medium','Large','Huge','Quadruple','Nonuple'],0)]:
                (helpers/f'MultiTileEntityPipe{helper}.java').write_text('\n'.join(
                    f'OreDictManager.INSTANCE.setTarget_(OP.pipe{size}, aMat, aRegistry.add("Name", "Pipes", aID+{i+start}, 0));'
                    for i,size in enumerate(sizes)),encoding='utf-8')
            result,_=pipe_identities(root,{'MT.Brass':'Brass','MT.Wood':'Wood'})
            self.assertEqual(len(result),13)
            self.assertEqual(result['@pipe.item.Brass.RESTRICTIVE_HUGE'],'gt.multitileentity.25007')
            self.assertEqual(result['@pipe.fluid.Wood.NONUPLE'],'gt.multitileentity.26006')
            (helpers/'MultiTileEntityPipeFluid.java').write_text('',encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'Incomplete original pipe helper'):
                pipe_identities(root,{'MT.Brass':'Brass','MT.Wood':'Wood'})

    def test_stone_full_and_oriented_slab_formulas_use_material_display_name(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java';data=java/'gregapi/data'
            data.mkdir(parents=True)
            (data/'MT.java').write_text('Rock = stone(10, "Rock", 0).setLocal("Layer Rock");',encoding='utf-8')
            loader=java/'gregtech/loaders/a/Loader_Rocks.java';loader.parent.mkdir(parents=True)
            loader.write_text('new BlockStonesGT("gt.stone.rock", MT.STONES.Rock, 1, 1, 0, false);',encoding='utf-8')
            wrapper=java/'gregtech/blocks/stone/BlockStonesGT.java';wrapper.parent.mkdir(parents=True)
            wrapper.write_text('super(null, null, null, aName, aMaterial.getLocal(), aMaterial);',encoding='utf-8')
            parent=java/'gregapi/block/metatype';parent.mkdir(parents=True)
            (parent/'BlockMetaType.java').write_text('super(Item.class, aName+".slab."+aSlabType);',encoding='utf-8')
            parts=[]
            for kind in ('public','protected'):
                parts.append(kind+' BlockStones(String aName) {')
                for i in range(16):
                    suffix=' Slab' if kind=='protected' else ''
                    expression='"Chiseled "+aDefaultLocalised+"'+suffix+'"' if i==6 else 'aDefaultLocalised+" form '+str(i)+suffix+'"'
                    parts.append('LH.add(getUnlocalizedName()+".'+str(i)+'", '+expression+');')
                parts.append('}')
            file=parent/'BlockStones.java';file.write_text('\n'.join(parts),encoding='utf-8')
            result,files=original_english(root)
            self.assertEqual(result['gt.stone.rock.6'],'Chiseled Layer Rock')
            self.assertEqual(result['gt.stone.rock.slab.0.6'],'Chiseled Layer Rock Slab')
            self.assertEqual(result['gt.stone.rock.slab.5.15'],'Layer Rock form 15 Slab')
            self.assertNotIn('gt.stone.rock.slab.6.0',result)
            self.assertEqual(sum(k.startswith('gt.stone.rock.') for k in result),16*7)
            self.assertIn(file,files)
            # Missing metadata or a new unknown expression must not silently pin partial/wrong names.
            file.write_text('\n'.join(p for p in parts if '".15"' not in p),encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'Incomplete original 16-variant stone names'):
                original_english(root)
            file.write_text('\n'.join(parts).replace('"Chiseled "+aDefaultLocalised','unknown(aDefaultLocalised)'),encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'unsupported original stone display formula'):
                original_english(root)

    def test_original_component_loop_stops_before_port_only_tier(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java';data=java/'gregapi/data'
            data.mkdir(parents=True)
            (data/'MT.java').write_text('',encoding='utf-8')
            (data/'CS.java').write_text('String[] VN={"ULV","LV","MV","HV","EV","IV","LuV","ZPM","UV","PUV1"};',encoding='utf-8')
            (java/'MultiItemTechnological.java').write_text('''
for (int i = 0; i < 10; i++) {
IL.MOTORS[i].set(addItem(12000+i, "Compact Electric Motor ("+VN[i]+")", ""));
}
''',encoding='utf-8')
            result,_=original_english(root)
            self.assertEqual(result['gt.multiitem.technological.12006'],'Compact Electric Motor (LuV)')
            self.assertEqual(result['gt.multiitem.technological.12009.tooltip'],'')
            self.assertNotIn('gt.multiitem.technological.12010',result)

    def test_il_shape_identity_preserves_native_word_order(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);items=root/'src/main/java/gregtech/items';items.mkdir(parents=True)
            (items/'MultiItemTechnological.java').write_text('''
IL.Shape_Extruder_Pipe_Tiny.set(addItem(10009,"Tiny shape","Description"));
IL.Shape_SimpleEx_CCC.set(addItem(10228,"Capsule shape","Description"));
IL.USB_Stick_1.set(addItem(32001,"USB stick","Stores Data"));
IL.USB_HDD_4.set(addItem(32024,"USB drive","Stores files"));
IL.Circuit_Crystal_Ruby.set(addItem(30402,"Ruby circuit","Control Ruby"));
IL.Processor_Crystal_Diamond.set(addItem(30501,"Diamond processor","Logic"));
IL.Shape_Foodmold_Baguette.set(addItem(10803,"Food grade mold",""));
IL.Shape_Slicer_Eigths_Hollow.set(addItem(10904,"Hollow eighths",""));
// IL.Shape_Extruder_Pipe_Tiny.set(addItem(99999,"Wrong",""));
''',encoding='utf-8')
            result,_=numbered_item_identities(root)
            self.assertEqual(result['item.gregtech.extruder_shape_tinypipe'],'gt.multiitem.technological.10009')
            self.assertEqual(result['item.gregtech.low_heat_extruder_shape_capsulecellcontainer'],'gt.multiitem.technological.10228')
            for native,ident in [('usb1_stick',32001),('usb4_hdd',32024),('crystal_circuit_ruby',30402),
                    ('crystal_processor_diamond',30501),('foodmold_shape_baguette',10803),('slicer_shape_eights_hollow',10904)]:
                self.assertEqual(result['item.gregtech.'+native],'gt.multiitem.technological.'+str(ident))

    def test_paired_spray_metadata_and_selector_constructor_names(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java';data=java/'gregapi/data';data.mkdir(parents=True)
            (data/'MT.java').write_text('',encoding='utf-8')
            (data/'CS.java').write_text('String[] DYE_NAMES={'+','.join('"Dye '+str(i)+'"' for i in range(16))+'};',encoding='utf-8')
            spray=java/'MultiItemRandomTools.java'
            spray.write_text('''for (byte i = 0; i < 16; i++) {
IL.SPRAY_CAN_DYES[i].set(addItem(1000+2*i, "Spray Paint ("+DYE_NAMES[i]+")", "Full"));
IL.SPRAY_CAN_DYES_USED[i].set(addItem(mLastID+1, "Spray Paint ("+DYE_NAMES[i]+")", "Used"));
}''',encoding='utf-8')
            (java/'ItemIntegratedCircuit.java').write_text('''
super(MD.GAPI.mID, "gt.integrated_circuit", "Selector Tag", "");
LH.add(mName + ".configuration", "Configuration: ");
''',encoding='utf-8')
            result,_=original_english(root)
            self.assertEqual(result['gt.multiitem.randomtools.1000'],'Spray Paint (Dye 0)')
            self.assertEqual(result['gt.multiitem.randomtools.1031'],'Spray Paint (Dye 15)')
            self.assertEqual(result['gt.multiitem.randomtools.1030.tooltip'],'Full')
            self.assertEqual(result['gt.multiitem.randomtools.1031.tooltip'],'Used')
            self.assertEqual(result['gt.integrated_circuit'],'Selector Tag')
            self.assertEqual(result['gt.integrated_circuit.configuration'],'Configuration: ')
            spray.write_text(spray.read_text().replace('mLastID+1','mLastID+2'),encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'Unsupported original full/used spray'):
                original_english(root)

    def test_literal_fluid_names_preserve_spaces_case_and_ignore_unresolved_expressions(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]/'work') as folder:
            root=Path(folder);java=root/'src/main/java';data=java/'gregapi/data';data.mkdir(parents=True)
            (data/'MT.java').write_text('',encoding='utf-8')
            (data/'LH.java').write_text('add("loot.gt.books", "+Random Books+");\n'
                                        'String LABEL="gt.lang.label"; add(LABEL,"Label: ");',encoding='utf-8')
            helper=data/'FL.java';helper.write_text('aName = aName.toLowerCase(); LH.add(rFluid.getUnlocalizedName(), aLocalized);',encoding='utf-8')
            (java/'Loader_Fluids.java').write_text('''
FL.create("Molten HSLA", "Molten HSLA Steel", MT.HSLA, 1, 144, 1873);
FL.create("aerotheum", "Zephyrean Aerotheum", null, 1, 1000, 300);
FL.create(variable, "Not a literal identity", null, 1);
FL.create("unresolved", prefix + " Fluid", null, 1);
// FL.create("aerotheum", "Comment cannot override", null, 1);
''',encoding='utf-8')
            names,_=original_english(root)
            self.assertEqual(names['fluid.molten hsla'],'Molten HSLA Steel')
            self.assertEqual(names['fluid.aerotheum'],'Zephyrean Aerotheum')
            self.assertEqual(names['loot.gt.books'],'+Random Books+')
            self.assertEqual(names['gt.lang.label'],'Label: ')
            self.assertNotIn('fluid.unresolved',names)
            helper.write_text('aName = aName.toUpperCase(); LH.add(rFluid.getUnlocalizedName(), aLocalized);',encoding='utf-8')
            with self.assertRaisesRegex(ValueError,'Unsupported original fluid language registration'):
                original_english(root)
