import json,tempfile,unittest
from pathlib import Path
from tools.integration.check_nonore_stop import check,NS,CASE
class StopChecker(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(prefix='gt6-nonore-stop-');self.addCleanup(self.tmp.cleanup)
  self.run=Path(self.tmp.name);self.world=self.run/'world';self.world.mkdir();self.nonce='a'*32
  self.xml=self.run/'report.xml';self.exit=self.run/'exit';self.log=self.run/'log';self.receipt=self.world/'.nonore-stop-event.json'
  self.goodxml=f'<testsuite><testcase name="{CASE}" classname="{NS}:test_empty"/></testsuite>'
  self.r=dict(schema=1,namespace=NS,test=CASE,nonce=self.nonce,world=str(self.world),event='ServerStoppedEvent',priority='LOWEST',armedActiveRows=2,realEventObserved=True,rowsAbsent=True,foreignMetadataAbsent=True,foreignRoutesAbsent=True,startupWinnerPreserved=True,passed=True,originalPacks=['vanilla'],selectedProbePacks=['vanilla','file/gt-nonore-stop-probe'])
  self.reset()
 def reset(self):
  self.xml.write_text(self.goodxml);self.exit.write_text('0');(self.world/'.nonore-stop-used').write_text(self.nonce);self.write()
 def write(self):
  self.receipt.write_text(json.dumps(self.r));a={k:self.r[k] for k in ('schema','namespace','test','nonce','world','armedActiveRows')};a['passed']=True
  self.log.write_text('[nonore-stop-arm] '+json.dumps(a)+'\n[nonore-stop-event] '+json.dumps(self.r)+'\n')
 def check(self):return check(self.xml,self.run,self.exit,self.log,self.nonce)
 def test_valid(self):self.assertFalse(self.check()['same_jvm_new_world_verified'])
 def test_receipt_flags_required_true(self):
  for k in ('realEventObserved','rowsAbsent','foreignMetadataAbsent','foreignRoutesAbsent','startupWinnerPreserved','passed'):
   for v in (False,1,'true',None):
    with self.subTest(k=k,v=v):
     self.r[k]=v;self.write()
     with self.assertRaises(ValueError):self.check()
   self.r[k]=True
 def test_exact_xml_and_failure_metadata(self):
  for x in (self.goodxml.replace(CASE,CASE+'.alias'),self.goodxml.replace(NS,NS+'_alias'),self.goodxml.replace('/>','><failure/></testcase>'),self.goodxml.replace('/>','><skipped/></testcase>'),self.goodxml.replace('<testsuite>','<testsuite errors="1">'),'<testsuite/>'):
   self.xml.write_text(x)
   with self.assertRaises(ValueError):self.check()
 def test_missing_receipt_or_event_or_arm(self):
  self.receipt.unlink()
  with self.assertRaises(ValueError):self.check()
  self.reset()
  for prefix in ('[nonore-stop-arm]','[nonore-stop-event]'):
   self.write();self.log.write_text('\n'.join(l for l in self.log.read_text().splitlines() if not l.startswith(prefix)))
   with self.assertRaises(ValueError):self.check()
 def test_nonce_marker_and_exit(self):
  (self.world/'.nonore-stop-used').write_text('b'*32)
  with self.assertRaises(ValueError):self.check()
  self.reset();self.exit.write_text('1')
  with self.assertRaises(ValueError):self.check()
  self.reset();self.r['nonce']='b'*32;self.write()
  with self.assertRaises(ValueError):self.check()
 def test_wrong_probe_pack_rejected(self):
  self.r['selectedProbePacks']=['vanilla','file/other'];self.write()
  with self.assertRaises(ValueError):self.check()
 def test_duplicate_log_and_mismatching_event(self):
  self.log.write_text(self.log.read_text()*2)
  with self.assertRaises(ValueError):self.check()
  self.reset();self.log.write_text(self.log.read_text().replace('"rowsAbsent": true','"rowsAbsent": false'))
  with self.assertRaises(ValueError):self.check()
 def test_order_error_and_unfinished_write(self):
  self.log.write_text('\n'.join(reversed(self.log.read_text().splitlines())))
  with self.assertRaises(ValueError):self.check()
  self.reset();self.r['error']='test';self.write()
  with self.assertRaises(ValueError):self.check()
  self.r.pop('error');self.reset();(self.world/'.nonore-stop-event.a.tmp').write_text('x')
  with self.assertRaises(ValueError):self.check()
 def test_outside_world_and_duplicate_json(self):
  self.r['world']=str(self.run);self.write()
  with self.assertRaises(ValueError):self.check()
  self.reset();self.receipt.write_text('{"passed":true,"passed":false}')
  with self.assertRaises(ValueError):self.check()
if __name__=='__main__':unittest.main()
