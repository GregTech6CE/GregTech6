package gregtech.content.recipe;

import static gregtech.content.recipe.WoodCraftingRow.Source.woods;
import static gregtech.content.recipe.WoodCraftingRow.Tool.FILE;
import static gregtech.content.recipe.WoodCraftingRow.Tool.SAW;
import static gregtech.content.recipe.WoodCraftingRow.shaped;
import static gregtech.content.recipe.WoodCraftingRow.shapeless;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.WoodCraftingRow.Key;
import gregtech.content.recipe.WoodCraftingRow.MaterialForm;
import gregtech.content.recipe.WoodCraftingRow.Source;
import gregtech.content.recipe.WoodCraftingRow.WoodBlock;
import gregtech.content.recipe.WoodCraftingRow.WoodForm;
import java.util.List;
import java.util.stream.Stream;

/**
 * The original wood dictionary and recipe loader define the row counts and source order.
 * The port represents GT6 beams as stripped logs.
 *
 * <p>The crafting-table half of GT6's wood dictionary recipes
 * ({@code gregtech/loaders/c/Loader_Recipes_Woods.java}, driven by
 * {@code gregapi/load/LoaderWoodDictionary.java} and {@code gregapi/wooddict/*}). GT6's file holds
 * 128 recipe statements: <b>71 {@code RM.*} machine rows</b> and
 * 57 {@code CR.shaped}/{@code CR.shapeless}/{@code CR.remove} crafting rows. The machine half is
 * already registered for the port's species by {@code content/recipe/RegisteredWoodSurvivalRecipes.java}
 * and {@code content/recipe/VanillaWoodProcessingRecipes.java}; the crafting half was missing and is
 * what this table carries.</p>
 *
 * <p>The rows iterate GT6's {@code WoodDictionary} entries. Only the entries backed by GT6's own nine
 * trees ({@code BlocksGT.LogA/LogB/LogC} metas 0-3/0-3/0) can be expressed: the port registers exactly
 * those species as {@code gt6ce:log_*} / {@code gt6ce:planks_*} / {@code gt6ce:stripped_log_*} blocks
 * ({@code registry/GTWoods.java}, {@code block/wood/WoodSpecies.java}). Every row below is registered
 * with the numbers GT6's own table carries for that species, with {@code CS.NERFED_WOOD = true}
 * ({@code CS} in the original), i.e. the hand/saw counts rather than the
 * buzz-saw/lathe ones.</p>
 *
 * <p>Port differences, all of them deliberate:</p>
 * <ul>
 *   <li>No fireproof wood: GT6's {@code BlocksGT.Log*FireProof}/{@code PlanksFireProof}/{@code Beam*FireProof}
 *       family (Laminator/Bath rows at {@code Loader_Recipes_Woods}) has no port block.</li>
 *   <li>No slabs or stairs for the port's species: GT6's {@code LIST_STAIRS}/{@code LIST_SLABS} rows
 *       are therefore skipped.</li>
 *   <li>Vanilla woods ({@code LoaderWoodDictionary}) keep vanilla's own recipes; the port's
 *       per-species log rows would only duplicate {@code minecraft:oak_planks} and friends.</li>
 *   <li>Other mods' wood (IC2/MFR/Atum/Fossil/BoP/TC/TF/BTL/Aether/Botania/Witchery/AbyssalCraft/
 *       Steamcraft/EBXL/Bamboo/Caveworld/TCFM/Forestry) is absent from 1.20.1.</li>
 *   <li>{@code MT.WOODS.Cinnamon} is the registered Cinnamonwood material (9317), separate from
 *       Cinnamon spice (9785). All eight dictionary rows are emitted for this species.</li>
 *   <li>The four fallen Log1 entries are registered separately by {@code NaturalWoodCraftingRows};
 *       this tree-species table does not duplicate those rows.</li>
 * </ul>
 *
 * <p>Each recipe carries its GT6 loader line. The species table also records the original
 * dictionary entry and plank metadata.
 */
public final class GTWoodRecipes {
    private GTWoodRecipes() {}

    public record OriginalBlock(String name, int metadata) {}

    public record Species(
            String id,
            String log,
            String planks,
            String strippedLog,
            String material,
            Source source,
            OriginalBlock originalPlanks) {}

    public static final List<Species> SPECIES = List.of(
            new Species(
                    "rubber",
                    "log_rubber",
                    "planks_rubber",
                    "stripped_log_rubber",
                    "WoodRubber",
                    new Source("LoaderWoodDictionary.java", 69),
                    new OriginalBlock("BlocksGT.Planks", 0)),
            new Species(
                    "maple",
                    "log_maple",
                    "planks_maple",
                    "stripped_log_maple",
                    "Maple",
                    new Source("LoaderWoodDictionary.java", 71),
                    new OriginalBlock("BlocksGT.Planks", 1)),
            new Species(
                    "willow",
                    "log_willow",
                    "planks_willow",
                    "stripped_log_willow",
                    "Willow",
                    new Source("LoaderWoodDictionary.java", 73),
                    new OriginalBlock("BlocksGT.Planks", 2)),
            new Species(
                    "blue_mahoe",
                    "log_blue_mahoe",
                    "planks_blue_mahoe",
                    "stripped_log_blue_mahoe",
                    "BlueMahoe",
                    new Source("LoaderWoodDictionary.java", 75),
                    new OriginalBlock("BlocksGT.Planks", 3)),
            new Species(
                    "hazel",
                    "log_hazel",
                    "planks_hazel",
                    "stripped_log_hazel",
                    "Hazel",
                    new Source("LoaderWoodDictionary.java", 91),
                    new OriginalBlock("BlocksGT.Planks", 4)),
            new Species(
                    "cinnamon",
                    "log_cinnamon",
                    "planks_cinnamon",
                    "stripped_log_cinnamon",
                    "Cinnamonwood",
                    new Source("LoaderWoodDictionary.java", 93),
                    new OriginalBlock("BlocksGT.Planks", 5)),
            new Species(
                    "coconut",
                    "log_coconut",
                    "planks_coconut",
                    "stripped_log_coconut",
                    "Coconutwood",
                    new Source("LoaderWoodDictionary.java", 95),
                    new OriginalBlock("BlocksGT.Planks", 6)),
            new Species(
                    "rainbowood",
                    "log_rainbowood",
                    "planks_rainbowood",
                    "stripped_log_rainbowood",
                    "Rainbowood",
                    new Source("LoaderWoodDictionary.java", 97),
                    new OriginalBlock("BlocksGT.Planks", 7)),
            new Species(
                    "bluespruce",
                    "log_bluespruce",
                    "planks_bluespruce",
                    "stripped_log_bluespruce",
                    "BlueSpruce",
                    new Source("LoaderWoodDictionary.java", 113),
                    new OriginalBlock("BlocksGT.Planks2", 0)));

