#!/usr/bin/env python3
"""Fixed-only isolated native gates. No baseline reversal, existing-world reuse or EULA writes."""
import argparse,json,os,signal,subprocess,sys,tempfile,uuid
from pathlib import Path
INPUT_PROFILES=(('cohort','gregtech_nonore_cohort_audit',3),('restoring','gregtech_nonore_tag_audit',1),('stop','gregtech_nonore_stop_audit',1))
SIFTING_PROFILES=(('sifting','gregtech_nonore_sifting_audit',5),('ice','gregtech_nonore_ice_sifting_audit',1),('ice_winner','gregtech_nonore_ice_winner_audit',1),('startup_grade','gregtech_nonore_ice_startup_grade_audit',1))
PROFILES=INPUT_PROFILES+SIFTING_PROFILES
PROFILE_CONFIG={
 'cohort':('gt6-nonore-cohort-',None), 'restoring':('gt6-nonore-tag-','TAG'),
 'stop':('gt6-nonore-stop-','STOP'), 'sifting':('gt6-nonore-sifting-',None),
 'ice':('gt6-nonore-ice-','ICE'), 'ice_winner':('gt6-nonore-ice-winner-','ICE_WINNER'),
 'startup_grade':('gt6-nonore-ice-startup-grade-','ICE_STARTUP_GRADE')}
def profiles(group):
 return {'all':PROFILES,'inputs':INPUT_PROFILES,'sifting':SIFTING_PROFILES}[group]
def profile_environment(name,run,nonce,inherited):
 env=dict(inherited)
 # Isolated profiles must not inherit a previous profile's directory/nonce.
 for key in tuple(env):
  if key.startswith('GT6_NONORE_') and key.endswith(('_DIRECTORY','_NONCE')):env.pop(key)
 stem=PROFILE_CONFIG[name][1]
 if stem:
  env['GT6_NONORE_'+stem+'_DIRECTORY']=str(run)
  if name!='restoring':env['GT6_NONORE_'+stem+'_NONCE']=nonce
 return env
def checker_arguments(root,name,xml,log,exitf,run,nonce):
 args=[sys.executable,str(root/'tools/integration'/('check_nonore_'+name+'.py')),
       '--xml',str(xml),'--exit-file',str(exitf),'--log',str(log)]
 if name in ('cohort','sifting'):args+=['--phase','fixed']
 else:args+=['--run-directory',str(run)]
 if name in ('stop','ice','ice_winner','startup_grade'):args+=['--nonce',nonce]
 return args
def executable_path(root, value):
 p=Path(value) if value is not None else root/'gradlew'
 p=p.resolve(strict=True)
 if not p.is_file() or not os.access(p,os.X_OK):raise ValueError('Gradle executable must be one executable file: '+str(p))
 return p
def command(root,loader,namespace,run,xml,executable=None,wrapper_mode=False):
 node={'forge':'1.20.1-forge','neoforge':'1.21.1-neoforge'}[loader]
 launch=[str(executable if executable is not None else root/'gradlew')]
 if wrapper_mode:
  # Explicit opt-in: wrapper owns daemon/worker/JDK/toolchain defaults. The reviewed
  # local wrapper already adds these; do not depend on duplicate-option semantics.
  launch+=['--stacktrace','-Dorg.gradle.jvmargs=-Xmx1536m']
 else:
  launch+=['--no-daemon','--stacktrace','--max-workers=1',
   '-Dorg.gradle.jvmargs=-Xmx1536m','-Dorg.gradle.java.home='+os.environ['JAVA17_HOME'],
   '-Dorg.gradle.java.installations.fromEnv=JAVA17_HOME,JAVA21_HOME',
   '-Dorg.gradle.java.installations.auto-download=false']
 return launch+['-PaddonSample=true',
 '-PgameTestHeap=3g','-PgameTestNamespaces='+namespace,
 '-PgameTestDirectory='+str(run),'-PneoforgeGameTestDirectory='+str(run),
 '-PgameTestReport='+str(xml),':multiversion:'+node+':runGameTestServer']
