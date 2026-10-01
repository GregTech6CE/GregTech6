package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.plant.BushBlock;
import com.gregtech.gregtech.blockentity.BushBlockEntity;
import com.gregtech.gregtech.blockentity.FluidSpringBlockEntity;
import com.gregtech.gregtech.content.plant.GTBerryBushes;
import com.gregtech.gregtech.registry.GTBushes;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.worldgen.GTFluidSpringsFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** A mined GT6 spring or berry bush must still work after its block item is placed again. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class WorldgenStatefulDropsTests {
    private static final BlockPos SPRING_FROM = new BlockPos(48000, 210, 48000);
    private static final BlockPos SPRING_TO = SPRING_FROM.east();
    private static final BlockPos BUSH_FROM = SPRING_FROM.offset(0, 0, 8);
    private static final BlockPos BUSH_TO = BUSH_FROM.east();

    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void springDropAndReplaceKeepsFluidAndAmount(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var block = GTDecorBlocks.FLUID_SPRING.get();
        for (var spring : GTFluidSpringsFeature.SPRINGS) {
            level.getBlockState(SPRING_FROM); // load the remote test chunk
            level.removeBlock(SPRING_FROM, false);
            level.removeBlock(SPRING_TO, false);
            level.setBlock(SPRING_FROM, block.defaultBlockState(), 3);
            var source = (FluidSpringBlockEntity) level.getBlockEntity(SPRING_FROM);
            helper.assertTrue(source != null, "spring creates its block entity");
            source.setSpring(spring.fluidId(), spring.amount());
            ItemStack mined = singleDrop(block, level, SPRING_FROM);
            CompoundTag saved = mined.getTagElement("BlockEntityTag");
            helper.assertTrue(saved != null && saved.getString("spring").equals(spring.fluidId())
                            && saved.getInt("amount") == spring.amount(),
                    spring.name() + " drop retains its GT6 fluid and output amount");
            helper.assertTrue(block.getCloneItemStack(level, SPRING_FROM, block.defaultBlockState())
                            .getTagElement("BlockEntityTag").equals(saved),
                    spring.name() + " pick-block retains its fluid");
            level.removeBlock(SPRING_FROM, false);
            level.setBlock(SPRING_TO, block.defaultBlockState(), 3);
            block.setPlacedBy(level, SPRING_TO, block.defaultBlockState(), null, mined);
            var replaced = (FluidSpringBlockEntity) level.getBlockEntity(SPRING_TO);
            helper.assertTrue(replaced != null && replaced.fluidId().equals(spring.fluidId())
                            && replaced.amount() == spring.amount(),
                    spring.name() + " placed item restores its spring");
        }
        level.removeBlock(SPRING_TO, false);
        level.setBlock(SPRING_TO, block.defaultBlockState(), 3);
        ItemStack ordinary = new ItemStack(block);
        block.setPlacedBy(level, SPRING_TO, block.defaultBlockState(), null, ordinary);
        var empty = (FluidSpringBlockEntity) level.getBlockEntity(SPRING_TO);
        helper.assertTrue(!ordinary.hasTag() && empty != null && empty.fluidId().isEmpty()
                        && !singleDrop(block, level, SPRING_TO).hasTag(),
                "a plain spring item must not fabricate a fluid");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void bushDropAndReplaceKeepsBerryButResetsGrowth(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var block = GTBushes.BUSH.get();
        level.getBlockState(BUSH_FROM);
        level.setBlock(BUSH_FROM.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(BUSH_TO.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        for (var berry : GTBerryBushes.types()) {
            level.removeBlock(BUSH_FROM, false);
            level.removeBlock(BUSH_TO, false);
            level.setBlock(BUSH_FROM, block.defaultBlockState(), 3);
            var source = (BushBlockEntity) level.getBlockEntity(BUSH_FROM);
            helper.assertTrue(source != null, "bush creates its block entity");
            source.setBerry(berry.id());
            ItemStack mined = singleDrop(block, level, BUSH_FROM);
            CompoundTag saved = mined.getTagElement("BlockEntityTag");
            helper.assertTrue(saved != null && saved.getString("berry").equals(berry.id())
                            && !saved.contains("growth"),
                    berry.id() + " bush drop keeps its berry but not the growth counter");
            level.removeBlock(BUSH_FROM, false);
            level.setBlock(BUSH_TO, block.defaultBlockState(), 3);
            block.setPlacedBy(level, BUSH_TO, block.defaultBlockState(), null, mined);
            var replaced = (BushBlockEntity) level.getBlockEntity(BUSH_TO);
            helper.assertTrue(replaced != null && replaced.berryId().equals(berry.id())
                            && replaced.growth() == 0
                            && level.getBlockState(BUSH_TO).getValue(BushBlock.STAGE) == 0,
                    berry.id() + " bush regrows after placement");
        }
        level.removeBlock(BUSH_TO, false);
        level.setBlock(BUSH_TO, block.defaultBlockState(), 3);
        ItemStack ordinary = new ItemStack(block);
        block.setPlacedBy(level, BUSH_TO, block.defaultBlockState(), null, ordinary);
        var empty = (BushBlockEntity) level.getBlockEntity(BUSH_TO);
        helper.assertTrue(!ordinary.hasTag() && empty != null && empty.berryId().isEmpty()
                        && !singleDrop(block, level, BUSH_TO).hasTag(),
                "a plain bush item must not fabricate berries");
        helper.succeed();
    }

    private static ItemStack singleDrop(Block block, ServerLevel level, BlockPos pos) {
        var drops = Block.getDrops(level.getBlockState(pos), level, pos, level.getBlockEntity(pos));
        if (drops.size() != 1 || !drops.get(0).is(block.asItem()))
            throw new AssertionError("Expected one GT6 block item drop: " + drops);
        return drops.get(0);
    }
}
