package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.energy.ReactorRodBlock;
import gregtech.content.item.FuelRodItem;
import gregtech.content.item.FuelRodItem.FuelType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Batch registration of nuclear fuel rod items. */
public final class GTFuelRods {
    private GTFuelRods() {}

    public static final List<RegisteredItem<FuelRodItem>> ALL = new ArrayList<>();
    public static final Map<String, RegisteredBlock<ReactorRodBlock>> BLOCKS = new LinkedHashMap<>();

    // Nuclear fuel rods — single (tier 0-4)
    public static RegisteredItem<FuelRodItem> FUEL_ROD_U_235,
            FUEL_ROD_U_238,
            FUEL_ROD_PU_239,
            FUEL_ROD_TH_232,
            FUEL_ROD_MOX,
            FUEL_ROD_CO_60,
            FUEL_ROD_AM_241,
            FUEL_ROD_AM_243,
            FUEL_ROD_NAQUADAH;

    public static void register(ContentRegistrar registrar) {
        if (!ALL.isEmpty()) throw new IllegalStateException("Fuel rods registered twice");
        FUEL_ROD_U_235 = register(registrar, "fuel_rod_u_235", FuelType.U_235, 0);
        FUEL_ROD_U_238 = register(registrar, "fuel_rod_u_238", FuelType.U_238, 0);
        FUEL_ROD_PU_239 = register(registrar, "fuel_rod_pu_239", FuelType.Pu_239, 0);
        FUEL_ROD_TH_232 = register(registrar, "fuel_rod_th_232", FuelType.Th_232, 0);
        FUEL_ROD_MOX = register(registrar, "fuel_rod_mox", FuelType.MOX, 1);
        FUEL_ROD_CO_60 = register(registrar, "fuel_rod_co_60", FuelType.Co_60, 0);
        FUEL_ROD_AM_241 = register(registrar, "fuel_rod_am_241", FuelType.Am_241, 0);
        FUEL_ROD_AM_243 = register(registrar, "fuel_rod_am_243", FuelType.Am_243, 0);
        var existing = Set.of(
                "fuel_rod_u_235",
                "fuel_rod_u_238",
                "fuel_rod_pu_239",
                "fuel_rod_th_232",
                "fuel_rod_co_60",
                "fuel_rod_am_241");
        for (var definition : gregtech.content.nuclear.ReactorRodCatalog.ALL)
            if (!existing.contains(definition.id())) {
                var block = registerBlock(registrar, definition.id());
                var item = registrar.item(
                        REGISTRY_NAMESPACE,
                        definition.id(),
                        () -> new FuelRodItem(block.get(), definition, new Item.Properties().stacksTo(16)));
                ALL.add(item);
                if (definition.id().equals("reactor_rod_9440")) FUEL_ROD_NAQUADAH = item;
            }
        GTRadiationProtection.register(registrar);
    }

    private static RegisteredItem<FuelRodItem> register(
            ContentRegistrar registrar, String id, FuelType type, int tier) {
        var block = registerBlock(registrar, id);
        Item.Properties props = new Item.Properties()
                .stacksTo(
                        switch (type) {
                            case MOX, Am_243, Naquadah -> 1;
                            default -> 16;
                        });
        var item = registrar.item(REGISTRY_NAMESPACE, id, () -> new FuelRodItem(block.get(), type, tier, props));
        ALL.add(item);
        return item;
    }

    private static RegisteredBlock<ReactorRodBlock> registerBlock(ContentRegistrar registrar, String id) {
        var block = registrar.blockWithoutItem(REGISTRY_NAMESPACE, id, ReactorRodBlock::new);
        BLOCKS.put(id, block);
        return block;
    }

    public static net.minecraft.world.item.ItemStack stack(int originalId) {
        var definition = gregtech.content.nuclear.ReactorRodCatalog.byOriginal(originalId);
        if (definition == null) return net.minecraft.world.item.ItemStack.EMPTY;
        return new net.minecraft.world.item.ItemStack(
                BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, definition.id())));
    }
}
