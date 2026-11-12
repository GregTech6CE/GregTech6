package gregtech.engine.material;

import gregtech.api.material.*;
import gregtech.content.data.MaterialPrefix;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime binding for the one effective item-composition snapshot.
 *
 * <p>Explicit producers submit candidates; tag producers replace a complete staged generation.
 * Effective traversal and single-item lookup read the same immutable snapshot.</p>
 */
public final class ItemMaterialRegistry {
    private static final Object LOCK = new Object();
    private static final Map<Item, List<ItemCompositionCandidate>> EXPLICIT_SUBMISSIONS = new IdentityHashMap<>();
    private static final Map<Item, ItemCompositionCandidate> LEGACY_SUBMISSIONS = new IdentityHashMap<>();
    private static final Map<Item, List<ItemCompositionCandidate>> TAG_SUBMISSIONS = new IdentityHashMap<>();
    private static final AtomicLong COMPOSITION_REVISION = new AtomicLong();
    private static boolean tagInputReady;
    private static boolean explicitClosed;
    private static long tagGeneration;

    private static final Map<Item, ItemComposition> EFFECTIVE = new IdentityHashMap<>();
    private static final Map<Item, ItemComposition> EXPLICIT = new IdentityHashMap<>();
    private static final Map<Item, ItemCompositionResolution> DIAGNOSTICS = new IdentityHashMap<>();
    private static Map<Item, ItemComposition> effectiveSnapshot = Map.of();
    private static long effectiveSnapshotRevision = Long.MIN_VALUE;

    public enum CompositionLookupState {
        PRESENT,
        ABSENT,
        TAG_INPUT_NOT_READY
    }

    public record CompositionState(long revision, long tagGeneration, boolean tagInputReady, boolean explicitClosed) {}

    private ItemMaterialRegistry() {}

    public static void register(Item item, @Nullable MaterialPrefix prefix, GTMaterial material) {
        boolean compositeHead = prefix == MaterialPrefix.toolHeadWrench
                || prefix == MaterialPrefix.toolHeadChainsaw
                || prefix == MaterialPrefix.toolHeadDrill
                || prefix == MaterialPrefix.toolHeadPickaxeGem;
        if (item != null && material != null && prefix != null && (prefix.hasEmptyForm() || compositeHead)) {
            var parts = MaterialChemistry.prefixMaterialWeights(material, prefix).stream()
                    .map(c -> MaterialComponent.of(c.material(), c.amount()))
                    .toList();
            String producer = compositeHead ? "GT6 OP tool-head components" : "GT6 OP ammunition components";
            if (!parts.isEmpty()) {
                ItemCompositionSource source = new ItemCompositionSource(
                        ItemCompositionSource.Kind.EXPLICIT,
                        "gt6ce:item-form/" + implementationId(item) + "/" + prefix.getName(),
                        producer,
                        producer,
                        "");
                submit(item, new ItemComposition(prefix, parts, material, List.of(source), true), null);
            }
            return;
        }
        register(item, prefix, material, prefix == null ? GTValues.U : prefix.getMaterialWeight());
    }

    public static void register(Item item, @Nullable MaterialPrefix prefix, GTMaterial material, long amount) {
        if (item != null && material != null && material.isValid() && amount > 0) {
            String prefixName = prefix == null ? "unprefixed" : prefix.getName();
            ItemCompositionSource source = new ItemCompositionSource(
                    ItemCompositionSource.Kind.EXPLICIT,
                    "gt6ce:item-form/" + implementationId(item) + "/" + prefixName,
                    "GT6 item form",
                    "GT6 item form",
                    "");
            submit(
                    item,
                    new ItemComposition(
                            prefix, List.of(MaterialComponent.of(material, amount)), material, List.of(source), true),
                    null);
        }
    }

    /**
     * Compatibility entry point for old addons. Repeated calls for one item retain the historical
     * last-write behavior; use {@link #registerDeclared(Item, ItemComposition)} for structured sources.
     */
    @Deprecated(forRemoval = false)
    public static void register(Item item, ItemComposition data) {
        submitLegacy(item, data);
    }

    /** Registers a composition whose single structured source already identifies its producer. */
    public static void registerDeclared(Item item, ItemComposition data) {
        submit(item, data, null);
    }

