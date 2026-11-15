package gregtech.content.recipe;

import gregtech.content.material.Materials;
import gregtech.content.registry.GTFluids;
import gregtech.content.worldgen.MaterialWorldgenFacts;
import gregtech.content.worldgen.MaterialWorldgenFacts.Fact;
import gregtech.content.worldgen.MaterialWorldgenFacts.Kind;
import gregtech.content.worldgen.MaterialWorldgenFacts.Ore;
import gregtech.engine.material.MaterialProcessGraph.*;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.material.MaterialPublication;
import gregtech.engine.material.MaterialViews;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.FluidStack;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MaterialProcessSnapshotTests {
    private MaterialProcessSnapshotTests() {}

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void presentationPreservesNativeItemState(GameTestHelper helper) {
        var input = new ItemStack(Items.IRON_INGOT);
        var data = gregtech.content.data.UsbDataMedia.tagOrEmpty(input);
        data.putString("process_test", "stateful_input");
        gregtech.content.data.UsbDataMedia.store(input, data);
        var recipe = row(new ItemStack[] {input}, Items.COPPER_INGOT);
        var map = new RecipeMap(new ArrayList<>(List.of(recipe)),
                "gt.test.process.presentation", "Test", "textures/gui/test", 9, 9, 0, 0, 0, 0, 0,
                false, false, false, false);
        RecipeMap.unregister(map);
        var snapshot = MaterialProcessSnapshot.build(MaterialPublication.current(), 1,
                new MaterialWorldgenFacts.Snapshot(List.of()), List.of(map));
        var root = snapshot.roots().stream().filter(value -> value.material().equals("Copper")).findFirst().orElseThrow();
        var pages = gregtech.compat.common.MaterialProcessProjection.pages(snapshot, root.resource(),
                gregtech.compat.common.MaterialProcessDisplay.Navigation.NONE);
        var displayInput = pages.get(0).steps().get(0).inputs().get(0);
        helper.assertTrue(gregtech.engine.stack.StackCompat.sameItem(displayInput.icons().get(0), input),
                "presentation keeps the native item data instead of reconstructing an untagged icon");
        helper.assertTrue(displayInput.identity().equals(MaterialProcessSnapshot.itemDisplayState(input)),
                "display identity preserves native state independently of snapshot references");
        var hammer = gregtech.compat.jei.RecipeMachines.collect().get(gregtech.content.data.MachineRecipeMaps.Hammer);
        var hammerIcon = gregtech.compat.common.RecipeViewerIcons.categories().get(gregtech.content.data.MachineRecipeMaps.Hammer);
        helper.assertTrue(hammer != null && !hammer.isEmpty() && hammerIcon != null && !hammerIcon.isEmpty()
                        && gregtech.engine.stack.StackCompat.sameItem(hammer.get(0), hammerIcon),
                "hammer processes reuse the native viewer category icon instead of a partial workstation lookup");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void overviewGroupsVisibleRoutesAndHidesAuxiliaries(GameTestHelper helper) {
        var steps = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Step>();
        for (int variant = 0; variant < 3; variant++) {
            var input = displayPort("selector_" + variant, "Copper", Items.COPPER_INGOT, false);
            var lubricant = displayPort("lubricant_" + variant, "Other", Items.WATER_BUCKET, false);
            var output = displayPort("output_" + variant, "Copper", Items.COPPER_BLOCK, true);
            steps.add(new gregtech.compat.common.MaterialProcessDisplay.Step(
                    new gregtech.api.addon.material.DataKey("test", "cutting_" + variant),
                    net.minecraft.network.chat.Component.literal("cutter"), new ItemStack(Items.STONECUTTER),
                    List.of(input, lubricant), List.of(output), List.of(), java.util.Optional.empty(), true));
        }
        var page = gregtech.compat.common.MaterialProcessDisplay.pages(
                net.minecraft.network.chat.Component.literal("Copper"), "Copper", steps, List.of(),
                List.of(net.minecraft.network.chat.Component.literal("test")),
                gregtech.compat.common.MaterialProcessDisplay.Navigation.NONE).get(0);
        var overview = new gregtech.compat.common.MaterialProcessOverview(page);
        helper.assertTrue(overview.nodes().size() == 3 && overview.edges().size() == 2,
                "visually identical A-machine-B routes merge across native selectors");
        var machine = overview.nodes().stream().filter(node -> node.machine()).findFirst().orElseThrow();
        helper.assertTrue(machine.steps.size() == 3 && machine.extra(),
                "every original recipe survives behind one route and its plus marker");
        helper.assertTrue(overview.edges().stream().allMatch(edge -> edge.endpoints().size() == 3),
                "merged wires retain all original port associations");
        helper.assertTrue(overview.nodes().stream().filter(node -> !node.machine())
                        .noneMatch(node -> node.port().material().equals("Other") || node.copy),
                "auxiliaries stay hidden and identical resources remain shared");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void overviewFoldsSimilarRoutesAcrossNativeRecipeDifferences(GameTestHelper helper) {
        var alternatives = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Step>();
        for (int index = 0; index < 4; index++) {
            var output = layoutPort("bark", true);
            var variableOutput = new gregtech.compat.common.MaterialProcessDisplay.Port(
                    output.resource(), output.material(), output.name(), output.icons(), output.role(), false,
                    output.amount(), net.minecraft.network.chat.Component.literal(Integer.toString(index + 1)),
                    java.util.OptionalInt.of(2500 * (index + 1)), output.details(), output.identity());
            var inputs = new ArrayList<>(List.of(layoutPort("wood_" + index, false),
                    displayPort("lubricant_" + index, "Other", index % 2 == 0 ? Items.WATER_BUCKET : Items.LAVA_BUCKET, false)));
            if (index == 1) inputs.add(layoutPort("extra_wood", false));
            alternatives.add(new gregtech.compat.common.MaterialProcessDisplay.Step(
                    new gregtech.api.addon.material.DataKey("test", "wood_" + index),
                    net.minecraft.network.chat.Component.literal("cutter"), new ItemStack(Items.STONECUTTER), inputs,
                    index == 3 ? List.of(variableOutput, layoutPort("wood_dust", true)) : List.of(variableOutput),
                    List.of(), java.util.Optional.empty(), true));
        }
        var graph = new gregtech.compat.common.MaterialProcessOverview(layoutPage(alternatives, List.of()));
        var machines = graph.nodes().stream().filter(node -> node.machine()).toList();
        helper.assertTrue(machines.size() == 1 && machines.get(0).steps.size() == 4,
                "similar wood routes fold despite different input sets, auxiliaries, yields and probabilities");
        helper.assertTrue(graph.grids().stream().anyMatch(grid -> grid.members.size() == 5)
                        && graph.grids().stream().anyMatch(grid -> grid.members.size() == 2),
                "alternative inputs and variant products remain visible beside the shared machine");
        helper.assertTrue(graph.nodes().stream().filter(node -> !node.machine())
                        .anyMatch(node -> node.port().identity().equals("bark") && node.chanceBadge().equals("…%")),
                "a shared product preserves the different native probabilities");
        for (var step : alternatives) for (boolean output : List.of(false, true)) {
            var ports = output ? step.outputs() : step.inputs();
            for (var port : ports) helper.assertTrue(
                    machines.get(0).endpoints.stream().anyMatch(endpoint -> endpoint.step() == step && endpoint.port() == port)
                            || machines.get(0).auxiliaries.contains(port),
                    "every original port survives visual folding");
        }
        helper.succeed();
    }

    private static gregtech.compat.common.MaterialProcessDisplay.Port displayPort(
            String reference, String material, net.minecraft.world.item.Item item, boolean output) {
        var name = net.minecraft.network.chat.Component.literal(reference);
        var icon = new ItemStack(item);
        return new gregtech.compat.common.MaterialProcessDisplay.Port(
                new gregtech.api.addon.material.DataKey("test", reference), material, name, List.of(icon),
                output ? gregtech.compat.common.MaterialProcessDisplay.Role.UNKNOWN
                        : gregtech.compat.common.MaterialProcessDisplay.Role.CONSUMED,
                false, name, name, java.util.OptionalInt.of(10000), List.of(), MaterialProcessSnapshot.itemDisplayState(icon));
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void overviewIndexesOnlyItsOwnMaterialAndFindsNativeFocus(GameTestHelper helper) {
        var input = displayPort("copper_input", "Copper", Items.COPPER_INGOT, false);
        var output = displayPort("copper_output", "Copper", Items.COPPER_BLOCK, true);
        var auxiliary = displayPort("water", "Other", Items.WATER_BUCKET, false);
        var byproduct = displayPort("gold", "Gold", Items.GOLD_INGOT, true);
        var catalyst = new gregtech.compat.common.MaterialProcessDisplay.Port(
                auxiliary.resource(), auxiliary.material(), auxiliary.name(), auxiliary.icons(),
                gregtech.compat.common.MaterialProcessDisplay.Role.CATALYST, false, auxiliary.amount(),
                auxiliary.quantity(), auxiliary.chance(), auxiliary.details(), auxiliary.identity());
        var step = new gregtech.compat.common.MaterialProcessDisplay.Step(
                new gregtech.api.addon.material.DataKey("test", "indexed"),
                net.minecraft.network.chat.Component.literal("indexed"), new ItemStack(Items.STONECUTTER),
                List.of(input, auxiliary, catalyst), List.of(output, byproduct), List.of(), java.util.Optional.empty(), true);
        var page = layoutPage(List.of(step), List.of());
        helper.assertTrue(page.indexedPorts().equals(List.of(input, output)),
                "foreign reagents, catalysts and byproducts cannot make this material page appear in their lookups");

        var stateful = new ItemStack(Items.COPPER_BLOCK);
        var data = gregtech.content.data.UsbDataMedia.tagOrEmpty(stateful);
        data.putString("process_test", "focused_state");
        gregtech.content.data.UsbDataMedia.store(stateful, data);
        var nativeOutput = new gregtech.compat.common.MaterialProcessDisplay.Port(
                new gregtech.api.addon.material.DataKey("test", "stateful_output"), output.material(), output.name(),
                List.of(stateful), output.role(), false, output.amount(), output.quantity(), output.chance(),
                output.details(), MaterialProcessSnapshot.itemDisplayState(stateful));
        var statefulStep = new gregtech.compat.common.MaterialProcessDisplay.Step(
                new gregtech.api.addon.material.DataKey("test", "native_focus"), step.name(), step.icon(),
                List.of(input), List.of(nativeOutput), List.of(), java.util.Optional.empty(), true);
        var graph = new gregtech.compat.common.MaterialProcessOverview(layoutPage(List.of(step, statefulStep), List.of()));
        var focus = graph.find(port -> port.identity().equals(nativeOutput.identity()));
        helper.assertTrue(focus != null && gregtech.engine.stack.StackCompat.sameItem(focus.port().icons().get(0), stateful),
                "lookup focus selects the requested form and full native state rather than its untagged icon");
        helper.assertTrue(graph.matchingNodes(focus).size() == 1,
                "equal item icons with different native state remain separate hover targets");
        helper.assertTrue(graph.find(port -> port.identity().equals("absent")) == null,
                "unmatched lookups can fall back to the material anchor");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void overviewMatchesItemsAcrossCopiesAndKeepsRecipesSeparate(GameTestHelper helper) {
        var source = new gregtech.compat.common.MaterialProcessDisplay.Step(
                new gregtech.api.addon.material.DataKey("test", "source"),
                net.minecraft.network.chat.Component.literal("source"), new ItemStack(Items.FURNACE),
                List.of(displayPort("external_ore", "Other", Items.RAW_COPPER, false)), List.of(layoutPort("raw", true)),
                List.of(), java.util.Optional.empty(), true);
        var graph = new gregtech.compat.common.MaterialProcessOverview(layoutPage(
                List.of(source, layoutStep("convert", "raw", "ingot"), layoutStep("branch", "raw", "plate")),
                List.of(layoutConnection("source", "convert", "raw"), layoutConnection("source", "branch", "raw"))));
        var raw = graph.find(port -> port.identity().equals("raw"));
        var matching = graph.matchingNodes(raw);
        helper.assertTrue(matching.size() > 1 && matching.stream().allMatch(node -> !node.machine()
                        && node.port().identity().equals("raw")),
                "hover matches the same resource across section copies without highlighting machines or production lines");
        var machine = graph.nodes().stream().filter(node -> node.machine() && node.steps.get(0).reference().path().equals("convert"))
                .findFirst().orElseThrow();
        helper.assertTrue(graph.matchingNodes(machine).size() == 2
                        && graph.matchingNodes(machine).stream().allMatch(node -> node.machine()),
                "matching machine items synchronize without lighting their inputs or outputs");
        helper.assertTrue(graph.matchingNodes(null).isEmpty(), "empty space clears item hover");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void overviewFramesRecipesWithoutEnclosingTheirNeighbors(GameTestHelper helper) {
        var steps = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Step>();
        for (int index = 0; index < 16; index++) steps.add(layoutStep("local_" + index, "raw_" + index, "product_" + index));
        var graph = new gregtech.compat.common.MaterialProcessOverview(layoutPage(steps, List.of()));
        helper.assertTrue(graph.frames().size() == 16, "each compact recipe gets its own light frame");
        for (var frame : graph.frames()) {
            helper.assertTrue(frame.left() < frame.machine().x && frame.right() > frame.machine().x
                            && frame.top() < frame.machine().y && frame.bottom() > frame.machine().y,
                    "the frame encloses its recipe machine");
            for (var node : graph.nodes()) if (node.machine() && node != frame.machine())
                helper.assertFalse(node.x >= frame.left() && node.x < frame.right()
                                && node.y >= frame.top() && node.y < frame.bottom(),
                        "a recipe frame never encloses a neighboring machine");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void overviewAlignsChainsAndPreservesFeedback(GameTestHelper helper) {
        var steps = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Step>();
        var links = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Connection>();
        for (int index = 0; index < 8; index++) {
            steps.add(layoutStep("chain_" + index, "form_" + index, "form_" + (index + 1)));
            if (index > 0) links.add(layoutConnection("chain_" + (index - 1), "chain_" + index, "form_" + index));
        }
        var chain = new gregtech.compat.common.MaterialProcessOverview(layoutPage(steps, links));
        helper.assertTrue(chain.nodes().size() == 17 && chain.nodes().stream().map(node -> node.y).distinct().count() == 1,
                "eight native steps align as one continuous chain");
        var positions = layoutPositions(chain);
        java.util.Collections.reverse(steps);
        java.util.Collections.reverse(links);
        helper.assertTrue(positions.equals(layoutPositions(new gregtech.compat.common.MaterialProcessOverview(layoutPage(steps, links)))),
                "stable references make layout independent of input order");
        var loop = new gregtech.compat.common.MaterialProcessOverview(layoutPage(
                List.of(layoutStep("loop_a", "loop_x", "loop_y"), layoutStep("loop_b", "loop_y", "loop_x"),
                        layoutStep("self", "self_form", "self_form")),
                List.of(layoutConnection("loop_a", "loop_b", "loop_y"), layoutConnection("loop_b", "loop_a", "loop_x"),
                        layoutConnection("self", "self", "self_form"))));
        helper.assertTrue(loop.nodes().stream().filter(node -> node.machine()).count() == 3,
                "pure loops and self loops retain every native step");
        helper.assertTrue(loop.nodes().stream().anyMatch(node -> node.returning)
                        && loop.edges().stream().allMatch(edge -> edge.confirmed()),
                "feedback is marked independently of confirmation");
        for (var edge : loop.edges()) {
            helper.assertTrue(edge.feedback() || edge.to().layer > edge.from().layer,
                    "forward edges obey dependency layers");
            var ports = edge.endpoint().output() ? edge.endpoint().step().outputs() : edge.endpoint().step().inputs();
            helper.assertTrue(ports.get(edge.endpoint().index()) == edge.endpoint().port(),
                    "every visible arrow retains its exact original endpoint");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void overviewSharesHubsWithoutInventingConnections(GameTestHelper helper) {
        var steps = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Step>();
        var links = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Connection>();
        steps.add(layoutStep("producer", "ore", "hub"));
        for (int index = 0; index < 8; index++) {
            steps.add(layoutStep("branch_" + index, "hub", "product_" + index));
            if (index != 7) links.add(layoutConnection("producer", "branch_" + index, "hub"));
        }
        var graph = new gregtech.compat.common.MaterialProcessOverview(layoutPage(steps, links));
        helper.assertTrue(graph.connections().equals(links.stream().sorted(java.util.Comparator.comparing(connection ->
                        connection.source() + ":" + connection.target() + ":" + connection.resource())).toList()),
                "layout retains only the supplied cross-step associations");
        helper.assertTrue(graph.nodes().stream().filter(node -> !node.machine() && node.port().resource().path().equals("hub")).count() == 1
                        && graph.nodes().stream().noneMatch(node -> node.copy),
                "busy resources stay shared instead of scattering copies across branches");
        for (var node : graph.nodes()) if (!node.machine() && node.endpoints.size() == 1) {
            helper.assertTrue(node.endpoints.size() == 1, "each local copy owns its native endpoint");
            var endpoint = node.endpoints.get(0);
            var machine = graph.nodes().stream().filter(candidate -> candidate.machine()
                    && candidate.steps.get(0).reference().equals(endpoint.step().reference())).findFirst().orElseThrow();
            helper.assertTrue(Math.abs(node.layer - machine.layer) == 1,
                    "local copies stay next to their consuming or producing machine");
        }
        var left = layoutStep("left", "left_input", "left_output");
        var right = layoutStep("right", "right_input", "right_output");
        var withAuxiliary = List.of(new gregtech.compat.common.MaterialProcessDisplay.Step(left.reference(), left.name(), left.icon(),
                left.inputs(), List.of(left.outputs().get(0), displayPort("water", "Other", Items.WATER_BUCKET, true)),
                List.of(), java.util.Optional.empty(), true),
                new gregtech.compat.common.MaterialProcessDisplay.Step(right.reference(), right.name(), right.icon(),
                        List.of(right.inputs().get(0), displayPort("water", "Other", Items.WATER_BUCKET, false)), right.outputs(),
                        List.of(), java.util.Optional.empty(), true));
        var noLink = new gregtech.compat.common.MaterialProcessOverview(layoutPage(withAuxiliary, List.of()));
        var auxiliaryLink = new gregtech.compat.common.MaterialProcessOverview(layoutPage(withAuxiliary,
                List.of(layoutConnection("left", "right", "water"))));
        helper.assertTrue(layoutPositions(noLink).equals(layoutPositions(auxiliaryLink)),
                "hidden auxiliary ports do not add layout dependencies or resources");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void overviewKeepsDenseEdgesLocal(GameTestHelper helper) {
        var random = new java.util.Random(42);
        var steps = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Step>();
        for (int index = 0; index < 96; index++)
            steps.add(layoutStep("dense_" + index, "form_" + random.nextInt(40), "form_" + random.nextInt(40)));
        var graph = new gregtech.compat.common.MaterialProcessOverview(layoutPage(steps, List.of()));
        helper.assertTrue(
                graph.nodes().stream()
                                .filter(node -> node.machine())
                                .mapToInt(node -> node.steps.size())
                                .sum()
                        == steps.size(),
                "local copies retain all native steps");
        helper.assertTrue(graph.nodes().stream().anyMatch(node -> node.copy && node.returning),
                "dense feedback uses marked local resources");
        helper.assertTrue(graph.nodes().stream().mapToInt(node -> node.y).min().orElseThrow() == 52,
                "feedback does not reserve empty lanes above the graph");
        for (var edge : graph.edges()) {
            helper.assertTrue(edge.path().size() == 2 && edge.to().layer > edge.from().layer,
                    "every visible connection is a single forward segment");
            for (var endpoint : edge.endpoints())
                helper.assertTrue(
                        (endpoint.output() ? endpoint.step().outputs() : endpoint.step().inputs())
                                        .get(endpoint.index())
                                == endpoint.port(),
                        "copied and shared connections retain exact native ports");
            var from = edge.path().get(0);
            var to = edge.path().get(1);
            helper.assertTrue(to.x() > from.x(), "visible routes read from left to right");
            double slope = (double) (to.y() - from.y()) / (to.x() - from.x());
            for (var node : graph.nodes()) {
                if (node == edge.from() || node == edge.to() || node.x + 12 <= from.x() || node.x - 12 >= to.x()) continue;
                double leftY = from.y() + (Math.max(node.x - 12, from.x()) - from.x()) * slope;
                double rightY = from.y() + (Math.min(node.x + 12, to.x()) - from.x()) * slope;
                helper.assertTrue(
                        Math.max(leftY, rightY) < node.y - 14 || Math.min(leftY, rightY) > node.y + 14,
                        "direct connections avoid intermediate icons and markers");
            }
        }
        java.util.Collections.shuffle(steps, random);
        helper.assertTrue(
                layoutPositions(graph)
                        .equals(layoutPositions(new gregtech.compat.common.MaterialProcessOverview(
                                layoutPage(steps, List.of())))),
                "dense cyclic layouts are stable when input order changes");
        helper.assertTrue(graph.connections().isEmpty(), "local copies do not invent logical associations");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void overviewPacksLocalRoutesWithoutInvisibleDependencies(GameTestHelper helper) {
        var random = new java.util.Random(42);
        var steps = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Step>();
        var links = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Connection>();
        for (int index = 0; index < 100; index++)
            steps.add(layoutStep("packed_" + index, "form_" + random.nextInt(40), "form_" + random.nextInt(40)));
        for (var source : steps) for (var target : steps)
            if (source.outputs().get(0).resource().equals(target.inputs().get(0).resource()))
                links.add(new gregtech.compat.common.MaterialProcessDisplay.Connection(source.reference(), target.reference(),
                        source.outputs().get(0).resource(), true));
        var graph = new gregtech.compat.common.MaterialProcessOverview(layoutPage(steps, links));
        var local = new gregtech.compat.common.MaterialProcessOverview(layoutPage(steps, List.of()));
        helper.assertTrue(layoutPositions(graph).equals(layoutPositions(local)) && graph.connections().size() == links.size(),
                "retained native associations do not stretch copies into invisible dependency columns");
        helper.assertTrue(graph.width() < 1000 && graph.height() < 1000,
                "dense local recipe blocks use a compact canvas");
        var islands = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Step>();
        for (int index = 0; index < 24; index++) islands.add(layoutStep("island_" + index, "input_" + index, "output_" + index));
        var packed = new gregtech.compat.common.MaterialProcessOverview(layoutPage(islands, List.of()));
        helper.assertTrue(packed.width() < 600 && packed.height() < 600
                        && packed.nodes().stream().filter(node -> node.machine()).count() == 24,
                "independent routes fill adjacent shelves while retaining every route");
        for (var first : graph.nodes()) for (var second : graph.nodes())
            if (first != second) helper.assertTrue(Math.abs(first.x - second.x) >= 24 || Math.abs(first.y - second.y) >= 24,
                    "packing keeps resource and machine boxes separate");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void overviewKeepsFirstOutputAndVariantsTogether(GameTestHelper helper) {
        var base = layoutStep("lens", "gem", "lens");
        var steps = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Step>();
        for (int index = 0; index < 2; index++)
            steps.add(new gregtech.compat.common.MaterialProcessDisplay.Step(
                    new gregtech.api.addon.material.DataKey("test", "lens_" + index), base.name(), base.icon(),
                    base.inputs(), List.of(base.outputs().get(0), layoutPort("a_dust_" + index, true)),
                    List.of(), java.util.Optional.empty(), true));
        for (int index = 0; index < 12; index++)
            steps.add(layoutStep("other_" + index, "gem", "other_output_" + index));
        var graph = new gregtech.compat.common.MaterialProcessOverview(layoutPage(steps, List.of()));
        var machine = graph.nodes().stream().filter(node -> node.machine() && node.steps.size() == 2)
                .findFirst().orElseThrow();
        var outputs = graph.edges().stream().filter(edge -> edge.from() == machine).toList();
        helper.assertTrue(outputs.size() == 3, "variants share a machine while retaining their distinct outputs");
        var grid = graph.grids().stream().filter(value -> value.members.contains(outputs.get(0).to())).findFirst().orElseThrow();
        helper.assertTrue(grid.members.size() == 3 && grid.members.stream().allMatch(node -> node.y == machine.y),
                "variant products occupy adjacent cells inside their recipe block");
        helper.assertTrue(outputs.stream().map(edge -> edge.path()).distinct().count() == 1,
                "one output arrow connects the machine to the whole product grid");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void recoveryOwnershipAndMachineKindDriveOverviewClassification(GameTestHelper helper) {
        var foreign = RecoveryRecipePublisher.generic(row(new ItemStack[] {new ItemStack(Items.IRON_INGOT)},
                Items.COPPER_INGOT), "test-recycling");
        var local = RecoveryRecipePublisher.generic(row(new ItemStack[] {new ItemStack(Items.COPPER_INGOT)},
                Items.IRON_INGOT), "test-recycling");
        var ordinary = row(new ItemStack[] {new ItemStack(Items.GOLD_INGOT)}, Items.COPPER_INGOT);
        for (var recoveryMap : List.of(gregtech.content.data.MachineRecipeMaps.Shredder,
                gregtech.content.data.MachineRecipeMaps.Melter, gregtech.content.data.MachineRecipeMaps.Smelter))
            helper.assertTrue(new MaterialProcessSnapshot.NativeRecipe(recoveryMap, local, 0).genericRecovery(),
                    "generic grinding and melting routes are recovery");
        helper.assertTrue(!new MaterialProcessSnapshot.NativeRecipe(gregtech.content.data.MachineRecipeMaps.Anvil, local, 0).genericRecovery()
                        && !new MaterialProcessSnapshot.NativeRecipe(gregtech.content.data.MachineRecipeMaps.Shredder, ordinary, 0).genericRecovery(),
                "anvil fallback ownership and ordinary shredder recipes do not imply recovery");
        var map = new RecipeMap(new ArrayList<>(List.of(foreign, local, ordinary)),
                "gt.test.process.recovery", "Test", "textures/gui/test", 9, 9, 0, 0, 0, 0, 0,
                false, false, false, false);
        RecipeMap.unregister(map);
        var snapshot = MaterialProcessSnapshot.build(MaterialPublication.current(), 1,
                new MaterialWorldgenFacts.Snapshot(List.of()), List.of(map));
        var root = snapshot.roots().stream().filter(value -> value.material().equals("Copper")).findFirst().orElseThrow();
        var query = snapshot.queryMaterial(root.resource(), new Budgets(0, 8, 16, 16));
        helper.assertTrue(query.steps().size() == 3,
                "fallback ownership on an ordinary process does not hide its material sources");
        var page = gregtech.compat.common.MaterialProcessProjection.pages(snapshot, root.resource(),
                gregtech.compat.common.MaterialProcessDisplay.Navigation.NONE).get(0);
        helper.assertTrue(page.steps().stream().noneMatch(value -> value.genericRecovery()),
                "projection and source indexing use the same recovery classification");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void overviewSeparatesSourcesConversionsAndUses(GameTestHelper helper) {
        var source = new gregtech.compat.common.MaterialProcessDisplay.Step(
                new gregtech.api.addon.material.DataKey("test", "source"),
                net.minecraft.network.chat.Component.literal("source"), new ItemStack(Items.STONECUTTER),
                List.of(displayPort("foreign", "Other", Items.IRON_INGOT, false)), List.of(layoutPort("ore", true)),
                List.of(), java.util.Optional.empty(), true);
        var use = new gregtech.compat.common.MaterialProcessDisplay.Step(
                new gregtech.api.addon.material.DataKey("test", "use"),
                net.minecraft.network.chat.Component.literal("use"), new ItemStack(Items.STONECUTTER),
                List.of(layoutPort("dust", false)), List.of(displayPort("foreign_product", "Other", Items.GOLD_INGOT, true)),
                List.of(), java.util.Optional.empty(), true);
        var uncertain = layoutConnection("source", "convert", "ore");
        var links = List.of(new gregtech.compat.common.MaterialProcessDisplay.Connection(
                uncertain.source(), uncertain.target(), uncertain.resource(), false),
                layoutConnection("convert", "use", "dust"));
        var graph = new gregtech.compat.common.MaterialProcessOverview(
                layoutPage(List.of(use, layoutStep("convert", "ore", "dust"), source), links));
        helper.assertTrue(graph.bands().size() == 3 && graph.connections().size() == 2,
                "one overview preserves all associations and only allocates populated bands");
        helper.assertTrue(graph.edges().stream().filter(edge -> !edge.endpoint().output()
                        && edge.endpoint().step().reference().equals(uncertain.target())).noneMatch(edge -> edge.confirmed()),
                "cross-band associations retain their uncertainty after local layout");
        for (var node : graph.nodes()) if (node.machine()) {
            var band = graph.bands().stream().filter(value -> value.section() == node.section).findFirst().orElseThrow();
            helper.assertTrue(node.x >= band.left() && node.x < band.right(),
                    "machines stay in their semantic source, conversion or use band");
        }
        helper.assertTrue(graph.nodes().stream().filter(node -> !node.machine()
                        && (node.port().identity().equals("ore") || node.port().identity().equals("dust")))
                        .allMatch(node -> node.copy),
                "boundary material instances carry copy markers instead of crossing separators with long wires");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void overviewFoldsAlternativeInputsAndKeepsProductGridsLocal(GameTestHelper helper) {
        var alternatives = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Step>();
        for (int index = 0; index < 13; index++) alternatives.add(layoutStep("smelt_" + index, "form_" + index, "molten"));
        var graph = new gregtech.compat.common.MaterialProcessOverview(layoutPage(alternatives, List.of()));
        var grid = graph.grids().get(0);
        helper.assertTrue(grid.members.size() == 13 && grid.right() - grid.left() == 72
                        && grid.bottom() - grid.top() == 120,
                "thirteen alternative inputs occupy a three-column, five-row inventory");
        helper.assertTrue(graph.nodes().stream().filter(node -> node.machine()).count() == 1
                        && graph.nodes().stream().filter(node -> node.machine()).findFirst().orElseThrow().steps.size() == 13,
                "alternative forms share one machine while retaining all native recipes");
        helper.assertTrue(graph.edges().stream().filter(edge -> !edge.endpoint().output()).map(edge -> edge.path()).distinct().count() == 1,
                "all input cells connect through one trunk");
        var yields = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Step>();
        for (int count : List.of(2, 4)) {
            var nativeStep = layoutStep("yield_" + count, "form_" + count, "molten");
            var nativePort = nativeStep.outputs().get(0);
            var port = new gregtech.compat.common.MaterialProcessDisplay.Port(nativePort.resource(), nativePort.material(), nativePort.name(),
                    nativePort.icons(), nativePort.role(), false, net.minecraft.network.chat.Component.literal(count + " items"),
                    net.minecraft.network.chat.Component.literal(Integer.toString(count)), nativePort.chance(), nativePort.details(), nativePort.identity());
            yields.add(new gregtech.compat.common.MaterialProcessDisplay.Step(nativeStep.reference(), nativeStep.name(), nativeStep.icon(),
                    nativeStep.inputs(), List.of(port), List.of(), java.util.Optional.empty(), true));
        }
        var yieldGraph = new gregtech.compat.common.MaterialProcessOverview(layoutPage(yields, List.of()));
        helper.assertTrue(yieldGraph.grids().size() == 1 && yieldGraph.nodes().stream().anyMatch(node -> !node.machine() && node.quantityBadge().equals("2/4")),
                "different batch yields do not prevent form folding or overwrite each other's quantities");
        var positions = layoutPositions(graph);
        java.util.Collections.reverse(alternatives);
        helper.assertTrue(positions.equals(layoutPositions(new gregtech.compat.common.MaterialProcessOverview(layoutPage(alternatives, List.of())))),
                "folded grids do not depend on snapshot order");
        var base = layoutStep("bolt", "bolt", "small_dust");
        var bolt = new gregtech.compat.common.MaterialProcessDisplay.Step(base.reference(), base.name(), base.icon(), base.inputs(),
                List.of(base.outputs().get(0), layoutPort("tiny_dust", true)), List.of(), java.util.Optional.empty(), true);
        var products = new gregtech.compat.common.MaterialProcessOverview(layoutPage(
                List.of(bolt, layoutStep("small_source", "ore", "small_dust"), layoutStep("tiny_use", "tiny_dust", "ingot")), List.of()));
        helper.assertTrue(products.grids().stream().anyMatch(value -> value.members.size() == 2
                        && value.members.stream().map(node -> node.port().identity()).collect(java.util.stream.Collectors.toSet())
                                .equals(java.util.Set.of("small_dust", "tiny_dust"))),
                "a bolt's two products stay adjacent even when one has other producers and the other has consumers");
        var manyOutputs = new ArrayList<gregtech.compat.common.MaterialProcessDisplay.Port>();
        for (int index = 0; index < 128; index++) manyOutputs.add(layoutPort("many_" + index, true));
        var many = new gregtech.compat.common.MaterialProcessDisplay.Step(base.reference(), base.name(), base.icon(), base.inputs(),
                manyOutputs, List.of(), java.util.Optional.empty(), true);
        var large = new gregtech.compat.common.MaterialProcessOverview(layoutPage(
                List.of(many, layoutStep("consume", "many_0", "result")), List.of()));
        helper.assertTrue(large.grids().stream().anyMatch(value -> value.members.size() == 128),
                "long-edge detection uses the grid anchor instead of detaching a tall grid's first cell");
        for (var edge : products.edges()) for (var endpoint : edge.endpoints())
            helper.assertTrue((endpoint.output() ? endpoint.step().outputs() : endpoint.step().inputs()).get(endpoint.index()) == endpoint.port(),
                    "grid cells and shared trunks retain exact native endpoint ownership");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void overviewShowsPortYieldsAndOrdersRecoveryAfterConversions(GameTestHelper helper) {
        var base = layoutPort("dust", true);
        var output = new gregtech.compat.common.MaterialProcessDisplay.Port(base.resource(), base.material(), base.name(), base.icons(), base.role(),
                base.main(), net.minecraft.network.chat.Component.literal("2 items"), net.minecraft.network.chat.Component.literal("2"),
                java.util.OptionalInt.of(1111), base.details(), base.identity());
        var step = layoutStep("grind", "bolt", "dust");
        var recovery = new gregtech.compat.common.MaterialProcessDisplay.Step(step.reference(), step.name(), step.icon(), step.inputs(),
                List.of(output), List.of(), java.util.Optional.empty(), true, false, true);
        var graph = new gregtech.compat.common.MaterialProcessOverview(layoutPage(List.of(recovery), List.of()));
        var product = graph.nodes().stream().filter(node -> !node.machine() && node.endpoints.stream().anyMatch(endpoint -> endpoint.output()))
                .findFirst().orElseThrow();
        helper.assertTrue(product.quantityBadge().equals("2") && product.chanceBadge().equals("11.11%"),
                "quantity and basis-point probability are rendered without reconstructing them from icon counts");
        helper.assertTrue(graph.bands().stream().map(value -> value.section()).toList().equals(List.of(
                        gregtech.compat.common.MaterialProcessDisplay.Section.RECOVERY)),
                "a recovery-only page does not reserve space for three empty bands");
        helper.assertTrue(product.returning, "recovery products retain their return marker");
        helper.succeed();
    }

    private static gregtech.compat.common.MaterialProcessDisplay.Step layoutStep(String id, String input, String output) {
        return new gregtech.compat.common.MaterialProcessDisplay.Step(new gregtech.api.addon.material.DataKey("test", id),
                net.minecraft.network.chat.Component.literal(id), new ItemStack(Items.STONECUTTER),
                List.of(layoutPort(input, false)),
                List.of(layoutPort(output, true)), List.of(), java.util.Optional.empty(), true);
    }

    private static gregtech.compat.common.MaterialProcessDisplay.Port layoutPort(String reference, boolean output) {
        var name = net.minecraft.network.chat.Component.literal(reference);
        return new gregtech.compat.common.MaterialProcessDisplay.Port(new gregtech.api.addon.material.DataKey("test", reference),
                "Copper", name, List.of(), output ? gregtech.compat.common.MaterialProcessDisplay.Role.UNKNOWN
                        : gregtech.compat.common.MaterialProcessDisplay.Role.CONSUMED,
                false, name, name, java.util.OptionalInt.of(10000), List.of(), reference);
    }

    private static gregtech.compat.common.MaterialProcessDisplay.Connection layoutConnection(String source, String target, String resource) {
        return new gregtech.compat.common.MaterialProcessDisplay.Connection(new gregtech.api.addon.material.DataKey("test", source),
                new gregtech.api.addon.material.DataKey("test", target), new gregtech.api.addon.material.DataKey("test", resource), true);
    }

    private static gregtech.compat.common.MaterialProcessDisplay.Page layoutPage(
            List<gregtech.compat.common.MaterialProcessDisplay.Step> steps,
            List<gregtech.compat.common.MaterialProcessDisplay.Connection> connections) {
        return gregtech.compat.common.MaterialProcessDisplay.pages(net.minecraft.network.chat.Component.literal("Copper"), "Copper",
                steps, connections, List.of(net.minecraft.network.chat.Component.literal("test")),
                gregtech.compat.common.MaterialProcessDisplay.Navigation.NONE).get(0);
    }

    private static java.util.Map<String, String> layoutPositions(gregtech.compat.common.MaterialProcessOverview graph) {
        var result = new java.util.TreeMap<String, String>();
        for (var node : graph.nodes()) result.put(node.key, node.x + ":" + node.y);
        return result;
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void nativeProjectionKeepsOutputsAndCrossMaterialRoutes(GameTestHelper helper) {
        var first = row(new ItemStack[] {new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.PAPER),
                new ItemStack(Items.STICK)}, Items.COPPER_INGOT).withCatalystInputs(2);
        var second = row(new ItemStack[] {new ItemStack(Items.COPPER_INGOT)}, Items.GOLD_INGOT);
        var third = row(new ItemStack[] {new ItemStack(Items.GOLD_INGOT)}, Items.DIAMOND);
        var tagged = new Recipe(new ItemStack[] {new ItemStack(Items.GOLD_INGOT)},
                new ItemStack[] {new ItemStack(Items.IRON_INGOT)}, null, null,
                new FluidStack[] {new FluidStack(Fluids.WATER, 1000)},
                new FluidStack[] {new FluidStack(Fluids.LAVA, 1000)}, 20, 16, 0)
                .withItemTagInput(0, ResourceLocation.fromNamespaceAndPath("c", "ingots/gold"))
                .withFluidTagInput(0, ResourceLocation.fromNamespaceAndPath("c", "water"));
        var map = new RecipeMap(new ArrayList<>(List.of(first, second, third, tagged)),
                "gt.test.process.snapshot", "Test", "textures/gui/test", 9, 9, 0, 0, 0, 0, 0,
                false, false, false, false);
        RecipeMap.unregister(map);
        var ores = List.of(new Ore(Materials.Iron, "item:minecraft:iron_nugget", 0, 20, 1, 0, List.of()));
        var facts = new MaterialWorldgenFacts.Snapshot(List.of(
                new Fact("test", "test", Kind.LARGE_VEIN, "minecraft:overworld", 0, 20, 1, 1, ores)));
        var snapshot = MaterialProcessSnapshot.build(MaterialPublication.current(), 1, facts, List.of(map));
        var root = snapshot.roots().stream().filter(value -> value.material().equals("Copper")).findFirst().orElseThrow();
        var query = snapshot.query(root.resource(), Direction.BOTH, Budgets.DEFAULT);
        helper.assertTrue(query.steps().size() == 4, "route traverses other materials and the native worldgen form");
        var firstStep = query.steps().stream().filter(step -> snapshot.recipe(step.reference())
                .map(nativeRow -> nativeRow.recipe() == first).orElse(false)).findFirst().orElseThrow();
        helper.assertTrue(firstStep.inputs().size() == 2, "ordinary co-input survives; auxiliary tool stays in details");
        helper.assertTrue(firstStep.inputs().stream().anyMatch(port -> port.resource().state().equals("item:minecraft:paper")),
                "unclassified ordinary input keeps its native identity");
        helper.assertTrue(firstStep.details().stream().anyMatch(detail -> detail.contains("minecraft:stick")),
                "auxiliary catalyst is available in recipe details");
        var taggedStep = snapshot.roots().stream()
                .flatMap(value -> snapshot.query(value.resource(), Direction.BOTH, Budgets.DEFAULT).steps().stream())
                .filter(step -> snapshot.recipe(step.reference())
                .map(nativeRow -> nativeRow.recipe() == tagged).orElse(false)).findFirst().orElseThrow();
        helper.assertTrue(taggedStep.outputs().get(0).resource().state().equals("item:minecraft:iron_ingot"),
                "input tag never changes the output identity");
        helper.assertTrue(taggedStep.outputs().stream().anyMatch(port -> port.resource().state().equals("fluid:minecraft:lava")),
                "fluid input tag never changes the fluid output identity");
        var replacement = MaterialProcessSnapshot.build(MaterialPublication.current(), 1, facts, List.of(map));
        helper.assertTrue(replacement.query(root.resource(), Direction.BOTH, Budgets.DEFAULT).steps().isEmpty(),
                "resource references cannot cross snapshots even with the same stamp");
        helper.assertTrue(snapshot.query(root.resource(), Direction.BOTH, Budgets.DEFAULT).equals(query),
                "old query remains self-consistent");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void worldgenRevisionIncludesSelectionAndKind(GameTestHelper helper) {
        var ores = List.of(new Ore(Materials.Iron, "item:minecraft:iron_nugget", 0, 20, 1, 0, List.of()));
        var baseline = new Fact("test", "test", Kind.LARGE_VEIN, "minecraft:overworld", 0, 20, 1, 10, ores);
        long revision = new MaterialWorldgenFacts.Snapshot(List.of(baseline)).revision();
        for (Fact changed : List.of(
                new Fact("test", "test", Kind.LARGE_VEIN, "minecraft:overworld", 0, 20, 2, 10, ores),
                new Fact("test", "test", Kind.LARGE_VEIN, "minecraft:overworld", 0, 20, 1, 20, ores),
                new Fact("test", "test", Kind.BEDROCK, "minecraft:overworld", 0, 20, 1, 10, ores),
                new Fact("test", "renamed", Kind.LARGE_VEIN, "minecraft:overworld", 0, 20, 1, 10, ores))) {
            helper.assertTrue(new MaterialWorldgenFacts.Snapshot(List.of(changed)).revision() != revision,
                    "each changed worldgen field changes the revision");
        }
        var activeFacts = MaterialWorldgenFacts.capture();
        helper.assertFalse(activeFacts.facts().isEmpty(), "active worldgen facts are projected");
        helper.assertFalse(activeFacts.facts().stream().flatMap(fact -> fact.ores().stream())
                .anyMatch(ore -> ore.itemState().equals("item:minecraft:stone")
                        || ore.itemState().equals("item:minecraft:air")),
                "missing ore registrations never turn into stone or air sources");
        var current = MaterialProcessSnapshot.Store.publish(MachineRecipeOverlay.lastPublication(),
                MachineRecipeOverlay.generation());
        helper.assertTrue(current.samePublication(MachineRecipeOverlay.lastPublication()),
                "snapshot follows the last successful publication");
        helper.assertTrue(MaterialProcessSnapshot.Store.publish(MachineRecipeOverlay.lastPublication(),
                MachineRecipeOverlay.generation()) == current, "unchanged publication reuses its index");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "material_process", timeoutTicks = 200)
    public static void specialProcessRulesDriveTagsAndRecipeOwnership(GameTestHelper helper) {
        var catalog = MaterialViews.catalog();
        var gold = new gregtech.api.addon.material.MaterialKey("Gold");
        var aquaRegia = new gregtech.api.addon.material.MaterialKey("AquaRegia");
        helper.assertTrue(
                catalog.specialProcesses()
                        .find(SpecialProcessMethods.AQUA_REGIA_LEACHING.id())
                        .isPresent(),
                "aqua regia method is published");
        helper.assertTrue(
                catalog.tags().contains(gold, new gregtech.api.addon.material.DataKey(
                        "gt6ce", "special_process/aqua_regia_leaching/target")),
                "target tag is derived from the target rule");
        helper.assertTrue(
                catalog.tags().contains(aquaRegia, new gregtech.api.addon.material.DataKey(
                        "gt6ce", "special_process/aqua_regia_leaching/reagent")),
                "reagent tag is derived from the reagent rule");
        var method = catalog.specialProcesses()
                .find(SpecialProcessMethods.AQUA_REGIA_LEACHING.id())
                .orElseThrow();
        var targetItemTag = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(
                        method.targetTag().namespace(), method.targetTag().path()));
        var targetItems = BuiltInRegistries.ITEM.getTag(targetItemTag).orElseThrow();
        helper.assertFalse(targetItems.size() == 0, "target item resource tag has members");
        for (var holder : targetItems) {
            var form = MaterialEquivalence.form(new ItemStack(holder.value()));
            helper.assertTrue(
                    form != null
                            && (form.prefix().getName().equals("crushedPurified")
                                    || form.prefix().getName().equals("crushedPurifiedTiny")),
                    "target item tag only contains declared purified forms");
        }
        var reagentFluidTag = TagKey.create(
                Registries.FLUID,
                ResourceLocation.fromNamespaceAndPath(
                        method.reagentTag().namespace(), method.reagentTag().path()));
        var reagentFluids = BuiltInRegistries.FLUID.getTag(reagentFluidTag).orElseThrow();
        helper.assertFalse(reagentFluids.size() == 0, "reagent fluid resource tag has members");
        for (var holder : reagentFluids) {
            var entry = GTFluids.entryForFluid(holder.value());
            helper.assertTrue(entry != null, "reagent tag member has a fluid entry");
            if (entry == null) continue;
            var material = gregtech.engine.fluid.FluidVisualPolicy.material(entry).resolve();
            helper.assertTrue(
                    material != null
                            && material.isValid()
                            && material.getName().equals("AquaRegia")
                            && !entry.gas(),
                    "reagent fluid tag only contains declared aqua regia liquid");
        }
        var bath = RecipeMap.RECIPE_MAP_LIST.stream()
                .filter(map -> map.mNameInternal.equals("gt.recipe.bath"))
                .findFirst()
                .orElseThrow();
        helper.assertTrue(
                bath.mRecipeList.stream()
                        .anyMatch(recipe -> SpecialProcessMethods.AQUA_REGIA_LEACHING
                                .id()
                                .equals(recipe.specialProcess())),
                "at least one Bath row is owned by the API method");
        var snapshot = MaterialProcessSnapshot.Store.current();
        var root = snapshot.roots().stream()
                .filter(value -> value.material().equals("Gold"))
                .findFirst()
                .orElseThrow();
        var query = snapshot.queryMaterial(root.resource(), new Budgets(0, 512, 128, 4096));
        helper.assertTrue(
                query.steps().stream().anyMatch(step -> step.specialProcess()
                        .filter(id -> id.equals(SpecialProcessMethods.AQUA_REGIA_LEACHING.id()))
                        .isPresent()),
                "published aqua regia rows retain method ownership");
        helper.succeed();
    }

    private static Recipe row(ItemStack[] inputs, net.minecraft.world.item.Item output) {
        return new Recipe(inputs, new ItemStack[] {new ItemStack(output)}, null, null, null, null, 20, 16, 0);
    }
}
