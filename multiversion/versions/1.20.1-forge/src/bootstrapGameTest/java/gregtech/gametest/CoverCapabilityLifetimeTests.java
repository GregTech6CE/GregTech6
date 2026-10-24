package gregtech.gametest;

import gregtech.block.inventory.DrawerQuadBlock;
import gregtech.blockentity.inventory.DrawerQuadBlockEntity;
import gregtech.content.cover.ForgeCoverCapabilityCache;
import gregtech.content.cover.PanelCoverHost;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class CoverCapabilityLifetimeTests {
    private static DrawerQuadBlockEntity drawer(GameTestHelper h) {
        var block = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof DrawerQuadBlock).findFirst().orElseThrow();
        h.setBlock(new BlockPos(1, 1, 1), block.defaultBlockState().setValue(DrawerQuadBlock.FACING, Direction.NORTH));
        return (DrawerQuadBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1, 1, 1)));
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void fallbackDrawerCapabilitiesInvalidateWithSideMapping(GameTestHelper h) {
        var drawer = drawer(h);
        drawer.items().setStackInSlot(0, new ItemStack(Items.DIRT, 3));
        drawer.items().setStackInSlot(72, new ItemStack(Items.STONE, 5));
        var old = drawer.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN);
        var handler = old.orElseThrow(IllegalStateException::new);
        var invalidations = new AtomicInteger();
        old.addListener(ignored -> invalidations.incrementAndGet());
        h.assertTrue(handler.getSlots() == 144 && handler.extractItem(0, 1, true).is(Items.DIRT), "initial unrestricted real drawer view");
        for (int i = 0; i < 100; i++)
            h.assertTrue(drawer.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN) == old, "repeated lookup reuses derived lifetime");
        var host = (PanelCoverHost) drawer;
        var filter = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "item_filter")));
        h.assertTrue(host.attachCover(Direction.DOWN, filter), "real fallback filter attaches");
        h.assertTrue(handler.extractItem(0, 1, true).isEmpty(), "cached wrapper still checks newly attached filter");
        host.removeCover(Direction.DOWN);
        h.assertTrue(handler.extractItem(0, 1, true).is(Items.DIRT), "cached wrapper sees filter removal");
        drawer.toggleSidedAccess();
        h.assertTrue(invalidations.get() == 1 && !old.isPresent() && old.resolve().isEmpty(), "mode change invalidates public optional exactly once");
        var current = drawer.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(IllegalStateException::new);
        h.assertTrue(current.getSlots() == 72 && current.getStackInSlot(0).is(Items.STONE), "fresh down mapping starts at actual slot72");
        h.assertTrue(current.extractItem(0, 1, true).is(Items.STONE) && drawer.items().getStackInSlot(72).getCount() == 5
                && drawer.items().getStackInSlot(0).getCount() == 3, "simulation preserves both allowed and excluded compartments");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void fallbackUnresolvedCapabilitiesInvalidateOnRotationAndRemoval(GameTestHelper h) {
        var drawer = drawer(h);
        var lazy = drawer.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST);
        var invalidations = new AtomicInteger();
        lazy.addListener(ignored -> invalidations.incrementAndGet());
        h.getLevel().setBlock(drawer.getBlockPos(), drawer.getBlockState().setValue(DrawerQuadBlock.FACING, Direction.EAST), 3);
        h.assertTrue(invalidations.get() == 1 && !lazy.isPresent() && lazy.resolve().isEmpty(), "unresolved rotated view becomes empty without supplier failure");
        var afterRotation = drawer.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST);
        h.assertTrue(afterRotation.isPresent() && afterRotation != lazy, "rotation supplies fresh lifetime");
        afterRotation.addListener(ignored -> invalidations.incrementAndGet());
        h.setBlock(new BlockPos(1, 1, 1), Blocks.AIR);
        h.assertTrue(invalidations.get() == 2 && !afterRotation.isPresent() && afterRotation.resolve().isEmpty(), "removal invalidates current unresolved view");
        h.assertTrue(!drawer.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST).isPresent(), "removed provider stays empty");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void fallbackCapabilityCacheReplacesItemAndFluidSources(GameTestHelper h) {
        var cache = new ForgeCoverCapabilityCache();
        LazyOptional<IItemHandler> firstSource = LazyOptional.of(() -> new ItemStackHandler(1));
        LazyOptional<IItemHandler> secondSource = LazyOptional.of(() -> new ItemStackHandler(2));
        LazyOptional<IFluidHandler> fluidSource = LazyOptional.of(() -> new FluidTank(1000));
        var first = cache.wrap(ForgeCapabilities.ITEM_HANDLER, Direction.UP, firstSource, raw -> raw);
        var fluid = cache.wrap(ForgeCapabilities.FLUID_HANDLER, Direction.UP, fluidSource, raw -> raw);
        var itemInvalidations = new AtomicInteger();
        var fluidInvalidations = new AtomicInteger();
        var reentrant = new AtomicReference<LazyOptional<IItemHandler>>();
        first.addListener(ignored -> {
            itemInvalidations.incrementAndGet();
            reentrant.set(cache.wrap(ForgeCapabilities.ITEM_HANDLER, Direction.UP, secondSource, raw -> raw));
        });
        fluid.addListener(ignored -> fluidInvalidations.incrementAndGet());
        var second = cache.wrap(ForgeCapabilities.ITEM_HANDLER, Direction.UP, secondSource, raw -> raw);
        h.assertTrue(second == reentrant.get() && itemInvalidations.get() == 1 && !first.isPresent(), "source replacement reuses a reentrant listener's new entry");
        firstSource.invalidate();
        h.assertTrue(second.isPresent() && fluid.isPresent(), "detached old source cannot evict current item or fluid entries");
        for (int i = 0; i < 100; i++)
            h.assertTrue(cache.wrap(ForgeCapabilities.FLUID_HANDLER, Direction.UP, fluidSource, raw -> raw) == fluid, "one fluid wrapper per source and face");
        fluidSource.invalidate();
        h.assertTrue(fluidInvalidations.get() == 1 && !fluid.isPresent() && fluid.resolve().isEmpty() && second.isPresent(), "fluid invalidation forwards without touching items");
        var empty = cache.wrap(ForgeCapabilities.ITEM_HANDLER, Direction.UP, LazyOptional.<IItemHandler>empty(), raw -> raw);
        h.assertTrue(!empty.isPresent() && !second.isPresent(), "empty replacement invalidates prior item lifetime without caching a live wrapper");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void fallbackSharedSourceInvalidationSupportsCrossFaceRequery(GameTestHelper h) {
        var cache = new ForgeCoverCapabilityCache();
        LazyOptional<IItemHandler> source = LazyOptional.of(() -> new ItemStackHandler(1));
        LazyOptional<IItemHandler> replacement = LazyOptional.of(() -> new ItemStackHandler(2));
        var north = cache.wrap(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH, source, raw -> raw);
        var south = cache.wrap(ForgeCapabilities.ITEM_HANDLER, Direction.SOUTH, source, raw -> raw);
        var invalidations = new AtomicInteger();
        var replacements = new java.util.EnumMap<Direction, LazyOptional<IItemHandler>>(Direction.class);
        // Keep both mapped suppliers unresolved. Either HashSet notification order
        // must safely handle reentrant queries of the other, not-yet-notified face.
        for (var side : new Direction[] {Direction.NORTH, Direction.SOUTH}) {
            var mapped = side == Direction.NORTH ? north : south;
            mapped.addListener(ignored -> {
                invalidations.incrementAndGet();
                var other = side.getOpposite();
                var expired = cache.wrap(ForgeCapabilities.ITEM_HANDLER, other, source, raw -> raw);
                h.assertTrue(!expired.isPresent() && expired.resolve().isEmpty(), "invalidating shared source cannot return unresolved live wrapper");
                var fresh = cache.wrap(ForgeCapabilities.ITEM_HANDLER, other, replacement, raw -> raw);
                replacements.put(other, fresh);
                h.assertTrue(fresh.orElseThrow(IllegalStateException::new).getSlots() == 2, "cross-face replacement resolves during source notification");
            });
        }
        source.invalidate();
        h.assertTrue(invalidations.get() == 2 && !north.isPresent() && !south.isPresent(), "shared source invalidates both public lifetimes exactly once");
        for (var side : new Direction[] {Direction.NORTH, Direction.SOUTH}) {
            var fresh = cache.wrap(ForgeCapabilities.ITEM_HANDLER, side, replacement, raw -> raw);
            h.assertTrue(fresh == replacements.get(side) && fresh.orElseThrow(IllegalStateException::new).getSlots() == 2, "late old listener does not evict replacement face");
            h.assertTrue(cache.wrap(ForgeCapabilities.ITEM_HANDLER, side, replacement, raw -> raw) == fresh, "replacement face retains stable cached identity");
        }
        replacement.invalidate();
        h.succeed();
    }


    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void fallbackReentrantNewerSourceSurvivesStaleOuterLookup(GameTestHelper h) {
        // Empty, invalidated and different live provider results are
        // snapshots that must yield to a newer same-key query from a callback.
        for (int requestKind = 0; requestKind < 3; requestKind++) {
            var cache = new ForgeCoverCapabilityCache();
            LazyOptional<IItemHandler> original = LazyOptional.of(() -> new ItemStackHandler(1));
            LazyOptional<IItemHandler> requested = requestKind == 0 ? LazyOptional.empty() : LazyOptional.of(() -> new ItemStackHandler(2));
            if (requestKind == 1) requested.invalidate();
            LazyOptional<IItemHandler> newer = LazyOptional.of(() -> new ItemStackHandler(3));
            var old = cache.wrap(ForgeCapabilities.ITEM_HANDLER, Direction.UP, original, raw -> raw);
            var replacement = new AtomicReference<LazyOptional<IItemHandler>>();
            var oldInvalidations = new AtomicInteger();
            var replacementInvalidations = new AtomicInteger();
            old.addListener(ignored -> {
                oldInvalidations.incrementAndGet();
                var fresh = cache.wrap(ForgeCapabilities.ITEM_HANDLER, Direction.UP, newer, raw -> raw);
                fresh.addListener(unused -> replacementInvalidations.incrementAndGet());
                replacement.set(fresh);
            });
            var staleResult = cache.wrap(ForgeCapabilities.ITEM_HANDLER, Direction.UP, requested, raw -> raw);
            h.assertTrue(!staleResult.isPresent() && staleResult.resolve().isEmpty(), "superseded outer lookup returns empty rather than wrong source");
            h.assertTrue(oldInvalidations.get() == 1 && !old.isPresent(), "explicit replacement still invalidates original entry");
            var fresh = cache.wrap(ForgeCapabilities.ITEM_HANDLER, Direction.UP, newer, raw -> raw);
            h.assertTrue(fresh == replacement.get() && replacementInvalidations.get() == 0
                    && fresh.orElseThrow(IllegalStateException::new).getSlots() == 3, "newer same-key callback entry survives stale outer lookup");
            original.invalidate();
            h.assertTrue(fresh.isPresent(), "detached original cannot invalidate newer generation");
            newer.invalidate();
            h.assertTrue(replacementInvalidations.get() == 1 && !fresh.isPresent(), "newer source retains native invalidation listener");
        }
        h.succeed();
    }

}
