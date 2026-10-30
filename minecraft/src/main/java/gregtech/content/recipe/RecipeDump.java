package gregtech.content.recipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.content.block.energy.SignalWireBlock;
import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.block.machine.ItemPipeBlock;
import gregtech.content.registry.GTFluids;
import gregtech.engine.machine.ItemPipeSpec;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.slf4j.Logger;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Development-only dump of every registered {@link RecipeMap}, written in the same shape as the
 * original GT6 recipe dump so a local comparator can diff this port against it.
 *
 * <p>Disabled unless {@code -Dgt6ce.recipeDump=<dir>} or the {@code GT6CE_RECIPE_DUMP=<dir>}
 * environment variable names an output directory; the environment form is what a Gradle run task
 * forwards to the forked game.</p>
 *
 * <p>Captures the static tables built by {@link MachineRecipeBootstrap}; addon recipes and the
 * datapack overlay are registered afterwards and are not included.</p>
 *
 * <p>Material items are written as the original's meta-item identity
 * ({@code gregtech:gt.meta.<prefix>} + the GT6 material id) instead of the port's one-item-per-pair
 * registry names, because that identity is what both sides can agree on. Material blocks (storage,
 * machine casings, crates, wires and pipes) carry the same identity from their prefix/spec, so they
 * are not left to display-name comparison. Fluid names use the catalog's original name
 * ({@code molten.cobalt}) rather than the sanitised registry path.
 * Stack data (NBT / data components) is not serialised on either side, so rows that differ only by
 * stack data compare as equal.</p>
 */
public final class RecipeDump {

    public static final String PROPERTY = "gt6ce.recipeDump";
    public static final String ENVIRONMENT = "GT6CE_RECIPE_DUMP";
    public static final String OVERLAY_PROPERTY = "gt6ce.recipeDumpOverlay";
    public static final String OVERLAY_ENVIRONMENT = "GT6CE_RECIPE_DUMP_OVERLAY";
    /** Optional comma-separated {@code nameInternal} filter; empty writes every map. */
    public static final String MAPS_PROPERTY = "gt6ce.recipeDumpMaps";
    public static final String MAPS_ENVIRONMENT = "GT6CE_RECIPE_DUMP_MAPS";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private RecipeDump() {}

    /** Writes the dump when a target directory was requested; never throws into recipe registration. */
    public static void afterRegistration() {
        dumpIfRequested(PROPERTY, ENVIRONMENT);
    }

    /**
     * Second snapshot after {@code MachineRecipeOverlay.apply}, which adds the tag/overlay producers
     * (thermal recycling, ore washing and the other rebuilds) that the static snapshot cannot see.
     */
    public static void afterOverlay() {
        dumpIfRequested(OVERLAY_PROPERTY, OVERLAY_ENVIRONMENT);
    }

    private static void dumpIfRequested(String property, String environment) {
        String target = System.getProperty(property);
        if (target == null || target.isBlank()) target = System.getenv(environment);
        if (target == null || target.isBlank()) return;
        try {
            write(Paths.get(target));
        } catch (RuntimeException | IOException failure) {
            LOGGER.error("[gregtech] Recipe dump to {} failed", target, failure);
        }
    }

    private static java.util.Set<String> mapFilter() {
        String value = System.getProperty(MAPS_PROPERTY);
        if (value == null || value.isBlank()) value = System.getenv(MAPS_ENVIRONMENT);
        if (value == null || value.isBlank()) return java.util.Set.of();
        var filter = new java.util.HashSet<String>();
        for (String name : value.split(",")) if (!name.isBlank()) filter.add(name.trim());
        return filter;
    }

