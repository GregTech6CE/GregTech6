package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Original full-cube solar panel, with separate generating and emitting states. */
public final class SolarPanelBlock extends EnergyNodeBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("solar_active");
    public SolarPanelBlock(EnergyNodeSpec spec, Properties properties) {
        super(spec, properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                           BlockPos source, boolean moving) {
        super.neighborChanged(state, level, pos, block, source, moving);
        if (level.getBlockEntity(pos) instanceof EnergyNodeBlockEntity node) node.checkSolarSky();
    }
}
