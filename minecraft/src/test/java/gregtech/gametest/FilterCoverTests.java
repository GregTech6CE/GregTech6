package gregtech.gametest;

import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import gregtech.content.blockentity.machine.ItemPipeBlockEntity;
import gregtech.content.blockentity.machine.LogisticsWireBlockEntity;
import gregtech.content.cover.CoverStackData;
import gregtech.content.cover.CoverUtilityBehaviors;
import gregtech.content.logistics.LogisticsCoverInteraction;
import gregtech.engine.stack.NbtCompat;
import gregtech.engine.stack.StackData;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * Filter cover overwrite, wildcard, tool-guard, reset and drop regressions.
 *
 * <p>GT6 {@code CoverFilterItem}, {@code CoverFilterFluid}, {@code CoverRetrieverItem} and the filtered
 * logistics buses take a template from a right-click: a second, different template replaces the stored
 * filter, the wildcard toggle belongs to the item filter alone, a tool click never becomes the filter,
 * the 1×1 self-loop recipes return a clean cover, and a broken host drops the same detached stack a
 * crowbar returns.</p>
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class FilterCoverTests {

    private static final String WILDCARD_KEY = "gt.filter.wildcard";
    private static final String INVERTED_KEY = "gt.cover.inverted";
    private static final String LOGISTICS_VALUE_KEY = "gt.logistics.value";

    /** Tools consumed by filter covers or machine/pipe hosts that must never become the filter template. */
    private static final List<GTToolType> COVER_HOST_GUARDED_TOOLS = List.of(
            GTToolType.SOFT_HAMMER,
            GTToolType.SCREWDRIVER,
            GTToolType.MAGNIFYING_GLASS,
            GTToolType.CROWBAR,
            GTToolType.WRENCH);

    /** Tools consumed by logistics buses or logistics wires that must never become the filter template. */
    private static final List<GTToolType> LOGISTICS_GUARDED_TOOLS = List.of(
            GTToolType.SOFT_HAMMER,
            GTToolType.SCREWDRIVER,
            GTToolType.MAGNIFYING_GLASS,
            GTToolType.CROWBAR,
            GTToolType.WIRE_CUTTER);

    private FilterCoverTests() {}

    // ── Item filter ─────────────────────────────────────────────────────────────

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void itemFilterOverwritesAndTogglesWildcardOnMachinesAndPipes(GameTestHelper helper) {
        var player = mockPlayer(helper);
        var lookup = helper.getLevel().registryAccess();
        var machinePos = place(helper, 1, 2, "mixer_bronze");
        var pipePos = place(helper, 3, 2, "item_pipe_medium_brass");
        var machine = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(machinePos);
        var pipe = (ItemPipeBlockEntity) helper.getLevel().getBlockEntity(pipePos);
        helper.assertTrue(machine != null && pipe != null, "the item filter fixture is incomplete");
        helper.assertTrue(
                machine.attachCover(Direction.UP, new ItemStack(item("item_filter")))
                        && pipe.attachCover(Direction.UP, new ItemStack(item("item_filter"))),
                "item_filter did not attach to the machine and the item pipe");

        var iron = new ItemStack(Items.IRON_INGOT);
        var gold = new ItemStack(Items.GOLD_INGOT);

        helper.assertTrue(
                click(player, machinePos, Direction.UP, iron),
                "the machine rejected the first item filter click");
        helper.assertTrue(
                storedItem(machine.getCover(Direction.UP), lookup).is(Items.IRON_INGOT)
                        && !wildcard(machine.getCover(Direction.UP)),
                "the machine did not store the iron filter in exact mode");

        click(player, machinePos, Direction.UP, iron);
        helper.assertTrue(
                storedItem(machine.getCover(Direction.UP), lookup).is(Items.IRON_INGOT)
                        && wildcard(machine.getCover(Direction.UP)),
                "a repeated identical machine click did not turn the wildcard on");

        click(player, machinePos, Direction.UP, iron);
        helper.assertTrue(
                storedItem(machine.getCover(Direction.UP), lookup).is(Items.IRON_INGOT)
                        && !wildcard(machine.getCover(Direction.UP)),
                "a third identical machine click did not turn the wildcard back off");

        click(player, machinePos, Direction.UP, iron);
        helper.assertTrue(
                wildcard(machine.getCover(Direction.UP)),
                "a fourth identical machine click did not turn the wildcard back on");

        click(player, machinePos, Direction.UP, gold);
        helper.assertTrue(
                storedItem(machine.getCover(Direction.UP), lookup).is(Items.GOLD_INGOT)
                        && !wildcard(machine.getCover(Direction.UP)),
                "a different item did not overwrite the machine filter and reset the wildcard");

        helper.assertTrue(
                click(player, pipePos, Direction.UP, iron),
                "the item pipe rejected the first item filter click");
        helper.assertTrue(
                storedItem(pipe.getCover(Direction.UP), lookup).is(Items.IRON_INGOT)
                        && !wildcard(pipe.getCover(Direction.UP)),
                "the item pipe did not store the iron filter in exact mode");

        click(player, pipePos, Direction.UP, iron);
        helper.assertTrue(
                wildcard(pipe.getCover(Direction.UP)),
                "a repeated identical pipe click did not turn the wildcard on");

        click(player, pipePos, Direction.UP, gold);
        helper.assertTrue(
                storedItem(pipe.getCover(Direction.UP), lookup).is(Items.GOLD_INGOT)
                        && !wildcard(pipe.getCover(Direction.UP)),
                "a different item did not overwrite the pipe filter and reset the wildcard");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void itemFilterOverwritesDamageVariantsAndRejectsDisplayAndTools(GameTestHelper helper) {
        var player = mockPlayer(helper);
        var lookup = helper.getLevel().registryAccess();
        var machinePos = place(helper, 1, 2, "mixer_bronze");
        var pipePos = place(helper, 3, 2, "item_pipe_medium_brass");
        var machine = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(machinePos);
        var pipe = (ItemPipeBlockEntity) helper.getLevel().getBlockEntity(pipePos);
        helper.assertTrue(machine != null && pipe != null, "the item filter fixture is incomplete");
        helper.assertTrue(
                machine.attachCover(Direction.UP, new ItemStack(item("item_filter")))
                        && pipe.attachCover(Direction.UP, new ItemStack(item("item_filter"))),
                "item_filter did not attach to the machine and the item pipe");

        var pristine = new ItemStack(Items.IRON_PICKAXE);
        pristine.setDamageValue(3);
        var worn = new ItemStack(Items.IRON_PICKAXE);
        worn.setDamageValue(7);

        click(player, machinePos, Direction.UP, pristine);
        helper.assertTrue(
                storedItem(machine.getCover(Direction.UP), lookup).getDamageValue() == 3,
                "the machine did not store the damaged pickaxe");
        click(player, machinePos, Direction.UP, worn);
        var machineFilter = storedItem(machine.getCover(Direction.UP), lookup);
        helper.assertTrue(
                machineFilter.is(Items.IRON_PICKAXE)
                        && machineFilter.getDamageValue() == 7
                        && !wildcard(machine.getCover(Direction.UP)),
                "a different damage value did not overwrite the machine filter in exact mode");

        click(player, pipePos, Direction.UP, pristine);
        click(player, pipePos, Direction.UP, worn);
        var pipeFilter = storedItem(pipe.getCover(Direction.UP), lookup);
        helper.assertTrue(
                pipeFilter.is(Items.IRON_PICKAXE)
                        && pipeFilter.getDamageValue() == 7
                        && !wildcard(pipe.getCover(Direction.UP)),
                "a different damage value did not overwrite the pipe filter in exact mode");

        var waterDisplay = display(lookup, Fluids.WATER);
        // The machine still takes the carrier as fluid, but an item filter must never store it.
        click(player, machinePos, Direction.UP, waterDisplay);
        helper.assertTrue(
                storedItem(machine.getCover(Direction.UP), lookup).is(Items.IRON_PICKAXE),
                "the fluid display carrier replaced the machine item filter");
        helper.assertFalse(
                click(player, pipePos, Direction.UP, waterDisplay),
                "the item pipe item filter consumed a fluid display carrier");
        helper.assertTrue(
                storedItem(pipe.getCover(Direction.UP), lookup).is(Items.IRON_PICKAXE),
                "the fluid display carrier replaced the pipe item filter");

        assertToolsNeverBecomeTheFilter(
                helper,
                player,
                machinePos,
                Direction.UP,
                COVER_HOST_GUARDED_TOOLS,
                () -> {
                    machine.removeCover(Direction.UP);
                    machine.attachCover(Direction.UP, new ItemStack(item("item_filter")));
                    CoverUtilityBehaviors.setItemFilter(
                            machine.getCover(Direction.UP), new ItemStack(Items.IRON_INGOT), lookup);
                },
                () -> storedItem(machine.getCover(Direction.UP), lookup));
        assertToolsNeverBecomeTheFilter(
                helper,
                player,
                pipePos,
                Direction.UP,
                COVER_HOST_GUARDED_TOOLS,
                () -> {
                    pipe.removeCover(Direction.UP);
                    pipe.attachCover(Direction.UP, new ItemStack(item("item_filter")));
                    CoverUtilityBehaviors.setItemFilter(
                            pipe.getCover(Direction.UP), new ItemStack(Items.IRON_INGOT), lookup);
                },
                () -> storedItem(pipe.getCover(Direction.UP), lookup));
        for (var allowedTool : List.of(GTToolType.PICKAXE, GTToolType.WIRE_CUTTER)) {
            var toolStack = GTToolHelper.displayTool(allowedTool);
            helper.assertTrue(
                    click(player, machinePos, Direction.UP, toolStack)
                            && storedItem(machine.getCover(Direction.UP), lookup).is(toolStack.getItem()),
                    allowedTool + " was blocked from becoming the machine item filter");
            helper.assertTrue(
                    click(player, pipePos, Direction.UP, toolStack)
                            && storedItem(pipe.getCover(Direction.UP), lookup).is(toolStack.getItem()),
                    allowedTool + " was blocked from becoming the item pipe filter");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void itemRetrieverCoverOverwritesWithoutWildcard(GameTestHelper helper) {
        var player = mockPlayer(helper);
        var lookup = helper.getLevel().registryAccess();
        var pipePos = place(helper, 1, 2, "item_pipe_medium_brass");
        var pipe = (ItemPipeBlockEntity) helper.getLevel().getBlockEntity(pipePos);
        helper.assertTrue(pipe != null, "the item retriever fixture is missing its pipe");
        helper.assertTrue(
                pipe.attachCover(Direction.UP, new ItemStack(item("item_retriever_cover"))),
                "item_retriever_cover did not attach to the item pipe");

        var iron = new ItemStack(Items.IRON_INGOT);
        click(player, pipePos, Direction.UP, iron);
        helper.assertTrue(
                storedItem(pipe.getCover(Direction.UP), lookup).is(Items.IRON_INGOT)
                        && !wildcard(pipe.getCover(Direction.UP)),
                "the retriever did not store the iron filter in exact mode");

        click(player, pipePos, Direction.UP, iron);
        helper.assertTrue(
                storedItem(pipe.getCover(Direction.UP), lookup).is(Items.IRON_INGOT)
                        && !wildcard(pipe.getCover(Direction.UP)),
                "a repeated identical retriever click toggled a wildcard it does not have");

        click(player, pipePos, Direction.UP, new ItemStack(Items.DIAMOND));
        helper.assertTrue(
                storedItem(pipe.getCover(Direction.UP), lookup).is(Items.DIAMOND),
                "a different item did not overwrite the retriever filter");

        helper.assertFalse(
                click(player, pipePos, Direction.UP, display(lookup, Fluids.WATER)),
                "the retriever consumed a fluid display carrier");
        helper.assertTrue(
                storedItem(pipe.getCover(Direction.UP), lookup).is(Items.DIAMOND),
                "the fluid display carrier replaced the retriever filter");

        assertToolsNeverBecomeTheFilter(
                helper,
                player,
                pipePos,
                Direction.UP,
                COVER_HOST_GUARDED_TOOLS,
                () -> {
                    pipe.removeCover(Direction.UP);
                    pipe.attachCover(Direction.UP, new ItemStack(item("item_retriever_cover")));
                    CoverUtilityBehaviors.setItemFilter(
                            pipe.getCover(Direction.UP), new ItemStack(Items.IRON_INGOT), lookup);
                },
                () -> storedItem(pipe.getCover(Direction.UP), lookup));
        for (var allowedTool : List.of(GTToolType.PICKAXE, GTToolType.WIRE_CUTTER)) {
            var toolStack = GTToolHelper.displayTool(allowedTool);
            helper.assertTrue(
                    click(player, pipePos, Direction.UP, toolStack)
                            && storedItem(pipe.getCover(Direction.UP), lookup).is(toolStack.getItem()),
                    allowedTool + " was blocked from becoming the retriever filter");
        }
        helper.succeed();
    }

    // ── Fluid filter ────────────────────────────────────────────────────────────

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void fluidFilterOverwritesOnPipesAndMachines(GameTestHelper helper) {
        var player = mockPlayer(helper);
        var lookup = helper.getLevel().registryAccess();
        var pipePos = place(helper, 1, 2, "pipe_large_steel");
        var machinePos = place(helper, 3, 2, "mixer_bronze");
        var pipe = (FluidPipeBlockEntity) helper.getLevel().getBlockEntity(pipePos);
        var machine = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(machinePos);
        helper.assertTrue(pipe != null && machine != null, "the fluid filter fixture is incomplete");
        helper.assertTrue(
                pipe.attachCover(Direction.UP, new ItemStack(item("fluid_filter")))
                        && machine.attachCover(Direction.UP, new ItemStack(item("fluid_filter"))),
                "fluid_filter did not attach to the fluid pipe and the machine");

        var water = new ItemStack(Items.WATER_BUCKET);
        var lava = new ItemStack(Items.LAVA_BUCKET);
        var lavaDisplay = display(lookup, Fluids.LAVA);

        helper.assertTrue(
                click(player, pipePos, Direction.UP, water),
                "the fluid pipe rejected the first fluid filter click");
        helper.assertTrue(
                isFluid(storedFluid(pipe.getCover(Direction.UP), lookup), Fluids.WATER),
                "the fluid pipe did not store the water sample");

        click(player, pipePos, Direction.UP, lava);
        helper.assertTrue(
                isFluid(storedFluid(pipe.getCover(Direction.UP), lookup), Fluids.LAVA),
                "a lava bucket did not overwrite the pipe filter");
        helper.assertTrue(pipe.getFluidInTank(0).isEmpty(), "the lava sample leaked into the pipe tank");

        // Repeating the same fluid leaves the filter unchanged and still blocks pouring into the pipe.
        click(player, pipePos, Direction.UP, lavaDisplay);
        helper.assertTrue(
                isFluid(storedFluid(pipe.getCover(Direction.UP), lookup), Fluids.LAVA),
                "a repeated lava click changed the pipe filter");

        var saved = NbtCompat.saveWithoutMetadata(pipe, lookup);
        helper.assertTrue(
                saved.contains(coverKey(Direction.UP)), "the pipe snapshot dropped its cover");
        helper.assertTrue(
                isFluid(storedFluid(NbtCompat.readItem(saved.getCompound(coverKey(Direction.UP)), lookup), lookup),
                        Fluids.LAVA),
                "the pipe filter did not survive saveWithoutMetadata");

        helper.assertTrue(
                click(player, machinePos, Direction.UP, water),
                "the machine rejected the first fluid filter click");
        helper.assertTrue(
                isFluid(storedFluid(machine.getCover(Direction.UP), lookup), Fluids.WATER),
                "the machine did not store the water sample");

        click(player, machinePos, Direction.UP, lava);
        helper.assertTrue(
                isFluid(storedFluid(machine.getCover(Direction.UP), lookup), Fluids.LAVA),
                "a lava bucket did not overwrite the machine filter");
        helper.assertTrue(
                machine.getTanksInput()[0].isEmpty(), "the lava sample leaked into the machine input tank");

        // As on the pipe, the repeated sample only leaves the filter alone.
        click(player, machinePos, Direction.UP, lavaDisplay);
        helper.assertTrue(
                isFluid(storedFluid(machine.getCover(Direction.UP), lookup), Fluids.LAVA),
                "a repeated lava click changed the machine filter");

//? if >=1.21.1 {
        /*var update = machine.getUpdateTag(lookup);
*///?} else {
        var update = machine.getUpdateTag();
//?}
        helper.assertTrue(
                update.contains(coverKey(Direction.UP)), "the machine update tag dropped its cover");
        helper.assertTrue(
                isFluid(storedFluid(NbtCompat.readItem(update.getCompound(coverKey(Direction.UP)), lookup), lookup),
                        Fluids.LAVA),
                "the machine filter did not survive getUpdateTag");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void itemPipeSyncClearsTheLastCoverWithoutSendingInventory(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess();
        var pos = place(helper, 1, 2, "item_pipe_medium_brass");
        var pipe = (ItemPipeBlockEntity) helper.getLevel().getBlockEntity(pos);
        var replica = new ItemPipeBlockEntity(pos, pipe.getBlockState());
        pipe.insertItem(0, new ItemStack(Items.GOLD_INGOT), false);
        helper.assertTrue(pipe.attachCover(Direction.UP, new ItemStack(item("item_filter")))
                        && pipe.clickFilterCover(Direction.UP, new ItemStack(Items.IRON_INGOT)),
                "the item pipe stores a filter and a separate inventory stack");

        var snapshot = NbtCompat.getUpdateTag(pipe, lookup);
        helper.assertFalse(snapshot.contains("gt.items") || snapshot.contains("gt.disabled_inputs")
                        || snapshot.contains("gt.disabled_outputs"),
                "the item pipe sends no inventory or disabled IO fields");
//? if >=1.21.1 {
        /*replica.handleUpdateTag(snapshot, lookup);
*///?} else {
        replica.handleUpdateTag(snapshot);
//?}
        helper.assertTrue(storedItem(replica.getCover(Direction.UP), lookup).is(Items.IRON_INGOT),
                "the chunk snapshot applies the filter");
        helper.assertTrue(replica.getStackInSlot(0).isEmpty(), "the chunk snapshot omits the server inventory");

        pipe.removeCover(Direction.UP);
        var packet = pipe.getUpdatePacket();
        helper.assertTrue(packet.getTag() != null, "an empty cover snapshot survives packet creation");
//? if >=1.21.1 {
        /*replica.onDataPacket(null, packet, lookup);
*///?} else {
        replica.onDataPacket(null, packet);
//?}
        helper.assertTrue(replica.getCover(Direction.UP).isEmpty(), "the update clears the last cover");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void fluidPipeChunkAndPacketSyncApplyTankAndCoverState(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess();
        var pos = place(helper, 1, 2, "pipe_medium_steel");
        var pipe = (FluidPipeBlockEntity) helper.getLevel().getBlockEntity(pos);
        var replica = new FluidPipeBlockEntity(pos, pipe.getBlockState());
        helper.assertTrue(pipe.fill(new FluidStack(Fluids.WATER, 80), FluidAction.EXECUTE) == 80
                        && pipe.attachCover(Direction.UP, new ItemStack(item("fluid_filter"))),
                "the fluid-pipe fixture contains water and a cover");
        var snapshot = NbtCompat.getUpdateTag(pipe, lookup);
//? if >=1.21.1 {
        /*replica.handleUpdateTag(snapshot, lookup);
*///?} else {
        replica.handleUpdateTag(snapshot);
//?}
        helper.assertTrue(replica.getFluidInTank(0).getAmount() == 80 && !replica.getCover(Direction.UP).isEmpty(),
                "the chunk snapshot applies both the tank and cover");

        pipe.drain(80, FluidAction.EXECUTE);
        pipe.removeCover(Direction.UP);
        var packet = pipe.getUpdatePacket();
//? if >=1.21.1 {
        /*replica.onDataPacket(null, packet, lookup);
*///?} else {
        replica.onDataPacket(null, packet);
//?}
        helper.assertTrue(replica.getFluidInTank(0).isEmpty() && replica.getCover(Direction.UP).isEmpty(),
                "the incremental update clears both the tank and cover");
        helper.succeed();
    }

    // ── Filtered logistics buses ────────────────────────────────────────────────

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void logisticsFilteredBusesOverwriteFiltersAndIgnoreTools(GameTestHelper helper) {
        var player = mockPlayer(helper);
        var lookup = helper.getLevel().registryAccess();
        var itemWirePos = place(helper, 1, 2, "logistics_wire");
        var fluidWirePos = place(helper, 3, 2, "logistics_wire");
        var itemWire = (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(itemWirePos);
        var fluidWire = (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(fluidWirePos);
        helper.assertTrue(itemWire != null && fluidWire != null, "the logistics fixture is incomplete");
        helper.assertTrue(
                itemWire
                                .logisticsCovers()
                                .attach(Direction.UP, new ItemStack(item("filtered_logistics_import_bus_item")))
                        && fluidWire
                                .logisticsCovers()
                                .attach(Direction.UP, new ItemStack(item("filtered_logistics_import_bus_fluid"))),
                "the filtered logistics buses did not attach");

        click(player, itemWirePos, Direction.UP, new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(
                logisticsItem(itemWire, Direction.UP, lookup).is(Items.IRON_INGOT),
                "the filtered item bus did not store the iron filter");
        click(player, itemWirePos, Direction.UP, new ItemStack(Items.GOLD_INGOT));
        helper.assertTrue(
                logisticsItem(itemWire, Direction.UP, lookup).is(Items.GOLD_INGOT),
                "a different item did not overwrite the bus filter");

        helper.assertFalse(
                click(player, itemWirePos, Direction.UP, display(lookup, Fluids.WATER)),
                "the filtered item bus consumed a fluid display carrier");
        helper.assertTrue(
                logisticsItem(itemWire, Direction.UP, lookup).is(Items.GOLD_INGOT),
                "the fluid display carrier replaced the bus filter");

        assertToolsNeverBecomeTheFilter(
                helper,
                player,
                itemWirePos,
                Direction.UP,
                LOGISTICS_GUARDED_TOOLS,
                () -> {
                    itemWire.logisticsCovers().remove(Direction.UP);
                    itemWire.logisticsCovers()
                            .attach(Direction.UP, new ItemStack(item("filtered_logistics_import_bus_item")));
                    LogisticsCoverInteraction.setItemFilter(
                            itemWire.logisticsCovers(), Direction.UP, new ItemStack(Items.IRON_INGOT));
                },
                () -> logisticsItem(itemWire, Direction.UP, lookup));
        var pickaxe = GTToolHelper.displayTool(GTToolType.PICKAXE);
        helper.assertTrue(
                click(player, itemWirePos, Direction.UP, pickaxe)
                        && logisticsItem(itemWire, Direction.UP, lookup).is(pickaxe.getItem()),
                "non-consumed GT pickaxe was blocked from becoming the logistics bus filter");

        click(player, fluidWirePos, Direction.UP, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(
                isFluid(logisticsFluid(fluidWire, Direction.UP, lookup), Fluids.WATER),
                "the filtered fluid bus did not store the water sample");
        click(player, fluidWirePos, Direction.UP, new ItemStack(Items.LAVA_BUCKET));
        helper.assertTrue(
                isFluid(logisticsFluid(fluidWire, Direction.UP, lookup), Fluids.LAVA),
                "a lava bucket did not overwrite the fluid bus filter");
        click(player, fluidWirePos, Direction.UP, display(lookup, Fluids.LAVA));
        helper.assertTrue(
                isFluid(logisticsFluid(fluidWire, Direction.UP, lookup), Fluids.LAVA),
                "a repeated lava click changed the fluid bus filter");
        helper.succeed();
    }

    // ── 1×1 reset recipes ───────────────────────────────────────────────────────

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void craftingGridResetClearsCoverData(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess();
        var iron = new ItemStack(Items.IRON_INGOT);
        var water = new FluidStack(Fluids.WATER, 1000);

        for (String path : List.of("item_filter", "fluid_filter", "item_retriever_cover")) {
            var cover = new ItemStack(item(path));
            if (path.equals("fluid_filter")) {
                helper.assertTrue(
                        CoverUtilityBehaviors.setFluidFilter(cover, water, lookup),
                        "the fluid filter fixture did not store its sample");
            } else {
                helper.assertTrue(
                        CoverUtilityBehaviors.setItemFilter(cover, iron, lookup),
                        path + " fixture did not store its item filter");
                CoverStackData.putBoolean(cover, WILDCARD_KEY, true);
            }
            CoverStackData.putBoolean(cover, INVERTED_KEY, true);
            helper.assertTrue(StackData.hasAnyData(cover), path + " fixture carries no cover data");

            var reset = recipe(helper, "logistics/" + path + "_reset");
            helper.assertTrue(
                    reset.getType() == RecipeType.CRAFTING, path + "_reset is not a crafting recipe");
            var result = craft(helper, reset, cover);
            helper.assertFalse(result.isEmpty(), path + "_reset assembled nothing");
            helper.assertTrue(
                    result.is(item(path)) && result.getCount() == 1, path + "_reset returned " + result);
            helper.assertFalse(
                    StackData.hasAnyData(result),
                    path + "_reset kept cover data: " + StackData.read(result));
            helper.assertTrue(
                    sameItem(result, new ItemStack(item(path))),
                    path + "_reset does not stack with a fresh cover");
        }

        // The cycle recipe that yields the filtered item bus still resolves from a configured export bus.
        var exportBus = new ItemStack(item("filtered_logistics_export_bus_item"));
        helper.assertTrue(
                CoverUtilityBehaviors.setItemFilter(exportBus, iron, lookup),
                "the export bus fixture did not store its item filter");
        CoverStackData.putInt(exportBus, LOGISTICS_VALUE_KEY, 7);
        var cycle = recipe(helper, "logistics/logistics_cycle_item_import");
        helper.assertTrue(
                cycle.getType() == RecipeType.CRAFTING, "the item import cycle is not a crafting recipe");
        var cycled = craft(helper, cycle, exportBus);
        helper.assertTrue(
                cycled.is(item("filtered_logistics_import_bus_item")) && cycled.getCount() == 1,
                "the item import cycle did not resolve from a configured export bus");
        helper.assertFalse(
                StackData.hasAnyData(cycled), "the item import cycle kept the export bus data");
        helper.succeed();
    }

    // ── Broken hosts ────────────────────────────────────────────────────────────

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void brokenHostsDropDetachedCovers(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess();
        var iron = new ItemStack(Items.IRON_INGOT);

        var machinePos = place(helper, 1, 2, "mixer_bronze");
        var machine = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(machinePos);
        helper.assertTrue(
                machine != null && machine.attachCover(Direction.UP, new ItemStack(item("item_filter"))),
                "the machine filter cover did not attach");
        helper.assertTrue(
                CoverUtilityBehaviors.setItemFilter(machine.getCover(Direction.UP), iron, lookup),
                "the machine filter cover did not store its filter");
        CoverStackData.putBoolean(machine.getCover(Direction.UP), INVERTED_KEY, true);
        var machineCover = machine.getCover(Direction.UP).copy();
        helper.getLevel().destroyBlock(machinePos, true);
        var machineDrop = singleDrop(helper, machinePos, item("item_filter"), "the broken machine");
        helper.assertTrue(
                sameItem(machineDrop, CoverStackData.detached(machineCover)),
                "the broken machine dropped " + machineDrop + " instead of " + CoverStackData.detached(machineCover));
        helper.assertTrue(
                storedItem(machineDrop, lookup).is(Items.IRON_INGOT),
                "the broken machine lost the item filter on its dropped cover");
        helper.assertTrue(inverted(machineDrop), "the broken machine lost the blacklist flag on its item filter");

        // Reattaching a crowbar-removed/broken blacklisted filter cover preserves blacklist; non-filter covers strip it.
        var freshMachinePos = place(helper, 2, 2, "mixer_bronze");
        var freshMachine = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(freshMachinePos);
        helper.assertTrue(
                freshMachine != null && freshMachine.attachCover(Direction.UP, machineDrop),
                "the removed blacklisted filter cover did not reattach");
        helper.assertTrue(
                inverted(freshMachine.getCover(Direction.UP))
                        && !freshMachine.coverFilterPermits(Direction.UP, iron),
                "reattaching a blacklisted filter cover lost its blacklist behavior");
        var crowbarRemoved = freshMachine.removeCover(Direction.UP);
        helper.assertTrue(
                inverted(crowbarRemoved) && storedItem(crowbarRemoved, lookup).is(Items.IRON_INGOT),
                "crowbar removal lost the blacklist flag or item template");

        var shutter = new ItemStack(item("shutter_cover"));
        CoverStackData.putBoolean(shutter, INVERTED_KEY, true);
        helper.assertFalse(
                inverted(CoverStackData.detached(shutter)),
                "detached non-filter cover unexpectedly kept gt.cover.inverted");

        var pipePos = place(helper, 3, 2, "pipe_large_steel");
        var pipe = (FluidPipeBlockEntity) helper.getLevel().getBlockEntity(pipePos);
        helper.assertTrue(
                pipe != null && pipe.attachCover(Direction.UP, new ItemStack(item("fluid_filter"))),
                "the pipe fluid filter cover did not attach");
        helper.assertTrue(
                CoverUtilityBehaviors.setFluidFilter(
                        pipe.getCover(Direction.UP), new FluidStack(Fluids.LAVA, 1000), lookup),
                "the pipe fluid filter cover did not store its sample");
        CoverStackData.putBoolean(pipe.getCover(Direction.UP), INVERTED_KEY, true);
        var pipeCover = pipe.getCover(Direction.UP).copy();
        helper.getLevel().destroyBlock(pipePos, true);
        var pipeDrop = singleDrop(helper, pipePos, item("fluid_filter"), "the broken fluid pipe");
        helper.assertTrue(
                sameItem(pipeDrop, CoverStackData.detached(pipeCover)),
                "the broken fluid pipe dropped " + pipeDrop + " instead of " + CoverStackData.detached(pipeCover));
        helper.assertTrue(
                isFluid(storedFluid(pipeDrop, lookup), Fluids.LAVA),
                "the broken fluid pipe lost the fluid sample on its dropped cover");
        helper.assertTrue(inverted(pipeDrop), "the broken fluid pipe lost the blacklist flag on its fluid filter");

        var wirePos = place(helper, 5, 2, "logistics_wire");
        var wire = (LogisticsWireBlockEntity) helper.getLevel().getBlockEntity(wirePos);
        helper.assertTrue(
                wire != null
                        && wire.logisticsCovers()
                                .attach(Direction.UP, new ItemStack(item("filtered_logistics_import_bus_item"))),
                "the filtered item bus did not attach");
        helper.assertTrue(
                LogisticsCoverInteraction.setItemFilter(
                        wire.logisticsCovers(), Direction.UP, iron),
                "the filtered item bus did not store its filter");
        CoverStackData.putInt(wire.logisticsCovers().get(Direction.UP), LOGISTICS_VALUE_KEY, 7);
        var busCover = wire.logisticsCovers().get(Direction.UP).copy();
        helper.getLevel().destroyBlock(wirePos, true);
        var busDrop = singleDrop(
                helper, wirePos, item("filtered_logistics_import_bus_item"), "the broken logistics wire");
        helper.assertTrue(
                sameItem(busDrop, CoverStackData.detached(busCover)),
                "the broken logistics wire dropped " + busDrop + " instead of " + CoverStackData.detached(busCover));
        helper.assertTrue(
                storedItem(busDrop, lookup).is(Items.IRON_INGOT),
                "the broken logistics wire lost the item filter on its dropped bus");
        helper.assertFalse(
                StackData.readOrEmpty(busDrop).contains(LOGISTICS_VALUE_KEY),
                "the broken logistics wire kept the routing priority on its dropped bus");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void renamedCoversAndCoverSampleUseItemOnPath(GameTestHelper helper) {
        var level = helper.getLevel();
        var lookup = level.registryAccess();

        // Renamed covers carry custom name data without gt.cover.inverted custom data; reading state must not throw.
        for (String path : List.of("item_filter", "fluid_filter", "item_retriever_cover")) {
            var renamed = new ItemStack(item(path));
            gregtech.platform.Mc.setCustomName(renamed, net.minecraft.network.chat.Component.literal("Renamed"));
            helper.assertFalse(
                    gregtech.content.cover.MachineCoverSpec.inverted(renamed),
                    path + " with only custom name resolved as inverted");
            helper.assertFalse(
                    gregtech.content.cover.MachineCoverSpec.strong(renamed),
                    path + " with only custom name resolved as strong");
        }

        var wirePos = place(helper, 1, 2, "logistics_wire");
        var machinePos = place(helper, 3, 2, "mixer_bronze");
        var itemPipePos = place(helper, 5, 2, "item_pipe_medium_brass");
        var fluidPipePos = place(helper, 1, 4, "pipe_large_steel");
        var wire = (LogisticsWireBlockEntity) level.getBlockEntity(wirePos);
        var machine = (BasicMachineBlockEntity) level.getBlockEntity(machinePos);
        var itemPipe = (ItemPipeBlockEntity) level.getBlockEntity(itemPipePos);
        var fluidPipe = (FluidPipeBlockEntity) level.getBlockEntity(fluidPipePos);
        helper.assertTrue(
                wire != null && machine != null && itemPipe != null && fluidPipe != null,
                "the useItemOn fixture is incomplete");
        helper.assertTrue(
                wire.logisticsCovers().attach(Direction.UP, new ItemStack(item("filtered_logistics_import_bus_item")))
                        && machine.attachCover(Direction.UP, new ItemStack(item("item_filter")))
                        && itemPipe.attachCover(Direction.UP, new ItemStack(item("item_filter")))
                        && fluidPipe.attachCover(Direction.UP, new ItemStack(item("fluid_filter"))),
                "failed to attach covers for useItemOn fixture");

        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "filter-cover-test");
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
//? if >=1.21.1 {
        /*var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(
                level.getServer(), connection, player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
*///?} else {
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(
                level.getServer(), connection, player) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
//?}
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.initInventoryMenu();

            // Standing clicks with items that double as covers (repeater, redstone torch) must configure templates.
            for (var sample : List.of(Items.REPEATER, Items.REDSTONE_TORCH)) {
                var stack = new ItemStack(sample);
                helper.assertTrue(
                        useItemOn(player, wirePos, Direction.UP, stack.copy()).consumesAction()
                                && logisticsItem(wire, Direction.UP, lookup).is(sample),
                        "filtered logistics bus rejected cover-capable item " + sample);
                helper.assertTrue(
                        useItemOn(player, machinePos, Direction.UP, stack.copy()).consumesAction()
                                && storedItem(machine.getCover(Direction.UP), lookup).is(sample),
                        "machine item_filter rejected cover-capable item " + sample);
                helper.assertTrue(
                        useItemOn(player, itemPipePos, Direction.UP, stack.copy()).consumesAction()
                                && storedItem(itemPipe.getCover(Direction.UP), lookup).is(sample),
                        "item pipe item_filter rejected cover-capable item " + sample);
            }

            // Repeating the same fluid container on fluid_filter keeps intercepting consistently without pouring.
            var waterBucket = new ItemStack(Items.WATER_BUCKET);
            helper.assertTrue(
                    useItemOn(player, fluidPipePos, Direction.UP, waterBucket).consumesAction()
                            && isFluid(storedFluid(fluidPipe.getCover(Direction.UP), lookup), Fluids.WATER)
                            && fluidPipe.getFluidInTank(0).isEmpty(),
                    "first water bucket click did not store sample cleanly");
            helper.assertTrue(
                    useItemOn(player, fluidPipePos, Direction.UP, waterBucket).consumesAction()
                            && isFluid(storedFluid(fluidPipe.getCover(Direction.UP), lookup), Fluids.WATER)
                            && fluidPipe.getFluidInTank(0).isEmpty()
                            && player.getMainHandItem().is(Items.WATER_BUCKET),
                    "repeated water bucket click leaked into the pipe behind fluid_filter");
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    private static net.minecraft.world.InteractionResult useItemOn(
            net.minecraft.server.level.ServerPlayer player, BlockPos pos, Direction face, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var hit = new BlockHitResult(
                Vec3.atCenterOf(pos)
                        .add(face.getStepX() * 0.5D, face.getStepY() * 0.5D, face.getStepZ() * 0.5D),
                face,
                pos,
                false);
        return player.gameMode.useItemOn(player, player.level(), held, InteractionHand.MAIN_HAND, hit);
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void pipeCrowbarReturnsCoverWithoutToolWear(GameTestHelper helper) {
        Player player = mockPlayer(helper);
        player.getAbilities().instabuild = false;
        var crowbar = GTToolHelper.displayTool(GTToolType.CROWBAR);
        int damage = crowbar.getDamageValue();
        var itemPipe = place(helper, 1, 2, "item_pipe_medium_brass");
        var fluidPipe = place(helper, 3, 2, "pipe_large_steel");
        for (BlockPos pos : List.of(itemPipe, fluidPipe)) {
            var host = (gregtech.content.cover.PipeCoverHost) helper.getLevel().getBlockEntity(pos);
            var filter = new ItemStack(item(pos.equals(itemPipe) ? "item_filter" : "fluid_filter"));
            helper.assertTrue(host.attachCover(Direction.UP, filter), "filter attaches to the pipe");
            int count = player.getInventory().countItem(filter.getItem());
            helper.assertTrue(click(player, pos, Direction.UP, crowbar) && host.getCover(Direction.UP).isEmpty(),
                    "a crowbar removes the pipe cover");
            helper.assertTrue(player.getInventory().countItem(filter.getItem()) == count + 1
                            && crowbar.getDamageValue() == damage,
                    "the cover is returned to the player without wearing the crowbar");
        }
        helper.succeed();
    }

    // ── Shared fixtures ─────────────────────────────────────────────────────────

    /** Places a GT block at a relative test position and returns its absolute position. */
    private static BlockPos place(GameTestHelper helper, int x, int z, String path) {
        var pos = helper.absolutePos(new BlockPos(x, 1, z));
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", path));
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        return pos;
    }

    /** A server-side player without a client connection, as the other cover tests use. */
    private static Player mockPlayer(GameTestHelper helper) {
//? if >=1.21.1 {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        return helper.makeMockPlayer();
//?}
    }

    /** Right-clicks one block face with {@code held} through the block's own use path. */
    private static boolean click(Player player, BlockPos pos, Direction face, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var level = player.level();
        var hit = new BlockHitResult(
                Vec3.atCenterOf(pos)
                        .add(face.getStepX() * 0.5D, face.getStepY() * 0.5D, face.getStepZ() * 0.5D),
                face,
                pos,
                false);
        BlockState state = level.getBlockState(pos);
//? if >=1.21.1 {
        /*return state.useItemOn(held, level, player, InteractionHand.MAIN_HAND, hit).consumesAction();
*///?} else {
        return state.use(level, player, InteractionHand.MAIN_HAND, hit).consumesAction();
//?}
    }

    /**
     * Clicks every guarded tool on one filter cover and checks the tool never becomes the stored filter.
     *
     * <p>{@code reinstall} restores a clean cover holding an iron filter, because a crowbar click removes
     * the cover and a soft-hammer click clears the filter.</p>
     */
    private static void assertToolsNeverBecomeTheFilter(GameTestHelper helper, Player player, BlockPos pos,
            Direction face, List<GTToolType> guardedTools, Runnable reinstall, Supplier<ItemStack> filter) {
        for (var type : guardedTools) {
            reinstall.run();
            helper.assertTrue(
                    filter.get().is(Items.IRON_INGOT),
                    "the tool guard fixture lost its iron filter before " + type);
            click(player, pos, face, GTToolHelper.displayTool(type));
            var stored = filter.get();
            helper.assertTrue(
                    stored.isEmpty() || stored.is(Items.IRON_INGOT),
                    type + " wrote itself into the item filter: " + stored);
        }
    }

    /** The item stored under {@code gt.filter.item} on a cover stack. */
    private static ItemStack storedItem(ItemStack cover, HolderLookup.Provider lookup) {
        return CoverUtilityBehaviors.itemFilter(cover, lookup);
    }

    /** The fluid stored under {@code gt.filter.fluid} on a cover stack. */
    private static FluidStack storedFluid(ItemStack cover, HolderLookup.Provider lookup) {
        return CoverUtilityBehaviors.fluidFilter(cover, lookup);
    }

    /** The item filter of a filtered logistics bus. */
    private static ItemStack logisticsItem(LogisticsWireBlockEntity wire, Direction face,
            HolderLookup.Provider lookup) {
        return LogisticsCoverInteraction.itemFilter(wire.logisticsCovers().get(face), lookup);
    }

    /** The fluid filter of a filtered logistics bus. */
    private static FluidStack logisticsFluid(LogisticsWireBlockEntity wire, Direction face,
            HolderLookup.Provider lookup) {
        return LogisticsCoverInteraction.fluidFilter(wire.logisticsCovers().get(face), lookup);
    }

    /** A creative fluid display carrier holding 1000 mB of {@code fluid}. */
    private static ItemStack display(HolderLookup.Provider lookup, net.minecraft.world.level.material.Fluid fluid) {
        return gregtech.engine.fluid.FluidDisplayBinding.display(lookup, new FluidStack(fluid, 1000));
    }

    private static boolean isFluid(FluidStack stack, net.minecraft.world.level.material.Fluid fluid) {
        return stack.getFluid() == fluid && stack.getAmount() == 1000;
    }

    private static boolean wildcard(ItemStack cover) {
        return StackData.getBoolean(cover, WILDCARD_KEY, false);
    }

    private static boolean inverted(ItemStack cover) {
        return StackData.getBoolean(cover, INVERTED_KEY, false);
    }

    /** The flat cover key a host stores one face under. */
    private static String coverKey(Direction face) {
        return "gt_cover_" + face.ordinal();
    }

    /** Resolves a GT recipe id or fails with the missing id. */
    private static Recipe<?> recipe(GameTestHelper helper, String path) {
        var id = ResourceLocation.fromNamespaceAndPath("gt6ce", path);
        var manager = helper.getLevel().getRecipeManager();
//? if >=1.21.1 {
        /*return manager.byKey(id)
                .map(holder -> (Recipe<?>) holder.value())
                .orElseThrow(() -> new IllegalStateException("missing recipe " + id));
*///?} else {
        return manager.byKey(id)
                .map(recipe -> (Recipe<?>) recipe)
                .orElseThrow(() -> new IllegalStateException("missing recipe " + id));
//?}
    }

    /** Matches and assembles a recipe in a 1×1 grid, or returns an empty stack when it does not match. */
    private static ItemStack craft(GameTestHelper helper, Recipe<?> recipe, ItemStack input) {
        var crafting = (CraftingRecipe) recipe;
        var items = List.of(input.copy());
//? if >=1.21.1 {
        /*var grid = CraftingInput.ofPositioned(1, 1, items).input();
        if (!crafting.matches(grid, helper.getLevel())) return ItemStack.EMPTY;
        return crafting.assemble(grid, helper.getLevel().registryAccess());
*///?} else {
        var grid = container(1, 1);
        grid.setItem(0, items.get(0));
        if (!crafting.matches(grid, helper.getLevel())) return ItemStack.EMPTY;
        return crafting.assemble(grid, helper.getLevel().registryAccess());
//?}
    }

    // Creates a 1.20 crafting container without opening a real menu.
    //? if <1.21.1 {
    private static TransientCraftingContainer container(int width, int height) {
        var menu = new AbstractContainerMenu(MenuType.CRAFTING, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        return new TransientCraftingContainer(menu, width, height);
    }
    //?}

    /** Whether two stacks are the same item with the same data on the active loader. */
    private static boolean sameItem(ItemStack left, ItemStack right) {
//? if >=1.21.1 {
        /*return ItemStack.isSameItemSameComponents(left, right);
*///?} else {
        return ItemStack.isSameItemSameTags(left, right);
//?}
    }

    /** The single item entity of {@code expected} the broken host left behind. */
    private static ItemStack singleDrop(GameTestHelper helper, BlockPos pos, Item expected, String label) {
        var found = new ArrayList<ItemStack>();
        for (var entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D)))
            if (entity.getItem().is(expected)) found.add(entity.getItem());
        helper.assertTrue(found.size() == 1, label + " dropped " + found.size() + " " + expected);
        return found.get(0);
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", path));
    }
}
