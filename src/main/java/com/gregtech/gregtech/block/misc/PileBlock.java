package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.blockentity.misc.PileBlockEntity;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's loose ingot / plate / gem-plate piles on the dungeon floor.
 *
 * <p>GT6 registers three placeable multi-tiles for these - {@code MultiTileEntityIngot} (id 32084),
 * {@code MultiTileEntityPlate} (32085) and {@code MultiTileEntityPlateGem} (32086), see
 * {@code Loader_MultiTileEntities.java:2036-2038}. All three share the same behaviour from
 * {@code gregapi/tileentity/misc/MultiTileEntityPlaceable.java}: they carry exactly one stack
 * ({@code mStack}, read from {@code NBT_VALUE} = {@code "gt.value"} in {@code :55-63}), a right click
 * merges an identical stack into the pile ({@code :81-96}) or takes one item back out of it
 * ({@code :98-111}), and breaking the pile drops that stack ({@code :72-74}). GT6's dungeon places
 * them from real material stacks ({@code DungeonData.java:262-275}) and dropped ingots and plates
 * snap into existing piles ({@code GT_API_Proxy.java:1468-1470}). The port keeps GT6's three block
 * ids and adds that behaviour through {@link PileBlockEntity}.
 *
 * <h2>Port differences</h2>
 *
 * <p>The port's pile is an ordinary block with its own block item, while GT6's pile <em>is</em> the
 * material stack item; everything below follows from that and is noted at the code that needs it:</p>
 * <ul>
 *   <li>A held stack is matched by prefix and material ({@link PileBlockEntity#canAccept}) instead of
 *       GT6's item identity ({@code ST.equal}, {@code ST.java:92-94}), so the port's own iron ingot
 *       and the unified vanilla iron ingot can share one pile.</li>
 *   <li>An empty pile refuses the click where GT6 removes the block on any click ({@code :80}) - the
 *       port's pile can legitimately exist empty, and deleting a block the player just placed would
 *       be surprising. A take that empties a non-empty pile still removes the block like GT6
 *       ({@code :107}).</li>
 *   <li>Clicks the port cannot use return {@link InteractionResult#PASS} instead of GT6's "always
 *       consume" ({@code onBlockActivated2} returns {@code T} on every path), so a refused click
 *       still lets the player place the held block.</li>
 *   <li>GT6 renders one layer per eight stored ingots and grows the collision box with the stack
 *       ({@code MultiTileEntityIngot.java:45-49} and {@code :129-131}); the port bakes GT6's own pass
 *       geometry into one model per stack size and carries that size in {@link #STACK}, and synchronizes material data with a block entity packet
 *       ({@code MultiTileEntityPlaceable.java:116-125}) - the block state travels with the chunk and
 *       with every placement, which is the port's equivalent of GT6's data packet.</li>
 *   <li>GT6's piles are soft (hardness 0.25F, {@code MultiTileEntityPlaceable.java:138}) and do not
 *       obstruct placement ({@code :131-133}); the port keeps the block properties these blocks
 *       already had.</li>
 * </ul>
 */
public class PileBlock extends Block implements EntityBlock {
    /**
     * The stack size GT6 draws, as a block state.
     *
     * <p>GT6 renders one box <em>per stored item</em>: {@code getRenderPasses} returns {@code mSize}
     * ({@code MultiTileEntityIngot.java:48}, {@code MultiTileEntityPlate.java:48}) and
     * {@code setBlockBounds} maps the pass index onto the box of that item
     * ({@code MultiTileEntityIngot.java:52-127} lays eight ingots per layer, alternating between an
     * X-aligned and a Z-aligned layer, and reaches a full block at 64;
     * {@code MultiTileEntityPlate.java:52-60} stacks at most four plates in a 2x2 arrangement whose
     * height follows {@code mSize}). A block model cannot grow a variable number of elements at
     * runtime, so the port bakes one model per stack size and carries the size in this property -
     * {@code 0} for an empty pile, otherwise exactly GT6's {@code mSize} of {@code 1..64}
     * ({@code MultiTileEntityPlaceable.java:58}, {@code UT.Code.bindStack}). {@link PileBlockEntity}
     * keeps the property in step with its contents; the model files behind the 65 values are generated
     * by {@code tools/generate_pile_assets.py}.</p>
     */
    public static final IntegerProperty STACK = IntegerProperty.create("stack", 0, PileBlockEntity.MAX_SIZE);

    /** The three piles GT6 knows, each bound to the ore prefix it may hold ({@code OP.ingot}, {@code OP.plate}, {@code OP.plateGem}). */
    public enum Kind {
        INGOT,
        PLATE,
        GEM_PLATE;

        /**
         * The port prefix this pile accepts ({@code OP.ingot}/{@code OP.plate}/{@code OP.plateGem}).
         *
         * <p>Resolved on every call instead of being stored in the enum constant: {@code MaterialPrefix}
         * registers its fields in a static block, and an enum constant must not depend on that
         * initialisation order.</p>
         */
        public MaterialPrefix prefix() {
            return switch (this) {
                case INGOT -> MaterialPrefix.ingot;
                case PLATE -> MaterialPrefix.plate;
                case GEM_PLATE -> MaterialPrefix.plateGem;
            };
        }

        /**
         * The port block of this form - GT6's multi-tile 32084 / 32085 / 32086
         * ({@code Loader_MultiTileEntities:2036-2038}). Resolved on every call for the same reason as
         * {@link #prefix()}: the blocks have to be registered before this is asked.
         */
        public Block block() {
            return switch (this) {
                case INGOT -> com.gregtech.gregtech.registry.GTDecorBlocks.INGOT_PILE.get();
                case PLATE -> com.gregtech.gregtech.registry.GTDecorBlocks.PLATE_PILE.get();
                case GEM_PLATE -> com.gregtech.gregtech.registry.GTDecorBlocks.PLATE_GEM_PILE.get();
            };
        }
    }

    private final Kind kind;

    public PileBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(this.stateDefinition.any().setValue(STACK, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STACK);
    }

    /** Which forms this pile accepts and stores. */
    public Kind kind() {
        return kind;
    }

    /** GT6 selects the partial top layer, but collides only with complete layers. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int count = state.getValue(STACK);
        int perLayer = kind == Kind.INGOT ? 8 : 4;
        int layerHeight = kind == Kind.INGOT ? 2 : 1;
        // An empty, directly placed port block remains selectable until filled.
        int height = Math.max(1, ((count + perLayer - 1) / perLayer) * layerHeight);
        return Block.box(0, 0, 0, 16, height, 16);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int perLayer = kind == Kind.INGOT ? 8 : 4;
        int height = state.getValue(STACK) / perLayer * (kind == Kind.INGOT ? 2 : 1);
        return height == 0 ? Shapes.empty() : Block.box(0, 0, 0, 16, height, 16);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PileBlockEntity(pos, state);
    }

    /**
     * GT6's click behaviour ({@code MultiTileEntityPlaceable.java:77-113}).
     *
     * <p>GT6 does its work on the server and reports the click as handled on the client
     * ({@code :78}); the port keeps that answer so the arm still swings. Server side the port follows
     * GT6: a matching stack is merged into the pile, a whole stack at a time and capped at 64
     * ({@code :81-96}), and anything else takes one item back out of the pile - out of the
     * <em>topmost</em> pile of a column of identical piles above the clicked one
     * ({@code :99-105}), because GT6 keeps its counter in the top block of the column. A take that
     * empties its pile removes that block ({@code :107}), since in GT6 the pile is its items.</p>
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PileBlockEntity pile)) return InteractionResult.PASS;
        // GT6: `if (isClientSide()) return T;` (MultiTileEntityPlaceable.java:78) - the client never
        // decides, it only reports the click as handled.
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);

        // GT6 :80-96: a matching stack is merged - an empty pile binds to the first stack of its own
        // form. The port's emptiness branch below replaces GT6's `ST.invalid(mStack) -> setToAir()`.
        if (pile.canAccept(held)) {
            int moved = pile.add(held);
            if (moved <= 0) return InteractionResult.PASS;   // GT6 :82: a full pile takes nothing
            // GT6 :84/:91/:94 shrinks the held stack unconditionally - unlike its coin pile, which
            // spares a creative player (MultiTileEntityCoin.java:168).
            held.shrink(moved);
            playCollect(level, pos);
            return InteractionResult.CONSUME;
        }
        if (pile.isEmpty()) return InteractionResult.PASS;

        // GT6 :98-111: everything else - an empty hand, another material or another prefix - takes
        // one item out of the top of the column.
        PileBlockEntity target = topOfColumn(level, pos, pile);
        ItemStack taken = target.take(1);
        if (taken.isEmpty()) return InteractionResult.PASS;
        if (!player.getInventory().add(taken)) player.drop(taken, false);
        playCollect(level, pos);
        if (target.isEmpty()) level.removeBlock(target.getBlockPos(), false);
        return InteractionResult.CONSUME;
    }

    /**
     * GT6's column walk ({@code MultiTileEntityPlaceable.java:99-105}): the item is taken from the
     * highest pile above the clicked one that carries the very same stack, which is where GT6 keeps
     * the stack size of the whole column.
     */
    private static PileBlockEntity topOfColumn(Level level, BlockPos pos, PileBlockEntity clicked) {
        PileBlockEntity top = clicked;
        for (int i = 1; i < 255; i++) {
            if (!(level.getBlockEntity(pos.above(i)) instanceof PileBlockEntity above)) break;
            if (!clicked.sameContents(above)) break;
            top = above;
        }
        return top;
    }

    /** GT6's {@code playCollect()} - the pickup sound both the merge and the take play. */
    static void playCollect(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    /**
     * GT6 drops the stored stack when the multi-tile is broken
     * ({@code MultiTileEntityPlaceable.java:72-74}).
     *
     * <p>Like the port's book shelf, the drop hangs on {@code onRemove} and not on
     * {@code playerWillDestroy}, because GT6 overrides {@code breakBlock}, which <em>every</em>
     * removal path goes through - a piston, an explosion or {@code /setblock} drop the pile too. The
     * contents are cleared right after dropping them, so no removal path can hand them out twice.
     * GT6 drops only the material stack, so a stocked port pile does not also produce its auxiliary
     * empty block item.</p>
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PileBlockEntity pile) {
            for (ItemStack stack : pile.contents()) popResource(level, pos, stack);
            pile.clearContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * GT6 hands the stored stack out through {@code getDrops}
     * ({@code MultiTileEntityPlaceable.java:72-74}) - the drop route a player break, a piston move with
     * drops and an explosion take: {@code Block.dropResources} passes the block entity as the
     * {@code BLOCK_ENTITY} loot parameter ({@code Level.destroyBlock:290-291}), which is where the port
     * reads the pile from.
     *
     * <p>The pile is emptied as it hands its contents over, so {@link #onRemove} - the catch-all for the
     * removal paths that roll no loot ({@code Level.removeBlock}, {@code /setblock}, a pushed piston) -
     * can never hand the same contents out twice. Overriding {@code getDrops} replaces the default loot,
     * and no auxiliary block item is added: GT6's drop is the stored material stack.</p>
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PileBlockEntity pile) {
            drops.addAll(pile.contents());
            pile.clearContents();
        }
        return drops;
    }

    /** GT6's middle-click returns one of the material item represented by the pile. */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof PileBlockEntity pile && !pile.isEmpty()) {
            return pile.stored().copyWithCount(1);
        }
        // Only the port can have an empty pile, since it exposes the placement block separately.
        return new ItemStack(this);
    }

    /** The block entity type all three piles share. */
    public static net.minecraft.world.level.block.entity.BlockEntityType<?> blockEntityType() {
        return GTBlockEntities.PILE.get();
    }
}
