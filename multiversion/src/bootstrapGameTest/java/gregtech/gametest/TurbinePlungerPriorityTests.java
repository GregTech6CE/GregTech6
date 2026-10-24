package gregtech.gametest;

import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.tool.GTToolHelper;
import gregtech.internal.tool.GTToolType;
import gregtech.content.blockentity.machine.LargeTurbineControllerBlockEntity;
import gregtech.content.blockentity.machine.TankBlockEntity;
import gregtech.content.multiblock.AxialGeneratorDefinitions;
import gregtech.content.multiblock.AxialStructureTransform;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTToolItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** DRAFT: original-semantic expectations; first two cases should fail on pre-fix source. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class TurbinePlungerPriorityTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static CompoundTag save(GameTestHelper h, BlockEntity entity) {
//? if >=1.21.1 {
/*        return entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return entity.saveWithoutMetadata();
//?}
    }

    private static void load(GameTestHelper h, BlockEntity entity, CompoundTag data) {
//? if >=1.21.1 {
/*        entity.loadWithComponents(data, h.getLevel().registryAccess());
*///?} else {
        entity.load(data);
//?}
    }

    private static IFluidHandler fluids(GameTestHelper h, BlockPos pos, Direction side) {
//? if >=1.21.1 {
/*        var result = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, side);
        if (result == null) throw new IllegalStateException("missing native fluid capability");
        return result;
*///?} else {
        var entity = h.getLevel().getBlockEntity(pos);
        if (entity == null) throw new IllegalStateException("missing native block entity");
        return entity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, side)
                .orElseThrow(() -> new IllegalStateException("missing native fluid capability"));
