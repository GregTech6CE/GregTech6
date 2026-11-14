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
        private Grid grid;
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

        /** Producers determine the badge on a shared resource; consumption amounts stay on native ports. */
        public List<Port> badgePorts() {
            boolean produced = endpoints.stream().anyMatch(Endpoint::output);
            return endpoints.stream()
                    .filter(endpoint -> endpoint.output() == produced)
                    .map(Endpoint::port)
                    .toList();
        }

        public String quantityBadge() {
            List<String> values = badgePorts().stream()
                    .map(port -> port.quantity().getString())
                    .distinct()
                    .sorted()
                    .toList();
            if (values.size() == 1) return values.get(0);
            if (values.size() > 3) return "…";
            return String.join(
                    "/",
                    values.stream().map(value -> value.isEmpty() ? "1" : value).toList());
        }

        public String chanceBadge() {
            if (endpoints.stream().noneMatch(Endpoint::output)) return "";
            List<String> values = badgePorts().stream()
                    .map(port -> port.chance().isPresent()
                            ? java.math.BigDecimal.valueOf(port.chance().getAsInt(), 2)
                                            .stripTrailingZeros()
                                            .toPlainString()
                                    + "%"
                            : "?%")
                    .distinct()
                    .sorted()
                    .toList();
            return values.equals(List.of("100%")) ? "" : values.size() > 3 ? "…%" : String.join("/", values);
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

    /** One inventory-like display box; each cell retains its own native ports and navigation. */
    public static final class Grid {
        public final List<Node> members;
        private final Node root;

        private Grid(List<Node> members) {
            this.members = List.copyOf(members);
            root = members.get(0);
            members.forEach(node -> node.grid = this);
        }

        public int left() {
            return root.x - 12;
        }

        public int right() {
            return left() + Math.min(3, members.size()) * 24;
        }

        public int top() {
            return root.y - 12;
        }

        public int bottom() {
            return top() + ((members.size() + 2) / 3) * 24;
        }

        public int centerY() {
            return (top() + bottom()) / 2;
        }
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

    private record RouteKey(IconKey icon, String fallback, List<String> inputs, String output, boolean recovery) {}

    private record EdgeKey(Node from, Node to) {}

    private record Arc(Node from, Node to, Edge edge) {}

    private record Occupancy(BitSet positive, BitSet negative) {
        private Occupancy() {
            this(new BitSet(), new BitSet());
        }

        private void add(int lane) {
            (lane >= 0 ? positive : negative).set(lane >= 0 ? lane : -lane - 1);
        }

        private boolean contains(int lane) {
            return (lane >= 0 ? positive : negative).get(lane >= 0 ? lane : -lane - 1);
        }
    }

    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<Grid> grids = new ArrayList<>();

    public record Band(Section section, int left, int right) {}

    private final List<Band> bands = new ArrayList<>();
    private final List<Connection> connections;
    private final Map<Node, Node> next = new HashMap<>(), previous = new HashMap<>();
    private final Set<Arc> feedback = new HashSet<>();
    private int width, height;
    private Node anchor;

    public MaterialProcessOverview(Page page) {
        this(page, true);
    }

    private MaterialProcessOverview(Page page, boolean partition) {
        connections = page.connections().stream()
                .sorted(Comparator.comparing(
                        connection -> connection.source() + ":" + connection.target() + ":" + connection.resource()))
                .toList();
        if (partition && !page.steps().isEmpty()) {
            partition(page);
            return;
        }
        var resources = new LinkedHashMap<String, Node>();
        var routes = new LinkedHashMap<RouteKey, Node>();
        var wires = new LinkedHashMap<EdgeKey, Edge>();
        for (Step step : page.steps().stream()
                .sorted(Comparator.comparing(value -> value.reference().toString()))
                .toList()) {
            List<Port> visibleInputs = visible(step.inputs(), page.material(), false);
            List<Port> visibleOutputs = visible(step.outputs(), page.material(), true);
            ItemStack icon = step.icon();
            // Similar routes share a machine and primary product; native recipe differences stay in the details.
            var key = new RouteKey(
                    new IconKey(icon),
                    icon.isEmpty() ? step.reference().toString() : "",
                    visibleOutputs.isEmpty() ? identities(visibleInputs) : List.of(),
                    visibleOutputs.isEmpty() ? "" : visibleOutputs.get(0).identity(),
                    step.genericRecovery());
            Node machine = routes.computeIfAbsent(key, ignored -> {
                var node = new Node(
                        "step:" + step.reference(),
                        MaterialProcessDisplay.section(step, page.material()).orElse(null));
                nodes.add(node);
                return node;
            });
            machine.steps.add(step);
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
                    machine.endpoints.add(endpoint);
                    String instance = output && step.genericRecovery()
                            ? "recovery:" + machine.key + ":" + port.identity()
                            : "resource:" + port.identity();
                    Node resource = resources.computeIfAbsent(instance, identity -> {
                        var node = new Node(identity, null);
                        node.feedbackCopy = output && step.genericRecovery();
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
        applyConfirmation();
        // Detect returns before detaching shared resources into inventory grids.
        layout();
        if (localizeGrids()) layout();
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

    public List<Band> bands() {
        return List.copyOf(bands);
    }

    public List<Grid> grids() {
        return List.copyOf(grids);
    }

    private Map<Node, Set<Node>> producers() {
        var result = new HashMap<Node, Set<Node>>();
        for (Edge edge : edges)
            if (edge.endpoint.output())
                result.computeIfAbsent(edge.to, ignored -> new HashSet<>()).add(edge.from);
        return result;
    }

    private Map<Node, List<Edge>> machineEdges() {
        var result = new HashMap<Node, List<Edge>>();
        for (Edge edge : edges)
            result.computeIfAbsent(edge.endpoint.output() ? edge.from : edge.to, ignored -> new ArrayList<>())
                    .add(edge);
        return result;
    }

    private boolean localizeGrids() {
        boolean changed = false;
        var producers = producers();
        var machineEdges = machineEdges();
        var counts = new HashMap<Node, Integer>();
        for (Edge edge : edges) counts.merge(resource(edge), 1, Integer::sum);
        for (Node machine : List.copyOf(nodes)) {
            if (!machine.machine()) continue;
            for (boolean output : List.of(false, true)) {
                List<Edge> side = machineEdges.getOrDefault(machine, List.of()).stream()
                        .filter(edge -> edge.endpoint.output() == output)
                        .toList();
                if (side.size() < 2) continue;
                for (Edge edge : side) {
                    Node resource = resource(edge);
                    if (counts.get(resource) < 2) continue;
                    // Keep a product's downstream chain when this is its only visible producer.
                    if (output && producers.getOrDefault(resource, Set.of()).size() == 1) continue;
                    split(edge);
                    counts.merge(resource, -1, Integer::sum);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private Node representative(Node node) {
        return node.grid == null ? node : node.grid.root;
    }

    private int rows(Node node) {
        return node.grid == null ? 1 : (node.grid.members.size() + 2) / 3;
    }

    private void buildGrids(Map<Node, Integer> degree) {
        var producers = producers();
        var machineEdges = machineEdges();
        grids.clear();
        nodes.forEach(node -> node.grid = null);
        for (Node machine : nodes) {
            if (!machine.machine()) continue;
            for (boolean output : List.of(false, true)) {
                List<Node> members = machineEdges.getOrDefault(machine, List.of()).stream()
                        .filter(edge -> edge.endpoint.output() == output && !edge.feedback)
                        .map(this::resource)
                        .filter(node -> degree.get(node) == 1
                                || output
                                        && node.layer == machine.layer + 1
                                        && producers
                                                        .getOrDefault(node, Set.of())
                                                        .size()
                                                == 1)
                        .sorted(Comparator.comparingInt(
                                        (Node node) -> node.endpoints.get(0).index())
                                .thenComparing(node -> node.key))
                        .toList();
                if (members.size() > 1) grids.add(new Grid(members));
            }
        }
    }

    private void applyConfirmation() {
        Set<String> uncertain = new HashSet<>();
        for (Connection connection : connections)
            if (!connection.confirmed()) uncertain.add(connection.target() + ":" + connection.resource());
        for (Edge edge : edges)
            if (edge.endpoints.stream()
                    .anyMatch(endpoint -> !endpoint.output()
                            && uncertain.contains(endpoint.step().reference() + ":"
                                    + endpoint.port().resource()))) edge.confirmed = false;
    }

    private void partition(Page page) {
        int offset = 0;
        for (Section section : Section.values()) {
            List<Step> steps = page.steps().stream()
                    .filter(step -> MaterialProcessDisplay.section(step, page.material())
                                    .orElse(Section.INTERNAL)
                            == section)
                    .toList();
            if (steps.isEmpty()) continue;
            Set<DataKey> references = new HashSet<>();
            steps.forEach(step -> references.add(step.reference()));
            var local = new Page(
                    page.title(),
                    page.material(),
                    steps,
                    connections.stream()
                            .filter(connection -> references.contains(connection.source())
                                    && references.contains(connection.target()))
                            .toList(),
                    page.notices(),
                    1,
                    1,
                    page.navigation());
            var graph = new MaterialProcessOverview(local, false);
            var copies = new HashMap<Node, Node>();
            for (Node original : graph.nodes) {
                var node = new Node("band:" + section.name() + ":" + original.key, original.section);
                node.endpoints.addAll(original.endpoints);
                node.steps.addAll(original.steps);
                node.auxiliaries.addAll(original.auxiliaries);
                node.x = original.x + offset;
                node.y = original.y + 28;
                node.layer = original.layer + offset / 52;
                node.returning = original.returning;
                node.copy = original.copy;
                copies.put(original, node);
                nodes.add(node);
            }
            for (Grid original : graph.grids)
                grids.add(new Grid(original.members.stream().map(copies::get).toList()));
            for (Edge original : graph.edges) {
                var edge = new Edge(copies.get(original.from), copies.get(original.to), original.endpoint);
                edge.endpoints.clear();
                edge.endpoints.addAll(original.endpoints);
                edge.confirmed = original.confirmed;
                edge.feedback = original.feedback;
                edges.add(edge);
            }
            graph.next.forEach((from, to) -> next.put(copies.get(from), copies.get(to)));
            if (graph.anchor != null && (anchor == null || section == Section.INTERNAL))
                anchor = copies.get(graph.anchor);
            int bandWidth = Math.max(72, graph.width);
            bands.add(new Band(section, offset, offset + bandWidth));
            offset += bandWidth;
        }
        var instances = new HashMap<String, Integer>();
        for (Node node : nodes) if (!node.machine()) instances.merge(node.port().identity(), 1, Integer::sum);
        for (Node node : nodes)
            if (!node.machine()) node.copy |= instances.get(node.port().identity()) > 1;
        applyConfirmation();
        route();
        width = offset;
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
        local.feedbackCopy = edge.feedback || original.returning || original.feedbackCopy;
        local.endpoints.addAll(edge.endpoints);
        nodes.add(local);
        original.endpoints.removeAll(edge.endpoints);
        if (edge.from == original) edge.from = local;
        else edge.to = local;
    }

    private static boolean primary(Edge edge) {
        Node machine = edge.from.machine() ? edge.from : edge.to;
        int first = machine.endpoints.stream()
                .filter(endpoint -> endpoint.output() == edge.endpoint.output())
                .filter(endpoint -> endpoint.step() == edge.endpoint.step())
                .mapToInt(Endpoint::index)
                .min()
                .orElse(edge.endpoint.index());
        return edge.endpoint.index() == first;
    }

    private boolean splitLongEdges() {
        boolean changed = false;
        var counts = new HashMap<Node, Integer>();
        for (Edge edge : edges) counts.merge(resource(edge), 1, Integer::sum);
        var columns = new java.util.TreeMap<Integer, java.util.NavigableMap<Integer, Node>>();
        for (Node node : nodes)
            columns.computeIfAbsent(node.x, ignored -> new java.util.TreeMap<>())
                    .put(node.y, node);
        for (Edge edge : edges) {
            Node resource = resource(edge);
            if (counts.get(resource) < 2) continue;
            int fromY = edge.from.grid == null ? edge.from.y : edge.from.grid.centerY();
            int toY = edge.to.grid == null ? edge.to.y : edge.to.grid.centerY();
            int distanceY = Math.abs(toY - fromY);
            if (!edge.feedback
                    && edge.to.layer - edge.from.layer <= 3
                    && distanceY <= 128
                    && (!edge.endpoint.output() || primary(edge) || distanceY <= 64)
                    && !crossesNode(edge, columns)) continue;
            split(edge);
            counts.merge(resource, -1, Integer::sum);
            changed = true;
        }
        return changed;
    }

    private boolean crossesNode(
            Edge edge, java.util.NavigableMap<Integer, java.util.NavigableMap<Integer, Node>> columns) {
        Node from = representative(edge.from), to = representative(edge.to);
        double sx = from.grid == null ? from.x + 12 : from.grid.right();
        double tx = to.grid == null ? to.x - 12 : to.grid.left();
        double sy = from.grid == null ? from.y : from.grid.centerY();
        double ty = to.grid == null ? to.y : to.grid.centerY();
        if (tx <= sx) return true;
        for (var column :
                columns.subMap((int) sx - 12, true, (int) tx + 12, true).entrySet()) {
            double left = Math.max(sx, column.getKey() - 12), right = Math.min(tx, column.getKey() + 12);
            if (left > right) continue;
            double ly = sy + (ty - sy) * (left - sx) / (tx - sx);
            double ry = sy + (ty - sy) * (right - sx) / (tx - sx);
            int low = (int) Math.ceil(Math.min(ly, ry) - 14), high = (int) Math.floor(Math.max(ly, ry) + 14);
            for (Node node : column.getValue().subMap(low, true, high, true).values())
                if (representative(node) != from && representative(node) != to) return true;
        }
        return false;
    }

    private List<Arc> arcs() {
        var result = new ArrayList<Arc>();
        // A local copy ends the visible dependency; retained facts must not reserve invisible columns.
        for (Edge edge : edges) result.add(new Arc(edge.from, edge.to, edge));
        result.sort(Comparator.comparing((Arc arc) -> arc.from.key)
                .thenComparing(arc -> arc.to.key)
                .thenComparing(arc -> arc.edge.endpoint.key()));
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
                    arc.edge.feedback = true;
                    resource(arc.edge).returning = true;
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
                    arc.to.layer = Math.max(arc.to.layer, node.layer + 1);
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
        buildGrids(degree);
        for (Grid grid : grids)
            for (Node member : grid.members)
                if (member != grid.root)
                    neighbors
                            .computeIfAbsent(grid.root, ignored -> new ArrayList<>())
                            .addAll(neighbors.getOrDefault(member, List.of()));
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
        ordered.sort(Comparator.<Edge>comparingInt(edge -> primary(edge) ? 0 : 1)
                .thenComparingInt(edge -> edge.endpoint.index())
                .thenComparingInt(edge -> degree.get(edge.from) + degree.get(edge.to))
                .thenComparingInt(edge -> -before.getOrDefault(edge.from, 0) - after.getOrDefault(edge.to, 0))
                .thenComparing(edge -> edge.endpoint.key()));
        for (Edge edge : ordered)
            if (!edge.feedback
                    && primary(edge)
                    && edge.to.layer == edge.from.layer + 1
                    && representative(edge.from) == edge.from
                    && representative(edge.to) == edge.to
                    && !next.containsKey(edge.from)
                    && !previous.containsKey(edge.to)) {
                next.put(edge.from, edge.to);
                previous.put(edge.to, edge.from);
            }
        var starts = nodes.stream()
                .filter(node -> !previous.containsKey(node))
                .filter(node -> representative(node) == node)
                .sorted(Comparator.<Node>comparingInt(node -> -chainLength(node))
                        .thenComparingInt(node -> node.layer)
                        .thenComparing(node -> family(node, neighbors))
                        .thenComparing(node -> node.key))
                .toList();
        var lanes = new HashMap<Node, Integer>();
        var chains = new ArrayList<List<Node>>();
        for (Node start : starts) {
            var chain = new ArrayList<Node>();
            for (Node node = start; node != null; node = next.get(node)) chain.add(node);
            chains.add(chain);
        }
        var chainRows = new HashMap<Node, Integer>();
        for (List<Node> chain : chains) {
            int size = chain.stream().mapToInt(this::rows).max().orElse(1);
            chain.forEach(node -> chainRows.put(node, size));
        }
        var order = new ArrayList<Integer>();
        for (int index = 0; index < chains.size(); index++) order.add(index);
        for (int pass = 0; pass < 4; pass++) {
            var prior = new HashMap<>(lanes);
            if (pass > 0)
                order.sort(Comparator.<Integer>comparingDouble(index -> chains.get(index).stream()
                                .flatMap(node -> neighbors.getOrDefault(node, List.of()).stream())
                                .filter(prior::containsKey)
                                .mapToInt(prior::get)
                                .average()
                                .orElse(0))
                        .thenComparingInt(Integer::intValue));
            lanes.clear();
            var occupied = new HashMap<Integer, Occupancy>();
            for (int index : order) {
                List<Node> chain = chains.get(index);
                double center = chain.stream()
                        .flatMap(node -> neighbors.getOrDefault(node, List.of()).stream())
                        .filter(node -> !chain.contains(node))
                        .filter(node -> lanes.containsKey(node) || prior.containsKey(node))
                        .mapToInt(node -> lanes.containsKey(node) ? lanes.get(node) : prior.get(node))
                        .average()
                        .orElse(0);
                var cells = new HashMap<Integer, Set<Integer>>();
                int first = chain.stream().mapToInt(node -> node.layer).min().orElse(0);
                int last = chain.stream().mapToInt(node -> node.layer).max().orElse(first);
                int blockRows = chain.stream().mapToInt(this::rows).max().orElse(1);
                // Reserve the whole recipe block, including the space between its icons.
                for (int layer = first; layer <= last; layer++)
                    for (int row = 0; row < blockRows; row++)
                        cells.computeIfAbsent(layer, ignored -> new HashSet<>()).add(row);
                int desired = (int) Math.round(center), lane = desired;
                for (int distance = 0; ; distance++) {
                    if (fits(cells, occupied, desired + distance)) {
                        lane = desired + distance;
                        break;
                    }
                    if (fits(cells, occupied, desired - distance)) {
                        lane = desired - distance;
                        break;
                    }
                }
                for (var cell : cells.entrySet())
                    for (int offset : cell.getValue())
                        occupied.computeIfAbsent(cell.getKey(), ignored -> new Occupancy())
                                .add(lane + offset);
                for (Node node : chain) lanes.put(node, lane);
            }
        }
        int minimum = lanes.values().stream().mapToInt(Integer::intValue).min().orElse(0);
        for (Node node : nodes) {
            Node root = representative(node);
            int index = node.grid == null ? 0 : node.grid.members.indexOf(node);
            node.y = 24 + (lanes.get(root) - minimum) * 32 + (chainRows.get(root) - rows(root)) * 12 + index / 3 * 24;
        }
        packComponents();
    }

    private record Block(List<Node> members, int width, int height) {}

    private record Shelf(int top, int height, int right) {}

    private void packComponents() {
        var neighbors = new HashMap<Node, List<Node>>();
        for (Edge edge : edges) {
            neighbors.computeIfAbsent(edge.from, ignored -> new ArrayList<>()).add(edge.to);
            neighbors.computeIfAbsent(edge.to, ignored -> new ArrayList<>()).add(edge.from);
        }
        var visited = new HashSet<Node>();
        var blocks = new ArrayList<Block>();
        for (Node start : nodes) {
            if (!visited.add(start)) continue;
            var members = new ArrayList<Node>();
            var queue = new ArrayDeque<Node>();
            queue.add(start);
            while (!queue.isEmpty()) {
                Node node = queue.removeFirst();
                members.add(node);
                for (Node neighbor : neighbors.getOrDefault(node, List.of()))
                    if (visited.add(neighbor)) queue.addLast(neighbor);
            }
            var widths = new java.util.TreeMap<Integer, Integer>();
            for (Node node : members) {
                Node root = representative(node);
                int size = root.grid == null ? 24 : Math.min(3, root.grid.members.size()) * 24;
                widths.merge(root.layer, size, Math::max);
            }
            var positions = new HashMap<Integer, Integer>();
            int x = 12;
            for (var column : widths.entrySet()) {
                positions.put(column.getKey(), x);
                x += column.getValue() + 20;
            }
            // Remove empty strips without changing a grid's cell spacing or a chain's alignment.
            var intervals = members.stream()
                    .map(node -> new int[] {node.y - 14, node.y + 14})
                    .sorted(Comparator.comparingInt(interval -> interval[0]))
                    .toList();
            var shifts = new java.util.TreeMap<Integer, Integer>();
            int bottom = intervals.get(0)[0], removed = 0;
            for (int[] interval : intervals) {
                removed += Math.max(0, interval[0] - bottom - 4);
                shifts.put(interval[0], removed);
                bottom = Math.max(bottom, interval[1]);
            }
            for (Node node : members) {
                Node root = representative(node);
                int index = node.grid == null ? 0 : node.grid.members.indexOf(node);
                node.x = positions.get(root.layer) + index % 3 * 24;
                node.y -= shifts.floorEntry(node.y).getValue();
            }
            int top = members.stream().mapToInt(node -> node.y - 14).min().orElseThrow();
            int width = members.stream().mapToInt(node -> node.x + 12).max().orElseThrow();
            int height = members.stream().mapToInt(node -> node.y + 14).max().orElseThrow() - top;
            members.forEach(node -> node.y -= top);
            blocks.add(new Block(members, width, height));
        }
        blocks.sort(Comparator.<Block>comparingInt(block -> -block.height)
                .thenComparingInt(block -> -block.width)
                .thenComparing(block -> block.members.get(0).key));
        long area = blocks.stream()
                .mapToLong(block -> (long) (block.width + 16) * (block.height + 16))
                .sum();
        int target = Math.max(blocks.stream().mapToInt(Block::width).max().orElse(0), (int) Math.ceil(Math.sqrt(area)));
        var shelves = new ArrayList<Shelf>();
        int bottom = 10;
        for (Block block : blocks) {
            int selected = -1;
            for (int index = 0; index < shelves.size(); index++) {
                Shelf shelf = shelves.get(index);
                if (block.height <= shelf.height && shelf.right + block.width <= target) {
                    selected = index;
                    break;
                }
            }
            if (selected < 0) {
                selected = shelves.size();
                shelves.add(new Shelf(bottom, block.height, 0));
                bottom += block.height + 16;
            }
            Shelf shelf = shelves.get(selected);
            block.members.forEach(node -> {
                node.x += shelf.right + 12;
                node.y += shelf.top;
            });
            shelves.set(selected, new Shelf(shelf.top, shelf.height, shelf.right + block.width + 16));
        }
    }

    private static boolean fits(Map<Integer, Set<Integer>> cells, Map<Integer, Occupancy> occupied, int lane) {
        for (var cell : cells.entrySet()) {
            Occupancy values = occupied.get(cell.getKey());
            if (values != null) for (int offset : cell.getValue()) if (values.contains(lane + offset)) return false;
        }
        return true;
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
            edge.path = List.of(
                    new Point(
                            from.grid == null ? from.x + 12 : from.grid.right(),
                            from.grid == null ? from.y : from.grid.centerY()),
                    new Point(
                            to.grid == null ? to.x - 12 : to.grid.left(), to.grid == null ? to.y : to.grid.centerY()));
        }
        width = nodes.stream().mapToInt(node -> node.x + 24).max().orElse(48);
        height = nodes.stream().mapToInt(node -> node.y + 24).max().orElse(48);
    }
}
