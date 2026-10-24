import os,unittest,subprocess,sys,signal,tempfile
from pathlib import Path
from unittest.mock import patch
from tools.integration.run_nonore_profiles import PROFILES,INPUT_PROFILES,SIFTING_PROFILES,BATCH_PROFILES,PROFILE_CONFIG,profiles,profile_environment,checker_arguments,command,stop_group,interrupted,executable_path
class NonOreCIContract(unittest.TestCase):
 def test_exact_profile_partition_and_commands(self):
  self.assertEqual(INPUT_PROFILES,(('cohort','gregtech_nonore_cohort_audit',3),('restoring','gregtech_nonore_tag_audit',1),('stop','gregtech_nonore_stop_audit',1)))
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

 def test_exact_sifting_partition_and_no_environment_leakage(self):
  self.assertEqual(SIFTING_PROFILES,(('sifting','gregtech_nonore_sifting_audit',5),('ice','gregtech_nonore_ice_sifting_audit',1),('ice_winner','gregtech_nonore_ice_winner_audit',1),('startup_grade','gregtech_nonore_ice_startup_grade_audit',1)))
  self.assertEqual(PROFILES,INPUT_PROFILES+SIFTING_PROFILES+BATCH_PROFILES)
  self.assertEqual(sum(p[2] for p in PROFILES),26)
  self.assertEqual(profiles('inputs'),INPUT_PROFILES);self.assertEqual(profiles('sifting'),SIFTING_PROFILES)
  self.assertEqual(profiles('all'),PROFILES)
  with self.assertRaises(KeyError):profiles('unknown')
  inherited={'GT6_NONORE_TAG_DIRECTORY':'old','GT6_NONORE_STOP_NONCE':'old','GT6_NONORE_ICE_DIRECTORY':'old','GT6_NONORE_ICE_STARTUP_GRADE_NONCE':'old','KEEP':'yes'}
  for name,_,_ in PROFILES:
   env=profile_environment(name,Path('/fresh'),'a'*32,inherited)
   self.assertEqual(env['KEEP'],'yes');self.assertNotIn('old',env.values());self.assertEqual(inherited['GT6_NONORE_TAG_DIRECTORY'],'old')
   stem=PROFILE_CONFIG[name][1]
   expected={} if stem is None else {'GT6_NONORE_'+stem+'_DIRECTORY':'/fresh'}
   if stem and name!='restoring':expected['GT6_NONORE_'+stem+'_NONCE']='a'*32
   self.assertEqual({k:v for k,v in env.items() if k.startswith('GT6_NONORE_')},expected)
   args=checker_arguments(Path('/repo'),name,Path('/x'),Path('/l'),Path('/e'),Path('/fresh'),'a'*32)
   self.assertIn('/repo/tools/integration/check_nonore_'+name+'.py',args)
   self.assertEqual('--phase' in args,name in ('cohort','sifting','batch'))
   self.assertEqual('--nonce' in args,name in ('stop','ice','ice_winner','startup_grade','raw_restoring','raw_winner'))
   self.assertEqual('--run-directory' in args,name not in ('cohort','sifting','batch'))
 def test_ci_group_time_budgets_and_unique_artifacts(self):
  root=Path(__file__).resolve().parents[2];s=(root/'.github/workflows/nonore-regressions.yml').read_text()
  self.assertIn('timeout-minutes: ${{ matrix.timeout }}',s)
  import re
  jobs=re.findall(r'- loader: (forge|neoforge)\s+group: (inputs|sifting|batch)\s+timeout: (\d+)',s)
  self.assertEqual(jobs,[('forge','inputs','90'),('neoforge','inputs','90'),('forge','sifting','120'),('neoforge','sifting','120'),('forge','batch','90'),('neoforge','batch','90')])
  self.assertEqual(s.count('group: inputs'),2);self.assertEqual(s.count('group: sifting'),2)
  self.assertEqual(s.count('timeout: 90'),4);self.assertEqual(s.count('timeout: 120'),2)
  self.assertIn('--profile-group ${{ matrix.group }}',s)
  self.assertIn('${{ matrix.loader }}-${{ matrix.group }}-nonore-regressions-',s)
  runner=(root/'tools/integration/run_nonore_profiles.py').read_text()
  self.assertIn('proc.wait(timeout=25*60)',runner)

 def test_shipped_source_provenance_is_bounded_and_available(self):
  import json,re
  root=Path(__file__).resolve().parents[2]
  d=json.loads((root/'docs/integration/nonore-sifting-source-profile.json').read_text())
  self.assertEqual({n:g['id'] for n,g in d['generation'].items()},{'Flint':8002,'Ice':9802,'Obsidian':8214})
  self.assertTrue(all(g['ORES'] is False for g in d['generation'].values()))
  self.assertEqual(d['generation']['Ice']['forced'],['gemChipped','gemFlawed','gem'])
  self.assertFalse(d['generation']['Ice']['upper_graded_generation'])
  self.assertIn('UNKNOWN',d['generation']['Flint']['actual_common_gem'])
  self.assertEqual({v['source'] for v in d['evidence']},{'gregtech6-original','gregtech6-w'})
  for v in d['evidence']:
   self.assertRegex(v['sha256'],r'^[0-9a-f]{64}$');self.assertTrue(v['lines'])
  self.assertIn('No late grade-tag binding feature',d['runtime_scope'])

 def test_exact_batch_membership_and_provenance(self):
  import json
  self.assertEqual(BATCH_PROFILES,(('batch','gregtech_ore_alignment_batch',11),('raw_restoring','gregtech_nonore_raw_lifecycle_audit',1),('raw_winner','gregtech_nonore_raw_winner_audit',1)))
  self.assertEqual(profiles('batch'),BATCH_PROFILES)
  self.assertEqual(len(PROFILES),10);self.assertEqual(len({p[0] for p in PROFILES}),10);self.assertEqual(len({p[1] for p in PROFILES}),10)
  self.assertEqual(sum(p[2] for p in INPUT_PROFILES+SIFTING_PROFILES),13);self.assertEqual(sum(p[2] for p in BATCH_PROFILES),13)
  root=Path(__file__).resolve().parents[2]
  d=json.loads((root/'docs/integration/ore-magnetic-positive-source-profile.json').read_text())
  self.assertEqual(d['count'],84);self.assertEqual(len(d['rows']),84)
  self.assertEqual(len({(v['id'],v['canonical_name']) for v in d['rows']}),84)
  self.assertEqual(set(d['sources']),{'original','w'})
  for v in d['sources'].values():self.assertRegex(v['sha256'],r'^[0-9a-f]{64}$')
  workflow=(root/'.github/workflows/nonore-regressions.yml').read_text()
  self.assertEqual(workflow.count('group: batch'),2)
  for module in ['test_nonore_batch_checker','test_nonore_raw_restoring_checker','test_nonore_raw_winner_checker']:self.assertIn('tools.tests.'+module,workflow)
