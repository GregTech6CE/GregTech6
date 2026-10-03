package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.block.misc.BumbleHiveBlock;
import com.gregtech.gregtech.blockentity.machine.SmelteryBlockEntityHelper;
import com.gregtech.gregtech.blockentity.misc.BumbleHiveBlockEntity;
import com.gregtech.gregtech.content.bumble.BumbleBeeGenes;
import com.gregtech.gregtech.content.bumble.BumbleBeeType;
import com.gregtech.gregtech.content.bumble.GTBumbleSpecies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Port of GT6's {@code WorldgenHives} ({@code gregtech/worldgen/WorldgenHives.java:54-204}): the wild
 * bumblebee colonies.
 *
 * <p>GT6 rolls one column per chunk ({@code tX = minX + rnd(16)}, {@code tZ = minZ + rnd(16)}) and
 * then, per dimension:</p>
 *
 * <table>
 *   <caption>GT6's hive passes</caption>
 *   <tr><th>pass</th><th>search</th><th>requirement</th><th>species / colour</th></tr>
 *   <tr><td>overworld underground ({@code :127-140})</td><td>y = 8..27</td>
 *       <td>natural rock, opaque, 5 of the 6 neighbours opaque and none liquid</td>
 *       <td>500 Stoned / light grey</td></tr>
 *   <tr><td>overworld surface ({@code :142-190})</td><td>y = height-50 down to 3</td>
 *       <td>first opaque, non-leaf/wood/ice block; then its five lower sides decide the colony</td>
 *       <td>see {@link #COLONIES}</td></tr>
 *   <tr><td>nether ({@code :104-111})</td><td>y = 16..111</td><td>netherrack embedded in 5 opaque sides</td>
 *       <td>300 Nether / {@code 0xaa0000}</td></tr>
 *   <tr><td>end ({@code :112-125})</td><td>y = 16..127, 1/3 chance</td><td>end stone embedded in 5 opaque sides</td>
 *       <td>400 End / {@code 0x00aaaa}</td></tr>
 * </table>
 *
 * <p>Both overworld passes run: a chunk can hold an underground <em>and</em> a surface colony.</p>
 *
 * <p>{@code placeHive} ({@code :195-204}) derives the colony's genome from the environment
 * ({@link BumbleBeeGenes#fromEnvironment}) and fills the hive with one comb of the species' tier, a
 * princess and {@code offspring} drones, all carrying that genome. The comb amount is GT6's
 * {@code UT.Code.units(workForce, 10000, 10, T)}, which rounds <em>up</em> — so it is
 * {@code ceil(work/1000)}, always 1..10.</p>
 *
 * <p>Port differences, all documented in {@code docs/PORTING_REMAINING_2026-09-14.md §75}:</p>
 * <ul>
 *   <li>Only the overworld, nether and end passes exist — GT6's Erebus, Betweenlands, Atum, Aether,
 *       Alfheim, Twilight Forest and space passes have no dimension in this port.</li>
 *   <li>The surface walk starts at 1.20.1's heightmap instead of GT6's fixed {@code 206}.</li>
 *   <li>GT6's absolute Y values go through {@link GTWorldgenScale#remapY}, so the rock pocket stays in
 *       the same layer above bedrock now that the overworld is 128 blocks deeper (the nether and the
 *       end keep GT6's own values).</li>
 *   <li>GT6 stores the hive colour as an NBT int and tints on the fly; the port bakes sixteen colour
 *       variants and carries the colour as a block state ({@link BumbleHiveBlock#COLOR}). GT6's three
 *       literal colours are mapped explicitly in {@link #dye(int)}.</li>
 *   <li>GT6's mod-biome trigger sets (magical, volcanic, lake, …) are mapped onto the vanilla 1.20.1
 *       biomes that stand in for them, or left empty when there is no stand-in.</li>
 * </ul>
 */
public class GTBumbleHivesFeature extends Feature<NoneFeatureConfiguration> {

    /** GT6's five "sides" of a hive position: the four horizontals plus down ({@code ALL_SIDES_HORIZONTAL_DOWN}). */
    private static final Direction[] SIDES_HORIZONTAL_DOWN =
            {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN};

    /** GT6's {@code ALL_SIDES_VALID}: every side, used by the embedded passes. */
    private static final Direction[] SIDES_ALL = Direction.values();

    /**
     * One of GT6's colony rules: the biome/contact trigger it came from, the species it spawns and
     * GT6's raw colour int ({@code NBT_COLOR}).
     */
    public record Colony(String trigger, int speciesId, int color, DyeColor dye) {}

    // GT6's surface-pass table (:155-186), in its own test order.
    public static final Colony WATER = new Colony("water", 100, 0x8080ff, DyeColor.LIGHT_BLUE);
    public static final Colony MAGICAL = new Colony("magical", 200, 0x800080, DyeColor.PURPLE);
    public static final Colony VOLCANIC = new Colony("volcanic", 300, 0x202020, DyeColor.BLACK);
    public static final Colony END = new Colony("end", 400, 0x00aaaa, DyeColor.CYAN);
    public static final Colony NETHER = new Colony("nether", 300, 0xaa0000, DyeColor.RED);
    public static final Colony SHROOM = new Colony("shroom", 800, 0xffc0c0, DyeColor.PINK);
    public static final Colony JUNGLE = new Colony("jungle", 600, 0x00ff00, DyeColor.GREEN);
    public static final Colony FROZEN = new Colony("frozen", 700, 0xffffff, DyeColor.WHITE);
    public static final Colony RED_SAND = new Colony("redSand", 900, 0xff0000, DyeColor.RED);
    public static final Colony SAND = new Colony("sand", 900, 0xffff00, DyeColor.YELLOW);
    public static final Colony ROCK = new Colony("rock", 500, 0xc0c0c0, DyeColor.LIGHT_GRAY);
    public static final Colony GRASS = new Colony("grass", 0, 0xffdd99, DyeColor.YELLOW);
    public static final Colony DIRT = new Colony("dirt", 0, 0x604000, DyeColor.BROWN);
    public static final Colony FALLBACK = MAGICAL;
    /** GT6's underground colony ({@code :135}): species 500, light grey. */
    public static final Colony UNDERGROUND = ROCK;

    /** GT6's {@code BIOMES_MAGICAL} stand-ins: the enchanted/mystic woods of 1.20.1. */
    public static final Set<ResourceLocation> MAGICAL_BIOMES = Set.of(id("flower_forest"), id("dark_forest"));
    /** GT6's {@code BIOMES_VOLCANIC} has no vanilla overworld stand-in (the nether pass covers 300). */
    public static final Set<ResourceLocation> VOLCANIC_BIOMES = Set.of();
    /** GT6's {@code BIOMES_FROZEN}, mapped onto 1.20.1's snowy biomes. */
    public static final Set<ResourceLocation> FROZEN_BIOMES = Set.of(
            id("snowy_plains"), id("snowy_taiga"), id("snowy_beach"), id("ice_spikes"),
            id("snowy_slopes"), id("frozen_peaks"), id("jagged_peaks"), id("grove"),
            id("frozen_river"), id("frozen_ocean"), id("deep_frozen_ocean"));
    /** GT6's {@code BIOMES_DESERT} / {@code BIOMES_MESA}: those hives are night colonies ({@code :131}). */
    public static final Set<ResourceLocation> DESERT_BIOMES = Set.of(
            id("desert"), id("badlands"), id("eroded_badlands"), id("wooded_badlands"));

    public GTBumbleHivesFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        // GT6 points one column per chunk at (minX + rnd(16), minZ + rnd(16)).
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        int x = minX + random.nextInt(16);
        int z = minZ + random.nextInt(16);
        if (level.getLevel().dimension() == Level.NETHER) return placeNether(level, x, z, random);
        if (level.getLevel().dimension() == Level.END) return placeEnd(level, x, z, random);
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        return placeOverworld(level, x, z, random);
    }

    // ── the overworld ─────────────────────────────────────────────────────────────────────────

    /** GT6's two overworld passes ({@code :126-190}); both may place a hive in the same chunk. */
    public static boolean placeOverworld(WorldGenLevel level, int x, int z, RandomSource random) {
        boolean placed = placeUnderground(level, x, z, random);
        placed |= placeSurface(level, x, z, random);
        return placed;
    }

    /**
     * GT6's rock-pocket pass ({@code :127-140}): y = 8..27 above bedrock, natural rock, five solid
     * sides. The band goes through {@link GTWorldgenScale#remapY} so it stays the same rock layer in
     * the 1.18+ deep world (the nether and the end keep GT6's own Y values).
     */
    public static boolean placeUnderground(WorldGenLevel level, int x, int z, RandomSource random) {
        for (int y = UNDERGROUND_MIN_Y; y < UNDERGROUND_MAX_Y; y++) {
            BlockPos pos = new BlockPos(x, GTWorldgenScale.remapY(level, y), z);
            BlockState state = level.getBlockState(pos);
            if (!isRock(state)) continue;
            if (!state.isSolidRender(level, pos)) continue;
            if (!embedded(level, pos)) continue;
            return placeHive(level, pos, UNDERGROUND, random, null);
        }
        return false;
    }

    /**
     * GT6's surface pass ({@code :142-190}): walk down to the first opaque, non-leaf/wood/ice block,
     * then let the first of its five lower sides that is free of collision pick the colony.
     */
    public static boolean placeSurface(WorldGenLevel level, int x, int z, RandomSource random) {
        int start = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int y = Math.min(start, level.getMaxBuildHeight() - 1); y > level.getMinBuildHeight() + 2; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState contact = level.getBlockState(pos);
            // GT6 bails out on the first liquid: no hive under water or inside lava.
            if (!contact.getFluidState().isEmpty()) return false;
            if (contact.is(BlockTags.LEAVES) || contact.is(BlockTags.LOGS) || contact.is(BlockTags.PLANKS)
                    || contact.is(BlockTags.ICE)) continue;
            if (!contact.isSolidRender(level, pos)) continue;
            BlockPos hivePos = pos.below();
            for (Direction side : SIDES_HORIZONTAL_DOWN) {
                BlockPos sidePos = hivePos.relative(side);
                if (hasCollision(level, sidePos)) continue;
                return placeHive(level, hivePos, colonyFor(level, sidePos, contact), random, null);
            }
            return false;
        }
        return false;
    }

    /** GT6's nether pass ({@code :104-111}): netherrack y = 16..111, five solid sides. */
    public static boolean placeNether(WorldGenLevel level, int x, int z, RandomSource random) {
        int y = GTWorldgenScale.remapY(level, 16 + random.nextInt(96));
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBlockState(pos).is(Blocks.NETHERRACK)) return false;
        if (!embedded(level, pos)) return false;
        return placeHive(level, pos, NETHER, random, null);
    }

    /** GT6's end pass ({@code :112-125}): a 1/3 roll, then the first end stone pocket. */
    public static boolean placeEnd(WorldGenLevel level, int x, int z, RandomSource random) {
        if (random.nextInt(3) > 0) return false;
        for (int y = 16; y < 128; y++) {
            BlockPos pos = new BlockPos(x, GTWorldgenScale.remapY(level, y), z);
            if (!level.getBlockState(pos).is(Blocks.END_STONE)) continue;
            if (embedded(level, pos)) return placeHive(level, pos, END, random, null);
        }
        return false;
    }

    // ── the colony table ──────────────────────────────────────────────────────────────────────

    /**
     * GT6's colour/species chain ({@code :155-186}). {@code side} is the hive position's free
     * neighbour the chain is running for, {@code contact} the ground block above it.
     */
    public static Colony colonyFor(WorldGenLevel level, BlockPos side, BlockState contact) {
        return colonyFor(level.getBiome(side),
                level.getBlockState(side).getFluidState().is(FluidTags.WATER), contact);
    }

    /**
     * The rule chain itself, with the biome and the water neighbour passed in: GT6 tests them in this
     * exact order, and the first match wins.
     */
    public static Colony colonyFor(Holder<Biome> biomeHolder, boolean waterNeighbour, BlockState contact) {
        // GT6: water/flowing water/a GT waterlike neighbour first, then the biome list, then the block.
        if (waterNeighbour) return WATER;
        ResourceLocation biome = biomeHolder.unwrapKey().map(ResourceKey::location).orElse(null);
        if (MAGICAL_BIOMES.contains(biome)) return MAGICAL;
        if (VOLCANIC_BIOMES.contains(biome)) return VOLCANIC;
        if (biomeHolder.is(BiomeTags.IS_END)) return END;
        if (biomeHolder.is(BiomeTags.IS_NETHER)) return NETHER;
        if (biomeHolder.is(Biomes.MUSHROOM_FIELDS)) return SHROOM;
        if (biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_BEACH)
                || biomeHolder.is(BiomeTags.IS_RIVER) || id("stony_shore").equals(biome)) return WATER;
        if (biomeHolder.is(BiomeTags.IS_JUNGLE)) return JUNGLE;
        if (FROZEN_BIOMES.contains(biome)) return FROZEN;
        if (contact.is(Blocks.MYCELIUM)) return SHROOM;
        if (contact.is(Blocks.RED_SAND)) return RED_SAND;
        if (contact.is(BlockTags.SAND) || contact.is(Blocks.SANDSTONE) || contact.is(Blocks.RED_SANDSTONE)
                || contact.is(Blocks.SMOOTH_SANDSTONE)) return SAND;
        if (contact.is(Blocks.GRAVEL) || isRock(contact)) return ROCK;
        if (contact.is(Blocks.GRASS_BLOCK) || contact.is(Blocks.MOSS_BLOCK)) return GRASS;
        if (contact.is(BlockTags.DIRT)) return DIRT;
        // GT6's own comment: magical bumbles are the default, so they stay obtainable.
        return FALLBACK;
    }

    // ── placing one hive ──────────────────────────────────────────────────────────────────────

    /** GT6's {@code placeHive} ({@code :195-204}). */
    public static boolean placeHive(WorldGenLevel level, BlockPos pos, Colony colony, RandomSource random,
                                    @Nullable CompoundTag overrideGenes) {
        GTBumbleSpecies.Species species = GTBumbleSpecies.byId(colony.speciesId());
        if (species == null) return false;
        Block hive = com.gregtech.gregtech.registry.GTBumbleBlocks.hive(colony.dye());
        if (hive == null) return false;

        CompoundTag genes = overrideGenes != null ? overrideGenes : genesFor(level, pos, random);
        BlockState state = hive.defaultBlockState();
        if (!level.setBlock(pos, state, 2)) return false;
        if (!(level.getBlockEntity(pos) instanceof BumbleHiveBlockEntity hiveEntity)) return false;

        // GT6: comb = UT.Code.units(workForce, 10000, 10, T) = ceil(work / 1000) of the tier's comb,
        // a princess of the species and `offspring` drones, all with the same genome.
        long combs = combCount(BumbleBeeGenes.workForce(genes));
        hiveEntity.inventory().setStackInSlot(0, comb(species.comb(), (int) combs));
        hiveEntity.inventory().setStackInSlot(1, BumbleBeeType.stack(species, BumbleBeeType.PRINCESS, genes, 1));
        int offspring = (int) BumbleBeeGenes.offspring(genes);
        if (offspring > 0) {
            hiveEntity.inventory().setStackInSlot(2,
                    BumbleBeeType.stack(species, BumbleBeeType.DRONE, genes, offspring));
        }
        return true;
    }

    /** GT6's {@code UT.Code.units(work, 10000, 10, T)}: rounds up, so 1..10. */
    public static long combCount(long workForce) {
        return Math.max(1, (workForce + 999) / 1000);
    }

    /**
     * The colony's fresh genome (GT6 {@code :197-202}): the overworld derives day/night from the
     * biome (deserts and mesas are night colonies), every other dimension from the time of day.
     */
    public static CompoundTag genesFor(WorldGenLevel level, BlockPos pos, RandomSource random) {
        Level serverLevel = level.getLevel();
        long temperature = SmelteryBlockEntityHelper.environmentTemperature(level, pos);
        float rainfall = level.getBiome(pos).value().getModifiedClimateSettings().downfall();
        boolean hasSky = serverLevel.dimensionType().hasSkyLight()
                && level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + 5;
        boolean day;
        boolean night;
        if (serverLevel.dimension() == Level.OVERWORLD) {
            boolean nocturnal = DESERT_BIOMES.contains(biomeId(level, pos));
            day = !nocturnal;
            night = nocturnal;
        } else {
            day = serverLevel.isDay();
            night = !day;
        }
        return BumbleBeeGenes.fromEnvironment(temperature, rainfall, hasSky, day, night, random);
    }

    // ── helpers ───────────────────────────────────────────────────────────────────────────────

    /** GT6's embedded check: five of the six sides opaque, none of them liquid. */
    public static boolean embedded(WorldGenLevel level, BlockPos pos) {
        int opaque = 0;
        for (Direction side : SIDES_ALL) {
            BlockPos neighbour = pos.relative(side);
            if (!level.getFluidState(neighbour).isEmpty()) return false;
            if (level.getBlockState(neighbour).isSolidRender(level, neighbour)) opaque++;
        }
        return opaque == 5;
    }

    /** GT6's {@code WD.hasCollide}: the block has a collision box. */
    public static boolean hasCollision(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    /** GT6's {@code NBT_COLOR} int as one of the port's sixteen baked hive colours. */
    public static DyeColor dye(int color) {
        return switch (color) {
            case 0x202020 -> DyeColor.BLACK;        // DYE_INT_Black
            case 0xff0000 -> DyeColor.RED;         // DYE_INT_Red
            case 0x00ff00 -> DyeColor.GREEN;       // DYE_INT_Green
            case 0x604000 -> DyeColor.BROWN;       // DYE_INT_Brown
            case 0x0000ff -> DyeColor.BLUE;        // DYE_INT_Blue
            case 0x800080 -> DyeColor.PURPLE;      // DYE_INT_Purple
            case 0x00ffff -> DyeColor.CYAN;        // DYE_INT_Cyan
            case 0xc0c0c0 -> DyeColor.LIGHT_GRAY;  // DYE_INT_LightGray
            case 0x808080 -> DyeColor.GRAY;        // DYE_INT_Gray
            case 0xffc0c0 -> DyeColor.PINK;        // DYE_INT_Pink
            case 0x80ff80 -> DyeColor.LIME;        // DYE_INT_Lime
            case 0xffff00 -> DyeColor.YELLOW;      // DYE_INT_Yellow
            case 0x8080ff -> DyeColor.LIGHT_BLUE;  // DYE_INT_LightBlue
            case 0xff00ff -> DyeColor.MAGENTA;     // DYE_INT_Magenta
            case 0xff8000 -> DyeColor.ORANGE;      // DYE_INT_Orange
            case 0xffffff -> DyeColor.WHITE;       // DYE_INT_White
            // GT6's literals: the end's 0x00aaaa and the nether's 0xaa0000 land on cyan and red;
            // the grass colony's 0xffdd99 is a pale tan whose nearest dye by RGB would be pink
            // (already the mushroom colony), so it is pinned to yellow on purpose.
            case 0x00aaaa -> DyeColor.CYAN;
            case 0xaa0000 -> DyeColor.RED;
            case 0xffdd99 -> DyeColor.YELLOW;
            default -> DyeColor.LIGHT_GRAY;
        };
    }

    /** A comb stack by item id, or an empty stack when the id is unknown. */
    public static ItemStack comb(String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", id));
        if (item == null || item == net.minecraft.world.item.Items.AIR || count <= 0) return ItemStack.EMPTY;
        return new ItemStack(item, Math.min(64, count));
    }

    private static ResourceLocation biomeId(WorldGenLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(ResourceKey::location).orElse(null);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    /** GT6's y range for the overworld rock pocket ({@code :127}). */
    public static final int UNDERGROUND_MIN_Y = 8;
    public static final int UNDERGROUND_MAX_Y = 28;

    /** Registry key of the configured feature ({@code worldgen/configured_feature/gt_bumble_hives.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath("gregtech", "gt_bumble_hives"));
    private static boolean isRock(BlockState state){return state.is(BlockTags.BASE_STONE_OVERWORLD)||state.is(BlockTags.STONE_BRICKS)||state.is(Blocks.OBSIDIAN)||state.is(Blocks.DEEPSLATE);}
}
