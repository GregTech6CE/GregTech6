package gregtech.content.data;

import com.google.gson.Gson;
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.data.generated.GTSensorRecipesGen;
import gregtech.platform.Mc;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Crafting recipes for the port's 20 sensor machines, taken from the original GT6 registrations
 * ({@link GTSensorRecipesGen}, generated from {@code Loader_MultiTileEntities}).
 * <p>
 * brought the port up to GT6's twenty sensors but none of them could be crafted. This pack
 * closes that: it is the same "generated table + server data pack" route {@link MultiblockRecipePack}
 * already uses for the multiblock controllers, with three deliberate translations.
 * </p>
 *
 * <h2>1. One recipe step instead of two</h2>
 * <p>
 * GT6 passes the pattern and the key map to {@code MultiTileEntityRegistry#add}, which emits
 * {@code CR.shaped(getItem(mID), CR.DEF_REV_NCC, aRecipe)} ({@code MultiTileEntityRegistry}) and
 * then, on every one of the twenty rows, a trailing
 * {@code CR.shapeless(aRegistry.getItem(), CR.DEF_NCC, new Object[]{aRegistry.getItem()})}. The port
 * has no shared multi-tile registry item - each sensor is its own block - so the shaped recipe
 * outputs the sensor block item directly and the shapeless conversion step has nothing to convert.
 * </p>
 *
 * <h2>2. {@code allow_mirror = false}</h2>
 * <p>
 * {@code CR.DEF_REV_NCC} is {@code BUF|NO_REM|REV|NO_COLLISION_CHECK} ({@code CR}); it
 * does <em>not</em> contain {@code MIR} ({@code CR}), so GT6's own recipes are not
 * mirror-craftable. The multiblock pack encodes the same flag set the same way.
 * </p>
 *
 * <h2>3. Tier 1</h2>
 * <p>
 * The port's sensors are not tiered machines - GT6 registers all twenty against {@code aUtilMetal}
 * with a {@code null} material argument, and the only tiered key in the twenty rows is
 * {@code IL.SENSORS[1]} (the Laser-O-Meter's centre), which is the <b>LV</b> compact
 * sensor. {@link #SENSOR_TIER} is therefore 1: it makes {@code il:SENSORS} resolve to
 * {@code gt6ce:compact_sensor_lv} exactly as GT6's index does, and no other key is tier-sensitive.
 * </p>
 *
 * <h2>4. Unused keys are dropped</h2>
 * <p>
 * GT6's rows declare keys their 3x3 pattern never spells - all twenty carry {@code G}, {@code B} and
 * {@code C} even where the pattern has no such cell. GT6's own recipe handler ignores extra keys, but
 * vanilla's does not: {@code ShapedRecipe.dissolvePattern} throws
 * {@code JsonSyntaxException("Key defines symbols that aren't used in pattern: […]")}
 * ({@code ShapedRecipe.java}), and the port's {@code gt6ce:tool_shaped} serializer
 * delegates straight to it. Emitting the rows verbatim therefore made all twenty recipes fail to
 * load. {@link #usedSymbols(List)} is the filter, and the key map is resolved
 * <em>after</em> filtering so an unused key cannot log a substitution it never needed.
 * </p>
 */
public final class SensorRecipePack {
    private static final Gson GSON = new Gson();
    /** GT6's twenty sensor rows are registered against {@code aUtilMetal} with a null material. */
    private static final String DEFAULT_MATERIAL = "StainlessSteel";
    /** The single tiered key is {@code IL.SENSORS[1]} = LV, so the rows resolve at tier 1. */
    public static final int SENSOR_TIER = 1;

    private static final Set<String> EMITTED = new HashSet<>();

    private SensorRecipePack() {}

    /** Port block ids that received a recipe in the last generated pack. */
    public static Set<String> emittedBlockIds() {
        return Collections.unmodifiableSet(EMITTED);
    }

    /**
     * The pattern symbols a row actually uses, in no particular order. Blank cells are not symbols.
     * <p>
     * Every one of GT6's twenty rows declares more keys than its pattern spells (see the class
     * javadoc, item 4); vanilla rejects the surplus, so both the pack and its test filter through
     * this method rather than re-deriving the rule.
     * </p>
     */
    public static Set<Character> usedSymbols(List<String> pattern) {
        Set<Character> used = new HashSet<>();
        for (String row : pattern) {
            for (int i = 0; i < row.length(); i++) {
                char symbol = row.charAt(i);
                if (symbol != ' ') used.add(symbol);
            }
        }
        return used;
    }

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        EMITTED.clear();

        for (GTSensorRecipesGen.Row row : GTSensorRecipesGen.ROWS) {
            ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath("gt6ce", row.blockId());
            Block block = Mc.block(blockId);
            if (block == null || block == Blocks.AIR) continue;
            Item item = block.asItem();
            if (item == Items.AIR) continue;

            Map<String, Object> recipe = buildRecipe(row, item);
            if (recipe == null) continue;

            generated.put(ResourceLocation.fromNamespaceAndPath("gt6ce",
//? if >=1.21.1 {
                    /*"recipe/machines/sensors/" + row.blockId() + ".json"),
*///?} else {
                    "recipes/machines/sensors/" + row.blockId() + ".json"),
//?}
                    GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8));
            EMITTED.add(row.blockId());
        }

//? if >=1.21.1 {
        /*com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original sensor crafting datapack: {} emitted, {} not registered or unresolved",
                EMITTED.size(), GTSensorRecipesGen.ROWS.size() - EMITTED.size());
*///?} else {
//?}
    }

    private static Map<String, Object> buildRecipe(GTSensorRecipesGen.Row row, Item resultItem) {
        List<String> pattern = new ArrayList<>(row.pattern());
        while (!pattern.isEmpty() && pattern.get(pattern.size() - 1).isBlank()) {
            pattern.remove(pattern.size() - 1);
        }
        if (pattern.isEmpty()) return null;

        // GT6 keeps its extra keys in the table (they are part of the original registration), but the
        // recipe JSON may only carry the symbols the pattern spells - see the class javadoc.
        Set<Character> used = usedSymbols(pattern);
        Map<Character, String> usedKeys = new TreeMap<>();
        for (var entry : row.keys().entrySet()) {
            if (used.contains(entry.getKey())) usedKeys.put(entry.getKey(), entry.getValue());
        }

        GTMaterial material = GTMaterialRegistry.get(DEFAULT_MATERIAL);
        Map<Character, Object> ingredients = MachineRecipeIngredients.resolveAll(usedKeys, material,
                SENSOR_TIER);
        if (ingredients.isEmpty()) return null;
        Map<String, Object> key = new HashMap<>();
        for (var pair : ingredients.entrySet()) {
            key.put(String.valueOf(pair.getKey()), pair.getValue());
        }

        ResourceLocation resultId = Mc.itemId(resultItem);
        if (resultId == null) return null;

        Map<String, Object> recipe = new HashMap<>();
        recipe.put("type", "gt6ce:tool_shaped");
        recipe.put("pattern", pattern);
        recipe.put("key", key);
        recipe.put("allow_mirror", false);
//? if >=1.21.1 {
        /*recipe.put("result", Map.of("id", resultId.toString()));
*///?} else {
        recipe.put("result", Map.of("item", resultId.toString()));
//?}
        recipe.put("group", "gregtech_sensors");
        return recipe;
    }

}
