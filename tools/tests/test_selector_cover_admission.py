"""Source-only guards for registered selector admission; native GameTests prove behavior."""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class SelectorCoverAdmissionSourceTests(unittest.TestCase):
    def test_manual_and_button_selectors_have_explicit_admission(self):
        source = (ROOT / "multiversion/src/main/java/com/gregtech/gregtech/content/cover/CoverItems.java").read_text()
        body = source.split("private static boolean portCoverId(String id)", 1)[1].split("/**", 1)[0]
        for selector in ("manual_selector", "button_panel_selector"):
            self.assertIn(f'id.equals("{selector}")', body)
        # Do not turn the exact fix into blanket acceptance of arbitrary lookalikes.
        self.assertNotRegex(body, r'id\.endsWith\("_?selector"\)')
        self.assertNotRegex(body, r'\|\|\s*id\.contains\("selector"\)\s*[;}]')

    def test_admitted_ids_are_registered_selector_panels(self):
        panels = (ROOT / "multiversion/src/main/java/com/gregtech/gregtech/content/cover/PanelCover.java").read_text()
        definitions = (ROOT / "core/src/main/java/com/gregtech/gregtech/content/recipe/TechnologicalItemDefinitions.java").read_text()
        for kind, selector in (("MANUAL", "manual_selector"), ("BUTTONS", "button_panel_selector")):
            self.assertIn(f'{kind}("{selector}")', panels)
            self.assertRegex(definitions, rf'put\("{re.escape(selector)}",')


if __name__ == "__main__":
    unittest.main()
