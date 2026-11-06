package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.machine.GTMachineBlockItem;
import gregtech.content.block.machine.TankBlock;
import gregtech.engine.machine.MachineSpec;
import gregtech.engine.machine.TankSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.Item;

/** GT6 fluid container blocks — holder populated by {@code Loader_MultiTileEntities}. */
public final class GTTanks {
    private static final List<RegisteredBlock<TankBlock>> ALL = new ArrayList<>();

    private GTTanks() {}

    public static RegisteredBlock<TankBlock> WOOD_BARREL;
    public static RegisteredBlock<TankBlock> DRUM_BRONZE;
    public static RegisteredBlock<TankBlock> DRUM_STEEL;
    /** GT6 32072, a one-million-litre direct logistics storage node. */
    public static RegisteredBlock<TankBlock> LOGISTICS_TANK;

    public static RegisteredBlock<TankBlock> register(
            ContentRegistrar registrar,
            String id,
            GTMaterial material,
            TankSpec.TankType type,
            long capacity,
            boolean gasProof,
            boolean acidProof,
            boolean plasmaProof,
            boolean magicProof,
            boolean simpleOnly,
            float hardness,
            float blastResistance) {
        TankSpec spec = TankSpec.of(
                id,
                material,
                type,
                capacity,
                gasProof,
                acidProof,
                plasmaProof,
                magicProof,
                simpleOnly,
                hardness,
                blastResistance);
        return register(registrar, spec);
    }

    public static RegisteredBlock<TankBlock> register(ContentRegistrar registrar, TankSpec spec) {
        String id = spec.id();
        var material = spec.material();
        float hardness = spec.hardness(), blastResistance = spec.blastResistance();
        RegisteredBlock<TankBlock> block = registrar.block(
                REGISTRY_NAMESPACE,
                id,
                () -> new TankBlock(spec, TankBlock.defaultProperties(spec)),
                bare -> new GTMachineBlockItem(
                        bare,
                        new Item.Properties().stacksTo(16),
                        new MachineSpec(
                                id,
                                material.getLocalName(),
                                material.getColor(),
                                0,
                                0,
                                gregtech.engine.machine.MachineTextures.BURNING_SOLID,
                                hardness,
                                blastResistance)));
        ALL.add(block);
        return block;
    }

    public static RegisteredBlock<TankBlock> registerLogisticsTank(ContentRegistrar registrar) {
        String id = "logistics_tank";
        var material = gregtech.content.material.Materials.Tungsten;
        TankSpec spec = gregtech.content.transport.fluid.LogisticsTankSpec.spec();
        RegisteredBlock<TankBlock> block = registrar.block(
                REGISTRY_NAMESPACE,
                id,
                () -> new gregtech.content.block.machine.LogisticsTankBlock(spec, TankBlock.defaultProperties(spec)),
                bare -> new GTMachineBlockItem(
                        bare,
                        new Item.Properties().stacksTo(16),
                        new MachineSpec(
                                id,
                                "Logistics Tank",
                                material.getColor(),
                                0,
                                0,
                                gregtech.engine.machine.MachineTextures.BURNING_SOLID,
                                1.0F,
                                10.0F)));
        ALL.add(block);
        return block;
    }

    public static List<RegisteredBlock<TankBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }
}
