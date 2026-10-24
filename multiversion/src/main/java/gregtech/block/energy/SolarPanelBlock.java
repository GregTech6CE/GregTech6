package gregtech.block.energy;

import gregtech.internal.energy.EnergyNodeSpec;
import gregtech.blockentity.energy.EnergyNodeBlockEntity;
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
        // Original solar panels occupy a full cube and cannot retain water.
        builder.add(FACING, ACTIVE);
    }
//? if >=1.21.1 {
    /*@Override public boolean canPlaceLiquid(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.BlockGetter level,
*///?} else {
    @Override public boolean canPlaceLiquid(net.minecraft.world.level.BlockGetter level,
//?}
            BlockPos pos, BlockState state, net.minecraft.world.level.material.Fluid fluid) { return false; }
    @Override public boolean placeLiquid(net.minecraft.world.level.LevelAccessor level, BlockPos pos,
            BlockState state, net.minecraft.world.level.material.FluidState fluid) { return false; }
//? if >=1.21.1 {
    /*@Override public net.minecraft.world.item.ItemStack pickupBlock(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.LevelAccessor level,
*///?} else {
    @Override public net.minecraft.world.item.ItemStack pickupBlock(net.minecraft.world.level.LevelAccessor level,
//?}
            BlockPos pos, BlockState state) { return net.minecraft.world.item.ItemStack.EMPTY; }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                           BlockPos source, boolean moving) {
        super.neighborChanged(state, level, pos, block, source, moving);
        if (level.getBlockEntity(pos) instanceof EnergyNodeBlockEntity node) node.checkSolarSky();
    }
}
