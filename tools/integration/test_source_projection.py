"""Focused regression for maintained Stonecutter branches used by read-only audits."""
import unittest

from check_relay_sharing import project


class SourceProjectionTests(unittest.TestCase):
    def test_masked_nested_comment_does_not_swallow_directives(self):
        source = """//? if >=1.21.1 {
/*/^* Neo's stack codec. ^/
int neo = 21;
*///?} else {
int forge = 20;
//?}
int shared = 1;
"""
        neo = project(source, 'neoforge')
        forge = project(source, 'forge')
        self.assertIn("/* Neo's stack codec. */", neo)
        self.assertIn('int neo = 21;', neo)
        self.assertNotIn('int forge', neo)
        self.assertIn('int forge = 20;', forge)
        self.assertNotIn('int neo', forge)
        self.assertIn('int shared = 1;', neo)
        self.assertIn('int shared = 1;', forge)

    def test_shared_comments_remain_untouched(self):
        source = '/* shared /^* marker ^/ */\nint shared = 1;\n'
        for platform in ('forge', 'neoforge'):
            self.assertEqual(source, project(source, platform))


if __name__ == '__main__':
    unittest.main()
