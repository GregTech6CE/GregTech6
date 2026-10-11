/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original converter trinary activity texture selection. */
package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class RotaryConverterBlock extends EnergyNodeBlock {
    public static final IntegerProperty ACTIVITY = IntegerProperty.create("activity", 0, 2);
    public RotaryConverterBlock(EnergyNodeSpec spec, Properties properties) {
        super(spec, properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVITY, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVITY);
    }
}
