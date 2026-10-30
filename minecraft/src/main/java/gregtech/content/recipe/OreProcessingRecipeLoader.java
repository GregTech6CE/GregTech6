package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.engine.material.*;
import gregtech.engine.material.*;
import gregtech.engine.material.*;
import gregtech.engine.recipe.RecipeMap;
import gregtech.content.data.*;
import gregtech.bootstrap.InitializationStep;
import gregtech.content.registry.GTFluids;
import gregtech.util.OM;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;

*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;
//?}

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.material.GTValues.*;

/**
 * GT6 ore-processing machine recipes.
 *
 * <p>Port of {@code gregtech.loaders.b.Loader_OreProcessing} (OreProcessing_Ore,
 * OreProcessing_Maceration, washing, byproduct listing, and recycling).</p>
 *
 * <p>Registered standard ores use GT6 crushing targets and separate hammer/crusher yields.
 * Small ore mining is handled by SmallOreDrops, not a standard-ore recipe.</p>
 */
public class OreProcessingRecipeLoader implements InitializationStep {

    /** Chances are 1/100th of a percent: 10_000 = 100 %. */
    private static final long CHANCE_100 = 10_000L;
    private static final long CHANCE_50  =  5_000L;
    private static final long CHANCE_15  =  1_500L;
    private static final long CHANCE_10  =  1_000L;
    private static final long CHANCE_5   =    500L;

    /** Default EU/t for the ore processing tier. */
    private static final long EU_TIER = 16;

    /** Tracks materials already added to the ByProductList (avoids duplicates). */
    private final List<GTMaterial> mByProductListed = new ArrayList<>();
    /** Deduplicate only immutable builtin raw descriptors, never independent recipe rows. */
    private final java.util.Set<GTMaterial> mRawDefinitions = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private SourceOreMacerationAdapter.Snapshot mMaceration;

    @Override
    public void run() {
        // Snapshot form provenance before any owned raw descriptors. The following entry loops
        // only add recipe rows; they do not publish material forms or dispatch addon callbacks.
        NonOreMacerationRows.begin();
        SharedOreCrushingRows.begin();
        SharedOilSandRows.begin();
        mMaceration = new SourceOreMacerationAdapter.Snapshot();
        processOreToCrushed();
        processOreBlocks();
        processDenseIconOres();
        processVanillaIconOres();
        processVanillaOres();
        processMaceration();
        SharedOreWashingRows.initialize();
        SharedPurifiedCentrifugeRows.initialize();
        SharedOreMagneticRows.initialize();
        SharedOreSiftingRows.initialize();
        SharedPurifiedAcidRows.initialize();
        SharedThermalInputRows.initialize();
        SharedThermalRecyclingRows.initialize();
        SharedMineralSteamCrackingRows.initialize();
        processByproductList();
        processRecycling();
        NonOreMacerationRows.seal();
        SourceOreOutputRows.seal();
        SharedOreCrushingRows.seal();
        SharedOilSandRows.seal();
    }

    // ========================================================================
    //  1.  Ore-to-Crushed  (OreProcessing_Ore)
    // ========================================================================

    /**
     * For every material with ore processing:
     * <ul>
     *   <li>Crusher / Hammer / Sifting on {@code oreRaw} (if it exists)</li>
     *   <li>rockGt → crushed for stone-type materials</li>
     *   <li>Furnace form ownership is deferred to the source listener when reviewed</li>
     *   <li>Oilsands centrifuge special case</li>
     * </ul>
     */
    private void processOreToCrushed() {
        for (GTMaterial mat : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = mat.resolve();
            if (!material.isValid() || !MaterialPrefix.crushed.isValidFor(material)) continue;

            boolean hasOreItem = MaterialPrefix.oreRaw.isValidFor(material);
            int baseMultiplier = Math.max(1, Math.min(64,
                    material.getOreMultiplier() * material.getOreProcessingMultiplier()));

            // A bearing rock is 9/4 material units, not a complete ore block.
            ItemStack rock = mat(MaterialPrefix.rockGt, material, 1);
            if (!rock.isEmpty() && !SourceOreMacerationAdapter.retireRock(material, rock, mMaceration)) {
                ItemStack powder = OM.dust(material.getTargetPulverMaterial(),
                        material.getTargetPulverAmount() * 9 / 4);
                if (!powder.isEmpty()) MachineRecipeMaps.pulverizing(rock, powder);
            }

            // ── Oilsands special handling ─────────────────────────────────
            if (isOilsands(material)) {
                registerOilsandsRecipe(material, hasOreItem, baseMultiplier);
                continue;
            }

            // ── Standard ore processing ───────────────────────────────────
            if (!hasOreItem) continue;
            ItemStack oreInput = mat(MaterialPrefix.oreRaw, material, 1);
            if (oreInput.isEmpty()) continue;

            registerOreEntryRecipes(material, oreInput, baseMultiplier, true);


        }
    }

