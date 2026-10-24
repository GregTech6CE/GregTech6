import json,tempfile,unittest
from pathlib import Path
import xml.etree.ElementTree as ET
from tools.integration.check_nonore_batch import check,compare,identities,CASES,NEG,NS,EXPECTED_FAILURES
class RawChecker(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory();self.addCleanup(self.t.cleanup);self.d=Path(self.t.name)
 def fixture(self,phase):
  x=self.d/(phase+'.xml');l=self.d/(phase+'.log');e=self.d/(phase+'.exit');r=ET.Element('testsuite')
  for n in CASES|({"waterkeysretainfluidoutputscopiesandexactownership"} if phase=="fixed" else set()):
   c=ET.SubElement(r,'testcase',name=n,classname=NS+':test_empty')
   if phase=='baseline' and n in EXPECTED_FAILURES:ET.SubElement(c,'failure',message=EXPECTED_FAILURES[n]+' rows')
  ET.ElementTree(r).write(x);e.write_text('1' if phase=='baseline' else '0');lines=[]
  for i,n in sorted(identities()):
   lines.append(f'[nonore-raw-classification] {i}/{n} overrides='+('[requested:crushed, requested:oreRaw]' if n=='Bakelite' else '[]'))
   lines.append(f'[nonore-raw-facts] {n} target={n} amount=648648000 multiplier=1/1 quality=0')
   for m in ['gt.recipe.crusher','gt.recipe.hammer']:
    active=phase=='baseline' or n=='Bakelite';v=str(active).lower();lines.append(f'[nonore-raw-row] {i}/{n} map={m} accepted={int(active)} selected={v} warm={v}')
    if phase=='baseline' and n in ['Wood','Snow','Ice','PVC']:lines.append(f'[nonore-raw-literal] {n}/{m} verified=true')
  lines += [f'[nonore-raw-preserved] control{i} recipe' for i in range(20)]+['[nonore-raw-preserved-dust] Wood/melter dust1']
  for i,n in sorted(identities()):
   for size in ['whole','tiny']:
    for w in ['Water','MnWtr','DistW','SpDew']:
     active=phase=='baseline' or n=='Bakelite'
     lines.append(f'[nonore-sluice-row] {n}/{size}/{w} accepted={int(active)} selected={str(active).lower()} noOverride={str(n!="Bakelite").lower()}')
  lines += [f'[nonore-sluice-preserved] control{i} recipe' for i in range(24)]
  cohort=json.loads((Path(__file__).parent/'fixtures/nonore-batch-magnetic-cohort.json').read_text())
  lines += [f'[ore-batch-magnetic] {identity}/{size} expected=[] accepted=0 selected=false deferred=false' for identity in cohort['identities'] for size in ['whole','tiny']]
  lines += ['[ore-batch-magnetic-cohort] sha256='+cohort['cohort_sha256']]
  lines += ['[ore-batch-magnetic-summary] materials=539 roles=1078 deferred=0 positives=84']
  for n in ['Titanium','Iron']:
   lines.append(f'[whole-dust-thermal-facts] {n} canonical=true ordinary=true unchanged=true rest')
   for m in ['Melter','Smelter']:lines.append(f'[whole-dust-thermal-census] {n}/{m} accepted=1')
  lines += [f'[ore-batch-packing] control{i} verified=true' for i in range(24)]
  l.write_text('\n'.join(lines));return x,l,e,phase
 def test_both_phases_and_cross_revision_multiset(self):
  a=self.fixture('baseline');b=self.fixture('fixed');self.assertEqual(check(*a)['raw_roles'],106);self.assertEqual(check(*b)['cases'],11);self.assertTrue(compare(a[1],b[1]))
 def test_fixture_error_or_wrong_case_not_expected_red(self):
  a=self.fixture('baseline');a[0].write_text(a[0].read_text().replace('source-NO fabricated raw Crusher/Hammer remain:','fixture absent'))
  with self.assertRaises(ValueError):check(*a)
  a=self.fixture('baseline');a[0].write_text(a[0].read_text().replace('test_empty','wrong'))
  with self.assertRaises(ValueError):check(*a)
 def test_missing_duplicate_and_contradictory_roles_rejected(self):
  for change in [lambda s:s.replace('[nonore-raw-row]','[missing]',1),lambda s:s+'\n'+next(l for l in s.splitlines() if '[nonore-raw-row]' in l),lambda s:s.replace('warm=false','warm=true',1)]:
   a=self.fixture('fixed');a[1].write_text(change(a[1].read_text()))
   with self.assertRaises(ValueError):check(*a)
 def test_requested_override_and_literal_or_preserved_evidence_required(self):
  for old in ['requested:oreRaw','[nonore-raw-literal]','[nonore-raw-preserved]','[nonore-raw-preserved-dust]']:
   a=self.fixture('baseline');a[1].write_text(a[1].read_text().replace(old,'missing',1))
   with self.assertRaises(ValueError):check(*a)
 def test_comparator_checks_duplicates_and_changes(self):
  a=self.fixture('baseline');b=self.fixture('fixed');b[1].write_text(b[1].read_text()+'\n[nonore-raw-preserved-dust] Wood/melter dust1')
  with self.assertRaises(ValueError):compare(a[1],b[1])

 def test_omitted_sluice_role_rejected(self):
  a=self.fixture('baseline');a[1].write_text(a[1].read_text().replace('[nonore-sluice-row]','[omitted]',1))
  with self.assertRaises(ValueError):check(*a)
 def test_duplicate_magnetic_role_rejected(self):
  a=self.fixture('baseline');a[1].write_text(a[1].read_text()+'\n[ore-batch-magnetic] 0/Material0/whole expected=[] accepted=0 selected=false deferred=false')
  with self.assertRaises(ValueError):check(*a)
 def test_thermal_deferred_is_not_policy_red(self):
  a=self.fixture('baseline');a[1].write_text(a[1].read_text()+'\n[whole-dust-thermal-deferred] Titanium/Melter')
  with self.assertRaises(ValueError):check(*a)
 def test_missing_named_failure_rejected(self):
  a=self.fixture('baseline');root=ET.parse(a[0]).getroot()
  for c in root:
   if c.get('name')=='wholedustthermalusessourcetargetpermissionandwork':c.remove(c.find('failure'))
  ET.ElementTree(root).write(a[0])
  with self.assertRaises(ValueError):check(*a)

 def test_same_count_wrong_material_identity_rejected(self):
  a=self.fixture('baseline');a[1].write_text(a[1].read_text().replace('8500/Stone/','8500/NotStone/'))
  with self.assertRaises(ValueError):check(*a)
