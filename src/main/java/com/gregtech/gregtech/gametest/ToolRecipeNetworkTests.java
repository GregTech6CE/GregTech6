package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.recipe.*;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Exercises the registered serializers, including consecutive rows in a shared packet. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class ToolRecipeNetworkTests {
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Recipe<?> roundTrip(Recipe<?> recipe, GameTestHelper helper) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            RecipeSerializer serializer = recipe.getSerializer();
            serializer.toNetwork(buffer, recipe);
            int end = buffer.writerIndex();
            buffer.writeInt(0x13579bdf);
            Recipe<?> copy = (Recipe<?>) serializer.fromNetwork(recipe.getId(), buffer);
            helper.assertTrue(buffer.readerIndex() == end, "serializer consumed exactly its row: " + recipe.getId());
            helper.assertTrue(buffer.readInt() == 0x13579bdf && !buffer.isReadable(), "next packet row preserved");
            helper.assertTrue(copy.getClass() == recipe.getClass(), "specialized recipe behavior preserved");
            helper.assertTrue(copy.getId().equals(recipe.getId()), "recipe identity preserved");
            return copy;
        } finally {
            buffer.release();
        }
    }

    @GameTest(template = "test_empty")
    public static void assemblyPacketBoundary(GameTestHelper helper) {
        var original = new GTToolAssemblyRecipe(GregTech.id("test/network_pickaxe"), GTToolType.PICKAXE);
        var copy = (GTToolAssemblyRecipe) roundTrip(original, helper);
        helper.assertTrue(copy.toolType() == original.toolType(), "assembly tool type preserved");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void flintSerializerPreservesSpecialization(GameTestHelper helper) {
        var id = GregTech.id("test/network_flint");
        roundTrip(new GTFlintAndTinderRecipe(id), helper);
        var json = new JsonObject();
        json.addProperty("tool", GTToolType.FLINT_AND_TINDER.id());
        Recipe<?> copy = GTToolRecipeSerializers.CRAFTING.fromJson(id, json);
        helper.assertTrue(copy instanceof GTFlintAndTinderRecipe, "JSON also restores striking-material semantics");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void allToolPatternIndicesSurviveNetwork(GameTestHelper helper) {
        for (var type : GTToolType.values()) {
            for (int index = 0; index < GTToolRecipes.shaped(type).size(); index++) {
                var id = GregTech.id("test/network/" + type.id() + "/" + index);
                GTToolPatternRecipe original = type == GTToolType.FLINT_AND_TINDER
                        ? new GTFlintAndTinderRecipe(id) : new GTToolCraftingRecipe(id, type, index);
                var copy = (GTToolPatternRecipe) roundTrip(original, helper);
                helper.assertTrue(copy.toolType() == type && copy.patternIndex() == index, "shaped pattern preserved");
            }
            for (int index = 0; index < GTToolRecipes.heads(type).size(); index++) {
                var id = GregTech.id("test/network/head/" + type.id() + "/" + index);
                var copy = (GTToolHeadRecipe) roundTrip(new GTToolHeadRecipe(id, type, index), helper);
                helper.assertTrue(copy.toolType() == type && copy.patternIndex() == index, "head pattern preserved");
            }
        }
        helper.succeed();
    }
}
