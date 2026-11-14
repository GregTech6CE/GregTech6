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
        helper.assertTrue(graph.nodes().stream().mapToInt(node -> node.y).min().orElseThrow() == 24,
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
            double slope = (double) (to.y() - from.y()) / (to.x() - from.x());
            for (var node : graph.nodes()) {
                if (node.layer <= edge.from().layer || node.layer >= edge.to().layer) continue;
                double leftY = from.y() + (node.x - 12 - from.x()) * slope;
                double rightY = from.y() + (node.x + 12 - from.x()) * slope;
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
