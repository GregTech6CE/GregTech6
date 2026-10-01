package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.SandwichBlock;
import com.gregtech.gregtech.blockentity.misc.SandwichBlockEntity;
import com.gregtech.gregtech.content.food.SandwichIngredients;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** GT6 MultiTileEntitySandwich's layer, shape, food, signal and drop contract. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class SandwichBlockTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static ItemStack gtFood(String id, int count) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(com.gregtech.gregtech.GregTech.id(id)), count);
    }

    private static SandwichBlockEntity placed(GameTestHelper h) {
        var p = h.absolutePos(POS);
        h.getLevel().setBlock(p.below(), Blocks.STONE.defaultBlockState(), 3);
        h.getLevel().setBlock(p, GTDecorBlocks.SANDWICH_BLOCK.get().defaultBlockState(), 3);
        return (SandwichBlockEntity) h.getLevel().getBlockEntity(p);
    }

    @GameTest(template = "test_empty")
    public static void defaultSandwichAndLayerRules(GameTestHelper h) {
        var sandwich = placed(h);
        h.assertTrue(sandwich.ingredient(0).is(ForgeRegistries.ITEMS.getValue(
                com.gregtech.gregtech.GregTech.id("toast"))), "sample begins with GT toast");
        h.assertTrue(sandwich.ingredient(11).is(ForgeRegistries.ITEMS.getValue(
                com.gregtech.gregtech.GregTech.id("toast"))), "sample has top toast");
        h.assertTrue(sandwich.sizePixels() == 13 && sandwich.comparatorSignal() == 12,
                "GT6 toast thickness makes a 13-pixel sample and comparator value 12");
        h.assertTrue(sandwich.redstoneSignal() == 0, "redstone output starts off");
        h.assertTrue(sandwich.addIngredient(gtFood("carrot_slice", 1)) == 1 && sandwich.sizePixels() == 14,
                "carrot uses GT6 four-slice layer at next pixel");
        h.assertTrue(sandwich.addIngredient(gtFood("carrot_slice", 1)) == 0,
                "same ingredient cannot repeat immediately");
        h.assertTrue(sandwich.addIngredient(new ItemStack(Items.REDSTONE)) == 1
                        && sandwich.redstoneSignal() == 13 && sandwich.sizePixels() == 14,
                "redstone enables signal without occupying a layer");
        h.assertTrue(sandwich.addIngredient(new ItemStack(Items.REDSTONE)) == 0,
                "redstone cannot be applied twice");
        h.assertTrue(SandwichIngredients.of(33).thickness() == 3
                        && SandwichIngredients.of(254).thickness() == 2,
                "GT6 ingredient thickness table is retained");
        h.assertTrue(SandwichIngredients.of(0).tint() == 0x202020
                        && SandwichIngredients.of(15).tint() == 0xFFFFFF
                        && SandwichIngredients.of(200).tint() == 0x202020
                        && SandwichIngredients.of(215).tint() == 0xFFFFFF,
                "condiment and spice tint order follows GT6 DYES, black through white");
        h.assertTrue(SandwichIngredients.forItem(gtFood("butter", 1)).id() == 11
                        && SandwichIngredients.forItem(gtFood("egg_yolk", 1)).id() == 211,
                "yellow GT6 food ingredients use the yellow layer index");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void thinShapeAndIngredientInteraction(GameTestHelper h) {
        var sandwich = placed(h);
        var p = h.absolutePos(POS);
        var state = h.getLevel().getBlockState(p);
        var shape = state.getCollisionShape(h.getLevel(), p);
        h.assertTrue(shape.bounds().minX == 1 / 16.0 && shape.bounds().maxX == 15 / 16.0
                        && shape.bounds().maxY == 13 / 16.0,
                "selection and collision follow the live sandwich height and inset");
        h.assertTrue(state.getBlock().getAnalogOutputSignal(state, h.getLevel(), p) == 12,
                "comparator sees sandwich size independently of redstone coating");

        var player = h.makeMockPlayer();
        player.getAbilities().instabuild = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, gtFood("carrot_slice", 2));
        var result = state.getBlock().use(state, h.getLevel(), p, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(p), Direction.UP, p, false));
        h.assertTrue(result.consumesAction() && player.getMainHandItem().getCount() == 1
                        && sandwich.sizePixels() == 14,
                "right click consumes one recognized ingredient and expands the block");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void preservedItemAndSingleLayerDrop(GameTestHelper h) {
        var sandwich = placed(h);
        var p = h.absolutePos(POS);
        var state = h.getLevel().getBlockState(p);
        var drops = Block.getDrops(state, h.getLevel(), p, sandwich);
        h.assertTrue(drops.size() == 1 && drops.get(0).is(GTDecorBlocks.SANDWICH_BLOCK.get().asItem()),
                "multi-layer sandwich drops its own item");
        var preserved = drops.get(0).getTagElement("BlockEntityTag");
        h.assertTrue(preserved != null && preserved.getList("Ingredients", 10).size() == 10,
                "all ten original ingredient stacks are preserved on the item");
        h.assertTrue(sandwich.consumeDrop().isEmpty(), "other removal path cannot duplicate its drop");
        var restored = new SandwichBlockEntity(p, state);
        restored.load(preserved);
        h.assertTrue(restored.sizePixels() == 13 && restored.layerId(11) == 254,
                "placed sandwich item restores its layer geometry");

        // GT6 returns the sole bare food item, rather than wrapping it in a sandwich block.
        var lone = placed(h);
        CompoundTag one = new CompoundTag();
        ListTag list = new ListTag();
        CompoundTag row = new CompoundTag();
        row.putByte("Slot", (byte) 0);
        row.put("Stack", gtFood("carrot_slice", 2).save(new CompoundTag()));
        list.add(row);
        one.put("Ingredients", list);
        lone.load(one);
        var single = Block.getDrops(state, h.getLevel(), p, lone);
        h.assertTrue(single.size() == 1 && single.get(0).is(gtFood("carrot_slice", 1).getItem())
                        && single.get(0).getCount() == 2,
                "one ingredient drops as that ingredient with its count");
        h.succeed();
    }
}
