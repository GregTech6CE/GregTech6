import unittest,tempfile,json
from pathlib import Path
import xml.etree.ElementTree as ET
from tools.integration.check_nonore_cohort import check,CASES,NEG,NS
class CohortChecker(unittest.TestCase):
 def fixture(self,root,phase):
  xml,log,exitf=root/'x.xml',root/'l',root/'e';suite=ET.Element('testsuite')
  for name in sorted(CASES):
   c=ET.SubElement(suite,'testcase',name=name,classname=NS+':test_empty')
   if phase=='baseline' and name==NEG:ET.SubElement(c,'failure',message='bounded source-owned input failures=728: sample')
  ET.ElementTree(suite).write(xml);exitf.write_text('1' if phase=='baseline' else '0')
  rows=json.loads((Path(__file__).resolve().parents[2]/'docs/integration/nonore-input-source-profile.json').read_text())['rows'];lines=[]
  six=['crushed','crushedTiny','crushedPurified','crushedPurifiedTiny','crushedCentrifuged','crushedCentrifugedTiny']
  for r in rows:
   if r['source_ores']!='NO':continue
   name=r['name'];reasons='[requested:crushed]' if name=='Bakelite' else '[]'
   lines.append(f"[nonore-cohort-classification] {r['id']}/{name} source=NO overrides={reasons}")
   roles=[(p,'gt.recipe.'+m) for p in six for m in ['crusher','shredder']]+[('crushed','gt.recipe.bath'),('crushedPurified','gt.recipe.centrifuge')]
   for p,m in roles:lines.append(f'[nonore-cohort-row] {name}/{p} map={m} accepted={1 if phase=="baseline" else 0} selected={"true" if phase=="baseline" else "false"}')
  lines.append('[nonore-dust-snapshot] ["dustx1 work16"]');log.write_text('\n'.join(lines));return xml,exitf,log
 def test_exact_baseline_and_fixed(self):
  with tempfile.TemporaryDirectory() as td:
   for phase in ['baseline','fixed']:
    a=self.fixture(Path(td),phase);self.assertEqual(check(*a,phase)['observed_roles'],742)
 def test_fixture_or_other_failure_not_expected_red(self):
  with tempfile.TemporaryDirectory() as td:
   a=self.fixture(Path(td),'baseline');s=a[0].read_text();a[0].write_text(s.replace('bounded source-owned input failures=728: sample','missing fixture'))
   with self.assertRaises(ValueError):check(*a,'baseline')
 def test_log_census_mutations_rejected(self):
  with tempfile.TemporaryDirectory() as td:
   a=self.fixture(Path(td),'fixed');s=a[2].read_text()
   for text in [s.replace('[requested:crushed]','[]'),s.replace('gt.recipe.bath','gt.recipe.hammer',1),s+'\n[nonore-cohort-row] duplicate',s.replace('[nonore-dust-snapshot]','[wrong]'),s.replace('source=NO','source=UNKNOWN',1),s.replace('accepted=0 selected=false','accepted=1 selected=true',1)]:
    a[2].write_text(text)
    with self.subTest(text=text[-70:]):
     with self.assertRaises(ValueError):check(*a,'fixed')
 def test_exit_and_namespace_rejected(self):
  with tempfile.TemporaryDirectory() as td:
   a=self.fixture(Path(td),'fixed');a[1].write_text('130')
   with self.assertRaises(ValueError):check(*a,'fixed')
   a=self.fixture(Path(td),'fixed');a[0].write_text(a[0].read_text().replace(NS,'wrong'))
   with self.assertRaises(ValueError):check(*a,'fixed')
if __name__=='__main__':unittest.main()
