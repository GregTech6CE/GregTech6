package gregtech.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import gregtech.content.data.ManualToolRecipePack;
import gregtech.content.data.OriginalCraftingJson;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class CraftingOutputComponentTests {
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void shapedAndShapelessPreserveComponents(GameTestHelper helper) {
        var output = new ItemStack(Items.COPPER_INGOT, 3);
        var data = new CompoundTag();
        data.putInt("addon:quality", 42);
        data.putLong("addon:timestamp", 42L);
        data.putIntArray("addon:values", new int[] {1, 2, 3});
        output.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        output.set(DataComponents.CUSTOM_NAME, Component.literal("Component output"));
        output.enchant(
                helper.getLevel()
                        .registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.UNBREAKING),
                2);
        if (net.neoforged.fml.ModList.get().isLoaded("tfc")) {
            helper.assertTrue(
                    output.getComponentsPatch().entrySet().stream()
                            .anyMatch(entry -> ResourceLocation.parse("tfc:heat")
                                    .equals(net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(
                                            entry.getKey()))),
                    "TFC attached heat after reload");
        }
        var rows = new LinkedHashMap<ResourceLocation, byte[]>();
        OriginalCraftingJson.shaped(
                rows,
                id("shaped"),
                "",
                CraftingBookCategory.MISC,
                output,
                new String[] {"I"},
                Map.of('I', Ingredient.of(Items.IRON_INGOT)),
                false);
        OriginalCraftingJson.shapeless(
                rows,
                id("shapeless"),
                "",
                CraftingBookCategory.MISC,
                output,
                List.of(Ingredient.of(Items.IRON_INGOT)),
                false);
        for (var row : rows.values()) {
            var json = JsonParser.parseString(new String(row, StandardCharsets.UTF_8));
            var recipe = Recipe.CODEC
                    .parse(helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE), json)
                    .getOrThrow();
            var decoded = recipe.getResultItem(helper.getLevel().registryAccess());
            helper.assertTrue(
                    decoded.getCount() == 3 && ItemStack.isSameItemSameComponents(output, decoded),
                    "recipe output preserves count and components");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void manualPackRegeneratesAfterDataReload(GameTestHelper helper) {
        var first = new LinkedHashMap<ResourceLocation, byte[]>();
        var second = new LinkedHashMap<ResourceLocation, byte[]>();
        ManualToolRecipePack.contribute(first);
        ManualToolRecipePack.contribute(second);
        helper.assertTrue(
                !first.isEmpty() && first.keySet().equals(second.keySet()), "stable recipe IDs on regeneration");
        helper.assertTrue(
                first.keySet().stream().anyMatch(id -> id.getPath().contains("form_conversion/")),
                "material conversions present after regeneration");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void invalidOutputReportsRecipeIdentity(GameTestHelper helper) {
        try {
            OriginalCraftingJson.result(id("invalid_output"), ItemStack.EMPTY);
            helper.fail("empty output accepted");
        } catch (IllegalArgumentException expected) {
            helper.assertTrue(expected.getMessage().contains("component_test/invalid_output"), "error names recipe");
        }
        helper.succeed();
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "component_test/" + name);
    }
}
