package gregtech.engine.material;

import gregtech.api.addon.material.DataKey;
import gregtech.engine.material.MaterialProcessGraph.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

final class MaterialProcessGraphTest {
    private static final Snapshot SNAPSHOT = new Snapshot(new MaterialPublication.Stamp(4, 1, 2, 3), 5, 6, 7, 8);

    @Test
    void traversalKeepsCoInputsAndAllOutputsForACycle() {
        DataKey ore = resource("ore");
        DataKey dust = resource("dust");
        DataKey ingot = resource("ingot");
        DataKey fluid = resource("fluid");
        var graph = MaterialProcessGraph.builder(SNAPSHOT)
                .notice("partial_dynamic", 0)
                .add(step(
                        "wash",
                        List.of(port(ore, Role.CONSUMED, "ore"), port(fluid, Role.CATALYST, "fluid")),
                        List.of(port(dust, Role.UNKNOWN, "dust"), port(ingot, Role.UNKNOWN, "ingot"))))
                .add(step(
                        "melt",
                        List.of(port(dust, Role.CONSUMED, "dust"), port(ingot, Role.CONSUMED, "ingot")),
                        List.of(port(ore, Role.UNKNOWN, "ore"))))
                .build();

        var result = graph.query(ore, Direction.BOTH, Budgets.DEFAULT);

        assertEquals(2, result.steps().size());
        assertEquals(3, result.edges().size());
        assertTrue(result.edges().stream().allMatch(MaterialProcessGraphTest::edgeIsConfirmed));
        assertFalse(result.truncated());
        assertTrue(result.notices().stream().anyMatch(notice -> notice.code().equals("partial_dynamic")));
    }

    @Test
    void everyTraversalBudgetReportsItsOwnTruncation() {
        var builder = MaterialProcessGraph.builder(SNAPSHOT);
        DataKey previous = resource("r0");
        for (int index = 0; index < 8; index++) {
            DataKey next = resource("r" + (index + 1));
            builder.add(step(
                    "s" + index,
                    List.of(port(previous, Role.CONSUMED, "dust")),
                    List.of(port(next, Role.UNKNOWN, "dust"))));
            previous = next;
        }
        var graph = builder.build();

        var depth = graph.query(resource("r0"), Direction.USES, new Budgets(1, 64, 64, 64));
        var nodes = graph.query(resource("r0"), Direction.USES, new Budgets(20, 3, 64, 64));
        var fan = MaterialProcessGraph.builder(SNAPSHOT)
                .add(step(
                        "fan",
                        List.of(port(resource("source"), Role.CONSUMED, "source")),
                        List.of(
                                port(resource("one"), Role.UNKNOWN, "one"),
                                port(resource("two"), Role.UNKNOWN, "two"))))
                .build();
        var ports = fan.query(resource("one"), Direction.BOTH, new Budgets(20, 64, 1, 64));
        var edges = graph.query(resource("r0"), Direction.USES, new Budgets(20, 64, 64, 0));

        assertEquals(2, depth.steps().size());
        assertTrue(depth.truncated());
        assertTrue(depth.notices().stream().anyMatch(notice -> notice.code().equals("depth_limit")));
        assertTrue(nodes.truncated());
        assertTrue(nodes.notices().stream().anyMatch(notice -> notice.code().equals("node_limit")));
        assertEquals(1, ports.steps().get(0).outputs().size());
        assertTrue(ports.truncated());
        assertTrue(ports.notices().stream().anyMatch(notice -> notice.code().equals("port_limit")));
        assertTrue(edges.truncated());
        assertTrue(edges.notices().stream().anyMatch(notice -> notice.code().equals("edge_limit")));
    }

    @Test
    void selectorExpansionStaysOutOfTheGraph() {
        DataKey ore = new DataKey("gt6ce", "resource/ore");
        DataKey unit = new DataKey("gt6ce", "resource/unit");
        var graph = MaterialProcessGraph.builder(SNAPSHOT)
                .add(step(
                        "unit",
                        List.of(port(unit, Role.CONSUMED, "unit:gt6ce:iron_ingot")),
                        List.of(port(ore, Role.UNKNOWN, "concrete"))))
                .build();

        var result = graph.query(unit, Direction.USES, Budgets.DEFAULT);

        assertEquals(1, result.steps().size());
        assertTrue(result.steps().get(0).inputs().get(0).resource().state().startsWith("unit:"));
        assertTrue(result.steps().get(0).inputs().get(0).amount().unit() == AmountUnit.MATERIAL_UNIT);
    }

