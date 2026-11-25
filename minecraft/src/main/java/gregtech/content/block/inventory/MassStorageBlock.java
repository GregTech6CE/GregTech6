package gregtech.content.block.inventory;

import gregtech.api.material.GTMaterial;
import gregtech.engine.inventory.MassStorageFace;
import gregtech.content.block.GTHorizontalDirectionalBlock;
import gregtech.content.blockentity.inventory.MassStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
*///?} else {
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.world.item.component.CustomData;
*///?} else {
//?}
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Bulk-storage front interaction, mode tools, tape packing and harvest; remaining tool gap: ALIGN-136. */
public class MassStorageBlock extends GTHorizontalDirectionalBlock
        implements EntityBlock, gregtech.engine.tool.ToolInteractionTarget {
    @Nullable
    private final GTMaterial material;

    public MassStorageBlock(Properties properties) {
        this(null, properties);
    }

    public MassStorageBlock(@Nullable GTMaterial material, Properties properties) {
        super(properties);
        this.material = material;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected InteractionResult useBlock(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        // (insert+extract in one click, double withdrawals, armor swap-backs)
        if (hand == InteractionHand.OFF_HAND || !(level.getBlockEntity(pos) instanceof MassStorageBlockEntity store))
            return InteractionResult.PASS;
        if (gregtech.engine.tool.ToolInteractions.use(state, level, pos, player, hand, hit))
            return InteractionResult.sidedSuccess(level.isClientSide);
        ItemStack held = player.getItemInHand(hand);
        if (store.isPacked() && isUnpackingTool(held)) {
            if (!level.isClientSide && store.unseal())
                gregtech.engine.tool.GTToolHelper.damageForToolClickReturn(held, 1000, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (useStorageModeTool(store, player, held)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (store.isPacked()) return InteractionResult.PASS;
        if (hit.getDirection() != state.getValue(FACING)) return InteractionResult.PASS;
        double x = gregtech.engine.inventory.MassStorageFace.x(state.getValue(FACING),
                hit.getLocation().x - pos.getX(), hit.getLocation().z - pos.getZ());
        double y = 16 * (1 - (hit.getLocation().y - pos.getY()));
        if (!gregtech.engine.inventory.MassStorageFace.active(x, y)) return InteractionResult.PASS;
        int requested = gregtech.engine.inventory.MassStorageFace.withdrawal(x, y);
        // The six buttons take priority even if the player is holding an item.
        if (requested > 0 && store.stored() > 0) {
            if (!level.isClientSide) {
                int left = requested;
                while (left > 0) {
                    ItemStack out = store.extractAmount(left);
                    if (out.isEmpty()) break;
                    left -= out.getCount();
                    deliverWithdrawal(player, out);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!held.isEmpty()) {
            if (!level.isClientSide) {
                int count = store.insert(held);
                if (count > 0 && !player.getAbilities().instabuild) held.shrink(count);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.gt6ce.mass_storage.status",
                        store.stored(), store.template().isEmpty()
                                ? net.minecraft.network.chat.Component.translatable("gui.gt6ce.empty")
                                : store.template().getHoverName()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && !store.template().isEmpty()
                && gregtech.engine.inventory.MassStorageFace.depositInventory(x, y)) {
            for (ItemStack stack : player.getInventory().items) {
                int inserted = store.insert(stack);
                if (inserted > 0 && !player.getAbilities().instabuild) stack.shrink(inserted);
            }
            player.getInventory().setChanged();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** GT6 casing material of this variant (drives the tint), null for the legacy block. */
    @Nullable
    public GTMaterial material() { return material; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MassStorageBlockEntity(pos, state);
    }

//? if >=1.21.1 {
    /*private ItemStack shell(BlockState state, MassStorageBlockEntity storage) {
        ItemStack shell = new ItemStack(this);
        CompoundTag tag = new CompoundTag();
        if (storage.isPacked()) {
            tag = gregtech.engine.stack.NbtCompat.getUpdateTag(storage, storage.getLevel().registryAccess());
            tag.remove("gt.partial_units");
        } else if ((storage.mode() & 7) != 0) tag.putInt("gt.mode", storage.mode() & 7);
        if (!tag.isEmpty()) {
            tag.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(storage.getType()).toString());
            shell.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        }
        return shell;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(entity instanceof MassStorageBlockEntity storage)) return List.of(new ItemStack(this));
        var drops = new ArrayList<ItemStack>();
        drops.add(shell(state, storage));
        drops.addAll(storage.looseDrops());
        return drops;
    }

    /^* Source creative pick retains the complete state, including untaped inventory and fractions. ^/
    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult hit,
            net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        var stack = new ItemStack(state.getBlock());
        if (level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage) {
            var data = gregtech.engine.stack.NbtCompat.getUpdateTag(storage, level.registryAccess());
            data.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(storage.getType()).toString());
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(data));
        }
        return stack;
    }
*///?} else {
    private static ItemStack packed(BlockState state, @Nullable BlockEntity be) {
        ItemStack stack = new ItemStack(state.getBlock());
        if (be instanceof MassStorageBlockEntity storage)
            stack.addTagElement("BlockEntityTag", gregtech.engine.stack.NbtCompat.getUpdateTag(storage, null));
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
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(be instanceof MassStorageBlockEntity storage)) return List.of(new ItemStack(state.getBlock()));
        List<ItemStack> drops = new ArrayList<>();
        drops.add(storage.isPacked() ? packedForHarvest(state, storage) : emptyShell(state, storage));
        drops.addAll(storage.looseDrops());
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return packed(state, level.getBlockEntity(pos));
    }
//?}

    @Override
    protected void appendTooltip(ItemStack stack, @javax.annotation.Nullable BlockGetter level,
            @javax.annotation.Nullable HolderLookup.Provider registries, List<Component> tooltip, TooltipFlag flag) {
        gregtech.client.StorageBlockTooltips.massStorage(stack, registries, tooltip);
    }

    public long capacity() { return gregtech.content.storage.OriginalStorageTooltipData.MASS_CAPACITY; }

    /** GT6 {@code TileEntityBase09FacingSingle}: the wrench re-aims the front on the 3x3 grid. */
    @Override
    public gregtech.engine.tool.ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return gregtech.engine.tool.GTToolHelper.matchesTool(tool, gregtech.engine.tool.GTToolType.WRENCH)
                ? gregtech.engine.tool.ToolInteractionSpec.facing(FACING,
                        gregtech.engine.tool.MachineRotationType.HORIZONTAL)
                : null;
    }

    /** Direct withdrawals enter the player's inventory; only a full-inventory remainder is dropped. */
    private static void deliverWithdrawal(Player player, ItemStack stack) {
//? if >=1.21.1 {
        /*ItemStack remainder = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(
                new net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper(player.getInventory()), stack, false);
*///?} else {
        ItemStack remainder = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(
                new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(player.getInventory()), stack, false);
//?}
        if (remainder.getCount() != stack.getCount()) player.getInventory().setChanged();
        if (!remainder.isEmpty()) {
            var drop = new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(),
                    player.getY() + .5, player.getZ(), remainder);
            drop.setNoPickUpDelay();
            drop.setTarget(player.getUUID());
            player.level().addFreshEntity(drop);
        }
    }

    private static boolean isUnpackingTool(ItemStack held) {
        return (gregtech.engine.tool.GTToolHelper.matchesTool(held,
                gregtech.engine.tool.GTToolType.SCISSORS)
                || gregtech.engine.tool.GTToolHelper.matchesTool(held,
                gregtech.engine.tool.GTToolType.KNIFE));
    }

    private static boolean useStorageModeTool(MassStorageBlockEntity store, Player player, ItemStack held) {
        boolean monkey = gregtech.engine.tool.GTToolHelper.isMonkeyWrench(held);
        boolean cutter = gregtech.engine.tool.GTToolHelper.isWireCutter(held);
        boolean screw = gregtech.engine.tool.GTToolHelper.isScrewdriver(held);
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
            gregtech.engine.tool.GTToolHelper.damageForToolClickReturn(held, 10000, player);
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
