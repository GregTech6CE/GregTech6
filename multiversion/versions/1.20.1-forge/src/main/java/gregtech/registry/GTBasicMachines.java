package gregtech.registry;

import gregtech.internal.machine.MachineRegistry;
import gregtech.block.machine.BasicMachineBlock;
import gregtech.content.machine.BasicMachineDefinitions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import java.util.List;

/** Forge registration adapter for built-in and addon machine specifications. */
public final class GTBasicMachines {
    private GTBasicMachines() {}
    public static void registerAll(IEventBus bus) {
        for (var spec : BasicMachineDefinitions.specifications()) {
            if ("gt6ce".equals(spec.namespace())) MachineRegistry.registerBasicMachine(spec);
            else AddonMachineRegistration.register(spec, bus);
        }
        BehaviorMachineRegistration.register(bus);
        FuelGeneratorRegistration.register(bus);
        CompressedGasTankRegistration.register(bus);
    }
    public static List<RegistryObject<BasicMachineBlock>> all() {
        return MachineRegistry.basicMachines();
    }
}
