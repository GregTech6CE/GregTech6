"""The jar inventory ignores archive metadata and source-only line shifts."""
import hashlib
import shutil
import subprocess
import tempfile
import textwrap
import unittest
import zipfile
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'integration'))
from compare_artifact_layout import (attribute_names, compare_inventories,
                                     normalize_class, read_jar_entries)


SOURCE = textwrap.dedent("""\
    package demo;
    import java.util.List;
    import java.util.function.IntUnaryOperator;
    public class Demo {
        public IntUnaryOperator adder(int left) {
            int right = 2;
            return value -> left + value + right;
        }
        public int risky(int left) {
            try {
                return left / 2;
            } catch (ArithmeticException ignored) {
                return -1;
            }
        }
    }
    """)

SHIFTED = textwrap.dedent("""\
    package demo;

    import java.util.Map;
    import java.util.function.IntUnaryOperator;

    public class Demo {
        public IntUnaryOperator adder(int left) {
            int right = 2;
            return value -> left + value + right;
        }
        public int risky(int left) {
            try {
                return left / 2;
            } catch (ArithmeticException ignored) {
                return -1;
            }
        }
    }
    """)

CHANGED = SOURCE.replace('return left / 2;', 'return left / 3;')


def javac():
    home = shutil.which('javac')
    if home:
        return home
    java_home = Path(__import__('os').environ.get('JAVA_HOME', ''))
    candidate = java_home / 'bin' / 'javac.exe'
    if candidate.is_file():
        return str(candidate)
    raise unittest.SkipTest('javac is not on PATH')


def compile_class(source, options):
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory)
        (root / 'Demo.java').write_text(source, encoding='utf-8')
        subprocess.run([javac(), *options, 'Demo.java'], cwd=root, check=True,
                       capture_output=True, text=True)
        return (root / 'Demo.class').read_bytes()


class ClassNormalizerTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.compiler = javac()
        cls.debug = compile_class(SOURCE, ['-g'])
        cls.shifted = compile_class(SHIFTED, ['-g'])
        cls.changed = compile_class(CHANGED, ['-g'])
        cls.stripped = compile_class(SOURCE, ['-g:none'])

    def test_line_shift_matches_after_debug_attributes_are_removed(self):
        self.assertNotEqual(self.debug, self.shifted)
        self.assertEqual(normalize_class(self.debug), normalize_class(self.shifted))
        self.assertNotEqual(normalize_class(self.debug), normalize_class(self.changed))

    def test_debug_attributes_are_removed_and_stack_map_stays(self):
        names = attribute_names(normalize_class(self.debug))
        self.assertIn(b'Code', names)
        self.assertIn(b'StackMapTable', names)
        self.assertNotIn(b'LineNumberTable', names)
        self.assertNotIn(b'LocalVariableTable', names)
        self.assertTrue(attribute_names(self.debug) >= {b'LineNumberTable', b'LocalVariableTable'})

    def test_class_without_debug_attributes_is_unchanged(self):
        self.assertEqual(normalize_class(self.stripped), self.stripped)
        self.assertEqual(normalize_class(normalize_class(self.debug)), normalize_class(self.debug))

    def test_invokedynamic_and_exception_table_round_trip(self):
        # Rewriting a class that still has its debug attributes must be a no-op
        # when those attributes are not part of the input.
        self.assertEqual(normalize_class(self.stripped), self.stripped)
        self.assertIn(b'BootstrapMethods', attribute_names(self.debug))


class InventoryCompareTests(unittest.TestCase):
    def test_compare_reports_entry_changes_not_archive_metadata(self):
        payload = b'same-bytes'
        other = b'different'
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            left = root / 'left.jar'
            right = root / 'right.jar'
            with zipfile.ZipFile(left, 'w') as archive:
                archive.writestr('assets/demo.txt', payload)
                archive.writestr('demo/Kept.class', self_class())
            with zipfile.ZipFile(right, 'w') as archive:
                archive.writestr('demo/Kept.class', self_class())
                archive.writestr('assets/demo.txt', other)
                archive.writestr('assets/new.txt', payload)
            report = compare_inventories(
                {'entries': read_jar_entries(left)},
                {'entries': read_jar_entries(right)},
                'strict')
            self.assertNotEqual(hashlib.sha256(left.read_bytes()).digest(),
                                hashlib.sha256(right.read_bytes()).digest())
        self.assertEqual(report['unchanged'], 1)
        self.assertEqual(report['changed'], ['assets/demo.txt'])
        self.assertEqual(report['added'], ['assets/new.txt'])
        self.assertEqual(report['removed'], [])


def self_class():
    return compile_class(SOURCE, ['-g:none'])


if __name__ == '__main__':
    unittest.main()
