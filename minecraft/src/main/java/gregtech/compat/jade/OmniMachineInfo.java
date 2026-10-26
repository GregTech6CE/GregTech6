package gregtech.compat.jade;

import gregtech.content.blockentity.machine.*;
import gregtech.content.blockentity.tool.*;
import gregtech.content.blockentity.energy.ReactorCoreBlockEntity;
import gregtech.engine.machine.ITileEntityMold;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Workshop contents and processing observations from GregTech6.xml, using native state only. */
final class OmniMachineInfo {
    private OmniMachineInfo() {}

    static void append(JadeInspection info, BlockEntity entity, snownee.jade.api.BlockAccessor accessor) {
        if (entity instanceof BasicMachineBlockEntity machine) {
            info.groupedInventory();
            var control = machine.machineControl(null);
            info.state(control.enabled(), control.active());
            if (control.progressMax() > 0) info.line("progress", control.progress(), control.progressMax(),
                    gregtech.content.energy.BoilerGauge.percent(control.progress(), control.progressMax()));
            if (machine.minimumEnergy() > 0 && machine.spec() != null)
                info.line("recipe_energy", machine.minimumEnergy(), machine.spec().energyType(), machine.parallelCount());
            var pending = machine.inspectionPendingItems();
            for (int i = 0; i < pending.size(); i++) info.item("pending", i, pending.get(i));
            int remaining = machine.inspectionPendingItemCount() - pending.size();
            if (remaining > 0) info.line("more_pending", remaining);
            var fluids = machine.inspectionPendingFluids();
            for (int i = 0; i < fluids.size(); i++) {
                var fluid = fluids.get(i);
                info.fluid("pending", i, fluid, fluid.getAmount(), fluid.getAmount());
            }
            info.inventory("input", machine.inventory(), 0, machine.inputSlots());
            info.inventory("output", machine.inventory(), machine.inputSlots(), machine.outputSlots());
            info.inventory("program", machine.autocraftingProgram(), 0, 1);
            info.tanks("input", machine.getTanksInput());
            info.tanks("output", machine.getTanksOutput());
            if (machine instanceof CokeOvenControllerBlockEntity coke && !control.active() && control.progressMax() == 0) {
                if (!coke.ignitionReady()) info.line("needs_ignition");
                info.line("coke_recipe_hint");
            }
        } else if (entity instanceof MaterialAnvilBlockEntity anvil) {
            info.line("anvil_hint");
            info.line("durability", anvil.remainingDurability());
            var facing = entity.getBlockState().getValue(gregtech.content.block.tool.MaterialAnvilBlock.FACING);
            var hit = accessor.getHitResult();
            double half = facing.getAxis() == Direction.Axis.Z ? hit.getLocation().x - entity.getBlockPos().getX()
                    : hit.getLocation().z - entity.getBlockPos().getZ();
            var surface = gregtech.content.tool.AnvilRules.surface(facing, hit.getDirection(), half);
            info.line("anvil_surface", JadeInspection.label("anvil_" + surface.name().toLowerCase(java.util.Locale.ROOT)));
            info.item("input", 0, anvil.workpiece(0));
            info.item("input", 1, anvil.workpiece(1));
        } else if (entity instanceof ManualToolBlockEntity tool) {
            if (tool.kind() == ManualToolBlockEntity.Kind.GRINDSTONE) {
                info.line("abrasive", tool.stoneUses());
                info.line("manual_progress", tool.workProgress());
            } else if (tool.kind() == ManualToolBlockEntity.Kind.SIFTING) {
                info.item("input", 0, tool.work());
                var outputs = tool.outputSnapshot();
                boolean blocked = outputs.stream().anyMatch(stack -> !stack.isEmpty());
                info.line("state", JadeInspection.label(blocked ? "collect_outputs" : tool.work().isEmpty() ? "waiting" : "manual_work"));
                for (int i = 0; i < outputs.size(); i++) info.item("output", i, outputs.get(i));
            }
        } else if (entity instanceof ProcessingToolBlockEntity tool) {
            info.groupedInventory();
            int inputs = tool.recipes().mInputItemsCount;
            info.inventory("input", tool.inventory(), 0, inputs);
            info.inventory("output", tool.inventory(), inputs, tool.recipes().mOutputItemsCount);
            info.tanks("input", tool.inputTanksForInspection());
            info.tanks("output", tool.outputTanksForInspection());
        } else if (entity instanceof PortableContainerBlockEntity vessel) {
            // Resolve the item capability on a copy; reading never changes the placed vessel.
//? if >=1.21.1 {
            /*var handler = net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(vessel.contents()).orElse(null);
*///?} else {
            var handler = net.minecraftforge.fluids.FluidUtil.getFluidHandler(vessel.contents()).orElse(null);
//?}
            info.tanks("contents", handler);
        } else if (entity instanceof CrucibleFaucetBlockEntity faucet) {
            info.flag("auto_output", faucet.isAutoPull());
        } else if (entity instanceof MoldBlockEntity mold) {
            moldSides(info, mold, mold.getAutoPullDirections());
            info.flag("redstone_required", mold.isUseRedstone());
        } else if (entity instanceof MoldBasinBlockEntity basin) {
            moldSides(info, basin, basin.getAutoPullDirections());
        } else if (entity instanceof DustFunnelBlockEntity funnel) {
            info.item("input", 0, funnel.inputForInspection());
            info.item("prepared", 0, funnel.preparedForInspection());
            info.item("output", 0, funnel.outputForInspection());
        } else if (entity instanceof ReactorCoreBlockEntity reactor) {
            for (int i = 0; i < reactor.rods.length; i++) if (!reactor.rods[i].isEmpty()) {
                info.item("rod", i, reactor.rods[i]);
                info.line("rod_neutrons", i + 1, reactor.neutrons[i]);
            }
        } else if (entity instanceof HeatExchangerBlockEntity heat) {
            info.state(true, heat.isConverting());
            info.line("buffer", heat.heatStored(), heat.heatCapacity(), "HU");
            info.line("rated_output", heat.settings().rate(), "HU");
            for (int i = 0; i < heat.getTanks(); i++)
                info.fluid(i == 0 ? "input" : "output", 0, heat.getFluidInTank(i), heat.tankAmount(i), heat.getTankCapacity(i));
        } else if (entity instanceof BurningBoxBlockEntity box) {
            info.state(true, box.isBurning());
            info.line("fuel_energy", box.remainingFuelEnergy());
            long rate = box.spec().outputRate();
            if (rate > 0) info.line("fuel_ticks", box.remainingFuelEnergy() / rate);
            info.line("rated_output", rate, "HU");
            info.item("fuel", 0, box.fuelForInspection());
            info.item("ash", 0, box.ashForInspection());
            info.item("pending", 0, box.pendingAshForInspection());
            var tank = box.fuelTankForInspection();
            if (tank != null) info.fluid("fuel", 0, tank.getFluid(), tank.getAmount(), tank.getCapacity());
        } else if (entity instanceof SolidBurningBoxBlockEntity box) {
            info.state(true, box.isBurning());
            long rate = box.getEnergySizeOutputRecommended(gregtech.content.data.GregTechTags.Energy.HU, Direction.UP);
            if (rate > 0) info.line("fuel_ticks", box.getStoredHeat() / rate);
            info.line("rated_output", rate, "HU");
        }
    }

    private static void moldSides(JadeInspection info, ITileEntityMold mold, int autoMask) {
        // Current molds expose input faces and an auto-pull mask, not the old saved connection field.
        for (Direction side : Direction.values()) if (mold.isMoldInputSide(side.ordinal()))
            info.line("mold_side", new JadeInspection.Label("jade.gt6ce.direction." + side.getSerializedName()),
                    JadeInspection.label((autoMask & (1 << side.ordinal())) != 0 ? "automatic" : "manual"));
    }
}