    public static void register(
            Item item, ItemComposition data, ItemCompositionSource.Kind kind, String declarationId, String producer) {
        register(item, data, kind, declarationId, producer, null);
    }

    public static void register(
            Item item,
            ItemComposition data,
            ItemCompositionSource.Kind kind,
            String declarationId,
            String producer,
            @Nullable String replacesSourceId) {
        register(
                item,
                data,
                kind,
                declarationId,
                producer,
                replacesSourceId,
                ItemCompositionSource.MaterialProcessingAuthority.DEFAULT);
    }

    public static void register(
            Item item,
            ItemComposition data,
            ItemCompositionSource.Kind kind,
            String declarationId,
            String producer,
            @Nullable String replacesSourceId,
            ItemCompositionSource.MaterialProcessingAuthority materialProcessingAuthority) {
        ItemCompositionSource source = new ItemCompositionSource(
                kind, declarationId, producer, sourceReference(data), "", List.of(), materialProcessingAuthority);
        submit(
                item,
                new ItemComposition(
                        data.prefix(),
                        data.components(),
                        data.primaryMaterial(),
                        List.of(source),
                        data.bulkRecoveryAllowed()),
                replacesSourceId);
    }

    /**
     * Replaces the complete external tag generation. Invalid or conflicting tag candidates fail
     * before the effective snapshot changes.
     */
    public static void publishTagCandidates(Map<Item, ? extends Collection<ItemComposition>> candidates) {
        var staged = new IdentityHashMap<Item, List<ItemCompositionCandidate>>();
        for (var entry : candidates.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            for (ItemComposition composition : entry.getValue()) {
                var source = new ItemCompositionSource(
                        ItemCompositionSource.Kind.FORM_TAG,
                        implementationId(entry.getKey()) + "/" + sourceSuffix(composition),
                        "loaded resource tags",
                        "",
                        composition.source());
                var tagged = new ItemComposition(
                        composition.prefix(),
                        composition.components(),
                        composition.primaryMaterial(),
                        List.of(source),
                        composition.bulkRecoveryAllowed());
                staged.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
                        .add(new ItemCompositionCandidate(tagged, null));
            }
        }
        boolean refreshForms;
        synchronized (LOCK) {
            boolean readinessChanged = !tagInputReady;
            var affected = new HashSet<Item>();
            affected.addAll(TAG_SUBMISSIONS.keySet());
            affected.addAll(staged.keySet());
            var resolved = new IdentityHashMap<Item, ItemComposition>();
            var diagnostics = new IdentityHashMap<Item, ItemCompositionResolution>();
            for (Item item : affected) {
                var itemCandidates = new ArrayList<ItemCompositionCandidate>();
                var explicit = EXPLICIT_SUBMISSIONS.get(item);
                if (explicit != null) itemCandidates.addAll(explicit);
                var legacy = LEGACY_SUBMISSIONS.get(item);
                if (legacy != null) itemCandidates.add(legacy);
                var tags = staged.get(item);
                if (tags != null) itemCandidates.addAll(tags);
                if (itemCandidates.isEmpty()) continue;
                var resolution = ItemCompositionResolver.resolveDetailed(Map.of(item, itemCandidates))
                        .get(item);
                if (resolution != null) {
                    resolved.put(item, resolution.selected());
                    diagnostics.put(item, resolution);
                }
            }
            boolean changed = false;
            for (Item item : affected) {
                ItemComposition previous = EFFECTIVE.get(item);
                ItemComposition replacement = resolved.get(item);
                var resolution = diagnostics.get(item);
                if (replacement == null) {
                    if (previous == null) continue;
                    changed = true;
                    EFFECTIVE.remove(item);
                    DIAGNOSTICS.remove(item);
                } else {
                    DIAGNOSTICS.put(item, resolution);
                    if (Objects.equals(previous, replacement)) continue;
                    changed = true;
                    EFFECTIVE.put(item, replacement);
                }
            }
            TAG_SUBMISSIONS.clear();
            TAG_SUBMISSIONS.putAll(staged);
            tagInputReady = true;
            if (changed || readinessChanged) COMPOSITION_REVISION.incrementAndGet();
            refreshForms = changed || readinessChanged;
        }
        if (refreshForms) MaterialFormContent.publish();
    }

    public static void clearTagCandidates() {
        publishTagCandidates(Map.of());
    }

