package gregtech.engine.material;

import gregtech.api.addon.material.DataKey;
import java.util.*;

/**
 * Immutable semantic process graph for one published material generation.
 *
 * <p>The graph owns no native recipe or world-generation objects. Queries traverse only their
 * published ports and return immutable rows for a viewer adapter to project, with optional query budgets.</p>
 */
public final class MaterialProcessGraph {
    private static final System.Logger LOGGER = System.getLogger("gt6ce.material.process");

    /** One transaction boundary with the form, tag and active worldgen revisions used by the rows. */
    public record Snapshot(
            MaterialPublication.Stamp publication,
            long sessionGeneration,
            long formRevision,
            long tagGeneration,
            long worldgenRevision) {
        public Snapshot {
            Objects.requireNonNull(publication);
            if (sessionGeneration <= 0 || formRevision < 0 || tagGeneration < 0 || worldgenRevision < 0)
                throw new IllegalArgumentException("Invalid process graph snapshot");
        }
    }

    public enum Direction {
        SOURCES,
        USES,
        BOTH
    }

    public enum Role {
        CONSUMED,
        CATALYST,
        PRESENCE,
        PRIMARY,
        BYPRODUCT,
        UNKNOWN
    }

    public enum AmountUnit {
        ITEM,
        MATERIAL_UNIT,
        MILLIBUCKET,
        UNKNOWN
    }

    /** Resource identity keeps material, form, phase and native selector/state separate. */
    public record Resource(DataKey key, String material, String form, String phase, String state) {
        public Resource {
            Objects.requireNonNull(key);
            requireText(material, "material");
            requireText(form, "form");
            requireText(phase, "phase");
            requireText(state, "state");
        }
    }

    public record Amount(long value, AmountUnit unit) {
        public Amount {
            if (value < 0) throw new IllegalArgumentException("Negative process amount");
            Objects.requireNonNull(unit);
        }

        public static Amount unknown() {
            return new Amount(0, AmountUnit.UNKNOWN);
        }
    }

    public record Port(
            Resource resource, Role role, Amount amount, OptionalInt chance, boolean main, List<String> details) {
        public Port {
            Objects.requireNonNull(resource);
            Objects.requireNonNull(role);
            Objects.requireNonNull(amount);
            Objects.requireNonNull(chance);
            if (chance.isPresent() && (chance.getAsInt() < 1 || chance.getAsInt() > 10_000))
                throw new IllegalArgumentException("Invalid process chance");
            details = List.copyOf(details);
        }

        public boolean output() {
            return role == Role.PRIMARY || role == Role.BYPRODUCT || role == Role.UNKNOWN;
        }
    }

    public record Step(
            DataKey reference,
            String name,
            List<Port> inputs,
            List<Port> outputs,
            List<String> details,
            Optional<DataKey> specialProcess,
            boolean resolved) {
        public Step {
            Objects.requireNonNull(reference);
            requireText(name, "process step name");
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
            var validInputs = inputs.stream().filter(port -> !port.output()).toList();
            var validOutputs = outputs.stream().filter(Port::output).toList();
            if (validInputs.size() != inputs.size() || validOutputs.size() != outputs.size()) {
                LOGGER.log(
                        System.Logger.Level.WARNING,
                        "Discarded ports on the wrong side of process step {0}",
                        reference);
                resolved = false;
            }
            inputs = validInputs;
            outputs = validOutputs;
            details = List.copyOf(details);
            Objects.requireNonNull(specialProcess);
        }

        public Step(
                DataKey reference,
                String name,
                List<Port> inputs,
                List<Port> outputs,
                List<String> details,
                boolean resolved) {
            this(reference, name, inputs, outputs, details, Optional.empty(), resolved);
        }
    }

    public record Edge(DataKey source, DataKey target, DataKey resource, boolean confirmed) {
        public Edge {
            Objects.requireNonNull(source);
            Objects.requireNonNull(target);
            Objects.requireNonNull(resource);
        }
    }

    /** Exact resource incidence; each source/target pair is implicit instead of allocated as an edge. */
    public record Flow(DataKey resource, List<DataKey> sources, List<DataKey> targets) {
        public Flow {
            Objects.requireNonNull(resource);
            sources = List.copyOf(sources);
            targets = List.copyOf(targets);
        }

