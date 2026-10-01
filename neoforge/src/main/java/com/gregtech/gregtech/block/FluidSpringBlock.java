package com.gregtech.gregtech.block;

import com.gregtech.gregtech.blockentity.FluidSpringBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * GT6's fluid spring block ({@code MultiTileEntityFluidSpring}).
 *
 * <p>GT6 places one of these at bedrock where {@code WorldgenFluidSpring} carved a crater; the
 * {@link FluidSpringBlockEntity} then pushes its fluid upwards. Player placed springs work the same
 * way, so a spring head plus a pump is a real (if slow) infinite fluid source.
 */
public class FluidSpringBlock extends Block implements EntityBlock {
    public FluidSpringBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidSpringBlockEntity(pos, state);
    }

    /** GT6 {@code MultiTileEntityFluidSpring.writeItemNBT} keeps the source fluid and amount. */
    private ItemStack packed(@Nullable BlockEntity entity) {
        ItemStack stack = new ItemStack(this);
        if (entity instanceof FluidSpringBlockEntity spring && !spring.fluidId().isEmpty()) {
            CompoundTag data = new CompoundTag();
            data.putString("id", net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(spring.getType()).toString());
            data.putString("spring", spring.fluidId());
            data.putInt("amount", spring.amount());
            stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data));
        }
        return stack;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(packed(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult hit,net.minecraft.world.level.LevelReader level,BlockPos pos,net.minecraft.world.entity.player.Player player) {
        return packed(level.getBlockEntity(pos));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        var packedData=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag data=packedData==null?null:packedData.copyTag();
        if (data == null || !data.contains("spring", Tag.TAG_STRING)
                || !data.contains("amount", Tag.TAG_INT)) return;
        ResourceLocation fluidId = ResourceLocation.tryParse(data.getString("spring"));
        if (fluidId == null || data.getInt("amount") <= 0
                || !BuiltInRegistries.FLUID.containsKey(fluidId)) return;
        if (level.getBlockEntity(pos) instanceof FluidSpringBlockEntity spring)
            spring.setSpring(fluidId.toString(), data.getInt("amount"));
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> {
            if (be instanceof FluidSpringBlockEntity spring) spring.tick();
        };
    }
}
