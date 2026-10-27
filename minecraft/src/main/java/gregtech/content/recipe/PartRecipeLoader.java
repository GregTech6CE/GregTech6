package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.engine.material.MaterialStackItemHelper;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.bootstrap.InitializationStep;
import gregtech.content.registry.GTTechnological;
import gregtech.util.OM;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Generic material part processing: plates, rods, bolts, screws, gears, rings,
 * springs, wires, foils, rails and gem plates for every registered material.
 *
 * <p>Port of the per-prefix machine handlers from GT6
 * {@code gregtech.loaders.c.Loader_Recipes_Handlers} (RollingMill / Wiremill /
 * RollBender / ClusterMill / RollFormer / Lathe / Cutter / Extruder tables) plus the
 * dust/crushed furnace smelting from {@code Loader_Recipes_Furnace}. Lathe uses a bounded
 * source-profile adapter; other machine families still need their easy/hard duration audit.
 * Extrusion uses registered shape molds, with a mold-free fallback if lookup fails.
 * Material eligibility, forging targets and the input-prefix matrix still require
 * alignment with the original; see ALIGN-011 in the integration audit.</p>
 */
public class PartRecipeLoader implements InitializationStep {

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
            lathe(entry);
            cutter(entry);
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

    private void lathe(GTMaterial entry) {
        GTMaterial m = entry.resolve();
        for (var rule : SourceLatheRules.rules()) {
            var decision = SourceLatheRules.evaluate(entry, rule);
            if (!decision.allowed()) continue;
            // The source lens-generation predicate applies only to the plate-to-ring alternative.
            // Existing native holders still decide whether the other processing rows can be emitted.
            lathePart(m, rule.input(), 1, rule.output(), rule.outputCount(), decision.ticks());
        }
    }

    /** Original Loader_Recipes_Handlers:371–393 enables pulverized remains for these lathe rows. */
    private void lathePart(GTMaterial m, MaterialPrefix in, int inCount,
                           MaterialPrefix out, int outCount, long duration) {
        ItemStack input = mat(in, m, inCount);
        ItemStack output = mat(out, m, outCount);
        if (input.isEmpty() || output.isEmpty()) return;
        MachineRecipeMaps.Lathe.addRecipe1(true, EU, duration, input,
                machiningOutputs(m, in, inCount, out, outCount, output));
    }

    /** RecipeMapHandlerPrefix:213–218 → OM.pulverize:370–371, including target identity and ratio. */
    private static ItemStack[] machiningOutputs(GTMaterial material, MaterialPrefix in, int inCount,
                                                MaterialPrefix out, int outCount, ItemStack output) {
        return machiningOutputs(material, in.getMaterialWeight() * inCount, out, outCount, output);
    }

    private static ItemStack[] machiningOutputs(GTMaterial material, long inputUnits,
                                                MaterialPrefix out, int outCount, ItemStack output) {
        long remains = inputUnits - out.getMaterialWeight() * outCount;
        ItemStack dust = pulverizedRemains(material, remains);
        return dust.isEmpty() ? new ItemStack[]{output} : new ItemStack[]{output, dust};
    }

    private static ItemStack pulverizedRemains(GTMaterial material, long remains) {
        return OM.dust(material.getTargetPulverMaterial(),
                PulverizationRules.amount(remains, material.getTargetPulverAmount()));
    }

    // ── Cutter: bounded source part matrix and four independent coolant variants ──

    private void cutter(GTMaterial entry) {
        GTMaterial material = entry.resolve();
        for (var rule : SourceCutterRules.rules()) {
            for (var coolant : SourceCutterRules.sourceCoolants()) {
                var decision = SourceCutterRules.evaluate(entry, rule, coolant);
                if (!decision.allowed()) continue;
                cut(material, rule, coolant, decision);
            }
        }
    }

    /** Source Handlers:632–652; holders and ordinary collisions remain native admission decisions. */
    private void cut(GTMaterial material, SourceCutterRules.Rule rule, SourceCutterRules.Coolant coolant,
                     SourceCutterRules.Decision decision) {
        ItemStack input = rule.blockInput() == null ? mat(rule.input(), material, 1)
                : gregtech.content.registry.GTBlocks.getStack(rule.blockInput(), material);
        ItemStack output = mat(rule.output(), material, rule.outputCount());
        if (input.isEmpty() || output.isEmpty()) return;
        var fluid = gregtech.content.registry.GTFluids.stack(coolant.field(), decision.fluidAmount());
        // Forge uses null for an absent binding; Neo uses EMPTY. Never substitute another coolant.
        if (fluid == null || fluid.isEmpty()) return;
        long inputUnits = rule.inputUnits();
        if (rule.blockInput() != null) {
            if (!verifiedCutterBlock(input, material)) return;
//? if >=1.21.1 {
            /*var recipe = gregtech.engine.recipe.RecipeMap.make(true, new ItemStack[]{input},
                    machiningOutputs(material, inputUnits, rule.output(), rule.outputCount(), output), null, null,
                    new net.neoforged.neoforge.fluids.FluidStack[]{fluid},
                    new net.neoforged.neoforge.fluids.FluidStack[0], decision.ticks(), rule.eut(), 0);
*///?} else {
            var recipe = gregtech.engine.recipe.RecipeMap.make(true, new ItemStack[]{input},
                    machiningOutputs(material, inputUnits, rule.output(), rule.outputCount(), output), null, null,
                    new net.minecraftforge.fluids.FluidStack[]{fluid},
                    new net.minecraftforge.fluids.FluidStack[0], decision.ticks(), rule.eut(), 0);
//?}
            if (recipe != null) MachineRecipeMaps.Cutter.addRecipe(
                    recipe.withItemInputGuard(0, offered -> verifiedCutterBlock(offered, material)));
            return;
        }
//? if >=1.21.1 {
        /*MachineRecipeMaps.Cutter.addRecipe1(true, rule.eut(), decision.ticks(), input, fluid,
                (net.neoforged.neoforge.fluids.FluidStack) null,
                machiningOutputs(material, inputUnits, rule.output(), rule.outputCount(), output));
*///?} else {
        MachineRecipeMaps.Cutter.addRecipe1(true, rule.eut(), decision.ticks(), input, fluid,
                (net.minecraftforge.fluids.FluidStack) null,
                machiningOutputs(material, inputUnits, rule.output(), rule.outputCount(), output));
//?}
    }

