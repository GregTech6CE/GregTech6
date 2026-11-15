package gregtech.engine.fluid;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.RegisteredFluids;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialIconDefinitions;
import gregtech.engine.mod.GregTechIdentity;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/** Texture and multiplication color are one decision. Original named PNGs are precolored;
 * material templates and shared dye textures require a color multiplier.
 */
public final class FluidVisualPolicy {
    public record Visual(ResourceLocation texture, int tint) {}

    private FluidVisualPolicy() {}

    public static Visual select(RegisteredFluids.FluidEntry entry, Predicate<ResourceLocation> exists) {
        if (entry.textureMode() == RegisteredFluids.FluidTextureMode.VANILLA_LAVA)
            return new Visual(ResourceLocation.withDefaultNamespace("block/lava_still"), 0xFFFFFFFF);
        String name = entry.registryName();
        var dedicated = RegisteredFluids.fluidTexture(name);
        if (exists.test(dedicated)) return new Visual(dedicated, explicitTint(entry));
        // Imported generated aliases can share their original, named fluid texture.
        if (name.startsWith("gas.") || name.startsWith("liquid.")) {
            var named = RegisteredFluids.fluidTexture(name.substring(name.indexOf('.') + 1));
            if (exists.test(named)) return new Visual(named, 0xFFFFFFFF);
        }
        String shared = name.startsWith("dye.watermixed.")
                ? "dyes.water"
                : name.startsWith("dye.flower.")
                        ? "dyes.flower"
                        : name.startsWith("dye.chemical.")
                                ? "dyes.chemical"
                                : name.startsWith("cfoam.") ? "cfoam" : null;
        if (shared != null) {
            var texture = RegisteredFluids.fluidTexture(shared);
            if (exists.test(texture)) return new Visual(texture, rawTint(entry));
        }
        var kind = FluidTexturePolicy.kind(entry);
        var template = kind.standard();
        var material = material(entry);
        // Molten metal icons exist per material texture set; the other phases share one template.
        if (material.isValid() && kind == FluidTexturePolicy.Kind.MOLTEN) {
            String folder = MaterialIconDefinitions.resolveTextureSet(material).folder();
            template = ResourceLocation.fromNamespaceAndPath(
                    GregTechIdentity.REGISTRY_NAMESPACE, "block/material_icons/" + folder + "/molten");
        }
        return new Visual(FluidTexturePolicy.select(template, kind, exists), rawTint(entry));
    }

    private static int explicitTint(RegisteredFluids.FluidEntry entry) {
        return entry.textureMode() == RegisteredFluids.FluidTextureMode.DEDICATED
                        && entry.tintTexture()
                        && entry.tintArgb() != 0
                ? entry.tintArgb() | 0xFF000000
                : 0xFFFFFFFF;
    }

    public static GTMaterial material(RegisteredFluids.FluidEntry entry) {
        String key = entry.materialKey();
        if (key == null) key = RegisteredFluids.boundMaterial(entry.registryName());
        if (key == null) key = entry.registryName().replaceFirst("^(liquid[._]|gas[._]|molten[._]|plasma[._])", "");
        return GTMaterialRegistry.get(key).resolve();
    }

    public static int rawTint(RegisteredFluids.FluidEntry entry) {
        var material = material(entry);
        if (entry.tintArgb() != 0) return entry.tintArgb() | 0xFF000000;
        if (FluidTexturePolicy.kind(entry) == FluidTexturePolicy.Kind.MOLTEN && material.isValid())
            return moltenTint(material);
        String name = entry.registryName();
        if (name.startsWith("dye.") || name.startsWith("cfoam."))
            return dyeTint(name.substring(name.lastIndexOf('.') + 1));
        return material.isValid() ? material.getColor() | 0xFF000000 : 0xFFFFFFFF;
    }
    /** MT.setRGBaLiquid values, before multiplying by the original grayscale sprite. */
    public static int moltenTint(GTMaterial material) {
        return FluidTintRules.moltenTint(material);
    }

    private static int dyeTint(String name) {
        return FluidTintRules.dyeTint(name);
    }
}
