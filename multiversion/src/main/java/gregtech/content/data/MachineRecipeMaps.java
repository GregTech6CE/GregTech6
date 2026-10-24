package gregtech.content.data;

import gregtech.internal.recipe.RecipeMap;
//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
import gregtech.internal.material.MaterialProperty;
//?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import gregtech.internal.material.MaterialProperty;
/^* Same shared original maps with Neo ItemStack/FluidStack rows. ^/
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
    public static final RecipeMap Autocrafter=fromDefinition(MachineRecipeMapDefinitions.Autocrafter).contextualRecipes(gregtech.content.recipe.AutocraftingRecipes::find);
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
    private static gregtech.internal.recipe.RecipeMap fromDefinition(gregtech.internal.recipe.RecipeMapSpec spec) {
        var map=new gregtech.internal.recipe.RecipeMap(null,spec.mNameInternal,spec.mNameLocal,spec.mGUIPath,
                spec.mInputItemsCount,spec.mOutputItemsCount,spec.mMinimalInputItems,
                spec.mInputFluidCount,spec.mOutputFluidCount,spec.mMinimalInputFluids,spec.mMinimalInputs,
                spec.mNeedsOutputs,spec.mCombinePower,spec.mUseBucketSizeIn,spec.mUseBucketSizeOut);
        map.viewer(spec.mViewerAllowed,spec.mShowVoltageAmperage);
        map.specialValueLabel(spec.mSpecialValuePre,spec.mSpecialValueMultiplier,spec.mSpecialValuePost);
        if(spec.mInstantRecipes)map.instantRecipes();return map;
*///?} else {
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
    Furnace                   = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Furnace)
    , Microwave                = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Microwave)
    , Roasting                 = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Roasting)
    , Distillery               = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Distillery)
    , Extruder                 = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Extruder)
    , Smelter                  = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Smelter)
    , CrystallisationCrucible  = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.CrystallisationCrucible)
    , Drying                   = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Drying)
    , Laminator                = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Laminator)
    , CatalyticCracking        = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.CatalyticCracking)
    , SteamCracking            = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.SteamCracking)
    , DistillationTower        = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.DistillationTower)
    , CryoDistillationTower    = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.CryoDistillationTower)
    // RU / KU machines
    , Shredder                 = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Shredder)
    , Lathe                    = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Lathe)
    , Cutter                   = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Cutter)
    , Centrifuge               = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Centrifuge)
    , RollingMill              = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.RollingMill)
    , RollBender               = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.RollBender)
    , RollFormer               = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.RollFormer)
    , ClusterMill              = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.ClusterMill)
    , Wiremill                 = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Wiremill)
    , Mixer                    = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Mixer)
    , Loom                     = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Loom)
    , Sluice                   = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Sluice)
    , Sharpening               = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Sharpening)
    , BurnMixer                = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.BurnMixer)
    , PressureWasher           = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.PressureWasher)
    , Crusher                  = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Crusher)
    , Sifting                  = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Sifting)
    , Squeezer                 = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Squeezer)
    , Compressor               = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Compressor)
    , Press                    = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Press)
    // Minimal-input validation relaxed vs GT6 (was: 1 item / 2 total): GT6 met those minimums
    // with ST.tag selector pseudo-items, which this port does not have; fluid-only electrolysis
    // (water) and single-dust decomposition are legitimate recipes here.
    , Electrolyzer             = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Electrolyzer)
    , Canner                   = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Canner)
    , Injector                 = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Injector)
    , Printer                  = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Printer)
    , ScannerVisuals           = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.ScannerVisuals)
    , Autocrafter              = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Autocrafter).contextualRecipes(gregtech.content.recipe.AutocraftingRecipes::find)
    , Slicer                   = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Slicer)
    , Nanofab                  = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Nanofab)
    , Plantalyzer              = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Plantalyzer)
    , Bumblelyzer              = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Bumblelyzer)
    , Boxinator                = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Boxinator)
    , Unboxinator              = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Unboxinator)
    , Polarizer                = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Polarizer)
    , MagneticSeparator        = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.MagneticSeparator)
    , LaserEngraver            = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.LaserEngraver)
    , Welder                   = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Welder)
    , Freezer                  = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Freezer)
    , CryoMixer                = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.CryoMixer)
    , Massfab                  = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Massfab)
    , ScannerMolecular         = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.ScannerMolecular)
    , Replicator               = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Replicator)
    , Autoclave                = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Autoclave)
    , Bath                     = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Bath)
    , Generifier               = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Generifier)
    , Coagulator               = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Coagulator)
    , Fermenter                = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Fermenter)
    , Melter                   = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Melter)
    , CokeOven                 = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.CokeOven)
    , Lightning                = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Lightning)
    , ImplosionCompressor      = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.ImplosionCompressor)
    , Fusion                   = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Fusion)

    // ── Aliases ───────────────────────────────────────────────────────────
    , Oven = Furnace, Cooker = Furnace
    , HeatMixer = Mixer
    , Debarker = PressureWasher
    , Mortar = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Mortar)
    , buzzsaw = Cutter
    , sander = Sharpening
    , debarker_alias = PressureWasher
    , electricmixer = Mixer
    , electricloom = Loom
    , electricsifter = Sifting

    // ── Misc / special ────────────────────────────────────────────────────
    , DidYouKnow               = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.DidYouKnow)
    , Other = DidYouKnow
    , ToolHeads                = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.ToolHeads)
    , Cooking                  = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Cooking)

    // ── Deprecated / reserved ──────────────────────────────────────────────
    , BlastFurnace             = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.BlastFurnace)
    , VacuumFreezer            = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.VacuumFreezer)
    , Assembler                = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Assembler)
    , CNC                      = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.CNC)

    // ── GT6 smeltery / alloying maps ──────────────────────────────────────
    // Both crucible maps carry GT6's NBT/NEI special value: the temperature in Kelvin
    // (RM.java declares "Temperature: " / 1 / " K" for CrucibleAlloying and CrucibleSmelting).
    // Both are instant: the crucible converts the moment the input reaches its melting point, so the
    // recipe viewer must not print a duration for them (RecipeMap#instantRecipes).
    , CrucibleAlloying         = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.CrucibleAlloying)
    , CrucibleSmelting         = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.CrucibleSmelting)

    // ── Extra / utility ───────────────────────────────────────────────────
    , BedrockOreList           = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.BedrockOreList)
    , ByProductList            = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.ByProductList)
    , Hammer                   = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Hammer)
    , Chisel                   = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Chisel)
    , Calciner                 = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Calciner)
    , Juicer                   = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Juicer)
    , Anvil                    = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Anvil)
    , AnvilBendSmall           = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.AnvilBendSmall)
    , AnvilBendBig             = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.AnvilBendBig)
    , BumbleQueens             = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.BumbleQueens)
    , Trees                    = fromDefinition(gregtech.content.data.MachineRecipeMapDefinitions.Trees)
    ;

    // -- Dynamic (input-computed) providers -------------------------------
    // GT6 subclasses RecipeMap and overrides findRecipe() whenever a row depends on the identity of
    // the inputs rather than on a fixed table: the scanner, printer and replicator read the NBT a
    // scanner wrote onto a USB stick (wired from GregTech's phase-C loader list), and the Bumblelyzer
    // scans whichever living bee sits in its input slots (RecipeMapBumblelyzer:51-74). The
    // Bumblelyzer's provider is installed here, in the map's own initializer, so it is in place for
    // every entry point - the loader list, the recipe-viewer index and the game tests alike.
    static {
        gregtech.content.recipe.GTBumbleBeeRecipes.register();
//?}
    }

    // ── Machine name → RecipeMap lookup ───────────────────────────────────

    /** No-op compatibility stub — maps are created in the static initializer. */
    public static void bootstrap() {}

    /**
     * Legacy import mapping. Unknown names fail instead of silently behaving as a furnace.
     */
    public static RecipeMap byMachineName(String machineName) {
//? if >=1.21.1 {
        /*return switch (MachineRecipeNames.field(machineName)) {
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
*///?} else {
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
//?}
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
        if (!(aOutput.getItem() instanceof gregtech.internal.material.MaterialFormItem materialItem)) return aOutput;
        gregtech.internal.material.GTMaterial material = materialItem.getMaterial().resolve();
        if (material == null || !material.isValid() || !material.has(MaterialProperty.NEVER_FURNACE)) {
            return aOutput;
        }
        long amount = materialItem.getPrefix().getMaterialWeight() * aOutput.getCount();
        long scrapUnit = MaterialPrefix.scrapGt.getMaterialWeight();
        ItemStack scrap = gregtech.content.registry.GTItems.getStack(MaterialPrefix.scrapGt, material,
                (int) Math.max(1, Math.min(64, amount / Math.max(1, scrapUnit))));
        return scrap.isEmpty() ? aOutput : scrap;
    }

    /** Whether the stack's material may not be produced by furnace smelting (GT6 NEVER_FURNACE). */
    public static boolean isNeverFurnace(ItemStack aStack) {
        return aStack != null && !aStack.isEmpty()
                && aStack.getItem() instanceof gregtech.internal.material.MaterialFormItem materialItem
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
        for (var variant : gregtech.content.recipe.SawingCoolants.variants(aIsFoodItem)) {
            var coolant = gregtech.content.recipe.DyeProcessingRecipes.fluid(variant.field(), Math.toIntExact(amount * variant.multiplier()));
            var recipe = new gregtech.internal.recipe.Recipe(new ItemStack[]{aInput}, aOutputs,
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
//? if >=1.21.1 {
//?} else {
    private static gregtech.internal.recipe.RecipeMap fromDefinition(gregtech.internal.recipe.RecipeMapSpec spec) {
        var map=new gregtech.internal.recipe.RecipeMap(null,spec.mNameInternal,spec.mNameLocal,spec.mGUIPath,
                spec.mInputItemsCount,spec.mOutputItemsCount,spec.mMinimalInputItems,
                spec.mInputFluidCount,spec.mOutputFluidCount,spec.mMinimalInputFluids,spec.mMinimalInputs,
                spec.mNeedsOutputs,spec.mCombinePower,spec.mUseBucketSizeIn,spec.mUseBucketSizeOut);
        map.viewer(spec.mViewerAllowed,spec.mShowVoltageAmperage);
        map.specialValueLabel(spec.mSpecialValuePre,spec.mSpecialValueMultiplier,spec.mSpecialValuePost);
        if(spec.mInstantRecipes)map.instantRecipes();return map;
    }
//?}
}
