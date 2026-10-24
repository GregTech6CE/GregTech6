"""Numeric qual(long) import must retain tool metadata and source call ordering."""
from pathlib import Path
import sys
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from transpile_gt6_materials import parse_metadata, emit_metadata_suffix

class MaterialQualityImportTests(unittest.TestCase):
    def suffix(self, source):
        return emit_metadata_suffix("Fixture", parse_metadata(source), {})
    def test_single_argument_harvest_quality_is_not_dropped(self):
        self.assertEqual(".setToolQuality(2)", self.suffix("Bauxite = oredustdcmp(9105).qual(2).heat(2800)"))
    def test_zero_quality_remains_an_explicit_source_call(self):
        self.assertEqual(".setToolQuality(0)", self.suffix("ore().qual(0)"))
    def test_single_argument_preserves_preceding_full_tool_stats(self):
        self.assertEqual(".setToolStats(2, 4.5F, 128, 1).setToolQuality(3)",
                         self.suffix("ore().qual(2, 4.5, 128, 1).qual(3)"))
    def test_later_full_call_can_override_earlier_harvest_quality(self):
        self.assertEqual(".setToolQuality(3).setToolStats(1, 2F, 16, 1)",
                         self.suffix("ore().qual(3).qual(1, 2.0, 16, 1)"))
    def test_signed_literal_is_preserved_for_source_runtime_clamping(self):
        self.assertEqual(".setToolQuality(-1).setToolQuality(99)", self.suffix("ore().qual(-1L).qual(+99)"))
    def test_material_copy_overload_is_explicitly_unresolved_not_numeric(self):
        meta = parse_metadata("alloy().qual(Steel)")
        self.assertEqual(["Steel"], meta["unresolved_qual_calls"])
        self.assertEqual("", emit_metadata_suffix("Fixture", meta, {}))
    def test_no_quality_call_does_not_infer_zero_or_change_defaults(self):
        self.assertEqual("", self.suffix("oredustdcmp(9105).heat(2800)"))
    def test_existing_four_argument_and_programmatic_contract_stays_unchanged(self):
        expected = ".setToolStats(3, 8F, 256, 2)"
        self.assertEqual(expected, self.suffix("ore().qual(3, 8.0, 256, 2)"))
        self.assertEqual(expected, emit_metadata_suffix("Fixture", {"qual": (3, 8.0, 256, 2)}, {}))

if __name__ == "__main__":
    unittest.main()
