package com.gregtech.gregtech.loaders.c;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.registry.GTTechnological;
import com.gregtech.gregtech.util.OM;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Generic material part processing: plates, rods, bolts, screws, gears, rings,
 * springs, wires, foils, rails and gem plates for every registered material.
 *
 * <p>Port of the per-prefix machine handlers from GT6
 * {@code gregtech.loaders.c.Loader_Recipes_Handlers} (RollingMill / Wiremill /
 * RollBender / ClusterMill / RollFormer / Lathe / Cutter / Extruder tables) plus the
 * dust/crushed furnace smelting from {@code Loader_Recipes_Furnace}. The GT6
 * easy/hard-workable duration split is collapsed to the standard duration; the
 * Extruder recipes omit the shape molds until those items are ported.</p>
 */
public class Loader_Recipes_Parts implements IGTLoader {

    private static final long EU = 16;

    // Physical wire blocks are handled by WireProcessingRecipes.

    @Override
    public void run() {
        for (GTMaterial entry : GTMaterialRegistry.allMaterials()) {
            GTMaterial m = entry.resolve();
            if (!m.isValid() || m.has(MaterialProperty.ANTIMATTER)) continue;

            rollingMill(m);
            wiremill(m);
            rollBender(m);
            clusterMillAndRollFormer(m);
            lathe(m);
            cutter(m);
            extruder(m);
            smelting(m);
        }
        com.mojang.logging.LogUtils.getLogger().info(
                "[gregtech] Part processing recipes added; extruder matrix: {} recipes ({} GT6 outputs skipped: {})",
                extruderPairCount, EXTRUDER_SKIPPED.size(), EXTRUDER_SKIPPED);
    }

    // ── RollingMill: ingots → plates ────────────────────────────────────────

    private void rollingMill(GTMaterial m) {
        if (!m.has(MaterialProperty.SMITHABLE)) return;
        roll(MachineRecipeMaps.RollingMill, m, MaterialPrefix.nugget, 1, MaterialPrefix.plateTiny, 1, 2);
        roll(MachineRecipeMaps.RollingMill, m, MaterialPrefix.ingot, 1, MaterialPrefix.plate, 1, 16);
        roll(MachineRecipeMaps.RollingMill, m, MaterialPrefix.ingotDouble, 1, MaterialPrefix.plateDouble, 1, 32);
        roll(MachineRecipeMaps.RollingMill, m, MaterialPrefix.ingotTriple, 1, MaterialPrefix.plateTriple, 1, 48);
        roll(MachineRecipeMaps.RollingMill, m, MaterialPrefix.ingotQuadruple, 1, MaterialPrefix.plateQuadruple, 1, 64);
        roll(MachineRecipeMaps.RollingMill, m, MaterialPrefix.ingotQuintuple, 1, MaterialPrefix.plateQuintuple, 1, 80);
        roll(MachineRecipeMaps.RollingMill, m, MaterialPrefix.plateCurved, 1, MaterialPrefix.plate, 1, 16);
    }

    // ── Wiremill: ingots/rods → wires ───────────────────────────────────────

    private void wiremill(GTMaterial m) {
        if (!m.has(MaterialProperty.SMITHABLE)) return;
        roll(MachineRecipeMaps.Wiremill, m, MaterialPrefix.stick, 1, MaterialPrefix.wireFine, 4, 8);
        roll(MachineRecipeMaps.Wiremill, m, MaterialPrefix.stickLong, 1, MaterialPrefix.wireFine, 8, 16);

    }

    // ── RollBender: plates/rods → curved plates, rings, springs ─────────────

    private void rollBender(GTMaterial m) {
        if (!m.has(MaterialProperty.SMITHABLE)) return;
        roll(MachineRecipeMaps.RollBender, m, MaterialPrefix.plate, 1, MaterialPrefix.plateCurved, 1, 16);
        roll(MachineRecipeMaps.RollBender, m, MaterialPrefix.stick, 1, MaterialPrefix.ring, 2, 4);
        roll(MachineRecipeMaps.RollBender, m, MaterialPrefix.stickLong, 1, MaterialPrefix.spring, 1, 16);
        roll(MachineRecipeMaps.RollBender, m, MaterialPrefix.wireFine, 2, MaterialPrefix.springSmall, 1, 4);
    }

