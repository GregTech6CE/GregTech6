package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.*;
import net.minecraft.world.item.Item;

/** Registers the logistics barrel using the shared specification and entity implementation. */
public final class GTLogisticsTank {
    private GTLogisticsTank() {}

    public static RegisteredBlock<LogisticsTankBlock> BLOCK;

    public static void initialize(ContentRegistrar registrar) {
        var spec = gregtech.content.transport.fluid.LogisticsTankSpec.spec();
        BLOCK = registrar.block(
                REGISTRY_NAMESPACE,
                spec.id(),
                () -> new LogisticsTankBlock(spec, TankBlock.defaultProperties(spec)),
                bare -> new gregtech.content.transport.fluid.FluidTransportBlockItem(
                        bare, new Item.Properties().stacksTo(16)));
    }
}
