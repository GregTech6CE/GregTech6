package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.tool.BumbliaryBlock;
import com.gregtech.gregtech.blockentity.tool.BumbliaryBlockEntity;
import com.gregtech.gregtech.client.gui.BumbliaryContainerMenu;
import com.gregtech.gregtech.content.bumble.BumbleBeeGenes;
import com.gregtech.gregtech.content.bumble.BumbleBeeType;
import com.gregtech.gregtech.content.bumble.GTBumbleProducts;
import com.gregtech.gregtech.content.bumble.GTBumbleSpecies;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTMenuTypes;
import com.gregtech.gregtech.registry.GTToolBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's bumbliary ({@code MultiTileEntityBumbliary}, 36 slots) and its advanced variant
 * ({@code MultiTileEntityBumbliaryAdvanced}, 20 slots) as machines: the block, the block entity, the
 * container, the slot rules, the tick logic, the automation capability and the NBT.
 *
 * <p>The tests never wait for game ticks - they build the block entity, {@code setLevel} it (so it
 * has a world to read the weather from) and call {@link BumbliaryBlockEntity#tickLogic()} in a loop,
 * which is GT6's {@code onTick2} body. The genomes they plant accept every environment GT6 could
 * check (temperature -1000..100000K, humidity 0..1, inside and outside, day and night, rainproof and
 * stormproof), so {@code checkEnvironment} and {@code checkWork} pass wherever the test structure
 * stands, and the work force is GT6's maximum 10000, which makes the working rolls deterministic.</p>
 *
 * <h2>The suite's own area ({@link #BASE_X}, {@link #BASE_Z})</h2>
 * <p>Everything this file places or drops lives in its own 512-block area at 60000/60000: every other
 * gametest suite keeps to a base of its own, two thousand blocks apart (20000 TreeGrowthTests, 24000
 * SurfaceFloraTests, 26000 TreeHoleTests, 28000 SapBagTests, 30000 BushTests, 31000 BumbleHiveTests,
 * 32000 PitTests, 34000 NetherScatterTests, 36000 LeafDecayTests, 38000 LootCrateTests, 40000
 * FluidSpringTests, 42000 CFoamTests, 44000 NetherQuartzTests, 46000 ColtanTests, 48000
 * BlackSandTurfTests, 50000 WaterBodyTests, 52000 NetherWorldgenTests, 54000 RockAndDeepOceanTests,
 * 56000 BookShelfTests, 58000 BedrockVeinTests), so 60000 is two thousand blocks clear of the highest
 * one, at every height. A leftover machine or a leftover drop from an earlier run therefore can never
 * reach another test, and {@link #resetSites} wipes this area before and after every test that writes
 * to the world, so the file is re-runnable against a world it already ran in.</p>
 *
 * <p>{@code helper.makeMockPlayer()} is a CREATIVE player, so every check that depends on survival
 * versus creative uses {@code FakePlayerFactory.getMinecraft} with {@code setGameMode} instead - and
 * asserts the mode it just set before it reads a slot rule.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BumbliaryTests {

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void scoopModeUnlocksBeesAndSynchronizes(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        for (boolean advanced : new boolean[]{false, true}) {
            var machine = machine(helper, advanced ? 1 : 0, advanced);
            var layout = machine.layout();
            var princess = bee(helper, SPECIES, BumbleBeeType.PRINCESS, genes(1200,1,100,2));
            var drone = bee(helper, SPECIES, BumbleBeeType.DRONE, genes(1200,1,100,2));
            var queen = bee(helper, SPECIES, BumbleBeeType.QUEEN, genes(1200,1,100,2));
            machine.inventory().setStackInSlot(layout.royal(), princess);
            machine.inventory().setStackInSlot(layout.drone(), drone);
            var normal = new BumbliaryContainerMenu(0, player.getInventory(), machine, false);
            var scoop = new BumbliaryContainerMenu(1, player.getInventory(), machine, true);
            helper.assertTrue(!normal.getSlot(layout.royal()).mayPickup(player)
                    && !normal.getSlot(layout.drone()).mayPickup(player), "normal access locks breeding bees");
            helper.assertTrue(scoop.getSlot(layout.royal()).mayPickup(player)
                    && scoop.getSlot(layout.drone()).mayPickup(player), "scoop access allows both breeding slots");
            helper.assertTrue(!scoop.quickMoveStack(player, layout.drone()).isEmpty()
                    && machine.inventory().getStackInSlot(layout.drone()).isEmpty(), "scoop shift-click actually extracts a drone");
            machine.inventory().setStackInSlot(layout.royal(), queen);
            helper.assertTrue(!scoop.getSlot(layout.royal()).mayPickup(player), "live queen remains locked even with scoop");
            var client = new BumbliaryContainerMenu(2, player.getInventory(), advanced);
            client.getSlot(layout.royal()).set(princess);
            helper.assertTrue(!client.getSlot(layout.royal()).mayPickup(player), "client begins with normal permissions");
            client.setData(0, 1);
            helper.assertTrue(client.scoopMode() && client.getSlot(layout.royal()).mayPickup(player), "menu data sync enables matching client permissions");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void ordinaryBumbliaryHasOriginalSurvivalRecipe(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("gregtech", "tool_blocks/bumbliary")).orElseThrow();
        helper.assertTrue(recipe instanceof com.gregtech.gregtech.recipe.ToolShapedRecipe, "bumbliary uses tool-aware crafting");
        var ingredients = recipe.getIngredients();
        helper.assertTrue(ingredients.size() == 9, "original 3x3 grid");
        for (int i=0;i<9;i++) helper.assertTrue(ingredients.get(i).getItems().length > 0, "resolved survival ingredient " + i);
        var iron = com.gregtech.gregtech.registry.GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.screw,
                com.gregtech.gregtech.content.material.Materials.Iron);
        var steel = com.gregtech.gregtech.registry.GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.screw,
                com.gregtech.gregtech.content.material.Materials.Steel);
        helper.assertTrue(ingredients.get(6).test(iron) && ingredients.get(6).test(steel), "ANY.Iron accepts iron and steel screws");
        var crafting = new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(Player player,int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        },3,3);
        for(int slot=0;slot<9;slot++) crafting.setItem(slot,ingredients.get(slot).getItems()[0].copy());
        var screwdriver = com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.SCREWDRIVER,
                com.gregtech.gregtech.content.material.Materials.Steel, com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        crafting.setItem(7,screwdriver);
        var shaped = (com.gregtech.gregtech.recipe.ToolShapedRecipe)recipe;
        helper.assertTrue(shaped.matches(crafting,helper.getLevel()), "real survival ingredients match original grid");
        helper.assertTrue(!shaped.getRemainingItems(crafting).get(7).isEmpty()
                && shaped.getRemainingItems(crafting).get(7).getDamageValue() > screwdriver.getDamageValue(), "craft returns the worn screwdriver");
        helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess()).is(GTToolBlocks.BUMBLIARY.get().asItem()), "recipe produces the functional bumbliary");
        helper.succeed();
    }

    /** The suite's own base: see the class javadoc for the ranges every other suite owns. */
    private static final int BASE_X = 60000;
    private static final int BASE_Z = 60000;
    private static final int BASE_Y = 200;

    /** How many 8-block sites the suite may use - {@link #resetSites} clears every one of them. */
    private static final int SITES = 8;

    /** The site the registration test places a machine on, and the two the break test uses. */
    private static final int SITE_IDLE = 5, SITE_PLACED = 6, SITE_RUNNING = 7;

    /** GT6's cultivated bumblebee (id 30): a level 3 species, so every product roll succeeds. */
    private static final int SPECIES = 30;

    /** A genome that accepts every place and can always work ({@code MultiItemBumbles:130-153}). */
    private static CompoundTag genes(long life, long work, long aggro, long offspring) {
        CompoundTag genes = new CompoundTag();
        BumbleBeeGenes.setHumidityMin(genes, 0F);
        BumbleBeeGenes.setHumidityMax(genes, 1F);
        BumbleBeeGenes.setTemperatureMin(genes, -1000L);
        BumbleBeeGenes.setTemperatureMax(genes, 100000L);
        BumbleBeeGenes.setLifeSpan(genes, life);
        BumbleBeeGenes.setWorkForce(genes, work);
        BumbleBeeGenes.setAggressiveness(genes, aggro);
        BumbleBeeGenes.setOffspring(genes, offspring);
        BumbleBeeGenes.setDayActive(genes, true);
        BumbleBeeGenes.setNightActive(genes, true);
        BumbleBeeGenes.setOutsideActive(genes, true);
        BumbleBeeGenes.setInsideActive(genes, true);
        BumbleBeeGenes.setRainproof(genes, true);
        BumbleBeeGenes.setStormproof(genes, true);
        return genes;
    }

    private static ItemStack bee(GameTestHelper helper, int speciesId, BumbleBeeType type, CompoundTag genes) {
        GTBumbleSpecies.Species species = GTBumbleSpecies.byId(speciesId);
        helper.assertTrue(species != null, "the port has GT6's species " + speciesId);
        ItemStack stack = BumbleBeeType.stack(species, type, genes, 1);
        helper.assertTrue(!stack.isEmpty(), "the bee " + speciesId + " " + type.suffix() + " is registered");
        return stack;
    }

    private static int speciesId(ItemStack stack) {
        GTBumbleSpecies.Species species = BumbleBeeType.speciesOf(stack);
        return species == null ? -1 : species.id();
    }

    /**
     * A detached machine: no ticker runs, so the test decides when a tick happens. The block entity
     * still knows its world, which is what the weather and biome checks read.
     */
    private static BumbliaryBlockEntity machine(GameTestHelper helper, int index, boolean advanced) {
        Block block = advanced ? GTToolBlocks.ADVANCED_BUMBLIARY.get() : GTToolBlocks.BUMBLIARY.get();
        BlockState state = block.defaultBlockState();
        BumbliaryBlockEntity machine = new BumbliaryBlockEntity(site(index), state);
        machine.setLevel(helper.getLevel());
        return machine;
    }

    /** One of the suite's own sites, 8 blocks apart inside the reserved area (class javadoc). */
    private static BlockPos site(int index) {
        return new BlockPos(BASE_X + index * 8, BASE_Y, BASE_Z);
    }

    /** The reserved area plus the room a drop needs - the only box this file ever writes in. */
    private static AABB siteBounds() {
        return new AABB(BASE_X - 4, BASE_Y - 4, BASE_Z - 4,
                BASE_X + (SITES - 1) * 8 + 4, BASE_Y + 4, BASE_Z + 4);
    }

    /** Whether a block of this suite's machine stands on a position ({@code null}-safe). */
    private static boolean isMachine(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof BumbliaryBlock;
    }

    /**
     * Makes the suite re-runnable in a world that already ran it: every site is emptied, every machine
     * of this suite is removed - which hands its contents out, exactly like GT6's break does - and
     * everything the suite can have left behind, blocks and items alike, is wiped from its own area.
     * Only {@link #siteBounds()} is ever touched, so no other test's work can be destroyed.
     */
    private static void resetSites(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int index = 0; index < SITES; index++) {
            BlockPos pos = site(index);
            if (level.getBlockEntity(pos) instanceof BumbliaryBlockEntity machine) machine.breakDrops();
            if (isMachine(level, pos)) level.removeBlock(pos, false);
        }
        AABB bounds = siteBounds();
        for (int x = (int) bounds.minX; x <= (int) bounds.maxX; x++) {
            for (int y = (int) bounds.minY; y <= (int) bounds.maxY; y++) {
                for (int z = (int) bounds.minZ; z <= (int) bounds.maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isMachine(level, pos)) level.removeBlock(pos, false);
                }
            }
        }
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, bounds)) entity.discard();
    }

    /** How many bees a stack holds - zero for anything that is not a bee. */
    private static int beeCount(ItemStack stack) {
        return BumbleBeeType.of(stack) == null ? 0 : stack.getCount();
    }

    /** The number of bees of the whole drone group, GT6's {@code SLOT_DRONE} plus {@code SLOTS_DRONE}. */
    private static int beesInDroneSlots(BumbliaryBlockEntity machine) {
        int found = beeCount(machine.inventory().getStackInSlot(BumbliaryBlockEntity.SLOT_DRONE));
        for (int slot : BumbliaryBlockEntity.SLOTS_DRONE) {
            found += beeCount(machine.inventory().getStackInSlot(slot));
        }
        return found;
    }

    /** The number of bees in GT6's spare drone slots only. */
    private static int beesInSpareSlots(BumbliaryBlockEntity machine) {
        int found = 0;
        for (int slot : BumbliaryBlockEntity.SLOTS_DRONE) {
            found += beeCount(machine.inventory().getStackInSlot(slot));
        }
        return found;
    }

    /** The bees of one slot group, for the "nothing outside the group changed" checks. */
    private static List<ItemStack> contents(BumbliaryBlockEntity machine, int[] slots) {
        List<ItemStack> found = new ArrayList<>();
        for (int slot : slots) {
            ItemStack stack = machine.inventory().getStackInSlot(slot);
            if (!stack.isEmpty()) found.add(stack);
        }
        return found;
    }

    /** Drives {@link BumbliaryBlockEntity#tickLogic()} until the queen dies, or the cap is hit. */
    private static int ticksUntilQueenDeath(BumbliaryBlockEntity machine) {
        int ticks = 0;
        while (machine.life() > 0 && ticks < 20000) {
            machine.tickLogic();
            ticks++;
        }
        return ticks;
    }

    /** The block, the block entity type, the menu type, the slot groups and the GUI assets. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void bumbliaryIsRegistered(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        for (String id : new String[]{"bumbliary", "advanced_bumbliary"}) {
            Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", id));
            if (block == null || block.asItem() == Items.AIR) {
                problems.add("the block " + id + " has no item");
                continue;
            }
            if (!GTBlockEntities.BUMBLIARY.get().isValid(block.defaultBlockState())) {
                problems.add("the bumbliary block entity type must cover " + id);
            }
            for (String language : new String[]{"en_us", "zh_cn"}) {
                if (!lang(language).contains("\"block.gregtech." + id + "\"")) {
                    problems.add(language + " has no name for block.gregtech." + id);
                }
            }
        }
        if (GTMenuTypes.BUMBLIARY == null || !GTMenuTypes.BUMBLIARY.isPresent()) {
            problems.add("the bumbliary menu type is missing");
        }
        if (GTMenuTypes.ADVANCED_BUMBLIARY == null || !GTMenuTypes.ADVANCED_BUMBLIARY.isPresent()) {
            problems.add("the advanced bumbliary menu type is missing");
        }
        // GT6's GUI sheets, lower case like every port texture.
        for (String texture : new String[]{"bumbliary.png", "bumbliary_advanced.png"}) {
            if (BumbliaryTests.class.getClassLoader()
                    .getResource("assets/gregtech/textures/gui/machines/" + texture) == null) {
                problems.add("missing GUI texture " + texture);
            }
        }
        // Both blocks need their assets: blockstate, block model, item model and GT6's six faces.
        for (String asset : new String[]{
                "assets/gregtech/blockstates/bumbliary.json",
                "assets/gregtech/blockstates/advanced_bumbliary.json",
                "assets/gregtech/models/block/tool/bumbliary.json",
                "assets/gregtech/models/block/tool/advanced_bumbliary.json",
                "assets/gregtech/models/item/bumbliary.json",
                "assets/gregtech/models/item/advanced_bumbliary.json"}) {
            if (BumbliaryTests.class.getClassLoader().getResource(asset) == null) {
                problems.add("missing " + asset);
            }
        }
        for (String machine : new String[]{"bumbliary", "bumbliary_adv"}) {
            for (String face : new String[]{"colored/bottom", "colored/top", "colored/sides",
                    "overlay/bottom", "overlay/top", "overlay/sides"}) {
                String texture = "assets/gregtech/textures/block/machines/tools/" + machine + "/" + face + ".png";
                if (BumbliaryTests.class.getClassLoader().getResource(texture) == null) {
                    problems.add("missing " + texture);
                }
            }
        }
        // GT6's own group indices (MultiTileEntityBumbliary:333-337).
        if (BumbliaryBlockEntity.SLOT_ROYAL != 13 || BumbliaryBlockEntity.SLOT_DRONE != 22) {
            problems.add("GT6's royal and drone slots are 13 and 22");
        }
        if (BumbliaryBlockEntity.LAYOUT.slots() != 36) problems.add("the bumbliary has 36 slots");
        if (BumbliaryBlockEntity.LAYOUT.combs().length != 18) problems.add("18 comb slots expected");
        if (BumbliaryBlockEntity.LAYOUT.drones().length != 7) problems.add("7 spare drone slots expected");
        if (BumbliaryBlockEntity.LAYOUT.dead().length != 9) problems.add("9 dead slots expected");
        if (BumbliaryBlockEntity.ADVANCED_LAYOUT.slots() != 20) problems.add("the advanced one has 20 slots");

        // A placed machine really does have its block entity. The suite's own area is wiped first, so
        // the check also holds in a world that already ran this file.
        resetSites(helper);
        BlockPos placedPos = site(SITE_PLACED);
        helper.getLevel().setBlock(placedPos, GTToolBlocks.BUMBLIARY.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(placedPos) instanceof BumbliaryBlockEntity placed)) {
            problems.add("the placed bumbliary has no block entity");
        } else if (placed.inventory().getSlots() != 36) {
            problems.add("the placed bumbliary has " + placed.inventory().getSlots() + " slots");
        }
        resetSites(helper);     // leave nothing behind for the next run

        helper.assertTrue(problems.isEmpty(), "bumbliary registration: " + problems);
        helper.succeed();
    }

    private static String lang(String language) {
        try (java.io.InputStream stream = BumbliaryTests.class.getClassLoader()
                .getResourceAsStream("assets/gregtech/lang/" + language + ".json")) {
            if (stream == null) return "";
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) {
            return "";
        }
    }

    /** GT6's breeding step ({@code :198-266}) and the lifespan the crowned queen gets ({@code :254}). */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void princessAndDroneEventuallyBreed(GameTestHelper helper) {
        BumbliaryBlockEntity machine = machine(helper, 0, false);
        // The princess lives 1200 ticks, the drone 5000: the queen must inherit the princess's gene.
        CompoundTag princessGenes = genes(1200, 1, 100, 3);
        ItemStack princess = bee(helper, SPECIES, BumbleBeeType.PRINCESS, princessGenes);
        ItemStack drone = bee(helper, SPECIES, BumbleBeeType.DRONE, genes(5000, 1, 100, 3));
        machine.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_ROYAL, princess);
        machine.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_DRONE, drone);

        int ticks = 0;
        while (machine.life() == 0 && ticks < 3000) {
            machine.tickLogic();
            ticks++;
        }
        helper.assertTrue(machine.life() > 0, "the princess and the drone breed within the countdown");
        helper.assertTrue(ticks == BumbliaryBlockEntity.COUNTDOWN,
                "GT6's countdown is 1200 ticks (:198), bred after " + ticks);

        ItemStack royal = machine.inventory().getStackInSlot(BumbliaryBlockEntity.SLOT_ROYAL);
        helper.assertTrue(BumbleBeeType.of(royal) == BumbleBeeType.QUEEN,
                "GT6 crowns the princess into a queen (:261), got " + BumbleBeeType.of(royal));
        helper.assertTrue(speciesId(royal) == SPECIES, "the queen keeps the princess's species");
        CompoundTag royalGenes = BumbleBeeGenes.peek(royal);
        helper.assertTrue(royalGenes != null && BumbleBeeGenes.lifeSpan(royalGenes) == 1200
                        && BumbleBeeGenes.workForce(royalGenes) == 1,
                "the queen keeps the princess's genome, not the drone's");
        helper.assertTrue(machine.life() == 1200 || machine.life() == 1199,
                "the queen's life is the princess's lifespan gene, got " + machine.life());

        helper.assertTrue(machine.inventory().getStackInSlot(BumbliaryBlockEntity.SLOT_DRONE).isEmpty(),
                "a single drone is consumed by the breeding (:256-259)");
        List<ItemStack> dead = contents(machine, BumbliaryBlockEntity.SLOTS_DEAD);
        helper.assertTrue(dead.size() == 1 && BumbleBeeType.of(dead.get(0)) == BumbleBeeType.DEAD
                        && speciesId(dead.get(0)) == SPECIES,
                "the consumed drone dies as a dead bee of its species (:257), got " + dead);

        ItemStack[] brood = machine.brood();
        int princessCount = 0;
        for (ItemStack child : brood) if (BumbleBeeType.of(child) == BumbleBeeType.PRINCESS) princessCount++;
        helper.assertTrue(brood.length >= 1 + 3 && brood.length <= 3 + 3,
                "the brood is the offspring gene plus 1..3 princesses (:218-220), got " + brood.length);
        for (int i = 0; i < brood.length; i++) {
            BumbleBeeType type = BumbleBeeType.of(brood[i]);
            helper.assertTrue(type == (i < princessCount ? BumbleBeeType.PRINCESS : BumbleBeeType.DRONE),
                    "the first princessCount children are princesses (:234-247), got " + type + " at " + i);
            int id = speciesId(brood[i]);
            helper.assertTrue(id == SPECIES || id == SPECIES - 10,
                    "a level 3 species only mutates down, got " + id);
            helper.assertTrue(BumbleBeeGenes.peek(brood[i]) != null, "every child inherits a genome (:252)");
        }
        helper.assertTrue(princessCount >= 1, "GT6 always breeds at least one princess (:218)");
        helper.succeed();
    }

    /** GT6's queen death ({@code :121-163}): the brood is handed out and a princess is promoted. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void queenDeathHandsOutTheBroodAndPromotesAPrincess(GameTestHelper helper) {
        // ── the main drone slot is free, so GT6's brood starts there (:137-138) ──────────────────
        BumbliaryBlockEntity machine = machine(helper, 0, false);
        machine.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_ROYAL,
                bee(helper, SPECIES, BumbleBeeType.PRINCESS, genes(1200, 10000, 100, 3)));
        machine.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_DRONE,
                bee(helper, SPECIES, BumbleBeeType.DRONE, genes(1200, 10000, 100, 3)));
        int bred = 0;
        while (machine.life() == 0 && bred < 3000) {
            machine.tickLogic();
            bred++;
        }
        helper.assertTrue(machine.life() > 0, "the pair breeds");
        ItemStack[] brood = machine.brood();
        helper.assertTrue(brood.length >= 4, "the brood has children, got " + brood.length);

        ticksUntilQueenDeath(machine);
        helper.assertTrue(machine.life() == 0, "the queen dies when its life runs out (:121)");
        helper.assertTrue(machine.endedQueen(), "GT6 flags the death for the one tick it happens on (:122)");
        helper.assertTrue(machine.brood().length == 0, "the brood is handed out and cleared (:145)");
        // The whole brood is handed out: the children go to the drone group (:135-138) and GT6 then
        // moves the most aggressive spare princess into the royal slot (:160-163), so that one leaves
        // the group again. Both cases are counted, whether a promotion happened or not.
        ItemStack royal = machine.inventory().getStackInSlot(BumbliaryBlockEntity.SLOT_ROYAL);
        helper.assertTrue(beesInDroneSlots(machine) + beeCount(royal) == brood.length,
                "every child that fits the place is handed out (:135-138, :160-163), got "
                        + beesInDroneSlots(machine) + " in the drone group plus " + beeCount(royal)
                        + " in the royal slot of " + brood.length);
        helper.assertTrue(!machine.inventory().getStackInSlot(BumbliaryBlockEntity.SLOT_DRONE).isEmpty(),
                "the main drone slot is filled first (:137)");
        List<ItemStack> dead = contents(machine, BumbliaryBlockEntity.SLOTS_DEAD);
        helper.assertTrue(dead.stream().anyMatch(stack -> BumbleBeeType.of(stack) == BumbleBeeType.DEAD),
                "the dead queen lies in a dead slot (:126-128)");

        // GT6 only promotes a princess out of the spare drone slots (:147-163); whether one of the
        // children that went there is a princess depends on GT6's princess count, so the check follows
        // the original: whenever the spare slots hold a princess, the royal slot has one.
        List<Integer> sparePrincesses = new ArrayList<>();
        for (int slot : BumbliaryBlockEntity.SLOTS_DRONE) {
            ItemStack stack = machine.inventory().getStackInSlot(slot);
            if (BumbleBeeType.of(stack) == BumbleBeeType.PRINCESS) sparePrincesses.add(speciesId(stack));
        }
        if (sparePrincesses.isEmpty()) {
            helper.assertTrue(royal.isEmpty(), "without a princess in the spare slots nothing is promoted");
        } else {
            helper.assertTrue(BumbleBeeType.of(royal) == BumbleBeeType.PRINCESS,
                    "the most aggressive princess of the spare slots becomes the queen (:160-163)");
            helper.assertTrue(sparePrincesses.contains(speciesId(royal)),
                    "the promoted princess comes from the brood");
        }

        // ── a blocked main drone slot forces the whole brood into the spare slots ────────────────
        BumbliaryBlockEntity second = machine(helper, 1, false);
        second.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_ROYAL,
                bee(helper, SPECIES, BumbleBeeType.PRINCESS, genes(1200, 10000, 100, 3)));
        second.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_DRONE,
                bee(helper, SPECIES, BumbleBeeType.DRONE, genes(1200, 10000, 100, 3)));
        int secondBred = 0;
        while (second.life() == 0 && secondBred < 3000) {
            second.tickLogic();
            secondBred++;
        }
        helper.assertTrue(second.life() > 0, "the second pair breeds too");
        int secondBrood = second.brood().length;
        // GT6 leaves the main drone slot to the bees it holds; an item the brood cannot merge with
        // sends every child into the spare slots (:137-138).
        second.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_DRONE, new ItemStack(Items.DIRT));
        ticksUntilQueenDeath(second);
        // Every child went into a spare slot, so at least one spare princess exists and GT6 promotes
        // it into the royal slot (:147-163) - that child therefore leaves the drone group again.
        ItemStack promoted = second.inventory().getStackInSlot(BumbliaryBlockEntity.SLOT_ROYAL);
        helper.assertTrue(BumbleBeeType.of(promoted) == BumbleBeeType.PRINCESS,
                "one of the brood's princesses is promoted, got " + BumbleBeeType.of(promoted));
        helper.assertTrue(speciesId(promoted) == SPECIES || speciesId(promoted) == SPECIES - 10,
                "the promoted princess is one of the brood's, got " + speciesId(promoted));
        helper.assertTrue(beesInSpareSlots(second) == secondBrood - 1,
                "the whole brood minus the promoted princess sits in the spare slots (:138), got "
                        + beesInSpareSlots(second) + " of " + (secondBrood - 1));
        helper.assertTrue(beesInDroneSlots(second) + beeCount(promoted) == secondBrood,
                "the brood is accounted for by the drone group plus the promoted princess (:160-163), got "
                        + beesInDroneSlots(second) + " plus " + beeCount(promoted) + " of " + secondBrood);
        helper.assertTrue(beesInDroneSlots(second) == secondBrood - 1,
                "exactly the promoted child left the drone group, got " + beesInDroneSlots(second));
        helper.succeed();
    }

    /** GT6's comb production ({@code :168-186}) plus the product tables of {@code MultiItemBumbles}. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void workingQueenProducesCombProducts(GameTestHelper helper) {
        // GT6's product tables (:481-494, :189-213).
        helper.assertTrue(GTBumbleProducts.count() == 1, "a species has exactly one product (:492-494)");
        helper.assertTrue(GTBumbleProducts.chance(0) == 2500 && GTBumbleProducts.chance(10) == 5000
                        && GTBumbleProducts.chance(20) == 7500 && GTBumbleProducts.chance(30) == 10000,
                "the species level sets the chance: 2500, 5000, 7500, 10000 (:481-489)");
        for (GTBumbleSpecies.Species species : GTBumbleSpecies.SPECIES) {
            ItemStack product = GTBumbleProducts.stack(species.id(), 1);
            helper.assertTrue(!product.isEmpty() && product.getCount() == 1,
                    "species " + species.id() + " produces its comb");
            helper.assertTrue(ForgeRegistries.ITEMS.getKey(product.getItem()).getPath().equals(species.comb()),
                    "the product is " + species.comb() + ", got " + product);
        }

        BumbliaryBlockEntity machine = machine(helper, 0, false);
        BlockPos flowerPos = machine.getBlockPos().east();
        BlockPos soilPos = flowerPos.below();
        // The queen is a flower species; without this workplace GT6 correctly makes no comb.
        helper.getLevel().setBlock(soilPos, Blocks.DIRT.defaultBlockState(), 3);
        helper.getLevel().setBlock(flowerPos, Blocks.POPPY.defaultBlockState(), 3);
        try {
        // GT6's breeding first, so the royal slot holds a queen that carries the princess's genes.
        machine.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_ROYAL,
                bee(helper, SPECIES, BumbleBeeType.PRINCESS, genes(1200, 10000, 100, 1)));
        machine.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_DRONE,
                bee(helper, SPECIES, BumbleBeeType.DRONE, genes(1200, 10000, 100, 1)));
        int bred = 0;
        while (machine.life() == 0 && bred < 3000) {
            machine.tickLogic();
            bred++;
        }
        helper.assertTrue(machine.life() == 1200, "the queen starts with the princess's 1200 life");

        for (int tick = 0; tick < 599; tick++) machine.tickLogic();
        helper.assertTrue(contents(machine, BumbliaryBlockEntity.SLOTS_COMBS).isEmpty(),
                "GT6 only lets a queen work on `life % 1200 == 600` (:168)");
        helper.assertTrue(machine.life() == 601, "the queen counts its life down, got " + machine.life());

        machine.tickLogic();
        helper.assertTrue(machine.life() == 600, "the working tick is the 600th, got " + machine.life());
        List<ItemStack> combs = contents(machine, BumbliaryBlockEntity.SLOTS_COMBS);
        helper.assertTrue(combs.size() == 1 && combs.get(0).getCount() == 1,
                "one comb per success (:170-181), got " + combs);
        helper.assertTrue(ForgeRegistries.ITEMS.getKey(combs.get(0).getItem()).getPath()
                        .equals(GTBumbleSpecies.byId(SPECIES).comb()),
                "the comb is the species' own, got " + combs.get(0));
        helper.assertTrue(contents(machine, BumbliaryBlockEntity.SLOTS_DRONE).isEmpty(),
                "the spare drone slots stay untouched by the products (:174-181)");
        helper.assertTrue(machine.inventory().getStackInSlot(BumbliaryBlockEntity.SLOT_DRONE).isEmpty(),
                "the main drone slot stays untouched by the products");
        helper.succeed();
        } finally {
            helper.getLevel().setBlock(flowerPos, Blocks.AIR.defaultBlockState(), 3);
            helper.getLevel().setBlock(soilPos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** GT6's automation access ({@code getAccessibleSlotsFromSide2:340-343}). */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void onlyTheDeadSlotsAreExposedToAutomation(GameTestHelper helper) {
        BumbliaryBlockEntity machine = machine(helper, 0, false);
        IItemHandler handler = machine.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                .resolve().orElse(null);
        helper.assertTrue(handler != null, "the bumbliary exposes an item handler");
        helper.assertTrue(handler.getSlots() == BumbliaryBlockEntity.SLOTS_DEAD.length,
                "automation sees GT6's nine dead slots, got " + handler.getSlots());

        // Every exposed index has to be a dead slot, and nothing else may be reachable.
        for (int index = 0; index < handler.getSlots(); index++) {
            machine.inventory().setStackInSlot(index, ItemStack.EMPTY);
        }
        for (int index = 0; index < BumbliaryBlockEntity.SLOTS_DEAD.length; index++) {
            int slot = BumbliaryBlockEntity.SLOTS_DEAD[index];
            machine.inventory().setStackInSlot(slot, new ItemStack(Items.DIRT, index + 1));
        }
        for (int index = 0; index < handler.getSlots(); index++) {
            helper.assertTrue(handler.getStackInSlot(index).is(Items.DIRT)
                            && handler.getStackInSlot(index).getCount() == index + 1,
                    "exposed index " + index + " is dead slot " + BumbliaryBlockEntity.SLOTS_DEAD[index]);
        }
        // Nothing may be inserted anywhere (GT6 canInsertItem2:342).
        helper.assertTrue(!handler.isItemValid(0, new ItemStack(Items.DIRT)),
                "GT6 refuses every insertion into the machine");
        ItemStack leftover = handler.insertItem(0, new ItemStack(Items.STONE, 4), false);
        helper.assertTrue(leftover.getCount() == 4, "an insertion comes straight back");
        // Extraction takes the dead bee out (GT6 canExtractItem2:343, `aSlot >= 27`).
        ItemStack extracted = handler.extractItem(0, 1, false);
        helper.assertTrue(extracted.is(Items.DIRT) && extracted.getCount() == 1,
                "the dead slots can be emptied, got " + extracted);
        helper.assertTrue(machine.inventory().getStackInSlot(BumbliaryBlockEntity.SLOTS_DEAD[0]).getCount() == 0,
                "the extraction really took the item out of the machine");

        // The advanced machine exposes its combs and its dead slots instead (:341-344).
        BumbliaryBlockEntity advanced = machine(helper, 1, true);
        IItemHandler advancedHandler = advanced.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                .resolve().orElse(null);
        helper.assertTrue(advancedHandler != null
                        && advancedHandler.getSlots() == BumbliaryBlockEntity.ADV_SLOTS_AUTO.length,
                "the advanced machine exposes GT6's twelve slots, got "
                        + (advancedHandler == null ? "none" : advancedHandler.getSlots()));
        helper.succeed();
    }

    /** GT6's GUI slot rules and slot grid ({@code :357-369, :395-434}). */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void guiSlotRulesFollowGt6(GameTestHelper helper) {
        BumbliaryBlockEntity machine = machine(helper, 0, false);
        // helper.makeMockPlayer() is creative, so the two roles below come from a fake player whose game
        // mode is set explicitly - otherwise GT6's setCanTake(F) slots would look open to a "survival"
        // player that is not one.
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        BumbliaryContainerMenu menu = new BumbliaryContainerMenu(0, player.getInventory(), machine);

        helper.assertTrue(menu.slots.size() == 36 + 36,
                "36 machine slots plus the player's 36, got " + menu.slots.size());
        // GT6's grid: 9 columns from x=8, rows at y=8, 26, 44 and 62 (ContainerCommon:250-287).
        int[][] expected = {{8, 8}, {80, 26}, {80, 44}, {152, 62}};
        int[] indices = {0, BumbliaryBlockEntity.SLOT_ROYAL, BumbliaryBlockEntity.SLOT_DRONE, 35};
        for (int i = 0; i < indices.length; i++) {
            Slot slot = menu.getSlot(indices[i]);
            helper.assertTrue(slot.x == expected[i][0] && slot.y == expected[i][1],
                    "slot " + indices[i] + " sits at " + expected[i][0] + "," + expected[i][1]
                            + ", got " + slot.x + "," + slot.y);
        }
        helper.assertTrue(menu.getSlot(36).x == 8 && menu.getSlot(36).y == BumbliaryContainerMenu.PLAYER_INVENTORY_Y,
                "GT6 puts the player inventory at y=84 (ContainerCommon:289)");
        // The menu holds 36 machine slots + 27 inventory slots + 9 hotbar slots = 72, so the hotbar
        // starts at index 63 (GT6 binds the hotbar last, ContainerCommon:327-334).
        helper.assertTrue(menu.getSlot(63).x == 8
                        && menu.getSlot(63).y == BumbliaryContainerMenu.PLAYER_INVENTORY_Y + 58,
                "and the hotbar's first slot 63 at y=142 of " + menu.slots.size());
        helper.assertTrue(menu.getSlot(menu.slots.size() - 1).y == BumbliaryContainerMenu.PLAYER_INVENTORY_Y + 58,
                "the last slot is the hotbar's ninth");

        ItemStack princess = bee(helper, SPECIES, BumbleBeeType.PRINCESS, genes(1200, 1, 100, 2));
        ItemStack drone = bee(helper, SPECIES, BumbleBeeType.DRONE, genes(1200, 1, 100, 2));
        ItemStack queen = bee(helper, SPECIES, BumbleBeeType.QUEEN, genes(1200, 1, 100, 2));
        ItemStack dirt = new ItemStack(Items.DIRT);

        // ── the standard machine, whole table, slot group by slot group ────────────────────────
        assertPlaceTable(helper, menu, BumbliaryBlockEntity.LAYOUT, princess, drone, queen, dirt);
        assertRoyalSlotSize(helper, menu, BumbliaryBlockEntity.LAYOUT);
        // Survival: combs, dead bees and the royal bee may leave; both bee slots' drones areas may not.
        player.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(!player.isCreative(), "the fake player runs in survival for the first table");
        assertTakeTable(helper, menu, BumbliaryBlockEntity.LAYOUT, player, false);
        assertRoyalTake(helper, menu, machine, BumbliaryBlockEntity.LAYOUT, princess, queen, player, false);
        // Creative: every slot opens (GT6 Slot_Base.canTakeStack:92), except a live queen.
        player.setGameMode(GameType.CREATIVE);
        helper.assertTrue(player.isCreative(), "and in creative for the second table");
        assertTakeTable(helper, menu, BumbliaryBlockEntity.LAYOUT, player, true);
        assertRoyalTake(helper, menu, machine, BumbliaryBlockEntity.LAYOUT, princess, queen, player, true);

        // The client copy of the menu has no machine and answers from the synced stack - same rule.
        helper.assertTrue(BumbliaryBlockEntity.canLeaveRoyalSlot(princess)
                        && !BumbliaryBlockEntity.canLeaveRoyalSlot(queen),
                "the stack rule the client menu uses agrees with the machine (GT6 :368)");

        // ── the advanced machine uses GT6's own 5x4 grid and its own flags (:397-419) ──────────
        BumbliaryBlockEntity advancedMachine = machine(helper, 1, true);
        BumbliaryContainerMenu advanced = new BumbliaryContainerMenu(0, player.getInventory(), advancedMachine);
        helper.assertTrue(advanced.slots.size() == 20 + 36, "20 machine slots on the advanced machine");
        helper.assertTrue(advanced.getSlot(0).x == 44 && advanced.getSlot(0).y == 8,
                "the advanced grid starts at x=44 (GT6 :397)");
        Slot advancedRoyal = advanced.getSlot(BumbliaryBlockEntity.ADV_SLOT_ROYAL);
        helper.assertTrue(advancedRoyal.x == 80 && advancedRoyal.y == 26,
                "the advanced royal slot sits at 80,26 (GT6 :405)");
        helper.assertTrue(advanced.advanced(), "the menu knows which machine it drives");
        assertPlaceTable(helper, advanced, BumbliaryBlockEntity.ADVANCED_LAYOUT, princess, drone, queen, dirt);
        assertRoyalSlotSize(helper, advanced, BumbliaryBlockEntity.ADVANCED_LAYOUT);
        player.setGameMode(GameType.SURVIVAL);
        assertTakeTable(helper, advanced, BumbliaryBlockEntity.ADVANCED_LAYOUT, player, false);
        assertRoyalTake(helper, advanced, advancedMachine, BumbliaryBlockEntity.ADVANCED_LAYOUT,
                princess, queen, player, false);
        player.setGameMode(GameType.CREATIVE);
        assertTakeTable(helper, advanced, BumbliaryBlockEntity.ADVANCED_LAYOUT, player, true);
        assertRoyalTake(helper, advanced, advancedMachine, BumbliaryBlockEntity.ADVANCED_LAYOUT,
                princess, queen, player, true);
        helper.succeed();
    }

    /** Whether a slot list holds an index. */
    private static boolean holds(int[] slots, int index) {
        for (int slot : slots) if (slot == index) return true;
        return false;
    }

    /**
     * GT6's insertion table, slot by slot ({@code MultiTileEntityBumbliary:396-434}): the royal slot
     * takes a princess ({@code isItemValidForSlotGUI:361}), the main drone slot a drone ({@code :360}),
     * and every other slot is output only ({@code setCanPut(F)}, {@code :362}).
     */
    private static void assertPlaceTable(GameTestHelper helper, BumbliaryContainerMenu menu,
                                         BumbliaryBlockEntity.Layout layout, ItemStack princess,
                                         ItemStack drone, ItemStack queen, ItemStack dirt) {
        for (int slot = 0; slot < layout.slots(); slot++) {
            Slot entry = menu.getSlot(slot);
            if (slot == layout.royal()) {
                helper.assertTrue(entry.mayPlace(princess) && !entry.mayPlace(queen)
                                && !entry.mayPlace(drone) && !entry.mayPlace(dirt),
                        "the royal slot " + slot + " takes princesses only (GT6 :361)");
            } else if (slot == layout.drone()) {
                helper.assertTrue(entry.mayPlace(drone) && !entry.mayPlace(princess) && !entry.mayPlace(dirt),
                        "the main drone slot " + slot + " takes drones only (GT6 :360)");
            } else {
                helper.assertTrue(!entry.mayPlace(dirt) && !entry.mayPlace(drone) && !entry.mayPlace(princess),
                        "slot " + slot + " is output only (GT6 setCanPut(F))");
            }
        }
    }

    /**
     * GT6's take table, slot by slot: a survival player empties the comb slots, the dead slots and - by
     * {@code canTakeOutOfSlotGUI:367-369}, checked separately - the royal slot; the main drone slot
     * ({@code :420}) and the spare drone slots ({@code :399-421}) are closed to them. A creative player
     * empties every slot ({@code Slot_Base.canTakeStack:92}); the royal slot is never part of this sweep,
     * because its answer depends on the bee inside it.
     */
    private static void assertTakeTable(GameTestHelper helper, BumbliaryContainerMenu menu,
                                        BumbliaryBlockEntity.Layout layout, Player player, boolean creative) {
        String role = creative ? "a creative player" : "a survival player";
        for (int slot = 0; slot < layout.slots(); slot++) {
            if (slot == layout.royal()) continue;
            boolean beeSlot = slot == layout.drone() || holds(layout.drones(), slot);
            boolean expected = creative || !beeSlot;
            helper.assertTrue(menu.getSlot(slot).mayPickup(player) == expected,
                    role + " must " + (expected ? "be able to take" : "not be able to take")
                            + " from slot " + slot + (beeSlot ? " (GT6 setCanTake(F))" : " (GT6 default)"));
        }
    }

    /** GT6's {@code getInventoryStackLimitGUI:341}: one bee in the royal slot, a stack elsewhere. */
    private static void assertRoyalSlotSize(GameTestHelper helper, BumbliaryContainerMenu menu,
                                            BumbliaryBlockEntity.Layout layout) {
        helper.assertTrue(menu.getSlot(layout.royal()).getMaxStackSize() == 1,
                "the royal slot " + layout.royal() + " holds one bee (GT6 :341)");
        helper.assertTrue(menu.getSlot(layout.drone()).getMaxStackSize() == 64,
                "a drone slot holds a stack (GT6 :341)");
    }

    /**
     * GT6's royal slot take rule ({@code canTakeOutOfSlotGUI:367-369}): anything may leave it while no
     * live queen sits there, {@code bumbleType % 5 == 2} is the one case the machine refuses.
     */
    private static void assertRoyalTake(GameTestHelper helper, BumbliaryContainerMenu menu,
                                        BumbliaryBlockEntity machine, BumbliaryBlockEntity.Layout layout,
                                        ItemStack princess, ItemStack queen, Player player, boolean creative) {
        String role = creative ? "a creative player" : "a survival player";
        Slot royal = menu.getSlot(layout.royal());
        machine.inventory().setStackInSlot(layout.royal(), ItemStack.EMPTY);
        helper.assertTrue(royal.mayPickup(player) == creative, role + " follows normal-menu royal take flag");
        machine.inventory().setStackInSlot(layout.royal(), princess);
        helper.assertTrue(royal.mayPickup(player) == creative,
                role + " needs scoop mode or creative for royal-slot extraction");
        machine.inventory().setStackInSlot(layout.royal(), queen);
        helper.assertTrue(!royal.mayPickup(player),
                role + " may not take a live queen out of the royal slot (GT6 :368)");
    }

    /** GT6's NBT ({@code readFromNBT2:66-74}, {@code writeToNBT2:76-85}). */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void progressAndBroodSurviveNbt(GameTestHelper helper) {
        BumbliaryBlockEntity machine = machine(helper, 0, false);
        ItemStack princess = bee(helper, SPECIES, BumbleBeeType.PRINCESS, genes(1200, 10000, 100, 3));
        machine.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_ROYAL, princess);
        machine.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_DRONE,
                bee(helper, SPECIES, BumbleBeeType.DRONE, genes(1200, 10000, 100, 3)));
        int bred = 0;
        while (machine.life() == 0 && bred < 3000) {
            machine.tickLogic();
            bred++;
        }
        helper.assertTrue(machine.life() > 0, "the pair breeds before the save");
        ItemStack[] brood = machine.brood();
        long life = machine.life();
        long countdown = machine.countdown();

        CompoundTag tag = machine.saveWithFullMetadata();
        helper.assertTrue(tag.contains(BumbliaryBlockEntity.NBT_PROGRESS)
                        && tag.contains(BumbliaryBlockEntity.NBT_COOLDOWN)
                        && tag.contains(BumbliaryBlockEntity.NBT_INV_OUT)
                        && tag.contains(BumbliaryBlockEntity.NBT_INVENTORY),
                "the save carries GT6's keys gt.progress, gt.cooldown and gt.invout plus the inventory");
        helper.assertTrue(tag.getLong(BumbliaryBlockEntity.NBT_PROGRESS) == life
                        && tag.getLong(BumbliaryBlockEntity.NBT_COOLDOWN) == countdown,
                "the numbers are GT6's mLife and mBreedingCountDown");
        helper.assertTrue(tag.getInt(BumbliaryBlockEntity.NBT_INV_OUT) == brood.length,
                "the brood list stores its length (GT6 :81)");

        BumbliaryBlockEntity restored = machine(helper, 1, false);
        restored.load(tag);
        helper.assertTrue(restored.life() == life, "the life survives, got " + restored.life());
        helper.assertTrue(restored.countdown() == countdown,
                "the countdown survives, got " + restored.countdown());
        helper.assertTrue(restored.brood().length == brood.length,
                "the brood size survives, got " + restored.brood().length);
        for (int i = 0; i < brood.length; i++) {
            helper.assertTrue(ItemStack.matches(brood[i], restored.brood()[i]),
                    "brood entry " + i + " survives unchanged");
        }
        ItemStack royal = restored.inventory().getStackInSlot(BumbliaryBlockEntity.SLOT_ROYAL);
        helper.assertTrue(BumbleBeeType.of(royal) == BumbleBeeType.QUEEN && speciesId(royal) == SPECIES,
                "the crowned queen survives the round trip, got " + royal);
        helper.assertTrue(royal.getTag() != null && royal.getTag().contains(BumbleBeeGenes.NBT_KEY),
                "and so does its genome");
        helper.assertTrue(restored.inventory().getSlots() == 36, "the inventory is restored");

        // The advanced machine keeps GT6's 20 slots through the round trip too.
        BumbliaryBlockEntity advanced = machine(helper, 2, true);
        advanced.inventory().setStackInSlot(BumbliaryBlockEntity.ADV_SLOT_ROYAL,
                bee(helper, SPECIES, BumbleBeeType.PRINCESS, genes(1200, 1, 100, 1)));
        BumbliaryBlockEntity restoredAdvanced = machine(helper, 3, true);
        restoredAdvanced.load(advanced.saveWithFullMetadata());
        helper.assertTrue(restoredAdvanced.inventory().getSlots() == 20
                        && BumbleBeeType.of(restoredAdvanced.inventory()
                        .getStackInSlot(BumbliaryBlockEntity.ADV_SLOT_ROYAL)) == BumbleBeeType.PRINCESS,
                "the advanced machine keeps its 20 slots and its royal bee");
        helper.succeed();
    }

    /** GT6's {@code breakDrop} ({@code :346}): a running machine kills the bees it hands out. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void breakingARunningMachineKillsItsBees(GameTestHelper helper) {
        // Re-runnable: wipe what an earlier run may have left in the suite's own area, and use two
        // sites 8 blocks apart so the drops of the first machine cannot be counted with the second.
        resetSites(helper);
        BlockPos idlePos = site(SITE_IDLE);
        helper.getLevel().setBlock(idlePos, GTToolBlocks.BUMBLIARY.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(idlePos) instanceof BumbliaryBlockEntity,
                "the placed bumbliary has its block entity");
        BumbliaryBlockEntity idle = (BumbliaryBlockEntity) helper.getLevel().getBlockEntity(idlePos);

        // An idle machine hands its bees out alive.
        idle.inventory().setStackInSlot(BumbliaryBlockEntity.SLOTS_DRONE[0],
                bee(helper, SPECIES, BumbleBeeType.DRONE, genes(1200, 1, 100, 1)));
        helper.getLevel().removeBlock(idlePos, false);
        List<ItemStack> idleDrops = droppedItems(helper, idlePos);
        helper.assertTrue(idleDrops.size() == 1 && BumbleBeeType.of(idleDrops.get(0)) == BumbleBeeType.DRONE,
                "an idle machine hands its bee out alive, got " + idleDrops);

        // A running machine kills the queen, the drone it holds and the drone the breeding killed.
        BlockPos runningPos = site(SITE_RUNNING);
        helper.getLevel().setBlock(runningPos, GTToolBlocks.BUMBLIARY.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(runningPos) instanceof BumbliaryBlockEntity,
                "the second machine is placed");
        BumbliaryBlockEntity running = (BumbliaryBlockEntity) helper.getLevel().getBlockEntity(runningPos);
        running.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_ROYAL,
                bee(helper, SPECIES, BumbleBeeType.PRINCESS, genes(1200, 1, 100, 1)));
        running.inventory().setStackInSlot(BumbliaryBlockEntity.SLOT_DRONE,
                bee(helper, SPECIES, BumbleBeeType.DRONE, genes(1200, 1, 100, 1)));
        int ticks = 0;
        while (running.life() == 0 && ticks < 3000) {
            running.tickLogic();
            ticks++;
        }
        helper.assertTrue(running.life() > 0, "the machine runs with its queen");
        running.inventory().setStackInSlot(BumbliaryBlockEntity.SLOTS_DRONE[0],
                bee(helper, SPECIES, BumbleBeeType.DRONE, genes(1200, 1, 100, 1)));
        helper.getLevel().removeBlock(runningPos, false);
        List<ItemStack> runningDrops = droppedItems(helper, runningPos);
        helper.assertTrue(runningDrops.size() == 3,
                "the queen, the spare drone and the killed drone drop, got " + runningDrops);
        for (ItemStack stack : runningDrops) {
            helper.assertTrue(BumbleBeeType.of(stack) == BumbleBeeType.DEAD,
                    "a running machine kills what it hands out (:346), got " + BumbleBeeType.of(stack));
            helper.assertTrue(speciesId(stack) == SPECIES, "the dead bee keeps its species");
        }
        resetSites(helper);     // the drops of both machines leave the world again
        helper.succeed();
    }

    /** The items lying around the machine's position. */
    private static List<ItemStack> droppedItems(GameTestHelper helper, BlockPos pos) {
        List<ItemStack> dropped = new ArrayList<>();
        for (ItemEntity entity : helper.getLevel()
                .getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0))) {
            dropped.add(entity.getItem());
        }
        return dropped;
    }

    /** GT6's advanced machine ({@code MultiTileEntityBumbliaryAdvanced:333-344}) works like the standard one. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void advancedMachineUsesGt6sTwentySlotLayout(GameTestHelper helper) {
        BumbliaryBlockEntity.Layout layout = BumbliaryBlockEntity.ADVANCED_LAYOUT;
        helper.assertTrue(layout.advanced(), "the layout knows it is the advanced machine");
        helper.assertTrue(layout.royal() == 7 && layout.drone() == 12,
                "GT6's advanced bee slots are 7 and 12 (:333)");
        helper.assertTrue(java.util.Arrays.equals(layout.combs(), BumbliaryBlockEntity.ADV_SLOTS_COMBS)
                        && java.util.Arrays.equals(layout.drones(), BumbliaryBlockEntity.ADV_SLOTS_DRONE)
                        && java.util.Arrays.equals(layout.dead(), BumbliaryBlockEntity.ADV_SLOTS_DEAD),
                "the advanced slot groups are GT6's own (:334-336)");
        helper.assertTrue(layout.productRoll() == GTBumbleProducts.ADVANCED_ROLL,
                "the advanced machine rolls rng(20000) for its products (:171)");

        BumbliaryBlockEntity machine = machine(helper, 0, true);
        helper.assertTrue(machine.layout() == BumbliaryBlockEntity.ADVANCED_LAYOUT,
                "the advanced block gives its block entity the advanced layout");
        helper.assertTrue(machine.inventory().getSlots() == 20, "20 slots, got " + machine.inventory().getSlots());
        helper.assertTrue(machine.inventory().getSlotLimit(BumbliaryBlockEntity.ADV_SLOT_ROYAL) == 1,
                "one bee in the advanced royal slot (GT6 :342)");

        machine.inventory().setStackInSlot(BumbliaryBlockEntity.ADV_SLOT_ROYAL,
                bee(helper, SPECIES, BumbleBeeType.PRINCESS, genes(1200, 1, 100, 2)));
        machine.inventory().setStackInSlot(BumbliaryBlockEntity.ADV_SLOT_DRONE,
                bee(helper, SPECIES, BumbleBeeType.DRONE, genes(1200, 1, 100, 2)));
        int ticks = 0;
        while (machine.life() == 0 && ticks < 3000) {
            machine.tickLogic();
            ticks++;
        }
        helper.assertTrue(machine.life() > 0, "the advanced machine breeds as well");
        helper.assertTrue(BumbleBeeType.of(machine.inventory().getStackInSlot(BumbliaryBlockEntity.ADV_SLOT_ROYAL))
                        == BumbleBeeType.QUEEN,
                "and crowns its queen into slot 7");
        helper.assertTrue(!contents(machine, BumbliaryBlockEntity.ADV_SLOTS_DEAD).isEmpty(),
                "and kills the drone into its own dead slots");
        helper.succeed();
    }
}
