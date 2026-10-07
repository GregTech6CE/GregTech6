package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.blockentity.CFoamBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.behavior.BehaviorDuctTape;
import com.gregtech.gregtech.item.behavior.BehaviorFlintAndTinder;
import com.gregtech.gregtech.item.behavior.BehaviorLighter;
import com.gregtech.gregtech.item.behavior.BehaviorPlungerFluid;
import com.gregtech.gregtech.item.behavior.BehaviorScanner;
import com.gregtech.gregtech.item.behavior.BehaviorSprayColorRemover;
import com.gregtech.gregtech.item.behavior.BehaviorSprayExtinguisher;
import com.gregtech.gregtech.item.behavior.BehaviorSprayFoamHardener;
import com.gregtech.gregtech.item.behavior.BehaviorSprayFoamRemover;
import com.gregtech.gregtech.item.behavior.ItemBehaviors;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * The port's GT6 item-behaviour layer ({@code com.gregtech.gregtech.item.behavior}) — one test per
 * behaviour, each with a positive case and at least one case where the behaviour must do nothing.
 *
 * <p>GT6 sources: {@code gregtech/items/behaviors/Behavior_*.java} in the original tree; every
 * concrete line number is quoted in the javadoc of the class under test.</p>
 *
 * <h2>Why these coordinates</h2>
 *
 * <p>{@link #BASE_X}/{@link #BASE_Z} are {@code 33000} and {@link #BASE_Y} is {@code 100}, the area
 * reserved for this batch; {@code grep -r "BASE_X" gametest} shows no other suite uses it. The
 * world behind a GameTest run is not wiped between runs, so every site is cleared — blocks
 * <em>and</em> entities — before it is used, which makes the file re-runnable.</p>
 *
 * <h2>Deterministic rolls</h2>
 *
 * <p>The flint and tinder and the lighter roll against a {@code RandomSource}. Instead of stubbing
 * the generator, the tests search for a seed whose <em>first</em> draw lands on the side of the
 * threshold they need ({@link #seedForRoll}) and then hand in
 * {@code RandomSource.create(thatSeed)}: the behaviour draws exactly once per click, so the seed
 * fully determines the outcome and the assertion message reports the roll it got.</p>
 *
 * <h2>Creative mode</h2>
 *
 * <p>{@link ItemBehaviors#creative} reads {@code Player.getAbilities().instabuild}, the 1.20.1 form
 * of GT6's {@code EntityPlayer.capabilities.isCreativeMode} and the same test
 * {@code GTToolHelper.isCreative} applies. The mock players of
 * {@link GameTestHelper#makeMockSurvivalPlayer()} leave {@code instabuild} at its default
 * {@code false}, which is what the consumption assertions need; the creative branch is then exercised
 * by setting the flag on the same player.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class ItemBehaviorTests {
    private static final int BASE_X = 33000;
    private static final int BASE_Y = 100;
    private static final int BASE_Z = 33000;

    /** One of this file's sites, {@code dx}/{@code dz} apart inside the reserved area. */
    private static BlockPos site(int dx, int dz) {
        return new BlockPos(BASE_X + dx, BASE_Y, BASE_Z + dz);
    }

    /** Wipes a site — blocks and entities — so a re-run starts from the same world state. */
    private static void clearSite(ServerLevel level, BlockPos pos, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    level.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box(pos, radius))) entity.discard();
    }

    private static AABB box(BlockPos pos, int radius) {
        return new AABB(pos.getX() - radius, pos.getY() - 1, pos.getZ() - radius,
                pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1);
    }

    /** A stone floor with one air block above it: the ordinary "click a block" site. */
    private static BlockPos floorAndAir(ServerLevel level, BlockPos pos) {
        clearSite(level, pos, 3);
        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        return pos;
    }

    /**
     * The first seed whose first {@code nextInt(bound)} lands on the requested side of the
     * threshold — the behaviours draw exactly once per click, so this pins the roll.
     */
    private static long seedForRoll(int bound, boolean below, int threshold) {
        for (long seed = 1L; seed < 1_000_000L; seed++) {
            int roll = RandomSource.create(seed).nextInt(bound);
            if (below == (roll < threshold)) return seed;
        }
        throw new IllegalStateException("no seed rolls " + (below ? "below " : "at or above ") + threshold
                + " out of " + bound);
    }

    /** The roll a seed produces, for the assertion messages. */
    private static int rollOf(long seed, int bound) {
        return RandomSource.create(seed).nextInt(bound);
    }

    private static Player survival(GameTestHelper helper) {
        return helper.makeMockSurvivalPlayer();
    }

    /**
     * Spawns an entity at an absolute position.
     *
     * <p>{@link GameTestHelper#spawnWithNoFreeWill} would run the position through
     * {@code absoluteVec}, i.e. it treats it as relative to the 1×1×1 {@code test_empty} template —
     * the same trap the brief warns about for blocks. This spawns through the level instead.</p>
     */
    private static <T extends Entity> T spawn(ServerLevel level, net.minecraft.world.entity.EntityType<T> type,
                                              BlockPos pos) {
        T entity = type.create(level);
        if (entity == null) throw new IllegalStateException("cannot create " + type);
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(entity);
        if (entity instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
        return entity;
    }

    /** A machine with one input tank, one output tank and an EU input, on its own site. */
    private static BasicMachineBlockEntity machine(ServerLevel level, BlockPos pos) {
        clearSite(level, pos, 2);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            level.setBlock(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
        }
        var block = MachineRegistry.basicMachines().get(0).get();
        level.setBlock(pos, block.defaultBlockState(), 3);
        var machine = (BasicMachineBlockEntity) level.getBlockEntity(pos);
        var recipes = new RecipeMap(null, "item_behavior_" + Long.toUnsignedString(pos.asLong()),
                "Item behaviour", "item_behavior", 1, 1, 1, 1, 1, 0, 1, false, false, false, false);
        machine.setSpec(BasicMachineSpec.builder("item_behavior_test", block.basicSpec().material())
                .machineType("test").energy(GregTechTags.Energy.EU, 32).recipes(recipes)
                .faces(com.gregtech.gregtech.api.energy.FaceConfig.ALL_SIDES).build());
        return machine;
    }

    // ── the behaviour table ──────────────────────────────────────────────

    /**
     * Every item the layer claims is really registered — the table is what an item class will use to
     * find its behaviour, so a typo in it would silently disable a whole family.
     */
    @GameTest(template = "test_empty")
    public static void everyPortedBehaviourNamesARegisteredItem(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        int behaviours = 0;
        for (ItemBehaviors.Entry entry : ItemBehaviors.PORTED) {
            if (entry.behaviour() == null) continue;
            behaviours++;
            if (!ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse("gregtech:" + entry.itemId()))) {
                missing.add(entry.itemId());
            }
        }
        helper.assertTrue(missing.isEmpty(),
                "every ported behaviour needs its item in the registry, missing: " + missing);
        helper.assertTrue(behaviours >= 30, "the table covers the batch's families, got " + behaviours);
        helper.assertTrue(ItemBehaviors.isPorted("duct_tape") && ItemBehaviors.isPorted("hardening_spray"),
                "duct_tape and hardening_spray are ported");
        helper.assertTrue(!ItemBehaviors.isPorted("spray_can_foam_red"),
                "a C-Foam colour spray is not registered in the port, so it must not be claimed");
        helper.succeed();
    }

    // ── the entry point an item class calls ─────────────────────────────

    /**
     * {@link ItemBehaviors#useOn} routes a click by the item's own registry path: an unrelated item is
     * refused, a tape roll is converted, a never-failing lighter lights a fire, a hardening spray
     * dries foam, and a scanner reports.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void dispatcherRoutesEachFamily(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = survival(helper);

        BlockPos pos = floorAndAir(level, site(192, 0));
        helper.assertFalse(ItemBehaviors.useOn(level, pos, Direction.UP, player,
                        new ItemStack(Blocks.STONE.asItem()), 0.5F, 0.5F, 0.5F).acted(),
                "an item without a behaviour is refused");
        helper.assertTrue(ItemBehaviors.itemId(ItemBehaviors.stack("gregtech:duct_tape")).equals("duct_tape"),
                "itemId() reports the registry path, got " + ItemBehaviors.itemId(ItemBehaviors.stack("gregtech:duct_tape")));

        ItemBehaviors.Outcome tape = ItemBehaviors.useOn(level, pos, Direction.UP, player,
                ItemBehaviors.stack("gregtech:duct_tape"), 0.5F, 0.5F, 0.5F);
        helper.assertTrue(tape.stack().is(ItemBehaviors.stack("gregtech:duct_tape_2").getItem()),
                "the dispatcher reaches the duct tape (Behavior_Duct_Tape:63-67), got " + tape.stack());

        ItemBehaviors.Outcome lit = ItemBehaviors.useOn(level, pos, Direction.UP, player,
                ItemBehaviors.stack("gregtech:lighter_full"), 0.5F, 0.5F, 0.5F);
        helper.assertTrue(lit.acted() && level.getBlockState(pos.above()).is(Blocks.FIRE),
                "the dispatcher reaches the Invar lighter, which never fails, got "
                        + level.getBlockState(pos.above()) + " and " + lit.stack());

        // The scanner runs and reports its cost; the port's scanner items have no energy buffer yet.
        List<String> ignored = new ArrayList<>();
        BehaviorScanner.scan(level, pos, Direction.UP, BehaviorScanner.PORTABLE_LEVEL, player, ignored);
        helper.assertFalse(ignored.isEmpty(), "the scanner produces lines");
        helper.assertTrue(ItemBehaviors.useOn(level, pos, Direction.UP, player,
                        ItemBehaviors.stack("gregtech:portable_scanner"), 0.5F, 0.5F, 0.5F).acted(),
                "the dispatcher reaches the portable scanner (Behavior_Scanner:54)");
        helper.assertTrue(ItemBehaviors.lastScanCost() == 0L,
                "a plain block's scan is free (WD.java:939 onwards only charges sections), got "
                        + ItemBehaviors.lastScanCost());
        helper.assertTrue(ItemBehaviors.lighterSpec("lighter").singleUse() == false
                        && ItemBehaviors.lighterSpec("match").singleUse()
                        && ItemBehaviors.lighterSpec("stick") == null,
                "the lighter family table separates the single-use items");

        BlockPos wet = site(204, 0);
        clearSite(level, wet, 3);
        level.setBlock(wet.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(wet, GTDecorBlocks.CFOAM_FRESH.get().defaultBlockState(), 3);
        helper.assertTrue(ItemBehaviors.useOn(level, wet, Direction.UP, player,
                        ItemBehaviors.stack("gregtech:hardening_spray"), 0.5F, 0.5F, 0.5F).acted(),
                "the dispatcher reaches the hardening spray");
        helper.assertTrue(level.getBlockState(wet).is(GTDecorBlocks.CFOAM.get()),
                "and the foam dried, got " + level.getBlockState(wet));
        helper.succeed();
    }

    /** {@link ItemBehaviors#useOnEntity}: only the tools that may light creepers do. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void dispatcherRoutesCreeperIgnition(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = floorAndAir(level, site(216, 0));
        Player player = survival(helper);

        ItemStack tinder = GTToolHelper.displayTool(GTToolType.FLINT_AND_TINDER);
        Creeper creeper = spawn(level, net.minecraft.world.entity.EntityType.CREEPER, pos.above());
        helper.assertTrue(ItemBehaviors.useOnEntity(creeper, player, tinder).acted(),
                "the flint and tinder lights a creeper (Behavior_FlintAndTinder:69-78)");
        helper.assertTrue(creeper.isIgnited(), "the creeper is lit");

        Creeper second = spawn(level, net.minecraft.world.entity.EntityType.CREEPER, pos.above().east());
        helper.assertFalse(ItemBehaviors.useOnEntity(second, player, ItemBehaviors.stack("gregtech:match")).acted(),
                "a single-use match cannot light a creeper (Behavior_Lighter:69)");
        helper.assertFalse(second.isIgnited(), "so the creeper stays unlit");

        Cow cow = spawn(level, net.minecraft.world.entity.EntityType.COW, pos.above().west());
        helper.assertFalse(ItemBehaviors.useOnEntity(cow, player, tinder).acted(),
                "a cow is not a creeper");

        creeper.discard();
        second.discard();
        cow.discard();
        helper.succeed();
    }

    // ── BehaviorScanner ─────────────────────────────────────────────────

    /**
     * GT6 {@code WD.java:921-936}: a block without a block entity is fully described and costs
     * nothing, because the original only charges for block-entity sections ({@code WD.java:939} on).
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void scannerDescribesAPlainBlockForFree(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = floorAndAir(level, site(0, 0));
        List<String> lines = new ArrayList<>();
        long cost = BehaviorScanner.scan(level, pos, Direction.UP, BehaviorScanner.PORTABLE_LEVEL, null, lines);

        helper.assertTrue(cost == 0L, "a block without a block entity costs nothing, got " + cost);
        helper.assertTrue(lines.contains("--- X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ()
                        + " ---"),
                "the header line is the original's (WD.java:921), got " + lines);
        helper.assertTrue(lines.stream().anyMatch(l -> l.equals("Registry: minecraft:stone")),
                "stone reports its registry name, got " + lines);
        helper.assertTrue(lines.stream().anyMatch(l -> l.startsWith("Hardness: 1.5")),
                "stone's hardness is 1.5, got " + lines);
        helper.assertTrue(lines.stream().anyMatch(l -> l.equals("Tool to Harvest: Pickaxe (0)")),
                "stone reports the harvest level GT6 reads, which is the raw one (WD.java:933) - vanilla "
                        + "stone is a level 0 pickaxe block, got " + lines);
        helper.assertTrue(!lines.stream().anyMatch(l -> l.startsWith("Block Class:")),
                "scan level " + BehaviorScanner.PORTABLE_LEVEL + " must not print classes (WD.java:925), got "
                        + lines);
        helper.succeed();
    }

    /**
     * GT6 {@code WD.java:987-1008}: the energy ranges and the tanks of a machine, each section
     * charged {@code CS.V[3] = 512} EU {@code (WD.java:939,988,1003}; the portable scanner's buffer is
     * exactly {@code V[3] * 8000} at {@code V[3]} per operation, {@code MultiItemRandomTools:517}).
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void scannerChargesPerSectionAndReportsTheMachine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(12, 0);
        BasicMachineBlockEntity machine = machine(level, pos);
        helper.assertTrue(machine.getTanksInput().length == 1 && machine.getTanksOutput().length == 1,
                "the test machine has one input and one output tank, got " + machine.getTanksInput().length
                        + "/" + machine.getTanksOutput().length);
        helper.assertTrue(!machine.getEnergyTypes(Direction.UP).isEmpty(),
                "the test machine has an energy type, got " + machine.getEnergyTypes(Direction.UP));

        List<String> lines = new ArrayList<>();
        long cost = BehaviorScanner.scan(level, pos, Direction.UP, BehaviorScanner.PORTABLE_LEVEL,
                survival(helper), lines);

        // GT6 prints the on/off, mode and running states as ONE line but charges once per part
        // (WD.java:959 State, :964 Mode, :973/977/981 Running), so the line is two charges here - the
        // machine reports State and Running. Energy (:988), tanks (:1003) and progress (:951) are one
        // charge each.
        boolean state = lines.stream().anyMatch(l -> l.contains("State: "));
        boolean running = lines.stream().anyMatch(l -> l.contains("Running: "));
        boolean mode = lines.stream().anyMatch(l -> l.contains("Mode: "));
        boolean energy = lines.stream().anyMatch(l -> l.startsWith("Input: "));
        boolean tanks = lines.stream().anyMatch(l -> l.startsWith("Tank 0: "));
        boolean progress = lines.stream().anyMatch(l -> l.startsWith("Progress: "));
        int sections = (state ? 1 : 0) + (running ? 1 : 0) + (mode ? 1 : 0)
                + (energy ? 1 : 0) + (tanks ? 1 : 0) + (progress ? 1 : 0);

        helper.assertTrue(state && running,
                "an idle machine still reports State and Running (WD.java:957-983), got " + lines);
        helper.assertTrue(energy, "the machine's energy range is reported (WD.java:987-993), got " + lines);
        helper.assertTrue(tanks, "both tanks are listed (WD.java:1002-1008), got " + lines);
        helper.assertTrue(sections >= 3, "at least the three sections above are reported, got " + sections
                + " in " + lines);
        helper.assertTrue(cost == sections * BehaviorScanner.COST_PER_SECTION,
                "GT6 charges V[3]=" + BehaviorScanner.COST_PER_SECTION + " per reported section, so "
                        + sections + " sections cost " + (sections * BehaviorScanner.COST_PER_SECTION)
                        + ", got " + cost + " for " + lines);
        helper.assertTrue(lines.stream().anyMatch(l -> l.endsWith("EU") && l.startsWith("Input: ")),
                "the energy unit is the tag's short name, got " + lines);
        helper.succeed();
    }

    /** GT6 {@code WD.java:925-928} and {@code Behavior_Scanner:62,71}: the two scan-level gates. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void scannerPrintsClassesOnlyAboveItsLevel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = floorAndAir(level, site(24, 0));

        List<String> low = new ArrayList<>();
        BehaviorScanner.scan(level, pos, Direction.UP, BehaviorScanner.PORTABLE_LEVEL, null, low);
        List<String> high = new ArrayList<>();
        BehaviorScanner.scan(level, pos, Direction.UP, BehaviorScanner.DEBUG_LEVEL, null, high);

        helper.assertTrue(BehaviorScanner.CLASS_LEVEL == 10, "GT6 prints classes from level 10 (WD.java:925)");
        helper.assertTrue(BehaviorScanner.ENTITY_LEVEL == 100, "GT6 reports entities above level 100 (:62)");
        helper.assertTrue(!low.stream().anyMatch(l -> l.startsWith("Block Class:")),
                "level " + BehaviorScanner.PORTABLE_LEVEL + " stays quiet, got " + low);
        helper.assertTrue(high.stream().anyMatch(l -> l.startsWith("Block Class: ")),
                "level " + BehaviorScanner.DEBUG_LEVEL + " prints the class, got " + high);

        Cow cow = spawn(level, net.minecraft.world.entity.EntityType.COW, pos.above());
        helper.assertTrue(BehaviorScanner.scanEntity(cow, BehaviorScanner.PORTABLE_LEVEL).isEmpty(),
                "a scanner at level " + BehaviorScanner.PORTABLE_LEVEL + " does not report entities");
        helper.assertTrue(BehaviorScanner.scanEntity(cow, BehaviorScanner.DEBUG_LEVEL).orElse("")
                        .equals(Cow.class.getName()),
                "the debug scanner reports the entity class, got "
                        + BehaviorScanner.scanEntity(cow, BehaviorScanner.DEBUG_LEVEL));
        cow.discard();

        // LH.java:272 thresholds, without the colour codes.
        helper.assertTrue(BehaviorScanner.blastResistance(3.9).equals("Blast Resistance: 3.9 (Terrible)"),
                "got " + BehaviorScanner.blastResistance(3.9));
        helper.assertTrue(BehaviorScanner.blastResistance(4.0).endsWith("(Ghast Proof)"),
                "4.0 is ghast proof, got " + BehaviorScanner.blastResistance(4.0));
        helper.assertTrue(BehaviorScanner.blastResistance(12.0).endsWith("(Creeper Proof)"),
                "12.0 is creeper proof, got " + BehaviorScanner.blastResistance(12.0));
        helper.assertTrue(BehaviorScanner.blastResistance(16.0).endsWith("(TNT Proof)")
                        && BehaviorScanner.blastResistance(40.0).endsWith("(TNT Proof)"),
                "16.0 to 40.0 is TNT proof");
        helper.assertTrue(BehaviorScanner.blastResistance(41.0).endsWith("(Strong Dynamite Proof)"),
                "above 40.0 is dynamite proof, got " + BehaviorScanner.blastResistance(41.0));
        helper.succeed();
    }

    // ── BehaviorDuctTape ────────────────────────────────────────────────

    /**
     * GT6 {@code Behavior_Duct_Tape:63-87}: a full roll becomes the used roll, a target that takes
     * tape subtracts its uses, and the roll is consumed when the counter runs out.
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void ductTapeConvertsAndSpendsUses(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = floorAndAir(level, site(36, 0));
        Player player = survival(helper);

        ItemStack roll = new ItemStack(ForgeRegistries.ITEMS.getValue(
                ResourceLocation.parse("gregtech:duct_tape")));
        ItemStack usedRoll = new ItemStack(ForgeRegistries.ITEMS.getValue(
                ResourceLocation.parse("gregtech:duct_tape_2")));
        helper.assertTrue(!roll.isEmpty() && !usedRoll.isEmpty(),
                "both duct tape variants are registered, got " + roll + " / " + usedRoll);

        // GT6 :92 - a face the player cannot reach is never taped, and the roll is not spent.
        long[] calls = {0};
        BehaviorDuctTape.Tapeable target = (lvl, p, side, who, tape, uses, quality, sneaking, hx, hy, hz) -> {
            calls[0]++;
            return 250L;
        };
        level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(ItemBehaviors.obstructed(level, pos, Direction.UP),
                "a stone block above makes the up face obstructed (WD.java:127-148)");
        long obstructed = BehaviorDuctTape.tape(level, pos, Direction.UP, target, player, roll,
                BehaviorDuctTape.DUCT_TAPE_USES, BehaviorDuctTape.DUCT_TAPE_QUALITY, false, 0.5F, 0.5F, 0.5F);
        helper.assertTrue(obstructed == 0L && calls[0] == 0,
                "an obstructed face consumes nothing and is not offered to the target, got " + obstructed
                        + " after " + calls[0] + " calls");
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);

        // The full roll turns into the used one and starts at mUses (:63-67), then 250 uses go (:69-74).
        // The explicit target isolates tape bookkeeping from Mass Storage's own sealing behavior.
        ItemBehaviors.Outcome outcome = BehaviorDuctTape.useOn(level, pos, Direction.UP, target, player, roll,
                0.5F, 0.5F, 0.5F);
        helper.assertTrue(outcome.acted(), "a reachable block with a target takes tape");
        helper.assertTrue(calls[0] == 1 && outcome.stack().is(usedRoll.getItem()),
                "a full roll becomes its used roll (:64-66), got " + outcome.stack());
        helper.assertTrue(BehaviorDuctTape.DUCT_TAPE_USES == 100000L,
                "GT6 registers the duct tape with 100000 uses (MultiItemRandomTools:568)");
        helper.assertTrue(outcome.stack().getTag() != null
                        && outcome.stack().getTag().getLong("gt.remaining") == 100000L - 250L,
                "the counter is 100000 - 250 (Behavior_Duct_Tape:72,77), got "
                        + (outcome.stack().getTag() == null ? "no tag" : outcome.stack().getTag().getLong("gt.remaining")));

        // A stack that is not an unstacked roll is refused before anything happens (:55).
        ItemStack stacked = new ItemStack(roll.getItem(), 2);
        int callsBefore = (int) calls[0];
        ItemBehaviors.Outcome refused = BehaviorDuctTape.useOn(level, pos, Direction.UP, target, player, stacked,
                0.5F, 0.5F, 0.5F);
        helper.assertFalse(refused.acted(), "a stacked roll is refused");
        helper.assertTrue(refused.stack().getCount() == 2 && !refused.stack().hasTag()
                        && calls[0] == callsBefore,
                "a refused click leaves the stack and the target alone, got " + refused.stack() + " and "
                        + (calls[0] - callsBefore) + " target calls");

        // The last use empties the roll (:80-87).
        ItemStack almost = new ItemStack(usedRoll.getItem());
        almost.getOrCreateTag().putLong("gt.remaining", 100L);
        ItemBehaviors.Outcome last = BehaviorDuctTape.useOn(level, pos, Direction.UP, target, player, almost,
                0.5F, 0.5F, 0.5F);
        helper.assertTrue(last.stack().isEmpty(),
                "GT6's tapes have no empty item, so the last use consumes the stack (Behavior_Duct_Tape:81-82), got "
                        + last.stack());

        // Stone is not tapeable, so an ordinary world click spends nothing.
        ItemStack unused = new ItemStack(roll.getItem());
        ItemBehaviors.Outcome nothing = BehaviorDuctTape.useOn(level, pos, Direction.UP, player, unused,
                0.5F, 0.5F, 0.5F);
        helper.assertFalse(nothing.acted(),
                "a non-tapeable stone block refuses a world click");
        helper.assertTrue(nothing.stack().is(usedRoll.getItem())
                        && nothing.stack().getTag() != null
                        && nothing.stack().getTag().getLong("gt.remaining") == BehaviorDuctTape.DUCT_TAPE_USES,
                "but the full to used conversion still happened (:63-67,76-78), got " + nothing.stack());
        helper.assertTrue(BehaviorDuctTape.consumableOf(new ItemStack(Blocks.STONE.asItem())) == null,
                "a stone is not a tape roll");
        helper.succeed();
    }

    // ── BehaviorPlungerFluid ────────────────────────────────────────────

    /**
     * GT6 {@code Behavior_Plunger_Fluid:53-56}: 1000 mB out of the first tank face that has fluid, at
     * {@code mCosts} durability ({@code GT_Tool_Plunger:87} → {@code ToolStats:64} = 100), and a tool
     * that cannot pay leaves the tank alone.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void plungerEmptiesTheTankAndCostsDurability(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(48, 0);
        BasicMachineBlockEntity machine = machine(level, pos);
        Player player = survival(helper);

        var tank = machine.getTanksOutput()[0];
        tank.setFluid(new FluidStack(Fluids.WATER, 1000));
        helper.assertTrue(tank.getAmount() == 1000, "the output tank holds 1000 mB, got " + tank.getAmount());

        ItemStack plunger = GTToolHelper.displayTool(GTToolType.PLUNGER);
        int maxDamage = GTToolHelper.getMaxDurability(plunger);
        helper.assertTrue(maxDamage > 2 * BehaviorPlungerFluid.COSTS,
                "the test plunger has room for a click or two, got max durability " + maxDamage);
        player.setItemInHand(InteractionHand.MAIN_HAND, plunger);

        helper.assertTrue(machine.getTanksInput().length == 1,
                "the machine has an input tank as well, got " + machine.getTanksInput().length);
        boolean drained = BehaviorPlungerFluid.plungeFluid(level, pos, player, plunger,
                BehaviorPlungerFluid.COSTS);

        helper.assertTrue(drained, "the plunger drains a tank that has fluid (Behavior_Plunger_Fluid:53-57)");
        helper.assertTrue(tank.isEmpty(), "1000 mB is GT6's drain amount, tank left with " + tank.getAmount());
        helper.assertTrue(plunger.getDamageValue() == BehaviorPlungerFluid.COSTS,
                "the click costs mCosts=" + BehaviorPlungerFluid.COSTS + " durability, got "
                        + plunger.getDamageValue());

        // An empty tank, a block without a tank, and a broken tool all do nothing.
        helper.assertFalse(BehaviorPlungerFluid.plungeFluid(level, pos, player, plunger,
                        BehaviorPlungerFluid.COSTS),
                "an empty tank is nothing to drain");
        helper.assertTrue(plunger.getDamageValue() == BehaviorPlungerFluid.COSTS,
                "a click that drained nothing damages nothing, got " + plunger.getDamageValue());

        BlockPos stone = floorAndAir(level, site(60, 0));
        helper.assertFalse(BehaviorPlungerFluid.plungeFluid(level, stone, player, plunger,
                        BehaviorPlungerFluid.COSTS),
                "a block without a fluid handler is refused (Behavior_Plunger_Fluid:52)");

        // Creative never pays (MultiItemTool:434), a broken tool cannot (:435).
        tank.setFluid(new FluidStack(Fluids.WATER, 1000));
        player.getAbilities().instabuild = true;
        helper.assertTrue(BehaviorPlungerFluid.plungeFluid(level, pos, player, plunger,
                        BehaviorPlungerFluid.COSTS),
                "creative still plunges");
        helper.assertTrue(plunger.getDamageValue() == BehaviorPlungerFluid.COSTS,
                "creative pays no durability (MultiItemTool:434), got " + plunger.getDamageValue());
        player.getAbilities().instabuild = false;

        ItemStack broken = plunger.copy();
        broken.setDamageValue(GTToolHelper.getMaxDurability(broken));
        tank.setFluid(new FluidStack(Fluids.WATER, 1000));
        helper.assertFalse(BehaviorPlungerFluid.plungeFluid(level, pos, player, broken,
                        BehaviorPlungerFluid.COSTS),
                "a used-up plunger cannot pay (MultiItemTool:435)");
        helper.assertTrue(tank.getAmount() == 1000,
                "and the tank keeps its fluid, got " + tank.getAmount());
        helper.succeed();
    }

    // ── BehaviorFlintAndTinder ──────────────────────────────────────────

    /** GT6 {@code Behavior_FlintAndTinder:49-55}: the three chances and the {@code bind(1,100,·)} clamp. */
    @GameTest(template = "test_empty")
    public static void flintAndTinderChancesMatchTheOriginal(GameTestHelper helper) {
        helper.assertTrue(BehaviorFlintAndTinder.DEFAULT_FLINT_CHANCE == 30,
                "GT6's default FlintAndSteelChance is 30 (GT_Proxy:95, GT6_Main:111), got "
                        + BehaviorFlintAndTinder.DEFAULT_FLINT_CHANCE);
        helper.assertTrue(BehaviorFlintAndTinder.ignitionChance(1, false, 30) == 100,
                "a durability-1 material always ignites (:49-50), got "
                        + BehaviorFlintAndTinder.ignitionChance(1, false, 30));
        helper.assertTrue(BehaviorFlintAndTinder.ignitionChance(0, false, 30) == 100,
                "the port's 'no tool stats' value is 0 and takes the same branch, got "
                        + BehaviorFlintAndTinder.ignitionChance(0, false, 30));
        helper.assertTrue(BehaviorFlintAndTinder.ignitionChance(512, true, 30) == 65,
                "a flammable material gets 30 + (100-30)/2 (:51-52), got "
                        + BehaviorFlintAndTinder.ignitionChance(512, true, 30));
        helper.assertTrue(BehaviorFlintAndTinder.ignitionChance(512, false, 30) == 30,
                "a plain material gets the configured chance (:53-54), got "
                        + BehaviorFlintAndTinder.ignitionChance(512, false, 30));
        helper.assertTrue(BehaviorFlintAndTinder.ignitionChance(512, false, 0) == 1
                        && BehaviorFlintAndTinder.ignitionChance(512, true, 100) == 100,
                "UT.Code.bind(1,100,·) clamps, got "
                        + BehaviorFlintAndTinder.ignitionChance(512, false, 0) + " and "
                        + BehaviorFlintAndTinder.ignitionChance(512, true, 100));
        helper.succeed();
    }

    /**
     * GT6 {@code Behavior_FlintAndTinder:46-60}: a successful roll lights a fire in front of the
     * face, an obstructed face is refused, and a failed roll still costs 100 durability.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void flintAndTinderLightsFireAndBurnsDurability(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = floorAndAir(level, site(72, 0));
        Player player = survival(helper);
        ItemStack tinder = GTToolHelper.displayTool(GTToolType.FLINT_AND_TINDER);
        player.setItemInHand(InteractionHand.MAIN_HAND, tinder);

        long success = seedForRoll(100, true, BehaviorFlintAndTinder.DEFAULT_FLINT_CHANCE);
        ItemBehaviors.Outcome lit = BehaviorFlintAndTinder.useOn(level, pos, Direction.UP, player, tinder,
                BehaviorFlintAndTinder.DEFAULT_FLINT_CHANCE, RandomSource.create(success), 0.5F, 0.5F, 0.5F);
        helper.assertTrue(lit.acted(), "a click past the gate reports true (:60)");
        helper.assertTrue(level.getBlockState(pos.above()).is(Blocks.FIRE),
                "the roll " + rollOf(success, 100) + " of 100 is below the chance, so a fire is lit in front "
                        + "(ToolCompat:214-222), got " + level.getBlockState(pos.above()));
        helper.assertTrue(tinder.getDamageValue() == 100,
                "GT6's units(max(10000, tDamage), 10000, 100, T) is 100 durability (:58), got "
                        + tinder.getDamageValue());

        // An obstructed face is the one branch that returns F (:46).
        level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
        int before = tinder.getDamageValue();
        ItemBehaviors.Outcome blocked = BehaviorFlintAndTinder.useOn(level, pos, Direction.UP, player, tinder,
                BehaviorFlintAndTinder.DEFAULT_FLINT_CHANCE, RandomSource.create(success), 0.5F, 0.5F, 0.5F);
        helper.assertFalse(blocked.acted(), "an obstructed face makes the click fail (:46)");
        helper.assertTrue(tinder.getDamageValue() == before,
                "and it costs nothing, got " + tinder.getDamageValue() + " after " + before);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);

        // A failed roll lights nothing but still costs the same 100 durability (:48,58).
        long failure = seedForRoll(100, false, BehaviorFlintAndTinder.DEFAULT_FLINT_CHANCE);
        before = tinder.getDamageValue();
        ItemBehaviors.Outcome missed = BehaviorFlintAndTinder.useOn(level, pos, Direction.UP, player, tinder,
                BehaviorFlintAndTinder.DEFAULT_FLINT_CHANCE, RandomSource.create(failure), 0.5F, 0.5F, 0.5F);
        helper.assertTrue(missed.acted(), "GT6 reports true even when the roll failed (:60)");
        helper.assertTrue(!level.getBlockState(pos.above()).is(Blocks.FIRE),
                "roll " + rollOf(failure, 100) + " is not below 30, so no fire, got " + level.getBlockState(pos.above()));
        helper.assertTrue(tinder.getDamageValue() == before + 100,
                "the failed click still costs 100 durability (:48,58), got " + tinder.getDamageValue());

        // Creepers light, everything else does not (:69-78).
        Creeper creeper = spawn(level, net.minecraft.world.entity.EntityType.CREEPER, pos.above());
        helper.assertTrue(BehaviorFlintAndTinder.igniteCreeper(creeper, tinder, player),
                "a creeper is lit (:72-75)");
        helper.assertTrue(creeper.isIgnited(), "Creeper.func_146079_cb() is 1.20.1's ignite() (:74)");
        Cow cow = spawn(level, net.minecraft.world.entity.EntityType.COW, pos.above());
        helper.assertFalse(BehaviorFlintAndTinder.igniteCreeper(cow, tinder, player),
                "a cow is not a creeper (:71)");
        creeper.discard();
        cow.discard();
        helper.succeed();
    }

    // ── BehaviorLighter ─────────────────────────────────────────────────

    /**
     * GT6 {@code Behavior_Lighter:82-120}: the multi-use lighter burns one fuel unit per attempt, a
     * failed roll included, and the single-use match is consumed either way.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void lighterBurnsFuelAndConsumesAMatch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = floorAndAir(level, site(84, 0));
        Player player = survival(helper);

        BehaviorLighter.LighterSpec invar = BehaviorLighter.invar();
        helper.assertTrue(invar.fuelAmount() == 100 && invar.chance() == 10000,
                "the Invar lighter has 100 uses and never fails (MultiItemRandomTools:368), got "
                        + invar.fuelAmount() + " at " + invar.chance());
        helper.assertTrue(BehaviorLighter.platinum().fuelAmount() == 1000
                        && BehaviorLighter.plastic().chance() == 9000,
                "platinum holds 1000 (:377), plastic fails one in ten (:387)");

        ItemStack full = ItemBehaviors.stack("gregtech:lighter_full");
        helper.assertTrue(!full.isEmpty(), "the full Invar lighter is registered, got " + full);
        ItemBehaviors.Outcome used = BehaviorLighter.useOn(level, pos, Direction.UP, player, full, invar,
                RandomSource.create(seedForRoll(10000, true, 10000)), 0.5F, 0.5F, 0.5F);

        helper.assertTrue(used.acted(), "a lighter that never fails always acts");
        helper.assertTrue(used.stack().is(ItemBehaviors.stack("gregtech:lighter").getItem()),
                "a full lighter becomes the used one (Behavior_Lighter:122-128), got " + used.stack());
        helper.assertTrue(level.getBlockState(pos.above()).is(Blocks.FIRE),
                "the click lights a fire in front, got " + level.getBlockState(pos.above()));
        helper.assertTrue(used.stack().getTag() != null
                        && used.stack().getTag().getLong("gt.lighter") == 99L,
                "the fuel is 100 - units(10000, 10000, 1, T) = 99 (:112-113), got "
                        + (used.stack().getTag() == null ? "no tag" : used.stack().getTag().getLong("gt.lighter")));

        // The last fuel unit empties the lighter into its empty variant (:114,130-137).
        ItemStack last = ItemBehaviors.stack("gregtech:lighter");
        last.getOrCreateTag().putLong("gt.lighter", 1L);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        ItemBehaviors.Outcome empty = BehaviorLighter.useOn(level, pos, Direction.UP, player, last, invar,
                RandomSource.create(seedForRoll(10000, true, 10000)), 0.5F, 0.5F, 0.5F);
        helper.assertTrue(empty.stack().is(ItemBehaviors.stack("gregtech:lighter_empty").getItem()),
                "the last use turns it into the empty lighter (:130-136), got " + empty.stack());

        // A stacked multi-use lighter is refused (:83).
        ItemStack stacked = new ItemStack(full.getItem(), 2);
        helper.assertFalse(BehaviorLighter.useOn(level, pos, Direction.UP, player, stacked, invar,
                        RandomSource.create(seedForRoll(10000, true, 10000)), 0.5F, 0.5F, 0.5F).acted(),
                "a stacked lighter is not usable (Behavior_Lighter:83)");

        // The single-use match: a failed roll consumes it without lighting anything (:94-98).
        BehaviorLighter.LighterSpec match = BehaviorLighter.match();
        helper.assertTrue(match.singleUse() && match.fuelAmount() == 1,
                "the match is the single-use shape (MultiItemRandomTools:343)");
        long failure = seedForRoll(10000, false, 9000);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        ItemStack matchStack = ItemBehaviors.stack("gregtech:match");
        ItemBehaviors.Outcome burned = BehaviorLighter.useOn(level, pos, Direction.UP, player, matchStack, match,
                RandomSource.create(failure), 0.5F, 0.5F, 0.5F);
        helper.assertTrue(burned.stack().isEmpty(),
                "roll " + rollOf(failure, 10000) + " fails the 90% roll, and a match that fails is still used up "
                        + "(:94-98), got " + burned.stack());
        helper.assertTrue(!level.getBlockState(pos.above()).is(Blocks.FIRE),
                "and nothing was lit, got " + level.getBlockState(pos.above()));

        // A successful match on a block with no air in front costs nothing at all (:96).
        level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
        ItemStack kept = ItemBehaviors.stack("gregtech:match");
        ItemBehaviors.Outcome nothing = BehaviorLighter.useOn(level, pos, Direction.UP, player, kept, match,
                RandomSource.create(seedForRoll(10000, true, 9000)), 0.5F, 0.5F, 0.5F);
        helper.assertFalse(nothing.acted(),
                "a successful roll that found nothing to ignite returns F (:96)");
        helper.assertTrue(nothing.stack().getCount() == 1,
                "so the match survives, got " + nothing.stack().getCount());

        // Creative burns no fuel (:111).
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        ItemStack creativeStack = ItemBehaviors.stack("gregtech:lighter");
        creativeStack.getOrCreateTag().putLong("gt.lighter", 50L);
        player.getAbilities().instabuild = true;
        ItemBehaviors.Outcome free = BehaviorLighter.useOn(level, pos, Direction.UP, player, creativeStack, invar,
                RandomSource.create(seedForRoll(10000, true, 10000)), 0.5F, 0.5F, 0.5F);
        player.getAbilities().instabuild = false;
        helper.assertTrue(free.stack().getTag() != null && free.stack().getTag().getLong("gt.lighter") == 50L,
                "creative keeps the fuel (Behavior_Lighter:111), got "
                        + (free.stack().getTag() == null ? "no tag" : free.stack().getTag().getLong("gt.lighter")));
        helper.succeed();
    }

    // ── the foam sprays ─────────────────────────────────────────────────

    /**
     * GT6 {@code Behavior_Spray_Foam_Hardener:95-101} and {@code Behavior_Spray_Foam_Remover:95-101}:
     * the hardener dries wet C-Foam (10 uses), the remover scrapes either variant away (10 uses), and
     * neither touches a block that is not foam.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void foamSpraysHardenAndRemoveCFoam(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = survival(helper);

        BlockPos wet = site(96, 0);
        clearSite(level, wet, 3);
        level.setBlock(wet.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(wet, GTDecorBlocks.CFOAM_FRESH.get().defaultBlockState(), 3);
        helper.assertTrue(level.getBlockEntity(wet) instanceof CFoamBlockEntity,
                "wet foam owns its block entity, got " + level.getBlockEntity(wet));

        ItemStack hardener = ItemBehaviors.stack("gregtech:hardening_spray");
        ItemBehaviors.Outcome hardened = BehaviorSprayFoamHardener.useOn(level, wet, Direction.UP, player,
                hardener, 0.5F, 0.5F, 0.5F);
        helper.assertTrue(hardened.acted(), "the hardening spray dries wet foam (BlockCFoamFresh:110-112)");
        helper.assertTrue(level.getBlockState(wet).is(GTDecorBlocks.CFOAM.get()),
                "the wet block became the hardened one, got " + level.getBlockState(wet));
        helper.assertTrue(hardened.stack().is(ItemBehaviors.stack("gregtech:hardening_spray_2").getItem()),
                "a full can becomes its used can, got " + hardened.stack());
        long remaining = hardened.stack().getTag() == null ? -1
                : hardened.stack().getTag().getLong("gt.remaining");
        helper.assertTrue(remaining == BehaviorSprayFoamHardener.USES * BehaviorSprayFoamHardener.USES_MULTIPLIER
                        - BehaviorSprayFoamHardener.HARDEN_COST,
                "256 * 10 uses minus the 10 this one cost (Behavior_Spray_Foam_Hardener:101), got " + remaining);

        // Already hardened: dryFoam answers F (BlockCFoam:57-60), so nothing happens.
        ItemStack can = ItemBehaviors.stack("gregtech:hardening_spray_2");
        can.getOrCreateTag().putLong("gt.remaining", 100L);
        helper.assertFalse(BehaviorSprayFoamHardener.useOn(level, wet, Direction.UP, player, can,
                0.5F, 0.5F, 0.5F).acted(), "hardened foam cannot be hardened again");
        helper.assertTrue(can.getTag().getLong("gt.remaining") == 100L,
                "and the can keeps its uses, got " + can.getTag().getLong("gt.remaining"));

        // The remover takes the hardened block away (BlockCFoam:63-65).
        ItemStack remover = ItemBehaviors.stack("gregtech:c_foam_removal_spray");
        ItemBehaviors.Outcome removed = BehaviorSprayFoamRemover.useOn(level, wet, Direction.UP, player,
                remover, 0.5F, 0.5F, 0.5F);
        helper.assertTrue(removed.acted(), "the C-Foam removal spray removes hardened foam too");
        helper.assertTrue(level.getBlockState(wet).isAir(),
                "GT6's removeFoam sets the block to air, got " + level.getBlockState(wet));
        long left = removed.stack().getTag() == null ? -1 : removed.stack().getTag().getLong("gt.remaining");
        helper.assertTrue(left == BehaviorSprayFoamRemover.USES * BehaviorSprayFoamRemover.USES_MULTIPLIER
                        - BehaviorSprayFoamRemover.REMOVE_COST,
                "2560 - " + BehaviorSprayFoamRemover.REMOVE_COST + " = 2550 (Behavior_Spray_Foam_Remover:101), got "
                        + left);

        // Neither spray does anything to a block that is not C-Foam.
        BlockPos stone = floorAndAir(level, site(108, 0));
        ItemStack remover2 = ItemBehaviors.stack("gregtech:c_foam_removal_spray_2");
        remover2.getOrCreateTag().putLong("gt.remaining", 100L);
        helper.assertFalse(BehaviorSprayFoamRemover.useOn(level, stone, Direction.UP, player, remover2,
                0.5F, 0.5F, 0.5F).acted(), "stone is not foam (Behavior_Spray_Foam_Remover:101)");
        ItemStack hardener2 = ItemBehaviors.stack("gregtech:hardening_spray_2");
        hardener2.getOrCreateTag().putLong("gt.remaining", 100L);
        helper.assertFalse(BehaviorSprayFoamHardener.useOn(level, stone, Direction.UP, player, hardener2,
                0.5F, 0.5F, 0.5F).acted(), "stone has no block entity to dry (:99)");
        helper.assertTrue(remover2.getTag().getLong("gt.remaining") == 100L
                        && hardener2.getTag().getLong("gt.remaining") == 100L,
                "refused clicks spend nothing, got " + remover2.getTag().getLong("gt.remaining") + " and "
                        + hardener2.getTag().getLong("gt.remaining"));

        // A stacked can is refused (:59).
        ItemStack stacked = new ItemStack(ItemBehaviors.stack("gregtech:c_foam_removal_spray").getItem(), 2);
        helper.assertFalse(BehaviorSprayFoamRemover.useOn(level, stone, Direction.UP, player, stacked,
                0.5F, 0.5F, 0.5F).acted(), "a stacked spray can is refused (Behavior_Spray_Foam_Remover:59)");
        helper.assertTrue(BehaviorSprayFoamRemover.SLAB_REMOVE_COST == 5
                        && BehaviorSprayFoamHardener.SLAB_HARDEN_COST == 5,
                "GT6 charges 5 for a C-Foam slab; the port registers no foam slabs");
        helper.succeed();
    }

    // ── BehaviorSprayExtinguisher ───────────────────────────────────────

    /**
     * GT6 {@code Behavior_Spray_Extinguisher:103-129}: a 3x3x3 of fire in front of the face, 10 uses
     * each, and burning entities doused for the same 10.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void extinguisherDousesFireAndEntities(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = floorAndAir(level, site(120, 0));
        Player player = survival(helper);
        BlockPos front = pos.above();

        // A 3x3 floor so every fire has something to stand on, and three fires at pairwise
        // NON-adjacent spots inside the scanned 3x3x3. Removing one fire makes an adjacent fire
        // vanish on its own (vanilla FireBlock.neighborChanged), and GT6's
        // aWorld.setBlock(..., 3) cascades exactly the same way - so only a diagonal layout actually
        // measures the "10 uses per fire" rule of Behavior_Spray_Extinguisher:113.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(pos.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        BlockPos[] fires = {front.offset(-1, 0, -1), front.offset(1, 0, 1), front.offset(-1, 0, 1)};
        for (BlockPos fire : fires) level.setBlock(fire, Blocks.FIRE.defaultBlockState(), 3);

        ItemStack can = ItemBehaviors.stack("gregtech:fire_extinguisher_co2");
        ItemBehaviors.Outcome out = BehaviorSprayExtinguisher.useOn(level, pos, Direction.UP, player, can,
                0.5F, 0.5F, 0.5F);

        helper.assertTrue(out.acted(), "the extinguisher puts out the fires it can reach");
        StringBuilder leftOver = new StringBuilder();
        for (BlockPos fire : fires) {
            if (!level.getBlockState(fire).isAir()) leftOver.append(fire).append(' ');
        }
        helper.assertTrue(leftOver.length() == 0, "all three fires are gone, left " + leftOver);
        long remaining = out.stack().getTag() == null ? -1 : out.stack().getTag().getLong("gt.remaining");
        helper.assertTrue(remaining == BehaviorSprayExtinguisher.USES * BehaviorSprayExtinguisher.USES_MULTIPLIER
                        - 3 * BehaviorSprayExtinguisher.EXTINGUISH_COST,
                "three fires cost three times 10 (Behavior_Spray_Extinguisher:113), got " + remaining);

        // A burning entity in the same box is doused for another 10; a non-burning one costs nothing.
        // Driven through extinguishEntity, which is the same call the area scan makes: the area scan
        // itself collects its entities with Level.getEntitiesOfClass, and that query does not see
        // entities in a chunk this test merely setBlock()ed into (PersistentEntitySectionManager:47,
        // EntitySectionStorage:55 - PileBlockTests:643-651).
        Creeper burning = spawn(level, net.minecraft.world.entity.EntityType.CREEPER, front);
        burning.setRemainingFireTicks(200);
        Cow calm = spawn(level, net.minecraft.world.entity.EntityType.COW, front.east());
        helper.assertTrue(!calm.isOnFire(), "the cow is not on fire");

        ItemStack can2 = ItemBehaviors.stack("gregtech:fire_extinguisher_co2_2");
        can2.getOrCreateTag().putLong("gt.remaining", 100L);
        long spent = BehaviorSprayExtinguisher.extinguishEntity(level, burning, player);
        helper.assertTrue(spent == BehaviorSprayExtinguisher.EXTINGUISH_COST,
                "only the burning creeper costs 10 (Behavior_Spray_Extinguisher:122-125), got " + spent);
        helper.assertTrue(!burning.isOnFire(), "the creeper was doused, burning ticks "
                + burning.getRemainingFireTicks());
        helper.assertTrue(BehaviorSprayExtinguisher.extinguishEntity(level, calm, player) == 0L,
                "a cow that is not on fire costs nothing");

        // An obstruction blocks the block hook, and a can with fewer than 10 uses cannot area-spray.
        level.setBlock(front, Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(ItemBehaviors.obstructed(level, pos, Direction.UP),
                "stone above makes the up face obstructed");
        helper.assertTrue(BehaviorSprayExtinguisher.extinguish(level, pos, Direction.UP, 100L, player, can2,
                0.5F, 0.5F, 0.5F) == 0L,
                "no fire left in the volume, so nothing is spent");
        helper.assertTrue(BehaviorSprayExtinguisher.extinguish(level, pos, Direction.UP,
                        BehaviorSprayExtinguisher.AREA_MIN_USES - 1, player, can2, 0.5F, 0.5F, 0.5F) == 0L,
                "the area half needs at least " + BehaviorSprayExtinguisher.AREA_MIN_USES
                        + " uses (Behavior_Spray_Extinguisher:103)");
        helper.assertTrue(BehaviorSprayExtinguisher.HOOK_MAX_COST == 10,
                "the block hook's answer is min(10, tDamage / 1000) (:100)");

        burning.discard();
        calm.discard();
        helper.succeed();
    }

    // ── BehaviorSprayColorRemover ───────────────────────────────────────

    /**
     * GT6 {@code Behavior_Spray_Color_Remover:96-106}: stained glass, stained panes and coloured
     * terracotta lose their colour for 10 uses; everything else is untouched.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void colorRemoverDecolorisesVanillaBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = survival(helper);

        BlockPos glass = site(132, 0);
        clearSite(level, glass, 3);
        level.setBlock(glass, Blocks.RED_STAINED_GLASS.defaultBlockState(), 3);
        ItemBehaviors.Outcome cleared = BehaviorSprayColorRemover.useOn(level, glass, Direction.UP, player,
                ItemBehaviors.stack("gregtech:paint_removal_spray"), 0.5F, 0.5F, 0.5F);
        helper.assertTrue(cleared.acted(), "stained glass is decolorised (Behavior_Spray_Color_Remover:103)");
        helper.assertTrue(level.getBlockState(glass).is(Blocks.GLASS),
                "the replacement is plain glass, got " + level.getBlockState(glass));
        long remaining = cleared.stack().getTag() == null ? -1
                : cleared.stack().getTag().getLong("gt.remaining");
        helper.assertTrue(remaining == BehaviorSprayColorRemover.USES * BehaviorSprayColorRemover.USES_MULTIPLIER
                        - BehaviorSprayColorRemover.REMOVE_COST,
                "2560 - 10 = 2550 uses (:77), got " + remaining);

        BlockPos pane = site(144, 0);
        clearSite(level, pane, 3);
        level.setBlock(pane, Blocks.LIME_STAINED_GLASS_PANE.defaultBlockState(), 3);
        helper.assertTrue(BehaviorSprayColorRemover.decolorize(level, pane, Direction.UP, 100L, player,
                ItemStack.EMPTY, 0.5F, 0.5F, 0.5F) == BehaviorSprayColorRemover.REMOVE_COST,
                "a stained pane costs the same 10 uses (:102)");
        helper.assertTrue(level.getBlockState(pane).is(Blocks.GLASS_PANE),
                "the replacement is a plain pane, got " + level.getBlockState(pane));

        BlockPos clay = site(156, 0);
        clearSite(level, clay, 3);
        level.setBlock(clay, Blocks.BLUE_TERRACOTTA.defaultBlockState(), 3);
        helper.assertTrue(BehaviorSprayColorRemover.decolorize(level, clay, Direction.UP, 100L, player,
                ItemStack.EMPTY, 0.5F, 0.5F, 0.5F) == BehaviorSprayColorRemover.REMOVE_COST,
                "coloured terracotta costs the same 10 uses (:101)");
        helper.assertTrue(level.getBlockState(clay).is(Blocks.TERRACOTTA),
                "stained hardened clay becomes plain hardened clay, got " + level.getBlockState(clay));

        // Not a colourable block at all.
        BlockPos stone = floorAndAir(level, site(168, 0));
        helper.assertTrue(BehaviorSprayColorRemover.decolorize(level, stone, Direction.UP, 100L, player,
                        ItemStack.EMPTY, 0.5F, 0.5F, 0.5F) == 0L,
                "stone is not decolorable (:101-104)");
        ItemStack can = ItemBehaviors.stack("gregtech:paint_removal_spray_2");
        can.getOrCreateTag().putLong("gt.remaining", 100L);
        helper.assertFalse(BehaviorSprayColorRemover.useOn(level, stone, Direction.UP, player, can,
                0.5F, 0.5F, 0.5F).acted(), "so the click reports false");
        helper.assertTrue(can.getTag().getLong("gt.remaining") == 100L,
                "and spends nothing, got " + can.getTag().getLong("gt.remaining"));

        // Plain glass is already uncoloured.
        BlockPos plain = site(180, 0);
        clearSite(level, plain, 3);
        level.setBlock(plain, Blocks.GLASS.defaultBlockState(), 3);
        helper.assertTrue(BehaviorSprayColorRemover.decolorize(level, plain, Direction.UP, 100L, player,
                        ItemStack.EMPTY, 0.5F, 0.5F, 0.5F) == 0L,
                "plain glass is not stained glass, so nothing happens");
        helper.succeed();
    }
}
