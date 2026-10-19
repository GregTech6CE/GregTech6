import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions;

/** Catalog observations and existing core channel/hazard contracts, not native world execution. */
class FluidPipeAudit {
    public static void main(String[] args) throws ReflectiveOperationException {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        for (var spec : FluidTransportDefinitions.pipes()) {
            System.out.println("PIPE|" + spec.id() + "|" + spec.material().getName() + "|" + spec.size()
                    + "|" + spec.capacity() + "|" + spec.tankCount() + "|" + spec.diameter()
                    + "|" + spec.gasProof() + "|" + spec.acidProof() + "|" + spec.plasmaProof()
                    + "|" + spec.magicProof() + "|" + spec.maxTemperature() + "|" + spec.flammability()
                    + "|" + spec.material().getMeltingPoint());
        }
        var contracts = Class.forName("com.gregtech.gregtech.core.CoreBehaviorContracts");
        var assertions = contracts.getDeclaredField("assertions");
        assertions.setAccessible(true);
        for (String method : new String[]{"fluidPipeCatalog", "fluidPipeChannels", "fluidPipeSafety"}) {
            int previous = assertions.getInt(null);
            var test = contracts.getDeclaredMethod(method);
            test.setAccessible(true);
            test.invoke(null);
            System.out.println("CONTRACTS|" + method + "|" + (assertions.getInt(null) - previous));
        }
    }
}
