import unittest,tempfile,json
from pathlib import Path
from tools.integration.check_nonore_raw_winner import check,NS,CASE
class IceChecker(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory(prefix='gt6-nonore-raw-winner-');self.addCleanup(self.t.cleanup);self.run=Path(self.t.name);self.world=self.run/'world';self.world.mkdir();self.nonce='a'*32
  self.xml=self.run/'x';self.log=self.run/'l';self.exit=self.run/'e';self.receipt=self.world/'.nonore-raw-winner-cleanup.json'
  self.xml.write_text(f'<testsuite><testcase name="{CASE}" classname="{NS}:test_empty"/></testsuite>');self.exit.write_text('0');(self.world/'.nonore-raw-winner-probe-used').write_text(self.nonce)
  self.r=dict(schema=1,namespace=NS,nonce=self.nonce,restored=True,sourceNoPolicyVerified=True,hammerLiteralAndCrusherWinnerVerified=True,sluiceStartupWinnerVerified=True,sluiceDatapackPriorityVerified=True,sluiceCleanupVerified=True,probePassed=True,verifiedRounds=2,world=str(self.world),originalPacks=['vanilla','mod:gt6ce'],restoredPacks=['vanilla','mod:gt6ce']);self.write()
 def write(self):
  sluice='[nonore-sluice-winner-startup] accepted=true\n'
  sluice+=''.join('[nonore-sluice-winner-identity] '+p+' verified=true\n' for p in ['initial','tagged-0','tagged-1','restored'])
  sluice+='[nonore-sluice-datapack] tagged-1 verified=true\n'
  sluice+=''.join('[nonore-sluice-literal] PVC/'+size+'/'+water+' row\n' for size in ['whole','tiny'] for water in ['Water','MnWtr','DistW','SpDew']
   for _ in range(0 if (size,water)==('whole','Water') else 1 if (size,water)==('tiny','MnWtr') else 2))
  sluice+=''.join('[nonore-sluice-winner-dormant] '+phase+'/'+size+'/'+water+' verified=true\n' for phase in ['initial','restored'] for size in ['whole','tiny']
   for water in ['Water','MnWtr','DistW','SpDew'] if (size,water)!=('whole','Water'))
  self.receipt.write_text(json.dumps(self.r));self.log.write_text(sluice+'[nonore-raw-winner-startup] accepted=true\n'+'\n'.join('[nonore-raw-winner-identity] '+p+' verified=true' for p in ['initial','tagged-0','tagged-1','restored'])+'\n[nonore-raw-winner-literal] hammer row\n[nonore-raw-winner-cleanup] '+json.dumps(self.r))
 def check(self):return check(self.xml,self.log,self.exit,self.run,self.nonce)
 def test_exact_receipt(self):self.assertEqual(self.check()['verified_rounds'],2)
 def test_flags_types_packs_and_nonce_fail_closed(self):
  good=dict(self.r)
  for k,v in [('hammerLiteralAndCrusherWinnerVerified',False),('schema',True),('verifiedRounds',True),('nonce','b'*32),('restoredPacks',list(reversed(good['originalPacks']))),('probePassed',False)]:
   self.r=dict(good);self.r[k]=v;self.write()
   with self.subTest(k=k):
    with self.assertRaises(ValueError):self.check()
 def test_no_xml_only_pass(self):
  self.receipt.unlink()
  with self.assertRaises(ValueError):self.check()
 def test_log_and_temporary_cleanup_required(self):
  self.log.write_text(self.log.read_text().replace('[nonore-raw-winner-literal] hammer','[missing]'))
  with self.assertRaises(ValueError):self.check()
  self.write();(self.world/'gt-nonore-raw-winner-probe').mkdir()
  with self.assertRaises(ValueError):self.check()
 def test_native_failure_and_wrong_profile(self):
  self.exit.write_text('1')
  with self.assertRaises(ValueError):self.check()
  self.exit.write_text('0');self.xml.write_text(self.xml.read_text().replace(CASE,'other'))
  with self.assertRaises(ValueError):self.check()

 def test_missing_or_duplicate_winner_phase_fails(self):
  self.log.write_text(self.log.read_text().replace('[nonore-raw-winner-identity] tagged-1 verified=true',''))
  with self.assertRaises(ValueError):self.check()
  self.write();self.log.write_text(self.log.read_text()+'\n[nonore-raw-winner-identity] restored verified=true')
  with self.assertRaises(ValueError):self.check()
 def test_rejected_startup_cannot_pass(self):
  self.log.write_text(self.log.read_text().replace('accepted=true','accepted=false'))
  with self.assertRaises(ValueError):self.check()

 def test_sluice_phase_variant_and_priority_fail_closed(self):
  for old,new in [('[nonore-sluice-winner-startup] accepted=true','[nonore-sluice-winner-startup] accepted=false'),
   ('[nonore-sluice-winner-identity] tagged-1 verified=true',''),
   ('[nonore-sluice-datapack] tagged-1 verified=true',''),
   ('[nonore-sluice-literal] PVC/tiny/MnWtr','[nonore-sluice-literal] PVC/whole/Water'),
   ('[nonore-sluice-winner-dormant] restored/tiny/MnWtr verified=true','')]:
   self.write();self.log.write_text(self.log.read_text().replace(old,new,1))
   with self.subTest(old=old):
    with self.assertRaises(ValueError):self.check()
  for extra in ['[nonore-sluice-winner-identity] restored verified=true','[nonore-sluice-datapack] tagged-1 verified=true',
   '[nonore-sluice-literal] PVC/tiny/Water row','[nonore-sluice-winner-dormant] restored/tiny/Water verified=true']:
   self.write();self.log.write_text(self.log.read_text()+'\n'+extra)
   with self.subTest(extra=extra):
    with self.assertRaises(ValueError):self.check()
 def test_sluice_completion_flags_required(self):
  for key in ['sluiceStartupWinnerVerified','sluiceDatapackPriorityVerified','sluiceCleanupVerified']:
   self.r[key]=False;self.write()
   with self.subTest(key=key):
    with self.assertRaises(ValueError):self.check()
   self.r[key]=True
