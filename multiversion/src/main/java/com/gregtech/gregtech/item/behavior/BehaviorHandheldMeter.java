package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.blockentity.energy.AxleBlockEntity;
import com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity;
import com.gregtech.gregtech.blockentity.energy.GearboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Original Behavior_Tool electrometer/tachometer readouts; measurement never changes machine state. */
public final class BehaviorHandheldMeter {
    private BehaviorHandheldMeter() {}

    public static ItemBehaviors.Outcome useOn(Level level, BlockPos pos, Player player, ItemStack stack, boolean electric) {
        var target = level.getBlockEntity(pos);
        if (electric && target instanceof ElectricWireBlockEntity wire) {
            send(level, player, Component.translatable("message.gt6ce.meter.electric", wire.lastWattage()));
        } else if (!electric && target instanceof AxleBlockEntity axle) {
            send(level, player, Component.translatable("message.gt6ce.meter.rotary", axle.transferredLast()));
        } else if (!electric && target instanceof GearboxBlockEntity gearbox) {
            for (var side : Direction.values()) {
                if (!gearbox.hasGear(side) && !gearbox.hasAxis(side.getAxis())) continue;
                send(level, player, Component.translatable(gearbox.receivedFrom(side)
                        ? "message.gt6ce.meter.input_side" : "message.gt6ce.meter.output_side",
                        Component.translatable("jade.gt6ce.direction." + side.getName())));
            }
            send(level, player, Component.translatable("message.gt6ce.meter.rotary", gearbox.transferredLast()));
        } else {
            return ItemBehaviors.Outcome.refused(stack);
        }
        return ItemBehaviors.Outcome.acted(stack);
    }

    private static void send(Level level, Player player, Component message) {
        if (!level.isClientSide && player != null) player.displayClientMessage(message, false);
    }
}
