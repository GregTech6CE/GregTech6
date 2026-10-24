"""Synthetic strict completion evidence; no ignored logs or native process launches."""
import re,tempfile,unittest
from pathlib import Path
import xml.etree.ElementTree as ET
from tools.integration import check_nonore_completion as c
def fixture(phase,loader='forge'):
 root=ET.Element('testsuite',tests='6',failures='3' if phase=='baseline' else '0',errors='0',skipped='0')
 for name in sorted(set(c.FAILURES)|c.CONTROLS):
  case=ET.SubElement(root,'testcase',name=name,classname=c.NS+':test_empty')
  if phase=='baseline' and name in c.FAILURES:ET.SubElement(case,'failure',message=c.FAILURES[name]+' literal vector')
 lines=['[ore-completion-preserved] thermal16+whole4 verified=true','[ore-completion-preserved] raw1+centrifuge2 verified=true']
 for name,ident,ticks,temperature,density in [('Titanium',220,519,1941,'4.54'),('Iron',260,830,1811,'7.874')]:
  lower=name.lower();fluid=(f'net.minecraftforge.fluids.FluidStack@{ident:x}' if loader=='forge' else f'144 gt6ce:molten_{lower}')
  lines += [f'[whole-dust-thermal-facts] {name} canonical=true ordinary=true unchanged=true melting={temperature} density={density} target=GTMaterial{{{name}, id={ident}}} amount=648648000 fluid={fluid} sourceSmelterTicks={ticks}',
   f'[whole-dust-thermal-row] {name}/Smelter selected=true items=[] fluids=[{fluid}] ticks={ticks} eut=16']
  for machine in ['Melter','Smelter']:lines.append(f'[whole-dust-thermal-census] {name}/{machine} accepted={int(machine=="Smelter")}')
 for size in ['tiny','whole']:
  for water in ['Water','MnWtr','DistW','SpDew']:lines.append(f'[slurry-recycling-role] Mixer/{size}/{water} accepted=1 selected=true warm=true exact=true')
 lines+=['[slurry-recycling-role] Drying/tiny accepted=1 selected=true warm=true exact=true','[slurry-recycling-preserved] Mixer/whole/Water literal']
 for key in ['Stone/rockGt','NetherBrick/rockGt','Cinnabar/oreRaw']:
  lines += [f'[anvil-source-facts] {key} canonical=true inputIdentity=true ordinary=true conditions=true',f'[anvil-source-census] {key} accepted=1 cold=literal warmSame=true occupiedSecond=none']
 for key,mb,ticks in [('Titanium/dustSmall',36,129),('Titanium/dustTiny',16,57),('Iron/dustSmall',36,207),('Iron/dustTiny',16,92)]:
  lines += [f'[fractional-thermal-facts] {key} canonical=true ordinary=true unchanged=true sourceMb={mb} sourceTicks={ticks}']
  for m in ['Melter','Smelter']:lines += [f'[fractional-thermal-census] {key}/{m} accepted=0 cold=none warmSame=true']
 for name,ident,ticks,quality in [('Copper',290,100,0),('Titanium',220,519,3),('Iron',260,830,2)]:
  lower=name.lower()
  lines += [f'[ore-completion-census] {ident}/{name}/dust/gt.recipe.smelter accepted=1 selected=true dynamic=false addon=false quality={quality} target={name} amount=648648000',
   f'[ore-completion-detail] {ident}/dust/gt.recipe.smelter [gt6ce:dust_{lower}x1]->[];chance=[];work={ticks}/16;fluidInputs=[];fluidOutputs=[gt6ce:molten_{lower}x144]']
 lines.append('[ore-completion-census-total] materials=3 roles=3 accepted=3 selected=3 sourceParityClaim=false')
 for size in ['tiny','whole']:
  for water in ['Water','MnWtr','DistW','SpDew']:lines.append(f'[slurry-recycling-row] Mixer/{size}/{water} selected=true actualSelected=true actualWarm=true literal')
 lines.append('[slurry-recycling-row] Drying/tiny selected=true actualSelected=true actualWarm=true literal')
 log='\n'.join(lines)
 if phase=='baseline':log=log.replace('Drying/tiny accepted=1 selected=true warm=true exact=true','Drying/tiny accepted=1 selected=true warm=true exact=false')
 return ET.tostring(root), log, '1' if phase=='baseline' else '0'
