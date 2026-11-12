package gregtech.content.recipe;

import com.mojang.logging.LogUtils;
import gregtech.api.material.*;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.*;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Tag-bound source prefix handlers. GT registers metadata for these forms, never new owned items. */
public final class ExternalOreProcessing {
    private ExternalOreProcessing() {}

    private static final Map<RecipeMap, Set<Recipe>> OWNED = new IdentityHashMap<>();

    /** Rows this adapter currently owns. The datapack overlay excludes them from its baseline. */
    public static synchronized Set<Recipe> owned() {
        var all = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        for (var rows : OWNED.values()) all.addAll(rows);
        return all;
    }
    /** Distinct from ore-prefix compositions so callers can tell tag-derived ingots, dusts and rods apart. */
    public static final String TAGGED_COMPOSITION_SOURCE = "GT6 common form tags";

    private static volatile int publishedCompositions;
    private static volatile int taggedCompositions;
    private static volatile int ambiguousTaggedItems;

    private static volatile Map<Item, FurnaceSmeltingRecipes.CookingData> cooking = Map.of();

    public static int taggedCompositions() {
        return taggedCompositions;
    }

    public static int ambiguousTaggedItems() {
        return ambiguousTaggedItems;
    }

    public static Optional<FurnaceSmeltingRecipes.CookingData> cooking(Item item) {
        return Optional.ofNullable(cooking.get(item));
    }

    /** Remove only this adapter's exact rows; other mods' processing rows remain registered. */
    public static synchronized void clear() {
        clearOwnedState();
        ItemMaterialRegistry.beginTagGeneration();
    }

    /** Removes adapter rows without starting a replacement tag generation. */
    static synchronized void clearOwnedRows() {
        clearOwnedState();
    }

    private static void clearOwnedState() {
        // Restore displaced source rows before removing them; otherwise an old mirror can resurrect this reload's
        // predecessors.
        gregtech.content.recipe.OvenRecipeBridge.clearMirrors();
        OWNED.forEach(RecipeMap::removeAll);
        OWNED.clear();
        publishedCompositions = 0;
        cooking = Map.of();
        taggedCompositions = 0;
        ambiguousTaggedItems = 0;
    }

