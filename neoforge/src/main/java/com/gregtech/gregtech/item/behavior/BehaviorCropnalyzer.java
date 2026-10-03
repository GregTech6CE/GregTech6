/*
 * Adapted from Gregorius Techneticies' Behavior_Cropnalyzer (2019), LGPL-3.0-or-later.
 */
package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.content.tool.OriginalCropScan;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import java.util.List;

/** Native world boundary for the optional original IC2 crop contract. */
public final class BehaviorCropnalyzer {
    private BehaviorCropnalyzer() {}
    public static boolean supports(Level level, BlockPos pos) {
        return OriginalCropScan.supports(level.getBlockEntity(pos));
    }
    public static long scan(Level level, BlockPos pos, List<String> lines) {
        var result = OriginalCropScan.scan(level.getBlockEntity(pos), pos.getX(), pos.getY(), pos.getZ(),
                key -> Component.translatable(key).getString());
        lines.addAll(result.lines());
        return result.cost();
    }
}

