import com.gregtech.gregtech.api.machine.BasicMachineEnergy;
import com.gregtech.gregtech.api.machine.PistonEngineState;
import com.gregtech.gregtech.content.machine.OriginalBasicMachineRules;
import com.gregtech.gregtech.content.multiblock.LargeMachineProcessingRules;
import com.gregtech.gregtech.data.BasicMachineOriginalParams;
import java.util.TreeSet;

/** Core observations only: no native block, input gate, controller tick or saved world is run. */
class BasicMachineInterruptAudit {
    public static void main(String[] args) {
        long variants = BasicMachineOriginalParams.PARAMS.stream()
                .filter(p -> OriginalBasicMachineRules.handles(p.machine(), p.tier())).count();
        var families = new TreeSet<String>();
        long kinetic = BasicMachineOriginalParams.PARAMS.stream().filter(p -> {
            if (!p.energyType().equals("KU")) return false;
            families.add(p.machine());
            return true;
        }).count();
        System.out.println("CATALOG|handled|" + variants + "|KU|" + kinetic + "|" + families);
        System.out.println("NO_CONSTANT_POWER|centrifuge1|"
                + OriginalBasicMachineRules.noConstantPower("centrifuge", 1) + "|bath1|"
                + OriginalBasicMachineRules.noConstantPower("bath", 1));
        System.out.println("CONSTANT_POWER|largecentrifuge|"
                + LargeMachineProcessingRules.constantEnergy(false, "largecentrifuge") + "|largecrusher|"
                + LargeMachineProcessingRules.constantEnergy(false, "largecrusher"));

        var engine = new PistonEngineState();
        engine.restore(128, 15, 0, true, false);
        long positive = engine.tick(1, 32, 32);
        engine.restore(128, 15, 1, true, false);
        long negative = engine.tick(17, 32, 32);
        System.out.println("ENGINE_PACKETS|" + positive + "|" + negative);
        System.out.println("BUFFER_SIGNS|" + BasicMachineEnergy.add(0, 64, positive, 1)
                + "|" + BasicMachineEnergy.add(0, 64, negative, 1));

        long initial = BasicMachineEnergy.add(0, 64, 20, 3);
        long accepted = BasicMachineEnergy.accepted(initial, 64, 32, 1);
        long current = BasicMachineEnergy.add(initial, 64, 32, accepted);
        System.out.println("BUFFER_BOUNDARY|" + initial + "|" + accepted + "|" + current
                + "|after-drain|" + BasicMachineEnergy.drain(current, 64));
    }
}