    // ── ClusterMill: plate → foils ── RollFormer: plate → rails ─────────────

    private void clusterMillAndRollFormer(GTMaterial m) {
        if (!m.has(MaterialProperty.SMITHABLE)) return;
        roll(MachineRecipeMaps.ClusterMill, m, MaterialPrefix.plate, 1, MaterialPrefix.foil, 4, 16);
        roll(MachineRecipeMaps.RollFormer, m, MaterialPrefix.plate, 1, MaterialPrefix.railGt, 4, 16);
    }

    // ── Lathe: ingots/gems → rods, bolts → screws, gem plates → lenses ──────

    private void lathe(GTMaterial m) {
        roll(MachineRecipeMaps.Lathe, m, MaterialPrefix.bolt, 1, MaterialPrefix.screw, 1, 2);
        roll(MachineRecipeMaps.Lathe, m, MaterialPrefix.nugget, 1, MaterialPrefix.round, 1, 2);
        roll(MachineRecipeMaps.Lathe, m, MaterialPrefix.plateGem, 1, MaterialPrefix.lens, 1, 16);
        roll(MachineRecipeMaps.Lathe, m, MaterialPrefix.lens, 1, MaterialPrefix.ring, 1, 16);
        roll(MachineRecipeMaps.Lathe, m, MaterialPrefix.bouleGt, 1, MaterialPrefix.stickLong, 3, 64);
        roll(MachineRecipeMaps.Lathe, m, MaterialPrefix.gemChipped, 1, MaterialPrefix.bolt, 1, 4);
        roll(MachineRecipeMaps.Lathe, m, MaterialPrefix.gemFlawed, 1, MaterialPrefix.bolt, 3, 8);
        roll(MachineRecipeMaps.Lathe, m, MaterialPrefix.billet, 1, MaterialPrefix.stick, 1, 16);

        // ingot/gem → stick swarfs half the material off as small dust
        latheToStick(m, MaterialPrefix.ingot);
        latheToStick(m, MaterialPrefix.gem);
    }

    private void latheToStick(GTMaterial m, MaterialPrefix input) {
        ItemStack in = mat(input, m, 1);
        ItemStack stick = mat(MaterialPrefix.stick, m, 1);
        if (in.isEmpty() || stick.isEmpty()) return;
        ItemStack swarf = mat(MaterialPrefix.dustSmall, m, 2);
        if (swarf.isEmpty()) {
            MachineRecipeMaps.Lathe.addRecipe1(true, EU, 16, in, stick);
        } else {
            MachineRecipeMaps.Lathe.addRecipe1(true, EU, 16, in, stick, swarf);
        }
    }

    // ── Cutter: long rods / multi-ingots / gems → halves and plates ─────────
    // The Cutter map requires a coolant fluid; GT6 used water scaled with cut time.

