import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.content.energy.WireCatalog;
import com.gregtech.gregtech.content.energy.WirePacketRules;

/** Catalog and arithmetic observations only; no native transfer, tick or collision simulation. */
class WireCatalogAudit {
    public static void main(String[] args) {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        for (var spec : WireCatalog.specifications()) {
            System.out.println("WIRE|" + WireCatalog.registryId(spec) + "|" + spec.material().getName()
                    + "|" + spec.size() + "|" + spec.voltage() + "|" + spec.amperage()
                    + "|" + spec.lossPerBlock() + "|" + spec.insulated() + "|" + spec.contactDamage()
                    + "|" + spec.thickness() + "|" + WireCatalog.stackSize(spec.size(), spec.insulated())
                    + "|" + spec.materialAmount());
        }
        System.out.println("AT_LOSS|" + WirePacketRules.canTransfer(2, 1, 2));
        System.out.println("ABOVE_LOSS|" + WirePacketRules.canTransfer(3, 1, 2));
        System.out.println("ZERO_PACKETS|" + WirePacketRules.canTransfer(32, 0, 2));
        System.out.println("MIN_VOLTAGE|" + WirePacketRules.canTransfer(Long.MIN_VALUE, 1, 2));
        System.out.println("POSITIVE_LOSS|" + WirePacketRules.afterLoss(32, 2));
        System.out.println("NEGATIVE_LOSS|" + WirePacketRules.afterLoss(-32, 2));
        System.out.println("WATTAGE|" + WirePacketRules.wattage(30, 4));
        System.out.println("WATTAGE_SATURATION|" + WirePacketRules.wattage(Long.MAX_VALUE, 2));
        System.out.println("COUNTER_SATURATION|" + WirePacketRules.saturatedAdd(Long.MAX_VALUE - 1, 2));
    }
}
