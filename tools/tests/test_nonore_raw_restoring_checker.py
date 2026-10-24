import unittest,tempfile,json
from pathlib import Path
from tools.integration.check_nonore_raw_restoring import check,NS,CASE
class IceChecker(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory(prefix='gt6-nonore-raw-lifecycle-');self.addCleanup(self.t.cleanup);self.run=Path(self.t.name);self.world=self.run/'world';self.world.mkdir();self.nonce='a'*32
  self.xml=self.run/'x';self.log=self.run/'l';self.exit=self.run/'e';self.receipt=self.world/'.nonore-raw-cleanup.json'
  self.xml.write_text(f'<testsuite><testcase name="{CASE}" classname="{NS}:test_empty"/></testsuite>');self.exit.write_text('0');(self.world/'.nonore-raw-probe-used').write_text(self.nonce)
  self.r=dict(schema=1,namespace=NS,nonce=self.nonce,restored=True,sourceNoPolicyVerified=True,literalRawPairVerified=True,literalSluiceEightVariantsVerified=True,thermalPreserved=True,probePassed=True,verifiedRounds=2,world=str(self.world),originalPacks=['vanilla','mod:gt6ce'],restoredPacks=['vanilla','mod:gt6ce']);self.write()
 def write(self):
  sluice=''.join(f'[nonore-sluice-literal] PVC/{size}/{water} row\n' for _ in range(2) for size in ['whole','tiny'] for water in ['Water','MnWtr','DistW','SpDew'])
  self.receipt.write_text(json.dumps(self.r));self.log.write_text(sluice+'[nonore-raw-literal] hammer row\n[nonore-raw-literal] crusher row\n[nonore-raw-literal] hammer row\n[nonore-raw-cleanup] '+json.dumps(self.r))
 def check(self):return check(self.xml,self.log,self.exit,self.run,self.nonce)
 def test_exact_receipt(self):self.assertEqual(self.check()['verified_rounds'],2)
 def test_flags_types_packs_and_nonce_fail_closed(self):
  good=dict(self.r)
  for k,v in [('literalRawPairVerified',False),('schema',True),('verifiedRounds',True),('nonce','b'*32),('restoredPacks',list(reversed(good['originalPacks']))),('probePassed',False)]:
   self.r=dict(good);self.r[k]=v;self.write()
   with self.subTest(k=k):
    with self.assertRaises(ValueError):self.check()
 def test_no_xml_only_pass(self):
  self.receipt.unlink()
  with self.assertRaises(ValueError):self.check()
 def test_log_and_temporary_cleanup_required(self):
  self.log.write_text(self.log.read_text().replace('[nonore-raw-literal] crusher','[missing]'))
  with self.assertRaises(ValueError):self.check()
  self.write();(self.world/'gt-nonore-raw-lifecycle-probe').mkdir()
  with self.assertRaises(ValueError):self.check()
 def test_native_failure_and_wrong_profile(self):
  self.exit.write_text('1')
  with self.assertRaises(ValueError):self.check()
  self.exit.write_text('0');self.xml.write_text(self.xml.read_text().replace(CASE,'other'))
  with self.assertRaises(ValueError):self.check()

 def test_sluice_missing_duplicate_or_false_receipt_rejected(self):
  self.log.write_text(self.log.read_text().replace('[nonore-sluice-literal] PVC/whole/Water','[missing]',1))
  with self.assertRaises(ValueError):self.check()
  self.write();self.r['literalSluiceEightVariantsVerified']=False;self.write()
  with self.assertRaises(ValueError):self.check()