class Guards(unittest.TestCase):
 def invoke(self,phase='baseline',mutate=None):
  xml,log,code=fixture(phase)
  if mutate:xml,log,code=mutate(xml,log,code)
  with tempfile.TemporaryDirectory() as d:
   p=Path(d);(p/'x').write_bytes(xml);(p/'l').write_text(log);(p/'e').write_text(code);return c.check(p/'x',p/'l',p/'e',phase)
 def reject(self,fn,phase='baseline'):
  with self.assertRaises((ValueError,AssertionError)):self.invoke(phase,fn)
 def test_baseline(self):self.assertEqual(self.invoke()['cases'],6)
 def test_fixed(self):self.assertEqual(self.invoke('fixed')['semantic_failures'],0)
 def test_wrong_exit(self):self.reject(lambda x,l,e:(x,l,'0'))
 def test_wrong_namespace(self):self.reject(lambda x,l,e:(x.replace(c.NS.encode(),b'wrong'),l,e))
 def test_prerequisite_failure(self):self.reject(lambda x,l,e:(x.replace(b'source slurry recycling mismatches:',b'fixture missing'),l,e))
 def test_missing_slurry(self):self.reject(lambda x,l,e:(x,l.replace('[slurry-recycling-role] Drying/tiny','[missing] Drying/tiny'),e))
 def test_duplicate_slurry(self):self.reject(lambda x,l,e:(x,l+'\n[slurry-recycling-role] Drying/tiny accepted=1 selected=true warm=true exact=true',e))
 def test_anvil_fixture(self):self.reject(lambda x,l,e:(x,l.replace('inputIdentity=true','inputIdentity=false'),e))
 def test_fraction_changed(self):self.reject(lambda x,l,e:(x,l.replace('sourceTicks=129','sourceTicks=130'),e))
 def test_fraction_incomplete(self):self.reject(lambda x,l,e:(x,l.replace('[fractional-thermal-census] Iron/dustTiny/Melter','[missing] Iron/dustTiny/Melter'),e))
 def test_census_count(self):self.reject(lambda x,l,e:(x,l.replace('materials=3 roles=3','materials=3 roles=4'),e))
 def test_preservation_deferred(self):self.reject(lambda x,l,e:(x,l+'\n[whole-dust-thermal-deferred] Titanium',e))
 def test_missing_detail(self):self.reject(lambda x,l,e:(x,l.replace('[slurry-recycling-row] Drying/tiny','[missing-detail] Drying/tiny'),e))
 def test_false_baseline(self):self.reject(lambda x,l,e:(x,l.replace('exact=false','exact=true'),e))
 def test_fixed_vector(self):self.reject(lambda x,l,e:(x,l.replace('exact=true','exact=false'),e),'fixed')

class SyntheticSemanticLog(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory(prefix='gt6-completion-checker-');self.addCleanup(self.tmp.cleanup)
  self.directory=Path(self.tmp.name);self.baseline=self.directory/'baseline.log'
  self.baseline.write_text(fixture('baseline')[1])

class DiffGuards(SyntheticSemanticLog):
 def compare(self,transform):
  with tempfile.TemporaryDirectory() as d:
   p=Path(d)/'fixed.log';p.write_text(transform(self.baseline.read_text()));return c.semantic_diff(self.baseline,p)
 def test_actual_self(self):self.assertTrue(self.compare(lambda s:s)['same_loader_semantic_comparison'])
 def test_allowed_new_fraction_only(self):
  self.compare(lambda s:s+'\n[ore-completion-detail] 220/dustSmall/gt.recipe.smelter new literal\n[ore-completion-census] 220/Titanium/dustSmall/gt.recipe.smelter accepted=1 selected=true dynamic=false addon=false quality=0 target=Titanium amount=648648000\n')
 def test_forbid_other_material(self):
  with self.assertRaises(ValueError):self.compare(lambda s:s+'\n[ore-completion-detail] 290/dustSmall/gt.recipe.smelter unapproved\n')
 def test_forbid_melter(self):
  with self.assertRaises(ValueError):self.compare(lambda s:s+'\n[ore-completion-detail] 220/dustSmall/gt.recipe.melter unapproved\n')
 def test_forbid_duplicate(self):
  with self.assertRaises(ValueError):self.compare(lambda s:s+'\n'+next(l for l in s.splitlines() if '[ore-completion-detail] ' in l)+'\n')
 def test_forbid_missing_detail(self):
  with self.assertRaises(ValueError):self.compare(lambda s:s.replace('[ore-completion-detail] ','[missing] ',1))

class FluidGuards(SyntheticSemanticLog):
 def run_diff(self,transform):
  with tempfile.TemporaryDirectory() as d:
   p=Path(d)/'fixed.log';p.write_text(transform(self.baseline.read_text()));return c.semantic_diff(self.baseline,p)
 def test_opaque_hash_only(self):self.run_diff(lambda s:re.sub(r'FluidStack@[a-f0-9]+','FluidStack@abc123',s))
 def test_changed_fluid_amount(self):
  with self.assertRaises(ValueError):self.run_diff(lambda s:s.replace('gt6ce:molten_titaniumx144','gt6ce:molten_titaniumx143'))
 def test_changed_fluid_identity(self):
  with self.assertRaises(ValueError):self.run_diff(lambda s:s.replace('gt6ce:molten_titaniumx144','gt6ce:molten_copperx144'))
 def test_missing_stable_proof(self):
  with self.assertRaises(ValueError):self.run_diff(lambda s:s.replace('[ore-completion-detail] 220/dust/gt.recipe.smelter','[missing] 220/dust/gt.recipe.smelter'))
 def test_duplicate_stable_proof(self):
  with self.assertRaises(ValueError):self.run_diff(lambda s:s+'\n'+next(l for l in s.splitlines() if '[ore-completion-detail] 220/dust/gt.recipe.smelter' in l))
 def test_changed_work(self):
  with self.assertRaises(ValueError):self.run_diff(lambda s:s.replace(';work=519/16;fluidInputs=[];fluidOutputs=[gt6ce:molten_titaniumx144]',';work=520/16;fluidInputs=[];fluidOutputs=[gt6ce:molten_titaniumx144]'))
 def test_neoforge_explicit_self(self):
  p=self.directory/'neoforge.log';p.write_text(fixture('baseline','neoforge')[1]);c.semantic_diff(p,p)

if __name__=='__main__':unittest.main()
