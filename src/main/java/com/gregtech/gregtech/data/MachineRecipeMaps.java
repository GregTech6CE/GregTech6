package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.MaterialProperty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

/**
 * GT6 Recipe Maps — one per machine type.
 * <p>
 * Each instance defines the slot/tank layout used by {@code BasicMachineContainerMenu}
 * and {@code BasicMachineBlockEntity}. Recipe data is not populated yet.
 * </p>
 */
@SuppressWarnings("unused")
public class MachineRecipeMaps {
    protected MachineRecipeMaps() {}

    private static final String GUI = "textures/gui/machines/";

    // ── RecipeMap instances ──────────────────────────────────────────────
    // Constructor order: (collection, internalName, localName, guiPath,
    //   inItems, outItems, minItems, inFluids, outFluids, minFluids, minInputs,
    //   needsOutputs, combinePower, useBucketIn, useBucketOut)

    public static final RecipeMap
    Furnace                   = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Furnace)
    , Microwave                = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Microwave)
    , Roasting                 = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Roasting)
    , Distillery               = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Distillery)
    , Extruder                 = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Extruder)
    , Smelter                  = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Smelter)
    , CrystallisationCrucible  = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.CrystallisationCrucible)
    , Drying                   = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Drying)
    , Laminator                = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Laminator)
    , CatalyticCracking        = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.CatalyticCracking)
    , SteamCracking            = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.SteamCracking)
    , DistillationTower        = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.DistillationTower)
    , CryoDistillationTower    = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.CryoDistillationTower)
    // RU / KU machines
    , Shredder                 = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Shredder)
    , Lathe                    = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Lathe)
    , Cutter                   = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Cutter)
    , Centrifuge               = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Centrifuge)
    , RollingMill              = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.RollingMill)
    , RollBender               = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.RollBender)
    , RollFormer               = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.RollFormer)
    , ClusterMill              = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.ClusterMill)
    , Wiremill                 = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Wiremill)
    , Mixer                    = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Mixer)
    , Loom                     = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Loom)
    , Sluice                   = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Sluice)
    , Sharpening               = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Sharpening)
    , BurnMixer                = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.BurnMixer)
    , PressureWasher           = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.PressureWasher)
    , Crusher                  = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Crusher)
    , Sifting                  = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Sifting)
    , Squeezer                 = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Squeezer)
    , Compressor               = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Compressor)
    , Press                    = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Press)
    // Minimal-input validation relaxed vs GT6 (was: 1 item / 2 total): GT6 met those minimums
    // with ST.tag selector pseudo-items, which this port does not have; fluid-only electrolysis
    // (water) and single-dust decomposition are legitimate recipes here.
    , Electrolyzer             = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Electrolyzer)
    , Canner                   = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Canner)
    , Injector                 = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Injector)
    , Printer                  = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Printer)
    , ScannerVisuals           = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.ScannerVisuals)
    , Autocrafter              = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Autocrafter)
    , Slicer                   = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Slicer)
    , Nanofab                  = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Nanofab)
    , Plantalyzer              = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Plantalyzer)
    , Bumblelyzer              = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Bumblelyzer)
    , Boxinator                = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Boxinator)
    , Unboxinator              = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Unboxinator)
    , Polarizer                = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Polarizer)
    , MagneticSeparator        = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.MagneticSeparator)
    , LaserEngraver            = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.LaserEngraver)
    , Welder                   = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Welder)
    , Freezer                  = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Freezer)
    , CryoMixer                = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.CryoMixer)
    , Massfab                  = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Massfab)
    , ScannerMolecular         = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.ScannerMolecular)
    , Replicator               = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Replicator)
    , Autoclave                = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Autoclave)
    , Bath                     = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Bath)
    , Generifier               = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Generifier)
    , Coagulator               = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Coagulator)
    , Fermenter                = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Fermenter)
    , Melter                   = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Melter)
    , CokeOven                 = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.CokeOven)
    , Lightning                = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Lightning)
    , ImplosionCompressor      = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.ImplosionCompressor)
    , Fusion                   = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Fusion)

    // ── Aliases ───────────────────────────────────────────────────────────
    , Oven = Furnace, Cooker = Furnace
    , HeatMixer = Mixer
    , Debarker = PressureWasher
    , Mortar = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Mortar)
    , buzzsaw = Cutter
    , sander = Sharpening
    , debarker_alias = PressureWasher
    , electricmixer = Mixer
    , electricloom = Loom
    , electricsifter = Sifting

    // ── Misc / special ────────────────────────────────────────────────────
    , DidYouKnow               = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.DidYouKnow)
    , Other = DidYouKnow
    , ToolHeads                = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.ToolHeads)
    , Cooking                  = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Cooking)

    // ── Deprecated / reserved ──────────────────────────────────────────────
    , BlastFurnace             = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.BlastFurnace)
    , VacuumFreezer            = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.VacuumFreezer)
    , Assembler                = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Assembler)
    , CNC                      = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.CNC)

    // ── GT6 smeltery / alloying maps ──────────────────────────────────────
    // Both crucible maps carry GT6's NBT/NEI special value: the temperature in Kelvin
    // (RM.java declares "Temperature: " / 1 / " K" for CrucibleAlloying and CrucibleSmelting).
    // Both are instant: the crucible converts the moment the input reaches its melting point, so the
    // recipe viewer must not print a duration for them (RecipeMap#instantRecipes).
    , CrucibleAlloying         = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.CrucibleAlloying)
    , CrucibleSmelting         = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.CrucibleSmelting)

    // ── Extra / utility ───────────────────────────────────────────────────
    , BedrockOreList           = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.BedrockOreList)
    , ByProductList            = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.ByProductList)
    , Hammer                   = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Hammer)
    , Chisel                   = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Chisel)
    , Calciner                 = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Calciner)
    , Juicer                   = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Juicer)
    , Anvil                    = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Anvil)
    , AnvilBendSmall           = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.AnvilBendSmall)
    , AnvilBendBig             = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.AnvilBendBig)
    , BumbleQueens             = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.BumbleQueens)
    , Trees                    = fromDefinition(com.gregtech.gregtech.data.MachineRecipeMapDefinitions.Trees)
    ;

    // -- Dynamic (input-computed) providers -------------------------------
    // GT6 subclasses RecipeMap and overrides findRecipe() whenever a row depends on the identity of
    // the inputs rather than on a fixed table: the scanner, printer and replicator read the NBT a
    // scanner wrote onto a USB stick (wired from GregTech's phase-C loader list), and the Bumblelyzer
    // scans whichever living bee sits in its input slots (RecipeMapBumblelyzer:51-74). The
    // Bumblelyzer's provider is installed here, in the map's own initializer, so it is in place for
    // every entry point - the loader list, the recipe-viewer index and the game tests alike.
    static {
        com.gregtech.gregtech.content.recipe.GTBumbleBeeRecipes.register();
    }

    // ── Machine name → RecipeMap lookup ───────────────────────────────────

    /** No-op compatibility stub — maps are created in the static initializer. */
    public static void bootstrap() {}

    /**
     * Legacy import mapping. Unknown names fail instead of silently behaving as a furnace.
     */
    public static RecipeMap byMachineName(String machineName) {
        return switch (machineName) {
            case "oven"                  -> Furnace;
            case "roaster"               -> Roasting;
            case "distillery"            -> Distillery;
            case "extruder"              -> Extruder;
            case "smelter"               -> Smelter;
            case "crystallisationcrucible" -> CrystallisationCrucible;
            case "dryer"                 -> Drying;
            case "laminator"             -> Laminator;
            case "catalyticcracker"      -> CatalyticCracking;
            case "steamcracker"          -> SteamCracking;
            case "shredder"              -> Shredder;
            case "lathe"                 -> Lathe;
            case "buzzsaw"              -> Cutter;
            case "centrifuge"            -> Centrifuge;
            case "rollingmill"           -> RollingMill;
            case "rollbender"            -> RollBender;
            case "rollformer"            -> RollFormer;
            case "clustermill"           -> ClusterMill;
            case "wiremill"              -> Wiremill;
            case "mixer"                 -> Mixer;
            case "loom"                  -> Loom;
            case "sluice"                -> Sluice;
            case "sander"               -> Sharpening;
            case "burnmixer"             -> BurnMixer;
            case "debarker"              -> PressureWasher;
            case "crusher"               -> Crusher;
            case "sifter"                -> Sifting;
            case "squeezer"              -> Squeezer;
            case "compressor"            -> Compressor;
            case "press"                 -> Press;
            case "electrolyzer"          -> Electrolyzer;
            case "canner"                -> Canner;
            case "injector"              -> Injector;
            case "printer"               -> Printer;
            case "scannervisuals"        -> ScannerVisuals;
            case "autocrafter"           -> Autocrafter;
            case "electricmixer"         -> Mixer;
            case "electricloom"          -> Loom;
            case "electricsifter"        -> Sifting;
            case "slicer"                -> Slicer;
            case "nanofab"               -> Nanofab;
            case "plantalyzer"           -> Plantalyzer;
            case "bumblelyzer"           -> Bumblelyzer;
            case "boxinator"             -> Boxinator;
            case "unboxinator"           -> Unboxinator;
            case "polarizer"             -> Polarizer;
            case "magneticseparator"     -> MagneticSeparator;
            case "laserengraver"         -> LaserEngraver;
            case "laserwelder"           -> Welder;
            case "freezer"               -> Freezer;
            case "cryomixer"             -> CryoMixer;
            case "massfab"               -> Massfab;
            case "scannermolecular"      -> ScannerMolecular;
            case "replicator"            -> Replicator;
            case "autoclave"             -> Autoclave;
            case "bath"                  -> Bath;
            case "generifier"            -> Generifier;
            case "coagulator"            -> Coagulator;
            case "fermenter"             -> Fermenter;
            case "melter"                -> Melter;
            case "cokeoven"              -> CokeOven;
            case "lightning"             -> Lightning;
            case "implosioncompressor"   -> ImplosionCompressor;
            case "fusionreactor"         -> Fusion;
            case "cryodistillationtower" -> CryoDistillationTower;
            case "distillationtower"     -> DistillationTower;
            // Large batch machines (GT6 "Multiblock Machines" tab, single-block here)
            case "largecentrifuge"       -> Centrifuge;
            case "largeelectrolyzer"     -> Electrolyzer;
            case "largecoagulator"       -> Coagulator;
            case "largeautoclave"        -> Autoclave;
            case "largebath"             -> Bath;
            case "largemixer"            -> Mixer;
            case "largefermenter"        -> Fermenter;
            case "largeoven"             -> Furnace;
            case "largesluice"           -> Sluice;
            case "largecrusher"          -> Crusher;
            case "largeshredder"         -> Shredder;
            case "largesqueezer"         -> Squeezer;
            case "largemassfab"          -> Massfab;
            // GT6 legacy/alias names for the same machines. Kept so imported definitions
            // and client menus never hit the throwing default branch below.
            case "pressurewasher"        -> PressureWasher;
            case "heatmixer"             -> Mixer;
            case "default"               -> DidYouKnow;
            default                      -> throw new IllegalArgumentException("Unknown machine recipe mapping: " + machineName);
        };
    }

    // ── Static recipe helpers (1.20.1 port of the gregapi.data.RM helpers) ─
    // Mod-bridge registrations of the original (IC2 macerator, TE pulverizer,
    // Railcraft rock crusher, AE grinder, EtFu smoker/blast) are dropped; the
    // GT6-internal RecipeMaps are filled instead.

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
    private static com.gregtech.gregtech.api.recipe.RecipeMap fromDefinition(com.gregtech.gregtech.api.recipe.RecipeMapSpec spec) {
        var map=new com.gregtech.gregtech.api.recipe.RecipeMap(null,spec.mNameInternal,spec.mNameLocal,spec.mGUIPath,
                spec.mInputItemsCount,spec.mOutputItemsCount,spec.mMinimalInputItems,
                spec.mInputFluidCount,spec.mOutputFluidCount,spec.mMinimalInputFluids,spec.mMinimalInputs,
                spec.mNeedsOutputs,spec.mCombinePower,spec.mUseBucketSizeIn,spec.mUseBucketSizeOut);
        map.specialValueLabel(spec.mSpecialValuePre,spec.mSpecialValueMultiplier,spec.mSpecialValuePost);
        if(spec.mInstantRecipes)map.instantRecipes();return map;
    }
}
