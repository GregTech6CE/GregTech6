import json,tempfile,unittest
from pathlib import Path
from tools.integration.check_nonore_restoring import check,CASE,NAMESPACE
class RestoringChecker(unittest.TestCase):
 def test_receipt_and_native_evidence_required(self):
  with tempfile.TemporaryDirectory(prefix='gt6-nonore-tag-') as td:
   run=Path(td);world=run/'world';world.mkdir();(world/'.nonore-tag-probe-used').touch()
   xml=run/'x.xml';exitf=run/'e';log=run/'l';receipt=world/'.nonore-tag-cleanup.json'
   xml.write_text(f'<testsuite><testcase name="{CASE}" classname="{NAMESPACE}:test_empty"/></testsuite>');exitf.write_text('0')
   log.write_text('[nonore-startup-winner] Rubber accepted=true\n[nonore-tag-cleanup] restored=true verifiedRounds=2 probePassed=true')
   r=dict(restored=True,probePassed=True,verifiedRounds=2,sourceNoPolicyVerified=True,sameInputStartupWinnerVerified=True,world=str(world));receipt.write_text(json.dumps(r))
   self.assertTrue(check(xml,run,exitf,log)['restored'])
   for key in ('restored','probePassed','sourceNoPolicyVerified','sameInputStartupWinnerVerified'):
    bad=dict(r);bad[key]=False;receipt.write_text(json.dumps(bad))
    with self.assertRaises(ValueError):check(xml,run,exitf,log)
   receipt.write_text(json.dumps(r));log.write_text('')
   with self.assertRaises(ValueError):check(xml,run,exitf,log)
   receipt.unlink()
   with self.assertRaises(ValueError):check(xml,run,exitf,log)
