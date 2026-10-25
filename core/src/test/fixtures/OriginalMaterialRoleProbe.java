import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.platform.neoforge.gametest.SharedMaterialBootstrapGameTests;
import java.util.Locale;

/** Runs ORIGINAL imported material/role rules under inert game stubs, never a game startup test. */
public final class OriginalMaterialRoleProbe {
    private OriginalMaterialRoleProbe() {}
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        PrefixRegistry.ensurePrefixesLoaded();
        com.gregtech.gregtech.data.ModReferences.UNKNOWN.getClass();
        com.gregtech.gregtech.data.MaterialGroups.Glowstone.getClass();
        GTMaterialRegistry.init();
        // Exactly the original Loader_Materials order, without calling the extracted helper.
        com.gregtech.gregtech.worldgen.GTWorldgenMaterials.flagOreMaterials();
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