    /** Only the new solid-block route needs this physical 9U proof; explicit metadata always wins. */
    private static boolean verifiedCutterBlock(ItemStack stack, GTMaterial material) {
        try {
            if (stack == null || stack.isEmpty() || stack.isDamaged() || material == null
                    || !material.isValid() || material.resolve() != material
                    || GTMaterialRegistry.get(material.getId()) != material
                    || GTMaterialRegistry.get(material.getName()) != material
                    || gregtech.engine.material.ItemMaterialRegistry.hasStoredContents(stack)) return false;
            var prefix = gregtech.engine.prefix.BlockMaterialPrefix.blockSolid;
            ItemStack canonical = gregtech.content.registry.GTBlocks.getStack(prefix, material);
            if (canonical.isEmpty() || canonical.getItem() != stack.getItem()
                    || !(stack.getItem() instanceof net.minecraft.world.item.BlockItem item)
                    || !(item.getBlock() instanceof gregtech.content.block.MaterialBlockLike block)
                    || block.prefix() != prefix || block.material().resolve() != material) return false;
            long units = 9 * GTValues.U;
            var data = gregtech.engine.material.ItemMaterialRegistry.base(stack.getItem()).orElse(null);
            if (data != null) return data.recoverable() && data.prefix() == null
                    && data.components().size() == 1 && data.material().resolve() == material
                    && data.amount() == units;
            // Forge has no general block composition registration; inspect the actual known block.
            var weights = gregtech.content.block.BlockMaterialWeights.contained(block.material(), block.prefix());
            return weights.size() == 1 && weights.get(0).material().resolve() == material
                    && weights.get(0).amount() == units;
        } catch (RuntimeException | LinkageError invalidBinding) {
            return false;
        }
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
                                 gregtech.engine.prefix.BlockMaterialPrefix blockPrefix) {
        static ExtruderOutput of(String mold, MaterialPrefix prefix) { return new ExtruderOutput(mold, prefix, null); }
        static ExtruderOutput block(String mold, gregtech.engine.prefix.BlockMaterialPrefix prefix) {
            return new ExtruderOutput(mold, null, prefix);
        }
        long weight() { return prefix != null ? prefix.getMaterialWeight() : blockPrefix.getMaterialWeight(); }
        ItemStack stack(GTMaterial material) {
            return prefix != null ? mat(prefix, material, 1)
                    : gregtech.content.registry.GTBlocks.getStack(blockPrefix, material);
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
            ExtruderOutput.block("block", gregtech.engine.prefix.BlockMaterialPrefix.blockSolid));

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
//? if >=1.21.1 {

                        /*gregtech.content.recipe.ExtrusionWorkRules.hotExtrusionTicks(m, outUnits));

*///?} else {
                        gregtech.content.recipe.CapsuleCellRecipes.hotExtrusionTicks(m, outUnits));
//?}
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
        if (m == gregtech.content.material.generated.ElementMaterials.Adamantium) return;
        GTMaterial target = m.getTargetSmeltingMaterial();
        long amount = m.getTargetSmeltingAmount();
        if (target == null || !target.isValid() || amount <= 0) return;
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
        // Native forms use the reviewed source listener; a replaced canonical holder keeps its old policy.
        if (!SourceFurnaceNativeAdapter.earlyFormAllowed(prefix, m, input)) return;
        MachineRecipeMaps.add_smelting(input, output.copy(), 0.0f, true, false, false);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Registers {@code inCount × in → outCount × out} when both stacks resolve. */
    private void roll(gregtech.engine.recipe.RecipeMap map, GTMaterial m,
                      MaterialPrefix in, int inCount, MaterialPrefix out, int outCount, long duration) {
        roll(map, m, in, inCount, out, outCount, duration, EU);
    }

    private void roll(gregtech.engine.recipe.RecipeMap map, GTMaterial m,
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
