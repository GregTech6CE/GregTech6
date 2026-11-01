package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.ArrayList;
import java.util.List;

/** Retained source wax identities used by mineral steam cracking. */
public final class SourceWaxMaterials {
    private SourceWaxMaterials() {}

    private record Identity(int id, String name) {}

    private static final List<Identity> WAXES = List.of(
            new Identity(8235, "Wax"),
            new Identity(8236, "WaxBee"),
            new Identity(8238, "WaxParaffin"),
            new Identity(8239, "WaxPlant"));

    public static List<GTMaterial> materials() {
        var result = new ArrayList<GTMaterial>();
        for (var identity : WAXES) {
            var material = GTMaterialRegistry.get(identity.name());
            if (material != null
                    && material.isValid()
                    && !AddonMaterials.owns(material)
                    && GTMaterialRegistry.isCoreMaterial(material.resolve())
                    && material.resolve().getId() == identity.id()
                    && material.resolve().getName().equals(identity.name())) result.add(material);
        }
        return List.copyOf(result);
    }
}
