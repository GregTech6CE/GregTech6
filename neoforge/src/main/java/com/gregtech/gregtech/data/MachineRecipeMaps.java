package com.gregtech.gregtech.data;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import com.gregtech.gregtech.api.material.MaterialProperty;
/** Same shared original maps with Neo ItemStack/FluidStack rows. */
public final class MachineRecipeMaps {
    private MachineRecipeMaps() {}
    public static final RecipeMap Furnace=fromDefinition(MachineRecipeMapDefinitions.Furnace);
    public static final RecipeMap Microwave=fromDefinition(MachineRecipeMapDefinitions.Microwave);
    public static final RecipeMap Roasting=fromDefinition(MachineRecipeMapDefinitions.Roasting);
    public static final RecipeMap Distillery=fromDefinition(MachineRecipeMapDefinitions.Distillery);
    public static final RecipeMap Extruder=fromDefinition(MachineRecipeMapDefinitions.Extruder);
    public static final RecipeMap Smelter=fromDefinition(MachineRecipeMapDefinitions.Smelter);
    public static final RecipeMap CrystallisationCrucible=fromDefinition(MachineRecipeMapDefinitions.CrystallisationCrucible);
    public static final RecipeMap Drying=fromDefinition(MachineRecipeMapDefinitions.Drying);
    public static final RecipeMap Laminator=fromDefinition(MachineRecipeMapDefinitions.Laminator);
    public static final RecipeMap CatalyticCracking=fromDefinition(MachineRecipeMapDefinitions.CatalyticCracking);
    public static final RecipeMap SteamCracking=fromDefinition(MachineRecipeMapDefinitions.SteamCracking);
    public static final RecipeMap DistillationTower=fromDefinition(MachineRecipeMapDefinitions.DistillationTower);
    public static final RecipeMap CryoDistillationTower=fromDefinition(MachineRecipeMapDefinitions.CryoDistillationTower);
    public static final RecipeMap Shredder=fromDefinition(MachineRecipeMapDefinitions.Shredder);
    public static final RecipeMap Lathe=fromDefinition(MachineRecipeMapDefinitions.Lathe);
    public static final RecipeMap Cutter=fromDefinition(MachineRecipeMapDefinitions.Cutter);
    public static final RecipeMap Centrifuge=fromDefinition(MachineRecipeMapDefinitions.Centrifuge);
    public static final RecipeMap RollingMill=fromDefinition(MachineRecipeMapDefinitions.RollingMill);
    public static final RecipeMap RollBender=fromDefinition(MachineRecipeMapDefinitions.RollBender);
    public static final RecipeMap RollFormer=fromDefinition(MachineRecipeMapDefinitions.RollFormer);
    public static final RecipeMap ClusterMill=fromDefinition(MachineRecipeMapDefinitions.ClusterMill);
    public static final RecipeMap Wiremill=fromDefinition(MachineRecipeMapDefinitions.Wiremill);
    public static final RecipeMap Mixer=fromDefinition(MachineRecipeMapDefinitions.Mixer);
    public static final RecipeMap Loom=fromDefinition(MachineRecipeMapDefinitions.Loom);
    public static final RecipeMap Sluice=fromDefinition(MachineRecipeMapDefinitions.Sluice);
    public static final RecipeMap Sharpening=fromDefinition(MachineRecipeMapDefinitions.Sharpening);
    public static final RecipeMap BurnMixer=fromDefinition(MachineRecipeMapDefinitions.BurnMixer);
    public static final RecipeMap PressureWasher=fromDefinition(MachineRecipeMapDefinitions.PressureWasher);
    public static final RecipeMap Crusher=fromDefinition(MachineRecipeMapDefinitions.Crusher);
    public static final RecipeMap Sifting=fromDefinition(MachineRecipeMapDefinitions.Sifting);
    public static final RecipeMap Squeezer=fromDefinition(MachineRecipeMapDefinitions.Squeezer);
    public static final RecipeMap Compressor=fromDefinition(MachineRecipeMapDefinitions.Compressor);
    public static final RecipeMap Press=fromDefinition(MachineRecipeMapDefinitions.Press);
    public static final RecipeMap Electrolyzer=fromDefinition(MachineRecipeMapDefinitions.Electrolyzer);
    public static final RecipeMap Canner=fromDefinition(MachineRecipeMapDefinitions.Canner);
    public static final RecipeMap Injector=fromDefinition(MachineRecipeMapDefinitions.Injector);
    public static final RecipeMap Printer=fromDefinition(MachineRecipeMapDefinitions.Printer);
    public static final RecipeMap ScannerVisuals=fromDefinition(MachineRecipeMapDefinitions.ScannerVisuals);
    public static final RecipeMap Autocrafter=fromDefinition(MachineRecipeMapDefinitions.Autocrafter).contextualRecipes(com.gregtech.gregtech.content.recipe.AutocraftingRecipes::find);
    public static final RecipeMap Slicer=fromDefinition(MachineRecipeMapDefinitions.Slicer);
    public static final RecipeMap Nanofab=fromDefinition(MachineRecipeMapDefinitions.Nanofab);
    public static final RecipeMap Plantalyzer=fromDefinition(MachineRecipeMapDefinitions.Plantalyzer);
    public static final RecipeMap Bumblelyzer=fromDefinition(MachineRecipeMapDefinitions.Bumblelyzer);
    public static final RecipeMap Boxinator=fromDefinition(MachineRecipeMapDefinitions.Boxinator);
    public static final RecipeMap Unboxinator=fromDefinition(MachineRecipeMapDefinitions.Unboxinator);
    public static final RecipeMap Polarizer=fromDefinition(MachineRecipeMapDefinitions.Polarizer);
    public static final RecipeMap MagneticSeparator=fromDefinition(MachineRecipeMapDefinitions.MagneticSeparator);
    public static final RecipeMap LaserEngraver=fromDefinition(MachineRecipeMapDefinitions.LaserEngraver);
    public static final RecipeMap Welder=fromDefinition(MachineRecipeMapDefinitions.Welder);
    public static final RecipeMap Freezer=fromDefinition(MachineRecipeMapDefinitions.Freezer);
    public static final RecipeMap CryoMixer=fromDefinition(MachineRecipeMapDefinitions.CryoMixer);
    public static final RecipeMap Massfab=fromDefinition(MachineRecipeMapDefinitions.Massfab);
    public static final RecipeMap ScannerMolecular=fromDefinition(MachineRecipeMapDefinitions.ScannerMolecular);
    public static final RecipeMap Replicator=fromDefinition(MachineRecipeMapDefinitions.Replicator);
    public static final RecipeMap Autoclave=fromDefinition(MachineRecipeMapDefinitions.Autoclave);
    public static final RecipeMap Bath=fromDefinition(MachineRecipeMapDefinitions.Bath);
    public static final RecipeMap Generifier=fromDefinition(MachineRecipeMapDefinitions.Generifier);
    public static final RecipeMap Coagulator=fromDefinition(MachineRecipeMapDefinitions.Coagulator);
    public static final RecipeMap Fermenter=fromDefinition(MachineRecipeMapDefinitions.Fermenter);
    public static final RecipeMap Melter=fromDefinition(MachineRecipeMapDefinitions.Melter);
    public static final RecipeMap CokeOven=fromDefinition(MachineRecipeMapDefinitions.CokeOven);
    public static final RecipeMap Lightning=fromDefinition(MachineRecipeMapDefinitions.Lightning);
    public static final RecipeMap ImplosionCompressor=fromDefinition(MachineRecipeMapDefinitions.ImplosionCompressor);
    public static final RecipeMap Fusion=fromDefinition(MachineRecipeMapDefinitions.Fusion);
    public static final RecipeMap Mortar=fromDefinition(MachineRecipeMapDefinitions.Mortar);
    public static final RecipeMap DidYouKnow=fromDefinition(MachineRecipeMapDefinitions.DidYouKnow);
    public static final RecipeMap ToolHeads=fromDefinition(MachineRecipeMapDefinitions.ToolHeads);
    public static final RecipeMap Cooking=fromDefinition(MachineRecipeMapDefinitions.Cooking);
    public static final RecipeMap BlastFurnace=fromDefinition(MachineRecipeMapDefinitions.BlastFurnace);
    public static final RecipeMap VacuumFreezer=fromDefinition(MachineRecipeMapDefinitions.VacuumFreezer);
    public static final RecipeMap Assembler=fromDefinition(MachineRecipeMapDefinitions.Assembler);
    public static final RecipeMap CNC=fromDefinition(MachineRecipeMapDefinitions.CNC);
    public static final RecipeMap CrucibleAlloying=fromDefinition(MachineRecipeMapDefinitions.CrucibleAlloying);
    public static final RecipeMap CrucibleSmelting=fromDefinition(MachineRecipeMapDefinitions.CrucibleSmelting);
    public static final RecipeMap BedrockOreList=fromDefinition(MachineRecipeMapDefinitions.BedrockOreList);
    public static final RecipeMap ByProductList=fromDefinition(MachineRecipeMapDefinitions.ByProductList);
    public static final RecipeMap Hammer=fromDefinition(MachineRecipeMapDefinitions.Hammer);
    public static final RecipeMap Chisel=fromDefinition(MachineRecipeMapDefinitions.Chisel);
    public static final RecipeMap Calciner=fromDefinition(MachineRecipeMapDefinitions.Calciner);
    public static final RecipeMap Juicer=fromDefinition(MachineRecipeMapDefinitions.Juicer);
    public static final RecipeMap Anvil=fromDefinition(MachineRecipeMapDefinitions.Anvil);
    public static final RecipeMap AnvilBendSmall=fromDefinition(MachineRecipeMapDefinitions.AnvilBendSmall);
    public static final RecipeMap AnvilBendBig=fromDefinition(MachineRecipeMapDefinitions.AnvilBendBig);
    public static final RecipeMap BumbleQueens=fromDefinition(MachineRecipeMapDefinitions.BumbleQueens);
    public static final RecipeMap Trees=fromDefinition(MachineRecipeMapDefinitions.Trees);
    private static com.gregtech.gregtech.api.recipe.RecipeMap fromDefinition(com.gregtech.gregtech.api.recipe.RecipeMapSpec spec) {
        var map=new com.gregtech.gregtech.api.recipe.RecipeMap(null,spec.mNameInternal,spec.mNameLocal,spec.mGUIPath,
                spec.mInputItemsCount,spec.mOutputItemsCount,spec.mMinimalInputItems,
                spec.mInputFluidCount,spec.mOutputFluidCount,spec.mMinimalInputFluids,spec.mMinimalInputs,
                spec.mNeedsOutputs,spec.mCombinePower,spec.mUseBucketSizeIn,spec.mUseBucketSizeOut);
        map.specialValueLabel(spec.mSpecialValuePre,spec.mSpecialValueMultiplier,spec.mSpecialValuePost);
        if(spec.mInstantRecipes)map.instantRecipes();return map;
    }
    public static void bootstrap() {}
    public static RecipeMap byMachineName(String machineName) {
        return switch (MachineRecipeNames.field(machineName)) {
            case "Furnace" -> Furnace;
            case "Roasting" -> Roasting;
            case "Distillery" -> Distillery;
            case "Extruder" -> Extruder;
            case "Smelter" -> Smelter;
            case "CrystallisationCrucible" -> CrystallisationCrucible;
            case "Drying" -> Drying;
            case "Laminator" -> Laminator;
            case "CatalyticCracking" -> CatalyticCracking;
            case "SteamCracking" -> SteamCracking;
            case "Shredder" -> Shredder;
            case "Lathe" -> Lathe;
            case "Cutter" -> Cutter;
            case "Centrifuge" -> Centrifuge;
            case "RollingMill" -> RollingMill;
            case "RollBender" -> RollBender;
            case "RollFormer" -> RollFormer;
            case "ClusterMill" -> ClusterMill;
            case "Wiremill" -> Wiremill;
            case "Mixer" -> Mixer;
            case "Loom" -> Loom;
            case "Sluice" -> Sluice;
            case "Sharpening" -> Sharpening;
            case "BurnMixer" -> BurnMixer;
            case "PressureWasher" -> PressureWasher;
            case "Crusher" -> Crusher;
            case "Sifting" -> Sifting;
            case "Squeezer" -> Squeezer;
            case "Compressor" -> Compressor;
            case "Press" -> Press;
            case "Electrolyzer" -> Electrolyzer;
            case "Canner" -> Canner;
            case "Injector" -> Injector;
            case "Printer" -> Printer;
            case "ScannerVisuals" -> ScannerVisuals;
            case "Autocrafter" -> Autocrafter;
            case "Slicer" -> Slicer;
            case "Nanofab" -> Nanofab;
            case "Plantalyzer" -> Plantalyzer;
            case "Bumblelyzer" -> Bumblelyzer;
            case "Boxinator" -> Boxinator;
            case "Unboxinator" -> Unboxinator;
            case "Polarizer" -> Polarizer;
            case "MagneticSeparator" -> MagneticSeparator;
            case "LaserEngraver" -> LaserEngraver;
            case "Welder" -> Welder;
            case "Freezer" -> Freezer;
            case "CryoMixer" -> CryoMixer;
            case "Massfab" -> Massfab;
            case "ScannerMolecular" -> ScannerMolecular;
            case "Replicator" -> Replicator;
            case "Autoclave" -> Autoclave;
            case "Bath" -> Bath;
            case "Generifier" -> Generifier;
            case "Coagulator" -> Coagulator;
            case "Fermenter" -> Fermenter;
            case "Melter" -> Melter;
            case "CokeOven" -> CokeOven;
            case "Lightning" -> Lightning;
            case "ImplosionCompressor" -> ImplosionCompressor;
            case "Fusion" -> Fusion;
            case "CryoDistillationTower" -> CryoDistillationTower;
            case "DistillationTower" -> DistillationTower;
            case "DidYouKnow" -> DidYouKnow;
            default -> throw new IllegalArgumentException("Unknown machine recipe mapping: " + machineName);
        };
    }
    private static boolean invalid(ItemStack aStack) {
        return aStack == null || aStack.isEmpty();
    }

