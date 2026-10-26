package gregtech.content.recipe;

import gregtech.engine.addon.AddonMaterials;
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import net.minecraft.world.item.ItemStack;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.recipe.generated.GeneratedRecipeResolver;
import net.minecraft.core.registries.BuiltInRegistries;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.HashSet;
import java.util.Set;

/** Producer-local source phase adapter. Never deletes rows or modifies global fluid registration. */
final class SourceDecompositionNativeFacts {
    private SourceDecompositionNativeFacts() {}
    private static boolean core(GTMaterial m) {
        return m != null && m.isValid() && m.resolve() == m
                && GTMaterialRegistry.isCoreMaterial(m) && !AddonMaterials.owns(m);
    }
    static boolean supports(GTMaterial material) {
        if (!core(material) || SourceDecompositionData.input(material.getId(), material.getName()) == null)
            return false;
        Set<String> relevant = new HashSet<>();
        relevant.add(material.getName());
        for (var component : material.getCompositionComponents()) {
            var part = component.material().resolve();
            if (!core(part)) return false;
            relevant.add(part.getName());
            var pulver = part.getTargetPulverMaterial();
            if (!core(pulver)) return false;
            relevant.add(pulver.getName()); // Dust output identity is one hop, not target-of-target.
            if (part.getMeltingPoint() <= 293) {
                var t = SourceDecompositionData.thermal(part.getId(), part.getName());
                if (t == null || t.melting() != part.getMeltingPoint() || t.boiling() != part.getBoilingPoint())
                    return false;
            }
        }
        // Unrelated addon material/plate declarations do not disable this producer. Relevant external
        // dust forms or foreign material-fluid bindings defer rather than inherit source default NONE.
        if (AddonMaterials.acceptedForms().stream().anyMatch(f -> relevant.contains(f.materialName())
                && Set.of("dust", "dustTiny", "dustSmall", "dustDiv72", "blockDust").contains(f.prefixName())))
            return false;
        Set<String> dustRoles = Set.of("dust", "dustTiny", "dustSmall", "dustDiv72", "blockDust");
        for (var entry : ItemMaterialRegistry.entries().entrySet()) {
            var id = BuiltInRegistries.ITEM.getKey(entry.getKey());
            if (id == null || id.getNamespace().equals("gt6ce") || id.getNamespace().equals("minecraft")) continue;
            var form = MaterialEquivalence.form(new ItemStack(entry.getKey()));
            if (form != null && dustRoles.contains(form.prefix().getName())
                    && relevant.contains(form.material().resolve().getName())) return false;
            var explicit = entry.getValue();
            // A foreign unprefixed composition may be a block-dust equivalent; do not infer the role.
            // Defer conservatively, rather than changing its producer semantics from incomplete proof.
            if ((explicit.prefix() == null || dustRoles.contains(explicit.prefix().getName()))
                    && explicit.components().stream().anyMatch(c -> relevant.contains(c.material().resolve().getName())))
                return false;
        }
        for (var field : RegisteredFluids.all().entrySet()) {
            var e = field.getValue();
            String bound = e.materialKey() != null ? e.materialKey() : RegisteredFluids.boundMaterial(e.registryName());
            if (bound == null) continue;
            var boundMaterial = GeneratedRecipeResolver.material(bound);
            if (boundMaterial == null || !relevant.contains(boundMaterial.getName())) continue;
            if (!RegisteredFluids.isBuiltInDefinition(e)
                    && !RegisteredFluids.isGeneratedGasLiquidDefinition(field.getKey(), e)
                    && !RegisteredFluids.isGeneratedMoltenDefinition(field.getKey(), e)) return false;
        }
        return true;
    }
    static FluidStack fluid(SourceDecompositionRules.Binding binding, long amount, boolean up) {
        int mb = SourceDecompositionRules.fluidAmount(binding, amount, up);
        if (binding == null || mb <= 0) return null;
        // sanitizePath is the port's declared source-registry translation, not material-name aliasing.
        String path = RegisteredFluids.sanitizePath(binding.registry());
        for (var field : RegisteredFluids.all().entrySet()) {
            var e = field.getValue();
            if (!RegisteredFluids.sanitizePath(e.registryName()).equals(path)) continue;
            var value = GeneratedRecipeResolver.namedFluid(field.getKey(), mb);
            if (value == null || value.isEmpty()) continue;
            var id = BuiltInRegistries.FLUID.getKey(value.getFluid());
            if (id != null && (id.getNamespace().equals("gt6ce") && id.getPath().equals(path)
                    || e.usesVanillaFluid() && id.getNamespace().equals("minecraft") && id.getPath().equals(path)))
                return value;
        }
        return null;
    }
    static FluidStack ambient(GTMaterial material, long amount) {
        var t = SourceDecompositionData.thermal(material.getId(), material.getName());
        return fluid(SourceDecompositionRules.ambient(t, 293), amount, false);
    }
}
