package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.inventory.MassStorageMaterialForms;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity;
import com.gregtech.gregtech.item.behavior.BehaviorDuctTape;
import com.gregtech.gregtech.item.behavior.ItemBehaviors;
import com.gregtech.gregtech.registry.GTStorage;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Harvesting must preserve bulk contents without duplicating taped fractional material. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class MassStorageHarvestTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private MassStorageHarvestTests() {}

    private static MassStorageBlockEntity storage(GameTestHelper helper) {
        helper.setBlock(POS, GTStorage.MASS_STORAGE.get());
        return (MassStorageBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(POS));
    }

    private static List<ItemStack> drops(GameTestHelper helper, MassStorageBlockEntity storage) {
        return helper.getLevel().getBlockState(storage.getBlockPos()).getDrops(
                new LootParams.Builder(helper.getLevel())
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(storage.getBlockPos()))
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, storage));
    }

    private static long materialUnits(ItemStack template, List<ItemStack> drops) {
        long units = 0;
        for (ItemStack drop : drops) {
            if (drop.getItem() == GTStorage.MASS_STORAGE.get().asItem()) continue;
            if (ItemStack.isSameItemSameTags(template, drop)) {
                units += GTValues.U * drop.getCount();
            } else {
                units += MassStorageMaterialForms.compatibleUnits(template, drop) * drop.getCount();
            }
        }
        return units;
    }

    @GameTest(template = "test_empty")
    public static void untapedStorageDropsShellWholeItemsAndPartialMaterial(GameTestHelper helper) {
        MassStorageBlockEntity storage = storage(helper);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        helper.assertTrue(storage.insert(new ItemStack(Items.IRON_INGOT, 64)) == 64
                        && storage.insert(new ItemStack(Items.IRON_INGOT, 64)) == 64
                        && storage.insert(new ItemStack(Items.IRON_INGOT, 2)) == 2
                        && storage.insert(new ItemStack(Items.IRON_NUGGET, 8)) == 8,
                "ordinary storage accepts 130 whole ingots and eight ninths of an ingot");
        List<ItemStack> drops = drops(helper, storage);
        ItemStack shell = drops.stream().filter(stack -> stack.is(GTStorage.MASS_STORAGE.get().asItem()))
                .findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(!shell.isEmpty() && shell.getTagElement("BlockEntityTag") == null,
                "untaped shell must not smuggle the same contents in BlockEntityTag");
        helper.assertTrue(materialUnits(iron, drops) == 130 * GTValues.U + 8 * GTValues.U9,
                "untaped harvest returns all whole and representable fractional iron exactly once");
        helper.assertTrue(drops.stream().allMatch(stack -> stack.getCount() <= stack.getMaxStackSize()),
                "harvested contents split into legal item stack sizes");
        BlockPos pos = storage.getBlockPos();
        helper.assertTrue(helper.getLevel().destroyBlock(pos, true),
                "ordinary world break invokes the Mass Storage loot path");
        List<ItemStack> inWorld = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(pos).inflate(2)).stream().map(entity -> entity.getItem().copy()).toList();
        helper.assertTrue(materialUnits(iron, inWorld) == 130 * GTValues.U + 8 * GTValues.U9
                        && inWorld.stream().filter(stack -> stack.is(GTStorage.MASS_STORAGE.get().asItem()))
                            .mapToInt(ItemStack::getCount).sum() == 1,
                "actual break spawns one shell and each stored material unit exactly once");
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(pos).inflate(2))) entity.discard();
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void ductTapePacksWholeItemsButStillDropsFraction(GameTestHelper helper) {
        MassStorageBlockEntity storage = storage(helper);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        helper.assertTrue(storage.insert(new ItemStack(Items.IRON_INGOT, 24)) == 24
                        && storage.insert(new ItemStack(Items.IRON_NUGGET, 8)) == 8,
                "prepared storage contains whole and fractional iron");

        var player = helper.makeMockPlayer();
        ItemStack tape = ItemBehaviors.stack(BehaviorDuctTape.DUCT_TAPE_FULL_ITEM);
        var outcome = BehaviorDuctTape.useOn(helper.getLevel(), storage.getBlockPos(),
                Direction.UP, player, tape, 0.5F, 0.5F, 0.5F);
        helper.assertTrue(outcome.acted() && storage.isPacked()
                        && outcome.stack().getTag() != null
                        && outcome.stack().getTag().getLong("gt.remaining")
                            == BehaviorDuctTape.DUCT_TAPE_USES - 100,
                "real duct tape click seals the storage and consumes GT6's minimum 100 uses");

        var handler = storage.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                .resolve().orElseThrow();
        helper.assertTrue(storage.insert(iron) == 0 && storage.extractAmount(1).isEmpty()
                        && !handler.isItemValid(0, iron),
                "sealed storage refuses both manual and capability item transfer");

        List<ItemStack> drops = drops(helper, storage);
        ItemStack packed = drops.stream().filter(stack -> stack.is(GTStorage.MASS_STORAGE.get().asItem()))
                .findFirst().orElse(ItemStack.EMPTY);
        CompoundTag tag = packed.getTagElement("BlockEntityTag");
        helper.assertTrue(tag != null && tag.getLong("gt.stored") == 24
                        && (tag.getInt("gt.mode") & 8) != 0
                        && tag.getLong("gt.partial_units") == 0,
                "taped block item contains whole items but not separately dropped fractional units");
        helper.assertTrue(materialUnits(iron, drops) == 8 * GTValues.U9,
                "taped harvest spills only the fractional iron once");

        helper.setBlock(POS, Blocks.AIR);
        MassStorageBlockEntity restored = storage(helper);
        restored.load(tag);
        helper.assertTrue(restored.isPacked() && restored.stored() == 24
                        && restored.partialUnits() == 0,
                "packed item NBT restores whole contents without fractional duplication");
        ItemStack scissors = GTToolItems.empty(GTToolType.SCISSORS);
        player.setItemInHand(InteractionHand.MAIN_HAND, scissors);
        var use = restored.getBlockState().use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(restored.getBlockPos()), Direction.NORTH,
                        restored.getBlockPos(), false));
        helper.assertTrue(!scissors.isEmpty() && use.consumesAction() && !restored.isPacked()
                        && restored.extractAmount(24).getCount() == 24,
                "scissors interaction removes the tape and restores access");
        helper.succeed();
    }
}
