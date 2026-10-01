package com.gregtech.gregtech.block.inventory;

import com.gregtech.gregtech.blockentity.inventory.BottleCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** GT6 bottle crate: nine X/Z cells, whole-stack interaction and inventory-preserving drops. */
public class BottleCrateBlock extends HorizontalDirectionalBlock implements EntityBlock, com.gregtech.gregtech.api.tool.ToolInteractionTarget {

    public BottleCrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override public com.gregtech.gregtech.api.tool.ToolInteractionSpec toolInteraction(BlockState state,ItemStack tool) {
        return com.gregtech.gregtech.api.tool.GTToolHelper.isMachineWrench(tool)
                ? com.gregtech.gregtech.api.tool.ToolInteractionSpec.facing(FACING,com.gregtech.gregtech.block.machine.MachineRotationType.HORIZONTAL) : null;
    }
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return 150; }
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return 150; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BottleCrateBlockEntity(pos, state);
    }

    /** GT6's world-coordinate grid, independent of the frame's facing. */
    public static int slotAt(BlockPos pos, BlockHitResult hit) {
        return com.gregtech.gregtech.content.storage.ContainerStorageRules.bottleSlot(hit.getLocation().x-pos.getX(),hit.getLocation().z-pos.getZ());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof BottleCrateBlockEntity crate)) return InteractionResult.PASS;
        if (com.gregtech.gregtech.api.tool.ToolInteractions.use(state,level,pos,player,hand,hit)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int slot=slotAt(pos,hit);
        ItemStack stored=crate.items().getStackInSlot(slot);
        if (!stored.isEmpty()) {
            int destination=destination(player,stored);
            if(destination>=0) {
                crate.items().setStackInSlot(slot,ItemStack.EMPTY);
                ItemStack existing=player.getInventory().getItem(destination);
                if(existing.isEmpty()) player.getInventory().setItem(destination,stored);
                else existing.grow(stored.getCount());
                player.getInventory().setChanged();
            }
        } else {
            ItemStack held=player.getItemInHand(hand);
            if(isBottle(held)) {
                // The original consumes the held stack even in creative mode.
                crate.items().setStackInSlot(slot,held.copy());
                player.setItemInHand(hand,ItemStack.EMPTY);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** ST.add(player, stack, true): nonheld merges, selected slot, then other empty slots. */
    private static int destination(Player player,ItemStack stack) {
        var inv=player.getInventory();
        for(int i=0;i<36;i++) if(i!=inv.selected && fits(inv.getItem(i),stack)) return i;
        if(inv.getSelected().isEmpty() || fits(inv.getSelected(),stack)) return inv.selected;
        for(int i=0;i<36;i++) if(inv.getItem(i).isEmpty()) return i;
        return -1;
    }
    private static boolean fits(ItemStack existing,ItemStack stack) {
        return !existing.isEmpty() && ItemStack.isSameItemSameTags(existing,stack)
                && existing.getCount()+stack.getCount()<=existing.getMaxStackSize();
    }

    public static boolean isBottle(ItemStack stack) {
        if(stack.isEmpty()) return false;
        if(stack.is(Items.GLASS_BOTTLE) || stack.is(Items.EXPERIENCE_BOTTLE)
                || stack.getItem() instanceof net.minecraft.world.item.PotionItem
                || stack.getItem() instanceof com.gregtech.gregtech.item.BottleItem
                || stack.is(com.gregtech.gregtech.item.BottleItem.emptyBottle().getItem())) return true;
        ItemStack remainder=stack.getCraftingRemainingItem();
        return remainder.is(Items.GLASS_BOTTLE) || remainder.is(com.gregtech.gregtech.item.BottleItem.emptyBottle().getItem());
    }

    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return box(0,0,0,16,6,16);
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return box(0,0,0,16,10,16);
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state,BlockGetter level,BlockPos pos) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof BottleCrateBlockEntity crate) crate.detachInventory();
        super.onRemove(state,level,pos,next,moving);
    }

    @Override public List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        if(builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof BottleCrateBlockEntity crate)
            return List.of(crate.packedStack());
        return List.of(new ItemStack(this));
    }

    @Override public void playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof BottleCrateBlockEntity crate
                && !crate.contents().isEmpty()) popResource(level,pos,crate.packedStack());
        super.playerWillDestroy(level,pos,state,player);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.bottle_crate")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
