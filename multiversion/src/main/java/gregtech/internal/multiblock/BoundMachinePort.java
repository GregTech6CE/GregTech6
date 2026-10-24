package gregtech.internal.multiblock;
import net.minecraft.core.BlockPos;
/** A port must not feed its own controller through ordinary adjacent machine traffic. */
public interface BoundMachinePort { boolean isBoundTo(BlockPos controller); }
