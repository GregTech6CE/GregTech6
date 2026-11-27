package gregtech.content.worldgen;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.OreBlock;
import gregtech.content.block.RockBlock;
import gregtech.content.blockentity.RockBlockEntity;
import gregtech.content.registry.GTBlocks;
import gregtech.engine.GTWaterloggable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** Places GT6-style surface pebbles ({@link gregtech.content.block.RockBlock}). */
public final class GTRockPlacement {
    private GTRockPlacement() {}

    /** Put a vein marker on natural ground, including on a lake or sea floor. */
    static boolean placeVeinMarker(WorldGenLevel level, ChunkAccess chunk, int x, int z, GTMaterial material) {
        String name = material.resolve().getName();
        int yTop = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15);
        var groundPos = new BlockPos.MutableBlockPos();
        for (int y = yTop; y >= level.getMinBuildHeight(); y--) {
            groundPos.set(x, y, z);
            BlockState ground = chunk.getBlockState(groundPos);
            if (!ground.getFluidState().isEmpty() || ground.is(Blocks.FARMLAND)) return false;
            if (ground.is(BlockTags.LOGS) || ground.is(BlockTags.LEAVES) || ground.getBlock() instanceof RockBlock)
                continue;
            if (!ground.isFaceSturdy(level, groundPos, Direction.UP)) {
                if (GTRocksFeature.replaceable(ground)) continue;
                return false;
            }
            if (!GTRocksFeature.rockGround(ground)
                    && GTOreBlockResolver.oreHost(ground) == null
                    && !(ground.getBlock() instanceof OreBlock)
                    && !ground.is(Blocks.CLAY)) return false;
            BlockPos pos = groundPos.above();
            if (level.isOutsideBuildHeight(pos.getY())) return false;
            BlockState space = chunk.getBlockState(pos);
            if (space.getBlock() instanceof RockBlock) {
                return level.getBlockEntity(pos) instanceof RockBlockEntity rock
                        && rock.getMaterial().equals(name);
            }
            boolean water = space.liquid() && GTWaterloggable.isWaterForLogging(space.getFluidState());
            if (!water && !GTRocksFeature.replaceable(space)) return false;
            return place(level, pos, name, null, false, water, RockBlock.Appearance.INDICATOR);
        }
        return false;
    }

    /**
     * Places GT6's lying-item rock at an exact position, carrying a material, an explicit item id
     * (GT6's {@code ST.save(NBT_VALUE, stack)}), or both. Used by the nether ground litter and by
     * {@link GTRocksFeature}'s surface rocks.
     */
    public static boolean place(WorldGenLevel level, BlockPos pos, String material, String itemId, boolean rawOre) {
        return place(
                level,
                pos,
                material,
                itemId,
                rawOre,
                false,
                "minecraft:flint".equals(itemId) ? RockBlock.Appearance.FLINT : RockBlock.Appearance.MATERIAL);
    }

    static boolean placeStratum(WorldGenLevel level, BlockPos pos, SurfaceRockRules.Pebble pebble) {
        return place(
                level,
                pos,
                pebble.material(),
                null,
                false,
                false,
                pebble.indicator() ? RockBlock.Appearance.INDICATOR : RockBlock.Appearance.MATERIAL);
    }

    private static boolean place(
            WorldGenLevel level,
            BlockPos pos,
            String material,
            String itemId,
            boolean rawOre,
            boolean waterlogged,
            RockBlock.Appearance appearance) {
        BlockState rockState = GTBlocks.ROCK
                .get()
                .defaultBlockState()
                .setValue(RockBlock.APPEARANCE, appearance)
                .setValue(GTWaterloggable.WATERLOGGED, waterlogged);
        if (!level.setBlock(pos, rockState, 2)) return false;
        if (level.getBlockEntity(pos) instanceof RockBlockEntity rock) {
            if (material != null && !material.isEmpty()) rock.setMaterial(material);
            if (itemId != null && !itemId.isEmpty()) rock.setItemId(itemId);
            rock.setRawOre(rawOre);
        }
        return true;
    }
}
