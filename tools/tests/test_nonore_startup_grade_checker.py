import unittest,tempfile,json
from pathlib import Path
from tools.integration.check_nonore_startup_grade import check,NS,CASE
class IceStartupGradeChecker(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory(prefix='gt6-nonore-ice-startup-grade-');self.addCleanup(self.t.cleanup);self.run=Path(self.t.name);self.world=self.run/'world';self.world.mkdir();self.nonce='a'*32
  self.xml=self.run/'x';self.log=self.run/'l';self.exit=self.run/'e';self.receipt=self.world/'.nonore-ice-startup-grade-cleanup.json'
  self.xml.write_text(f'<testsuite><testcase name="{CASE}" classname="{NS}:test_empty"/></testsuite>');self.exit.write_text('0');(self.world/'.nonore-ice-startup-grade-used').write_text(self.nonce)
  self.r=dict(schema=1,namespace=NS,nonce=self.nonce,restored=True,sourceNoPolicyVerified=True,acceptedUpperGradesVerified=True,probePassed=True,verifiedRounds=2,world=str(self.world),originalPacks=['vanilla','mod:gt6ce'],restoredPacks=['vanilla','mod:gt6ce']);self.write()
 def write(self):
  self.receipt.write_text(json.dumps(self.r));self.log.write_text('[nonore-ice-startup-grade-requests] accepted=3\n[nonore-ice-startup-grade-literal] tiny row\n[nonore-ice-startup-grade-literal] whole row\n[nonore-ice-startup-grade-literal] tiny row\n[nonore-ice-startup-grade-cleanup] '+json.dumps(self.r))
 def check(self):return check(self.xml,self.log,self.exit,self.run,self.nonce)
 def test_exact_receipt(self):self.assertEqual(self.check()['verified_rounds'],2)
 def test_flags_types_packs_and_nonce_fail_closed(self):
  good=dict(self.r)
  for k,v in [('acceptedUpperGradesVerified',False),('schema',True),('verifiedRounds',True),('nonce','b'*32),('restoredPacks',list(reversed(good['originalPacks']))),('probePassed',False)]:
   self.r=dict(good);self.r[k]=v;self.write()
   with self.subTest(k=k):
    with self.assertRaises(ValueError):self.check()
 def test_no_xml_only_pass(self):
  self.receipt.unlink()
  with self.assertRaises(ValueError):self.check()
 def test_log_and_temporary_cleanup_required(self):
  self.log.write_text(self.log.read_text().replace('[nonore-ice-startup-grade-literal] whole','[missing]'))
  with self.assertRaises(ValueError):self.check()
  self.write();(self.world/'gt-nonore-ice-startup-grade-probe').mkdir()
  with self.assertRaises(ValueError):self.check()
 def test_native_failure_and_wrong_profile(self):
  self.exit.write_text('1')
  with self.assertRaises(ValueError):self.check()
  self.exit.write_text('0');self.xml.write_text(self.xml.read_text().replace(CASE,'other'))
  with self.assertRaises(ValueError):self.check()
