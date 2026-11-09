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
        var result = new LinkedHashMap<K, ItemComposition>();
        for (var entry : submissions.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) continue;
            ItemComposition selected = resolveItem(entry.getKey(), entry.getValue());
            if (selected != null) result.put(entry.getKey(), selected);
        }
        var ordered = new LinkedHashMap<K, ItemComposition>();
        result.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(String::valueOf)))
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(ordered);
    }

    private static ItemComposition resolveItem(Object item, Collection<ItemCompositionCandidate> submitted) {
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
                        + previous.composition().source() + " and " + candidate.composition().source());
            }
            groups.computeIfAbsent(candidate.composition().sources().get(0).priority(), ignored -> new ArrayList<>())
                    .add(candidate);
        }
        for (var level : groups.values()) {
            var resolved = resolveLevel(item, level);
            if (resolved != null) return resolved;
        }
        return null;
    }

    private static ItemComposition resolveLevel(Object item, List<ItemCompositionCandidate> level) {
        var bySource = new HashMap<String, ItemCompositionCandidate>();
        for (var candidate : level) bySource.put(candidate.sourceId(), candidate);
        var replaced = new HashSet<String>();
        for (var candidate : level) {
            String target = candidate.replacesSourceId();
            if (target == null) continue;
            var source = candidate.composition().sources().get(0);
            if (source.kind().priority() != 1)
                throw new IllegalStateException("Only an explicit-layer declaration may replace another: "
                        + candidate.sourceId() + " for " + item);
            if (!bySource.containsKey(target))
                throw new IllegalStateException("Item composition override " + candidate.sourceId()
                        + " for " + item + " names missing declaration " + target);
            replaced.add(target);
        }
        validateReplacementCycles(item, level);
        var winners = level.stream()
                .filter(candidate -> !replaced.contains(candidate.sourceId()))
                .toList();
        if (winners.isEmpty())
            throw new IllegalStateException("Item composition overrides form a cycle for " + item);
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
                .flatMap(candidate -> candidate.composition().sources().stream())
                .sorted(Comparator.comparing(ItemCompositionSource::declarationId))
                .toList();
        return first.withSources(sources);
    }

    private static void validateReplacementCycles(Object item, List<ItemCompositionCandidate> level) {
        var replacements = new HashMap<String, String>();
        for (var candidate : level)
            if (candidate.replacesSourceId() != null) replacements.put(candidate.sourceId(), candidate.replacesSourceId());
        var visiting = new HashSet<String>();
        var visited = new HashSet<String>();
        for (String source : replacements.keySet()) visit(item, source, replacements, visiting, visited);
    }

    private static void visit(
            Object item,
            String source,
            Map<String, String> replacements,
            Set<String> visiting,
            Set<String> visited) {
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
}
