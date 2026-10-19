import com.gregtech.gregtech.api.energy.EnergyGate;
import com.gregtech.gregtech.api.energy.EnergyPackets;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.content.energy.GearboxCatalog;
import com.gregtech.gregtech.content.energy.GearboxRotationRules;
import com.gregtech.gregtech.data.GregTechTags;

/** Core observations; does not instantiate native entities, receivers, controls or rendering. */
class RotationTransformerAudit {
    public static void main(String[] args) {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        for (var tier : GearboxCatalog.tiers()) {
            var spec = GearboxCatalog.transformer(tier);
            System.out.println("TRANSFORMER|" + tier.suffix() + "|" + spec.material().getName()
                    + "|" + spec.inputRate() + "|" + spec.outputRate() + "|" + spec.capacity());
        }
        System.out.println("RU_NEGATIVE|" + EnergyPackets.magnitude(-32, true));
        System.out.println("LAST_PACKET|" + EnergyPackets.fitting(1, 4, 32));
        System.out.println("OVERSPEED_FITTING|" + EnergyPackets.fitting(1, 64, 65));
        System.out.println("OVERSPEED_GATE_CALLBACK|" + EnergyGate.gateInjection(
                GregTechTags.Energy.RU, true, 65, 16, 1, () -> 99));

        // Enumeration is an observation of the maintained helper, not a full entity comparison.
        int working = 0, nonzero = 0;
        for (int mask = 0; mask < 64; mask++) {
            for (int axis = 0; axis < 4; axis++) {
                if (GearboxRotationRules.gearsWork(mask, axis)) working++;
                for (int face = 0; face < 6; face++) {
                    for (boolean negative : new boolean[]{false, true}) {
                        if (GearboxRotationRules.rotations(mask, axis, face, negative) != 0) nonzero++;
                    }
                }
            }
        }
        System.out.println("GEAR_HELPER|working=" + working + "|nonzeroRotations=" + nonzero);
    }
}
