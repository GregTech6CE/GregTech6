package com.gregtech.gregtech.loaders.c;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.util.OM;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;


import java.util.ArrayList;
import java.util.List;

import static com.gregtech.gregtech.api.material.GTValues.*;

/**
 * GT6 ore-processing machine recipes.
 *
 * <p>Port of {@code gregtech.loaders.b.Loader_OreProcessing} (OreProcessing_Ore,
 * OreProcessing_Maceration, washing, byproduct listing, and recycling).</p>
 *
 * <p>Registered standard ores use GT6 crushing targets and separate hammer/crusher yields.
 * Small ore mining is handled by SmallOreDrops, not a standard-ore recipe.</p>
 */
public class Loader_Recipes_OreProcessing implements IGTLoader {

    /** Chances are 1/100th of a percent: 10_000 = 100 %. */
    private static final long CHANCE_100 = 10_000L;
    private static final long CHANCE_50  =  5_000L;
    private static final long CHANCE_15  =  1_500L;
    private static final long CHANCE_10  =  1_000L;
    private static final long CHANCE_5   =    500L;

    /** Default EU/t for the ore processing tier. */
    private static final long EU_TIER = 16;

    /**
     * GT6 {@code Loader_Recipes_Ores} chances for the magnetic separator
     * ({@code tMagnet}): primary output always, five byproduct fines at 6 % each.
     */
    private static final long[] MAGNET_CHANCES = {CHANCE_100, 600, 600, 600, 600, 600};

    /** GT6 sifting chances for purified ore: legendary → dust, per 10 000. */
    private static final long[] SIFT_GEM_CHANCES = {9, 90, 360, 1_350, 1_800, 3_600, 4_500};
    /** Same route from the tiny purified form (GT6 divides every chance by ten). */
    private static final long[] SIFT_GEM_CHANCES_TINY = {1, 10, 40, 150, 200, 400, 500};

    /**
     * GT6 {@code Loader_Recipes_Ores:383} {@code tSluice = {10000, 300, 300, 300, 300, 300, 300,
     * 300, 300}} for the eight outputs of a sluice recipe: the purified ore itself and seven
     * byproduct fines at 3 % each (GT6's ninth entry is unused — the recipe has eight outputs).
     */
    private static final long[] SLUICE_CHANCES = {CHANCE_100, 300, 300, 300, 300, 300, 300, 300};

    private static final FluidStack WATER_MB = new FluidStack(Fluids.WATER, 1000);

    /** Tracks materials already added to the ByProductList (avoids duplicates). */
    private final List<GTMaterial> mByProductListed = new ArrayList<>();

    @Override
    public void run() {
        processOreToCrushed();
        processOreBlocks();
        processDenseIconOres();
        processVanillaIconOres();
        processVanillaOres();
        processMaceration();
        processWashing();
        processPurifiedRoutes();
        processSluiceRoutes();
        processByproductList();
        processRecycling();
    }

    // ========================================================================
    //  1.  Ore-to-Crushed  (OreProcessing_Ore)
    // ========================================================================

    /**
     * For every material with ore processing:
     * <ul>
     *   <li>Crusher / Hammer / Sifting on {@code oreRaw} (if it exists)</li>
     *   <li>rockGt → crushed for stone-type materials</li>
     *   <li>Furnace smelting if the material has melting data</li>
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
            if (!rock.isEmpty()) {
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

            registerOreEntryRecipes(material, oreInput, baseMultiplier);


        }
    }

    /** Oilsands centrifuge: oreRaw → Oil fluid + sand dust + byproducts. */
    private void registerOilsandsRecipe(GTMaterial material, boolean hasOreItem, int baseMultiplier) {
        if(hasOreItem) registerOilsandsInput(mat(MaterialPrefix.oreRaw,material,1));
    }

    private void registerOilsandsInput(ItemStack input) {
        if(input.isEmpty())return;
        var oil=GTFluids.still("Oil_Normal");
        if(oil==null||!oil.isBound())throw new IllegalStateException("Oilsands require registered crude oil");
        // oreRaw/standard ore are 2 U: 128 ticks, 500 mB oil and 4 U sand.
        ItemStack sand=OM.dust(lookup("Sand"),4*U);
        MachineRecipeMaps.Centrifuge.addRecipe1(true,EU_TIER,128,input,(FluidStack)null,
                new FluidStack(oil.get(),500),sand);
    }

