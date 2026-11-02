package gregtech.gametest;

import gregtech.api.multiblock.StructureRole;
import gregtech.api.addon.platform.AddonStructures;
import gregtech.api.addon.platform.StructureProblem;
import gregtech.content.blockentity.machine.BehaviorMachineBlockEntity;
import gregtech.content.multiblock.LargeMachineParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MultiblockBehaviorApiTests {
    @GameTest(template = "test_empty", timeoutTicks = 180)
    public static void partsOpenTheControllerMenuAndInvalidateWithTheStructure(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(1, 1, 1));
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce_api_test", "structure_api_test"));
        level.setBlockAndUpdate(origin, block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        var machine = (BehaviorMachineBlockEntity) level.getBlockEntity(origin);
        for (int y = 0; y < 2; y++) {
            level.setBlockAndUpdate(origin.offset(1, y, 0), LargeMachineParts.block(18009).defaultBlockState());
            level.setBlockAndUpdate(origin.offset(2, y, 0), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(origin.offset(3, y, 0), Blocks.OAK_LOG.defaultBlockState());
        }
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk(), "Menu fixture did not form"))
                .thenExecute(() -> {
                    var player = menuPlayer(helper);
                    try {
                        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
                        for (var part : java.util.List.of(origin.east(), origin.east(2), origin.east(3).above())) {
                            player.setPos(part.getX() + 0.5, part.getY() + 1, part.getZ() + 0.5);
                            var hit = new net.minecraft.world.phys.BlockHitResult(part.getCenter(), Direction.UP, part, false);
                            var result = player.gameMode.useItemOn(player, level, net.minecraft.world.item.ItemStack.EMPTY, hand, hit);
                            helper.assertTrue(result.consumesAction(), "Part click was not handled: " + part);
                            helper.assertTrue(player.containerMenu instanceof gregtech.client.gui.BehaviorMachineMenu menu
                                    && menu.machine() == machine && menu.stillValid(player), "Part opened the wrong menu or closed it immediately");
                            player.closeContainer();
                        }
                        var part = origin.east();
                        player.setPos(part.getX() + 0.5, part.getY() + 1, part.getZ() + 0.5);
                        player.setShiftKeyDown(true);
                        helper.assertTrue(gregtech.engine.multiblock.MultiblockMenus.use(player, hand, part)
                                == net.minecraft.world.InteractionResult.PASS, "Sneaking was intercepted");
                        player.setShiftKeyDown(false);
                        player.setItemInHand(hand, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
                        helper.assertTrue(gregtech.engine.multiblock.MultiblockMenus.use(player, hand, part)
                                == net.minecraft.world.InteractionResult.PASS, "Fluid container interaction was intercepted");
                        player.setItemInHand(hand, net.minecraft.world.item.ItemStack.EMPTY);
                        helper.assertTrue(gregtech.engine.multiblock.MultiblockMenus.use(player, hand, part).consumesAction(), "Part menu did not reopen");
                        var menu = player.containerMenu;
                        player.setPos(part.getX() + 30, part.getY(), part.getZ());
                        helper.assertTrue(!menu.stillValid(player), "Part menu ignored distance");
                        player.setPos(part.getX() + 0.5, part.getY() + 1, part.getZ() + 0.5);
                        level.setBlockAndUpdate(origin.east(3).above(), Blocks.AIR.defaultBlockState());
                        helper.assertTrue(!menu.stillValid(player), "Part menu survived a broken structure");
                        player.closeContainer();
                        helper.assertTrue(gregtech.engine.multiblock.MultiblockMenus.use(player, hand, part)
                                == net.minecraft.world.InteractionResult.PASS, "Incomplete structure opened a menu through its retained binding");
                    } finally {
                        player.closeContainer();
                        player.discard();
                    }
                })
                .thenSucceed();
    }

    /** A real server menu/session without pretending to complete optional mods' client handshakes. */
    public static net.minecraft.server.level.ServerPlayer menuPlayer(GameTestHelper helper) {
        var level = helper.getLevel();
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "menu-test");
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
//? if >=1.21.1 {
        /*var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), connection, player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
*///?} else {
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), connection, player) {
//?}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) {}
        };
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        return player;
    }

    @GameTest(template = "test_empty", timeoutTicks = 180)
    public static void addonRulesCellsAndLifecycleUseTheHostMatcher(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(1, 1, 1));
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce_api_test", "structure_api_test"));
        helper.assertTrue(block != Blocks.AIR, "Test addon did not register through the public API");
        level.setBlockAndUpdate(origin, block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        var machine = (BehaviorMachineBlockEntity) level.getBlockEntity(origin);
        var behavior = (StructureApiTestAddon.Behavior) machine.behavior();
        var steel = LargeMachineParts.block(18009).defaultBlockState();
        var titanium = LargeMachineParts.block(18006).defaultBlockState();
        for (int y = 0; y < 2; y++) {
            level.setBlockAndUpdate(origin.offset(1, y, 0), steel);
            level.setBlockAndUpdate(origin.offset(2, y, 0), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(origin.offset(3, y, 0), Blocks.OAK_LOG.defaultBlockState());
        }
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk() && behavior.cells.size() == 6, "API structure did not form"))
                .thenExecute(() -> {
                    helper.assertTrue(behavior.formations == 1 && behavior.losses == 0 && behavior.ordered, "Formation callback order/count");
                    assertDesign(helper, origin.east(), 3);
                    assertDesign(helper, origin.east().above(), 3);
                    helper.assertTrue(behavior.cells.stream().filter(c -> c.role() == StructureRole.ENERGY_INPUT).count() == 2, "Lost ordinary part roles");
                    helper.assertTrue(behavior.cells.stream().filter(c -> c.state().is(Blocks.STONE)).count() == 2, "Plain blocks missing from cell view");
                    level.setBlockAndUpdate(origin.offset(2, 1, 0), Blocks.DIRT.defaultBlockState());
                    behavior.diagnose = true;
                })
                .thenWaitUntil(() -> helper.assertTrue(!machine.isStructureOk() && behavior.losses == 1 && !behavior.diagnose, "Wrong matched alternative count was accepted"))
                .thenExecute(() -> {
                    assertDesign(helper, origin.east(), 0);
                    assertDesign(helper, origin.east().above(), 0);
                    helper.assertTrue(behavior.problems.stream().anyMatch(problem ->
                        problem instanceof StructureProblem.Count count && count.actual() == 1 && count.minimum() == 2
                                && count.maximum() == 2 && count.selection().contains("minecraft:stone")),
                        "Behavior diagnostic did not expose the actual count and constraint");
                })
                .thenExecute(() -> level.setBlockAndUpdate(origin.offset(2, 1, 0), Blocks.STONE.defaultBlockState()))
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk() && behavior.formations == 2, "Count repair did not reform"))
                .thenExecute(() -> level.setBlockAndUpdate(origin.offset(1, 1, 0), titanium))
                .thenWaitUntil(() -> helper.assertTrue(!machine.isStructureOk() && behavior.losses == 2, "Mixed part blocks were accepted"))
                .thenExecute(() -> helper.assertTrue(AddonStructures.diagnose(level, origin).stream().anyMatch(problem ->
                        problem instanceof StructureProblem.DifferentBlocks different
                                && different.position().equals(origin.offset(1, 1, 0))
                                && different.firstPosition().equals(origin.offset(1, 0, 0))
                                && !different.first().equals(different.found())), "Mixed-material diagnostic lost block identities or positions"))
                .thenExecute(() -> level.setBlockAndUpdate(origin.offset(1, 0, 0), titanium))
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk() && behavior.formations == 3, "Uniform alternative parts were rejected"))
                .thenExecute(() -> level.setBlockAndUpdate(origin.offset(3, 1, 0), Blocks.SPRUCE_LOG.defaultBlockState()))
                .thenWaitUntil(() -> helper.assertTrue(!machine.isStructureOk() && behavior.losses == 3, "Same-tag replacement did not invalidate the block-identity rule"))
                .thenExecute(() -> level.setBlockAndUpdate(origin.offset(3, 0, 0), Blocks.SPRUCE_LOG.defaultBlockState()))
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk() && behavior.formations == 4, "Uniform tagged blocks were rejected"))
                .thenExecute(() -> {
                    level.setBlockAndUpdate(origin.offset(1, 2, 0), titanium);
                    level.setBlockAndUpdate(origin.offset(2, 2, 0), Blocks.DIRT.defaultBlockState());
                    level.setBlockAndUpdate(origin.offset(3, 2, 0), Blocks.SPRUCE_LOG.defaultBlockState());
                })
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk() && behavior.repeats == 2 && behavior.cells.size() == 9, "Repeat cells or constraints were lost"))
                .thenExecute(() -> {
                    helper.assertTrue(AddonStructures.diagnose(level, origin).isEmpty()
                            && AddonStructures.diagnose(level, origin.east()).isEmpty(), "Complete controller/part reported a false failure");
                    assertDesign(helper, origin.east(), 3);
                    assertDesign(helper, origin.east().above(2), 3);
                    int previous = behavior.losses;
                    level.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
                    helper.assertTrue(behavior.losses == previous + 1 && behavior.ordered, "Removal did not invalidate the API exactly once");
                    assertDesign(helper, origin.east(), 0);
                    assertDesign(helper, origin.east().above(2), 0);
                })
                .thenSucceed();
    }

    private static void assertDesign(GameTestHelper helper, BlockPos pos, int expected) {
        var part = (gregtech.content.blockentity.machine.MultiblockPortBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(part != null && part.structureDesign() == expected,
                "Wrong formed/released part design at " + pos + ", expected " + expected);
//? if >=1.21.1 {
        /*var tag = part.getUpdateTag(helper.getLevel().registryAccess());
*///?} else {
        var tag = part.getUpdateTag();
//?}
        helper.assertTrue((tag.getByte("gt.design") & 255) == expected, "Part appearance missing from client update");
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void diagnosticDetailsDoNotClaimPartsOrLoadChunks(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(1, 1, 1));
        var other = helper.absolutePos(new BlockPos(0, 1, 4));
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce_api_test", "structure_api_test"));
        var state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        level.setBlockAndUpdate(origin, state);
        level.setBlockAndUpdate(other, state);
        var part = origin.east();
        level.setBlockAndUpdate(part, LargeMachineParts.block(18009).defaultBlockState());
        var claims = gregtech.engine.multiblock.tracking.WorldStructureClaims.table(level);
        helper.assertTrue(gregtech.engine.multiblock.tracking.WorldStructureClaims.claim(level, part, other), "Fixture could not claim part");
        var problems = AddonStructures.diagnose(level, origin);
        helper.assertTrue(problems.stream().anyMatch(problem -> problem instanceof StructureProblem.Occupied occupied
                && occupied.position().equals(part) && occupied.controller().equals(other)), "Missing owner diagnostic");
        helper.assertTrue(problems.stream().anyMatch(problem -> problem instanceof StructureProblem.WrongBlock wrong
                && wrong.position().equals(origin.east(2)) && wrong.expected().containsAll(java.util.List.of("minecraft:stone", "minecraft:dirt"))
                && wrong.found().equals(BuiltInRegistries.BLOCK.getKey(Blocks.AIR))), "Missing alternative block expectations");
        helper.assertTrue(problems.stream().anyMatch(problem -> problem instanceof StructureProblem.Layers layers
                && layers.group().equals("upper") && layers.actual() == 0 && layers.minimum() == 1), "Missing repeat-layer diagnostic");
        helper.assertTrue(claims.owner(part.asLong()) == other.asLong(), "Diagnostic stole or released a part");
        helper.assertTrue(AddonStructures.describe(problems.get(0)).getContents()
                instanceof net.minecraft.network.chat.contents.TranslatableContents, "Diagnostic text was not localizable");
        var far = origin.offset(10000, 0, 10000);
        helper.assertTrue(!level.hasChunkAt(far), "Fixture target was already loaded");
        helper.assertTrue(AddonStructures.diagnose(level, far).get(0) instanceof StructureProblem.Unloaded
                && !level.hasChunkAt(far), "Diagnostic loaded a chunk");
        helper.succeed();
    }
}
