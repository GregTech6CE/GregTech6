"""The accepted source, not a hand-written expected translation, is authoritative."""
import sys
import re
import unittest
import tempfile
import json
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from localization import ROOT, LANG_PATH, CONFIG_PATH, read_json, synchronize, check_english


class StandardChineseTests(unittest.TestCase):
    def test_import_preflight_checks_proposed_values_without_changing_accepted_files(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'work') as folder:
            repo=Path(folder);config=repo/CONFIG_PATH;config.mkdir(parents=True)
            accepted=config/'english_source.json'
            accepted.write_text('{"values": {}}',encoding='utf-8')
            before=accepted.read_bytes()
            key='tooltip.gregtech.example'
            english={key:'Original %s'}
            aliases={key:'gt.original'}
            source={'gt.original':'原文 %s'}
            proposal={key:{'source_key':'gt.original','value':'Original %s'}}
            self.assertEqual(check_english(repo,english,{key:'原文 %s'},aliases,source,pinned=proposal)
                             ['exact_original_declarations'],1)
            for en,zh in [({key:'Wrong %s'},{key:'原文 %s'}),
                          (english,{key:'丢失参数'}),
                          ({key:'Original %s', 'item.gregtech.empty':''},
                           {key:'原文 %s', 'item.gregtech.empty':''})]:
                with self.subTest(en=en,zh=zh),self.assertRaises(ValueError):
                    check_english(repo,en,zh,aliases,source,pinned=proposal)
            self.assertEqual(accepted.read_bytes(),before)

    def test_english_source_check_does_not_require_a_missing_chinese_translation(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'work') as folder:
            repo=Path(folder);config=repo/CONFIG_PATH;config.mkdir(parents=True)
            key='block.gregtech.example';original='gt.multitileentity.1'
            row={'source_key':original,'value':'Original Name','chinese_source_missing':True}
            (config/'english_source.json').write_text(json.dumps({'values':{key:row}}),encoding='utf-8')
            english={key:'Original Name'}
            result=check_english(repo,english,english,{}, {})
            self.assertEqual(result['original_english_without_chinese_source'],1)
            # The exception cannot overwrite an available original translation, conceal
            # a competing identity, or permit the English value to drift.
            for en,zh,aliases,source in [
                (english,english,{}, {original:'原名'}),
                (english,english,{key:'gt.multitileentity.2'}, {}),
                ({key:'Invented Name'},{key:'Invented Name'}, {}, {}),
                (english,{key:'自行翻译'}, {}, {})]:
                with self.subTest(en=en,zh=zh,aliases=aliases,source=source),self.assertRaises(ValueError):
                    check_english(repo,en,zh,aliases,source)
    def test_all_values_match_pinned_source_or_exact_english_fallback(self):
        report = synchronize()
        self.assertGreater(report['source_keys'], 100000)
        self.assertGreater(report['native_aliases'], 6000)

    def test_sensor_and_canvas_bindings_keep_original_identity(self):
        aliases = read_json(ROOT / CONFIG_PATH / 'aliases.json')
        self.assertEqual(aliases['block.gregtech.sensor_electrometer'], 'gt.multitileentity.31015')
        self.assertEqual(aliases['item.gregtech.canvas_white.tooltip'], 'gt.multiitem.randomtools.7045.tooltip')
        # XV has no original family registration; do not manufacture a translated tier.
        english = read_json(ROOT / LANG_PATH / 'en_us.json')
        chinese = read_json(ROOT / LANG_PATH / 'zh_cn.json')
        key = 'item.gregtech.compact_electric_conveyor_xv'
        self.assertNotIn(key, aliases)
        self.assertEqual(chinese[key], english[key])

    def test_bumble_species_and_scanned_states_keep_original_numeric_identity(self):
        aliases = read_json(ROOT / CONFIG_PATH / 'aliases.json')
        data = (ROOT / 'core/src/main/java/com/gregtech/gregtech/content/bumble/GTBumbleSpecies.java').read_text(encoding='utf-8')
        species = re.findall(r'new Species\((\d+),\s*"([^"]+)"', data)
        # Original MultiItemBumbles.make: four base metas and their +5 scanned counterparts.
        states = {'drone': 0, 'princess': 1, 'queen': 2, 'dead': 4,
                  'scanned_drone': 5, 'scanned_princess': 6, 'scanned_queen': 7, 'scanned_dead': 9}
        self.assertEqual(len(species), 80)
        registrations = (ROOT / 'core/src/main/java/com/gregtech/gregtech/registry/GTMultiItemsGen.java').read_text(encoding='utf-8')
        registered = set(re.findall(r'"([^"]+)",\s*"[^"]+",\s*"bumblebee"', registrations))
        self.assertEqual(registered, {prefix + '_' + state for _, prefix in species for state in states},
                         'Regeneration must not drop the scanned bee items')
        for ident, prefix in species:
            for state, meta in states.items():
                key = 'item.gregtech.' + prefix + '_' + state
                original = 'gt.multiitem.bumblebee.' + str(int(ident) + meta)
                self.assertEqual(aliases.get(key), original, key)
                self.assertEqual(aliases.get(key + '.tooltip'), original + '.tooltip', key)

    def test_anvil_material_symbols_do_not_shift_original_registrations(self):
        aliases = read_json(ROOT / CONFIG_PATH / 'aliases.json')
        # Source Loader_MultiTileEntities registrations, independent of English word order.
        for native, ident in {'anvil': 32031, 'anvil_lead': 32050, 'anvil_arsenicbronze': 32107,
                              'anvil_tungsten': 32045, 'anvil_hslatungstenalloy': 32091,
                              'anvil_draconiumawakened': 32068}.items():
            self.assertEqual(aliases['block.gregtech.' + native], 'gt.multitileentity.' + str(ident))
