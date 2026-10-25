import importlib.util,json,tempfile,unittest
from pathlib import Path
P=Path(__file__).resolve().parents[1]/'integration/check_nonore_ore_form_startup.py'
S=importlib.util.spec_from_file_location('ore_startup_checker',P);M=importlib.util.module_from_spec(S);S.loader.exec_module(M)
class StartupChecker(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(prefix='gt6-ore-form-startup-');self.run=Path(self.tmp.name)
  self.world=self.run/'world';self.world.mkdir();self.nonce='a'*32
  (self.world/'.ore-form-startup-used').write_text(self.nonce)
  self.xml=self.run/'result.xml';self.xml.write_text('<testsuite tests="1" failures="0"><testcase name="'+M.CASE+'" classname="'+M.NS+':test_empty"/></testsuite>')
  self.exit=self.run/'exit';self.exit.write_text('0');self.log=self.run/'log'
  self.receipt=dict(schema=1,namespace=M.NS,nonce=self.nonce,world=str(self.world),acceptedCoreForm=True,publicStartupWinner=True,winnerIdentityPreserved=True,rawControlPreserved=True,restored=True,probePassed=True,verifiedRounds=2,form='addon_sample:Dilithium/crushedPurifiedTiny',winner='Dolamide/crushed/Water900',originalPacks=['vanilla'],restoredPacks=['vanilla'])
  self.write()
 def tearDown(self):self.tmp.cleanup()
 def write(self):
  data=json.dumps(self.receipt);(self.world/'.ore-form-startup-cleanup.json').write_text(data)
  self.log.write_text('[ore-form-startup-winner] accepted=true\n[ore-form-startup-cleanup] '+data+'\n')
 def check(self):return M.check(self.xml,self.log,self.exit,self.run,self.nonce)
 def test_valid(self):self.assertTrue(self.check()['accepted_core_form'])
 def test_each_required_literal_rejected(self):
  original=self.receipt.copy()
  for key in ['schema','namespace','nonce','acceptedCoreForm','publicStartupWinner','winnerIdentityPreserved','rawControlPreserved','restored','probePassed','verifiedRounds','form','winner']:
   with self.subTest(key=key):
    self.receipt=original.copy();self.receipt[key]=None;self.write()
    with self.assertRaises((ValueError,TypeError)):self.check()
 def test_no_startup_acceptance(self):
  self.log.write_text(self.log.read_text().replace('accepted=true','accepted=false'))
  with self.assertRaises(ValueError):self.check()
 def test_wrong_exit(self):
  self.exit.write_text('1')
  with self.assertRaises(ValueError):self.check()
 def test_failed_xml(self):
  self.xml.write_text(self.xml.read_text().replace('/>','><failure/></testcase>'))
  with self.assertRaises(ValueError):self.check()
 def test_wrong_marker(self):
  (self.world/'.ore-form-startup-used').write_text('b'*32)
  with self.assertRaises(ValueError):self.check()
 def test_duplicate_receipt_key(self):
  p=self.world/'.ore-form-startup-cleanup.json';p.write_text(p.read_text()[:-1]+',"schema":1}')
  with self.assertRaises(ValueError):self.check()
 def test_no_restoration(self):
  self.receipt['restoredPacks']=['wrong'];self.write()
  with self.assertRaises(ValueError):self.check()
 def test_unfinished(self):
  (self.world/'.ore-form-startup-cleanup.tmp').write_text('x')
  with self.assertRaises(ValueError):self.check()
if __name__=='__main__':unittest.main()
