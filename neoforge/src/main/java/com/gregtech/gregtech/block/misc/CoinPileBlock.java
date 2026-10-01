package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.blockentity.misc.CoinPileBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's coin pile ({@code MultiTileEntityCoin}, multi-tile id 32700, registered in
 * {@code Loader_MultiTileEntities.java:2240} under the "Coins" category).
 *
 * <p>GT6's coin pile is a 4x4 grid of coin stacks on one block - sixteen faces, each holding up to
 * {@code COIN_STACKSIZE} = 16 coins ({@code MultiTileEntityCoin.java:71} and {@code :74}). Only the
 * top face can be clicked ({@code :156}), the click lands on one face ({@code :158}), an empty hand
 * takes one coin off that face ({@code :159-165}), a coin of the pile's own kind adds one
 * ({@code :166-172}) and the pile removes its block once the last coin is gone ({@code :174-176}).
 * Breaking the pile drops all of its coins, in stacks of at most 64 ({@code :112-122}).</p>
 *
 * <h2>The coins</h2>
 *
 * <p>GT6's coin <em>item</em> is the placeable pile itself: {@code MultiTileEntityCoin} is registered
 * like any other multi-tile, and its item stack carries the material in {@code gt.material}
 * ({@code :83}, {@code :90}) plus a {@code gt.coin.unique} flag ({@code :82}) and the two 16x16
 * pixel-shape bitsets {@code gt.coin.shape.0.<i>}/{@code gt.coin.shape.1.<i>} ({@code :79-80}) that
 * only its own renderer reads. Its {@code COIN_MAP} ({@code :124-133}, filled in
 * {@code onRegistration :285-328}) holds one such stack per material. GT6's {@code OP.coin} prefix is
 * declared {@code unused} ({@code gregapi/data/OP.java:473}) and generates no items at all.</p>
 *
 * <p>The port has no multi-tile items, so it registers <b>one coin item per material</b> like every
 * other form ({@code MaterialPrefix.coin}, {@code gregtech:coin_<i>&lt;material&gt;</i>}, 586 items -
 * the port's tiny-plate materials, which is GT6's own condition plus its four forced precious
 * metals), and {@link #coinItem()} answers with the copper one for a pile that has not bound a
 * material yet, GT6's {@code MT.Cu} being the first of the four {@code WorldgenDungeonGT.java:195}
 * prefers.</p>
 *
 * <h2>Port differences</h2>
 *
 * <ul>
 *   <li>A pile compares the full coin item and tags, as GT6's {@code ST.equal} against
 *       {@code getCoin(1, registry, id)} does ({@code :157} and {@code :167}). It therefore keeps
 *       both one material and one die pattern per pile.</li>
 *   <li>The pile's own block item is <b>not</b> a coin and is refused. In GT6 the pile's item and the
 *       coin are one and the same stack, but a bare pile item carries no material, so it matches no
 *       entry of {@code COIN_MAP} and its {@code ST.equal} against a real coin fails too.</li>
 *   <li>Registered material coins use GT6's default minted 3D relief. Mold-struck coins may carry
 *       {@code gt.coin.unique} and always carry all 32 {@code gt.coin.shape.0/1.<i>} bitset rows in item NBT;
 *       the placed pile renders that custom die. Inventory models still show the default relief.
 *       {@link #COINS} and {@link #FILL} are legacy summary state for other consumers.</li>
 *   <li>No tooltips: GT6's pile lists the material and the three {@code gt.tooltip.coins.*} lines
 *       about its "3D Coins" client config ({@code :136-147}); the port has no such config, and the
 *       material is already the coin item's own name, which the pile shows in the world through the
 *       tint of its model.</li>
 *   <li>The bare {@code coin_pile} block item starts empty because it carries no material; placing a
 *       material coin item instead seeds the clicked cell, as GT6's {@code onPlaced} does
 *       ({@code :251-255}).</li>
 *   <li>GT6 merges dropped coins into a matching nearby pile or creates a new pile after 200 ticks
 *       ({@code IMTE_OnDespawn}, {@code :186-232}); {@link CoinItemExpireHandler} handles this.</li>
 * </ul>
 */
public class CoinPileBlock extends Block implements EntityBlock {
    /** Compact legacy state used for saved block states; the renderer reads every cell from the entity. */
    public static final IntegerProperty COINS = IntegerProperty.create("coins", 0, CoinPileBlockEntity.FACES);

    /** Legacy coarse fill state. Geometry and collision use exact per-cell counts, not this value. */
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 1, CoinPileBlockEntity.MAX_FILL);

    public CoinPileBlock(Properties properties) {
        super(properties.dynamicShape().noOcclusion());
        registerDefaultState(this.stateDefinition.any().setValue(COINS, 0).setValue(FILL, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COINS, FILL);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return Block.box(0, 0, 0, 16, 2, 16); // GT6's selected bounding box.
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        var shape = net.minecraft.world.phys.shapes.Shapes.empty();
        if (level.getBlockEntity(pos) instanceof CoinPileBlockEntity pile) {
            for (int face = 0; face < 16; face++) if (pile.faceCount(face) > 0)
                shape = net.minecraft.world.phys.shapes.Shapes.or(shape, net.minecraft.world.phys.shapes.Shapes.create(
                        com.gregtech.gregtech.content.tool.CoinGeometry.cell(face, pile.faceCount(face))));
        }
        return shape;
    }

    /**
     * The coin a port coin pile starts with - the port's stand-in for GT6's unbound pile.
     *
     * <p>GT6's dungeon writes only the sixteen face counters and no material at all
     * ({@code DungeonData.java:169-173}), which leaves its pile rendering {@code UNCOLOURED}
     * ({@code MultiTileEntityCoin.java:430-435}); the older {@code WorldgenDungeonGT.java:195} picks
     * the material from {@code {Cu, Cu, Cu, Ag, Ag, Au, Au, Pt}}. The port cannot render an uncoloured
     * coin, so a pile without a bound material uses the copper coin, GT6's most frequent dungeon
     * coin.</p>
     */
    public static Item coinItem() {
        return GTItems.getStack(MaterialPrefix.coin, Materials.Copper).getItem();
    }

    /** One copper coin as an item stack (used for drops and for a pile that lost its stored coin). */
    public static ItemStack defaultCoin() {
        return new ItemStack(coinItem());
    }

    /**
     * True when the stack is one of the port's coins - the port's equivalent of GT6's
     * {@code ST.equal(aStack, getCoin(1, registry, id))} ({@code MultiTileEntityCoin.java:157}).
     *
     * <p>The pile's own block item, a bare {@code gregtech:coin_pile}, is not a coin: it carries no
     * material, so GT6's pile would not match it either - see the class javadoc.</p>
     */
    public static boolean isCoin(ItemStack stack) {
        return !stack.isEmpty() && MaterialItem.getPrefix(stack) == MaterialPrefix.coin;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoinPileBlockEntity(pos, state);
    }

    /**
     * GT6's click behaviour ({@code MultiTileEntityCoin.java:155-179}).
     *
     * <p>GT6 only reacts to the top face ({@code :156}) and works out which of the sixteen faces was
     * hit from the click position ({@code :158}); the port does the same. An empty hand takes one coin
     * off that face ({@code :159-165}), a coin of the pile's material adds one unless the face already
     * holds {@code COIN_STACKSIZE} ({@code :166-172}) and anything else does nothing at all - GT6's
     * coin pile never hands a coin out for a non-matching item, unlike the ingot piles.</p>
     */
    @Override public net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME,CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME;case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override public InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
    public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CoinPileBlockEntity pile)) return InteractionResult.PASS;
        // GT6 :178 returns true for every face, including an ignored side click.
        if (hit.getDirection() != Direction.UP) return InteractionResult.sidedSuccess(level.isClientSide);
        // GT6 :156-178 does all of its work on the server side.
        if (level.isClientSide) return InteractionResult.SUCCESS;
        // GT6's block-relative click coordinates (:158 and :253).
        int face = CoinPileBlockEntity.faceAt(hit.getLocation().x - pos.getX(), hit.getLocation().z - pos.getZ());
        ItemStack held = player.getItemInHand(hand);

        if (held.isEmpty()) {
            if (pile.faceCount(face) > 0) {
                // GT6 :161 takes a coin only if it can enter the inventory; creative players delete
                // it without receiving one. A full inventory leaves the coin on the pile.
                ItemStack offered = pile.coinItem().isEmpty() ? defaultCoin() : pile.coinItem().copyWithCount(1);
                if (player.isCreative() || player.getInventory().add(offered)) {
                    pile.take(face);
                    PileBlock.playCollect(level, pos);
                }
            }
        } else if (pile.add(face, held) > 0) {
            // GT6 :168: a creative player does not spend the held coin.
            if (!player.isCreative()) held.shrink(1);
        }
        // GT6 :174-178 also clears a bare empty pile after any top click and always consumes the click.
        if (pile.isEmpty()) level.removeBlock(pos, false);
        return InteractionResult.CONSUME;
    }

    /**
     * GT6 drops every coin of the pile, in stacks of at most 64
     * ({@code MultiTileEntityCoin.java:112-122}).
     *
     * <p>As with {@link PileBlock}, the drop hangs on {@code onRemove} so that every removal path drops
     * the coins and none of them drops them twice - GT6 has no {@code playerWillDestroy} equivalent.</p>
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CoinPileBlockEntity pile) {
            for (ItemStack stack : pile.contents()) popResource(level, pos, stack);
            pile.clearContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * GT6 hands every coin of the pile out through {@code getDrops}, in stacks of at most 64
     * ({@code MultiTileEntityCoin.java:112-122}); the port reads them from the {@code BLOCK_ENTITY} loot
     * parameter the real drop paths pass (see {@link PileBlock#getDrops}). The original drops only
     * the coins, never an extra empty pile block item.
     *
     * <p>The pile is emptied as it hands the coins over, so {@link #onRemove} - the catch-all for the
     * removal paths that roll no loot - cannot hand them out a second time.</p>
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CoinPileBlockEntity pile) {
            drops.addAll(pile.contents());
            pile.clearContents();
        }
        return drops;
    }

    /** The block entity type of the coin pile. */
    public static BlockEntityType<?> blockEntityType() {
        return GTBlockEntities.COIN_PILE.get();
    }
}
