import unittest,tempfile,json
from pathlib import Path
from tools.integration.compare_nonore_dust_snapshots import compare,PREFIX
class DustComparison(unittest.TestCase):
 def test_exact_rows_and_duplicates_preserved(self):
  with tempfile.TemporaryDirectory() as td:
   a,b=Path(td)/'a',Path(td)/'b';a.write_text(PREFIX+json.dumps(['x amount1','y amount2','x amount1']));b.write_text(PREFIX+json.dumps(['y amount2','x amount1','x amount1']));self.assertEqual(compare(a,b)['matched_dust_rows'],3)
 def test_mutations_fail(self):
  with tempfile.TemporaryDirectory() as td:
   a,b=Path(td)/'a',Path(td)/'b';a.write_text(PREFIX+json.dumps(['waterx1000 work16','dustx1 chance10000']))
   for rows in [['waterx999 work16','dustx1 chance10000'],['waterx1000 work17','dustx1 chance10000'],['waterx1000 work16','dustx2 chance10000'],['waterx1000 work16','dustx1 chance9999'],['waterx1000 work16'],['waterx1000 work16','dustx1 chance10000','dustx1 chance10000']]:
    with self.subTest(rows=rows):
     b.write_text(PREFIX+json.dumps(rows))
     with self.assertRaises(ValueError):compare(a,b)
 def test_missing_duplicate_invalid_snapshots_fail(self):
  with tempfile.TemporaryDirectory() as td:
   a,b=Path(td)/'a',Path(td)/'b';a.write_text(PREFIX+json.dumps(['valid']))
   for text in ['',PREFIX+'[]',PREFIX+'{}',PREFIX+'[null]',PREFIX+'[""]',PREFIX+'["valid"]\n'+PREFIX+'["valid"]']:
    with self.subTest(text=text):
     b.write_text(text)
     with self.assertRaises(ValueError):compare(a,b)
if __name__=='__main__':unittest.main()
