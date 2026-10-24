import tempfile,unittest
from pathlib import Path
import xml.etree.ElementTree as ET
from tools.integration.check_nonore_sifting import check,NEG,POS,NS
class Checker(unittest.TestCase):
 def fixture(self,d,phase):
  xml=d/'x';log=d/'l';ex=d/'e';root=ET.Element('testsuite')
  for name in NEG|POS:
   c=ET.SubElement(root,'testcase',name=name,classname=NS+':test_empty')
   if phase=='baseline' and name in NEG:ET.SubElement(c,'failure',message='source-NO fabricated Sifting inputs remain: source')
  ET.ElementTree(root).write(xml);ex.write_text('1' if phase=='baseline' else '0')
  log.write_text('\n'.join(f'[nonore-sifting-input] {i}/{n}/{p} accepted={1 if phase=="baseline" else 0} selected={str(phase=="baseline").lower()} warm={str(phase=="baseline").lower()}' for i,n in [(8002,'Flint'),(9802,'Ice'),(8214,'Obsidian')] for p in ['crushedPurified','crushedPurifiedTiny']))
  return xml,log,ex,phase
 def test_phase_and_reject_fixture_errors(self):
  with tempfile.TemporaryDirectory() as td:
   for phase in ['baseline','fixed']:
    args=self.fixture(Path(td),phase);self.assertEqual(check(*args)['cases'],5)
   args=self.fixture(Path(td),'baseline');args[0].write_text(args[0].read_text().replace('source-NO fabricated Sifting inputs remain:','fixture failure:'))
   with self.assertRaises(ValueError):check(*args)
 def test_fixed_log_contradiction_and_missing_roles(self):
  with tempfile.TemporaryDirectory() as td:
   args=self.fixture(Path(td),'fixed');text=args[1].read_text()
   for changed in [text.replace('accepted=0','accepted=1',1),text.replace('warm=false','warm=true',1),'\n'.join(text.splitlines()[:-1])]:
    args[1].write_text(changed)
    with self.assertRaises(ValueError):check(*args)
 def test_missing_case_and_unexpected_exit(self):
  with tempfile.TemporaryDirectory() as td:
   args=self.fixture(Path(td),'fixed');args[2].write_text('1')
   with self.assertRaises(ValueError):check(*args)
   args=self.fixture(Path(td),'fixed');root=ET.parse(args[0]).getroot();root.remove(root[0]);ET.ElementTree(root).write(args[0])
   with self.assertRaises(ValueError):check(*args)
