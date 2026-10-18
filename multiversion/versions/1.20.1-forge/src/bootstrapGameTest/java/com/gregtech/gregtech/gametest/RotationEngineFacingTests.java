package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.machine.RotationEngineSpec;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.machine.EngineBlock;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.blockentity.machine.KineticRotationEngineBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTMiscBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The original engine's physical shaft follows wrench facing, not a screwdriver mask. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class RotationEngineFacingTests {
    private static final BlockPos CENTER = new BlockPos(2, 1, 2);

    private static EngineBlock bronzeBlock() {
        return MachineRegistry.rotationEngines().stream().map(holder -> holder.get())
                .filter(block -> block.engineSpec(RotationEngineSpec.class).id().equals("engine_rotation_bronze"))
                .findFirst().orElseThrow();
    }

    private static ItemStack tool(GTToolType type) {
        return GTToolItem.create(type, Materials.Steel, GTMaterialRegistry.get("Wood"));
    }

    private static BlockHitResult hit(BlockPos pos, Direction side) {
        return new BlockHitResult(Vec3.atCenterOf(pos).add(
                side.getStepX() * .5, side.getStepY() * .5, side.getStepZ() * .5), side, pos, false);
    }

    private static final class KuSink extends GTEnergyBlockEntity {
        private long packets;
        private long lastSize;

        private KuSink(BlockPos pos, BlockState state) {
            super(GTBlockEntities.CHARGING_CRAFTING_TABLE.get(), pos, state);
        }

        @Override public boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) {
            return !emitting && type == GregTechTags.Energy.KU;
        }
        @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, Direction side, boolean theoretical) {
            return type == GregTechTags.Energy.KU;
        }
        @Override public long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) { return Long.MAX_VALUE; }
        @Override public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) { return 0; }
        @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) { return 16; }
        @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type, Direction side) { return 0; }
        @Override public long doEnergyInjection(GregTechTags.Tag type, Direction side, long size,
                                                 long amount, boolean execute) {
            if (type != GregTechTags.Energy.KU) return 0;
            if (execute) {
                packets += amount;
                lastSize = size;
            }
            return amount;
        }
    }

    private static KuSink sink(GameTestHelper helper, BlockPos offset) {
        helper.setBlock(offset, GTMiscBlocks.CHARGING_CRAFTING_TABLE.get());
        BlockPos absolute = helper.absolutePos(offset);
        KuSink sink = new KuSink(absolute, helper.getLevel().getBlockState(absolute));
        helper.getLevel().setBlockEntity(sink);
        return sink;
    }

    @GameTest(template = "test_empty")
    public static void screwdriverAndSoftHammerDoNotChangeBipolarShaft(GameTestHelper helper) {
        EngineBlock block = bronzeBlock();
        helper.setBlock(CENTER, block.defaultBlockState().setValue(EngineBlock.FACING, Direction.NORTH));
        BlockPos pos = helper.absolutePos(CENTER);
        var engine = (KineticRotationEngineBlockEntity) helper.getLevel().getBlockEntity(pos);
        var player = helper.makeMockSurvivalPlayer();
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        for (GTToolType type : new GTToolType[]{GTToolType.SCREWDRIVER, GTToolType.SOFT_HAMMER}) {
            ItemStack held = tool(type);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            var before = engine.saveWithoutMetadata();
            int wear = held.getDamageValue();
            InteractionResult result = block.use(engine.getBlockState(), helper.getLevel(), pos,
                    player, InteractionHand.MAIN_HAND, hit(pos, Direction.NORTH));
            helper.assertTrue(result == InteractionResult.PASS && held.getDamageValue() == wear
                            && before.equals(engine.saveWithoutMetadata())
                            && engine.getBlockState().getValue(EngineBlock.FACING) == Direction.NORTH,
                    "GT6 rotation engine has no screwdriver or soft-hammer face action: " + type);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void wrenchRotationMovesRealRuInputAndKuOutputFaces(GameTestHelper helper) {
        EngineBlock block = bronzeBlock();
        helper.setBlock(CENTER, block.defaultBlockState().setValue(EngineBlock.FACING, Direction.NORTH));
        BlockPos pos = helper.absolutePos(CENTER);
        var engine = (KineticRotationEngineBlockEntity) helper.getLevel().getBlockEntity(pos);
        var player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool(GTToolType.WRENCH));
        KuSink west = sink(helper, CENTER.west());
        KuSink south = sink(helper, CENTER.south());

        helper.assertTrue(engine.isEnergyAcceptingFrom(GregTechTags.Energy.RU, Direction.WEST, false)
                        && engine.isEnergyEmittingTo(GregTechTags.Energy.KU, Direction.SOUTH, false),
                "north-facing engine takes RU west and outputs KU south");
        helper.assertTrue(block.use(engine.getBlockState(), helper.getLevel(), pos,
                        player, InteractionHand.MAIN_HAND, hit(pos, Direction.EAST)).consumesAction(),
                "GT6 wrench selects the clicked east-facing shaft");
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) == engine
                        && engine.getBlockState().getValue(EngineBlock.FACING) == Direction.EAST
                        && !engine.isEnergyAcceptingFrom(GregTechTags.Energy.RU, Direction.WEST, false)
                        && engine.isEnergyAcceptingFrom(GregTechTags.Energy.RU, Direction.SOUTH, false)
                        && engine.isEnergyEmittingTo(GregTechTags.Energy.KU, Direction.WEST, false)
                        && !engine.isEnergyEmittingTo(GregTechTags.Energy.KU, Direction.SOUTH, false),
                "wrench changes the shaft axis and the real RU/KU face gates");
        helper.assertTrue(engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, 32, 1, true) == 0
                        && engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.SOUTH, 32, 1, true) == 1,
                "rotated machine rejects former input and accepts new radial input");
        KineticRotationEngineBlockEntity.serverTick(helper.getLevel(), pos, engine.getBlockState(), engine);
        helper.assertTrue(west.packets == 1 && west.lastSize == 16 && south.packets == 0,
                "first tick sends +16 KU out of new west shaft end, not former south end");
        helper.succeed();
    }
}
