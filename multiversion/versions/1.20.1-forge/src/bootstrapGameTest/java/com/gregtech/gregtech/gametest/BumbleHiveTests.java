package com.gregtech.gregtech.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.block.misc.BumbleHiveBlock;
import com.gregtech.gregtech.blockentity.misc.BumbleHiveBlockEntity;
import com.gregtech.gregtech.content.bumble.BumbleBeeGenes;
import com.gregtech.gregtech.content.bumble.BumbleBeeType;
import com.gregtech.gregtech.content.bumble.GTBumbleSpecies;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.worldgen.GTBumbleHivesFeature;
import com.gregtech.gregtech.worldgen.GTFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Guards GT6's wild bumblebee hives: {@code WorldgenHives} (the two overworld passes, the nether and
 * end passes, GT6's colony table and {@code placeHive}) plus the {@code MultiTileEntityBumbleHive}
 * block itself.
 *
 * <p>The colony chain is checked against the biome it is given ({@link GTBumbleHivesFeature#colonyFor})
 * rather than against whatever biome the test world happens to have at the test column.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BumbleHiveTests {
    private static final int BASE_X = 31000;
    private static final int BASE_Z = 31000;
    private static final int BASE_Y = 210;

    /** GT6's colony species: the base species of each comb tier ({@code meta / 100}). */
    private static final int[] COLONY_SPECIES = {0, 100, 200, 300, 400, 500, 600, 700, 800, 900};

    /** GT6's {@code DYE_INT_*} values (CS.java:403-418) and the port's baked colour for each. */
    private static final Object[][] DYE_MAP = {
            {0xFFFFFF, DyeColor.WHITE}, {0xFF8000, DyeColor.ORANGE}, {0xFF00FF, DyeColor.MAGENTA},
            {0x8080FF, DyeColor.LIGHT_BLUE}, {0xFFFF00, DyeColor.YELLOW}, {0x80FF80, DyeColor.LIME},
            {0xFFC0C0, DyeColor.PINK}, {0x808080, DyeColor.GRAY}, {0xC0C0C0, DyeColor.LIGHT_GRAY},
            {0x00FFFF, DyeColor.CYAN}, {0x800080, DyeColor.PURPLE}, {0x0000FF, DyeColor.BLUE},
            {0x604000, DyeColor.BROWN}, {0x00FF00, DyeColor.GREEN}, {0xFF0000, DyeColor.RED},
            {0x202020, DyeColor.BLACK},
            // GT6's literals: the end, the nether and the grass colony.
            {0x00AAAA, DyeColor.CYAN}, {0xAA0000, DyeColor.RED}, {0xFFDD99, DyeColor.YELLOW}};

    private static BlockPos base(int index) {
        return new BlockPos(BASE_X + index * 16, BASE_Y, BASE_Z);
    }

    private static String itemId(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.getPath();
    }

    private static JsonObject resourceJson(String path) throws Exception {
        try (InputStream stream = BumbleHiveTests.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("missing resource " + path);
            return GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    private static boolean resourceExists(String path) {
        return BumbleHiveTests.class.getClassLoader().getResource(path) != null;
    }

    private static Holder<Biome> biome(GameTestHelper helper, String id) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(id)));
    }

    /** GTBlockEntities.BUMBLE_HIVE missing from the class under test until the feature is wired. */
    private static final Direction[] FIVE_SIDES =
            {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN};

    /** The hive block, its block entity, its sixteen baked colours and GT6's colony species. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void hiveIsRegistered(GameTestHelper helper) throws Exception {
        List<String> problems = new ArrayList<>();
        Block hive = GTDecorBlocks.BUMBLE_HIVE.get();
        if (!GTBlockEntities.BUMBLE_HIVE.get().isValid(hive.defaultBlockState())) {
            problems.add("the hive block entity type must cover the hive");
        }
        if (hive.defaultBlockState().getValue(BumbleHiveBlock.COLOR) != DyeColor.LIGHT_GRAY) {
            problems.add("GT6's default hive colour is light grey");
        }
        if (GTFeatures.BUMBLE_HIVES == null || !GTFeatures.BUMBLE_HIVES.getId().getPath().equals("gt_bumble_hives")) {
            problems.add("the feature is registered as gt_bumble_hives");
        }
        // GT6 tints one pair of icons at runtime; the port bakes all sixteen colours.
        JsonObject variants = resourceJson("assets/gregtech/blockstates/bumble_hive.json").getAsJsonObject("variants");
        if (variants.size() != 16) problems.add("16 baked hive colours expected, got " + variants.size());
        for (DyeColor color : DyeColor.values()) {
            String name = color.getName();
            JsonElement variant = variants.get("color=" + name);
            if (variant == null) {
                problems.add("no block state variant for " + name);
                continue;
            }
            String model = variant.getAsJsonObject().get("model").getAsString();
            String path = model.startsWith("gregtech:") ? model.substring("gregtech:".length()) : model;
            // The model id's path already starts with "block/", e.g. gregtech:block/bumble_hive_white.
            if (!resourceExists("assets/gregtech/models/" + path + ".json")) {
                problems.add("missing model " + model);
            }
            for (String face : new String[]{"bottom", "top", "side"}) {
                if (!resourceExists("assets/gregtech/textures/block/nature/bumblehive/" + name + "/" + face + ".png")) {
                    problems.add("missing texture " + name + "/" + face + ".png");
                }
            }
        }
        for (String asset : new String[]{
                "assets/gregtech/models/item/bumble_hive.json",
                // GT6's own tint sources, kept next to the baked colours.
                "assets/gregtech/textures/block/nature/bumblehive/colored/side.png",
                "assets/gregtech/textures/block/nature/bumblehive/overlay/side.png"}) {
            if (!resourceExists(asset)) problems.add("missing " + asset);
        }
        if (!resourceJson("assets/gregtech/lang/en_us.json").has("block.gregtech.bumble_hive")
                || !resourceJson("assets/gregtech/lang/zh_cn.json").has("block.gregtech.bumble_hive")) {
            problems.add("the hive needs its en_us and zh_cn name");
        }
        // Every colony species GT6's table can pick has to exist, with its comb and its four bee types.
        for (int speciesId : COLONY_SPECIES) {
            GTBumbleSpecies.Species species = GTBumbleSpecies.byId(speciesId);
            if (species == null) {
                problems.add("species " + speciesId + " is missing");
                continue;
            }
            if (ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("gregtech", species.comb())) == null) {
                problems.add("the comb " + species.comb() + " is missing");
            }
            for (BumbleBeeType type : new BumbleBeeType[]{BumbleBeeType.DRONE, BumbleBeeType.PRINCESS,
                    BumbleBeeType.QUEEN, BumbleBeeType.DEAD}) {
                if (!BumbleBeeType.exists(species, type)) {
                    problems.add("missing bee " + BumbleBeeType.itemId(species, type));
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), "hive registration (" + problems.size() + "): "
                + problems.subList(0, Math.min(6, problems.size())));
        helper.succeed();
    }

    /** GT6's colony chain ({@code WorldgenHives:155-186}), in its own test order. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void hiveColonyRulesFollowGt6(GameTestHelper helper) {
        Holder<Biome> plains = biome(helper, "plains");
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState sand = Blocks.SAND.defaultBlockState();

        // The block rules.
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(plains, false, grass) == GTBumbleHivesFeature.GRASS,
                "grass is the grass colony");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(plains, false, dirt) == GTBumbleHivesFeature.DIRT,
                "dirt is the dirt colony");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(plains, false, Blocks.RED_SAND.defaultBlockState())
                        == GTBumbleHivesFeature.RED_SAND,
                "red sand is the red-sand colony (GT6 checks it before plain sand)");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(plains, false, sand) == GTBumbleHivesFeature.SAND,
                "sand is the sandy colony");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(plains, false, Blocks.SANDSTONE.defaultBlockState())
                        == GTBumbleHivesFeature.SAND,
                "sandstone is the sandy colony too");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(plains, false, Blocks.GRAVEL.defaultBlockState())
                        == GTBumbleHivesFeature.ROCK,
                "gravel is the rocky colony");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(plains, false, stone) == GTBumbleHivesFeature.ROCK,
                "rock is the rocky colony");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(plains, false, Blocks.MYCELIUM.defaultBlockState())
                        == GTBumbleHivesFeature.SHROOM,
                "mycelium is the mushroom colony");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(plains, false, Blocks.IRON_BLOCK.defaultBlockState())
                        == GTBumbleHivesFeature.MAGICAL,
                "anything else falls back to the magical colony, so magic combs stay obtainable");

        // Water next to the hive wins over every other rule.
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(plains, true, grass) == GTBumbleHivesFeature.WATER,
                "a water neighbour makes it a water colony");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(biome(helper, "jungle"), true, grass)
                        == GTBumbleHivesFeature.WATER,
                "the water neighbour is checked before the biome");

        // The biome rules, each before the block rules.
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(biome(helper, "flower_forest"), false, stone)
                        == GTBumbleHivesFeature.MAGICAL,
                "magical woods are the magical colony");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(biome(helper, "jungle"), false, stone)
                        == GTBumbleHivesFeature.JUNGLE,
                "jungles are the jungle colony");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(biome(helper, "snowy_plains"), false, stone)
                        == GTBumbleHivesFeature.FROZEN,
                "snowy biomes are the frosty colony");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(biome(helper, "mushroom_fields"), false, stone)
                        == GTBumbleHivesFeature.SHROOM,
                "mushroom fields are the mushroom colony");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(biome(helper, "ocean"), false, stone)
                        == GTBumbleHivesFeature.WATER,
                "oceans are the water colony");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(biome(helper, "beach"), false, stone)
                        == GTBumbleHivesFeature.WATER,
                "beaches are the water colony");
        helper.assertTrue(GTBumbleHivesFeature.colonyFor(biome(helper, "desert"), false, sand)
                        == GTBumbleHivesFeature.SAND,
                "a desert alone is no colony: the block still decides");

        // GT6's species/colour table.
        helper.assertTrue(GTBumbleHivesFeature.UNDERGROUND.speciesId() == 500
                        && GTBumbleHivesFeature.UNDERGROUND.dye() == DyeColor.LIGHT_GRAY,
                "the underground pass plants the rocky colony");
        helper.assertTrue(GTBumbleHivesFeature.NETHER.speciesId() == 300 && GTBumbleHivesFeature.NETHER.color() == 0xAA0000,
                "the nether pass plants the nether colony in GT6's 0xaa0000");
        helper.assertTrue(GTBumbleHivesFeature.END.speciesId() == 400 && GTBumbleHivesFeature.END.color() == 0x00AAAA,
                "the end pass plants the end colony in GT6's 0x00aaaa");
        helper.succeed();
    }

    /** GT6's {@code placeHive}: genome from the environment, then comb, princess and drones. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void hiveContentsFollowGt6(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        BlockPos pos = base(1);
        server.getBlockState(pos);
        server.removeBlock(pos, false);

        CompoundTag genes = new CompoundTag();
        BumbleBeeGenes.setWorkForce(genes, 2500);
        BumbleBeeGenes.setOffspring(genes, 7);
        helper.assertTrue(GTBumbleHivesFeature.placeHive(level, pos, GTBumbleHivesFeature.ROCK,
                RandomSource.create(1L), genes), "placing a hive succeeds");
        BlockState state = server.getBlockState(pos);
        helper.assertTrue(state.getBlock() instanceof BumbleHiveBlock, "the hive block is placed");
        helper.assertTrue(BumbleHiveBlock.colorOf(state) == DyeColor.LIGHT_GRAY,
                "the rock colony is GT6's light grey");
        helper.assertTrue(server.getBlockEntity(pos) instanceof BumbleHiveBlockEntity, "the hive has its block entity");
        BumbleHiveBlockEntity hive = (BumbleHiveBlockEntity) server.getBlockEntity(pos);

        // GT6: UT.Code.units(workForce, 10000, 10, T) = ceil(work / 1000).
        helper.assertTrue(GTBumbleHivesFeature.combCount(1) == 1 && GTBumbleHivesFeature.combCount(999) == 1
                        && GTBumbleHivesFeature.combCount(1000) == 1 && GTBumbleHivesFeature.combCount(1001) == 2
                        && GTBumbleHivesFeature.combCount(10000) == 10,
                "GT6 rounds the comb amount up, so it is ceil(work / 1000)");
        ItemStack comb = hive.inventory().getStackInSlot(0);
        helper.assertTrue("rock_comb".equals(itemId(comb)) && comb.getCount() == 3,
                "2500 work force means 3 rock combs, got " + comb);
        ItemStack princess = hive.inventory().getStackInSlot(1);
        helper.assertTrue("stoned_bumblebee_princess".equals(itemId(princess)),
                "a stoned princess, got " + itemId(princess));
        helper.assertTrue(BumbleBeeType.of(princess) == BumbleBeeType.PRINCESS
                        && BumbleBeeType.speciesOf(princess) == GTBumbleSpecies.byId(500),
                "the princess carries its species");
        ItemStack drones = hive.inventory().getStackInSlot(2);
        helper.assertTrue("stoned_bumblebee_drone".equals(itemId(drones)) && drones.getCount() == 7,
                "GT6 puts `offspring` drones in, got " + drones);
        for (ItemStack stack : new ItemStack[]{princess, drones}) {
            CompoundTag carried = BumbleBeeGenes.peek(stack);
            helper.assertTrue(carried != null && BumbleBeeGenes.workForce(carried) == 2500,
                    "the colony's bees carry the hive's genome");
        }

        // Zero offspring means no drone stack at all (GT6's makeInv skips the empty stack).
        CompoundTag lonely = new CompoundTag();
        BumbleBeeGenes.setWorkForce(lonely, 1);
        BumbleBeeGenes.setOffspring(lonely, 0);
        BlockPos second = base(2);
        server.removeBlock(second, false);
        helper.assertTrue(GTBumbleHivesFeature.placeHive(level, second, GTBumbleHivesFeature.WATER,
                RandomSource.create(2L), lonely), "the second hive is placed");
        BumbleHiveBlockEntity other = (BumbleHiveBlockEntity) server.getBlockEntity(second);
        helper.assertTrue(other != null, "the second hive has its block entity");
        helper.assertTrue("water_comb".equals(itemId(other.inventory().getStackInSlot(0)))
                        && other.inventory().getStackInSlot(0).getCount() == 1,
                "1 work force is one comb");
        helper.assertTrue(other.inventory().getStackInSlot(2).isEmpty(),
                "an offspring of 0 leaves the drone slot empty");
        helper.assertTrue(BumbleHiveBlock.colorOf(server.getBlockState(second)) == DyeColor.LIGHT_BLUE,
                "the water colony is light blue");

        // Only a player's break hands the colony out (GT6's mDroppable).
        Player player = helper.makeMockPlayer();
        state.getBlock().playerWillDestroy(server, pos, state, player);
        helper.assertTrue(hive.contents().isEmpty(), "breaking the hive empties it");
        List<ItemEntity> drops = server.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0));
        helper.assertTrue(drops.size() == 3, "comb, princess and drones drop, got " + drops.size());
        helper.succeed();
    }

    /** GT6's embedded rule: five of the six sides solid, and no liquid anywhere. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void hiveEmbeddedRuleFollowsGt6(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        BlockPos centre = base(3);
        server.getBlockState(centre);
        for (Direction side : Direction.values()) {
            server.setBlock(centre.relative(side), Blocks.STONE.defaultBlockState(), 3);
        }
        helper.assertFalse(GTBumbleHivesFeature.embedded(level, centre),
                "six solid sides is a sealed pocket, GT6 wants exactly five");
        server.setBlock(centre.above(), Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(GTBumbleHivesFeature.embedded(level, centre), "five solid sides is a pocket");
        server.setBlock(centre.north(), Blocks.AIR.defaultBlockState(), 3);
        helper.assertFalse(GTBumbleHivesFeature.embedded(level, centre), "four solid sides is not");
        server.setBlock(centre.north(), Blocks.STONE.defaultBlockState(), 3);
        server.setBlock(centre.above(), Blocks.WATER.defaultBlockState(), 3);
        helper.assertFalse(GTBumbleHivesFeature.embedded(level, centre),
                "GT6 bails out when any side is liquid");
        server.setBlock(centre.above(), Blocks.LAVA.defaultBlockState(), 3);
        helper.assertFalse(GTBumbleHivesFeature.embedded(level, centre), "lava counts as liquid too");
        server.setBlock(centre.above(), Blocks.AIR.defaultBlockState(), 3);

        // The collision test behind the surface pass.
        helper.assertTrue(GTBumbleHivesFeature.hasCollision(server, centre.relative(Direction.DOWN)),
                "stone has a collision box");
        helper.assertFalse(GTBumbleHivesFeature.hasCollision(server, centre.above()),
                "air has none");
        helper.succeed();
    }

    /** The datapack wiring: one configured feature, added to the overworld, nether and end. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void hiveWorldgenDataMatchesGt6(GameTestHelper helper) throws Exception {
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTBumbleHivesFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_bumble_hives.json is loaded");
        JsonObject placed = resourceJson("data/gregtech/worldgen/placed_feature/gt_bumble_hives.json");
        helper.assertTrue(placed.get("feature").getAsString().equals("gregtech:gt_bumble_hives"),
                "the placed feature points at the feature");
        helper.assertTrue(placed.getAsJsonArray("placement").isEmpty(),
                "an empty placement list is GT6's one attempt per chunk");
        String[][] modifiers = {
                {"gt_bumble_hives", "#minecraft:is_overworld"},
                {"gt_bumble_hives_nether", "#minecraft:is_nether"},
                {"gt_bumble_hives_end", "#minecraft:is_end"}};
        for (String[] entry : modifiers) {
            JsonObject modifier = resourceJson("data/gregtech/forge/biome_modifier/" + entry[0] + ".json");
            helper.assertTrue(modifier.get("biomes").getAsString().equals(entry[1]),
                    entry[0] + " targets " + entry[1]);
            JsonArray features = modifier.getAsJsonArray("features");
            helper.assertTrue(features.size() == 1
                            && features.get(0).getAsString().equals("gregtech:gt_bumble_hives"),
                    entry[0] + " adds the hive feature");
            helper.assertTrue(modifier.get("step").getAsString().equals("vegetal_decoration"),
                    entry[0] + " runs after the terrain");
        }
        // GT6's colour ints as the port's baked colours.
        for (Object[] entry : DYE_MAP) {
            int color = (Integer) entry[0];
            DyeColor expected = (DyeColor) entry[1];
            helper.assertTrue(GTBumbleHivesFeature.dye(color) == expected,
                    String.format("GT6's 0x%06x must bake as %s", color, expected.getName()));
        }
        helper.assertTrue(GTBumbleHivesFeature.dye(0x123456) == DyeColor.LIGHT_GRAY,
                "an unknown colour falls back to the default light grey");
        helper.succeed();
    }

    /** The overworld's two passes and their placement positions ({@code WorldgenHives:127-190}). */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void hivePassesPlaceColonies(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        RandomSource random = RandomSource.create(11L);

        // ── the rock pocket pass: y 8..27 above bedrock, embedded in natural rock ──────────────
        int x = BASE_X + 200;
        int z = BASE_Z + 200;
        // The band is remapped into 1.18's deeper overworld, exactly like the ore tables.
        int lowY = com.gregtech.gregtech.worldgen.GTWorldgenScale.remapY(
                (WorldGenLevel) server, GTBumbleHivesFeature.UNDERGROUND_MIN_Y);
        int highY = com.gregtech.gregtech.worldgen.GTWorldgenScale.remapY(
                (WorldGenLevel) server, GTBumbleHivesFeature.UNDERGROUND_MAX_Y);
        int pocketY = com.gregtech.gregtech.worldgen.GTWorldgenScale.remapY((WorldGenLevel) server, 20);
        for (int y = lowY; y < highY; y++) {
            server.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        BlockPos pocket = new BlockPos(x, pocketY, z);
        server.setBlock(pocket, Blocks.STONE.defaultBlockState(), 3);
        // GT6's pocket is a solid block with exactly ONE open side, and its scan runs bottom-up, so
        // the open side must be DOWN: a solid block directly below would itself be a valid pocket at
        // pocketY - 1, and the pass would place the hive there instead.
        for (Direction side : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH,
                Direction.WEST, Direction.EAST}) {
            server.setBlock(pocket.relative(side), Blocks.STONE.defaultBlockState(), 3);
        }
        server.setBlock(pocket.below(), Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(GTBumbleHivesFeature.placeUnderground(level, x, z, random),
                "the rock pocket pass finds the pocket");
        BlockState underground = server.getBlockState(pocket);
        if (!(underground.getBlock() instanceof BumbleHiveBlock)) {
            StringBuilder found = new StringBuilder();
            for (int y = lowY; y < highY; y++) {
                BlockState state = server.getBlockState(new BlockPos(x, y, z));
                if (state.getBlock() instanceof BumbleHiveBlock) found.append(" y=").append(y);
            }
            helper.assertTrue(false, "the hive replaces the rock block, got " + underground
                    + " (pocket y=" + pocketY + ", hives in the band:" + (found.length() == 0 ? " none" : found) + ")");
        }
        helper.assertTrue(BumbleHiveBlock.colorOf(underground) == DyeColor.LIGHT_GRAY,
                "the underground colony is the rocky light grey one");
        BumbleHiveBlockEntity caveHive = (BumbleHiveBlockEntity) server.getBlockEntity(pocket);
        helper.assertTrue(caveHive != null && "rock_comb".equals(itemId(caveHive.inventory().getStackInSlot(0))),
                "the cave hive holds rock combs");

        // ── the surface pass: the hive goes under the first opaque block ───────────────────────
        int surfaceX = BASE_X + 220;
        int surfaceZ = BASE_Z + 220;
        // Level#getHeight answers with the world floor while the chunk is still unloaded, and this
        // column is far away from every other test, so load it before asking for its heightmap.
        server.getBlockState(new BlockPos(surfaceX, 0, surfaceZ));
        int top = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, surfaceX, surfaceZ);
        for (int y = top - 2; y <= top + 3; y++) {
            server.setBlock(new BlockPos(surfaceX, y, surfaceZ), Blocks.AIR.defaultBlockState(), 3);
        }
        BlockPos ground = new BlockPos(surfaceX, top - 1, surfaceZ);
        BlockPos hivePos = ground.below();
        server.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        server.setBlock(hivePos, Blocks.DIRT.defaultBlockState(), 3);
        for (Direction side : FIVE_SIDES) {
            server.setBlock(hivePos.relative(side), Blocks.AIR.defaultBlockState(), 3);
        }
        boolean surfacePlaced = GTBumbleHivesFeature.placeSurface(level, surfaceX, surfaceZ, random);
        if (!surfacePlaced) {
            helper.assertTrue(false, "the surface pass places a hive under the ground block:"
                    + " heightmap=" + server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, surfaceX, surfaceZ)
                    + " top=" + top
                    + " at-top=" + server.getBlockState(new BlockPos(surfaceX, top, surfaceZ))
                    + " ground=" + server.getBlockState(ground)
                    + " hive=" + server.getBlockState(hivePos)
                    + " north=" + server.getBlockState(hivePos.north())
                    + " down=" + server.getBlockState(hivePos.below()));
        }
        BlockState surface = server.getBlockState(hivePos);
        helper.assertTrue(surface.getBlock() instanceof BumbleHiveBlock,
                "the hive sits where the ground block's support was, got " + surface);
        // The colony comes from the biome at that spot plus the grass above it - the water rule cannot
        // fire because the neighbours were cleared.
        GTBumbleHivesFeature.Colony expected = GTBumbleHivesFeature.colonyFor(
                server.getBiome(hivePos), false, Blocks.GRASS_BLOCK.defaultBlockState());
        helper.assertTrue(BumbleHiveBlock.colorOf(surface) == expected.dye(),
                "the grass colony of this biome is " + expected.trigger() + ", got "
                        + BumbleHiveBlock.colorOf(surface).getName());
        BumbleHiveBlockEntity colony = (BumbleHiveBlockEntity) server.getBlockEntity(hivePos);
        helper.assertTrue(colony != null, "the surface hive has its block entity");
        ItemStack comb = colony.inventory().getStackInSlot(0);
        GTBumbleSpecies.Species species = GTBumbleSpecies.byId(expected.speciesId());
        helper.assertTrue(species != null && species.comb().equals(itemId(comb)),
                "the colony's comb is " + (species == null ? "?" : species.comb()) + ", got " + itemId(comb));
        helper.assertTrue(BumbleBeeType.speciesOf(colony.inventory().getStackInSlot(1)) == species,
                "the princess is of the same species as the comb");
        helper.succeed();
    }
}
