package gregtech.content.recipe.generated;

import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefixes;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialBlockDefinitions;
import gregtech.engine.prefix.BlockMaterialPrefix;

import java.util.List;

import static gregtech.content.recipe.SourceCentrifugeBlockDustInputs.*;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** A physical block holder is never substituted by nine loose dust items. */
final class BlockDustCentrifugeContracts {
    private BlockDustCentrifugeContracts() {}

    static void run() {
        List<Binding> expectedBindings = List.of(
                new Binding("Bedrock", "Bedrock", 8599),
                new Binding("Netherrack", "Netherrack", 8502),
                new Binding("Endstone", "Endstone", 8504),
                new Binding("EndSandWhite", "WhiteEndSand", 8105),
                new Binding("EndSandBlack", "BlackEndSand", 8106),
                new Binding("SpaceRock", "SpaceStone", 8512),
                new Binding("MoonTurf", "MoonTurf", 8514),
                new Binding("MoonRock", "MoonStone", 8513));
        equal(expectedBindings, bindings(), "finite source identities and material IDs");
        for (Binding binding : expectedBindings) {
            equal(binding, binding(binding.sourceField()), "exact field lookup");
            equal(Decision.PRESENT, decide(binding, new Facts(true, true, true, true, true,
                    binding.materialName(), binding.materialId())), "reviewed physical holder");
            equal(Decision.UNRESOLVED, decide(binding, new Facts(false, true, true, true, true,
                    binding.materialName(), binding.materialId())), "missing holder must not become dust9");
            equal(Decision.UNRESOLVED, decide(binding, null), "missing native observations");
            for (int bad = 0; bad < 6; bad++) {
                Facts facts = new Facts(true, bad != 0, bad != 1, bad != 2, bad != 3,
                        bad == 4 ? "other" : binding.materialName(), bad == 5 ? -1 : binding.materialId());
                equal(Decision.UNREVIEWED, decide(binding, facts), "reject mismatched native fact " + bad);
            }
        }
        equal(null, binding("MoonStone"), "native material spelling is not a source field");
        equal(Decision.UNRESOLVED, decide(new Binding("foreign", "other", 123),
                new Facts(true, true, true, true, true, "other", 123)), "caller cannot invent a binding");
        sourceRows();
        physicalDefinitions();
    }

    private static void sourceRows() {
        List<String> gases = List.of("mu:He:gas:81081000:down", "mu:He_3:gas:11675664:down",
                "mu:NO2:gas:162162000:down", "mu:CO2:gas:162162000:down",
                "mu:H:gas:81081000:down", "mu:D:gas:81081000:down");
        List<Row> expected = List.of(
                centrifuge("cbd:Bedrock:1", 256, 1296, List.of(7000L, 3000L, 3000L, 3000L, 3000L, 3000L),
                        List.of(), "i:dust:Deepslate:9", "i:dustTiny:Adamantite:9", "i:dustTiny:Draconium:9",
                        "i:dustTiny:Monazite:18", "i:dustTiny:Zircon:27", "i:dustTiny:Graphite:27"),
                centrifuge("cbd:Netherrack:1", 64, 144, List.of(8000L, 1000L, 500L, 500L, 100L),
                        List.of(), "i:dust:Stone:9", "i:dustTiny:S:18", "i:dustTiny:Redstone:9",
                        "i:dustTiny:Coal:9", "i:dustTiny:Au:9"),
                endStone("Endstone", gases),
                endStone("EndSandWhite", gases),
                endStone("EndSandBlack", gases),
                centrifuge("cbd:SpaceRock:1", 64, 144, List.of(8000L, 100L, 100L), gases,
                        "i:dust:Sand:9", "i:dustTiny:Pt:9", "i:dustTiny:MeteoricIron:9"),
                centrifuge("cbd:MoonTurf:1", 64, 288, List.of(8000L, 1000L, 1000L, 1000L, 1000L, 100L),
                        gases, "i:dust:Sand:9", "i:dustTiny:Basalt:9", "i:dustTiny:Al2O3:9",
                        "i:dustTiny:Ilmenite:9", "i:dustTiny:MnO2:9", "i:dustTiny:MeteoricIron:9"),
                centrifuge("cbd:MoonRock:1", 64, 144, List.of(8000L, 500L, 400L, 300L, 100L, 50L),
                        List.of(), "i:dust:Basalt:9", "i:dustTiny:SiO2:9", "i:dustTiny:Al2O3:9",
                        "i:dustTiny:Ilmenite:9", "i:dustTiny:RareEarth:9", "i:dustTiny:Pt:9"));
        List<Row> rows = collect(GTOresGen::load);
        equal(expected, rows.stream().filter(r -> r.inputs().stream().anyMatch(s -> s.startsWith("cbd:"))).toList(),
                "eight source-exact ordered block rows, including chance/fluid slots");
        for (Row row : rows) {
            for (List<String> slots : List.of(row.outputs(), row.fluidInputs(), row.fluidOutputs())) {
                check(slots.stream().noneMatch(s -> s.startsWith("cbd:")), "physical-block token is input-only");
            }
            if (row.map().equals("Centrifuge")) {
                check(row.inputs().stream().noneMatch(s -> s.startsWith("i:blockDust:")),
                        "no legacy blockDust token may normalize to dust9");
            }
        }
    }

