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
    private final com.gregtech.gregtech.worldgen.FluidSpringRules.Spring spring;
    public FluidSpringBlock(Properties properties) { this(null,properties); }
    public FluidSpringBlock(com.gregtech.gregtech.worldgen.FluidSpringRules.Spring spring,Properties properties) {
        super(properties);this.spring=spring;
    }

    public com.gregtech.gregtech.worldgen.FluidSpringRules.Spring spring() { return spring; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidSpringBlockEntity(pos, state);
    }

    /** A spring's output and rate are part of its registered block identity. */
    private ItemStack packed(@Nullable BlockEntity entity) {
        if(spring==null && entity instanceof FluidSpringBlockEntity be) {
            var variant=com.gregtech.gregtech.registry.GTFluidSprings.byFluid(be.fluidId());
            if(variant!=null)return new ItemStack(variant);
            if(!be.fluidId().isEmpty()) {
                // Preserve an unmapped legacy/external output rather than silently changing it.
                var data=new CompoundTag();data.putString("spring",be.fluidId());
                data.putInt("amount",be.amount());
                data.putString("id",net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString());
                var stack=new ItemStack(this);stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data));return stack;
            }
        }
        return new ItemStack(this);
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
        if(spring!=null)return;
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
