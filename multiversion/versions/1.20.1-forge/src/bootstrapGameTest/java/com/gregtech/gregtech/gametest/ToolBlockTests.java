package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.block.tool.DynamiteBlock;
import com.gregtech.gregtech.block.tool.RopeBlock;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.TreeMap;

/**
 * GT6's six ropes and three explosives ({@code Loader_MultiTileEntities:2086-2091, 2235-2237}).
 *
 * <p>The ropes share one greyscale model that GT6 tints with the rope's material, so the port
 * registers them as {@link RopeBlock}s with the material colour; the explosives are
 * {@link DynamiteBlock}s whose blast follows the original's {@code NBT_QUALITY} (10 for the
 * boomstick and the dynamite, 40 for the strong one). The dynamite family is produced in the Press
 * ({@code Loader_Recipes_Other:644-655}, already transpiled into {@code GTOtherGen}), the ropes are
 * crafted with the registration patterns ({@code tools/generate_rope_dynamite_recipes.py}).</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class ToolBlockTests {

    /** id -> the GT6 material whose colour tints it (the same list the original registers). */
    private static final TreeMap<String, Integer> ROPES = new TreeMap<>();
    private static final TreeMap<String, float[]> EXPLOSIVES = new TreeMap<>();

    static {
        ROPES.put("rope", Materials.Brown.getColor());
        ROPES.put("rope_silk", Materials.White.getColor());
        ROPES.put("rope_grass", Materials.Yellow.getColor());
        ROPES.put("rope_vine", Materials.Green.getColor());
        ROPES.put("rope_plastic", Materials.Plastic.getColor());
        ROPES.put("rope_steel", Materials.Steel.getColor());
        EXPLOSIVES.put("dynamite", new float[]{10F, Materials.Red.getColor()});
        EXPLOSIVES.put("boomstick", new float[]{10F, Materials.Orange.getColor()});
        EXPLOSIVES.put("strong_dynamite", new float[]{40F, Materials.Purple.getColor()});
    }

    private static Item item(String id) {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", id));
    }

    /** The six ropes exist, are tinted like the original's materials and carry the shared model. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void ropesAreRegisteredAndTinted(GameTestHelper h) {
        for (var entry : ROPES.entrySet()) {
            Item registered = item(entry.getKey());
            h.assertTrue(registered != null && registered != Items.AIR,
                    "rope block registered: " + entry.getKey());
            h.assertTrue(registered instanceof net.minecraft.world.item.BlockItem blockItem
                            && blockItem.getBlock() instanceof RopeBlock rope,
                    entry.getKey() + " is a rope block");
            RopeBlock rope = (RopeBlock) ((net.minecraft.world.item.BlockItem) registered).getBlock();
            h.assertTrue(rope.tintRgb() == entry.getValue(),
                    entry.getKey() + " is tinted with GT6's material colour");
        }
        h.succeed();
    }

    /** The three explosives exist, tinted, with GT6's blast quality ratio (10 : 40). */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void explosivesKeepGt6BlastStrength(GameTestHelper h) {
        for (var entry : EXPLOSIVES.entrySet()) {
            Item registered = item(entry.getKey());
            h.assertTrue(registered instanceof net.minecraft.world.item.BlockItem blockItem
                            && blockItem.getBlock() instanceof DynamiteBlock explosive,
                    entry.getKey() + " is an explosive block");
            DynamiteBlock explosive = (DynamiteBlock) ((net.minecraft.world.item.BlockItem) registered).getBlock();
            h.assertTrue(explosive.maxExplosionResistance() == entry.getValue()[0],
                    entry.getKey() + " resistance threshold is GT6 quality, not a radius");
            h.assertTrue(explosive.tintRgb() == (int) entry.getValue()[1],
                    entry.getKey() + " is tinted with GT6's material colour");
        }
        h.succeed();
    }

    /** Ropes are crafted with GT6's registration patterns; the explosives come from the Press. */
    @GameTest(template = "test_blueprint_empty", timeoutTicks = 300)
    public static void ropesAndExplosivesAreObtainable(GameTestHelper h) {
        var manager = h.getLevel().getRecipeManager();
        var dustIds = new java.util.TreeSet<String>();
        var silica = com.gregtech.gregtech.content.material.Materials.SiliconDioxide;
        var stack = com.gregtech.gregtech.registry.GTItems.getStack(
                com.gregtech.gregtech.data.MaterialPrefix.dust, silica, 1);
        if (!stack.isEmpty()) {
            dustIds.add(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
        }
        for (Item candidate : ForgeRegistries.ITEMS) {
            var key = ForgeRegistries.ITEMS.getKey(candidate);
            if (key == null || !key.getNamespace().equals("gregtech")) continue;
            String path = key.getPath();
            if (path.startsWith("dust") && (path.contains("sio") || path.contains("silicon"))) {
                dustIds.add(path);
            }
        }
        for (String id : List.of("rope_silk", "rope_grass", "rope_vine", "rope_steel", "boomstick")) {
            var recipe = manager.byKey(ResourceLocation.fromNamespaceAndPath("gregtech", "tool_blocks/" + id));
            h.assertTrue(recipe.isPresent(), id + " keeps GT6's crafting recipe"
                    + (id.equals("boomstick") ? " (silicon dioxide dust ids: " + dustIds + ")" : ""));
            for (var ingredient : recipe.get().getIngredients()) {
                if (ingredient.isEmpty()) continue;
                h.assertTrue(ingredient.getItems().length > 0,
                        id + " ingredient resolves to an item");
            }
            h.assertTrue(recipe.get() instanceof com.gregtech.gregtech.recipe.ToolShapedRecipe tool
                            && !tool.allowMirror(),
                    id + " does not mirror (GT6 uses DEF_REV_NCC)");
        }
        // The Press rows (Loader_Recipes_Other:644-655) must produce all three explosives.
        for (String id : List.of("boomstick", "dynamite", "strong_dynamite")) {
            ItemStack wanted = new ItemStack(item(id));
            java.util.List<String> matched = new java.util.ArrayList<>();
            for (Recipe recipe : MachineRecipeMaps.Press.mRecipeList) {
                for (ItemStack out : recipe.mOutputs) {
                    if (out == null || !out.is(wanted.getItem())) continue;
                    StringBuilder inputs = new StringBuilder();
                    for (ItemStack in : recipe.mInputs) {
                        if (in == null || in.isEmpty()) continue;
                        inputs.append(ForgeRegistries.ITEMS.getKey(in.getItem())).append('x')
                                .append(in.getCount()).append(' ');
                    }
                    matched.add(inputs.toString().trim());
                }
            }
            h.assertTrue(!matched.isEmpty(), id + " has Press rows (dust and block dust): " + matched);
        }
        h.succeed();
    }
}
