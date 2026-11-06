package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.energy.MagicFieldAbsorberBlock;
import gregtech.content.block.energy.PowerCellBlock;
import gregtech.content.energy.PowerCellSpec;
import gregtech.content.item.PowerCellItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/** Shared registrations: no loader-specific duplicate identities. */
public final class GTLegacyEnergyDevices {
    private GTLegacyEnergyDevices() {}

    public static final List<RegisteredBlock<PowerCellBlock>> POWER_CELLS = new ArrayList<>();
    public static RegisteredBlock<MagicFieldAbsorberBlock> MAGIC_FIELD_ABSORBER;

    /** Queues the blocks at the block phase; the platform entry submits the registers later. */
    public static void registerAll(ContentRegistrar registrar) {
        if (!POWER_CELLS.isEmpty()) throw new IllegalStateException("Legacy energy devices registered twice");
        for (var spec : PowerCellSpec.ALL)
            POWER_CELLS.add(
                    registrar.block(REGISTRY_NAMESPACE, spec.id(), () -> new PowerCellBlock(spec), PowerCellItem::new));
        MAGIC_FIELD_ABSORBER = registrar.block(
                REGISTRY_NAMESPACE,
                "magic_field_absorber",
                MagicFieldAbsorberBlock::new,
                block -> new BlockItem(block, new Item.Properties().stacksTo(16)));
    }
}
