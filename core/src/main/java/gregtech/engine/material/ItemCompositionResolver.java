package gregtech.engine.material;

import java.util.*;

/**
 * Resolves composition candidates by source priority. The first priority with surviving
 * candidates wins as a whole; component and recovery authorization never mix across priorities.
 */
public final class ItemCompositionResolver {
    private ItemCompositionResolver() {}

    public static <K> Map<K, ItemComposition> resolve(
            Map<K, ? extends Collection<ItemCompositionCandidate>> submissions) {
        return resolve(submissions, true);
    }

    /** Resolves candidates while retaining every candidate and the reason it did not win. */
    public static <K> Map<K, ItemCompositionResolution> resolveDetailed(
            Map<K, ? extends Collection<ItemCompositionCandidate>> submissions) {
        return resolveDetailed(submissions, true);
    }

    /** Registration may precede an explicitly named override target; the final snapshot validates it. */
    static <K> Map<K, ItemComposition> resolveLenient(
            Map<K, ? extends Collection<ItemCompositionCandidate>> submissions) {
        return resolve(submissions, false);
    }

    static <K> Map<K, ItemCompositionResolution> resolveDetailedLenient(
            Map<K, ? extends Collection<ItemCompositionCandidate>> submissions) {
        return resolveDetailed(submissions, false);
    }