    private static void write(Path directory) throws IOException {
        long started = System.nanoTime();
        Path maps = directory.resolve("maps");
        Files.createDirectories(maps);
        JsonObject index = new JsonObject();
        JsonArray indexMaps = new JsonArray();
        var filter = mapFilter();
        int total = 0;
        int written = 0;
        for (RecipeMap map : RecipeMap.RECIPE_MAP_LIST) {
            if (map.mNameInternal.isEmpty()) continue;
            if (!filter.isEmpty() && !filter.contains(map.mNameInternal)) continue;
            JsonObject json = mapJson(map);
            String file = "maps/" + fileName(map.mNameInternal) + ".json";
            Files.writeString(maps.resolve(fileName(map.mNameInternal) + ".json"),
                    GSON.toJson(json), StandardCharsets.UTF_8);
            JsonObject entry = new JsonObject();
            entry.addProperty("nameInternal", map.mNameInternal);
            entry.addProperty("file", file);
            entry.addProperty("recipeCount", json.get("recipeCount").getAsInt());
            indexMaps.add(entry);
            total += json.get("recipeCount").getAsInt();
            written++;
        }
        index.addProperty("generatedBy", "gt6ce RecipeDump");
        index.addProperty("mapCount", written);
        index.addProperty("recipeCount", total);
        index.addProperty("elapsedMs", (System.nanoTime() - started) / 1_000_000L);
        index.add("maps", indexMaps);
        Files.writeString(directory.resolve("index.json"), GSON.toJson(index), StandardCharsets.UTF_8);
        LOGGER.info("[gregtech] Recipe dump: {} maps, {} rows, {} ms, written to {}",
                written, total, (System.nanoTime() - started) / 1_000_000L, directory.toAbsolutePath());
    }

    private static JsonObject mapJson(RecipeMap map) {
        JsonObject json = new JsonObject();
        json.addProperty("nameInternal", map.mNameInternal);
        json.addProperty("nameLocal", map.mNameLocal);
        json.addProperty("guiPath", map.mGUIPath);
        json.addProperty("power", map.mPower);
        json.addProperty("inputItemsCount", map.mInputItemsCount);
        json.addProperty("outputItemsCount", map.mOutputItemsCount);
        json.addProperty("inputFluidCount", map.mInputFluidCount);
        json.addProperty("outputFluidCount", map.mOutputFluidCount);
        json.addProperty("minimalInputItems", map.mMinimalInputItems);
        json.addProperty("minimalInputFluids", map.mMinimalInputFluids);
        json.addProperty("minimalInputs", map.mMinimalInputs);
        json.addProperty("neiAllowed", map.mViewerAllowed);
        json.addProperty("needsOutputs", map.mNeedsOutputs);
        json.addProperty("combinePower", map.mCombinePower);
        json.addProperty("useBucketSizeIn", map.mUseBucketSizeIn);
        json.addProperty("useBucketSizeOut", map.mUseBucketSizeOut);
        json.addProperty("maxFluidInputSize", map.mMaxFluidInputSize);
        json.addProperty("maxFluidOutputSize", map.mMaxFluidOutputSize);
        JsonArray rows = new JsonArray();
        int count = 0;
        for (Recipe recipe : map.allRecipes()) {
            rows.add(recipeJson(recipe));
            count++;
        }
        json.add("recipes", rows);
        json.addProperty("recipeCount", count);
        return json;
    }

    private static JsonObject recipeJson(Recipe recipe) {
        JsonObject json = new JsonObject();
        json.addProperty("euPerTick", recipe.mEUt);
        json.addProperty("duration", recipe.mDuration);
        json.addProperty("specialValue", recipe.mSpecialValue);
        json.addProperty("enabled", recipe.mEnabled);
        json.addProperty("hidden", recipe.mHidden);
        json.addProperty("fake", recipe.mFakeRecipe);
        json.addProperty("canBeBuffered", recipe.mCanBeBuffered);
        json.addProperty("needsEmptyOutput", recipe.mNeedsEmptyOutput);
        json.add("inputs", itemArray(recipe.mInputs));
        json.add("outputs", itemArray(recipe.mOutputs));
        json.add("fluidInputs", fluidArray(recipe.mFluidInputs));
        json.add("fluidOutputs", fluidArray(recipe.mFluidOutputs));
        json.add("chances", longArray(recipe.mChances));
        json.add("maxChances", longArray(recipe.mMaxChances));
        return json;
    }