    /** Oilsands centrifuge: oreRaw → Oil fluid + sand dust + byproducts. */
    private void registerOilsandsRecipe(GTMaterial material, boolean hasOreItem, int baseMultiplier) {
        if(hasOreItem) registerOilsandsInput(mat(MaterialPrefix.oreRaw,material,1));
    }

    private void registerOilsandsInput(ItemStack input) {
        if(input.isEmpty())return;
        var oil=GTFluids.still("Oil_Normal");
//? if >=1.21.1 {
        /*if(oil==null||!oil.isBound())throw new IllegalStateException("Oilsands require registered crude oil");
*///?} else {
        if(oil==null||!oil.isPresent())throw new IllegalStateException("Oilsands require registered crude oil");
//?}
        // Preserve this special event's early return: independently wiring the generic machines
        // must not route OilSand through the standard OPL Furnace/byproduct listener.
        var candidate = SharedOilSandRows.candidate(input);
        SharedOreCrushingRows.capture(candidate, MachineRecipeMaps.Crusher, null);
        SharedOreCrushingRows.capture(candidate, MachineRecipeMaps.Hammer, null);
        // Capture only the exact old Centrifuge admission; source host rows replace it after tags bind.
        ItemStack sand=OM.dust(lookup("Sand"),4*U);
        SharedOilSandRows.capture(candidate, MachineRecipeMaps.Centrifuge.addRecipe1(true,EU_TIER,128,input,(FluidStack)null,
                new FluidStack(oil.get(),500),sand));
    }

    /** Keep raw-form ownership separate from the ordinary block's crushing-target permission. */
    private void registerSmelting(GTMaterial material, GTMaterial crushing, ItemStack input, boolean builtinRaw) {
        if (input.isEmpty()) return;
        if (builtinRaw) {
            if (!SourceFurnaceNativeAdapter.earlyFormAllowed(MaterialPrefix.oreRaw, material, input)) return;
        } else {
            var source = SourceFurnaceNativeAdapter.block(material, crushing, input,
                    new SourceFurnaceNativeAdapter.Snapshot());
            if (source.action() != SourceFurnaceNativeAdapter.Action.LEGACY_UNSUPPORTED) {
                source.admit();
                return;
            }
        }
        // Unknown/overridden contexts and vanilla/external inputs retain the former producer.
        // This is intentionally not the expanded native-only source permission above.
        long smeltAmount = material.getTargetSmeltingAmount();
        GTMaterial smeltTarget = material.getTargetSmeltingMaterial();
        if (smeltTarget == null || !smeltTarget.isValid() || smeltAmount <= 0) return;
        if (!ReviewedOreSourceProfile.decision(material, ReviewedOreSourceProfile.Rule.FURNACE)
                .orLegacy(material.has(MaterialProperty.MELTING))) return;

        ItemStack smeltOutput = OM.dustOrIngot(smeltTarget, smeltAmount);
        if (smeltOutput.isEmpty()) {
            smeltOutput = OM.gem(smeltTarget, smeltAmount);
        }
        if (smeltOutput.isEmpty()) return;

        MachineRecipeMaps.add_smelting(input, smeltOutput.copy(), 0.0f, true, false, false);
    }

