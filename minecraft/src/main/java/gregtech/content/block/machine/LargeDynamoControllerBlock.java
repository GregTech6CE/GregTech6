package gregtech.content.block.machine;

import gregtech.content.blockentity.machine.LargeDynamoControllerBlockEntity;
import gregtech.content.multiblock.OriginalGeneratorParameters;
import gregtech.content.multiblock.OriginalGeneratorParameters.AxialGrade;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public class LargeDynamoControllerBlock extends AxialGeneratorBlock {

    public LargeDynamoControllerBlock(Properties properties) {
        this(OriginalGeneratorParameters.DYNAMO.get(0), properties);
    }

    public LargeDynamoControllerBlock(AxialGrade grade, Properties properties) {
        super(grade, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeDynamoControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != gregtech.content.registry.GTBlockEntities.LARGE_DYNAMO.get()) return null;
        return (tickLevel, tickPos, tickState, blockEntity) -> ((LargeDynamoControllerBlockEntity) blockEntity).tick();
    }
}
