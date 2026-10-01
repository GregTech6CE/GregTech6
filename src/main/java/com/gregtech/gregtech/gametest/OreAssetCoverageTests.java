package com.gregtech.gregtech.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.OreBlock;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Every registered {@link OreBlock} must ship the three JSON files the client's model loader reads:
 * a blockstate, a block model and an item model.
 *
 * <p>{@code ModelBakery} resolves a block's models from its blockstate
 * ({@code ModelBakery#loadBlockState}, and the state's model location as
 * {@code BlockModelShaper#stateToModelLocation}); a blockstate it cannot find — or one whose model it
 * cannot resolve — logs {@code "Exception loading blockstate definition: '<id>' missing model for
 * variant: '<id>#'"} for <em>every state</em> of that block.  The port registers one ore block per
 * material and prefix ({@code ore_*} and {@code ore_small_*}, one state per host rock —
 * {@link OreBlockStateTests} counts them), so a material without assets costs two warnings per run.
 * The runtime replacement ({@code client/OreClientModels}) only re-points the models that exist: the
 * failed lookups happen first and are logged either way.
 *
 * <p>{@code tools/generate_ore_block_assets.py} writes these files.  This test is the gate: it walks
 * the registry instead of the generator's input, so a material the generator's selection rules miss
 * shows up here as a missing asset rather than as a client log line.  It touches no world and needs no
 * test structure.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class OreAssetCoverageTests {

    private static JsonObject resourceJson(String path) throws Exception {
        try (InputStream stream = OreAssetCoverageTests.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("missing resource " + path);
            return GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    private static boolean resourceExists(String path) {
        return OreAssetCoverageTests.class.getClassLoader().getResource(path) != null;
    }

    /** {@code gregtech:block/ores/ore_iron} -> {@code assets/gregtech/models/block/ores/ore_iron.json}. */
    private static String modelResource(String reference) {
        int split = reference.indexOf(':');
        String namespace = split < 0 ? "minecraft" : reference.substring(0, split);
        String path = split < 0 ? reference : reference.substring(split + 1);
        return "assets/" + namespace + "/models/" + path + ".json";
    }

    /** The parent of a model, or {@code null} when the model declares none (a texture-root model). */
    private static String parentOf(JsonObject model) {
        JsonElement parent = model.get("parent");
        return parent == null || parent.isJsonNull() ? null : parent.getAsString();
    }

    /** The model of the default variant, or {@code null} when the blockstate declares none. */
    private static String defaultVariantModel(JsonObject blockstate) {
        JsonElement variants = blockstate.get("variants");
        if (variants == null || !variants.isJsonObject()) {
            return null;
        }
        JsonElement variant = variants.getAsJsonObject().get("");
        if (variant == null || !variant.isJsonObject()) {
            return null;
        }
        JsonElement model = variant.getAsJsonObject().get("model");
        return model == null || model.isJsonNull() ? null : model.getAsString();
    }

    /**
     * Every ore block resolves its blockstate, its block model and its item model, and the references
     * inside those files resolve too.  The failure message carries the number of blocks that failed, so
     * a client run that reports the same count can be traced back to this list.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void everyOreBlockShipsItsAssets(GameTestHelper helper) throws Exception {
        List<String> problems = new ArrayList<>();
        int blocks = 0;
        int small = 0;
        for (Block block : ForgeRegistries.BLOCKS) {
            if (!(block instanceof OreBlock ore)) {
                continue;
            }
            blocks++;
            if (ore.isSmall()) {
                small++;
            }
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            if (id == null || !id.getNamespace().equals(GregTech.MODID)) {
                problems.add(block + ": registered under " + id + ", not " + GregTech.MODID);
                continue;
            }
            // The asset path is the registry path: BlockMaterialPrefix#getBlockId is
            // `<prefix registry name>_<material name, lowercased>`.
            String path = id.getPath();
            String blockstatePath = "assets/gregtech/blockstates/" + path + ".json";
            String modelPath = "assets/gregtech/models/block/ores/" + path + ".json";
            String itemPath = "assets/gregtech/models/item/" + path + ".json";
            if (!resourceExists(blockstatePath)) {
                problems.add(path + ": missing " + blockstatePath);
                continue;
            }
            if (!resourceExists(modelPath)) {
                problems.add(path + ": missing " + modelPath);
            }
            if (!resourceExists(itemPath)) {
                problems.add(path + ": missing " + itemPath);
                continue;
            }
            // A file that exists but points at a file that does not is the second half of the same
            // ModelBakery warning ("Unable to load model: ... referenced from: ..."), so the references
            // are followed as well.
            String referenced = defaultVariantModel(resourceJson(blockstatePath));
            if (referenced == null) {
                problems.add(path + ": blockstate has no default variant model");
            } else if (referenced.startsWith(GregTech.MODID + ":") && !resourceExists(modelResource(referenced))) {
                problems.add(path + ": blockstate points at the missing model " + referenced);
            }
            String parent = parentOf(resourceJson(itemPath));
            if (parent == null) {
                problems.add(path + ": item model has no parent to render from");
            } else if (parent.startsWith(GregTech.MODID + ":") && !resourceExists(modelResource(parent))) {
                problems.add(path + ": item model points at the missing model " + parent);
            }
        }
        GregTech.LOGGER.info("[gametest] ore assets: checked {} ore blocks ({} small), missing assets {}",
                blocks, small, problems.size());
        // The port registers the ores (the gate's §103.B test counts 970 blocks); this test is about
        // their assets, so it only guards against the registry being empty.
        helper.assertTrue(blocks > 400, "the port registers the ore blocks, got " + blocks);
        helper.assertTrue(problems.isEmpty(), "ore blocks with missing assets (" + problems.size()
                + "): " + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }
}
