# Sediment ore harvest: source correction before physics promotion

The original candidate incorrectly equated preferred shovel with a mandatory tool. Its wrong-pick rejection assertion was an incorrect oracle, not a production target to preserve.

## Evidence
- Both source Loader_Ores constructors use Material.sand for sand, red-sand and gravel, normal/small. PrefixBlock_ forwards it; original PrefixBlock extends native Block and passes the material unchanged, with no canHarvestBlock override.
- Official Forge1.7.10 ForgeHooks.canHarvestBlock checks material tool-not-required before inspecting held tools: https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/src/main/java/net/minecraftforge/common/ForgeHooks.java (lines88–107). Official Block patch routes block strength/harvest through these hooks.
- The paired -w Material.java explicitly initializes toolNotRequired=true, adventureExempt=false; sand has no setters. Its PrefixBlock.mkProps197–200 and GT_API_Proxy1665–1676 explicitly preserve the material-based no-tool rule.
- Independently downloaded Mojang1.7.10 client is SHA1-verified against official version metadata; bytecode evidence is preserved ignored under primary-1.7.10. Registry literal sand constructs anh→akx, whose constructor passes Material awt.p. That field uses plain constructor, whose M=true and O=false fields are exposed by l/q getters. This corroborates the separate no-tool/adventure flags without executing downloaded code. MCP mapping download403 is recorded; no proof depends on it.
- Original MultiItemTool.getDigSpeed469–483 separately enforces usability, quality and effective category for mining speed. Allowing drops must not accidentally grant GT pickaxe speed on sediment.
- Source mud and smallmud use Material.ground, also no-tool. Native OreHostStone has no mud identity; no speculative mapping/registration is added. Three sediment BROKEN modes remain outside the reviewed table.

## Corrected bounded behavior
Only the six already-reviewed Material.sand normal/small tuples report requiresToolForDrops=false. The existing ownership/state admission rules still exclude unknown hosts/modes, foreign/addon-owned/noncanonical blocks and EXPLOSIVE material states. The actual BlockState getter, player drop decision and tool effectiveness agree: no required tool for drops; preferred shovel speed remains separate. Custom tool/component speed remains untouched; the source block-material no-tool rule applies independently of whatever item is held, as in Forge's early return.

The GT speed predicate remains gated by category, tier and power/usability. Hardness, resistance, 75 numeric tuples, registered IDs, fluid movement and existing unknown-state fallback are unchanged.

## Intentional modern UI distinction
Old LH tests adventureExempt, not toolNotRequired; sand's old LH branch therefore advertises a tool/tier even though drops allow bare hands. As an intentional modern UI correction, only these six proven modes now show accurate hand-harvestable/preferred-shovel text. This is NOT byte-identical old LH tooltip parity. A new localized optional-tool key is supplied in both en_us and zh_cn, with placeholder/localization guards; no global tooltip policy is rewritten.

Native assertions retain seven case IDs. Existing harvest case checks the actual requiresCorrectToolForDrops flag first, then hard-rock restrictions; includes all six sediment mode combinations with hand/wrong/under-tier tools, GT wrong-tool drops vs ineffective mining, hand progress1/30 at source hardness1, and existing power/custom-component controls. Tooltip case asserts localized optional shovel and no mandatory tier, with idempotence. Existing wrong-shovel-required wording/assertion is explicitly corrected rather than weakened.

Paired cached compilation and pure core checks remain separate evidence. The revised V2 unchanged-production baseline reproduced six semantic failures per loader (62 selected,56 passed), including the actual no-tool state flag first. The subsequent full local663 retry **passed**: Forge336/336 in8m50s and NeoForge327/327 in4m32s, native/strict exits0 and no failures/errors/skips. Actual969 quality/421 physics assertions,182 Python tests,491 REMI groups and packaged mixin/refmap guards passed; input/final snapshots matched.

The first fixed attempt had stopped before native execution on localization provenance. The exact four-file `dc8b67cfd57c6ecdbd25ba60e50f486f5509e6d2b2e1a66b76399fdaee1f54da` correction records the new optional-tool Chinese text as project-authored, not TeamNED source text, without changing original source snapshots or either language resource. The fresh retry is green; this does not erase the source-oracle correction above.

See [full quality/physics audit](ORE_QUALITY_HOST_PHYSICS_AUDIT.md) and local `work/validation/ore-quality-physics-fixed-retry/local-validation-receipt.md` for XML hashes and chronology. Remote CI and merge remain unclaimed.
