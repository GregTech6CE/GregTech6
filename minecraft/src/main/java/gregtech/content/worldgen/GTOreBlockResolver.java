package gregtech.content.worldgen;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.DenseOreBlock;
import gregtech.content.block.OreBlock;
import gregtech.content.block.OreHostStone;
import gregtech.content.block.VanillaOreBlock;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.registry.GTBlocks;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.platform.Mc;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Maps (GT material, host stone) to an ore placement, mirroring GT6 {@code WD.setOre} /
 * {@code WD.setSmallOre}. Each material registers a single {@code ore_<material>} /
 * {@code ore_small_<material>} block which carries the background stone in its
 * {@link OreBlock#STONE} block-state property, and the model renders it dynamically.
 *
 * <p>The host stone is part of the state the placement writes, so no block entity is involved.
 *
 * <p>Hosts are the {@link OreHostStone} values: the vanilla rocks ({@code stone}, {@code granite},
 * {@code diorite}, {@code andesite}, {@code deepslate}, {@code tuff}, {@code netherrack},
 * {@code end_stone}), {@code bedrock}, the loose sediments ({@code sand}, {@code red_sand},
 * {@code gravel}, {@code mud} — GT6's DUST_ORE hosts) and GT's own rocks via
 * {@link gregtech.content.block.stone.StoneType#registryId()} ({@code stone_basalt}, ...).</p>
 */
public final class GTOreBlockResolver {
    private record OreKey(GTMaterial material, boolean small) {}

    private static final Map<OreKey, OreBlock> CACHE = new ConcurrentHashMap<>();
    private static final Set<OreKey> MISSING_LOGGED = ConcurrentHashMap.newKeySet();

    private GTOreBlockResolver() {}

    /** Writes a position in the supplied generation chunk, preserving region and block-state callbacks. */
    static boolean setInChunk(WorldGenLevel level, ChunkAccess chunk, BlockPos pos, BlockState state) {
        if (level.getClass() != WorldGenRegion.class || state.hasBlockEntity())
            return level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        var region = (WorldGenRegion) level;
        if (!region.ensureCanWrite(pos)) return false;
        BlockState replaced = chunk.setBlockState(pos, state, false);
        if (replaced != null) {
            region.getLevel().onBlockStateChange(pos, replaced, state);
            if (replaced.hasBlockEntity()) chunk.removeBlockEntity(pos);
        }
        if (state.hasPostProcess(region, pos)) chunk.markPosForPostprocessing(pos);
        return true;
    }

    /** Material of a registered GT ore, including dense ores and whole-rock ore seams. */
    @Nullable
    public static GTMaterial oreMaterialOf(Block block) {
        if (block instanceof OreBlock ore) return ore.material();
        if (block instanceof DenseOreBlock ore) return ore.material();
        if (block instanceof VanillaOreBlock ore) return ore.material();
        return null;
    }

    /** Base rock used for geology samples, including the raw variants of GT stones. */
    public static boolean isBaseRock(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(Blocks.END_STONE)
                || state.getBlock() instanceof GTStoneBlock stone && stone.variant() == StoneVariant.STONE;
    }

    /** A resolved ore placement: the block state (host stone included) plus the host rock. */
    public record OrePlacement(BlockState state, OreHostStone stone) {}

    /**
     * Places the ore for {@code material} at {@code pos}, replacing the host stone there and
     * carrying the host stone into the ore block's state. Returns false when the host is
     * not ore-replaceable or no ore block is registered for the material.
     */
    public static boolean placeOre(WorldGenLevel level, BlockPos pos, GTMaterial material, boolean small) {
        return placeOre(level, pos, level.getBlockState(pos), material, small);
    }

    /** Variant of {@link #placeOre(WorldGenLevel, BlockPos, GTMaterial, boolean)} with a pre-fetched host state. */
    public static boolean placeOre(
            WorldGenLevel level, BlockPos pos, BlockState host, GTMaterial material, boolean small) {
        OrePlacement placement = resolve(host, material, small);
        return placement != null && level.setBlock(pos, placement.state(), 2);
    }

    /**
     * Places ore with an explicit host rock, ignoring what is at {@code pos} (GT6's
     * {@code WD.setOre(aWorld, x, y, z, aMaterial, aBlock)} picks the stone variant itself).
     */
    public static boolean placeOre(
            WorldGenLevel level, BlockPos pos, OreHostStone stone, GTMaterial material, boolean small) {
        OrePlacement placement = resolve(stone, material, small);
        if (placement == null) return false;
        return level.setBlock(pos, placement.state(), 2);
    }

    /**
     * @param host     the block currently at the position (must be an ore-replaceable stone)
     * @param material the GT ore material
     * @param small    true for {@code oreSmall} blocks, false for normal {@code ore} blocks
     * @return the placement, or null if the host is not replaceable or no ore block is registered
     */
    @Nullable
    public static OrePlacement resolve(BlockState host, GTMaterial material, boolean small) {
        return resolve(oreHost(host), material, small);
    }

    /**
     * @param stone    the host rock to record, or null when ores must not generate in it
     * @param material the GT ore material
     * @param small    true for {@code oreSmall} blocks, false for normal {@code ore} blocks
     * @return the placement, or null if the host is not replaceable or no ore block is registered
     */
    @Nullable
    public static OrePlacement resolve(@Nullable OreHostStone stone, GTMaterial material, boolean small) {
        if (stone == null || material == null) return null;

        var key = new OreKey(material.resolve(), small);
        OreBlock found = CACHE.computeIfAbsent(key, GTOreBlockResolver::lookup);
        if (found == null) {
            if (MISSING_LOGGED.add(key)) {
                Mc.logger()
                        .warn(
                                "[worldgen] No '{}' block registered for material '{}' — skipping ore placement",
                                small ? "ore_small" : "ore",
                                key.material().getName());
            }
            return null;
        }
        return new OrePlacement(found.stateFor(stone), stone);
    }

    @Nullable
    private static OreBlock lookup(OreKey key) {
        var prefix = key.small() ? BlockMaterialPrefix.oreSmall : BlockMaterialPrefix.ore;
        if (prefix == null) return null;
        var block = GTBlocks.getObject(prefix, key.material());
        return block != null && block.isPresent() && block.get() instanceof OreBlock ore ? ore : null;
    }

    @Nullable
    static OreHostStone oreHost(BlockState host) {
        OreHostStone stone = stoneOf(host);
        return stone == null ? sedimentOf(host) : stone;
    }

    /**
     * Host rock of the host block, or null when ores must not generate in it
     * (GT6 {@code BlocksGT.stoneToNormalOres} / {@code oreGenReplaceable}).
     */
    @Nullable
    public static OreHostStone stoneOf(BlockState host) {
        if (host.hasBlockEntity() || host.getBlock() instanceof OreBlock) return null;
        if (host.is(Blocks.STONE)) return OreHostStone.STONE;
        if (host.is(Blocks.GRANITE)) return OreHostStone.GRANITE;
        if (host.is(Blocks.DIORITE)) return OreHostStone.DIORITE;
        if (host.is(Blocks.ANDESITE)) return OreHostStone.ANDESITE;
        if (host.is(Blocks.DEEPSLATE)) return OreHostStone.DEEPSLATE;
        if (host.is(Blocks.INFESTED_DEEPSLATE)) return OreHostStone.DEEPSLATE;
        if (host.is(Blocks.TUFF)) return OreHostStone.TUFF;
        if (host.is(Blocks.INFESTED_STONE)) return OreHostStone.STONE;
        if (host.is(Blocks.NETHERRACK)) return OreHostStone.NETHERRACK;
        if (host.is(Blocks.END_STONE)) return OreHostStone.END_STONE;
        // GT6 ore.normal.sandstone: sandstone-hosted ores have the sandstone host and no gangue.
        if (host.is(Blocks.SANDSTONE) || host.is(Blocks.CUT_SANDSTONE) || host.is(Blocks.CHISELED_SANDSTONE))
            return OreHostStone.SANDSTONE;
        // Deepslate before stone; modded base stone also resolves to the stone host.
        if (host.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) return OreHostStone.DEEPSLATE;
        if (host.is(BlockTags.STONE_ORE_REPLACEABLES)) return OreHostStone.STONE;
        if (host.getBlock() instanceof GTStoneBlock stone && stone.variant() == StoneVariant.STONE) {
            return OreHostStone.of(stone.stoneType());
        }
        if (host.is(BlockTags.BASE_STONE_OVERWORLD)) return OreHostStone.STONE;
        return null;
    }

    /** GT6's DUST_ORE backgrounds: both normal and small ores replace sand and gravel. */
    @Nullable
    public static OreHostStone sedimentOf(BlockState host) {
        if (host.is(Blocks.SAND)) return OreHostStone.SAND;
        if (host.is(Blocks.RED_SAND)) return OreHostStone.RED_SAND;
        if (host.is(Blocks.GRAVEL)) return OreHostStone.GRAVEL;
        return null;
    }

    /**
     * Replaces a BEDROCK block with an unbreakable bedrock-background ore
     * (GT6 bedrock deposits). Only the bedrock-ore feature calls this — the
     * generic {@link #stoneOf} intentionally never matches bedrock.
     */
    public static boolean placeBedrockOre(WorldGenLevel level, BlockPos pos, GTMaterial material, boolean small) {
        return pos.getY() == level.getMinBuildHeight()
                && level.getBlockState(pos).is(Blocks.BEDROCK)
                && placeOre(level, pos, OreHostStone.BEDROCK, material, small);
    }
}
