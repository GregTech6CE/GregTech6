package gregtech.api.material;

import gregtech.block.stone.StoneType;
import gregtech.worldgen.GTWorldgenMaterials;

/** Original catalog roles, applied after linking and before platform item forms/post-init. */
public final class MaterialRoleFlags {
    private MaterialRoleFlags() {}

    public static void apply() {
        if (GTMaterialRegistry.registrationPhase() != GTMaterialRegistry.RegistrationPhase.READY) {
            throw new IllegalStateException("Material roles require a fully linked catalog");
        }
        GTWorldgenMaterials.flagOreMaterials();
        // Imported compound catalogs include natural rocks, notably both granites.
        for (StoneType stone : StoneType.values()) {
            stone.material().put(MaterialProperty.STONE);
        }
    }
}
