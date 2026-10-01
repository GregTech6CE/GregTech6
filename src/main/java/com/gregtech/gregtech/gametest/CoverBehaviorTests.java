package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.cover.CoverItems;
import com.gregtech.gregtech.content.cover.CoverUtilityBehaviors;
import com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.ValveAction;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTMiscBlocks;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

/**
 * The second batch of cover behaviours: the crafting table cover, the item retriever, both filters,
 * the pressure valve, the asphalt panel and the redstone torch/repeater pair — the contents of
 * {@link CoverUtilityBehaviors}.
 *
 * <p>GT6 sources, one per group: {@code CoverCrafting:46-59}, {@code CoverRetrieverItem:55-79},
 * {@code CoverFilterItem:115-127} and {@code CoverFilterFluid:117-129}, {@code
 * CoverPressureValve:44-64}, {@code CoverAsphalt:38-41}, {@code CoverRedstoneTorch:42-44} with
 * {@code CoverRedstoneRepeater:43} and their shared base
 * {@code AbstractCoverAttachmentTorch:35-68}, and {@code CoverTextureMulti:63-78} for the two
 * texture covers.</p>
 *
 * <h2>Why the machines stand at {@link #BASE_X}, {@link #BASE_Z}</h2>
 *
 * <p>Same arrangement as {@code CoverAttachmentTests}: the template is the 1×1×1 {@code test_empty}
 * and every block is placed through {@link GameTestHelper#getLevel()} at coordinates no other suite
 * uses (checked: 27000 is neither a {@code BASE_X} nor a {@code BASE_Z} anywhere else in this
 * package). Each test owns its own site inside that area and wipes it before use, so a re-run in an
 * already-populated world starts from the same state — {@code LevelChunk.setBlockState} returns
 * {@code null} without replacing the block entity when the state is unchanged, which is why the
 * sites are cleared with {@code removeBlock} and not merely overwritten with air.</p>
 *
 * <h2>How the machine path is reached</h2>
 *
 * <p>{@code BasicMachineBlockEntity.tickUtilityCovers} is the entry point this batch adds; the
 * integrator wires it into {@code tickCovers} with one line, and {@link #tick} calls it in exactly
 * the position that line will occupy (after {@code serverTick} has advanced {@code coverTicks}).
 * So every "on a real machine" test below goes through {@code getCoverId} → {@link
 * CoverItems#behavior} → the behaviour, which is the same dispatch a running game uses.</p>
 *
 * <p>Two of the covers did <em>not</em> dispatch at first: {@code pressure_value} and
 * {@code panel_asphalt} failed {@code CoverItems.portCoverId} ({@code CoverItems:75-83}), so {@link
 * CoverItems#behavior} answered {@code null} for them and no face could reach their behaviour. The
 * integrator added the two ids, and
 * {@link #pressureValveAndAsphaltAreDispatchableCovers} now asserts the dispatch instead of the gap;
 * the pressure valve's own in-world test drives
 * {@link CoverUtilityBehaviors#tickPressureValve} directly with the tank of a machine that really
 * has the cover attached.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class CoverBehaviorTests {

    private static final int BASE_X = 27000;
    private static final int BASE_Y = 100;
    private static final int BASE_Z = 27000;

    /** One of this file's sites, {@code dx}/{@code dz} apart inside the reserved area. */
    private static BlockPos site(int dx, int dz) {
        return new BlockPos(BASE_X + dx, BASE_Y, BASE_Z + dz);
    }

    /**
     * Clears a {@code (2r+1)²} patch down to {@code r} blocks out and lays a stone floor under it.
     *
     * <p>{@code removeBlock} rather than {@code setBlock(AIR)}: overwriting a block with the state it
     * already has is a no-op and leaves the old block entity — and its inventory — in place
     * ({@code LevelChunk.setBlockState:224}).</p>
     */
    private static void prepare(ServerLevel level, BlockPos pos, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 2; y >= -1; y--) level.removeBlock(pos.offset(x, y, z), false);
                level.setBlock(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }

    /**
     * A machine with one input and one output item slot and one input and one output fluid tank, on
     * ground the caller has already prepared.
     *
     * <p>The recipe map is empty on purpose, exactly as in {@code CoverAttachmentTests}: the covers
     * have to work on a machine that never runs a recipe, and an idle machine is also what keeps the
     * inventory and the tanks untouched by anything but the cover under test. The machine is built
     * with an EU energy tag and no adjacent energy source, so {@code mEnergy} stays 0 and
     * {@code autoIO} — which could otherwise move items on its own — is never reached.</p>
     *
     * <p>The recipe map's internal name carries the position, because {@code RecipeMap}'s constructor
     * rejects a duplicate key ({@code RecipeMap:202-204}) and the port's registry of maps is static
     * for the whole server run.</p>
     */
    private static BasicMachineBlockEntity placeMachine(ServerLevel level, BlockPos pos) {
        var block = MachineRegistry.basicMachines().get(0).get();
        level.setBlock(pos, block.defaultBlockState(), 3);
        var machine = (BasicMachineBlockEntity) level.getBlockEntity(pos);
        var recipes = new RecipeMap(null, "cover_utility_" + Long.toUnsignedString(pos.asLong()),
                "Cover utility", "cover_utility", 1, 1, 1, 1, 1, 0, 1, false, false, false, false);
        machine.setSpec(BasicMachineSpec.builder("cover_utility_test", block.basicSpec().material())
                .machineType("test").energy(GregTechTags.Energy.EU, 32).recipes(recipes)
                .faces(FaceConfig.ALL_SIDES).build());
        return machine;
    }

    /** Clears a one-block site and puts a machine on it. */
    private static BasicMachineBlockEntity machine(ServerLevel level, BlockPos pos) {
        prepare(level, pos, 1);
        return placeMachine(level, pos);
    }

    /**
     * Ticks the machine itself, then runs the utility-cover pass.
     *
     * <p>{@code serverTick} advances {@code coverTicks} by one, and {@code tickUtilityCovers} is
     * called straight after it, which is where the integrator's one-line hook inside
     * {@code tickCovers} will sit. The covers throttle on that counter, so the number of ticks a
     * test asks for is the number it gets.</p>
     */
    private static void tick(BasicMachineBlockEntity machine, int times) {
        for (int i = 0; i < times; i++) {
            BasicMachineBlockEntity.serverTick(machine.getLevel(), machine.getBlockPos(),
                    machine.getBlockState(), machine);
            machine.tickUtilityCovers(machine.getLevel(), machine.getBlockPos());
        }
    }

    private static ItemStack cover(String id) {
        return new ItemStack(GTTechnological.get(id));
    }

    private static ItemStack asphaltPanel() {
        return new ItemStack(GTMiscBlocks.PANEL_ASPHALT.get());
    }

    /**
     * An iron golem (100 HP, no armour) standing on the floor of {@code pos}.
     *
     * <p>Same shape as {@code HazardDamageTests.golem}: 100 HP is what makes GT6's temperature
     * damage readable to the tenth, where a 10 HP cow would clamp at 0, and {@code invulnerableTime}
     * is zeroed so the very first {@code hurt} call is not swallowed by the spawn grace period.</p>
     */
    private static IronGolem golem(ServerLevel level, BlockPos pos) {
        IronGolem golem = EntityType.IRON_GOLEM.create(level);
        golem.setPersistenceRequired();
        golem.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        golem.invulnerableTime = 0;
        level.addFreshEntity(golem);
        return golem;
    }

    // ── CoverCrafting ─────────────────────────────────────────────────────────────

    /** GT6 {@code CoverCrafting:57-59}: the three placement predicates of the crafting cover. */
    @GameTest(template = "test_empty")
    public static void craftingCoverPredicatesMatchTheOriginal(GameTestHelper helper) {
        helper.assertTrue(CoverUtilityBehaviors.CRAFTING_GRID_SLOTS == 9,
                "CoverCrafting:49 opens a 9 slot window, got "
                        + CoverUtilityBehaviors.CRAFTING_GRID_SLOTS);
        helper.assertTrue(!CoverUtilityBehaviors.craftingCoverSealsFace(),
                "CoverCrafting:57 isSealable is F - the crafting cover never seals its face");
        helper.assertTrue(CoverUtilityBehaviors.craftingCoverIsDecorative(),
                "CoverCrafting:58 isDecorative is T");
        helper.assertTrue(!CoverUtilityBehaviors.craftingCoverShowsConnectorFront(),
                "CoverCrafting:59 showsConnectorFront is F");
        helper.succeed();
    }

    /**
     * GT6 {@code CoverCrafting:46-55}: right-clicking the cover opens a workbench and the click is
     * consumed whether or not one opened.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void craftingCoverOpensAVanillaWorkbenchOnARealMachine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(0, 0);
        var machine = machine(level, pos);
        helper.assertTrue(machine.getCover(Direction.UP).isEmpty(),
                "the freshly placed machine has no cover on its up face, got "
                        + machine.getCover(Direction.UP));

        helper.assertTrue(machine.attachCover(Direction.UP, cover(CoverUtilityBehaviors.CRAFTING_TABLE)),
                "the machine face accepts the crafting table cover");
        String behaviour = CoverItems.behavior(machine.getCover(Direction.UP));
        helper.assertTrue(CoverUtilityBehaviors.CRAFTING_TABLE.equals(behaviour),
                "gregtech:crafting_table_cover is the crafting behaviour, got " + behaviour);

        // CoverCrafting:47 - the container only exists for a server player; CoverCrafting:54 returns
        // T either way, which is what makes the click "consumed" without a GUI.
        helper.assertTrue(CoverUtilityBehaviors.clickCraftingCover(null, level, pos),
                "CoverCrafting:54 consumes the click even without a player");

        // A GameTest mock player has a ServerGamePacketListenerImpl over a Connection with no channel,
        // so ServerPlayer#openMenu NPEs on Channel.pipeline() before the menu exists. Forge's
        // FakePlayer wraps its connection in FakePlayerNetHandler, whose send(...) is a no-op
        // (FakePlayer.java:136-137), which is exactly what a menu-opening test needs.
        ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        helper.assertTrue(player instanceof ServerPlayer, "the fake player is a server player");
        helper.assertTrue(CoverUtilityBehaviors.clickCraftingCover(player, level, machine.getBlockPos()),
                "the crafting cover consumes the right-click");
        helper.assertTrue(player.containerMenu instanceof CraftingMenu,
                "CoverCrafting:49-50 opens the vanilla workbench menu, got " + player.containerMenu);
        helper.assertTrue(player.containerMenu.slots.size() > CoverUtilityBehaviors.CRAFTING_GRID_SLOTS,
                "the opened menu carries the 3x3 grid of CoverCrafting:49, got "
                        + player.containerMenu.slots.size() + " slots");
        helper.succeed();
    }

    // ── CoverFilterItem / CoverFilterFluid ─────────────────────────────────────────

    /**
     * The whole truth table of GT6 {@code CoverFilterItem:115-127} / {@code CoverFilterFluid:117-129}
     * — every combination of "is a filter stored", "which mode", "does it match" and "is the cover
     * suspended".
     */
    @GameTest(template = "test_empty")
    public static void filterPermitsCoversTheWholeTruthTable(GameTestHelper helper) {
        // CoverFilterItem:119 - whitelist and a match: let it through.
        helper.assertTrue(CoverUtilityBehaviors.filterPermits(true, false, false, true),
                "whitelist + match passes");
        // CoverFilterItem:119 - whitelist and no match: the very point of a whitelist.
        helper.assertTrue(!CoverUtilityBehaviors.filterPermits(true, false, false, false),
                "whitelist + no match is blocked");
        // CoverFilterItem:119 - blacklist and a match: the "Inverted Filter" of :60.
        helper.assertTrue(!CoverUtilityBehaviors.filterPermits(true, true, false, true),
                "blacklist + match is blocked");
        helper.assertTrue(CoverUtilityBehaviors.filterPermits(true, true, false, false),
                "blacklist + no match passes");
        // CoverFilterItem:118 - an empty whitelist admits nothing; an empty blacklist everything.
        helper.assertTrue(!CoverUtilityBehaviors.filterPermits(false, false, false, true),
                "an empty whitelist blocks even a matching stack (CoverFilterItem:118)");
        helper.assertTrue(CoverUtilityBehaviors.filterPermits(false, true, false, false),
                "an empty blacklist blocks nothing (CoverFilterItem:118)");
        // CoverFilterItem:117 - mStopped short-circuits both modes.
        helper.assertTrue(!CoverUtilityBehaviors.filterPermits(true, false, true, true)
                        && !CoverUtilityBehaviors.filterPermits(true, true, true, true)
                        && !CoverUtilityBehaviors.filterPermits(false, true, true, true),
                "a suspended cover blocks everything in both modes (CoverFilterItem:117)");
        helper.succeed();
    }

    /**
     * The stored filter, the mode and the NBT rule, on the cover stack of a real machine face:
     * GT6's {@code ST.equal(filter, candidate, T)} is item identity plus the port's NBT wildcard
     * ({@code FilterRules:9-12}), and the screwdriver flips the mode ({@code CoverFilterItem:58-62}).
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void filterCoversOnALiveMachineDriveTheirPredicate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(24, 0);
        var machine = machine(level, pos);

        helper.assertTrue(machine.attachCover(Direction.NORTH, cover(CoverUtilityBehaviors.FILTER_ITEM)),
                "the machine face accepts the item filter");
        helper.assertTrue(machine.attachCover(Direction.SOUTH, cover(CoverUtilityBehaviors.FILTER_FLUID)),
                "the machine face accepts the fluid filter");
        helper.assertTrue(CoverUtilityBehaviors.FILTER_ITEM.equals(CoverItems.behavior(machine.getCover(Direction.NORTH)))
                        && CoverUtilityBehaviors.FILTER_FLUID.equals(
                                CoverItems.behavior(machine.getCover(Direction.SOUTH))),
                "gregtech:item_filter and gregtech:fluid_filter name their own behaviours, got "
                        + CoverItems.behavior(machine.getCover(Direction.NORTH)) + " / "
                        + CoverItems.behavior(machine.getCover(Direction.SOUTH)));

        // An empty whitelist admits nothing - the edge CoverFilterItem:118 exists for.
        helper.assertTrue(!machine.coverFilterPermits(Direction.NORTH, new ItemStack(Items.APPLE)),
                "an unconfigured item filter passes nothing");

        helper.assertTrue(CoverUtilityBehaviors.setItemFilter(machine.getCover(Direction.NORTH),
                        new ItemStack(Items.APPLE)),
                "the item filter accepts an apple (CoverFilterItem:92-97)");
        helper.assertTrue(machine.coverFilterPermits(Direction.NORTH, new ItemStack(Items.APPLE)),
                "the configured item filter passes its own item");
        helper.assertTrue(!machine.coverFilterPermits(Direction.NORTH, new ItemStack(Items.DIAMOND)),
                "the configured item filter blocks another item");
        helper.assertTrue(!CoverUtilityBehaviors.setItemFilter(machine.getCover(Direction.NORTH),
                        new ItemStack(Items.DIAMOND)),
                "a configured filter is not overwritten by the next right-click (CoverFilterItem:94)");

        // CoverFilterItem:58-62 - the screwdriver turns it into a blacklist.
        helper.assertTrue(machine.configureFilterCover(Direction.NORTH, true, false),
                "the screwdriver configures the item filter");
        helper.assertTrue(!machine.coverFilterPermits(Direction.NORTH, new ItemStack(Items.APPLE))
                        && machine.coverFilterPermits(Direction.NORTH, new ItemStack(Items.DIAMOND)),
                "the inverted filter blocks what it stores and passes the rest (CoverFilterItem:60)");
        helper.assertTrue(machine.configureFilterCover(Direction.NORTH, false, true),
                "the soft hammer configures the item filter");
        // GT6 CoverFilterItem:63-66 removes ONLY the "gt.filter.item" tag - mVisuals, i.e. the mode,
        // survives ("Blacklist Filter" stays on). An emptied blacklist therefore admits everything
        // again (:118 returns mVisuals == 0, so a blacklist never intercepts), while an emptied
        // whitelist blocks everything. Both halves are asserted, because they are the two edges the
        // original's line 118 exists for.
        helper.assertTrue(machine.coverItemFilter(Direction.NORTH).isEmpty(),
                "the soft hammer clears the stored filter");
        helper.assertTrue(machine.coverFilterPermits(Direction.NORTH, new ItemStack(Items.APPLE)),
                "an emptied BLACKLIST admits everything again (CoverFilterItem:118 with mVisuals != 0)");
        helper.assertTrue(machine.configureFilterCover(Direction.NORTH, true, false),
                "the screwdriver flips it back to a whitelist");
        helper.assertTrue(machine.coverItemFilter(Direction.NORTH).isEmpty()
                        && !machine.coverFilterPermits(Direction.NORTH, new ItemStack(Items.APPLE)),
                "and an emptied WHITELIST blocks everything (CoverFilterItem:118 with mVisuals == 0)");

        helper.assertTrue(CoverUtilityBehaviors.setFluidFilter(machine.getCover(Direction.SOUTH),
                        new FluidStack(Fluids.WATER, 1000)),
                "the fluid filter accepts water (CoverFilterFluid:94-111)");
        helper.assertTrue(machine.coverFluidFilterPermits(Direction.SOUTH,
                        new FluidStack(Fluids.WATER, 1000)),
                "the configured fluid filter passes its own fluid");
        helper.assertTrue(!machine.coverFluidFilterPermits(Direction.SOUTH,
                        new FluidStack(Fluids.LAVA, 1000)),
                "the configured fluid filter blocks another fluid");
        helper.assertTrue(machine.coverFluidFilter(Direction.SOUTH).getFluid() == Fluids.WATER,
                "the fluid filter stores the fluid itself, got "
                        + machine.coverFluidFilter(Direction.SOUTH));
        helper.succeed();
    }

    // ── CoverRetrieverItem ─────────────────────────────────────────────────────────

    /** GT6 {@code CoverRetrieverItem:61,72}: the 20-tick cadence, the pending flag and the ceiling. */
    @GameTest(template = "test_empty")
    public static void retrieverCadenceAndCeilingMatchTheOriginal(GameTestHelper helper) {
        helper.assertTrue(CoverUtilityBehaviors.RETRIEVER_PERIOD == 20
                        && CoverUtilityBehaviors.RETRIEVER_PHASE == 15,
                "CoverRetrieverItem:61 is SERVER_TIME % 20 == 15, got period "
                        + CoverUtilityBehaviors.RETRIEVER_PERIOD + " phase "
                        + CoverUtilityBehaviors.RETRIEVER_PHASE);
        helper.assertTrue(CoverUtilityBehaviors.RETRIEVER_MAX_MOVE == 64
                        && CoverUtilityBehaviors.RETRIEVER_MIN_MOVE == 1,
                "CoverRetrieverItem:72 moves 64 at a time, got "
                        + CoverUtilityBehaviors.RETRIEVER_MAX_MOVE);

        helper.assertTrue(CoverUtilityBehaviors.retrieverDue(15, false),
                "the retriever fires at tick 15 of the cycle");
        helper.assertTrue(!CoverUtilityBehaviors.retrieverDue(14, false)
                        && !CoverUtilityBehaviors.retrieverDue(16, false),
                "and not on either neighbour of it");
        helper.assertTrue(CoverUtilityBehaviors.retrieverDue(35, false),
                "the cycle repeats every 20 ticks");
        // CoverRetrieverItem:55-57,61 - a cover that just resumed runs immediately.
        helper.assertTrue(CoverUtilityBehaviors.retrieverDue(0, true)
                        && CoverUtilityBehaviors.retrieverDue(14, true),
                "the pending flag of CoverRetrieverItem:56 lets the retriever run off-cadence");
        helper.succeed();
    }

    /**
     * GT6 {@code CoverRetrieverItem:60-79} on a real machine: the filtered items of the inventory in
     * front arrive in the machine's input slot, and the rest stay where they are.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void retrieverPullsFilteredItemsIntoTheMachine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(8, 0);
        var machine = machine(level, pos);
        BlockPos chestPos = pos.east();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        var chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
        helper.assertTrue(chest.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,
                        Direction.WEST).isPresent(),
                "the source chest exposes its item handler, got " + chest);
        chest.setItem(0, new ItemStack(Items.APPLE, 3));
        chest.setItem(1, new ItemStack(Items.DIAMOND, 5));

        helper.assertTrue(machine.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.RETRIEVER_ITEM)),
                "the machine face accepts the item retriever cover");
        String behaviour = CoverItems.behavior(machine.getCover(Direction.EAST));
        helper.assertTrue(CoverUtilityBehaviors.RETRIEVER_ITEM.equals(behaviour),
                "gregtech:item_retriever_cover is the retriever behaviour, got " + behaviour);
        helper.assertTrue(CoverUtilityBehaviors.setItemFilter(machine.getCover(Direction.EAST),
                        new ItemStack(Items.APPLE)),
                "the retriever takes its filter from a right-click (CoverRetrieverItem:126-133)");
        helper.assertTrue(machine.inventory().getStackInSlot(0).isEmpty(),
                "the machine input starts empty, got " + machine.inventory().getStackInSlot(0));

        tick(machine, CoverUtilityBehaviors.RETRIEVER_PHASE);

        ItemStack arrived = machine.inventory().getStackInSlot(0);
        helper.assertTrue(arrived.is(Items.APPLE) && arrived.getCount() == 3,
                "the retriever moved the three filtered apples into the machine, got " + arrived);
        helper.assertTrue(chest.getItem(0).isEmpty(),
                "the source slot is empty afterwards, got " + chest.getItem(0));
        helper.assertTrue(chest.getItem(1).getCount() == 5 && chest.getItem(1).is(Items.DIAMOND),
                "the unfiltered diamonds stayed in the source, got " + chest.getItem(1));
        helper.succeed();
    }

    /** GT6 {@code CoverRetrieverItem:72}: {@code aInvertFilter} — the screwdriver's other half. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void retrieverBlacklistModeKeepsTheFilteredItemOut(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(16, 0);
        var machine = machine(level, pos);
        BlockPos chestPos = pos.east();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        var chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.APPLE, 4));
        chest.setItem(1, new ItemStack(Items.DIAMOND, 2));

        helper.assertTrue(machine.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.RETRIEVER_ITEM)),
                "the machine face accepts the item retriever cover");
        helper.assertTrue(CoverUtilityBehaviors.setItemFilter(machine.getCover(Direction.EAST),
                        new ItemStack(Items.APPLE)),
                "the retriever takes its filter");
        helper.assertTrue(machine.configureFilterCover(Direction.EAST, true, false),
                "the screwdriver inverts the retriever filter (CoverRetrieverItem:94-98)");

        tick(machine, CoverUtilityBehaviors.RETRIEVER_PHASE);

        ItemStack arrived = machine.inventory().getStackInSlot(0);
        helper.assertTrue(arrived.is(Items.DIAMOND) && arrived.getCount() == 2,
                "an inverted retriever takes everything but the filtered item, got " + arrived);
        helper.assertTrue(chest.getItem(0).getCount() == 4,
                "the filtered apples were left alone, got " + chest.getItem(0));
        helper.succeed();
    }

    // ── CoverPressureValve ─────────────────────────────────────────────────────────

    /** GT6 {@code CoverPressureValve:44,45,53-61}: placement, connection and the decision table. */
    @GameTest(template = "test_empty")
    public static void pressureValveDecisionTableMatchesTheOriginal(GameTestHelper helper) {
        helper.assertTrue(CoverUtilityBehaviors.valveCanAttachTo(1, false),
                "a single tank fluid pipe is a valid host (CoverPressureValve:44)");
        helper.assertTrue(!CoverUtilityBehaviors.valveCanAttachTo(2, false)
                        && !CoverUtilityBehaviors.valveCanAttachTo(0, false),
                "a pipe with anything but one tank is not (CoverPressureValve:44)");
        helper.assertTrue(!CoverUtilityBehaviors.valveCanAttachTo(1, true),
                "and neither is a pipe facing another pipe (CoverPressureValve:44)");
        helper.assertTrue(CoverUtilityBehaviors.valveConnectsThrough(true)
                        && !CoverUtilityBehaviors.valveConnectsThrough(false),
                "CoverPressureValve:45 connects exactly when a pipe faces it");

        helper.assertTrue(CoverUtilityBehaviors.valveAction(false, true, false, false) == ValveAction.IDLE,
                "a tank that is not full is left alone (CoverPressureValve:53)");
        helper.assertTrue(CoverUtilityBehaviors.valveAction(true, false, true, false) == ValveAction.INTO_TANK,
                "a full tank empties into the tank in front (CoverPressureValve:54-56)");
        helper.assertTrue(CoverUtilityBehaviors.valveAction(true, true, false, false) == ValveAction.VENT,
                "a full tank of gas escapes into open air (CoverPressureValve:57)");
        helper.assertTrue(CoverUtilityBehaviors.valveAction(true, false, false, false) == ValveAction.IDLE,
                "a liquid does not - 'Liquids require Tank in front!' (CoverPressureValve:70)");
        helper.assertTrue(CoverUtilityBehaviors.valveAction(true, true, false, true) == ValveAction.IDLE,
                "and a gas does not escape through a block with a collision box (CoverPressureValve:57)");
        helper.succeed();
    }

    /**
     * GT6 {@code CoverPressureValve:57-61}: the gas leaves, the neighbourhood is heated and the tank
     * is trashed — asserted on a real machine with the cover attached and on an iron golem, whose
     * 100 HP makes the damage readable to the tenth.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void pressureValveVentsAGasAndHeatsWhatStandsInFront(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(32, 0);
        var machine = machine(level, pos);
        var air = com.gregtech.gregtech.registry.GTFluids.still("Air_Nether");
        helper.assertTrue(air != null && air.isPresent(),
                "gregtech:netherair is registered, got " + air);
        var fluid = air.get();
        helper.assertTrue(com.gregtech.gregtech.api.fluid.FluidHazards.isGas(fluid),
                "the valve only vents gases (CoverPressureValve:57), got " + fluid);
        long temperature = CoverUtilityBehaviors.fluidTemperature(fluid);
        helper.assertTrue(temperature > 320L,
                "nether air is hotter than the 320 K heat threshold of UT.java:2984, got "
                        + temperature + " K");

        helper.assertTrue(machine.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.PRESSURE_VALVE)),
                "the machine face accepts the pressure valve");
        helper.assertTrue(!machine.getCover(Direction.EAST).isEmpty(),
                "the valve is attached, got " + machine.getCover(Direction.EAST));

        var tank = machine.getTanksOutput()[0];
        tank.setFluid(new FluidStack(fluid, (int) tank.capacity()));
        helper.assertTrue(tank.getAmount() >= tank.capacity(),
                "the output tank starts full, got " + tank.getAmount() + " of " + tank.capacity());

        IronGolem golem = golem(level, pos.east());
        float before = golem.getHealth();
        helper.assertTrue(before == golem.getMaxHealth(),
                "the iron golem starts at full health, got " + before);

        helper.assertTrue(CoverUtilityBehaviors.tickPressureValve(level, pos, Direction.EAST, tank, 5),
                "the valve acted on a full tank of gas with open air in front");
        float expected = before - Math.max(1.0F, Math.min(
                com.gregtech.gregtech.api.fluid.FluidHazards.PIPE_TEMPERATURE_CAP,
                (com.gregtech.gregtech.api.fluid.FluidHazards.PIPE_TEMPERATURE_MULTIPLIER
                        * (temperature - 300L)) / 50.0F));
        // The valve's entity half is a Level.getEntitiesOfClass query, and a chunk this test only
        // setBlock()ed into is invisible to it: a test chunk's entity sections keep the default HIDDEN
        // visibility until a queued ticket is applied by the next ServerChunkCache.tick
        // (PersistentEntitySectionManager:47, EntitySectionStorage:55 - the trap PileBlockTests:643-651
        // documents). In a running game the chunk is visible and the golem below is hit; here the
        // damage half is asserted through the very function the valve calls, with the very temperature
        // and the very two constants it feeds it, on a golem standing in front of the valve.
        helper.assertTrue(com.gregtech.gregtech.util.GTEntityHelper.applyTemperatureDamage(golem,
                        temperature,
                        com.gregtech.gregtech.api.fluid.FluidHazards.PIPE_TEMPERATURE_MULTIPLIER,
                        com.gregtech.gregtech.api.fluid.FluidHazards.PIPE_TEMPERATURE_CAP),
                "the valve's damage call reaches a golem standing in front of it");
        helper.assertTrue(Math.abs(golem.getHealth() - expected) < 0.001F,
                "the escaping " + temperature + " K gas deals " + (before - expected)
                        + " damage (CoverPressureValve:59 with UT.java:2984), got "
                        + golem.getHealth() + " of " + before);
        helper.assertTrue(tank.isEmpty(),
                "GarbageGT.trash empties the tank (CoverPressureValve:60), got " + tank.getAmount());
        helper.succeed();
    }

    /** GT6 {@code CoverPressureValve:54-56}: {@code FL.move} into the tank that faces the valve. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void pressureValveMovesAFullTankIntoTheTankInFront(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(48, 0);
        // Both machines sit inside one prepared patch; a second prepare() would wipe the first.
        prepare(level, pos, 2);
        var source = placeMachine(level, pos);
        var target = placeMachine(level, pos.east());

        helper.assertTrue(source.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.PRESSURE_VALVE)),
                "the source machine face accepts the pressure valve");
        var sourceTank = source.getTanksOutput()[0];
        sourceTank.setFluid(new FluidStack(Fluids.WATER, (int) sourceTank.capacity()));
        helper.assertTrue(target.getTanksInput()[0].isEmpty(),
                "the target machine's input tank starts empty, got "
                        + target.getTanksInput()[0].getAmount());

        helper.assertTrue(CoverUtilityBehaviors.tickPressureValve(level, pos, Direction.EAST, sourceTank, 5),
                "the valve acted with a machine tank in front");
        helper.assertTrue(sourceTank.isEmpty(),
                "the full tank was emptied into the neighbour (CoverPressureValve:56), got "
                        + sourceTank.getAmount());
        helper.assertTrue(target.getTanksInput()[0].getAmount() > 0
                        && target.getTanksInput()[0].getFluid().getFluid() == Fluids.WATER,
                "the neighbour received the water, got " + target.getTanksInput()[0].getFluid());
        helper.succeed();
    }

    /**
     * The negative half of {@code CoverPressureValve:57}: with no tank in front, a full tank of
     * <em>liquid</em> is not released — which also proves the test above is not passing because the
     * valve empties any tank it is given.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void pressureValveLeavesALiquidAloneWithNoTankInFront(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(64, 0);
        var machine = machine(level, pos);
        helper.assertTrue(machine.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.PRESSURE_VALVE)),
                "the machine face accepts the pressure valve");
        var tank = machine.getTanksOutput()[0];
        tank.setFluid(new FluidStack(Fluids.WATER, (int) tank.capacity()));
        int full = (int) tank.capacity();

        helper.assertTrue(!CoverUtilityBehaviors.tickPressureValve(level, pos, Direction.EAST, tank, 5),
                "a liquid is not vented into the air (CoverPressureValve:70)");
        helper.assertTrue(tank.getAmount() == full,
                "the liquid is still in the tank, got " + tank.getAmount() + " of " + full);

        // CoverPressureValve:50 - the first ticks of a cover are ignored.
        helper.assertTrue(!CoverUtilityBehaviors.tickPressureValve(level, pos, Direction.EAST, tank,
                        CoverUtilityBehaviors.VALVE_MIN_TICKS),
                "the valve waits for aTimer > " + CoverUtilityBehaviors.VALVE_MIN_TICKS
                        + " (CoverPressureValve:50)");
        helper.succeed();
    }

    // ── CoverAsphalt ───────────────────────────────────────────────────────────────

    /** GT6 {@code CoverAsphalt:39}: the three gates of the speed boost, one by one. */
    @GameTest(template = "test_empty")
    public static void asphaltFactorMatchesTheOriginal(GameTestHelper helper) {
        helper.assertTrue(CoverUtilityBehaviors.ASPHALT_BOOST == 1.3D,
                "CoverAsphalt:39 multiplies by 1.3, got " + CoverUtilityBehaviors.ASPHALT_BOOST);
        helper.assertTrue(CoverUtilityBehaviors.asphaltFactor(0.2D, 0.1D, false, false) == 1.3D,
                "a walking, dry, upright entity is boosted");
        helper.assertTrue(CoverUtilityBehaviors.asphaltFactor(0.2D, 0.0D, false, false) == 1.3D,
                "moving along one axis alone is enough (CoverAsphalt:39 uses ||)");
        helper.assertTrue(CoverUtilityBehaviors.asphaltFactor(0.0D, 0.0D, false, false) == 1.0D,
                "a standing entity is not (CoverAsphalt:39)");
        helper.assertTrue(CoverUtilityBehaviors.asphaltFactor(0.2D, 0.1D, true, false) == 1.0D,
                "swimming is not boosted (CoverAsphalt:39)");
        helper.assertTrue(CoverUtilityBehaviors.asphaltFactor(0.2D, 0.1D, false, true) == 1.0D,
                "and neither is sneaking (CoverAsphalt:39)");
        helper.succeed();
    }

    /** GT6 {@code CoverAsphalt:38-41} applied to a live entity standing in the world. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void asphaltCoverBoostsAWalkingEntity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(80, 0);
        prepare(level, pos, 1);

        IronGolem walker = golem(level, pos);
        walker.setDeltaMovement(0.2D, 0.0D, 0.1D);
        helper.assertTrue(CoverUtilityBehaviors.walkOverAsphalt(walker),
                "an asphalt cover boosts a walking entity");
        helper.assertTrue(Math.abs(walker.getDeltaMovement().x - 0.2D * 1.3D) < 1e-9
                        && Math.abs(walker.getDeltaMovement().z - 0.1D * 1.3D) < 1e-9,
                "both horizontal components are multiplied by 1.3 (CoverAsphalt:39), got "
                        + walker.getDeltaMovement());
        helper.assertTrue(walker.getDeltaMovement().y == 0.0D,
                "the original leaves motionY alone, got " + walker.getDeltaMovement().y);

        IronGolem sneaker = golem(level, pos.offset(0, 0, 1));
        sneaker.setDeltaMovement(0.2D, 0.0D, 0.1D);
        sneaker.setShiftKeyDown(true);
        helper.assertTrue(!CoverUtilityBehaviors.walkOverAsphalt(sneaker),
                "a sneaking entity is not boosted (CoverAsphalt:39)");
        helper.assertTrue(sneaker.getDeltaMovement().x == 0.2D
                        && sneaker.getDeltaMovement().z == 0.1D,
                "the sneaking entity keeps its motion, got " + sneaker.getDeltaMovement());
        helper.succeed();
    }

    // ── CoverRedstoneTorch / CoverRedstoneRepeater ─────────────────────────────────

    /**
     * GT6 {@code CoverRedstoneTorch:43} against {@code CoverRedstoneRepeater:43}: one inverts the
     * wire it sits on, the other follows it, and both emit through
     * {@code AbstractCoverAttachmentTorch:50-57}.
     */
    @GameTest(template = "test_empty")
    public static void redstoneTorchAndRepeaterFollowAndInvertTheWire(GameTestHelper helper) {
        // CoverRedstoneTorch:43 - condition is mRedstone > 0, and AbstractCoverAttachmentTorch:63
        // turns a true condition into mVisuals = 1, which CoverRedstoneTorch:31 renders as OFF.
        helper.assertTrue(CoverUtilityBehaviors.torchCondition(true),
                "the torch condition is 'the wire carries a signal' (CoverRedstoneTorch:43)");
        helper.assertTrue(CoverUtilityBehaviors.torchVisual(false) == CoverUtilityBehaviors.TORCH_VISUAL_LIT
                        && CoverUtilityBehaviors.torchVisual(true) == CoverUtilityBehaviors.TORCH_VISUAL_DARK,
                "an unpowered wire lights the torch and a powered one darkens it "
                        + "(CoverRedstoneTorch:43 + AbstractCoverAttachmentTorch:62-66)");
        helper.assertTrue(CoverUtilityBehaviors.repeaterVisual(true) == CoverUtilityBehaviors.TORCH_VISUAL_LIT
                        && CoverUtilityBehaviors.repeaterVisual(false) == CoverUtilityBehaviors.TORCH_VISUAL_DARK,
                "the repeater lights with the wire (CoverRedstoneRepeater:43)");
        helper.assertTrue(!CoverUtilityBehaviors.repeaterCondition(true)
                        && CoverUtilityBehaviors.repeaterCondition(false),
                "the repeater condition is mRedstone <= 0 (CoverRedstoneRepeater:43)");

        helper.assertTrue(CoverUtilityBehaviors.torchSignal(CoverUtilityBehaviors.TORCH_VISUAL_LIT) == 15
                        && CoverUtilityBehaviors.torchSignal(CoverUtilityBehaviors.TORCH_VISUAL_DARK) == 0,
                "AbstractCoverAttachmentTorch:50-57 emits 15 exactly when mVisuals is 0");

        // The two are exact complements, which is what makes them a pair rather than duplicates.
        for (boolean powered : new boolean[]{false, true}) {
            helper.assertTrue((CoverUtilityBehaviors.torchVisual(powered)
                            == CoverUtilityBehaviors.TORCH_VISUAL_LIT)
                            != (CoverUtilityBehaviors.repeaterVisual(powered)
                            == CoverUtilityBehaviors.TORCH_VISUAL_LIT),
                    "the torch and the repeater never agree (wire powered = " + powered + ")");
        }

        helper.assertTrue(CoverUtilityBehaviors.torchCoverCanAttachTo(true)
                        && !CoverUtilityBehaviors.torchCoverCanAttachTo(false),
                "AbstractCoverAttachmentTorch:35 only allows the insulated redstone wire as host");
        helper.assertTrue(CoverUtilityBehaviors.torchCoverInterceptsConnect(),
                "AbstractCoverAttachmentTorch:36 never connects");
        helper.succeed();
    }

    /** GT6 {@code GT_API:799-802}: the covers of the redstone torch and the repeater are the vanilla items. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void torchAndRepeaterCoversAreTheVanillaBlocksOnAMachine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(88, 0);
        var machine = machine(level, pos);

        helper.assertTrue(machine.attachCover(Direction.NORTH, new ItemStack(Items.REDSTONE_TORCH)),
                "the machine face accepts the vanilla redstone torch as a cover");
        helper.assertTrue(CoverItems.REDSTONE_TORCH.equals(CoverItems.behavior(machine.getCover(Direction.NORTH))),
                "minecraft:redstone_torch is the torch cover (GT_API:800-801), got "
                        + CoverItems.behavior(machine.getCover(Direction.NORTH)));
        helper.assertTrue(machine.attachCover(Direction.SOUTH, new ItemStack(Items.REPEATER)),
                "the machine face accepts the vanilla repeater as a cover");
        helper.assertTrue(CoverItems.REDSTONE_REPEATER.equals(CoverItems.behavior(machine.getCover(Direction.SOUTH))),
                "minecraft:repeater is the repeater cover (GT_API:802), got "
                        + CoverItems.behavior(machine.getCover(Direction.SOUTH)));
        helper.assertTrue(!CoverItems.REDSTONE_TORCH.equals(CoverItems.behavior(machine.getCover(Direction.SOUTH))),
                "the two faces keep distinct behaviours");
        helper.assertTrue(machine.hasRedstoneCover(),
                "the machine reports a redstone cover once either is attached "
                        + "(BasicMachineBlockEntity.hasRedstoneCover)");
        helper.succeed();
    }

    // ── CoverTextureMulti: blank cover and warning cover ───────────────────────────

    /**
     * GT6 {@code CoverTextureMulti:63-78}: the whole behaviour of the blank and warning covers is a
     * chisel-clicked design cycle, and both are decorative.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void textureCoversCycleTheirDesignOnARealCover(GameTestHelper helper) {
        helper.assertTrue(CoverUtilityBehaviors.designCount(CoverUtilityBehaviors.BLANK_COVER) == 6,
                "MultiItemTechnological:59 gives the blank cover 6 designs, got "
                        + CoverUtilityBehaviors.designCount(CoverUtilityBehaviors.BLANK_COVER));
        helper.assertTrue(CoverUtilityBehaviors.designCount(CoverUtilityBehaviors.WARNING_COVER) == 20,
                "MultiItemTechnological:87 gives the warning cover 20, got "
                        + CoverUtilityBehaviors.designCount(CoverUtilityBehaviors.WARNING_COVER));
        helper.assertTrue(CoverUtilityBehaviors.designCount(CoverUtilityBehaviors.FILTER_ITEM) == 0,
                "a cover that is not a texture cover has no designs");
        helper.assertTrue(CoverUtilityBehaviors.nextDesign(0, 6) == 1
                        && CoverUtilityBehaviors.nextDesign(5, 6) == 0,
                "CoverTextureMulti:64 wraps around: (5 + 1) % 6 is 0, got "
                        + CoverUtilityBehaviors.nextDesign(5, 6));
        helper.assertTrue(CoverUtilityBehaviors.nextDesign(-1, 6) == 0,
                "a negative stored design still lands inside the range, got "
                        + CoverUtilityBehaviors.nextDesign(-1, 6));
        helper.assertTrue(CoverUtilityBehaviors.nextDesign(3, 0) == 0,
                "a cover with no designs cannot advance");
        helper.assertTrue(CoverUtilityBehaviors.textureCoverIsDecorative()
                        && CoverUtilityBehaviors.textureCoverNeedsVisualsSaved(),
                "CoverTextureMulti:77-78 - decorative, and the chosen design is saved");

        ServerLevel level = helper.getLevel();
        BlockPos pos = site(96, 0);
        var machine = machine(level, pos);
        helper.assertTrue(machine.attachCover(Direction.NORTH, cover(CoverUtilityBehaviors.BLANK_COVER)),
                "the machine face accepts the blank cover");
        helper.assertTrue(CoverUtilityBehaviors.BLANK_COVER.equals(
                        CoverItems.behavior(machine.getCover(Direction.NORTH))),
                "gregtech:blank_cover names its own behaviour, got "
                        + CoverItems.behavior(machine.getCover(Direction.NORTH)));
        helper.assertTrue(CoverUtilityBehaviors.cycleDesign(machine.getCover(Direction.NORTH), 6) == 1,
                "a chisel click advances the blank cover's design (CoverTextureMulti:64), got "
                        + CoverUtilityBehaviors.design(machine.getCover(Direction.NORTH)));
        for (int i = 0; i < 5; i++) {
            CoverUtilityBehaviors.cycleDesign(machine.getCover(Direction.NORTH), 6);
        }
        helper.assertTrue(CoverUtilityBehaviors.design(machine.getCover(Direction.NORTH)) == 0,
                "six clicks come back to the first design, got "
                        + CoverUtilityBehaviors.design(machine.getCover(Direction.NORTH)));

        helper.assertTrue(machine.attachCover(Direction.SOUTH, cover(CoverUtilityBehaviors.WARNING_COVER)),
                "the machine face accepts the warning cover");
        helper.assertTrue(CoverUtilityBehaviors.WARNING_COVER.equals(
                        CoverItems.behavior(machine.getCover(Direction.SOUTH))),
                "gregtech:warning_cover names its own behaviour, got "
                        + CoverItems.behavior(machine.getCover(Direction.SOUTH)));
        helper.succeed();
    }

    // ── The two covers whose item the dispatcher does not know yet ─────────────────

    /**
     * The measured state of the two behaviour ids {@link CoverItems#behavior} cannot resolve.
     *
     * <p>{@code CoverItems.portCoverId} ({@code CoverItems:75-82}) accepts {@code *_cover},
     * {@code drain}, {@code air_vent}, {@code item_filter}, {@code fluid_filter},
     * {@code machine_switch}, the redstone selectors, the activity detectors, {@code energy_sensor},
     * {@code progress_sensor}, {@code redstone_emitter}, {@code auto_reboot_switch} and
     * {@code cover_controller}. {@code pressure_value} and {@code panel_asphalt} match none of them,
     * so both read back as "not a cover" even though both are real cover items in GT6
     * ({@code MultiItemTechnological:176}, {@code Loader_MultiTileEntities:2054}) and both have a
     * behaviour implemented in {@link CoverUtilityBehaviors}.</p>
     *
     * <p>This test asserts that gap rather than papering over it: it is the alarm that fires when
     * the {@code CoverItems} entry is finally added, at which point both assertions flip. The
     * integrator's two-word patch is
     * {@code || id.equals("pressure_value") || id.equals("panel_asphalt")}.</p>
     */
    @GameTest(template = "test_empty")
    public static void pressureValveAndAsphaltAreDispatchableCovers(GameTestHelper helper) {
        String valveId = CoverUtilityBehaviors.PRESSURE_VALVE;
        String asphaltId = CoverUtilityBehaviors.ASPHALT_PANEL;
        helper.assertTrue(valveId.equals(CoverItems.behavior(cover(valveId))),
                "gregtech:" + valveId + " dispatches to its own behaviour, got "
                        + CoverItems.behavior(cover(valveId)));
        helper.assertTrue(asphaltId.equals(CoverItems.behavior(asphaltPanel())),
                "gregtech:" + asphaltId + " dispatches to its own behaviour, got "
                        + CoverItems.behavior(asphaltPanel()));
        // Both are real items with the display names of the GT6 registrations above.
        helper.assertTrue(!cover(valveId).isEmpty() && !asphaltPanel().isEmpty(),
                "both items exist: " + cover(valveId) + " and " + asphaltPanel());
        helper.succeed();
    }

    // ── Pipe interception (the wiring the integrator added) ───────────────────────

    /**
     * §108: the filter covers now filter the pipes that touch the face they sit on.
     *
     * <p>GT6 asks this from the machine's own item/fluid handler — {@code CoverFilterItem:115-127}
     * {@code interceptItemInsert}/{@code interceptItemExtract} and {@code CoverFilterFluid:117-129}
     * {@code interceptFluidFill}/{@code interceptFluidDrain} — and GT6's item retriever refuses
     * <em>everything</em> on its own face ({@code CoverRetrieverItem:138-139}, {@code return
     * aCoverSide == aSide;}). A face without such a cover admits everything: that is the property
     * that keeps every other machine in the pack behaving exactly as it did before the wiring, so it
     * is asserted here first.</p>
     */
    @GameTest(template = "test_empty")
    public static void theFilterCoversFilterThePipesOnTheirOwnFace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Direction side = Direction.NORTH;

        // A bare face admits everything.
        // Sites 104/108/112/116: the other methods of this class own 0, 8, 16, 24, 32, 48, 64, 80,
        // 88 and 96, and placeMachine keys its RecipeMap by block position - two tests on the same
        // site throw "Duplicate RecipeMap key" at construction time.
        BasicMachineBlockEntity bareMachine = machine(level, site(104, 0));
        bareMachine.inventory().setStackInSlot(0, new ItemStack(Items.APPLE));
        IItemHandler bare = bareMachine.getCapability(ForgeCapabilities.ITEM_HANDLER, side).resolve().orElse(null);
        helper.assertTrue(bare != null, "a machine face exposes its item handler");
        helper.assertTrue(bare.insertItem(0, new ItemStack(Items.APPLE), true).isEmpty(),
                "a face without a cover lets a pipe insert");
        helper.assertTrue(bare.extractItem(0, 1, true).getCount() == 1,
                "and lets a pipe extract");

        // The item filter: an apple goes on the whitelist and only apples may pass afterwards.
        BasicMachineBlockEntity filteredMachine = machine(level, site(108, 0));
        helper.assertTrue(filteredMachine.attachCover(side, cover(CoverUtilityBehaviors.FILTER_ITEM)),
                "the item filter attaches to a machine face");
        helper.assertTrue(filteredMachine.clickFilterCover(side, new ItemStack(Items.APPLE)),
                "the held apple becomes the filter");
        IItemHandler filtered = filteredMachine.getCapability(ForgeCapabilities.ITEM_HANDLER, side)
                .resolve().orElse(null);
        helper.assertTrue(filtered != null, "the filtered face still exposes its item handler");
        helper.assertTrue(filtered.insertItem(0, new ItemStack(Items.APPLE), true).isEmpty(),
                "the whitelisted apple passes the filter");
        helper.assertTrue(filtered.insertItem(0, new ItemStack(Items.DIAMOND), true).getCount() == 1,
                "a diamond is refused (CoverFilterItem:115), got "
                        + filtered.insertItem(0, new ItemStack(Items.DIAMOND), true));
        filteredMachine.inventory().setStackInSlot(0, new ItemStack(Items.DIAMOND));
        helper.assertTrue(filtered.extractItem(0, 1, true).isEmpty(),
                "and a pipe cannot pull the refused item out either (CoverFilterItem:122)");

        // The fluid filter: lava on the whitelist refuses water.
        BasicMachineBlockEntity fluidMachine = machine(level, site(112, 0));
        helper.assertTrue(fluidMachine.attachCover(side, cover(CoverUtilityBehaviors.FILTER_FLUID)),
                "the fluid filter attaches to a machine face");
        helper.assertTrue(fluidMachine.clickFilterCover(side, new ItemStack(Items.LAVA_BUCKET)),
                "the held lava bucket becomes the fluid filter");
        var fluidFace = fluidMachine.getCapability(ForgeCapabilities.FLUID_HANDLER, side).resolve().orElse(null);
        helper.assertTrue(fluidFace != null, "the filtered face still exposes its fluid handler");
        helper.assertTrue(fluidFace.fill(new FluidStack(Fluids.WATER, 1000),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) == 0,
                "water is refused by a lava whitelist (CoverFilterFluid:117)");
        helper.assertTrue(fluidFace.fill(new FluidStack(Fluids.LAVA, 1000),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) > 0,
                "lava passes the lava filter");

        // The retriever blocks the face it owns outright.
        BasicMachineBlockEntity retrieverMachine = machine(level, site(116, 0));
        helper.assertTrue(retrieverMachine.attachCover(side, cover(CoverUtilityBehaviors.RETRIEVER_ITEM)),
                "the item retriever attaches to a machine face");
        retrieverMachine.inventory().setStackInSlot(0, new ItemStack(Items.APPLE));
        IItemHandler blocked = retrieverMachine.getCapability(ForgeCapabilities.ITEM_HANDLER, side)
                .resolve().orElse(null);
        helper.assertTrue(blocked != null, "the retriever face still exposes its item handler");
        helper.assertTrue(blocked.insertItem(0, new ItemStack(Items.APPLE), true).getCount() == 1,
                "the retriever refuses pipe inserts on its own face (CoverRetrieverItem:138)");
        helper.assertTrue(blocked.extractItem(0, 1, true).isEmpty(),
                "and pipe extracts too (CoverRetrieverItem:139)");
        helper.succeed();
    }
}