    @Test
    void forwardAndReverseQueriesUseTheSamePortIndex() {
        DataKey dust = resource("dust");
        DataKey ingot = resource("ingot");
        var graph = MaterialProcessGraph.builder(SNAPSHOT)
                .add(step(
                        "cast",
                        List.of(port(dust, Role.CONSUMED, "dust")),
                        List.of(port(ingot, Role.UNKNOWN, "ingot"))))
                .add(step(
                        "melt",
                        List.of(port(ingot, Role.CONSUMED, "ingot")),
                        List.of(port(dust, Role.UNKNOWN, "dust"))))
                .build();

        var immediate = new Budgets(0, 64, 64, 128);
        var sources = graph.query(dust, Direction.SOURCES, immediate);
        var uses = graph.query(dust, Direction.USES, immediate);

        assertEquals(1, sources.steps().size());
        assertEquals("melt", sources.steps().get(0).name());
        assertEquals(1, uses.steps().size());
        assertEquals("cast", uses.steps().get(0).name());
    }

    @Test
    void materialQueryKeepsEveryDirectFormAndStopsAtAnotherMaterial() {
        DataKey ore = new DataKey("gt6ce", "resource/iron/ore");
        DataKey crushed = new DataKey("gt6ce", "resource/iron/crushed");
        DataKey dust = new DataKey("gt6ce", "resource/iron/dust");
        DataKey sourceMaterial = new DataKey("gt6ce", "resource/copper/ore");
        DataKey productMaterial = new DataKey("gt6ce", "resource/copper/ingot");
        DataKey farther = new DataKey("gt6ce", "resource/gold/ingot");
        var graph = MaterialProcessGraph.builder(SNAPSHOT)
                .add(step(
                        "source",
                        List.of(port(sourceMaterial, Role.CONSUMED, "copper ore", "Copper")),
                        List.of(port(ore, Role.UNKNOWN, "iron ore", "Iron"))))
                .add(step(
                        "crush",
                        List.of(port(ore, Role.CONSUMED, "iron ore", "Iron")),
                        List.of(port(crushed, Role.UNKNOWN, "iron crushed", "Iron"))))
                .add(step(
                        "grind",
                        List.of(port(crushed, Role.CONSUMED, "iron crushed", "Iron")),
                        List.of(port(dust, Role.UNKNOWN, "iron dust", "Iron"))))
                .add(step(
                        "use",
                        List.of(port(dust, Role.CONSUMED, "iron dust", "Iron")),
                        List.of(port(productMaterial, Role.UNKNOWN, "copper ingot", "Copper"))))
                .add(step(
                        "transitive",
                        List.of(port(productMaterial, Role.CONSUMED, "copper ingot", "Copper")),
                        List.of(port(farther, Role.UNKNOWN, "gold ingot", "Gold"))))
                .build();

        var result = graph.queryMaterial(ore, new Budgets(0, 64, 64, 128));

        assertEquals(
                List.of("source", "crush", "grind", "use"),
                result.steps().stream().map(Step::name).toList());
        assertEquals(3, result.edges().size());
        assertFalse(result.truncated());
    }

    private static boolean edgeIsConfirmed(MaterialProcessGraph.Edge edge) {
        return edge.confirmed();
    }

    @Test
    void materialPageKeepsLargeRecipesAndDenseConnections() {
        var builder = MaterialProcessGraph.builder(SNAPSHOT);
        var common = resource("common");
        var outputs = new java.util.ArrayList<Port>();
        for (int index = 0; index < 160; index++)
            outputs.add(port(resource("product_" + index), Role.UNKNOWN, "product_" + index));
        builder.add(step("large", List.of(port(common, Role.CONSUMED, "common")), outputs));
        for (int index = 0; index < 70; index++) {
            builder.add(step("producer_" + index, List.of(), List.of(port(common, Role.UNKNOWN, "common"))));
            builder.add(step("consumer_" + index, List.of(port(common, Role.CONSUMED, "common")), List.of()));
        }
        for (int index = 0; index < 600; index++)
            builder.add(step(
                    "route_" + index,
                    List.of(),
                    List.of(port(resource("local_" + index), Role.UNKNOWN, "local_" + index))));
        var graph = builder.build();
        var query = graph.queryMaterial(common, Budgets.MATERIAL_PAGE);
        assertEquals(741, query.steps().size());
        assertEquals(4970, query.edges().size());
        assertEquals(160, query.steps().get(0).outputs().size());
        assertFalse(query.truncated());
        assertTrue(query.notices().isEmpty());
    }