    private static ItemStack nn(ItemStack aStack) {
        return aStack == null ? ItemStack.EMPTY : aStack;
    }

    private static FluidStack water(long aAmount) {
        return new FluidStack(Fluids.WATER, (int) Math.max(1, Math.min(Integer.MAX_VALUE, aAmount)));
    }

    // ── Pulverizing (Crusher + Shredder) ──────────────────────────────────

    public static boolean pulverizing(ItemStack aInput, ItemStack aOutput1)                                          {return pulverizing(aInput, aOutput1, null, 0, false);}
    public static boolean pulverizing(ItemStack aInput, ItemStack aOutput1, ItemStack aOutput2)                      {return pulverizing(aInput, aOutput1, aOutput2, 100, false);}
    public static boolean pulverizing(ItemStack aInput, ItemStack aOutput1, ItemStack aOutput2, int aChance)         {return pulverizing(aInput, aOutput1, aOutput2, aChance, false);}
    public static boolean pulverizing(ItemStack aInput, ItemStack aOutput1, boolean aOverwrite)                      {return pulverizing(aInput, aOutput1, null, 0, aOverwrite);}
    public static boolean pulverizing(ItemStack aInput, ItemStack aOutput1, ItemStack aOutput2, boolean aOverwrite)  {return pulverizing(aInput, aOutput1, aOutput2, 100, aOverwrite);}
    public static boolean pulverizing(ItemStack aInput, ItemStack aOutput1, ItemStack aOutput2, int aChance, boolean aOverwrite) {return pulverizing(aInput, aOutput1, aOutput2, aChance, null, 0, aOverwrite);}

