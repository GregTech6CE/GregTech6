package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.content.machine.BasicMachineDefinitions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import java.util.List;

/** Forge registration adapter for built-in and addon machine specifications. */
public final class GTBasicMachines {
    private GTBasicMachines() {}
    public static void registerAll(IEventBus bus) {
        for (var spec : BasicMachineDefinitions.specifications()) {
            if ("gregtech".equals(spec.namespace())) MachineRegistry.registerBasicMachine(spec);
            else AddonMachineRegistration.register(spec, bus);
        }
    }
    public static List<RegistryObject<BasicMachineBlock>> all() {
        return MachineRegistry.basicMachines();
    }
}