    @Test
    void compactMaterialQueryStoresDenseAndCyclicAssociationsAsPortIncidence() {
        var builder = MaterialProcessGraph.builder(SNAPSHOT);
        var common = resource("dense_common");
        var input = port(common, Role.CONSUMED, "common");
        var output = port(common, Role.UNKNOWN, "common");
        for (int index = 0; index < 10_000; index++) {
            builder.add(step("dense_source_" + index, List.of(), List.of(output)));
            builder.add(step("dense_use_" + index, List.of(input), List.of()));
        }
        builder.add(step("dense_loop", List.of(input), List.of(output)));
        var graph = builder.build();
        var query = graph.queryMaterial(common);
        assertEquals(20_001, query.steps().size());
        assertEquals(query.steps(), graph.materialSteps(common));
        assertEquals(1, query.flows().size());
        var flow = query.flows().get(0);
        assertEquals(10_001, flow.sources().size());
        assertEquals(10_001, flow.targets().size());
        assertEquals(100_020_001L, flow.connectionCount());
        assertTrue(flow.sources().contains(new DataKey("gt6ce", "step/dense_loop")));
        assertTrue(flow.targets().contains(new DataKey("gt6ce", "step/dense_loop")));
        assertTrue(query.notices().isEmpty());
    }

    @Test
    void compactFlowsPreserveExactResourcesAndConfirmationWithoutInventingSelectorLinks() {
        var first = resource("selector_first");
        var second = resource("selector_second");
        var produced = port(first, Role.UNKNOWN, "same_display");
        var used = port(first, Role.CONSUMED, "same_display");
        var other = port(second, Role.CONSUMED, "same_display");
        var source = new Step(
                new DataKey("gt6ce", "step/unconfirmed"),
                "unconfirmed",
                List.of(),
                List.of(produced),
                List.of(),
                false);
        var graph = MaterialProcessGraph.builder(SNAPSHOT)
                .add(source)
                .add(step("local_loop", List.of(used), List.of(produced)))
                .add(step("independent_selector", List.of(other), List.of()))
                .build();
        var compact = graph.queryMaterial(first);
        var expanded = graph.queryMaterial(first, new Budgets(0, 32, 32, 128));
        var restored = new java.util.HashSet<Edge>();
        for (Flow flow : compact.flows())
            for (DataKey from : flow.sources())
                for (DataKey to : flow.targets())
                    restored.add(new Edge(
                            from,
                            to,
                            flow.resource(),
                            graph.step(from).orElseThrow().resolved()
                                    && graph.step(to).orElseThrow().resolved()));
        assertEquals(expanded.steps(), compact.steps());
        assertEquals(new java.util.HashSet<>(expanded.edges()), restored);
        assertEquals(1, compact.flows().size());
        assertEquals(
                List.of(new DataKey("gt6ce", "step/local_loop")),
                compact.flows().get(0).targets());
        assertTrue(restored.stream().anyMatch(edge -> !edge.confirmed()));
    }

