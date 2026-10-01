package com.gregtech.gregtech.gametest;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.block.misc.DungeonPortalBlock;
import com.gregtech.gregtech.block.misc.DungeonPortalBlockEntity;
import com.gregtech.gregtech.block.stone.GTStoneSlabBlock;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.item.GTDungeonKeyItem;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTDungeonBlocks;
import com.gregtech.gregtech.registry.GTDungeonKeys;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomPortalEnd;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomPortalNether;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomStorage;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GT6's five dungeon keys ({@code WorldgenDungeonGT:169-173}, the ten key items of
 * {@code gregtech/items/MultiItemRandomTools.java:589-598}) and its two ported portal rooms
 * ({@code DungeonChunkRoomPortalNether} and {@code DungeonChunkRoomPortalEnd}).
 *
 * <p>The tests build the rooms' cells directly - the way {@code DungeonTests} does - from a
 * {@link GTDungeonData} of their own, so every key id, every block and every refusal is under the
 * test's control. The teleport is driven through
 * {@link DungeonPortalBlockEntity#teleport(ServerLevel, BlockPos, Entity)} instead of by waiting for
 * an entity to be ticked, because the game only calls the block's {@code entityInside} while an entity
 * moves, which a GameTest cannot wait for.</p>
 *
 * <h2>The suite's own area ({@link #BASE_X}, {@link #BASE_Z})</h2>
 * <p>Everything this file places lives in its own area at 62000/62000: the dungeon cells sit on the
 * first four of the six 32-block sites of {@link #site(int)} (a cell is 16 blocks wide and 12 tall),
 * and the two portal pairs of the teleport tests use the last two. Every other gametest suite keeps to
 * a base of its own, two thousand blocks apart (the highest is 60000, see {@code BumbliaryTests}), so
 * 62000 is clear of all of them at every height. {@link #reset} wipes the area before every test -
 * including the two spots the teleport targets in the Nether and the End - so this file is re-runnable
 * against a world it already ran in.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class DungeonKeysAndPortalsTests {

    /** The suite's own base, two thousand blocks above the highest base of the other suites. */
    private static final int BASE_X = 62000;
    private static final int BASE_Z = 62000;
    private static final int BASE_Y = 200;

    /** Six 32-block sites: four dungeon cells, then the overworld portals of the teleport tests. */
    private static final int SITES = 6;
    private static final int SITE_NETHER_ROOM = 0, SITE_END_ROOM = 1, SITE_REFUSED = 2, SITE_ACTIVATION = 3;
    private static final int SITE_PORTAL_NETHER = 4, SITE_PORTAL_END = 5;

    /** A key id the tests own, so it can never collide with a dungeon's own ({@code 1..1000000}). */
    private static final long TEST_KEY = 987654321L;

    /** The dungeon seed behind {@link GTDungeonFeature#keySeed(long, int)}, i.e. the real key ids. */
    private static final long SEED = 20260916L;

    /** GT6's two distance factors of the portal search ({@code MultiTileEntityMiniPortalNether:71}). */
    private static final int NETHER_FACTOR = 8, END_FACTOR = 128;

    /** The ten keys in GT6's {@code IL.KEYS} order, as the port names them. */
    private static final String[] KEY_MATERIALS = {"brass", "bronze", "copper", "gold", "iron", "lead",
            "plastic", "platinum", "silver", "tin"};
    /** GT6's display names of those ten ({@code MultiItemRandomTools:589-598}). */
    private static final String[] KEY_NAMES = {"Brass Key", "Bronze Key", "Copper Key", "Gold Key",
            "Iron Key", "Lead Key", "Plastic Key", "Platinum Key", "Silver Key", "Tin Key"};

    private static JsonObject lang(String language) throws Exception {
        try (InputStream stream = DungeonKeysAndPortalsTests.class.getClassLoader()
                .getResourceAsStream("assets/gregtech/lang/" + language + ".json")) {
            if (stream == null) throw new IllegalStateException("missing " + language + " language file");
            return GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    /** One of the suite's own sites, 32 blocks apart inside the reserved area (class javadoc). */
    private static BlockPos site(int index) {
        return new BlockPos(BASE_X + index * 32, BASE_Y, BASE_Z);
    }

    /** The coordinate GT6's distance factor produces (8 for the Nether, 128 for the End). */
    private static BlockPos scaled(BlockPos pos, int factor) {
        return new BlockPos(Math.floorDiv(pos.getX(), factor), pos.getY(), Math.floorDiv(pos.getZ(), factor));
    }

    /** GT6's key ids of a dungeon ({@code WorldgenDungeonGT:169-171}) and the stacks built from them. */
    private static long[] keyIds() {
        long[] ids = new long[GTDungeonFeature.KEY_COUNT];
        for (int index = 0; index < ids.length; index++) ids[index] = GTDungeonFeature.keySeed(SEED, index);
        return ids;
    }

    private static ItemStack[] keyStacks(long[] keyIds) {
        ItemStack[] stacks = new ItemStack[keyIds.length];
        for (int index = 0; index < keyIds.length; index++) {
            stacks[index] = GTDungeonFeature.keyStack(keyIds, index);
        }
        return stacks;
    }

    /**
     * The cell of a dead end whose only connection leads east, so the +X block of the portal rooms is
     * the one that fires - GT6's first branch of both rooms.
     */
    private static GTDungeonData cell(ServerLevel level, BlockPos origin, long[] keyIds, boolean tagged) {
        byte[][] cells = new byte[5][5];
        cells[2][2] = 1;
        cells[3][2] = GTDungeonLayout.CORRIDOR;
        Set<String> tags = new HashSet<>();
        if (tagged) tags.add(GTDungeonChunkRoomPortalNether.TAG_PORTAL_NETHER);
        return new GTDungeonData(level, origin.getX(), origin.getY(), origin.getZ(), StoneType.LIMESTONE,
                StoneType.SLATE, 3, cells, 2, 2, GTDungeonLayout.connectionCount(cells, 2, 2),
                keyIds, keyStacks(keyIds), new boolean[keyIds.length], new HashSet<>(), tags,
                RandomSource.create(SEED));
    }

    private static BlockState at(GameTestHelper helper, BlockPos origin, int x, int y, int z) {
        return helper.getLevel().getBlockState(origin.offset(x, y, z));
    }

    private static void check(List<String> problems, boolean condition, String message) {
        if (!condition) problems.add(message);
    }

    /** Empties the cells and the portal spots of this suite's own area, in all three dimensions. */
    private static void reset(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int index = 0; index < SITES; index++) {
            BlockPos origin = site(index);
            clear(level, origin.getX() - 2, origin.getX() + 17, origin.getY() - 2, origin.getY() + 9,
                    origin.getZ() - 2, origin.getZ() + 17);
        }
        for (ServerLevel other : new ServerLevel[]{level.getServer().getLevel(Level.NETHER),
                level.getServer().getLevel(Level.END)}) {
            if (other == null) continue;
            for (int index = SITE_PORTAL_NETHER; index <= SITE_PORTAL_END; index++) {
                int factor = index == SITE_PORTAL_NETHER ? NETHER_FACTOR : END_FACTOR;
                BlockPos target = scaled(site(index), factor);
                clear(other, target.getX() - 3, target.getX() + 3, target.getY() - 2, target.getY() + 3,
                        target.getZ() - 3, target.getZ() + 3);
            }
        }
        for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(
                BASE_X - 4, BASE_Y - 4, BASE_Z - 4,
                BASE_X + SITES * 32 + 20, BASE_Y + 8, BASE_Z + 20))) {
            entity.discard();
        }
    }

    private static void clear(ServerLevel level, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        level.getChunkAt(new BlockPos(minX, minY, minZ));
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) level.removeBlock(pos, false);
                }
            }
        }
    }

    /** Places a portal of the given kind with GT6's key id, loading its chunk first. */
    private static DungeonPortalBlockEntity placePortal(ServerLevel level, BlockPos pos, Block block, long keyId) {
        level.getChunkAt(pos);
        level.setBlock(pos, block.defaultBlockState(), 3);
        DungeonPortalBlockEntity portal = (DungeonPortalBlockEntity) level.getBlockEntity(pos);
        portal.setKeyId(keyId);
        return portal;
    }

    /** Places an active portal of the given kind. */
    private static DungeonPortalBlockEntity openPortal(ServerLevel level, BlockPos pos, Block block, long keyId) {
        DungeonPortalBlockEntity portal = placePortal(level, pos, block, keyId);
        portal.activate();
        return portal;
    }

    /** GT6's ten key items, and both languages naming every one of them. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void dungeonKeysAreRegisteredInBothLanguages(GameTestHelper helper) throws Exception {
        List<String> problems = new ArrayList<>();
        JsonObject english = lang("en_us");
        JsonObject chinese = lang("zh_cn");
        for (int index = 0; index < KEY_MATERIALS.length; index++) {
            String id = "key_" + KEY_MATERIALS[index];
            String name = KEY_NAMES[index];
            var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", id));
            if (item == null || item == Items.AIR) {
                problems.add("the item " + id + " is not registered");
                continue;
            }
            check(problems, item instanceof GTDungeonKeyItem, id + " is not a dungeon key item");
            check(problems, english.has("item.gregtech." + id), "en_us has no name for " + id);
            check(problems, chinese.has("item.gregtech." + id), "zh_cn has no name for " + id);
            check(problems, name.equals(english.get("item.gregtech." + id).getAsString()),
                    id + " is not GT6's " + name);
            check(problems, new ItemStack(item).getHoverName().getString().equals(name),
                    id + " does not name itself " + name);
        }
        check(problems, GTDungeonKeys.all().size() == 10,
                "GT6 has ten keys, got " + GTDungeonKeys.all().size());
        check(problems, GTDungeonKeys.byIndex(0) != null && ForgeRegistries.ITEMS
                        .getKey(GTDungeonKeys.byIndex(0)).getPath().equals("key_brass"),
                "the first key of IL.KEYS is the brass one");
        // Both portal blocks and their items, with a name in both languages (the localization guard
        // reads the block item's name for a block).
        for (String id : new String[]{"dungeon_portal_nether", "dungeon_portal_end"}) {
            var block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", id));
            if (block == null || block == Blocks.AIR) {
                problems.add("the block " + id + " is not registered");
                continue;
            }
            check(problems, block instanceof DungeonPortalBlock, id + " is not a dungeon portal");
            check(problems, block.asItem() != Items.AIR, "the block " + id + " has no item");
            check(problems, com.gregtech.gregtech.registry.GTBlockEntities.DUNGEON_PORTAL.get()
                            .isValid(block.defaultBlockState()),
                    "the dungeon portal block entity type covers " + id);
            check(problems, english.has("block.gregtech." + id), "en_us has no name for " + id);
            check(problems, chinese.has("block.gregtech." + id), "zh_cn has no name for " + id);
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /** The assets of the ten keys and the two portals: blockstates, models and GT6's key textures. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void dungeonKeyAndPortalAssetsExist(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        ClassLoader loader = DungeonKeysAndPortalsTests.class.getClassLoader();
        for (String material : KEY_MATERIALS) {
            for (String asset : new String[]{"models/item/key_" + material + ".json",
                    "textures/item/key_" + material + ".png"}) {
                if (loader.getResource("assets/gregtech/" + asset) == null) problems.add("missing " + asset);
            }
        }
        for (String id : new String[]{"dungeon_portal_nether", "dungeon_portal_end"}) {
            for (String asset : new String[]{"blockstates/" + id + ".json",
                    "models/block/dungeon/" + id + ".json",
                    "models/block/dungeon/" + id + "_frame.json",
                    "models/item/" + id + ".json"}) {
                if (loader.getResource("assets/gregtech/" + asset) == null) problems.add("missing " + asset);
            }
        }
        // GT6's End portal plane: its own black multiply of the portal texture, 32 frames with the
        // animation metadata the pack ships.
        if (loader.getResource("assets/gregtech/textures/block/dungeon/portal_end.png") == null) {
            problems.add("missing the End portal texture");
        }
        if (loader.getResource("assets/gregtech/textures/block/dungeon/portal_end.png.mcmeta") == null) {
            problems.add("missing the End portal texture's animation metadata");
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /** A key stack carries GT6's {@code gt.key} id and the {@code Key #N} name, and both survive NBT. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void dungeonKeyStacksCarryIdAndName(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        long[] ids = keyIds();
        for (int index = 0; index < ids.length; index++) {
            check(problems, ids[index] != 0, "key " + (index + 1) + " has the blank id");
        }
        check(problems, new HashSet<>(java.util.stream.LongStream.of(ids).boxed().collect(java.util.stream.Collectors.toList())).size() == ids.length,
                "the five key ids are not distinct");
        ItemStack[] stacks = keyStacks(ids);
        for (int index = 0; index < stacks.length; index++) {
            ItemStack stack = stacks[index];
            String name = "Key #" + (index + 1);
            if (stack.isEmpty()) {
                problems.add("key stack " + (index + 1) + " is empty");
                continue;
            }
            check(problems, stack.getItem() instanceof GTDungeonKeyItem,
                    "key stack " + (index + 1) + " is not a key item");
            check(problems, GTDungeonKeyItem.keyId(stack) == ids[index],
                    "key stack " + (index + 1) + " lost its id");
            check(problems, stack.hasCustomHoverName() && stack.getHoverName().getString().equals(name),
                    "key stack " + (index + 1) + " is named " + stack.getHoverName().getString()
                            + " instead of " + name);
            // GT6's rooms hand the stack to a container, so it has to survive a save and a load.
            ItemStack roundTrip = ItemStack.of(stack.save(new CompoundTag()));
            if (roundTrip.isEmpty() || roundTrip.getItem() != stack.getItem()) {
                problems.add("key stack " + (index + 1) + " does not survive an NBT round trip");
                continue;
            }
            check(problems, GTDungeonKeyItem.keyId(roundTrip) == ids[index],
                    "the round tripped key " + (index + 1) + " lost its id");
            check(problems, roundTrip.getHoverName().getString().equals(name),
                    "the round tripped key " + (index + 1) + " lost its name");
        }
        // The ten ids of the item table are all different items, so a key cannot be mistaken for another.
        Set<ItemStack> types = new HashSet<>();
        for (var key : GTDungeonKeys.all()) {
            if (key.isPresent()) types.add(new ItemStack(key.get()));
        }
        check(problems, types.size() == 10, "the ten keys are ten items, got " + types.size());
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /** GT6's dead end list is the storage room plus the two ported portal rooms ({@code :96-103}). */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void deadEndsHoldTheTwoPortals(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        check(problems, GTDungeonFeature.DEAD_END.size() == 3,
                "three dead ends, got " + GTDungeonFeature.DEAD_END.size());
        check(problems, GTDungeonFeature.DEAD_END.get(0) instanceof GTDungeonChunkRoomStorage,
                "GT6's first dead end is the storage room");
        check(problems, GTDungeonFeature.DEAD_END.get(1) instanceof GTDungeonChunkRoomPortalNether,
                "GT6's second dead end is the Nether portal room");
        check(problems, GTDungeonFeature.DEAD_END.get(2) instanceof GTDungeonChunkRoomPortalEnd,
                "GT6's third dead end is the End portal room");
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /** GT6's Nether room: floor, ceiling, soul sand rows, the frame and the portal inside it. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void netherPortalRoomBuildsGt6Frame(GameTestHelper helper) {
        reset(helper);
        BlockPos origin = site(SITE_NETHER_ROOM);
        long[] ids = keyIds();
        helper.assertTrue(new GTDungeonChunkRoomPortalNether().generate(cell(helper.getLevel(), origin, ids, false)),
                "the Nether portal room accepts a dead end");
        List<String> problems = new ArrayList<>();

        // GT6 :50-56: the floor, GT6 :58-71 the ceiling (Netherlicious in GT6, vanilla here).
        check(problems, at(helper, origin, 7, 0, 7).is(Blocks.NETHERRACK), "the floor is netherrack");
        check(problems, at(helper, origin, 3, 0, 3).is(Blocks.GLOWSTONE),
                "the lamp grid of the floor is glowstone");
        check(problems, at(helper, origin, 7, 7, 7).is(Blocks.NETHER_WART_BLOCK),
                "the ceiling is a wart block");
        check(problems, at(helper, origin, 3, 7, 3).is(Blocks.SHROOMLIGHT),
                "the lamp grid of the ceiling is shroomlight");
        check(problems, at(helper, origin, 7, 8, 7)
                        .is(GTBlocks.getStoneState(StoneType.LIMESTONE, StoneVariant.TILES).getBlock()),
                "the tile skin above the ceiling is the primary rock");
        // GT6 :76-85: the soul sand rows with their nether wart and their smooth rock slab kerbs, in
        // GT6's own slab orientations - mSlabs[SIDE_Y_POS] above the wart is upside down, mSlabs[SIDE_Z_NEG]
        // at z = 2 has its half towards the soul sand at z = 1.
        check(problems, at(helper, origin, 5, 1, 1).is(Blocks.SOUL_SAND), "soul sand at (5,1,1)");
        check(problems, at(helper, origin, 5, 2, 1).is(Blocks.NETHER_WART), "nether wart at (5,2,1)");
        check(problems, at(helper, origin, 5, 1, 14).is(Blocks.SOUL_SAND), "soul sand at (5,1,14)");
        check(problems, at(helper, origin, 5, 3, 1).is(GTBlocks.getStoneSlab(StoneType.LIMESTONE, StoneVariant.SMOOTH))
                        && at(helper, origin, 5, 3, 1).getValue(GTStoneSlabBlock.FACING) == Direction.UP,
                "the slab above a soul sand row is the primary rock's smooth top half slab");
        check(problems, at(helper, origin, 5, 1, 2).is(GTBlocks.getStoneSlab(StoneType.LIMESTONE, StoneVariant.SMOOTH))
                        && at(helper, origin, 5, 1, 2).getValue(GTStoneSlabBlock.FACING) == Direction.NORTH,
                "the kerb of a soul sand row is a smooth rock slab with its half towards the sand");
        // GT6's base portal room :32-62: the concrete patch, its corner lamp and the tile slab kerb, which
        // GT6 writes with mSlabs[0], its bottom half (SIDE_Y_NEG).
        check(problems, at(helper, origin, 1, 1, 5).is(Blocks.REDSTONE_LAMP)
                        && at(helper, origin, 1, 1, 5).getValue(RedstoneLampBlock.LIT),
                "the patch's corner is a lit lamp");
        check(problems, at(helper, origin, 4, 1, 4).is(GTBlocks.getStoneSlab(StoneType.LIMESTONE, StoneVariant.TILES))
                        && at(helper, origin, 4, 1, 4).getValue(GTStoneSlabBlock.FACING) == Direction.DOWN,
                "the tile kerb stands at (4,1,4) as GT6's bottom half slab");
        // GT6 :87-100: the obsidian frame of the +X block, four wide and five tall at local x = 2.
        int[][] frame = {{2, 1, 6}, {2, 1, 7}, {2, 1, 8}, {2, 1, 9}, {2, 2, 6}, {2, 2, 9}, {2, 3, 6}, {2, 3, 9},
                {2, 4, 6}, {2, 4, 9}, {2, 5, 6}, {2, 5, 7}, {2, 5, 8}, {2, 5, 9}};
        for (int[] pos : frame) {
            check(problems, at(helper, origin, pos[0], pos[1], pos[2]).is(Blocks.OBSIDIAN),
                    "obsidian at (" + pos[0] + "," + pos[1] + "," + pos[2] + ")");
        }
        // GT6 :87-100 leaves the six interior cells empty for the player to light a vanilla portal.
        for (int y = 2; y <= 4; y++) {
            for (int z = 7; z <= 8; z++) {
                BlockState state = at(helper, origin, 2, y, z);
                check(problems, state.isAir(),
                        "GT6's Nether portal frame is unlit at (2," + y + "," + z + ")");
            }
        }
        // GT6 :41-48: the supply chest uses seven sparse slots, not slots 0..6.
        check(problems, at(helper, origin, 1, 2, 5).getBlock() instanceof com.gregtech.gregtech.block.inventory.MetalChestBlock,
                "the GT supply chest stands at (1,2,5)");
        if (helper.getLevel().getBlockEntity(origin.offset(1, 2, 5)) instanceof com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity chest) {
            var inventory = chest.inventory();
            check(problems, inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(3).isEmpty(),
                    "the original supply slots before slot 4 remain empty");
            check(problems, inventory.getStackInSlot(4).is(Blocks.OBSIDIAN.asItem())
                            && inventory.getStackInSlot(4).getCount() == 16,
                    "slot 4 holds GT6's 16 obsidian");
            check(problems, inventory.getStackInSlot(11).is(Blocks.NETHERRACK.asItem())
                            && inventory.getStackInSlot(11).getCount() == 16,
                    "slot 11 holds GT6's 16 netherrack");
            check(problems, inventory.getStackInSlot(15).is(Blocks.GLOWSTONE.asItem())
                            && inventory.getStackInSlot(15).getCount() == 16,
                    "slot 15 holds GT6's 16 glowstone");
            check(problems, inventory.getStackInSlot(22).is(Items.WRITTEN_BOOK),
                    "slot 22 holds GT6's hunting guide");
            check(problems, inventory.getStackInSlot(29).is(Items.GHAST_TEAR)
                            && inventory.getStackInSlot(29).getCount() == 4,
                    "slot 29 holds GT6's four ghast tears");
            check(problems, inventory.getStackInSlot(33).is(Items.BLAZE_ROD)
                            && inventory.getStackInSlot(33).getCount() == 4,
                    "slot 33 holds GT6's four blaze rods");
            check(problems, ResourceLocation.fromNamespaceAndPath("gregtech", "match_box_full")
                            .equals(ForgeRegistries.ITEMS.getKey(inventory.getStackInSlot(40).getItem()))
                            && inventory.getStackInSlot(40).getCount() == 1,
                    "slot 40 holds GT6's full matchbox");
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /** GT6 :38: a cell whose dungeon already has the Nether portal is refused before anything is built. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void netherPortalRoomRefusesASecondPortal(GameTestHelper helper) {
        reset(helper);
        BlockPos origin = site(SITE_REFUSED);
        long[] ids = keyIds();
        helper.assertTrue(!new GTDungeonChunkRoomPortalNether()
                        .generate(cell(helper.getLevel(), origin, ids, true)),
                "the room refuses a dungeon that already has its portal");
        helper.assertTrue(at(helper, origin, 7, 0, 7).isAir(), "a refused cell writes nothing at all");
        // The same cell without the tag is accepted, which proves the tag is what refused it.
        helper.assertTrue(new GTDungeonChunkRoomPortalNether()
                        .generate(cell(helper.getLevel(), origin, ids, false)),
                "the same cell is accepted without the tag");
        helper.assertTrue(at(helper, origin, 7, 0, 7).is(Blocks.NETHERRACK), "and then builds its floor");
        helper.succeed();
    }

    /** GT6's End room: the purpur shell, the frame ring with its eyes and the portal in the middle. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void endPortalRoomBuildsGt6Frame(GameTestHelper helper) {
        reset(helper);
        BlockPos origin = site(SITE_END_ROOM);
        long[] ids = keyIds();
        helper.assertTrue(new GTDungeonChunkRoomPortalEnd().generate(cell(helper.getLevel(), origin, ids, false)),
                "the End portal room accepts a dead end");
        List<String> problems = new ArrayList<>();

        // GT6 :46-69: the purpur room, which overwrites the end stone floor of GT6's first loop.
        check(problems, at(helper, origin, 3, 0, 3).is(Blocks.PURPUR_PILLAR), "a pillar at (3,0,3)");
        check(problems, at(helper, origin, 3, 7, 3).is(Blocks.GLOWSTONE), "glowstone at (3,7,3)");
        check(problems, at(helper, origin, 1, 0, 1).is(Blocks.PURPUR_PILLAR), "a corner post at (1,0,1)");
        check(problems, at(helper, origin, 1, 7, 1).is(Blocks.PURPUR_PILLAR), "the post reaches y = 7");
        check(problems, at(helper, origin, 2, 0, 5).is(Blocks.PURPUR_BLOCK),
                "the floor is purpur where it is neither a post nor a lamp");
        check(problems, at(helper, origin, 5, 7, 5).is(Blocks.PURPUR_BLOCK), "the ceiling is purpur");
        check(problems, at(helper, origin, 7, 8, 7)
                        .is(GTBlocks.getStoneState(StoneType.LIMESTONE, StoneVariant.TILES).getBlock()),
                "the tile skin above the ceiling is the primary rock");
        // GT6 :71-83: the corners of the portal, obsidian with GT6's own glowstone fallback.
        check(problems, at(helper, origin, 5, 0, 5).is(Blocks.OBSIDIAN), "obsidian at (5,0,5)");
        check(problems, at(helper, origin, 5, 1, 5).is(Blocks.OBSIDIAN), "obsidian at (5,1,5)");
        check(problems, at(helper, origin, 5, 2, 5).is(Blocks.GLOWSTONE), "glowstone at (5,2,5)");
        // GT6 :84-91: the eight frames, all with an eye, facing the portal.
        int[][] frames = {{7, 6, Direction.SOUTH.ordinal()}, {8, 6, Direction.SOUTH.ordinal()},
                {9, 7, Direction.WEST.ordinal()}, {9, 8, Direction.WEST.ordinal()},
                {7, 9, Direction.NORTH.ordinal()}, {8, 9, Direction.NORTH.ordinal()},
                {6, 7, Direction.EAST.ordinal()}, {6, 8, Direction.EAST.ordinal()}};
        for (int[] frame : frames) {
            BlockState state = at(helper, origin, frame[0], 0, frame[1]);
            if (!state.is(Blocks.END_PORTAL_FRAME)) {
                problems.add("an end portal frame at (" + frame[0] + ",0," + frame[1] + ")");
                continue;
            }
            check(problems, state.getValue(EndPortalFrameBlock.HAS_EYE),
                    "the frame at (" + frame[0] + ",0," + frame[1] + ") carries an eye");
            check(problems, state.getValue(EndPortalFrameBlock.FACING).ordinal() == frame[2],
                    "the frame at (" + frame[0] + ",0," + frame[1] + ") faces the portal");
        }
        // GT6 :92-99: the portal itself and the obsidian below it.
        for (int x = 7; x <= 8; x++) {
            for (int z = 7; z <= 8; z++) {
                check(problems, at(helper, origin, x, 0, z).is(Blocks.END_PORTAL),
                        "GT6's vanilla End portal stands at (" + x + ",0," + z + ")");
                check(problems, at(helper, origin, x, -1, z).is(Blocks.OBSIDIAN),
                        "obsidian below the portal at (" + x + ",-1," + z + ")");
            }
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /** GT6's key rule: only the key with the portal's own id opens it, and flint and steel toggles it. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void portalOpensWithTheMatchingKeyOnly(GameTestHelper helper) {
        reset(helper);
        BlockPos pos = site(SITE_ACTIVATION);
        ServerLevel level = helper.getLevel();
        Block block = GTDungeonBlocks.PORTAL_NETHER.get();
        DungeonPortalBlockEntity portal = placePortal(level, pos, block, TEST_KEY);
        Player player = helper.makeMockPlayer();
        List<String> problems = new ArrayList<>();

        // GT6's Behavior_Key:50-51 / MultiTileEntitySafeKeyLocked.useKey:79-91.
        player.setItemInHand(InteractionHand.MAIN_HAND, GTDungeonKeyItem.key(GTDungeonKeys.byIndex(0),
                TEST_KEY + 1, 0));
        block.use(level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit(pos));
        check(problems, !portal.isActive(), "a key with another id must not open the portal");

        player.setItemInHand(InteractionHand.MAIN_HAND, GTDungeonKeyItem.key(GTDungeonKeys.byIndex(1),
                TEST_KEY, 0));
        block.use(level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit(pos));
        check(problems, portal.isActive(), "the key with the portal's id opens it");
        check(problems, level.getBlockState(pos).getValue(DungeonPortalBlock.ACTIVE),
                "the active portal says so in its block state");

        // GT6's TOOL_igniter toggle (MultiTileEntityMiniPortalNether:116-128).
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        block.use(level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit(pos));
        check(problems, !portal.isActive(), "flint and steel closes the portal again");

        // A portal without an id adopts the id of the key that is used on it (Behavior_Key:53-57).
        BlockPos blankPos = pos.offset(0, 0, 2);
        DungeonPortalBlockEntity blank = placePortal(level, blankPos, block, 0L);
        ItemStack blankKey = GTDungeonKeyItem.key(GTDungeonKeys.byIndex(2), TEST_KEY + 7, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, blankKey);
        block.use(level.getBlockState(blankPos), level, blankPos, player, InteractionHand.MAIN_HAND, hit(blankPos));
        check(problems, blank.isActive() && blank.keyId() == GTDungeonKeyItem.keyId(blankKey),
                "a portal without an id adopts the key it is used with");

        // The portal's state and id survive a save and a load.
        CompoundTag tag = blank.saveWithoutMetadata();
        DungeonPortalBlockEntity reloaded = placePortal(level, pos.offset(0, 0, 4), block, 0L);
        reloaded.deactivate();
        reloaded.load(tag);
        check(problems, reloaded.isActive() && reloaded.keyId() == blank.keyId(),
                "the portal state survives a reload");
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /**
     * The teleport itself: an entity inside an open Nether portal arrives in the matching portal of the
     * Nether and comes back through it. Both trips are driven through
     * {@link DungeonPortalBlockEntity#teleport(ServerLevel, BlockPos, Entity)}, because the game only
     * calls the block's {@code entityInside} while an entity moves.
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void netherPortalTeleportsThereAndBack(GameTestHelper helper) {
        reset(helper);
        ServerLevel overworld = helper.getLevel();
        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the GameTest server runs a Nether dimension");

        BlockPos origin = site(SITE_PORTAL_NETHER);
        BlockPos target = scaled(origin, NETHER_FACTOR);
        Block block = GTDungeonBlocks.PORTAL_NETHER.get();
        openPortal(overworld, origin, block, TEST_KEY);
        DungeonPortalBlockEntity netherPortal = openPortal(nether, target, block, TEST_KEY);

        List<String> problems = new ArrayList<>();
        ArmorStand traveller = traveller(overworld, origin);
        Entity moved = DungeonPortalBlockEntity.teleport(overworld, origin, traveller);
        if (moved == null) {
            problems.add("the open portal moved the entity");
        } else {
            check(problems, moved.level().dimension() == Level.NETHER,
                    "the entity is in the Nether, got " + moved.level().dimension().location());
            check(problems, moved.blockPosition().equals(target),
                    "the entity arrived in the matching portal at " + target + ", got " + moved.blockPosition());
            // Vanilla's portal cooldown keeps it from bouncing straight back out of the arrival portal.
            check(problems, moved.isOnPortalCooldown(), "the arrival sets vanilla's portal cooldown");
            check(problems, DungeonPortalBlockEntity.teleport(nether, target, moved) == null,
                    "the cooldown blocks the immediate return");
            moved.setPortalCooldown(0);
            Entity returned = DungeonPortalBlockEntity.teleport(nether, target, moved);
            if (returned == null) {
                problems.add("the return trip works");
            } else {
                check(problems, returned.level().dimension() == Level.OVERWORLD,
                        "the return trip lands in the overworld, got "
                                + returned.level().dimension().location());
                check(problems, returned.blockPosition().equals(origin),
                        "the return trip lands in the origin portal at " + origin + ", got "
                                + returned.blockPosition());
                returned.discard();
            }
        }
        // A closed portal is inert, which is what keeps a built frame harmless until it is opened.
        BlockPos closedPos = origin.offset(0, 0, 2);
        DungeonPortalBlockEntity closed = openPortal(overworld, closedPos, block, TEST_KEY);
        closed.deactivate();
        ArmorStand idle = traveller(overworld, closedPos);
        check(problems, DungeonPortalBlockEntity.teleport(overworld, closedPos, idle) == null,
                "a closed portal moves nobody");
        idle.discard();
        // A portal without a counterpart still takes the entity across, onto the platform the port lays.
        netherPortal.deactivate();
        Entity fell = DungeonPortalBlockEntity.teleport(overworld, origin, traveller(overworld, origin));
        if (fell == null) {
            problems.add("a portal without a counterpart still teleports");
        } else {
            check(problems, fell.level().dimension() == Level.NETHER,
                    "the entity without a counterpart arrives in the Nether, got "
                            + fell.level().dimension().location());
            check(problems, fell.blockPosition().equals(target),
                    "it arrives at the scaled coordinate " + target + ", got " + fell.blockPosition());
            check(problems, nether.getBlockState(target.below()).is(Blocks.OBSIDIAN),
                    "the port lays an obsidian platform under the arrival");
            fell.discard();
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    /** The End portal pair of the same mechanism, which uses GT6's 128 factor and 512 margin. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void endPortalTeleportsThereAndBack(GameTestHelper helper) {
        reset(helper);
        ServerLevel overworld = helper.getLevel();
        ServerLevel end = overworld.getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "the GameTest server runs an End dimension");

        BlockPos origin = site(SITE_PORTAL_END);
        BlockPos target = scaled(origin, END_FACTOR);
        Block block = GTDungeonBlocks.PORTAL_END.get();
        openPortal(overworld, origin, block, TEST_KEY);
        openPortal(end, target, block, TEST_KEY);

        List<String> problems = new ArrayList<>();
        Entity moved = DungeonPortalBlockEntity.teleport(overworld, origin, traveller(overworld, origin));
        if (moved == null) {
            problems.add("the open End portal moved the entity");
        } else {
            check(problems, moved.level().dimension() == Level.END,
                    "the entity is in the End, got " + moved.level().dimension().location());
            check(problems, moved.blockPosition().equals(target),
                    "it arrived in the matching portal at " + target + ", got " + moved.blockPosition());
            moved.setPortalCooldown(0);
            Entity returned = DungeonPortalBlockEntity.teleport(end, target, moved);
            check(problems, returned != null && returned.level().dimension() == Level.OVERWORLD
                            && returned.blockPosition().equals(origin),
                    "the return trip lands in the origin portal at " + origin);
            if (returned != null) returned.discard();
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    private static ArmorStand traveller(ServerLevel level, BlockPos pos) {
        ArmorStand stand = new ArmorStand(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        stand.setNoGravity(true);
        level.addFreshEntity(stand);
        return stand;
    }

    /** A hit result at the centre of a position, for driving the block's own {@code use}. */
    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }
}
