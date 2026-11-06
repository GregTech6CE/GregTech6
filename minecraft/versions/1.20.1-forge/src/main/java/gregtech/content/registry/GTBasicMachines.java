package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.machine.BasicMachineDefinitions;
import gregtech.engine.machine.MachineRegistry;
import java.util.List;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

/** Forge registration adapter for built-in and addon machine specifications. */
public final class GTBasicMachines {
    private GTBasicMachines() {}

    public static void registerAll(IEventBus bus) {
        for (var spec : BasicMachineDefinitions.specifications()) {
            if (REGISTRY_NAMESPACE.equals(spec.namespace())) MachineRegistry.registerBasicMachine(spec);
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
