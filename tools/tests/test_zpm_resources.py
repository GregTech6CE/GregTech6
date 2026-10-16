from pathlib import Path
SHARED_ASSETS = Path(__file__).resolve().parents[2] / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
import json
import unittest
from tools.restore_laser_models import ASSETS
from tools.check_render_resources import check


class ZpmResources(unittest.TestCase):
    def test_all_discharger_states_and_module_models_resolve(self):
        paths = [SHARED_ASSETS / 'blockstates/zpm.json']
        for name in ['basic', 'advanced', 'elite']:
            path = SHARED_ASSETS / f'blockstates/zpm_discharger_{name}.json'
            paths.append(path)
            model = json.loads(path.read_text(encoding='utf8'))
            self.assertEqual(24, len(model['variants']))
        issues, _ = check(paths)
        self.assertEqual([], issues)
