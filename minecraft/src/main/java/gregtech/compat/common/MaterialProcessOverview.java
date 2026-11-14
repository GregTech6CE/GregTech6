package gregtech.compat.common;

import gregtech.api.addon.material.DataKey;
import gregtech.compat.common.MaterialProcessDisplay.Connection;
import gregtech.compat.common.MaterialProcessDisplay.Page;
import gregtech.compat.common.MaterialProcessDisplay.Port;
import gregtech.compat.common.MaterialProcessDisplay.Role;
import gregtech.compat.common.MaterialProcessDisplay.Section;
import gregtech.compat.common.MaterialProcessDisplay.Step;
import gregtech.engine.stack.StackCompat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/** A bounded layout of native steps; resource copies never introduce recipe facts. */
public final class MaterialProcessOverview {
    /** The original port, including its side and slot, survives sharing and local copying. */
    public record Endpoint(Step step, boolean output, int index, Port port) {
        public String key() {
            return step.reference() + (output ? ":out:" : ":in:") + index + ":" + port.resource();
        }
    }

    public static final class Node {
        public final String key;
        public final Section section;
        public final List<Endpoint> endpoints = new ArrayList<>();
        public final List<Step> steps = new ArrayList<>();
        public int x, y, layer;
        public boolean copy, returning;
        private boolean feedbackCopy;

        private Node(String key, Section section) {
            this.key = key;
            this.section = section;
        }

        public boolean machine() {
            return !steps.isEmpty();
        }

        public Port port() {
            return endpoints.get(0).port();
        }

        public boolean extra() {
            return steps.size() > 1
                    || !auxiliaries.isEmpty()
                    || steps.stream()
                            .anyMatch(step -> java.util.stream.Stream.concat(
                                            step.inputs().stream(), step.outputs().stream())
                                    .anyMatch(port -> port.icons().size() > 1));
        }

        public final List<Port> auxiliaries = new ArrayList<>();
    }

    public record Point(int x, int y) {}

    public static final class Edge {
        private Node from, to;
        private final Endpoint endpoint;
        private final List<Endpoint> endpoints = new ArrayList<>();
        private boolean confirmed;
        private boolean feedback;
        private List<Point> path = List.of();

        private Edge(Node from, Node to, Endpoint endpoint) {
            this.from = from;
            this.to = to;
            this.endpoint = endpoint;
            endpoints.add(endpoint);
            confirmed = endpoint.step().resolved();
        }

        public Node from() {
            return from;
        }

        public Node to() {
            return to;
        }

        public Endpoint endpoint() {
            return endpoint;
        }

        public List<Endpoint> endpoints() {
            return List.copyOf(endpoints);
        }

        public boolean confirmed() {
            return confirmed;
        }

        public boolean feedback() {
            return feedback;
        }

        public List<Point> path() {
            return path;
        }
    }

    private static final class IconKey {
        private final ItemStack icon;

        private IconKey(ItemStack icon) {
            this.icon = icon;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof IconKey key && StackCompat.sameItem(icon, key.icon);
        }

        @Override
        public int hashCode() {
            return icon.getItem().hashCode();
        }
    }

    private record RouteKey(IconKey icon, String fallback, List<String> inputs, List<String> outputs) {}

    private record EdgeKey(Node from, Node to) {}

    private record SideKey(DataKey step, DataKey resource) {}

    private record Arc(Node from, Node to, int distance, Edge edge, Connection connection) {}

    private record Occupancy(BitSet positive, BitSet negative) {
        private Occupancy() {
            this(new BitSet(), new BitSet());
        }

        private void add(int lane) {
            (lane >= 0 ? positive : negative).set(lane >= 0 ? lane : -lane - 1);
        }

        private int above(int center) {
            if (center >= 0) return positive.nextClearBit(center);
            int negativeIndex = negative.previousClearBit(-center - 1);
            return negativeIndex >= 0 ? -negativeIndex - 1 : positive.nextClearBit(0);
        }

