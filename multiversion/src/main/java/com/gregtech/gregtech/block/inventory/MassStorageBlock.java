package com.gregtech.gregtech.block.inventory;

import com.gregtech.gregtech.api.GTWaterloggable;
//? if >=1.21.1 {
/*import com.gregtech.gregtech.api.inventory.MassStorageFace;
import com.gregtech.gregtech.api.material.GTMaterial;
*///?} else {
//?}
import com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
*///?} else {
import net.minecraft.network.chat.Component;
//?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.21.1 {
/*import net.minecraft.world.ItemInteractionResult;
*///?} else {
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.world.item.component.CustomData;
*///?} else {
import net.minecraft.world.item.TooltipFlag;
//?}
import net.minecraft.world.item.context.BlockPlaceContext;
//? if >=1.21.1 {
/*import net.minecraft.world.level.Level;
*///?} else {
//?}
import net.minecraft.world.level.BlockGetter;
//? if >=1.21.1 {
//?} else {
import net.minecraft.world.level.Level;
//?}
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
//? if >=1.21.1 {
//?} else {
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
//?}
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
//? if >=1.21.1 {
/*import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
*///?} else {
//?}
import net.minecraft.world.level.material.FluidState;
//? if >=1.21.1 {
/*import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
*///?} else {
//?}
import net.minecraft.world.phys.BlockHitResult;
//? if >=1.21.1 {
/*import java.util.ArrayList;
*///?} else {

import javax.annotation.Nullable;
//?}
import java.util.List;

//? if >=1.21.1 {
/*/^* Bulk-storage front interaction, mode tools, tape packing and harvest; remaining tool gap: ALIGN-136. ^/
public class MassStorageBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final GTMaterial material;
    public MassStorageBlock(GTMaterial material, Properties properties) {
        super(properties); this.material = material;
*///?} else {
/** Bulk-storage front interaction, mode tools, tape packing and harvest; remaining tool gap: ALIGN-136. */
public class MassStorageBlock extends HorizontalDirectionalBlock implements EntityBlock, SimpleWaterloggedBlock {

    @Nullable
    private final com.gregtech.gregtech.api.material.GTMaterial material;

    public MassStorageBlock(Properties properties) {
        this(null, properties);
    }

    public MassStorageBlock(@Nullable com.gregtech.gregtech.api.material.GTMaterial material, Properties properties) {
        super(properties);
        this.material = material;
//?}
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(GTWaterloggable.WATERLOGGED, false));
    }
//? if >=1.21.1 {
    /*public GTMaterial material() { return material; }
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING, GTWaterloggable.WATERLOGGED); }
    public BlockState getStateForPlacement(BlockPlaceContext context) { return GTWaterloggable.getStateForPlacement(defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()), context); }
    protected FluidState getFluidState(BlockState state) { return GTWaterloggable.getFluidState(state); }
    protected BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) { GTWaterloggable.updateShape(state, level, pos); return state; }
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MassStorageBlockEntity(pos, state); }
    private ItemStack shell(BlockState state, MassStorageBlockEntity storage) {
        ItemStack shell = new ItemStack(this); CompoundTag tag = new CompoundTag();
        if (storage.isPacked()) {
            tag = storage.getUpdateTag(storage.getLevel().registryAccess());
            tag.remove("gt.partial_units");
        } else if ((storage.mode() & 7) != 0) tag.putInt("gt.mode", storage.mode() & 7);
        if (!tag.isEmpty()) {
            tag.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(storage.getType()).toString());
            shell.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        }
        return shell;
    }
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(entity instanceof MassStorageBlockEntity storage)) return List.of(new ItemStack(this));
        var drops = new ArrayList<ItemStack>(); drops.add(shell(state, storage)); drops.addAll(storage.looseDrops()); return drops;
    }
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(state, level, pos, player, hand, hit) ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(state, level, pos, player, InteractionHand.MAIN_HAND, hit) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    /^* Source creative pick retains the complete state, including untaped inventory and fractions. ^/
*///?} else {

    /** GT6 casing material of this variant (drives the tint), null for the legacy block. */
    @Nullable
    public com.gregtech.gregtech.api.material.GTMaterial material() { return material; }

//?}
    @Override
//? if >=1.21.1 {
    /*public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult hit,
            net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        var stack = new ItemStack(state.getBlock());
        if (level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage) {
            var data = storage.getUpdateTag(level.registryAccess());
            data.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(storage.getType()).toString());
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(data));
*///?} else {
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
//?}
        }
        return stack;
    }

    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
            List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        com.gregtech.gregtech.client.StorageBlockTooltips.massStorage(stack, context, tooltip);
*///?} else {
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
        return interact(state, level, pos, player, hand, hit)
                ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        com.gregtech.gregtech.client.StorageBlockTooltips.massStorage(stack, tooltip);
