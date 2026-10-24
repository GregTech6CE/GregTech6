package gregtech.block.inventory;

import gregtech.api.material.GTMaterialRegistry;
import gregtech.content.storage.ItemBarrelCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/** GT6's wooden mass storage shares all inventory, tool, hopper and packed-drop behavior. */
public final class ItemBarrelBlock extends MassStorageBlock {
    private final ItemBarrelCatalog.Spec spec;
    public ItemBarrelBlock(ItemBarrelCatalog.Spec spec) {
        super(GTMaterialRegistry.get(spec.material()), Properties.of().strength(spec.hardness(), spec.hardness()).sound(SoundType.WOOD));
        this.spec = spec;
    }
    public ItemBarrelCatalog.Spec spec() { return spec; }
    @Override public long capacity() { return spec.capacity(); }
    @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return spec.flammability(); }
}