        private int below(int center) {
            if (center < 0) return -negative.nextClearBit(-center - 1) - 1;
            int positiveIndex = positive.previousClearBit(center);
            return positiveIndex >= 0 ? positiveIndex : -negative.nextClearBit(0) - 1;
        }
    }

    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<Connection> connections;
    private final Map<DataKey, Node> machines = new LinkedHashMap<>();
    private final Set<SideKey> inputs = new HashSet<>(), outputs = new HashSet<>();
    private final Map<Node, Node> next = new HashMap<>(), previous = new HashMap<>();
    private final Set<Arc> feedback = new HashSet<>();
    private int width, height;
    private Node anchor;

    public MaterialProcessOverview(Page page) {
        connections = page.connections().stream()
                .sorted(Comparator.comparing(
                        connection -> connection.source() + ":" + connection.target() + ":" + connection.resource()))
                .toList();
        var resources = new LinkedHashMap<String, Node>();
        var routes = new LinkedHashMap<RouteKey, Node>();
        var wires = new LinkedHashMap<EdgeKey, Edge>();
        for (Step step : page.steps().stream()
                .sorted(Comparator.comparing(value -> value.reference().toString()))
                .toList()) {
            List<Port> visibleInputs = visible(step.inputs(), page.material(), false);
            List<Port> visibleOutputs = visible(step.outputs(), page.material(), true);
            ItemStack icon = step.icon();
            var key = new RouteKey(
                    new IconKey(icon),
                    icon.isEmpty() ? step.reference().toString() : "",
                    identities(visibleInputs),
                    identities(visibleOutputs));
            Node machine = routes.computeIfAbsent(key, ignored -> {
                var node = new Node(
                        "step:" + step.reference(),
                        MaterialProcessDisplay.section(step, page.material()).orElse(null));
                nodes.add(node);
                return node;
            });
            machine.steps.add(step);
            machines.put(step.reference(), machine);
            for (boolean output : List.of(false, true)) {
                List<Port> ports = output ? step.outputs() : step.inputs();
                List<Port> visible = output ? visibleOutputs : visibleInputs;
                for (int index = 0; index < ports.size(); index++) {
                    Port port = ports.get(index);
                    if (!visible.contains(port)) {
                        machine.auxiliaries.add(port);
                        continue;
                    }
                    var endpoint = new Endpoint(step, output, index, port);
                    (output ? outputs : inputs).add(new SideKey(step.reference(), port.resource()));
                    Node resource = resources.computeIfAbsent(port.identity(), identity -> {
                        var node = new Node("resource:" + identity, null);
                        nodes.add(node);
                        return node;
                    });
                    if (port.hasIcons()
                            && !resource.endpoints.isEmpty()
                            && !resource.port().hasIcons()) resource.endpoints.add(0, endpoint);
                    else resource.endpoints.add(endpoint);
                    Node from = output ? machine : resource, to = output ? resource : machine;
                    Edge edge = wires.get(new EdgeKey(from, to));
                    if (edge == null) {
                        edge = new Edge(from, to, endpoint);
                        wires.put(new EdgeKey(from, to), edge);
                        edges.add(edge);
                    } else {
                        edge.endpoints.add(endpoint);
                        edge.confirmed &= step.resolved();
                    }
                }
            }
        }
        Set<String> uncertain = new HashSet<>();
        for (Connection connection : connections)
            if (!connection.confirmed()) uncertain.add(connection.target() + ":" + connection.resource());
        for (Edge edge : edges)
            if (edge.endpoints.stream()
                    .anyMatch(endpoint -> !endpoint.output()
                            && uncertain.contains(endpoint.step().reference() + ":"
                                    + endpoint.port().resource()))) edge.confirmed = false;
        layout();
        // Each pass detaches at least one shared port; copies themselves are never split again.
        while (splitLongEdges()) layout();
        Map<String, Integer> counts = new HashMap<>();
        for (Node node : nodes) if (!node.machine()) counts.merge(node.port().identity(), 1, Integer::sum);
        for (Node node : nodes)
            if (!node.machine()) node.copy = counts.get(node.port().identity()) > 1;
        route();
        anchor = nodes.stream()
                .filter(node -> !node.machine() && node.port().material().equals(page.material()))
                .min(Comparator.<Node>comparingInt(node -> -chainLength(node)).thenComparing(node -> node.key))
                .orElse(nodes.isEmpty() ? null : nodes.get(0));
    }