    /**
     * Registers a pulverization recipe (Crusher + Shredder).
     * Chances are in percent (like the original: {@code 100} = 100%); {@code <= 0} defaults to 10%.
     */
    public static boolean pulverizing(ItemStack aInput, ItemStack aOutput1, ItemStack aOutput2, int aChance2, ItemStack aOutput3, int aChance3, boolean aOverwrite) {
        if (invalid(aInput) || invalid(aOutput1)) return false;
        if (aOverwrite) {
            Crusher.removeRecipesByInput(aInput);
            Shredder.removeRecipesByInput(aInput);
        }
        long[] tChances = {10000, (aChance2 <= 0 ? 10 : aChance2) * 100L, (aChance3 <= 0 ? 10 : aChance3) * 100L};
        Crusher .addRecipe1(true, 16, 64, tChances, aInput, aOutput1, nn(aOutput2), nn(aOutput3));
        Shredder.addRecipe1(true, 16, 64, tChances, aInput, aOutput1, nn(aOutput2), nn(aOutput3));
        return true;
    }

    // ── Mortar (Mortar + Shredder) ────────────────────────────────────────

    public static boolean mortarize(ItemStack aInput, ItemStack aOutput)                                   {return mortarize(1, aInput, aOutput, ItemStack.EMPTY);}
    public static boolean mortarize(ItemStack aInput, ItemStack aOutput1, ItemStack aOutput2)              {return mortarize(1, aInput, aOutput1, aOutput2);}
    public static boolean mortarize(long aPower, ItemStack aInput, ItemStack aOutput)                      {return mortarize(aPower, aInput, aOutput, ItemStack.EMPTY);}
    public static boolean mortarize(long aPower, ItemStack aInput, ItemStack aOutput1, ItemStack aOutput2) {
        if (invalid(aInput) || invalid(aOutput1)) return false;
        Mortar  .addRecipe1(true, 16, 16 * aPower, aInput, aOutput1, nn(aOutput2));
        Shredder.addRecipe1(true, 16, 16 * aPower, aInput, aOutput1, nn(aOutput2));
        return true;
    }

