package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.block.machine.OriginalLargeBoilerControllerBlock;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import com.gregtech.gregtech.blockentity.machine.OriginalLargeBoilerControllerBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerSpecs;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTTanks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Source IDs 17201–17205: tier walls, dedicated ports, heat conversion and pressure retention. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class OriginalLargeBoilerRepairTests {
    private static final BlockPos CONTROL = new BlockPos(6, 5, 6);
    private OriginalLargeBoilerRepairTests() {}

    private static OriginalLargeBoilerControllerBlockEntity place(GameTestHelper helper,
                                                                  OriginalLargeBoilerSpecs.Variant spec) {
        for (var cell : OriginalLargeBoilerSpecs.LAYOUT.cells())
            helper.setBlock(cell.at(CONTROL, Direction.NORTH), Blocks.AIR);
        Block controller = LargeMachineParts.block(spec.originalId());
        helper.setBlock(CONTROL, controller.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        return (OriginalLargeBoilerControllerBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(CONTROL));
    }
    private static void build(GameTestHelper helper, OriginalLargeBoilerSpecs.Variant spec) {
        for (var cell : OriginalLargeBoilerSpecs.LAYOUT.cells()) {
            BlockPos pos = cell.at(CONTROL, Direction.NORTH);
            if (pos.equals(CONTROL)) continue;
            Block part = switch (cell.role()) {
                case HEAT_INPUT -> com.gregtech.gregtech.registry.GTMultiblocks.HEAT_TRANSMITTER.get();
                case AIR -> Blocks.AIR;
                default -> spec.wall();
            };
            helper.setBlock(pos, part);
        }
    }

    @GameTest(template = "test_blueprint_large", timeoutTicks = 200)
    public static void fiveOriginalBoilersRequireTheirOwnDenseWall(GameTestHelper helper) {
        helper.assertTrue(OriginalLargeBoilerSpecs.all().size() == 5,
                "GT6 defines exactly five material-specific main barometers");
        for (var spec : OriginalLargeBoilerSpecs.all()) {
            Block block = LargeMachineParts.block(spec.originalId());
            helper.assertTrue(block instanceof OriginalLargeBoilerControllerBlock,
                    "original ID has a real controller block: " + spec.originalId());
            var boiler = place(helper, spec);
            helper.assertTrue(boiler.getType() == GTBlockEntities.ORIGINAL_LARGE_BOILER.get(),
                    "each original ID creates its own boiler block entity");
            helper.assertTrue(!boiler.isStructureOk(), "unassembled boiler is inert");
            build(helper, spec);
            helper.assertTrue(boiler.isStructureOk(), "correct dense wall forms tier " + spec.originalId());
            helper.assertTrue(spec.previewCells().size() == 35
                            && spec.previewCells().values().stream()
                                    .filter(b -> b == spec.wall()).count() == 25,
                    "JEI preview includes all 25 matching dense walls, nine heat transmitters and air");
            helper.setBlock(CONTROL.south(), spec.denseWallId() == 18022
                    ? LargeMachineParts.block(18026) : LargeMachineParts.block(18022));
            helper.assertTrue(!boiler.isStructureOk(), "other material wall invalidates tier " + spec.originalId());
        }
        helper.succeed();
    }

    @GameTest(template = "test_blueprint_large", timeoutTicks = 200)
    public static void lowerWallWaterAndBaseHeatMakeSteam(GameTestHelper helper) {
        var spec = OriginalLargeBoilerSpecs.byOriginalId(17201);
        var boiler = place(helper, spec);
        build(helper, spec);
        helper.assertTrue(boiler.isStructureOk(), "stainless boiler forms");
        BlockPos waterPos = helper.absolutePos(CONTROL.south());
        BlockPos steamPos = helper.absolutePos(CONTROL.south().above(2));
        BlockPos heatPos = helper.absolutePos(CONTROL.below());
        var waterPort = (MultiblockPortBlockEntity) helper.getLevel().getBlockEntity(waterPos);
        var upperPort = (MultiblockPortBlockEntity) helper.getLevel().getBlockEntity(steamPos);
        var heatPort = (MultiblockPortBlockEntity) helper.getLevel().getBlockEntity(heatPos);
        IFluidHandler input = waterPort.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.SOUTH)
                .resolve().orElseThrow();
        IFluidHandler output = upperPort.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                .resolve().orElseThrow();
        helper.assertTrue(input.getTankCapacity(0) == 128_000
                        && output.getTankCapacity(0) == spec.steamCapacity(),
                "GT6 water and steam capacities are separate");
        helper.assertTrue(input.fill(new FluidStack(Fluids.WATER, 1_000),
                        IFluidHandler.FluidAction.EXECUTE) == 1_000,
                "water enters lower walls");
        helper.assertTrue(output.fill(new FluidStack(Fluids.WATER, 1_000),
                        IFluidHandler.FluidAction.EXECUTE) == 0,
                "upper walls cannot accept water");
        helper.assertTrue(heatPort.doInject(GregTechTags.Energy.HU, Direction.DOWN,
                        80, 1_000, true) == 1_000,
                "heat enters through the transmitter base");
        OriginalLargeBoilerControllerBlockEntity.serverTick(helper.getLevel(),
                helper.absolutePos(CONTROL), boiler.getBlockState(), boiler);
        helper.assertTrue(boiler.waterAmount() == 0 && boiler.storedHeat() == 0
                        && boiler.steamAmount() == 160_000,
                "one litre water and 80 HU become 160 litres steam");
        helper.assertTrue(output.drain(500, IFluidHandler.FluidAction.EXECUTE).getAmount() == 500
                        && input.drain(500, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "steam exits upper walls only");
        helper.succeed();
    }

    @GameTest(template = "test_blueprint_large", timeoutTicks = 200)
    public static void tierBuffersAutoOutputAndCoolDropNbt(GameTestHelper helper) {
        for (var spec : OriginalLargeBoilerSpecs.all()) {
            helper.assertTrue(spec.heatCapacity() == spec.steamOutput() * 10_000L
                            && spec.steamCapacity() == spec.steamOutput() * 10_000L,
                    "GT6 tier-specific buffers: " + spec.originalId());
        }
        var spec = OriginalLargeBoilerSpecs.byOriginalId(17201);
        var boiler = place(helper, spec);
        build(helper, spec);
        var input = boiler.directFluidHandler();
        helper.assertTrue(input.fill(new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE) == 1_000, "formed controller receives water");
        var drops = Block.getDrops(boiler.getBlockState(), helper.getLevel(),
                helper.absolutePos(CONTROL), boiler);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(LargeMachineParts.block(17201).asItem())
                        && drops.get(0).getTagElement("BlockEntityTag") != null,
                "cool boiler drops its own item with fluid NBT");
        var restoredTag = drops.get(0).getTagElement("BlockEntityTag");
        helper.setBlock(CONTROL, Blocks.AIR);
        boiler = place(helper, spec);
        build(helper, spec);
        boiler.load(restoredTag);
        helper.assertTrue(boiler.waterAmount() == 1_000,
                "original water storage survives a safe pick-up and replacement");

        CompoundTag saved = boiler.saveWithoutMetadata();
        CompoundTag steamTag = saved.getCompound("gt.steam");
        steamTag.putLong("Amount", spec.steamCapacity() / 2 + spec.steamOutput() * 2);
        steamTag.put("Fluid", new FluidStack(GTFluids.still("Steam").get(), 1)
                .writeToNBT(new CompoundTag()));
        saved.put("gt.steam", steamTag);
        boiler.load(saved);
        BlockPos receiver = CONTROL.south().above(3);
        helper.setBlock(receiver, GTTanks.DRUM_STEEL.get());
        OriginalLargeBoilerControllerBlockEntity.serverTick(helper.getLevel(),
                helper.absolutePos(CONTROL), boiler.getBlockState(), boiler);
        var drum = helper.getLevel().getBlockEntity(helper.absolutePos(receiver));
        int accepted = drum.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN)
                .resolve().orElseThrow().getFluidInTank(0).getAmount();
        helper.assertTrue(accepted == spec.steamOutput(),
                "between half and three-quarter pressure the top opening exports one rated steam packet");
        helper.succeed();
    }

    @GameTest(template = "test_blueprint_large", timeoutTicks = 200)
    public static void steamOutputDoublesOnlyAboveThreeQuarterPressure(GameTestHelper helper) {
        var spec = OriginalLargeBoilerSpecs.byOriginalId(17201);
        var boiler = place(helper, spec);
        build(helper, spec);
        helper.assertTrue(boiler.isStructureOk(), "boiler forms before testing pressure-dependent output");
        BlockPos receiver = CONTROL.south().above(3);
        long capacity = spec.steamCapacity();
        long[] stored = {capacity * 3 / 4, capacity * 3 / 4 + 1};
        long[] expected = {spec.steamOutput(), spec.steamOutput() * 2};
        for (int i = 0; i < stored.length; i++) {
            CompoundTag saved = boiler.saveWithoutMetadata();
            CompoundTag steamTag = saved.getCompound("gt.steam");
            steamTag.putLong("Amount", stored[i]);
            steamTag.put("Fluid", new FluidStack(GTFluids.still("Steam").get(), 1)
                    .writeToNBT(new CompoundTag()));
            saved.put("gt.steam", steamTag);
            boiler.load(saved);
            helper.setBlock(receiver, Blocks.AIR);
            helper.setBlock(receiver, GTTanks.DRUM_STEEL.get());
            OriginalLargeBoilerControllerBlockEntity.serverTick(helper.getLevel(),
                    helper.absolutePos(CONTROL), boiler.getBlockState(), boiler);
            var drum = helper.getLevel().getBlockEntity(helper.absolutePos(receiver));
            int accepted = drum.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN)
                    .resolve().orElseThrow().getFluidInTank(0).getAmount();
            helper.assertTrue(accepted == expected[i],
                    "steam output at the three-quarter-pressure boundary follows GT6, case " + i);
        }
        helper.succeed();
    }

    @GameTest(template = "test_blueprint_large", timeoutTicks = 200)
    public static void damagedUnpressurisedBoilerStillCools(GameTestHelper helper) {
        var spec = OriginalLargeBoilerSpecs.byOriginalId(17201);
        var boiler = place(helper, spec);
        build(helper, spec);
        helper.assertTrue(boiler.isStructureOk(), "boiler forms before its wall is damaged");
        CompoundTag saved = boiler.saveWithoutMetadata();
        saved.putLong("gt.heat", spec.steamOutput() * 32 + 1);
        saved.putInt("gt.cooldown", 0);
        CompoundTag steamTag = saved.getCompound("gt.steam");
        steamTag.putLong("Amount", spec.steamOutput() * 64 + 100);
        steamTag.put("Fluid", new FluidStack(GTFluids.still("Steam").get(), 1)
                .writeToNBT(new CompoundTag()));
        saved.put("gt.steam", steamTag);
        boiler.load(saved);
        helper.setBlock(CONTROL.south(), Blocks.AIR);
        helper.assertTrue(!boiler.isStructureOk() && boiler.barometerValue() <= 4,
                "damaged boiler is invalid but below explosive pressure");
        OriginalLargeBoilerControllerBlockEntity.serverTick(helper.getLevel(),
                helper.absolutePos(CONTROL), boiler.getBlockState(), boiler);
        helper.assertTrue(boiler.storedHeat() == 1 && boiler.steamAmount() == 100,
                "missing wall does not freeze the original heat and steam cooldown");
        helper.succeed();
    }
}
