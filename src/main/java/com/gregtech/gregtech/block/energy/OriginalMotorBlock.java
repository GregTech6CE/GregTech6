/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from MultiTileEntityMotorElectric/Flux.onWalkOver2 and TileEntityBase11Motor. */
package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class OriginalMotorBlock extends RotaryConverterBlock {
    public static final BooleanProperty COUNTER_CLOCKWISE = BooleanProperty.create("counter_clockwise");
    public static final BooleanProperty FAST = BooleanProperty.create("fast");
    public OriginalMotorBlock(EnergyNodeSpec spec, Properties properties) {
        super(spec, properties);
        registerDefaultState(defaultBlockState().setValue(COUNTER_CLOCKWISE, false).setValue(FAST, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COUNTER_CLOCKWISE, FAST);
    }
    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMonkeyWrench(tool) ? null : super.toolInteraction(state, tool);
    }
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (state.getValue(FACING) == Direction.UP && state.getValue(ACTIVITY) > 0 && entity instanceof LivingEntity living) {
            float angle = (state.getValue(COUNTER_CLOCKWISE) ? -5 : 5) * (state.getValue(FAST) ? 2 : 1);
            living.setYRot(living.getYRot() + angle);
            living.yHeadRot += angle;
        }
        super.stepOn(level, pos, state, entity);
    }
}
