package gregtech.content.block.tool;

import gregtech.engine.fluid.PortableFluidContainerSpec;
import gregtech.content.blockentity.tool.PortableContainerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.List;

/** Placeable vessel; the same item fluid data survives placement and removal. */
public final class PortableContainerBlock extends Block implements EntityBlock {
//? if >=1.21.1 {
    /*@Override public com.mojang.serialization.MapCodec<PortableContainerBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
*///?} else {
//?}
    private final PortableFluidContainerSpec spec;
    private final VoxelShape shape;
    public PortableContainerBlock(PortableFluidContainerSpec spec, Properties properties) {
        super(properties.noOcclusion());
        this.spec = spec;
        VoxelShape result = Shapes.empty();
        for (int[] b : gregtech.content.tool.PortableContainerShapes.bounds(spec.shapeId()))
            result = Shapes.or(result, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        shape = spec.shapeId().equals("cell")?Block.box(5,0,5,11,12,11):result.optimize();
    }
    public PortableFluidContainerSpec spec() { return spec; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shape; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PortableContainerBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof PortableContainerBlockEntity vessel) vessel.setContents(stack);
    }
//? if >=1.21.1 {
    /*private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
*///?} else {
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
//?}
        if (level.getBlockEntity(pos) instanceof PortableContainerBlockEntity vessel) {
            if(player.getItemInHand(hand).isEmpty()&&hit.getDirection().getAxis().isHorizontal()
                    &&gregtech.content.tool.PortableContainerLimits.adjustable(spec)){
                if(!level.isClientSide){
                    int limit=vessel.adjustLimit(hit.getLocation().y-pos.getY(),player.isShiftKeyDown());
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.gt6ce.portable_fluid.limit",limit),true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (vessel.interact(player, hand, hit.getDirection())) return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
//? if >=1.21.1 {
    /*@Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME->net.minecraft.world.ItemInteractionResult.CONSUME;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
*///?} else {
//?}
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return List.of(be instanceof PortableContainerBlockEntity vessel ? vessel.contents() : new ItemStack(this));
    }
//? if >=1.21.1 {
    /*@Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult target,LevelReader level,BlockPos pos,Player player) {
*///?} else {
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
//?}
        return level.getBlockEntity(pos) instanceof PortableContainerBlockEntity vessel ? vessel.contents() : new ItemStack(this);
    }
}
