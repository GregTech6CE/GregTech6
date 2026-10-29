/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

/**
 * One sensor sample. {@code pos} is the sensor block, {@code target} the block in front of it,
 * and {@code side} the world face of the target the sensor reads.
 * The target is null when the block in front is missing or has no block entity.
 */
public record SensorContext(Level level, BlockPos pos, @Nullable BlockEntity target, Direction side) {}
