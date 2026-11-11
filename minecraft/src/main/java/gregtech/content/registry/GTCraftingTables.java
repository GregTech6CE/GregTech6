package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.misc.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTCraftingTables {
    private GTCraftingTables() {}

    public static final java.util.List<RegisteredBlock<AdvancedCraftingTableBlock>> ADVANCED =
            new java.util.ArrayList<>();
    public static final java.util.List<RegisteredBlock<ChargingCraftingTableBlock>> CHARGING =
            new java.util.ArrayList<>();

    public static AdvancedCraftingTableBlock advanced(gregtech.api.material.GTMaterial material) {
        for (var table : ADVANCED) if (table.get().material() == material) return table.get();
        throw new IllegalArgumentException("No crafting table for " + material.getName());
    }

    public static void initialize(ContentRegistrar registrar) {
        for (var spec : GTStorageMetals.ALL) {
            String suffix = spec.suffix().equals("steel") ? "" : "_" + spec.suffix();
            var a = registrar.block(
                    REGISTRY_NAMESPACE,
                    "advanced_crafting_table" + suffix,
                    () -> new AdvancedCraftingTableBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()),
                    bare -> new BlockItem(bare, new Item.Properties().stacksTo(16)));
            ADVANCED.add(a);
            var c = registrar.block(
                    REGISTRY_NAMESPACE,
                    "charging_crafting_table" + suffix,
                    () -> new ChargingCraftingTableBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()),
                    bare -> new BlockItem(bare, new Item.Properties()));
            CHARGING.add(c);
        }
    }
}