    @Test
    void invalidRowsDoNotPoisonPublishedIndexesOrDiscardValidRows() {
        var ore = resource("ore");
        var dust = resource("dust");
        var valid = step("valid", List.of(port(ore, Role.CONSUMED, "ore")), List.of(port(dust, Role.UNKNOWN, "dust")));
        var conflict = step(
                "conflict",
                List.of(port(resource("partial"), Role.CONSUMED, "partial")),
                List.of(port(ore, Role.UNKNOWN, "another_state")));
        var intraRow = step(
                "intra_row",
                List.of(port(resource("intra"), Role.CONSUMED, "one")),
                List.of(port(resource("intra"), Role.UNKNOWN, "two")));
        var next = step("next", List.of(port(dust, Role.CONSUMED, "dust")), List.of());
        var graph = MaterialProcessGraph.builder(SNAPSHOT)
                .add(valid)
                .add(valid)
                .add(conflict)
                .add(intraRow)
                .add(next)
                .build();
        assertEquals(
                List.of(valid, next),
                graph.queryMaterial(ore, Budgets.MATERIAL_PAGE).steps());
        assertEquals(
                List.of(ore, dust).stream()
                        .sorted(java.util.Comparator.comparing(DataKey::toString))
                        .toList(),
                graph.resources().stream().map(Resource::key).toList());
        assertTrue(graph.step(conflict.reference()).isEmpty());
        assertTrue(graph.step(intraRow.reference()).isEmpty());
        assertEquals(
                3,
                graph.queryMaterial(ore, Budgets.MATERIAL_PAGE).notices().stream()
                        .filter(notice -> notice.code().equals("invalid_process_data"))
                        .mapToInt(Notice::omitted)
                        .sum());
    }

    @Test
    void mainHintsMayRepeatAndWrongSidePortsAreRemovedWithoutConfirmingTheRow() {
        var first = port(resource("first"), Role.CONSUMED, "first");
        var second = port(resource("second"), Role.CONSUMED, "second");
        first = new Port(first.resource(), first.role(), first.amount(), first.chance(), true, first.details());
        second = new Port(second.resource(), second.role(), second.amount(), second.chance(), true, second.details());
        var row = step("hints", List.of(first, second), List.of(first));
        assertEquals(2, row.inputs().size());
        assertTrue(row.outputs().isEmpty());
        assertFalse(row.resolved());
    }

    @Test
    void portsTruncatedAtTheDepthBoundaryStillReportPartialCoverage() {
        var graph = MaterialProcessGraph.builder(SNAPSHOT)
                .add(step(
                        "split",
                        List.of(port(resource("ore"), Role.CONSUMED, "ore")),
                        List.of(
                                port(resource("one"), Role.UNKNOWN, "dust"),
                                port(resource("two"), Role.UNKNOWN, "dust"))))
                .build();
        for (Direction direction : Direction.values()) {
            DataKey root = direction == Direction.USES ? resource("ore") : resource("one");
            var result = graph.query(root, direction, new Budgets(0, 8, 1, 8));
            assertTrue(result.truncated());
            assertTrue(
                    result.notices().stream().anyMatch(notice -> notice.code().equals("port_limit")));
        }
    }

    @Test
    void edgesNeverReferToPortsDiscardedByTheBudget() {
        var graph = MaterialProcessGraph.builder(SNAPSHOT)
                .add(step(
                        "split",
                        List.of(port(resource("ore"), Role.CONSUMED, "ore")),
                        List.of(
                                port(resource("one"), Role.UNKNOWN, "dust"),
                                port(resource("two"), Role.UNKNOWN, "dust"))))
                .add(step(
                        "use",
                        List.of(port(resource("two"), Role.CONSUMED, "dust")),
                        List.of(port(resource("end"), Role.UNKNOWN, "dust"))))
                .build();
        var result = graph.query(resource("two"), Direction.BOTH, new Budgets(4, 8, 1, 8));
        assertEquals(2, result.steps().size());
        assertTrue(result.edges().isEmpty());
        assertTrue(result.truncated());
    }

