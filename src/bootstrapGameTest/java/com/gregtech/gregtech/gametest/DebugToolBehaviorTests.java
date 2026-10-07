package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.item.behavior.BehaviorBuilderWand;
import com.gregtech.gregtech.item.behavior.BehaviorChunkEraser;
import com.gregtech.gregtech.item.behavior.BehaviorRemote;
import com.gregtech.gregtech.item.behavior.BehaviorWorldgenDebugger;
import com.gregtech.gregtech.item.behavior.ItemBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * §110: the second half of GT6's item-behaviour layer — the builder's wand, the two debug wands and
 * the Remote Activator (the five "next batch" candidates listed in §108.6, minus the USB HDD tooltip).
 *
 * <p>Sources: {@code gregtech/items/behaviors/Behavior_Builderwand.java},
 * {@code Behavior_Chunk_Remover.java}, {@code Behavior_Worldgen_Debugger.java},
 * {@code Behavior_Remote.java}; every claim below quotes the line it comes from.</p>
 *
 * <h2>Why these coordinates</h2>
 *
 * <p>{@link #BASE_X}/{@link #BASE_Z} are {@code 55000} and {@link #BASE_Y} is {@code 100}, an area no
 * other suite uses ({@code grep -r "BASE_X" gametest}); 55000 also starts a chunk of its own, which is
 * what the two chunk-wide debug wands need — they erase 16×16×249 per click, so a site shared with
 * another suite would take that suite's blocks with it. Every site is cleared before use, blocks
 * <em>and</em> entities, so the file is re-runnable on a reused world.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class DebugToolBehaviorTests {
    private static final int BASE_X = 55000;
    private static final int BASE_Y = 100;
    private static final int BASE_Z = 55000;

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

    private static Player survival(GameTestHelper helper) {
        return helper.makeMockSurvivalPlayer();
    }

    /** The wand item of {@code GTToolType.BUILDER_WAND} ({@code GTToolItems:24}, id {@code tool_…}). */
    private static ItemStack wand() {
        return ItemBehaviors.stack("gregtech:tool_builder_wand");
    }

    /** A dynamite block — GT6 {@code MultiTileEntityDynamite}, the port's one {@code RemoteActivatable}. */
    private static Block dynamite(GameTestHelper helper) {
        Block block = ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", "dynamite"));
        helper.assertTrue(block != null && block != Blocks.AIR, "gregtech:dynamite is registered");
        return block;
    }

    /**
     * The wand copies the surface it was clicked on one layer up.
     *
     * <p>{@code Behavior_Builderwand:89-91} walks the plane perpendicular to the clicked face — here the
     * horizontal plane of the clicked block — and {@code :123} places each copy with
     * {@code SIDE_TOP}, which is vanilla {@code Direction.UP} ({@code CS.java:517}), so every matching
     * block gets a twin <em>on top of it</em>. A 3×3 floor of stone is the matching surface; the
     * cobblestone in it is not the clicked block and must stay untouched (GT6 {@code :92} requires the
     * same block and metadata).</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void builderWandCopiesTheSurfaceOneLayerUp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = survival(helper);
        BlockPos centre = site(0, 0);
        clearSite(level, centre, 6);

        List<BlockPos> stone = new ArrayList<>();
        BlockPos cobble = centre.offset(1, 0, 1);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = centre.offset(x, 0, z);
                if (pos.equals(cobble)) {
                    level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                } else {
                    level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                    stone.add(pos);
                }
            }
        }
        player.getInventory().setItem(9, new ItemStack(Blocks.STONE, 64));
        ItemStack wand = wand();

        ItemBehaviors.Outcome outcome = BehaviorBuilderWand.useOn(level, centre, Direction.UP, player,
                wand, 0.5F, 0.25F, 0.5F);
        helper.assertTrue(outcome.acted(), "the wand reports the click as handled (Behavior_Builderwand:130)");

        for (BlockPos pos : stone) {
            helper.assertTrue(level.getBlockState(pos.above()).is(Blocks.STONE),
                    "a copy landed on top of " + pos + ", got " + level.getBlockState(pos.above()));
        }
        helper.assertTrue(level.getBlockState(cobble.above()).isAir(),
                "the cobblestone is not the clicked block, so nothing was built on it (:92), got "
                        + level.getBlockState(cobble.above()));
        helper.assertTrue(level.getBlockState(centre.offset(0, 1, 0)).is(Blocks.STONE),
                "the clicked block itself is part of the plane too (:89-91)");

        // The floor is 4 blocks around the centre in every direction; outside it the plane holds air,
        // which does not match the clicked block, so the 9x9 walk stops there.
        int placed = stone.size();
        helper.assertTrue(player.getInventory().getItem(9).getCount() == 64 - placed,
                "one block per placement is taken from the inventory: expected " + (64 - placed)
                        + " left, got " + player.getInventory().getItem(9).getCount());
        helper.assertTrue(!wand.isDamageableItem() || wand.getDamageValue() == placed,
                "the wand also takes 1 damage per placement (:128), got " + wand.getDamageValue()
                        + " of " + wand.getMaxDamage());
        helper.succeed();
    }

    /** A position that cannot be built on is skipped instead of aborting the whole plane. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void builderWandSkipsBlockedPositions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = survival(helper);
        BlockPos centre = site(16, 0);
        clearSite(level, centre, 6);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(centre.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        // One of the nine copies has nowhere to go: the space above it is occupied.
        BlockPos blocked = centre.offset(-1, 0, -1);
        level.setBlock(blocked.above(), Blocks.DIRT.defaultBlockState(), 3);
        player.getInventory().setItem(9, new ItemStack(Blocks.STONE, 64));
        ItemStack wand = wand();

        helper.assertTrue(BehaviorBuilderWand.useOn(level, centre, Direction.UP, player, wand,
                        0.5F, 0.25F, 0.5F).acted(),
                "the other eight placements still happen");
        helper.assertTrue(level.getBlockState(blocked.above()).is(Blocks.DIRT),
                "the blocked position is left alone, got " + level.getBlockState(blocked.above()));
        helper.assertTrue(player.getInventory().getItem(9).getCount() == 64 - 8,
                "so only 8 blocks were consumed, got " + player.getInventory().getItem(9).getCount());
        helper.succeed();
    }

    /** Without a matching block in the inventory the wand does nothing at all (GT6 {@code :98-101}). */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void builderWandNeedsBlocksInTheInventory(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = survival(helper);
        BlockPos centre = site(32, 0);
        clearSite(level, centre, 6);
        level.setBlock(centre, Blocks.STONE.defaultBlockState(), 3);

        ItemStack wand = wand();
        helper.assertFalse(BehaviorBuilderWand.useOn(level, centre, Direction.UP, player, wand,
                        0.5F, 0.25F, 0.5F).acted(),
                "an empty inventory means nothing is placed");
        helper.assertTrue(level.getBlockState(centre.above()).isAir(),
                "and the space above stays empty, got " + level.getBlockState(centre.above()));
        helper.assertTrue(wand.getDamageValue() == 0, "and the wand is untouched");

        // Wrong block in the inventory: still nothing (the scan compares the block, :108).
        player.getInventory().setItem(9, new ItemStack(Blocks.DIRT, 64));
        helper.assertFalse(BehaviorBuilderWand.useOn(level, centre, Direction.UP, player, wand,
                        0.5F, 0.25F, 0.5F).acted(),
                "dirt is not the clicked block, so no copy is placed");
        helper.assertTrue(player.getInventory().getItem(9).getCount() == 64,
                "and nothing is consumed, got " + player.getInventory().getItem(9).getCount());
        helper.succeed();
    }

    /** Creative mode places without spending blocks or durability (GT6 {@code :124-128}). */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void builderWandIsFreeInCreative(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = survival(helper);
        BlockPos centre = site(48, 0);
        clearSite(level, centre, 6);
        level.setBlock(centre, Blocks.STONE.defaultBlockState(), 3);
        player.getInventory().setItem(9, new ItemStack(Blocks.STONE, 1));
        player.getAbilities().instabuild = true;
        ItemStack wand = wand();

        helper.assertTrue(BehaviorBuilderWand.useOn(level, centre, Direction.UP, player, wand,
                        0.5F, 0.25F, 0.5F).acted(), "a creative player still builds");
        helper.assertTrue(level.getBlockState(centre.above()).is(Blocks.STONE),
                "the copy is placed, got " + level.getBlockState(centre.above()));
        helper.assertTrue(player.getInventory().getItem(9).getCount() == 1,
                "and the stack is restored (UT.Entities.hasInfiniteItems), got "
                        + player.getInventory().getItem(9).getCount());
        helper.assertTrue(wand.getDamageValue() == 0,
                "and the wand takes no damage, got " + wand.getDamageValue());
        helper.succeed();
    }

    /**
     * The chunk eraser empties the chunk's column but keeps the very bottom, and the y-range seam
     * clears exactly the band it is given.
     *
     * <p>{@code Behavior_Chunk_Remover:41} sweeps {@code for (int tY = 1; tY < 250; tY++)}, so y=0
     * survives — "Deletes Chunks except for the very Bottom" ({@code :48}).</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void chunkEraserClearsTheColumnExceptTheBottom(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = survival(helper);
        BlockPos centre = site(64, 64);
        clearSite(level, centre, 2);

        int chunkX = centre.getX() >> 4;
        int chunkZ = centre.getZ() >> 4;
        BlockPos bottom = new BlockPos(centre.getX(), 0, centre.getZ());
        BlockPos low = new BlockPos(centre.getX(), 1, centre.getZ());
        BlockPos mid = new BlockPos(centre.getX(), 100, centre.getZ());
        BlockPos top = new BlockPos(centre.getX(), BehaviorChunkEraser.MAX_Y, centre.getZ());
        for (BlockPos pos : List.of(bottom, low, mid, top)) {
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        }

        int erased = BehaviorChunkEraser.erase(level, chunkX, chunkZ,
                BehaviorChunkEraser.MIN_Y, BehaviorChunkEraser.MAX_Y);
        helper.assertTrue(erased >= 3, "the eraser reports the blocks it removed, got " + erased);
        helper.assertTrue(level.getBlockState(low).isAir() && level.getBlockState(mid).isAir()
                        && level.getBlockState(top).isAir(),
                "y=1, y=100 and y=" + BehaviorChunkEraser.MAX_Y + " are all cleared");
        helper.assertTrue(level.getBlockState(bottom).is(Blocks.STONE),
                "y=0 is the \"very Bottom\" GT6 keeps (:41/:48), got " + level.getBlockState(bottom));

        // The seam the tests drive: a band is cleared, everything outside it is not.
        BlockPos band = new BlockPos(centre.getX() + 1, 120, centre.getZ() + 1);
        BlockPos outside = new BlockPos(centre.getX() + 1, 140, centre.getZ() + 1);
        level.setBlock(band, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(outside, Blocks.STONE.defaultBlockState(), 3);
        BehaviorChunkEraser.erase(level, chunkX, chunkZ, 120, 130);
        helper.assertTrue(level.getBlockState(band).isAir(), "the band is cleared");
        helper.assertTrue(level.getBlockState(outside).is(Blocks.STONE),
                "and the block just outside it survives, got " + level.getBlockState(outside));

        // The click entry point uses the GT6 range and reports the click as handled.
        helper.assertTrue(BehaviorChunkEraser.useOn(level, centre, Direction.UP, player,
                        ItemBehaviors.stack("gregtech:chunk_eraser"), 0.5F, 0.5F, 0.5F).acted(),
                "the Chunk Eraser handles the click (Behavior_Chunk_Remover:44)");
        helper.succeed();
    }

    /**
     * The worldgen debug wand clears the chunk <em>except</em> the GT ores, which is what makes a vein
     * visible in game ({@code Behavior_Worldgen_Debugger:45-52}).
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void worldgenDebugWandKeepsTheOres(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = survival(helper);
        BlockPos centre = site(80, 80);
        clearSite(level, centre, 2);

        Block ore = ForgeRegistries.BLOCKS.getValues().stream()
                .filter(b -> b instanceof com.gregtech.gregtech.block.OreBlock)
                .findFirst().orElseThrow();
        int chunkX = centre.getX() >> 4;
        int chunkZ = centre.getZ() >> 4;
        BlockPos orePos = new BlockPos(centre.getX(), 40, centre.getZ());
        BlockPos stonePos = new BlockPos(centre.getX() + 1, 40, centre.getZ());
        level.setBlock(orePos, ore.defaultBlockState(), 3);
        level.setBlock(stonePos, Blocks.STONE.defaultBlockState(), 3);

        int erased = BehaviorWorldgenDebugger.eraseExceptOres(level, chunkX, chunkZ, 1, 249);
        helper.assertTrue(erased >= 1, "the stone was removed, got " + erased + " blocks");
        helper.assertTrue(level.getBlockState(stonePos).isAir(),
                "stone is not a GT prefix block, so it goes");
        helper.assertTrue(level.getBlockState(orePos).getBlock() == ore,
                "the ore survives — GT6 keeps every IPrefixBlock (:47-49), got "
                        + level.getBlockState(orePos));

        helper.assertTrue(BehaviorWorldgenDebugger.useOn(level, centre, Direction.UP, player,
                        ItemBehaviors.stack("gregtech:worldgen_debug_wand"), 0.5F, 0.5F, 0.5F).acted(),
                "the Worldgen Debug Wand handles the click (Behavior_Worldgen_Debugger:54)");
        helper.assertTrue(level.getBlockState(orePos).getBlock() == ore,
                "and a second sweep still keeps the ore");
        helper.succeed();
    }

    /**
     * The Remote Activator: sneak-click binds a charge, a plain right-click fires every bound charge
     * within {@link BehaviorRemote#RANGE}, and a fired coordinate is dropped again.
     *
     * <p>{@code Behavior_Remote:52-68} decides add/remove/refuse, {@code :56-58} caps the list at 64 per
     * dimension, and {@code :78-87} keeps only the coordinates it could not reach or could not fire —
     * the port's dynamite returns {@code false} from {@code remoteActivate} exactly like GT6's
     * ({@code MultiTileEntityDynamite:193}), so firing a charge forgets its coordinate.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void remoteActivatorBindsAndFiresCharges(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = survival(helper);
        // Far enough from this file's other sites that the blast cannot disturb them.
        BlockPos charge = site(40, 120);
        clearSite(level, charge, 3);
        level.setBlock(charge, dynamite(helper).defaultBlockState(), 3);
        ItemStack remote = ItemBehaviors.stack("gregtech:remote_activator");

        player.setShiftKeyDown(true);
        helper.assertTrue(BehaviorRemote.useOn(level, charge, Direction.UP, player, remote,
                        0.5F, 0.5F, 0.5F).acted(), "sneak-clicking a charge binds it (:59-64)");
        helper.assertTrue(BehaviorRemote.getCoords(remote.getOrCreateTag(), level)
                        .equals(List.of(charge)),
                "one coordinate is bound, got " + BehaviorRemote.getCoords(remote.getTag(), level));

        // GT6's NBT layout, field for field (:121-135): one compound per dimension, c<i>/x<i>/y<i>/z<i>.
        CompoundTag dim = remote.getTag().getCompound(BehaviorRemote.dimKey(level));
        helper.assertTrue(dim.getBoolean("c0") && dim.getInt("x0") == charge.getX()
                        && dim.getInt("y0") == charge.getY() && dim.getInt("z0") == charge.getZ(),
                "the stored layout is GT6's c0/x0/y0/z0, got " + dim);

        // A block that is not remote activatable cannot be added (:65-68). GT6 still answers T here —
        // `return T` at :72 sits *after* all four branches — so the invariant to assert is the list.
        BlockPos stone = site(48, 120);
        clearSite(level, stone, 3);
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
        player.setShiftKeyDown(true);
        helper.assertTrue(BehaviorRemote.useOn(level, stone, Direction.UP, player, remote,
                        0.5F, 0.5F, 0.5F).acted(),
                "GT6 reports the click as handled even when nothing was bound (:72)");
        helper.assertTrue(BehaviorRemote.getCoords(remote.getTag(), level).equals(List.of(charge)),
                "and stone is not added, got " + BehaviorRemote.getCoords(remote.getTag(), level));

        // The 64-coordinate cap (:56-58). The list must not contain the clicked position itself, or the
        // "already bound, remove it" branch (:52) would run first and never reach the cap.
        List<BlockPos> full = new ArrayList<>();
        for (int i = 0; i < BehaviorRemote.MAX_COORDS; i++) full.add(charge.offset(i + 1, 0, 0));
        BehaviorRemote.setCoords(remote.getOrCreateTag(), level, full);
        helper.assertTrue(BehaviorRemote.useOn(level, charge, Direction.UP, player, remote,
                        0.5F, 0.5F, 0.5F).acted(), "a full list still handles the click (:72)");
        helper.assertTrue(BehaviorRemote.getCoords(remote.getTag(), level).size()
                        == BehaviorRemote.MAX_COORDS
                        && !BehaviorRemote.getCoords(remote.getTag(), level).contains(charge),
                "the 65th coordinate is not added, got "
                        + BehaviorRemote.getCoords(remote.getTag(), level).size() + " entries");

        // Fire: back to the single charge, then a plain (not sneaking) right-click in the air. The
        // activation half is range-gated (:80), so the player has to stand near the charge — a mock
        // player is created at the world origin, which is 55k blocks away from this suite's sites.
        BehaviorRemote.setCoords(remote.getOrCreateTag(), level, new ArrayList<>(List.of(charge)));
        player.setShiftKeyDown(false);
        player.moveTo(charge.getX() + 8.5D, charge.getY(), charge.getZ() + 0.5D, 0.0F, 0.0F);
        helper.assertTrue(BehaviorRemote.useInAir(level, player, remote).acted(),
                "a right-click in the air runs the activation (:76-90)");
        helper.assertTrue(level.getBlockEntity(charge) instanceof com.gregtech.gregtech.blockentity.tool.DynamiteBlockEntity chargeEntity
                        && chargeEntity.remainingTicks() == 20,
                "remote activation arms a 20-tick fuse instead of immediately detonating");
        // This isolated fixture is outside active chunks; drive its real server ticker explicitly.
        var chargeEntity = (com.gregtech.gregtech.blockentity.tool.DynamiteBlockEntity) level.getBlockEntity(charge);
        for (int tick=0; tick<20; tick++) chargeEntity.serverTick();
        helper.assertTrue(level.getBlockState(charge).isAir(), "charge detonates after its fuse");
        helper.assertTrue(BehaviorRemote.getCoords(remote.getTag(), level).isEmpty(),
                "and the fired coordinate is dropped (:82), got "
                        + BehaviorRemote.getCoords(remote.getTag(), level));

        // Coordinates out of range are kept even though they cannot be fired (:83-85). "Out of range"
        // is measured from the player, who now stands 8 blocks from the (already fired) charge, so this
        // one has to clear RANGE by more than that offset.
        BlockPos far = charge.offset(BehaviorRemote.RANGE + 200, 0, 0);
        BehaviorRemote.setCoords(remote.getOrCreateTag(), level, new ArrayList<>(List.of(far)));
        helper.assertTrue(BehaviorRemote.useInAir(level, player, remote).acted(),
                "an unreachable coordinate still counts as handled");
        helper.assertTrue(BehaviorRemote.getCoords(remote.getTag(), level).equals(List.of(far)),
                "and it stays bound, got " + BehaviorRemote.getCoords(remote.getTag(), level));
        helper.succeed();
    }

    /**
     * §110 regression guard for the {@code MultiItem.use} wiring: an edible multi-item must still start
     * the eating animation.
     *
     * <p>{@code GTMultiItems.MultiItem.use} runs the remote activator's behaviour first and now falls
     * through to {@code super.use} when no behaviour claims the click. Returning a bare
     * {@code InteractionResultHolder.pass(...)} instead would have skipped {@code Item.use}'s default
     * implementation — the one that calls {@code startUsingItem} for an edible item — and silently made
     * every one of GT's ~60 food items inedible (§108.7.3: always ask what happens when the behaviour
     * being wired is <em>not</em> the one in your hand).</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void edibleMultiItemsStillStartEatingThroughUse(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = survival(helper);
        ItemStack food = ItemBehaviors.stack("gregtech:lemon");
        helper.assertTrue(food.isEdible(), "gregtech:lemon carries GT6's own food values");
        helper.assertFalse(ItemBehaviors.isPorted("lemon"),
                "no behaviour claims the food, so its click has to fall through to Item#use");

        // Item#use only starts eating when the player is actually hungry.
        player.getFoodData().setFoodLevel(10);
        player.setItemInHand(InteractionHand.MAIN_HAND, food);
        ItemStack held = player.getMainHandItem();
        InteractionResultHolder<ItemStack> result = held.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult().consumesAction(),
                "use() starts using the item instead of passing, got " + result.getResult());
        helper.assertTrue(player.isUsingItem(), "and the player is now eating it");
        helper.succeed();
    }
}