    /**
     * Starts a new external-tag generation. Explicit definitions remain process-static, while the
     * previous session's tag candidates stop being observable until their replacement is published.
     */
    public static void beginTagGeneration() {
        boolean refresh;
        synchronized (LOCK) {
            long before = COMPOSITION_REVISION.get();
            boolean hadGeneration = tagInputReady || !TAG_SUBMISSIONS.isEmpty();
            var affected = new HashSet<>(TAG_SUBMISSIONS.keySet());
            TAG_SUBMISSIONS.clear();
            tagInputReady = false;
            tagGeneration++;
            for (Item item : affected) updateItemLocked(item, true);
            if (COMPOSITION_REVISION.get() == before && hadGeneration) COMPOSITION_REVISION.incrementAndGet();
            refresh = hadGeneration;
        }
        if (refresh) MaterialFormContent.publish();
    }

    /** Candidate caches depend on the complete effective composition snapshot. */
    public static long compositionRevision() {
        return COMPOSITION_REVISION.get();
    }

    public static CompositionState compositionState() {
        synchronized (LOCK) {
            return new CompositionState(COMPOSITION_REVISION.get(), tagGeneration, tagInputReady, explicitClosed);
        }
    }

    /** Validates deferred replacement targets after all built-in and recipe-side producers have submitted. */
    public static void validateExplicitSnapshot() {
        synchronized (LOCK) {
            for (var item : EXPLICIT_SUBMISSIONS.keySet()) updateItemLocked(item, true);
            explicitClosed = true;
        }
    }

    /** Explicit-layer result, retained for source diagnostics and migration compatibility. */
    public static Optional<ItemComposition> explicit(Item item) {
        synchronized (LOCK) {
            return Optional.ofNullable(EXPLICIT.get(item));
        }
    }

    /** Effective composition, including a tag-derived fallback. */
    public static Optional<ItemComposition> base(Item item) {
        synchronized (LOCK) {
            return Optional.ofNullable(EFFECTIVE.get(item));
        }
    }

    /** Effective composition whose winner is not an external resource-tag import. */
    public static Optional<ItemComposition> declared(Item item) {
        return base(item)
                .filter(composition -> composition.sources().stream()
                        .noneMatch(source -> source.kind() == ItemCompositionSource.Kind.FORM_TAG));
    }

    /** Complete effective arbitration record, including replaced and lower-priority candidates. */
    public static Optional<ItemCompositionResolution> compositionDiagnosis(Item item) {
        synchronized (LOCK) {
            return Optional.ofNullable(DIAGNOSTICS.get(item));
        }
    }

    /** Distinguishes a missing composition from a snapshot whose tag input has not been published yet. */
    public static CompositionLookupState compositionStatus(Item item) {
        synchronized (LOCK) {
            if (EFFECTIVE.containsKey(item)) return CompositionLookupState.PRESENT;
            return tagInputReady ? CompositionLookupState.ABSENT : CompositionLookupState.TAG_INPUT_NOT_READY;
        }
    }

    /** Whether the complete external-tag generation has been published at least once this session. */
    public static boolean tagInputReady() {
        synchronized (LOCK) {
            return tagInputReady;
        }
    }

    /** Stable by registry id where possible; this is the authoritative bulk traversal view. */
    public static Map<Item, ItemComposition> entries() {
        synchronized (LOCK) {
            long revision = COMPOSITION_REVISION.get();
            if (effectiveSnapshotRevision != revision) {
                effectiveSnapshot = stableSnapshot(EFFECTIVE);
                effectiveSnapshotRevision = revision;
            }
            return effectiveSnapshot;
        }
    }

    public static Optional<ItemComposition> get(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        var data = base(stack.getItem()).orElse(null);
        if (data == null || !stack.isDamaged()) return Optional.ofNullable(data);
        int maximum = stack.getMaxDamage();
        if (maximum <= 0) return Optional.of(data);
        int remaining = Math.max(0, Math.min(maximum, maximum - stack.getDamageValue()));
        return Optional.of(ItemCompositionScaling.scale(data, remaining, maximum));
    }

