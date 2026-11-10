package gregtech.engine.material;

import gregtech.api.material.*;
import gregtech.content.block.machine.MoldBlock;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.ClayMoldCatalog;
import gregtech.engine.stack.StackData;
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
    private static final Map<Item, List<ItemCompositionCandidate>> TAG_SUBMISSIONS = new IdentityHashMap<>();
    private static final AtomicLong COMPOSITION_REVISION = new AtomicLong();
    private static boolean tagInputReady;
    private static boolean explicitClosed;

    private static final Map<Item, ItemComposition> EFFECTIVE = new IdentityHashMap<>();
    private static final Map<Item, ItemComposition> EXPLICIT = new IdentityHashMap<>();

    public enum CompositionLookupState {
        PRESENT,
        ABSENT,
        TAG_INPUT_NOT_READY
    }

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

    /** Compatibility entry point: the composition's structured source decides its candidate priority. */
    @Deprecated(forRemoval = false)
    public static void register(Item item, ItemComposition data) {
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
        ItemCompositionSource source =
                new ItemCompositionSource(kind, declarationId, producer, sourceReference(data), "");
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
        synchronized (LOCK) {
            boolean readinessChanged = !tagInputReady;
            var affected = new HashSet<Item>();
            affected.addAll(TAG_SUBMISSIONS.keySet());
            affected.addAll(staged.keySet());
            var resolved = new IdentityHashMap<Item, ItemComposition>();
            for (Item item : affected) {
                var itemCandidates = new ArrayList<ItemCompositionCandidate>();
                var explicit = EXPLICIT_SUBMISSIONS.get(item);
                if (explicit != null) itemCandidates.addAll(explicit);
                var tags = staged.get(item);
                if (tags != null) itemCandidates.addAll(tags);
                if (itemCandidates.isEmpty()) continue;
                var value = ItemCompositionResolver.resolve(Map.of(item, itemCandidates))
                        .get(item);
                if (value != null) resolved.put(item, value);
            }
            boolean changed = false;
            for (Item item : affected) {
                ItemComposition previous = EFFECTIVE.get(item);
                ItemComposition replacement = resolved.get(item);
                if (Objects.equals(previous, replacement)) continue;
                changed = true;
                if (replacement == null) EFFECTIVE.remove(item);
                else EFFECTIVE.put(item, replacement);
            }
            TAG_SUBMISSIONS.clear();
            TAG_SUBMISSIONS.putAll(staged);
            tagInputReady = true;
            if (changed || readinessChanged) COMPOSITION_REVISION.incrementAndGet();
        }
    }

    public static void clearTagCandidates() {
        publishTagCandidates(Map.of());
    }

    /** Candidate caches depend on the complete effective composition snapshot. */
    public static long compositionRevision() {
        return COMPOSITION_REVISION.get();
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
        return base(item).filter(composition -> composition.sources().stream()
                .noneMatch(source -> source.kind() == ItemCompositionSource.Kind.FORM_TAG));
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
            return stableSnapshot(EFFECTIVE);
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

    /**
     * GT6 {@code OM.anydata_} admission for the melting crucible: any registered composition is
     * meltable, including rows marked descriptive for bulk recovery.
     */
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
     * <p>Composition availability, damage, stored contents and the bulk-recovery declaration are
     * separate facts. Runtime rows can distinguish a design rejection from a missing binding.</p>
     */
    public enum RecoveryRejection {
        ELIGIBLE,
        NO_COMPOSITION,
        NO_COMPONENTS,
        NOT_BULK_RECOVERABLE,
        DAMAGED,
        STORED_CONTENTS,
        UNSUPPORTED_CONTAINER
    }

    public static RecoveryRejection recoveryRejection(ItemStack stack) {
        if (stack.isEmpty()) return RecoveryRejection.NO_COMPOSITION;
        var data = base(stack.getItem()).orElse(null);
        if (data == null) return RecoveryRejection.NO_COMPOSITION;
        if (data.components().isEmpty()) return RecoveryRejection.NO_COMPONENTS;
        if (!data.bulkRecoveryAllowed()) return RecoveryRejection.NOT_BULK_RECOVERABLE;
        if (stack.isDamaged()) return RecoveryRejection.DAMAGED;
        if (hasStoredContents(stack)) return RecoveryRejection.STORED_CONTENTS;
        if (stack.getItem() instanceof net.minecraft.world.item.BucketItem
                && !stack.is(net.minecraft.world.item.Items.BUCKET)) return RecoveryRejection.UNSUPPORTED_CONTAINER;
        return RecoveryRejection.ELIGIBLE;
    }

    /** Bulk-machine recovery authorization, independent of composition existence and melting. */
    public static boolean canRecover(ItemStack stack) {
        return recoveryRejection(stack) == RecoveryRejection.ELIGIBLE;
    }

    /** Whether item data should replace a material cooking rule; descriptive compositions do not. */
    public static boolean hasMaterialProcessingRule(ItemStack stack) {
        if (stack.getItem() instanceof MaterialFormItem) return true;
        synchronized (LOCK) {
            var composition = EFFECTIVE.get(stack.getItem());
            return composition != null
                    && composition.sources().stream()
                            .anyMatch(source -> authorizesMaterialProcessingRule(stack.getItem(), source));
        }
    }

    private static boolean authorizesMaterialProcessingRule(Item item, ItemCompositionSource source) {
        return switch (source.kind()) {
            case VANILLA_JSON -> gregtech.content.material.VanillaCompositionLoader.inheritsVanillaUnification(item);
            case CORRECTION, BLOCK_WEIGHT -> false;
            default -> true;
        };
    }

    public static boolean hasStoredContents(ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && item.getBlock() instanceof gregtech.content.block.inventory.LootChestBlock) {
//? if >=1.21.1 {
            /*var loot = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
                        if (loot == null || !loot.copyTag().getBoolean("GTLootGenerated")) return true;
*///?} else {
            var loot = stack.getTagElement("BlockEntityTag");
            if (loot == null || !loot.getBoolean("GTLootGenerated")) return true;
//?}
        }
        var tag = StackData.read(stack);
        if (tag != null
                && (hasStoredBlockEntityData(stack, tag.getCompound("BlockEntityTag"))
                        || tag.contains("Items")
                        || tag.contains("EntityTag")
                        || tag.contains("ChargedProjectiles"))) return true;
//? if >=1.21.1 {
        /*        var blockData = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
                return (blockData != null && hasStoredBlockEntityData(stack, blockData.copyTag()))
                        || stack.has(net.minecraft.core.component.DataComponents.ENTITY_DATA)
                        || stack.has(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA)
                        || stack.has(net.minecraft.core.component.DataComponents.CONTAINER_LOOT)
                        || stack.has(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES)
                        || (stack.get(net.minecraft.core.component.DataComponents.CONTAINER) != null
                            && !stack.get(net.minecraft.core.component.DataComponents.CONTAINER).equals(
                                    net.minecraft.world.item.component.ItemContainerContents.EMPTY));
*///?} else {
        return false;
//?}
    }

    private static void submit(Item item, ItemComposition data, @Nullable String replacesSourceId) {
        if (item == null || data == null) return;
        if (data.sources().size() != 1)
            throw new IllegalArgumentException(
                    "Submitted item composition must have exactly one source: " + data.source());
        var candidate = new ItemCompositionCandidate(data, replacesSourceId);
        synchronized (LOCK) {
            if (explicitClosed)
                throw new IllegalStateException("Explicit item composition registration is closed");
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

    private static void updateItemLocked(Item item, boolean requireReplacementTargets) {
        var explicitCandidates = EXPLICIT_SUBMISSIONS.get(item);
        if (explicitCandidates == null || explicitCandidates.isEmpty()) {
            EXPLICIT.remove(item);
        } else {
            var explicit = (requireReplacementTargets
                            ? ItemCompositionResolver.resolve(Map.of(item, explicitCandidates))
                            : ItemCompositionResolver.resolveLenient(Map.of(item, explicitCandidates)))
                    .get(item);
            if (explicit == null) EXPLICIT.remove(item);
            else EXPLICIT.put(item, explicit);
        }
        var combined = new IdentityHashMap<Item, List<ItemCompositionCandidate>>();
        var explicit = EXPLICIT_SUBMISSIONS.get(item);
        if (explicit != null) combined.put(item, explicit);
        var tags = TAG_SUBMISSIONS.get(item);
        if (tags != null)
            combined.computeIfAbsent(item, ignored -> new ArrayList<>()).addAll(tags);
        var replacement = (requireReplacementTargets
                        ? ItemCompositionResolver.resolve(combined)
                        : ItemCompositionResolver.resolveLenient(combined))
                .get(item);
        var previous = EFFECTIVE.get(item);
        boolean changed = !Objects.equals(previous, replacement);
        if (replacement == null) EFFECTIVE.remove(item);
        else EFFECTIVE.put(item, replacement);
        if (changed) COMPOSITION_REVISION.incrementAndGet();
    }

    private static Map<Item, ItemComposition> stableSnapshot(Map<Item, ItemComposition> source) {
        var rows = new ArrayList<>(source.entrySet());
        rows.sort(Comparator.comparing(entry -> implementationId(entry.getKey())));
        var result = new LinkedHashMap<Item, ItemComposition>();
        for (var entry : rows) result.put(entry.getKey(), entry.getValue());
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

    private static boolean hasStoredBlockEntityData(ItemStack stack, net.minecraft.nbt.CompoundTag tag) {
        if (tag.isEmpty()) return false;
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && item.getBlock() instanceof MoldBlock
                && Set.of("id", ClayMoldCatalog.SHAPE_KEY).containsAll(tag.getAllKeys())
                && "gt6ce:mold".equals(tag.getString("id"))) return false;
        // Old empty chest drops carry loot bookkeeping even though their inventory drops separately.
        // Only admit those known keys; inventories, pending loot, covers and unknown data stay protected.
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem item)
                || !(item.getBlock() instanceof gregtech.content.block.inventory.MetalChestBlock)) return true;
        if (!Set.of("id", "GTLootGenerated", "GTDungeonLootSeed").containsAll(tag.getAllKeys())) return true;
        return item.getBlock() instanceof gregtech.content.block.inventory.LootChestBlock
                && !tag.getBoolean("GTLootGenerated");
    }
}
