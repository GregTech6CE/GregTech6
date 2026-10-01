package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.MachineSpec;
import com.gregtech.gregtech.blockentity.machine.SolidBurningBoxBlockEntity;
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
/** Metal solid burning box shell (GT6 {@code MultiTileEntityGeneratorMetal}). */
public class SolidBurningBoxBlock extends GTFacingMachineBlock implements EntityBlock {
    /** GT6 collision height {@code box(0,0,0,1,0.875,1)} — enables {@code entityInside} contact damage. */
    private static final VoxelShape COLLISION_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.875D, 1.0D);

    public SolidBurningBoxBlock(MachineSpec spec, Properties properties) {
        super(spec, properties);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolidBurningBoxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != GTBlockEntities.SOLID_BURNING_BOX.get()) {
            return null;
        }
        BlockEntityTicker<SolidBurningBoxBlockEntity> ticker = SolidBurningBoxBlockEntity::serverTick;
        return (BlockEntityTicker<T>) ticker;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SolidBurningBoxBlockEntity burningBox) {
                burningBox.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SolidBurningBoxBlockEntity burningBox) {
            InteractionResult tool = burningBox.handleToolUse(player, hand, hit);
            if (tool != InteractionResult.PASS) {
                return tool;
            }
            InteractionResult result = burningBox.handleUse(player, hand, hit);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SolidBurningBoxBlockEntity burningBox && burningBox.isBurning()) {
                GTEntityHelper.applyHeatDamage(entity, burningBox.getHeatContactDamage());
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) {
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SolidBurningBoxBlockEntity burningBox && burningBox.isBurning()) {
            GTEntityHelper.applyHeatDamage(entity, burningBox.getHeatContactDamage());
        }
    }
}
