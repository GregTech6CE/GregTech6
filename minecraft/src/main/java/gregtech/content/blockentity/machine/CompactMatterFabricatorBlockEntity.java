package gregtech.content.blockentity.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** GT6 20411-20415. Efficiency, cheap overclocking, and constant energy are on the spec. */
public final class CompactMatterFabricatorBlockEntity extends BasicMachineBlockEntity {
    public CompactMatterFabricatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
