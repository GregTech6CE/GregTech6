import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.material.MaterialProperty;
import gregtech.internal.prefix.PrefixRegistry;
import gregtech.content.block.stone.StoneType;
import gregtech.platform.neoforge.gametest.SharedMaterialBootstrapGameTests;
import java.util.Locale;

/** Runs ORIGINAL imported material/role rules under inert game stubs, never a game startup test. */
public final class OriginalMaterialRoleProbe {
    private OriginalMaterialRoleProbe() {}
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        PrefixRegistry.ensurePrefixesLoaded();
        gregtech.content.data.ModReferences.UNKNOWN.getClass();
        gregtech.content.data.MaterialGroups.Glowstone.getClass();
        GTMaterialRegistry.init();
        // Exactly the original Loader_Materials order, without calling the extracted helper.
        gregtech.content.worldgen.GTWorldgenMaterials.flagOreMaterials();
        for (StoneType stone : StoneType.values()) stone.material().put(MaterialProperty.STONE);
        GTMaterialRegistry.postInit();
        System.out.println("role_sha256=" + SharedMaterialBootstrapGameTests.roleSnapshotSha256());
        System.out.println("ore=" + GTMaterialRegistry.allMaterials().stream().filter(m -> m.has(MaterialProperty.GENERATE_ORE)).count());
        System.out.println("ore_processing=" + GTMaterialRegistry.allMaterials().stream().filter(m -> m.has(MaterialProperty.GENERATE_ORE_PROCESSING)).count());
        System.out.println("stone=" + GTMaterialRegistry.allMaterials().stream().filter(m -> m.has(MaterialProperty.STONE)).count());
        System.out.println("stone_descriptors=" + StoneType.values().length);
        if (args.length > 0 && args[0].equals("check")) {
            var helper = new net.minecraft.gametest.framework.GameTestHelper();
            SharedMaterialBootstrapGameTests.completeMaterialDirectory(helper);
            SharedMaterialBootstrapGameTests.originalRolesUnitsAndComposition(helper);
            SharedMaterialBootstrapGameTests.originalCopperTinReaction(helper);
            SharedMaterialBootstrapGameTests.originalAlternateReactions(helper);
            System.out.println("Four shared-material fixtures passed against ORIGINAL rules under inert game stubs.");
        }
    }
}
