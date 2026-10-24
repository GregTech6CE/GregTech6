import tempfile,unittest
from pathlib import Path
from tools.integration import migrate_slurry_recycling as m

class SlurryMigration(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.root=Path(self.tmp.name)
  self.ores=self.root/'Ores.java';self.constants=self.root/'CS.java';self.fluids=self.root/'FL.java'
  self.ores.write_text(m.SOURCE);self.constants.write_text('public static final long U=648648000,U9=U/9;')
  self.fluids.write_text('public static FluidStack[] waters(long aWater) {return waters(aWater,aWater,aWater);}\n'+m.FL_SOURCE)
  self.other='        r("Other", 7, 9, null, sp("omd:Other:111"), sp("w:100"), sp("f:Other:100"), NONE);\n'
  self.legacy='/* 3 of 3 RM calls translated into 4 recipes; */\n'+'\n'.join(m.OLD)+'\n'+self.other
 def migrate(self,text=None):return m.migrate_ores(self.legacy if text is None else text,self.ores,self.constants,self.fluids)
 def test_exact_nine_rows_and_other_tokens_unchanged(self):
  value=self.migrate();self.assertIn(self.other,value);self.assertIn('into 10 recipes;',value)
  self.assertEqual(value.count('omdu:SluiceSand:72072000'),5)
  for line in m.NEW:self.assertEqual(value.splitlines().count(line),1)
  self.assertEqual(value.splitlines().count(m.OLD[1]),1)
  self.assertNotIn(m.OLD[0],value);self.assertNotIn(m.OLD[2],value)
 def test_idempotent_and_exact_four_water_order(self):
  value=self.migrate();self.assertEqual(self.migrate(value),value)
  self.assertEqual(m.WATERS,('Water','MnWtr','DistW','SpDew'));self.assertEqual(len(m.NEW),9)
  self.assertEqual([line for line in value.splitlines() if 'r("Mixer"' in line],list(m.NEW[:8]))
 def test_source_rule_change_rejected(self):
  for old,new in [('FL.Sluice.make(100)','FL.Sluice.make(99)'),('U9','U8'),('FL.waters(1000)','FL.waters(900)'),('16,16','16,17')]:
   self.ores.write_text(m.SOURCE.replace(old,new))
   with self.subTest(old=old),self.assertRaises(ValueError):self.migrate()
 def test_source_unit_and_water_identity_changes_rejected(self):
  self.constants.write_text('public static final long U=648648001,U9=U/9;')
  with self.assertRaises(ValueError):self.migrate()
  self.constants.write_text('public static final long U=648648000,U9=U/9;')
  self.fluids.write_text(self.fluids.read_text().replace('FL.MnWtr.make','FL.Water.make'))
  with self.assertRaises(ValueError):self.migrate()
 def test_partial_duplicate_or_missing_migration_rejected(self):
  for value in [self.legacy.replace(m.OLD[0],m.NEW[0]),self.legacy+'\n'+m.OLD[1]+'\n',self.legacy.replace(m.OLD[2],'')]:
   with self.subTest(value=value),self.assertRaises(ValueError):self.migrate(value)
 def test_comments_and_whitespace_do_not_change_source_meaning(self):
  self.ores.write_text('// source note\n'+m.SOURCE.replace(',', ' , ').replace('RM.Mixer','/* identity */ RM.Mixer'))
  self.assertEqual(self.migrate().count('omdu:SluiceSand:72072000'),5)
 def test_checked_in_catalog_and_regeneration_hook(self):
  root=Path(__file__).resolve().parents[2]
  value=(root/'core/src/main/java/com/gregtech/gregtech/content/recipe/generated/GTOresGen.java').read_text()
  for line in m.NEW:self.assertEqual(value.splitlines().count(line),1)
  self.assertNotIn(m.OLD[0],value);self.assertNotIn(m.OLD[2],value)
  producer=(root/'tools/transpile_gt6_chem.py').read_text()
  self.assertIn('if SET == "ores":\n        from integration.migrate_slurry_recycling import migrate_ores',producer)
if __name__=='__main__':unittest.main()
