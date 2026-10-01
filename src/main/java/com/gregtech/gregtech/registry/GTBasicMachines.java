package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.content.machine.BasicMachineDefinitions;
import net.minecraftforge.registries.RegistryObject;
import java.util.List;

/** Forge registration adapter for built-in machine specifications. */
public final class GTBasicMachines {
    private GTBasicMachines() {}
    public static void registerAll() {
        BasicMachineDefinitions.specifications().forEach(MachineRegistry::registerBasicMachine);
    }
    public static List<RegistryObject<BasicMachineBlock>> all() {
        return MachineRegistry.basicMachines();
    }
}