    private void cutter(GTMaterial m) {
        cut(m, MaterialPrefix.stickLong, 1, MaterialPrefix.stick, 2, 16, 32);
        cut(m, MaterialPrefix.stick, 1, MaterialPrefix.bolt, 4, 48, 32);
        cut(m, MaterialPrefix.plate, 1, MaterialPrefix.plateTiny, 8, 64, 32);
        cut(m, MaterialPrefix.plateGem, 1, MaterialPrefix.plateGemTiny, 8, 64, 32);
        cut(m, MaterialPrefix.gemChipped, 1, MaterialPrefix.plateGemTiny, 2, 16, 32);
        cut(m, MaterialPrefix.gemFlawed, 1, MaterialPrefix.plateGemTiny, 4, 32, 32);
        cut(m, MaterialPrefix.gem, 1, MaterialPrefix.plateGem, 1, 16, 96);
        cut(m, MaterialPrefix.gemFlawless, 1, MaterialPrefix.plateGem, 2, 16, 96);
        cut(m, MaterialPrefix.gemExquisite, 1, MaterialPrefix.plateGem, 4, 48, 96);
        cut(m, MaterialPrefix.gemLegendary, 1, MaterialPrefix.plateGem, 8, 112, 96);
        cut(m, MaterialPrefix.bouleGt, 1, MaterialPrefix.plateGem, 4, 48, 32);
        cut(m, MaterialPrefix.ingotDouble, 1, MaterialPrefix.ingot, 2, 16, 32);
        cut(m, MaterialPrefix.ingotTriple, 1, MaterialPrefix.ingot, 3, 32, 32);
        cut(m, MaterialPrefix.ingotQuadruple, 1, MaterialPrefix.ingot, 4, 48, 32);
        cut(m, MaterialPrefix.ingotQuintuple, 1, MaterialPrefix.ingot, 5, 64, 32);
        cut(m, MaterialPrefix.plateDouble, 1, MaterialPrefix.plate, 2, 16, 32);
        cut(m, MaterialPrefix.plateTriple, 1, MaterialPrefix.plate, 3, 32, 32);
        cut(m, MaterialPrefix.plateQuadruple, 1, MaterialPrefix.plate, 4, 48, 32);
        cut(m, MaterialPrefix.plateQuintuple, 1, MaterialPrefix.plate, 5, 64, 32);
    }

    private void cut(GTMaterial m, MaterialPrefix in, int inCount, MaterialPrefix out, int outCount,
                     long duration, long eut) {
        ItemStack input = mat(in, m, inCount);
        if (input.isEmpty()) return;
        ItemStack output = mat(out, m, outCount);
        if (output.isEmpty()) return;
        net.minecraftforge.fluids.FluidStack water = new net.minecraftforge.fluids.FluidStack(
                net.minecraft.world.level.material.Fluids.WATER, (int) Math.max(16, duration));
        MachineRecipeMaps.Cutter.addRecipe1(true, eut, duration, input, water, (net.minecraftforge.fluids.FluidStack) null, output);
    }

    // ── Extruder: any extruder-capable input + shape mold → any part ────────
    //
    // GT6 Loader_Recipes_Handlers:740-800 registers the whole matrix generically: for every prefix in
    // EXTRUDER_FODDER / INGOT_BASED / GEM_BASED (and DUST_BASED for the low-heat family) it walks a
    // fixed output list and calls addExtruderRecipe(input, output, hot, shape). Amounts are scaled by
    // the two prefixes' material weights (line 812). The port used to register only the ingot row of
    // the hot family, so tool heads, blocks, and every low-heat mold were missing.

    /** One GT6 extruder output: mold name (shape item suffix) plus the port form it produces. */
    private record ExtruderOutput(String mold, MaterialPrefix prefix,
                                 com.gregtech.gregtech.api.prefix.BlockMaterialPrefix blockPrefix) {
        static ExtruderOutput of(String mold, MaterialPrefix prefix) { return new ExtruderOutput(mold, prefix, null); }
        static ExtruderOutput block(String mold, com.gregtech.gregtech.api.prefix.BlockMaterialPrefix prefix) {
            return new ExtruderOutput(mold, null, prefix);
        }
        long weight() { return prefix != null ? prefix.getMaterialWeight() : blockPrefix.getMaterialWeight(); }
        ItemStack stack(GTMaterial material) {
            return prefix != null ? mat(prefix, material, 1)
                    : com.gregtech.gregtech.registry.GTBlocks.getStack(blockPrefix, material);
        }
    }

