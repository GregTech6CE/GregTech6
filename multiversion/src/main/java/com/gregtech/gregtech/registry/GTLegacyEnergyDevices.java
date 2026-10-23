package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.energy.MagicFieldAbsorberBlock;
import com.gregtech.gregtech.block.energy.PowerCellBlock;
import com.gregtech.gregtech.content.energy.PowerCellSpec;
import com.gregtech.gregtech.item.PowerCellItem;
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
