import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.content.energy.AxleCatalog;
import com.gregtech.gregtech.content.energy.AxleConnectionRules;

/** Core catalog observations, not native gearbox ticking or overload execution. */
class AxleCatalogAudit {
    public static void main(String[] args) {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        for (var spec : AxleCatalog.specifications()) {
            System.out.println("AXLE|" + spec.id() + "|" + spec.material().getName() + "|" + spec.size()
                    + "|" + spec.maxSpeed() + "|" + spec.maxPower() + "|" + spec.lossPerBlock()
                    + "|" + spec.thickness());
        }
        int valid = 0;
        for (int mask = 0; mask < 64; mask++) {
            if (AxleConnectionRules.valid(mask)) valid++;
        }
        System.out.println("VALID_SIX_FACE_MASKS|" + valid);
    }
}