    // ── Smelting (vanilla-style furnace results -> RM.Furnace) ────────────

    public static boolean add_smelting(ItemStack aInput, ItemStack aOutput)                                            {return add_smelting(aInput, aOutput, 0, true);}
    public static boolean add_smelting(ItemStack aInput, ItemStack aOutput, boolean aRemoveOthers)                     {return add_smelting(aInput, aOutput, 0, aRemoveOthers);}
    public static boolean add_smelting(ItemStack aInput, ItemStack aOutput, boolean aRemoveOthers, boolean aSmoker, boolean aBlast) {return add_smelting(aInput, aOutput, 0, aRemoveOthers, aSmoker, aBlast);}
    public static boolean add_smelting(ItemStack aInput, ItemStack aOutput, float aEXP)                                {return add_smelting(aInput, aOutput, aEXP, true);}
    public static boolean add_smelting(ItemStack aInput, ItemStack aOutput, float aEXP, boolean aRemoveOthers)         {return add_smelting(aInput, aOutput, aEXP, aRemoveOthers, false, false);}

    /**
     * Registers a furnace smelting recipe directly into {@link #Furnace}.
     * The smoker/blast furnace and EXP parameters are accepted for source compatibility
     * but currently ignored (vanilla recipe conversion happens in Loader_OvenRecipes).
     * <p>
     * GT6 {@code Loader_Recipes_Furnace:122-130}: an output material flagged
     * {@code TD.Processing.NEVER_FURNACE} (Iron, Wrought Iron, Steel, Titanium, Tungsten — the
     * metals that must be melted in a crucible) is <em>un-smelted</em>: the recipe yields
     * {@code scrapGt} of that material instead of the metal itself.
     * </p>
     */
    public static boolean add_smelting(ItemStack aInput, ItemStack aOutput, float aEXP, boolean aRemoveOthers, boolean aSmoker, boolean aBlast) {
        if (invalid(aInput) || invalid(aOutput)) return false;
        if (aRemoveOthers) rem_smelting(aInput);
        return Furnace.addRecipe1(true, 16, 16, aInput, neverFurnaceOutput(aOutput)) != null;
    }