        public long connectionCount() {
            return (long) sources.size() * targets.size();
        }
    }

    public record Budgets(int maxDepth, int maxSteps, int maxPortsPerSide, int maxEdges) {
        public static final Budgets DEFAULT = new Budgets(8, 64, 64, 128);
        /** Explicit expansion budget; viewer pages use {@link MaterialProcessGraph#queryMaterial(DataKey)} instead. */
        public static final Budgets MATERIAL_PAGE =
                new Budgets(0, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

        public Budgets {
            if (maxDepth < 0 || maxSteps <= 0 || maxPortsPerSide <= 0 || maxEdges < 0)
                throw new IllegalArgumentException("Invalid process graph budget");
        }
    }

    public record Notice(String code, int omitted) {
        public Notice {
            requireText(code, "process notice code");
            if (omitted < 0) throw new IllegalArgumentException("Negative process notice count");
        }
    }

    public record Query(
            Snapshot snapshot,
            DataKey root,
            Direction direction,
            List<Step> steps,
            List<Edge> edges,
            List<Notice> notices,
            boolean truncated) {
        public Query {
            Objects.requireNonNull(snapshot);
            Objects.requireNonNull(root);
            Objects.requireNonNull(direction);
            steps = List.copyOf(steps);
            edges = List.copyOf(edges);
            notices = List.copyOf(notices);
        }
    }

    /** A complete material page whose storage follows port occurrences, not producer/consumer products. */
    public record MaterialQuery(
            Snapshot snapshot, DataKey root, List<Step> steps, List<Flow> flows, List<Notice> notices) {
        public MaterialQuery {
            Objects.requireNonNull(snapshot);
            Objects.requireNonNull(root);
            steps = List.copyOf(steps);
            flows = List.copyOf(flows);
            notices = List.copyOf(notices);
        }
    }

    private record IndexedStep(int order, Step step, boolean genericRecovery) {}

    private record Node(DataKey reference, int depth) {}

    private final Snapshot snapshot;
    private final Map<DataKey, IndexedStep> steps;
    private final Map<DataKey, Resource> resources;
    private final Map<String, List<Resource>> resourcesByMaterial;
    private final Map<DataKey, List<IndexedStep>> inputs;
    private final Map<DataKey, List<IndexedStep>> outputs;
    private final List<Notice> notices;

    private MaterialProcessGraph(Builder builder) {
        snapshot = builder.snapshot;
        steps = Map.copyOf(builder.steps);
        resources = Map.copyOf(builder.resources);
        var byMaterial = new HashMap<String, List<Resource>>();
        resources
                .values()
                .forEach(resource -> byMaterial
                        .computeIfAbsent(resource.material(), ignored -> new ArrayList<>())
                        .add(resource));
        var frozenByMaterial = new HashMap<String, List<Resource>>(byMaterial.size());
        byMaterial.forEach((material, values) -> frozenByMaterial.put(
                material,
                values.stream()
                        .sorted(Comparator.comparing(resource -> resource.key().toString()))
                        .toList()));
        resourcesByMaterial = Map.copyOf(frozenByMaterial);
        inputs = freezeOccurrences(builder.inputs);
        outputs = freezeOccurrences(builder.outputs);
        notices = List.copyOf(builder.notices);
    }

