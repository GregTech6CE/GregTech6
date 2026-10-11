package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.LogisticsTankBlock;
import com.gregtech.gregtech.block.machine.LogisticsWireBlock;
import com.gregtech.gregtech.blockentity.machine.LogisticsTankBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LogisticsWireBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.content.logistics.LogisticsCoverType;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTIconSetBlocks;
import com.gregtech.gregtech.registry.GTTanks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** GT6 32072: a dedicated direct-network barrel with a remembered fluid filter. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LogisticsTankTests {
    private LogisticsTankTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void explicitMeltingPointAcceptsHotPlasma(GameTestHelper helper) {
        // GT6 Loader_MultiTileEntities:2170 writes NBT_CAPACITY_HU=100000. The barrel base
        // reads that key into mMeltingPoint (kelvin), despite the misleading NBT key name.
        var block = GTTanks.LOGISTICS_TANK.get();
        var tank = new LogisticsTankBlockEntity(helper.absolutePos(new BlockPos(1, 2, 1)),
                block.defaultBlockState());
        var heliumPlasma = ForgeRegistries.FLUIDS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", "heliumplasma"));
        helper.assertTrue(heliumPlasma != null, "GT6 helium plasma exists");
        if (heliumPlasma == null) return;
        var plasma = new FluidStack(heliumPlasma, 1000);
        helper.assertTrue(GTFluids.entryForFluid(heliumPlasma) != null
                        && GTFluids.entryForFluid(heliumPlasma).temperature() == 10_000,
                "GT6 helium plasma is 10000 K, above tungsten's inferred barrel limit");
        helper.assertTrue(tank.spec().maxTemperature() == 100_000
                        && tank.getFluidTank().maxTemperature() == 100_000,
                "logistics tank uses GT6's explicit 100000 K melting point");
        helper.assertTrue(tank.isFluidValid(0, plasma)
                        && tank.fill(plasma, IFluidHandler.FluidAction.EXECUTE) == 1000,
                "the plasma-proof logistics tank accepts 10000 K fluid below its actual limit");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void barrelFillRejectsFluidAboveItsMeltingPoint(GameTestHelper helper) {
        var wood = new TankBlockEntity(helper.absolutePos(new BlockPos(1, 2, 1)),
                GTTanks.WOOD_BARREL.get().defaultBlockState());
        var lava = new FluidStack(Fluids.LAVA, 1000);
        helper.assertTrue(wood.spec().maxTemperature() < Fluids.LAVA.getFluidType().getTemperature(lava),
                "the test fluid is hotter than a wooden barrel can tolerate");
        helper.assertTrue(!wood.isFluidValid(0, lava)
                        && wood.fill(lava, IFluidHandler.FluidAction.SIMULATE) == 0
                        && wood.fill(lava, IFluidHandler.FluidAction.EXECUTE) == 0,
                "the barrel rejects hot fluid at the fill gate, not after it has already been stored");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void retainsFilterAndCapacityAcrossDrainAndSave(GameTestHelper helper) {
        var block = GTTanks.LOGISTICS_TANK.get();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        var tank = (LogisticsTankBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(block instanceof LogisticsTankBlock
                        && GTBlockEntities.LOGISTICS_TANK.get().isValid(block.defaultBlockState())
                        && !GTBlockEntities.TANK.get().isValid(block.defaultBlockState())
                        && tank != null && tank.getTankCapacity(0) == 1_000_000
                        && tank.fluidStoragePriority() == 1 && tank.itemStoragePriority() == 0,
                "GT6 logistics tank is a distinct, one-million-litre Generic network host");
        tank.toggleSoftHammerState();
        helper.assertTrue(!tank.getSoftHammerState(), "GT6 logistics barrels cannot be sealed");
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000
                        && tank.fluidStoragePriority() == 2 && tank.fluidStorageFilter() == Fluids.WATER,
                "first fill learns the semi-filtered fluid type");
        helper.assertTrue(tank.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1000
                        && tank.getFluidTank().isEmpty() && tank.fluidStoragePriority() == 2
                        && tank.fluidStorageFilter() == Fluids.WATER
                        && tank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.SIMULATE) == 0,
                "empty logistics tank remembers water without preventing a full drain");
        var saved = tank.saveWithoutMetadata();
        var restored = new LogisticsTankBlockEntity(pos, block.defaultBlockState());
        restored.load(saved);
        helper.assertTrue(restored.fluidStoragePriority() == 2 && restored.fluidStorageFilter() == Fluids.WATER
                        && restored.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0
                        && restored.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                "the retained type survives save and reload and still accepts its own fluid");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                        "gregtech", "logistics/logistics_tank")).isPresent(),
                "GT6 logistics tank has its screw, tungsten drum, bus and field-generator recipe");
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void storesAndExportsFluidWithoutABusCover(GameTestHelper helper) {
        var core = LogisticsNetworkTests.minimumCore(helper);
        BlockPos center = core.getBlockPos().south(2);
        BlockPos wirePos = center.offset(3, 0, 0);
        BlockPos storagePos = center.offset(-3, 0, 0);
        var wireBlock = (LogisticsWireBlock) GTIconSetBlocks.LOGISTICS_WIRE.get();
        helper.getLevel().setBlock(wirePos, wireBlock.defaultBlockState()
                .setValue(ElectricWireBlock.WEST, true), 3);
        helper.getLevel().setBlock(wirePos.east(), GTTanks.DRUM_STEEL.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(storagePos, GTTanks.LOGISTICS_TANK.get().defaultBlockState(), 3);
        var wire = (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(wirePos);
        var drum = (TankBlockEntity) helper.getLevel().getBlockEntity(wirePos.east());
        var storage = (LogisticsTankBlockEntity) helper.getLevel().getBlockEntity(storagePos);
        var importItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(
                "gregtech", LogisticsCoverType.GENERIC_IMPORT.id()));
        var exportItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(
                "gregtech", LogisticsCoverType.GENERIC_EXPORT.id()));
        helper.assertTrue(wire != null && drum != null && storage != null
                        && importItem != null && exportItem != null
                        && wire.logisticsCovers().attach(Direction.EAST, new ItemStack(importItem))
                        && core.scanNetwork().contains(storagePos),
                "the bare logistics tank joins a formed network without a storage bus");
        drum.fill(new FluidStack(Fluids.WATER, 10_000), IFluidHandler.FluidAction.EXECUTE);
        // Energy injection is tested in the core suite; give the router the same threshold here.
        var wallCell = com.gregtech.gregtech.content.logistics.LogisticsCoreStructure.cells().stream()
                .filter(cell -> cell.kind() == com.gregtech.gregtech.content.logistics.LogisticsCoreStructure.Kind.WALL)
                .findFirst().orElseThrow();
        var wall = (com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity)
                helper.getLevel().getBlockEntity(wallCell.at(core.getBlockPos(), Direction.NORTH));
        helper.assertTrue(wall.doEnergyInjection(com.gregtech.gregtech.data.GregTechTags.Energy.EU,
                Direction.UP, 512, 1, true) == 1, "core is powered");
        var stored = core.routeNetworkOnce();
        helper.assertTrue(stored.fluidMoved() == 10_000 && storage.getFluidTank().getAmount() == 10_000
                        && storage.fluidStoragePriority() == 2 && drum.getFluidTank().isEmpty(),
                "Import routes water into the direct logistics tank");
        wire.logisticsCovers().remove(Direction.EAST);
        helper.assertTrue(wire.logisticsCovers().attach(Direction.EAST, new ItemStack(exportItem)),
                "export bus replaces the import bus");
        var exported = core.routeNetworkOnce();
        helper.assertTrue(exported.fluidMoved() == 10_000 && storage.getFluidTank().isEmpty()
                        && storage.fluidStoragePriority() == 2 && storage.fluidStorageFilter() == Fluids.WATER
                        && drum.getFluidTank().getAmount() == 10_000,
                "direct storage exports all contents and retains the water filter");
        helper.succeed();
    }
}
