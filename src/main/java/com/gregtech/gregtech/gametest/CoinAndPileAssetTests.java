package com.gregtech.gregtech.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.block.misc.CoinPileBlock;
import com.gregtech.gregtech.block.misc.PileBlock;
import com.gregtech.gregtech.blockentity.misc.CoinPileBlockEntity;
import com.gregtech.gregtech.blockentity.misc.PileBlockEntity;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The port's coins and the four pile shapes.
 *
 * <p>GT6 has no coin prefix item at all - {@code OP.coin} is declared {@code unused}
 * ({@code gregapi/data/OP.java:473}) and a coin is the placeable multi-tile
 * {@code MultiTileEntityCoin} (id 32700, {@code Loader_MultiTileEntities.java:2240}) whose stack
 * carries the material in {@code gt.material} ({@code MultiTileEntityCoin.java:83}) and is handed out
 * through {@code COIN_MAP} ({@code :124-133}). The port registers one coin item per material
 * ({@code MaterialPrefix.coin}, {@code gregtech:coin_<material>}), which is the port's own way of
 * registering a material form. These tests pin the item count, the pile's accept/refuse rule, the
 * dungeon's coins and the whole asset chain the four pile blocks reference.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class CoinAndPileAssetTests {
    private static final int BASE_X = 65000;
    private static final int BASE_Z = 65000;
    private static final int Y = 210;

    /**
     * The two items whose registry path starts with {@code coin_} without being a coin, which is why the
     * raw {@code coin_}-prefixed item count is two higher than the coin count: the coin pile's own block
     * item ({@code GTDecorBlocks.COIN_PILE}, {@code coin_pile}) and GT6's coinage mold
     * ({@code MultiTileEntityMoldCoinage}, multi-tile 32701, {@code Loader_MultiTileEntities.java:2239};
     * the port's block {@code GTToolBlocks.java:172}, {@code coin_mold}). Named instead of counted, so a
     * third item joining them shows up as its own name.
     */
    private static final Set<String> COIN_PREFIXED_NON_COINS = Set.of("coin_mold", "coin_pile");

    /** GT6's four forced coin metals ({@code MultiTileEntityCoin.java:286}). */
    private static final List<String> FORCED_COIN_METALS = List.of("Copper", "Silver", "Gold", "Platinum");

    /** The four pile blocks, in registration order, with the number of models their block state uses. */
    private static final Map<String, Integer> PILE_BLOCKS = Map.of(
            "ingot_pile", 65, "plate_pile", 65, "plate_gem_pile", 65, "coin_pile", 68);

    // ── resources ─────────────────────────────────────────────────────────────────────────────────

    /**
     * A resource as text: the jar first, then the source tree.
     *
     * <p>The classpath copy is what the game reads, so that is what is checked first - but the source
     * tree is the file a change actually lands in, so it stands in while {@code processResources} has
     * not run yet.</p>
     */
    private static String resource(String path) throws Exception {
        try (InputStream stream = CoinAndPileAssetTests.class.getClassLoader().getResourceAsStream(path)) {
            if (stream != null) return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        Path source = Path.of("src/main/resources", path);
        if (Files.exists(source)) return Files.readString(source, StandardCharsets.UTF_8);
        throw new IllegalStateException("missing resource " + path);
    }

    private static JsonObject resourceJson(String path) throws Exception {
        return GsonHelper.parse(resource(path));
    }

    private static boolean resourceExists(String path) {
        if (CoinAndPileAssetTests.class.getClassLoader().getResource(path) != null) return true;
        return Files.exists(Path.of("src/main/resources", path));
    }

    // ── the coins ─────────────────────────────────────────────────────────────────────────────────

    private static ItemStack coin(String material) {
        return GTItems.getStack(MaterialPrefix.coin, GTMaterialRegistry.get(material));
    }

    /**
     * The coin items this run's material registry asks for: the very loop
     * {@code Loader_Items.registerPrefix} runs - hidden materials and aliases out, the prefix's own rule
     * in - turned into the item ids it registers ({@code MaterialPrefix.getItemId}).
     *
     * <p>Reading the expectation from the registry instead of a written-down number is what keeps the
     * guard honest: it fails when a material of the rule has no coin, and it follows the port when the
     * material set legitimately grows ({@code Loader_Materials} adds {@code STONE} to the stone materials
     * after {@code init()}, which turns seven of them into coin materials).</p>
     */
    private static Set<String> expectedCoins() {
        Set<String> ids = new TreeSet<>();
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            if (material.has(MaterialProperty.HIDDEN)) continue;
            if (material.resolve() != material) continue;
            if (!MaterialPrefix.coin.isValidFor(material)) continue;
            ids.add(MaterialPrefix.coin.getItemId(material));
        }
        return ids;
    }

    /** Up to four sorted entries of a set, for a failure message that names the difference. */
    private static String first(Set<String> entries) {
        List<String> list = new ArrayList<>(entries);
        return list.subList(0, Math.min(4, list.size())).toString();
    }

    /**
     * Every material of GT6's coin condition has its coin item, and both counts come out of this run's
     * own registries.
     *
     * <p>GT6 rolls those materials up in {@code onRegistration} ({@code MultiTileEntityCoin.java:285-328});
     * the port registers them as ordinary material items, so the prefix's own rule decides the item set
     * here - {@link #expectedCoins} reads the material registry the same way
     * {@code Loader_Items.registerPrefix} does, so a material the port gains or loses later moves both
     * sides of the comparison together and cannot break the guard by a legitimate material-set
     * difference.</p>
     *
     * <p>Measured in the running game at this revision: <b>593</b> coin materials, i.e. <b>593</b>
     * {@code gregtech:coin_<i>&lt;material&gt;</i>} items. 586 of them already pass the coin rule without
     * {@code Loader_Materials}' last pass, and the other seven - Chalk, Dolomite, Gypsum, OilShale, Salt,
     * Sylvite and Talc - are {@code StoneType} materials that only become plate (and therefore tiny-plate
     * and coin) materials once {@code Loader_Materials.java:14} puts {@code MaterialProperty.STONE} on
     * them, which {@code HAS_PLATE} treats as a plate source. <b>595</b> items carry a {@code coin_} path
     * because two of them are not coins at all: the coin pile's own block item ({@code coin_pile},
     * {@code GTDecorBlocks}) and GT6's coinage mold ({@code coin_mold}, {@code GTToolBlocks.java:172}) -
     * see the {@code coinPrefixed} check below, which names them instead of counting them as coins.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void coinItemsExistForTheirMaterials(GameTestHelper helper) throws Exception {
        List<String> problems = new ArrayList<>();
        // Per material: its own coin item, of its own material, under the prefix's own item id.
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            if (material.has(MaterialProperty.HIDDEN)) continue;
            if (material.resolve() != material) continue;
            if (!MaterialPrefix.coin.isValidFor(material)) continue;
            ItemStack stack = GTItems.getStack(MaterialPrefix.coin, material);
            if (stack.isEmpty()) {
                problems.add("no coin for " + material.getName());
                continue;
            }
            if (!(stack.getItem() instanceof MaterialItem item) || item.getPrefix() != MaterialPrefix.coin
                    || item.getMaterial().resolve() != material.resolve()) {
                problems.add("the coin of " + material.getName() + " is not its own material item, got "
                        + stack);
                continue;
            }
            String id = MaterialPrefix.coin.getItemId(material);
            if (ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(GregTech.MODID + ":" + id))
                    != stack.getItem()) {
                problems.add(GregTech.MODID + ":" + id + " is not registered under the prefix's own id");
            }
        }
        // The registry's own answer, split into the coins and whatever else shares their path prefix.
        Set<String> expected = expectedCoins();
        Set<String> coins = new TreeSet<>();
        Set<String> coinPrefixed = new TreeSet<>();
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null || !id.getNamespace().equals(GregTech.MODID)) continue;
            if (item instanceof MaterialItem materialItem && materialItem.getPrefix() == MaterialPrefix.coin) {
                coins.add(id.getPath());
            } else if (id.getPath().startsWith("coin_")) {
                coinPrefixed.add(id.getPath());
            }
        }
        if (!coins.equals(expected)) {
            Set<String> absent = new TreeSet<>(expected);
            absent.removeAll(coins);
            Set<String> extra = new TreeSet<>(coins);
            extra.removeAll(expected);
            problems.add("registered " + coins.size() + " coin items for " + expected.size()
                    + " coin materials; without an item: " + first(absent) + ", unexpected: " + first(extra));
        }
        if (!coinPrefixed.equals(COIN_PREFIXED_NON_COINS)) {
            problems.add("coin_-prefixed items that are no coins: " + coinPrefixed
                    + ", expected " + COIN_PREFIXED_NON_COINS);
        }
        if (expected.size() < 500) {
            problems.add("only " + expected.size() + " coin materials: the port's tiny-plate set is ~590");
        }
        for (String metal : FORCED_COIN_METALS) {
            if (coin(metal).isEmpty()) problems.add("GT6's forced coin metal " + metal + " has no port coin");
        }
        if (MaterialPrefix.coin.getMaterialWeight() != com.gregtech.gregtech.api.material.GTValues.U9) {
            problems.add("GT6 recycles one coin as one ninth of a unit (MultiTileEntityCoin.java:99)");
        }
        // One coin per material is one *item* per material: the coin's texture is GT6's COIN icon in
        // every texture set, so the shared model has to exist for all of them.
        for (String set : new String[]{"metallic", "dull", "shiny", "fine", "stone", "wood", "ruby", "copper"}) {
            if (!resourceExists("assets/gregtech/models/item/material/" + set + "/coin.json")) {
                problems.add("missing shared coin model for the " + set + " texture set");
            }
            if (!resourceExists("assets/gregtech/textures/item/material_icons/" + set + "/coin.png")) {
                problems.add("missing coin icon for the " + set + " texture set");
            }
        }
        helper.assertTrue(problems.isEmpty(), "coins (" + problems.size() + "): "
                + problems.subList(0, Math.min(6, problems.size())));
        helper.succeed();
    }

    /** A coin stack keeps its identity through item NBT and through the pile's own key. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void coinStackRoundTripsNbt(GameTestHelper helper) {
        ItemStack silver = coin("Silver").copyWithCount(7);
        helper.assertTrue(!silver.isEmpty(), "the port registers a silver coin");

        CompoundTag saved = silver.save(new CompoundTag());
        ItemStack restored = ItemStack.of(saved);
        helper.assertTrue(ItemStack.isSameItemSameTags(silver, restored) && restored.getCount() == 7,
                "a coin stack round-trips its item and count, got " + restored);
        helper.assertTrue(CoinPileBlock.isCoin(restored), "and it is still a coin after the round trip");

        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(BASE_X, Y, BASE_Z);
        level.setBlock(pos, GTDecorBlocks.COIN_PILE.get().defaultBlockState(), 2);
        CoinPileBlockEntity pile = (CoinPileBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(pile != null, "the coin pile has its block entity");
        helper.assertTrue(pile.add(5, silver.copyWithCount(1)) == 1, "the pile takes the silver coin");
        CompoundTag tag = pile.saveWithoutMetadata();
        helper.assertTrue(tag.contains(CoinPileBlockEntity.NBT_COIN), "the pile stores its coin item");
        CoinPileBlockEntity reloaded = new CoinPileBlockEntity(pos, level.getBlockState(pos));
        reloaded.load(tag);
        helper.assertTrue(ItemStack.isSameItemSameTags(reloaded.coinItem(), silver.copyWithCount(1)),
                "the pile's coin survived the round trip, got " + reloaded.coinItem());
        helper.assertTrue(reloaded.total() == 1 && reloaded.faceCount(5) == 1,
                "and so did its faces, got " + reloaded.total());
        level.removeBlock(pos, false);
        helper.succeed();
    }

    /**
     * The pile takes real coins of its own metal and refuses everything else - including the pile's own
     * block item, which carries no material and therefore matches no coin of GT6's {@code COIN_MAP}.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void coinPileTakesRealCoinsOnly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(BASE_X + 8, Y, BASE_Z);
        level.setBlock(pos, GTDecorBlocks.COIN_PILE.get().defaultBlockState(), 2);
        CoinPileBlockEntity pile = (CoinPileBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(pile != null, "the coin pile has its block entity");

        ItemStack blockItem = new ItemStack(GTDecorBlocks.COIN_PILE.get());
        helper.assertTrue(!CoinPileBlock.isCoin(blockItem), "the pile's own item is not a coin");
        helper.assertTrue(pile.add(0, blockItem) == 0,
                "GT6's pile refuses a stack that matches no coin of its material");
        helper.assertTrue(pile.isEmpty(), "and takes nothing out of the pile for it");

        ItemStack copper = coin("Copper");
        helper.assertTrue(!copper.isEmpty(), "the port registers a copper coin");
        helper.assertTrue(pile.add(0, copper.copyWithCount(1)) == 1, "a copper coin goes on face 0");
        helper.assertTrue(ItemStack.isSameItemSameTags(pile.coinItem(), copper.copyWithCount(1)),
                "the pile binds to the copper coin, got " + pile.coinItem());
        helper.assertTrue(pile.add(1, copper.copyWithCount(1)) == 1, "a second face takes another one");
        helper.assertTrue(pile.add(2, coin("Silver").copyWithCount(1)) == 0,
                "GT6 keeps one material per pile (mMaterial, MultiTileEntityCoin.java:72)");
        helper.assertTrue(pile.add(2, coin("Gold").copyWithCount(1)) == 0, "and refuses the next metal");
        helper.assertTrue(pile.total() == 2, "no refused coin landed on the pile, got " + pile.total());

        // The pile's look follows its contents: GT6 draws one coin per occupied face
        // (MultiTileEntityCoin.java:346-367) and the port carries that count in the block state.
        helper.assertTrue(level.getBlockState(pos).getValue(CoinPileBlock.COINS) == 2,
                "two of the sixteen faces carry coins, state " + level.getBlockState(pos));
        helper.assertTrue(level.getBlockState(pos).getValue(CoinPileBlock.FILL) == 1,
                "one coin is GT6's lowest coin height, state " + level.getBlockState(pos));
        for (int i = 0; i < 4; i++) {
            helper.assertTrue(pile.add(0, copper.copyWithCount(1)) == 1, "one more coin on face 0");
        }
        helper.assertTrue(pile.faceCount(0) == 5, "face 0 holds five coins, got " + pile.faceCount(0));
        helper.assertTrue(level.getBlockState(pos).getValue(CoinPileBlock.FILL) == 2,
                "five coins round up to the second of four heights, state " + level.getBlockState(pos));
        helper.assertTrue(level.getBlockState(pos).getValue(CoinPileBlock.COINS) == 2,
                "the face count did not change, state " + level.getBlockState(pos));
        level.removeBlock(pos, false);
        helper.succeed();
    }

    /** GT6's dungeon places real coins, out of GT6's own dungeon coin list. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void dungeonPlacesRealCoins(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        BlockPos origin = new BlockPos(BASE_X + 64, Y, BASE_Z + 64);
        for (int dx = 1; dx <= 9; dx += 2) level.removeBlock(origin.offset(dx, 1, 1), false);
        GTDungeonData data = new GTDungeonData(level, origin.getX(), origin.getY(), origin.getZ(),
                StoneType.LIMESTONE, StoneType.SLATE, 3, new byte[5][5], 2, 2, 0,
                new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                RandomSource.create(65000L));

        helper.assertTrue(data.coins(1, 1, 1), "the dungeon places its coin pile");
        CoinPileBlockEntity rolled = (CoinPileBlockEntity) server.getBlockEntity(origin.offset(1, 1, 1));
        helper.assertTrue(rolled != null, "the coin pile arrived with its block entity");
        helper.assertTrue(rolled.total() >= 1, "GT6's guaranteed face left at least one coin behind");
        helper.assertTrue(CoinPileBlock.isCoin(rolled.coinItem()),
                "the dungeon's pile is bound to a real coin, got " + rolled.coinItem());
        GTMaterial metal = PileBlockEntity.materialOf(rolled.coinItem());
        helper.assertTrue(metal != null && inPool(metal, GTDungeonData.COIN_METALS),
                "and its metal is one of GT6's dungeon coin list (WorldgenDungeonGT.java:195), got "
                        + (metal == null ? "none" : metal.getName()));

        // The same pile rolled twice is the same pile: the coin comes from a stream seeded by the world
        // and the pile's position, not from the dungeon's own stream, so GT6's draws are untouched.
        ItemStack first = rolled.coinItem();
        level.removeBlock(origin.offset(1, 1, 1), false);
        helper.assertTrue(data.coins(1, 1, 1), "the pile can be placed again at the same spot");
        CoinPileBlockEntity again = (CoinPileBlockEntity) server.getBlockEntity(origin.offset(1, 1, 1));
        helper.assertTrue(again != null && ItemStack.isSameItemSameTags(again.coinItem(), first),
                "the same pile position rolls the same metal, got " + again.coinItem() + " after " + first);

        // And the explicit helper a room uses when it knows the face.
        helper.assertTrue(data.coin(3, 1, 1, 4, 3), "an explicit coin pile is placed");
        CoinPileBlockEntity explicit = (CoinPileBlockEntity) server.getBlockEntity(origin.offset(3, 1, 1));
        helper.assertTrue(explicit != null && explicit.faceCount(4) == 3 && explicit.total() == 3,
                "it holds the three coins it was asked for");
        helper.assertTrue(CoinPileBlock.isCoin(explicit.coinItem()),
                "and it is bound to a real coin too, got " + explicit.coinItem());
        helper.assertTrue(server.getBlockState(origin.offset(3, 1, 1)).getValue(CoinPileBlock.COINS) == 1,
                "one face carries the three coins, state "
                        + server.getBlockState(origin.offset(3, 1, 1)));
        helper.succeed();
    }

    private static boolean inPool(GTMaterial material, String[] pool) {
        for (String name : pool) {
            GTMaterial candidate = GTMaterialRegistry.get(name);
            if (candidate.isValid() && candidate.resolve() == material.resolve()) return true;
        }
        return false;
    }

    /** The block state of the three material piles reports what GT6 draws: the stored stack size. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void pileStateShowsItsStack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        int index = 0;
        for (Block block : new Block[]{GTDecorBlocks.INGOT_PILE.get(), GTDecorBlocks.PLATE_PILE.get(),
                GTDecorBlocks.PLATE_GEM_PILE.get()}) {
            BlockPos pos = new BlockPos(BASE_X + 16 + index * 4, Y, BASE_Z);
            index++;
            level.setBlock(pos, block.defaultBlockState(), 2);
            PileBlockEntity pile = (PileBlockEntity) level.getBlockEntity(pos);
            if (pile == null) {
                problems.add(block + " has no block entity");
                continue;
            }
            ItemStack item = firstOf(pile.kind().prefix());
            if (item.isEmpty()) {
                problems.add("no item of the form " + pile.kind().prefix().getName());
                continue;
            }
            if (level.getBlockState(pos).getValue(PileBlock.STACK) != 0) {
                problems.add("an empty pile draws no item, state " + level.getBlockState(pos));
            }
            int moved = pile.add(item.copyWithCount(64));
            if (moved != 64) problems.add("the pile took " + moved + " of 64 " + item);
            if (level.getBlockState(pos).getValue(PileBlock.STACK) != 64) {
                problems.add("GT6 draws mSize boxes (MultiTileEntityIngot.java:48), state "
                        + level.getBlockState(pos));
            }
            pile.take(1);
            if (level.getBlockState(pos).getValue(PileBlock.STACK) != 63) {
                problems.add("a taken item shrinks the drawn pile, state " + level.getBlockState(pos));
            }
            level.removeBlock(pos, false);
        }
        helper.assertTrue(problems.isEmpty(), "pile states (" + problems.size() + "): "
                + problems.subList(0, Math.min(6, problems.size())));
        helper.succeed();
    }

    private static ItemStack firstOf(MaterialPrefix prefix) {
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            ItemStack stack = GTItems.getStack(prefix, material);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Every asset the four pile blocks reference exists: the block state, every model of every variant,
     * the texture of every face of those models, the item model of each block and both translations.
     *
     * <p>Walked the way {@code gametest/BumbleHiveTests} walks the hive: the block state JSON names the
     * models, the models name the textures, and each name is looked up as a resource.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void pileAssetsResolve(GameTestHelper helper) throws Exception {
        List<String> problems = new ArrayList<>();
        Set<String> models = new HashSet<>();
        for (Map.Entry<String, Integer> entry : PILE_BLOCKS.entrySet()) {
            String block = entry.getKey();
            JsonObject variants = resourceJson("assets/gregtech/blockstates/" + block + ".json")
                    .getAsJsonObject("variants");
            if (variants.size() != entry.getValue()) {
                problems.add(block + " has " + variants.size() + " variants, expected " + entry.getValue());
            }
            for (Map.Entry<String, JsonElement> variant : variants.entrySet()) {
                String model = variant.getValue().getAsJsonObject().get("model").getAsString();
                models.add(model);
                if (model.startsWith("gregtech:block/decoration/pile/")
                        && !resourceExists("assets/gregtech/models/" + model.substring("gregtech:".length())
                        + ".json")) {
                    problems.add(block + " " + variant.getKey() + " names the missing model " + model);
                }
            }
            // The block/item name of the block, in both languages.
            for (String language : new String[]{"en_us", "zh_cn"}) {
                if (!resourceJson("assets/gregtech/lang/" + language + ".json").has("block.gregtech." + block)) {
                    problems.add(language + " has no name for block.gregtech." + block);
                }
            }
            // The item model of the block points at one of those models.
            JsonObject item = resourceJson("assets/gregtech/models/item/" + block + ".json");
            String parent = item.get("parent").getAsString();
            if (!resourceExists("assets/gregtech/models/" + parent.substring("gregtech:".length()) + ".json")) {
                problems.add("the item model of " + block + " names the missing parent " + parent);
            }
            models.add(parent);
        }

        for (String model : models) {
            if (!model.startsWith("gregtech:")) continue;   // vanilla parents are not in this resource tree
            JsonObject json = resourceJson("assets/gregtech/models/" + model.substring("gregtech:".length())
                    + ".json");
            List<String> textures = new ArrayList<>();
            if (json.has("textures")) {
                for (Map.Entry<String, JsonElement> texture : json.getAsJsonObject("textures").entrySet()) {
                    textures.add(texture.getValue().getAsString());
                }
            }
            if (json.has("elements")) {
                for (JsonElement element : json.getAsJsonArray("elements")) {
                    JsonObject faces = element.getAsJsonObject().getAsJsonObject("faces");
                    for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
                        if (!face.getValue().getAsJsonObject().has("tintindex")) {
                            problems.add(model + " leaves " + face.getKey()
                                    + " untinted, so the pile would not show its material");
                        }
                        textures.add(face.getValue().getAsJsonObject().get("texture").getAsString());
                    }
                }
            }
            for (String texture : textures) {
                if (texture.startsWith("#") || !texture.startsWith("gregtech:")) continue;
                String path = texture.substring("gregtech:".length());
                if (!resourceExists("assets/gregtech/textures/" + path + ".png")) {
                    problems.add(model + " names the missing texture " + texture);
                }
            }
        }

        // GT6's own pile textures are the ones the models use.
        for (String[] texture : new String[][]{
                {"ingot", "top"}, {"ingot", "sides"}, {"plate", "top"}, {"plate", "sides"},
                {"plategem", "top"}, {"plategem", "sides"}}) {
            if (!resourceExists("assets/gregtech/textures/block/machines/placeables/"
                    + texture[0] + "/" + texture[1] + ".png")) {
                problems.add("GT6's " + texture[0] + "/" + texture[1] + " pile texture is missing");
            }
        }
        // GT6's coin face and rim, which the coin pile's model puts on its faces.
        for (String icon : new String[]{"coin", "coin_side"}) {
            if (!resourceExists("assets/gregtech/textures/block/iconsets/" + icon + ".png")) {
                problems.add("GT6's " + icon + " icon is missing");
            }
        }
        // The coin's own names, in both languages. One key per locale, because every coin is a material
        // item of the same prefix (`item.gregtech.coin` = "%s Coin"); `tab_icon_coin` is the creative tab
        // icon item GTCreativeTabIcons registers per prefix, whose name the suite's translation guard
        // (MaterialCompatibilityTests.registeredNamesHaveTranslations) reads like any other item's.
        for (String language : new String[]{"en_us", "zh_cn"}) {
            JsonObject lang = resourceJson("assets/gregtech/lang/" + language + ".json");
            for (String key : new String[]{"item.gregtech.coin", "itemGroup.gregtech.coin",
                    "item.gregtech.tab_icon_coin"}) {
                if (!lang.has(key)) problems.add(language + " has no " + key);
            }
        }
        helper.assertTrue(problems.isEmpty(), "pile assets (" + problems.size() + "): "
                + problems.subList(0, Math.min(8, problems.size())));
        helper.succeed();
    }
}