    /** Crafting rows in GT6's own order per species. */
    public static final List<WoodCraftingRow> CRAFT_ROWS =
            SPECIES.stream().flatMap(species -> recipes(species).stream()).toList();

    /** Registered building-category rows, in publication order. */
    public static List<WoodCraftingRow> buildingRows() {
        return Stream.of(CRAFT_ROWS, NaturalWoodCraftingRows.ROWS, VanillaWoodCraftingRows.ROWS)
                .flatMap(List::stream)
                .toList();
    }

    private static List<WoodCraftingRow> recipes(Species species) {
        String path = species.id() + "/";
        var log = new WoodBlock(WoodForm.LOG, species.id());
        var planks = new WoodBlock(WoodForm.PLANKS, species.id());
        var stripped = new WoodBlock(WoodForm.STRIPPED, species.id());
        var rod = new MaterialForm(MaterialPrefix.stick, species.material());
        var longRod = new MaterialForm(MaterialPrefix.stickLong, species.material());
        var saw = new Key('s', SAW);
        var file = new Key('f', FILE);
        var logKey = new Key('L', log);
        var strippedKey = new Key('B', stripped);
        var plankKey = new Key('P', planks);
        return List.of(
                shaped(path + "log_to_sticks", woods(183), longRod, 2, List.of("sLf"), saw, logKey, file),
                shaped(path + "log_to_planks_saw", woods(184), planks, 4, List.of("s", "L"), saw, logKey),
                shapeless(path + "log_to_planks_hand", woods(185), planks, 2, log),
                shaped(path + "stripped_to_sticks", woods(203), longRod, 3, List.of("sBf"), saw, strippedKey, file),
                shaped(path + "stripped_to_planks_saw", woods(204), planks, 5, List.of("s", "B"), saw, strippedKey),
                shapeless(path + "stripped_to_planks_hand", woods(205), planks, 3, stripped),
                shaped(path + "planks_to_sticks_hand", woods(243), rod, 2, List.of("P", "P"), plankKey),
                shaped(path + "planks_to_sticks_saw", woods(244), rod, 2, List.of("s", "P"), saw, plankKey));
    }

    public enum StatementKind {
        RM,
        CR
    }

    public record SkippedStatement(Source source, StatementKind kind, String reason, String statement) {}

