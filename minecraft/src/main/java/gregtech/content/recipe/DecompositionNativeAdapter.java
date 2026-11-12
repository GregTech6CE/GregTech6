package gregtech.content.recipe;

import gregtech.engine.recipe.UnitConversion;

import gregtech.api.addon.material.MaterialDecompositionRule;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.recipe.generated.GeneratedRecipeResolver;
import gregtech.content.registry.GTFluids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Producer-local native adapter. It resolves declared identities without owning decomposition facts. */
final class DecompositionNativeAdapter {
    private DecompositionNativeAdapter() {}

    static FluidStack fluid(MaterialDecompositionRule.FluidBinding binding, long amount, boolean up) {
        if (binding == null) return null;
        int mb = UnitConversion.fluidAmount(
                binding.sourceUnits(), binding.millibuckets(), amount, up);
        return fluid(binding.fluid(), mb);
    }

    static FluidStack fluid(
            String registry, int mb, boolean declared, String generatedMaterial) {
        if (declared) return fluid(registry, mb);
        FluidStack named = GeneratedRecipeResolver.namedFluid(generatedMaterial, mb);
        if (named != null && !named.isEmpty()) return named;
        return GeneratedRecipeResolver.materialFluid(generatedMaterial, mb);
    }

    static FluidStack fluid(String registry, int mb) {
        if (registry == null || mb <= 0) return null;
        if (registry.indexOf(':') >= 0) {
            ResourceLocation id = ResourceLocation.tryParse(registry);
            if (id == null || !BuiltInRegistries.FLUID.containsKey(id)) return null;
            var fluid = BuiltInRegistries.FLUID.get(id);
            return fluid == Fluids.EMPTY || !id.equals(BuiltInRegistries.FLUID.getKey(fluid))
                    ? null : new FluidStack(fluid, mb);
        }
        // Water is vanilla water; it has no GT catalog entry.
        if ("water".equals(registry))
            return new FluidStack(Fluids.WATER, mb);
        // sanitizePath is the port's declared source-registry translation, not material-name aliasing.
        String path = RegisteredFluids.sanitizePath(registry);
        for (var field : RegisteredFluids.all().entrySet()) {
            var e = field.getValue();
            if (!RegisteredFluids.sanitizePath(e.registryName()).equals(path)) continue;
            var value = GeneratedRecipeResolver.namedFluid(field.getKey(), mb);
            if (value == null || value.isEmpty()) continue;
            var id = BuiltInRegistries.FLUID.getKey(value.getFluid());
            if (id != null && (id.getNamespace().equals(GTFluids.namespace(e)) && id.getPath().equals(path)
                    || e.usesVanillaFluid() && id.getNamespace().equals("minecraft") && id.getPath().equals(path)))
                return value;
        }
        return null;
    }
}
