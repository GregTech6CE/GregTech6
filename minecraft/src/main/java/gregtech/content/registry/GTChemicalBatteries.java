package gregtech.content.registry;

import gregtech.content.block.energy.ChemicalBatteryBlock;
import gregtech.content.blockentity.energy.ChemicalBatteryBlockEntity;
import gregtech.content.energy.ChemicalBatterySpec;
import gregtech.content.item.ChemicalBatteryItem;
import java.util.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class GTChemicalBatteries {
    private static final List<RegisteredBlock<ChemicalBatteryBlock>> ALL = new ArrayList<>();
    public static RegisteredBlockEntity<ChemicalBatteryBlockEntity> CHEMICAL_BATTERY;

    private GTChemicalBatteries() {}

    public static List<RegisteredBlock<ChemicalBatteryBlock>> allRegistered() {
        return Collections.unmodifiableList(ALL);
    }

    public static List<RegisteredBlock<ChemicalBatteryBlock>> all() {
        return ALL.stream()
                .filter(holder -> holder.get().spec() instanceof ChemicalBatterySpec)
                .toList();
    }

    public static ChemicalBatteryItem item(ChemicalBatterySpec.Chemistry chemistry, int tier) {
        var id = gregtech.platform.Mc.gregtechId(new ChemicalBatterySpec(chemistry, tier).id());
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        if (!(item instanceof ChemicalBatteryItem battery))
            throw new IllegalStateException("Missing chemical battery " + id);
        return battery;
    }

    public static void registerAll(ContentRegistrar registrar) {
        var specifications = new ArrayList<gregtech.content.energy.PortableBatterySpec>(ChemicalBatterySpec.all());
        specifications.addAll(gregtech.content.energy.EnergyCrystalSpec.all());
        for (var spec : specifications) {
            // Energium keeps its existing material item ID, created through GTItems.
            if (spec instanceof gregtech.content.energy.EnergyCrystalSpec) {
                var block = registrar.blockWithoutItem(
                        gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                        spec.id(),
                        () -> new ChemicalBatteryBlock(spec));
                ALL.add(block);
                continue;
            }
            var block = registrar.block(
                    gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                    spec.id(),
                    () -> new ChemicalBatteryBlock(spec),
                    ChemicalBatteryItem::new);
            ALL.add(block);
        }
        CHEMICAL_BATTERY = registrar.blockEntity(
                gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                "chemical_battery",
                () -> BlockEntityType.Builder.of(
                                ChemicalBatteryBlockEntity::new,
                                allRegistered().stream()
                                        .map(RegisteredBlock::get)
                                        .toArray(Block[]::new))
                        .build(null));
    }
}
