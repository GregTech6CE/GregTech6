package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.item.FuelRodItem;
import com.gregtech.gregtech.item.FuelRodItem.FuelType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Batch registration of nuclear fuel rod items. */
public final class GTFuelRods {
    private GTFuelRods() {}

    public static final List<RegistryObject<FuelRodItem>> ALL = new ArrayList<>();
    public static final java.util.Map<String, RegistryObject<com.gregtech.gregtech.block.energy.ReactorRodBlock>> BLOCKS = new java.util.LinkedHashMap<>();

    // Nuclear fuel rods — single (tier 0-4)
    public static final RegistryObject<FuelRodItem> FUEL_ROD_U_235 = register("fuel_rod_u_235", FuelType.U_235, 0);
    public static final RegistryObject<FuelRodItem> FUEL_ROD_U_238 = register("fuel_rod_u_238", FuelType.U_238, 0);
    public static final RegistryObject<FuelRodItem> FUEL_ROD_PU_239 = register("fuel_rod_pu_239", FuelType.Pu_239, 0);
    public static final RegistryObject<FuelRodItem> FUEL_ROD_TH_232 = register("fuel_rod_th_232", FuelType.Th_232, 0);
    public static final RegistryObject<FuelRodItem> FUEL_ROD_MOX = register("fuel_rod_mox", FuelType.MOX, 1);
    public static final RegistryObject<FuelRodItem> FUEL_ROD_CO_60 = register("fuel_rod_co_60", FuelType.Co_60, 0);
    public static final RegistryObject<FuelRodItem> FUEL_ROD_AM_241 = register("fuel_rod_am_241", FuelType.Am_241, 0);
    public static final RegistryObject<FuelRodItem> FUEL_ROD_AM_243 = register("fuel_rod_am_243", FuelType.Am_243, 0);
    public static final RegistryObject<FuelRodItem> FUEL_ROD_NAQUADAH = register("fuel_rod_naquadah", FuelType.Naquadah, 2);

    private static RegistryObject<FuelRodItem> register(String id, FuelType type, int tier) {
        var block = GTBlocks.BLOCKS.register(id, com.gregtech.gregtech.block.energy.ReactorRodBlock::new);
        BLOCKS.put(id, block);
        Item.Properties props = new Item.Properties().stacksTo(switch(type){case MOX,Am_243,Naquadah->1;default->16;});
        RegistryObject<FuelRodItem> obj = GTItems.ITEMS.register(id,
                () -> new FuelRodItem(block.get(), type, tier, props));
        ALL.add(obj);
        return obj;
    }

    static {
        var existing=java.util.Set.of("fuel_rod_u_235","fuel_rod_u_238","fuel_rod_pu_239","fuel_rod_th_232","fuel_rod_co_60","fuel_rod_am_241");
        for(var definition:com.gregtech.gregtech.content.nuclear.ReactorRodCatalog.ALL) if(!existing.contains(definition.id())) {
            var block = GTBlocks.BLOCKS.register(definition.id(), com.gregtech.gregtech.block.energy.ReactorRodBlock::new);
            BLOCKS.put(definition.id(), block);
            ALL.add(GTItems.ITEMS.register(definition.id(),()->new FuelRodItem(block.get(),definition,new Item.Properties().stacksTo(16))));
        }
    }
    public static net.minecraft.world.item.ItemStack stack(int originalId) {
        var definition=com.gregtech.gregtech.content.nuclear.ReactorRodCatalog.byOriginal(originalId);
        if(definition==null)return net.minecraft.world.item.ItemStack.EMPTY;
        return new net.minecraft.world.item.ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",definition.id())));
    }
    /** Call during mod construction to trigger static init. */
    public static void registerAll() {
        GTRadiationProtection.registerAll();
        // static init triggers the field initializers above
    }
}
