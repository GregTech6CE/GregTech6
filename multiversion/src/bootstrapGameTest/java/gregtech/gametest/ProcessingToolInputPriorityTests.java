package gregtech.gametest;

import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.tool.GTToolType;
import gregtech.content.blockentity.tool.ProcessingToolBlockEntity;
import gregtech.content.cover.ComponentCoverFallback;
import gregtech.content.cover.ComponentCoverEvents;
import gregtech.content.cover.CoverItems;
import gregtech.content.cover.PanelCoverHost;
import gregtech.content.material.Materials;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.GTToolItem;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.List;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Issue #107: full native right-click dispatch, real bath recipes, and legacy cover recovery. */
@GameTestHolder("gt6ce_utility_fixes")
@PrefixGameTestTemplate(false)
public final class ProcessingToolInputPriorityTests {
    private static final List<String> BATHS = List.of("bathing_pot", "bathing_pot_table",
            "bathing_pot_wood", "bathing_pot_table_wood");
    private static final List<String> TOOLS = List.of("bathing_pot", "bathing_pot_table",
            "bathing_pot_wood", "bathing_pot_table_wood", "mixing_bowl", "mixing_bowl_table", "juicer");

    private static ItemStack woodPlate(int count) {
        return GTItems.getStack(MaterialPrefix.plate, GTMaterialRegistry.get("Wood"), count);
    }

    private static CompoundTag save(GameTestHelper h, BlockEntity entity) {
//? if >=1.21.1 {
/*        return entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return entity.saveWithoutMetadata();
//?}
    }

    private static void readCovers(GameTestHelper h, BlockEntity entity, CompoundTag data) {
//? if >=1.21.1 {
/*        ComponentCoverFallback.read(entity, data, h.getLevel().registryAccess());
*///?} else {
        ComponentCoverFallback.read(entity, data);
//?}
    }

    private static final class Fixture implements AutoCloseable {
        private record Saved(BlockState state, CompoundTag data) {}
        private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        final GameTestHelper h;
        final BlockPos pos;
        final ServerPlayer player;
        final LinkedHashMap<BlockPos, Saved> saved = new LinkedHashMap<>();

        Fixture(GameTestHelper h, String path) {
            this.h = h;
            pos = h.absolutePos(new BlockPos(2, 2, 2));
//? if >=1.21.1 {
/*            player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
            player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[gt-bath-input]"));
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.setShiftKeyDown(false);
            for (var at : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                var immutable = at.immutable();
                var entity = h.getLevel().getBlockEntity(immutable);
                saved.put(immutable, new Saved(h.getLevel().getBlockState(immutable),
                        entity == null ? null : save(h, entity)));
            }
            for (var at : saved.keySet()) set(at, Blocks.AIR.defaultBlockState());
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", path));
            h.assertTrue(block != Blocks.AIR, "actual registered host " + path);
            set(pos, block.defaultBlockState());
            host().panels(); // Initialize lazy cover storage before snapshots and native capability queries.
        }

        private void set(BlockPos at, BlockState state) {
            h.getLevel().removeBlockEntity(at);
            h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(at, state, FLAGS);
        }

        ProcessingToolBlockEntity tool() {
            return (ProcessingToolBlockEntity) h.getLevel().getBlockEntity(pos);
        }

        PanelCoverHost host() { return (PanelCoverHost) h.getLevel().getBlockEntity(pos); }

        InteractionResult click(ItemStack held, boolean sneaking) {
            player.setShiftKeyDown(sneaking);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            player.setPos(pos.getX() + .5, pos.getY() + 2.5, pos.getZ() + .5);
            return player.gameMode.useItemOn(player, h.getLevel(), held, InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5), Direction.UP, pos, false));
        }

        IFluidHandler fluids() {
//? if >=1.21.1 {
/*            var handler = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    pos, Direction.UP);
            if (handler == null) throw new AssertionError("actual open-vessel fluid capability");
            return handler;
*///?} else {
            return tool().getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                    .orElseThrow(() -> new AssertionError("actual open-vessel fluid capability"));
//?}
        }

        int inputCount(ItemStack sample) {
            int count = 0;
            for (int slot = 0; slot < tool().recipes().mInputItemsCount; slot++) {
                var stack = tool().inventory().getStackInSlot(slot);
                if (stack.is(sample.getItem())) count += stack.getCount();
            }
            return count;
        }

        int outputCount(ItemStack sample) {
            int count = 0;
            for (int slot = tool().recipes().mInputItemsCount; slot < tool().inventory().getSlots(); slot++) {
                var stack = tool().inventory().getStackInSlot(slot);
                if (stack.is(sample.getItem())) count += stack.getCount();
            }
            return count;
        }

        int fluidAmount() {
            int amount = 0;
            var handler = fluids();
            for (int tank = 0; tank < handler.getTanks(); tank++) amount += handler.getFluidInTank(tank).getAmount();
            return amount;
        }

        void assertNoCovers(String message) {
            for (var side : Direction.values()) h.assertTrue(host().getCover(side).isEmpty(), message + ": " + side);
        }

