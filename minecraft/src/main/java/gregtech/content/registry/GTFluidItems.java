package gregtech.content.registry;
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
import java.util.LinkedHashMap;
import java.util.Map;

/** GT6 fluid item holder — lookup maps populated by {@code Loader_Fluids}. */
//?}
public final class GTFluidItems {
//? if >=1.21.1 {
 /*private GTFluidItems(){}
 public static Item get(String field){var entry=RegisteredFluids.get(field);if(entry==null)return null;var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(GTFluids.namespace(entry),"fluid_item_"+RegisteredFluids.sanitizePath(entry.registryName())));return item==Items.AIR?null:item;}
*///?} else {
    private static final Map<String, RegisteredItem<FluidItem>> BY_FIELD = new LinkedHashMap<>();

    private static final Map<ResourceLocation, RegisteredItem<FluidItem>> BY_FLUID = new LinkedHashMap<>();
    public static FluidItem forFluid(net.minecraft.world.level.material.Fluid fluid) {
        var entry=BY_FLUID.get(net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(fluid));
        return entry!=null && entry.isPresent()?entry.get():null;
    }
    private GTFluidItems() {}

    /** Binds a fluid item into the lookup map. Called by Loader_Fluids during registration. */
    /** Every fluid display item, built-in and addon namespace alike. */
    public static java.util.Collection<RegisteredItem<FluidItem>> allEntries() {
        return java.util.List.copyOf(BY_FIELD.values());
    }

    public static void bindFluidItem(String field, RegisteredItem<FluidItem> ro) {
        BY_FIELD.put(field, ro);
        var entry = RegisteredFluids.get(field);
        if (entry != null && entry.usesVanillaFluid()) {
            BY_FLUID.put(ResourceLocation.parse("minecraft:lava"), ro);
            BY_FLUID.put(ResourceLocation.parse("minecraft:flowing_lava"), ro);
        }
        var still=GTFluids.still(field); var flowing=GTFluids.flowing(field);
        if(still!=null) BY_FLUID.put(still.getId(),ro);
        if(flowing!=null) BY_FLUID.put(flowing.getId(),ro);
    }

    @org.jetbrains.annotations.Nullable
    public static FluidItem get(String field) {
        RegisteredItem<FluidItem> ro = BY_FIELD.get(field);
        return ro != null && ro.isPresent() ? ro.get() : null;
    }

    @org.jetbrains.annotations.Nullable
    public static FluidItem getFirst() {
        for (RegisteredItem<FluidItem> ro : BY_FIELD.values()) {
            if (ro.isPresent()) return ro.get();
        }
        return null;
    }

    public static ResourceLocation itemTexture(RegisteredFluids.FluidEntry entry) {
        return gregtech.GregTech.id("item/fluid_item/" + RegisteredFluids.sanitizeTextureId(entry.registryName()));
    }
//?}
}
