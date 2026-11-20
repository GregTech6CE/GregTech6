/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * RecipeMapShredder.getRecipeFor accepts original OM.anydata/CR.REV machine data. */
package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.content.data.*;
import gregtech.content.machine.MachineConstructionMaterials;
import gregtech.content.material.CraftingItemCompositions;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.*;
import gregtech.engine.recipe.*;
import gregtech.engine.stack.StackCompat;
import gregtech.platform.Mc;
import java.util.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** GT6 RecipeMapShredder decomposition for vanilla and explicitly audited native compositions. */
public final class VanillaRecoveryRecipes {
    private static final List<Recipe> RECIPES = new ArrayList<>();
    private static final List<Recipe> CRAFTING_ROWS = new ArrayList<>();
    private static final Map<String, Integer> SKIPPED = new TreeMap<>();

    public static List<Recipe> recipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static Map<String, Integer> skipped() {
        return Collections.unmodifiableMap(SKIPPED);
    }

    private VanillaRecoveryRecipes() {}

    public static int register() {
        SKIPPED.clear();
        for (var item : Mc.itemStream()
                .sorted(Comparator.comparing(i -> Mc.itemId(i).toString()))
                .toList()) {
            if (CraftingItemCompositions.tracks(item)) continue;
            var id = Mc.itemId(item);
            boolean auditedMachine = originalMachine(item);
            boolean meatCut = gregtech.content.material.MeatItemMaterials.contains(item);
            if (!id.getNamespace().equals("minecraft")
                    && !gregtech.content.material.PanelMaterialCompositions.recoveryItems()
                            .contains(item)
                    && !gregtech.content.material.TransportMaterialCompositions.recoveryItems()
                            .contains(item)
                    && !auditedMachine
                    && !meatCut) continue;
            register(item, false, SKIPPED);
        }
        Mc.logger()
                .info("Registered {} composition-driven vanilla recovery recipes; skipped {}", RECIPES.size(), SKIPPED);
        return RECIPES.size();
    }

    static void rebuildCraftingRows() {
        clearCraftingRows();
        var skipped = new TreeMap<String, Integer>();
        for (Item item : ItemMaterialRegistry.entries().keySet()) {
            if (CraftingItemCompositions.tracks(item)) register(item, true, skipped);
        }
        Mc.logger().debug("Registered {} crafting-derived shredder recipes; skipped {}", CRAFTING_ROWS.size(), skipped);
    }

    static List<Recipe> craftingRows() {
        return List.copyOf(CRAFTING_ROWS);
    }

    static void clearCraftingRows() {
        MachineRecipeMaps.Shredder.removeAll(CRAFTING_ROWS);
        CRAFTING_ROWS.clear();
    }

    private static void register(Item item, boolean crafting, Map<String, Integer> skipped) {
        var id = Mc.itemId(item);
        boolean meatCut = gregtech.content.material.MeatItemMaterials.contains(item);
        var stack = new ItemStack(item);
        var rejection = ItemMaterialRegistry.recoveryRejection(stack);
        if (rejection != ItemMaterialRegistry.RecoveryRejection.ELIGIBLE) {
            skip(skipped, "input/" + rejection.name().toLowerCase(Locale.ROOT));
            return;
        }
        if (!meatCut
                && gregtech.engine.material.VanillaUnificationDefinitions.oreInputKind(id.getPath())
                        != gregtech.engine.material.VanillaUnificationDefinitions.OreInputKind.OTHER) {
            skip(skipped, "source-ore-category");
            return;
        }
        var data = ItemMaterialRegistry.base(item).orElseThrow();
        if (data.prefix() != null
                && (data.prefix().getName().startsWith("dust") || data.prefix() == MaterialPrefix.oreRaw)) {
            skip(skipped, "existing-recovery-family");
            return;
        }
        var plan = MaterialRecoveryRules.planShredder(data, MachineRecipeMaps.Shredder.itemOutputSlots);
        if (!plan.accepted()) {
            skip(skipped, "plan/" + plan.rejection().name().toLowerCase(Locale.ROOT));
            return;
        }
        var outputs = new ArrayList<ItemStack>();
        boolean bindingsComplete = true;
        for (var output : plan.outputs()) {
            for (var pile : output.dust().piles()) {
                var dust = GTItems.getStack(pile.prefix(), output.material(), pile.count());
                if (dust.isEmpty()) {
                    bindingsComplete = false;
                    break;
                }
                outputs.add(dust);
            }
            if (!bindingsComplete) break;
        }
        if (!bindingsComplete) {
            skip(skipped, "output/binding-missing");
            return;
        }
        if (outputs.size() == 1
                && StackCompat.sameItem(stack, outputs.get(0))
                && outputs.get(0).getCount() == 1) return;
        var recipe = new Recipe(
                        new ItemStack[] {stack},
                        outputs.toArray(ItemStack[]::new),
                        null,
                        null,
                        null,
                        plan.duration(),
                        16,
                        0)
                .withMaterialRecovery();
        if (crafting) {
            // Mold shapes are catalysts in production recipes but are consumed during recycling.
            recipe.explicitCatalystsOnly = true;
            recipe.withItemInputGuard(
                    0,
                    offered -> offered.is(item)
                            && ItemMaterialRegistry.canRecover(offered)
                            && ItemMaterialRegistry.get(offered)
                                    .filter(data::equals)
                                    .isPresent());
            recipe.withSource("recovery:crafting-composition");
            if (RecoveryRecipePublisher.publishIsolated(
                            MachineRecipeMaps.Shredder, RecoveryRecipePublisher.generic(recipe, "crafting-recovery"))
                    != null) CRAFTING_ROWS.add(recipe);
        } else {
            recipe.withRuntimeResolver(inputs -> ShredderDamageScaling.resolve(inputs, recipe, false));
            recipe.withSource("recovery:vanilla-generic");
            if (RecoveryRecipePublisher.publish(
                            MachineRecipeMaps.Shredder, RecoveryRecipePublisher.generic(recipe, "vanilla-recovery"))
                    != null) RECIPES.add(recipe);
            else skip(skipped, "existing-owner");
        }
    }

    private static void skip(Map<String, Integer> skipped, String reason) {
        skipped.merge(reason, 1, Integer::sum);
    }

    /** Admit audited source compositions; coarse legacy hull estimates are not recovery data. */
    private static boolean originalMachine(net.minecraft.world.item.Item item) {
        if (!(item instanceof net.minecraft.world.item.BlockItem blockItem)) return false;
        var block = blockItem.getBlock();
        var id = Mc.blockId(block);
        if (!id.getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)) return false;
        var expected = MachineConstructionMaterials.block(id.getPath());
        if (expected.isEmpty() && block instanceof gregtech.content.block.machine.BasicMachineBlock machine)
            expected = MachineConstructionMaterials.machine(
                    machine.basicSpec().machineName(), machine.basicSpec().tier());
        if (expected.isEmpty()) return false;
        var registered = ItemMaterialRegistry.base(item);
        return registered.isPresent()
                && registered.get().components().equals(expected.get().components());
    }
}
