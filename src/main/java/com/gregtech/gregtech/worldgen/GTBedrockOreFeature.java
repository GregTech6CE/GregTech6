package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.api.material.GTMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Random;

/**
 * Bedrock ore deposits (GT6 {@code WorldgenOresBedrock}): one weighted deposit per
 * 16x16-chunk grid cell, sitting in the deepest layers above bedrock, with indicator
 * flowers (real-world ore-indicator plants) on the surface above it.
 */
public class GTBedrockOreFeature extends Feature<NoneFeatureConfiguration> {

    public GTBedrockOreFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunkPos = new ChunkPos(context.origin());
        // One candidate origin per 16x16-chunk cell.
        if (Math.floorMod(chunkPos.x, 16) != 8 || Math.floorMod(chunkPos.z, 16) != 8) return false;

        // GT6 registers separate overworld and nether tables and gates them by dimension.
        Random gridRandom = gridRandom(level, chunkPos);
        GTBedrockOres.BedrockOre pick = select(level, gridRandom);
        if (pick == null) return false;

        int centerX = chunkPos.getMinBlockX() + 4 + gridRandom.nextInt(8);
        int centerZ = chunkPos.getMinBlockZ() + 4 + gridRandom.nextInt(8);

        RandomSource random = context.random();
        // §56: GT6's vein is chunk anchored (its shape spans the chunk's own 16x16 columns).
        if (!placeVein(level, random, chunkPos.getMinBlockX(), chunkPos.getMinBlockZ(), pick.material())) {
            return false;
        }

