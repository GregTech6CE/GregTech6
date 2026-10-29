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
 * Extrusion follows the original input-prefix matrix, material eligibility and forging targets
 * through SourceExtruderRecipes; molds are always required catalysts.</p>
 */
public class PartRecipeLoader implements InitializationStep {

    private static final long EU = 16;

    // Physical wire blocks are handled by WireProcessingRecipes.

    @Override
    public void run() {
        for (GTMaterial entry : GTMaterialRegistry.allMaterials()) {
            GTMaterial m = entry.resolve();
            if (!m.isValid() || m.has(MaterialProperty.ANTIMATTER)) continue;

            rollingMill(entry);
            wiremill(entry);
            forming(entry);
            lathe(entry);
            cutter(entry);
            extruder(m);
            smelting(m);
        }
        extruderPairCount = SourceExtruderRecipes.finishRegistration();
        com.mojang.logging.LogUtils.getLogger().info(
                "[gregtech] Part processing recipes added; extruder matrix: {} recipes ({} GT6 outputs skipped: {})",
                extruderPairCount, EXTRUDER_SKIPPED.size(), EXTRUDER_SKIPPED);
    }

    // ── RollingMill: ingots → plates ────────────────────────────────────────

    private void rollingMill(GTMaterial entry) {
        rollingWire(entry, SourceRollingWireRules.Machine.ROLLING_MILL);
    }

    private void wiremill(GTMaterial entry) {
        rollingWire(entry, SourceRollingWireRules.Machine.WIREMILL);
    }

    private void rollingWire(GTMaterial entry, SourceRollingWireRules.Machine machine) {
        GTMaterial material = entry.resolve();
        var map = machine == SourceRollingWireRules.Machine.ROLLING_MILL ? MachineRecipeMaps.RollingMill : MachineRecipeMaps.Wiremill;
        for (var rule : SourceRollingWireRules.rules()) {
            if (rule.machine() != machine) continue;
            var decision = SourceRollingWireRules.evaluate(entry, rule);
            if (!decision.allowed()) continue;
            if (rule.blockInput() == null) {
                roll(map, material, rule.input(), 1, rule.output(), rule.outputCount(), decision.ticks());
                continue;
            }
            ItemStack input = gregtech.content.registry.GTBlocks.getStack(rule.blockInput(), material);
            ItemStack output = mat(rule.output(), material, rule.outputCount());
            if (output.isEmpty() || !verifiedSolidBlock(input, material)) continue;
            var recipe = new gregtech.engine.recipe.Recipe(new ItemStack[]{input}, new ItemStack[]{output},
                    null, null, null, null, decision.ticks(), SourceRollingWireRules.EU_PER_TICK, 0);
            map.addRecipe(recipe.withItemInputGuard(0, offered -> verifiedSolidBlock(offered, material)));
        }
    }

    // ── Six generic RollBender / ClusterMill / RollFormer pairs ────────────

    private void forming(GTMaterial entry) {
        GTMaterial material = entry.resolve();
        for (var rule : SourceFormingRules.rules()) {
            var decision = SourceFormingRules.evaluate(entry, rule);
            if (!decision.allowed()) continue;
            var map = switch (rule.machine()) {
                case ROLL_BENDER -> MachineRecipeMaps.RollBender;
                case CLUSTER_MILL -> MachineRecipeMaps.ClusterMill;
                case ROLL_FORMER -> MachineRecipeMaps.RollFormer;
            };
            roll(map, material, rule.input(), rule.inputCount(), rule.output(), rule.outputCount(),
                    decision.ticks(), SourceFormingRules.EU_PER_TICK);
        }
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

    /** RecipeMapHandlerPrefix:213–218 → OM.pulverize, including target identity and ratio. */
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

    /** Source Handlers; holders and ordinary collisions remain native admission decisions. */
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
            if (!verifiedSolidBlock(input, material)) return;
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
                    recipe.withItemInputGuard(0, offered -> verifiedSolidBlock(offered, material)));
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

    /** The reviewed solid-block routes require physical 9U proof; explicit metadata always wins. */
    private static boolean verifiedSolidBlock(ItemStack stack, GTMaterial material) {
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

    private int extruderPairCount;
    public static final List<String> EXTRUDER_SKIPPED = List.of();
    private void extruder(GTMaterial material) {
        SourceExtruderRecipes.register(material);
    }
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
