package gregtech.gametest;

import gregtech.bootstrap.registration.OriginCreativeContents;
import gregtech.block.stone.StoneType;
import gregtech.worldgen.GTDungeonFeature;
import gregtech.worldgen.dungeon.GTDungeonData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import gregtech.block.tool.DynamiteBlock;
import gregtech.content.tool.DynamiteSubstrates;
import gregtech.registry.GTElectricItems;
import gregtech.registry.GTToolBlocks;
import gregtech.api.material.GTMaterialRegistry;
import gregtech.block.misc.PileBlock;
import gregtech.blockentity.misc.PileBlockEntity;
import gregtech.registry.GTItems;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;

/** Focused regressions for issues #64, #77, #79, #80 and #81; not client visual tests. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class OpenIssueRegressionTests {
    private static final BlockPos SUPPORT = new BlockPos(2,2,2);
    private static Player player(GameTestHelper h, boolean creative) {
        var player = new Player(h.getLevel(), BlockPos.ZERO, 0,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "drill-test")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return creative; }
        };
        player.getAbilities().instabuild = creative;
        player.getAbilities().mayBuild = true;
        var drill = new ItemStack(GTElectricItems.ELECTRIC_DRILL.get());
        drill.getOrCreateTag().putLong("gt.charge", 1000);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        player.getInventory().items.set(35, new ItemStack(GTToolBlocks.DYNAMITE.get(), 3));
        return player;
    }
    private static boolean place(GameTestHelper h, Player player, Direction face) {
        var support = h.absolutePos(SUPPORT);
        var hit = new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)), face, support, false);
        var drill = player.getMainHandItem();
        return drill.getItem().onItemUseFirst(drill, new UseOnContext(h.getLevel(), player,
                InteractionHand.MAIN_HAND, drill, hit)).consumesAction();
    }

    @GameTest(template="test_empty")
    public static void extruderShapesDoNotSplitTheMachineCreativeFamily(GameTestHelper h) {
        int shapes = 0;
        for (var item : ForgeRegistries.ITEMS.getValues()) {
            var id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null || !id.getNamespace().equals("gt6ce")) continue;
            if (id.getPath().startsWith("extruder_shape_") || id.getPath().startsWith("low_heat_extruder_shape_")) {
                h.assertTrue(OriginCreativeContents.family(item).equals("molds"), "shape belongs to molds: " + id);
                shapes++;
            }
        }
        h.assertTrue(shapes > 0, "actual shape registrations were checked");
        var invar = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gt6ce", "extruder_invar"));
        h.assertTrue(invar != null && OriginCreativeContents.family(invar).equals("basic_machines"),
                "invar extruder stays with machines rather than its shapes");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void dungeonWaterPlacementSchedulesFlowOnlyAfterSuccessfulWrite(GameTestHelper h) {
        boolean[] success = {true};
        var ticks = new ArrayList<Object[]>();
        WorldGenLevel recorder = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("setBlock")) return success[0];
                    if (method.getName().equals("scheduleTick")) { ticks.add(args.clone()); return null; }
                    throw new AssertionError("unexpected world access: " + method.getName());
                });
        var data = new GTDungeonData(recorder, 32, 40, 48, StoneType.LIMESTONE, StoneType.SLATE, 3,
                new byte[5][5], 2, 2, 0, new long[GTDungeonFeature.KEY_COUNT],
                new ItemStack[GTDungeonFeature.KEY_COUNT], new boolean[GTDungeonFeature.KEY_COUNT],
                new HashSet<>(), new HashSet<>(), RandomSource.create(79));
        h.assertTrue(data.set(1, 10, 1, Blocks.WATER.defaultBlockState()), "source placement succeeds");
        h.assertTrue(ticks.size() == 1 && ticks.get(0)[0].equals(new BlockPos(33, 50, 49))
                        && ticks.get(0)[1] == Fluids.WATER && ((Integer) ticks.get(0)[2]) > 0,
                "source gets a deferred water tick at the exact world position");
        data.set(2, 10, 1, Blocks.STONE.defaultBlockState());
        h.assertTrue(ticks.size() == 1, "dry blocks do not schedule fluid ticks");
        success[0] = false;
        h.assertTrue(!data.set(3, 10, 1, Blocks.WATER.defaultBlockState()), "failed placement is preserved");
        h.assertTrue(ticks.size() == 1, "failed water writes do not schedule phantom ticks");
        h.succeed();
    }
    /** Issues #80/#81: the host state, not an ore tag, controls embedded charges. */
    @GameTest(template="test_empty")
    public static void gtOreHostsAdmitSmallOreAndRejectBedrockWithoutConsumption(GameTestHelper h) {
        // The current LV drill catalog charges 200 EU per use (not the old fixture's 100 EU).
        h.assertTrue(gregtech.content.tool.ElectricToolCatalog.get("electric_drill").energyPerUse() == 200,
                "LV drill contract is exactly 200 EU per use");
        for (String prefix : java.util.List.of("ore_iron", "ore_small_iron")) {
            var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", prefix);
            var block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(id);
            h.assertTrue(block instanceof gregtech.block.OreBlock, "registered GT ore: " + id);
            for (var host : java.util.List.of(gregtech.block.OreHostStone.STONE,
                    gregtech.block.OreHostStone.DEEPSLATE,
                    gregtech.block.OreHostStone.BEDROCK)) {
                h.setBlock(SUPPORT.above(), Blocks.AIR);
                h.setBlock(SUPPORT, block.defaultBlockState().setValue(gregtech.block.OreBlock.STONE, host));
                var player = player(h, false);
                boolean allowed = host != gregtech.block.OreHostStone.BEDROCK;
                h.assertTrue(DynamiteSubstrates.canDrill(h.getLevel(), h.absolutePos(SUPPORT)) == allowed,
                        id + " admission for " + host);
                h.assertTrue(place(h, player, Direction.UP) == allowed, id + " placement for " + host);
                int remainingCharges = player.getInventory().items.get(35).getCount();
                long remainingEnergy = player.getMainHandItem().getTag().getLong("gt.charge");
                h.assertTrue(remainingCharges == (allowed ? 2 : 3),
                        id + " / " + host + ": expected dynamite count " + (allowed ? 2 : 3)
                                + ", actual " + remainingCharges);
                h.assertTrue(remainingEnergy == (allowed ? 800 : 1000),
                        id + " / " + host + ": expected EU " + (allowed ? 800 : 1000)
                                + ", actual " + remainingEnergy);
                if (allowed) {
                    var target = h.absolutePos(SUPPORT.above());
                    var state = h.getLevel().getBlockState(target);
                    h.assertTrue(state.getValue(DynamiteBlock.SUNK), "GT ore charge is embedded");
                    ((DynamiteBlock) state.getBlock()).refreshSupport(h.getLevel(), target);
                    h.assertTrue(h.getLevel().getBlockState(target).getValue(DynamiteBlock.SUNK),
                            "small and normal ore preserve embedded support");
                } else h.assertTrue(h.getBlockState(SUPPORT.above()).isAir(), "bedrock ore creates no charge");
            }
        }
        h.succeed();
    }

    /** Issue #77: cover interception must preserve native sneak-add and clear the final snapshot. */
    @GameTest(template = "test_empty")
    public static void pileCoverEventPreservesSneakAddAndEmptySnapshotClearsLegacyCover(GameTestHelper helper) {
        var kind = PileBlock.Kind.PLATE;
        ItemStack material = ItemStack.EMPTY;
        for (var candidate : GTMaterialRegistry.sortedMaterials()) {
            material = GTItems.getStack(kind.prefix(), candidate);
            if (!material.isEmpty()) break;
        }
        helper.assertTrue(!material.isEmpty() && gregtech.content.cover.CoverItems.isCover(material),
                "plate material also qualifies as a cover, reproducing interception");
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();
        level.setBlock(pos, kind.block().defaultBlockState(), 3);
        var pile = (PileBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(pile != null && pile.add(material.copyWithCount(1)) == 1, "initial plate pile");
        var player = helper.makeMockPlayer();
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, material.copyWithCount(2));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false);
        var event = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                player, InteractionHand.MAIN_HAND, pos, hit);
        gregtech.content.cover.ComponentCoverEvents.use(event);
        helper.assertTrue(!event.isCanceled()
                        && event.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.ALLOW,
                "sneak-add reaches the native block callback, not cover placement");
        helper.assertTrue(level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hit).consumesAction()
                        && pile.count() == 3 && player.getMainHandItem().isEmpty(),
                "native callback adds both held plates");
        helper.assertTrue(!gregtech.content.cover.ComponentCoverFallback.eligible(pile),
                "piles cannot receive new fallback covers");
        var host = (gregtech.content.cover.FallbackCoverHost) (Object) pile;
        var storage = host.gregtechComponentStorage(true);
        // Reproduce an old-save cover directly; new cover attachment is intentionally forbidden.
        var legacy = new net.minecraft.nbt.CompoundTag();
        legacy.put("gt_cover_" + Direction.UP.ordinal(), material.copyWithCount(1).save(new net.minecraft.nbt.CompoundTag()));
        storage.load(legacy);
        helper.assertTrue(!host.getCover(Direction.UP).isEmpty(), "legacy cover seeded");
        gregtech.content.cover.ComponentCoverFallback.read(pile, new net.minecraft.nbt.CompoundTag());
        helper.assertTrue(!host.getCover(Direction.UP).isEmpty(), "partial update preserves existing cover");
        storage.remove(Direction.UP);
        var emptySnapshot = new net.minecraft.nbt.CompoundTag();
        gregtech.content.cover.ComponentCoverFallback.write(pile, emptySnapshot);
        helper.assertTrue(emptySnapshot.getBoolean("gt.component_covers"), "last removal writes authoritative empty marker");
        storage.load(legacy); // Simulate receiver still holding the previous snapshot.
        gregtech.content.cover.ComponentCoverFallback.read(pile, emptySnapshot);
        helper.assertTrue(host.getCover(Direction.UP).isEmpty(), "authoritative empty snapshot removes stale last cover");
        helper.succeed();
    }

}