    private static JsonArray itemArray(ItemStack[] stacks) {
        JsonArray array = new JsonArray();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            array.add(itemJson(stack));
        }
        return array;
    }

    private static JsonObject itemJson(ItemStack stack) {
        JsonObject json = new JsonObject();
        Item item = stack.getItem();
        String[] form = null;
        if (item instanceof MaterialFormItem formItem) {
            json.addProperty("item", "gregtech:gt.meta." + formItem.getPrefix().getName());
            json.addProperty("meta", formItem.getMaterial().getId());
        } else if (item instanceof BlockItem blockItem && (form = blockForm(blockItem.getBlock())) != null) {
            json.addProperty("item", "gregtech:gt.meta." + form[0]);
            json.addProperty("meta", Integer.parseInt(form[1]));
        } else {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            json.addProperty("item", id.toString());
            json.addProperty("meta", 0);
        }
        json.addProperty("count", stack.getCount());
        json.addProperty("displayName", stack.getHoverName().getString());

//? if >=1.21.1 {
/*        if (!stack.getComponentsPatch().isEmpty()) json.addProperty("components", true);
*///?} else {
        if (stack.hasTag()) json.addProperty("nbt", true);
//?}
        return json;
    }

    /**
     * GT6 form identity of a material block: the OP prefix name and material id, which is the same
     * {@code gt.meta.<prefix>#<id>} identity the material items use. GT6 keeps some of these forms in
     * material meta items and others in multi-tile metas; the local comparator maps the original
     * side onto the same prefix names through its generated multi-tile identity table.
     */
    private static String[] blockForm(Block block) {
        if (block instanceof MaterialBlockLike material) {
            return new String[] {material.prefix().getName(), Integer.toString(material.material().getId())};
        }
        if (block instanceof ElectricWireBlock wire) {
            var spec = wire.spec();
            return new String[] {
                (spec.insulated() ? "cableGt" : "wireGt") + String.format("%02d", spec.size()),
                Integer.toString(spec.material().getId())
            };
        }
        if (block instanceof SignalWireBlock signal) {
            return new String[] {
                signal.insulated() ? "cableGt01" : "wireGt01",
                Integer.toString(signal.material().getId())
            };
        }
        if (block instanceof FluidPipeBlock pipe) {
            var spec = pipe.spec();
            String size = spec.size().name();
            return new String[] {
                "pipe" + size.charAt(0) + size.substring(1).toLowerCase(Locale.ROOT),
                Integer.toString(spec.material().getId())
            };
        }
        if (block instanceof ItemPipeBlock pipe) {
            var spec = pipe.spec();
            StringBuilder name = new StringBuilder("ItemPipe");
            for (String part : spec.size().name().split("_")) {
                name.append(part.charAt(0)).append(part.substring(1).toLowerCase(Locale.ROOT));
            }
            return new String[] {name.toString(), Integer.toString(spec.material().getId())};
        }
        return null;
    }

    private static JsonArray fluidArray(FluidStack[] stacks) {
        JsonArray array = new JsonArray();
        for (FluidStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            array.add(fluidJson(stack));
        }
        return array;
    }

    private static JsonObject fluidJson(FluidStack stack) {
        JsonObject json = new JsonObject();
        Fluid fluid = stack.getFluid();
        var entry = GTFluids.entryForFluid(fluid);
        String name = entry != null ? entry.registryName() : BuiltInRegistries.FLUID.getKey(fluid).toString();
        json.addProperty("fluid", name);
        json.addProperty("amount", stack.getAmount());
        json.addProperty("displayName", stack.getDisplayName().getString());
        return json;
    }

    private static JsonArray longArray(long[] values) {
        JsonArray array = new JsonArray();
        if (values != null) for (long value : values) array.add(value);
        return array;
    }

    private static String fileName(String mapName) {
        return mapName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }
}
