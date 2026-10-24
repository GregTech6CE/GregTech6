package gregtech.content.registry;
//? if >=1.21.1 {
//?} else {

import gregtech.GregTech;
//?}
import gregtech.content.data.RegisteredFluids;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import gregtech.content.item.FluidItem;
//?}
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.minecraft.world.item.*;
*///?} else {
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** GT6 fluid item holder — DeferredRegister and lookup map populated by {@code Loader_Fluids}. */
//?}
public final class GTFluidItems {
//? if >=1.21.1 {
 /*private GTFluidItems(){}
 public static Item get(String field){var entry=RegisteredFluids.get(field);if(entry==null)return null;var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(GTFluids.namespace(entry),"fluid_item_"+RegisteredFluids.sanitizePath(entry.registryName())));return item==Items.AIR?null:item;}
*///?} else {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GregTech.NAMESPACE);
    private static final Map<String, DeferredRegister<Item>> ADDON_ITEMS = new LinkedHashMap<>();

    /** Built-in display items stay on {@link #ITEMS}; an addon fluid uses that addon's namespace. */
    public static DeferredRegister<Item> items(String namespace) {
        if (GregTech.NAMESPACE.equals(namespace)) return ITEMS;
        return ADDON_ITEMS.computeIfAbsent(namespace, id -> DeferredRegister.create(ForgeRegistries.ITEMS, id));
    }

    public static void registerAddonItems(net.minecraftforge.eventbus.api.IEventBus bus) {
        for (DeferredRegister<Item> register : ADDON_ITEMS.values()) register.register(bus);
    }

    private static final Map<String, RegistryObject<FluidItem>> BY_FIELD = new LinkedHashMap<>();

    private static final Map<ResourceLocation,RegistryObject<FluidItem>> BY_FLUID = new LinkedHashMap<>();
    public static FluidItem forFluid(net.minecraft.world.level.material.Fluid fluid) {
        var entry=BY_FLUID.get(ForgeRegistries.FLUIDS.getKey(fluid));
        return entry!=null && entry.isPresent()?entry.get():null;
    }
    private GTFluidItems() {}

    /** Binds a fluid item into the lookup map. Called by Loader_Fluids during registration. */
    /** Every fluid display item, built-in and addon namespace alike. */
    public static java.util.Collection<RegistryObject<FluidItem>> allEntries() {
        return java.util.List.copyOf(BY_FIELD.values());
    }

    public static void bindFluidItem(String field, RegistryObject<FluidItem> ro) {
        BY_FIELD.put(field, ro);
        var still=GTFluids.still(field); var flowing=GTFluids.flowing(field);
        if(still!=null) BY_FLUID.put(still.getId(),ro);
        if(flowing!=null) BY_FLUID.put(flowing.getId(),ro);
    }

    @org.jetbrains.annotations.Nullable
    public static FluidItem get(String field) {
        RegistryObject<FluidItem> ro = BY_FIELD.get(field);
        return ro != null && ro.isPresent() ? ro.get() : null;
    }

    @org.jetbrains.annotations.Nullable
    public static FluidItem getFirst() {
        for (RegistryObject<FluidItem> ro : BY_FIELD.values()) {
            if (ro.isPresent()) return ro.get();
        }
        return null;
    }

    public static ResourceLocation itemTexture(RegisteredFluids.FluidEntry entry) {
        return GregTech.id("item/fluid_item/" + RegisteredFluids.sanitizeTextureId(entry.registryName()));
    }
//?}
}
