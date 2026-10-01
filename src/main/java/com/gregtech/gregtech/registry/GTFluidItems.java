package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.item.FluidItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** GT6 fluid item holder — DeferredRegister and lookup map populated by {@code Loader_Fluids}. */
public final class GTFluidItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GregTech.MODID);

    private static final Map<String, RegistryObject<FluidItem>> BY_FIELD = new LinkedHashMap<>();

    private static final Map<ResourceLocation,RegistryObject<FluidItem>> BY_FLUID = new LinkedHashMap<>();
    public static FluidItem forFluid(net.minecraft.world.level.material.Fluid fluid) {
        var entry=BY_FLUID.get(ForgeRegistries.FLUIDS.getKey(fluid));
        return entry!=null && entry.isPresent()?entry.get():null;
    }
    private GTFluidItems() {}

    /** Binds a fluid item into the lookup map. Called by Loader_Fluids during registration. */
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
}
