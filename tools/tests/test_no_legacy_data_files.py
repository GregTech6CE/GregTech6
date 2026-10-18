
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class LegacyDataCleanupTests(unittest.TestCase):
    def test_old_facade_files_are_removed(self):
        data = native_path(ROOT / 'src/main/java/com/gregtech/gregtech/data')
        for name in 'AM ANY BI CS FL FM IL LH MD MT OD OP RM TC TD'.split():
            self.assertFalse((data / f'{name}.java').exists(), name)
        self.assertTrue((data / 'ImportedMaterialData.java').exists())