    /** GT6 {@code OM.anydata_} admission for the melting crucible. */
    public static boolean canMelt(ItemStack stack) {
        var data = get(stack);
        if (data.isEmpty() || data.get().components().isEmpty()) return false;
        if (hasStoredContents(stack)) return false;
        return !(stack.getItem() instanceof net.minecraft.world.item.BucketItem)
                || stack.is(net.minecraft.world.item.Items.BUCKET);
    }

    /**
     * Why a stack may or may not enter a bulk recovery recipe.
     *
     * <p>Composition availability, stored contents and container state are recovery facts. GT6 has
     * no item-level recovery permission, so actual damaged compositions remain eligible.</p>
     */
    public enum RecoveryRejection {
        ELIGIBLE,
        NO_COMPOSITION,
        NO_COMPONENTS,
        /** @deprecated No longer emitted; retained for binary and source compatibility. */
        @Deprecated(forRemoval = false)
        NOT_BULK_RECOVERABLE,
        /** @deprecated Damaged compositions are eligible and scaled by their process. */
        @Deprecated(forRemoval = false)
        DAMAGED,
        STORED_CONTENTS,
        UNSUPPORTED_CONTAINER
    }

    public static RecoveryRejection recoveryRejection(ItemStack stack) {
        if (stack.isEmpty()) return RecoveryRejection.NO_COMPOSITION;
        var data = get(stack).orElse(null);
        if (data == null) return RecoveryRejection.NO_COMPOSITION;
        if (data.components().isEmpty()) return RecoveryRejection.NO_COMPONENTS;
        if (hasStoredContents(stack)) return RecoveryRejection.STORED_CONTENTS;
        if (stack.getItem() instanceof net.minecraft.world.item.BucketItem
                && !stack.is(net.minecraft.world.item.Items.BUCKET)) return RecoveryRejection.UNSUPPORTED_CONTAINER;
        return RecoveryRejection.ELIGIBLE;
    }

    /** Item-side recovery admission; the concrete recipe still owns its form and process checks. */
    public static boolean canRecover(ItemStack stack) {
        return recoveryRejection(stack) == RecoveryRejection.ELIGIBLE;
    }

    /** Whether item data should replace a material cooking rule; descriptive compositions do not. */
    public static boolean hasMaterialProcessingRule(ItemStack stack) {
        if (stack.getItem() instanceof MaterialFormItem) return true;
        synchronized (LOCK) {
            var composition = EFFECTIVE.get(stack.getItem());
            return composition != null
                    && composition.sources().stream().anyMatch(ItemCompositionSource::allowsMaterialProcessingRule);
        }
    }

    public static boolean hasStoredContents(ItemStack stack) {
        return ItemMaterialRuntimeFacts.hasStoredContents(stack);
    }

    private static void submit(Item item, ItemComposition data, @Nullable String replacesSourceId) {
        if (item == null || data == null) return;
        if (data.sources().size() != 1)
            throw new IllegalArgumentException(
                    "Submitted item composition must have exactly one source: " + data.source());
        var candidate = new ItemCompositionCandidate(data, replacesSourceId);
        synchronized (LOCK) {
            if (explicitClosed) throw new IllegalStateException("Explicit item composition registration is closed");
            var candidates = EXPLICIT_SUBMISSIONS.computeIfAbsent(item, ignored -> new ArrayList<>());
            var previous = candidates.stream()
                    .filter(existing -> existing.sourceId().equals(candidate.sourceId()))
                    .findFirst()
                    .orElse(null);
            if (previous != null) {
                if (sameCandidate(previous, candidate)) return;
                throw new IllegalStateException("Duplicate item composition declaration " + candidate.sourceId()
                        + " for " + implementationId(item));
            }
            var trial = new ArrayList<>(candidates);
            trial.add(candidate);
            ItemCompositionResolver.resolveLenient(Map.of(item, trial));
            candidates.add(candidate);
            updateItemLocked(item, false);
        }
    }

