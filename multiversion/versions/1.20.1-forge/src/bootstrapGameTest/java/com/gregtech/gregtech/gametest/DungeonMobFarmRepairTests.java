package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.ItemPipeBlock;
import com.gregtech.gregtech.block.misc.SpikeBlock;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomFarmMobs;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;

/** GT6 mob-farm item pipe 25377 and its preloaded mass storage ring. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DungeonMobFarmRepairTests {
    private static BlockPos origin(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(48, 70, 48));
    }

    private static void storage(GameTestHelper helper, BlockPos origin, int x, int y, int z, Item item) {
        var entity = helper.getLevel().getBlockEntity(origin.offset(x, y, z));
        helper.assertTrue(entity instanceof MassStorageBlockEntity, "GT bulk storage at " + x + "," + y + "," + z);
        var storage = (MassStorageBlockEntity) entity;
        helper.assertTrue(storage.template().is(item) && storage.stored() >= 1 && storage.stored() <= 8,
                "GT6 preload and 1..8 quantity at " + x + "," + y + "," + z);
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void mobFarmHasOriginalArsenicBronzePipesAndPreloadedStorage(GameTestHelper helper) {
        BlockPos origin = origin(helper);
        GTDungeonData data = new GTDungeonData(helper.getLevel(), origin.getX(), origin.getY(), origin.getZ(),
                StoneType.LIMESTONE, StoneType.SLATE, 3, new byte[5][5], 2, 2, 0,
                new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                RandomSource.create(77));
        helper.assertTrue(new GTDungeonChunkRoomFarmMobs().generate(data), "mob farm builds");

        for (int y = 4; y <= 6; y++) {
            var state = helper.getLevel().getBlockState(origin.offset(8, y, 8));
            helper.assertTrue(state.getBlock() instanceof ItemPipeBlock pipe
                    && ForgeRegistries.BLOCKS.getKey(pipe).getPath().equals("item_pipe_medium_arsenic_bronze")
                    && state.getValue(ItemPipeBlock.DOWN) && state.getValue(ItemPipeBlock.UP),
                    "GT6 pipe 25377 vertical run at y=" + y);
        }
        var junction = helper.getLevel().getBlockState(origin.offset(8, 3, 8));
        helper.assertTrue(junction.getBlock() instanceof ItemPipeBlock, "storage ring central pipe");
        for (Direction side : Direction.values())
            helper.assertTrue(junction.getValue(ItemPipeBlock.propFor(side)), "six-way pipe at storage ring");
        var below = helper.getLevel().getBlockState(origin.offset(8, 2, 8));
        helper.assertTrue(below.getBlock() instanceof ItemPipeBlock
                && !below.getValue(ItemPipeBlock.DOWN) && below.getValue(ItemPipeBlock.UP),
                "second ring uses GT6 connection mask 62");
        var upper = helper.getLevel().getBlockState(origin.offset(8, 7, 8));
        helper.assertTrue(upper.getBlock() instanceof ItemPipeBlock, "upper hopper junction has a real pipe");

        storage(helper, origin, 6, 3, 7, Items.GLASS_BOTTLE);
        storage(helper, origin, 6, 3, 8, Items.SLIME_BALL);
        storage(helper, origin, 7, 3, 6, Items.STRING);
        storage(helper, origin, 7, 3, 9, Items.REDSTONE);
        storage(helper, origin, 8, 3, 6, Items.SPIDER_EYE);
        storage(helper, origin, 8, 3, 9, Items.GLOWSTONE_DUST);
        storage(helper, origin, 9, 3, 7, Items.BONE);
        storage(helper, origin, 9, 3, 8, Items.STICK);
        storage(helper, origin, 6, 2, 8, Items.FEATHER);
        storage(helper, origin, 7, 2, 9, Items.GUNPOWDER);
        storage(helper, origin, 8, 2, 6, Items.ROTTEN_FLESH);
        storage(helper, origin, 8, 2, 9, Items.SUGAR);
        storage(helper, origin, 9, 2, 7, Items.ARROW);
        var woodenArrows = helper.getLevel().getBlockEntity(origin.offset(9, 2, 8));
        helper.assertTrue(woodenArrows instanceof MassStorageBlockEntity
                && ((MassStorageBlockEntity) woodenArrows).stored() >= 1
                && ((MassStorageBlockEntity) woodenArrows).template().getDescriptionId().contains("gregtech"),
                "last storage has GT wooden arrows");

        Boolean redSteel = null;
        for (int x = 7; x <= 8; x++) for (int z = 7; z <= 8; z++) {
            var state = helper.getLevel().getBlockState(origin.offset(x, 9, z));
            helper.assertTrue(state.getBlock() instanceof SpikeBlock
                    && state.getValue(SpikeBlock.MODE) == SpikeBlock.Mode.OMNI, "steel omni-spike");
            boolean red = state.getValue(SpikeBlock.SECONDARY);
            helper.assertTrue(redSteel == null || redSteel == red, "four spikes share GT6 material roll");
            redSteel = red;
        }
        helper.succeed();
    }
}