    // ========================================================================
    //  1b.  Ore Blocks → Crushed (mined ore blocks enter the chain directly)
    // ========================================================================

    /**
     * GT6 fed the ore <em>block</em> itself into Crusher/Hammer (prefix {@code ore}).
     * The port's ore blocks drop themselves (with the stone NBT), so the block item
     * uses the same primary crushing rule as oreRaw. Host-specific processing remains separate.
     */
    private void processOreBlocks() {
        for (GTMaterial mat : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = mat.resolve();
            if (!material.isValid() || !MaterialPrefix.crushed.isValidFor(material)) continue;

            int baseMultiplier = Math.max(1, Math.min(64,
                    material.getOreMultiplier() * material.getOreProcessingMultiplier()));

            ItemStack oreBlock = blockStack(gregtech.engine.prefix.BlockMaterialPrefix.ore, material);
            if (!oreBlock.isEmpty()) {
                registerOreEntryRecipes(material, oreBlock, baseMultiplier);
            }

            // GT6 RecipeMapHammer/RecipeMapHandlerCrushing explicitly leave oreSmall unsupported.
            // Do not invent a half-yield standard-ore recipe for it.

        }
    }

    /** GT6 BlockRockOres and BlockCrystalOres are OP.oreDense: twice a standard ore yield. */
    private void processDenseIconOres() {
//? if >=1.21.1 {
        /*for (var entry
                : gregtech.content.registry.GTSpecialOreBlocks.all()) {
            if (!entry.isBound() || !(entry.get() instanceof gregtech.content.block.DenseOreBlock denseOre))
*///?} else {
        for (RegistryObject<net.minecraft.world.level.block.Block> entry
                : gregtech.content.registry.GTIconSetBlocks.all()) {
            if (!entry.isPresent() || !(entry.get() instanceof gregtech.content.block.DenseOreBlock denseOre))
//?}
                continue;
            net.minecraft.world.level.block.Block block = entry.get();
            GTMaterial material = denseOre.material().resolve();
            if (!material.isValid()) continue;
            int multiplier = Math.max(1, Math.min(64,
                    material.getOreMultiplier() * material.getOreProcessingMultiplier() * 2));
            registerOreEntryRecipes(material, new ItemStack(block), multiplier);
        }
    }

    /** GT6 BlockVanillaOresA is OP.oreVanillastone: one standard ore yield, not oreDense. */
    private void processVanillaIconOres() {
//? if >=1.21.1 {
        /*for (var entry
                : gregtech.content.registry.GTSpecialOreBlocks.all()) {
            if (!entry.isBound() || !(entry.get() instanceof gregtech.content.block.VanillaOreBlock ore))
*///?} else {
        for (RegistryObject<net.minecraft.world.level.block.Block> entry
                : gregtech.content.registry.GTIconSetBlocks.all()) {
            if (!entry.isPresent() || !(entry.get() instanceof gregtech.content.block.VanillaOreBlock ore))
//?}
                continue;
            GTMaterial material = ore.material().resolve();
            if (!material.isValid()) continue;
            int multiplier = Math.max(1, Math.min(64,
                    material.getOreMultiplier() * material.getOreProcessingMultiplier()));
            registerOreEntryRecipes(material, new ItemStack(entry.get()), multiplier);
        }
    }