//?}
    }

    @SuppressWarnings("unchecked")
    private static void tick(GameTestHelper h, LargeTurbineControllerBlockEntity turbine) {
        var type = (BlockEntityType<LargeTurbineControllerBlockEntity>) (BlockEntityType<?>) turbine.getType();
        var ticker = ((EntityBlock) turbine.getBlockState().getBlock()).getTicker(h.getLevel(), turbine.getBlockState(), type);
        h.assertTrue(ticker != null, "registered native steam-turbine ticker exists");
        ticker.tick(h.getLevel(), turbine.getBlockPos(), turbine.getBlockState(), turbine);
    }

    private static ItemStack plunger() {
        return GTToolHelper.write(GTToolItems.empty(GTToolType.PLUNGER),
                GTMaterialRegistry.get("Steel"), GTMaterialRegistry.get("Wood"));
    }

    private static void sameProcessing(GameTestHelper h, CompoundTag before, CompoundTag after) {
        h.assertTrue(after.getLong("gt.pending") == before.getLong("gt.pending")
                        && after.getLong("gt.axial.energy") == before.getLong("gt.axial.energy")
                        && after.getInt("gt.steam_counter") == before.getInt("gt.steam_counter")
                        && after.getBoolean("gt.stopped") == before.getBoolean("gt.stopped")
                        && after.getBoolean("gt.overloaded") == before.getBoolean("gt.overloaded"),
                "plunger only purges selected fluid, not pending conversion, remainder, energy or controls");
    }

    private static final class Fixture implements AutoCloseable {
        private record Saved(BlockState state, CompoundTag data) {}
        final GameTestHelper h;
        final BlockPos pos;
        final java.util.Map<BlockPos, Saved> saved = new java.util.LinkedHashMap<>();
        final ServerPlayer player;
        final ItemStack tool;
        LargeTurbineControllerBlockEntity turbine;

        Fixture(GameTestHelper h, int index) {
            this.h = h;
            // Isolated from test_empty (1x1x1), natural terrain and other suite fixtures.
            pos = new BlockPos(21_800_000 + 128 * index, 200, 21_800_000);
//? if >=1.21.1 {
/*            player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
            player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[gt-plunger]"));
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.setShiftKeyDown(false);
            player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() - 2);
            tool = plunger();
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        }

        void set(BlockPos at, BlockState state) {
            if (!saved.containsKey(at)) {
                var entity = h.getLevel().getBlockEntity(at);
                saved.put(at.immutable(), new Saved(h.getLevel().getBlockState(at), entity == null ? null : save(h, entity)));
            }
            h.getLevel().removeBlockEntity(at);
            h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(at, state, FLAGS);
        }

        void clear() {
            for (var at : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 6)))
                set(at, Blocks.AIR.defaultBlockState());
        }

        void buildTurbine() {
            clear();
            var block = AxialGeneratorDefinitions.blocks(true).get(0).get();
            var grade = AxialGeneratorDefinitions.grade(block);
            set(pos, block.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH));
            for (var cell : grade.cells().entrySet()) {
                var offset = cell.getKey();
                set(AxialStructureTransform.at(pos, Direction.NORTH, offset.getX(), offset.getY(), offset.getZ()),
                        cell.getValue().defaultBlockState());
            }
            turbine = (LargeTurbineControllerBlockEntity) h.getLevel().getBlockEntity(pos);
            h.assertTrue(StructureTestSupport.ok(turbine), "actual registered turbine and 35 native casing blocks form");
        }

        void makeCondensate() {
            var inlet = fluids(h, pos, Direction.NORTH);
            h.assertTrue(inlet.fill(GTFluids.stack("Steam", 17000), IFluidHandler.FluidAction.EXECUTE) == 17000,
                    "real native inlet accepts 17000 mB steam");
            tick(h, turbine);
            h.assertTrue(turbine.getFluidInTank(0).isEmpty()
                            && turbine.getFluidInTank(1).getAmount() == 100
                            && turbine.getFluidInTank(1).getFluid() == GTFluids.still("DistW").get(),
                    "actual registered ticker consumes steam and produces 100 mB distilled water");
            var data = save(h, turbine);
            h.assertTrue(data.getLong("gt.pending") == 8500 && data.getInt("gt.steam_counter") == 0
                            && !turbine.isOverloaded(),
                    "real first conversion retains second half without overload or fabricated tank NBT");
        }

        void click() {
            // Neither call purgeFluid nor block.use directly: exercise native loader event -> item-first -> block path.
            player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(pos.getX() + .5, pos.getY() + .5, pos.getZ()), Direction.NORTH, pos, false));
        }

        @Override public void close() {
            player.closeContainer();
            player.getInventory().clearContent();
            player.discard();
            for (var at : saved.keySet()) {
                h.getLevel().removeBlockEntity(at);
                h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
            }
            for (var entry : saved.entrySet()) {
                h.getLevel().setBlock(entry.getKey(), entry.getValue().state(), FLAGS);
                var entity = h.getLevel().getBlockEntity(entry.getKey());
                if (entity != null && entry.getValue().data() != null) load(h, entity, entry.getValue().data());
            }
        }
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void nativePlungerPurgesSteamBeforeRealCondensate(GameTestHelper h) {
        try (var f = new Fixture(h, 0)) {
            f.buildTurbine();
            f.makeCondensate();
            h.assertTrue(fluids(h, f.pos, Direction.NORTH).fill(GTFluids.stack("Steam", 2000), IFluidHandler.FluidAction.EXECUTE) == 2000,
                    "second native input is present together with real condensate");
            f.turbine.machineControl(null).setEnabled(false);
            var before = save(h, f.turbine);
            f.click();
            int steam = f.turbine.getFluidInTank(0).getAmount();
            int water = f.turbine.getFluidInTank(1).getAmount();
            int wear = f.tool.getDamageValue();
            h.assertTrue(steam == 0 && water == 100 && wear == 20,
                    "source-priority click must leave steam=0, water=100, wear=20; actual steam=" + steam + ", water=" + water + ", wear=" + wear);
            sameProcessing(h, before, save(h, f.turbine));
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void nativePlungerPurgesRealCondensateAtOriginalWear(GameTestHelper h) {
        try (var f = new Fixture(h, 1)) {
            f.buildTurbine();
            f.makeCondensate();
            f.turbine.machineControl(null).setEnabled(false);
            var before = save(h, f.turbine);
            f.click();
            h.assertTrue(f.turbine.getFluidInTank(0).isEmpty() && f.turbine.getFluidInTank(1).isEmpty()
                            && f.tool.getDamageValue() == 1,
                    "empty-input fallback purges real 100 mB condensate once for original 1 wear; actual wear=" + f.tool.getDamageValue());
            sameProcessing(h, before, save(h, f.turbine));
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void emptySteamTurbinePlungerDoesNotSpendWear(GameTestHelper h) {
        try (var f = new Fixture(h, 2)) {
            f.buildTurbine();
            f.turbine.machineControl(null).setEnabled(false);
            var before = save(h, f.turbine);
            f.click();
            h.assertTrue(f.turbine.getFluidInTank(0).isEmpty() && f.turbine.getFluidInTank(1).isEmpty()
                            && f.tool.getDamageValue() == 0, "empty turbine purge does not fabricate fluid or charge wear");
            sameProcessing(h, before, save(h, f.turbine));
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void nonTurbineTankKeepsGenericThousandMbPlunger(GameTestHelper h) {
        try (var f = new Fixture(h, 3)) {
            f.clear();
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:drum_stainless_steel"));
            f.set(f.pos, block.defaultBlockState());
            var tank = (TankBlockEntity) h.getLevel().getBlockEntity(f.pos);
            h.assertTrue(fluids(h, f.pos, Direction.NORTH).fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 2000),
                            IFluidHandler.FluidAction.EXECUTE) == 2000, "ordinary registered non-turbine drum holds water");
            f.click();
            h.assertTrue(tank.getFluidInTank(0).getAmount() == 1000 && f.tool.getDamageValue() == 100,
                    "non-dedicated tank still uses generic 1000 mB, 100 wear fallback");
            f.click();
            h.assertTrue(tank.getFluidInTank(0).isEmpty() && f.tool.getDamageValue() == 200, "generic second purge commits once");
            f.click();
            h.assertTrue(tank.getFluidInTank(0).isEmpty() && f.tool.getDamageValue() == 200, "empty generic tank does not charge wear");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void nativePlungerSneakingOffhandPreservesSteamPriority(GameTestHelper h) {
        try (var f = new Fixture(h, 4)) {
            f.buildTurbine();
            f.makeCondensate();
            h.assertTrue(fluids(h, f.pos, Direction.NORTH).fill(GTFluids.stack("Steam", 2000), IFluidHandler.FluidAction.EXECUTE) == 2000,
                    "offhand fixture holds real condensate and native steam input");
            f.turbine.machineControl(null).setEnabled(false);
            var control = new ItemStack(net.minecraft.world.item.Items.STICK);
            f.player.setItemInHand(InteractionHand.MAIN_HAND, control);
            f.player.setItemInHand(InteractionHand.OFF_HAND, f.tool);
            f.player.setShiftKeyDown(true);
            var hit = new BlockHitResult(new Vec3(f.pos.getX() + .5, f.pos.getY() + .5, f.pos.getZ()),
                    Direction.NORTH, f.pos, false);
            var before = save(h, f.turbine);
            // A harmless main-hand stick passes normally, allowing an ordinary offhand attempt.
            var first = f.player.gameMode.useItemOn(f.player, h.getLevel(), control, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(!first.consumesAction() && f.tool.getDamageValue() == 0
                            && f.turbine.getFluidInTank(0).getAmount() == 2000
                            && f.turbine.getFluidInTank(1).getAmount() == 100,
                    "main-hand stick does not consume the offhand interaction or mutate turbine stock");
            sameProcessing(h, before, save(h, f.turbine));
            var result = f.player.gameMode.useItemOn(f.player, h.getLevel(), f.player.getOffhandItem(), InteractionHand.OFF_HAND, hit);
            h.assertTrue(result.consumesAction() && f.turbine.getFluidInTank(0).isEmpty()
                            && f.turbine.getFluidInTank(1).getAmount() == 100 && f.tool.getDamageValue() == 20,
                    "sneaking offhand item-first purge keeps steam priority, 100 mB water and exact 20 tool wear");
            var main = f.player.getMainHandItem();
            h.assertTrue(main.is(net.minecraft.world.item.Items.STICK) && main.getCount() == 1 && main.getDamageValue() == 0
                            && f.player.getOffhandItem().getDamageValue() == 20,
                    "only the offhand plunger wears; harmless main-hand control stays unchanged");
            sameProcessing(h, before, save(h, f.turbine));
        }
        h.succeed();
    }
}