    public static synchronized int rebuild() {
        clearOwnedState();
        CraftingMaterialForms.rebuild(false);
        var forms = CraftingMaterialForms.nativeAliases();
        var materialData = new IdentityHashMap<Item, List<ItemComposition>>();
        for (var prefix : ExternalOreProcessingRules.COMPOSITION_FORMS)
            for (var entry : forms.entrySet()) {
                if (!entry.getKey().prefix().equals(prefix.getName())) continue;
                var material = GTMaterialRegistry.get(entry.getKey().material()).resolve();
                if (!material.isValid()) continue;
                for (var item : entry.getValue())
                    materialData
                            .computeIfAbsent(item, ignored -> new ArrayList<>())
                            .add(new ItemComposition(
                                    prefix,
                                    List.of(MaterialComponent.of(material, prefix.getMaterialWeight())),
                                    material,
                                    "GT6 OP external ore-prefix tags",
                                    true));
            }
        int oreCompositions = materialData.size();
        taggedCompositions = bindTaggedForms(materialData);
        ItemMaterialRegistry.publishTagCandidates(materialData);
        publishedCompositions = materialData.size();
        CraftingMaterialForms.rebuild(true);
        forms = CraftingMaterialForms.nativeAliases();
        int added = 0;
        for (var route : ExternalOreProcessingRules.ROUTES) {
            var map = map(route.map());
            for (var entry : forms.entrySet()) {
                if (!entry.getKey().prefix().equals(route.input().getName())) continue;
                var material = GTMaterialRegistry.get(entry.getKey().material()).resolve();
                if (!material.isValid()) continue;
                var output = output(route.output(), material, route.count());
                for (var item : entry.getValue()) {
                    if (!ExternalOreProcessingRules.allows(material) || output.isEmpty()) continue;
                    // Prefix handlers disable optimizing; costs use the larger input/output unit amount.
                    var recipe = publishRoute(
                            map,
                            new Recipe(
                                    new ItemStack[] {new ItemStack(item)},
                                    new ItemStack[] {output.copy()},
                                    null,
                                    null,
                                    null,
                                    null,
                                    route.duration(material.getToolQuality()),
                                    16,
                                    0));
                    if (recipe != null) {
                        OWNED.computeIfAbsent(map, unused -> Collections.newSetFromMap(new IdentityHashMap<>()))
                                .add(recipe);
                        added++;
                    }
                }
            }
        }
        for (var route : ExternalOreProcessingRules.GRINDING) {
            var map = map(route.map());
            for (var entry : forms.entrySet()) {
                if (!entry.getKey().prefix().equals(route.input().getName())) continue;
                var material = GTMaterialRegistry.get(entry.getKey().material()).resolve();
                if (!route.allows(material)) continue;
                var target = route.outputMaterial(material);
                var dust = route.pulverizedRemains()
                        ? MortarGrindingRecipes.pulverizeAll(
                                material, route.input().getMaterialWeight())
                        : java.util.List.of(output(MaterialPrefix.dust, target, route.dustCount()));
                var fines =
                        route.fines() ? output(MaterialPrefix.dustTiny, target, route.fineCount()) : ItemStack.EMPTY;
                if (dust.isEmpty() || (route.fines() && fines.isEmpty())) continue;
                for (var item : entry.getValue()) {
                    var outputRows = new ArrayList<ItemStack>(dust.size() + (fines.isEmpty() ? 0 : 1));
                    for (var dustStack : dust) outputRows.add(dustStack.copy());
                    if (!fines.isEmpty()) outputRows.add(fines.copy());
                    var outputs = outputRows.toArray(ItemStack[]::new);
                    var recipe = publishRoute(
                            map,
                            new Recipe(
                                    route.requiresEmptySlot()
                                            ? new ItemStack[] {new ItemStack(item), ItemStack.EMPTY}
                                            : new ItemStack[] {new ItemStack(item)},
                                    outputs,
                                    null,
                                    null,
                                    null,
                                    null,
                                    route.duration(material),
                                    16,
                                    0));
                    if (recipe != null) {
                        OWNED.computeIfAbsent(map, unused -> Collections.newSetFromMap(new IdentityHashMap<>()))
                                .add(recipe);
                        added++;
                    }
                }
            }
        }
        var cookingData = new IdentityHashMap<Item, FurnaceSmeltingRecipes.CookingData>();
        for (var row : FurnaceSmeltingRules.EXTERNAL)
            for (var entry : forms.entrySet()) {
                if (!entry.getKey().prefix().equals(row.input().getName())) continue;
                var material = GTMaterialRegistry.get(entry.getKey().material()).resolve();
                if (!FurnaceSmeltingRules.legacyAllows(material)) continue;
                var target = material.getTargetSmeltingMaterial();
                if (target == null || !target.isValid()) continue;
                long amount = FurnaceSmeltingRules.amount(row.amount(), material.getTargetSmeltingAmount());
                var output = gregtech.util.OM.ingot(target, amount);
                if (output.isEmpty()) continue;
                for (var item : entry.getValue()) {
                    var map = MachineRecipeMaps.Furnace;
                    var recipe = map.addRecipe1(
                            true, 16, 16, new ItemStack(item), MachineRecipeMaps.neverFurnaceOutput(output.copy()));
                    if (recipe == null) continue;
                    OWNED.computeIfAbsent(map, unused -> Collections.newSetFromMap(new IdentityHashMap<>()))
                            .add(recipe);
                    cookingData.put(
                            item,
                            new FurnaceSmeltingRecipes.CookingData(
                                    row.experience()
                                            ? FurnaceSmeltingRules.experience(amount, material.getToolQuality())
                                            : 0,
                                    gregtech.content.data.generated.MaterialWorkability.isFood(material)));
                    added++;
                }
            }
        cooking = Collections.unmodifiableMap(cookingData);
        for (var prefix : ExternalOreProcessingRules.CRUCIBLE_FORMS)
            for (var entry : forms.entrySet()) {
                if (!entry.getKey().prefix().equals(prefix.getName())) continue;
                var material = GTMaterialRegistry.get(entry.getKey().material()).resolve();
                var preview = gregtech.engine.machine.crucible.CrucibleInputRules.smeltingPreview(material, prefix);
                if (preview == null) continue;
                var output = gregtech.util.OM.ingotOrDust(preview.material(), preview.amount());
                if (output.isEmpty()) continue;
                for (var item : entry.getValue()) {
                    var map = MachineRecipeMaps.CrucibleSmelting;
                    var recipe = map.addFakeRecipe(
                            false,
                            new ItemStack[] {new ItemStack(item)},
                            new ItemStack[] {output.copy()},
                            null,
                            null,
                            null,
                            null,
                            0,
                            0,
                            preview.temperatureK());
                    if (recipe == null) continue;
                    OWNED.computeIfAbsent(map, unused -> Collections.newSetFromMap(new IdentityHashMap<>()))
                            .add(recipe);
                    added++;
                }
            }
        LogUtils.getLogger()
                .info(
                        "[gregtech] Bound {} external ore processing rows and {} material compositions from loaded tags ({} native prefix, {} tagged form); skipped {} ambiguous tagged items",
                        added,
                        publishedCompositions,
                        oreCompositions,
                        taggedCompositions,
                        ambiguousTaggedItems);
        return added;
    }