    private static void submitLegacy(Item item, ItemComposition data) {
        if (item == null || data == null) return;
        var previousSource = data.sources().isEmpty() ? null : data.sources().get(0);
        var source = new ItemCompositionSource(
                ItemCompositionSource.Kind.LEGACY,
                "gt6ce:legacy-item-composition/" + implementationId(item),
                "deprecated ItemMaterialRegistry.register(Item, ItemComposition)",
                sourceReference(data),
                previousSource == null
                        ? ""
                        : "original category=" + previousSource.kind().name().toLowerCase(Locale.ROOT));
        var compatibility = new ItemComposition(
                data.prefix(), data.components(), data.primaryMaterial(), List.of(source), data.bulkRecoveryAllowed());
        var candidate = new ItemCompositionCandidate(compatibility, null);
        synchronized (LOCK) {
            if (explicitClosed) throw new IllegalStateException("Explicit item composition registration is closed");
            var previous = LEGACY_SUBMISSIONS.put(item, candidate);
            try {
                updateItemLocked(item, false);
            } catch (RuntimeException failure) {
                if (previous == null) LEGACY_SUBMISSIONS.remove(item);
                else LEGACY_SUBMISSIONS.put(item, previous);
                updateItemLocked(item, false);
                throw failure;
            }
        }
    }

    private static void updateItemLocked(Item item, boolean requireReplacementTargets) {
        var explicitCandidates = EXPLICIT_SUBMISSIONS.get(item);
        var legacy = LEGACY_SUBMISSIONS.get(item);
        if ((explicitCandidates == null || explicitCandidates.isEmpty()) && legacy == null) {
            EXPLICIT.remove(item);
        } else {
            var submitted = new ArrayList<ItemCompositionCandidate>();
            if (explicitCandidates != null) submitted.addAll(explicitCandidates);
            if (legacy != null) submitted.add(legacy);
            var explicit = (requireReplacementTargets
                            ? ItemCompositionResolver.resolve(Map.of(item, submitted))
                            : ItemCompositionResolver.resolveLenient(Map.of(item, submitted)))
                    .get(item);
            if (explicit == null) EXPLICIT.remove(item);
            else EXPLICIT.put(item, explicit);
        }
        var combined = new IdentityHashMap<Item, List<ItemCompositionCandidate>>();
        var explicit = EXPLICIT_SUBMISSIONS.get(item);
        if (explicit != null) combined.put(item, explicit);
        legacy = LEGACY_SUBMISSIONS.get(item);
        if (legacy != null)
            combined.computeIfAbsent(item, ignored -> new ArrayList<>()).add(legacy);
        var tags = TAG_SUBMISSIONS.get(item);
        if (tags != null)
            combined.computeIfAbsent(item, ignored -> new ArrayList<>()).addAll(tags);
        var resolution = (requireReplacementTargets
                        ? ItemCompositionResolver.resolveDetailed(combined)
                        : ItemCompositionResolver.resolveDetailedLenient(combined))
                .get(item);
        var replacement = resolution == null ? null : resolution.selected();
        var previous = EFFECTIVE.get(item);
        boolean changed = !Objects.equals(previous, replacement);
        if (replacement == null) {
            EFFECTIVE.remove(item);
            DIAGNOSTICS.remove(item);
        } else {
            EFFECTIVE.put(item, replacement);
            DIAGNOSTICS.put(item, resolution);
        }
        if (changed) COMPOSITION_REVISION.incrementAndGet();
    }

    private static Map<Item, ItemComposition> stableSnapshot(Map<Item, ItemComposition> source) {
        record SnapshotRow(String id, Item item, ItemComposition composition) {}
        var rows = source.entrySet().stream()
                .map(entry -> new SnapshotRow(implementationId(entry.getKey()), entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(SnapshotRow::id))
                .toList();
        var result = new LinkedHashMap<Item, ItemComposition>();
        for (var row : rows) result.put(row.item(), row.composition());
        return Collections.unmodifiableMap(result);
    }

    private static boolean sameCandidate(ItemCompositionCandidate left, ItemCompositionCandidate right) {
        return left.composition().equals(right.composition())
                && Objects.equals(left.replacesSourceId(), right.replacesSourceId());
    }

    private static String implementationId(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? item.getClass().getName() + "@" + System.identityHashCode(item) : id.toString();
    }

    private static String sourceSuffix(ItemComposition composition) {
        return Integer.toHexString(Objects.hash(
                composition.prefix(),
                composition.components(),
                composition.sources(),
                composition.bulkRecoveryAllowed()));
    }

    private static String sourceReference(ItemComposition composition) {
        if (composition.sources().isEmpty()) return "";
        var source = composition.sources().get(0);
        return source.reference().isBlank() ? source.description() : source.reference();
    }
}
