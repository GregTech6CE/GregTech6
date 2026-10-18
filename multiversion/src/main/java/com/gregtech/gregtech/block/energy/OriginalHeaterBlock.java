/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from MultiTileEntityHeaterElectric/Flux collision and contact damage. */
package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.content.energy.OriginalThermalConverter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OriginalHeaterBlock extends RotaryConverterBlock {
    public OriginalHeaterBlock(EnergyNodeSpec spec, Properties properties) { super(spec, properties); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> Block.box(0, 2, 0, 16, 16, 16);
            case UP -> Block.box(0, 0, 0, 16, 14, 16);
            case NORTH -> Block.box(0, 0, 2, 16, 16, 16);
            case SOUTH -> Block.box(0, 0, 0, 16, 16, 14);
            case WEST -> Block.box(2, 0, 0, 16, 16, 16);
            case EAST -> Block.box(0, 0, 0, 14, 16, 16);
        };
    }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && state.getValue(ACTIVITY) > 0)
            com.gregtech.gregtech.util.GTEntityHelper.applyHeatDamage(entity, OriginalThermalConverter.contactDamage(spec()));
        super.entityInside(state, level, pos, entity);
    }
}