    public static Builder builder(Snapshot snapshot) {
        return new Builder(snapshot);
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public Optional<Step> step(DataKey reference) {
        IndexedStep indexed = steps.get(reference);
        return indexed == null ? Optional.empty() : Optional.of(indexed.step());
    }

    public List<Resource> resources() {
        return resources.values().stream()
                .sorted(Comparator.comparing(resource -> resource.key().toString()))
                .toList();
    }

    public List<Step> materialSteps(DataKey root) {
        Resource resource = resources.get(Objects.requireNonNull(root));
        return resource == null
                ? List.of()
                : selectMaterial(resource).stream().map(IndexedStep::step).toList();
    }

    /** Keeps every direct row and association without enumerating a resource's source/target cross-product. */
    public MaterialQuery queryMaterial(DataKey root) {
        Objects.requireNonNull(root);
        Resource resource = resources.get(root);
        if (resource == null) return new MaterialQuery(snapshot, root, List.of(), List.of(), notices);
        var selected = selectMaterial(resource);
        var references = new HashSet<DataKey>();
        selected.forEach(indexed -> references.add(indexed.step().reference()));
        var flows = new ArrayList<Flow>();
        for (Resource local : materialResources(resource)) {
            var sources = outputs.getOrDefault(local.key(), List.of()).stream()
                    .map(indexed -> indexed.step().reference())
                    .filter(references::contains)
                    .toList();
            var targets = inputs.getOrDefault(local.key(), List.of()).stream()
                    .map(indexed -> indexed.step().reference())
                    .filter(references::contains)
                    .toList();
            if (!sources.isEmpty() && !targets.isEmpty()) flows.add(new Flow(local.key(), sources, targets));
        }
        var resultNotices = new ArrayList<>(notices);
        if (selected.isEmpty()
                && resultNotices.stream().noneMatch(notice -> notice.code().equals("no_both")))
            resultNotices.add(new Notice("no_both", 0));
        return new MaterialQuery(
                snapshot, root, selected.stream().map(IndexedStep::step).toList(), flows, resultNotices);
    }

    private List<Resource> materialResources(Resource root) {
        return root.material().equals("unknown")
                ? List.of(root)
                : resourcesByMaterial.getOrDefault(root.material(), List.of());
    }

    private List<IndexedStep> selectMaterial(Resource root) {
        var selected = new TreeSet<IndexedStep>(
                Comparator.comparing(IndexedStep::genericRecovery).thenComparingInt(IndexedStep::order));
        for (Resource resource : materialResources(root)) {
            selected.addAll(inputs.getOrDefault(resource.key(), List.of()));
            selected.addAll(sources(resource.key()));
        }
        return List.copyOf(selected);
    }

    /**
     * Traverse from one resource occurrence. The adjacency index is already published; this method
     * never enumerates unrelated steps or expands a selector into its member matrix.
     */
    public Query query(DataKey root, Direction direction, Budgets budgets) {
        Objects.requireNonNull(root);
        Objects.requireNonNull(direction);
        Objects.requireNonNull(budgets);
        if (!resources.containsKey(root))
            return new Query(snapshot, root, direction, List.of(), List.of(), notices, false);

        var queue = new ArrayDeque<Node>();
        var queued = new HashSet<DataKey>();
        boolean startsSources = direction != Direction.USES;
        boolean startsUses = direction != Direction.SOURCES;
        int omittedSteps = enqueue(root, startsSources, startsUses, 0, budgets, queue, queued);
        var visited = new LinkedHashMap<DataKey, Integer>();
        var edges = new LinkedHashMap<Edge, Boolean>();
        var truncations = new LinkedHashMap<String, Integer>();
        int omittedPorts = 0;
        int omittedEdges = 0;
        int omittedDepth = 0;
        boolean stopped = false;

        while (!queue.isEmpty() && !stopped) {
            Node node = queue.removeFirst();
            if (visited.containsKey(node.reference)) continue;
            if (visited.size() >= budgets.maxSteps()) {
                addNotice(truncations, "node_limit", 1);
                break;
            }
            visited.put(node.reference, node.depth);
            IndexedStep indexed = steps.get(node.reference);
            if (indexed == null) continue;
            omittedPorts += Math.max(0, indexed.step.inputs().size() - budgets.maxPortsPerSide());
            omittedPorts += Math.max(0, indexed.step.outputs().size() - budgets.maxPortsPerSide());
            if (node.depth >= budgets.maxDepth()) {
                if (direction != Direction.USES)
                    omittedDepth += indexed.step.inputs().stream()
                            .limit(budgets.maxPortsPerSide())
                            .map(port -> sources(port.resource().key()))
                            .mapToInt(occurrences -> unseen(occurrences, visited, queued))
                            .sum();
                if (direction != Direction.SOURCES)
                    omittedDepth += indexed.step.outputs().stream()
                            .limit(budgets.maxPortsPerSide())
                            .map(port -> inputs.getOrDefault(port.resource().key(), List.of()))
                            .mapToInt(occurrences -> unseen(occurrences, visited, queued))
                            .sum();
                continue;
            }

            boolean expandInputs = direction != Direction.USES;
            boolean expandOutputs = direction != Direction.SOURCES;
            if (expandInputs) {
                int available = Math.min(indexed.step.inputs().size(), budgets.maxPortsPerSide());
                for (int index = 0; index < available && !stopped; index++) {
                    Port port = indexed.step.inputs().get(index);
                    for (IndexedStep source : sources(port.resource().key())) {
                        Edge edge = new Edge(
                                source.step.reference(),
                                node.reference,
                                port.resource().key(),
                                confirmed(source, indexed));
                        if (edges.putIfAbsent(edge, Boolean.TRUE) == null && edges.size() > budgets.maxEdges()) {
                            edges.remove(edge);
                            omittedEdges++;
                            stopped = true;
                            break;
                        }
                        if (stopped) break;
                        if (!visited.containsKey(source.step.reference())
                                && !queued.contains(source.step.reference())) {
                            if (queued.size() >= budgets.maxSteps()) {
                                omittedSteps++;
                                continue;
                            }
                            queue.addLast(new Node(source.step.reference(), node.depth + 1));
                            queued.add(source.step.reference());
                        }
                    }
                }
            }
            if (expandOutputs && !stopped) {
                int available = Math.min(indexed.step.outputs().size(), budgets.maxPortsPerSide());
                for (int index = 0; index < available && !stopped; index++) {
                    Port port = indexed.step.outputs().get(index);
                    for (IndexedStep target :
                            inputs.getOrDefault(port.resource().key(), List.of())) {
                        Edge edge = new Edge(
                                node.reference,
                                target.step.reference(),
                                port.resource().key(),
                                confirmed(indexed, target));
                        if (edges.putIfAbsent(edge, Boolean.TRUE) == null && edges.size() > budgets.maxEdges()) {
                            edges.remove(edge);
                            omittedEdges++;
                            stopped = true;
                            break;
                        }
                        if (stopped) break;
                        if (!visited.containsKey(target.step.reference())
                                && !queued.contains(target.step.reference())) {
                            if (queued.size() >= budgets.maxSteps()) {
                                omittedSteps++;
                                continue;
                            }
                            queue.addLast(new Node(target.step.reference(), node.depth + 1));
                            queued.add(target.step.reference());
                        }
                    }
                }
            }
        }

        addNotice(truncations, "node_limit", omittedSteps);
        addNotice(truncations, "port_limit", omittedPorts);
        addNotice(truncations, "edge_limit", omittedEdges);
        addNotice(truncations, "depth_limit", omittedDepth);
        var resultSteps = visited.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> boundedStep(steps.get(entry.getKey()).step, budgets.maxPortsPerSide()))
                .toList();
        var retained = new HashMap<DataKey, Step>();
        resultSteps.forEach(step -> retained.put(step.reference(), step));
        var resultEdges = edges.keySet().stream()
                .filter(edge -> visited.containsKey(edge.source()) && visited.containsKey(edge.target()))
                .filter(edge -> retained.get(edge.source()).outputs().stream()
                        .anyMatch(port -> port.resource().key().equals(edge.resource())))
                .filter(edge -> retained.get(edge.target()).inputs().stream()
                        .anyMatch(port -> port.resource().key().equals(edge.resource())))
                .toList();
        var resultNotices = new ArrayList<>(notices);
        truncations.forEach((code, omitted) -> resultNotices.add(new Notice(code, omitted)));
        if (resultSteps.isEmpty()
                && resultNotices.stream()
                        .noneMatch(notice ->
                                notice.code().equals("no_" + direction.name().toLowerCase())))
            resultNotices.add(new Notice("no_" + direction.name().toLowerCase(), 0));
        return new Query(
                snapshot,
                root,
                direction,
                resultSteps,
                resultEdges,
                List.copyOf(resultNotices),
                !truncations.isEmpty());
    }

