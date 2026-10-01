package com.gregtech.gregtech.block.tool;

import com.gregtech.gregtech.blockentity.tool.DynamiteBlockEntity;
import com.gregtech.gregtech.item.behavior.RemoteActivatable;
import com.gregtech.gregtech.content.tool.DynamiteExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import javax.annotation.Nullable;
import java.util.List;

/** GT6 mining charges: fixed 3x3x3 blast, material-resistance threshold and a cancellable fuse. */
public class DynamiteBlock extends DirectionalBlock implements RemoteActivatable, EntityBlock {
    @Override public com.mojang.serialization.MapCodec<DynamiteBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    public static final BooleanProperty ARMED = BooleanProperty.create("armed");
    public static final BooleanProperty SUNK = BooleanProperty.create("sunk");
    private final float maxResistance;
    private final int fortune;
    private final int tintRgb;
    public DynamiteBlock(Properties properties, float maxResistance, int fortune, int tintRgb) {
        super(properties.noOcclusion());
        this.maxResistance = maxResistance;
        this.fortune = fortune;
        this.tintRgb = tintRgb;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ARMED,false).setValue(SUNK,false));
    }
    public float maxExplosionResistance() { return maxResistance; }
    public int fortune() { return fortune; }
    public int tintRgb() { return tintRgb; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING,ARMED,SUNK); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING,ctx.getClickedFace()); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DynamiteBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l,p,s,be) -> { if(be instanceof DynamiteBlockEntity charge) charge.serverTick(); };
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        if (state.getValue(SUNK)) return switch (facing) {
            case NORTH -> box(5,5,14,11,11,16); case SOUTH -> box(5,5,0,11,11,2);
            case WEST -> box(14,5,5,16,11,11); case EAST -> box(0,5,5,2,11,11);
            case DOWN -> box(5,14,5,11,16,11); case UP -> box(5,0,5,11,2,11);
        };
        return switch(facing.getAxis()) {
            case X -> box(0,5,5,16,11,11); case Y -> box(5,0,5,11,16,11); case Z -> box(5,5,0,11,11,16);
        };
    }
    private InteractionResult originalUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held=player.getItemInHand(hand);
        if (!(held.is(Items.FLINT_AND_STEEL)||held.is(Items.FIRE_CHARGE))) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DynamiteBlockEntity charge
                && charge.onIgnite(level,pos,hit.getDirection(),player,held,player.isShiftKeyDown(),0,0,0)>0
                && !player.isCreative()) {
            if(held.is(Items.FIRE_CHARGE)) held.shrink(1);
            else held.hurtAndBreak(1,player,hand==InteractionHand.OFF_HAND?net.minecraft.world.entity.EquipmentSlot.OFFHAND:net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return switch(originalUse(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME,CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME;case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    /** Original mSunk is retained only while the back is drillable stone/ore. */
    public void refreshSupport(Level level, BlockPos pos) {
        var state=level.getBlockState(pos);
        if(!state.is(this)||!state.getValue(SUNK)) return;
        var support=pos.relative(state.getValue(FACING).getOpposite());
        boolean drillable=com.gregtech.gregtech.content.tool.DynamiteSubstrates.canDrill(level,support);
        if(!drillable) level.setBlock(pos,state.setValue(SUNK,false),Block.UPDATE_ALL);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if(!level.isClientSide) {
            refreshSupport(level,pos);
            checkFuseInputs(level,pos);
        }
    }
    public void checkFuseInputs(Level level, BlockPos pos) {
        if(level.getBlockEntity(pos) instanceof DynamiteBlockEntity charge) {
            boolean burning=false;
            for(var side:Direction.values()) if(level.getBlockState(pos.relative(side)).is(net.minecraft.tags.BlockTags.FIRE)) burning=true;
            if(burning || charge.remainingTicks()==0 && level.hasNeighborSignal(pos)) charge.armRemote();
        }
    }
    @Override public void onCaughtFire(BlockState state, Level level, BlockPos pos, Direction face, net.minecraft.world.entity.LivingEntity igniter) {
        remoteActivate(level,pos);
    }
    // Vanilla FireBlock removes flammable blocks immediately after onCaughtFire. GT6 instead
    // keeps the charge for its fuse: nearby fire is handled by checkFuseInputs, not vanilla burning.
    @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return 0; }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return state.getValue(ARMED)?15:0; }
    @Override public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return getSignal(state,level,pos,side); }
    @Override public boolean remoteActivate(Level level, BlockPos pos) {
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof DynamiteBlockEntity charge) charge.armRemote();
        return false;
    }
    public void detonate(Level level, BlockPos pos) {
        var state=level.getBlockState(pos);
        if(level.isClientSide || !state.is(this)) return;
        var center=state.getValue(SUNK)?pos.relative(state.getValue(FACING).getOpposite()):pos;
        level.removeBlock(pos,false);
        DynamiteExplosion.detonate((net.minecraft.server.level.ServerLevel)level,center,maxResistance,fortune);
    }
    @Override public boolean dropFromExplosion(Explosion explosion) { return false; }
    @Override public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) { detonate(level,pos); }
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gregtech.dynamite.resistance",maxResistance));
        tooltip.add(Component.translatable("tooltip.gregtech.dynamite.range"));
        tooltip.add(Component.translatable("tooltip.gregtech.dynamite.fortune",fortune));
        tooltip.add(Component.translatable("gt.tooltip.dynamite.1").withStyle(net.minecraft.ChatFormatting.RED));
    }
}
