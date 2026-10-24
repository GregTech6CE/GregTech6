package gregtech.content.registry;

import gregtech.content.block.energy.MagicFieldAbsorberBlock;
import gregtech.content.block.energy.PowerCellBlock;
import gregtech.content.energy.PowerCellSpec;
import gregtech.content.item.PowerCellItem;
import java.util.List;

/** Shared registrations: no loader-specific duplicate identities. */
public final class GTLegacyEnergyDevices {
    private GTLegacyEnergyDevices() {}
    public static final List<RegisteredBlock<PowerCellBlock>> POWER_CELLS = PowerCellSpec.ALL.stream()
            .map(spec -> RegisteredBlock.register(spec.id(), () -> new PowerCellBlock(spec), PowerCellItem::new)).toList();
    public static final RegisteredBlock<MagicFieldAbsorberBlock> MAGIC_FIELD_ABSORBER =
            RegisteredBlock.register("magic_field_absorber", MagicFieldAbsorberBlock::new,
                    block -> new net.minecraft.world.item.BlockItem(block, new net.minecraft.world.item.Item.Properties().stacksTo(16)));
    public static void registerAll() { /* Triggers deferred registration during block bootstrap. */ }
}
