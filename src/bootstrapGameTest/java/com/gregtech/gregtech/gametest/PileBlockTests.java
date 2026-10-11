package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.block.misc.CoinPileBlock;
import com.gregtech.gregtech.block.misc.PileBlock;
import com.gregtech.gregtech.blockentity.misc.CoinPileBlockEntity;
import com.gregtech.gregtech.blockentity.misc.PileBlockEntity;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;

/**
 * GT6's coin pile and ingot / plate / gem-plate piles.
 *
 * <p>The behaviour under test comes from {@code MultiTileEntityCoin.java} (multi-tile 32700) and from
 * {@code gregapi/tileentity/misc/MultiTileEntityPlaceable.java}, the shared base of
 * {@code MultiTileEntityIngot} / {@code MultiTileEntityPlate} / {@code MultiTileEntityPlateGem}
 * (32084 / 32085 / 32086). Every assertion names the GT6 line it pins down, so a later change that
 * walks away from the original shows up here.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class PileBlockTests {
    private static final int BASE_X = 64000;
    private static final int BASE_Z = 64000;
    private static final int Y = 210;

    /** A click on one face of a block, at the given block-relative position. */
    private static BlockHitResult hit(BlockPos pos, Direction face, double hitX, double hitY, double hitZ) {
        return new BlockHitResult(new Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ),
                face, pos, false);
    }

    /** A click on the top face, where GT6's coin pile does all of its work (MultiTileEntityCoin:156). */
    private static BlockHitResult topHit(BlockPos pos, double hitX, double hitZ) {
        return hit(pos, Direction.UP, hitX, 1.0D, hitZ);
    }

    private static BlockState state(Block block) {
        return block.defaultBlockState();
    }

    /** Places the block and asserts that its block entity arrived - which a plain block never has. */
    private static <T extends BlockEntity> T place(GameTestHelper helper, BlockPos pos, Block block, Class<T> type) {
        ServerLevel level = helper.getLevel();
        level.removeBlock(pos, false);
        level.setBlock(pos, state(block), 2);
        BlockEntity entity = level.getBlockEntity(pos);
        helper.assertTrue(type.isInstance(entity), block + " at " + pos + " created " + entity);
        return type.cast(entity);
    }

    /** The first registered item of a form, used when a hand-picked material has no such item. */
    private static ItemStack anyStack(MaterialPrefix prefix) {
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            ItemStack stack = GTItems.getStack(prefix, material);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * A registered item of one form, preferring the named materials so the test reads clearly.
     *
     * <p>The lookup goes through {@code GTItems.getStack(prefix, material)}, the port's own prefix and
     * material API - the same form lookup GT6 does with {@code OP.ingot.mat(mMaterial, 1)}
     * ({@code MultiTileEntityIngot.java:133}).</p>
     */
    private static ItemStack stack(MaterialPrefix prefix, String... materials) {
        for (String name : materials) {
            GTMaterial material = GTMaterialRegistry.get(name);
            if (!material.isValid()) continue;
            ItemStack stack = GTItems.getStack(prefix, material);
            if (!stack.isEmpty()) return stack;
        }
        return anyStack(prefix);
    }

    /** How many of the given item lie on the ground around the position. */
    private static int droppedAround(ServerLevel level, BlockPos pos, Item item) {
        int dropped = 0;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0D))) {
            if (entity.getItem().is(item)) dropped += entity.getItem().getCount();
        }
        return dropped;
    }

    /**
     * Every item entity near a position, whatever it carries - the second number of a failed drop count,
     * so a failure says whether nothing was dropped or the drop is invisible to the query.
     */
    private static int itemsNear(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0D)).size();
    }

    /** How many of the given item the player carries, held slot included. */
    private static int carried(Player player, Item item) {
        int carried = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) carried += stack.getCount();
        }
        return carried;
    }

    /** The player's non-empty slots as {@code slot: count x item} - the diagnostic of every take. */
    private static String inventoryDump(Player player) {
        StringBuilder dump = new StringBuilder("[");
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            if (dump.length() > 1) dump.append(", ");
            dump.append(slot).append(slot == player.getInventory().selected ? "(held)" : "")
                    .append(": ").append(stack.getCount()).append("x ")
                    .append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        return dump.append(']').toString();
    }

    /**
     * Empties the clicked hand without losing what is in it: what a previous take handed over sits in the
     * held slot ({@code Inventory.add} fills the first free slot, which is the selected one after a take),
     * and the next click must not see it - a held ingot or coin turns GT6's take branch
     * ({@code MultiTileEntityPlaceable:98-111}, {@code MultiTileEntityCoin:159-165}) into the merge/refuse
     * branch instead.
     *
     * <p>The stack goes to the first main-inventory slot that is not the held one and either empty or
     * already the very same stack, so nothing is overwritten and <b>nothing changes item</b> - parking a
     * coin next to a parked ingot must not turn it into an ingot ({@code copyWithCount} of the parked
     * ingot would do exactly that and lose the coin). Only when the 36 main slots are full is the stack
     * dropped into the world, never deleted.</p>
     */
    private static void parkHeld(Player player) {
        int selected = player.getInventory().selected;
        ItemStack held = player.getInventory().getItem(selected);
        if (held.isEmpty()) return;
        player.getInventory().setItem(selected, ItemStack.EMPTY);
        int slots = Math.min(player.getInventory().getContainerSize(), 36);
        for (int slot = 0; slot < slots; slot++) {
            if (slot == selected) continue;
            ItemStack parked = player.getInventory().getItem(slot);
            if (parked.isEmpty()) {
                player.getInventory().setItem(slot, held);
                return;
            }
            if (ItemStack.isSameItemSameTags(parked, held) && parked.getCount() < parked.getMaxStackSize()) {
                parked.grow(held.getCount());
                return;
            }
        }
        player.drop(held, false);
    }

    /**
     * Discards the leftover item entities of earlier runs around a site. Only what the entity query can
     * see is removed (a freshly loaded far chunk hides its entities, see the drop test's note), which is
     * fine: no assertion counts item entities any more, the drop evidence is the loot list.
     */
    private static void clearGround(ServerLevel level, BlockPos pos) {
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0D))) {
            entity.discard();
        }
    }

    /**
     * The loot parameters a real break passes on: the origin, a tool and the block entity the port reads
     * its contents from ({@code Block.dropResources} fills exactly these, see {@code Level.destroyBlock}).
     */
    private static LootParams.Builder lootParams(ServerLevel level, BlockPos pos, BlockEntity entity) {
        return new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, entity);
    }

    /** How many of the given item a drop list hands out - the number every drop assertion is about. */
    private static int countOf(List<ItemStack> stacks, Item item) {
        int count = 0;
        for (ItemStack stack : stacks) {
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    /**
     * All four blocks carry a block entity type, are no longer plain decorative blocks, and expose
     * neither automation nor a comparator - GT6's multi-tiles 32084/32085/32086
     * ({@code Loader_MultiTileEntities:2036-2038}) and 32700 ({@code :2240}) provide none.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void pileBlocksHaveBlockEntities(GameTestHelper helper) {
        helper.assertTrue(GTBlockEntities.PILE != null && GTBlockEntities.PILE.isPresent(),
                "the ingot/plate/gem-plate pile block entity type is registered");
        helper.assertTrue(GTBlockEntities.COIN_PILE != null && GTBlockEntities.COIN_PILE.isPresent(),
                "the coin pile block entity type is registered");

        // The four block ids, in registration order; the coin pile is the fourth.
        Block[] blocks = {GTDecorBlocks.INGOT_PILE.get(), GTDecorBlocks.PLATE_PILE.get(),
                GTDecorBlocks.PLATE_GEM_PILE.get(), GTDecorBlocks.COIN_PILE.get()};
        for (int i = 0; i < blocks.length; i++) {
            Block block = blocks[i];
            helper.assertTrue(block instanceof net.minecraft.world.level.block.EntityBlock,
                    block + " is an entity block");
            helper.assertTrue(block.getClass() != Block.class, block + " is not a plain block any more");
            BlockPos pos = new BlockPos(BASE_X + i * 8, Y, BASE_Z);
            if (i == 3) {
                CoinPileBlockEntity coin = place(helper, pos, block, CoinPileBlockEntity.class);
                helper.assertTrue(coin.getType() == GTBlockEntities.COIN_PILE.get(),
                        "the coin pile carries the coin pile block entity type");
                helper.assertTrue(coin.isEmpty() && coin.total() == 0,
                        "a freshly placed coin pile holds no coin");
                helper.assertTrue(CoinPileBlockEntity.FACES == 16 && CoinPileBlockEntity.FACE_STACK_SIZE == 16,
                        "GT6's coin pile has sixteen faces of at most sixteen coins"
                                + " (MultiTileEntityCoin:71 and :74)");
            } else {
                PileBlockEntity pile = place(helper, pos, block, PileBlockEntity.class);
                helper.assertTrue(pile.getType() == GTBlockEntities.PILE.get(),
                        "the pile carries the pile block entity type");
                helper.assertTrue(pile.isEmpty(), "a freshly placed pile is empty");
            }
        }

        // The three piles accept the form their block is bound to (OP.ingot / OP.plate / OP.plateGem).
        PileBlock ingotPile = (PileBlock) GTDecorBlocks.INGOT_PILE.get();
        PileBlock platePile = (PileBlock) GTDecorBlocks.PLATE_PILE.get();
        PileBlock gemPlatePile = (PileBlock) GTDecorBlocks.PLATE_GEM_PILE.get();
        helper.assertTrue(ingotPile.kind() == PileBlock.Kind.INGOT
                        && ingotPile.kind().prefix() == MaterialPrefix.ingot,
                "gregtech:ingot_pile is GT6's ingot pile (OP.ingot)");
        helper.assertTrue(platePile.kind() == PileBlock.Kind.PLATE
                        && platePile.kind().prefix() == MaterialPrefix.plate,
                "gregtech:plate_pile is GT6's plate pile (OP.plate)");
        helper.assertTrue(gemPlatePile.kind() == PileBlock.Kind.GEM_PLATE
                        && gemPlatePile.kind().prefix() == MaterialPrefix.plateGem,
                "gregtech:plate_gem_pile is GT6's gem plate pile (OP.plateGem)");

        // GT6 exposes no automation and no comparator for these multi-tiles: neither
        // MultiTileEntityCoin nor MultiTileEntityPlaceable implements getAccessibleSlotsFromSide2, and
        // the placeable base is not an inventory (TileEntityBase05Inventories is not in its hierarchy),
        // so the port deliberately adds no item handler capability and no analog output.
        for (int i = 0; i < blocks.length; i++) {
            BlockPos pos = new BlockPos(BASE_X + i * 8, Y, BASE_Z);
            BlockEntity entity = helper.getLevel().getBlockEntity(pos);
            helper.assertTrue(entity != null
                            && !entity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(),
                    blocks[i] + " exposes no item handler, like GT6");
            helper.assertTrue(!blocks[i].hasAnalogOutputSignal(state(blocks[i])),
                    blocks[i] + " has no comparator output, like GT6");
        }
        helper.succeed();
    }

    /**
     * GT6's merge and take on an ingot pile: a whole matching stack goes in, capped at 64, and a take
     * hands exactly one item back and comes off the top of a column.
     * ({@code MultiTileEntityPlaceable:81-96} for the merge, {@code :98-111} for the take.)
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void pileTakesAndReturnsTheClickedStack(GameTestHelper helper) {
        ItemStack ingot = stack(MaterialPrefix.ingot, "Iron", "Copper");
        helper.assertTrue(!ingot.isEmpty(), "the port registers an iron ingot");
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(BASE_X, Y, BASE_Z + 40);
        BlockPos above = pos.above();
        // No chunk ticket here: this test's evidence is the block entity and the player's inventory, not
        // the dropped item entities, and a ticket added now would only be applied by the level on the next
        // tick (DistanceManager.addTicket queues it, ServerChunkCache applies it), so it could not affect
        // anything this test observes anyway.
        // Clear the whole click box first, not just the positions this test places at: a pile left above
        // by an earlier run joins GT6's column walk (MultiTileEntityPlaceable:99-105) and would swallow the
        // take that this test expects on the clicked pile.
        for (int dy = 0; dy <= 2; dy++) level.removeBlock(pos.above(dy), false);
        PileBlock block = (PileBlock) GTDecorBlocks.INGOT_PILE.get();
        PileBlockEntity pile = place(helper, pos, block, PileBlockEntity.class);
        Player player = helper.makeMockPlayer();
        // makeMockPlayer() is creative by construction (GameTestHelper:214-228), and GT6's pile merge
        // shrinks the held stack even for a creative player (:84/:91/:94) - unlike the coin pile, which
        // spares creative players (MultiTileEntityCoin:168).

        // A whole stack is merged at once (:91-96).
        player.setItemInHand(InteractionHand.MAIN_HAND, ingot.copyWithCount(5));
        helper.assertTrue(block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND,
                hit(pos, Direction.UP, 0.5D, 1.0D, 0.5D)).consumesAction(), "a matching stack is merged");
        helper.assertTrue(pile.count() == 5 && ItemStack.isSameItemSameTags(pile.stored(), ingot),
                "five ingots are piled up, got " + pile.stored());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "GT6 moves the whole held stack, so the hand is empty");
        helper.assertTrue(pile.contents().size() == 1, "a pile holds one stack (GT6's mStack)");

        // A partial merge stops at 64 (:83-90) and a full pile takes nothing (:82).
        helper.assertTrue(pile.take(PileBlockEntity.MAX_SIZE).getCount() == 5, "the pile is emptied again");
        helper.assertTrue(pile.add(ingot.copyWithCount(60)) == 60, "sixty ingots are piled up");
        player.setItemInHand(InteractionHand.MAIN_HAND, ingot.copyWithCount(10));
        block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND,
                hit(pos, Direction.UP, 0.5D, 1.0D, 0.5D));
        helper.assertTrue(pile.count() == PileBlockEntity.MAX_SIZE,
                "GT6 caps a pile at 64, got " + pile.count());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 6,
                "only the six ingots that still fit went in (GT6 :84), got "
                        + player.getItemInHand(InteractionHand.MAIN_HAND).getCount());
        block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND,
                hit(pos, Direction.UP, 0.5D, 1.0D, 0.5D));
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 6,
                "a full pile takes nothing (GT6 :82)");

        // One click with a provably empty hand takes exactly one item (:98-111). The six ingots left over
        // from the merge above are parked first (not dropped or overwritten), and only the difference is
        // asserted, so the assertion cannot be fooled by what the player already carries.
        parkHeld(player);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "the clicked hand is empty before the take, inventory " + inventoryDump(player));
        int before = carried(player, ingot.getItem());
        int piled = pile.count();
        helper.assertTrue(block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND,
                hit(pos, Direction.UP, 0.5D, 1.0D, 0.5D)).consumesAction(), "an empty hand takes an item");
        helper.assertTrue(pile.count() == piled - 1,
                "the take removes exactly one ingot: " + piled + " -> " + pile.count());
        helper.assertTrue(carried(player, ingot.getItem()) == before + 1,
                "the player received one ingot, went from " + before + " to " + carried(player, ingot.getItem())
                        + ", inventory " + inventoryDump(player));

        // A column of identical piles hands the item out of its TOP block (GT6 :99-105).
        PileBlockEntity top = place(helper, above, block, PileBlockEntity.class);
        helper.assertTrue(top.add(ingot.copyWithCount(2)) == 2, "the upper pile holds two ingots");
        parkHeld(player);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "the clicked hand is empty before the column take");
        helper.assertTrue(block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND,
                hit(pos, Direction.UP, 0.5D, 1.0D, 0.5D)).consumesAction(), "the column take is handled");
        helper.assertTrue(top.count() == 1 && pile.count() == 63,
                "the take came off the top of the column, got " + top.count() + " / " + pile.count());
        parkHeld(player);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "the clicked hand is empty before the second column take");
        helper.assertTrue(block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND,
                hit(pos, Direction.UP, 0.5D, 1.0D, 0.5D)).consumesAction(), "the second column take is handled");
        helper.assertTrue(top.count() == 0 && pile.count() == 63,
                "the second take empties the top pile, got " + top.count() + " / " + pile.count());
        // GT6's setToAir (:107): the emptied pile removes its block. Only the block state is asserted -
        // a chunk that is loaded without a ticking ticket keeps the removed block's block entity in its
        // chunk map, because LevelChunk.removeBlockEntity only acts when the chunk is "in level"
        // (LevelChunk:394-395); a real player's chunk always is.
        helper.assertTrue(level.getBlockState(above).isAir(),
                "an emptied pile removes its block, as GT6's setToAir does (:107), state "
                        + level.getBlockState(above));
        helper.assertTrue(pile.count() == 63, "the pile below the column is untouched");
        helper.succeed();
    }

    /**
     * GT6 accepts one form and one material per pile: a stack of another prefix or another material is
     * never piled up, because {@code MultiTileEntityPlaceable:81} compares the stored stack with
     * {@code ST.equal} ({@code ST.java:92-94}).
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void pileRefusesAnotherFormOrMaterial(GameTestHelper helper) {
        ItemStack ingot = stack(MaterialPrefix.ingot, "Iron", "Copper");
        ItemStack plate = stack(MaterialPrefix.plate, "Iron", "Copper");
        ItemStack gemPlate = stack(MaterialPrefix.plateGem, "Diamond", "Emerald", "Quartz");
        ItemStack otherIngot = stack(MaterialPrefix.ingot, "Copper", "Gold", "Tin");
        helper.assertTrue(!ingot.isEmpty() && !plate.isEmpty() && !gemPlate.isEmpty() && !otherIngot.isEmpty(),
                "the forms under test are registered");
        helper.assertTrue(!ItemStack.isSameItemSameTags(ingot, otherIngot),
                "two different ingot materials are available");
        ServerLevel level = helper.getLevel();
        // The mock player is creative by construction, which the take below does not depend on: GT6's take
        // branch only needs a stack that does not match the pile (MultiTileEntityPlaceable:98-111).
        Player player = helper.makeMockPlayer();

        // An ingot pile refuses a plate, a gem plate pile refuses a plain plate and an ingot.
        PileBlock ingotPileBlock = (PileBlock) GTDecorBlocks.INGOT_PILE.get();
        BlockPos ingotPos = new BlockPos(BASE_X + 8, Y, BASE_Z + 40);
        PileBlockEntity ingotPile = place(helper, ingotPos, ingotPileBlock, PileBlockEntity.class);
        helper.assertTrue(ingotPile.add(ingot.copyWithCount(3)) == 3, "the ingot pile holds three ingots");
        helper.assertTrue(!ingotPile.canAccept(plate), "a plate is not accepted by an ingot pile");
        helper.assertTrue(!ingotPile.canAccept(new ItemStack(Items.STICK)), "an unrelated item is not accepted");

        PileBlock platePileBlock = (PileBlock) GTDecorBlocks.PLATE_PILE.get();
        BlockPos platePos = new BlockPos(BASE_X + 16, Y, BASE_Z + 40);
        PileBlockEntity platePile = place(helper, platePos, platePileBlock, PileBlockEntity.class);
        helper.assertTrue(platePile.add(plate.copyWithCount(2)) == 2, "the plate pile holds two plates");
        helper.assertTrue(!platePile.canAccept(ingot),
                "an ingot is not accepted by a plate pile (different prefix)");
        helper.assertTrue(platePile.add(ingot.copyWithCount(1)) == 0, "and it is not stored either");

        PileBlock gemPileBlock = (PileBlock) GTDecorBlocks.PLATE_GEM_PILE.get();
        BlockPos gemPos = new BlockPos(BASE_X + 24, Y, BASE_Z + 40);
        PileBlockEntity gemPile = place(helper, gemPos, gemPileBlock, PileBlockEntity.class);
        helper.assertTrue(gemPile.add(gemPlate.copyWithCount(2)) == 2, "the gem plate pile holds two gem plates");
        helper.assertTrue(!gemPile.canAccept(plate) && !gemPile.canAccept(ingot),
                "only gem plates go onto a gem plate pile");

        // GT6's click on a pile with a stack that does not match it stores nothing: it hands one of the
        // pile's own items back instead (MultiTileEntityPlaceable:98-111). The form on the pile decides.
        player.setItemInHand(InteractionHand.MAIN_HAND, ingot.copyWithCount(4));
        helper.assertTrue(platePileBlock.use(level.getBlockState(platePos), level, platePos, player,
                        InteractionHand.MAIN_HAND, hit(platePos, Direction.UP, 0.5D, 1.0D, 0.5D)).consumesAction(),
                "GT6's pile answers a foreign stack by handing one of its own items out");
        helper.assertTrue(platePile.count() == 1, "one plate left the pile, got " + platePile.count());
        helper.assertTrue(PileBlockEntity.prefixOf(platePile.stored()) == MaterialPrefix.plate,
                "the pile still holds plates, not the ingot that was clicked with");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 4,
                "the refused ingot was not touched");
        helper.assertTrue(carried(player, plate.getItem()) == 1, "the player got the plate back");
        helper.assertTrue(ingotPile.count() == 3, "the other pile was not involved");

        // One pile holds one material: a different material of the same form never goes in.
        PileBlockEntity otherPile = place(helper, new BlockPos(BASE_X + 32, Y, BASE_Z + 40),
                ingotPileBlock, PileBlockEntity.class);
        helper.assertTrue(otherPile.add(otherIngot.copyWithCount(2)) == 2,
                "the second pile holds the other ingot");
        helper.assertTrue(!otherPile.canAccept(ingot), "one pile holds one material");
        helper.assertTrue(otherPile.add(ingot.copyWithCount(1)) == 0, "the second material does not go in");

        // The port's prefix lookup also covers unified vanilla items, GT6's ore dictionary
        // (MultiTileEntityPlaceable:59 uses OM.anydata): a vanilla iron ingot shares the pile with the
        // port's own iron ingot.
        ItemStack vanillaIron = new ItemStack(Items.IRON_INGOT);
        helper.assertTrue(PileBlockEntity.prefixOf(vanillaIron) == MaterialPrefix.ingot,
                "a vanilla iron ingot carries the ingot prefix");
        helper.assertTrue(PileBlockEntity.materialOf(vanillaIron) != null
                        && PileBlockEntity.materialOf(ingot) != null
                        && PileBlockEntity.materialOf(vanillaIron).resolve()
                        == PileBlockEntity.materialOf(ingot).resolve(),
                "a vanilla iron ingot is the same material as the port's iron ingot");
        helper.assertTrue(ingotPile.canAccept(vanillaIron), "the pile takes the unified vanilla ingot too");
        helper.assertTrue(!ingotPile.canAccept(ItemStack.EMPTY), "an empty hand never adds");

        // An empty pile refuses instead of deleting itself: GT6 removes the block on any click
        // (MultiTileEntityPlaceable:80), which the port cannot do for a block that can be placed empty.
        BlockPos emptyPos = new BlockPos(BASE_X + 40, Y, BASE_Z + 40);
        PileBlockEntity emptyPile = place(helper, emptyPos, ingotPileBlock, PileBlockEntity.class);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(!ingotPileBlock.use(state(ingotPileBlock), level, emptyPos, player,
                        InteractionHand.MAIN_HAND, hit(emptyPos, Direction.UP, 0.5D, 1.0D, 0.5D)).consumesAction(),
                "an empty pile has nothing to hand out");
        helper.assertTrue(level.getBlockEntity(emptyPos) instanceof PileBlockEntity,
                "the empty pile is still there (port difference to GT6's setToAir)");
        helper.assertTrue(emptyPile.add(plate.copyWithCount(1)) == 0,
                "an empty pile still refuses the wrong form");
        helper.assertTrue(emptyPile.add(ingot.copyWithCount(1)) == 1, "and it takes the right form");
        helper.succeed();
    }

    /**
     * GT6's coin pile fills sixteen faces of at most {@code COIN_STACKSIZE} = 16 coins each, one coin
     * per click on the clicked face ({@code MultiTileEntityCoin:74}, {@code :158}, {@code :166-172}).
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void coinPileFillsFacesUpToTheCap(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(BASE_X, Y, BASE_Z + 80);
        // No chunk ticket: everything asserted here lives in the block entity or the player's inventory.
        CoinPileBlock block = (CoinPileBlock) GTDecorBlocks.COIN_PILE.get();
        CoinPileBlockEntity pile = place(helper, pos, block, CoinPileBlockEntity.class);
        Item coin = CoinPileBlock.coinItem();
        // Two players, because the harness fixes the mode: makeMockPlayer() reports creative no matter
        // what abilities are set (GameTestHelper:214-228), makeMockSurvivalPlayer() reports survival
        // (GameTestHelper:197-207). GT6's coin pile spares a creative player's held coin (:168), so the
        // two halves of that rule need one player of each.
        Player survival = helper.makeMockSurvivalPlayer();
        Player creative = helper.makeMockPlayer();
        Player player = survival;

        // GT6's face index from the click position (:158, and :253 for the placement): a 4x4 grid, and
        // each coordinate is clamped to 0..0.99 first, so the index is floor(hitX*4)*4 + floor(hitZ*4)
        // with both factors in 0..3.
        helper.assertTrue(CoinPileBlockEntity.faceAt(0.1D, 0.1D) == 0, "the near corner is face 0");
        helper.assertTrue(CoinPileBlockEntity.faceAt(0.3D, 0.1D) == 4, "the next face in x is 4");
        helper.assertTrue(CoinPileBlockEntity.faceAt(0.1D, 0.3D) == 1, "the next face in z is 1");
        helper.assertTrue(CoinPileBlockEntity.faceAt(0.9D, 0.9D) == 15, "the far corner is face 15");
        helper.assertTrue(CoinPileBlockEntity.faceAt(1.5D, -1.0D) == 12,
                "x is clamped up to 0.99 (factor 3) and z down to 0 (factor 0): 3*4+0 = 12, got "
                        + CoinPileBlockEntity.faceAt(1.5D, -1.0D));
        helper.assertTrue(CoinPileBlockEntity.faceAt(-1.0D, 1.5D) == 3,
                "and the other way round: 0*4+3 = 3, got " + CoinPileBlockEntity.faceAt(-1.0D, 1.5D));
        helper.assertTrue(CoinPileBlockEntity.faceAt(1.5D, 1.5D) == 15,
                "both clamped up: 3*4+3 = 15, got " + CoinPileBlockEntity.faceAt(1.5D, 1.5D));

        // Only the top face reacts (:156).
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(coin, 64));
        helper.assertTrue(block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND,
                        hit(pos, Direction.NORTH, 0.5D, 0.5D, 0.1D)).consumesAction(),
                "GT6 consumes a side click without changing the pile (GT6 :156, :178)");
        helper.assertTrue(pile.total() == 0, "the side click added no coin");

        // Twenty clicks on the same face, one coin per click (:166-172): sixteen go in - each one spends
        // exactly one coin of the hand (:168) - and the last four are refused by the per-face cap (:167).
        int face = CoinPileBlockEntity.faceAt(0.1D, 0.1D);
        for (int click = 1; click <= 20; click++) {
            int expected = Math.min(click, CoinPileBlockEntity.FACE_STACK_SIZE);
            boolean consumed = block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND,
                    topHit(pos, 0.1D, 0.1D)).consumesAction();
            helper.assertTrue(pile.faceCount(face) == expected,
                    "click " + click + " leaves " + expected + " coins on the face, got " + pile.faceCount(face));
            helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 64 - expected,
                    "click " + click + " spends one coin, hand holds "
                            + player.getItemInHand(InteractionHand.MAIN_HAND).getCount());
            helper.assertTrue(consumed,
                    "GT6 consumes the click even when the face is full, click " + click);
        }
        helper.assertTrue(pile.faceCount(face) == CoinPileBlockEntity.FACE_STACK_SIZE,
                "the face holds GT6's cap of " + CoinPileBlockEntity.FACE_STACK_SIZE + ", got "
                        + pile.faceCount(face));
        helper.assertTrue(pile.total() == CoinPileBlockEntity.FACE_STACK_SIZE,
                "only one face was filled, got " + pile.total());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 48,
                "sixteen coins were spent, got " + player.getItemInHand(InteractionHand.MAIN_HAND).getCount());

        // Another face takes the next coin and binds the pile to that coin item.
        int secondFace = CoinPileBlockEntity.faceAt(0.5D, 0.5D);
        block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND, topHit(pos, 0.5D, 0.5D));
        helper.assertTrue(pile.faceCount(secondFace) == 1, "the clicked face took one coin");
        helper.assertTrue(pile.total() == 17, "seventeen coins on the pile, got " + pile.total());
        helper.assertTrue(!pile.coinItem().isEmpty() && pile.coinItem().is(coin),
                "the pile remembers its coin item");

        // A non-coin item is refused and - unlike an ingot pile - takes nothing out of the pile: GT6's coin
        // pile has no take branch for a held item (:166-172). Clicked on the face that still has room, so
        // the refusal is the item's doing and not the cap's.
        player.setItemInHand(InteractionHand.MAIN_HAND,
                stack(MaterialPrefix.ingot, "Iron", "Copper").copyWithCount(8));
        helper.assertTrue(block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND,
                        topHit(pos, 0.5D, 0.5D)).consumesAction(),
                "an ingot is refused while GT6 still consumes the click");
        helper.assertTrue(pile.total() == 17, "no coin left the pile for the refused ingot");
        helper.assertTrue(pile.faceCount(secondFace) == 1, "and the face with room kept its coin");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 8,
                "the refused ingot was not touched");

        // A creative player adds without spending the held coin (GT6 :168, GameTestHelper's mock player).
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(coin, 4));
        block.use(state(block), level, pos, creative, InteractionHand.MAIN_HAND, topHit(pos, 0.5D, 0.5D));
        helper.assertTrue(pile.total() == 18, "the creative click added one coin, got " + pile.total());
        helper.assertTrue(creative.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 4,
                "a creative player keeps the coin (GT6 :168)");

        // An empty hand takes one coin off the clicked face (:159-165), one coin per take. Every take
        // click parks what the previous one handed over and proves the clicked hand is empty first: the
        // mock player's inventory fills the held slot first, and a held coin would make the next click an
        // add again (:166-172) instead of a take.
        parkHeld(player);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "the clicked hand is empty before the take, inventory " + inventoryDump(player));
        int received = carried(player, coin);
        helper.assertTrue(block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND,
                topHit(pos, 0.1D, 0.1D)).consumesAction(), "an empty hand takes a coin");
        helper.assertTrue(pile.faceCount(face) == CoinPileBlockEntity.FACE_STACK_SIZE - 1,
                "one coin left the clicked face, got " + pile.faceCount(face));
        helper.assertTrue(carried(player, coin) == received + 1,
                "the player received one coin, went from " + received + " to " + carried(player, coin)
                        + ", inventory " + inventoryDump(player));

        // Drain what is left face by face, one coin per take click and one coin into the inventory each
        // time; the block goes with the very last coin (GT6 :174-176).
        for (int take = 1; take <= CoinPileBlockEntity.FACE_STACK_SIZE - 1; take++) {
            parkHeld(player);
            helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                    "the clicked hand is empty before take " + take + ", inventory " + inventoryDump(player));
            helper.assertTrue(block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND,
                    topHit(pos, 0.1D, 0.1D)).consumesAction(), "face 0 hands out coin " + take);
            helper.assertTrue(carried(player, coin) == received + 1 + take,
                    "the player received " + (1 + take) + " coin(s) so far, got " + carried(player, coin)
                            + ", inventory " + inventoryDump(player));
            helper.assertTrue(pile.faceCount(face) == CoinPileBlockEntity.FACE_STACK_SIZE - 1 - take,
                    "take " + take + " costs the face one coin, got " + pile.faceCount(face));
        }
        helper.assertTrue(pile.faceCount(face) == 0 && pile.total() == 2,
                "face 0 is empty, two coins are left on the other face, got " + pile.total());
        parkHeld(player);
        helper.assertTrue(block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND,
                topHit(pos, 0.5D, 0.5D)).consumesAction(), "the other face hands out a coin");
        helper.assertTrue(pile.total() == 1 && level.getBlockState(pos).is(GTDecorBlocks.COIN_PILE.get()),
                "one coin left, so the pile still stands");
        helper.assertTrue(carried(player, coin) == received + 17,
                "seventeen coins are with the player, got " + carried(player, coin)
                        + ", inventory " + inventoryDump(player));
        parkHeld(player);
        helper.assertTrue(block.use(state(block), level, pos, player, InteractionHand.MAIN_HAND,
                topHit(pos, 0.5D, 0.5D)).consumesAction(), "the last coin is taken");
        helper.assertTrue(pile.total() == 0, "the pile is drained, got " + pile.total());
        // Eighteen coins were put on the pile (16 + 1 + the creative one) and eighteen takes handed them
        // back: sixteen in the loop, one before it and one after it. The test's own 48-coin input stack was
        // replaced by the ingot stack of the refusal check, so the player starts this phase with no coin.
        helper.assertTrue(carried(player, coin) == received + 18,
                "the player collected all eighteen coins, got " + carried(player, coin)
                        + ", inventory " + inventoryDump(player));
        // GT6 :174-176 removes the block with the last coin; the block state is what is asserted, because a
        // chunk without a ticking ticket keeps the removed block's block entity in its chunk map
        // (LevelChunk.removeBlockEntity is gated by isInLevel(), LevelChunk:394-395).
        helper.assertTrue(level.getBlockState(pos).isAir(),
                "the coin pile removed its block with the last coin (GT6 :174-176), state "
                        + level.getBlockState(pos));
        helper.succeed();
    }

    /**
     * Breaking a pile hands its contents out exactly once: GT6 drops the stored stack
     * ({@code MultiTileEntityPlaceable:72-74}) and all coins in stacks of at most 64
     * ({@code MultiTileEntityCoin:112-122}). The port hangs that on {@code onRemove}, so the player path
     * and a plain block removal both drop, and neither of them drops twice.
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void breakingAPileDropsItsContentsOnce(GameTestHelper helper) {
        ItemStack ingot = stack(MaterialPrefix.ingot, "Iron", "Copper");
        helper.assertTrue(!ingot.isEmpty(), "the port registers an iron ingot");
        ServerLevel level = helper.getLevel();
        PileBlock block = (PileBlock) GTDecorBlocks.INGOT_PILE.get();

        // GT6's getDrops is what hands the stored stack over (MultiTileEntityPlaceable:72-74), and the
        // port reads the pile from the BLOCK_ENTITY loot parameter - exactly the parameter the real drop
        // paths pass (Block.dropResources via Level.destroyBlock:290-291). That list is the deterministic
        // evidence: the *item entities* of a far chunk are not, because a chunk loaded by this test is not
        // entity-ticking yet - a ticket only takes effect in the next ServerChunkCache.tick
        // (DistanceManager.addTicket:155-168 queues it, ServerChunkCache:273 applies it), so its entity
        // sections still carry the default HIDDEN visibility (PersistentEntitySectionManager:47) and
        // Level.getEntitiesOfClass skips them (EntitySectionStorage:55). The entity counts below are
        // therefore printed as diagnostics, not asserted.
        BlockPos pos = new BlockPos(BASE_X + 8, Y, BASE_Z + 80);
        BlockPos second = new BlockPos(BASE_X + 16, Y, BASE_Z + 80);
        BlockPos coinPos = new BlockPos(BASE_X + 24, Y, BASE_Z + 80);
        BlockPos third = new BlockPos(BASE_X + 32, Y, BASE_Z + 80);

        // The loot route: GT6 drops only the three stored ingots, not an extra empty pile block.
        PileBlockEntity pile = place(helper, pos, block, PileBlockEntity.class);
        clearGround(level, pos);
        helper.assertTrue(pile.add(ingot.copyWithCount(3)) == 3, "three ingots on the pile");
        ItemStack picked = block.getCloneItemStack(level, pos, level.getBlockState(pos));
        helper.assertTrue(ItemStack.isSameItemSameTags(picked, ingot) && picked.getCount() == 1,
                "middle click gives the represented material ingot rather than an empty pile block");
        List<ItemStack> loot = block.getDrops(level.getBlockState(pos), lootParams(level, pos, pile));
        helper.assertTrue(countOf(loot, ingot.getItem()) == 3,
                "the break hands out exactly the three stored ingots, got " + countOf(loot, ingot.getItem())
                        + " from " + loot + " (stored " + pile.stored() + ")");
        helper.assertTrue(countOf(loot, block.asItem()) == 0,
                "GT6 has no separate empty pile item in the break loot, got " + loot);
        helper.assertTrue(pile.contents().isEmpty(),
                "the pile gave its contents up, stored " + pile.stored());
        List<ItemStack> again = block.getDrops(level.getBlockState(pos), lootParams(level, pos, pile));
        helper.assertTrue(countOf(again, ingot.getItem()) == 0,
                "a second break call hands out no contents, got " + again);

        // The removal paths that roll no loot (a piston, /setblock) drop through onRemove instead, and the
        // emptied pile has nothing left for a second path either.
        level.removeBlock(pos, false);
        helper.assertTrue(level.getBlockState(pos).isAir(),
                "the pile block is gone, state " + level.getBlockState(pos));
        helper.assertTrue(pile.contents().isEmpty(),
                "it kept nothing, stored " + pile.stored()
                        + " (ingots dropped and visible here: " + droppedAround(level, pos, ingot.getItem())
                        + ", item entities nearby: " + itemsNear(level, pos) + ")");

        // The player path runs the real break API: its loot roll hands the contents over (which empties
        // the pile) and the block ends up gone, so the same contents cannot be handed out twice - the
        // ingot count on the ground is printed as a diagnostic, see the note above.
        PileBlockEntity other = place(helper, second, block, PileBlockEntity.class);
        clearGround(level, second);
        helper.assertTrue(other.add(ingot.copyWithCount(4)) == 4, "four ingots on the second pile");
        level.destroyBlock(second, true, helper.makeMockPlayer());
        helper.assertTrue(level.getBlockState(second).isAir(),
                "the second pile is gone, state " + level.getBlockState(second));
        helper.assertTrue(other.contents().isEmpty(),
                "and emptied, stored " + other.stored()
                        + " (ingots dropped and visible here: "
                        + droppedAround(level, second, ingot.getItem())
                        + ", item entities nearby: " + itemsNear(level, second) + ")");

        // Coins: twenty of them come back as one stack of at most 64 (MultiTileEntityCoin:112-122).
        CoinPileBlockEntity coinPile = place(helper, coinPos, GTDecorBlocks.COIN_PILE.get(),
                CoinPileBlockEntity.class);
        clearGround(level, coinPos);
        ItemStack coin = CoinPileBlock.defaultCoin().copyWithCount(1);
        for (int face = 0; face < 16; face++) {
            helper.assertTrue(coinPile.add(face, coin) == 1, "a coin on face " + face);
        }
        for (int face = 0; face < 4; face++) {
            helper.assertTrue(coinPile.add(face, coin) == 1, "a second coin on face " + face);
        }
        helper.assertTrue(coinPile.total() == 20, "twenty coins on the pile, got " + coinPile.total());
        helper.assertTrue(coinPile.contents().size() == 1 && coinPile.contents().get(0).getCount() == 20,
                "GT6 splits the coins into stacks of at most 64 (:112-122)");
        List<ItemStack> coinLoot = GTDecorBlocks.COIN_PILE.get()
                .getDrops(level.getBlockState(coinPos), lootParams(level, coinPos, coinPile));
        // GT6's getDrops returns the twenty coins alone, with no extra empty pile item.
        helper.assertTrue(countOf(coinLoot, coin.getItem()) == 20,
                "the break hands the twenty stored coins out, got "
                        + countOf(coinLoot, coin.getItem()) + " from " + coinLoot);
        helper.assertTrue(countOf(coinLoot, GTDecorBlocks.COIN_PILE.get().asItem()) == 0,
                "GT6 never adds a separate empty pile block to coin drops, got " + coinLoot);
        helper.assertTrue(CoinPileBlock.isCoin(coin),
                "the dropped coins are the port's coin items, got " + coin);
        helper.assertTrue(coinPile.contents().isEmpty(),
                "the coin pile gave its coins up, stored " + coinPile.contents());
        level.removeBlock(coinPos, false);
        helper.assertTrue(level.getBlockState(coinPos).isAir(),
                "the coin pile block is gone, state " + level.getBlockState(coinPos));

        // A pile that was drained by taking hands out nothing at all.
        PileBlockEntity drained = place(helper, third, block, PileBlockEntity.class);
        clearGround(level, third);
        helper.assertTrue(drained.add(ingot.copyWithCount(1)) == 1, "one ingot on the third pile");
        helper.assertTrue(drained.take(1).getCount() == 1, "and it is taken back out");
        helper.assertTrue(drained.contents().isEmpty(), "the third pile is empty again");
        List<ItemStack> none = block.getDrops(level.getBlockState(third), lootParams(level, third, drained));
        helper.assertTrue(countOf(none, ingot.getItem()) == 0,
                "an emptied pile hands out no ingots, got " + none);
        level.removeBlock(third, false);
        helper.assertTrue(level.getBlockState(third).isAir(),
                "the third pile block is gone, state " + level.getBlockState(third)
                        + " (item entities nearby: " + itemsNear(level, third) + ")");
        helper.succeed();
    }

    /**
     * The stored contents survive NBT, under GT6's own keys: {@code "gt.value"} for a pile
     * ({@code MultiTileEntityPlaceable:56} and {@code :68}, {@code CS.java:1218}) and
     * {@code gt.coin.stacksize.<i>} for every coin face ({@code MultiTileEntityCoin:81} and {@code :93}).
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void pileNbtRoundTripsThroughAPlacedBlock(GameTestHelper helper) {
        ItemStack ingot = stack(MaterialPrefix.ingot, "Iron", "Copper");
        helper.assertTrue(!ingot.isEmpty(), "the port registers an iron ingot");
        ServerLevel level = helper.getLevel();
        PileBlock block = (PileBlock) GTDecorBlocks.INGOT_PILE.get();

        BlockPos pos = new BlockPos(BASE_X, Y, BASE_Z + 120);
        PileBlockEntity pile = place(helper, pos, block, PileBlockEntity.class);
        helper.assertTrue(!pile.saveWithoutMetadata().contains(PileBlockEntity.NBT_VALUE),
                "an empty pile writes no stored stack");
        helper.assertTrue(pile.add(ingot.copyWithCount(7)) == 7, "seven ingots on the pile");
        CompoundTag tag = pile.saveWithoutMetadata();
        helper.assertTrue(tag.contains(PileBlockEntity.NBT_VALUE),
                "the pile uses GT6's key " + PileBlockEntity.NBT_VALUE);
        helper.assertTrue(PileBlockEntity.NBT_VALUE.equals("gt.value"), "GT6's NBT_VALUE is \"gt.value\"");

        // Load the tag into a freshly placed pile: the contents come back unchanged.
        level.removeBlock(pos, false);
        BlockPos reloadPos = new BlockPos(BASE_X + 8, Y, BASE_Z + 120);
        PileBlockEntity reloaded = place(helper, reloadPos, block, PileBlockEntity.class);
        reloaded.load(tag);
        helper.assertTrue(reloaded.count() == 7, "the reloaded pile holds seven ingots, got " + reloaded.count());
        helper.assertTrue(ItemStack.isSameItemSameTags(reloaded.stored(), ingot),
                "the reloaded pile holds the same ingot, got " + reloaded.stored());
        helper.assertTrue(reloaded.contents().size() == 1, "and one stored stack");

        // The same for the coin pile's sixteen faces.
        BlockPos coinPos = new BlockPos(BASE_X + 16, Y, BASE_Z + 120);
        CoinPileBlockEntity coinPile = place(helper, coinPos, GTDecorBlocks.COIN_PILE.get(),
                CoinPileBlockEntity.class);
        ItemStack coin = CoinPileBlock.defaultCoin().copyWithCount(1);
        helper.assertTrue(coinPile.add(3, coin) == 1 && coinPile.add(3, coin) == 1, "two coins on face 3");
        helper.assertTrue(coinPile.add(15, coin) == 1, "one coin on face 15");
        CompoundTag coinTag = coinPile.saveWithoutMetadata();
        helper.assertTrue(coinTag.contains(CoinPileBlockEntity.NBT_STACKSIZE + 3)
                        && coinTag.getByte(CoinPileBlockEntity.NBT_STACKSIZE + 3) == 2,
                "face 3 is stored under GT6's key " + CoinPileBlockEntity.NBT_STACKSIZE + "3");
        helper.assertTrue(coinTag.getByte(CoinPileBlockEntity.NBT_STACKSIZE + 15) == 1,
                "face 15 holds one coin");
        helper.assertTrue(coinTag.getByte(CoinPileBlockEntity.NBT_STACKSIZE + 7) == 0, "face 7 is empty");
        helper.assertTrue(coinTag.contains(CoinPileBlockEntity.NBT_COIN), "the coin item is stored");
        helper.assertTrue(CoinPileBlockEntity.NBT_STACKSIZE.equals("gt.coin.stacksize."),
                "the port keeps GT6's coin stack size key");

        level.removeBlock(coinPos, false);
        BlockPos coinReloadPos = new BlockPos(BASE_X + 24, Y, BASE_Z + 120);
        CoinPileBlockEntity coinReloaded = place(helper, coinReloadPos, GTDecorBlocks.COIN_PILE.get(),
                CoinPileBlockEntity.class);
        coinReloaded.load(coinTag);
        helper.assertTrue(coinReloaded.total() == 3,
                "the reloaded coin pile holds three coins, got " + coinReloaded.total());
        helper.assertTrue(coinReloaded.faceCount(3) == 2 && coinReloaded.faceCount(15) == 1,
                "the face counts survived");
        helper.assertTrue(!coinReloaded.coinItem().isEmpty() && coinReloaded.coinItem().is(coin.getItem()),
                "the coin item survived");
        List<ItemStack> drops = coinReloaded.contents();
        helper.assertTrue(drops.size() == 1 && drops.get(0).getCount() == 3,
                "the drops mirror the reloaded faces, got " + drops);

        // A hand-written tag cannot push a face past GT6's cap.
        CompoundTag overfull = new CompoundTag();
        overfull.putByte(CoinPileBlockEntity.NBT_STACKSIZE + 0, (byte) 99);
        overfull.putByte(CoinPileBlockEntity.NBT_STACKSIZE + 1, (byte) -5);
        coinReloaded.load(overfull);
        helper.assertTrue(coinReloaded.faceCount(0) == CoinPileBlockEntity.FACE_STACK_SIZE
                        && coinReloaded.faceCount(1) == 0,
                "counters from a hand-written tag are clamped into GT6's cap");
        helper.succeed();
    }

    /**
     * The dungeon's own decoration helpers leave stocked piles behind: GT6 writes the coin faces and the
     * pile stack while it builds the cell ({@code DungeonData.java:169-173} for the coins,
     * {@code :265-276} for the ingots, plates and gem plates), and the port's
     * {@link GTDungeonData#coins} / {@link GTDungeonData#pile} now fill the block entities the same way.
     *
     * <p>The cell is hand-made at this suite's own base - no dungeon feature is run, only the two helpers
     * a room calls.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void dungeonPilesArriveStocked(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        WorldGenLevel level = server;
        BlockPos origin = new BlockPos(BASE_X + 64, Y, BASE_Z + 160);
        // The GameTest world is reused between runs, so this cell is emptied first: the piles of the
        // previous run are still there, and {@code GTDungeonData.set} - like {@code Level.setBlock} - then
        // reports "no change" (LevelChunk.setBlockState returns null when the old state is the very same
        // instance, Level.setBlock:220-222), so the placement helpers would refuse to stock anything.
        for (int dx = 1; dx <= 15; dx += 2) level.removeBlock(origin.offset(dx, 1, 1), false);
        GTDungeonData data = new GTDungeonData(level, origin.getX(), origin.getY(), origin.getZ(),
                StoneType.LIMESTONE, StoneType.SLATE, 3, new byte[5][5], 2, 2, 0,
                new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                RandomSource.create(64000L));

        // GT6's coin roll (:170-171): every face 1-in-3 with 0..7 coins, plus one face with 1..8.
        helper.assertTrue(data.coins(1, 1, 1), "the dungeon places its coin pile");
        CoinPileBlockEntity coins = (CoinPileBlockEntity) server.getBlockEntity(origin.offset(1, 1, 1));
        helper.assertTrue(coins != null, "the coin pile arrived with its block entity");
        int overSeven = 0, highest = 0;
        for (int face = 0; face < CoinPileBlockEntity.FACES; face++) {
            int count = coins.faceCount(face);
            helper.assertTrue(count <= CoinPileBlockEntity.FACE_STACK_SIZE,
                    "face " + face + " respects GT6's per-face cap, got " + count);
            if (count > 7) overSeven++;
            highest = Math.max(highest, count);
        }
        helper.assertTrue(coins.total() >= 1, "GT6's guaranteed face left at least one coin behind");
        helper.assertTrue(overSeven <= 1 && highest <= 8,
                "only the one guaranteed face may carry up to eight coins, got max " + highest);
        GTMaterial coinMetal = PileBlockEntity.materialOf(coins.coinItem());
        helper.assertTrue(CoinPileBlock.isCoin(coins.coinItem()) && coinMetal != null
                        && inPool(coinMetal, GTDungeonData.COIN_METALS),
                "the dungeon coins are real port coins of GT6's own dungeon coin list"
                        + " (WorldgenDungeonGT:195), got " + coins.coinItem());

        // GT6's three pile forms, each stocked with a real stack of the form's material (:265-276).
        assertStockedPile(helper, level, origin, 3, 1, 1, GTDungeonData.PileKind.INGOT,
                MaterialPrefix.ingot, GTDungeonData.PILE_METALS, data);
        assertStockedPile(helper, level, origin, 5, 1, 1, GTDungeonData.PileKind.PLATE,
                MaterialPrefix.plate, GTDungeonData.PILE_METALS, data);
        assertStockedPile(helper, level, origin, 7, 1, 1, GTDungeonData.PileKind.GEM_PLATE,
                MaterialPrefix.plateGem, GTDungeonData.PILE_GEMS, data);

        // GT6's aStackSize argument (0 means nextStack(), 1..64 otherwise).
        helper.assertTrue(data.pile(9, 1, 1, GTDungeonData.PileKind.INGOT, 5), "a pile with a fixed size");
        PileBlockEntity sized = (PileBlockEntity) server.getBlockEntity(origin.offset(9, 1, 1));
        helper.assertTrue(sized != null && sized.count() == 5,
                "GT6's bindStack argument gives exactly five items, got " + (sized == null ? -1 : sized.count()));

        // The explicit forms a room (or a test) uses when it knows the stack and the face.
        helper.assertTrue(data.coin(11, 1, 1, 4, 3), "an explicit coin pile");
        CoinPileBlockEntity explicit = (CoinPileBlockEntity) server.getBlockEntity(origin.offset(11, 1, 1));
        helper.assertTrue(explicit != null && explicit.faceCount(4) == 3 && explicit.total() == 3,
                "the asked-for face holds three coins");
        ItemStack iron = stack(MaterialPrefix.ingot, "Iron", "Copper");
        ItemStack twelve = iron.copyWithCount(12);
        helper.assertTrue(data.pileStack(13, 1, 1, GTDungeonData.PileKind.INGOT, twelve),
                "an explicit pile stack is placed");
        PileBlockEntity explicitPile = (PileBlockEntity) server.getBlockEntity(origin.offset(13, 1, 1));
        helper.assertTrue(explicitPile != null && explicitPile.count() == 12
                        && ItemStack.isSameItemSameTags(explicitPile.stored(), iron),
                "the pile holds the twelve ingots it was handed, got "
                        + (explicitPile == null ? "no block entity" : explicitPile.stored()));

        // GT6 places nothing at all when the stack is invalid (ST.valid(aStack) && set(...)).
        helper.assertTrue(!data.pileStack(15, 1, 1, GTDungeonData.PileKind.INGOT, ItemStack.EMPTY),
                "an empty stack places no pile");
        helper.assertTrue(level.getBlockState(origin.offset(15, 1, 1)).isAir(),
                "and it leaves the site empty, like GT6");
        helper.succeed();
    }

    /** One dungeon pile: placed, non-empty, of the form's prefix and of a material of GT6's own list. */
    private static void assertStockedPile(GameTestHelper helper, WorldGenLevel level, BlockPos origin,
                                          int x, int y, int z, GTDungeonData.PileKind kind,
                                          MaterialPrefix prefix, String[] pool, GTDungeonData data) {
        helper.assertTrue(data.pile(x, y, z, kind), "the dungeon places a " + kind + " pile");
        PileBlockEntity pile = (PileBlockEntity) level.getBlockEntity(origin.offset(x, y, z));
        helper.assertTrue(pile != null, "the " + kind + " pile arrived with its block entity");
        helper.assertTrue(!pile.isEmpty(), "the dungeon's " + kind + " pile is not empty");
        helper.assertTrue(pile.count() >= 1 && pile.count() <= PileBlockEntity.MAX_SIZE,
                "GT6's stack size is 1..64, got " + pile.count());
        helper.assertTrue(PileBlockEntity.prefixOf(pile.stored()) == prefix,
                "the pile holds " + prefix.getName() + "s, got " + pile.stored());
        GTMaterial material = PileBlockEntity.materialOf(pile.stored());
        helper.assertTrue(material != null, "the piled material is known to the port");
        // GT6's list is what the roll draws from; a form the list cannot supply at all would be a port
        // gap, and then only the prefix above is checked.
        helper.assertTrue(!poolHasForm(prefix, pool) || inPool(material, pool),
                "the material comes from GT6's own list, got " + (material == null ? "none" : material.getName()));
    }

    /** Whether a material is one of the names of GT6's list, aliases resolved. */
    private static boolean inPool(GTMaterial material, String[] pool) {
        for (String name : pool) {
            GTMaterial candidate = GTMaterialRegistry.get(name);
            if (candidate.isValid() && candidate.resolve() == material.resolve()) return true;
        }
        return false;
    }

    /** Whether the port registers any item of the form for a material of GT6's list. */
    private static boolean poolHasForm(MaterialPrefix prefix, String[] pool) {
        for (String name : pool) {
            if (!GTItems.getStack(prefix, GTMaterialRegistry.get(name)).isEmpty()) return true;
        }
        return false;
    }
}