//?}
    }

    public long capacity() { return com.gregtech.gregtech.content.storage.OriginalStorageTooltipData.MASS_CAPACITY; }

    private boolean interact(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        // (insert+extract in one click, double withdrawals, armor swap-backs)
        if (hand == InteractionHand.OFF_HAND || !(level.getBlockEntity(pos) instanceof MassStorageBlockEntity store))
            return false;
        ItemStack held = player.getItemInHand(hand);
        if (store.isPacked() && isUnpackingTool(held)) {
            if (!level.isClientSide && store.unseal())
                com.gregtech.gregtech.api.tool.GTToolHelper.damageForToolClickReturn(held, 1000, player);
            return true;
        }
        if (useStorageModeTool(store, player, held)) return true;
        if (store.isPacked()) return false;
        if (hit.getDirection() != state.getValue(FACING)) return false;
        double x = com.gregtech.gregtech.api.inventory.MassStorageFace.x(state.getValue(FACING),
                hit.getLocation().x - pos.getX(), hit.getLocation().z - pos.getZ());
        double y = 16 * (1 - (hit.getLocation().y - pos.getY()));
        if (!com.gregtech.gregtech.api.inventory.MassStorageFace.active(x, y)) return false;
        int requested = com.gregtech.gregtech.api.inventory.MassStorageFace.withdrawal(x, y);
        // The six buttons take priority even if the player is holding an item.
        if (requested > 0 && store.stored() > 0) {
            if (!level.isClientSide) {
                Direction front = state.getValue(FACING);
                int left = requested;
                while (left > 0) {
                    ItemStack out = store.extractAmount(left);
                    if (out.isEmpty()) break;
                    left -= out.getCount();
                    com.gregtech.gregtech.util.GTItemDrops.dropItemStack(level, pos.getX() + .5 + front.getStepX(),
                            pos.getY() + .5, pos.getZ() + .5 + front.getStepZ(), out);
                }
            }
            return true;
        }
        if (!held.isEmpty()) {
            if (!level.isClientSide) {
                int count = store.insert(held);
                if (count > 0 && !player.getAbilities().instabuild) held.shrink(count);
            }
            return true;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.gt6ce.mass_storage.status",
                        store.stored(), store.template().isEmpty()
                                ? net.minecraft.network.chat.Component.translatable("gui.gt6ce.empty")
                                : store.template().getHoverName()), true);
            }
            return true;
        }
        if (!level.isClientSide && !store.template().isEmpty()
                && com.gregtech.gregtech.api.inventory.MassStorageFace.depositInventory(x, y)) {
            for (ItemStack stack : player.getInventory().items) {
                int inserted = store.insert(stack);
                if (inserted > 0 && !player.getAbilities().instabuild) stack.shrink(inserted);
            }
            player.getInventory().setChanged();
        }
        return true;
    }

    private static boolean isUnpackingTool(ItemStack held) {
//? if >=1.21.1 {
        /*return (com.gregtech.gregtech.platform.neoforge.NeoToolBindings.matches(held,"scissors") || com.gregtech.gregtech.platform.neoforge.NeoToolBindings.matches(held,"knife"));
*///?} else {
        return (com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(held,
                com.gregtech.gregtech.api.tool.GTToolType.SCISSORS)
                || com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(held,
                com.gregtech.gregtech.api.tool.GTToolType.KNIFE));
//?}
    }

    private static boolean useStorageModeTool(MassStorageBlockEntity store, Player player, ItemStack held) {
        boolean monkey = com.gregtech.gregtech.api.tool.GTToolHelper.isMonkeyWrench(held);
        boolean cutter = com.gregtech.gregtech.api.tool.GTToolHelper.isWireCutter(held);
        boolean screw = com.gregtech.gregtech.api.tool.GTToolHelper.isScrewdriver(held);
        if (!monkey && !cutter && !screw) return false;
        if (!player.level().isClientSide) {
            String key;
            if (monkey) {
                store.toggleAutoOutput();
                key = store.autoOutput() ? "message.gt6ce.storage.auto_output_on" : "message.gt6ce.storage.auto_output_off";
            } else if (cutter) {
                store.toggleOverflow();
                key = store.emitsOverflow() ? "message.gt6ce.storage.overflow_on" : "message.gt6ce.storage.overflow_off";
            } else {
                store.toggleFilterReset();
                key = store.resetsFilter() ? "message.gt6ce.storage.filter_reset" : "message.gt6ce.storage.filter_keep";
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key), true);
            com.gregtech.gregtech.api.tool.GTToolHelper.damageForToolClickReturn(held, 10000, player);
        }
        return true;
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (world, pos, block, entity) -> {
            if (entity instanceof MassStorageBlockEntity storage) storage.tickAutomaticOutput();
        };
    }
}
