package com.gregtech.gregtech.block.wood;

import com.gregtech.gregtech.blockentity.TreeHoleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * GT6's tree holes: {@code MultiTileEntityResinHoleRubber}, {@code MultiTileEntitySapHoleMaple} and
 * {@code MultiTileEntitySapHoleRainbowood} (registry ids 32762/32761/32760).
 *
 * <p>A hole sits in a tree trunk, faces the side a player drilled, and slowly fills with resin/sap:
 * every 600 ticks the {@link TreeHoleBlockEntity} counts the leaves of the tree it belongs to and
 * rolls GT6's probability — so a chopped-down or sparse tree stops producing, exactly like GT6.
 * Right-clicking a filled hole hands out the resin item (rubber) or fills the held container with
 * the sap fluid (maple/rainbowood); breaking it drops the log it was cut into.
 */
public class TreeHoleBlock extends HorizontalDirectionalBlock implements EntityBlock {
    @Override public com.mojang.serialization.MapCodec<TreeHoleBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** GT6's {@code mHasResin} — the hole is full and ready to be tapped. */
    public static final BooleanProperty RESIN = BooleanProperty.create("resin");

    private final WoodSpecies species;
    /** {@code item:gregtech:rubber_resin} for rubber, {@code fluid:<name>} for the sap holes. */
    private final String yield;

    public TreeHoleBlock(WoodSpecies species, String yield, Properties properties) {
        super(properties);
        this.species = species;
        this.yield = yield;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RESIN, false));
    }

    public WoodSpecies species() { return species; }

    /** GT6 {@code getResinItem}/{@code getResinFluid}: {@code item:<id>} or {@code fluid:<registry name>}. */
    public String yield() { return yield; }

    public boolean yieldsItem() { return yield.startsWith("item:"); }

    public ItemStack resinItem() {
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                ResourceLocation.parse(yield.substring("item:".length()))));
    }

    public FluidStack resinFluid() {
        var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(ResourceLocation.parse(yield.substring("fluid:".length())));
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, 250);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RESIN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TreeHoleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> {
            if (be instanceof TreeHoleBlockEntity hole) hole.tick();
        };
    }

    /** GT6 {@code onBlockActivated3}: hand out the resin, or fill the container the player holds. */
    private InteractionResult originalUse(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || hit.getDirection() != state.getValue(FACING)
                // Modern portable vessels are BlockItems too; retain their container interaction.
                || player.getItemInHand(hand).getItem() instanceof net.minecraft.world.item.BlockItem
                    && !FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()
                || !state.getValue(RESIN)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (yieldsItem()) {
            ItemStack resin = resinItem();
            if (resin.isEmpty()) return InteractionResult.PASS;
            if (!level.setBlock(pos, state.setValue(RESIN, false), 3)) return InteractionResult.PASS;
            com.gregtech.gregtech.api.fluid.HandContainerTransfer.give(player, resin);
        } else {
            var filled = com.gregtech.gregtech.api.fluid.HandContainerTransfer.prepare(player.getItemInHand(hand), resinFluid(), true);
            if (filled == null) return InteractionResult.PASS;
            if (!level.setBlock(pos, state.setValue(RESIN, false), 3)) return InteractionResult.PASS;
            com.gregtech.gregtech.api.fluid.HandContainerTransfer.replaceOne(player, hand, filled.container());
        }
        return InteractionResult.CONSUME;
    }

    /** GT6 {@code getDrops}: a hole drops the log it was cut into. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(com.gregtech.gregtech.registry.GTWoods.log(species)));
    }

    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        return switch(originalUse(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME,CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME;case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return originalUse(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
}
