import com.gregtech.gregtech.content.multiblock.LargeMachineProcessingRules;
import com.gregtech.gregtech.content.multiblock.SharedMatterFabricatorStructure;
import com.gregtech.gregtech.data.BasicMachineOriginalParams;
import java.util.TreeMap;

/** Source-coordinate and parameter observation only; no native controller execution. */
class MatterControllerAudit {
    public static void main(String[] args) {
        var counts = new TreeMap<Integer, Integer>();
        for (var cell : SharedMatterFabricatorStructure.CELLS) {
            counts.merge(cell.part(), 1, Integer::sum);
            System.out.println("CELL|" + cell.right() + "|" + cell.up() + "|" + cell.back()
                    + "|" + cell.part() + "|" + cell.role());
        }
        System.out.println("PARTS|" + counts);
        var large = BasicMachineOriginalParams.find("largemassfab", 1);
        System.out.println("LARGE|" + large.energyType() + "|" + large.energyInput() + "|"
                + large.energyInputMin() + "|" + large.energyInputMax() + "|" + large.parallel()
                + "|" + large.parallelDuration());
        for (int tier = 1; tier <= 5; tier++) {
            var compact = BasicMachineOriginalParams.find("massfab", tier);
            System.out.println("COMPACT|" + tier + "|" + compact.energyType() + "|"
                    + compact.energyInput() + "|" + LargeMachineProcessingRules.compactMatterEfficiency(tier));
        }
    }
}
