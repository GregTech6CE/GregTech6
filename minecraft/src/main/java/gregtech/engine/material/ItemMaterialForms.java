package gregtech.engine.material;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.block.OreBlock;
import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.ExternalMaterialForms;
import gregtech.engine.prefix.PrefixRegistry;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Material/form identities shared by machine processing and hand crafting. */
public final class ItemMaterialForms {
    private ItemMaterialForms() {}

    private record NativeIndex(
            boolean complete,
            Map<String, String> materialNames,
            Map<String, String> prefixNames,
            Map<Item, ItemMaterialForm> vanilla,
            Map<Item, List<ItemMaterialForm>> extraForms,
            Map<ItemMaterialForm, Item> preferred,
            Map<Item, ItemMaterialForm> primaryForms,
            Map<ItemMaterialForm, List<Item>> primaryAliases,
            Map<ItemMaterialForm, List<Item>> aliases,
            Map<Item, Integer> itemOrder,
            List<Item> tagCandidates) {}

    record TaggedIndex(Map<Item, ItemMaterialForm> forms, Map<ItemMaterialForm, List<Item>> aliases) {}

    private static volatile NativeIndex nativeIndex;

    private static NativeIndex nativeIndex() {
        var known = nativeIndex;
        if (known != null && (known.complete() || !ItemMaterialRegistry.explicitClosed())) return known;
        synchronized (ItemMaterialForms.class) {
            boolean complete = ItemMaterialRegistry.explicitClosed();
            if (nativeIndex == null || (complete && !nativeIndex.complete()))
                nativeIndex = buildNativeIndex(complete);
            return nativeIndex;
        }
    }

