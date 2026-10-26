package gregtech.compat.jade;

import gregtech.content.blockentity.energy.LaserConverterBlockEntity;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.machine.ItemPipeBlockEntity;
import gregtech.content.blockentity.sensor.SensorBlockEntity;
import gregtech.content.cover.*;
import gregtech.content.registry.GTItemPipes;
import gregtech.content.registry.GTSensors;
import gregtech.engine.sensor.SensorControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import snownee.jade.api.BlockAccessor;

/** Native registered stacks/entities: query real providers, retaining the installed optional Jade API. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class JadeCoverRegressionTests {
    private JadeCoverRegressionTests() {}

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void configuredCoversRemainIdentifiableAfterRemoval(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, GTItemPipes.all().get(0).get().defaultBlockState());
        var pipe = (ItemPipeBlockEntity) helper.getLevel().getBlockEntity(pos);
        var filter = item("item_filter");
        CoverUtilityBehaviors.setItemFilter(filter, new ItemStack(Items.DIAMOND));
        helper.assertTrue(pipe.attachCover(Direction.NORTH, filter), "Filter must attach to pipe");
        var before = pipe.getCover(Direction.NORTH).copy();
        var data = inspect(pipe, true);
        helper.assertTrue(has(data, "gt_omni", "jade.gt6ce.omni.cover_face"), "Pipe must show its cover");
        helper.assertTrue(has(data, "gt_omni", "jade.gt6ce.omni.pipe_capacity"), "Pipe capacity must be visible");
        helper.assertTrue(has(data, "gt_omni", "tooltip.gt6ce.cover.filter_target"), "Cover target must be visible");
        helper.assertTrue(ItemStack.matches(before, pipe.getCover(Direction.NORTH)), "Inspection must not mutate filters");
        var removed = pipe.removeCover(Direction.NORTH);
        helper.assertTrue(CoverUtilityBehaviors.itemFilter(removed).is(Items.DIAMOND), "Removal must retain filter target");
        helper.assertTrue(!ItemStack.isSameItemSameTags(removed, item("item_filter")), "Configured and empty filters differ");
        assertTarget(helper, removed, Items.DIAMOND);
        var fluid = item("fluid_filter");
        CoverUtilityBehaviors.setFluidFilter(fluid, new FluidStack(Fluids.WATER, 1000));
        var fluidTarget = CoverDisplayInfo.describe(fluid, helper.getLevel()).stream()
                .filter(line -> line.key().endsWith("filter_target")).findFirst().orElseThrow().arguments()[0];
        helper.assertTrue(fluidTarget instanceof FluidStack f && f.getFluid() == Fluids.WATER, "Fluid name must use saved fluid");
        var plain = item("item_filter");
        CoverStackData.putBoolean(plain, "gt.cover.inverted", true);
        helper.assertTrue(ItemStack.isSameItemSameTags(CoverStackData.detached(plain), item("item_filter")),
                "Host-only mode tags must not prevent empty covers stacking");
        helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void componentRatesUseActualTierRules(GameTestHelper helper) {
        // Independent GT6 ULV..XV values from MultiItemTechnological's cover registration.
        int[] litresPerSecond = {250, 1000, 4000, 16000, 64000, 256000, 1024000, 4096000, 16384000, 65536000, 262144000};
        int[] ticksPerStack = {512, 256, 128, 64, 32, 16, 8, 4, 2, 1, 1};
        int tested = 0;
        for (var registered : BuiltInRegistries.ITEM) {
            var stack = new ItemStack(registered);
            var kind = ComponentCoverRuntime.kind(stack);
            if (kind == null) continue;
            var rate = CoverDisplayInfo.describe(stack, helper.getLevel()).stream()
                    .filter(line -> line.key().endsWith("_rate")).findFirst().orElseThrow();
            int tier = CoverItems.tierIndex(stack);
            int expected = kind == ComponentCoverRules.Kind.PUMP ? litresPerSecond[tier] : ticksPerStack[tier];
            helper.assertTrue(rate.arguments()[0].equals(expected), "Tooltip must agree with transfer executor");
            tested++;
        }
        helper.assertTrue(tested >= 24, "All component families and tiers must be exercised");
        helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void machinesHaveIndependentRecipeDetailsAndConvertersReportTransfers(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(4, 2, 4));
        for (String id : new String[]{"crusher_steel", "replicator_osmiridium_t4"}) {
            helper.getLevel().setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(id(id)).defaultBlockState());
            var machine = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(pos);
            machine.inventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 7));
            var data = inspect(machine, false);
            helper.assertTrue(has(data, "gt_bm_details", "jade.gt6ce.omni.state"), id + " requires state without Omni");
            helper.assertTrue(has(data, "gt_bm_details", "jade.gt6ce.omni.item"), id + " requires grouped inventory without Omni");
            helper.assertTrue(data.getBoolean("gt_bm_details_inventory"), "Machine must replace Jade's generic inventory");
            var extra = inspect(machine, true);
            helper.assertTrue(!has(extra, "gt_omni", "jade.gt6ce.omni.state"), "State must not be duplicated");
        }
        helper.getLevel().setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(id("quantum_energizer_zpm")).defaultBlockState());
        var converter = (LaserConverterBlockEntity) helper.getLevel().getBlockEntity(pos);
        var spec = converter.spec();
        helper.assertTrue(converter.doInject(spec.inputType(), converter.front().getOpposite(), spec.input(), 1, true) == 1,
                "Converter must accept its rated packet");
        converter.tick();
        var data = inspect(converter, true);
        helper.assertTrue(has(data, "gt_omni", "jade.gt6ce.omni.rated_output"), "Quantum converter requires ratings");
        helper.assertTrue(has(data, "gt_omni", "jade.gt6ce.omni.converter_input"), "Quantum converter requires actual input");
        helper.assertTrue(converter.lastInput() == spec.input() && converter.lastOutput() == 0,
                "Reading must distinguish consumed input from delivery to a disconnected output");
        helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void everySensorReportsItsModeAndLastMeasurement(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(6, 2, 6));
        for (var registered : GTSensors.all()) {
            helper.getLevel().setBlockAndUpdate(pos, registered.get().defaultBlockState());
            var sensor = (SensorBlockEntity) helper.getLevel().getBlockEntity(pos);
            for (var mode : SensorControl.Mode.values()) {
                var saved = sensor.saveWithoutMetadata();
                saved.putInt("gt.control_mode", mode.ordinal());
                saved.putInt("gt.threshold", 25);
                saved.putLong("gt.measured_value", 42);
                saved.putLong("gt.measured_max", 100);
                saved.putIntArray("gt.average", new int[]{42, 42, 42, 42});
                sensor.load(saved);
                var before = sensor.saveWithoutMetadata();
                var data = inspect(sensor, true);
                helper.assertTrue(has(data, "gt_omni", "jade.gt6ce.omni.sensor_reading"), "Sensor must show measured value");
                helper.assertTrue(has(data, "gt_omni", "jade.gt6ce.omni.sensor_average"), "Sensor must show averaging");
                helper.assertTrue(has(data, "gt_omni", "jade.gt6ce.omni.sensor_threshold") == SensorControl.hasThreshold(mode),
                        "Threshold only belongs to comparison and scaling modes");
                helper.assertTrue(sensor.inspection().mode() == mode && sensor.inspection().value() == 42,
                        "Mode and measured value must come from the server sample");
                helper.assertTrue(sensor.saveWithoutMetadata().equals(before), "Jade must not measure again or change averaging");
            }
        }
        helper.succeed();
    }

    private static void assertTarget(GameTestHelper helper, ItemStack cover, net.minecraft.world.item.Item target) {
        Object value = CoverDisplayInfo.describe(cover, helper.getLevel()).stream()
                .filter(line -> line.key().endsWith("filter_target")).findFirst().orElseThrow().arguments()[0];
        helper.assertTrue(value instanceof ItemStack stack && stack.is(target), "Tooltip must identify configured item");
    }

    private static CompoundTag inspect(BlockEntity entity, boolean omni) {
        var data = new CompoundTag();
        // Only the read-only accessor fields these providers consume; no simulated game logic.
        var accessor = (BlockAccessor) java.lang.reflect.Proxy.newProxyInstance(BlockAccessor.class.getClassLoader(),
                new Class<?>[]{BlockAccessor.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getBlockEntity" -> entity;
                    case "getBlock" -> entity.getBlockState().getBlock();
                    case "getBlockState" -> entity.getBlockState();
                    case "getLevel" -> entity.getLevel();
                    case "getPosition" -> entity.getBlockPos();
                    case "getServerData" -> data;
                    case "getHitResult" -> new BlockHitResult(Vec3.atCenterOf(entity.getBlockPos()), Direction.NORTH, entity.getBlockPos(), false);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        if (omni) OmniJadeProvider.INSTANCE.appendServerData(data, accessor);
        else BasicMachineJadeProvider.INSTANCE.appendServerData(data, accessor);
        return data;
    }

    private static boolean has(CompoundTag data, String section, String key) {
        return data.getList(section, 10).stream().map(tag -> (CompoundTag) tag).anyMatch(row -> row.getString("key").equals(key));
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("gt6ce", path); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }
}