    /**
     * Return every step directly adjacent to any form of the root's material. Unlike {@link #query}, this
     * does not recurse through another material, so a common reagent cannot pull unrelated downstream chains
     * into the page. Internal form conversions, immediate sources and immediate uses share this one result.
     */
    public Query queryMaterial(DataKey root, Budgets budgets) {
        Objects.requireNonNull(root);
        Objects.requireNonNull(budgets);
        Resource rootResource = resources.get(root);
        if (rootResource == null)
            return new Query(snapshot, root, Direction.BOTH, List.of(), List.of(), notices, false);

        var selected = selectMaterial(rootResource);
        var retained = new ArrayList<IndexedStep>(Math.min(selected.size(), budgets.maxSteps()));
        int omittedSteps = 0;
        for (IndexedStep indexed : selected) {
            if (retained.size() >= budgets.maxSteps()) {
                omittedSteps++;
                continue;
            }
            retained.add(indexed);
        }

        int omittedPorts = 0;
        for (IndexedStep indexed : retained) {
            omittedPorts += Math.max(0, indexed.step.inputs().size() - budgets.maxPortsPerSide());
            omittedPorts += Math.max(0, indexed.step.outputs().size() - budgets.maxPortsPerSide());
        }
        var resultSteps = retained.stream()
                .map(indexed -> boundedStep(indexed.step, budgets.maxPortsPerSide()))
                .toList();
        var retainedByReference = new HashMap<DataKey, Step>();
        resultSteps.forEach(step -> retainedByReference.put(step.reference(), step));
        var retainedInputs = new HashMap<DataKey, Set<DataKey>>();
        resultSteps.forEach(step -> {
            var keys = new HashSet<DataKey>();
            step.inputs().forEach(port -> keys.add(port.resource().key()));
            retainedInputs.put(step.reference(), keys);
        });

        var resultEdges = new LinkedHashSet<Edge>();
        int omittedEdges = 0;
        for (IndexedStep source : retained) {
            for (Port port : retainedByReference.get(source.step.reference()).outputs()) {
                if (!port.resource().material().equals(rootResource.material())) continue;
                for (IndexedStep target : inputs.getOrDefault(port.resource().key(), List.of())) {
                    if (!retainedInputs
                            .getOrDefault(target.step.reference(), Set.of())
                            .contains(port.resource().key())) continue;
                    Edge edge = new Edge(
                            source.step.reference(),
                            target.step.reference(),
                            port.resource().key(),
                            confirmed(source, target));
                    if (resultEdges.contains(edge)) continue;
                    if (resultEdges.size() >= budgets.maxEdges()) {
                        omittedEdges++;
                        continue;
                    }
                    resultEdges.add(edge);
                }
            }
        }

        var truncations = new LinkedHashMap<String, Integer>();
        addNotice(truncations, "node_limit", omittedSteps);
        addNotice(truncations, "port_limit", omittedPorts);
        addNotice(truncations, "edge_limit", omittedEdges);
        var resultNotices = new ArrayList<>(notices);
        truncations.forEach((code, omitted) -> resultNotices.add(new Notice(code, omitted)));
        if (resultSteps.isEmpty()
                && resultNotices.stream().noneMatch(notice -> notice.code().equals("no_both")))
            resultNotices.add(new Notice("no_both", 0));
        return new Query(
                snapshot,
                root,
                Direction.BOTH,
                resultSteps,
                List.copyOf(resultEdges),
                List.copyOf(resultNotices),
                !truncations.isEmpty());
    }

