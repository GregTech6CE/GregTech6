import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.content.fluid.FluidDefinitions;
import com.gregtech.gregtech.content.recipe.MatterConversionCatalog;
import com.gregtech.gregtech.data.FluidCatalog;
import java.util.Set;

/** Catalog evidence only: no Minecraft fluid registration or native recipe execution. */
class MatterFluidAudit {
    public static void main(String[] args) {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        FluidDefinitions.prepare();
        int missedBoundElements = 0;
        for (var row : FluidCatalog.all().entrySet()) {
            var fluid = row.getValue();
            String bound = FluidCatalog.boundMaterial(fluid.registryName());
            if (fluid.materialKey() == null && bound != null && !fluid.isHidden()) {
                var material = GTMaterialRegistry.get(bound).resolve();
                if (material.has(MaterialProperty.ELEMENT) && !material.has(MaterialProperty.ANTIMATTER)
                        && material.getProtons() + material.getNeutrons() > 0) {
                    missedBoundElements++;
                    System.out.println("MISSED_BOUND|" + row.getKey() + "|" + fluid.registryName() + "|" + bound);
                }
            }
            if (fluid.materialKey() != null && Set.of("Mercury", "Iron").contains(fluid.materialKey())
                    || Set.of("HeliumPlasma", "NitrogenPlasma", "Hydrogen").contains(row.getKey())) {
                int loaderAmount = fluid.registryName().startsWith("molten.")
                        || fluid.registryName().endsWith(".molten") ? 144 : 1000;
                System.out.println("FLUID|" + row.getKey() + "|" + fluid.registryName() + "|"
                        + fluid.materialKey() + "|" + bound + "|hidden=" + fluid.isHidden()
                        + "|loaderAmount=" + loaderAmount);
            }
        }
        System.out.println("MISSED_BOUND_COUNT|" + missedBoundElements);
        var mercury = GTMaterialRegistry.get("Mercury");
        System.out.println("MERCURY_OUTPUT|" + mercury.getProtons() + "|" + mercury.getNeutrons()
                + "|" + (mercury.getProtons() + mercury.getNeutrons()) * 131072L);
        for (var conversion : MatterConversionCatalog.SPECIAL_CONVERSIONS) {
            System.out.println("SPECIAL|" + conversion.material() + "|" + conversion.form() + "|"
                    + conversion.circuit() + "|" + conversion.duration() + "|"
                    + conversion.outputFluid() + "|" + conversion.outputAmount());
        }
    }
}
