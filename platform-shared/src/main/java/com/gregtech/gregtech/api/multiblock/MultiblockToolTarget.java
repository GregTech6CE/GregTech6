/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. */
package com.gregtech.gregtech.api.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.UseOnContext;
import java.util.List;

/** Source tool clicks can reach a controller through an incomplete, still bound shell. */
public interface MultiblockToolTarget {
    boolean containsToolPosition(BlockPos pos);
    long useMultiblockTool(UseOnContext context, List<Component> messages);
}
