package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.SpikeBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import com.gregtech.gregtech.registry.GTSpikes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class SpikeConversionAuditTests {
    /** Existing native pick-block producer; no new item class, fixture recipe or arbitrary NBT. */
    private static ItemStack variant(GameTestHelper h, SpikeBlock block, SpikeBlock.Mode mode, boolean secondary) {
        var state = block.defaultBlockState().setValue(SpikeBlock.MODE, mode).setValue(SpikeBlock.SECONDARY, secondary);
        var pos = h.absolutePos(new net.minecraft.core.BlockPos(4, 20, 4));
//? if >=1.21.1 {
        /*return block.getCloneItemStack(state, new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false), h.getLevel(), pos, null);
*///?} else {
        return block.getCloneItemStack(h.getLevel(), pos, state);
//?}
    }

    private static CraftingRecipe row(GameTestHelper h, String path) {
        var id = ResourceLocation.fromNamespaceAndPath("gt6ce", path);
//? if >=1.21.1 {
        /*return (CraftingRecipe) h.getLevel().getRecipeManager().byKey(id).orElseThrow().value();
*///?} else {
        return (CraftingRecipe) h.getLevel().getRecipeManager().byKey(id).orElseThrow();
//?}
    }
//? if >=1.21.1 {
    /*private static CraftingInput grid(List<ItemStack> stacks) { return CraftingInput.of(stacks.size(), 1, stacks); }
    private static CraftingRecipe wire(GameTestHelper h, CraftingRecipe row) {
        var b = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try { ((RecipeSerializer<CraftingRecipe>) row.getSerializer()).streamCodec().encode(b, row); return ((RecipeSerializer<CraftingRecipe>) row.getSerializer()).streamCodec().decode(b); }
        finally { b.release(); }
    }
*///?} else {
    private static net.minecraft.world.inventory.CraftingContainer grid(List<ItemStack> stacks) {
        var menu = new net.minecraft.world.inventory.AbstractContainerMenu(null, 0) {
            public boolean stillValid(net.minecraft.world.entity.player.Player p) { return true; }
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int slot) { return ItemStack.EMPTY; }
        };
        var input = new net.minecraft.world.inventory.TransientCraftingContainer(menu, stacks.size(), 1);
        for (int i = 0; i < stacks.size(); i++) input.setItem(i, stacks.get(i));
        return input;
    }
    private static CraftingRecipe wire(GameTestHelper h, CraftingRecipe row) {
        var b = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try { ((RecipeSerializer<CraftingRecipe>) row.getSerializer()).toNetwork(b, row); return ((RecipeSerializer<CraftingRecipe>) row.getSerializer()).fromNetwork(row.getId(), b); }
        finally { b.release(); }
    }
//?}

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void twentyConversionsMatchOnlyTheirSourceVariant(GameTestHelper h) {
        Map<SpikeBlock.Family, List<String>> materials = Map.of(
                SpikeBlock.Family.METAL, List.of("copper", "lead"), SpikeBlock.Family.STEEL, List.of("blue_steel", "red_steel"),
                SpikeBlock.Family.SHARP, List.of("steel", "titanium"), SpikeBlock.Family.FANCY, List.of("gold", "silver"),
                SpikeBlock.Family.SUPER, List.of("tungstensteel", "adamantium"));
        int pairs = 0, matches = 0, rows = 0;
        for (var registered : GTSpikes.all()) {
            var block = registered.get();
            for (int secondary = 0; secondary < 2; secondary++) {
                for (var to : List.of(SpikeBlock.Mode.OMNI, SpikeBlock.Mode.FALLING)) {
                    String path = "catalog_repairs/spike_" + block.family().name().toLowerCase(Locale.ROOT) + "_"
                            + materials.get(block.family()).get(secondary) + "_convert_to_" + to.getSerializedName();
                    var recipe = row(h, path);
                    h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer())
                            .equals(ResourceLocation.fromNamespaceAndPath("gt6ce", "spike_conversion")), "dedicated serializer identity");
                    var network = wire(h, recipe); rows++;
                    for (var candidate : GTSpikes.all()) for (boolean material : new boolean[]{false, true})
                        for (var mode : SpikeBlock.Mode.values()) {
                            var stack = variant(h, candidate.get(), mode, material);
                            boolean expected = candidate.get() == block && material == (secondary == 1)
                                    && mode != SpikeBlock.Mode.WALL && mode != to;
                            var input = grid(List.of(stack));
                            h.assertTrue(recipe.matches(input, h.getLevel()) == expected, path + " native match");
                            h.assertTrue(network.matches(input, h.getLevel()) == expected, path + " network match");
                            pairs++; if (expected) matches++;
                        }
                    var from = to == SpikeBlock.Mode.OMNI ? SpikeBlock.Mode.FALLING : SpikeBlock.Mode.OMNI;
                    var inputStack = variant(h, block, from, secondary == 1); inputStack.setCount(64);
                    var input = grid(List.of(inputStack));
                    h.assertTrue(recipe.matches(input, h.getLevel()), "one occupied stack64 matches");
                    var result = recipe.assemble(input, h.getLevel().registryAccess());
                    h.assertTrue(result.getCount() == 1 && result.is(block.asItem())
                            && SpikeBlock.itemMode(result) == to && SpikeBlock.itemSecondary(result) == (secondary == 1), "exact output state");
                    h.assertTrue(inputStack.getCount() == 64, "assembly does not mutate input; crafting slot owns consumption");
                    h.assertTrue(!recipe.matches(grid(List.of(inputStack.copy(), inputStack.copy())), h.getLevel()), "extra occupied slot rejected");
                    h.assertTrue(!recipe.matches(grid(List.of(new ItemStack(block))), h.getLevel()), "bare wall fallback rejected");
                }
            }
        }
        h.assertTrue(rows == 20 && pairs == 600 && matches == 20, "20 source conversions × 30 item identities");
        h.succeed();
    }
}
