package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.blockentity.energy.PlacedReactorRodBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.*;
import java.util.List;

/** Standalone GT6 rod, 4 x 16 x 4 pixels; only a core performs neutron simulation. */
public final class ReactorRodBlock extends Block implements EntityBlock {
    @Override public com.mojang.serialization.MapCodec<? extends Block> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    private static final VoxelShape SHAPE = box(6, 0, 6, 10, 16, 10);
    public ReactorRodBlock() { super(Properties.of().strength(10,10).noOcclusion().sound(SoundType.METAL)); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PlacedReactorRodBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        if (level.getBlockEntity(pos) instanceof PlacedReactorRodBlockEntity rod) rod.setRod(stack);
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PlacedReactorRodBlockEntity rod)
            return List.of(rod.rod());
        return List.of(new ItemStack(this));
    }
    @Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult hit,LevelReader level,BlockPos pos,net.minecraft.world.entity.player.Player player) {
        return level.getBlockEntity(pos) instanceof PlacedReactorRodBlockEntity rod ? rod.rod() : new ItemStack(this);
    }
}
