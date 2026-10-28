package gregtech.engine.material;

import gregtech.api.material.GTValues;
import gregtech.api.material.*;

import gregtech.content.data.MaterialPrefix;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** Immutable per-item compositions; stack damage is evaluated without caching ItemStacks. */
public final class ItemMaterialRegistry {
    private static final Map<Item, ItemComposition> BY_ITEM = new IdentityHashMap<>();
    private static final java.util.concurrent.atomic.AtomicLong COMPOSITION_REVISION =
            new java.util.concurrent.atomic.AtomicLong();
    private ItemMaterialRegistry() {}
    public static void register(Item item, @Nullable MaterialPrefix prefix, GTMaterial material) {
        boolean compositeHead = prefix == MaterialPrefix.toolHeadWrench || prefix == MaterialPrefix.toolHeadChainsaw
                || prefix == MaterialPrefix.toolHeadDrill || prefix == MaterialPrefix.toolHeadPickaxeGem;
        if (item != null && material != null && prefix != null
                && (prefix.hasEmptyForm() || prefix == MaterialPrefix.plantGtTwig || compositeHead)) {
            var parts = MaterialChemistry.prefixMaterialWeights(material, prefix).stream()
                    .map(c -> MaterialComponent.of(c.material(), c.amount())).toList();
            String source = compositeHead ? "GT6 OP tool-head components"
                    : prefix == MaterialPrefix.plantGtTwig ? "GT6 OP twig components"
                    : prefix == MaterialPrefix.glasstube ? "GT6 OP chemtube components"
                    : "GT6 OP ammunition components";
            if (!parts.isEmpty()) register(item, new ItemComposition(prefix, parts, source, true));
            return;
        }
        register(item, prefix, material, prefix == null ? GTValues.U : prefix.getMaterialWeight());
    }
    public static void register(Item item, @Nullable MaterialPrefix prefix, GTMaterial material, long amount) {
        if (item != null && material != null && material.isValid() && amount > 0)
            register(item, new ItemComposition(prefix, material, amount));
    }
    public static void register(Item item, ItemComposition data) {
        if (item != null && data != null) {
            var previous = BY_ITEM.put(item, data);
            if (!Objects.equals(previous, data)) COMPOSITION_REVISION.incrementAndGet();
        }
    }
    /** Candidate caches depend on explicit and tag-derived composition, not just stack identity. */
    public static long compositionRevision() { return COMPOSITION_REVISION.get(); }

    /** Called after the external tag adapter publishes changed immutable composition data. */
    public static void notifyExternalCompositionChanged() { COMPOSITION_REVISION.incrementAndGet(); }
    /** Data registered for this item. Tag-derived compositions are not included. */
    public static Optional<ItemComposition> explicit(Item item) {
        var data = BY_ITEM.get(item);
        return data == null ? Optional.empty() : Optional.of(data);
    }
    public static Optional<ItemComposition> base(Item item) {
        var data = explicit(item);
        return data.isPresent() ? data : gregtech.content.recipe.ExternalOreProcessing.composition(item);
    }
    public static Map<Item, ItemComposition> entries() { return Collections.unmodifiableMap(BY_ITEM); }
    public static Optional<ItemComposition> get(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        var data = base(stack.getItem()).orElse(null);
        if (data == null) return Optional.empty();
        if (!stack.isDamaged()) return Optional.of(data);
        long maximum = stack.getMaxDamage();
        long remaining = Math.max(0, maximum - stack.getDamageValue());
        var scaled = data.components().stream().map(c -> MaterialComponent.of(c.material(), c.amount() * remaining / maximum))
                .filter(c -> c.amount() > 0).toList();
        return Optional.of(new ItemComposition(data.prefix(), scaled, data.source(), data.recoverable()));
    }
    /** Do not consume stored inventories/entities or fluid containers as empty shells. */
    public static boolean canRecover(ItemStack stack) {
        var data = get(stack);
        if (data.isEmpty() || !data.get().recoverable() || data.get().components().isEmpty()) return false;
        if (hasStoredContents(stack)) return false;
        return !(stack.getItem() instanceof net.minecraft.world.item.BucketItem) || stack.is(net.minecraft.world.item.Items.BUCKET);
    }
    public static boolean hasStoredContents(ItemStack stack) {
//? if >=1.21.1 {
        /*var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        var tag = data == null ? null : data.copyTag();
        if (tag != null && (tag.contains("BlockEntityTag") || tag.contains("Items")
                || tag.contains("EntityTag") || tag.contains("ChargedProjectiles"))) return true;
        return stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA)
                || stack.has(net.minecraft.core.component.DataComponents.ENTITY_DATA)
                || stack.has(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA)
                || stack.has(net.minecraft.core.component.DataComponents.CONTAINER_LOOT)
                || stack.has(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES)
                || (stack.get(net.minecraft.core.component.DataComponents.CONTAINER) != null
                    && !stack.get(net.minecraft.core.component.DataComponents.CONTAINER).equals(
                            net.minecraft.world.item.component.ItemContainerContents.EMPTY));
*///?} else {
        var tag = stack.getTag();
        return tag != null && (tag.contains("BlockEntityTag") || tag.contains("Items") || tag.contains("EntityTag") || tag.contains("ChargedProjectiles"));
//?}
    }
}
