package gregtech.content.data;

import gregtech.content.recipe.RecipeMapSpec;

/** Complete original machine recipe-map catalogue; actual rows live at platform boundaries. */
public final class MachineRecipeMapDefinitions {
    private static final String GUI = "textures/gui/machines/";

    private MachineRecipeMapDefinitions() {}

    public static final RecipeMapSpec Furnace = RecipeMapSpec.builder("mc.recipe.furnace", "Furnace", GUI + "Oven")
            .items(1, 1, 1)
            .fluids(1, 1, 0)
            .build();
    public static final RecipeMapSpec Microwave = RecipeMapSpec.builder(
                    "gt.recipe.microwave", "Microwave", GUI + "Oven")
            .items(1, 1, 1)
            .fluids(1, 1, 0)
            .build();
    public static final RecipeMapSpec Roasting = RecipeMapSpec.builder("gt.recipe.roaster", "Roaster", GUI + "Roaster")
            .items(1, 3, 1)
            .fluids(1, 1, 1)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Distillery = RecipeMapSpec.builder(
                    "gt.recipe.distillery", "Distillery", GUI + "Distillery")
            .items(1, 2, 1)
            .fluids(1, 2, 1)
            .minimumInputs(2)
            .build();
    public static final RecipeMapSpec Extruder = RecipeMapSpec.builder(
                    "gt.recipe.extruder", "Extruder", GUI + "Extruder")
            .items(2, 2, 2)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Smelter = RecipeMapSpec.builder("gt.recipe.smelter", "Smelter", GUI + "Smelter")
            .items(1, 1, 0)
            .fluids(1, 1, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec CrystallisationCrucible = RecipeMapSpec.builder(
                    "gt.recipe.crystallisationcrucible", "Crystallisation Crucible", GUI + "CrystallisationCrucible")
            .items(1, 1, 1)
            .fluids(3, 0, 1)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Drying = RecipeMapSpec.builder("gt.recipe.drying", "Dryer", GUI + "Dryer")
            .items(1, 1, 0)
            .fluids(1, 3, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Laminator = RecipeMapSpec.builder(
                    "gt.recipe.laminator", "Laminator", GUI + "Laminator")
            .items(2, 1, 2)
            .fluids(0, 0, 0)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec CatalyticCracking = RecipeMapSpec.builder(
                    "gt.recipe.catalyticcracking", "Catalytic Cracking", GUI + "CatalyticCracking")
            .items(1, 3, 0)
            .fluids(2, 9, 1)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec SteamCracking = RecipeMapSpec.builder(
                    "gt.recipe.steamcracking", "Steam Cracking", GUI + "SteamCracking")
            .items(1, 3, 0)
            .fluids(2, 9, 1)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec DistillationTower = RecipeMapSpec.builder(
                    "gt.recipe.distillationtower", "Distillation Tower", GUI + "DistillationTower")
            .items(1, 3, 0)
            .fluids(1, 9, 0)
            .minimumInputs(1)
            .build();
    public static final RecipeMapSpec CryoDistillationTower = RecipeMapSpec.builder(
                    "gt.recipe.cryodistillationtower", "Cryo Distillation Tower", GUI + "CryoDistillationTower")
            .items(1, 3, 0)
            .fluids(1, 9, 0)
            .minimumInputs(1)
            .build();
    public static final RecipeMapSpec Shredder = RecipeMapSpec.builder(
                    "gt.recipe.shredder", "Shredder", GUI + "Shredder")
            .items(1, 12, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Lathe = RecipeMapSpec.builder("gt.recipe.lathe", "Lathe", GUI + "Lathe")
            .items(1, 2, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Cutter = RecipeMapSpec.builder("gt.recipe.cutter", "Cutter", GUI + "Cutter")
            .items(1, 3, 1)
            .fluids(1, 0, 1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Centrifuge = RecipeMapSpec.builder(
                    "gt.recipe.centrifuge", "Centrifuge", GUI + "Centrifuge")
            .items(1, 6, 0)
            .fluids(1, 6, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec RollingMill = RecipeMapSpec.builder(
                    "gt.recipe.rollingmill", "Rolling Mill", GUI + "RollingMill")
            .items(1, 1, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec RollBender = RecipeMapSpec.builder(
                    "gt.recipe.rollbender", "Roll Bender", GUI + "RollBender")
            .items(1, 1, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec RollFormer = RecipeMapSpec.builder(
                    "gt.recipe.rollformer", "Roll Former", GUI + "RollFormer")
            .items(1, 1, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec ClusterMill = RecipeMapSpec.builder(
                    "gt.recipe.clustermill", "Cluster Mill", GUI + "ClusterMill")
            .items(1, 1, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Wiremill = RecipeMapSpec.builder(
                    "gt.recipe.wiremill", "Wiremill", GUI + "Wiremill")
            .items(1, 1, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Mixer = RecipeMapSpec.builder("gt.recipe.mixer", "Mixer", GUI + "Mixer")
            .items(6, 1, 0)
            .fluids(6, 2, 0)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Loom = RecipeMapSpec.builder("gt.recipe.loom", "Loom", GUI + "Loom")
            .items(6, 1, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Sluice = RecipeMapSpec.builder("gt.recipe.sluice", "Sluice", GUI + "Sluice")
            .items(1, 9, 1)
            .fluids(1, 1, 1)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Sharpening = RecipeMapSpec.builder(
                    "gt.recipe.sharpener", "Sharpener", GUI + "Sharpener")
            .items(1, 2, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec BurnMixer = RecipeMapSpec.builder(
                    "gt.recipe.burnmixer", "Burner Mixer", GUI + "BurnMixer")
            .items(6, 1, 0)
            .fluids(6, 2, 0)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec PressureWasher = RecipeMapSpec.builder(
                    "gt.recipe.pressurewasher", "Pressure Washer", GUI + "PressureWasher")
            .items(1, 2, 1)
            .fluids(1, 0, 1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Crusher = RecipeMapSpec.builder("gt.recipe.crusher", "Crusher", GUI + "Crusher")
            .items(1, 12, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Sifting = RecipeMapSpec.builder("gt.recipe.sifter", "Sifter", GUI + "Sifter")
            .items(1, 12, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Squeezer = RecipeMapSpec.builder(
                    "gt.recipe.squeezer", "Squeezer", GUI + "Squeezer")
            .items(1, 2, 1)
            .fluids(0, 1, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Compressor = RecipeMapSpec.builder(
                    "gt.recipe.compressor", "Compressor", GUI + "Compressor")
            .items(1, 1, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Press = RecipeMapSpec.builder("gt.recipe.press", "Press", GUI + "Press")
            .items(3, 1, 2)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Electrolyzer = RecipeMapSpec.builder(
                    "gt.recipe.electrolyzer", "Electrolyzer", GUI + "Electrolyzer")
            .items(2, 6, 0)
            .fluids(2, 6, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Canner = RecipeMapSpec.builder(
                    "gt.recipe.canner", "Canning Machine", GUI + "Canner")
            .items(2, 2, 1)
            .fluids(1, 1, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Injector = RecipeMapSpec.builder(
                    "gt.recipe.injector", "Injector", GUI + "Injector")
            .items(2, 1, 0)
            .fluids(2, 1, 0)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Printer = RecipeMapSpec.builder("gt.recipe.printer", "Printer", GUI + "Printer")
            .items(2, 1, 1)
            .fluids(6, 0, 1)
            .minimumInputs(2)
            .build();
    public static final RecipeMapSpec ScannerVisuals = RecipeMapSpec.builder(
                    "gt.recipe.scannervisuals", "Scanner (Visuals)", GUI + "ScannerVisuals")
            .items(2, 2, 2)
            .fluids(0, 0, 0)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Autocrafter = RecipeMapSpec.builder(
                    "gt.recipe.autocrafting", "Crafting", GUI + "Crafting")
            .items(9, 12, 1)
            .fluids(0, 0, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .viewerAllowed(false)
            .build();
    public static final RecipeMapSpec Slicer = RecipeMapSpec.builder("gt.recipe.slicer", "Slicer", GUI + "Slicer")
            .items(2, 2, 2)
            .fluids(0, 0, 0)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Nanofab = RecipeMapSpec.builder(
                    "gt.recipe.nanofab", "Nanoscale Fabricator", GUI + "Nanofab")
            .items(2, 1, 0)
            .fluids(1, 1, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Plantalyzer = RecipeMapSpec.builder(
                    "gt.recipe.plantalyzer", "Plantalyzer", GUI + "Plantalyzer")
            .items(2, 2, 0)
            .fluids(1, 0, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Bumblelyzer = RecipeMapSpec.builder(
                    "gt.recipe.bumblelyzer", "Bumblelyzer", GUI + "Bumblelyzer")
            .items(2, 2, 0)
            .fluids(1, 0, 0)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Boxinator = RecipeMapSpec.builder(
                    "gt.recipe.boxinator", "Boxinator", GUI + "Boxinator")
            .items(2, 1, 2)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Unboxinator = RecipeMapSpec.builder(
                    "gt.recipe.unboxinator", "Unboxinator", GUI + "Unboxinator")
            .items(1, 12, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Polarizer = RecipeMapSpec.builder(
                    "gt.recipe.polarizer", "Polarizer", GUI + "Polarizer")
            .items(1, 1, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec MagneticSeparator = RecipeMapSpec.builder(
                    "gt.recipe.magneticseparator", "Magnetic Separator", GUI + "MagneticSeparator")
            .items(1, 6, 0)
            .fluids(1, 6, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec LaserEngraver = RecipeMapSpec.builder(
                    "gt.recipe.laserengraver", "Precision Laser Engraver", GUI + "LaserEngraver")
            .items(2, 1, 2)
            .fluids(0, 0, 0)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Welder = RecipeMapSpec.builder(
                    "gt.recipe.welder", "Welding Machine", GUI + "Welder")
            .items(9, 1, 2)
            .fluids(1, 0, 0)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Freezer = RecipeMapSpec.builder("gt.recipe.freezer", "Freezer", GUI + "Freezer")
            .items(1, 1, 1)
            .fluids(1, 1, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec CryoMixer = RecipeMapSpec.builder(
                    "gt.recipe.cryomixer", "Cryo Mixer", GUI + "CryoMixer")
            .items(6, 1, 0)
            .fluids(6, 2, 0)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Massfab = RecipeMapSpec.builder(
                    "gt.recipe.massfab", "Matter Fabricator", GUI + "Massfab")
            .items(2, 1, 0)
            .fluids(1, 2, 0)
            .minimumInputs(1)
            .build();
    public static final RecipeMapSpec ScannerMolecular = RecipeMapSpec.builder(
                    "gt.recipe.scannermolecular", "Molecular Scanner", GUI + "ScannerMolecular")
            .items(2, 1, 1)
            .fluids(0, 0, 0)
            .minimumInputs(2)
            .build();
    public static final RecipeMapSpec Replicator = RecipeMapSpec.builder(
                    "gt.recipe.replicator", "Matter Replicator", GUI + "Replicator")
            .items(3, 3, 1)
            .fluids(3, 3, 0)
            .minimumInputs(2)
            .build();
    public static final RecipeMapSpec Autoclave = RecipeMapSpec.builder(
                    "gt.recipe.autoclave", "Autoclave", GUI + "Autoclave")
            .items(2, 3, 2)
            .fluids(1, 1, 1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Bath = RecipeMapSpec.builder("gt.recipe.bath", "Bath", GUI + "Bath")
            .items(6, 6, 1)
            .fluids(1, 3, 1)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Generifier = RecipeMapSpec.builder(
                    "gt.recipe.generifier", "Generifier", GUI + "Generifier")
            .items(1, 1, 0)
            .fluids(1, 1, 0)
            .minimumInputs(1)
            .build();
    public static final RecipeMapSpec Coagulator = RecipeMapSpec.builder(
                    "gt.recipe.coagulator", "Coagulator", GUI + "Coagulator")
            .items(0, 1, 0)
            .fluids(1, 0, 1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Fermenter = RecipeMapSpec.builder(
                    "gt.recipe.fermenter", "Fermenter", GUI + "Fermenter")
            .items(1, 1, 1)
            .fluids(1, 1, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .potionInputs(1)
            .build();
    public static final RecipeMapSpec Melter = RecipeMapSpec.builder("gt.recipe.melter", "Melter", GUI + "Melter")
            .items(1, 1, 0)
            .fluids(1, 1, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec CokeOven = RecipeMapSpec.builder(
                    "gt.recipe.cokeoven", "Coke Oven", GUI + "CokeOven")
            .items(1, 9, 1)
            .fluids(0, 1, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Lightning = RecipeMapSpec.builder(
                    "gt.recipe.lightning", "Lightning Processor", GUI + "Lightning")
            .items(6, 6, 0)
            .fluids(6, 6, 0)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec ImplosionCompressor = RecipeMapSpec.builder(
                    "gt.recipe.implosioncompressor", "Implosion Compressor", GUI + "ImplosionCompressor")
            .items(3, 3, 3)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Fusion = RecipeMapSpec.builder(
                    "gt.recipe.fusionreactor", "Fusion Reactor", GUI + "Fusion")
            .items(2, 6, 1)
            .fluids(2, 6, 0)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Mortar = RecipeMapSpec.builder("gt.recipe.mortar", "Mortar", GUI + "Mortar")
            .items(1, 2, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec DidYouKnow = RecipeMapSpec.builder(
                    "gt.recipe.other", "Did you know...?", GUI + "Default")
            .items(6, 6, 0)
            .fluids(3, 3, 0)
            .minimumInputs(1)
            .showVoltageAmperage(false)
            .build();
    public static final RecipeMapSpec ToolHeads = RecipeMapSpec.builder(
                    "gt.recipe.toolhead", "Craft Head on Handle", GUI + "Crafting2By2")
            .items(4, 1, 0)
            .fluids(0, 0, 0)
            .showVoltageAmperage(false)
            .build();
    public static final RecipeMapSpec Cooking = RecipeMapSpec.builder("gt.recipe.cooker", "Cooker", GUI + "Cooker")
            .items(9, 1, 1)
            .fluids(3, 1, 1)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec BlastFurnace = RecipeMapSpec.builder(
                    "gt.recipe.blastfurnace", "Blast Furnace", GUI + "Default")
            .items(2, 2, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .viewerAllowed(false)
            .showVoltageAmperage(false)
            .build();
    public static final RecipeMapSpec VacuumFreezer = RecipeMapSpec.builder(
                    "gt.recipe.vacuumfreezer", "Vacuum Freezer", GUI + "Default")
            .items(1, 1, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .viewerAllowed(false)
            .build();
    public static final RecipeMapSpec Assembler = RecipeMapSpec.builder(
                    "gt.recipe.assembler", "Assembler", GUI + "Assembler")
            .items(2, 1, 1)
            .fluids(1, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .viewerAllowed(false)
            .build();
    public static final RecipeMapSpec CNC = RecipeMapSpec.builder(
                    "gt.recipe.cncmachine", "CNC Machine", GUI + "Default")
            .items(2, 1, 2)
            .fluids(1, 0, 1)
            .bucketInput(true)
            .bucketOutput(true)
            .viewerAllowed(false)
            .build();
    public static final RecipeMapSpec CrucibleAlloying = RecipeMapSpec.builder(
                    "gt.recipe.cruciblealloying", "Combination Smelting", GUI + "Alloying")
            .items(12, 12, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .specialValueLabel("Temperature: ", 1, " K")
            .instantRecipes()
            .build();
    public static final RecipeMapSpec CrucibleSmelting = RecipeMapSpec.builder(
                    "gt.recipe.cruciblesmelting", "Crucible Smelting", GUI + "Default")
            .items(6, 6, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .specialValueLabel("Temperature: ", 1, " K")
            .instantRecipes()
            .build();
    public static final RecipeMapSpec BedrockOreList = RecipeMapSpec.builder(
                    "gt.recipe.bedrockorelist", "Bedrock Drill", GUI + "BedrockOreList")
            .items(1, 12, 1)
            .fluids(1, 0, 1)
            .build();
    public static final RecipeMapSpec ByProductList = RecipeMapSpec.builder(
                    "gt.recipe.byproductlist", "Ore Byproduct List", GUI + "OreByproducts")
            .items(6, 12, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Hammer = RecipeMapSpec.builder("gt.recipe.hammer", "Hammer", GUI + "Hammer")
            .items(1, 1, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Chisel = RecipeMapSpec.builder("gt.recipe.chisel", "Chisel", GUI + "Chisel")
            .items(1, 1, 1)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .viewerAllowed(false)
            .build();
    public static final RecipeMapSpec Calciner = RecipeMapSpec.builder(
                    "gt.recipe.calciner", "Calciner", GUI + "Calciner")
            .items(3, 3, 0)
            .fluids(3, 3, 0)
            .minimumInputs(2)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Juicer = RecipeMapSpec.builder("gt.recipe.juicer", "Juicer", GUI + "Juicer")
            .items(1, 3, 1)
            .fluids(0, 1, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec Anvil = RecipeMapSpec.builder("gt.recipe.anvil", "Anvil", GUI + "Anvil")
            .items(2, 2, 2)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec AnvilBendSmall = RecipeMapSpec.builder(
                    "gt.recipe.anvil.bend.small", "Anvil Bending (Small)", GUI + "AnvilBendingSmall")
            .items(2, 2, 2)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec AnvilBendBig = RecipeMapSpec.builder(
                    "gt.recipe.anvil.bend.big", "Anvil Bending (Big)", GUI + "AnvilBendingBig")
            .items(2, 2, 2)
            .fluids(0, 0, 0)
            .bucketInput(true)
            .bucketOutput(true)
            .build();
    public static final RecipeMapSpec BumbleQueens = RecipeMapSpec.builder(
                    "gt.recipe.bumblequeen", "Bumblebee Queen", GUI + "Default")
            .items(2, 6, 0)
            .fluids(0, 0, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .showVoltageAmperage(false)
            .build();
    public static final RecipeMapSpec Trees = RecipeMapSpec.builder(
                    "gt.recipe.trees", "Family Tree", GUI + "FamilyTree")
            .items(3, 12, 0)
            .fluids(0, 0, 0)
            .minimumInputs(1)
            .bucketInput(true)
            .bucketOutput(true)
            .showVoltageAmperage(false)
            .build();

    public static java.util.List<RecipeMapSpec> all() {
        return java.util.List.of(
                Furnace,
                Microwave,
                Roasting,
                Distillery,
                Extruder,
                Smelter,
                CrystallisationCrucible,
                Drying,
                Laminator,
                CatalyticCracking,
                SteamCracking,
                DistillationTower,
                CryoDistillationTower,
                Shredder,
                Lathe,
                Cutter,
                Centrifuge,
                RollingMill,
                RollBender,
                RollFormer,
                ClusterMill,
                Wiremill,
                Mixer,
                Loom,
                Sluice,
                Sharpening,
                BurnMixer,
                PressureWasher,
                Crusher,
                Sifting,
                Squeezer,
                Compressor,
                Press,
                Electrolyzer,
                Canner,
                Injector,
                Printer,
                ScannerVisuals,
                Autocrafter,
                Slicer,
                Nanofab,
                Plantalyzer,
                Bumblelyzer,
                Boxinator,
                Unboxinator,
                Polarizer,
                MagneticSeparator,
                LaserEngraver,
                Welder,
                Freezer,
                CryoMixer,
                Massfab,
                ScannerMolecular,
                Replicator,
                Autoclave,
                Bath,
                Generifier,
                Coagulator,
                Fermenter,
                Melter,
                CokeOven,
                Lightning,
                ImplosionCompressor,
                Fusion,
                Mortar,
                DidYouKnow,
                ToolHeads,
                Cooking,
                BlastFurnace,
                VacuumFreezer,
                Assembler,
                CNC,
                CrucibleAlloying,
                CrucibleSmelting,
                BedrockOreList,
                ByProductList,
                Hammer,
                Chisel,
                Calciner,
                Juicer,
                Anvil,
                AnvilBendSmall,
                AnvilBendBig,
                BumbleQueens,
                Trees);
    }
}
