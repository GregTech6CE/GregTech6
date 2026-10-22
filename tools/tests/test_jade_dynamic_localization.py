"""Source regressions for client-localized Jade payloads (not a native/runtime test)."""
import json
import re
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/integration'))
from check_relay_sharing import project

JADE = ROOT / 'multiversion/src/main/java/com/gregtech/gregtech/integration/jade'


class JadeDynamicLocalization(unittest.TestCase):
    def source(self, name, platform):
        return project((JADE / (name + '.java')).read_text(encoding='utf-8'), platform)

    def test_machine_names_are_client_components_with_legacy_payload(self):
        for platform in ('forge', 'neoforge'):
            for name in ('BasicMachineJadeProvider', 'EngineJadeProvider'):
                with self.subTest(platform=platform, provider=name):
                    source = self.source(name, platform)
                    self.assertIn('getDescriptionId()', source)
                    self.assertIn('tag.putString(NBT_NAME, ', source)
                    self.assertIn('Component name = JadeText.translated(data, NBT_NAME + "_key", NBT_NAME)', source)

    def test_fluid_payload_roundtrip_uses_matching_loader_and_client_name(self):
        for platform, save, load, wrong in (
                ('forge', 'fs.writeToNBT(new CompoundTag())', 'FluidStack.loadFluidStackFromNBT', 'net.neoforged'),
                ('neoforge', 'fs.save(accessor.getLevel().registryAccess())', 'FluidStack.parseOptional', 'net.minecraftforge')):
            with self.subTest(platform=platform):
                source = self.source('BasicMachineJadeProvider', platform)
                self.assertEqual(source.count('ft.put("stack", ' + save + ')'), 2)
                self.assertIn(load, source)
                self.assertNotIn(wrong, source)
                client = source.split('public void appendTooltip', 1)[1]
                self.assertIn('if (!fluid.isEmpty()) fName = fluid.getDisplayName()', client)
                self.assertIn('Component.literal(ft.getString("name"))', client)
                self.assertNotIn('.getString()', client)
                for field in ('amount', 'cap'):
                    self.assertIn('ft.get' + ('Int' if field == 'amount' else 'Long') + '("' + field + '")', client)

    def test_mold_material_and_shape_are_keys_with_fallbacks(self):
        for platform in ('forge', 'neoforge'):
            source = self.source('MoldJadeProvider', platform)
            self.assertIn('material.getTranslationKey()', source)
            for prefix in ('mp', 'bp'):
                self.assertIn('"itemGroup.gt6ce." + ' + prefix + '.getRegistryName()', source)
            for field in ('NBT_MATERIAL', 'NBT_MOLD_OUTPUT'):
                self.assertIn(f'JadeText.translated(data, {field} + "_key", {field})', source)
            self.assertNotIn('getLocalName()', source)
            self.assertIn('mp == MaterialPrefix.billet', source)
            self.assertIn('jade.gt6ce.mold.shape.billet', source)

    def test_rock_material_is_not_a_literal_local_name(self):
        for platform in ('forge', 'neoforge'):
            source = self.source('RockTwigJadeProvider', platform)
            self.assertIn('MaterialPresentation.name(material.resolve())', source)
            self.assertNotIn('getLocalName()', source)

    def test_missing_keys_and_old_server_payload_have_fallback(self):
        source = self.source('JadeText', 'forge')
        self.assertIn('key.isEmpty() ? Component.literal(fallback)', source)
        self.assertIn('Component.translatableWithFallback(key, fallback)', source)
        self.assertIn('Component.translatableWithFallback("jade.gt6ce.direction." + direction, direction)', source)

    def test_reactor_directions_and_steam_are_localized_without_unit_changes(self):
        for platform in ('forge', 'neoforge'):
            reactor = self.source('ReactorJadeProvider', platform)
            for side in ('hot', 'cold'):
                self.assertIn(f'JadeText.direction(d.getString("gt_reactor_{side}_side"))', reactor)
            engine = self.source('EngineJadeProvider', platform)
            self.assertIn('Component.translatable("jade.gt6ce.engine.input.steam") : Component.literal(inUnit)', engine)
            self.assertIn('format(outRate), format(stored), format(capacity), pct, inUnit', engine)

    def test_large_fluid_amount_percentage_promotes_before_multiplication(self):
        for platform in ('forge', 'neoforge'):
            source = self.source('BasicMachineJadeProvider', platform)
            self.assertIn('amount * 100L / cap', source)
            self.assertNotIn('amount * 100 / cap', source)

    def test_shipped_machine_block_keys_and_mold_forms_are_translated(self):
        catalog = (ROOT / 'core/src/main/java/com/gregtech/gregtech/content/machine/BasicMachineCatalog.java').read_text()
        names = set(re.findall(r'new MachineDef\("([^"]+)"', catalog))
        ids = set()
        for base in (ROOT / 'core/src/main/resources', ROOT / 'multiversion/src/main/resources',
                     ROOT / 'multiversion/versions/1.20.1-forge/src/main/resources',
                     ROOT / 'multiversion/versions/1.21.1-neoforge/src/main/resources'):
            ids.update(p.stem for p in (base / 'assets/gt6ce/blockstates').glob('*.json')
                       if p.stem.startswith('engine_') or any(p.stem.startswith(n + '_') for n in names))
        self.assertGreater(len(ids), 250)
        shapes = (ROOT / 'core/src/main/java/com/gregtech/gregtech/api/machine/crucible/MoldShapes.java').read_text()
        prefixes = set(re.findall(r'MaterialPrefix\.(\w+)', shapes))
        forms = {'block_solid'} | {re.sub(r'[A-Z]', lambda m: '_' + m[0].lower(), p) for p in prefixes if p != 'billet'}
        keys = {'block.gt6ce.' + i for i in ids} | {'itemGroup.gt6ce.' + form for form in forms}
        for locale in ('en_us', 'zh_cn'):
            language = json.loads((ROOT / f'core/src/main/resources/assets/gt6ce/lang/{locale}.json').read_text())
            for key in keys:
                with self.subTest(locale=locale, key=key):
                    self.assertTrue(language.get(key), key)
                    if locale == 'zh_cn':
                        self.assertRegex(language[key], r'[\u3400-\u9fff]')

    def test_dynamic_keys_have_both_languages(self):
        keys = ['jade.gt6ce.direction.' + side for side in ('down', 'up', 'north', 'south', 'west', 'east')]
        keys.extend(('jade.gt6ce.engine.input.steam', 'jade.gt6ce.mold.shape.billet'))
        for locale in ('en_us', 'zh_cn'):
            language = json.loads((ROOT / f'core/src/main/resources/assets/gt6ce/lang/{locale}.json').read_text())
            for key in keys:
                with self.subTest(locale=locale, key=key):
                    self.assertTrue(language.get(key))
                    if locale == 'zh_cn':
                        self.assertRegex(language[key], r'[\u3400-\u9fff]')


if __name__ == '__main__':
    unittest.main()
