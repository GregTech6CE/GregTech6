/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * RecipeMapShredder.getRecipeFor accepts original OM.anydata/CR.REV machine data. */
package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.content.data.*;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.*;
import gregtech.engine.recipe.*;
import gregtech.engine.stack.StackCompat;
import gregtech.platform.Mc;
import java.util.*;
import net.minecraft.world.item.ItemStack;

/** GT6 RecipeMapShredder decomposition for vanilla and explicitly audited native compositions. */
public final class VanillaRecoveryRecipes {
    private static final List<Recipe> RECIPES = new ArrayList<>();
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
            var id = Mc.itemId(item);
            boolean auditedMachine = originalMachine(item);
            boolean meatCut = gregtech.content.material.MeatItemMaterials.contains(item);
            if (!id.getNamespace().equals("minecraft")
                    && !PanelMaterialRegistration.recoveryItems().contains(item)
                    && !TransportMaterialRegistration.recoveryItems().contains(item)
                    && !auditedMachine
                    && !meatCut) continue;
            var stack = new ItemStack(item);
            var rejection = ItemMaterialRegistry.recoveryRejection(stack);
            if (rejection != ItemMaterialRegistry.RecoveryRejection.ELIGIBLE) {
                skip("input/" + rejection.name().toLowerCase(Locale.ROOT));
                continue;
            }
            if (!meatCut
                    && gregtech.engine.material.VanillaUnificationDefinitions.oreInputKind(id.getPath())
                            != gregtech.engine.material.VanillaUnificationDefinitions.OreInputKind.OTHER) {
                skip("source-ore-category");
                continue;
            }
            var data = ItemMaterialRegistry.base(item).orElseThrow();
            if (data.prefix() != null
                    && (data.prefix().getName().startsWith("dust") || data.prefix() == MaterialPrefix.oreRaw)) {
                skip("existing-recovery-family");
                continue;
            }
            var plan = MaterialRecoveryRules.planShredder(data, MachineRecipeMaps.Shredder.mOutputItemsCount);
            if (!plan.accepted()) {
                skip("plan/" + plan.rejection().name().toLowerCase(Locale.ROOT));
                continue;
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
                skip("output/binding-missing");
                continue;
            }
            if (outputs.size() == 1
                    && StackCompat.sameItem(stack, outputs.get(0))
                    && outputs.get(0).getCount() == 1) continue;
            var recipe = new Recipe(
                            new ItemStack[] {stack},
                            outputs.toArray(ItemStack[]::new),
                            null,
                            null,
                            null,
                            null,
                            plan.duration(),
                            16,
                            0)
                    .withMaterialRecovery();
            // Existing explicit GT6 processing recipes keep priority and their special yields.
            if (MachineRecipeMaps.Shredder.addRecipe(recipe) != null) RECIPES.add(recipe);
            else skip("existing-owner");
        }
        Mc.logger()
                .info("Registered {} composition-driven vanilla recovery recipes; skipped {}", RECIPES.size(), SKIPPED);
        return RECIPES.size();
    }

    private static void skip(String reason) {
        SKIPPED.merge(reason, 1, Integer::sum);
    }

    /** Admit audited source compositions; coarse legacy hull estimates are not recovery data. */
    private static boolean originalMachine(net.minecraft.world.item.Item item) {
        if (!(item instanceof net.minecraft.world.item.BlockItem blockItem)) return false;
        var block = blockItem.getBlock();
        var id = Mc.blockId(block);
        if (!id.getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)) return false;
        var expected = gregtech.content.machine.MachineConstructionMaterials.block(id.getPath());
        if (expected.isEmpty() && block instanceof gregtech.content.block.machine.BasicMachineBlock machine)
            expected = gregtech.content.machine.OriginalMachineMaterialData.find(
                    machine.basicSpec().machineName(), machine.basicSpec().tier());
        if (expected.isEmpty()) return false;
        var registered = ItemMaterialRegistry.base(item);
        return registered.isPresent()
                && registered.get().components().equals(expected.get().components());
    }
}