    private static NativeIndex buildNativeIndex(boolean complete) {
        var materials = new HashMap<String, String>();
        for (var material : GTMaterialRegistry.allMaterials()) if (material.isValid()) {
            String name = material.resolve().getName();
            String canonical = MaterialEquivalence.materialName(material);
            materials.put(canonical, name);
            materials.put(material.getName().toLowerCase(Locale.ROOT), name);
            String alternate = MaterialTagAliases.alternate(canonical);
            if (alternate != null) materials.put(alternate, name);
        }
        var prefixes = new HashMap<String, String>();
        for (String prefix : ItemMaterialForm.STANDARD_ORES)
            prefixes.put(MaterialPrefix.camelToSnake(prefix), prefix);
        for (String prefix : ItemMaterialForm.DENSE_ORES)
            prefixes.put(MaterialPrefix.camelToSnake(prefix), prefix);
        for (var prefix : List.of("blockRaw", "blockDust", "blockIngot", "blockGem", "blockPlate", "blockPlateGem"))
            prefixes.put(MaterialPrefix.camelToSnake(prefix), prefix);
        // Both the source spelling and the port's identical renamed form tags are accepted.
        for (var prefix : PrefixRegistry.all()) {
            String sourceName = PrefixRegistry.sourceName(prefix.getName());
            prefixes.put(MaterialPrefix.camelToSnake(prefix.getName()), sourceName);
            if (!sourceName.equals(prefix.getName()))
                prefixes.putIfAbsent(MaterialPrefix.camelToSnake(sourceName), sourceName);
        }
        for (int size = 1; size <= 16; size++) prefixes.put(String.format(Locale.ROOT, "wire_%02d", size),
                String.format(Locale.ROOT, "wireGt%02d", size));
        var forms = new IdentityHashMap<Item, ItemMaterialForm>();
        var extras = new IdentityHashMap<Item, List<ItemMaterialForm>>();
        var targets = new HashMap<ItemMaterialForm, Item>();
        for (var definition : VanillaUnificationDefinitions.entries()) {
            String prefix = definition.prefix() == null ? null : definition.prefix().getName();
            String path = definition.itemPath();
            if (prefix == null) {
                // Modern raw metals are not GT6 native ore chunks or their storage blocks.
                if (Set.of("raw_iron", "raw_gold", "raw_copper").contains(path)) prefix = "rawVanilla";
                else if (Set.of("raw_iron_block", "raw_gold_block", "raw_copper_block").contains(path)) prefix = "blockRawVanilla";
                else if (path.endsWith("_ore") || path.equals("ancient_debris"))
                    prefix = VanillaUnificationDefinitions.vanillaOreForm(path);
            }
            if (prefix == null) continue;
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(path));
            var material = GTMaterialRegistry.get(definition.materialName()).resolve();
            if (item == Items.AIR || !material.isValid()) continue;
            var form = new ItemMaterialForm(prefix, material.getName());
            forms.put(item, form);
            if (!ItemMaterialForm.STANDARD_ORES.contains(prefix)) targets.putIfAbsent(form, item);
            for (var extraPrefix : definition.extraPrefixes()) {
                var extraForm = new ItemMaterialForm(extraPrefix.getName(), material.getName());
                extras.computeIfAbsent(item, unused -> new ArrayList<>()).add(extraForm);
                targets.putIfAbsent(extraForm, item);
            }
        }
        for (var target : VanillaUnificationDefinitions.blockTargets()) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(target.itemPath()));
            var material = GTMaterialRegistry.get(target.materialName()).resolve();
            if (item == Items.AIR || !material.isValid()) continue;
            var form = new ItemMaterialForm(target.blockPrefix(), material.getName());
            forms.put(item, form);
            targets.putIfAbsent(form, item);
        }
        // Real plank blocks are registered outside VanillaUnificationDefinitions. Prefer them to
        // generic plate forms, deterministically choosing vanilla before an equivalent mod block.
        var planks = new ArrayList<Item>(BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof BlockItem).toList());
        planks.sort(Comparator.comparing((Item item) -> !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))
                .thenComparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        for (var item : planks) {
            var data = ItemMaterialRegistry.explicit(item).orElse(null);
            if (data == null || data.prefix() != MaterialPrefix.plate || data.components().size() != 1
                    || data.amount() != GTValues.U || !data.material().has(MaterialProperty.WOOD)) continue;
            var form = new ItemMaterialForm("plate", data.material().resolve().getName());
            forms.put(item, form);
            targets.putIfAbsent(form, item);
        }
        targets.putIfAbsent(new ItemMaterialForm("plate", "Wood"), Items.OAK_PLANKS);
        var primaryForms = new IdentityHashMap<Item, ItemMaterialForm>();
        var primaryAliases = new HashMap<ItemMaterialForm, List<Item>>();
        var itemOrder = new IdentityHashMap<Item, Integer>();
        var tagCandidates = new ArrayList<Item>();
        for (var item : BuiltInRegistries.ITEM) {
            itemOrder.put(item, itemOrder.size());
            var form = direct(new ItemStack(item), forms);
            if (form == null) {
                if (item != Items.AIR) tagCandidates.add(item);
            } else {
                primaryForms.put(item, form);
                primaryAliases.computeIfAbsent(form, unused -> new ArrayList<>()).add(item);
            }
        }
        primaryAliases.replaceAll((form, values) -> List.copyOf(values));
        extras.replaceAll((item, values) -> List.copyOf(values));
        var aliases = new HashMap<>(primaryAliases);
        extras.forEach((item, values) -> values.forEach(form -> {
            var list = new ArrayList<>(aliases.getOrDefault(form, List.of()));
            list.add(item);
            aliases.put(form, List.copyOf(list));
        }));
        return new NativeIndex(complete, Map.copyOf(materials), Map.copyOf(prefixes), Map.copyOf(forms), Map.copyOf(extras),
                Map.copyOf(targets), Map.copyOf(primaryForms), Map.copyOf(primaryAliases), Map.copyOf(aliases),
                Map.copyOf(itemOrder), List.copyOf(tagCandidates));
    }

    /** Rebuild only aliases that may change when new tags and tag compositions are bound. */
    public static void refreshTaggedAliases() {
        var index = nativeIndex();
        var state = ItemMaterialRegistry.compositionState();
        var updated = new HashMap<ItemMaterialForm, List<Item>>();
        var byItem = new IdentityHashMap<Item, ItemMaterialForm>();
        for (var item : index.tagCandidates()) {
            var form = tagged(new ItemStack(item));
            if (form == null) continue;
            byItem.put(item, form);
            updated.computeIfAbsent(form, unused -> new ArrayList<>()).add(item);
        }
        var aliases = new HashMap<>(index.aliases());
        updated.forEach((form, tagged) -> {
            var primary = index.primaryAliases().getOrDefault(form, List.of());
            var combined = new ArrayList<>(primary);
            combined.addAll(tagged);
            // The first candidate is used as an output fallback. Keep the registry order
            // across native and tagged primary aliases, followed by secondary vanilla aliases.
            combined.sort(Comparator.comparingInt(index.itemOrder()::get));
            var nativeAliases = index.aliases().getOrDefault(form, List.of());
            combined.addAll(nativeAliases.subList(primary.size(), nativeAliases.size()));
            aliases.put(form, List.copyOf(combined));
        });
        ItemMaterialRegistry.installTagFormIndex(state, new TaggedIndex(Map.copyOf(byItem), Map.copyOf(aliases)));
    }

    public static Map<ItemMaterialForm, List<Item>> nativeAliases() {
        return nativeIndex().aliases();
    }

    public static Map<ItemMaterialForm, List<Item>> aliases() {
        var bound = ItemMaterialRegistry.tagFormIndex();
        return bound == null ? nativeAliases() : bound.aliases();
    }

    /** Optional source forms supplied by another mod, without manufacturing a GT item for them. */
    public static ItemStack stack(String prefix, GTMaterial material, int count) {
        return stack(aliases(), prefix, material, count);
    }

    public static ItemStack stack(Map<ItemMaterialForm, List<Item>> aliases, String prefix, GTMaterial material, int count) {
        var items = aliases.get(new ItemMaterialForm(prefix, material.resolve().getName()));
        return items == null || items.isEmpty() ? ItemStack.EMPTY : new ItemStack(items.get(0), count);
    }

    public static boolean contains(String prefix) {
        return aliases().keySet().stream().anyMatch(form -> form.prefix().equals(prefix));
    }

    private static ItemMaterialForm direct(ItemStack stack, Map<Item, ItemMaterialForm> vanilla) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof BlockItem item) {
            var block = item.getBlock();
            if (block instanceof OreBlock ore) {
                var host = OreBlock.stoneOfStack(stack);
                String prefix = host.isBedrock() ? "oreBedrock" : host.isSediment() ? "oreDust" : ore.prefix().getName();
                return new ItemMaterialForm(prefix, ore.material().resolve().getName());
            }
            if (block instanceof MaterialBlockLike material) {
                String prefix = material.prefix().getName();
                if (prefix.equals("blockSolid") && MaterialUnification.hiddenDuplicate(stack)) prefix = "blockIngot";
                return new ItemMaterialForm(prefix, material.material().resolve().getName());
            }
            if (block instanceof ElectricWireBlock wire && !wire.spec().insulated())
                return new ItemMaterialForm(String.format(Locale.ROOT, "wireGt%02d", wire.spec().size()), wire.spec().material().resolve().getName());
        }
        var known = vanilla.get(stack.getItem());
        if (known != null) return known;
        var declared = ItemMaterialRegistry.explicit(stack.getItem()).orElse(null);
        if (declared == null
                || declared.prefix() == null
                || declared.components().size() != 1
                || declared.amount() != declared.prefix().getMaterialWeight()) return null;
        return new ItemMaterialForm(declared.prefix().getName(), declared.material().resolve().getName());
    }

    public static ItemMaterialForm form(ItemStack stack) {
        var form = direct(stack, nativeIndex().vanilla());
        return form == null ? tagged(stack) : form;
    }

    public static ItemMaterialForm plainForm(ItemStack stack) {
        if (stack.isEmpty() || stack.isDamaged()) return form(stack);
//? if >=1.21.1 {
        /*if (!stack.getComponentsPatch().isEmpty()) return form(stack);
*///?} else {
        if (stack.hasTag()) return form(stack);
//?}
        var index = nativeIndex();
        if (!index.complete()) return form(stack);
        var nativeForm = index.primaryForms().get(stack.getItem());
        if (nativeForm != null) return nativeForm;
        var bound = ItemMaterialRegistry.tagFormIndex();
        return bound == null ? tagged(stack) : bound.forms().get(stack.getItem());
    }

    private static ItemMaterialForm tagged(ItemStack stack) {
        var composition = ItemMaterialRegistry.base(stack.getItem()).orElse(null);
        if (composition != null && composition.prefix() != null)
            return new ItemMaterialForm(composition.prefix().getName(), composition.material().resolve().getName());
        return compositionTagForm(taggedForms(stack));
    }

    public static ItemMaterialForm compositionTagForm(List<ItemMaterialForm> forms) {
        var allowed = forms.stream()
                .filter(form -> ExternalMaterialForms.isCompositionForm(form.prefix()))
                .toList();
        return allowed.size() == 1 ? allowed.get(0) : null;
    }

    /** Every gregtech, forge or c form tag on this item. Callers use a single hit to reject ambiguous items. */
    public static List<ItemMaterialForm> taggedForms(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        var found = new ArrayList<ItemMaterialForm>();
        var seen = new HashSet<ItemMaterialForm>();
        var index = nativeIndex();
        for (var tag : stack.getTags().toList()) {
            var form = formOf(tag.location(), index);
            if (form != null && seen.add(form)) found.add(form);
        }
        return List.copyOf(found);
    }

    private static ItemMaterialForm formOf(ResourceLocation tag, NativeIndex index) {
        String namespace = tag.getNamespace(), path = tag.getPath();
        if (!Set.of("gt6ce", "forge", "c").contains(namespace)) return null;
        int slash = path.lastIndexOf('/');
        if (slash < 0) return null;
        String material = index.materialNames().get(path.substring(slash + 1));
        if (material == null) return null;
        String group = path.substring(0, slash), prefix = index.prefixNames().get(group);
        if (prefix == null) prefix = MaterialEquivalence.prefixForCommonGroup(group);
        return prefix == null ? null : new ItemMaterialForm(prefix, material);
    }

    /** Resolves a common tag's material suffix without assigning it to a {@link MaterialPrefix}. */
    public static String tagMaterial(ResourceLocation tag) {
        String namespace = tag.getNamespace(), path = tag.getPath();
        if (!Set.of("forge", "c").contains(namespace)) return null;
        int slash = path.lastIndexOf('/');
        return slash < 0 ? null : nativeIndex().materialNames().get(path.substring(slash + 1));
    }

    public static boolean matches(String prefix, String material, ItemStack stack) {
        var expected = new ItemMaterialForm(prefix, material);
        if (expected.equals(form(stack))) return true;
        var extras = nativeIndex().extraForms().get(stack.getItem());
        return extras != null && extras.contains(expected);
    }
    public static ItemStack canonical(ItemStack output) {
//? if >=1.21.1 {
        /*if (output.isEmpty() || !output.getComponentsPatch().isEmpty() || output.isDamaged()) return output;
*///?} else {
        if (output.isEmpty() || output.hasTag() || output.isDamaged()) return output;
//?}
        var index = nativeIndex();
        var form = direct(output, index.vanilla());
        // Ordinary recipe outputs need not have a material form; Map.copyOf rejects null keys.
        var target = form == null ? null : index.preferred().get(form);
        return target == null ? MaterialUnification.canonical(output) : new ItemStack(target, output.getCount());
    }
}
