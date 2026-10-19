import com.gregtech.gregtech.api.machine.ItemPipeSpec;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.content.transport.ItemPipeCatalog;

/** Shared catalog observations and the existing focused routing/delivery contracts, not native ticks. */
class ItemPipeAudit {
    public static void main(String[] args) throws ReflectiveOperationException {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        for (var family : ItemPipeCatalog.ITEM_PIPE_MATS) {
            for (var size : ItemPipeSpec.ItemPipeSize.values()) {
                String id = "item_pipe_" + size.name().toLowerCase(java.util.Locale.ROOT) + "_" + family.idSuffix();
                var spec = ItemPipeSpec.of(id, family.material(), size, family.stepSize(), family.invSize(), true);
                System.out.println("PIPE|" + spec.id() + "|" + spec.material().getName() + "|" + size
                        + "|" + spec.stepSize() + "|" + spec.invSize() + "|" + spec.diameter()
                        + "|" + spec.restrictive() + "|" + spec.blocking());
            }
        }
        var contracts = Class.forName("com.gregtech.gregtech.core.CoreBehaviorContracts");
        var test = contracts.getDeclaredMethod("itemPipeRoutingAndDelivery");
        test.setAccessible(true);
        test.invoke(null);
        var assertions = contracts.getDeclaredField("assertions");
        assertions.setAccessible(true);
        System.out.println("CORE_ROUTE_DELIVERY_ASSERTIONS|" + assertions.getInt(null));
    }
}
