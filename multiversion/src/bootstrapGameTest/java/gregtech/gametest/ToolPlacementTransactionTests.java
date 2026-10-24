package gregtech.gametest;

import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.tool.GTToolHelper;
import gregtech.internal.tool.GTToolType;
import gregtech.content.registry.GTElectricItems;
import gregtech.content.registry.GTToolItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.BlockEvent;
//?}

/** Real server interaction and cancellable placement events, not calls to a mirrored placement model. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class ToolPlacementTransactionTests {
    private static ServerPlayer player(GameTestHelper h) {
//? if >=1.21.1 {
        /*var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[gt-placement]"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(h.absolutePos(new BlockPos(0, 20, 0)).getX(), h.absolutePos(new BlockPos(0, 20, 0)).getY(),
                h.absolutePos(new BlockPos(0, 20, 0)).getZ());
        return player;
    }

    private static ItemStack wand(String material) {
        return GTToolHelper.write(GTToolItems.empty(GTToolType.BUILDER_WAND),
                GTMaterialRegistry.get(material), GTMaterialRegistry.get("Wood"));
    }

    private static void click(ServerPlayer player, BlockPos pos, Direction face, double hitY) {
        player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(pos.getX() + .5, pos.getY() + hitY, pos.getZ() + .5), face, pos, false));
    }

    public static final class PlacementListener {
        private final ServerPlayer player;
        public boolean cancel;
        public int calls;
        public PlacementListener(ServerPlayer player) { this.player = player; }
        @SubscribeEvent public void placed(BlockEvent.EntityPlaceEvent event) {
            if (event.getEntity() != player) return;
            calls++;
            if (cancel) event.setCanceled(true);
        }
    }

    private static void register(PlacementListener listener) {
//? if >=1.21.1 {
        /*net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(listener);
*///?} else {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(listener);
//?}
    }
    private static void unregister(PlacementListener listener) {
//? if >=1.21.1 {
        /*net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
*///?} else {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(listener);
//?}
    }

    @GameTest(template = "test_empty")
    public static void builderSixFacesCommitExactlyOnce(GameTestHelper h) {
        var player = player(h);
        // test_empty is 1x1x1. Its clearing does not cover y=20 or the wand's source plane;
        // natural stone there can be copied before the clicked cell. Isolate the entire native scan.
        var support = new BlockPos(24_000_000, 200, 24_000_000);
        int radius = Math.max(0, GTToolHelper.getHead(wand("Steel")).getToolQuality() + 1) + 1;
        player.setPos(support.getX() - 2.5, support.getY() + 1, support.getZ() - 2.5);
        record Saved(net.minecraft.world.level.block.state.BlockState state, net.minecraft.nbt.CompoundTag data) {}
        var saved = new java.util.LinkedHashMap<BlockPos, Saved>();
        var listener = new PlacementListener(player);
        register(listener);
        try {
            for (var pos : BlockPos.betweenClosed(support.offset(-radius, -radius, -radius),
                    support.offset(radius, radius, radius))) {
                var entity = h.getLevel().getBlockEntity(pos);
//? if >=1.21.1 {
/*                var data = entity == null ? null : entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
                var data = entity == null ? null : entity.saveWithoutMetadata();
//?}
                saved.put(pos.immutable(), new Saved(h.getLevel().getBlockState(pos), data));
            }
            for (var face : Direction.values()) {
                player.getInventory().clearContent();
                var tool = wand("Steel");
                player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                player.getInventory().setItem(35, new ItemStack(Items.STONE, 8));
                for (var pos : saved.keySet()) {
                    h.getLevel().removeBlockEntity(pos);
                    h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(),
                            net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
                }
                h.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
                int events = listener.calls;
                click(player, support, face, .75);
                h.assertTrue(h.getLevel().getBlockState(support.relative(face)).is(Blocks.STONE), "clicked face " + face);
                for (var side : Direction.values()) if (side != face)
                    h.assertTrue(h.getLevel().isEmptyBlock(support.relative(side)), "no wrong-face copy " + side);
                h.assertTrue(player.getInventory().getItem(35).getCount() == 7, "one source consumed");
                h.assertTrue(tool.getDamageValue() == 1, "one wear committed");
                h.assertTrue(listener.calls == events + 1, "one placement event, no duplicate outer event");
            }
        } finally {
            unregister(listener);
            for (var entry : saved.entrySet()) {
                var pos = entry.getKey(); var original = entry.getValue();
                h.getLevel().removeBlockEntity(pos);
                h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
                h.getLevel().setBlock(pos, original.state(),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
                var restored = h.getLevel().getBlockEntity(pos);
                if (restored != null && original.data() != null) {
//? if >=1.21.1 {
/*                    restored.loadWithComponents(original.data(), h.getLevel().registryAccess());
*///?} else {
                    restored.load(original.data());
//?}
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "builder_placement_empty", timeoutTicks = 160)
    public static void builderUsesHeadQualityAndHitCoordinates(GameTestHelper h) {
        runInOwnedBuilderFixture(h, origin -> {
            var player = player(h);
            player.setPos(origin.getX() - 2, origin.getY() + 1, origin.getZ() - 2);
            int low = GTMaterialRegistry.get("Wood").getToolQuality() + 1;
            int high = GTMaterialRegistry.get("Steel").getToolQuality() + 1;
            h.assertTrue(low < high, "fixture has unequal real material qualities");
            try {
                for (String material : new String[]{"Wood", "Steel"}) {
                    player.getInventory().clearContent();
                    player.setItemInHand(InteractionHand.MAIN_HAND, wand(material));
                    player.getInventory().setItem(35, new ItemStack(Items.STONE, 8));
                    for (int x : new int[]{0, low, high, high + 1}) {
                        h.getLevel().setBlockAndUpdate(origin.offset(x, 0, 0), Blocks.STONE.defaultBlockState());
                        h.getLevel().setBlockAndUpdate(origin.offset(x, 1, 0), Blocks.AIR.defaultBlockState());
                    }
                    click(player, origin, Direction.UP, 1);
                    h.assertTrue(h.getLevel().getBlockState(origin.offset(low, 1, 0)).is(Blocks.STONE), "inclusive low edge");
                    h.assertTrue(h.getLevel().getBlockState(origin.offset(high, 1, 0)).is(Blocks.STONE) == material.equals("Steel"), "material-dependent high edge");
                    h.assertTrue(h.getLevel().isEmptyBlock(origin.offset(high + 1, 1, 0)), "outside high edge excluded");
                    h.assertTrue(player.getInventory().getItem(35).getCount() == (material.equals("Steel") ? 5 : 6), "plane exact count");
                }
            } finally {
                for (int x : new int[]{0, low, high, high + 1}) for (int y = 0; y <= 1; y++) h.getLevel().setBlockAndUpdate(origin.offset(x, y, 0), Blocks.AIR.defaultBlockState());
            }
            try {
                player.getInventory().clearContent();
                player.setItemInHand(InteractionHand.MAIN_HAND, wand("Steel"));
                player.getInventory().setItem(35, new ItemStack(Items.OAK_SLAB, 2));
                h.getLevel().setBlockAndUpdate(origin, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
                click(player, origin, Direction.EAST, .8);
                var placed = h.getLevel().getBlockState(origin.east());
                h.assertTrue(placed.is(Blocks.OAK_SLAB) && placed.getValue(SlabBlock.TYPE) == SlabType.TOP, "original local hit height reaches BlockItem");
            } finally {
                h.getLevel().setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
                h.getLevel().setBlockAndUpdate(origin.east(), Blocks.AIR.defaultBlockState());
            }
        });
    }

    @GameTest(template = "builder_placement_empty", timeoutTicks = 160)
    public static void builderCancellationRollsBackWorldStockAndWear(GameTestHelper h) {
        runInOwnedBuilderFixture(h, pos -> {
            var player = player(h);
            player.setPos(pos.getX() - 2, pos.getY() + 1, pos.getZ() - 2);
            var tool = wand("Steel");
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            player.getInventory().setItem(35, new ItemStack(Items.STONE, 8));
            var listener = new PlacementListener(player);
            listener.cancel = true;
            register(listener);
            try {
                h.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                h.getLevel().setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
                click(player, pos, Direction.EAST, .5);
                h.assertTrue(listener.calls == 1, "cancellation event delivered exactly once");
                h.assertTrue(h.getLevel().isEmptyBlock(pos.east()), "canceled world placement restored");
                h.assertTrue(player.getInventory().getItem(35).getCount() == 8 && tool.getDamageValue() == 0, "canceled stock and wear unchanged");
                h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(2)).isEmpty(), "cancellation creates no loose item refund");
                listener.cancel = false;
                click(player, pos, Direction.EAST, .5);
                h.assertTrue(listener.calls == 2 && player.getInventory().getItem(35).getCount() == 7 && tool.getDamageValue() == 1,
                        "retry commits one placement and one cost");
                h.getLevel().setBlockAndUpdate(pos.east(), Blocks.BEDROCK.defaultBlockState());
                click(player, pos, Direction.EAST, .5);
                h.assertTrue(listener.calls == 2 && player.getInventory().getItem(35).getCount() == 7 && tool.getDamageValue() == 1,
                        "blocked destination consumes neither source nor wear");
                h.getLevel().setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
                player.setGameMode(GameType.CREATIVE);
                click(player, pos, Direction.EAST, .5);
                h.assertTrue(listener.calls == 3 && player.getInventory().getItem(35).getCount() == 7 && tool.getDamageValue() == 1,
                        "creative builder preserves source and wear");
            } finally {
                unregister(listener);
                h.getLevel().setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
                h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        });
    }

    /** Owns both full scan planes and their destinations; never retries transaction assertions. */
    private static void runInOwnedBuilderFixture(GameTestHelper h, java.util.function.Consumer<BlockPos> body) {
        int radius = Math.max(Math.max(0, GTToolHelper.getHead(wand("Wood")).getToolQuality() + 1),
                Math.max(0, GTToolHelper.getHead(wand("Steel")).getToolQuality() + 1));
        // Current source qualities: Wood=0 (reach 1), Steel=2 (reach 3).
        // +/-radius covers UP and EAST source planes. EAST adds +1 X; the quality test
        // additionally probes X=radius+1. One further shell cell gives size (11,10,10).
        var origin = h.absolutePos(new BlockPos(radius + 2, radius + 2, radius + 2));
        var low = origin.offset(-radius - 1, -radius - 1, -radius - 1);
        var high = origin.offset(radius + 2, radius + 1, radius + 1);
        var owned = new java.util.HashSet<BlockPos>();
        h.forEveryBlockInStructure(local -> owned.add(h.absolutePos(local)));
        for (var at : BlockPos.betweenClosed(low, high)) {
            h.assertTrue(owned.contains(at), "complete builder chamber is inside dedicated fixture: " + at);
            h.assertTrue(!at.equals(h.absolutePos(BlockPos.ZERO)), "builder chamber excludes structure controller");
        }
        h.startSequence().thenWaitUntil(() -> {
            for (var at : BlockPos.betweenClosed(low, high)) {
                h.assertTrue(h.getLevel().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(at))
                                && h.getLevel().isPositionEntityTicking(at),
                        "waiting for loaded/ticking builder chamber: " + at);
            }
        }).thenExecute(() -> {
            int quiet = net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                    | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
            try {
                for (var at : BlockPos.betweenClosed(low, high)) {
                    boolean shell = at.getX() == low.getX() || at.getX() == high.getX()
                            || at.getY() == low.getY() || at.getY() == high.getY()
                            || at.getZ() == low.getZ() || at.getZ() == high.getZ();
                    h.getLevel().setBlock(at, (shell ? Blocks.STONE : Blocks.AIR).defaultBlockState(), quiet);
                }
                // Adversarial pre-clear witnesses for the two old source-plane assumptions.
                h.getLevel().setBlock(origin.west(), Blocks.STONE.defaultBlockState(), quiet);
                h.getLevel().setBlock(origin.below(), Blocks.STONE.defaultBlockState(), quiet);
                h.assertTrue(h.getLevel().getBlockState(origin.west()).is(Blocks.STONE)
                                && h.getLevel().getBlockState(origin.below()).is(Blocks.STONE),
                        "extra matching source cells exist before explicit isolation");
                var liquidPositions = new BlockPos[]{origin.offset(radius + 1, 1, 0), origin.east()};
                var liquids = new net.minecraft.world.level.block.Block[]{Blocks.WATER, Blocks.LAVA};
                for (int i = 0; i < liquidPositions.length; i++) {
                    var at = liquidPositions[i];
                    h.getLevel().setBlock(at, liquids[i].defaultBlockState(), quiet);
                    h.getLevel().removeBlock(at, false);
                    h.assertTrue(h.getLevel().getBlockState(at).is(liquids[i]),
                            "removeBlock preserves seeded liquid before explicit builder clearing");
                }
                // All scan sources, destinations and the outside-radius guard are dry, not
                // merely the few cells each transaction subsequently prepares as its supports.
                for (var at : BlockPos.betweenClosed(low.offset(1, 1, 1), high.offset(-1, -1, -1))) {
                    h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), quiet);
                    h.assertTrue(h.getLevel().isEmptyBlock(at) && h.getLevel().getFluidState(at).isEmpty(),
                            "complete builder interior is explicit dry air: " + at);
                }
                var chamber = new net.minecraft.world.phys.AABB(low.getX(), low.getY(), low.getZ(),
                        high.getX() + 1, high.getY() + 1, high.getZ() + 1);
                // Only ambient setup drops; transaction-time refund assertions remain unfiltered.
                h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, chamber)
                        .forEach(net.minecraft.world.entity.item.ItemEntity::discard);
                body.accept(origin);
            } finally {
                for (var at : BlockPos.betweenClosed(low, high))
                    h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), quiet);
            }
            h.succeed();
        });
    }

    @GameTest(template = "torch_placement_empty", timeoutTicks = 160)
    public static void drillTorchCancellationAndCreativePreserveInventory(GameTestHelper h) {
        var owned = new java.util.HashSet<BlockPos>();
        h.forEveryBlockInStructure(local -> owned.add(h.absolutePos(local)));
        h.startSequence().thenWaitUntil(() -> {
            for (var local : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(7, 7, 7))) {
                var at = h.absolutePos(local);
                h.assertTrue(owned.contains(at), "torch chamber is inside dedicated nine-cube fixture");
                h.assertTrue(h.getLevel().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(at))
                                && h.getLevel().isPositionEntityTicking(at),
                        "waiting for loaded/ticking torch chamber: " + at);
            }
        }).thenExecute(() -> {
            // Only readiness retries: setup and all three native transaction cycles execute once.
            var low = h.absolutePos(new BlockPos(1, 1, 1));
            var high = h.absolutePos(new BlockPos(7, 7, 7));
            int quiet = net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                    | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
            try {
                for (var at : BlockPos.betweenClosed(low, high)) {
                    boolean shell = at.getX() == low.getX() || at.getX() == high.getX()
                            || at.getY() == low.getY() || at.getY() == high.getY()
                            || at.getZ() == low.getZ() || at.getZ() == high.getZ();
                    h.getLevel().setBlock(at, (shell ? Blocks.STONE : Blocks.AIR).defaultBlockState(), quiet);
                }
                // Discard only ambient setup drops; no item cleanup occurs during/after transactions.
                var chamber = new net.minecraft.world.phys.AABB(low.getX(), low.getY(), low.getZ(),
                        high.getX() + 1, high.getY() + 1, high.getZ() + 1);
                h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, chamber)
                        .forEach(net.minecraft.world.entity.item.ItemEntity::discard);
                for (var initial : new net.minecraft.world.level.block.Block[]{Blocks.AIR, Blocks.WATER, Blocks.LAVA}) {
                    torchCycle(h, initial);
                    h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                            chamber).isEmpty(), "no loose refund anywhere in chamber after " + initial);
                }
            } finally {
                for (var at : BlockPos.betweenClosed(low, high))
                    h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), quiet);
            }
            h.succeed();
        });
    }

    private static int torchCount(ServerPlayer player) {
        return java.util.stream.IntStream.range(0, player.getInventory().getContainerSize())
                .map(slot -> { var stack = player.getInventory().getItem(slot);
                    return stack.is(Items.TORCH) ? stack.getCount() : 0; }).sum();
    }

    private static void torchCycle(GameTestHelper h, net.minecraft.world.level.block.Block initial) {
        var player = player(h);
        var pos = h.absolutePos(new BlockPos(4, 3, 4));
        player.setPos(pos.getX() - 2, pos.getY() + 1, pos.getZ() - 2);
        var drill = GTElectricItems.get("electric_mining_drill").assembled(GTMaterialRegistry.get("Steel"), 10000);
//? if >=1.21.1 {
        /*gregtech.platform.neoforge.StackCustomData.update(drill, tag -> tag.putLong("gt.charge", 1000));
*///?} else {
        drill.getOrCreateTag().putLong("gt.charge", 1000);
//?}
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        player.getInventory().setItem(35, new ItemStack(Items.TORCH, 8));
        var listener = new PlacementListener(player);
        register(listener);
        try {
            h.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            // Adversarial native proof: removeBlock preserves a liquid's legacy block, not air.
            h.getLevel().setBlock(pos.above(), initial.defaultBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
            h.getLevel().removeBlock(pos.above(), false);
            h.assertTrue(h.getLevel().getBlockState(pos.above()).is(initial),
                    "native removeBlock retains seeded air/water/lava before explicit fixture clearing");
            h.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            h.assertTrue(h.getLevel().isEmptyBlock(pos.above()) && h.getLevel().getFluidState(pos.above()).isEmpty(),
                    "owned destination is explicitly dry air before native interaction");
            listener.cancel = true;
            click(player, pos, Direction.UP, 1);
            h.assertTrue(listener.calls == 1 && h.getLevel().isEmptyBlock(pos.above()), "torch event denied and world restored");
            h.assertTrue(player.getInventory().getItem(35).getCount() == 8, "canceled torch stock retained");
            h.assertTrue(torchCount(player) == 8, "cancellation retains exactly eight torches across all slots");
            h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(2)).isEmpty(), "no loose torch refund or duplication");
            listener.cancel = false;
            click(player, pos, Direction.UP, 1);
            h.assertTrue(listener.calls == 2 && h.getLevel().getBlockState(pos.above()).is(Blocks.TORCH), "one accepted event, no recursion/double placement");
            h.assertTrue(player.getInventory().getItem(35).getCount() == 7, "successful torch costs exactly one");
            h.assertTrue(torchCount(player) == 7, "successful placement leaves exactly seven torches across all slots");
            h.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            player.setGameMode(GameType.CREATIVE);
            click(player, pos, Direction.UP, 1);
            h.assertTrue(listener.calls == 3 && player.getInventory().getItem(35).getCount() == 7, "creative placement keeps torch stock");
            h.assertTrue(torchCount(player) == 7, "creative placement leaves exactly seven torches across all slots");
            long charge = ((gregtech.content.item.ElectricToolItem) drill.getItem()).getEnergyStored(drill,
                    gregtech.content.data.GregTechTags.Energy.EU);
            h.assertTrue(charge == 1000 && player.getMainHandItem() == drill, "held drill identity/charge unchanged");
        } finally {
            unregister(listener);
            h.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            player.getInventory().clearContent();
        }
    }
}
