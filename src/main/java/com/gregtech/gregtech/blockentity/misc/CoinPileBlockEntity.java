package com.gregtech.gregtech.blockentity.misc;

import com.gregtech.gregtech.block.misc.CoinPileBlock;
import com.gregtech.gregtech.content.tool.CoinGeometry;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's coin pile ({@code gregtech/tileentity/placeables/MultiTileEntityCoin.java}, multi-tile id
 * 32700).
 *
 * <p>GT6 keeps sixteen face counters in a {@code byte[16]} ({@code mCoinStackSizes},
 * {@code MultiTileEntityCoin.java:71}) - the 4x4 grid of coin stacks players see on top of the block
 * - and one material for the whole pile ({@code mMaterial}, {@code :72}). Each face holds at most
 * {@code COIN_STACKSIZE} coins ({@code :74}). The counters are the amount, the material is the kind;
 * GT6's coin <em>item</em> is the pile's own placeable item, whose stack size is the coin count
 * ({@code getCoin}, {@code :107-109}).</p>
 *
 * <h2>Storage layout</h2>
 *
 * <pre>
 * gt.coin.stacksize.&lt;0..15&gt; : byte, coins on that face, 0..16          (GT6's own keys, :81 and :93)
 * gt.coin.item               : ItemStack compound, the coin of this pile (port addition)
 * </pre>
 *
 * <p>GT6 writes {@code gt.coin.stacksize.<i>} for the sixteen faces and, instead of an item, the
 * keys {@code gt.material} ({@code :83}/{@code :90}), {@code gt.coin.unique} ({@code :82}) and the two
 * 16x16 shape bitsets {@code gt.coin.shape.0.<i>}/{@code gt.coin.shape.1.<i>} ({@code :79-80}), which
 * only its own 3D coin renderer reads. The port's coin is a real item
 * ({@code gregtech:coin_<i>&lt;material&gt;</i>}, see {@link CoinPileBlock}), so the coin stack replaces
 * the material string and carries the optional shape bitsets. The renderer reads that coin's die
 * pattern and the block entity packet synchronizes it together with the face counts.</p>
 */
public class CoinPileBlockEntity extends BlockEntity {
    /** GT6's sixteen faces: {@code mCoinStackSizes = new byte[16]} ({@code MultiTileEntityCoin.java:71}). */
    public static final int FACES = com.gregtech.gregtech.block.CoinPileRules.FACES;
    /**
     * GT6's per-face cap: {@code private static final byte COIN_STACKSIZE = 16}
     * ({@code MultiTileEntityCoin.java:74}), used by the add ({@code :167}) and the item merge
     * ({@code :201}, {@code :217}).
     */
    public static final int FACE_STACK_SIZE = com.gregtech.gregtech.block.CoinPileRules.PER_FACE;
    /**
     * The legacy coarse fill state has four values. The block entity renderer itself draws all
     * sixteen exact per-face heights from GT6 ({@code MultiTileEntityCoin.java:385}).
     */
    public static final int MAX_FILL = 4;
    /** GT6's per-face key prefix ({@code MultiTileEntityCoin.java:81} and {@code :93}). */
    public static final String NBT_STACKSIZE = "gt.coin.stacksize.";
    /**
     * The coin this pile is made of - the port's stand-in for GT6's {@code gt.material} ({@code :83},
     * {@code :90}), which the port's coin item already carries as its registry identity.
     */
    public static final String NBT_COIN = "gt.coin.item";

    private final byte[] faces = new byte[FACES];
    private ItemStack coin = ItemStack.EMPTY;
    private CoinGeometry.MintPattern coinPattern = CoinGeometry.defaultPattern();

    public CoinPileBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.COIN_PILE.get(), pos, state);
    }

    /**
     * GT6's face index from block-relative click coordinates ({@code MultiTileEntityCoin.java:158} for
     * the click and {@code :253} for the placement): {@code floor(hitX*4)*4 + floor(hitZ*4)}, with both
     * coordinates clamped into {@code [0, 0.99)}.
     */
    public static int faceAt(double hitX, double hitZ) {
        return com.gregtech.gregtech.block.CoinPileRules.faceAt(hitX,hitZ);
    }

    private static int faceAxis(double value) {
        return (int) (Math.min(0.99D, Math.max(0.0D, value)) * 4.0D);
    }

    /** Coins on one face ({@code 0..} {@link #FACE_STACK_SIZE}). */
    public int faceCount(int face) {
        return face < 0 || face >= FACES ? 0 : faces[face];
    }

    /** Every coin on the pile, over all sixteen faces. */
    public int total() {
        int total = 0;
        for (byte face : faces) total += face;
        return total;
    }

    public boolean isEmpty() {
        return total() == 0;
    }

    /** The coin item this pile is made of; {@link ItemStack#EMPTY} while the pile holds no coin. */
    public ItemStack coinItem() {
        return coin;
    }

    /** The shape is decoded only when this pile's coin changes, never on every render frame. */
    public CoinGeometry.MintPattern coinPattern() { return coinPattern; }

    private void storeCoin(ItemStack stack) {
        coin = stack;
        coinPattern = CoinGeometry.pattern(stack);
    }

    /**
     * How many of the sixteen faces carry a coin - what {@link CoinPileBlock#COINS} is set to. GT6
     * draws exactly those faces ({@code MultiTileEntityCoin.java:346-367}).
     */
    public int occupiedFaces() {
        int occupied = 0;
        for (byte face : faces) if (face > 0) occupied++;
        return occupied;
    }

    /**
     * A coarse {@code 1..}{@link #MAX_FILL} summary for {@link CoinPileBlock#FILL}.
     *
     * <p>The fullest face decides: GT6 would draw it {@code count / 16} of a block high
     * ({@code MultiTileEntityCoin.java:385}), while the renderer reads the exact count. The summary
     * rounds the fullest face up to groups of four.</p>
     */
    public int fillLevel() {
        int max = 0;
        for (byte face : faces) max = Math.max(max, face);
        return Math.max(1, Math.min(MAX_FILL, (max + 3) / 4));
    }

    /**
     * GT6's drops: every coin of the pile as stacks of at most 64
     * ({@code MultiTileEntityCoin.java:112-122}, which splits the total with {@code Math.min(64, ...)}).
     * The stacks are copies - the pile keeps its own counters.
     */
    public List<ItemStack> contents() {
        int remaining = total();
        if (remaining <= 0) return List.of();
        ItemStack coin = this.coin.isEmpty() ? CoinPileBlock.defaultCoin() : this.coin;
        if (coin.isEmpty()) return List.of();
        List<ItemStack> drops = new ArrayList<>();
        while (remaining > 0) {
            int stackSize = Math.min(64, remaining);
            drops.add(coin.copyWithCount(stackSize));
            remaining -= stackSize;
        }
        return drops;
    }

    /**
     * GT6's add branch ({@code MultiTileEntityCoin.java:166-172}): one coin per click, only onto a face
     * that still has room ({@code :167}) and only a coin of the pile's own material - GT6 compares the
     * held stack with {@code getCoin(1, registry, id)} ({@code ST.equal}, {@code :157}, {@code :167}),
     * and the port's coin item carries that material itself ({@link CoinPileBlock#isCoin}). A pile
     * whose first coin fixed its material refuses every other metal, which is GT6's rule for a pile
     * that already has a {@code mMaterial}.
     *
     * <p>The pile's own block item is refused with them: it is no coin, and GT6's bare pile item - one
     * without a material - matches no {@code COIN_MAP} entry either.</p>
     *
     * <p>GT6 shrinks the held stack itself ({@code :168}); the port returns the moved amount and lets
     * {@link CoinPileBlock#use} do that, as the port's blocks normally do.</p>
     *
     * @return how many coins moved onto the face ({@code 0} or {@code 1})
     */
    public int add(int face, ItemStack held) {
        if (face < 0 || face >= FACES) return 0;
        if (faces[face] >= FACE_STACK_SIZE) return 0;
        if (!CoinPileBlock.isCoin(held)) return 0;
        if (!coin.isEmpty() && !ItemStack.isSameItemSameTags(coin, held)) return 0;
        if (coin.isEmpty()) storeCoin(held.copyWithCount(1));
        faces[face]++;
        setChanged();
        syncState();
        return 1;
    }

    /**
     * GT6 {@code MultiTileEntityCoin.onDespawn}: fill matching faces from a dropped coin stack,
     * then redistribute the coins across all sixteen faces. The caller retains the stack and
     * reduces its count by the returned amount. One update is sent after the whole operation.
     */
    public int absorbDroppedCoins(ItemStack dropped) {
        if (!CoinPileBlock.isCoin(dropped) || dropped.getCount() <= 0) return 0;
        if (!coin.isEmpty() && !ItemStack.isSameItemSameTags(coin, dropped)) return 0;
        int moved = Math.min(dropped.getCount(), FACES * FACE_STACK_SIZE - total());
        if (moved <= 0) return 0;
        if (coin.isEmpty()) storeCoin(dropped.copyWithCount(1));
        int remaining = moved;
        for (int face = 0; face < FACES && remaining > 0; face++) {
            int count = Math.min(remaining, FACE_STACK_SIZE - faces[face]);
            faces[face] += count;
            remaining -= count;
        }
        // GT6 clears and randomly repacks the counters after absorbing dropped coins. Keeping
        // the total and the 16-per-cell cap invariant also gives the same varied pile silhouette.
        if (level != null) {
            int total = total();
            com.gregtech.gregtech.block.CoinPileRules.repack(faces,total,level.random::nextInt);
        }
        setChanged();
        syncState();
        return moved;
    }

    /**
     * GT6's take branch ({@code MultiTileEntityCoin.java:159-165}): one coin off the clicked face,
     * handed to the player. GT6 skips the hand-over for creative players
     * ({@code UT.Entities.hasInfiniteItems}) and just destroys the coin; the port always returns it.
     * The last coin taken clears the pile's kind, which is what GT6's {@code setBlockToAir()} amounts
     * to ({@code :174-176}).
     *
     * @return the taken coin, or {@link ItemStack#EMPTY} when the face is bare
     */
    public ItemStack take(int face) {
        if (face < 0 || face >= FACES || faces[face] <= 0) return ItemStack.EMPTY;
        ItemStack taken = coin.isEmpty() ? CoinPileBlock.defaultCoin() : coin.copyWithCount(1);
        faces[face]--;
        if (total() == 0) storeCoin(ItemStack.EMPTY);
        setChanged();
        syncState();
        return taken;
    }

    /**
     * Binds the pile to one coin before any face is filled - the port's equivalent of GT6 writing
     * {@code gt.material} into the multi-tile's NBT ({@code MultiTileEntityCoin.java:83}),
     * which its dungeon generator does through {@code COIN_MAP}
     * ({@code WorldgenDungeonGT.java:195}). A stack that is no coin is refused.
     *
     * @return true when the pile is bound to that coin afterwards
     */
    public boolean bindCoin(ItemStack stack) {
        if (!CoinPileBlock.isCoin(stack)) return false;
        ItemStack bound = stack.copyWithCount(1);
        if (!coin.isEmpty() && !ItemStack.isSameItemSameTags(coin, bound)) return false;
        if (coin.isEmpty()) {
            storeCoin(bound);
            setChanged();
            syncState();
        }
        return true;
    }

    /**
     * Sets the coin of the pile, whatever it held before - GT6's dungeon writes {@code gt.material}
     * into the multi-tile's NBT before it is placed ({@code DungeonData.java:169-173} writes the face
     * bytes, {@code WorldgenDungeonGT.java:195} takes the whole coin compound out of {@code COIN_MAP}),
     * so the pile never sees a default material at all. The port fills the faces first and binds the
     * metal afterwards, which is why this exists next to {@link #bindCoin}.
     *
     * @return true when the stack was a coin and is now this pile's coin
     */
    public boolean setCoin(ItemStack stack) {
        if (!CoinPileBlock.isCoin(stack)) return false;
        storeCoin(stack.copyWithCount(1));
        setChanged();
        syncState();
        return true;
    }

    /**
     * Writes one face's counter directly, the port's equivalent of GT6 setting the byte itself
     * ({@code DungeonData.java:170-171} prefills the sixteen {@code gt.coin.stacksize.<i>} keys before
     * the multi-tile is placed, see {@code MultiTileEntityCoin.java:81}). Used by the dungeon fill and
     * by tests; {@link #add} remains the player-facing path with its own checks.
     *
     * <p>The count is clamped into {@code 0..}{@link #FACE_STACK_SIZE}, so GT6's cap holds no matter who
     * writes. A face that gets coins binds the pile to the default copper coin if it had none, and a pile
     * whose last coin is written away forgets its coin item again.</p>
     *
     * @return the count that ended up on the face
     */
    public int setFaceCount(int face, int count) {
        if (face < 0 || face >= FACES) return 0;
        int clamped = Math.max(0, Math.min(FACE_STACK_SIZE, count));
        faces[face] = (byte) clamped;
        if (clamped > 0 && coin.isEmpty()) storeCoin(CoinPileBlock.defaultCoin());
        if (total() == 0) storeCoin(ItemStack.EMPTY);
        setChanged();
        syncState();
        return clamped;
    }

    /**
     * Empties the pile without dropping anything - used after the drops have been handed out.
     *
     * <p>Deliberately without a {@link #syncState}: every caller is a removal or drop path
     * ({@code CoinPileBlock#getDrops} and {@code CoinPileBlock#onRemove}), and
     * {@code LevelChunk.setBlockState} stores the new state <em>before</em> it runs {@code onRemove} -
     * so the position already holds air (or the block that replaces the pile) while this runs, and
     * writing the state from here would re-enter the chunk's own block-state update.</p>
     */
    public void clearContents() {
        boolean changed = !coin.isEmpty();
        for (int face = 0; face < FACES; face++) {
            if (faces[face] != 0) {
                faces[face] = 0;
                changed = true;
            }
        }
        storeCoin(ItemStack.EMPTY);
        if (changed) setChanged();
    }

    /**
     * Moves {@link CoinPileBlock#COINS} and {@link CoinPileBlock#FILL} onto the legacy block state.
     * The visible renderer reads the sixteen exact counters directly, as GT6 does
     * ({@code MultiTileEntityCoin.java:341-367}); these two properties are just summary state.
     *
     * <p>{@code UPDATE_CLIENTS} only: the block itself does not change, so nothing has to be dropped
     * ({@code CoinPileBlock#onRemove} checks the block, not the state) and no neighbour needs an
     * update.</p>
     */
    private void syncState() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof CoinPileBlock)) return;
        BlockState wanted = state
                .setValue(CoinPileBlock.COINS, occupiedFaces())
                .setValue(CoinPileBlock.FILL, fillLevel());
        if (wanted != state) level.setBlock(worldPosition, wanted, Block.UPDATE_CLIENTS);
        else level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    /**
     * Picks the pile's look up again after the block entity was read from disk or from the network -
     * both state properties are stored with the chunk and agree in the normal case, but a pile written
     * by {@code /setblock ... {BlockEntityTag:...}} or placed by worldgen only gets its contents here.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        syncState();
    }

    /** Synchronize the coin material and all sixteen cell counters, including on chunk entry. */
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int face = 0; face < FACES; face++) {
            // GT6 trusts its bytes; the port clamps them into 0..FACE_STACK_SIZE so a hand-written tag
            // cannot build a face that holds more coins than the cap allows.
            faces[face] = (byte) Math.max(0, Math.min(FACE_STACK_SIZE, tag.getByte(NBT_STACKSIZE + face)));
        }
        storeCoin(tag.contains(NBT_COIN) ? ItemStack.of(tag.getCompound(NBT_COIN)) : ItemStack.EMPTY);
    }

    /** GT6 writes the sixteen {@code gt.coin.stacksize.<i>} bytes ({@code MultiTileEntityCoin.java:93}). */
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int face = 0; face < FACES; face++) tag.putByte(NBT_STACKSIZE + face, faces[face]);
        if (!coin.isEmpty()) tag.put(NBT_COIN, coin.save(new CompoundTag()));
    }
}