    private int enqueue(
            DataKey root,
            boolean sources,
            boolean uses,
            int depth,
            Budgets budgets,
            ArrayDeque<Node> queue,
            Set<DataKey> queued) {
        int omitted = 0;
        for (List<IndexedStep> starts : List.of(
                sources ? sources(root) : List.<IndexedStep>of(),
                uses ? inputs.getOrDefault(root, List.of()) : List.<IndexedStep>of())) {
            for (int index = 0; index < starts.size(); index++) {
                if (queued.size() >= budgets.maxSteps()) {
                    omitted += starts.size() - index;
                    break;
                }
                IndexedStep start = starts.get(index);
                if (queued.add(start.step.reference())) queue.addLast(new Node(start.step.reference(), depth));
            }
        }
        return omitted;
    }

    private Step boundedStep(Step step, int maxPorts) {
        if (step.inputs().size() <= maxPorts && step.outputs().size() <= maxPorts) return step;
        return new Step(
                step.reference(),
                step.name(),
                boundedPorts(step.inputs(), maxPorts),
                boundedPorts(step.outputs(), maxPorts),
                step.details(),
                step.specialProcess(),
                step.resolved());
    }

    private List<IndexedStep> sources(DataKey resource) {
        return outputs.getOrDefault(resource, List.of()).stream()
                .filter(indexed -> !indexed.genericRecovery())
                .toList();
    }

