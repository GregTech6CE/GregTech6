import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.core.MaterialCatalogSnapshot;
import java.util.Locale;

public class MaterialBaselineProbe {
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        try {
            com.gregtech.gregtech.api.mod.ModData.class.getMethod("bindPresence", java.util.function.Predicate.class)
                    .invoke(null, (java.util.function.Predicate<String>) id -> id.equals("minecraft") || id.equals("gregtech"));
        } catch (NoSuchMethodException originalPlatformBinding) {}
        PrefixRegistry.ensurePrefixesLoaded();
        com.gregtech.gregtech.data.ModReferences.UNKNOWN.getClass();
        com.gregtech.gregtech.data.MaterialGroups.Glowstone.getClass();
        GTMaterialRegistry.init();
        System.out.println("declared=" + GTMaterialRegistry.allMaterials().size());
        System.out.println("aliases=" + MaterialCatalogSnapshot.aliases().size());
        System.out.println("prefixes=" + PrefixRegistry.all().size());
        System.out.println("definitions_sha256=" + MaterialCatalogSnapshot.sha256());
        GTMaterialRegistry.postInit();
        System.out.println("post_init_sha256=" + MaterialCatalogSnapshot.sha256());
        System.out.println("positive_ids=" + GTMaterialRegistry.allMaterials().stream().filter(m -> m.getId() > 0).count());
        System.out.println("explicit_reactions=" + com.gregtech.gregtech.api.machine.crucible.CrucibleReactions.recipes().size());
        System.out.println("all_reactions=" + com.gregtech.gregtech.api.machine.crucible.CrucibleReactions.allRecipes().size());
    }
}
