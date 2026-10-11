package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.OreBlock;
import com.gregtech.gregtech.block.OreHostStone;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.blockentity.OreBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.worldgen.GTOreBlockResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * §103.B: the ore host rock moved from a per-ore {@code OreBlockEntity} to the {@link OreBlock#STONE}
 * block-state property, which removes one block entity per ore block (~497 per chunk, 1.8M in the
 * 3.6k-chunk session of §101/§102).
 *
 * <p>{@link OreBlock} deliberately does not implement {@code EntityBlock}, so no ore placement creates
 * an entity at all: worldgen's {@code DUMMY} placeholder path ({@code WorldGenRegion:267-281}) never
 * triggers, and {@code Level#getBlockEntity} — which asks vanilla for an <em>immediate</em> creation
 * ({@code LevelChunk:313-320}) — cannot grow one on a lookup. Worlds generated before the change are
 * still handled: their saved {@code gregtech:ore} tags are loaded through the block-entity type and
 * {@link OreBlockEntity#onLoad()} folds the stone into the state.
 *
 * <p>What is guarded here: no entity survives any placement path, an ore never reports
 * {@code hasBlockEntity()}, the ids of the old entity still parse (and unknown ones fall back to stone),
 * the state round-trips every host rock, bedrock deposits stay unbreakable/undroppable, and a legacy
 * entity migrates into the state instead of being kept.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class OreBlockStateTests {
    private static final int BASE_X = 22000;
    private static final int BASE_Z = 22000;
    private static final int BASE_Y = 100;

    /** A registered normal (non-small) ore block — the tests do not care which material. */
    private static OreBlock oreBlock() {
        return ForgeRegistries.BLOCKS.getValues().stream()
                .filter(b -> b instanceof OreBlock ore && !ore.isSmall())
                .map(b -> (OreBlock) b)
                .findFirst()
                .orElseThrow();
    }

    /**
     * Asserts no block entity is left at {@code pos}. {@code Level#getBlockEntity} itself asks vanilla
     * for an <em>immediate</em> creation ({@code LevelChunk:313-320}), so this is also the regression
     * guard against an ore block that reports {@code hasBlockEntity()} — such a block would grow a fresh
     * entity on the first lookup. The chunk map is checked as well, so a removed-but-referenced entity
     * cannot hide here.
     */
    private static void assertNoEntity(GameTestHelper helper, BlockPos pos, String what) {
        BlockEntity be = helper.getLevel().getBlockEntity(pos);
        BlockEntity inMap = helper.getLevel().getChunkAt(pos).getBlockEntities().get(pos);
        helper.assertTrue(be == null && inMap == null, what + " — got " + be
                + " removed=" + (be != null && be.isRemoved()) + " inChunkMap=" + inMap);
    }

    /**
     * ① Placing an ore block leaves no block entity behind — asserted right after placement, because
     * {@code Level#getBlockEntity} would create one on the spot if the ore reported
     * {@code hasBlockEntity()}.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void placingAnOreCreatesNoBlockEntity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        OreBlock ore = oreBlock();
        BlockPos plain = new BlockPos(BASE_X + 1, BASE_Y, BASE_Z + 1);
        BlockPos granite = new BlockPos(BASE_X + 2, BASE_Y, BASE_Z + 1);
        BlockPos bedrock = new BlockPos(BASE_X + 3, BASE_Y, BASE_Z + 1);
        level.setBlock(plain, ore.defaultBlockState(), 3);
        level.setBlock(granite, ore.stateFor(OreHostStone.GRANITE_BLACK), 3);
        level.setBlock(bedrock, ore.stateFor(OreHostStone.BEDROCK), 3);
        assertNoEntity(helper, plain, "§103.B: a default-host ore must not keep a block entity");
        assertNoEntity(helper, granite, "§103.B: a granite ore must not keep a block entity");
        assertNoEntity(helper, bedrock, "§103.B: a bedrock ore must not keep a block entity");
        helper.assertTrue(OreBlock.stoneOf(level.getBlockState(granite)) == OreHostStone.GRANITE_BLACK,
                "the state holds the host rock right after placement");
        helper.assertTrue(OreBlock.stoneOf(level.getBlockState(bedrock)) == OreHostStone.BEDROCK,
                "the bedrock host rock survives placement");
        helper.succeed();
    }

    /** ② The ids the old block entity wrote still parse; anything unknown falls back to stone. */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void hostStoneIdsFallBackToStone(GameTestHelper helper) {
        helper.assertTrue(OreHostStone.byId(OreBlockEntity.BEDROCK_STONE) == OreHostStone.BEDROCK,
                "the old entity id 'bedrock' still parses");
        helper.assertTrue(OreHostStone.byId(OreBlockEntity.DEFAULT_STONE) == OreHostStone.STONE,
                "the old entity default 'stone' still parses");
        helper.assertTrue(OreHostStone.byId("stone_granite_black") == OreHostStone.GRANITE_BLACK,
                "GT stone ids parse");
        for (StoneType type : StoneType.values()) {
            helper.assertTrue(OreHostStone.of(type).id().equals(type.registryId()),
                    "StoneType " + type.name() + " maps to its host rock, got " + OreHostStone.of(type).id());
        }
        for (String unknown : new String[]{"not_a_stone", "minecraft:stone", "stone_", ""}) {
            helper.assertTrue(OreHostStone.byId(unknown) == OreHostStone.STONE,
                    "unknown host id '" + unknown + "' falls back to stone");
        }
        helper.assertTrue(OreHostStone.byId(null) == OreHostStone.STONE, "a missing host id falls back to stone");
        Set<String> ids = new HashSet<>();
        for (OreHostStone host : OreHostStone.values()) {
            helper.assertTrue(ids.add(host.id()), "duplicate host rock id " + host.id());
        }
        helper.assertTrue(OreHostStone.SAND.isSediment() && OreHostStone.RED_SAND.isSediment()
                        && OreHostStone.GRAVEL.isSediment() && !OreHostStone.STONE.isSediment(),
                "the loose sediments are marked as such (§103.B added them: small ores generate in sand/gravel)");
        helper.assertTrue(OreHostStone.BEDROCK.isBedrock() && !OreHostStone.DEEPSLATE.isBedrock(),
                "only the bedrock host rock is a bedrock deposit");
        helper.succeed();
    }

    /** ③ Every host rock round-trips through the state, under the id the blockstate key uses. */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void hostRockRoundTripsThroughTheBlockState(GameTestHelper helper) {
        OreBlock ore = oreBlock();
        for (OreHostStone host : OreHostStone.values()) {
            BlockState state = ore.stateFor(host);
            helper.assertTrue(OreBlock.stoneOf(state) == host,
                    "round trip failed for " + host.id() + ", got " + OreBlock.stoneOf(state).id());
            helper.assertTrue(state.getValue(OreBlock.STONE) == host, "the property holds " + host.id());
            helper.assertTrue(state.getBlock() == ore, "the state belongs to the ore block");
            // The model replacement keys off BlockModelShaper#stateToModelLocation, which appends
            // "stone=<Property#getName>": the serialized name must be the canonical id, not the enum name.
            helper.assertTrue(OreBlock.STONE.getName(host).equals(host.id()),
                    "the property serializes as the canonical id, got " + OreBlock.STONE.getName(host));
        }
        helper.assertTrue(OreBlock.STONE.getName(OreHostStone.STONE).equals("stone")
                        && OreBlock.STONE.getName(OreHostStone.GRANITE_BLACK).equals("stone_granite_black"),
                "the blockstate variant key stays the id OreBlockEntity used to store");
        helper.assertTrue(OreBlock.stoneOf(ore.defaultBlockState()) == OreHostStone.STONE,
                "the default host rock is stone");
        helper.assertTrue(OreBlock.stoneOf(null) == OreHostStone.STONE,
                "a null state reads as stone instead of throwing");
        helper.assertTrue(OreBlock.stoneOf(Blocks.STONE.defaultBlockState()) == OreHostStone.STONE,
                "a foreign block state reads as stone instead of throwing");
        helper.assertTrue(ore.stateFor(null) == ore.defaultBlockState(),
                "a null host rock places as the default state");
        helper.succeed();
    }

    /** ④ A bedrock host rock is unbreakable, explosion-proof and drops nothing — without an entity. */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void bedrockHostIsUnbreakableAndDropsNothing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        OreBlock ore = oreBlock();
        BlockPos pos = new BlockPos(BASE_X + 2, BASE_Y, BASE_Z + 2);
        BlockState bedrock = ore.stateFor(OreHostStone.BEDROCK);
        level.setBlock(pos, bedrock, 3);
        BlockState placed = level.getBlockState(pos);
        helper.assertTrue(OreBlock.isBedrockOre(placed), "the bedrock host rock reads as a bedrock deposit");
        helper.assertTrue(!OreBlock.isBedrockOre(ore.stateFor(OreHostStone.DEEPSLATE)),
                "a deepslate ore is not a bedrock deposit");
        Player player = helper.makeMockSurvivalPlayer();
        helper.assertTrue(ore.getDestroyProgress(placed, player, level, pos) == 0.0F,
                "a bedrock ore cannot be broken");
        helper.assertTrue(ore.getExplosionResistance(placed, level, pos, null) >= 3600000.0F,
                "a bedrock ore resists explosions like bedrock");
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                placed, level, pos, null, null, new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(drops.isEmpty(), "a bedrock ore drops nothing, got " + drops.size());
        // …while the same block with a normal host rock is mineable.
        BlockState normal = ore.stateFor(OreHostStone.STONE);
        level.setBlock(pos, normal, 3);
        helper.assertTrue(ore.getDestroyProgress(level.getBlockState(pos), player, level, pos) > 0.0F,
                "an ordinary ore is breakable");
        assertNoEntity(helper, pos, "§103.B: a breakable ore keeps no block entity either");
        helper.succeed();
    }

    /**
     * ⑤ A world generated before §103.B migrates its ore entity into the state. The entity is never
     * stored (the ore state accepts none), so what matters is that the loader's promote path — set the
     * level, then {@code addAndRegisterBlockEntity} — still calls {@code onLoad}, and that the stone it
     * carries lands in the block state.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void legacyBlockEntityMigratesIntoTheBlockState(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        OreBlock ore = oreBlock();
        BlockPos[] legacySpots = new BlockPos[4];
        int i = 0;
        for (OreHostStone host : new OreHostStone[]{OreHostStone.BEDROCK, OreHostStone.GRANITE_BLACK,
                OreHostStone.DEEPSLATE, OreHostStone.STONE}) {
            BlockPos pos = new BlockPos(BASE_X + 3, BASE_Y, BASE_Z + 3 + host.ordinal());
            legacySpots[i++] = pos;
            level.setBlock(pos, ore.defaultBlockState(), 3);
            OreBlockEntity legacy = new OreBlockEntity(pos, ore.defaultBlockState());
            CompoundTag tag = new CompoundTag();
            tag.putString(OreBlockEntity.TAG_STONE, host.id());
            legacy.load(tag);
            // The chunk loader path for a saved tag: LevelChunk#promotePendingBlockEntity sets the level
            // (LevelChunk:538) and calls addAndRegisterBlockEntity, which refuses to store the entity
            // because the ore state no longer has a block entity — but still calls onLoad.
            legacy.setLevel(level);
            level.getChunkAt(pos).addAndRegisterBlockEntity(legacy);
            legacy.onLoad();
            helper.assertTrue(OreBlock.stoneOf(level.getBlockState(pos)) == host,
                    "the legacy tag moved into the block state for " + host.id());
            helper.assertTrue(legacy.isRemoved(), "the migrated entity released itself for " + host.id());
            assertNoEntity(helper, pos, "§103.B: the migrated entity is not kept (" + host.id() + ")");
        }
        // An unknown tag value must not lose the ore: it falls back to the default host rock.
        BlockPos unknown = new BlockPos(BASE_X + 4, BASE_Y, BASE_Z + 4);
        level.setBlock(unknown, ore.defaultBlockState(), 3);
        OreBlockEntity stale = new OreBlockEntity(unknown, ore.defaultBlockState());
        CompoundTag tag = new CompoundTag();
        tag.putString(OreBlockEntity.TAG_STONE, "some_other_mods_stone");
        stale.load(tag);
        stale.setLevel(level);
        level.getChunkAt(unknown).addAndRegisterBlockEntity(stale);
        stale.onLoad();
        helper.assertTrue(OreBlock.stoneOf(level.getBlockState(unknown)) == OreHostStone.STONE,
                "an unknown legacy host rock falls back to stone");
        assertNoEntity(helper, unknown, "§103.B: the migrated entity is not kept (unknown host)");
        helper.succeed();
    }

    /**
     * ⑥ A leftover worldgen placeholder: vanilla writes a {@code DUMMY} block-entity tag into the proto
     * chunk for every block whose state has a block entity, and a world generated before §103.B may
     * still carry one where an ore was overwritten later. Promoting it must neither resurrect an entity
     * nor touch the block state.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void leftoverWorldgenPlaceholderResurrectsNothing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        OreBlock ore = oreBlock();
        BlockPos pos = new BlockPos(BASE_X + 6, BASE_Y, BASE_Z + 6);
        level.setBlock(pos, ore.stateFor(OreHostStone.PRISMARINE_DARK), 3);
        CompoundTag dummy = new CompoundTag();
        dummy.putInt("x", pos.getX());
        dummy.putInt("y", pos.getY());
        dummy.putInt("z", pos.getZ());
        dummy.putString("id", "DUMMY");
        level.getChunkAt(pos).setBlockEntityNbt(dummy);
        assertNoEntity(helper, pos, "§103.B: a leftover placeholder promotes to nothing");
        helper.assertTrue(OreBlock.stoneOf(level.getBlockState(pos)) == OreHostStone.PRISMARINE_DARK,
                "a placeholder carries no stone, so the state keeps the host rock it was placed with, got "
                        + OreBlock.stoneOf(level.getBlockState(pos)).id());
        helper.succeed();
    }

    /** ⑦ The real worldgen placement paths record the host rock and keep no entity. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void worldgenPlacementRecordsTheHostRock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int x = BASE_X + 16;
        int z = BASE_Z + 16;

        BlockPos inStone = new BlockPos(x, BASE_Y, z);
        level.setBlock(inStone, Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(GTOreBlockResolver.placeOre(level, inStone, Materials.Iron, false),
                "the resolver places iron ore in stone");
        helper.assertTrue(OreBlock.stoneOf(level.getBlockState(inStone)) == OreHostStone.STONE,
                "the stone host is recorded");

        BlockPos inGranite = new BlockPos(x + 1, BASE_Y, z);
        level.setBlock(inGranite, Blocks.GRANITE.defaultBlockState(), 3);
        helper.assertTrue(GTOreBlockResolver.placeOre(level, inGranite, Materials.Iron, false),
                "the resolver places iron ore in granite");
        helper.assertTrue(OreBlock.stoneOf(level.getBlockState(inGranite)) == OreHostStone.GRANITE,
                "the granite host is recorded");

        // Small ores also generate in loose sediments — the three hosts §103.B had to add.
        BlockPos inSand = new BlockPos(x + 2, BASE_Y, z);
        level.setBlock(inSand, Blocks.SAND.defaultBlockState(), 3);
        helper.assertTrue(GTOreBlockResolver.placeOre(level, inSand, Materials.Iron, true),
                "a small ore generates in sand");
        helper.assertTrue(OreBlock.stoneOf(level.getBlockState(inSand)) == OreHostStone.SAND,
                "the sediment host is recorded for small ores");
        // …but a normal (non-small) ore does not generate in a loose sediment at all.
        BlockPos sandNormal = new BlockPos(x + 5, BASE_Y, z);
        level.setBlock(sandNormal, Blocks.SAND.defaultBlockState(), 3);
        helper.assertTrue(!GTOreBlockResolver.placeOre(level, sandNormal, Materials.Iron, false),
                "a normal (non-small) ore does not generate in a loose sediment");
        helper.assertTrue(level.getBlockState(sandNormal).is(Blocks.SAND), "the sand is left alone");

        BlockPos bedrock = new BlockPos(x + 3, BASE_Y - 1, z);
        level.setBlock(bedrock, Blocks.BEDROCK.defaultBlockState(), 3);
        helper.assertTrue(GTOreBlockResolver.placeBedrockOre(level, bedrock, Materials.Gold),
                "the bedrock deposit replaces bedrock");
        helper.assertTrue(OreBlock.isBedrockOre(level.getBlockState(bedrock)),
                "the bedrock deposit records its bedrock host");

        BlockPos deepslate = new BlockPos(x + 4, BASE_Y, z);
        level.setBlock(deepslate, Blocks.DEEPSLATE.defaultBlockState(), 3);
        helper.assertTrue(GTOreBlockResolver.placeOre(level, deepslate, Materials.Gold, false),
                "the resolver places ore in deepslate");
        helper.assertTrue(OreBlock.stoneOf(level.getBlockState(deepslate)) == OreHostStone.DEEPSLATE,
                "the deepslate host is recorded");

        assertNoEntity(helper, inStone, "§103.B: the resolver keeps no block entity (stone host)");
        assertNoEntity(helper, inGranite, "§103.B: the resolver keeps no block entity (granite host)");
        assertNoEntity(helper, inSand, "§103.B: the resolver keeps no block entity (sediment host)");
        assertNoEntity(helper, bedrock, "§103.B: the bedrock deposit keeps no block entity");
        assertNoEntity(helper, deepslate, "§103.B: the resolver keeps no block entity (deepslate host)");
        helper.succeed();
    }

    /**
     * ⑧ The state budget: one intact and one broken state per host rock per ore block. This is a
     * fixed block-state cost, still avoiding a block entity for every ore in the world.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void oreBlockStateBudget(GameTestHelper helper) {
        int blocks = 0;
        int states = 0;
        for (var block : ForgeRegistries.BLOCKS) {
            if (block instanceof OreBlock ore) {
                blocks++;
                states += ore.getStateDefinition().getPossibleStates().size();
            }
        }
        GregTech.LOGGER.info("[gametest] §103.B ore blocks={} hosts={} states={}",
                blocks, OreHostStone.values().length, states);
        helper.assertTrue(blocks > 400, "the port registers the ore blocks, got " + blocks);
        helper.assertTrue(states == blocks * OreHostStone.values().length * 2,
                "every ore block carries intact/broken states per host rock: " + states + " != "
                        + blocks + " x " + OreHostStone.values().length + " x 2");
        helper.succeed();
    }
}
