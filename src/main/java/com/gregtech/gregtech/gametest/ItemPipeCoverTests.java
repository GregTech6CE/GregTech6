package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.ItemPipeBlock;
import com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity;
import com.gregtech.gregtech.content.cover.CoverItems;
import com.gregtech.gregtech.content.cover.CoverUtilityBehaviors;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * §112: item pipes are cover hosts, the other half of §111.
 *
 * <p>GT6's item filter ({@code CoverFilterItem}) and item retriever ({@code CoverRetrieverItem}) are
 * covers of an item pipe, exactly as the pressure valve and the fluid filter are covers of a fluid pipe.
 * §111 gave the fluid pipe a host; this suite covers the item pipe — per-face attachment, the filter's
 * two-way interception at the capability, the retriever's outright refusal of the face it owns, the
 * sending-face half of the filter, and the regression guard that a coverless pipe is unchanged.</p>
 *
 * <h2>Why these coordinates</h2>
 *
 * <p>{@link #BASE_X}/{@link #BASE_Z} are {@code 61000}, free between the 60000 (bumbliary) and 62000
 * (dungeon keys) reservations. Every site is cleared before use, blocks <em>and</em> entities.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class ItemPipeCoverTests {
    private static final int BASE_X = 61000;
    private static final int BASE_Y = 100;
    private static final int BASE_Z = 61000;

    /** A steel item pipe — the id scheme is {@code item_pipe_<size>_<material>}, so the registry is scanned. */
    private static final String PIPE_CANDIDATES = "item_pipe_medium_steel";

    private static BlockPos site(int dx, int dz) {
        return new BlockPos(BASE_X + dx, BASE_Y, BASE_Z + dz);
    }

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

    /**
     * An item pipe with a buffer big enough for the stack assertions below.
     *
     * <p>Item pipe ids are {@code item_pipe_<size>_<material>} and the size decides the buffer
     * ({@code ItemPipeSpec.invSize()}): a medium pipe of a common material holds a single item, so the
     * tests that move four or eight have to pick a large-buffer one. Scanning the registry for it keeps
     * the suite independent of any one material's id.</p>
     */
    private static Block pipeBlock(GameTestHelper helper) {
        Block any = ForgeRegistries.BLOCKS.getValues().stream()
                .filter(b -> b instanceof ItemPipeBlock)
                .filter(b -> ((ItemPipeBlock) b).spec().invSize() >= 8)
                .findFirst().orElse(null);
        helper.assertTrue(any != null, "an item pipe with a buffer of at least 8 is registered");
        return any;
    }

    private static ItemPipeBlockEntity setPipe(GameTestHelper helper, BlockPos pos) {
        ServerLevel level = helper.getLevel();
        level.setBlock(pos, pipeBlock(helper).defaultBlockState(), 3);
        helper.assertTrue(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity,
                "the item pipe carries its block entity");
        return (ItemPipeBlockEntity) level.getBlockEntity(pos);
    }

    private static ItemPipeBlockEntity pipe(GameTestHelper helper, BlockPos pos) {
        clearSite(helper.getLevel(), pos, 2);
        return setPipe(helper, pos);
    }

    /**
     * Opens one face for flow, which is what placement does when a player connects two pipes.
     *
     * <p>§111's trap: the transfer loops skip every face whose connection property is false, so two
     * pipes placed with {@code setBlock} alone are completely disconnected — a negative assertion about
     * "nothing flows" then passes for the wrong reason.</p>
     */
    private static void connect(ServerLevel level, BlockPos pos, Direction side) {
        level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(ItemPipeBlock.propFor(side), true));
    }

    private static void tick(ServerLevel level, BlockPos pos, ItemPipeBlockEntity pipe) {
        ItemPipeBlockEntity.serverTick(level, pos, level.getBlockState(pos), pipe);
    }

    private static ItemStack cover(String id) {
        return new ItemStack(GTTechnological.get(id));
    }

    private static net.minecraftforge.items.IItemHandler face(ItemPipeBlockEntity pipe, Direction side) {
        return pipe.getCapability(ForgeCapabilities.ITEM_HANDLER, side).resolve().orElse(null);
    }

    // ── the host itself ─────────────────────────────────────────────────────────────

    /** A pipe face takes a cover, dispatches it by the shared behaviour table and gives it back on removal. */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void anItemPipeHostsCoversPerFace(GameTestHelper helper) {
        ItemPipeBlockEntity pipe = pipe(helper, site(0, 0));
        helper.assertTrue(pipe.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.FILTER_ITEM)),
                "an item pipe face accepts an item filter (GT6 attaches covers to item pipes)");
        helper.assertTrue(CoverUtilityBehaviors.FILTER_ITEM.equals(
                        CoverItems.behavior(pipe.getCover(Direction.EAST))),
                "the face dispatches to the item-filter behaviour, got "
                        + CoverItems.behavior(pipe.getCover(Direction.EAST)));
        for (Direction side : Direction.values()) {
            if (side == Direction.EAST) continue;
            helper.assertTrue(pipe.getCover(side).isEmpty(), side + " is still free, got " + pipe.getCover(side));
        }
        helper.assertFalse(pipe.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.RETRIEVER_ITEM)),
                "an occupied face refuses a second cover");
        ItemStack removed = pipe.removeCover(Direction.EAST);
        helper.assertTrue(CoverUtilityBehaviors.FILTER_ITEM.equals(CoverItems.behavior(removed))
                        && pipe.getCover(Direction.EAST).isEmpty(),
                "removal hands the cover back and frees the face, got " + removed);
        helper.succeed();
    }

    // ── the per-face item interception ──────────────────────────────────────────────

    /**
     * The item filter lives on the face: the filtered face refuses what the filter refuses while the
     * other five faces keep working ({@code CoverFilterItem:115-127}).
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void theItemFilterOnAPipeFaceFiltersThatFaceOnly(GameTestHelper helper) {
        ItemPipeBlockEntity pipe = pipe(helper, site(16, 0));
        helper.assertTrue(pipe.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.FILTER_ITEM)),
                "the item filter attaches to a pipe face");
        helper.assertTrue(pipe.clickFilterCover(Direction.EAST, new ItemStack(Items.APPLE)),
                "the held apple becomes the pipe's item filter (CoverFilterItem:88-112)");

        var east = face(pipe, Direction.EAST);
        var west = face(pipe, Direction.WEST);
        helper.assertTrue(east != null && west != null, "both faces expose an item handler");
        helper.assertTrue(east.insertItem(0, new ItemStack(Items.APPLE), true).isEmpty(),
                "the whitelisted apple passes the filtered face");
        helper.assertTrue(east.insertItem(0, new ItemStack(Items.DIAMOND), true).getCount() == 1,
                "a diamond is refused there (CoverFilterItem:115), got "
                        + east.insertItem(0, new ItemStack(Items.DIAMOND), true));
        helper.assertTrue(west.insertItem(0, new ItemStack(Items.DIAMOND), true).isEmpty(),
                "and the uncovered face still accepts diamonds — no cover means permit (§108)");
        helper.succeed();
    }

    /**
     * The retriever refuses <em>both</em> directions on the face it owns ({@code CoverRetrieverItem:138-139}):
     * a pipe may neither push into nor pull out of that face.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void theRetrieverOwnsItsFaceOutright(GameTestHelper helper) {
        ItemPipeBlockEntity pipe = pipe(helper, site(32, 0));
        helper.assertTrue(pipe.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.RETRIEVER_ITEM)),
                "the item retriever attaches to a pipe face");
        pipe.insertItem(0, new ItemStack(Items.APPLE, 4), false);
        helper.assertTrue(pipe.getStackInSlot(0).getCount() == 4, "the pipe holds 4 apples");

        var east = face(pipe, Direction.EAST);
        var west = face(pipe, Direction.WEST);
        helper.assertTrue(east.insertItem(0, new ItemStack(Items.APPLE), true).getCount() == 1,
                "nothing may be inserted through the retriever's face (CoverRetrieverItem:138)");
        helper.assertTrue(east.extractItem(0, 1, true).isEmpty(),
                "and nothing may be pulled out of it either (:139)");
        helper.assertTrue(west.extractItem(0, 1, true).getCount() == 1,
                "the other faces still expose the pipe's contents");
        helper.assertTrue(pipe.getStackInSlot(0).getCount() == 4,
                "and nothing was actually moved, got " + pipe.getStackInSlot(0));
        helper.succeed();
    }

    // ── the two directions of the face filter ───────────────────────────────────────

    /**
     * Drives {@code ticks} real server ticks, calling the pipes' own ticker on every tick whose game
     * time passes the pipe's {@code % 4} routing gate, then runs {@code done}.
     *
     * <p>Two facts about this suite force the shape: block entities do <b>not</b> tick on their own in a
     * GameTest (the whole suite drives them by hand — {@code grep "serverTick(" gametest}), and the item
     * pipe routes only when {@code level.getGameTime() % 4 == 0} ({@code ItemPipeBlockEntity:205}), which
     * a single GameTest body cannot guarantee. Looping over real ticks through {@code runAfterDelay}
     * gives the game time a chance to advance into that residue class instead of leaving a 25 % flake.</p>
     */
    private static void pump(GameTestHelper helper, ServerLevel level, List<BlockPos> pipes, int ticks,
                             Runnable done) {
        if (ticks <= 0) {
            done.run();
            return;
        }
        helper.runAfterDelay(1, () -> {
            if (level.getGameTime() % 4 == 0) {
                for (BlockPos pos : pipes) {
                    if (level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
                        ItemPipeBlockEntity.serverTick(level, pos, level.getBlockState(pos), pipe);
                    }
                }
            }
            pump(helper, level, pipes, ticks - 1, done);
        });
    }

    /**
     * The sending half of §112: a filter gates what passes <b>through</b> its face, so the face the pipe
     * pushes items out of has to refuse them too — otherwise a player could stop items entering a pipe
     * but not leaving it.
     *
     * <p>The line ends in a <b>chest</b>: pipe routing only forwards towards a non-pipe inventory
     * ({@code forwardToBestPipe} → {@code findBestExitCost}). It runs in three phases, in this order for
     * a reason:</p>
     * <ol>
     *   <li><b>no cover</b> — apples travel the line, which proves the line itself works. Without this
     *       phase the negative phase below would pass merely because nothing moves at all (§111.4);</li>
     *   <li><b>an apple filter</b> — the sender now carries diamonds, which the filter refuses: they must
     *       stay in the sender;</li>
     *   <li><b>a diamond filter</b> — the same face now lets the same items through.</li>
     * </ol>
     *
     * <p>Each phase carries one item type only: an item pipe has a <b>single</b> internal slot
     * ({@code inventory = NonNullList.withSize(1, …)}, and {@code insertItem} ignores the requested slot
     * and picks the first empty/matching one), so a second stack of a different item is simply refused.
     * {@code ItemPipeSpec.invSize()} is that one slot's <em>limit</em>, not a slot count.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void theFilterOnTheSendingFaceGatesOutgoingItems(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(48, 0);
        clearSite(level, pos, 4);
        ItemPipeBlockEntity sender = setPipe(helper, pos);
        setPipe(helper, pos.east());
        BlockPos chestPos = pos.east(2);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        connect(level, pos, Direction.EAST);
        connect(level, pos.east(), Direction.WEST);
        connect(level, pos.east(), Direction.EAST);
        helper.assertTrue(level.getBlockState(pos).getValue(ItemPipeBlock.propFor(Direction.EAST))
                        && level.getBlockState(pos.east()).getValue(ItemPipeBlock.propFor(Direction.WEST)),
                "the two pipes are connected, otherwise nothing could flow at all");
        helper.assertTrue(level.getBlockEntity(chestPos) instanceof ChestBlockEntity,
                "the line ends in a real inventory, which is the only thing pipe routing forwards to");
        List<BlockPos> line = List.of(pos, pos.east());

        // Phase 1: no cover on the sending face — the line has to move items, or nothing below means
        // anything.
        helper.assertTrue(sender.insertItem(0, new ItemStack(Items.APPLE, 4), false).isEmpty(),
                "the sender takes the apples");
        pump(helper, level, line, 24, () -> {
            int apples = chestCount(level, chestPos, Items.APPLE);
            helper.assertTrue(apples > 0,
                    "with no cover the apples travel the line — so the line itself works, got " + apples);

            // Phase 2: an apple filter on the sending face, a diamond in the pipe.
            helper.assertTrue(sender.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.FILTER_ITEM)),
                    "the sender takes an item filter on the face it pushes through");
            helper.assertTrue(sender.clickFilterCover(Direction.EAST, new ItemStack(Items.APPLE)),
                    "an apple becomes that face's filter");
            helper.assertTrue(sender.coverPermitsItemTraffic(Direction.EAST, new ItemStack(Items.APPLE))
                            && !sender.coverPermitsItemTraffic(Direction.EAST, new ItemStack(Items.DIAMOND)),
                    "the face lets apples out and refuses diamonds");
            // The pipe has one slot, so whatever the line has not drained yet has to go before the next
            // item type can be loaded — and the assertion says how much was left, either way.
            ItemStack leftover = sender.extractItem(0, sender.getStackInSlot(0).getCount(), false);
            helper.assertTrue(sender.getStackInSlot(0).isEmpty(),
                    "the sender's single slot is free for the next phase, it still held " + leftover);
            helper.assertTrue(sender.insertItem(0, new ItemStack(Items.DIAMOND, 4), false).isEmpty(),
                    "the sender now carries diamonds, got " + sender.getStackInSlot(0));

            pump(helper, level, line, 24, () -> {
                int diamonds = chestCount(level, chestPos, Items.DIAMOND);
                helper.assertTrue(diamonds == 0,
                        "a diamond never leaves through an apple-filtered face, got " + diamonds);
                helper.assertTrue(countItems(sender, Items.DIAMOND) == 4,
                        "and it is still in the sender, got " + countItems(sender, Items.DIAMOND)
                                + " [" + describe(sender) + "]");

                // Phase 3 runs on a *fresh* line: every item pipe has a single slot, so a line that has
                // already carried one item type can refuse the next one for reasons that have nothing to
                // do with the filter (the middle pipe's slot, in particular). A clean line isolates the
                // rule under test.
                BlockPos third = site(96, 0);
                clearSite(level, third, 4);
                ItemPipeBlockEntity sender3 = setPipe(helper, third);
                setPipe(helper, third.east());
                BlockPos chest3 = third.east(2);
                level.setBlock(chest3, Blocks.CHEST.defaultBlockState(), 3);
                connect(level, third, Direction.EAST);
                connect(level, third.east(), Direction.WEST);
                connect(level, third.east(), Direction.EAST);
                helper.assertTrue(sender3.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.FILTER_ITEM)),
                        "the fresh line's sender takes an item filter");
                // Exercise the screwdriver/soft-hammer entry points too, then install a matching filter.
                helper.assertTrue(sender3.clickFilterCover(Direction.EAST, new ItemStack(Items.APPLE))
                                && sender3.configureFilterCover(Direction.EAST, false, true)
                                && sender3.clickFilterCover(Direction.EAST, new ItemStack(Items.DIAMOND)),
                        "the filter can be set, cleared and set again (CoverFilterItem:63-66,88-112)");
                helper.assertTrue(sender3.coverPermitsItemTraffic(Direction.EAST, new ItemStack(Items.DIAMOND)),
                        "the matching filter lets the diamond out, got " + sender3.getCover(Direction.EAST));
                helper.assertTrue(sender3.insertItem(0, new ItemStack(Items.DIAMOND, 4), false).isEmpty(),
                        "the fresh sender carries diamonds, got " + sender3.getStackInSlot(0));

                pump(helper, level, List.of(third, third.east()), 32, () -> {
                    helper.assertTrue(chestCount(level, chest3, Items.DIAMOND) > 0,
                            "a diamond that matches the filter does leave, got "
                                    + chestCount(level, chest3, Items.DIAMOND)
                                    + " [sender " + describe(sender3) + "]");
                    helper.succeed();
                });
            });
        });
    }

    /** How many of one item the inventory at {@code pos} holds; −1 when there is no container there. */
    private static int chestCount(ServerLevel level, BlockPos pos, net.minecraft.world.item.Item item) {
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return -1;
        int count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }

    /** A pipe's slots, for the failure messages: an item pipe has exactly one. */
    private static String describe(ItemPipeBlockEntity pipe) {
        StringBuilder text = new StringBuilder();
        for (int slot = 0; slot < pipe.getSlots(); slot++) {
            if (slot > 0) text.append(", ");
            text.append(slot).append('=').append(pipe.getStackInSlot(slot));
        }
        return text.toString();
    }

    private static int countItems(ItemPipeBlockEntity pipe, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < pipe.getSlots(); slot++) {
            ItemStack stack = pipe.getStackInSlot(slot);
            if (stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }

    // ── the regression guard and the drop hook ──────────────────────────────────────

    /** A pipe with no covers must behave exactly as before (§108.7.3's question about the new path). */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void anItemPipeWithoutCoversStillMovesItems(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(64, 0);
        ItemPipeBlockEntity pipe = pipe(helper, pos);
        helper.assertTrue(pipe.insertItem(0, new ItemStack(Items.APPLE, 8), false).isEmpty(),
                "an uncovered pipe accepts items");
        helper.assertTrue(pipe.getStackInSlot(0).getCount() == 8,
                "and stores them, got " + pipe.getStackInSlot(0));
        for (int i = 0; i < 5; i++) tick(level, pos, pipe);
        helper.assertTrue(pipe.getStackInSlot(0).getCount() == 8,
                "ticking a coverless pipe leaves its contents alone, got " + pipe.getStackInSlot(0));
        helper.assertTrue(pipe.extractItem(0, 3, false).getCount() == 3,
                "and it still hands them back out");
        helper.succeed();
    }

    /** Breaking the pipe gives the covers back, counted one tick later (§110's entity-visibility rule). */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void anItemPipeDropsItsCoversWhenBroken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = site(80, 0);
        ItemPipeBlockEntity pipe = pipe(helper, pos);
        helper.assertTrue(pipe.attachCover(Direction.UP, cover(CoverUtilityBehaviors.FILTER_ITEM)),
                "the pipe takes an item filter on its top face");
        ItemStack expected = pipe.getCover(Direction.UP).copy();
        helper.assertTrue(!expected.isEmpty(), "the cover was stored, got " + expected);

        level.removeBlock(pos, false);
        AABB area = box(pos, 2);
        helper.runAfterDelay(1, () -> {
            int found = 0;
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof ItemEntity item)) continue;
                if (!area.contains(item.position())) continue;
                if (ItemStack.isSameItemSameTags(item.getItem(), expected)) found += item.getItem().getCount();
            }
            helper.assertTrue(found == 1, "the cover drops when the pipe is broken, found " + found);
            helper.succeed();
        });
    }
}
