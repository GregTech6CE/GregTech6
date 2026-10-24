package gregtech.gametest;

import gregtech.api.addon.AddonMaterials;
import gregtech.api.machine.CrucibleSpec;
import gregtech.api.machine.HeatHandler;
import gregtech.api.machine.crucible.CrucibleInputRules;
import gregtech.api.machine.crucible.CrucibleMaterialStack;
import gregtech.api.machine.crucible.ThermalStep;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTMaterialRegistry;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.data.MaterialPrefix;
import gregtech.registry.GTItems;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Public addon removal, real generated item intake, and both native crucible phase adapters. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class SmeltingTargetRemovalTests {
    private record Vessel(BlockPos pos, HeatHandler heat, CrucibleSpec spec,
                          Supplier<List<CrucibleMaterialStack>> content, Supplier<ItemStack> cache, Runnable tick) {}

    /** Only readiness retries. Sealing, item admission and thermal operations run once. */
    private static void whenReady(GameTestHelper h, Runnable start) {
        var owned = new HashSet<BlockPos>();
        h.forEveryBlockInStructure(at -> owned.add(h.absolutePos(at)));
        h.startSequence().thenWaitUntil(() -> {
            for (var local : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(7, 7, 7))) {
                var at = h.absolutePos(local);
                h.assertTrue(owned.contains(at), "crucible fixture stays inside its dedicated nine-cube template");
                h.assertTrue(h.getLevel().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(at))
                                && h.getLevel().isPositionEntityTicking(at),
                        "waiting for loaded/ticking crucible fixture: " + at);
            }
        }).thenExecute(start);
    }

    private static void seal(GameTestHelper h) {
        var low = h.absolutePos(new BlockPos(1, 1, 1));
        var high = h.absolutePos(new BlockPos(7, 7, 7));
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (var at : BlockPos.betweenClosed(low, high)) {
            if (at.getX() == low.getX() || at.getX() == high.getX()
                    || at.getY() == low.getY() || at.getY() == high.getY()
                    || at.getZ() == low.getZ() || at.getZ() == high.getZ()) {
                h.getLevel().setBlock(at, Blocks.STONE.defaultBlockState(), quiet);
            }
        }
        for (var at : BlockPos.betweenClosed(low.offset(1, 1, 1), high.offset(-1, -1, -1))) {
            h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), quiet);
        }
        h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(low.getX() + 1, low.getY() + 1, low.getZ() + 1,
                        high.getX(), high.getY(), high.getZ()))
                .forEach(ItemEntity::discard);
    }

    private static Vessel vessel(GameTestHelper h, BlockPos local) {
        var pos = h.absolutePos(local);
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "smelting_crucible_steel"));
        h.assertTrue(block instanceof gregtech.block.machine.SmeltingCrucibleBlock,
                "actual registered steel crucible block exists");
        h.getLevel().setBlock(pos, block.defaultBlockState(), 3);
//? if >=1.21.1 {
        /*var entity = (gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity) h.getLevel().getBlockEntity(pos);
        Runnable tick = entity::serverTick;
*///?} else {
        var entity = (gregtech.blockentity.machine.SmeltingCrucibleBlockEntity) h.getLevel().getBlockEntity(pos);
        Runnable tick = () -> gregtech.blockentity.machine.SmeltingCrucibleBlockEntity.serverTick(
                h.getLevel(), pos, entity.getBlockState(), entity);
