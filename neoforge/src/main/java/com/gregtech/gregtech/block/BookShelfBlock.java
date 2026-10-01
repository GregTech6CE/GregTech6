package com.gregtech.gregtech.block;

import com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity;
import com.gregtech.gregtech.content.book.GTBookList;
import com.gregtech.gregtech.content.book.BookShelfGeometry;
import com.gregtech.gregtech.content.book.BookShelfVariants;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.block.machine.MachineRotationType;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * GT6's Book Shelf ({@code MultiTileEntityBookShelf} + {@code LoaderBookList}).
 *
 * <p>GT6's shelf is a facing block with 28 slots that display the books stored in it; inserting
 * and removing is done by clicking the shelf (no GUI at all), and the shelf adds
 * {@code getEnchantPowerBonus} — twelve book points per power unit (plain=1, enchanted=2). Which items may
 * sit on a shelf comes from GT6's {@code BooksGT.BOOK_REGISTER} list (books, paper, maps, name tags,
 * item frames, paintings, buttons, levers and redstone decoys), see {@link GTBookList}.
 *
 * <p>The frame is a block model; synchronized inventory entries render as GT6 book cuboids
 * with the original spine and side textures.
 */
public class BookShelfBlock extends HorizontalDirectionalBlock implements EntityBlock, ToolInteractionTarget {
    @Override protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final BookShelfVariants.Variant variant;
    // GT6's isSideSolid2 is false on the two book faces and true on the other four.
    // The frame supplies those four faces even though the physical collision remains a full cube.
    private static final VoxelShape NORTH_SOUTH_SUPPORT = Shapes.or(
            Block.box(0, 0, 0, 16, 1, 16), Block.box(0, 15, 0, 16, 16, 16),
            Block.box(0, 0, 0, 1, 16, 16), Block.box(15, 0, 0, 16, 16, 16));
    private static final VoxelShape EAST_WEST_SUPPORT = Shapes.or(
            Block.box(0, 0, 0, 16, 1, 16), Block.box(0, 15, 0, 16, 16, 16),
            Block.box(0, 0, 0, 16, 16, 1), Block.box(0, 0, 15, 16, 16, 16));

    public BookShelfBlock(Properties properties) {
        this(properties, BookShelfVariants.defaultVariant());
    }