    private static <K> Map<K, ItemComposition> resolve(
            Map<K, ? extends Collection<ItemCompositionCandidate>> submissions, boolean requireReplacementTargets) {
        var detailed = resolveDetailed(submissions, requireReplacementTargets);
        var ordered = new LinkedHashMap<K, ItemComposition>();
        detailed.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(String::valueOf)))
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue().selected()));
        return Collections.unmodifiableMap(ordered);
    }

    private static <K> Map<K, ItemCompositionResolution> resolveDetailed(
            Map<K, ? extends Collection<ItemCompositionCandidate>> submissions, boolean requireReplacementTargets) {
        var ordered = new LinkedHashMap<K, ItemCompositionResolution>();
        submissions.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(String::valueOf)))
                .forEach(entry -> ordered.put(
                        entry.getKey(), resolveItem(entry.getKey(), entry.getValue(), requireReplacementTargets)));
        return Collections.unmodifiableMap(ordered);
    }

    private static ItemCompositionResolution resolveItem(
            Object item, Collection<ItemCompositionCandidate> submitted, boolean requireReplacementTargets) {
        var candidates = new ArrayList<>(submitted);
        candidates.sort(Comparator.comparing(ItemCompositionCandidate::sourceId)
                .thenComparing(candidate -> candidate.composition().source()));
        var groups = new TreeMap<Integer, List<ItemCompositionCandidate>>();
        var bySource = new HashMap<String, ItemCompositionCandidate>();
        for (var candidate : candidates) {
            var previous = bySource.putIfAbsent(candidate.sourceId(), candidate);
            if (previous != null) {
                throw new IllegalStateException("Duplicate item composition declaration "
                        + candidate.sourceId() + " for " + item + " from "
                        + previous.composition().source() + " and "
                        + candidate.composition().source());
            }
            groups.computeIfAbsent(candidate.composition().sources().get(0).priority(), ignored -> new ArrayList<>())
                    .add(candidate);
        }
        for (var entry : groups.entrySet()) {
            var selected = resolveLevel(item, entry.getValue(), requireReplacementTargets);
            var decisions = new ArrayList<>(selected.decisions());
            for (var lower : groups.tailMap(entry.getKey(), false).values()) {
                for (var candidate : lower) {
                    decisions.add(new ItemCompositionDecision(
                            candidate.composition(),
                            ItemCompositionDecision.Status.LOWER_PRIORITY,
                            "priority "
                                    + candidate.composition().sources().get(0).priority()
                                    + " is lower than selected priority " + entry.getKey()));
                }
            }
            return new ItemCompositionResolution(selected.composition(), entry.getKey(), decisions);
        }
        throw new IllegalStateException("Item composition candidates resolved to no selected layer for " + item);
    }

    private static LevelResolution resolveLevel(
            Object item, List<ItemCompositionCandidate> level, boolean requireReplacementTargets) {
        var bySource = new HashMap<String, ItemCompositionCandidate>();
        for (var candidate : level) bySource.put(candidate.sourceId(), candidate);
        var replaced = new HashSet<String>();
        var overriddenBy = new HashMap<String, String>();
        for (var candidate : level) {
            String target = candidate.replacesSourceId();
            if (target == null) continue;
            var source = candidate.composition().sources().get(0);
            if (source.kind().priority() != 1)
                throw new IllegalStateException("Only an explicit-layer declaration may replace another: "
                        + candidate.sourceId() + " for " + item);
            var previous = overriddenBy.putIfAbsent(target, candidate.sourceId());
            if (previous != null)
                throw new IllegalStateException("Item composition declaration " + target + " for " + item
                        + " is replaced by both " + previous + " and " + candidate.sourceId());
            if (!bySource.containsKey(target)) {
                if (requireReplacementTargets) {
                    throw new IllegalStateException("Item composition override " + candidate.sourceId() + " for " + item
                            + " names missing declaration " + target);
                }
                continue;
            }
            replaced.add(target);
        }
        validateReplacementCycles(item, level);
        var winners = level.stream()
                .filter(candidate -> !replaced.contains(candidate.sourceId()))
                .toList();
        if (winners.isEmpty()) throw new IllegalStateException("Item composition overrides form a cycle for " + item);
        var first = winners.get(0).composition();
        for (int index = 1; index < winners.size(); index++) {
            var candidate = winners.get(index).composition();
            if (!sameContent(first, candidate)) {
                throw new IllegalStateException("Conflicting item compositions for " + item + ": "
                        + first.sources().get(0).description() + " provides " + first.components()
                        + " bulkRecovery=" + first.bulkRecoveryAllowed() + ", while "
                        + candidate.sources().get(0).description() + " provides " + candidate.components()
                        + " bulkRecovery=" + candidate.bulkRecoveryAllowed());
            }
        }
        var sources = winners.stream()
                .map(candidate -> withReplacementTrace(candidate, bySource))
                .sorted(Comparator.comparing(ItemCompositionSource::declarationId))
                .toList();
        var decisions = level.stream()
                .map(candidate -> {
                    String replacedBy = replacedBy(candidate, overriddenBy);
                    if (replacedBy != null) {
                        return new ItemCompositionDecision(
                                candidate.composition(),
                                ItemCompositionDecision.Status.REPLACED,
                                "replaced by " + replacedBy);
                    }
                    String reason = "selected at priority "
                            + candidate.composition().sources().get(0).priority();
                    if (winners.size() > 1) reason += " and merged with equivalent declarations";
                    return new ItemCompositionDecision(
                            candidate.composition(), ItemCompositionDecision.Status.SELECTED, reason);
                })
                .toList();
        return new LevelResolution(first.withSources(sources), decisions);
    }

    private static String replacedBy(ItemCompositionCandidate candidate, Map<String, String> overriddenBy) {
        for (var entry : overriddenBy.entrySet())
            if (entry.getKey().equals(candidate.sourceId())) return entry.getValue();
        return null;
    }

    private static ItemCompositionSource withReplacementTrace(
            ItemCompositionCandidate candidate, Map<String, ItemCompositionCandidate> bySource) {
        var source = candidate.composition().sources().get(0);
        if (candidate.replacesSourceId() == null) return source;
        var replacements = new ArrayList<String>();
        String target = candidate.replacesSourceId();
        while (target != null && !replacements.contains(target)) {
            replacements.add(target);
            var replaced = bySource.get(target);
            target = replaced == null ? null : replaced.replacesSourceId();
        }
        return new ItemCompositionSource(
                source.kind(),
                source.declarationId(),
                source.producer(),
                source.reference(),
                source.note(),
                replacements,
                source.materialProcessingAuthority());
    }

    private static void validateReplacementCycles(Object item, List<ItemCompositionCandidate> level) {
        var replacements = new HashMap<String, String>();
        for (var candidate : level)
            if (candidate.replacesSourceId() != null)
                replacements.put(candidate.sourceId(), candidate.replacesSourceId());
        var visiting = new HashSet<String>();
        var visited = new HashSet<String>();
        for (String source : replacements.keySet()) visit(item, source, replacements, visiting, visited);
    }

    private static void visit(
            Object item, String source, Map<String, String> replacements, Set<String> visiting, Set<String> visited) {
        if (visited.contains(source)) return;
        if (!visiting.add(source))
            throw new IllegalStateException("Item composition override cycle for " + item + " through " + source);
        String target = replacements.get(source);
        if (target != null) visit(item, target, replacements, visiting, visited);
        visiting.remove(source);
        visited.add(source);
    }

    private static boolean sameContent(ItemComposition left, ItemComposition right) {
        return Objects.equals(left.prefix(), right.prefix())
                && left.components().equals(right.components())
                && left.primaryMaterial().resolve() == right.primaryMaterial().resolve()
                && left.bulkRecoveryAllowed() == right.bulkRecoveryAllowed();
    }

    private record LevelResolution(ItemComposition composition, List<ItemCompositionDecision> decisions) {}
}