    /**
     * GT6's "unsmelt" substitution: the scrap form of a {@code NEVER_FURNACE} material, or the
     * stack itself when the material may be furnace-smelted. Amount follows the original
     * {@code (materialAmount * outputCount) / scrapAmount}.
     */
    public static ItemStack neverFurnaceOutput(ItemStack aOutput) {
        if (invalid(aOutput)) return aOutput;
        if (!(aOutput.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem materialItem)) return aOutput;
        com.gregtech.gregtech.api.material.GTMaterial material = materialItem.getMaterial().resolve();
        if (material == null || !material.isValid() || !material.has(MaterialProperty.NEVER_FURNACE)) {
            return aOutput;
        }
        long amount = materialItem.getPrefix().getMaterialWeight() * aOutput.getCount();
        long scrapUnit = MaterialPrefix.scrapGt.getMaterialWeight();
        ItemStack scrap = com.gregtech.gregtech.registry.GTItems.getStack(MaterialPrefix.scrapGt, material,
                (int) Math.max(1, Math.min(64, amount / Math.max(1, scrapUnit))));
        return scrap.isEmpty() ? aOutput : scrap;
    }

    /** Whether the stack's material may not be produced by furnace smelting (GT6 NEVER_FURNACE). */
    public static boolean isNeverFurnace(ItemStack aStack) {
        return aStack != null && !aStack.isEmpty()
                && aStack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem materialItem
                && materialItem.getMaterial().resolve() != null
                && materialItem.getMaterial().resolve().has(MaterialProperty.NEVER_FURNACE);
    }

    /** Removes all {@link #Furnace} recipes using the given input. */
    public static boolean rem_smelting(ItemStack aInput) {
        if (invalid(aInput)) return false;
        return Furnace.removeRecipesByInput(aInput);
    }

    // ── Generifier ────────────────────────────────────────────────────────

    public static boolean generify(ItemStack aStack1, ItemStack aStack2) {
        if (invalid(aStack1) || invalid(aStack2)) return false;
        return Generifier.addRecipe1(false, true, false, false, false, 0, 1, aStack1, aStack2) != null;
    }