    public BookShelfBlock(Properties properties, BookShelfVariants.Variant variant) {
        super(properties.noOcclusion());
        this.variant = java.util.Objects.requireNonNull(variant);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public BookShelfVariants.Variant variant() { return variant; }

    @Override
    public net.minecraft.network.chat.MutableComponent getName() {
        return variant.displayName();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** GT6 TileEntityBase09FacingSingle: ordinary wrench, four horizontal directions. */
    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.matchesTool(tool, GTToolType.WRENCH)
                ? ToolInteractionSpec.facing(FACING, MachineRotationType.HORIZONTAL) : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BookShelfBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide || type != GTBlockEntities.BOOKSHELF.get()) return null;
        return (world, pos, current, entity) -> {
            if (entity instanceof BookShelfBlockEntity shelf) shelf.serverTick();
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X
                ? Block.box(2, 0, 0, 14, 16, 16) : Block.box(0, 0, 2, 16, 16, 14);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // GT6 inherits the full collision box from TileEntityBase01Root; only selection is inset.
        return Shapes.block();
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z
                ? NORTH_SOUTH_SUPPORT : EAST_WEST_SUPPORT;
    }

    /**
     * GT6's click handling (§57): the click lands in the exact slot the player aimed at (GT6's
     * {@code tIndex} formula over the 28-slot layout). Ordinary occupied slots give their item back;
     * buttons and levers activate until taken with sneaking or pincers.
     */
    @Override public net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME,CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME;case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override public InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
    public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (ToolInteractions.use(state, level, pos, player, hand, hit))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (!(level.getBlockEntity(pos) instanceof BookShelfBlockEntity shelf)) return InteractionResult.PASS;
        if (!level.isClientSide) shelf.generateDungeonLoot();
        // GT6's tCoords are block relative.
        int slot = BookShelfGeometry.slotAt(state.getValue(FACING), hit.getDirection(),
                hit.getLocation().x - pos.getX(), hit.getLocation().y - pos.getY(), hit.getLocation().z - pos.getZ());
        if (slot < 0) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        ItemStack stored = shelf.inventory().getStackInSlot(slot);

        // GT6 routes shelf tools before ordinary right-click: pincers take even secret controls,
        // while a magnifier inspects the item without dislodging it.
        if (!stored.isEmpty() && GTToolHelper.matchesTool(held, GTToolType.MAGNIFYING_GLASS)) {
            if (!level.isClientSide) player.displayClientMessage(stored.getHoverName(), false);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        boolean pincers = GTToolHelper.matchesTool(held, GTToolType.PINCERS);

        // GT6 first activates special controls, then hands other occupied slots to the player.
        if (!stored.isEmpty()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (!pincers && !player.isShiftKeyDown()) {
                if (stored.is(Items.OAK_BUTTON) || stored.is(Items.STONE_BUTTON)) {
                    shelf.pressButton();
                    level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5F, 1.0F);
                    return InteractionResult.CONSUME;
                }
                if (stored.is(Items.LEVER) || stored.is(Items.REDSTONE_TORCH)) {
                    shelf.toggleLever();
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 1.0F);
                    return InteractionResult.CONSUME;
                }
            }
            ItemStack taken = stored.copy();
            if (!player.getInventory().add(taken)) return InteractionResult.PASS;
            shelf.inventory().setStackInSlot(slot, ItemStack.EMPTY);
            if (pincers) GTToolHelper.damageForUse(held, 1, player);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() || !GTBookList.canPlace(held)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack inserted = shelf.inventory().insertItem(slot, held.copyWithCount(1), false);
        if (inserted.isEmpty()) {
            if (!player.isCreative()) held.shrink(1);
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.6F, 1.0F);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /**
     * GT6's shelf drops everything it holds when broken (§61). GT6 overrides {@code breakBlock},
     * which every removal path goes through, so the port hangs the drop on {@code onRemove} instead
     * of on the player-only hook — a piston, an explosion or {@code /setblock} now drop the books
     * too.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof BookShelfBlockEntity shelf) {
            for (ItemStack stack : shelf.contents()) popResource(level, pos, stack);
            shelf.inventory().deserializeNBT(level.registryAccess(),new net.minecraft.nbt.CompoundTag());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** GT6's shelf tooltips (§62): no GUI, pincers take the books out, the magnifying glass reads them. */
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gregtech.machine.nogui.click_front")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.gregtech.smeltery.tool.pincers")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.gregtech.machine.tool.magnifying_glass")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** GT6 converts twelve book points into one enchanting-power unit. */
    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BookShelfBlockEntity shelf ? shelf.enchantPower() / 12.0F : 0.0F;
    }

    /** A comparator reads how full the shelf is (GT6 reads the stored book count). */
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BookShelfBlockEntity shelf)) return 0;
        int stored = 0;
        for (int slot = 0; slot < shelf.inventory().getSlots(); slot++) {
            if (!shelf.inventory().getStackInSlot(slot).isEmpty()) stored++;
        }
        return stored == 0 ? 0 : (int) Math.ceil(stored * 15.0D / GTBookList.SLOTS);
    }

    /** GT6's wooden MultiTileEntityBlock is non-opaque (lightOpacity=0), despite full collision. */
    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof BookShelfBlockEntity shelf ? shelf.redstoneSignal() : 0;
    }

    /** GT6 wooden shelves burn at 150; the metalset variants are nonflammable. */
    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return variant.flammability();
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return variant.flammability();
    }
}
