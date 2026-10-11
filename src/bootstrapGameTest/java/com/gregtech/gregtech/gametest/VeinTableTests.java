package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.worldgen.GTOreBlockResolver;
import com.gregtech.gregtech.worldgen.GTOreVeins;
import com.gregtech.gregtech.worldgen.GTStoneLayersGen;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * §51: GT6's large ore veins ({@code WorldgenOresLarge}, {@code Loader_Worldgen:885-924}) against
 * the port's tables — order, parameters, and whether every named material can actually be placed.
 *
 * <p>The five veins added in §51 ({@code lignite}, {@code coal}, {@code bauxite},
 * {@code iodinesalt}, {@code rocksalt}) had been skipped early on with the note "those resources
 * generate as whole-block seam layers instead" — but GT6 has both: the same resources also appear as
 * whole-block seams in its stone layers, which the port keeps in {@link GTStoneLayersGen}.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class VeinTableTests {

    /** GT6's {@code ORE_OVERWORLD} veins, in GT6 registration order. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void overworldVeinsMatchGt6(GameTestHelper helper) {
        List<String> expected = List.of(
                "lignite", "coal", "apatite", "lapis", "bauxite", "iodinesalt", "rocksalt", "asbestos",
                "sapphire", "sapphire2", "garnet", "pitchblende", "monazite", "diamond", "galena",
                "quartz", "peridot", "gold", "platinum", "molybdenum", "cassiterite", "tungstate",
                "manganese", "beryllium", "beryllium2", "titanium", "nickel", "redstone",
                "tetrahedrite", "iron", "copper");
        List<String> actual = GTOreVeins.OVERWORLD_VEINS.stream().map(GTOreVeins.OreVein::name).toList();
        helper.assertTrue(actual.equals(expected),
                "GT6 registers 31 overworld veins in this order; got " + actual.size() + ": " + actual);

        // GT6's numbers for the five veins §51 added (name -> minY, maxY, weight, density, size).
        Object[][] added = {
                {"lignite", 50, 130, 160, 8, 32}, {"coal", 50, 80, 80, 6, 32},
                {"bauxite", 50, 90, 80, 4, 24}, {"iodinesalt", 50, 60, 30, 3, 24},
                {"rocksalt", 50, 60, 30, 3, 24}};
        List<String> problems = new ArrayList<>();
        for (Object[] entry : added) {
            String name = (String) entry[0];
            GTOreVeins.OreVein vein = GTOreVeins.OVERWORLD_VEINS.stream()
                    .filter(v -> v.name().equals(name)).findFirst().orElse(null);
            if (vein == null) {
                problems.add(name + " missing");
                continue;
            }
            if (vein.minY() != (Integer) entry[1] || vein.maxY() != (Integer) entry[2]
                    || vein.weight() != (Integer) entry[3] || vein.density() != (Integer) entry[4]
                    || vein.size() != (Integer) entry[5]) {
                problems.add(name + " = " + vein.minY() + "/" + vein.maxY() + "/" + vein.weight()
                        + "/" + vein.density() + "/" + vein.size());
            }
        }
        helper.assertTrue(problems.isEmpty(), "§51 veins must keep GT6's parameters: " + problems);
        helper.assertTrue(GTOreVeins.TOTAL_VEIN_WEIGHT
                        == GTOreVeins.OVERWORLD_VEINS.stream().mapToInt(GTOreVeins.OreVein::weight).sum(),
                "the grid pick total is the sum of all weights");
        helper.succeed();
    }

    /** GT6's {@code ORE_END} veins: the three shared ones plus the two End-exclusive ones. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void endVeinsMatchGt6(GameTestHelper helper) {
        List<String> expected = List.of("platinum", "molybdenum", "cassiterite", "naquadah", "trinium");
        List<String> actual = GTOreVeins.END_VEINS.stream().map(GTOreVeins.OreVein::name).toList();
        helper.assertTrue(actual.equals(expected),
                "GT6's ORE_END set is " + expected + ", got " + actual);

        // The three shared veins carry exactly the overworld parameters.
        List<String> problems = new ArrayList<>();
        for (String name : List.of("platinum", "molybdenum", "cassiterite")) {
            GTOreVeins.OreVein overworld = GTOreVeins.OVERWORLD_VEINS.stream()
                    .filter(v -> v.name().equals(name)).findFirst().orElse(null);
            GTOreVeins.OreVein end = GTOreVeins.END_VEINS.stream()
                    .filter(v -> v.name().equals(name)).findFirst().orElse(null);
            if (overworld == null || end == null) {
                problems.add(name + " missing");
            } else if (overworld.minY() != end.minY() || overworld.maxY() != end.maxY()
                    || overworld.weight() != end.weight() || overworld.density() != end.density()
                    || overworld.size() != end.size()) {
                problems.add(name + " differs between overworld and end");
            }
        }
        helper.assertTrue(problems.isEmpty(), "shared veins: " + problems);
        helper.assertTrue(GTOreVeins.END_VEINS.stream().anyMatch(v -> v.name().equals("naquadah"))
                        && GTOreVeins.END_VEINS.stream().anyMatch(v -> v.name().equals("trinium")),
                "the End-exclusive veins are still there");
        helper.succeed();
    }

    /** Every material a vein names must have an ore block, or that vein generates nothing. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void everyVeinMaterialResolves(GameTestHelper helper) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        List<String> missing = new ArrayList<>();
        for (List<GTOreVeins.OreVein> table : List.of(GTOreVeins.OVERWORLD_VEINS, GTOreVeins.END_VEINS)) {
            for (GTOreVeins.OreVein vein : table) {
                for (GTMaterial material : List.of(vein.top(), vein.bottom(), vein.between(), vein.spread())) {
                    if (GTOreBlockResolver.resolve(stone, material, false) == null) {
                        missing.add(vein.name() + "/" + (material == null ? "null" : material.getName()));
                    }
                }
            }
        }
        helper.assertTrue(missing.isEmpty(), "vein materials without an ore block: " + missing);
        // The newly added veins in particular: lignite/coal/bauxite also exist as whole-block seams,
        // which is why GT6 has both and the port now generates both too.
        List<String> seamBlocks = GTStoneLayersGen.LAYERS.stream()
                .map(GTStoneLayersGen.LayerDef::blockId).filter(java.util.Objects::nonNull).toList();
        for (String seam : List.of("block_ore_lignite", "block_ore_anthracite", "block_ore_bauxite",
                "block_ore_salt", "block_ore_rocksalt")) {
            helper.assertTrue(seamBlocks.contains(seam),
                    "the stone-layer seam " + seam + " must still exist (GT6 has seams *and* veins)");
        }
        helper.succeed();
    }
}