    /** Resolve every imported form together so aliases merge and conflicting tags are diagnosed once. */
    private static int bindTaggedForms(Map<Item, List<ItemComposition>> materialData) {
        var allowed = new HashSet<String>();
        allowed.add(ExternalOreProcessingRules.TAGGED_ORE_FORM);
        for (var prefix : ExternalOreProcessingRules.COMPOSITION_FORMS) allowed.add(prefix.getName());
        for (var prefix : ExternalOreProcessingRules.TAGGED_MATERIAL_FORMS) allowed.add(prefix.getName());
        int added = 0;
        int ambiguous = 0;
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || item == Items.AIR) continue;
            String namespace = id.getNamespace();
            if (namespace.equals("minecraft") || namespace.equals("gt6ce")) continue;
            if (ItemMaterialRegistry.declared(item).isPresent() || materialData.containsKey(item)) continue;
            var candidates = new ArrayList<ItemCompositionCandidate>();
            var stack = new ItemStack(item);
            for (var form : CraftingMaterialForms.taggedForms(stack)) {
                if (!allowed.contains(form.prefix())) continue;
                var material = GTMaterialRegistry.get(form.material()).resolve();
                if (!material.isValid()) continue;
                var sourceId = "gt6ce:external-tag/" + id + "/" + form.prefix() + "/" + material.getName();
                var source = new ItemCompositionSource(
                        ItemCompositionSource.Kind.FORM_TAG, sourceId, TAGGED_COMPOSITION_SOURCE, "", "");
                if (ExternalOreProcessingRules.TAGGED_ORE_FORM.equals(form.prefix())) {
                    if (BlockMaterialPrefix.ore == null) continue;
                    candidates.add(new ItemCompositionCandidate(
                            new ItemComposition(
                                    null,
                                    List.of(MaterialComponent.of(
                                            material, BlockMaterialPrefix.ore.getMaterialWeight())),
                                    material,
                                    List.of(source),
                                    false),
                            null));
                    continue;
                }
                var prefix = gregtech.engine.prefix.PrefixRegistry.byName(form.prefix());
                if (prefix == null || !prefix.isValidFor(material)) continue;
                candidates.add(new ItemCompositionCandidate(
                        new ItemComposition(
                                prefix,
                                List.of(MaterialComponent.of(material, prefix.getMaterialWeight())),
                                material,
                                List.of(source),
                                true),
                        null));
            }
            var seenSpecialTags = new HashSet<String>();
            for (var tag : stack.getTags().map(itemTag -> itemTag.location()).toList()) {
                String tagNamespace = tag.getNamespace(), path = tag.getPath();
                if (!Set.of("forge", "c").contains(tagNamespace)) continue;
                int slash = path.lastIndexOf('/');
                if (slash < 0) continue;
                String group = path.substring(0, slash);
                long amount;
                if (group.equals(ExternalOreProcessingRules.TAGGED_STORAGE_BLOCK_GROUP)) amount = 9 * GTValues.U;
                else if (group.equals(ExternalOreProcessingRules.TAGGED_CABLE_GROUP)) amount = GTValues.U / 2;
                else continue;
                String materialName = CraftingMaterialForms.tagMaterial(tag);
                if (materialName == null || !seenSpecialTags.add(group + "/" + materialName)) continue;
                var material = GTMaterialRegistry.get(materialName).resolve();
                if (!material.isValid()) continue;
                var sourceId = "gt6ce:external-tag/" + id + "/" + group + "/" + material.getName();
                candidates.add(new ItemCompositionCandidate(
                        new ItemComposition(
                                null,
                                List.of(MaterialComponent.of(material, amount)),
                                material,
                                List.of(new ItemCompositionSource(
                                        ItemCompositionSource.Kind.FORM_TAG,
                                        sourceId,
                                        TAGGED_COMPOSITION_SOURCE,
                                        "",
                                        "")),
                                true),
                        null));
            }
            if (candidates.isEmpty()) continue;
            ItemComposition resolved;
            try {
                resolved = ItemCompositionResolver.resolve(Map.of(item, candidates))
                        .get(item);
            } catch (IllegalStateException conflict) {
                ambiguous++;
                LogUtils.getLogger()
                        .warn(
                                "[gregtech] Ignoring ambiguous external material tags for {}: {}",
                                id,
                                conflict.getMessage());
                continue;
            }
            if (resolved == null) continue;
            materialData.computeIfAbsent(item, ignored -> new ArrayList<>()).add(resolved);
            added++;
        }
        ambiguousTaggedItems = ambiguous;
        return added;
    }

    private static RecipeMap map(String name) {
        return switch (name) {
            case "Crusher" -> MachineRecipeMaps.Crusher;
            case "Sifting" -> MachineRecipeMaps.Sifting;
            case "Shredder" -> MachineRecipeMaps.Shredder;
            case "Anvil" -> MachineRecipeMaps.Anvil;
            case "Mortar" -> MachineRecipeMaps.Mortar;
            default -> throw new IllegalArgumentException("Unknown source external ore map " + name);
        };
    }

    private static Recipe publishRoute(RecipeMap map, Recipe recipe) {
        if (map != MachineRecipeMaps.Shredder && map != MachineRecipeMaps.Anvil) return map.addRecipe(recipe);
        return RecoveryRecipePublisher.publishIsolated(
                map, RecoveryRecipePublisher.explicit(recipe, "external-ore/dynamic"));
    }

    private static ItemStack output(MaterialPrefix prefix, GTMaterial material, int count) {
        var output = gregtech.content.registry.GTItems.getStack(prefix, material, count);
        return output.isEmpty() ? CraftingMaterialForms.stack(prefix.getName(), material, count) : output;
    }
}
