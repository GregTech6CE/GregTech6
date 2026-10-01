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
    Furnace                   = new RecipeMap(null, "mc.recipe.furnace"                  , "Furnace"                  , GUI+"Oven"                 , 1, 1,1, 1, 1,0, 0, false,false,false,false)
    , Microwave                = new RecipeMap(null, "gt.recipe.microwave"               , "Microwave"                , GUI+"Oven"                 , 1, 1,1, 1, 1,0, 0, false,false,false,false)
    , Roasting                 = new RecipeMap(null, "gt.recipe.roaster"                 , "Roaster"                  , GUI+"Roaster"              , 1, 3,1, 1, 1,1, 2, false,false,true ,true )
    , Distillery               = new RecipeMap(null, "gt.recipe.distillery"              , "Distillery"               , GUI+"Distillery"           , 1, 2,1, 1, 2,1, 2, false,false,false,false)
    , Extruder                 = new RecipeMap(null, "gt.recipe.extruder"                , "Extruder"                 , GUI+"Extruder"             , 2, 2,2, 0, 0,0, 0, false,false,true ,true )
    , Smelter                  = new RecipeMap(null, "gt.recipe.smelter"                 , "Smelter"                  , GUI+"Smelter"              , 1, 1,0, 1, 1,0, 1, false,false,true ,true )
    , CrystallisationCrucible  = new RecipeMap(null, "gt.recipe.crystallisationcrucible" , "Crystallisation Crucible" , GUI+"CrystallisationCrucible", 1, 1,1, 3, 0,1, 1, false,false,true ,true )
    , Drying                   = new RecipeMap(null, "gt.recipe.drying"                  , "Dryer"                    , GUI+"Dryer"                , 1, 1,0, 1, 3,0, 1, false,false,true ,true )
    , Laminator                = new RecipeMap(null, "gt.recipe.laminator"               , "Laminator"                , GUI+"Laminator"            , 2, 1,2, 0, 0,0, 2, false,false,true ,true )
    , CatalyticCracking        = new RecipeMap(null, "gt.recipe.catalyticcracking"       , "Catalytic Cracking"       , GUI+"CatalyticCracking"    , 1, 3,0, 2, 9,1, 2, false,false,true ,true )
    , SteamCracking            = new RecipeMap(null, "gt.recipe.steamcracking"           , "Steam Cracking"           , GUI+"SteamCracking"        , 1, 3,0, 2, 9,1, 2, false,false,true ,true )
    , DistillationTower        = new RecipeMap(null, "gt.recipe.distillationtower"       , "Distillation Tower"       , GUI+"DistillationTower"    , 1, 3,0, 1, 9,0, 1, false,false,false,false)
    , CryoDistillationTower    = new RecipeMap(null, "gt.recipe.cryodistillationtower"   , "Cryo Distillation Tower"  , GUI+"CryoDistillationTower", 1, 3,0, 1, 9,0, 1, false,false,false,false)
    // RU / KU machines
    , Shredder                 = new RecipeMap(null, "gt.recipe.shredder"                , "Shredder"                 , GUI+"Shredder"             , 1,12,1, 0, 0,0, 0, false,false,true ,true )
    , Lathe                    = new RecipeMap(null, "gt.recipe.lathe"                   , "Lathe"                    , GUI+"Lathe"                , 1, 2,1, 0, 0,0, 0, false,false,true ,true )
    , Cutter                   = new RecipeMap(null, "gt.recipe.cutter"                  , "Cutter"                   , GUI+"Cutter"               , 1, 3,1, 1, 0,1, 0, false,false,true ,true )
    , Centrifuge               = new RecipeMap(null, "gt.recipe.centrifuge"              , "Centrifuge"               , GUI+"Centrifuge"           , 1, 6,0, 1, 6,0, 0, false,false,true ,true )
    , RollingMill              = new RecipeMap(null, "gt.recipe.rollingmill"             , "Rolling Mill"             , GUI+"RollingMill"          , 1, 1,1, 0, 0,0, 0, false,false,true ,true )
    , RollBender               = new RecipeMap(null, "gt.recipe.rollbender"              , "Roll Bender"              , GUI+"RollBender"           , 1, 1,1, 0, 0,0, 0, false,false,true ,true )
    , RollFormer               = new RecipeMap(null, "gt.recipe.rollformer"              , "Roll Former"              , GUI+"RollFormer"           , 1, 1,1, 0, 0,0, 0, false,false,true ,true )
    , ClusterMill              = new RecipeMap(null, "gt.recipe.clustermill"             , "Cluster Mill"             , GUI+"ClusterMill"          , 1, 1,1, 0, 0,0, 0, false,false,true ,true )
    , Wiremill                 = new RecipeMap(null, "gt.recipe.wiremill"                , "Wiremill"                 , GUI+"Wiremill"             , 1, 1,1, 0, 0,0, 0, false,false,true ,true )
    , Mixer                    = new RecipeMap(null, "gt.recipe.mixer"                   , "Mixer"                    , GUI+"Mixer"                , 6, 1,0, 6, 2,0, 2, false,false,true ,true )
    , Loom                     = new RecipeMap(null, "gt.recipe.loom"                    , "Loom"                     , GUI+"Loom"                 , 6, 1,1, 0, 0,0, 0, false,false,true ,true )
    , Sluice                   = new RecipeMap(null, "gt.recipe.sluice"                  , "Sluice"                   , GUI+"Sluice"               , 1, 9,1, 1, 1,1, 2, false,false,true ,true )
    , Sharpening               = new RecipeMap(null, "gt.recipe.sharpener"               , "Sharpener"                , GUI+"Sharpener"            , 1, 2,1, 0, 0,0, 0, false,false,true ,true )
    , BurnMixer                = new RecipeMap(null, "gt.recipe.burnmixer"               , "Burner Mixer"             , GUI+"BurnMixer"            , 6, 1,0, 6, 2,0, 2, false,false,true ,true )
    , PressureWasher           = new RecipeMap(null, "gt.recipe.pressurewasher"          , "Pressure Washer"          , GUI+"PressureWasher"       , 1, 2,1, 1, 0,1, 0, false,false,true ,true )
    , Crusher                  = new RecipeMap(null, "gt.recipe.crusher"                 , "Crusher"                  , GUI+"Crusher"              , 1,12,1, 0, 0,0, 0, false,false,true ,true )
    , Sifting                  = new RecipeMap(null, "gt.recipe.sifter"                  , "Sifter"                   , GUI+"Sifter"               , 1,12,1, 0, 0,0, 0, false,false,true ,true )
    , Squeezer                 = new RecipeMap(null, "gt.recipe.squeezer"                , "Squeezer"                 , GUI+"Squeezer"             , 1, 2,1, 0, 1,0, 0, false,false,true ,true )
    , Compressor               = new RecipeMap(null, "gt.recipe.compressor"              , "Compressor"               , GUI+"Compressor"           , 1, 1,1, 0, 0,0, 0, false,false,true ,true )
    , Press                    = new RecipeMap(null, "gt.recipe.press"                   , "Press"                    , GUI+"Press"                , 3, 1,2, 0, 0,0, 0, false,false,true ,true )
    // Minimal-input validation relaxed vs GT6 (was: 1 item / 2 total): GT6 met those minimums
    // with ST.tag selector pseudo-items, which this port does not have; fluid-only electrolysis
    // (water) and single-dust decomposition are legitimate recipes here.
    , Electrolyzer             = new RecipeMap(null, "gt.recipe.electrolyzer"            , "Electrolyzer"             , GUI+"Electrolyzer"         , 2, 6,0, 2, 6,0, 1, false,false,true ,true )
    , Canner                   = new RecipeMap(null, "gt.recipe.canner"                  , "Canning Machine"          , GUI+"Canner"               , 2, 2,1, 1, 1,0, 1, false,false,true ,true )
    , Injector                 = new RecipeMap(null, "gt.recipe.injector"                , "Injector"                 , GUI+"Injector"             , 2, 1,0, 2, 1,0, 2, false,false,true ,true )
    , Printer                  = new RecipeMap(null, "gt.recipe.printer"                 , "Printer"                  , GUI+"Printer"              , 2, 1,1, 6, 0,1, 2, false,false,false,false)
    , ScannerVisuals           = new RecipeMap(null, "gt.recipe.scannervisuals"          , "Scanner (Visuals)"        , GUI+"ScannerVisuals"       , 2, 2,2, 0, 0,0, 2, false,false,true ,true )
    , Autocrafter              = new RecipeMap(null, "gt.recipe.autocrafting"            , "Crafting"                 , GUI+"Crafting"             , 9,12,1, 0, 0,0, 1, false,false,true ,true )
    , Slicer                   = new RecipeMap(null, "gt.recipe.slicer"                  , "Slicer"                   , GUI+"Slicer"               , 2, 2,2, 0, 0,0, 2, false,false,true ,true )
    , Nanofab                  = new RecipeMap(null, "gt.recipe.nanofab"                 , "Nanoscale Fabricator"     , GUI+"Nanofab"              , 2, 1,0, 1, 1,0, 1, false,false,true ,true )
    , Plantalyzer              = new RecipeMap(null, "gt.recipe.plantalyzer"             , "Plantalyzer"              , GUI+"Plantalyzer"          , 2, 2,0, 1, 0,0, 1, false,false,true ,true )
    , Bumblelyzer              = new RecipeMap(null, "gt.recipe.bumblelyzer"             , "Bumblelyzer"              , GUI+"Bumblelyzer"          , 2, 2,0, 1, 0,0, 2, false,false,true ,true )
    , Boxinator                = new RecipeMap(null, "gt.recipe.boxinator"               , "Boxinator"                , GUI+"Boxinator"            , 2, 1,2, 0, 0,0, 0, false,false,true ,true )
    , Unboxinator              = new RecipeMap(null, "gt.recipe.unboxinator"             , "Unboxinator"              , GUI+"Unboxinator"          , 1,12,1, 0, 0,0, 0, false,false,true ,true )
    , Polarizer                = new RecipeMap(null, "gt.recipe.polarizer"               , "Polarizer"                , GUI+"Polarizer"            , 1, 1,1, 0, 0,0, 0, false,false,true ,true )
    , MagneticSeparator        = new RecipeMap(null, "gt.recipe.magneticseparator"       , "Magnetic Separator"       , GUI+"MagneticSeparator"    , 1, 6,0, 1, 6,0, 1, false,false,true ,true )
    , LaserEngraver            = new RecipeMap(null, "gt.recipe.laserengraver"           , "Precision Laser Engraver" , GUI+"LaserEngraver"        , 2, 1,2, 0, 0,0, 2, false,false,true ,true )
    , Welder                   = new RecipeMap(null, "gt.recipe.welder"                  , "Welding Machine"          , GUI+"Welder"               , 9, 1,2, 1, 0,0, 2, false,false,true ,true )
    , Freezer                  = new RecipeMap(null, "gt.recipe.freezer"                 , "Freezer"                  , GUI+"Freezer"              , 1, 1,1, 1, 1,0, 1, false,false,true ,true )
    , CryoMixer                = new RecipeMap(null, "gt.recipe.cryomixer"               , "Cryo Mixer"               , GUI+"CryoMixer"            , 6, 1,0, 6, 2,0, 2, false,false,true ,true )
    , Massfab                  = new RecipeMap(null, "gt.recipe.massfab"                 , "Matter Fabricator"        , GUI+"Massfab"              , 2, 1,0, 1, 2,0, 1, false,false,false,false)
    , ScannerMolecular         = new RecipeMap(null, "gt.recipe.scannermolecular"        , "Molecular Scanner"        , GUI+"ScannerMolecular"     , 2, 1,1, 0, 0,0, 2, false,false,false,false)
    , Replicator               = new RecipeMap(null, "gt.recipe.replicator"              , "Matter Replicator"        , GUI+"Replicator"           , 3, 3,1, 3, 3,0, 2, false,false,false,false)
    , Autoclave                = new RecipeMap(null, "gt.recipe.autoclave"               , "Autoclave"                , GUI+"Autoclave"            , 2, 3,2, 1, 1,1, 0, false,false,true ,true )
    , Bath                     = new RecipeMap(null, "gt.recipe.bath"                    , "Bath"                     , GUI+"Bath"                 , 6, 6,1, 1, 3,1, 2, false,false,true ,true )
    , Generifier               = new RecipeMap(null, "gt.recipe.generifier"              , "Generifier"               , GUI+"Generifier"           , 1, 1,0, 1, 1,0, 1, false,false,false,false)
    , Coagulator               = new RecipeMap(null, "gt.recipe.coagulator"              , "Coagulator"               , GUI+"Coagulator"           , 0, 1,0, 1, 0,1, 0, false,false,true ,true )
    , Fermenter                = new RecipeMap(null, "gt.recipe.fermenter"               , "Fermenter"                , GUI+"Fermenter"            , 1, 1,1, 1, 1,0, 1, false,false,true ,true )
    , Melter                   = new RecipeMap(null, "gt.recipe.melter"                  , "Melter"                   , GUI+"Melter"               , 1, 1,0, 1, 1,0, 1, false,false,true ,true )
    , CokeOven                 = new RecipeMap(null, "gt.recipe.cokeoven"                , "Coke Oven"                , GUI+"CokeOven"             , 1, 9,1, 0, 1,0, 1, false,false,true ,true )
    , Lightning                = new RecipeMap(null, "gt.recipe.lightning"               , "Lightning Processor"      , GUI+"Lightning"            , 6, 6,0, 6, 6,0, 2, false,false,true ,true )
    , ImplosionCompressor      = new RecipeMap(null, "gt.recipe.implosioncompressor"     , "Implosion Compressor"     , GUI+"ImplosionCompressor"  , 3, 3,3, 0, 0,0, 0, false,false,true ,true )
    , Fusion                   = new RecipeMap(null, "gt.recipe.fusionreactor"           , "Fusion Reactor"           , GUI+"Fusion"               , 2, 6,1, 2, 6,0, 2, false,false,true ,true )

    // ── Aliases ───────────────────────────────────────────────────────────
    , Oven = Furnace, Cooker = Furnace
    , HeatMixer = Mixer
    , Debarker = PressureWasher
    , Mortar = new RecipeMap(null, "gt.recipe.mortar", "Mortar", GUI+"Mortar", 1, 2, 1, 0, 0, 0, 0, false, false, true, true)
    , buzzsaw = Cutter
    , sander = Sharpening
    , debarker_alias = PressureWasher
    , electricmixer = Mixer
    , electricloom = Loom
    , electricsifter = Sifting

    // ── Misc / special ────────────────────────────────────────────────────
    , DidYouKnow               = new RecipeMap(null, "gt.recipe.other"                  , "Did you know...?"         , GUI+"Default"              , 6, 6,0, 3, 3,0, 1, false,false,false,false)
    , Other = DidYouKnow
    , ToolHeads                = new RecipeMap(null, "gt.recipe.toolhead"               , "Craft Head on Handle"     , GUI+"Crafting2By2"         , 4, 1,0, 0, 0,0, 0, false,false,false,false)
    , Cooking                  = new RecipeMap(null, "gt.recipe.cooker"                 , "Cooker"                   , GUI+"Cooker"               , 9, 1,1, 3, 1,1, 2, false,false,true ,true )

    // ── Deprecated / reserved ──────────────────────────────────────────────
    , BlastFurnace             = new RecipeMap(null, "gt.recipe.blastfurnace"           , "Blast Furnace"            , GUI+"Default"              , 2, 2,1, 0, 0,0, 0, false,false,true ,true )
    , VacuumFreezer            = new RecipeMap(null, "gt.recipe.vacuumfreezer"          , "Vacuum Freezer"           , GUI+"Default"              , 1, 1,1, 0, 0,0, 0, false,false,true ,true )
    , Assembler                = new RecipeMap(null, "gt.recipe.assembler"              , "Assembler"                , GUI+"Assembler"            , 2, 1,1, 1, 0,0, 0, false,false,true ,true )
    , CNC                      = new RecipeMap(null, "gt.recipe.cncmachine"             , "CNC Machine"              , GUI+"Default"              , 2, 1,2, 1, 0,1, 0, false,false,true ,true )

    // ── GT6 smeltery / alloying maps ──────────────────────────────────────
    // Both crucible maps carry GT6's NBT/NEI special value: the temperature in Kelvin
    // (RM.java declares "Temperature: " / 1 / " K" for CrucibleAlloying and CrucibleSmelting).
    // Both are instant: the crucible converts the moment the input reaches its melting point, so the
    // recipe viewer must not print a duration for them (RecipeMap#instantRecipes).
    , CrucibleAlloying         = new RecipeMap(null, "gt.recipe.cruciblealloying"       , "Combination Smelting"     , GUI+"Alloying"             ,12,12,1, 0, 0,0, 0, false,false,true ,true ).specialValueLabel("Temperature: ", 1, " K").instantRecipes()
    , CrucibleSmelting         = new RecipeMap(null, "gt.recipe.cruciblesmelting"       , "Crucible Smelting"        , GUI+"Default"              , 6, 6,1, 0, 0,0, 0, false,false,true ,true ).specialValueLabel("Temperature: ", 1, " K").instantRecipes()

    // ── Extra / utility ───────────────────────────────────────────────────
    , BedrockOreList           = new RecipeMap(null, "gt.recipe.bedrockorelist"         , "Bedrock Drill"            , GUI+"BedrockOreList"       , 1,12,1, 1, 0,1, 0, false,false,false,false)
    , ByProductList            = new RecipeMap(null, "gt.recipe.byproductlist"          , "Ore Byproduct List"       , GUI+"OreByproducts"        , 6,12,1, 0, 0,0, 0, false,false,true ,true )
    , Hammer                   = new RecipeMap(null, "gt.recipe.hammer"                 , "Hammer"                   , GUI+"Hammer"               , 1, 1,1, 0, 0,0, 0, false,false,true ,true )
    , Chisel                   = new RecipeMap(null, "gt.recipe.chisel"                 , "Chisel"                   , GUI+"Chisel"               , 1, 1,1, 0, 0,0, 0, false,false,true ,true )
    , Calciner                 = new RecipeMap(null, "gt.recipe.calciner"               , "Calciner"                 , GUI+"Calciner"             , 3, 3,0, 3, 3,0, 2, false,false,true ,true )
    , Juicer                   = new RecipeMap(null, "gt.recipe.juicer"                 , "Juicer"                   , GUI+"Juicer"               , 1, 3,1, 0, 1,0, 0, false,false,true ,true )
    , Anvil                    = new RecipeMap(null, "gt.recipe.anvil"                  , "Anvil"                    , GUI+"Anvil"                , 2, 2,2, 0, 0,0, 0, false,false,true ,true )
    , AnvilBendSmall           = new RecipeMap(null, "gt.recipe.anvil.bend.small"       , "Anvil Bending (Small)"    , GUI+"AnvilBendingSmall"    , 2, 2,2, 0, 0,0, 0, false,false,true ,true )
    , AnvilBendBig             = new RecipeMap(null, "gt.recipe.anvil.bend.big"         , "Anvil Bending (Big)"      , GUI+"AnvilBendingBig"      , 2, 2,2, 0, 0,0, 0, false,false,true ,true )
    , BumbleQueens             = new RecipeMap(null, "gt.recipe.bumblequeen"            , "Bumblebee Queen"          , GUI+"Default"              , 2, 6,0, 0, 0,0, 1, false,false,true ,true )
    , Trees                    = new RecipeMap(null, "gt.recipe.trees"                  , "Family Tree"              , GUI+"FamilyTree"           , 3,12,0, 0, 0,0, 1, false,false,true ,true )
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
        if (!(aOutput.getItem() instanceof com.gregtech.gregtech.item.MaterialItem materialItem)) return aOutput;
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
                && aStack.getItem() instanceof com.gregtech.gregtech.item.MaterialItem materialItem
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