    public static boolean genericycle(ItemStack... aStacks) {
        java.util.List<ItemStack> tList = new java.util.ArrayList<>();
        if (aStacks != null) for (ItemStack tStack : aStacks) if (!invalid(tStack)) tList.add(tStack);
        if (tList.size() < 2) return false;
        for (int i = 0; i < tList.size(); i++) generify(tList.get(i), tList.get((i + 1) % tList.size()));
        return true;
    }

    public static boolean generify(FluidStack aFluid1, FluidStack aFluid2) {
        if (aFluid1 == null || aFluid1.isEmpty() || aFluid2 == null || aFluid2.isEmpty()) return false;
        return Generifier.addRecipe0(false, true, false, false, false, 0, 1, aFluid1, aFluid2, RecipeMap.ZL_IS) != null;
    }

    public static boolean genericycle(FluidStack... aFluids) {
        java.util.List<FluidStack> tList = new java.util.ArrayList<>();
        if (aFluids != null) for (FluidStack tFluid : aFluids) if (tFluid != null && !tFluid.isEmpty()) tList.add(tFluid);
        if (tList.size() < 2) return false;
        for (int i = 0; i < tList.size(); i++) generify(tList.get(i), tList.get((i + 1) % tList.size()));
        return true;
    }

    // ── Pressure washing / debarking ──────────────────────────────────────

    public static boolean pressurewash(ItemStack aInput, ItemStack... aOutputs)                            {return pressurewash(16, 64, 200, aInput, aOutputs);}
    public static boolean pressurewash(long aEUt, long aDuration, ItemStack aInput, ItemStack... aOutputs) {return pressurewash(aEUt, aDuration, 1000, aInput, aOutputs);}
    public static boolean pressurewash(long aEUt, long aDuration, long aWater, ItemStack aInput, ItemStack... aOutputs) {
        if (invalid(aInput) || aOutputs == null || aOutputs.length <= 0 || invalid(aOutputs[0])) return false;
        PressureWasher.addRecipe1(true, aEUt, aDuration, aInput, water(aWater < 1 ? 1 : aWater), (FluidStack) null, aOutputs);
        return true;
    }

    @Deprecated public static boolean debarking(ItemStack aInput, ItemStack... aOutputs)                            {return pressurewash(16, 64, 1000, aInput, aOutputs);}
    @Deprecated public static boolean debarking(long aEUt, long aDuration, ItemStack aInput, ItemStack... aOutputs) {return pressurewash(aEUt, aDuration, 1000, aInput, aOutputs);}
    @Deprecated public static boolean debarking(long aEUt, long aDuration, long aWater, ItemStack aInput, ItemStack... aOutputs) {return pressurewash(aEUt, aDuration, aWater, aInput, aOutputs);}

    // ── Sawing (Cutter) ───────────────────────────────────────────────────

    /** GT6 water/distilled water/lubricant variants; food never uses industrial oil. */
    public static boolean sawing(long aEUt, long aDuration, boolean aIsFoodItem, long aLubricantAmount, ItemStack aInput, ItemStack... aOutputs) {
        if (invalid(aInput) || aOutputs == null || aOutputs.length <= 0 || invalid(aOutputs[0])) return false;
        long amount = Math.max(1, aLubricantAmount);
        boolean registered = false;
        for (var variant : com.gregtech.gregtech.content.recipe.SawingCoolants.variants(aIsFoodItem)) {
            var coolant = com.gregtech.gregtech.content.recipe.DyeProcessingRecipes.fluid(variant.field(), Math.toIntExact(amount * variant.multiplier()));
            var recipe = new com.gregtech.gregtech.api.recipe.Recipe(new ItemStack[]{aInput}, aOutputs,
                    null, null, new FluidStack[]{coolant}, null, aDuration * variant.multiplier(), aEUt, 0).withEmptyContainerInputs();
            registered |= Cutter.addRecipe(recipe) != null;
        }
        return registered;
    }

    // ── Lathing ───────────────────────────────────────────────────────────

    public static boolean lathing(long aEUt, long aDuration, ItemStack aInput, ItemStack... aOutputs) {
        if (invalid(aInput) || aOutputs == null || aOutputs.length <= 0 || invalid(aOutputs[0])) return false;
        Lathe.addRecipe1(true, aEUt, aDuration, aInput, aOutputs);
        return true;
    }
}