//?}
        h.assertTrue(entity != null && entity.getContentView().isEmpty() && entity.getCacheStack().isEmpty(),
                "new native crucible starts empty");
        return new Vessel(pos, entity, entity.spec(), entity::getContentView, entity::getCacheStack, tick);
    }

    private static void heatTo(GameTestHelper h, Vessel vessel, long kelvin) {
        long before = vessel.heat().temperatureK();
        h.assertTrue(before < kelvin, "heat control starts below target");
        long required = ThermalStep.requiredEnergy(vessel.spec().smeltingThermalMassKg()
                + CrucibleMaterialStack.weight(vessel.content().get()));
        long energy = Math.multiplyExact(required, kelvin - before);
        h.assertTrue(vessel.heat().insertHeat(energy, false) == energy, "native heat facade accepts the exact HU");
        vessel.tick().run();
        h.assertTrue(vessel.heat().temperatureK() == kelvin && vessel.heat().heatStored() == 0,
                "native thermal step reaches the requested temperature without a hidden buffer");
    }

    private static void consumeDust(GameTestHelper h, Vessel vessel, ItemStack dust) {
        var item = new ItemEntity(h.getLevel(), vessel.pos().getX() + .5, vessel.pos().getY() + .5,
                vessel.pos().getZ() + .5, dust.copy());
        item.setNoGravity(true);
        h.assertTrue(h.getLevel().addFreshEntity(item), "actual generated dust enters the loaded native world");
        vessel.tick().run();
        h.assertTrue(item.isRemoved() && vessel.cache().get().isEmpty(),
                "native suction and cache consume exactly one dust, rather than merely retaining the input item");
    }

    private static void retained(GameTestHelper h, Vessel vessel, GTMaterial source, String boundary) {
        var contents = vessel.content().get();
        h.assertTrue(contents.size() == 1 && contents.get(0).material == source && contents.get(0).amount == GTValues.U,
                "removed target retains the consumed native dust identity and exact U: " + boundary);
        h.assertTrue(h.getLevel().getBlockState(vessel.pos()).getBlock()
                        instanceof gregtech.block.machine.SmeltingCrucibleBlock,
                "safe retained matter leaves the crucible intact: " + boundary);
    }

    @GameTest(template = "crucible_removal_empty", timeoutTicks = 160)
    public static void removedSmeltingTargetRetainsConsumedDustAcrossHotPhase(GameTestHelper h) {
        whenReady(h, () -> {
            seal(h);
            var source = GTMaterialRegistry.get("SampleRetainedMatter");
            h.assertTrue(source.isValid() && "addon_sample".equals(AddonMaterials.owner(source))
                            && source.getTargetSmeltingMaterial() == source && source.getTargetSmeltingAmount() == 0
                            && !source.has(MaterialProperty.MELTING),
                    "real startup service removal overrides the isolated declared target");
            var dust = GTItems.getStack(MaterialPrefix.dust, source, 1);
            h.assertTrue(!dust.isEmpty() && dust.getCount() == 1
                            && "addon_sample".equals(BuiltInRegistries.ITEM.getKey(dust.getItem()).getNamespace()),
                    "input is the actual generated addon dust, not a directly installed material stack");
            h.assertTrue(CrucibleInputRules.smeltingPreview(source, MaterialPrefix.dust) == null,
                    "removed target stays excluded from smelting recipe/display generation");

            var crossing = vessel(h, new BlockPos(3, 4, 4));
            h.assertTrue(crossing.heat().temperatureK() < source.getMeltingPoint(), "cold intake starts below melting");
            consumeDust(h, crossing, dust);
            retained(h, crossing, source, "cold native intake");
            heatTo(h, crossing, 500);
            crossing.tick().run();
            retained(h, crossing, source, "crossing melting through native heat and phase ticks");

            var hot = vessel(h, new BlockPos(5, 4, 4));
            heatTo(h, hot, 500);
            hot.tick().run(); // Set previous temperature above melting before admitting new content.
            consumeDust(h, hot, dust);
            h.assertTrue(hot.heat().temperatureK() >= source.getMeltingPoint()
                            && hot.heat().temperatureK() < source.getBoilingPoint(),
                    "real cold-dust mixing still leaves the already hot crucible safely above melting");
            retained(h, hot, source, "new content admitted into an already hot crucible");
            for (int i = 0; i < 12; i++) { crossing.tick().run(); hot.tick().run(); }
            retained(h, crossing, source, "repeated native phase ticks after heating");
            retained(h, hot, source, "repeated native phase ticks after hot intake");
            h.succeed();
        });
    }
}
