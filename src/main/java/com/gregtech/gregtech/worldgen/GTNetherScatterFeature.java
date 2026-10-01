package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.block.RockBlock;
import com.gregtech.gregtech.blockentity.RockBlockEntity;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Port of GT6's {@code WorldgenRacks} ({@code Loader_Worldgen:619}) — despite the name it does not
 * place racks: it scatters <em>loose items on the ground</em> in the Nether, i.e. GT6's multi-tile
 * 32757 "item lying around" (the same tile entity the surface rocks use).
 *
 * <p>GT6 skips half of the chunks, then makes 16 attempts: a random column, a ray from
 * {@code random(200 or 80) + 47} down up to 40 blocks to the first solid Nether ground, and then a
 * 1-in-24 table that depends on what the item lies on — nether brick gives Ancient Debris raw rocks,
 * soul sand gives Gloomstone gems, gravel gives flint, and everything else gets nether quartz,
 * glowstone, obsidian, basalt or blackstone rocks. The port keeps the table and the ray; the items
 * are the port's own material items (gems via {@code gem}, rocks via {@code rockGt}, raw ores via
 * {@code oreRaw}), placed as {@link RockBlock}s carrying that item.
 */
public class GTNetherScatterFeature extends Feature<NoneFeatureConfiguration> {
    /** GT6: {@code if (aRandom.nextBoolean()) return F;} — half of the chunks are skipped. */
    public static final int ATTEMPTS = 16;
    /** GT6's ray: from {@code random.nextInt(200 or 80) + 47} down at most 40 blocks. */
    public static final int RAY_LENGTH = 40;
    /** GT6's {@code aRandom.nextInt(24)} table. */
    public static final int TABLE_SIZE = 24;

    /** One entry of GT6's 1-in-24 table. */
    public record Scatter(String name, String material, String itemId, boolean rawOre) {}

    /** The GT6 table in order; {@code itemId} wins over {@code material} when both are set. */
    public static final java.util.List<Scatter> TABLE = java.util.List.of(
            new Scatter("gem.netherquartz", "NetherQuartz", "", false),      // case 0
            new Scatter("gem.glowstone", "Glowstone", "", false),            // case 1
            new Scatter("raw.ancientdebris", "AncientDebris", "", true),     // case 2
            new Scatter("brick.quartz_or_debris", "AncientDebris", "", true),// case 3
            new Scatter("brick.glowstone_or_debris", "Glowstone", "", false),// case 4
            new Scatter("brick.obsidian_or_debris", "Obsidian", "", false),  // case 5
            new Scatter("soul.gloomstone", "Gloomstone", "", false),         // case 6
            new Scatter("soul.gloomstone2", "Gloomstone", "", false),        // case 7
            new Scatter("soul.quartz", "NetherQuartz", "", false),           // case 8
            new Scatter("soul.quartz2", "NetherQuartz", "", false),          // case 9
            new Scatter("soul.quartz3", "NetherQuartz", "", false),          // case 10
            new Scatter("soul.quartz4", "NetherQuartz", "", false),          // case 11
            new Scatter("rock.obsidian", "Obsidian", "", false),             // case 12
            new Scatter("rock.basalt", "Basalt", "", false),                 // case 13
            new Scatter("rock.basalt2", "Basalt", "", false),                // case 14
            new Scatter("rock.basalt3", "Basalt", "", false),                // case 15
            new Scatter("gravel.flint1", "Blackstone", "", false),           // cases 16-23 use flint on gravel
            new Scatter("gravel.flint2", "Blackstone", "", false),
            new Scatter("gravel.flint3", "Blackstone", "", false),
            new Scatter("gravel.flint4", "Blackstone", "", false),
            new Scatter("gravel.flint5", "Blackstone", "", false),
            new Scatter("gravel.flint6", "Blackstone", "", false),
            new Scatter("gravel.flint7", "Blackstone", "", false),
            new Scatter("gravel.flint8", "Blackstone", "", false));

    public GTNetherScatterFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
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
            int start = random.nextInt(level.getBlockState(new BlockPos(x, 254, z)).isAir() ? 200 : 80) + 47;
            placed |= scatter(level, x, z, start, random);
        }
        return placed;
    }

    /**
     * One GT6 attempt: cast down from {@code start} to the first solid Nether ground and place the
     * table's item above it. Public so the GameTest can drive a prepared column.
     */
    public static boolean scatter(WorldGenLevel level, int x, int z, int start, RandomSource random) {
        int floor = Math.max(level.getMinBuildHeight(), start - RAY_LENGTH);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = start; y > floor; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (state.liquid()) return false;                       // GT6 breaks on liquids
            if (!state.isSolidRender(level, cursor)) continue;       // air and plants are skipped
            BlockPos above = new BlockPos(x, y + 1, z);
            if (!level.getBlockState(above).isAir()) return false;   // GT6 needs a free spot
            return placeAt(level, above, state, random);
        }
        return false;
    }

    /** GT6's 1-in-24 switch, including the contact-block special cases. */
    public static boolean placeAt(WorldGenLevel level, BlockPos pos, BlockState ground, RandomSource random) {
        int choice = random.nextInt(TABLE_SIZE);
        Scatter entry = TABLE.get(choice);
        String itemId = "";
        String material = entry.material();
        boolean rawOre = entry.rawOre();
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
                case 6, 7 -> "Gloomstone";
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
        return place(level, pos, material, itemId, rawOre);
    }

    /** Places GT6's ground item: the port's rock block carrying a material and/or an explicit item. */
    public static boolean place(WorldGenLevel level, BlockPos pos, String material, String itemId, boolean rawOre) {
        BlockState ground = level.getBlockState(pos.below());
        BlockState rock = GTBlocks.ROCK.get().defaultBlockState()
                .setValue(RockBlock.GROUND, RockBlock.Ground.of(ground));
        if (!level.setBlock(pos, rock, 2)) return false;
        if (level.getBlockEntity(pos) instanceof RockBlockEntity be) {
            if (material != null && !material.isEmpty()) be.setMaterial(material);
            if (itemId != null && !itemId.isEmpty()) be.setItemId(itemId);
            be.setRawOre(rawOre);
        }
        return true;
    }

    private static ItemStack mat(MaterialPrefix prefix, String material) {
        GTMaterial resolved = GTMaterialRegistry.get(material);
        if (resolved == null || !resolved.resolve().isValid()) return ItemStack.EMPTY;
        return MaterialStackItemHelper.mat(prefix, resolved.resolve(), 1);
    }

    private static String key(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    /** Flints are vanilla items in the port (GT6 uses the vanilla flint too). */
    public static ItemStack flint() {
        return new ItemStack(Items.FLINT);
    }

    /** Registry key of the configured feature ({@code worldgen/configured_feature/gt_nether_scatter.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath("gregtech", "gt_nether_scatter"));

    /** GT6 registers {@code nether.rocks} through {@code GEN_NETHER}. */
    public static String featureId() {
        return GTFeatures.NETHER_SCATTER.getId().getPath();
    }
}
