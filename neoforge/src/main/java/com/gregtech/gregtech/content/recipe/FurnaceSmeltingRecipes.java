package com.gregtech.gregtech.content.recipe;


import com.gregtech.gregtech.api.machine.crucible.CrucibleMath;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialGroups;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.util.OM;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GT6's furnace smelting rows for ore-processing forms: {@code Loader_Recipes_Furnace:137-166} and
 * the {@code Listener_Furnace_Smelting} class at the end of that file.
 *
 * <p>Every material form — dust, small/tiny pile, gem ladder, rock, raw ore, crushed ore — can be
 * smelted in a furnace into the metal/gem it belongs to. This is the mechanic that makes GT6's early
 * game work: without it a player who crushes an ore has no way back to an ingot until the first
 * crucible. The amount follows the original exactly:</p>
 *
 * <pre>
 * targetAmount = units(units(mTargetSmelting.mAmount, U, mTargetSolidifying.mAmount, round),
 *                      U, row.fixedAmount &lt; 0 ? prefix.mAmount : row.fixedAmount, round)
 * output       = OM.ingot(mTargetSmelting.mMaterial.mTargetSolidifying.mMaterial, targetAmount)
 * experience   = row.exp ? units(targetAmount, U, toolQuality + 1, ceil) : 0
 * </pre>
 *
 * <p>{@code mTargetSolidifying} is not modelled by the port: in GT6 only H2O → Ice and Lava → Obsidian
 * set it ({@code MT.java:1880-1881}) and neither is furnace-smeltable, so the smelting target is its
 * own solidifying target for every row this table touches. {@code OM.ingot}'s cascade
 * (block → ingot → chunk → nugget) is the port's, so a small pile of dust comes back as one chunk and
 * a nugget stays a nugget, exactly as in the original.</p>
 *
 * <p>Deviations, both recorded rather than guessed at:</p>
 * <ul>
 *   <li>{@code dustPure}, {@code dustRefined}, {@code chunk}, {@code rubble}, {@code pebbles},
 *       {@code cluster}, {@code cleanGravel}, {@code dirtyGravel}, {@code crystalline},
 *       {@code reduced} and {@code rawOreChunk} are GT6 prefixes this port does not register; their
 *       rows are listed in {@link #skipped()} instead of being approximated.</li>
 *   <li>GT6 registers food materials as <em>smoker</em> recipes and everything else as blast furnace
 *       recipes; the port's {@code MachineRecipeMaps.add_smelting} keeps one furnace table, so the
 *       smoker/blast distinction is passed through but not materialised.</li>
 * </ul>
 */
public final class FurnaceSmeltingRecipes {
    /** One row of GT6's listener list: prefix, fixed target amount in units (-1 = the prefix's own) and exp flag. */
    private record Row(String prefix, long fixedAmount, boolean exp) {
        static Row of(String prefix) { return new Row(prefix, -1, false); }
        static Row exp(String prefix) { return new Row(prefix, -1, true); }
        static Row fixed(String prefix, long units) { return new Row(prefix, units, true); }
    }

    /** GT6 {@code Loader_Recipes_Furnace:137-166}, in the original's order. */
    private static final List<Row> ROWS = List.of(
            Row.of("scrapGt"),
            Row.of("dust"), Row.of("dustSmall"), Row.of("dustTiny"),
            Row.of("dustImpure"), Row.of("dustPure"), Row.of("dustRefined"),
            Row.of("gemChipped"), Row.of("gemFlawed"), Row.of("gem"), Row.of("gemFlawless"),
            Row.of("gemExquisite"), Row.of("gemLegendary"),
            Row.exp("rockGt"), Row.exp("rawOreChunk"),
            Row.fixed("oreRaw", GTValues.U),
            Row.fixed("chunk", GTValues.U * 2), Row.fixed("rubble", GTValues.U * 2),
            Row.fixed("pebbles", GTValues.U * 3), Row.fixed("cluster", GTValues.U * 3),
            Row.exp("cleanGravel"), Row.exp("dirtyGravel"), Row.exp("crystalline"), Row.exp("reduced"),
            Row.exp("crushed"), Row.exp("crushedTiny"),
            Row.exp("crushedPurified"), Row.exp("crushedPurifiedTiny"),
            Row.exp("crushedCentrifuged"), Row.exp("crushedCentrifugedTiny"));

    /** GT6 {@code Loader_Recipes_Furnace:168-177}: clay-family forms that fire into ceramic ones. */
    private static final String[][] CERAMIC_ROWS = {
            {"dust", "dust"}, {"dustSmall", "dustSmall"}, {"dustTiny", "dustTiny"}, {"dustDiv72", "dustDiv72"},
            {"plate", "plate"}, {"plateTiny", "plateTiny"}, {"plateCurved", "plateCurved"}};

    private static final List<String> SKIPPED = new ArrayList<>();
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private static boolean registered;

    private FurnaceSmeltingRecipes() {}

    /** One registered row: the form that goes in and the solid form that comes out. */
    public record Entry(String prefix, GTMaterial material, ItemStack input, ItemStack output) {}

    /** Every registered row, for tests and reports. */
    public static List<Entry> entries() { return Collections.unmodifiableList(ENTRIES); }

    public static List<String> skipped() { return Collections.unmodifiableList(SKIPPED); }

    public static int register() {
        if (registered) throw new IllegalStateException("Furnace smelting recipes registered twice");
        registered = true;
        for (Row row : ROWS) {
            MaterialPrefix prefix = PrefixRegistry.byName(row.prefix());
            if (prefix == null) {
                SKIPPED.add(row.prefix() + ": the port registers no such prefix");
                continue;
            }
            for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
                form(material, prefix, row);
            }
        }
        ceramic();
        ClayMoldRecipes.register();
        com.mojang.logging.LogUtils.getLogger().info("Registered {} GT6 furnace smelting rows ({} GT6 rows skipped: {})",
                ENTRIES.size(), SKIPPED.size(), SKIPPED);
        return ENTRIES.size();
    }

    /** One {@code <form> -> <metal>} row for one material (GT6 {@code Listener_Furnace_Smelting}). */
    private static void form(GTMaterial material, MaterialPrefix prefix, Row row) {
        // GT6: only materials flagged FURNACE, and never the placeholder materials
        if (!MaterialWorkability.isFurnace(material)) return;
        if (material.has(MaterialProperty.HIDDEN)) return;                        // GT6 UNUSED_MATERIAL
        ItemStack input = GTItems.getStack(prefix, material, 1);
        if (input.isEmpty()) return;

        GTMaterial smelting = material.getTargetSmeltingMaterial();
        if (smelting == null || !smelting.isValid()) smelting = material;
        long smeltingAmount = Math.max(0, material.getTargetSmeltingAmount());
        if (smeltingAmount <= 0) return;

        long solidAmount = smeltingAmount;                                        // GT6 mTargetSolidifying
        long formAmount = row.fixedAmount() < 0 ? prefix.getMaterialWeight() : row.fixedAmount();
        long target = CrucibleMath.units(
                CrucibleMath.units(smeltingAmount, GTValues.U, solidAmount, false),
                GTValues.U, formAmount, false);
        if (target <= 0) return;

        ItemStack output = OM.ingot(smelting, target);
        if (output.isEmpty()) return;
        long experience = row.exp()
                ? CrucibleMath.units(target, GTValues.U, material.getToolQuality() + 1L, true) : 0;
        boolean food = MaterialWorkability.isFood(material);
        // GT6 passes aRemoveOthers = !RUNNING, and RUNNING is true while the loader registers.
        if (MachineRecipeMaps.add_smelting(input, output, experience, false, food, !food)) {
            ENTRIES.add(new Entry(prefix.getName(), material, input, output));
        }
    }

    /** GT6 :168-177 — the clay family fires into the same ceramic form, ingots into bricks. */
    private static void ceramic() {
        GTMaterial ceramic = GTMaterialRegistry.get("Ceramic");
        if (ceramic == null || !ceramic.isValid()) {
            SKIPPED.add("clay family rows: the port has no Ceramic material");
            return;
        }
        for (GTMaterial material : MaterialGroups.Clay.getReRegistrations()) {
            if (material == null || !material.isValid()) continue;
            for (String[] pair : CERAMIC_ROWS) {
                MaterialPrefix from = PrefixRegistry.byName(pair[0]);
                MaterialPrefix to = PrefixRegistry.byName(pair[1]);
                if (from == null || to == null) continue;
                ItemStack input = GTItems.getStack(from, material, 1);
                ItemStack output = GTItems.getStack(to, ceramic, 1);
                if (input.isEmpty() || output.isEmpty()) continue;
                if (MachineRecipeMaps.add_smelting(input, output, 0, false, false, true)) {
                    ENTRIES.add(new Entry(from.getName(), material, input, output));
                }
            }
            MaterialPrefix ingot = PrefixRegistry.byName("ingot");
            ItemStack clayIngot = ingot == null ? ItemStack.EMPTY : GTItems.getStack(ingot, material, 1);
            if (!clayIngot.isEmpty()) {
                ItemStack brick = new ItemStack(Items.BRICK);
                if (MachineRecipeMaps.add_smelting(clayIngot, brick, 0, false, false, true)) {
                    ENTRIES.add(new Entry("ingot", material, clayIngot, brick));
                }
            }
        }
    }
}