    public List<Node> nodes() {
        return List.copyOf(nodes);
    }

    public List<Edge> edges() {
        return List.copyOf(edges);
    }

    public List<Connection> connections() {
        return connections;
    }

    public Node anchor() {
        return anchor;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    private static List<Port> visible(List<Port> ports, String material, boolean output) {
        List<Port> candidates = ports.stream()
                .filter(port -> output || port.role() == Role.CONSUMED)
                .toList();
        List<Port> local = candidates.stream()
                .filter(port -> port.material().equals(material))
                .toList();
        if (!local.isEmpty()) return local;
        // Foreign co-inputs and tools stay in the + details. The main hint never changes port roles.
        return candidates.stream()
                .filter(Port::main)
                .findFirst()
                .or(() -> candidates.stream()
                        .filter(port -> !output || port.role() != Role.BYPRODUCT)
                        .findFirst())
                .map(List::of)
                .orElse(List.of());
    }

    private static List<String> identities(List<Port> ports) {
        return ports.stream().map(Port::identity).distinct().sorted().toList();
    }

    private Node resource(Edge edge) {
        return edge.from.machine() ? edge.to : edge.from;
    }

    private void split(Edge edge) {
        Node original = resource(edge);
        Node local = new Node("copy:" + edge.endpoint.key(), null);
        local.feedbackCopy = edge.feedback || original.returning;
        local.endpoints.addAll(edge.endpoints);
        nodes.add(local);
        original.endpoints.removeAll(edge.endpoints);
        if (edge.from == original) edge.from = local;
        else edge.to = local;
    }

    private boolean splitLongEdges() {
        boolean changed = false;
        var counts = new HashMap<Node, Integer>();
        for (Edge edge : edges) counts.merge(resource(edge), 1, Integer::sum);
        var columns = new HashMap<Integer, java.util.NavigableMap<Integer, Node>>();
        for (Node node : nodes)
            columns.computeIfAbsent(node.layer, ignored -> new java.util.TreeMap<>())
                    .put(node.y, node);
        for (Edge edge : edges) {
            Node resource = resource(edge);
            if (counts.get(resource) < 2 || next.get(edge.from) == edge.to) continue;
            if (!edge.feedback
                    && edge.to.layer - edge.from.layer <= 3
                    && Math.abs(edge.to.y - edge.from.y) <= 192
                    && !crossesNode(edge, columns)) continue;
            split(edge);
            counts.merge(resource, -1, Integer::sum);
            changed = true;
        }
        return changed;
    }

    private static boolean crossesNode(Edge edge, Map<Integer, java.util.NavigableMap<Integer, Node>> columns) {
        double sx = edge.from.x + 12, tx = edge.to.x - 12;
        if (tx <= sx) return true;
        double slope = (edge.to.y - edge.from.y) / (tx - sx);
        for (int layer = edge.from.layer + 1; layer < edge.to.layer; layer++) {
            var column = columns.get(layer);
            if (column == null) continue;
            double y = edge.from.y + (24 + layer * 52 - sx) * slope;
            double margin = 14 + 12 * Math.abs(slope);
            var candidate = column.ceilingEntry((int) Math.ceil(y - margin));
            if (candidate != null && candidate.getKey() <= y + margin) return true;
        }
        return false;
    }

    private List<Arc> arcs() {
        var result = new ArrayList<Arc>();
        for (Edge edge : edges) result.add(new Arc(edge.from, edge.to, 1, edge, null));
        // Supplied associations constrain layers even when a local copy replaces the long visible wire.
        for (Connection connection : connections) {
            if (!outputs.contains(new SideKey(connection.source(), connection.resource()))
                    || !inputs.contains(new SideKey(connection.target(), connection.resource()))) continue;
            Node source = machines.get(connection.source()), target = machines.get(connection.target());
            if (source != null && target != null) result.add(new Arc(source, target, 2, null, connection));
        }
        result.sort(Comparator.comparing((Arc arc) -> arc.from.key)
                .thenComparing(arc -> arc.to.key)
                .thenComparing(arc ->
                        arc.connection == null ? "" : arc.connection.resource().toString()));
        return result;
    }

    private void layout() {
        next.clear();
        previous.clear();
        feedback.clear();
        List<Arc> arcs = arcs();
        var outgoing = new HashMap<Node, List<Arc>>();
        var indegree = new HashMap<Node, Integer>();
        for (Node node : nodes) {
            node.layer = 0;
            node.returning = node.feedbackCopy;
        }
        for (Edge edge : edges) edge.feedback = false;
        for (Arc arc : arcs) {
            outgoing.computeIfAbsent(arc.from, ignored -> new ArrayList<>()).add(arc);
            indegree.merge(arc.to, 1, Integer::sum);
        }
        // Iterative DFS cuts only feedback constraints, including pure cycles and self loops.
        var state = new HashMap<Node, Integer>();
        record Frame(Node node, java.util.Iterator<Arc> iterator) {}
        var roots = new ArrayList<>(nodes);
        roots.sort(Comparator.<Node>comparingInt(node -> indegree.getOrDefault(node, 0) == 0 ? 0 : 1)
                .thenComparing(node -> node.key));
        for (Node root : roots) {
            if (state.containsKey(root)) continue;
            var stack = new ArrayDeque<Frame>();
            state.put(root, 1);
            stack.push(new Frame(root, outgoing.getOrDefault(root, List.of()).iterator()));
            while (!stack.isEmpty()) {
                Frame frame = stack.peek();
                if (!frame.iterator.hasNext()) {
                    state.put(frame.node, 2);
                    stack.pop();
                    continue;
                }
                Arc arc = frame.iterator.next();
                if (state.getOrDefault(arc.to, 0) == 1) {
                    feedback.add(arc);
                    indegree.merge(arc.to, -1, Integer::sum);
                    if (arc.edge != null) {
                        arc.edge.feedback = true;
                        resource(arc.edge).returning = true;
                    } else
                        for (Edge edge : edges)
                            if (edge.endpoint.step().reference().equals(arc.connection.target())
                                    && edge.endpoint.port().resource().equals(arc.connection.resource()))
                                resource(edge).returning = true;
                } else if (!state.containsKey(arc.to)) {
                    state.put(arc.to, 1);
                    stack.push(new Frame(
                            arc.to, outgoing.getOrDefault(arc.to, List.of()).iterator()));
                }
            }
        }
        var ready = new java.util.PriorityQueue<Node>(Comparator.comparing(node -> node.key));
        for (Node node : roots) if (indegree.getOrDefault(node, 0) == 0) ready.add(node);
        while (!ready.isEmpty()) {
            Node node = ready.remove();
            for (Arc arc : outgoing.getOrDefault(node, List.of()))
                if (!feedback.contains(arc)) {
                    arc.to.layer = Math.max(arc.to.layer, node.layer + arc.distance);
                    if (indegree.merge(arc.to, -1, Integer::sum) == 0) ready.add(arc.to);
                }
        }
        var nearestConsumer = new HashMap<Node, Integer>();
        for (Edge edge : edges)
            if (!edge.endpoint.output()) nearestConsumer.merge(edge.from, edge.to.layer - 1, Math::min);
        for (Node node : nodes)
            if (!node.machine() && node.endpoints.stream().noneMatch(Endpoint::output))
                node.layer = Math.max(0, nearestConsumer.getOrDefault(node, node.layer));
        var degree = new HashMap<Node, Integer>();
        var neighbors = new HashMap<Node, List<Node>>();
        for (Edge edge : edges) {
            degree.merge(edge.from, 1, Integer::sum);
            degree.merge(edge.to, 1, Integer::sum);
            neighbors.computeIfAbsent(edge.from, ignored -> new ArrayList<>()).add(edge.to);
            neighbors.computeIfAbsent(edge.to, ignored -> new ArrayList<>()).add(edge.from);
        }
        var ordered = new ArrayList<>(edges);
        var before = new HashMap<Node, Integer>();
        var after = new HashMap<Node, Integer>();
        var forwards = edges.stream()
                .filter(edge -> !edge.feedback && edge.to.layer > edge.from.layer)
                .sorted(Comparator.comparingInt(edge -> edge.from.layer))
                .toList();
        for (Edge edge : forwards) before.merge(edge.to, before.getOrDefault(edge.from, 0) + 1, Math::max);
        for (int index = forwards.size() - 1; index >= 0; index--) {
            Edge edge = forwards.get(index);
            after.merge(edge.from, after.getOrDefault(edge.to, 0) + 1, Math::max);
        }
        ordered.sort(Comparator.<Edge>comparingInt(edge -> degree.get(edge.from) + degree.get(edge.to))
                .thenComparing(edge -> !edge.endpoint.port().main())
                .thenComparingInt(edge -> -before.getOrDefault(edge.from, 0) - after.getOrDefault(edge.to, 0))
                .thenComparing(edge -> edge.endpoint.key()));
        for (Edge edge : ordered)
            if (!edge.feedback
                    && edge.to.layer == edge.from.layer + 1
                    && !next.containsKey(edge.from)
                    && !previous.containsKey(edge.to)) {
                next.put(edge.from, edge.to);
                previous.put(edge.to, edge.from);
            }
        var starts = nodes.stream()
                .filter(node -> !previous.containsKey(node))
                .sorted(Comparator.<Node>comparingInt(node -> -chainLength(node))
                        .thenComparingInt(node -> node.layer)
                        .thenComparing(node -> family(node, neighbors))
                        .thenComparing(node -> node.key))
                .toList();
        var lanes = new HashMap<Node, Integer>();
        var occupied = new HashMap<Integer, Occupancy>();
        for (Node start : starts) {
            var chain = new ArrayList<Node>();
            for (Node node = start; node != null; node = next.get(node)) chain.add(node);
            Set<Node> members = new HashSet<>(chain);
            double center = chain.stream()
                    .flatMap(node -> neighbors.getOrDefault(node, List.of()).stream())
                    .filter(node -> !members.contains(node))
                    .filter(lanes::containsKey)
                    .mapToInt(lanes::get)
                    .average()
                    .orElse(0);
            int first = start.layer, last = chain.get(chain.size() - 1).layer, desired = (int) Math.round(center);
            var blocked = new Occupancy();
            for (int layer = first; layer <= last; layer++) {
                Occupancy values = occupied.get(layer);
                if (values != null) {
                    blocked.positive.or(values.positive);
                    blocked.negative.or(values.negative);
                }
            }
            int above = blocked.above(desired), below = blocked.below(desired);
            int lane = above - desired <= desired - below ? above : below;
            for (int layer = first; layer <= last; layer++)
                occupied.computeIfAbsent(layer, ignored -> new Occupancy()).add(lane);
            for (Node node : chain) lanes.put(node, lane);
        }
        int minimum = lanes.values().stream().mapToInt(Integer::intValue).min().orElse(0);
        for (Node node : nodes) {
            node.x = 24 + node.layer * 52;
            node.y = 24 + (lanes.get(node) - minimum) * 32;
        }
    }

    private static String family(Node start, Map<Node, List<Node>> neighbors) {
        if (!start.machine()) return start.port().identity();
        return neighbors.getOrDefault(start, List.of()).stream()
                .filter(node -> !node.machine())
                .map(node -> node.port().identity())
                .sorted()
                .findFirst()
                .orElse(start.key);
    }

    private int chainLength(Node start) {
        int count = 0;
        for (Node node = start; node != null; node = next.get(node)) count++;
        return count;
    }

    private void route() {
        for (Edge edge : edges) {
            Node from = edge.from, to = edge.to;
            edge.path = List.of(new Point(from.x + 12, from.y), new Point(to.x - 12, to.y));
        }
        width = nodes.stream().mapToInt(node -> node.x + 24).max().orElse(48);
        height = nodes.stream().mapToInt(node -> node.y + 24).max().orElse(48);
    }
}
