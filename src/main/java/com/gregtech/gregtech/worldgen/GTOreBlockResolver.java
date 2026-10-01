package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.OreBlock;
import com.gregtech.gregtech.block.OreHostStone;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.block.stone.StoneVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps (GT material, host stone) to an ore placement, mirroring GT6 {@code WD.setOre} /
 * {@code WD.setSmallOre}. Each material registers a single {@code ore_<material>} /
 * {@code ore_small_<material>} block which carries the background stone in its
 * {@link OreBlock#STONE} block-state property, and the model renders it dynamically.
 *
 * <p>§103.B: the host stone used to be written into a per-block {@code OreBlockEntity}; it is now
 * part of the state the placement writes, so no block entity is created at all.
 *
 * <p>Hosts are the {@link OreHostStone} values: the vanilla rocks ({@code stone}, {@code granite},
 * {@code diorite}, {@code andesite}, {@code deepslate}, {@code tuff}, {@code netherrack},
 * {@code end_stone}), {@code bedrock}, the loose sediments ({@code sand}, {@code red_sand},
 * {@code gravel} — small ores only) and GT's own rocks via
 * {@link com.gregtech.gregtech.block.stone.StoneType#registryId()} ({@code stone_basalt}, ...).</p>
 */
public final class GTOreBlockResolver {
    private static final Map<String, Block> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> MISSING_LOGGED = ConcurrentHashMap.newKeySet();
    /** Sentinel for negative cache entries (ConcurrentHashMap cannot store null). */
    private static final Block NO_BLOCK = Blocks.AIR;

    private GTOreBlockResolver() {}

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
    public static boolean placeOre(WorldGenLevel level, BlockPos pos, BlockState host, GTMaterial material, boolean small) {
        OreHostStone stone = stoneOf(host);
        // Small ores additionally generate in loose sediments (GT6 sand/gravel small ores).
        if (stone == null && small) stone = sedimentOf(host);
        return placeOre(level, pos, stone, material, small);
    }

    /**
     * Places ore with an explicit host rock, ignoring what is at {@code pos} (GT6's
     * {@code WD.setOre(aWorld, x, y, z, aMaterial, aBlock)} picks the stone variant itself — the
     * deep-ocean pylons sprinkle GT6's normal-stone ore into prismarine, see §58).
     */
    public static boolean placeOre(WorldGenLevel level, BlockPos pos, OreHostStone stone, GTMaterial material, boolean small) {
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
        OreHostStone stone = stoneOf(host);
        // Small ores additionally generate in loose sediments (GT6 sand/gravel small ores).
        if (stone == null && small) stone = sedimentOf(host);
        return resolve(stone, material, small);
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

        String basePrefix = small ? "ore_small" : "ore";
        String materialName = material.getName().toLowerCase();
        String cacheKey = basePrefix + "/" + materialName;

        Block found = CACHE.get(cacheKey);
        if (found == null) {
            found = lookup(basePrefix + "_" + materialName);
            CACHE.put(cacheKey, found == null ? NO_BLOCK : found);
        }
        if (found == null || found == NO_BLOCK) {
            if (MISSING_LOGGED.add(cacheKey)) {
                GregTech.LOGGER.warn("[worldgen] No '{}' block registered for material '{}' — skipping ore placement",
                        basePrefix, materialName);
            }
            return null;
        }
        BlockState state = found instanceof OreBlock ore ? ore.stateFor(stone) : found.defaultBlockState();
        return new OrePlacement(state, stone);
    }

    @Nullable
    private static Block lookup(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, path);
        return ForgeRegistries.BLOCKS.containsKey(id) ? ForgeRegistries.BLOCKS.getValue(id) : null;
    }

    /**
     * Host rock of the host block, or null when ores must not generate in it
     * (GT6 {@code BlocksGT.stoneToNormalOres} / {@code oreGenReplaceable}).
     */
    @Nullable
    public static OreHostStone stoneOf(BlockState host) {
        if (host.is(Blocks.STONE)) return OreHostStone.STONE;
        if (host.is(Blocks.GRANITE)) return OreHostStone.GRANITE;
        if (host.is(Blocks.DIORITE)) return OreHostStone.DIORITE;
        if (host.is(Blocks.ANDESITE)) return OreHostStone.ANDESITE;
        if (host.is(Blocks.DEEPSLATE)) return OreHostStone.DEEPSLATE;
        if (host.is(Blocks.TUFF)) return OreHostStone.TUFF;
        if (host.is(Blocks.NETHERRACK)) return OreHostStone.NETHERRACK;
        if (host.is(Blocks.END_STONE)) return OreHostStone.END_STONE;
        if (host.is(BlockTags.STONE_ORE_REPLACEABLES)) return OreHostStone.STONE;
        if (host.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) return OreHostStone.DEEPSLATE;
        if (host.getBlock() instanceof GTStoneBlock stone && stone.variant() == StoneVariant.STONE) {
            return OreHostStone.of(stone.stoneType());
        }
        return null;
    }

    /** Loose-sediment backgrounds — valid hosts for SMALL ores only. */
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
    public static boolean placeBedrockOre(WorldGenLevel level, BlockPos pos, GTMaterial material) {
        if (!level.getBlockState(pos).is(Blocks.BEDROCK) || material == null) return false;
        String materialName = material.getName().toLowerCase();
        Block found = lookup("ore_" + materialName);
        if (!(found instanceof OreBlock ore)) return false;
        return level.setBlock(pos, ore.stateFor(OreHostStone.BEDROCK), 2);
    }
}
