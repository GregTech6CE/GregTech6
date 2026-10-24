package gregtech.content.recipe;
import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.ItemMaterialRegistry;
import gregtech.internal.material.MaterialEquivalence;
import gregtech.internal.addon.AddonMaterials;
import gregtech.data.MaterialPrefix;
import gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
/** Recipe-local source availability. Never unregisters legacy items or changes their IDs.
 * Call after explicit external material-form registrations are available; reload rebuild must use
 * the same producer ownership boundary. Generic tags and chemical composition alone are not grades.
 */
public final class SourceGemGradeResolver {
    private SourceGemGradeResolver() {}
    public static ItemStack gradeOrGem(MaterialPrefix prefix,GTMaterial material,int count,
                                       ItemStack ordinaryGem,int fallbackCount) {
        material=material.resolve();
        // Original OM may obtain an externally registered grade despite a false generate condition.
        // Honour a canonical external target or an explicitly accepted extra-form declaration first.
        ItemStack canonical=GTItems.getStack(prefix,material,count);
        final String materialName=material.getName();
        boolean requested=AddonMaterials.acceptedForms().stream().anyMatch(f ->
                f.materialName().equals(materialName) && f.prefixName().equals(prefix.getName()));
        if(!canonical.isEmpty() && (requested || !BuiltInRegistries.ITEM.getKey(canonical.getItem())
                .getNamespace().equals("gt6ce"))) return canonical;
        ItemStack external=ItemStack.EMPTY;
        for(var item:ItemMaterialRegistry.entries().keySet()) {
            if(BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gt6ce")) continue;
            ItemStack stack=new ItemStack(item,count);
            var form=MaterialEquivalence.form(stack);
            if(form==null||form.prefix()!=prefix||form.material()!=material) continue;
            if(!external.isEmpty()&&external.getItem()!=item) {
                gregtech.internal.material.GTMaterialRegistry.warn(
                        "Unknown source grade target (multiple external mappings), retaining legacy selection: "
                        +prefix.getName()+"/"+material.getName());
                return canonical.isEmpty() ? copyAmount(ordinaryGem,fallbackCount) : canonical;
            }
            external=stack;
        }
        if(!external.isEmpty()) return external;
        var decision=ReviewedOreSourceProfile.decision(material,ReviewedOreSourceProfile.Rule.GRADED_GEM);
        if(decision!=ReviewedOreSourceProfile.Decision.NO&&!canonical.isEmpty()) return canonical;
        return copyAmount(ordinaryGem,fallbackCount);
    }
    private static ItemStack copyAmount(ItemStack input,int count) {
        ItemStack result=input.copy();result.setCount(count);return result;
    }
}
