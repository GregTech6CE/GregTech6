package com.gregtech.gregtech.blockentity;

import com.gregtech.gregtech.block.OreBlock;
import com.gregtech.gregtech.block.OreHostStone;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>Legacy only (§103.B).</b> Ore blocks carry no block entity: the background ("host") rock an ore
 * generated in is the {@link OreBlock#STONE} block-state property now, and this entity's single field
 * was that stone id. It cost one entity per ore block — ~497 per chunk, 1.8M in the 3.6k-chunk session
 * of §101/§102 (~0.3–0.5 GB once the chunk's block-entity map entry and the client's
 * {@code BlockEntityInfo} are counted).
 *
 * <p>Nothing creates one any more ({@link OreBlock} deliberately does not implement
 * {@code EntityBlock}); the class and its {@code gregtech:ore} block-entity type stay registered for
 * one reason: a world generated before §103.B has an entity per ore, and vanilla loads them from the
 * chunk tag ({@code BlockEntity#loadStatic}).
 *
 * <p>Such an entity is short-lived. The chunk loader creates it, sets its level
 * ({@code LevelChunk:538}), and {@link #onLoad()} writes a stone it carries into the block state and
 * removes itself; {@code LevelChunk#setBlockEntity} refuses to store it in the first place because the
 * ore state no longer reports {@code hasBlockEntity()}. A chunk from an old world therefore converges
 * to the new representation — no entities, host rock in the state — the first time it is loaded, and
 * the tags are gone from the next save.
 */
public class OreBlockEntity extends BlockEntity {
    public static final String TAG_STONE = "stone";
    public static final String DEFAULT_STONE = "stone";
    /** Bedrock-background ores are unbreakable like bedrock (GT6 bedrock deposits). */
    public static final String BEDROCK_STONE = "bedrock";

    private String stone = DEFAULT_STONE;
    /** The stone id the chunk tag carried — empty for entities that were not loaded from a chunk. */
    private String tagStone = "";

    public OreBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.ORE.get(), pos, state);
    }

    public String getStone() {
        return stone;
    }

    public void setStone(String stone) {
        if (stone == null || stone.isEmpty()) stone = DEFAULT_STONE;
        this.stone = stone;
        setChanged();
    }

    /** True for ores embedded in bedrock — indestructible, no drops. */
    public boolean isBedrockOre() {
        return BEDROCK_STONE.equals(getStone());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        String s = tag.getString(TAG_STONE);
        if (!s.isEmpty()) {
            stone = s;
            tagStone = s;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!DEFAULT_STONE.equals(stone)) tag.putString(TAG_STONE, stone);
    }

    /**
     * §103.B migration: a world generated before the host rock moved to the block state still has an
     * ore entity per ore; write the stone it carries into {@link OreBlock#STONE} and release the entity.
     * An old world converges to the new representation instead of carrying inert entities forever.
     *
     * <p>The entity is never in a chunk map (see the class comment), so this only has to fix the state;
     * {@code level} is valid because the loader sets it before calling this ({@code LevelChunk:538}).
     * If the state cannot be written the entity is kept, so the next load retries.
     */
    @Override
    public void onLoad() {
        if (level == null || level.isClientSide) return;
        if (!tagStone.isEmpty()) {
            BlockState current = getBlockState();
            if (current.getBlock() instanceof OreBlock ore) {
                BlockState migrated = ore.stateFor(OreHostStone.byId(tagStone));
                if (current != migrated && !level.setBlock(worldPosition, migrated, 2)) return;
            }
        }
        setRemoved();
        level.removeBlockEntity(worldPosition);
    }
}