def interrupted(signum, frame):
 raise InterruptedError("runner interrupted by signal "+str(signum))
def stop_group(proc):
 # Bound cleanup of this process group; CI VM teardown remains authoritative for
 # any independently detached tool process. Never continue into another profile.
 try:os.killpg(proc.pid,signal.SIGKILL)
 except ProcessLookupError:pass
 proc.wait(timeout=30)
def main():
 for sig in (signal.SIGTERM,signal.SIGINT):signal.signal(sig,interrupted)
 p=argparse.ArgumentParser();p.add_argument('--loader',choices=('forge','neoforge'),required=True);p.add_argument('--profile-group',choices=('all','inputs','sifting'),default='all');p.add_argument('--gradle-executable',type=Path);p.add_argument('--gradle-wrapper-mode',action='store_true');a=p.parse_args()
 root=Path(__file__).resolve().parents[2]
 executable=executable_path(root,a.gradle_executable)
 if a.gradle_wrapper_mode and a.gradle_executable is None:raise ValueError('wrapper mode requires explicit executable')
 if not a.gradle_wrapper_mode:
  for jdk in ('JAVA17_HOME','JAVA21_HOME'):
   if not (Path(os.environ[jdk])/'bin/java').is_file():raise ValueError('missing JDK '+jdk)
 # An invocation never resumes or consumes old evidence, including after an interrupted job.
 base=root/'work/nonore-ci';base.mkdir(parents=True,exist_ok=True)
 out=Path(tempfile.mkdtemp(prefix=a.loader+'-',dir=base));print('Evidence: '+str(out),flush=True)
 for name,namespace,count in profiles(a.profile_group):
  evidence=out/name;evidence.mkdir()
  prefix=PROFILE_CONFIG[name][0]
  run=Path(tempfile.mkdtemp(prefix=prefix,dir=evidence));xml=evidence/'native.xml';log=evidence/'native.log';exitf=evidence/'native.exit'
  nonce=uuid.uuid4().hex;env=profile_environment(name,run,nonce,os.environ)
  if a.gradle_wrapper_mode:env['LOCAL_GRADLE_MAX_WORKERS']='1'
  cmd=command(root,a.loader,namespace,run,xml,executable,a.gradle_wrapper_mode)
  (evidence/'invocation.json').write_text(json.dumps({'command':cmd,'namespace':namespace,'cases':count,'wrapperMode':a.gradle_wrapper_mode,'run':str(run),'nonce':nonce if name in ('stop','ice','ice_winner','startup_grade') else None},indent=2))
  # One Gradle invocation launches one disposable native JVM. On timeout kill its whole
  # process group, retain evidence, and abort: never run the next profile after taint.
  with log.open('w') as stream:
   proc=subprocess.Popen(cmd,cwd=root,env=env,stdout=stream,stderr=subprocess.STDOUT,start_new_session=True)
   try:code=proc.wait(timeout=25*60)
   except subprocess.TimeoutExpired:
    stop_group(proc);code=124
   except BaseException:
    exitf.write_text("130\n")
    stop_group(proc)
    raise
  exitf.write_text(str(code)+'\n')
  args=checker_arguments(root,name,xml,log,exitf,run,nonce)
  result=subprocess.run(args,cwd=root,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
  (evidence/'check.log').write_text(result.stdout);print(result.stdout,flush=True)
  if code or result.returncode:raise RuntimeError(name+' failed; no subsequent profile permitted; evidence '+str(evidence))
 print('PASS: isolated '+a.profile_group+' profiles; '+str(sum(p[2] for p in profiles(a.profile_group)))+' cases; normal-suite count unchanged.')
if __name__=='__main__':
 try:main()
 except Exception as exc:sys.exit('FAIL: '+str(exc))