    private void processVanillaOres() {
        // Vanilla ancient debris is a normal 2U ore for GT processing even though its material
        // deliberately has no FURNACE flag; the vanilla smelting row stays authoritative.
        for(String[] pair:new String[][]{{"nether_quartz_ore","NetherQuartz"},{"nether_gold_ore","Gold"},{"ancient_debris","AncientDebris"}}) {
            GTMaterial material=lookup(pair[1]);
//? if >=1.21.1 {
            /*var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:"+pair[0]));
*///?} else {
            var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("minecraft:"+pair[0]));
//?}
            if(material!=null&&material.isValid()&&item!=null)registerOreEntryRecipes(material,new ItemStack(item),Math.max(1,material.getOreMultiplier()*material.getOreProcessingMultiplier()));
        }
        for(String[] pair : new String[][]{{"iron","Iron"},{"gold","Gold"},{"copper","Copper"},{"coal","Coal"},
                {"redstone","Redstone"},{"lapis","Lapis"},{"diamond","Diamond"},{"emerald","Emerald"}}) {
            GTMaterial material=lookup(pair[1]);
            if(material==null||!material.isValid())continue;
            if(pair[0].equals("iron")||pair[0].equals("gold")||pair[0].equals("copper")) {
                for(String suffix:new String[]{"","_block"}) {
//? if >=1.21.1 {
                    /*var raw=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:raw_"+pair[0]+suffix));
*///?} else {
                    var raw=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("minecraft:raw_"+pair[0]+suffix));
//?}
                    if(raw!=null&&raw!=net.minecraft.world.item.Items.AIR)
                        registerOreEntryRecipes(material,new ItemStack(raw),Math.max(1,material.getOreMultiplier()*material.getOreProcessingMultiplier())*(suffix.isEmpty()?1:9));
                }
            }
            for(String prefix:new String[]{"","deepslate_"}) {
//? if >=1.21.1 {
                /*var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:"+prefix+pair[0]+"_ore"));
*///?} else {
                var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("minecraft:"+prefix+pair[0]+"_ore"));
//?}
                if(item!=null&&item!=net.minecraft.world.item.Items.AIR)
                    registerOreEntryRecipes(material,new ItemStack(item),Math.max(1,material.getOreMultiplier()*material.getOreProcessingMultiplier()));
            }
        }
    }

    /** Standard Crusher / Hammer / smelting for one ore-shaped input stack. */
    private void registerOreEntryRecipes(GTMaterial material, ItemStack oreInput, int multiplier) {
        registerOreEntryRecipes(material, oreInput, multiplier, false);
    }

    /** Only processOreToCrushed supplies builtinRaw; block and vanilla ownership remain distinct. */
    private void registerOreEntryRecipes(GTMaterial material, ItemStack oreInput, int multiplier, boolean builtinRaw) {
        if (isOilsands(material)) { registerOilsandsInput(oreInput); return; }

        var crushing = SharedOreCrushingRows.candidate(material, oreInput, builtinRaw);
        GTMaterial target = material.getTargetCrushingMaterial().resolve();
        int count = (int)Math.min(64, material.getTargetCrushingAmount() * multiplier / U);
        if (count <= 0) {
            SharedOreCrushingRows.capture(crushing, MachineRecipeMaps.Crusher, null);
            SharedOreCrushingRows.capture(crushing, MachineRecipeMaps.Hammer, null);
            return;
        }
        ItemStack out = mat(MaterialPrefix.crushed, target, count);
        if (out.isEmpty()) out = mat(MaterialPrefix.dust, target, count);
        if (out.isEmpty()) {
            SharedOreCrushingRows.capture(crushing, MachineRecipeMaps.Crusher, null);
            SharedOreCrushingRows.capture(crushing, MachineRecipeMaps.Hammer, null);
            return;
        }
        long quality = Math.max(1, material.getToolQuality() + 1);
        boolean dormantRaw = builtinRaw && NonOreMacerationRows.reviewed(material)
                && !SourceOreInputForms.allowGeneratedInput(material, MaterialPrefix.oreRaw, oreInput,
                        SourceOreInputForms.Evidence.NO);
        if (dormantRaw) {
            if (mRawDefinitions.add(material)) {
                NonOreMacerationRows.register(material, MaterialPrefix.oreRaw, "raw/crusher", MachineRecipeMaps.Crusher,
                        true, oreInput, new ItemStack[]{out, out.copy()}, null, new FluidStack[0], 128L * count * quality, EU_TIER);
                NonOreMacerationRows.register(material, MaterialPrefix.oreRaw, "raw/hammer", MachineRecipeMaps.Hammer,
                        true, oreInput, new ItemStack[]{out.copy()}, null, new FluidStack[0], 32L * count * quality, EU_TIER);
            }
        } else {
            // Unreviewed/external/startup overrides retain original direct admission and ordering.
            // GT6 crusher has two guaranteed primary outputs; hammer has one.
            if (ReviewedCinnabarFacts.supports(material)
                    && oreInput.getItem()==mat(MaterialPrefix.oreRaw,material,1).getItem()) {
                SourceOreFollowonRows.rawCrusher(material,ReviewedCinnabarFacts.INSTANCE);
            } else SharedOreCrushingRows.capture(crushing, MachineRecipeMaps.Crusher,
                    MachineRecipeMaps.Crusher.addRecipe1(true, EU_TIER, 128L * count * quality, oreInput, out, out.copy()));
            SharedOreCrushingRows.capture(crushing, MachineRecipeMaps.Hammer,
                    MachineRecipeMaps.Hammer.addRecipe1(true, EU_TIER, 32L * count * quality, oreInput, out.copy()));
        }
        // Furnace ownership/eligibility is separate, even when the two machine rows are dormant.
        // Sifting is for DUST_ORE prefixes, not ordinary raw/stone ores.
        registerSmelting(material, target, oreInput, builtinRaw);
    }

    /** ItemStack of the registered ore block ({@code ore_<material>} / {@code ore_small_<material>}), or EMPTY. */
    private static ItemStack blockStack(gregtech.engine.prefix.BlockMaterialPrefix prefix, GTMaterial material) {
        if (prefix == null) return ItemStack.EMPTY;
//? if >=1.21.1 {
        /*net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", prefix.getBlockId(material)));
*///?} else {
        net.minecraft.world.level.block.Block block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(gregtech.GregTech.NAMESPACE, prefix.getBlockId(material)));
