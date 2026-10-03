package com.gregtech.gregtech.content.data;

import com.gregtech.gregtech.api.data.UsbDataPort;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** GT6 USB cable item: resolves a directly adjacent port, optionally pinned to gt.usb.dir. */
public final class UsbDataCable {
    public static final String NBT_DIRECTION = UsbDataRules.DIRECTION;
    private UsbDataCable() {}

    @Nullable
    public static CompoundTag readAdjacent(BlockEntity machine, ItemStack cable, int requestedTier) {
        return readAdjacentMatching(machine, cable, requestedTier, data -> true);
    }
    @Nullable
    public static CompoundTag readAdjacentMatching(BlockEntity machine, ItemStack cable, int requestedTier,
            java.util.function.Predicate<CompoundTag> accepts) {
        if (machine == null || machine.getLevel() == null || machine.isRemoved()
                || UsbDataMedia.cableTier(cable) < requestedTier) return null;
        var level = machine.getLevel();
        CompoundTag tag = UsbDataMedia.tag(cable);
        if (tag != null && tag.contains(NBT_DIRECTION, 99)) {
            int index = tag.getByte(NBT_DIRECTION);
            if (index < 0 || index >= Direction.values().length) return null;
            CompoundTag data = readSide(machine, Direction.values()[index], requestedTier);
            return data != null && accepts.test(data) ? data : null;
        }
        for (Direction side : Direction.values()) {
            CompoundTag data = readSide(machine, side, requestedTier);
            if (data != null && accepts.test(data)) return data;
        }
        return null;
    }

    @Nullable
    private static CompoundTag readSide(BlockEntity machine, Direction side, int tier) {
        var level = machine.getLevel();
        var neighbor = machine.getBlockPos().relative(side);
        if (level == null || !level.hasChunkAt(neighbor)) return null;
        BlockEntity target = level.getBlockEntity(neighbor);
        return target instanceof UsbDataPort port && !target.isRemoved()
                ? port.readUsbData(side.getOpposite(), tier) : null;
    }
}
