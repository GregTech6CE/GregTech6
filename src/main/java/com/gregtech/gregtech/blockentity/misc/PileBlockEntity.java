package com.gregtech.gregtech.blockentity.misc;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.block.misc.PileBlock;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * GT6's loose ingot / plate / gem-plate pile ({@code MultiTileEntityIngot} 32084,
 * {@code MultiTileEntityPlate} 32085, {@code MultiTileEntityPlateGem} 32086) and their shared base
 * {@code gregapi/tileentity/misc/MultiTileEntityPlaceable.java}.
 *
 * <p>GT6 keeps exactly one stack in {@code mStack} ({@code MultiTileEntityPlaceable.java:49}), read
 * from and written to the key {@code "gt.value"} ({@code NBT_VALUE}, {@code CS.java:1218}), and
 * derives the material from the stack's ore dictionary data ({@code OM.anydata},
 * {@code :55-63}). The port stores the same single stack under the same NBT key and derives the
 * prefix and material from the port's own registries instead (see {@link #prefixOf} and
 * {@link #materialOf}).</p>
 *
 * <h2>Storage layout</h2>
 *
 * <pre>
 * gt.value : ItemStack compound of the stored stack, count 1..64   (GT6's NBT_VALUE)
 * </pre>
 *
 * <p>A pile holds one stack of one material up to {@code MAX_SIZE} = 64 items - GT6's own cap
 * ({@code MultiTileEntityPlaceable.java:82}). GT6 additionally renders one layer per eight stored
 * ingots and widens the collision box with the stack size ({@code MultiTileEntityIngot.java:129-131},
 * {@code MultiTileEntityPlate.java:62-64}); the port drives geometry, selection and collision through
 * {@link PileBlock#STACK}, which {@link #syncState} keeps equal to {@link #count}.</p>
 */
public class PileBlockEntity extends BlockEntity {
    /** GT6's {@code NBT_VALUE} ({@code gregapi/data/CS.java:1218}) - the key GT6 saves the stack under. */
    public static final String NBT_VALUE = "gt.value";
    /** GT6's stack limit for a pile ({@code MultiTileEntityPlaceable.java:82}). */
    public static final int MAX_SIZE = 64;

    private final PileBlock.Kind kind;
    private ItemStack stored = ItemStack.EMPTY;

    public PileBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.PILE.get(), pos, state);
        this.kind = state.getBlock() instanceof PileBlock pile ? pile.kind() : PileBlock.Kind.INGOT;
    }

    /** Which forms this pile accepts - the ingot, plate or gem-plate prefix of its block. */
    public PileBlock.Kind kind() {
        return kind;
    }

    /** The stored stack, {@link ItemStack#EMPTY} while the pile is empty (GT6's {@code mStack}). */
    public ItemStack stored() {
        return stored;
    }

    /** How many items are piled up ({@code 0..} {@link #MAX_SIZE}). */
    public int count() {
        return stored.getCount();
    }

    /**
     * GT6's {@code mSize}, which is what its renderer draws one box per
     * ({@code MultiTileEntityPlaceable.java:58}, {@code MultiTileEntityIngot.java:48}): the stored
     * stack size, or {@code 0} for the port's empty pile, which GT6 cannot have because its pile
     * <em>is</em> the item stack.
     */
    public int stackLevel() {
        return count();
    }

    /**
     * Moves {@link PileBlock#STACK} to the current content size, which is what makes a bigger pile
     * look bigger - GT6 needs no such step, its renderer reads {@code mSize} directly
     * ({@code MultiTileEntityPlaceable.java:117-123}).
     *
     * <p>{@code UPDATE_CLIENTS} only: the block itself does not change, so nothing has to be dropped
     * or re-shaped, and no neighbour needs an update.</p>
     */
    private void syncState() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof PileBlock)) return;
        BlockState wanted = state.setValue(PileBlock.STACK, stackLevel());
        if (wanted != state) level.setBlock(worldPosition, wanted, Block.UPDATE_CLIENTS);
        else level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    /**
     * Picks the stack size up again after the block entity was read from disk or from the network -
     * the state is stored with the chunk, so the two agree in the normal case, but a pile written by
     * {@code /setblock ... {BlockEntityTag:...}} or by worldgen only gets its contents here.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        syncState();
    }

    public boolean isEmpty() {
        return stored.isEmpty();
    }

    /**
     * GT6's {@code getDrops} returns the stored stack ({@code MultiTileEntityPlaceable.java:72-74}),
     * so this is a list of zero or one stack. The stack is a copy - the pile keeps its own.
     */
    public List<ItemStack> contents() {
        return stored.isEmpty() ? List.of() : List.of(stored.copy());
    }

    /**
     * GT6's {@code OM.anydata(aStack).mPrefix} for any stack: the port's material items answer with
     * their registered prefix, everything else - unified vanilla items included - through
     * {@link ItemMaterialRegistry}.
     *
     * @return the prefix, or {@code null} when the stack carries no material form at all
     */
    @Nullable
    public static MaterialPrefix prefixOf(ItemStack stack) {
        MaterialPrefix prefix = MaterialItem.getPrefix(stack);
        if (prefix != null) return prefix;
        return ItemMaterialRegistry.get(stack).map(ItemMaterialRegistry.ItemMaterialData::prefix).orElse(null);
    }

    /**
     * GT6's {@code OM.anydata(aStack)} material lookup.
     *
     * @return the material of the stack, or {@code null} when the port has no material for it
     */
    @Nullable
    public static GTMaterial materialOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof MaterialItem materialItem) return materialItem.getMaterial();
        return ItemMaterialRegistry.get(stack)
                .map(ItemMaterialRegistry.ItemMaterialData::material)
                .filter(GTMaterial::isValid)
                .orElse(null);
    }

    /**
     * GT6 accepts a click when the held stack is the very same stack as the stored one
     * ({@code ST.equal}, {@code MultiTileEntityPlaceable.java:81} and {@code ST.java:92-94}).
     *
     * <p>The port's equivalent is "same form and same material", because the port registers its own
     * iron ingot next to the unified vanilla one and GT6's ore dictionary would have unified them
     * anyway. An empty pile accepts the first stack of its own prefix, which is the stack it binds to
     * from then on ({@code MultiTileEntityPlaceable.java:91}).</p>
     */
    public boolean canAccept(ItemStack held) {
        if (held.isEmpty() || prefixOf(held) != kind.prefix()) return false;
        if (stored.isEmpty()) return true;
        GTMaterial heldMaterial = materialOf(held);
        GTMaterial storedMaterial = materialOf(stored);
        return heldMaterial != null && storedMaterial != null && heldMaterial.resolve() == storedMaterial.resolve();
    }

    /** True when both piles hold the very same stack - GT6's {@code ST.equal} in its column walk. */
    public boolean sameContents(PileBlockEntity other) {
        if (stored.isEmpty() || other.stored.isEmpty()) return false;
        return ItemStack.isSameItemSameTags(stored, other.stored);
    }

    /**
     * GT6's merge branch ({@code MultiTileEntityPlaceable.java:81-96}), on the port's terms: the whole
     * held stack goes in, but never past {@code MAX_SIZE} - and a full pile takes nothing at all,
     * which is where GT6 returns early ({@code :82}).
     *
     * <p>GT6 shrinks the player's stack itself; the port returns the moved amount and lets
     * {@link PileBlock#use} do that, like the port's book shelf does.</p>
     *
     * @return how many items moved into the pile ({@code 0} when it is full or the stack is refused)
     */
    public int add(ItemStack held) {
        if (!canAccept(held)) return 0;
        int room = MAX_SIZE - count();
        if (room <= 0) return 0;
        int moved = Math.min(room, held.getCount());
        if (stored.isEmpty()) stored = held.copyWithCount(moved);
        else stored.grow(moved);
        setChanged();
        syncState();
        return moved;
    }

    /**
     * GT6's take branch ({@code MultiTileEntityPlaceable.java:98-111}) takes exactly one item
     * ({@code ST.amount(1, mStack)}); the port keeps the amount as a parameter so a future machine can
     * take more. Emptying the pile clears the stored stack and hence the material the pile was bound
     * to, which is what GT6's {@code setToAir()} amounts to ({@code :107}).
     *
     * @return the taken stack, or {@link ItemStack#EMPTY} when the pile is empty
     */
    public ItemStack take(int amount) {
        if (stored.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        int taken = Math.min(amount, stored.getCount());
        ItemStack result = stored.copyWithCount(taken);
        stored.shrink(taken);
        if (stored.isEmpty()) stored = ItemStack.EMPTY;
        setChanged();
        syncState();
        return result;
    }

    /**
     * Empties the pile without dropping anything - used after the drops have been handed out.
     *
     * <p>Deliberately without a {@link #syncState}: every caller is a removal or drop path
     * ({@code PileBlock#getDrops} and {@code PileBlock#onRemove}), and {@code LevelChunk.setBlockState}
     * stores the new state <em>before</em> it runs {@code onRemove} - so the position already holds air
     * (or the block that replaces the pile) while this runs, and writing the state from here would
     * re-enter the chunk's own block-state update. The pile is going away; its look does not matter.</p>
     */
    public void clearContents() {
        if (!stored.isEmpty()) {
            stored = ItemStack.EMPTY;
            setChanged();
        }
    }

    /** Initial chunk data carries the same material information as subsequent updates. */
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * The baked pile model has a tint index, but the material is stored in this block entity rather
     * than the block state. A block-state packet may be rendered before its block-entity packet arrives;
     * explicitly invalidating the section after the latter prevents a white pile until the next chunk
     * update. GT6 sent its material ID and size together in {@code getClientDataPacket}.
     */
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        ItemStack before = stored;
        super.handleUpdateTag(tag);
        if (level != null && level.isClientSide && !ItemStack.isSameItemSameTags(before, stored)) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stored = tag.contains(NBT_VALUE) ? ItemStack.of(tag.getCompound(NBT_VALUE)) : ItemStack.EMPTY;
    }

    /** GT6 writes {@code ST.save(aNBT, NBT_VALUE, mStack)} ({@code MultiTileEntityPlaceable.java:68}). */
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!stored.isEmpty()) tag.put(NBT_VALUE, stored.save(new CompoundTag()));
    }
}
