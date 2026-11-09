package gregtech.content.registry;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Common typed access to a single block entity type registration. */
public record RegisteredBlockEntity<T extends BlockEntity>(ResourceLocation getId, Supplier<BlockEntityType<T>> value)
        implements Supplier<BlockEntityType<T>> {
    @Override
    public BlockEntityType<T> get() {
        return value.get();
    }
}