    /** GT6 statements of {@code Loader_Recipes_Woods} that have no port row. */
    public static final List<SkippedStatement> SKIPPED_ROWS = List.of(
            new SkippedStatement(
                    woods(48),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, plate.mat(MT.WaxRefractory, 6), ST.make(BlocksGT.Log1 , 1, i), ST.make(BlocksGT.Log1FireProof , 1, i));"),
            new SkippedStatement(
                    woods(49),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, plate.mat(MT.WaxRefractory, 6), ST.make(BlocksGT.LogA , 1, i), ST.make(BlocksGT.LogAFireProof , 1, i));"),
            new SkippedStatement(
                    woods(50),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, plate.mat(MT.WaxRefractory, 6), ST.make(BlocksGT.LogB , 1, i), ST.make(BlocksGT.LogBFireProof , 1, i));"),
            new SkippedStatement(
                    woods(51),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, plate.mat(MT.WaxRefractory, 6), ST.make(BlocksGT.LogC , 1, i), ST.make(BlocksGT.LogCFireProof , 1, i));"),
            new SkippedStatement(
                    woods(53),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, plate.mat(MT.WaxRefractory, 6), ST.make(BlocksGT.Beam1, 1, i), ST.make(BlocksGT.Beam1FireProof, 1, i));"),
            new SkippedStatement(
                    woods(54),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, plate.mat(MT.WaxRefractory, 6), ST.make(BlocksGT.Beam2, 1, i), ST.make(BlocksGT.Beam2FireProof, 1, i));"),
            new SkippedStatement(
                    woods(55),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, plate.mat(MT.WaxRefractory, 6), ST.make(BlocksGT.Beam3, 1, i), ST.make(BlocksGT.Beam3FireProof, 1, i));"),
            new SkippedStatement(
                    woods(56),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, plate.mat(MT.WaxRefractory, 6), ST.make(BlocksGT.BeamA, 1, i), ST.make(BlocksGT.BeamAFireProof, 1, i));"),
            new SkippedStatement(
                    woods(57),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, plate.mat(MT.WaxRefractory, 6), ST.make(BlocksGT.BeamB, 1, i), ST.make(BlocksGT.BeamBFireProof, 1, i));"),
            new SkippedStatement(
                    woods(58),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, plate.mat(MT.WaxRefractory, 6), ST.make(BlocksGT.BeamC, 1, i), ST.make(BlocksGT.BeamCFireProof, 1, i));"),
            new SkippedStatement(
                    woods(61),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, foil.mat(MT.WaxRefractory, 24), ST.make(BlocksGT.Log1 , 1, i), ST.make(BlocksGT.Log1FireProof , 1, i));"),
            new SkippedStatement(
                    woods(62),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, foil.mat(MT.WaxRefractory, 24), ST.make(BlocksGT.LogA , 1, i), ST.make(BlocksGT.LogAFireProof , 1, i));"),
            new SkippedStatement(
                    woods(63),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, foil.mat(MT.WaxRefractory, 24), ST.make(BlocksGT.LogB , 1, i), ST.make(BlocksGT.LogBFireProof , 1, i));"),
            new SkippedStatement(
                    woods(64),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, foil.mat(MT.WaxRefractory, 24), ST.make(BlocksGT.LogC , 1, i), ST.make(BlocksGT.LogCFireProof , 1, i));"),
            new SkippedStatement(
                    woods(66),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, foil.mat(MT.WaxRefractory, 24), ST.make(BlocksGT.Beam1, 1, i), ST.make(BlocksGT.Beam1FireProof, 1, i));"),
            new SkippedStatement(
                    woods(67),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, foil.mat(MT.WaxRefractory, 24), ST.make(BlocksGT.Beam2, 1, i), ST.make(BlocksGT.Beam2FireProof, 1, i));"),
            new SkippedStatement(
                    woods(68),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, foil.mat(MT.WaxRefractory, 24), ST.make(BlocksGT.Beam3, 1, i), ST.make(BlocksGT.Beam3FireProof, 1, i));"),
            new SkippedStatement(
                    woods(69),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, foil.mat(MT.WaxRefractory, 24), ST.make(BlocksGT.BeamA, 1, i), ST.make(BlocksGT.BeamAFireProof, 1, i));"),
            new SkippedStatement(
                    woods(70),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, foil.mat(MT.WaxRefractory, 24), ST.make(BlocksGT.BeamB, 1, i), ST.make(BlocksGT.BeamBFireProof, 1, i));"),
            new SkippedStatement(
                    woods(71),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, foil.mat(MT.WaxRefractory, 24), ST.make(BlocksGT.BeamC, 1, i), ST.make(BlocksGT.BeamCFireProof, 1, i));"),
            new SkippedStatement(
                    woods(74),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 192, ST.make(BlocksGT.Log1 , 1, i), FL.Potion_FireResistance_1L.make(100), NF, ST.make(BlocksGT.Log1FireProof , 1, i));"),
            new SkippedStatement(
                    woods(75),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 192, ST.make(BlocksGT.LogA , 1, i), FL.Potion_FireResistance_1L.make(100), NF, ST.make(BlocksGT.LogAFireProof , 1, i));"),
            new SkippedStatement(
                    woods(76),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 192, ST.make(BlocksGT.LogB , 1, i), FL.Potion_FireResistance_1L.make(100), NF, ST.make(BlocksGT.LogBFireProof , 1, i));"),
            new SkippedStatement(
                    woods(77),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 192, ST.make(BlocksGT.Beam1, 1, i), FL.Potion_FireResistance_1L.make(100), NF, ST.make(BlocksGT.Beam1FireProof, 1, i));"),
            new SkippedStatement(
                    woods(78),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 192, ST.make(BlocksGT.Beam2, 1, i), FL.Potion_FireResistance_1L.make(100), NF, ST.make(BlocksGT.Beam2FireProof, 1, i));"),
            new SkippedStatement(
                    woods(79),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 192, ST.make(BlocksGT.Beam3, 1, i), FL.Potion_FireResistance_1L.make(100), NF, ST.make(BlocksGT.Beam3FireProof, 1, i));"),
            new SkippedStatement(
                    woods(80),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 192, ST.make(BlocksGT.BeamA, 1, i), FL.Potion_FireResistance_1L.make(100), NF, ST.make(BlocksGT.BeamAFireProof, 1, i));"),
            new SkippedStatement(
                    woods(81),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 192, ST.make(BlocksGT.BeamB, 1, i), FL.Potion_FireResistance_1L.make(100), NF, ST.make(BlocksGT.BeamBFireProof, 1, i));"),
            new SkippedStatement(
                    woods(82),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 192, ST.make(BlocksGT.BeamC, 1, i), FL.Potion_FireResistance_1L.make(100), NF, ST.make(BlocksGT.BeamCFireProof, 1, i));"),
            new SkippedStatement(
                    woods(86),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 32, plate.mat(MT.WaxRefractory, 1), ST.make(BlocksGT.Planks , 1, i), ST.make(BlocksGT.PlanksFireProof , 1, i));"),
            new SkippedStatement(
                    woods(87),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 32, plate.mat(MT.WaxRefractory, 1), ST.make(((BlockMetaType)BlocksGT.Planks ).mSlabs[0], 2, i), ST.make(((BlockMetaTyp"),
            new SkippedStatement(
                    woods(88),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 32, plate.mat(MT.WaxRefractory, 1), ST.make(BlocksGT.Planks2 , 1, i), ST.make(BlocksGT.Planks2FireProof , 1, i));"),
            new SkippedStatement(
                    woods(89),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 32, plate.mat(MT.WaxRefractory, 1), ST.make(((BlockMetaType)BlocksGT.Planks2).mSlabs[0], 2, i), ST.make(((BlockMetaTyp"),
            new SkippedStatement(
                    woods(91),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 32, foil .mat(MT.WaxRefractory, 4), ST.make(BlocksGT.Planks , 1, i), ST.make(BlocksGT.PlanksFireProof , 1, i));"),
            new SkippedStatement(
                    woods(92),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 16, foil .mat(MT.WaxRefractory, 2), ST.make(((BlockMetaType)BlocksGT.Planks ).mSlabs[0], 1, i), ST.make(((BlockMetaTyp"),
            new SkippedStatement(
                    woods(93),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 32, foil .mat(MT.WaxRefractory, 4), ST.make(BlocksGT.Planks2 , 1, i), ST.make(BlocksGT.Planks2FireProof , 1, i));"),
            new SkippedStatement(
                    woods(94),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 16, foil .mat(MT.WaxRefractory, 2), ST.make(((BlockMetaType)BlocksGT.Planks2).mSlabs[0], 1, i), ST.make(((BlockMetaTyp"),
            new SkippedStatement(
                    woods(96),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 32, ST.make(BlocksGT.Planks , 1, i), FL.Potion_FireResistance_1L.make(20), NF, ST.make(BlocksGT.PlanksFireProof , 1, i));"),
            new SkippedStatement(
                    woods(97),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 16, ST.make(((BlockMetaType)BlocksGT.Planks ).mSlabs[0], 1, i), FL.Potion_FireResistance_1L.make(10), NF, ST.make(((BlockMet"),
            new SkippedStatement(
                    woods(98),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 32, ST.make(BlocksGT.Planks2 , 1, i), FL.Potion_FireResistance_1L.make(20), NF, ST.make(BlocksGT.Planks2FireProof , 1, i));"),
            new SkippedStatement(
                    woods(99),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 16, ST.make(((BlockMetaType)BlocksGT.Planks2).mSlabs[0], 1, i), FL.Potion_FireResistance_1L.make(10), NF, ST.make(((BlockMet"),
            new SkippedStatement(
                    woods(104),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, plate.mat(MT.WaxRefractory, 6), ST.make(tLog1 , 1, i), ST.make(tLog2 , 1, i));"),
            new SkippedStatement(
                    woods(105),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 32, plate.mat(MT.WaxRefractory, 1), ST.make(tPlank1, 1, i), ST.make(tPlank2, 1, i));"),
            new SkippedStatement(
                    woods(106),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 32, plate.mat(MT.WaxRefractory, 1), ST.make(tStair1, 1, i), ST.make(tStair2, 1, i));"),
            new SkippedStatement(
                    woods(107),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 32, plate.mat(MT.WaxRefractory, 1), ST.make(tSlab1 , 2, i), ST.make(tSlab2 , 2, i));"),
            new SkippedStatement(
                    woods(109),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 192, foil .mat(MT.WaxRefractory, 24), ST.make(tLog1 , 1, i), ST.make(tLog2 , 1, i));"),
            new SkippedStatement(
                    woods(110),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 32, foil .mat(MT.WaxRefractory, 4), ST.make(tPlank1, 1, i), ST.make(tPlank2, 1, i));"),
            new SkippedStatement(
                    woods(111),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 24, foil .mat(MT.WaxRefractory, 3), ST.make(tStair1, 1, i), ST.make(tStair2, 1, i));"),
            new SkippedStatement(
                    woods(112),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Laminator .addRecipe2(T, 16, 16, foil .mat(MT.WaxRefractory, 2), ST.make(tSlab1 , 1, i), ST.make(tSlab2 , 1, i));"),
            new SkippedStatement(
                    woods(114),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 192, ST.make(tLog1 , 1, i), FL.Potion_FireResistance_1L.make(100), NF, ST.make(tLog2 , 1, i));"),
            new SkippedStatement(
                    woods(115),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 32, ST.make(tPlank1, 1, i), FL.Potion_FireResistance_1L.make( 20), NF, ST.make(tPlank2, 1, i));"),
            new SkippedStatement(
                    woods(116),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 24, ST.make(tStair1, 1, i), FL.Potion_FireResistance_1L.make( 15), NF, ST.make(tStair2, 1, i));"),
            new SkippedStatement(
                    woods(117),
                    StatementKind.RM,
                    "fireproofing (Laminator/Bath): GT6's BlocksGT.*FireProof wood family is not registered by the port, so these rows have no output to produce",
                    "RM.Bath .addRecipe1(T, 0, 16, ST.make(tSlab1 , 1, i), FL.Potion_FireResistance_1L.make( 10), NF, ST.make(tSlab2 , 1, i));"),
            new SkippedStatement(
                    woods(122),
                    StatementKind.RM,
                    "Squeezer row (RM half): the latex/resin/sap chain is a machine row, not part of this crafting batch - the port has no Latex/Resin_Spruce/Sap_Maple/Sap_Rainbow recipe at all today",
                    "RM.Squeezer .addRecipe1(T, 16, 64, ST.make(BlocksGT.Leaves_AB , 1, 0), NF, FL.Latex.make(L/72), NI);"),
            new SkippedStatement(
                    woods(123),
                    StatementKind.RM,
                    "Squeezer row (RM half): the latex/resin/sap chain is a machine row, not part of this crafting batch - the port has no Latex/Resin_Spruce/Sap_Maple/Sap_Rainbow recipe at all today",
                    "RM.Squeezer .addRecipe1(T, 16, 64, ST.make(BlocksGT.Leaves_AB , 1, 8), NF, FL.Latex.make(L/72), NI);"),
            new SkippedStatement(
                    woods(124),
                    StatementKind.RM,
                    "Squeezer row (RM half): the latex/resin/sap chain is a machine row, not part of this crafting batch - the port has no Latex/Resin_Spruce/Sap_Maple/Sap_Rainbow recipe at all today",
                    "RM.Squeezer .addRecipe1(T, 16, 64, ST.make(BlocksGT.Saplings_AB, 1, 0), NF, FL.Latex.make(L/ 4), NI);"),
            new SkippedStatement(
                    woods(125),
                    StatementKind.RM,
                    "Squeezer row (RM half): the latex/resin/sap chain is a machine row, not part of this crafting batch - the port has no Latex/Resin_Spruce/Sap_Maple/Sap_Rainbow recipe at all today",
                    "RM.Squeezer .addRecipe1(T, 16, 64, ST.make(BlocksGT.Saplings_AB, 1, 8), NF, FL.Latex.make(L/ 4), NI);"),
            new SkippedStatement(
                    woods(126),
                    StatementKind.RM,
                    "Squeezer row (RM half): the latex/resin/sap chain is a machine row, not part of this crafting batch - the port has no Latex/Resin_Spruce/Sap_Maple/Sap_Rainbow recipe at all today",
                    "RM.Squeezer .addRecipe1(T, 16, 16, OM.dust(MT.WoodRubber ), NF, FL.Latex .make(L/9) , OM.dust(MT.Wood));"),
            new SkippedStatement(
                    woods(127),
                    StatementKind.RM,
                    "Squeezer row (RM half): the latex/resin/sap chain is a machine row, not part of this crafting batch - the port has no Latex/Resin_Spruce/Sap_Maple/Sap_Rainbow recipe at all today",
                    "RM.Squeezer .addRecipe1(T, 16, 16, OM.dust(MT.WOODS.Spruce ), NF, FL.Resin_Spruce.make(5, FL.Resin), OM.dust(MT.Wood));"),
            new SkippedStatement(
                    woods(128),
                    StatementKind.RM,
                    "Squeezer row (RM half): the latex/resin/sap chain is a machine row, not part of this crafting batch - the port has no Latex/Resin_Spruce/Sap_Maple/Sap_Rainbow recipe at all today",
                    "RM.Squeezer .addRecipe1(T, 16, 16, OM.dust(MT.WOODS.BlueSpruce ), NF, FL.Resin_Spruce.make(5, FL.Resin), OM.dust(MT.Wood));"),
            new SkippedStatement(
                    woods(129),
                    StatementKind.RM,
                    "Squeezer row (RM half): the latex/resin/sap chain is a machine row, not part of this crafting batch - the port has no Latex/Resin_Spruce/Sap_Maple/Sap_Rainbow recipe at all today",
                    "RM.Squeezer .addRecipe1(T, 16, 16, OM.dust(MT.WOODS.Maple ), NF, FL.Sap_Maple .make(5) , OM.dust(MT.Wood));"),
            new SkippedStatement(
                    woods(130),
                    StatementKind.RM,
                    "Squeezer row (RM half): the latex/resin/sap chain is a machine row, not part of this crafting batch - the port has no Latex/Resin_Spruce/Sap_Maple/Sap_Rainbow recipe at all today",
                    "RM.Squeezer .addRecipe1(T, 16, 16, OM.dust(MT.WOODS.Rainbowood ), NF, FL.Sap_Rainbow .make(5) , OM.dust(MT.Wood));"),
            new SkippedStatement(
                    woods(132),
                    StatementKind.CR,
                    "OD.plankWood bowl: vanilla ships minecraft:bowl from minecraft:planks, which does not contain the port's plank blocks; GT6's own row targets its ore dictionary name instead",
                    "CR.shaped(ST.make(Items.bowl, 1, 0), DEF | DEL_OTHER_SHAPED_RECIPES, \"k\", \"X\", 'X', OD.plankWood);"),
            new SkippedStatement(
                    woods(134),
                    StatementKind.CR,
                    "OD.stickAnyWood: the port publishes per-material rod tags only (data/MaterialTagPack.java), so there is no umbrella wood-rod tag to key this row on",
                    "CR.shaped(IL.Stick.get(1), CR.DEF_NCC, \" \", \" S\", 'S', stick.dat(ANY.WoodDefault));"),
            new SkippedStatement(
                    woods(137),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(gearGt .mat(MT.Wood, 1), CR.DEF_NCC, \"BPB\", \"PsP\", \"BPB\", 'P', OD.plankAnyWood, 'B', OD.buttonWood);"),
            new SkippedStatement(
                    woods(138),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(gearGtSmall .mat(MT.Wood, 1), CR.DEF_NCC, \"P \", \" s\" , 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(139),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(casingSmall .mat(MT.Wood, 2), CR.DEF_NCC, \" P\", \"s \" , 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(140),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(plateTiny .mat(MT.Wood, 9), CR.DEF_NCC, \"s \", \" P\" , 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(141),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(ring .mat(MT.Wood, 4), CR.DEF_NCC, \"P \", \" k\" , 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(142),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(round .mat(MT.Wood, 9), CR.DEF_NCC, \"P \", \"fk\" , 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(143),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(toolHeadHammer .mat(MT.Wood, 1), CR.DEF_NCC, \"PP \", \"PPg\", \"PPv\", 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(144),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(toolHeadRawArrow .mat(MT.Wood, 4), CR.DEF_NCC, \" P\", \"r v\", 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(145),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(toolHeadRawSword .mat(MT.Wood, 1), CR.DEF_NCC, \" P \", \"rPv\", 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(146),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(toolHeadRawPickaxe .mat(MT.Wood, 1), CR.DEF_NCC, \"PPP\", \"rgv\", 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(147),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(toolHeadRawShovel .mat(MT.Wood, 1), CR.DEF_NCC, \"rPv\", 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(148),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(toolHeadRawSpade .mat(MT.Wood, 1), CR.DEF_NCC, \" P \", \"r v\", 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(149),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(toolHeadRawAxe .mat(MT.Wood, 1), CR.DEF_NCC, \" PP\", \"rPv\", 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(150),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(toolHeadRawHoe .mat(MT.Wood, 1), CR.DEF_NCC, \" PP\", \"r v\", 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(151),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(toolHeadRawSense .mat(MT.Wood, 1), CR.DEF_NCC, \"PPP\", \" \", \"r v\", 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(152),
                    StatementKind.CR,
                    "GT6's wooden-button rows: the output form (OP.gearGt/casingSmall/toolHead*) is not generated for MT.Wood - MT.java:312 G_WOOD (TD.java:604) carries no PARTS/TOOL_HEAD, so the original's CR.shaped finds no output itself",
                    "CR.shaped(toolHeadRawPlow .mat(MT.Wood, 1), CR.DEF_NCC, \"PPP\", \"PPP\", \"r v\", 'P', OD.buttonWood);"),
            new SkippedStatement(
                    woods(157),
                    StatementKind.RM,
                    "Output is a Railcraft wooden tie, so this stays in the Railcraft batch. IE treated slabs exist on both loaders.",
                    "RM.Bath.addRecipe1(T, 0, 16, IL.IE_Treated_Slab .get(1), FL.Oil_Creosote.make(250), NF, IL.RC_Tie_Wood.get(1));"),
            new SkippedStatement(
                    woods(158),
                    StatementKind.RM,
                    "Output is a Railcraft wooden tie, so this stays in the Railcraft batch.",
                    "RM.Bath.addRecipe1(T, 0, 16, IL.Treated_Planks_Slab.get(1), FL.Oil_Creosote.make(250), NF, IL.RC_Tie_Wood.get(1));"),
            new SkippedStatement(
                    woods(161),
                    StatementKind.CR,
                    "Input is Railcraft creosote-treated wood, so this stays in the Railcraft batch.",
                    "CR.shapeless(IL.Plank.get(NERFED_WOOD?4:5), CR.DEF_NCC, new Object[] {IL.RC_Creosote_Wood});"),
            new SkippedStatement(
                    woods(172),
                    StatementKind.RM,
                    "Output is Railcraft creosote-treated wood, so this stays in the Railcraft batch.",
                    "RM.Bath.addRecipe1(T, 0,144, aEntry.mLog, FL.Oil_Creosote.make(1000), NF, IL.RC_Creosote_Wood.get(1));"),
            new SkippedStatement(
                    woods(174),
                    StatementKind.CR,
                    "Per-species log clubs are registered by OriginalHandCraftingRows.woodenClubs using the wooden-stick tag, outside this compatibility batch.",
                    "CR.shaped(ToolsGT.sMetaTool.getToolWithStats(ToolsGT.CLUB, aEntry.mMaterialWood, MT.Wood), CR.DEF_MIR, \" L\", \"S \", 'L', aEntry.mLog, 'S', OD.stickAnyW"),
            new SkippedStatement(
                    woods(179),
                    StatementKind.RM,
                    "Per-species log coke oven, not an Immersive Engineering or Railcraft recipe. It stays outside this compat batch.",
                    "RM.CokeOven.addRecipe1(T, 0, 3600, aEntry.mLog, NF, aEntry.mCreosoteAmount <= 0 ? NF : FL.Oil_Creosote.make(aEntry.mCreosoteAmount), tOutputs);"),
            new SkippedStatement(
                    woods(182),
                    StatementKind.CR,
                    "Removes recipes whose output is the log. Not an Immersive Engineering recipe, and this batch does not delete those rows.",
                    "CR.remove(ST.validMeta(1, aEntry.mLog));"),
            new SkippedStatement(
                    woods(195),
                    StatementKind.CR,
                    "Per-species beam clubs are registered by OriginalHandCraftingRows.woodenClubs using the wooden-stick tag, outside this compatibility batch.",
                    "CR.shaped(ToolsGT.sMetaTool.getToolWithStats(ToolsGT.CLUB, aEntry.mMaterialBeam, MT.Wood), CR.DEF_MIR, \" B\", \"S \", 'B', aEntry.mBeam, 'S', OD.stickAny"),
            new SkippedStatement(
                    woods(200),
                    StatementKind.RM,
                    "RM-half machine row (pressure washer / pulverizer / sawing / lathing / coke oven): the port registers these for its own species in content/recipe/RegisteredWoodSurvivalRecipes.java",
                    "RM.CokeOven.addRecipe1(T, 0, 3600, aEntry.mBeam, NF, aEntry.mCreosoteAmount <= 0 ? NF : FL.Oil_Creosote.make(aEntry.mCreosoteAmount), tOutputs);"),
            new SkippedStatement(
                    woods(215),
                    StatementKind.RM,
                    "RM-half machine row (Injector petrified wood / CNC gear): not part of this crafting batch",
                    "RM.Injector.addRecipe1(T, 16, 128, ST.amount(9, aEntry.mPlank), FL.MnWtr.make(1000), NF, OP.rockGt.mat(MT.PetrifiedWood, 4));"),
            new SkippedStatement(
                    woods(217),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(gearGt .mat(aEntry.mMaterialPlank, 1), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \"BPB\", \"PsP\", \"BPB\", 'P', aEntry.mPlank, 'B', bolt.dat(aEntry.mMa"),
            new SkippedStatement(
                    woods(218),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(gearGtSmall .mat(aEntry.mMaterialPlank, 1), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \"P \", \" s\" , 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(219),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(casingSmall .mat(aEntry.mMaterialPlank, 2), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \" P\", \"s \" , 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(220),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(plateTiny .mat(aEntry.mMaterialPlank, 9), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \"s \", \" P\" , 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(221),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(ring .mat(aEntry.mMaterialPlank, 4), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \"P \", \" k\" , 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(222),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(round .mat(aEntry.mMaterialPlank, 9), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \"P \", \"fk\" , 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(223),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(toolHeadBuilderwand.mat(aEntry.mMaterialPlank, 1), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \"ks\", \"fP\" , 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(224),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(toolHeadHammer .mat(aEntry.mMaterialPlank, 1), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \"PP \", \"PPg\", \"PPv\", 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(226),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(toolHeadRawArrow .mat(aEntry.mMaterialPlank, 4), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \" P\", \"r v\", 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(227),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(toolHeadRawSword .mat(aEntry.mMaterialPlank, 1), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \" P \", \"rPv\", 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(228),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(toolHeadRawPickaxe .mat(aEntry.mMaterialPlank, 1), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \"PPP\", \"rgv\", 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(229),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(toolHeadRawShovel .mat(aEntry.mMaterialPlank, 1), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \"rPv\", 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(230),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(toolHeadRawSpade .mat(aEntry.mMaterialPlank, 1), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \" P \", \"r v\", 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(231),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(toolHeadRawAxe .mat(aEntry.mMaterialPlank, 1), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \" PP\", \"rPv\", 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(232),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(toolHeadRawHoe .mat(aEntry.mMaterialPlank, 1), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \" PP\", \"r v\", 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(233),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(toolHeadRawSense .mat(aEntry.mMaterialPlank, 1), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \"PPP\", \" \", \"r v\", 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(234),
                    StatementKind.CR,
                    "per-plank forms (gear/casing/tiny plate/round/ring/tool heads/rolling pin): both GT6's ONLY_IF_HAS_RESULT flag and the port skip them - MT.Wood carries no PARTS/TOOL_HEAD form",
                    "CR.shaped(toolHeadRawPlow .mat(aEntry.mMaterialPlank, 1), CR.ONLY_IF_HAS_RESULT | CR.DEF_NCC, \"PPP\", \"PPP\", \"r v\", 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(237),
                    StatementKind.CR,
                    "OD.stickAnyWood: the port publishes per-material rod tags only (data/MaterialTagPack.java), so there is no umbrella wood-rod tag to key this row on",
                    "CR.shaped(ToolsGT.sMetaTool.getToolWithStats(ToolsGT.ROLLING_PIN, aEntry.mMaterialPlank, MT.Wood), CR.DEF_MIR, \" S\", \" P \", \"S f\", 'P', aPlank, 'S', O"),
            new SkippedStatement(
                    woods(238),
                    StatementKind.CR,
                    "OD.stickAnyWood: the port publishes per-material rod tags only (data/MaterialTagPack.java), so there is no umbrella wood-rod tag to key this row on",
                    "CR.shaped(ToolsGT.sMetaTool.getToolWithStats(ToolsGT.ROLLING_PIN, aEntry.mMaterialPlank, MT.Wood), CR.DEF_MIR, \" S\", \" P \", \"S k\", 'P', aPlank, 'S', O"),
            new SkippedStatement(
                    woods(242),
                    StatementKind.CR,
                    "CR.remove: removes the recipes competing with the rows above; the port has no competing recipe for its log/plank/beam blocks, so there is nothing to remove",
                    "CR.remove(aPlank, NI, NI, aPlank);"),
            new SkippedStatement(
                    woods(262),
                    StatementKind.RM,
                    "RM-half machine row (Injector petrified wood / CNC gear): not part of this crafting batch",
                    "RM.CNC.addRecipe2(T, 16, 64, ST.amount(4, aEntry.mPlank), ST.tag(0), tFluid, NF, tGear);"),
            new SkippedStatement(
                    woods(266),
                    StatementKind.CR,
                    "stairs: the port registers no stairs block for its own wood species",
                    "CR.shaped(ST.validMeta_(4, aEntry.mStair), CR.DEF_NCC_MIR, \"vP\", \"PP\", 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(270),
                    StatementKind.CR,
                    "slabs: the port registers no slab block for its own wood species",
                    "CR.shaped(ST.validMeta_(2, aEntry.mSlab), CR.DEF_NCC_MIR, \"vP\", 'P', aEntry.mPlank);"),
            new SkippedStatement(
                    woods(281),
                    StatementKind.CR,
                    "CR.remove: removes the recipes competing with the rows above; the port has no competing recipe for its log/plank/beam blocks, so there is nothing to remove",
                    "CR.remove(NI, NI, aPlank, NI, aPlank, aPlank, aPlank, aPlank, aPlank);"),
            new SkippedStatement(
                    woods(282),
                    StatementKind.CR,
                    "CR.remove: removes the recipes competing with the rows above; the port has no competing recipe for its log/plank/beam blocks, so there is nothing to remove",
                    "CR.remove(aPlank, NI, NI, aPlank, aPlank, NI, aPlank, aPlank, aPlank);"),
            new SkippedStatement(
                    woods(285),
                    StatementKind.RM,
                    "RM-half machine row (Injector petrified wood / CNC gear): not part of this crafting batch",
                    "RM.Injector.addRecipe1(T, 16, 96, ST.amount(9, aEntry.mStair), FL.MnWtr.make(750), NF, OP.rockGt.mat(MT.PetrifiedWood, 3));"),
            new SkippedStatement(
                    woods(288),
                    StatementKind.CR,
                    "stairs: the port registers no stairs block for its own wood species",
                    "CR.shaped(ST.validMeta_(1, aEntry.mSlab ), CR.DEF_NCC_MIR, \"vP\", 'P', aEntry.mStair);"),
            new SkippedStatement(
                    woods(299),
                    StatementKind.CR,
                    "CR.remove: removes the recipes competing with the rows above; the port has no competing recipe for its log/plank/beam blocks, so there is nothing to remove",
                    "CR.remove(aPlank, aPlank, aPlank);"),
            new SkippedStatement(
                    woods(302),
                    StatementKind.RM,
                    "RM-half machine row (Injector petrified wood / CNC gear): not part of this crafting batch",
                    "RM.Injector.addRecipe1(T, 16, 64, ST.amount(9, aEntry.mSlab), FL.MnWtr.make(500), NF, OP.rockGt.mat(MT.PetrifiedWood, 2));"),
            new SkippedStatement(
                    woods(305),
                    StatementKind.CR,
                    "stairs: the port registers no stairs block for its own wood species",
                    "CR.shaped(ST.validMeta_(2, aEntry.mStair), CR.DEF_NCC_MIR, \"vP\", \"PP\", 'P', aEntry.mSlab);"));

    /**
     * Rows the eight dictionary statements would produce but this port cannot express, with the GT6
     * line and the reason. The registered and skipped row counts together cover all nine species.
     */
    public static final List<SkippedCraftRow> SKIPPED_CRAFT_ROWS = List.of();

    public record SkippedCraftRow(String path, Source source, String reason) {}

    public record DictionaryEntry(String entry, String source, List<String> notes) {
        public DictionaryEntry {
            notes = List.copyOf(notes);
        }
    }

    /** GT6 wood-dictionary entries with no port species, with the GT6 line and the reason. */
    public static final List<DictionaryEntry> UNMAPPED_ENTRIES = List.of(
            new DictionaryEntry(
                    "BlocksGT.Beam1FireProof",
                    "LoaderWoodDictionary.java:57-60",
                    List.of(
                            "4 entries",
                            "GT6-only wood block family: the port registers none of these (dead/rotten/mossy/frozen planks, the fireproof twins and GT6's own beam blocks)")),
            new DictionaryEntry(
                    "BlocksGT.Beam2FireProof",
                    "LoaderWoodDictionary.java:61-176",
                    List.of(
                            "4 entries",
                            "GT6-only wood block family: the port registers none of these (dead/rotten/mossy/frozen planks, the fireproof twins and GT6's own beam blocks)")),
            new DictionaryEntry(
                    "BlocksGT.Beam3FireProof",
                    "LoaderWoodDictionary.java:229-273",
                    List.of(
                            "4 entries",
                            "GT6-only wood block family: the port registers none of these (dead/rotten/mossy/frozen planks, the fireproof twins and GT6's own beam blocks)")),
            new DictionaryEntry(
                    "BlocksGT.Log1FireProof",
                    "LoaderWoodDictionary.java:166-172",
                    List.of(
                            "4 entries",
                            "GT6-only wood block family: the port registers none of these (dead/rotten/mossy/frozen planks, the fireproof twins and GT6's own beam blocks)")),
            new DictionaryEntry(
                    "BlocksGT.Log1",
                    "LoaderWoodDictionary.java:165-171",
                    List.of(
                            "4 entries",
                            "GT6-only wood block family: the port registers none of these (dead/rotten/mossy/frozen planks, the fireproof twins and GT6's own beam blocks)")),
            new DictionaryEntry(
                    "BlocksGT.LogAFireProof",
                    "LoaderWoodDictionary.java:70-84",
                    List.of(
                            "8 entries",
                            "GT6-only wood block family: the port registers none of these (dead/rotten/mossy/frozen planks, the fireproof twins and GT6's own beam blocks)")),
            new DictionaryEntry(
                    "BlocksGT.LogBFireProof",
                    "LoaderWoodDictionary.java:92-106",
                    List.of(
                            "8 entries",
                            "GT6-only wood block family: the port registers none of these (dead/rotten/mossy/frozen planks, the fireproof twins and GT6's own beam blocks)")),
            new DictionaryEntry(
                    "BlocksGT.LogCFireProof",
                    "LoaderWoodDictionary.java:114-122",
                    List.of(
                            "2 entries",
                            "GT6-only wood block family: the port registers none of these (dead/rotten/mossy/frozen planks, the fireproof twins and GT6's own beam blocks)")),
            new DictionaryEntry(
                    "BlocksGT.PlanksFireProof",
                    "LoaderWoodDictionary.java:158-162",
                    List.of(
                            "3 entries",
                            "the extra plank blocks of this family: metas 0-7 are the mapped species' planks, the rest are GT6's compressed/treated/dead/rotten/fireproof planks, which the port does not register")),
            new DictionaryEntry(
                    "BlocksGT.Planks",
                    "LoaderWoodDictionary.java:157-161",
                    List.of(
                            "3 entries",
                            "the extra plank blocks of this family: metas 0-7 are the mapped species' planks, the rest are GT6's compressed/treated/dead/rotten/fireproof planks, which the port does not register")),
            new DictionaryEntry(
                    "LogA meta 0", "LoaderWoodDictionary.java:69", List.of("mapped to the port's rubber blocks")),
            new DictionaryEntry(
                    "LogA meta 1", "LoaderWoodDictionary.java:71", List.of("mapped to the port's maple blocks")),
            new DictionaryEntry(
                    "LogA meta 2", "LoaderWoodDictionary.java:73", List.of("mapped to the port's willow blocks")),
            new DictionaryEntry(
                    "LogA meta 3", "LoaderWoodDictionary.java:75", List.of("mapped to the port's blue_mahoe blocks")),
            new DictionaryEntry(
                    "LogB meta 0", "LoaderWoodDictionary.java:91", List.of("mapped to the port's hazel blocks")),
            new DictionaryEntry(
                    "LogB meta 1", "LoaderWoodDictionary.java:93", List.of("mapped to the port's cinnamon blocks")),
            new DictionaryEntry(
                    "LogB meta 2", "LoaderWoodDictionary.java:95", List.of("mapped to the port's coconut blocks")),
            new DictionaryEntry(
                    "LogB meta 3", "LoaderWoodDictionary.java:97", List.of("mapped to the port's rainbowood blocks")),
            new DictionaryEntry(
                    "LogC meta 0", "LoaderWoodDictionary.java:113", List.of("mapped to the port's bluespruce blocks")),
            new DictionaryEntry(
                    "MD.BOTA",
                    "LoaderWoodDictionary.java:294-297",
                    List.of(
                            "2 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "MD.BTL",
                    "LoaderWoodDictionary.java:263-263",
                    List.of(
                            "1 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "MD.CHSL",
                    "LoaderWoodDictionary.java:928-933",
                    List.of(
                            "6 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "MD.EBXL",
                    "LoaderWoodDictionary.java:359-373",
                    List.of(
                            "15 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "MD.EB",
                    "LoaderWoodDictionary.java:451-451",
                    List.of(
                            "1 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "MD.ERE",
                    "LoaderWoodDictionary.java:786-792",
                    List.of(
                            "5 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "MD.EtFu",
                    "LoaderWoodDictionary.java:893-916",
                    List.of(
                            "19 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "MD.IE",
                    "LoaderWoodDictionary.java:953-954",
                    List.of(
                            "2 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "MD.MF2",
                    "LoaderWoodDictionary.java:752-752",
                    List.of(
                            "1 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "MD.MoCr",
                    "LoaderWoodDictionary.java:939-939",
                    List.of(
                            "1 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "MD.NePl",
                    "LoaderWoodDictionary.java:873-880",
                    List.of(
                            "5 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "MD.RH",
                    "LoaderWoodDictionary.java:653-685",
                    List.of(
                            "32 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "MD.SC2",
                    "LoaderWoodDictionary.java:335-338",
                    List.of(
                            "4 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "MD.TF",
                    "LoaderWoodDictionary.java:256-258",
                    List.of(
                            "3 entries",
                            "another mod's block (IC2/MFR/BoP/TC/TF/EBXL/RH/EtFu/...), not present in 1.20.1")),
            new DictionaryEntry(
                    "ebony (Ebony)",
                    "LoaderWoodDictionary.java:541",
                    List.of(
                            "mapped 1:0 - the only Ebony dictionary row is another mod's block",
                            "the port's own ebony log/planks/beam have no GT6 source row")),
            new DictionaryEntry(
                    "pine (Pine)",
                    "LoaderWoodDictionary.java:402",
                    List.of(
                            "mapped 1:0 - the only Pine dictionary row is another mod's block",
                            "the port's own pine log/planks/beam have no GT6 source row")),
            new DictionaryEntry(
                    "tLog",
                    "LoaderWoodDictionary.java:532-643",
                    List.of("96 entries", "a local variable of a mod-specific row of the table (other mod)")),
            new DictionaryEntry(
                    "tPlank",
                    "LoaderWoodDictionary.java:343-790",
                    List.of("16 entries", "a local variable of a mod-specific row of the table (other mod)")),
            new DictionaryEntry(
                    "white_mahoe (Mahoe)",
                    "LoaderWoodDictionary.java (no row)",
                    List.of(
                            "mapped 1:0 - GT6 never registered a dictionary row for Mahoe",
                            "the port's own white_mahoe log/planks/beam have no GT6 source row")));

    /** Number of crafting rows this table carries. */
    public static int craftRowCount() {
        return CRAFT_ROWS.size();
    }

    /** Number of GT6 recipe statements the source file holds, for reports. */
    public static int gt6StatementCount() {
        return 128;
    }

    /** Number of {@code RM.*} machine rows in GT6's file (the "71 rows"). */
    public static int gt6MachineRowCount() {
        return 71;
    }

    /** Number of {@code CR.*} crafting statements in GT6's file. */
    public static int gt6CraftingStatementCount() {
        return 57;
    }
}