    private static void physicalDefinitions() {
        GTMaterialRegistry.init();
        MaterialPrefixes.bootstrap();
        var prefix = BlockMaterialPrefix.blockDust;
        var definitions = MaterialBlockDefinitions.all().stream()
                .filter(d -> d.prefix() == prefix).toList();
        equal(1000, definitions.size(), "restored blockDust directory");
        equal(71L, definitions.stream().filter(d -> d.material().has(MaterialProperty.STONE)).count(),
                "71 stone-family additions; the original 929 non-stone IDs remain");
        for (Binding binding : bindings()) {
            var material = GTMaterialRegistry.get(binding.sourceField()).resolve();
            equal(binding.materialName(), material.getName(), "source alias resolves to exact material");
            equal(binding.materialId(), material.getId(), "source numeric material identity");
            var matches = definitions.stream().filter(d -> d.material() == material).toList();
            equal(1, matches.size(), "one physical block holder per strict binding");
            equal("block_dust_" + material.getName().toLowerCase(java.util.Locale.ROOT),
                    matches.get(0).blockId(), "unchanged native block registry convention");
        }
        for (String name : List.of("Stone", "Sand", "RedSand")) {
            check(!prefix.isValidFor(GTMaterialRegistry.get(name)), "retain exact source exclusion " + name);
        }
        // These three port IDs already exist in saves, even though original special sand
        // blocks use their own recipe identities. Do not remove the legacy holders.
        for (String name : List.of("Magnetite", "BasalticMineralSand", "GraniticMineralSand")) {
            check(definitions.stream().anyMatch(d -> d.blockId().equals("block_dust_"
                    + name.toLowerCase(java.util.Locale.ROOT))), "preserve legacy blockDust ID " + name);
        }
        equal(GTValues.U * 9, prefix.getMaterialWeight(), "one physical block is nine material units");
        equal(0.5F, prefix.baseHardness(), "dust block base hardness");
        equal(4.5F, prefix.baseResistance(), "dust block base resistance");
        equal(-2, prefix.harvestLevelOffset(), "dust block harvest offset");
        equal(BlockMaterialPrefix.MapColor.SAND, prefix.mapColor(), "dust block map color");
        equal(BlockMaterialPrefix.SoundType.SAND, prefix.soundType(), "dust block sound");
        check(prefix.falling(), "dust blocks retain their falling behavior");
    }

    private static Row endStone(String field, List<String> gases) {
        return centrifuge("cbd:" + field + ":1", 64, 144, List.of(8000L, 100L, 300L, 100L, 10L),
                gases, "i:dust:Sand:9", "i:dustTiny:Pt:9", "i:dustTiny:WO3:9",
                "i:dustTiny:Endium:9", "i:gem:Sugilite:9");
    }
}
