package gregtech.content.recipe;

//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.generated.MaterialWorkability;
import gregtech.content.registry.GTItems;
import gregtech.util.OM;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GT6's Mortar handler block: {@code Loader_Recipes_Handlers:79-112}.
 * <p>
 * Every row is a {@code RecipeMapHandlerPrefix} with a {@code null} output prefix, which is GT6's
 * "<em>pulverized remains</em>" form: the input's whole material amount comes back as dust
 * ({@link #pulverize(GTMaterial, long)} = {@code OM.pulverize}, GT6 {@code gregapi/util/OM.java:370-372}). Except for
 * small rocks, the rows are gated on the material flag {@code MORTAR} ("can be ground in a mortar", {@code TD.java:515}),
 * plus, for gems, sticks, ingots and plates, {@code BRITTLE} or {@code FOOD}
 * ({@code Or(BRITTLE, FOOD, <finer form>.NOT)} / {@code Or(BRITTLE, FOOD, WOOD)}).
 * <p>
 * Hand-grinding is GT6's early-game ore/dust path — before the first machine you break ore chunks,
 * nuggets and gems in a mortar — so without this table the port's mortar could only grind the handful
 * of hand-written vanilla entries. The flags come from {@code tools/extract_gt6_workability.py}
 * The generated flag table currently omits factory-inherited flags and has field/name mismatches;
 * its size is not a count of all mortar-grindable materials in the original (ALIGN-010).
 */
public final class MortarGrindingRecipes {

    /** One registered grinding route, for tests and reports. */
    public record Entry(String input, RecipeMap map, Recipe recipe) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = List.of(
            "six external-form rows (:84-89) bind actual items through ExternalOreProcessing after tags");

    /** Rows gated on MORTAR; the original rockGt row is handled separately. */
    private static final List<String> PLAIN_ROWS = List.of(
            "crushedPurified", "crushedPurifiedTiny", "crushedCentrifuged", "crushedCentrifugedTiny",
            "scrapGt", "billet", "chunkGt", "nugget", "plateTiny", "plateGemTiny", "round", "screw", "bolt",
            "wireFine", "toolHeadArrow", "toolHeadRawArrow", "gemChipped");

    /**
     * The gem ladder: GT6 only grinds a gem row when the material has no finer gem form (or is
     * brittle/food), so a real gem is not destroyed by accident.
     */
    private static final List<String[]> GEM_ROWS = List.of(
            new String[]{"gemFlawed", "gemChipped"},
            new String[]{"gem", "gemFlawed"},
            new String[]{"gemFlawless", "gem"},
            new String[]{"gemExquisite", "gemFlawless"},
            new String[]{"gemLegendary", "gemExquisite"});

    /** Rows gated on {@code MORTAR} plus {@code Or(BRITTLE, FOOD, WOOD)}. */
    private static final List<String> WOOD_ROWS = List.of(
            "stick", "stickLong", "ingot", "plate", "plateGem");

    private static boolean registered;

    private MortarGrindingRecipes() {
    }

    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static List<String> skipped() {
        return SKIPPED;
    }

    public static int register() {
        if (registered) throw new IllegalStateException("Mortar grinding recipes registered twice");
        registered = true;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.has(MaterialProperty.ANTIMATTER)) continue;
            // small rocks only require ANTIMATTER.NOT, not MORTAR.
            grind(material, "rockGt");
            if (!MaterialWorkability.isMortarGrindable(material)) continue;
            for (String row : PLAIN_ROWS) grind(material, row);
            boolean brittleOrFood = MaterialWorkability.isBrittle(material) || MaterialWorkability.isFood(material);
            for (String[] row : GEM_ROWS) {
                if (brittleOrFood || GTItems.getStack(prefixOf(row[1]), material, 1).isEmpty()) grind(material, row[0]);
            }
            if (brittleOrFood || material.has(MaterialProperty.WOOD)) {
                for (String row : WOOD_ROWS) grind(material, row);
            }
        }
//? if >=1.21.1 {
        /*com.mojang.logging.LogUtils.getLogger().info("Registered {} GT6 mortar grinding recipes ({} GT6 rows skipped: {})",
*///?} else {
        GregTech.LOGGER.info("Registered {} GT6 mortar grinding recipes ({} GT6 rows skipped: {})",
//?}
                ENTRIES.size(), SKIPPED.size(), SKIPPED);
        return ENTRIES.size();
    }

    private static void grind(GTMaterial material, String prefixName) {
        MaterialPrefix prefix = prefixOf(prefixName);
        if (prefix == null) return;
        ItemStack input = GTItems.getStack(prefix, material, 1);
        if (input.isEmpty()) return;
        long units = prefix.getMaterialWeight();
        ItemStack output = pulverize(material, units);
        if (output.isEmpty()) return;
        // GT6 RecipeMapHandlerPrefix#getCosts: ceil(units / U * 16 * (1 + toolQuality)), 16 EU/t.
        long duration = Math.max(1, (units * 16L * (material.getToolQuality() + 1) + GTValues.U - 1) / GTValues.U);
        Recipe recipe = MachineRecipeMaps.Mortar.addRecipe1(true, 16, duration, input, output);
        if (recipe != null) ENTRIES.add(new Entry(prefixName, MachineRecipeMaps.Mortar, recipe));
    }

    /** GT6 {@code OM.pulverize(material, amount)} ({@code gregapi/util/OM.java:370-372}): dust of the pulverization target. */
    public static ItemStack pulverize(GTMaterial material, long amount) {
        GTMaterial target = material.getTargetPulverMaterial();
        if (target == null || !target.isValid()) return ItemStack.EMPTY;
        return OM.dust(target, PulverizationRules.amount(amount, material.getTargetPulverAmount()));
    }

    /** Port prefix by GT6 name; null when the port has no such form (recorded in {@link #SKIPPED}). */
    private static MaterialPrefix prefixOf(String prefixName) {
        return gregtech.engine.prefix.PrefixRegistry.byName(prefixName);
    }
}
