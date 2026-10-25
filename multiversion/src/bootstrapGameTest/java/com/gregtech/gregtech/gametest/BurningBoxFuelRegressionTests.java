package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.block.machine.BurningBoxBlock;
import com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BurningBoxBlockEntity;
import com.gregtech.gregtech.content.cover.PanelCoverHost;
import com.gregtech.gregtech.data.FuelRecipeMaps;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import java.util.List;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Issue #109: native registered boxes, actual fuel tables/capabilities, and a real boiler HU sink. */
@GameTestHolder("gt6ce_burning_box_fuels")
@PrefixGameTestTemplate(false)
public final class BurningBoxFuelRegressionTests {
    private static final String EMPTY = "test_empty";

    private static CompoundTag save(GameTestHelper h, BlockEntity entity) {
//? if >=1.21.1 {
/*        return entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return entity.saveWithoutMetadata();
//?}
    }

    private static void load(GameTestHelper h, BlockEntity entity, CompoundTag data) {
//? if >=1.21.1 {
/*        entity.loadWithComponents(data, h.getLevel().registryAccess());
*///?} else {
        entity.load(data);
//?}
    }

    private static final class Fixture implements AutoCloseable {
        private record Saved(BlockState state, CompoundTag data) {}
        final GameTestHelper h;
        final BlockPos pos;
        final java.util.Map<BlockPos, Saved> saved = new java.util.LinkedHashMap<>();
        final BurningBoxBlockEntity box;
        final BoilerTankBlockEntity boiler;

        Fixture(GameTestHelper h, String id) {
            this.h = h;
            pos = h.absolutePos(new BlockPos(2, 2, 2));
            var block = registered(id);
            h.assertTrue(block instanceof BurningBoxBlock, "actual registered burning box " + id);
            for (var direction : Direction.values()) set(pos.relative(direction), Blocks.AIR.defaultBlockState());
            set(pos, block.defaultBlockState());
            box = (BurningBoxBlockEntity) h.getLevel().getBlockEntity(pos);
            h.assertTrue(box != null && box.spec() != null, "native box/spec exists");
            // Capability gates lazily create cover storage; initialize it before simulation snapshots.
            ((PanelCoverHost) (Object) box).panels();
            set(pos.relative(box.getFrontFacing()), Blocks.AIR.defaultBlockState());
            set(pos.above(), registered("steam_boiler_tungsten").defaultBlockState());
            boiler = (BoilerTankBlockEntity) h.getLevel().getBlockEntity(pos.above());
            h.assertTrue(boiler != null, "actual registered boiler receives emitted HU");
        }

        void set(BlockPos at, BlockState state) {
            if (!saved.containsKey(at)) {
                var entity = h.getLevel().getBlockEntity(at);
                saved.put(at.immutable(), new Saved(h.getLevel().getBlockState(at), entity == null ? null : save(h, entity)));
            }
            h.getLevel().removeBlockEntity(at);
            h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            h.getLevel().setBlock(at, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }

        IFluidHandler fluids() {
//? if >=1.21.1 {
/*            var result = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, Direction.DOWN);
            if (result == null) throw new AssertionError("native box fluid capability");
            return result;
*///?} else {
            return box.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, Direction.DOWN)
                    .orElseThrow(() -> new AssertionError("native box fluid capability"));
//?}
        }

        IItemHandler items() {
//? if >=1.21.1 {
/*            var result = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
            if (result == null) throw new AssertionError("native box item capability");
            return result;
*///?} else {
            return box.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.DOWN)
                    .orElseThrow(() -> new AssertionError("native box item capability"));
//?}
        }

        void ignite() {
            h.assertTrue(box.usePoweredIgniter(box.getFrontFacing(), Long.MAX_VALUE, 0), "native ignition succeeds");
        }

        void tick() {
            BurningBoxBlockEntity.serverTick(h.getLevel(), pos, box.getBlockState(), box);
        }

        long heat() { return save(h, box).getLong("gt.energy"); }

        @Override public void close() {
            for (var at : saved.keySet()) h.getLevel().removeBlockEntity(at);
            for (var entry : saved.entrySet()) {
                var at = entry.getKey(); var previous = entry.getValue();
                h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                h.getLevel().setBlock(at, previous.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                var entity = h.getLevel().getBlockEntity(at);
                if (entity != null && previous.data() != null) load(h, entity, previous.data());
            }
        }
    }

