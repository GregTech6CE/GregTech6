import os,unittest,subprocess,sys,signal,tempfile
from pathlib import Path
from unittest.mock import patch
from tools.integration.run_nonore_profiles import PROFILES,command,stop_group,interrupted,executable_path
class NonOreCIContract(unittest.TestCase):
 def test_exact_profile_partition_and_commands(self):
  self.assertEqual(PROFILES,(('cohort','gregtech_nonore_cohort_audit',3),('restoring','gregtech_nonore_tag_audit',1),('stop','gregtech_nonore_stop_audit',1)))
  with patch.dict(os.environ,{'JAVA17_HOME':'/jdk17'}):
   for loader,node in [('forge','1.20.1-forge'),('neoforge','1.21.1-neoforge')]:
    for _,ns,_ in PROFILES:
     c=command(Path('/repo'),loader,ns,Path('/fresh'),Path('/report'))
     self.assertIn('-PgameTestNamespaces='+ns,c);self.assertEqual(c[-1],':multiversion:'+node+':runGameTestServer')
     self.assertIn('-PgameTestDirectory=/fresh',c);self.assertIn('-PneoforgeGameTestDirectory=/fresh',c)
     self.assertIn('--no-daemon',c);self.assertIn('--max-workers=1',c)
 def test_profile_source_and_workflow_boundaries(self):
  root=Path(__file__).resolve().parents[2];s=(root/'tools/integration/run_nonore_profiles.py').read_text()
  self.assertIn('start_new_session=True',s);self.assertIn('os.killpg',s);self.assertIn('tempfile.mkdtemp',s)
  self.assertNotIn('eula=true',s);self.assertNotIn('git reset',s)
  workflow=(root/'.github/workflows/nonore-regressions.yml').read_text()
  self.assertIn('include-hidden-files: true',workflow);self.assertIn('if: always()',workflow)
  regular=(root/'.github/workflows/issue-regressions.yml').read_text()
  for _,ns,_ in PROFILES:self.assertNotIn(ns,regular)

 def test_bounded_process_group_cleanup_and_signal(self):
  proc=subprocess.Popen([sys.executable,'-c','import time; time.sleep(60)'],start_new_session=True)
  try:
   stop_group(proc);self.assertIsNotNone(proc.poll())
   with self.assertRaises(InterruptedError):interrupted(signal.SIGTERM,None)
  finally:
   if proc.poll() is None:proc.kill();proc.wait()

 def test_executable_is_one_validated_file_not_shell_text(self):
  with tempfile.TemporaryDirectory() as td:
   root=Path(td);exe=root/'wrapper with spaces';exe.write_text('#!/bin/sh\nexit 0\n');exe.chmod(0o755)
   resolved=executable_path(root,exe);self.assertEqual(resolved,exe.resolve())
   with patch.dict(os.environ,{'JAVA17_HOME':'/jdk17'}):
    base=command(root,'forge','only',root/'run',root/'xml')
    changed=command(root,'forge','only',root/'run',root/'xml',resolved)
    self.assertEqual(changed[0],str(resolved));self.assertEqual(changed[1:],base[1:])
    self.assertEqual(base[:4],[str(root/'gradlew'),'--no-daemon','--stacktrace','--max-workers=1'])
   for invalid in (root,root/'missing',str(exe)+' --fake-arg'):
    with self.assertRaises((ValueError,FileNotFoundError)):executable_path(root,invalid)
 def test_wrapper_mode_avoids_duplicate_owned_options(self):
  with patch.dict(os.environ,{},clear=True):
   c=command(Path('/repo'),'forge','only',Path('/fresh'),Path('/report'),Path('/wrapper with spaces'),True)
  self.assertEqual(c[0],'/wrapper with spaces')
  for prefix in ('--no-daemon','--max-workers','-Dorg.gradle.java.home','-Dorg.gradle.java.installations'):
   self.assertFalse(any(a.startswith(prefix) for a in c))
  self.assertIn('-Dorg.gradle.jvmargs=-Xmx1536m',c);self.assertIn('-PgameTestHeap=3g',c)
