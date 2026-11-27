package gregtech.content.worldgen;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.MaterialStackItemHelper;
import gregtech.platform.Mc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/** Surface markers for an already generated bedrock deposit. */
final class BedrockIndicatorPlacement {
    private BedrockIndicatorPlacement() {}

    static int place(
            WorldGenLevel level,
            ChunkPos origin,
            ChunkPos target,
            GTBedrockOres.BedrockOre ore,
            long seed,
            int terrainTop,
            boolean underRoof) {
        Block flower = ore.flowerId() == null ? null : Mc.block(Mc.gregtechId(ore.flowerId()));
        BlockState flowerState = flower == null || flower == Blocks.AIR ? null : flower.defaultBlockState();
        GTMaterial material = ore.material().resolve();
        var raw = MaterialStackItemHelper.mat(MaterialPrefix.oreRaw, material, 1);
        ResourceLocation rawId = raw.isEmpty() ? null : Mc.itemId(raw.getItem());
        if (flowerState == null && rawId == null) return 0;
        String rawItem = rawId == null ? null : rawId.toString();
        return BedrockIndicatorRules.scatterInto(
                origin.x, origin.z, target.x, target.z, RandomSource.create(seed)::nextInt, (x, z) -> {
                    return underRoof
                            ? placeUnderground(level, x, z, material.getName(), rawItem, terrainTop)
                            : placeColumn(
                                    level,
                                    x,
                                    z,
                                    flowerState,
                                    material.getName(),
                                    rawItem,
                                    WorldgenHash.roll(WorldgenHash.at(seed, x, z), 4) != 0);
                });
    }

    private static boolean placeColumn(
            WorldGenLevel level,
            int x,
            int z,
            @Nullable BlockState flower,
            String material,
            @Nullable String rawItem,
            boolean preferFlower) {
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        var supportPos = new BlockPos.MutableBlockPos(x, surface, z);
        for (int y = surface; y >= level.getMinBuildHeight(); y--) {
            supportPos.setY(y);
            BlockState ground = level.getBlockState(supportPos);
            if (!ground.getFluidState().isEmpty() || ground.is(Blocks.FARMLAND)) return false;
            if (!ground.isSolidRender(level, supportPos) || ground.is(BlockTags.LOGS) || ground.is(BlockTags.LEAVES))
                continue;

            BlockPos pos = supportPos.above();
            if (level.isOutsideBuildHeight(pos) || !GTRocksFeature.replaceable(level.getBlockState(pos))) return false;
            if (flower != null
                    && (rawItem == null || preferFlower)
                    && flower.canSurvive(level, pos)
                    && level.setBlock(pos, flower, Block.UPDATE_CLIENTS)) {
                return true;
            }
            return rawItem != null
                    && ground.isFaceSturdy(level, supportPos, Direction.UP)
                    && GTRockPlacement.place(level, pos, material, rawItem, true);
        }
        return false;
    }

    /** Find the uppermost walkable shelf below the Nether roof. */
    private static boolean placeUnderground(
            WorldGenLevel level, int x, int z, String material, @Nullable String rawItem, int terrainTop) {
        if (rawItem == null) return false;
        var support = new BlockPos.MutableBlockPos(x, terrainTop - 2, z);
        for (int y = terrainTop - 2; y > level.getMinBuildHeight(); y--) {
            support.setY(y);
            var ground = level.getBlockState(support);
            if (ground.is(Blocks.BEDROCK)
                    || ground.hasBlockEntity()
                    || !ground.getFluidState().isEmpty()
                    || !ground.isFaceSturdy(level, support, Direction.UP)) continue;
            var pos = support.above();
            if (!level.getBlockState(pos).isAir()
                    || !level.getBlockState(pos.above()).isAir()) continue;
            return GTRockPlacement.place(level, pos, material, rawItem, true);
        }
        return false;
    }
}
