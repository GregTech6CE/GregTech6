package gregtech.content.item.behavior;

import gregtech.engine.machine.ITileEntityCrucible;
import gregtech.content.blockentity.machine.BoilerTankBlockEntity;
import gregtech.content.blockentity.machine.MoldBasinBlockEntity;
import gregtech.content.blockentity.machine.MoldBlockEntity;
import gregtech.content.blockentity.machine.TankBlockEntity;
import gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity;
import gregtech.content.blockentity.energy.ReactorCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** GT6's reusable quicksilver thermometer, reading the target's actual thermal state. */
public final class BehaviorThermometer {

    private BehaviorThermometer() {
    }

    public static ItemBehaviors.Outcome useOn(Level level, BlockPos pos, Player player, ItemStack stack) {
        var blockEntity = level.getBlockEntity(pos);
        Component message;
        if (blockEntity instanceof BoilerTankBlockEntity boiler) {
            message = Component.translatable("jade.gt6ce.boiler.heat", boiler.storedHeat(),
                    boiler.spec().heatCapacity());
        } else if (blockEntity instanceof ReactorCoreBlockEntity reactor) {
            message = Component.translatable("jade.gt6ce.boiler.heat_nocap", reactor.lastHeat);
        } else {
            Long temperature = blockEntity instanceof ITileEntityCrucible crucible ? crucible.getCrucibleTemperature()
            : blockEntity instanceof MoldBlockEntity mold ? mold.getTemperature()
            : blockEntity instanceof MoldBasinBlockEntity basin ? basin.getTemperature()
            : blockEntity instanceof TankBlockEntity tank ? tank.getTemperature()
            : blockEntity instanceof MultiblockTankControllerBlockEntity tank ? fluidTemperature(tank) : null;
            if (temperature == null) return ItemBehaviors.Outcome.refused(stack);
            message = Component.translatable("jade.gt6ce.crucible.temperature", temperature);
        }
        if (!level.isClientSide && player != null) player.displayClientMessage(message, false);
        return ItemBehaviors.Outcome.acted(stack);
    }

    private static long fluidTemperature(MultiblockTankControllerBlockEntity tank) {
        var fluid = tank.fluidHandler().getFluidInTank(0);
        if (fluid.isEmpty()) return 293;
        var fluidEntry = gregtech.content.registry.GTFluids.entryForFluid(fluid.getFluid());
        return fluidEntry == null ? fluid.getFluid().getFluidType().getTemperature(fluid) : fluidEntry.temperature();
    }
}
