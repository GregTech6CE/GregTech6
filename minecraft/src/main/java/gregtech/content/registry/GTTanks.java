package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.GTMachineBlockItem;
import gregtech.content.block.machine.TankBlock;
import gregtech.content.transport.fluid.FluidTransportDefinitions;
import gregtech.engine.machine.TankSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.Item;

/** GT6 fluid container blocks, populated from the shared transport catalog. */
public final class GTTanks {
    private static final List<RegisteredBlock<TankBlock>> ALL = new ArrayList<>();

    private GTTanks() {}

    public static RegisteredBlock<TankBlock> WOOD_BARREL;
    public static RegisteredBlock<TankBlock> DRUM_BRONZE;
    public static RegisteredBlock<TankBlock> DRUM_STEEL;
    /** GT6 32072, a one-million-litre direct logistics storage node. */
    public static RegisteredBlock<TankBlock> LOGISTICS_TANK;

    public static void registerAll(ContentRegistrar registrar) {
        for (var spec : FluidTransportDefinitions.tanks()) {
            var block = register(registrar, spec);
            if (spec.id().equals("wood_barrel")) WOOD_BARREL = block;
            if (spec.id().equals("drum_bronze")) DRUM_BRONZE = block;
            if (spec.id().equals("drum_steel")) DRUM_STEEL = block;
        }
        LOGISTICS_TANK = registerLogisticsTank(registrar);
    }

    public static RegisteredBlock<TankBlock> register(ContentRegistrar registrar, TankSpec spec) {
        String id = spec.id();
        RegisteredBlock<TankBlock> block = registrar.block(
                REGISTRY_NAMESPACE,
                id,
                () -> new TankBlock(spec, TankBlock.defaultProperties(spec)),
                bare -> new GTMachineBlockItem(bare, new Item.Properties().stacksTo(16)));
        ALL.add(block);
        return block;
    }

    public static RegisteredBlock<TankBlock> registerLogisticsTank(ContentRegistrar registrar) {
        String id = "logistics_tank";
        TankSpec spec = gregtech.content.transport.fluid.LogisticsTankSpec.spec();
        RegisteredBlock<TankBlock> block = registrar.block(
                REGISTRY_NAMESPACE,
                id,
                () -> new gregtech.content.block.machine.LogisticsTankBlock(spec, TankBlock.defaultProperties(spec)),
                bare -> new GTMachineBlockItem(bare, new Item.Properties().stacksTo(16)));
        ALL.add(block);
        return block;
    }

    public static List<RegisteredBlock<TankBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }
}
