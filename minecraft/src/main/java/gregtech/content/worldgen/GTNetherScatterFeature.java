package gregtech.content.worldgen;

import gregtech.GregTechConfig;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.RockBlock;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialStackItemHelper;
import gregtech.platform.Mc;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of GT6's {@code WorldgenRacks} ({@code Loader_Worldgen}) — despite the name it does not
 * place racks: it scatters <em>loose items on the ground</em> in the Nether, i.e. GT6's multi-tile
 * 32757 "item lying around" (the same tile entity the surface rocks use).
 *
 * <p>GT6 skips half of the chunks, then makes 16 attempts: a random column, a ray from
 * {@code random(200 or 80) + 47} down up to 40 blocks to the first solid Nether ground, and then a
 * 1-in-24 table that depends on what the item lies on — nether brick gives Ancient Debris raw rocks,
 * soul sand gives quartz or leaves the spot empty, gravel gives flint, and everything else gets nether quartz,
 * glowstone, obsidian, basalt or blackstone rocks. The port keeps the table and the ray; the items
 * are the port's own material items (gems via {@code gem}, rocks via {@code rockGt}, raw ores via
 * {@code oreRaw}), placed as {@link RockBlock}s carrying that item.
 */
public class GTNetherScatterFeature extends Feature<NoneFeatureConfiguration> {
    /** GT6: {@code if (aRandom.nextBoolean()) return F;} — half of the chunks are skipped. */
    private static final int ATTEMPTS = 16;
    /** GT6's ray: from {@code random.nextInt(200 or 80) + 47} down at most 40 blocks. */
    private static final int RAY_LENGTH = 40;
    /** GT6's {@code aRandom.nextInt(24)} table. */
    private static final int TABLE_SIZE = 24;

    public GTNetherScatterFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GregTechConfig.Worldgen.SURFACE_ROCKS.enabled()) return false;
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (level.getLevel().dimension() != Level.NETHER) return false;
        if (random.nextBoolean()) return false; // GT6 skips half of the chunks
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        boolean placed = false;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            // GT6: `aRandom.nextInt(bedrock ? 200 : 80) + 47`, where 255 is bedrock in 1.7.10.
            int start =
                    random.nextInt(level.getBlockState(new BlockPos(x, 254, z)).isAir() ? 200 : 80) + 47;
            placed |= scatter(level, x, z, start, random);
        }
        return placed;
    }

    /**
     * One GT6 attempt: cast down from {@code start} to the first solid Nether ground and place the
     * table's item above it. Split out so one attempt can be driven on a prepared column.
     */
    private static boolean scatter(WorldGenLevel level, int x, int z, int start, RandomSource random) {
        int floor = Math.max(level.getMinBuildHeight(), start - RAY_LENGTH);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = start; y > floor; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (state.liquid()) return false; // GT6 breaks on liquids
            if (!state.isSolidRender(level, cursor)) continue; // air and plants are skipped
            BlockPos above = new BlockPos(x, y + 1, z);
            if (!level.getBlockState(above).isAir()) return false; // GT6 needs a free spot
            return placeAt(level, above, state, random);
        }
        return false;
    }

    /** GT6's 1-in-24 switch, including the contact-block special cases. */
    private static boolean placeAt(WorldGenLevel level, BlockPos pos, BlockState ground, RandomSource random) {
        int choice = random.nextInt(TABLE_SIZE);
        String itemId = "";
        String material = switch (choice) {
            case 0, 8, 9, 10, 11 -> "NetherQuartz";
            case 1, 4 -> "Glowstone";
            case 2, 3 -> "AncientDebris";
            case 5, 12 -> "Obsidian";
            case 6, 7 -> "";
            case 13, 14, 15 -> "Basalt";
            default -> "Blackstone";
        };
        boolean rawOre = choice == 2 || choice == 3;
        if (ground.is(Blocks.NETHER_BRICKS)) {
            // GT6: a quarter of the nether brick spots hide a raw Ancient Debris rock.
            if (random.nextInt(4) == 0) {
                material = "AncientDebris";
                rawOre = true;
            } else {
                material = "NetherQuartz";
                rawOre = false;
            }
        } else if (ground.is(Blocks.SOUL_SAND) || ground.is(Blocks.SOUL_SOIL)) {
            material = switch (choice) {
                case 6, 7 -> "";
                default -> "NetherQuartz";
            };
            rawOre = false;
        } else if (ground.is(Blocks.GRAVEL)) {
            itemId = "minecraft:flint";
        }
        if (itemId.isEmpty() && material.equals("NetherQuartz") && (choice == 0)) {
            // GT6 case 0 is a gem lying around rather than a rock.
            ItemStack gem = mat(MaterialPrefix.gem, "NetherQuartz");
            itemId = gem.isEmpty() ? "" : key(gem);
        } else if (itemId.isEmpty() && material.equals("Glowstone") && choice == 1) {
            ItemStack gem = mat(MaterialPrefix.gem, "Glowstone");
            itemId = gem.isEmpty() ? "" : key(gem);
        }
        if (itemId.isEmpty() && material.isEmpty()) return false;
        return GTRockPlacement.place(level, pos, material, itemId, rawOre);
    }

    private static ItemStack mat(MaterialPrefix prefix, String material) {
        GTMaterial resolved = GTMaterialRegistry.get(material);
        if (resolved == null || !resolved.resolve().isValid()) return ItemStack.EMPTY;
        return MaterialStackItemHelper.mat(prefix, resolved.resolve(), 1);
    }

    private static String key(ItemStack stack) {
        ResourceLocation id = Mc.itemId(stack.getItem());
        return id == null ? "" : id.toString();
    }
}
