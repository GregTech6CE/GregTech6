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

    private static boolean edgeIsConfirmed(MaterialProcessGraph.Edge edge) {
        return edge.confirmed();
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

    private static DataKey resource(String name) {
        return new DataKey("gt6ce", "resource/" + name);
    }

    private static Port port(DataKey key, Role role, String state) {
        boolean output = role == Role.PRIMARY || role == Role.BYPRODUCT || role == Role.UNKNOWN;
        return new Port(
                new Resource(key, "Iron", "dust", "solid", state),
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