    private static Block registered(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
    }

    @GameTest(template = EMPTY)
    public static void weakLiquidAndGasFuelsSustainRatedNativeHeatOutput(GameTestHelper h) {
        for (String kind : List.of("liquid", "gas")) {
            try (var f = new Fixture(h, "burning_box_" + kind + "_titanium")) {
                var fuel = GTFluids.stack(kind.equals("gas") ? "Hydrogen" : "Oil_Creosote", 100);
                var map = kind.equals("gas") ? FuelRecipeMaps.Gas : FuelRecipeMaps.Burn;
                var recipe = map.findRecipe(List.of(), List.of(fuel), false, 0, 0);
                h.assertTrue(recipe != null && recipe.mFluidInputs.length == 1, "real bootstrapped weak fuel recipe");
                long chargeHeat = Math.abs(recipe.mEUt) * recipe.mDuration * f.box.spec().efficiency() / 10000;
                long rate = f.box.spec().outputRate();
                h.assertTrue(chargeHeat > 0 && chargeHeat < rate, "one source fuel charge cannot reach the registered output rate");
                h.assertTrue(f.fluids().fill(fuel, IFluidHandler.FluidAction.EXECUTE) == 100, "fill through native capability");
                f.ignite();
                f.tick();
                int consumed = 100 - f.fluids().getFluidInTank(0).getAmount();
                h.assertTrue(consumed > recipe.mFluidInputs[0].getAmount() && f.box.isBurning(), "weak fuel burns multiple charges instead of extinguishing");
                h.assertTrue(f.heat() >= rate * 2 && f.heat() < rate * 2 + chargeHeat, "native box accumulates its two-output reserve");
                h.assertTrue(f.heat() == consumed / recipe.mFluidInputs[0].getAmount() * chargeHeat, "all consumed startup fuel is retained as heat");
                f.tick();
                consumed = 100 - f.fluids().getFluidInTank(0).getAmount();
                h.assertTrue(f.boiler.storedHeat() == rate && f.box.isBurning(), "real boiler receives exactly one rated HU packet");
                h.assertTrue(f.heat() + f.boiler.storedHeat() == consumed / recipe.mFluidInputs[0].getAmount() * chargeHeat, "fuel heat is conserved across refill and native emission");
                h.assertTrue(save(h, f.box).getInt("gt.cooldown") == 99, "successful refuelling refreshes source cooldown");
            }
        }
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void originalEngineLiquidsAreAlsoAvailableToRegisteredLiquidBurningBoxes(GameTestHelper h) {
        String[] fuels = {"Diesel", "Kerosine", "JetFuel", "Fuel", "NitroFuel", "BioDiesel", "BioFuel", "BioEthanol", "GenGas_Ethanol", "Reikanol"};
        long[] originalHeat = {320, 320, 1152, 384, 576, 384, 576, 144, 144, 144};
        for (int i = 0; i < fuels.length; i++) {
            var fuel = GTFluids.stack(fuels[i], 32);
            h.assertTrue(fuel != null && !fuel.isEmpty(), "real registered original liquid fuel " + fuels[i]);
            var recipe = FuelRecipeMaps.Burn.findRecipe(List.of(), List.of(fuel), false, 0, 0);
            h.assertTrue(recipe != null && recipe.mFluidInputs.length == 1 && recipe.mFluidInputs[0].getAmount() == 1
                    && Math.abs(recipe.mEUt) * recipe.mDuration == originalHeat[i], "original Burn charge, independent of Engine map: " + fuels[i]);
            try (var f = new Fixture(h, "burning_box_liquid_lead")) {
                h.assertTrue(f.fluids().fill(fuel, IFluidHandler.FluidAction.EXECUTE) == 32, "native liquid box accepts " + fuels[i]);
                f.ignite();
                f.tick();
                h.assertTrue(f.box.isBurning() && f.fluids().getFluidInTank(0).getAmount() == 31 && f.heat() == originalHeat[i] / 2,
                        "one real liquid fuel charge yields source heat at registered efficiency: " + fuels[i]);
                f.tick();
                h.assertTrue(f.boiler.storedHeat() == f.box.spec().outputRate(), "original engine liquid also heats a real boiler: " + fuels[i]);
            }
        }
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void finalReserveKeepsBurningUntilAnotherRatedPacketCannotBeEmitted(GameTestHelper h) {
        try (var f = new Fixture(h, "burning_box_liquid_titanium")) {
            long rate = f.box.spec().outputRate();
            var data = save(h, f.box);
            data.putBoolean("gt.active", true);
            data.putLong("gt.energy", rate * 2 + rate / 2);
            load(h, f.box, data);
            f.tick();
            h.assertTrue(f.boiler.storedHeat() == rate && f.heat() == rate + rate / 2 && f.box.isBurning(),
                    "failed refill preserves a full remaining output packet");
            f.tick();
            h.assertTrue(f.boiler.storedHeat() == rate * 2 && f.heat() == rate / 2 && !f.box.isBurning(),
                    "last full packet reaches boiler before residual heat becomes insufficient");
            f.tick();
            h.assertTrue(f.boiler.storedHeat() == rate * 2 && f.heat() == rate / 2, "extinguished box does not emit or discard fractional reserve");
        }
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void roundedZeroHeatRecipeCannotConsumeFuelOrSpinRefillLoop(GameTestHelper h) {
        var recipe = new Recipe(null, null, null, null, new FluidStack[]{new FluidStack(Fluids.WATER, 1)}, null, 1, -1, 0);
        h.assertTrue(FuelRecipeMaps.Burn.addRecipe(recipe) == recipe, "isolated one-HU datapack-style row accepts otherwise unused water");
        try (var f = new Fixture(h, "burning_box_liquid_lead")) {
            h.assertTrue(f.box.spec().efficiency() == 5000, "real lead box rounds one-HU recipe to zero");
            h.assertTrue(f.fluids().fill(new FluidStack(Fluids.WATER, 2), IFluidHandler.FluidAction.EXECUTE) == 2, "test row uses actual native tank");
            f.ignite();
            f.tick();
            h.assertTrue(f.fluids().getFluidInTank(0).getAmount() == 2 && f.heat() == 0 && !f.box.isBurning(), "zero useful heat neither consumes fuel nor leaves a burning box");
        } finally {
            FuelRecipeMaps.Burn.unlink(recipe);
        }
        h.succeed();
    }

    private static Recipe coalRecipe(GameTestHelper h, String prefix, int calcite) {
        var input = prefix.equals("blockDust") ? GTBlocks.getStack(BlockMaterialPrefix.blockDust, GTMaterialRegistry.get("Coal"))
                : GTItems.getStack(com.gregtech.gregtech.api.prefix.PrefixRegistry.byName(prefix), GTMaterialRegistry.get("Coal"), 1);
        h.assertTrue(!input.isEmpty(), "registered Coal form " + prefix);
        var recipe = FuelRecipeMaps.FluidBed.findRecipe(List.of(input), List.of(GTFluids.stack("Calcite", calcite)), false, 1, 1);
        h.assertTrue(recipe != null && recipe.mFluidInputs.length == 1 && recipe.mFluidInputs[0].getAmount() == calcite,
                "original FluidBed row and calcite dose for " + prefix);
        return recipe;
    }

    @GameTest(template = EMPTY)
    public static void originalCoalFormsConsumeExactCalciteAndRetainHeatAndAsh(GameTestHelper h) {
        String[] forms = {"blockDust", "dust", "dustSmall", "dustTiny", "dustDiv72"};
        int[] doses = {648, 72, 18, 8, 1};
        for (int i = 0; i < forms.length; i++) {
            var recipe = coalRecipe(h, forms[i], doses[i]);
            try (var f = new Fixture(h, "burning_box_fluidbed_lead")) {
                var fluid = recipe.mFluidInputs[0].copy();
                h.assertTrue(f.fluids().getTankCapacity(0) >= 1000, "lowest-rate native fluidized bed holds the 648 L block charge");
                var before = save(h, f.box);
                h.assertTrue(f.fluids().fill(fluid, IFluidHandler.FluidAction.SIMULATE) == doses[i] && save(h, f.box).equals(before), "calcite simulation preserves native tank and machine state");
                h.assertTrue(f.fluids().fill(fluid, IFluidHandler.FluidAction.EXECUTE) == doses[i], "native tank accepts complete original calcite dose");
                h.assertTrue(f.items().insertItem(0, recipe.mInputs[0].copy(), false).isEmpty(), "real source Coal form enters fuel slot");
                f.ignite();
                f.tick();
                h.assertTrue(f.items().getStackInSlot(0).isEmpty() && f.fluids().getFluidInTank(0).isEmpty(), "exactly one solid charge and calcite dose consumed");
                h.assertTrue(f.heat() + f.boiler.storedHeat() == Math.abs(recipe.mEUt) * recipe.mDuration / 2, "native 50 percent box retains half original recipe heat");
                ItemStack expected = recipe.mOutputs.length == 0 ? ItemStack.EMPTY : recipe.mOutputs[0];
                var ash = f.items().extractItem(1, 64, false);
                h.assertTrue(ItemStack.matches(ash, expected), "ash output matches original solid-fuel residue, including no-ash microcharges");
            }
        }
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void legacyFluidBedTankCapacityRestoresWithoutLosingSavedCalcite(GameTestHelper h) {
        try (var f = new Fixture(h, "burning_box_fluidbed_lead")) {
            h.assertTrue(f.fluids().fill(GTFluids.stack("Calcite", 72), IFluidHandler.FluidAction.EXECUTE) == 72,
                    "native low-rate bed contains an original dust charge of calcite");
            var oldSave = save(h, f.box);
            oldSave.getCompound("gt.tank").putLong("Capacity", 160);
            load(h, f.box, oldSave);
            h.assertTrue(f.fluids().getTankCapacity(0) >= 1000 && f.fluids().getFluidInTank(0).getAmount() == 72,
                    "loading the old persisted 160 L limit raises capacity without removing the saved 72 L");
            h.assertTrue(f.fluids().fill(GTFluids.stack("Calcite", 576), IFluidHandler.FluidAction.EXECUTE) == 576
                            && f.fluids().getFluidInTank(0).getAmount() == 648,
                    "an upgraded loaded tank accepts the remaining calcite for an original block-dust charge");

            var excessSave = save(h, f.box);
            excessSave.getCompound("gt.tank").putLong("Capacity", 160);
            excessSave.getCompound("gt.tank").putLong("Amount", 1001);
            load(h, f.box, excessSave);
            h.assertTrue(f.fluids().getTankCapacity(0) >= 1001 && f.fluids().getFluidInTank(0).getAmount() == 1001,
                    "legacy over-capacity saved fluid is retained while the tank grows to hold it");
            h.assertTrue(f.fluids().drain(1001, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1001,
                    "every saved litre remains available through native fluid extraction");

            oldSave.getCompound("gt.tank").putLong("Capacity", 10000);
            load(h, f.box, oldSave);
            h.assertTrue(f.fluids().getTankCapacity(0) == 10000 && f.fluids().getFluidInTank(0).getAmount() == 72,
                    "the capacity upgrade preserves larger existing tank limits and their contents");
        }
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void fullAshOutputBlocksBothInputsUntilNativeExtractionFreesSpace(GameTestHelper h) {
        var recipe = coalRecipe(h, "dust", 72);
        h.assertTrue(recipe.mOutputs.length == 1 && !recipe.mOutputs[0].isEmpty(), "normal Coal dust has original dark ash output");
        try (var f = new Fixture(h, "burning_box_fluidbed_lead")) {
            h.assertTrue(f.items().insertItem(0, recipe.mInputs[0].copy(), false).isEmpty(), "native insertion stores fuel");
            h.assertTrue(f.fluids().fill(recipe.mFluidInputs[0].copy(), IFluidHandler.FluidAction.EXECUTE) == 72, "native insertion stores calcite");
            var slots = new ItemStackHandler(2);
            slots.setStackInSlot(0, recipe.mInputs[0].copy());
            slots.setStackInSlot(1, recipe.mOutputs[0].copyWithCount(64));
            var data = save(h, f.box);
//? if >=1.21.1 {
/*            data.put("gt.inv", slots.serializeNBT(h.getLevel().registryAccess()));
*///?} else {
            data.put("gt.inv", slots.serializeNBT());
//?}
            load(h, f.box, data);
            f.ignite();
            f.tick();
            h.assertTrue(f.items().getStackInSlot(0).getCount() == 1 && f.fluids().getFluidInTank(0).getAmount() == 72
                    && f.heat() == 0 && f.boiler.storedHeat() == 0, "full residue slot never consumes solid fuel or calcite");
            h.assertTrue(f.items().extractItem(1, 64, false).getCount() == 64, "automation extracts the existing residue");
            f.ignite();
            f.tick();
            h.assertTrue(f.items().getStackInSlot(0).isEmpty() && f.fluids().getFluidInTank(0).isEmpty()
                    && ItemStack.matches(f.items().getStackInSlot(1), recipe.mOutputs[0]), "cleared output resumes one complete original charge without duplication");
        }
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void fuelChargesPreserveStoredFluidAmountsBeyondNativeIntViews(GameTestHelper h) {
        long storedAmount = (long) Integer.MAX_VALUE + 500;
        for (String kind : List.of("liquid", "gas")) {
            try (var f = new Fixture(h, "burning_box_" + kind + "_titanium")) {
                var fluid = GTFluids.stack(kind.equals("gas") ? "Hydrogen" : "Oil_Creosote", 100);
                var map = kind.equals("gas") ? FuelRecipeMaps.Gas : FuelRecipeMaps.Burn;
                var recipe = map.findRecipe(List.of(), List.of(fluid), false, 0, 0);
                h.assertTrue(recipe != null && recipe.mFluidInputs.length == 1,
                        "real source liquid/gas charge exists for long-storage regression");
                long heatPerCharge = Math.abs(recipe.mEUt) * recipe.mDuration * f.box.spec().efficiency() / 10000;
                long rate = f.box.spec().outputRate();
                h.assertTrue(heatPerCharge > 0 && heatPerCharge < rate,
                        "registered weak fuel requires several charges through the bounded int view");
                h.assertTrue(f.fluids().fill(fluid, IFluidHandler.FluidAction.EXECUTE) == 100,
                        "real fluid identity enters the native tank");
                var saved = save(h, f.box);
                saved.getCompound("gt.tank").putLong("Amount", storedAmount);
                load(h, f.box, saved);
                h.assertTrue(save(h, f.box).getCompound("gt.tank").getLong("Amount") == storedAmount
                                && f.fluids().getFluidInTank(0).getAmount() == Integer.MAX_VALUE,
                        "NBT retains long storage while native FluidStack exposes its bounded view");
                long charges = (rate * 2 + heatPerCharge - 1) / heatPerCharge;
                long consumed = charges * recipe.mFluidInputs[0].getAmount();
                f.ignite();
                f.tick();
                h.assertTrue(save(h, f.box).getCompound("gt.tank").getLong("Amount") == storedAmount - consumed,
                        "liquid/gas refilling subtracts only actual charges from the original long amount");
                h.assertTrue(f.box.isBurning() && f.heat() == charges * heatPerCharge && f.boiler.storedHeat() == 0,
                        "all long-storage fuel charges become the exact source heat reserve");
            }
        }
        var recipe = coalRecipe(h, "dust", 72);
        try (var f = new Fixture(h, "burning_box_fluidbed_lead")) {
            h.assertTrue(f.fluids().fill(recipe.mFluidInputs[0].copy(), IFluidHandler.FluidAction.EXECUTE) == 72,
                    "real calcite identity enters the fluidized-bed tank");
            int fuelPerCharge = recipe.mInputs[0].getCount();
            h.assertTrue(f.items().insertItem(0, recipe.mInputs[0].copyWithCount(fuelPerCharge * 2), false).isEmpty(),
                    "two actual coal charges enter the native fuel slot");
            var saved = save(h, f.box);
            saved.getCompound("gt.tank").putLong("Amount", storedAmount);
            load(h, f.box, saved);
            f.ignite();
            f.tick();
            h.assertTrue(save(h, f.box).getCompound("gt.tank").getLong("Amount") == storedAmount - 72
                            && f.items().getStackInSlot(0).getCount() == fuelPerCharge,
                    "one solid charge consumes exactly 72 calcite from the complete long amount");
            h.assertTrue(f.heat() == Math.abs(recipe.mEUt) * recipe.mDuration / 2 && f.boiler.storedHeat() == 0,
                    "one fluidized-bed charge credits only the registered fifty-percent source heat");
            var ash = recipe.mOutputs.length == 0 ? ItemStack.EMPTY : recipe.mOutputs[0];
            h.assertTrue(ItemStack.matches(f.items().getStackInSlot(1), ash),
                    "one fluidized-bed charge produces exactly one source ash batch");
        }
        h.succeed();
    }
}
