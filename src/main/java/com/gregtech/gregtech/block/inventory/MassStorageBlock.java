package com.gregtech.gregtech.block.inventory;

import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/** GT6 Mass Storage front controls, tape packing and harvest behavior. */
public class MassStorageBlock extends HorizontalDirectionalBlock implements EntityBlock, SimpleWaterloggedBlock {

    @Nullable
    private final com.gregtech.gregtech.api.material.GTMaterial material;

    public MassStorageBlock(Properties properties) {
        this(null, properties);
    }

    public MassStorageBlock(@Nullable com.gregtech.gregtech.api.material.GTMaterial material, Properties properties) {
        super(properties);
        this.material = material;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(GTWaterloggable.WATERLOGGED, false));
    }

    /** GT6 casing material of this variant (drives the tint), null for the legacy block. */
    @Nullable
    public com.gregtech.gregtech.api.material.GTMaterial material() { return material; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState base = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        return GTWaterloggable.getStateForPlacement(base, ctx);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return state;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MassStorageBlockEntity(pos, state);
    }

    private static ItemStack packed(BlockState state, @Nullable BlockEntity be) {
        ItemStack stack = new ItemStack(state.getBlock());
        if (be instanceof MassStorageBlockEntity storage)
            stack.addTagElement("BlockEntityTag", storage.getUpdateTag());
        return stack;
    }

    private static ItemStack packedForHarvest(BlockState state, MassStorageBlockEntity storage) {
        ItemStack stack = packed(state, storage);
        // GT6 breakBlock emits the fractional material separately and clears it
        // before the kept inventory is written to the harvested block item.
        if (stack.getTagElement("BlockEntityTag") != null)
            stack.getTagElement("BlockEntityTag").remove("gt.partial_units");
        return stack;
    }

    private static ItemStack emptyShell(BlockState state, MassStorageBlockEntity storage) {
        ItemStack stack = new ItemStack(state.getBlock());
        // The lower GT6 mode bits belong to the machine, but untaped inventory
        // does not travel inside the harvested block item.
        if ((storage.mode() & 7) != 0) {
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putInt("gt.mode", storage.mode() & 7);
            stack.addTagElement("BlockEntityTag", tag);
        }
        return stack;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(
                net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (!(be instanceof MassStorageBlockEntity storage)) return List.of(new ItemStack(state.getBlock()));
        List<ItemStack> drops = new java.util.ArrayList<>();
        drops.add(storage.isPacked() ? packedForHarvest(state, storage) : emptyShell(state, storage));
        drops.addAll(storage.looseDrops());
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return packed(state, level.getBlockEntity(pos));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        // (insert+extract in one click, double withdrawals, armor swap-backs)
        if (hand == net.minecraft.world.InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof MassStorageBlockEntity store)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (store.isPacked() && (com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(held,
                com.gregtech.gregtech.api.tool.GTToolType.SCISSORS)
                || com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(held,
                com.gregtech.gregtech.api.tool.GTToolType.KNIFE))) {
            if (!level.isClientSide && store.unseal())
                com.gregtech.gregtech.api.tool.GTToolHelper.damageForToolClickReturn(held, 1000, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (store.isPacked()) return InteractionResult.PASS;
        if (com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(held, com.gregtech.gregtech.api.tool.GTToolType.SCREWDRIVER)) {
            if (!level.isClientSide) {
                store.toggleFilterReset();
                player.displayClientMessage(Component.translatable(store.resetsFilter() ? "message.gregtech.storage.filter_reset" : "message.gregtech.storage.filter_keep"), true);
                com.gregtech.gregtech.api.tool.GTToolHelper.damageForUse(held, 1, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hit.getDirection() != state.getValue(FACING)) return InteractionResult.PASS;
        double x = com.gregtech.gregtech.api.inventory.MassStorageFace.x(state.getValue(FACING),
                hit.getLocation().x - pos.getX(), hit.getLocation().z - pos.getZ());
        double y = 16 * (1 - (hit.getLocation().y - pos.getY()));
        if (!com.gregtech.gregtech.api.inventory.MassStorageFace.active(x,y)) return InteractionResult.PASS;
        int requested = com.gregtech.gregtech.api.inventory.MassStorageFace.withdrawal(x,y);
        // The six buttons take priority even if the player is holding an item.
        if (requested > 0 && store.stored() > 0) {
            if (!level.isClientSide) {
                Direction front = state.getValue(FACING);
                int left = requested;
                while (left > 0) {
                    ItemStack out = store.extractAmount(left);
                    if (out.isEmpty()) break;
                    left -= out.getCount();
                    com.gregtech.gregtech.util.GTItemDrops.dropItemStack(level, pos.getX()+.5+front.getStepX(),
                            pos.getY()+.5, pos.getZ()+.5+front.getStepZ(), out);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!held.isEmpty()) {
            if (!level.isClientSide) {
                int taken = store.insert(held);
                if (taken > 0 && !player.getAbilities().instabuild) held.shrink(taken);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.gregtech.mass_storage.status",
                        store.stored(), store.template().isEmpty()
                                ? Component.translatable("gui.gregtech.empty")
                                : store.template().getHoverName()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && !store.template().isEmpty()
                && com.gregtech.gregtech.api.inventory.MassStorageFace.depositInventory(x,y)) {
            for (ItemStack stack : player.getInventory().items) {
                int inserted = store.insert(stack);
                if (inserted > 0 && !player.getAbilities().instabuild) stack.shrink(inserted);
            }
            player.getInventory().setChanged();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.mass_storage.1"));
        tooltip.add(Component.translatable("gt.tooltip.mass_storage.2"));
        tooltip.add(Component.translatable("gt.tooltip.mass_storage.3"));
        net.minecraft.nbt.CompoundTag stored = stack.getTagElement("BlockEntityTag");
        if (stored != null && stored.contains("gt.template") && stored.getLong("gt.stored") > 0) {
            ItemStack template = ItemStack.of(stored.getCompound("gt.template"));
            if (!template.isEmpty())
                tooltip.add(Component.translatable("message.gregtech.mass_storage.status",
                        stored.getLong("gt.stored"), template.getHoverName()));
        }
    }
}
