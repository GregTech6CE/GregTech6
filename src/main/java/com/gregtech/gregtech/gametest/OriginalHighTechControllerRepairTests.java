package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.OriginalBedrockDrillControllerBlock;
import com.gregtech.gregtech.block.machine.OriginalLightningRodControllerBlock;
import com.gregtech.gregtech.blockentity.machine.BedrockDrillControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LightningRodControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The original GT6 numeric controllers must be usable independently of the later *_main blocks. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class OriginalHighTechControllerRepairTests {
    private OriginalHighTechControllerRepairTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void originalLightningControllerFormsEmitsAndKeepsCharge(GameTestHelper helper) {
        var world = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(6, 2, 6));
        var original = LargeMachineParts.block(17998);
        helper.assertTrue(original instanceof OriginalLightningRodControllerBlock,
                "GT6 17998 must be a real controller block, not a passive port");
        world.setBlock(center, original.defaultBlockState(), 3);
        for (int y = 0; y < 5; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            world.setBlock(center.offset(x, y, z), LargeMachineParts.block(y % 2 == 0 ? 18004 : 18041).defaultBlockState(), 3);
        }
        world.setBlock(center.above(5), LargeMachineParts.block(18104).defaultBlockState(), 3);
        var machine = (LightningRodControllerBlockEntity) world.getBlockEntity(center);
        helper.assertTrue(machine != null && machine.isStructureOk(),
                "five exact 3x3 layers and the original rod form GT6 17998");
        CompoundTag charge = new CompoundTag();
        charge.putLong("gt.energy", LightningRodControllerBlockEntity.PACKET * 3);
        machine.load(charge);
        helper.assertTrue(machine.getEnergyOffered(GregTechTags.Energy.EU, Direction.DOWN,
                        LightningRodControllerBlockEntity.PACKET) == 3,
                "stored lightning energy is offered only from the bottom");
        helper.assertTrue(machine.doEnergyExtraction(GregTechTags.Energy.EU, Direction.UP,
                        LightningRodControllerBlockEntity.PACKET, 1, true) == 0,
                "top face does not output EU");
        helper.assertTrue(machine.doEnergyExtraction(GregTechTags.Energy.EU, Direction.DOWN,
                        LightningRodControllerBlockEntity.PACKET, 1, true) == 1,
                "bottom face extracts one original voltage packet");
        var drops = world.getBlockState(center).getDrops(new LootParams.Builder(world)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, machine));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(original.asItem())
                        && drops.get(0).getTagElement("BlockEntityTag").getLong("gt.energy")
                        == LightningRodControllerBlockEntity.PACKET * 2,
                "the original controller item preserves the remaining charge on break");
        machine.load(new CompoundTag());
        ((OriginalLightningRodControllerBlock) original).setPlacedBy(world, center,
                world.getBlockState(center), null, drops.get(0));
        helper.assertTrue(machine.getEnergyStored(GregTechTags.Energy.EU, null)
                        == LightningRodControllerBlockEntity.PACKET * 2,
                "placing the stored controller restores its charge");
        world.setBlock(center.offset(1, 1, 0), Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(!machine.isStructureOk(), "a missing original niobium-titanium coil breaks formation");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void originalBedrockDrillMinesWithDrillHeadsAndKeepsResources(GameTestHelper helper) {
        var world = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(6, 6, 6));
        var original = LargeMachineParts.block(17999);
        helper.assertTrue(original instanceof OriginalBedrockDrillControllerBlock,
                "GT6 17999 must be a real drill controller, not a passive port");
        world.setBlock(center, original.defaultBlockState(), 3);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = -5; y <= 0; y++) {
            if (x == 0 && y == 0 && z == 0) continue;
            var part = y == -5 ? Blocks.BEDROCK : LargeMachineParts.block(y == -4 ? 18103 : 18026);
            world.setBlock(center.offset(x, y, z), part.defaultBlockState(), 3);
        }
        var machine = (BedrockDrillControllerBlockEntity) world.getBlockEntity(center);
        helper.assertTrue(machine != null && machine.isStructureOk(),
                "GT6 18103 drill heads and four dense-titanium layers form above a bedrock floor");
        var wall = (MultiblockPortBlockEntity) world.getBlockEntity(center.offset(1, -1, 0));
        var tank = machine.getCapability(ForgeCapabilities.FLUID_HANDLER, null).orElseThrow(IllegalStateException::new);
        helper.assertTrue(tank.fill(new FluidStack(GTFluids.still("Lubricant").get(), 200),
                IFluidHandler.FluidAction.EXECUTE) == 200, "lubricant fills the drill tank");
        helper.assertTrue(wall.doEnergyInjection(GregTechTags.Energy.RU, Direction.EAST, 2048, 16, true) == 16,
                "dense-titanium side wall accepts 32768 RU");
        machine.tick();
        var output = machine.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                .orElseThrow(IllegalStateException::new);
        helper.assertTrue(!output.getStackInSlot(0).isEmpty()
                        && tank.getFluidInTank(0).getAmount() == 100
                        && machine.getEnergyStored(GregTechTags.Energy.RU, null) == 0,
                "one original drilling cycle produces an item and consumes 32768 RU plus 100 mB lubricant");
        var drops = world.getBlockState(center).getDrops(new LootParams.Builder(world)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, machine));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(original.asItem())
                        && drops.get(0).getTagElement("BlockEntityTag") != null
                        && !drops.get(0).getTagElement("BlockEntityTag").contains("gt.output"),
                "the controller block drops with fluid state but without duplicating its separately dropped output");
        world.setBlock(center.offset(1, -4, 0), Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(!machine.isStructureOk(), "a missing original drill head disables mining");
        helper.succeed();
    }
}