    private static final List<ExtruderOutput> EXTRUDER_OUTPUTS = List.of(
            ExtruderOutput.of("ingot", MaterialPrefix.ingot),
            ExtruderOutput.of("plate", MaterialPrefix.plate),
            ExtruderOutput.of("curvedplate", MaterialPrefix.plateCurved),
            ExtruderOutput.of("rod", MaterialPrefix.stick),
            ExtruderOutput.of("longrod", MaterialPrefix.stickLong),
            ExtruderOutput.of("bolt", MaterialPrefix.bolt),
            ExtruderOutput.of("ring", MaterialPrefix.ring),
            ExtruderOutput.of("tinyplate", MaterialPrefix.plateTiny),
            ExtruderOutput.of("foil", MaterialPrefix.foil),
            ExtruderOutput.of("finewire", MaterialPrefix.wireFine),
            ExtruderOutput.of("smallgear", MaterialPrefix.gearGtSmall),
            ExtruderOutput.of("gear", MaterialPrefix.gearGt),
            ExtruderOutput.of("shovelhead", MaterialPrefix.toolHeadRawShovel),
            ExtruderOutput.of("swordblade", MaterialPrefix.toolHeadRawSword),
            ExtruderOutput.of("hoehead", MaterialPrefix.toolHeadRawHoe),
            ExtruderOutput.of("sawblade", MaterialPrefix.toolHeadRawSaw),
            ExtruderOutput.of("pickaxehead", MaterialPrefix.toolHeadRawPickaxe),
            ExtruderOutput.of("axehead", MaterialPrefix.toolHeadRawAxe),
            ExtruderOutput.of("filehead", MaterialPrefix.toolHeadFile),
            ExtruderOutput.of("hammerhead", MaterialPrefix.toolHeadHammer),
            // GT6 Loader_Recipes_Handlers:769 — a block of cast metal (9U) needs nine ingots.
            ExtruderOutput.block("block", com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.blockSolid));

    /** GT6 outputs whose port form does not exist yet (recorded instead of guessed). */
    public static final List<String> EXTRUDER_SKIPPED = List.of(
            "wireGt01 (wires are physical blocks in this port: registry/GTWires)",
            "casingSmall (no MaterialPrefix.casingSmall)",
            "pipeTiny/small/medium/large/huge (port pipes are blocks: MaterialPrefixes.fluidPipe*)",
            "capcellcon / cell / bottle (capsule containers have their own loader: CapsuleCellRecipes)");

    /** GT6's input side: EXTRUDER_FODDER / INGOT_BASED / GEM_BASED (dust joins only the low-heat run). */
    private static final List<MaterialPrefix> EXTRUDER_INPUTS = List.of(
            MaterialPrefix.ingot, MaterialPrefix.plate, MaterialPrefix.gem, MaterialPrefix.dust);

    private int extruderPairCount;

    private void extruder(GTMaterial m) {
        // PolymerFormingRecipes owns their original low-heat costs and both mold families.
        if (m.getName().equals("Rubber") || m.getName().equals("Plastic")) return;

        for (MaterialPrefix input : EXTRUDER_INPUTS) {
            ItemStack in = mat(input, m, 1);
            if (in.isEmpty()) continue;
            for (ExtruderOutput out : EXTRUDER_OUTPUTS) {
                if (out.prefix() == input) continue;                      // GT6: aInput == aOutput returns
                ItemStack output = out.stack(m);
                if (output.isEmpty()) continue;
                long inWeight = input.getMaterialWeight(), outWeight = out.weight();
                if (inWeight <= 0 || outWeight <= 0) continue;
                // GT6 Loader_Recipes_Handlers:811-813
                long inCount = 1, outCount = 1;
                if (outWeight > inWeight) inCount = (outWeight + inWeight - 1) / inWeight;
                else outCount = inWeight / outWeight;
                if (inCount > 64 || outCount < 1) continue;

                // Hot family: GT6's shape extruder runs at 96 GU/t with the forging cost of
                // RecipeMapHandlerPrefixForging (same formula the capsule cells use). Low-heat family:
                // 16 GU/t plus GT6's computed duration
                // (units(outAmount * outCount, U, 64, roundUp), Loader_Recipes_Handlers:821).
                long outUnits = outWeight * outCount;
                extrude(m, in, inCount, "extruder_shape_" + out.mold(), output, outCount, 96,
                        com.gregtech.gregtech.content.recipe.CapsuleCellRecipes.hotExtrusionTicks(m, outUnits));
                long lowHeatDuration = Math.max(16, (outUnits * 64 + GTValues.U - 1) / GTValues.U);
                extrude(m, in, inCount, "low_heat_extruder_shape_" + out.mold(), output, outCount, EU, lowHeatDuration);
            }
        }
    }

