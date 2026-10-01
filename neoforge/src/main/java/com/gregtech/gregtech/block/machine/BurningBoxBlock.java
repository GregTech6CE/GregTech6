package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.BurningBoxFuelType;
import com.gregtech.gregtech.api.machine.MachineSpec;
import com.gregtech.gregtech.blockentity.machine.BurningBoxBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.util.GTEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Generic GT6 burning box block for liquid, gas, and fluidized-bed fuel types. */
public class BurningBoxBlock extends GTFacingMachineBlock implements EntityBlock {
    private static final VoxelShape COLLISION_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.875D, 1.0D);

    private final BurningBoxFuelType fuelType;

    @Override public com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec(){return com.mojang.serialization.MapCodec.unit(this);}

    public BurningBoxBlock(BurningBoxFuelType fuelType, MachineSpec spec, Properties properties) {
        super(spec, properties);
        this.fuelType = fuelType;
    }

    public BurningBoxFuelType fuelType() { return fuelType; }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BurningBoxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != GTBlockEntities.BURNING_BOX.get()) {
            return null;
        }
        BlockEntityTicker<BurningBoxBlockEntity> ticker = BurningBoxBlockEntity::serverTick;
        return (BlockEntityTicker<T>) ticker;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BurningBoxBlockEntity burningBox) {
                burningBox.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BurningBoxBlockEntity burningBox) {
            InteractionResult tool = burningBox.handleToolUse(player, hand, hit);
            if (tool != InteractionResult.PASS) {
                return tool;
            }
            InteractionResult result = burningBox.handleUse(player, hand, hit);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        InteractionResult result=interact(state,level,pos,player,hand,hit);
        return result==InteractionResult.PASS?net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION:net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){return java.util.List.of(new net.minecraft.world.item.ItemStack(this));}

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BurningBoxBlockEntity burningBox && burningBox.isBurning()) {
                GTEntityHelper.applyHeatDamage(entity, getHeatContactDamage(burningBox));
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BurningBoxBlockEntity burningBox && burningBox.isBurning()) {
            GTEntityHelper.applyHeatDamage(entity, getHeatContactDamage(burningBox));
        }
    }

    private float getHeatContactDamage(BurningBoxBlockEntity be) {
        return Math.min(10.0F, be.spec().outputRate() / 10.0F);
    }
}