        @Override public void close() {
            player.closeContainer();
            player.getInventory().clearContent();
            player.discard();
            for (var at : saved.keySet()) set(at, Blocks.AIR.defaultBlockState());
            for (var entry : saved.entrySet()) {
                set(entry.getKey(), entry.getValue().state());
                var entity = h.getLevel().getBlockEntity(entry.getKey());
                if (entity != null && entry.getValue().data() != null) {
//? if >=1.21.1 {
/*                    entity.loadWithComponents(entry.getValue().data(), h.getLevel().registryAccess());
*///?} else {
                    entity.load(entry.getValue().data());
//?}
                    readCovers(h, entity, entry.getValue().data());
                }
            }
        }
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void allFourBathsReceiveMaterialAndOrdinaryRecipeInputsThroughNativeClicks(GameTestHelper h) {
        for (String path : BATHS) for (boolean sneaking : new boolean[]{false, true}) {
            try (var f = new Fixture(h, path)) {
                var plates = woodPlate(4);
                h.assertTrue(!plates.isEmpty() && CoverItems.isCover(plates)
                                && f.tool().recipes() == MachineRecipeMaps.Bath && f.tool().recipes().containsInput(plates),
                        "real wood plate is both a cover item and a registered Bath ingredient");
                h.assertTrue(f.click(plates, sneaking).consumesAction(), "native bath input interaction is consumed");
                h.assertTrue(f.inputCount(woodPlate(1)) == 4 && f.player.getMainHandItem().isEmpty(),
                        "all four plates move into inputs exactly once for " + path + ", sneaking=" + sneaking);
                f.assertNoCovers("recipe plates must never become covers");

                var sticks = new ItemStack(Items.STICK, 3);
                h.assertTrue(!CoverItems.isCover(sticks) && f.tool().recipes().containsInput(sticks),
                        "ordinary vanilla stick is a genuine non-cover Bath ingredient");
                f.click(sticks, false);
                h.assertTrue(f.inputCount(sticks) == 3 && f.inputCount(woodPlate(1)) == 4
                                && f.player.getMainHandItem().isEmpty(),
                        "ordinary ingredient path still conserves all seven inputs");
                f.assertNoCovers("ordinary input does not install an attachment");
            }
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void nativeBathClicksTreatWoodWithExactOilAndOutputAccounting(GameTestHelper h) {
        var recipe = MachineRecipeMaps.Bath.findRecipe(List.of(woodPlate(1)),
                List.of(GTFluids.stack("Oil_Creosote", 100)), false, 1, 1);
        h.assertTrue(recipe != null && recipe.mEUt == 0 && recipe.mInputs.length == 1
                        && recipe.mInputs[0].getCount() == 1 && recipe.mFluidInputs.length == 1
                        && recipe.mFluidInputs[0].getAmount() == 100 && recipe.mOutputs.length == 1,
                "actual source wood treatment is one plate plus 100mB creosote at zero manual power");
        var expected = recipe.mOutputs[0];
        for (String path : BATHS) try (var f = new Fixture(h, path)) {
            f.click(woodPlate(2), false);
            h.assertTrue(f.inputCount(woodPlate(1)) == 2 && f.player.getMainHandItem().isEmpty(), "two raw plates entered the native bath");
            h.assertTrue(f.fluids().fill(GTFluids.stack("Oil_Creosote", 200), IFluidHandler.FluidAction.EXECUTE) == 200,
                    "real vessel accepts exactly two original creosote charges");
            f.click(ItemStack.EMPTY, false);
            h.assertTrue(f.inputCount(woodPlate(1)) == 1 && f.fluidAmount() == 100
                            && f.outputCount(expected) == expected.getCount(),
                    "first native processing click consumes one plate and one fluid charge only");
            f.click(ItemStack.EMPTY, false);
            h.assertTrue(f.inputCount(woodPlate(1)) == 0 && f.fluidAmount() == 0
                            && f.outputCount(expected) == expected.getCount() * 2,
                    "second native processing click retains the exact two original outputs");
            f.click(ItemStack.EMPTY, false);
            h.assertTrue(f.outputCount(expected) == 0
                            && f.player.getInventory().countItem(expected.getItem()) == expected.getCount() * 2,
                    "native empty-hand collection returns both products without duplication");
            f.assertNoCovers("wood processing never turns its ingredient into a plate cover");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void manualVesselsRejectNewCoversAndCrowbarsRecoverLegacyCoverSnapshots(GameTestHelper h) {
        var blank = GTTechnological.get("blank_cover");
        h.assertTrue(blank != null && blank != Items.AIR, "actual registered blank cover exists");
        for (String path : TOOLS) try (var f = new Fixture(h, path)) {
            h.assertTrue(!f.tool().recipes().containsInput(new ItemStack(blank)), "blank cover is not a fabricated vessel ingredient");
            f.click(new ItemStack(blank, 2), false);
            if (path.equals("juicer")) {
                h.assertTrue(f.player.getMainHandItem().getCount() == 2, "juicer has no manual item storage");
            } else {
                h.assertTrue(f.player.getMainHandItem().isEmpty() && f.inputCount(new ItemStack(blank)) == 2,
                        "a cover item may be stored as an ordinary manual input without a recipe: " + path);
            }
            f.assertNoCovers("original hand-processing host forbids new attachments");

            var legacy = save(h, f.tool());
            legacy.putBoolean("gt.component_covers", true);
//? if >=1.21.1 {
/*            legacy.put("gt_cover_" + Direction.UP.ordinal(), new ItemStack(blank).save(h.getLevel().registryAccess()));
*///?} else {
            legacy.put("gt_cover_" + Direction.UP.ordinal(), new ItemStack(blank).save(new CompoundTag()));
//?}
            readCovers(h, f.tool(), legacy);
            h.assertTrue(f.host().getCover(Direction.UP).is(blank), "existing saved erroneous cover is retained for recovery");
            var mirror = new ProcessingToolBlockEntity(f.pos, f.tool().getBlockState());
            try {
                readCovers(h, mirror, legacy);
                var mirrorHost = (PanelCoverHost) (Object) mirror;
                h.assertTrue(mirrorHost.getCover(Direction.UP).is(blank), "update-receiver mirror begins with the old cover");
                f.player.getInventory().clearContent();
                f.click(GTToolItem.create(GTToolType.CROWBAR, Materials.Steel, GTMaterialRegistry.get("Wood")), false);
                h.assertTrue(f.host().getCover(Direction.UP).isEmpty() && f.player.getInventory().countItem(blank) == 1,
                        "actual crowbar click recovers exactly one legacy cover from " + path);
                var cleared = save(h, f.tool());
                h.assertTrue(cleared.getBoolean("gt.component_covers"), "last-cover removal emits an authoritative empty snapshot");
                readCovers(h, mirror, cleared);
                h.assertTrue(mirrorHost.getCover(Direction.UP).isEmpty(), "empty authoritative snapshot clears the previous receiver cover");
                f.click(f.player.getMainHandItem(), false);
                h.assertTrue(f.player.getInventory().countItem(blank) == 1, "repeated crowbar click cannot duplicate recovered cover");
            } finally {
                mirror.setRemoved();
            }
        }
        try (var f = new Fixture(h, "steam_boiler_tungsten")) {
            f.click(new ItemStack(blank, 2), false);
            h.assertTrue(f.host().getCover(Direction.UP).is(blank) && f.player.getMainHandItem().getCount() == 1,
                    "ordinary registered boiler still installs covers and consumes exactly one");
        }
        h.succeed();
    }

    public static final class ProtectInput {
        final ServerPlayer player;
        int calls;
        ProtectInput(ServerPlayer player) { this.player = player; }
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void click(PlayerInteractEvent.RightClickBlock event) {
            if (event.getEntity() != player) return;
            calls++;
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void protectionEventsStillDenyRecipeInputsAfterNativePriority(GameTestHelper h) {
        for (String path : BATHS) for (boolean sneaking : new boolean[]{false, true}) {
            try (var f = new Fixture(h, path)) {
                f.player.setShiftKeyDown(sneaking);
                f.player.setItemInHand(InteractionHand.MAIN_HAND, woodPlate(4));
                var denied = new PlayerInteractEvent.RightClickBlock(f.player, InteractionHand.MAIN_HAND, f.pos,
                        new BlockHitResult(Vec3.atCenterOf(f.pos).add(0, .5, 0), Direction.UP, f.pos, false));
//? if >=1.21.1 {
/*                denied.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
*///?} else {
                denied.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
//?}
                ComponentCoverEvents.use(denied);
//? if >=1.21.1 {
/*                h.assertTrue(denied.getUseBlock() == net.neoforged.neoforge.common.util.TriState.FALSE,
*///?} else {
                h.assertTrue(denied.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.DENY,
//?}
                        "native input priority preserves an earlier explicit block-use denial");
                h.assertTrue(f.inputCount(woodPlate(1)) == 0 && f.player.getMainHandItem().getCount() == 4,
                        "pre-denied event never transfers ingredients or consumes held plates");
                var protector = new ProtectInput(f.player);
//? if >=1.21.1 {
/*                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(protector);
*///?} else {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(protector);
//?}
                try {
                    f.click(woodPlate(4), sneaking);
                    h.assertTrue(protector.calls == 1 && f.inputCount(woodPlate(1)) == 0
                                    && f.player.getMainHandItem().getCount() == 4,
                            "later protection event still cancels native ingredient transfer: " + path);
                    f.assertNoCovers("protected click must not install a cover instead");
                } finally {
//? if >=1.21.1 {
/*                    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(protector);
*///?} else {
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(protector);
//?}
                }
                f.click(f.player.getMainHandItem(), sneaking);
                h.assertTrue(f.inputCount(woodPlate(1)) == 4 && f.player.getMainHandItem().isEmpty(),
                        "same real ingredient is accepted once protection is removed");
            }
        }
        h.succeed();
    }
}
