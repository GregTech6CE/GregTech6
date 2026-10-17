"""Regression: blockstate `model` fields silently produce empty item models."""
import json
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
CORE = ROOT / "core/src/main/resources/assets/gregtech"


class GearboxItemRoots(unittest.TestCase):
    def test_registered_families_inherit_drawable_geometry(self):
        catalog = (ROOT / "core/src/main/java/com/gregtech/gregtech/content/energy/GearboxCatalog.java").read_text()
        tiers = re.findall(r'new Tier\("([^"]+)"', catalog)
        for platform in ("src", "neoforge/src"):
            assets = ROOT / platform / "main/resources/assets/gregtech"

            def geometry(model_id):
                relative = Path("models") / (model_id.removeprefix("gregtech:") + ".json")
                file = next(p for p in (assets / relative, CORE / relative) if p.is_file())
                model = json.loads(file.read_text())
                self.assertNotIn("model", model, str(file))
                if model.get("elements"):
                    return sum(len(element.get("faces", {})) for element in model["elements"])
                parts = model.get("children", model.get("parts", {}))
                if parts:
                    return sum(len(element.get("faces", {})) for part in parts.values()
                               for element in part.get("elements", []))
                return geometry(model["parent"])

            for family in ("gearbox", "rotation_transformer"):
                for tier in tiers:
                    with self.subTest(platform=platform, family=family, tier=tier):
                        self.assertGreaterEqual(geometry(f"gregtech:item/{family}_{tier}"), 6)


if __name__ == "__main__":
    unittest.main()