    @Test
    void materialEdgesUseTheRetainedPortsBeforeSpendingTheEdgeBudget() {
        var first = resource("first");
        var second = resource("second");
        var third = resource("third");
        var graph = MaterialProcessGraph.builder(SNAPSHOT)
                .add(step(
                        "producer",
                        List.of(port(resource("raw"), Role.CONSUMED, "raw")),
                        List.of(
                                port(first, Role.UNKNOWN, "first"),
                                port(second, Role.UNKNOWN, "second"),
                                port(third, Role.UNKNOWN, "third"))))
                .add(step(
                        "consumer",
                        List.of(
                                port(third, Role.CONSUMED, "third"),
                                port(first, Role.CONSUMED, "first"),
                                port(second, Role.CONSUMED, "second")),
                        List.of(port(resource("end"), Role.UNKNOWN, "end"))))
                .build();
        for (int ports = 1; ports <= 3; ports++) {
            for (int edges : List.of(0, 1, 3)) {
                var result = graph.queryMaterial(first, new Budgets(0, 8, ports, edges));
                int available = ports == 1 ? 0 : ports == 2 ? 1 : 3;
                assertEquals(Math.min(available, edges), result.edges().size());
                for (Edge edge : result.edges()) {
                    var source = result.steps().stream()
                            .filter(row -> row.reference().equals(edge.source()))
                            .findFirst()
                            .orElseThrow();
                    var target = result.steps().stream()
                            .filter(row -> row.reference().equals(edge.target()))
                            .findFirst()
                            .orElseThrow();
                    assertTrue(source.outputs().stream()
                            .anyMatch(port -> port.resource().key().equals(edge.resource())));
                    assertTrue(target.inputs().stream()
                            .anyMatch(port -> port.resource().key().equals(edge.resource())));
                }
                int omittedPorts = 2 * (3 - ports);
                assertEquals(
                        omittedPorts,
                        result.notices().stream()
                                .filter(notice -> notice.code().equals("port_limit"))
                                .mapToInt(Notice::omitted)
                                .sum());
                assertEquals(
                        Math.max(0, available - edges),
                        result.notices().stream()
                                .filter(notice -> notice.code().equals("edge_limit"))
                                .mapToInt(Notice::omitted)
                                .sum());
            }
        }
    }

    private static DataKey resource(String name) {
        return new DataKey("gt6ce", "resource/" + name);
    }

    @Test
    void genericRecoveryIsAnInputUseAndDoesNotSpendTheSourceBudget() {
        var iron = resource("iron");
        var copper = resource("copper");
        var dust = resource("iron_dust");
        var reclaim = step(
                "reclaim",
                List.of(port(copper, Role.CONSUMED, "copper", "Copper")),
                List.of(port(dust, Role.UNKNOWN, "dust", "Iron")));
        var internal = step(
                "internal_recovery",
                List.of(port(iron, Role.CONSUMED, "iron")),
                List.of(port(dust, Role.UNKNOWN, "dust")));
        var source = step(
                "ordinary_source",
                List.of(port(copper, Role.CONSUMED, "copper", "Copper")),
                List.of(port(iron, Role.UNKNOWN, "iron")));
        var graph = MaterialProcessGraph.builder(SNAPSHOT)
                .add(reclaim, true)
                .add(internal, true)
                .add(source)
                .build();
        var material = graph.queryMaterial(dust, new Budgets(0, 2, 16, 16));
        assertEquals(List.of(source, internal), material.steps());
        assertEquals(
                List.of(source),
                graph.queryMaterial(dust, new Budgets(0, 1, 16, 16)).steps());
        assertFalse(material.truncated());
        assertTrue(graph.query(dust, Direction.SOURCES, Budgets.DEFAULT).steps().isEmpty());
        assertEquals(
                List.of(reclaim),
                graph.query(copper, Direction.USES, new Budgets(0, 1, 16, 16)).steps());
        assertEquals(
                List.of(source),
                graph.query(iron, Direction.SOURCES, Budgets.DEFAULT).steps());
        assertEquals(
                List.of(internal),
                graph.query(iron, Direction.USES, new Budgets(0, 8, 16, 16)).steps());
    }

    private static Port port(DataKey key, Role role, String state) {
        return port(key, role, state, "Iron");
    }

    private static Port port(DataKey key, Role role, String state, String material) {
        boolean output = role == Role.PRIMARY || role == Role.BYPRODUCT || role == Role.UNKNOWN;
        return new Port(
                new Resource(key, material, "dust", "solid", state),
                role,
                new Amount(1, state.startsWith("unit:") ? AmountUnit.MATERIAL_UNIT : AmountUnit.ITEM),
                output ? OptionalInt.of(10_000) : OptionalInt.empty(),
                false,
                List.of());
    }

    private static Step step(String name, List<Port> inputs, List<Port> outputs) {
        return new Step(new DataKey("gt6ce", "step/" + name), name, inputs, outputs, List.of(), true);
    }
}
