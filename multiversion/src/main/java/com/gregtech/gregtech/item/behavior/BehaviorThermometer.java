package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.api.machine.ITileEntityCrucible;
import com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MoldBasinBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MoldBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MultiblockTankControllerBlockEntity;
import com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** GT6's reusable quicksilver thermometer, reading the target's actual thermal state. */
public final class BehaviorThermometer {
    private BehaviorThermometer() {}

    public static ItemBehaviors.Outcome useOn(Level level, BlockPos pos, Player player, ItemStack stack) {
        var target = level.getBlockEntity(pos);
        Component message;
        if (target instanceof BoilerTankBlockEntity boiler) {
            message = Component.translatable("jade.gt6ce.boiler.heat", boiler.storedHeat(),
                    boiler.spec().heatCapacity());
        } else if (target instanceof ReactorCoreBlockEntity reactor) {
            message = Component.translatable("jade.gt6ce.boiler.heat_nocap", reactor.lastHeat);
        } else {
            Long temperature = target instanceof ITileEntityCrucible crucible ? crucible.getCrucibleTemperature()
                    : target instanceof MoldBlockEntity mold ? mold.getTemperature()
                    : target instanceof MoldBasinBlockEntity basin ? basin.getTemperature()
                    : target instanceof TankBlockEntity tank ? tank.getTemperature()
                    : target instanceof MultiblockTankControllerBlockEntity tank ? fluidTemperature(tank) : null;
            if (temperature == null) return ItemBehaviors.Outcome.refused(stack);
            message = Component.translatable("jade.gt6ce.crucible.temperature", temperature);
        }
        if (!level.isClientSide && player != null) player.displayClientMessage(message, false);
        return ItemBehaviors.Outcome.acted(stack);
    }

    private static long fluidTemperature(MultiblockTankControllerBlockEntity tank) {
        var fluid = tank.fluidHandler().getFluidInTank(0);
        if (fluid.isEmpty()) return 293;
        var entry = com.gregtech.gregtech.registry.GTFluids.entryForFluid(fluid.getFluid());
        return entry == null ? fluid.getFluid().getFluidType().getTemperature(fluid) : entry.temperature();
    }
}
