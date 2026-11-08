package gregtech.engine.material;

import gregtech.api.material.*;
import gregtech.content.block.stone.StoneType;
import gregtech.content.material.SourceLatheMaterialProfiles;
import gregtech.content.worldgen.GTWorldgenMaterials;

/** Original catalog roles, applied after linking and before platform item forms/post-init. */
public final class MaterialRoleFlags {
    private MaterialRoleFlags() {}

    public static void apply() {
        if (GTMaterialRegistry.registrationPhase() != GTMaterialRegistry.RegistrationPhase.READY) {
            throw new IllegalStateException("Material roles require a fully linked catalog");
        }
    }

    /** Final derived pass, after addon form publication and before immutable views are captured. */
    static void applyDerived() {
        GTWorldgenMaterials.flagOreMaterials();
        // Imported compound catalogs include natural rocks, notably both granites.
        for (StoneType stone : StoneType.values()) {
            SourceLatheMaterialProfiles.applyOfficialStoneRole(stone);
        }
    }
}