    private static List<Port> boundedPorts(List<Port> ports, int maxPorts) {
        return ports.size() <= maxPorts ? ports : List.copyOf(ports.subList(0, maxPorts));
    }

    private static boolean confirmed(IndexedStep source, IndexedStep target) {
        return source.step.resolved() && target.step.resolved();
    }

    private static int unseen(List<IndexedStep> occurrences, Map<DataKey, Integer> visited, Set<DataKey> queued) {
        int count = 0;
        for (IndexedStep occurrence : occurrences)
            if (!visited.containsKey(occurrence.step.reference()) && !queued.contains(occurrence.step.reference()))
                count++;
        return count;
    }

    private static void addNotice(Map<String, Integer> notices, String code, int count) {
        if (count > 0) notices.merge(code, count, Math::addExact);
    }

    private static Map<DataKey, List<IndexedStep>> freezeOccurrences(Map<DataKey, List<IndexedStep>> source) {
        var frozen = new HashMap<DataKey, List<IndexedStep>>(source.size());
        source.forEach((key, values) -> frozen.put(key, List.copyOf(values)));
        return Map.copyOf(frozen);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + name);
    }

    public static final class Builder {
        private final Snapshot snapshot;
        private final Map<DataKey, IndexedStep> steps = new LinkedHashMap<>();
        private final Map<DataKey, Resource> resources = new LinkedHashMap<>();
        private final Map<DataKey, List<IndexedStep>> inputs = new HashMap<>();
        private final Map<DataKey, List<IndexedStep>> outputs = new HashMap<>();
        private final List<Notice> notices = new ArrayList<>();

        private Builder(Snapshot snapshot) {
            this.snapshot = Objects.requireNonNull(snapshot);
        }

        public Builder notice(String code, int omitted) {
            notices.add(new Notice(code, omitted));
            return this;
        }

        public Builder add(Step step) {
            return add(step, false);
        }

        /** Generic recovery remains a use of its input, without advertising every reclaimable item as a source. */
        public Builder add(Step step, boolean genericRecovery) {
            Objects.requireNonNull(step);
            if (steps.containsKey(step.reference())) {
                LOGGER.log(System.Logger.Level.WARNING, "Discarded duplicate process step {0}", step.reference());
                return notice("invalid_process_data", 1);
            }
            // Validate the whole row before changing any index; a rejected row must leave no partial resources.
            var additions = new HashMap<DataKey, Resource>();
            for (List<Port> ports : List.of(step.inputs(), step.outputs()))
                for (Port port : ports) {
                    Resource resource = port.resource();
                    Resource previous = additions.putIfAbsent(resource.key(), resource);
                    if (previous == null) previous = resources.get(resource.key());
                    if (previous != null && !previous.equals(resource)) {
                        LOGGER.log(
                                System.Logger.Level.WARNING,
                                "Discarded process step {0}: conflicting resource identity {1}",
                                step.reference(),
                                resource.key());
                        return notice("invalid_process_data", 1);
                    }
                }
            int order = steps.size();
            IndexedStep indexed = new IndexedStep(order, step, genericRecovery);
            steps.put(step.reference(), indexed);
            resources.putAll(additions);
            var inputResources = new HashSet<DataKey>();
            for (Port port : step.inputs()) {
                if (inputResources.add(port.resource().key()))
                    inputs.computeIfAbsent(port.resource().key(), ignored -> new ArrayList<>())
                            .add(indexed);
            }
            var outputResources = new HashSet<DataKey>();
            for (Port port : step.outputs()) {
                if (outputResources.add(port.resource().key()))
                    outputs.computeIfAbsent(port.resource().key(), ignored -> new ArrayList<>())
                            .add(indexed);
            }
            return this;
        }

        public MaterialProcessGraph build() {
            return new MaterialProcessGraph(this);
        }
    }
}
