package gregtech.content.block.energy;

import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import gregtech.engine.GTWaterloggable;
import gregtech.content.energy.PowerCellSpec;
import gregtech.content.blockentity.energy.PowerCellBlockEntity;
import gregtech.content.item.PowerCellItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.List;

public final class PowerCellBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
//? if >=1.21.1 {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        return gregtech.engine.tool.ToolInteractions.toItemResult(use(state, level, pos, player, hand, hit));
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.phys.BlockHitResult hit) {
        return use(state, level, pos, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
    }
*///?} else {
    @Override
//?}
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        var tool=player.getItemInHand(hand);
        if(!gregtech.engine.tool.GTToolHelper.matchesTool(tool,gregtech.engine.tool.GTToolType.MAGNIFYING_GLASS))
            return net.minecraft.world.InteractionResult.PASS;
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof PowerCellBlockEntity be) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tooltip.gt6ce.electric_tool.energy",be.stored(),spec.capacity()),true);
            gregtech.engine.tool.GTToolHelper.damageForToolClickReturn(tool,1,player,
                    hand==net.minecraft.world.InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static final net.minecraft.world.level.block.state.properties.IntegerProperty CHARGE_LEVEL =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("charge", 0, 15);
    private final PowerCellSpec spec;
    private final VoxelShape shape;
    public PowerCellBlock(PowerCellSpec spec){
        super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(0.5f,1f).sound(SoundType.METAL).noOcclusion().lightLevel(state -> state.getValue(CHARGE_LEVEL)));
        this.spec=spec;shape=Block.box(spec.inset(),0,spec.inset(),16-spec.inset(),spec.height(),16-spec.inset());
        registerDefaultState(defaultBlockState().setValue(CHARGE_LEVEL, spec.filled() ? 15 : 0).setValue(GTWaterloggable.WATERLOGGED, false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GTWaterloggable.WATERLOGGED); builder.add(CHARGE_LEVEL); }
    public PowerCellSpec spec(){return spec;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return shape;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new PowerCellBlockEntity(p,s);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,@javax.annotation.Nullable LivingEntity player,ItemStack stack){
        if(l.getBlockEntity(p) instanceof PowerCellBlockEntity be&&stack.getItem() instanceof PowerCellItem item)be.setCharge(item.stored(stack));
    }
    @Override public List<ItemStack> getDrops(BlockState s,LootParams.Builder loot){
        if (loot.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PowerCellBlockEntity be) return List.of(be.asItem());
        return List.of(new ItemStack(this));
    }
//? if >=1.21.1 {
    /*@Override public ItemStack getCloneItemStack(BlockState s,HitResult target,LevelReader l,BlockPos p,Player player){
*///?} else {
    @Override public ItemStack getCloneItemStack(BlockState s,HitResult target,BlockGetter l,BlockPos p,Player player){
//?}
        return l.getBlockEntity(p) instanceof PowerCellBlockEntity be?be.asItem():new ItemStack(this);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return GTWaterloggable.getStateForPlacement(super.getStateForPlacement(context), context);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }
}
