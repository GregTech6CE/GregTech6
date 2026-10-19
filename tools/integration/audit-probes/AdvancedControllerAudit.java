import com.gregtech.gregtech.content.multiblock.OriginalAdvancedControllerData;
import com.gregtech.gregtech.content.multiblock.SharedFusionStructure;
import com.gregtech.gregtech.content.multiblock.SharedImplosionStructure;
import com.gregtech.gregtech.data.BasicMachineOriginalParams;
import java.util.TreeMap;

/** Observes shared definitions only; does not execute Minecraft controller ticks. */
class AdvancedControllerAudit {
    public static void main(String[] args) {
        var parts = new TreeMap<Integer, Integer>();
        var roles = new TreeMap<String, Integer>();
        for (var cell : SharedFusionStructure.CELLS) {
            parts.merge(cell.part(), 1, Integer::sum);
            roles.merge(cell.role().name(), 1, Integer::sum);
        }
        System.out.println("FUSION_PARTS|" + parts);
        System.out.println("FUSION_ROLES|" + roles);
        var implosion = new TreeMap<String, Integer>();
        for (var cell : SharedImplosionStructure.LAYOUT.cells()) {
            implosion.merge(cell.role().name(), 1, Integer::sum);
        }
        System.out.println("IMPLOSION_ROLES|" + implosion);
        for (var name : new String[]{"fusionreactor", "implosioncompressor"}) {
            var p = BasicMachineOriginalParams.find(name, 1);
            System.out.println("PARAMS|" + name + "|" + p.energyType() + "|" + p.energyInput()
                    + "|" + p.energyInputMin() + "|" + p.energyInputMax() + "|" + p.parallel()
                    + "|" + OriginalAdvancedControllerData.chargedEnergy(name));
        }
    }
}
