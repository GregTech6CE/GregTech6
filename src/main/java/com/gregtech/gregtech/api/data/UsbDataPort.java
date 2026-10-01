package com.gregtech.gregtech.api.data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/** A GT6 USB port exposed to a directly adjacent machine. Data is copied across this boundary. */
public interface UsbDataPort {
    /** Returns the selected file when the medium can serve the requested USB tier. */
    @Nullable CompoundTag readUsbData(Direction side, int requestedTier);

    /** Writes or clears the selected file without removing its medium. */
    boolean writeUsbData(Direction side, int requestedTier, @Nullable CompoundTag data);
}
