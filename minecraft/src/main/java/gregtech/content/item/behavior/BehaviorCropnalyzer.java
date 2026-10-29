/*
 * Adapted from Gregorius Techneticies' Behavior_Cropnalyzer (2019), LGPL-3.0-or-later.
 */
package gregtech.content.item.behavior;

import gregtech.content.tool.OriginalCropScan;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** Native world boundary for the optional crop-analysis contract. */
public final class BehaviorCropnalyzer {
    private BehaviorCropnalyzer() {}

    public static boolean supports(Level level, BlockPos pos) {
        return OriginalCropScan.supports(level.getBlockEntity(pos));
    }

    public static long scan(Level level, BlockPos pos, List<String> lines) {
        var result = OriginalCropScan.scan(
                level.getBlockEntity(pos),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                key -> Component.translatable(key).getString());
        lines.addAll(result.lines());
        return result.cost();
    }
}