//?}
        if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(block);
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    // ========================================================================
    //  2.  Maceration  (OreProcessing_Maceration)
    // ========================================================================

    /** Native prefix Shredder handlers; unsupported contexts retain the former compatibility producer. */
    private void processMaceration() {
        var reviewedSeen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<GTMaterial, Boolean>());
        for (GTMaterial mat : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = mat.resolve();
            if (NonOreMacerationRows.reviewed(material) && !reviewedSeen.add(material)) continue;
            if (!material.isValid()) continue;

            var claimed = new java.util.HashSet<MaterialPrefix>();
            // The legacy shapes below are consulted only when the source handler does not claim that input.
            if (pulverizeOne(MaterialPrefix.crushed, material, 1, MaterialPrefix.dust, U)) claimed.add(MaterialPrefix.crushed);
            // crushedTiny → dustTiny
            if (pulverizeOne(MaterialPrefix.crushedTiny, material, 1, MaterialPrefix.dustTiny, U9)) claimed.add(MaterialPrefix.crushedTiny);
            // crushedPurified → dust
            if (pulverizeOne(MaterialPrefix.crushedPurified, material, 1, MaterialPrefix.dust, U)) claimed.add(MaterialPrefix.crushedPurified);
            // crushedCentrifuged → 11 × dustTiny
            if (pulverizeMulti(MaterialPrefix.crushedCentrifuged, material, 1, MaterialPrefix.dustTiny, 11, U9)) claimed.add(MaterialPrefix.crushedCentrifuged);
            // crushedCentrifugedTiny → dustTiny
            if (pulverizeOne(MaterialPrefix.crushedCentrifugedTiny, material, 1, MaterialPrefix.dustTiny, U9)) claimed.add(MaterialPrefix.crushedCentrifugedTiny);
            // crushedPurifiedTiny → dustTiny
            if (pulverizeOne(MaterialPrefix.crushedPurifiedTiny, material, 1, MaterialPrefix.dustTiny, U9)) claimed.add(MaterialPrefix.crushedPurifiedTiny);
            // Hand off only unclaimed shapes to the previous reviewed producer, not duplicate registrations.
            if (claimed.size() < SourceOreShreddingRules.shapes().size()) {
                if (ReviewedCinnabarFacts.supports(material)) SourceOreFollowonRows.shredder(material,ReviewedCinnabarFacts.INSTANCE,claimed);
                else if (ReviewedOreShredderFacts.supports(material))
                    SourceOreFollowonRows.shredder(material, ReviewedOreShredderFacts.INSTANCE,claimed);
            }
        }
    }

    /**
     * Retain the port's old single-output compatibility fallback only for unclaimed inputs.
     */
    private boolean pulverizeOne(MaterialPrefix inputPref, GTMaterial material, int inCount,
                              MaterialPrefix outputPref, long outAmount) {
        ItemStack input = mat(inputPref, material, inCount);
        if (sourceShredding(material, inputPref, input)) return true;
        if (!inputPref.isValidFor(material) || input.isEmpty()) return false;
        ItemStack output = stackFor(outputPref, material, outAmount);
        if (output.isEmpty()) return false;
        if (ReviewedCinnabarFacts.supports(material) || ReviewedOreShredderFacts.supports(material)) {
            // Unsupported contexts preserve the former reviewed Crusher/follow-on split.
            MachineRecipeMaps.Crusher.addRecipe1(true,16,64,new long[]{10000,1000,1000},input,output,ItemStack.EMPTY,ItemStack.EMPTY);
        } else if (NonOreMacerationRows.reviewed(material)) {
            for (var map : new gregtech.engine.recipe.RecipeMap[]{MachineRecipeMaps.Crusher,MachineRecipeMaps.Shredder})
                NonOreMacerationRows.register(material,inputPref,"maceration/"+inputPref.getName(),map,true,input,
                        new ItemStack[]{output,ItemStack.EMPTY,ItemStack.EMPTY},new long[]{10000,1000,1000},null,64,16);
        } else MachineRecipeMaps.pulverizing(input, output);
        return false;
    }

    /**
     * Retain the port's old counted-output compatibility fallback only for unclaimed inputs.
     */
    private boolean pulverizeMulti(MaterialPrefix inputPref, GTMaterial material, int inCount,
                                MaterialPrefix outputPref, int outCount, long perUnitAmount) {
        ItemStack input = mat(inputPref, material, inCount);
        if (sourceShredding(material, inputPref, input)) return true;
        if (!inputPref.isValidFor(material) || input.isEmpty()) return false;
        ItemStack output = stackFor(outputPref, material, perUnitAmount);
        if (output.isEmpty()) return false;
        ItemStack multiOut = amount(outCount, output);
        if (NonOreMacerationRows.reviewed(material)) {
            for (var map : new gregtech.engine.recipe.RecipeMap[]{MachineRecipeMaps.Crusher,MachineRecipeMaps.Shredder})
                NonOreMacerationRows.register(material,inputPref,"maceration/"+inputPref.getName(),map,true,input,
                        new ItemStack[]{multiOut},null,null,32,EU_TIER);
            return false;
        }
        MachineRecipeMaps.Crusher .addRecipe1(true, EU_TIER, 32, input, multiOut);
        if (!ReviewedCinnabarFacts.supports(material) && !ReviewedOreShredderFacts.supports(material))
            MachineRecipeMaps.Shredder.addRecipe1(true, EU_TIER, 32, input, multiOut);
        return false;
    }

    private boolean sourceShredding(GTMaterial material, MaterialPrefix prefix, ItemStack input) {
        var outcome = SourceOreMacerationAdapter.shredder(material, prefix, input, mMaceration);
        if (outcome.action() == SourceOreMacerationAdapter.Action.LEGACY_UNSUPPORTED) return false;
        if (outcome.row() != null) outcome.row().admit();
        return true;
    }

    // ========================================================================
    //  4.  Byproduct List (informational fake recipes)
    // ========================================================================
    /**
     * For each ore material with byproducts, register a fake recipe on
     * {@link RM#ByProductList} showing the processing chain and its outputs.
     */
    private void processByproductList() {
        for (GTMaterial mat : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = mat.resolve();
            if (!material.isValid()) continue;
            if (material.getByProducts().isEmpty()) continue;
            if (!MaterialPrefix.crushed.isValidFor(material)) continue;
            if (mByProductListed.contains(material)) continue;
            mByProductListed.add(material);

            // Input chain: oreRaw → rockGt → crushed → crushedPurified → crushedCentrifuged
            List<ItemStack> inputs = new ArrayList<>();
            addIfValid(inputs, MaterialPrefix.oreRaw, material, 1);
            addIfValid(inputs, MaterialPrefix.rockGt, material, 1);
            addIfValid(inputs, MaterialPrefix.crushed, material, 1);
            addIfValid(inputs, MaterialPrefix.crushedPurified, material, 1);
            addIfValid(inputs, MaterialPrefix.crushedCentrifuged, material, 1);
            if (inputs.isEmpty()) continue;

            // Outputs: all byproduct materials as dust-or-ingot full units
            List<ItemStack> outputs = new ArrayList<>();
            for (GTMaterial bp : material.getByProducts()) {
                if (bp == null || !bp.isValid()) continue;
                ItemStack bpStack = OM.dustOrIngot(bp, U);
                if (!bpStack.isEmpty()) outputs.add(bpStack);
            }
            if (outputs.isEmpty()) continue;

            MachineRecipeMaps.ByProductList.addFakeRecipe(false,
                    inputs.toArray(RecipeMap.ZL_IS),
                    outputs.toArray(RecipeMap.ZL_IS),
                    null, null, null, null, 0, 0, 0);
        }
    }

    // ========================================================================
    //  5.  Recycling Processing (Melter + Smelter)
    // ========================================================================

    /**
     * Generate Melter + Smelter recipes for materials with composition data
     * and a valid smelting target.
     *
     * <p>Keep one OM-selected dust-or-ingot unit, not the original all-recyclable-item listener.
     * Reviewed source facts choose the output target's phase and amount. Unknown identities and
     * explicit overrides retain the old compatibility branch; a known missing native phase does not.</p>
     */
    private void processRecycling() {
        SourceThermalNativeAdapter.begin();
        var thermalSnapshot = new SourceThermalNativeAdapter.Snapshot();
        for (GTMaterial mat : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = mat.resolve();
            if (!material.isValid()) continue;
            if (material.has(MaterialProperty.ANTIMATTER)) continue;
            if (ReviewedCinnabarThermalRecipes.emitIfSupported(material)) continue;

            ItemStack recyclableInput = OM.dustOrIngot(material, U);
            var thermal = SourceThermalNativeAdapter.inspect(material, recyclableInput, thermalSnapshot);
            SourceThermalNativeAdapter.note(material, thermal);
            if (thermal.action() != SourceThermalNativeAdapter.Action.LEGACY_UNSUPPORTED) {
                for (var row : thermal.rows()) row.admit();
                continue; // Known missing native resources never revive the old input-fluid/144mB row.
            }

            // Explicitly unresolved/overridden environments keep this producer's old compatibility boundary.
            GTMaterial smeltTarget = material.getTargetSmeltingMaterial();
            long smeltAmount = material.getTargetSmeltingAmount();
            if (smeltTarget == null || !smeltTarget.isValid() || smeltAmount <= 0) continue;
            if (!material.has(MaterialProperty.MELTING)) continue;

            if (recyclableInput.isEmpty()) continue;

            // Try to get the molten fluid for this material
            FluidStack moltenFluid = getMoltenFluid(material);
            if (moltenFluid == null) continue;

            long duration = computeRecycleDuration(material, moltenFluid);

            // Melter: item → molten fluid (no item output)
            MachineRecipeMaps.Melter.addRecipe1(true, EU_TIER, duration, recyclableInput,
                    (FluidStack) null, moltenFluid, RecipeMap.ZL_IS);

            // Smelter: item → molten fluid (no item output)
            MachineRecipeMaps.Smelter.addRecipe1(true, EU_TIER, duration, recyclableInput,
                    (FluidStack) null, moltenFluid, RecipeMap.ZL_IS);
        }
    }

    /**
     * Looks up the molten fluid for a material via the {@link FL} + {@link GTFluids} system.
     * Returns {@code null} if no fluid is registered for this material.
     */
    private FluidStack getMoltenFluid(GTMaterial material) {
        String field = "GenMolten_" + material.getName();
        RegisteredFluids.FluidEntry entry = RegisteredFluids.get(field);
        if (entry == null) return null;

//? if >=1.21.1 {
        /*var fluidRO = GTFluids.still(field);
        if (fluidRO == null || !fluidRO.isBound()) return null;
*///?} else {
        RegistryObject<Fluid> fluidRO = GTFluids.still(field);
        if (fluidRO == null || !fluidRO.isPresent()) return null;
//?}

        // GT6 convention: 1 ingot-worth of material = 144 mB of fluid
        return new FluidStack(fluidRO.get(), 144);
    }

    /** Legacy unsupported-context duration, intentionally not the reviewed source calculation. */
    private long computeRecycleDuration(GTMaterial material, FluidStack moltenFluid) {
        int fluidTemp = RegisteredFluids.get("GenMolten_" + material.getName()) != null
                ? RegisteredFluids.get("GenMolten_" + material.getName()).temperature()
                : material.getMeltingPoint();
        int temp = Math.max(material.getMeltingPoint(), fluidTemp);
        return Math.max(16, (temp - 300) / 16L);
    }

    // ========================================================================
    //  Utility helpers
    // ========================================================================

    /** Shortcut for {@link MaterialStackItemHelper#mat}. */
    private static ItemStack mat(MaterialPrefix prefix, GTMaterial material, int count) {
        return MaterialStackItemHelper.mat(prefix, material, count);
    }

    /** Resolve output ItemStack for a prefix+material+amount. */
    private static ItemStack stackFor(MaterialPrefix prefix, GTMaterial material, long amountU) {
        if (!prefix.isValidFor(material)) return ItemStack.EMPTY;
        // Try to use the prefix directly if amount matches its weight
        return mat(prefix, material, Math.max(1, (int) (amountU / Math.max(1, prefix.getMaterialWeight()))));
    }

    /** Creates a copy of {@code stack} with the given count (clamped to [1,64]). */
    private static ItemStack amount(int count, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(Math.max(1, Math.min(64, count)));
        return copy;
    }

    /** Add a prefix+material stack to a list if it is valid and non-empty. */
    private static void addIfValid(List<ItemStack> list, MaterialPrefix prefix, GTMaterial material, int count) {
        if (!prefix.isValidFor(material)) return;
        ItemStack stack = mat(prefix, material, count);
        if (!stack.isEmpty()) list.add(stack);
    }

    /**
     * Returns a tiny dust stack for the byproduct at {@code index} in the list.
     * Falls back to {@code fallback} if the index is out of bounds, or
     * {@code material}'s own tiny dust if both are empty.
     */
    private static ItemStack byProductTiny(List<GTMaterial> byProds, int index, ItemStack fallback) {
        if (index < byProds.size()) {
            GTMaterial bp = byProds.get(index);
            if (bp != null && bp.isValid()) {
                ItemStack stack = OM.dustOrIngot(bp, U9);
                if (!stack.isEmpty()) return stack;
            }
        }
        return fallback.isEmpty() ? ItemStack.EMPTY : fallback;
    }

    /**
     * Returns a tiny dust (U9) for the byproduct at the given index.
     * Falls back to the material itself if the index is out of range.
     */
    private static ItemStack byProductTiny(List<GTMaterial> byProds, int index, GTMaterial material) {
        if (index < byProds.size()) {
            GTMaterial bp = byProds.get(index);
            if (bp != null && bp.isValid()) {
                ItemStack stack = OM.dustOrIngot(bp, U9);
                if (!stack.isEmpty()) return stack;
            }
        }
        return OM.dustOrIngot(material, U9);
    }

    /** Resolve a material by name via the registry. */
    private static GTMaterial lookup(String name) {
        GTMaterial m = GTMaterialRegistry.get(name);
        return m != null && m.isValid() ? m.resolve() : null;
    }

    /** Check if a material is oilsands (internal name = "OilSand"). */
    private static boolean isOilsands(GTMaterial material) {
        return "OilSand".equals(material.getName());
    }
}
