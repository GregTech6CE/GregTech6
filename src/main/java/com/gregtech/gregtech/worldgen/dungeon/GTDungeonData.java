package com.gregtech.gregtech.worldgen.dungeon;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.content.plant.BedrockFlowers;
import com.gregtech.gregtech.block.misc.CoinPileBlock;
import com.gregtech.gregtech.block.misc.ColoredGlassBlock;
import com.gregtech.gregtech.block.misc.ConcreteBlock;
import com.gregtech.gregtech.block.misc.PileBlock;
import com.gregtech.gregtech.block.stone.GTStoneSlabBlock;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.blockentity.misc.CoinPileBlockEntity;
import com.gregtech.gregtech.blockentity.misc.PileBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.registry.GTDungeonBlocks;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTLasers;
import com.gregtech.gregtech.worldgen.GTSurfaceFlora;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Port of GT6's {@code DungeonData} ({@code gregapi/worldgen/dungeon/DungeonData.java}): the context a
 * {@link GTDungeonChunk} builds its cell with — the two rock types GT6 picked for this dungeon, the
 * colour theme, the layout, the room's grid indices and every block helper the room implementations
 * call.
 *
 * <p>GT6 hands every helper a <b>local</b> offset, relative to the cell origin at {@code (mX, mY, mZ)},
 * and mixes the two rocks by height: {@code aY == 2 ? secondary : primary}. All of that is kept here.</p>
 *
 * <p>The local Y range belongs to the cell type, exactly as in GT6: <b>corridors</b> are five blocks tall
 * ({@code 0..4} — floor, three room layers, ceiling, see {@link GTDungeonChunkCorridor}), while
 * <b>rooms</b> are nine blocks tall ({@code 0..8} — floor at 0, interior {@code 1..6}, ceiling at 7 and a
 * tile skin at 8, see {@link GTDungeonChunkRoomEmpty}). Negative Y ({@code -1}, {@code -2}, …) is
 * foundations and pillars.</p>
 *
 * <p>Port substitutions, all documented in {@code docs/PORTING_REMAINING_2026-09-14.md}:</p>
 * <ul>
 *   <li>GT6's own {@code BlocksGT.Concrete}/{@code Glass}/{@code GlowGlass} colour families use
 *       their 16-colour port blockstates, in GT6's dye metadata order.</li>
 *   <li>GT6's decorative multi-tiles (coin piles, ingot/plate/gem-plate piles, cups, shelves and the
 *       corridor's wall decorations) become the port's own blocks. The piles keep GT6's stored contents
 *       now: {@link #pile} fills a {@link PileBlockEntity} with a real material stack and {@link #coins}
 *       rolls GT6's sixteen {@code gt.coin.stacksize.<i>} faces onto a {@link CoinPileBlockEntity}, both
 *       under GT6's own NBT keys. The piles also draw GT6's own geometry from those stacks - one box per
 *       stored ingot and plate, one quarter-block coin per occupied face, in the material's colour
 *       ({@code PileBlock.STACK} and {@code CoinPileBlock.COINS}) - and the only thing GT6 draws that the
 *       port cannot is its per-face 3D coin image, the two 16x16 shape bitmaps with their 128x128 top
 *       and bottom sheets ({@code MultiTileEntityCoin.java:341-449}); the port draws GT6's flat coin
 *       instead, which is the renderer GT6 itself falls back to with its 3D coins off.</li>
 *   <li>Coins: GT6's dungeon writes no coin material at all ({@code DungeonData.java:169-173}), leaving
 *       its pile uncoloured, while the port's coins are real items. {@link #coins} therefore binds the
 *       pile to one metal of {@link #COIN_METALS}, GT6's own dungeon coin list
 *       ({@code WorldgenDungeonGT.java:195}). A dungeon-wide seed picks that metal once through
 *       {@link GTDungeonSharedState}, so every pile of the dungeon agrees without cross-chunk state.</li>
 *   <li>Loot: GT6 fills its own shelf/chest multi-tiles from {@code ChestGenHooks}. The port places
 *       its GT shelf and material chest with corresponding 1.20.1 loot tables.</li>
 *   <li>Flower pots: GT6 fills a vanilla pot with one of thirteen vanilla plants or its A/B indicator
 *       flowers. The port uses the original random choices and potted block variants, including
 *       harvest drops for its seventeen GT flowers.</li>
 *   <li>Lamps: 1.20.1 has no separate lit redstone lamp block, so {@link #lamp} sets
 *       {@code RedstoneLampBlock.LIT}.</li>
 *   <li>Keys: GT6 hands out five named dungeon keys, one per dungeon ({@code WorldgenDungeonGT:169-173}).
 *       The port registers the ten key items ({@code GTDungeonKeys}) and builds the five stacks
 *       ({@code GTDungeonFeature.keyStack}); the barracks, the workshop and the library put them into a
 *       container slot, and the portal rooms key their portals with them.</li>
 *   <li>Slabs: GT6's rooms use half blocks of their two rocks for the farm rims, the barracks walls, the
 *       entrance's staircase and the portal rooms' kerbs ({@code mSlabs[SIDE_*]}, see {@link #slab}).
 *       The port's stone family carries the same half blocks with the same six orientations
 *       ({@code GTStoneSlabBlock.FACING} = GT6's side constant), so those sites are GT6's again; only
 *       GT6's random brick variant roll of {@code bricks(...)} ({@code DungeonData:124}, meta
 *       {@code 3 + next(3)}) stays collapsed onto {@link StoneVariant#BRICKS}, for slabs and full
 *       blocks alike.</li>
 *   <li>Fluid containers: GT6's {@code MultiTileEntityBarrelWood}, {@code MultiTileEntityBarrelMetal},
 *       {@code MultiTileEntityBarometerGasCylinder} and {@code MultiTileEntityMeasuringPot} multi-tiles
 *       are the port's own tanks and vessels now - {@link #tank} places a {@code TankBlock} (wood
 *       barrel, metal drum) or a portable vessel block (gas cylinder, measuring pot) and fills it with
 *       GT6's fluid and amount. Earlier batches of the port used a vanilla barrel as the stand-in for
 *       those containers; the rooms name the GT6 multi-tile every port block stands for.</li>
 * </ul>
 */
public final class GTDungeonData {

    /** A corridor's local Y range, GT6 {@code 0..4} (floor, 3 room layers, ceiling). */
    public static final int CORRIDOR_FLOOR_Y = 0, CORRIDOR_CEILING_Y = 4;
    /** A room's local Y range, GT6 {@code 0..8} (floor, 6 interior layers, ceiling, tile skin). */
    public static final int ROOM_FLOOR_Y = 0, ROOM_CEILING_Y = 7, ROOM_SKIN_Y = 8;

    /** GT6 {@code CS.DYE_NAMES}: its coloured blocks use black=0 through white=15. */
    private static final DyeColor[] GT6_DYES = {
            DyeColor.BLACK, DyeColor.RED, DyeColor.GREEN, DyeColor.BROWN,
            DyeColor.BLUE, DyeColor.PURPLE, DyeColor.CYAN, DyeColor.LIGHT_GRAY,
            DyeColor.GRAY, DyeColor.PINK, DyeColor.LIME, DyeColor.YELLOW,
            DyeColor.LIGHT_BLUE, DyeColor.MAGENTA, DyeColor.ORANGE, DyeColor.WHITE};

    /** GT6 {@code BlocksGT.POT_FLOWER_TILES/METAS}, including all nine red-flower metadata values. */
    private static final Block[] POT_FLOWERS = {
            Blocks.POTTED_CACTUS, Blocks.POTTED_BROWN_MUSHROOM, Blocks.POTTED_RED_MUSHROOM,
            Blocks.POTTED_DANDELION, Blocks.POTTED_POPPY, Blocks.POTTED_BLUE_ORCHID,
            Blocks.POTTED_ALLIUM, Blocks.POTTED_AZURE_BLUET, Blocks.POTTED_RED_TULIP,
            Blocks.POTTED_ORANGE_TULIP, Blocks.POTTED_WHITE_TULIP, Blocks.POTTED_PINK_TULIP,
            Blocks.POTTED_OXEYE_DAISY};

    public final WorldGenLevel level;
    /** Cell origin (GT6's {@code mX}/{@code mY}/{@code mZ}). */
    public final int x, y, z;
    /** The two rock types GT6 rolled for this dungeon (GT6's {@code BlocksGT.stones[rnd]}). */
    public final StoneType primary, secondary;
    /** The dungeon's colour theme, {@code 0..15} (GT6's {@code mColor}). */
    public final int color, colorInversed;
    public final byte[][] layout;
    public final int roomX, roomZ, connectionCount;
    public final long[] keyIds;
    public final ItemStack[] keyStacks;
    public final boolean[] generatedKeys;
    public final Set<BlockPos> lightUpdates;
    public final Set<String> tags;
    public final RandomSource random;
    private final @Nullable GTDungeonSharedState.CellState sharedState;

    public GTDungeonData(WorldGenLevel level, int x, int y, int z, StoneType primary, StoneType secondary,
                         int color, byte[][] layout, int roomX, int roomZ, int connectionCount,
                         long[] keyIds, ItemStack[] keyStacks, boolean[] generatedKeys,
                         Set<BlockPos> lightUpdates, Set<String> tags, RandomSource random) {
        this(level, x, y, z, primary, secondary, color, layout, roomX, roomZ, connectionCount,
                keyIds, keyStacks, generatedKeys, lightUpdates, tags, random, null);
    }

    public GTDungeonData(WorldGenLevel level, int x, int y, int z, StoneType primary, StoneType secondary,
                         int color, byte[][] layout, int roomX, int roomZ, int connectionCount,
                         long[] keyIds, ItemStack[] keyStacks, boolean[] generatedKeys,
                         Set<BlockPos> lightUpdates, Set<String> tags, RandomSource random,
                         @Nullable GTDungeonSharedState.CellState sharedState) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.primary = primary;
        this.secondary = secondary;
        this.color = color & 15;
        this.colorInversed = 15 - this.color;
        this.layout = layout;
        this.roomX = roomX;
        this.roomZ = roomZ;
        this.connectionCount = connectionCount;
        this.keyIds = keyIds;
        this.keyStacks = keyStacks;
        this.generatedKeys = generatedKeys;
        this.lightUpdates = lightUpdates;
        this.tags = tags;
        this.random = random;
        this.sharedState = sharedState;
    }

    // ── GT6's probability helpers ─────────────────────────────────────────────────────────────

    public int next(int bound) { return random.nextInt(bound); }
    /** GT6: a random positive stack size. */
    public int nextStack() { return 1 + random.nextInt(64); }
    public int nextMetaA() { return next1in3() ? color : next(16); }
    public int nextMetaB() { return next1in3() ? colorInversed : next(16); }
    public boolean next1in2() { return random.nextBoolean(); }
    public boolean next1in3() { return next(3) < 1; }
    public boolean next1in4() { return next(4) < 1; }
    public boolean next1in5() { return next(5) < 1; }
    public boolean next1in6() { return next(6) < 1; }
    public boolean next1in7() { return next(7) < 1; }
    public boolean next1in8() { return next(8) < 1; }
    public boolean next1in9() { return next(9) < 1; }
    public boolean next2in3() { return next(3) < 2; }
    public boolean next2in5() { return next(5) < 2; }
    public boolean next2in7() { return next(7) < 2; }
    public boolean next3in4() { return next(4) < 3; }
    public boolean next3in5() { return next(5) < 3; }
    public boolean next4in5() { return next(5) < 4; }
    public boolean next5in6() { return next(6) < 5; }

    // ── the rock family (GT6's {@code DungeonData:124-151}) ────────────────────────────────────

    /** Whether this cell's local height uses the dungeon's secondary rock (GT6: {@code aY == 2}). */
    public StoneType rockAt(int localY) { return localY == 2 ? secondary : primary; }

    public boolean stone(int ax, int ay, int az, StoneVariant variant) {
        return set(ax, ay, az, GTBlocks.getStoneState(rockAt(ay), variant));
    }

    public boolean bricks(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.BRICKS); }
    public boolean brick(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.BRICKS); }
    public boolean cobbles(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.COBBLE); }
    public boolean cobble(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.COBBLE); }
    public boolean mossycobble(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.COBBLE_MOSSY); }
    public boolean cracked(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.BRICKS_CRACKED); }
    public boolean mossy(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.BRICKS_MOSSY); }
    public boolean chiseled(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.BRICKS_CHISELED); }
    public boolean tiles(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.TILES); }
    public boolean smalltiles(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.SMALL_TILES); }
    public boolean smallbricks(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.SMALL_BRICKS); }
    public boolean smooth(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.SMOOTH); }
    public boolean redstoned(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.BRICKS_REDSTONE); }
    public boolean reinforced(int ax, int ay, int az) { return stone(ax, ay, az, StoneVariant.BRICKS_REINFORCED); }
    public boolean air(int ax, int ay, int az) { return set(ax, ay, az, Blocks.AIR.defaultBlockState()); }

    // --- the rock slabs (GT6's {@code BlockStones.mSlabs[SIDE_*]}) ------------------------------

    /**
     * GT6's half block of one of the dungeon's two rocks, in the orientation GT6 names by its side
     * constant ({@code aData.mPrimary.mSlabs[SIDE_*]}, e.g.
     * {@code DungeonChunkRoomFarmCrop:42-55}).
     *
     * <p>GT6's {@code BlockMetaType.mSlabs} ({@code BlockMetaType:74-81}) holds six separate half-block
     * blocks of the same rock, indexed by GT6's side constants ({@code CS:516-521}):
     * {@code SIDE_Y_NEG = 0}, {@code SIDE_Y_POS = 1}, {@code SIDE_Z_NEG = 2}, {@code SIDE_Z_POS = 3},
     * {@code SIDE_X_NEG = 4}, {@code SIDE_X_POS = 5} - the same order as vanilla's {@link Direction}
     * ({@code DOWN, UP, NORTH, SOUTH, WEST, EAST}), and the port's {@link GTStoneSlabBlock} carries
     * exactly that orientation in its {@code FACING} property. Its six collision boxes
     * ({@code GTStoneSlabBlock:47-54}) are GT6's own block bounds
     * ({@code BlockMetaType:121-128}) block for block, so a GT6 side becomes a vanilla direction
     * without any mirroring.</p>
     *
     * <p>The rock is the cell's rock for that local height ({@link #rockAt}), exactly as in GT6's
     * {@code set(..., aY == 2 ? aSecondary : aPrimary, ...)} slab overloads.</p>
     *
     * @param variant the rock form (GT6's slab carries the same metadata as its full block, so
     *                {@code BlockStones.SMOTH} becomes {@link StoneVariant#SMOOTH} and
     *                {@code BlockStones.TILES} becomes {@link StoneVariant#TILES})
     * @param side    GT6's side constant, i.e. the half of the block the slab fills
     */
    public boolean slab(int ax, int ay, int az, StoneVariant variant, Direction side) {
        Block slab = GTBlocks.getStoneSlab(rockAt(ay), variant);
        if (slab == null) {
            // The port registers a slab for every rock/variant pair (Loader_Blocks:117-133); should one
            // ever be missing, GT6's half block cannot be expressed and the full block stands in.
            return stone(ax, ay, az, variant);
        }
        return set(ax, ay, az, slab.defaultBlockState().setValue(GTStoneSlabBlock.FACING, side));
    }

    /** GT6's slab by half: {@code true} is GT6's {@code SIDE_Y_POS} (upside down), {@code false} its {@code SIDE_Y_NEG}. */
    public boolean slab(int ax, int ay, int az, StoneVariant variant, boolean topHalf) {
        return slab(ax, ay, az, variant, topHalf ? Direction.UP : Direction.DOWN);
    }

    /** GT6's {@code smooth(x, y, z, mSlabs[side], ...)}: the smooth rock as a half block. */
    public boolean smoothSlab(int ax, int ay, int az, Direction side) {
        return slab(ax, ay, az, StoneVariant.SMOOTH, side);
    }

    /** GT6's {@code tiles(x, y, z, mSlabs[side], ...)}: the rock tiles as a half block. */
    public boolean tilesSlab(int ax, int ay, int az, Direction side) {
        return slab(ax, ay, az, StoneVariant.TILES, side);
    }

    /** GT6's {@code bricks(x, y, z, mSlabs[side], ...)}: the rock bricks as a half block. */
    public boolean bricksSlab(int ax, int ay, int az, Direction side) {
        return slab(ax, ay, az, StoneVariant.BRICKS, side);
    }

    /**
     * One of GT6's straight runs of tile slabs (the kerbs of {@link GTDungeonChunkRoomPortal}, GT6
     * {@code DungeonChunkRoomPortal:43-62} and its three siblings): {@code count} tile slabs starting at
     * the given local offset, stepping by {@code (dx, dz)} - GT6 writes every kerb as such a run, in
     * either direction. All of them use GT6's {@code mSlabs[0]}, the bottom half.
     */
    public boolean slabKerbs(int ax, int ay, int az, int count, int dx, int dz) {
        boolean placed = true;
        for (int i = 0; i < count; i++) {
            placed &= tilesSlab(ax + dx * i, ay, az + dz * i, Direction.DOWN);
        }
        return placed;
    }

    // ── the colour family (GT6's {@code DungeonData:153-166}) ──────────────────────────────────

    public boolean glass(int ax, int ay, int az) {
        return set(ax, ay, az, GTDecorBlocks.GLASS_CLEAR.get().defaultBlockState()
                .setValue(ColoredGlassBlock.COLOR, GT6_DYES[color]));
    }
    /** GT6's coloured glowing glass uses the same 16-colour metadata as its normal glass. */
    public boolean glassglow(int ax, int ay, int az) {
        return set(ax, ay, az, GTDecorBlocks.GLASS_GLOW.get().defaultBlockState()
                .setValue(ColoredGlassBlock.COLOR, GT6_DYES[color]));
    }
    public boolean colored(int ax, int ay, int az) {
        return set(ax, ay, az, GTDecorBlocks.CONCRETE.get().defaultBlockState()
                .setValue(ConcreteBlock.COLOR, GT6_DYES[color]));
    }

    /**
     * GT6's ceiling lamp: a redstone lamp, with a redstone brick above it when GT6 passes a non-zero offset
     * (the brick is what powers the lamp in GT6's own {@code lit_redstone_lamp} variant).
     *
     * <p>Port difference: GT6 wrote an <em>unlit</em> lamp when the offset was zero and then faked block
     * light 15 at that position through {@code WorldgenDungeonGT:296-302} (its {@code mLightUpdateCoords}
     * pass). 1.20.1 recomputes light from real emitters, so an unlit lamp would leave the dungeon dark —
     * the port therefore always places the lamp lit, which keeps GT6's light level without the hack.</p>
     */
    public boolean lamp(int ax, int ay, int az, int redstoneBrickOffset) {
        light(new BlockPos(x + ax, y + ay, z + az));
        if (redstoneBrickOffset != 0) redstoned(ax, ay + redstoneBrickOffset, az);
        return set(ax, ay, az, Blocks.REDSTONE_LAMP.defaultBlockState()
                .setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true));
    }

    // ── decorations (GT6's {@code DungeonData:169-348}) ────────────────────────────────────────

    /**
     * GT6's storage-room metal list ({@code DungeonChunkRoomStorage:45}), by port material name. GT6
     * passes it to {@code ingots_or_plates} for the loose piles of that room; the port's {@link #pile}
     * rolls from it when a room passes no list of its own.
     */
    public static final String[] PILE_METALS = {"Copper", "Copper", "Tin", "Bronze", "Iron", "Iron", "Iron",
            "Steel", "Steel", "StainlessSteel", "StainlessSteel", "DamascusSteel"};

    /**
     * GT6's gem list ({@code DungeonChunkRoomStorage:44}) reduced to the gem and crystal entries GT6's
     * {@code OP.plateGem} can carry (a gem or boule material with plates, {@code DungeonData:273-276}).
     * The list feeds {@link PileKind#GEM_PLATE} the same way {@link #PILE_METALS} feeds the other two
     * forms; entries the port has no item for are skipped by {@link #rollPileItem}, which walks the list.
     */
    public static final String[] PILE_GEMS = {"Diamond", "Emerald", "Ruby", "GreenSapphire", "BlueSapphire",
            "Amethyst", "NetherQuartz", "CertusQuartz", "Lapis", "Redstone", "Glowstone", "Apatite", "Coal"};

    /**
     * GT6's dungeon coin materials ({@code WorldgenDungeonGT.java:195}):
     * {@code UT.Code.select(MT.NULL, MT.Cu, MT.Cu, MT.Cu, MT.Ag, MT.Ag, MT.Au, MT.Au, MT.Pt)} - one of
     * eight equally likely entries, so copper 3/8, silver 2/8, gold 2/8 and platinum 1/8.
     *
     * <p>GT6's newer dungeon ({@code DungeonData.java:169-173}) writes no material at all and leaves its
     * pile uncoloured ({@code MultiTileEntityCoin.java:430-435}); the port's coin is a real item that has
     * to name the metal it is made of, so a dungeon pile rolls GT6's own coin list - the one the older
     * {@code WorldgenDungeonGT} used for exactly this purpose. The names are the port's
     * ({@code ElementMaterials.java:51} Copper, {@code :69} Silver and {@code :100} Platinum; Gold is the
     * element material of the same file).</p>
     */
    public static final String[] COIN_METALS =
            {"Copper", "Copper", "Copper", "Silver", "Silver", "Gold", "Gold", "Platinum"};

    // ── the storage room's crates (GT6's {@code DungeonChunkRoomStorage:39-46}) ─────────────────

    /**
     * GT6's crate material tables ({@code DungeonChunkRoomStorage:42-46}) as port materials, in GT6's
     * own order <em>and multiplicity</em> (a material listed three times is three times as likely).
     *
     * <p>The port keeps GT6's symbol facade ({@link com.gregtech.gregtech.data.generated.GT6Materials}),
     * so every entry is the port's material for that GT6 symbol. {@code MT.WOODS.*} is held by
     * {@code Woods}, the ore list by {@code Ores} and the dusts/gems by {@code Compounds}: GT6's dust
     * table mixes ore materials (salt, clay, bone, rare earth) with compounds, exactly as its symbol
     * names say.</p>
     *
     * <p>Kept out: GT6's {@code MT.Porcelain} (the port has no such material; one of 40 dust entries)
     * and the two other-mod tables ({@code sHexoriums} of Hexxit and {@code sInfused} of Thaumcraft),
     * which GT6 itself only uses when those mods are loaded.</p>
     */
    public static final GTMaterial[] CRATE_DUSTS = {
            GT6Materials.Ores.NaCl, GT6Materials.Ores.NaCl, GT6Materials.Ores.KCl, GT6Materials.Ores.KCl,
            GT6Materials.Compounds.Gunpowder, GT6Materials.Compounds.Gunpowder,
            GT6Materials.Compounds.Gunpowder, GT6Materials.Ores.Bone, GT6Materials.Ores.Bone,
            GT6Materials.Compounds.Asphalt, GT6Materials.Compounds.Asphalt, GT6Materials.Ores.Clay,
            GT6Materials.Compounds.ClayBrown, GT6Materials.Compounds.ClayRed,
            GT6Materials.Compounds.Bentonite, GT6Materials.Compounds.Palygorskite,
            GT6Materials.Compounds.Kaolinite, GT6Materials.Ores.RareEarth, GT6Materials.Compounds.Sugar,
            GT6Materials.Compounds.Cocoa, GT6Materials.Compounds.Coffee, GT6Materials.Compounds.Vanilla,
            GT6Materials.Compounds.PepperBlack, GT6Materials.Compounds.Curry, GT6Materials.Compounds.Wheat,
            GT6Materials.Compounds.Barley, GT6Materials.Compounds.Rye, GT6Materials.Compounds.Rice,
            GT6Materials.Compounds.Oat, GT6Materials.Compounds.OatAbyssal, GT6Materials.Compounds.Corn,
            GT6Materials.Compounds.Potato, GT6Materials.Compounds.WaxBee,
            GT6Materials.Compounds.WaxRefractory, GT6Materials.Compounds.Blaze,
            GT6Materials.Compounds.Breeze, GT6Materials.Compounds.Blizz, GT6Materials.Compounds.Blitz,
            GT6Materials.Compounds.Basalz};

    /** GT6's {@code sWoods} ({@code :43}): the crate material list of the wood quadrant. */
    public static final GTMaterial[] CRATE_WOODS = {
            GT6Materials.Woods.Oak, GT6Materials.Woods.Birch, GT6Materials.Woods.Spruce,
            GT6Materials.Woods.Jungle, GT6Materials.Woods.Acacia, GT6Materials.Woods.DarkOak,
            GT6Materials.Woods.Crimson, GT6Materials.Woods.Warped, GT6Materials.Woods.Compressed,
            GT6Materials.Woods.WoodRubber, GT6Materials.Woods.Maple, GT6Materials.Woods.Willow,
            GT6Materials.Woods.BlueMahoe, GT6Materials.Woods.Hazel, GT6Materials.Compounds.Cinnamon,
            GT6Materials.Woods.Coconut, GT6Materials.Woods.Rainbowood, GT6Materials.Woods.BlueSpruce,
            GT6Materials.Woods.Livingwood, GT6Materials.Woods.Greatwood, GT6Materials.Woods.Silverwood,
            GT6Materials.Woods.WoodTreated, GT6Materials.Woods.Weedwood, GT6Materials.Woods.Skyroot};

    /** GT6's {@code sGems} ({@code :44}): 47 entries covering gems, crystals, coals and lapis. */
    public static final GTMaterial[] CRATE_GEMS = {
            GT6Materials.Compounds.EnderPearl, GT6Materials.Compounds.EnderPearl,
            GT6Materials.Compounds.EnderEye, GT6Materials.Compounds.Diamond,
            GT6Materials.Compounds.DiamondPink, GT6Materials.Compounds.Emerald,
            GT6Materials.Compounds.Aquamarine, GT6Materials.Compounds.Ruby,
            GT6Materials.Compounds.GreenSapphire, GT6Materials.Compounds.BlueSapphire,
            GT6Materials.Compounds.Amethyst, GT6Materials.Compounds.Craponite, GT6Materials.Compounds.Amber,
            GT6Materials.Compounds.VoidQuartz, GT6Materials.Compounds.NetherQuartz,
            GT6Materials.Compounds.NetherQuartz, GT6Materials.Compounds.MilkyQuartz,
            GT6Materials.Compounds.MilkyQuartz, GT6Materials.Compounds.CertusQuartz,
            GT6Materials.Compounds.ChargedCertusQuartz, GT6Materials.Compounds.Lapis,
            GT6Materials.Compounds.Lapis, GT6Materials.Compounds.Lapis, GT6Materials.Compounds.Redstone,
            GT6Materials.Compounds.Redstone, GT6Materials.Compounds.Redstone,
            GT6Materials.Compounds.Glowstone, GT6Materials.Compounds.Glowstone,
            GT6Materials.Compounds.Gloomstone, GT6Materials.Compounds.Apatite,
            GT6Materials.Compounds.Apatite, GT6Materials.Compounds.Apatite, GT6Materials.Compounds.Coal,
            GT6Materials.Compounds.Coal, GT6Materials.Compounds.Coal, GT6Materials.Compounds.Coal,
            GT6Materials.Compounds.Coal, GT6Materials.Compounds.Charcoal, GT6Materials.Compounds.Charcoal,
            GT6Materials.Compounds.Charcoal, GT6Materials.Compounds.Charcoal,
            GT6Materials.Compounds.Charcoal, GT6Materials.Compounds.Lignite,
            GT6Materials.Compounds.Lignite, GT6Materials.Compounds.Lignite,
            GT6Materials.Compounds.Lignite, GT6Materials.Compounds.Lignite};

    /** GT6's {@code sOres} ({@code :46}): 54 entries, the raw-ore crates and the raw ore blocks. */
    public static final GTMaterial[] CRATE_ORES = {
            GT6Materials.Ores.TiO2, GT6Materials.Ores.TiO2, GT6Materials.Ores.MnO2,
            GT6Materials.Ores.MnO2, GT6Materials.Ores.MnO2, GT6Materials.Ores.Fe2O3,
            GT6Materials.Ores.Fe2O3, GT6Materials.Ores.Fe2O3, GT6Materials.Ores.Fe2O3,
            GT6Materials.Ores.Fe2O3, GT6Materials.Ores.Cassiterite, GT6Materials.Ores.Cassiterite,
            GT6Materials.Ores.Cassiterite, GT6Materials.Ores.Cassiterite,
            GT6Materials.Ores.Cassiterite, GT6Materials.Ores.Zeolite, GT6Materials.Ores.Pollucite,
            GT6Materials.Ores.Borax, GT6Materials.Ores.BrownLimonite,
            GT6Materials.Ores.YellowLimonite, GT6Materials.Ores.Garnierite,
            GT6Materials.Ores.Scheelite, GT6Materials.Ores.Wolframite, GT6Materials.Ores.Ferberite,
            GT6Materials.Ores.Huebnerite, GT6Materials.Ores.Tungstate, GT6Materials.Ores.Realgar,
            GT6Materials.Ores.Cinnabar, GT6Materials.Ores.Molybdenite, GT6Materials.Ores.Sphalerite,
            GT6Materials.Ores.Stibnite, GT6Materials.Ores.Pentlandite, GT6Materials.Ores.Chalcopyrite,
            GT6Materials.Ores.Arsenopyrite, GT6Materials.Ores.Cobaltite, GT6Materials.Ores.Galena,
            GT6Materials.Ores.Cooperite, GT6Materials.Ores.Tetrahedrite, GT6Materials.Ores.Kesterite,
            GT6Materials.Ores.Stannite, GT6Materials.Ores.Barite, GT6Materials.Ores.Celestine,
            GT6Materials.Ores.Ilmenite, GT6Materials.Ores.Bauxite, GT6Materials.Ores.Chromite,
            GT6Materials.Ores.Powellite, GT6Materials.Ores.Wulfenite,
            GT6Materials.Ores.Ferrovanadium, GT6Materials.Ores.Bastnasite, GT6Materials.Ores.Coltan,
            GT6Materials.Ores.Malachite, GT6Materials.Ores.Bromargyrite,
            GT6Materials.Ores.Smithsonite, GT6Materials.Ores.Sperrylite};

    /** GT6's {@code sMetals} ({@code :45}), the same list the loose piles of that room roll from. */
    public static final GTMaterial[] CRATE_METALS = {
            GT6Materials.Elements.Cu, GT6Materials.Elements.Cu, GT6Materials.Elements.Sn,
            GT6Materials.Compounds.Bronze, GT6Materials.Elements.Fe, GT6Materials.Elements.Fe,
            GT6Materials.Elements.Fe, GT6Materials.Compounds.Steel, GT6Materials.Compounds.Steel,
            GT6Materials.Compounds.StainlessSteel, GT6Materials.Compounds.StainlessSteel,
            GT6Materials.Compounds.DamascusSteel};

    /** GT6's two real crate sizes: sixteen contents in a partial crate, sixty-four in a full crate. */
    public enum CrateForm {
        RAW(BlockMaterialPrefix.crateGtRaw),
        GEM(BlockMaterialPrefix.crateGtGem),
        DUST(BlockMaterialPrefix.crateGtDust),
        INGOT(BlockMaterialPrefix.crateGtIngot),
        PLATE(BlockMaterialPrefix.crateGtPlate),
        PLATE_GEM(BlockMaterialPrefix.crateGtPlateGem),
        RAW64(BlockMaterialPrefix.crateGt64Raw),
        GEM64(BlockMaterialPrefix.crateGt64Gem),
        DUST64(BlockMaterialPrefix.crateGt64Dust),
        INGOT64(BlockMaterialPrefix.crateGt64Ingot),
        PLATE64(BlockMaterialPrefix.crateGt64Plate),
        PLATE_GEM64(BlockMaterialPrefix.crateGt64PlateGem);

        private final BlockMaterialPrefix prefix;

        CrateForm(BlockMaterialPrefix prefix) {
            this.prefix = prefix;
        }

        /** The port's block prefix behind this GT6 crate form. */
        public BlockMaterialPrefix prefix() {
            return prefix;
        }
    }

    /**
     * GT6's crate multi-tiles ({@code BlocksGT.crateGtDust} and friends, {@code :135-183}): one crate of
     * {@code form}, made of one material out of {@code materials}.
     *
     * <p>GT6's {@code set(IPrefixBlock[] aBlocks, ..., OreDictMaterial... aMaterials)}
     * ({@code DungeonData:209-210}) draws the <em>block</em> first and the <em>material</em> second -
     * Java evaluates the array index of the receiver before the arguments - so the port draws them in
     * that same order out of {@link #random}.</p>
     *
     * <p>Both GT6 sizes now have distinct prefixes and material weights. For the material, the port
     * walks the list from GT6's random index when a material has no such form
     * (GT6's wood list carries woods without plates, its gem list coals), so a crate is always placed
     * when the form exists at all.</p>
     *
     * @return whether a crate was placed
     */
    public boolean crate(int ax, int ay, int az, CrateForm form, GTMaterial[] materials) {
        Block block = pickCrate(form, materials, next(materials.length));
        return block != null && set(ax, ay, az, block.defaultBlockState());
    }

    /** GT6's {@code set(IPrefixBlock[] aBlocks, ...)}: a random block of {@code forms}, then a material. */
    public boolean crate(int ax, int ay, int az, CrateForm[] forms, GTMaterial[] materials) {
        CrateForm form = forms[next(forms.length)];
        return crate(ax, ay, az, form, materials);
    }

    /** GT6's {@code BlocksGT.blockRaw} ({@code :179-182}): a raw ore block of one of {@code materials}. */
    public boolean rawBlock(int ax, int ay, int az, GTMaterial[] materials) {
        Block block = pickBlock(BlockMaterialPrefix.blockRaw, materials, next(materials.length));
        return block != null && set(ax, ay, az, block.defaultBlockState());
    }

    /**
     * GT6's {@code set(BlocksGT.blockRaw, x, y, z, aMaterial.mID)} - the single material form, which the
     * bedrock mine uses for the pillars it grows out of its vein ({@code DungeonChunkRoomMiningBedrock:130-132}).
     *
     * @return whether the raw ore block of that material exists in the port and was placed
     */
    public boolean rawBlock(int ax, int ay, int az, GTMaterial material) {
        Block block = pickBlock(BlockMaterialPrefix.blockRaw, new GTMaterial[] {material}, 0);
        return block != null && set(ax, ay, az, block.defaultBlockState());
    }

    /** The port's crate block of a form and a random index into GT6's material list. */
    private static @Nullable Block pickCrate(CrateForm form, GTMaterial[] materials, int start) {
        return pickBlock(form.prefix(), materials, start);
    }

    /** A material block of {@code prefix}, walking GT6's list from {@code start} past missing forms. */
    private static @Nullable Block pickBlock(BlockMaterialPrefix prefix, GTMaterial[] materials, int start) {
        for (int step = 0; step < materials.length; step++) {
            GTMaterial material = materials[(start + step) % materials.length];
            var object = GTBlocks.getObject(prefix, material);
            if (object != null && object.isPresent()) return object.get();
        }
        return null;
    }

    /**
     * GT6's coin pile ({@code DungeonData.java:169-173}).
     *
     * <p>GT6 rolls every one of the sixteen faces with {@code next1in3() ? next(8) : 0}, then overwrites
     * one random face with {@code 1 + next(8)} - so a dungeon coin pile always carries at least one coin
     * and never more than eight per face. Both bytes are written through
     * {@link CoinPileBlockEntity#setFaceCount}, which keeps GT6's cap of sixteen coins per face
     * ({@code MultiTileEntityCoin.java:74}).</p>
     *
     * <p>Port difference: GT6's dungeon coins carry no material, the port's have to, so the pile is
     * bound to the dungeon's one chosen metal of {@link #COIN_METALS} afterwards ({@link #rollCoin}).
     * The choice comes from a separate seeded stream, so the cell's furniture rolls stay untouched.</p>
     */
    public boolean coins(int ax, int ay, int az) {
        if (!set(ax, ay, az, GTDecorBlocks.COIN_PILE.get().defaultBlockState())) return false;
        BlockPos pos = new BlockPos(x + ax, y + ay, z + az);
        if (level.getBlockEntity(pos) instanceof CoinPileBlockEntity pile) {
            for (int face = 0; face < CoinPileBlockEntity.FACES; face++) {
                pile.setFaceCount(face, next1in3() ? next(8) : 0);          // GT6 DungeonData:170
            }
            pile.setFaceCount(next(CoinPileBlockEntity.FACES), 1 + next(8)); // GT6 DungeonData:171
            // GT6 hands the multi-tile its material through the placed NBT; the port has to set it on
            // the block entity. After the faces, so that GT6's draws above are not disturbed.
            pile.setCoin(rollCoin(ax, ay, az));
        }
        return true;
    }

    /**
     * One coin of GT6's {@link #COIN_METALS}, selected from a stream of its own.
     *
     * <p>Worldgen uses the dungeon-wide metal from {@link GTDungeonSharedState}. Standalone room tests
     * and direct helper calls retain the position-seeded fallback. Neither draws from the cell's room
     * stream, so later furniture and loot rolls stay untouched.</p>
     */
    private ItemStack rollCoin(int ax, int ay, int az) {
        if (sharedState != null) {
            ItemStack dungeonCoin = GTItems.getStack(MaterialPrefix.coin,
                    GTMaterialRegistry.get(sharedState.coinMetal()));
            return dungeonCoin.isEmpty() ? CoinPileBlock.defaultCoin() : dungeonCoin;
        }
        RandomSource pick = RandomSource.create(level.getSeed()
                ^ ((long) (x + ax) * 341873128712L)
                ^ ((long) (z + az) * 132897987541L)
                ^ (long) (y + ay) * 42317861L);
        ItemStack coin = GTItems.getStack(MaterialPrefix.coin,
                GTMaterialRegistry.get(COIN_METALS[pick.nextInt(COIN_METALS.length)]));
        return coin.isEmpty() ? CoinPileBlock.defaultCoin() : coin;
    }

    /**
     * GT6's {@code UT.Code.select(aReplacement, aArray)} ({@code UT.java}, the {@code E...} overload):
     * one random element of a list, or the replacement when the list is empty.
     *
     * <p>GT6's dungeon calls this for things whose choice is <b>not</b> part of the dungeon's own stream:
     * the storage room picks its tank fluid that way ({@code DungeonChunkRoomStorage:70},
     * {@code UT.Code.select(NF, tFluids[tType])}), because {@code UT.Code.select} draws from
     * {@code RNGSUS}, GT6's shared global RNG. The port therefore draws it from a stream of its own
     * ({@link #wandering}) instead of {@link #random} - every later roll of the cell, and with it the
     * dungeon's layout, has to stay exactly where GT6 left it.</p>
     */
    public <T> T select(T[] array, int ax, int ay, int az) {
        return array[wandering(ax, ay, az).nextInt(array.length)];
    }

    /**
     * The port's stand-in for GT6's {@code RNGSUS} at one position: a stream seeded from the world seed
     * and the coordinates, so it is reproducible and consumes nothing from {@link #random}. The salt
     * keeps it apart from {@link #rollCoin}'s stream, which is seeded the same way.
     */
    private RandomSource wandering(int ax, int ay, int az) {
        return RandomSource.create(level.getSeed()
                ^ ((long) (x + ax) * 341873128712L)
                ^ ((long) (z + az) * 132897987541L)
                ^ (long) (y + ay) * 42317861L
                ^ 0x5DEECE66DL);
    }

    /**
     * One of the port's fluid containers by registry name - GT6 hands a dungeon room the numeric
     * multi-tile id (32714 wooden barrel, 32102 bronze drum, 32055 gas cylinder, ...), and this is the
     * port's translation of that id. Falls back to the plain wood barrel like the workshop's own copy
     * used to; the port registers every id used by the dungeon unconditionally.
     */
    public static Block container(String id) {
        Block block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", id));
        return block == null ? com.gregtech.gregtech.registry.GTTanks.WOOD_BARREL.get() : block;
    }

    /**
     * A coin pile with one face filled to a fixed count - the port's explicit form of GT6's face bytes
     * ({@code DungeonData:170-171}), for a room that wants a specific pile and for tests.
     *
     * @param face  GT6's face index, {@code 0..15} (see {@link CoinPileBlockEntity#faceAt})
     * @param count coins on that face, clamped into GT6's cap
     */    public boolean coin(int ax, int ay, int az, int face, int count) {
        if (!set(ax, ay, az, GTDecorBlocks.COIN_PILE.get().defaultBlockState())) return false;
        BlockPos pos = new BlockPos(x + ax, y + ay, z + az);
        if (level.getBlockEntity(pos) instanceof CoinPileBlockEntity pile) {
            pile.setCoin(rollCoin(ax, ay, az));
            pile.setFaceCount(face, count);
        }
        return true;
    }

    /**
     * GT6's loose ingot / plate / gem-plate pile on the floor ({@code DungeonData.java:265-276}), whose
     * multi-tiles carry one stack in {@code NBT_VALUE} - the port's {@link PileBlockEntity} carries the
     * same stack, filled here.
     *
     * <p>GT6 rolls the material out of the list the room hands it and the size with
     * {@code aStackSize <= 0 ? nextStack() : ...} ({@code :260-274}, {@code nextStack()} is
     * {@code 1 + next(64)} at {@code :91}); this overload takes those defaults, i.e. a random metal of
     * {@link #PILE_METALS} (a random gem of {@link #PILE_GEMS} for a gem plate pile) with a size of
     * {@code 1..64}.</p>
     *
     * <p>Rooms that need GT6's two-candidate ingot/plate draw use
     * {@link #ingotsOrPlates(int, int, int, int, String...)} instead.</p>
     */
    public boolean pile(int ax, int ay, int az, PileKind kind) {
        return pile(ax, ay, az, kind, 0);
    }

    /** GT6's {@code ingots}/{@code plates}/{@code gemplates} with their {@code aStackSize} argument. */
    public boolean pile(int ax, int ay, int az, PileKind kind, int stackSize) {
        return pile(ax, ay, az, kind, stackSize,
                kind == PileKind.GEM_PLATE ? PILE_GEMS : PILE_METALS);
    }

    /**
     * GT6's {@code ingots(x, y, z, aStackSize, materials...)} and friends ({@code DungeonData:265-276}):
     * one material out of {@code materials}, one stack size,
     * {@code aStackSize <= 0 ? nextStack() : bindStack(aStackSize)}.
     */
    public boolean pile(int ax, int ay, int az, PileKind kind, int stackSize, String... materials) {
        String[] fallback = kind == PileKind.GEM_PLATE ? PILE_GEMS : PILE_METALS;
        String[] pool = materials == null || materials.length == 0 ? fallback : materials;
        ItemStack item = rollPileItem(kind, pool);
        if (item.isEmpty()) return false;
        int size = stackSize <= 0 ? nextStack() : Math.max(1, Math.min(stackSize, PileBlockEntity.MAX_SIZE));
        return pileStack(ax, ay, az, kind, item.copyWithCount(size));
    }

    /**
     * GT6 {@code DungeonData.ingots_or_plates:259-264}: draw an ingot material and size, then a plate
     * material and size, and only then decide which valid candidate is placed. Both candidate draws
     * matter to every later decoration in the dungeon's random stream.
     */
    public boolean ingotsOrPlates(int ax, int ay, int az, int stackSize, String... materials) {
        String[] pool = materials == null || materials.length == 0 ? PILE_METALS : materials;
        ItemStack ingot = rollPileItem(PileKind.INGOT, pool);
        int ingotCount = stackSize <= 0 ? nextStack() : Math.max(1, Math.min(stackSize, PileBlockEntity.MAX_SIZE));
        ItemStack plate = rollPileItem(PileKind.PLATE, pool);
        int plateCount = stackSize <= 0 ? nextStack() : Math.max(1, Math.min(stackSize, PileBlockEntity.MAX_SIZE));
        if (!ingot.isEmpty()) {
            if (!plate.isEmpty() && next1in2())
                return pileStack(ax, ay, az, PileKind.PLATE, plate.copyWithCount(plateCount));
            return pileStack(ax, ay, az, PileKind.INGOT, ingot.copyWithCount(ingotCount));
        }
        return !plate.isEmpty() && pileStack(ax, ay, az, PileKind.PLATE, plate.copyWithCount(plateCount));
    }

    /**
     * A pile of an explicit stack - GT6's {@code set(..., ST.save(NBT_VALUE, aStack))} with the stack
     * already at hand, for a room that knows its material.
     *
     * <p>Like GT6, an invalid stack places nothing at all ({@code ST.valid(aStack) && set(...)},
     * {@code DungeonData:265-276}); the port's pile then clamps the stack to its own cap of 64
     * ({@code MultiTileEntityPlaceable.java:82}) when it takes it.</p>
     */
    public boolean pileStack(int ax, int ay, int az, PileKind kind, @Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!set(ax, ay, az, kind.blockKind().block().defaultBlockState())) return false;
        BlockPos pos = new BlockPos(x + ax, y + ay, z + az);
        if (level.getBlockEntity(pos) instanceof PileBlockEntity pile) pile.add(stack.copy());
        return true;
    }

    /**
     * GT6's {@code OP.<prefix>.mat(material, 1)} for a dungeon pile ({@code DungeonData:260-274}): one
     * item of the form, of a material the port actually registers an item for.
     *
     * <p>GT6 draws {@code aMaterials[next(aMaterials.length)]} once; the port starts at the same random
     * index and walks the list, because a port material may lack the form (GT6's gem list holds coals and
     * lapis, which have no gem plate). Should the whole list come up empty - GT6's list names materials
     * the port does not import - the first material of the port with that form stands in, and an empty
     * result means the form has no item at all ({@link #pileStack} then places nothing, as GT6 does).</p>
     */
    private ItemStack rollPileItem(PileKind kind, String[] pool) {
        MaterialPrefix prefix = kind.blockKind().prefix();
        int start = pool.length == 0 ? 0 : next(pool.length);
        for (int offset = 0; offset < pool.length; offset++) {
            ItemStack stack = GTItems.getStack(prefix, GTMaterialRegistry.get(pool[(start + offset) % pool.length]));
            if (!stack.isEmpty()) return stack;
        }
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            ItemStack stack = GTItems.getStack(prefix, material);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** GT6's three pile forms, each pointing at the port's pile block and its ore prefix. */
    public enum PileKind {
        INGOT(PileBlock.Kind.INGOT),
        PLATE(PileBlock.Kind.PLATE),
        GEM_PLATE(PileBlock.Kind.GEM_PLATE);

        private final PileBlock.Kind blockKind;

        PileKind(PileBlock.Kind blockKind) {
            this.blockKind = blockKind;
        }

        /** The port's pile form (block and ore prefix) behind this dungeon form. */
        public PileBlock.Kind blockKind() {
            return blockKind;
        }
    }

    /** GT6 places a dungeon chest with a vanilla loot category; the port uses the 1.20.1 table. */
    public boolean chest(int ax, int ay, int az, String lootTable) {
        return chest(ax, ay, az, lootTable, com.gregtech.gregtech.registry.GTStorage.chest(com.gregtech.gregtech.content.material.Materials.Steel), shelfFacing(ax, az));
    }

    /** GT6 steel mechanical/key safes 2010/3010, retaining the room's loot and key. */
    public boolean safe(int ax, int ay, int az, String lootTable, long keyId, boolean keyLocked) {
        var block = com.gregtech.gregtech.registry.GTStorage.safe(com.gregtech.gregtech.content.material.Materials.Steel, keyLocked);
        if (!set(ax, ay, az, block.defaultBlockState().setValue(com.gregtech.gregtech.block.inventory.SafeBlock.FACING, shelfFacing(ax, az)))) return false;
        if (level.getBlockEntity(new BlockPos(x + ax, y + ay, z + az)) instanceof com.gregtech.gregtech.blockentity.inventory.SafeBlockEntity safe) {
            if (keyLocked) safe.setKeyId(keyId);
            safe.setDungeonLoot(dungeonLootId(lootTable), random.nextLong());
        }
        return true;
    }

    public boolean chest(int ax, int ay, int az, String lootTable,
                         com.gregtech.gregtech.block.inventory.MetalChestBlock block, Direction facing) {
        if (!set(ax, ay, az, block.defaultBlockState().setValue(com.gregtech.gregtech.block.inventory.MetalChestBlock.FACING, facing))) return false;
        BlockPos pos = new BlockPos(x + ax, y + ay, z + az);
        if (level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity chest) {
            chest.setDungeonLoot(dungeonLootId(lootTable), random.nextLong());
        }
        return true;
    }

    /** GT6 fish-farm chest IDs 508, 509, 510: brass, bronze, steel reinforcement. */
    public boolean fishFarmChest(int ax, int ay, int az, String lootTable, Direction facing) {
        var material = switch (next(3)) {
            case 0 -> com.gregtech.gregtech.content.material.Materials.Brass;
            case 1 -> com.gregtech.gregtech.content.material.Materials.Bronze;
            default -> com.gregtech.gregtech.content.material.Materials.Steel;
        };
        return chest(ax, ay, az, lootTable, com.gregtech.gregtech.registry.GTStorage.reinforcedChest(material), facing);
    }

    public boolean corridorChest(int ax, int ay, int az, String lootTable) {
        boolean wooden = next1in2();
        var material = switch (next(3)) {
            case 0 -> com.gregtech.gregtech.content.material.Materials.Brass;
            case 1 -> com.gregtech.gregtech.content.material.Materials.Bronze;
            default -> com.gregtech.gregtech.content.material.Materials.Steel;
        };
        return chest(ax, ay, az, lootTable, wooden ? com.gregtech.gregtech.registry.GTStorage.reinforcedChest(material)
                : com.gregtech.gregtech.registry.GTStorage.chest(material), shelfFacing(ax, az));
    }

    /** Shelf loot is stored on the block entity and rolled lazily on interaction or destruction. */
    /** Existing room shelves are against a chunk wall; their front points into the room. */
    public static Direction shelfFacing(int x, int z) {
        int xWall = Math.min(x, 15 - x), zWall = Math.min(z, 15 - z);
        return xWall < zWall ? (x < 8 ? Direction.EAST : Direction.WEST)
                : (z < 8 ? Direction.SOUTH : Direction.NORTH);
    }

    public static ResourceLocation dungeonLootId(String name) {
        String path = name.contains(":") || name.contains("/") ? name : "chests/" + name;
        path = path.replace("chests/village_weaponsmith", "chests/village/village_weaponsmith");
        return ResourceLocation.parse(path);
    }

    public boolean shelf(int ax, int ay, int az, @Nullable String lootTable) {
        return shelf(ax, ay, az, lootTable, null, 0);
    }

    /**
     * GT6's shelving with one of the dungeon's key stacks inside it
     * ({@code DungeonChunkBarracks:111-121}, {@code DungeonChunkRoomLibrary:122-128}): GT6 rolls the
     * slot with {@code next(28)} (barracks, hinting at a back slot with the cobble behind the shelf) or
     * {@code next(14)} (library) and stores the key stack there.
     *
     * <p>The port's shelf has GT6's 28 slots ({@code GTBookList.SLOTS}), front half first, so GT6's slot
     * index is used unchanged. GT6 hands the stack to its own shelf inventory; the port writes it into
     * the {@code BookShelfBlockEntity}'s handler, which accepts anything when written directly and hands
     * the key back when the shelf is broken - like GT6's shelf, whose inventory drops with it.</p>
     */
    public boolean shelf(int ax, int ay, int az, @Nullable String lootTable, @Nullable ItemStack key, int keySlot) {
        if (!set(ax, ay, az, GTDecorBlocks.BOOKSHELF.get().defaultBlockState()
                .setValue(com.gregtech.gregtech.block.BookShelfBlock.FACING, shelfFacing(ax, az)))) return false;
        BlockPos pos = new BlockPos(x + ax, y + ay, z + az);
        if (level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity shelf) {
            if (lootTable != null) shelf.setDungeonLoot(dungeonLootId(lootTable), null, random.nextLong());
            if (key != null && !key.isEmpty() && keySlot >= 0 && keySlot < shelf.inventory().getSlots()) {
                shelf.inventory().setStackInSlot(keySlot, key.copy());
            }
            shelf.setChanged();
        }
        return true;
    }

    /**
     * GT6's chest multi-tile with a fixed inventory instead of a loot table - the supply chest of the
     * Nether portal room ({@code DungeonChunkRoomPortalNether:41-48}, multi-tile 502 with
     * {@code NBT_INV_LIST}).
     */
    public boolean chestContents(int ax, int ay, int az, ItemStack... contents) {
        if (!set(ax, ay, az, com.gregtech.gregtech.registry.GTStorage.reinforcedChest(com.gregtech.gregtech.content.material.Materials.Gold)
                .defaultBlockState().setValue(com.gregtech.gregtech.block.inventory.MetalChestBlock.FACING, shelfFacing(ax, az)))) return false;
        BlockPos pos = new BlockPos(x + ax, y + ay, z + az);
        if (level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity chest) {
            for (int slot = 0; slot < contents.length; slot++) {
                ItemStack stack = contents[slot];
                if (stack != null && !stack.isEmpty()) chest.inventory().setStackInSlot(slot, stack.copy());
            }
        }
        return true;
    }

    /**
     * One of the port's own fluid containers with GT6's contents - the dungeon's replacement for the
     * vanilla barrels an earlier batch of this port used as stand-ins.
     *
     * <p>GT6 writes a filled container as
     * {@code aData.set(x, y, z, SIDE_UNKNOWN, id, new FluidTankGT(FL.X.make(n)).writeToNBT(..., NBT_TANK), T, T)}
     * ({@code DungeonChunkRoomWorkshop:51}, {@code :167}, {@code :196}), so the fluid and its amount
     * belong to the block. The port owns both halves now and this helper fills whichever the block
     * carries:</p>
     * <ul>
     *   <li>a {@link com.gregtech.gregtech.block.machine.TankBlock} (wood barrel, metal drum) keeps its
     *       fluid in a {@link com.gregtech.gregtech.blockentity.machine.TankBlockEntity}, which is
     *       filled through its tank - GT6's {@code FluidTankGT} of a
     *       {@code MultiTileEntityBarrelWood}/{@code MultiTileEntityBarrelMetal};</li>
     *   <li>a portable vessel block (gas cylinder, measuring pot) keeps it in the vessel <em>item</em>
     *       of a {@link com.gregtech.gregtech.blockentity.tool.PortableContainerBlockEntity}, which is
     *       filled through its fluid handler - GT6's {@code MultiTileEntityBarometerGasCylinder} and
     *       {@code MultiTileEntityMeasuringPot}.</li>
     * </ul>
     *
     * <p>Port difference: GT6 stores the fluid in its tile entity NBT and the port splits it that way,
     * so the amount is written after the block was placed and the block entity exists (the same two
     * step pattern {@link #chest} and {@link #coins} use). A {@code null} stack places the container
     * empty, which is what GT6's own measuring pot write does.</p>
     */
    public boolean tank(int ax, int ay, int az, Block block, @Nullable FluidStack fluid) {
        if (!set(ax, ay, az, block.defaultBlockState())) return false;
        if (fluid == null || fluid.isEmpty()) return true;
        BlockPos pos = new BlockPos(x + ax, y + ay, z + az);
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof TankBlockEntity tank) {
            tank.getFluidTank().setFluid(fluid.copy());
            return true;
        }
        if (entity != null) {
            IFluidHandler handler = entity.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
            if (handler != null) handler.fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
        }
        return true;
    }

    /**
     * One of the portal rooms' portal blocks (GT6's miniature portal multi-tiles 32766 and 32000 as the
     * port's own block, see {@code DungeonPortalBlock}): the block remembers the id of the dungeon key
     * that opens it, which is what GT6's portal multi-tile would carry in its own predecessor's
     * {@code gt.key} tag.
     */
    public boolean portal(int ax, int ay, int az, Block block, long keyId) {
        if (!set(ax, ay, az, block.defaultBlockState())) return false;
        BlockPos pos = new BlockPos(x + ax, y + ay, z + az);
        if (level.getBlockEntity(pos)
                instanceof com.gregtech.gregtech.block.misc.DungeonPortalBlockEntity portal) {
            portal.setKeyId(keyId);
        }
        return true;
    }

    /** GT6's 250 mB cup, empty when the original room supplies no drink. */
    public boolean cup(int ax, int ay, int az) {
        return cup(ax, ay, az, null);
    }

    /** GT6 {@code DungeonData.cup}: multi-tile 32739 with a 250 mB {@code NBT_TANK}. */
    public boolean cup(int ax, int ay, int az, @Nullable String fluidField) {
        FluidStack fluid = fluidField == null ? null : GTFluids.stack(fluidField, 250);
        if (fluidField != null && fluid == null) return false;
        Block cup = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                com.gregtech.gregtech.GregTech.id("fluid_cup"));
        return cup != null && tank(ax, ay, az, cup, fluid);
    }

    /** GT6 {@code DungeonData.pot}: draw the vanilla-flower index first, then select vanilla/A/B. */
    public boolean pot(int ax, int ay, int az) {
        int vanillaIndex = next(POT_FLOWERS.length);
        Block pot;
        if (next1in2()) {
            pot = POT_FLOWERS[vanillaIndex];
        } else if (next1in2()) {
            // BlockFlowersA has nine actual metadata values, including Hexalily.
            pot = GTDungeonBlocks.pottedFlower(BedrockFlowers.ALL.get(next(9)).id());
        } else {
            // BlockFlowersB has eight actual metadata values.
            pot = GTDungeonBlocks.pottedFlower(BedrockFlowers.ALL.get(9 + next(8)).id());
        }
        return set(ax, ay, az, pot.defaultBlockState());
    }

    /** GT6's loose flower on the floor. */
    public boolean flower(int ax, int ay, int az) {
        Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET,
                Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY,
                Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY};
        return set(ax, ay, az, flowers[next(flowers.length)].defaultBlockState());
    }

    /** GT6's {@code BlocksGT.Glowtus} with {@code nextMetaA()} converted to a colour block. */
    public boolean glowtus(int ax, int ay, int az) {
        int meta = nextMetaA();
        Block plant = GTSurfaceFlora.glowtus(GTSurfaceFlora.GLOWTUS_COLOURS.get(meta));
        return plant != null && set(ax, ay, az, plant.defaultBlockState());
    }

    /** GT6's two-in-three charged dungeon artifact; persist charge in the existing module. */
    public boolean zpm(int ax, int ay, int az, boolean active) {
        if (!set(ax, ay, az, GTLasers.ZPM.get().defaultBlockState())) return false;
        if (!(level.getBlockEntity(new BlockPos(x + ax,y + ay,z + az))
                instanceof com.gregtech.gregtech.blockentity.energy.ZpmModuleBlockEntity module))
            throw new IllegalStateException("Dungeon ZPM entity missing");
        module.initializeDungeonEnergy(active);
        return true;
    }

    public boolean zpm(int ax, int ay, int az) { return zpm(ax, ay, az, next2in3()); }

    /** GT6's obsidian wall piece; the port always uses plain obsidian. */
    public boolean obsidian(int ax, int ay, int az) {
        return set(ax, ay, az, Blocks.OBSIDIAN.defaultBlockState());
    }

    // ── raw block access ──────────────────────────────────────────────────────────────────────

    public boolean set(int ax, int ay, int az, BlockState state) {
        BlockPos pos = new BlockPos(x + ax, y + ay, z + az);
        boolean placed = level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        Block block = state.getBlock();
        // Shape checks must see the completed room and neighbouring chunks. Vanilla consumes
        // this queue in LevelChunk.postProcessGeneration after the feature stages finish.
        if (placed && level instanceof net.minecraft.server.level.WorldGenRegion
                && (block instanceof net.minecraft.world.level.block.RedStoneWireBlock
                    || block instanceof net.minecraft.world.level.block.CrossCollisionBlock
                    || block instanceof net.minecraft.world.level.block.WallBlock
                    || block instanceof net.minecraft.world.level.block.BaseRailBlock
                    || block instanceof net.minecraft.world.level.block.ScaffoldingBlock
                    || block instanceof com.gregtech.gregtech.block.tool.ScaffoldBlock)) {
            level.getChunk(pos).markPosForPostprocessing(pos);
        }
        return placed;
    }

    public BlockState get(int ax, int ay, int az) {
        return level.getBlockState(new BlockPos(x + ax, y + ay, z + az));
    }

    /** GT6's {@code WD.liquid}. */
    public boolean liquid(int ax, int ay, int az) {
        return !level.getFluidState(new BlockPos(x + ax, y + ay, z + az)).isEmpty();
    }

    /** GT6's {@code aWorld.canBlockSeeTheSky}: a cell that reaches the surface gets glass instead of rock. */
    public boolean seesSky(int ax, int ay, int az) {
        BlockPos pos = new BlockPos(x + ax, y + ay, z + az);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY();
    }

    /** GT6's {@code WD.opq}: an opaque cube. */
    public boolean opaque(int ax, int ay, int az) {
        BlockPos pos = new BlockPos(x + ax, y + ay, z + az);
        return level.getBlockState(pos).isSolidRender(level, pos);
    }

    public void light(BlockPos pos) { lightUpdates.add(pos); }

    /** GT6's {@code ChunkCoordinates}, for rooms that remember where to light. */
    public void light(int ax, int ay, int az) { light(new BlockPos(x + ax, y + ay, z + az)); }

    /** The layout value of a neighbouring cell, or {@code 1} when outside (GT6 indexes the array directly). */
    public byte neighbour(int dx, int dz) {
        int i = roomX + dx, j = roomZ + dz;
        if (i < 0 || j < 0 || i >= layout.length || j >= layout[i].length) return 0;
        return layout[i][j];
    }

    /** Whether a neighbouring cell is part of the dungeon (GT6's {@code != 0} test). */
    public boolean connected(int dx, int dz) { return neighbour(dx, dz) != 0; }

    /** The port's server level, for the few helpers that need it. */
    public ServerLevel server() { return level.getLevel(); }

    /** Unused so far: GT6 tags the cell for the rooms that need a specific tag. */
    public boolean hasTag(String tag) { return tags.contains(tag); }

    /**
     * GT6's record of which dungeon keys were already handed out ({@code mGeneratedKeys}). Worldgen
     * supplies the values preceding this cell from the reproducible dungeon-wide plan; direct room
     * helpers use the array their caller supplies.
     */
    public boolean keyAvailable(int index) {
        return index >= 0 && index < generatedKeys.length && !generatedKeys[index];
    }

    /** A key choice and shelf slot, with the original room stream advanced exactly as in GT6. */
    public record WorkshopKeyDecision(int index, int slot) {}

    public WorkshopKeyDecision workshopKeyDecision() {
        int drawn = next(generatedKeys.length * 2);
        // The original slot roll happens only when its original key roll picked a real key. Preserve
        // that consumption even when the dungeon-wide plan assigns this cell a different key outcome.
        int drawnSlot = drawn < generatedKeys.length ? 10 + next(18) : -1;
        if (sharedState == null) return new WorkshopKeyDecision(drawn, drawnSlot);
        int planned = sharedState.workshopKey();
        int slot = drawnSlot >= 0 ? drawnSlot : 10 + sharedState.workshopSlot();
        return new WorkshopKeyDecision(planned, slot);
    }

    /** GT6's four library shelf rows are 1, 2, 4 and 5. */
    public int libraryKeyIndex(int row) {
        int drawn = next(3) + next(3);
        return sharedState == null ? drawn : sharedState.libraryKey(row);
    }

    /** Convenience for tests: the block at a local offset. */
    public CompoundTag debugTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("color", color);
        tag.putString("primary", primary.registryId());
        tag.putString("secondary", secondary.registryId());
        tag.putInt("roomX", roomX);
        tag.putInt("roomZ", roomZ);
        tag.putInt("connectionCount", connectionCount);
        return tag;
    }

    /** Unused so far: the port's water check for the fish farm. */
    public boolean water(int ax, int ay, int az) {
        return level.getFluidState(new BlockPos(x + ax, y + ay, z + az)).is(FluidTags.WATER);
    }

    /** Whether the cell's floor is above bedrock (GT6's {@code aData.mWorld.getBlock(x, 0, z)} hardness test). */
    public boolean aboveBedrock(int ax, int az) {
        return !level.getBlockState(new BlockPos(x + ax, level.getMinBuildHeight(), z + az)).is(Blocks.BEDROCK);
    }

    /** GT6's {@code BlocksGT.Diggables}/{@code Sands} test: blocks the pillar must not dig through. */
    public boolean softGround(int ax, int ay, int az) {
        BlockState state = get(ax, ay, az);
        return state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY);
    }

    /** True for the fluids GT6 treats as water in its dungeon rooms. */
    public boolean waterFluid() { return level.getFluidState(new BlockPos(x, y, z)).is(Fluids.WATER); }
}