    /** Add furnace smelting for materials with melting data. */
    private void registerSmelting(GTMaterial material, ItemStack... possibleInputs) {
        long smeltAmount = material.getTargetSmeltingAmount();
        GTMaterial smeltTarget = material.getTargetSmeltingMaterial();
        if (smeltTarget == null || !smeltTarget.isValid() || smeltAmount <= 0) return;
        if (!material.has(MaterialProperty.MELTING)) return;

        ItemStack smeltOutput = OM.dustOrIngot(smeltTarget, smeltAmount);
        if (smeltOutput.isEmpty()) {
            smeltOutput = OM.gem(smeltTarget, smeltAmount);
        }
        if (smeltOutput.isEmpty()) return;

        for (ItemStack input : possibleInputs) {
            if (!input.isEmpty()) {
                MachineRecipeMaps.add_smelting(input, smeltOutput.copy(), 0.0f, true, false, false);
            }
        }
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

            ItemStack oreBlock = blockStack(com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.ore, material);
            if (!oreBlock.isEmpty()) {
                registerOreEntryRecipes(material, oreBlock, baseMultiplier);
            }

            // GT6 RecipeMapHammer/RecipeMapHandlerCrushing explicitly leave oreSmall unsupported.
            // Do not invent a half-yield standard-ore recipe for it.

        }
    }

    /** GT6 BlockRockOres and BlockCrystalOres are OP.oreDense: twice a standard ore yield. */
    private void processDenseIconOres() {
        for (var entry
                : com.gregtech.gregtech.registry.GTSpecialOreBlocks.all()) {
            if (!entry.isBound() || !(entry.get() instanceof com.gregtech.gregtech.block.DenseOreBlock denseOre))
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
        for (var entry
                : com.gregtech.gregtech.registry.GTSpecialOreBlocks.all()) {
            if (!entry.isBound() || !(entry.get() instanceof com.gregtech.gregtech.block.VanillaOreBlock ore))
                continue;
            GTMaterial material = ore.material().resolve();
            if (!material.isValid()) continue;
            int multiplier = Math.max(1, Math.min(64,
                    material.getOreMultiplier() * material.getOreProcessingMultiplier()));
            registerOreEntryRecipes(material, new ItemStack(entry.get()), multiplier);
        }
    }

    private void processVanillaOres() {
        for(String[] pair:new String[][]{{"nether_quartz_ore","NetherQuartz"},{"nether_gold_ore","Gold"}}) {
            GTMaterial material=lookup(pair[1]);
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:"+pair[0]));
            if(material!=null&&material.isValid()&&item!=null)registerOreEntryRecipes(material,new ItemStack(item),Math.max(1,material.getOreMultiplier()*material.getOreProcessingMultiplier()));
        }
        for(String[] pair : new String[][]{{"iron","Iron"},{"gold","Gold"},{"copper","Copper"},{"coal","Coal"},
                {"redstone","Redstone"},{"lapis","Lapis"},{"diamond","Diamond"},{"emerald","Emerald"}}) {
            GTMaterial material=lookup(pair[1]);
            if(material==null||!material.isValid())continue;
            if(pair[0].equals("iron")||pair[0].equals("gold")||pair[0].equals("copper")) {
                for(String suffix:new String[]{"","_block"}) {
                    var raw=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:raw_"+pair[0]+suffix));
                    if(raw!=null&&raw!=net.minecraft.world.item.Items.AIR)
                        registerOreEntryRecipes(material,new ItemStack(raw),Math.max(1,material.getOreMultiplier()*material.getOreProcessingMultiplier())*(suffix.isEmpty()?1:9));
                }
            }
            for(String prefix:new String[]{"","deepslate_"}) {
                var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:"+prefix+pair[0]+"_ore"));
                if(item!=null&&item!=net.minecraft.world.item.Items.AIR)
                    registerOreEntryRecipes(material,new ItemStack(item),Math.max(1,material.getOreMultiplier()*material.getOreProcessingMultiplier()));
            }
        }
    }

    /** Standard Crusher / Hammer / smelting for one ore-shaped input stack. */
    private void registerOreEntryRecipes(GTMaterial material, ItemStack oreInput, int multiplier) {
        if (isOilsands(material)) { registerOilsandsInput(oreInput); return; }

        GTMaterial target = material.getTargetCrushingMaterial().resolve();
        int count = (int)Math.min(64, material.getTargetCrushingAmount() * multiplier / U);
        if (count <= 0) return;
        ItemStack out = mat(MaterialPrefix.crushed, target, count);
        if (out.isEmpty()) out = mat(MaterialPrefix.dust, target, count);
        if (out.isEmpty()) return;
        long quality = Math.max(1, material.getToolQuality() + 1);
        // GT6 crusher has two guaranteed primary outputs; hammer has one.
        MachineRecipeMaps.Crusher.addRecipe1(true, EU_TIER, 128L * count * quality, oreInput, out, out.copy());
        MachineRecipeMaps.Hammer.addRecipe1(true, EU_TIER, 32L * count * quality, oreInput, out.copy());
        // Sifting is for DUST_ORE prefixes, not ordinary raw/stone ores.
        registerSmelting(material, oreInput);
    }

    /** ItemStack of the registered ore block ({@code ore_<material>} / {@code ore_small_<material>}), or EMPTY. */
    private static ItemStack blockStack(com.gregtech.gregtech.api.prefix.BlockMaterialPrefix prefix, GTMaterial material) {
        if (prefix == null) return ItemStack.EMPTY;
        net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", prefix.getBlockId(material)));
        if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(block);
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    // ========================================================================
    //  2.  Maceration  (OreProcessing_Maceration)
    // ========================================================================

    /**
     * Crushed → dust chain using Crusher + Shredder ({@link RM#pulverizing}).
     */
    private void processMaceration() {
        for (GTMaterial mat : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = mat.resolve();
            if (!material.isValid()) continue;

            // crushed → dust
            pulverizeOne(MaterialPrefix.crushed, material, 1, MaterialPrefix.dust, U);
            // crushedTiny → dustTiny
            pulverizeOne(MaterialPrefix.crushedTiny, material, 1, MaterialPrefix.dustTiny, U9);
            // crushedPurified → dust
            pulverizeOne(MaterialPrefix.crushedPurified, material, 1, MaterialPrefix.dust, U);
            // crushedCentrifuged → 11 × dustTiny
            pulverizeMulti(MaterialPrefix.crushedCentrifuged, material, 1, MaterialPrefix.dustTiny, 11, U9);
            // crushedCentrifugedTiny → dustTiny
            pulverizeOne(MaterialPrefix.crushedCentrifugedTiny, material, 1, MaterialPrefix.dustTiny, U9);
            // crushedPurifiedTiny → dustTiny
            pulverizeOne(MaterialPrefix.crushedPurifiedTiny, material, 1, MaterialPrefix.dustTiny, U9);
        }
    }

    /**
     * Single pulverizing: inputPrefix → outputPrefix with the given material amounts.
     * Uses RM.pulverizing (Crusher + Shredder).
     */
    private void pulverizeOne(MaterialPrefix inputPref, GTMaterial material, int inCount,
                              MaterialPrefix outputPref, long outAmount) {
        if (!inputPref.isValidFor(material)) return;
        ItemStack input = mat(inputPref, material, inCount);
        if (input.isEmpty()) return;
        ItemStack output = stackFor(outputPref, material, outAmount);
        if (output.isEmpty()) return;
        MachineRecipeMaps.pulverizing(input, output);
    }

    /**
     * Multi-output pulverizing: inputPrefix → count × outputPrefix.
     * Registers directly on Crusher + Shredder (RM.pulverizing only handles
     * 1-3 outputs with vanilla chances).
     */
    private void pulverizeMulti(MaterialPrefix inputPref, GTMaterial material, int inCount,
                                MaterialPrefix outputPref, int outCount, long perUnitAmount) {
        if (!inputPref.isValidFor(material)) return;
        ItemStack input = mat(inputPref, material, inCount);
        if (input.isEmpty()) return;
        ItemStack output = stackFor(outputPref, material, perUnitAmount);
        if (output.isEmpty()) return;
        ItemStack multiOut = amount(outCount, output);
        MachineRecipeMaps.Crusher .addRecipe1(true, EU_TIER, 32, input, multiOut);
        MachineRecipeMaps.Shredder.addRecipe1(true, EU_TIER, 32, input, multiOut);
    }

    // ========================================================================
    //  3.  Washing Chain (Bath + Centrifuge)
    // ========================================================================

    /**
     * After crushing, materials can be washed:
     * <ul>
     *   <li>crushed + water → Bath → crushedPurified + first byproduct dustTiny (50 %)</li>
     *   <li>crushedPurified → Centrifuge → crushedCentrifuged + second byproduct dustTiny (50 %)</li>
     * </ul>
     */
    private void processWashing() {
        for (GTMaterial mat : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = mat.resolve();
            if (!material.isValid() || !MaterialPrefix.crushed.isValidFor(material)) continue;

            List<GTMaterial> byProds = material.getByProducts();

            // ── Bath: crushed + water → crushedPurified + first byproduct ──
            if (MaterialPrefix.crushedPurified.isValidFor(material)) {
                ItemStack crushed = mat(MaterialPrefix.crushed, material, 1);
                ItemStack crushedPurified = mat(MaterialPrefix.crushedPurified, material, 1);
                if (!crushed.isEmpty() && !crushedPurified.isEmpty()) {
                    ItemStack bypTiny = byProductTiny(byProds, 0, material);
                    MachineRecipeMaps.Bath.addRecipe1(true, EU_TIER, 120,
                            new long[]{CHANCE_100, CHANCE_50},
                            crushed, WATER_MB, (FluidStack) null,
                            crushedPurified, bypTiny);
                }
            }

            // ── Centrifuge: crushedPurified → crushedCentrifuged + second byproduct ──
            if (MaterialPrefix.crushedCentrifuged.isValidFor(material)) {
                ItemStack crushedPurified = mat(MaterialPrefix.crushedPurified, material, 1);
                ItemStack crushedCentrifuged = mat(MaterialPrefix.crushedCentrifuged, material, 1);
                if (!crushedPurified.isEmpty() && !crushedCentrifuged.isEmpty()) {
                    ItemStack bypTiny = byProductTiny(byProds, 1, byProductTiny(byProds, 0, material));
                    MachineRecipeMaps.Centrifuge.addRecipe1(true, EU_TIER, 120,
                            new long[]{CHANCE_100, CHANCE_50},
                            crushedPurified, crushedCentrifuged, bypTiny);
                }
            }
        }
    }

    // ========================================================================
    //  3b. Purified routes: Magnetic Separator + Sifting  (GT6 Loader_Recipes_Ores)
    // ========================================================================

    /**
     * GT6's two downstream routes from purified ore
     * ({@code Loader_Recipes_Ores} lines 455-461):
     * <ul>
     *   <li><b>Magnetic Separator</b>: purified → centrifuged, plus five byproduct fines
     *       (guaranteed primary, 6 % per byproduct).</li>
     *   <li><b>Sifting</b>: purified → gem grades and dust, from legendary (0.09 %) down to a
     *       plain gem and the material's own dust (45 %).</li>
     * </ul>
     * Both were missing entirely, which left the Magnetic Separator and Sifter machines with no
     * recipes at all.
     */
    private void processPurifiedRoutes() {
        for (GTMaterial mat : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = mat.resolve();
            if (!material.isValid()) continue;

            ItemStack purified = mat(MaterialPrefix.crushedPurified, material, 1);
            ItemStack purifiedTiny = mat(MaterialPrefix.crushedPurifiedTiny, material, 1);
            if (purified.isEmpty() || purifiedTiny.isEmpty()) continue;

            // GT6 orders the byproducts non-magnetic first, then magnetic (Loader_Recipes_Ores:389-393).
            List<GTMaterial> byProds = magnetOrder(material.getByProducts());

            // ── Magnetic Separator ────────────────────────────────────────────
            ItemStack centrifuged = mat(MaterialPrefix.crushedCentrifuged, material, 1);
            ItemStack centrifugedTiny = mat(MaterialPrefix.crushedCentrifugedTiny, material, 1);
            if (!byProds.isEmpty() && !centrifuged.isEmpty() && !centrifugedTiny.isEmpty()) {
                MachineRecipeMaps.MagneticSeparator.addRecipe1(true, EU_TIER, 144, MAGNET_CHANCES,
                        purified, centrifuged,
                        byProductCentrifugedTiny(byProds, 0, 18, material),
                        byProductCentrifugedTiny(byProds, 1, 18, material),
                        byProductCentrifugedTiny(byProds, 2, 18, material),
                        byProductCentrifugedTiny(byProds, 3, 18, material),
                        byProductCentrifugedTiny(byProds, 4, 18, material));
                MachineRecipeMaps.MagneticSeparator.addRecipe1(true, EU_TIER, 16, MAGNET_CHANCES,
                        purifiedTiny, centrifugedTiny,
                        byProductCentrifugedTiny(byProds, 0, 2, material),
                        byProductCentrifugedTiny(byProds, 1, 2, material),
                        byProductCentrifugedTiny(byProds, 2, 2, material),
                        byProductCentrifugedTiny(byProds, 3, 2, material),
                        byProductCentrifugedTiny(byProds, 4, 2, material));
            }

            // ── Sifting (gem route) ───────────────────────────────────────────
            if (!MaterialPrefix.gem.isValidFor(material)) continue;
            ItemStack[] outputs = gemRouteOutputs(material);
            if (outputs == null) continue;
            MachineRecipeMaps.Sifting.addRecipe1(true, EU_TIER, 144, SIFT_GEM_CHANCES,
                    purified, outputs[0], outputs[1], outputs[2], outputs[3], outputs[4], outputs[5], outputs[6]);
            MachineRecipeMaps.Sifting.addRecipe1(true, EU_TIER, 16, SIFT_GEM_CHANCES_TINY,
                    purifiedTiny, outputs[0].copy(), outputs[1].copy(), outputs[2].copy(),
                    outputs[3].copy(), outputs[4].copy(), outputs[5].copy(), outputs[6].copy());
        }
    }

    /** Gem grade ladder of GT6's sifting route, or {@code null} when the gem form is missing. */
    private static ItemStack[] gemRouteOutputs(GTMaterial material) {
        ItemStack gem = mat(MaterialPrefix.gem, material, 1);
        ItemStack dust = mat(MaterialPrefix.dust, material, 1);
        if (gem.isEmpty() || dust.isEmpty()) return null;
        // GT6 scales the rarer grades by the gem's stack value: 8x, 4x, 2x, then plain 1x.
        ItemStack legendary = mat(MaterialPrefix.gemLegendary, material, 8);
        ItemStack exquisite = mat(MaterialPrefix.gemExquisite, material, 4);
        ItemStack flawless = mat(MaterialPrefix.gemFlawless, material, 2);
        ItemStack flawed = mat(MaterialPrefix.gemFlawed, material, 2);
        ItemStack chipped = mat(MaterialPrefix.gemChipped, material, 4);
        if (legendary.isEmpty() || exquisite.isEmpty() || flawless.isEmpty()
                || flawed.isEmpty() || chipped.isEmpty()) return null;
        return new ItemStack[]{legendary, exquisite, flawless, gem, flawed, chipped, dust};
    }

    // ========================================================================
    //  3c. Sluice  (GT6 Loader_Recipes_Ores:415-420)
    // ========================================================================

    /**
     * GT6's sluice route, which the port was missing entirely — leaving the four Sluice tiers
     * and the Large Sluice with an empty recipe map.
     * <p>
     * {@code Loader_Recipes_Ores:415-420}: a crushed ore plus 900 mB of any water variant
     * (water, mineral water, distilled water, spectral dew) yields one purified ore plus seven
     * byproduct fines of 9 each, in 144 ticks; the tiny crushed ore form takes 100 mB and 16
     * ticks and yields the same set at 1 each. Both are registered for every water variant, and
     * left-over water leaves the machine as GT6's {@code FL.Sluice} fluid.
     * </p>
     */
    private void processSluiceRoutes() {
        FluidStack[] water900 = waterVariants(900);
        FluidStack[] water100 = waterVariants(100);
        FluidStack sluice900 = fluidStack("Sluice", 900);
        FluidStack sluice100 = fluidStack("Sluice", 100);
        if (water900.length == 0 || sluice900 == null || sluice100 == null) return;

        for (GTMaterial mat : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = mat.resolve();
            if (!material.isValid() || !MaterialPrefix.crushed.isValidFor(material)) continue;

            ItemStack crushed = mat(MaterialPrefix.crushed, material, 1);
            ItemStack crushedTiny = mat(MaterialPrefix.crushedTiny, material, 1);
            ItemStack purified = mat(MaterialPrefix.crushedPurified, material, 1);
            ItemStack purifiedTiny = mat(MaterialPrefix.crushedPurifiedTiny, material, 1);
            if (crushed.isEmpty() || crushedTiny.isEmpty() || purified.isEmpty() || purifiedTiny.isEmpty()) {
                continue;
            }

            // GT6 takes the first seven byproducts in registration order and falls back to the
            // material itself when there are fewer (Loader_Recipes_Ores:416).
            List<GTMaterial> byProds = material.getByProducts();
            ItemStack[] fines9 = new ItemStack[7];
            ItemStack[] fines1 = new ItemStack[7];
            boolean complete = true;
            for (int i = 0; i < 7; i++) {
                GTMaterial product = i < byProds.size() && byProds.get(i) != null && byProds.get(i).isValid()
                        ? byProds.get(i) : material;
                fines9[i] = mat(MaterialPrefix.crushedPurifiedTiny, product, 9);
                fines1[i] = mat(MaterialPrefix.crushedPurifiedTiny, product, 1);
                complete &= !fines9[i].isEmpty() && !fines1[i].isEmpty();
            }
            if (!complete) continue;

            for (FluidStack water : water900) {
                MachineRecipeMaps.Sluice.addRecipe1(true, EU_TIER, 144, SLUICE_CHANCES,
                        crushed, water, sluice900.copy(), purified,
                        fines9[0].copy(), fines9[1].copy(), fines9[2].copy(), fines9[3].copy(),
                        fines9[4].copy(), fines9[5].copy(), fines9[6].copy());
            }
            for (FluidStack water : water100) {
                MachineRecipeMaps.Sluice.addRecipe1(true, EU_TIER, 16, SLUICE_CHANCES,
                        crushedTiny, water, sluice100.copy(), purifiedTiny,
                        fines1[0].copy(), fines1[1].copy(), fines1[2].copy(), fines1[3].copy(),
                        fines1[4].copy(), fines1[5].copy(), fines1[6].copy());
            }
        }
    }

    /** GT6 {@code FL.waters(mB)}: water, mineral water, distilled water, spectral dew. */
    private static FluidStack[] waterVariants(int mb) {
        List<FluidStack> result = new ArrayList<>();
        for (String name : new String[]{"Water", "MnWtr", "DistW", "SpDew"}) {
            FluidStack stack = fluidStack(name, mb);
            if (stack != null) result.add(stack);
        }
        return result.toArray(new FluidStack[0]);
    }

    /** A registered GT6 fluid as a {@link FluidStack}, or {@code null} when it is not registered. */
    private static FluidStack fluidStack(String field, int mb) {
        return GTFluids.stack(field, mb);
    }

    /** Byproducts ordered the way GT6's magnetic separator consumes them. */
    private static List<GTMaterial> magnetOrder(List<GTMaterial> byProds) {
        List<GTMaterial> ordered = new ArrayList<>();
        for (GTMaterial byProduct : byProds) {
            if (byProduct != null && byProduct.isValid() && !byProduct.has(MaterialProperty.MAGNETIC)) ordered.add(byProduct);
        }
        for (GTMaterial byProduct : byProds) {
            if (byProduct != null && byProduct.isValid() && byProduct.has(MaterialProperty.MAGNETIC)) ordered.add(byProduct);
        }
        return ordered;
    }

    /** {@code count} tiny centrifuged ore of the byproduct at {@code index}, else of {@code fallback}. */
    private static ItemStack byProductCentrifugedTiny(List<GTMaterial> byProds, int index, int count,
                                                     GTMaterial fallback) {
        if (index < byProds.size()) {
            GTMaterial byProduct = byProds.get(index);
            if (byProduct != null && byProduct.isValid()) {
                ItemStack stack = mat(MaterialPrefix.crushedCentrifugedTiny, byProduct, count);
                if (!stack.isEmpty()) return stack;
            }
        }
        return mat(MaterialPrefix.crushedCentrifugedTiny, fallback, count);
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
     * <p>This is a simplified port of {@code RecyclingProcessing}: instead of
     * iterating every registered item, we iterate all materials and generate
     * recipes that convert the dust/ingot form into the material's molten
     * fluid (when available) or smelting output.</p>
     */
    private void processRecycling() {
        for (GTMaterial mat : GTMaterialRegistry.allMaterials()) {
            GTMaterial material = mat.resolve();
            if (!material.isValid()) continue;
            if (material.has(MaterialProperty.ANTIMATTER)) continue;

            GTMaterial smeltTarget = material.getTargetSmeltingMaterial();
            long smeltAmount = material.getTargetSmeltingAmount();
            if (smeltTarget == null || !smeltTarget.isValid() || smeltAmount <= 0) continue;
            if (!material.has(MaterialProperty.MELTING)) continue;

            // Use dust form as the recyclable input
            ItemStack recyclableInput = OM.dustOrIngot(material, U);
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

        var fluidRO = GTFluids.still(field);
        if (fluidRO == null || !fluidRO.isBound()) return null;

        // GT6 convention: 1 ingot-worth of material = 144 mB of fluid
        return new FluidStack(fluidRO.get(), 144);
    }

    /**
     * Duration for recycling: scaled by material melting point (original uses
     * {@code (weight * (max(melting, fluidTemp) - 300)) / 1600}).
     */
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