    /** Registers one extruder recipe with its mold as a catalyst, or without a mold when it is absent. */
    private void extrude(GTMaterial m, ItemStack input, long inCount, String moldId, ItemStack output,
                         long outCount, long eut, long duration) {
        ItemStack in = input.copy();
        in.setCount((int) inCount);
        ItemStack out = output.copy();
        out.setCount((int) outCount);
        ItemStack mold = shape(moldId);
        boolean added = mold.isEmpty()
                ? MachineRecipeMaps.Extruder.addRecipe1(true, eut, duration, in, out) != null
                : MachineRecipeMaps.Extruder.addRecipe2(true, eut, duration, in, mold, out) != null;
        if (added) extruderPairCount++;
    }

    /** Extruder shape mold from {@link GTTechnological}, EMPTY when not registered. */
    private static ItemStack shape(String name) {
        net.minecraft.world.item.Item item = GTTechnological.get(name);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** Extruder recipes added by the matrix above, for tests and reports. */
    public int extruderRecipes() { return extruderPairCount; }

    // ── Furnace: dust / crushed chain → smelting target ─────────────────────

    private void smelting(GTMaterial m) {
        // MT.Ad has MELTING but not FURNACE: restoring its ingot form must not unlock ordinary furnace smelting.
        if (m == com.gregtech.gregtech.content.material.generated.ElementMaterials.Adamantium) return;
        GTMaterial target = m.getTargetSmeltingMaterial();
        long amount = m.getTargetSmeltingAmount();
        if (target == null || !target.isValid() || amount <= 0) return;
        if (!m.has(MaterialProperty.MELTING)) return;

        ItemStack output = OM.dustOrIngot(target, amount);
        if (output.isEmpty()) output = OM.gem(target, amount);
        if (output.isEmpty()) return;

        smeltInput(MaterialPrefix.dust, m, output);
        smeltInput(MaterialPrefix.crushed, m, output);
        smeltInput(MaterialPrefix.crushedPurified, m, output);
        smeltInput(MaterialPrefix.crushedCentrifuged, m, output);
    }

    private void smeltInput(MaterialPrefix prefix, GTMaterial m, ItemStack output) {
        ItemStack input = mat(prefix, m, 1);
        if (input.isEmpty()) return;
        MachineRecipeMaps.add_smelting(input, output.copy(), 0.0f, true, false, false);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Registers {@code inCount × in → outCount × out} when both stacks resolve. */
    private void roll(com.gregtech.gregtech.api.recipe.RecipeMap map, GTMaterial m,
                      MaterialPrefix in, int inCount, MaterialPrefix out, int outCount, long duration) {
        roll(map, m, in, inCount, out, outCount, duration, EU);
    }

    private void roll(com.gregtech.gregtech.api.recipe.RecipeMap map, GTMaterial m,
                      MaterialPrefix in, int inCount, MaterialPrefix out, int outCount, long duration, long eut) {
        ItemStack input = mat(in, m, inCount);
        if (input.isEmpty()) return;
        ItemStack output = mat(out, m, outCount);
        if (output.isEmpty()) return;
        map.addRecipe1(true, eut, duration, input, output);
    }

    private static ItemStack mat(MaterialPrefix prefix, GTMaterial material, int count) {
        if (prefix == null || !prefix.isValidFor(material)) return ItemStack.EMPTY;
        return MaterialStackItemHelper.mat(prefix, material, count);
    }

    private static MaterialPrefix byName(String name) {
        for (MaterialPrefix prefix : PrefixRegistry.all()) {
            if (name.equals(prefix.getName())) return prefix;
        }
        return null;
    }
}