        // Indicator flowers on the surface above the deposit (the nether table has none).
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        if (pick.flowerId() != null) {
            Block flower = ForgeRegistries.BLOCKS.getValue(
                    new ResourceLocation(com.gregtech.gregtech.GregTech.NAMESPACE, pick.flowerId()));
            if (flower != null && flower != Blocks.AIR) {
                BlockState flowerState = flower.defaultBlockState();
                for (int attempt = 0; attempt < 6; attempt++) {
                    int x = centerX + random.nextInt(7) - 3;
                    int z = centerZ + random.nextInt(7) - 3;
                    int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).isAir()) continue;
                    BlockState ground = level.getBlockState(cursor.below());
                    if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.DIRT) && !ground.is(Blocks.SAND)
                            && !ground.is(Blocks.PODZOL) && !ground.is(Blocks.COARSE_DIRT)) continue;
                    if (!flowerState.canSurvive(level, cursor)) continue;
                    level.setBlock(new BlockPos(x, y, z), flowerState, 2);
                }
            }
        }
        // Surface rocks above the deposit hide raw ore chunks of the material.
        for (int attempt = 0; attempt < 3; attempt++) {
            GTRockPlacement.placeRock(level,
                    centerX + random.nextInt(9) - 4, centerZ + random.nextInt(9) - 4,
                    pick.material(), true);
        }
        return true;
    }

    private static Random gridRandom(WorldGenLevel level, ChunkPos chunkPos) {
        return GTOreVeinFeature.gridRandom(level.getSeed() ^ 0xBED7E5L, chunkPos.x, chunkPos.z);
    }

    /**
     * GT6's {@code WorldgenOresBedrock.generateVein} (§56): the vein is <b>chunk anchored</b>, not a
     * blob around a point.
     *
     * <ol>
     *   <li>The chunk needs bedrock at its centre column (GT6 checks {@code (minX+8, 0, minZ+8)}).</li>
     *   <li>Bedrock core: the 6x6 area {@code x/z = 5..10} at y=0 gets a large ore on a 1-in-6 roll
     *       and a small one on 2-in-6, and one large ore is forced in the middle so a deposit always
     *       has a breakable core.</li>
     *   <li>"Muffin" blob: layers y=1..6, each a square ring of GT6's own bounds
     *       ({@code tD1 = 5,4,2,1,0,2,5}, {@code tD2 = 11,12,14,15,16,14,11}) — so the vein is widest
     *       in the middle (the full 16x16 chunk at y=4). The first vein of a chunk replaces the stone
     *       around it with deepslate first (GT6's {@code GENERATED_NO_BEDROCK_ORE} flag); each block
     *       then rolls 1-in-6 for a full ore and 2-in-6 for a small one.</li>
     *   <li>Sprinkle: up to eight random walks from the blob up to the water level, drifting one
     *       block sideways on a 1-in-7 roll per axis, dropping small ores on two thirds of the steps.</li>
     * </ol>
     *
     * <p>Port deviation: GT6's {@code GENERATED_NO_BEDROCK_ORE} flag (only the first vein of a chunk
     * gets the deepslate fill) is not modelled — the port's deposits are one per 16x16-chunk grid
     * cell, so every one of them gets the fill.
     *
     * @return true when at least one ore block was placed
     */
    public static boolean placeVein(WorldGenLevel level, RandomSource random, int minX, int minZ,
                                    GTMaterial material) {
        if(material==null)return false;
        return MineralWorldgenRules.bedrockVein(minX,minZ,level.getMinBuildHeight(),level.getSeaLevel(),random::nextInt,new MineralWorldgenRules.VeinSink(){
            public boolean isBedrockFloor(int x,int y,int z){return level.getBlockState(new BlockPos(x,y,z)).is(Blocks.BEDROCK);}
            public void prepareStone(int x, int y, int z) {
                // WorldgenOresBedrock.generateVein replaces the entire muffin before rolling ores.
                BlockState host = level.getLevel().dimension() == net.minecraft.world.level.Level.NETHER
                        ? Blocks.NETHERRACK.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
                level.setBlock(new BlockPos(x, y, z), host, 2);
            }
            public boolean bedrock(int x,int y,int z,boolean small){return GTOreBlockResolver.placeBedrockOre(level,new BlockPos(x,y,z),material);}
            public boolean ore(int x,int y,int z,boolean small){if(level.isOutsideBuildHeight(y))return false;return GTOreBlockResolver.placeOre(level,new BlockPos(x,y,z),material,small);}
        });
    }

    /** GT6's muffin bounds: {@code tD1}/{@code tD2} indexed by the layer ({@code tY} 1..6). */
    public static final int[] MUFFIN_INNER = MineralWorldgenRules.MUFFIN_INNER;
    public static final int[] MUFFIN_OUTER = MineralWorldgenRules.MUFFIN_OUTER;

    /** A small ore inside the bedrock floor (GT6's {@code oreSmallBedrock}). */
    private static boolean placeSmallBedrockOre(WorldGenLevel level, BlockPos pos, GTMaterial material) {
        // The port keeps one ore block per material and records the background on the block entity.
        return GTOreBlockResolver.placeBedrockOre(level, pos, material);
    }

    /** One sprinkle block above the blob: a small ore in whatever stone is there. */
    private static boolean placeSprinkle(WorldGenLevel level, int x, int y, int z, GTMaterial material) {
        if (level.isOutsideBuildHeight(y)) return false;
        return GTOreBlockResolver.placeOre(level, new BlockPos(x, y, z), material, true);
    }

    /**
     * Weighted pick — weight inversely proportional to the GT6 rarity divisor, inside the table of
     * the level's dimension (the port generates bedrock ores in the Overworld and the Nether).
     */
    private static GTBedrockOres.BedrockOre select(WorldGenLevel level, Random random) {
        List<GTBedrockOres.BedrockOre> table = GTBedrockOreDimensions.forDimension(level.getLevel().dimension());
        long total = 0;
        for (GTBedrockOres.BedrockOre ore : table) {
            total += weight(ore);
        }
        if (total <= 0) return null;
        long w = (long) (random.nextDouble() * total);
        for (GTBedrockOres.BedrockOre ore : table) {
            w -= weight(ore);
            if (w < 0) return ore;
        }
        return null;
    }

    private static long weight(GTBedrockOres.BedrockOre ore) {
        return MineralWorldgenRules.bedrockWeight(ore.chance());
    }
}
