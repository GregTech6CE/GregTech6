package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.entity.MaterialArrowEntity;
import com.gregtech.gregtech.item.MaterialArrowItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GTProjectiles {
    private GTProjectiles() {}
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "gregtech");
    public static final DeferredHolder<EntityType<?>, EntityType<MaterialArrowEntity>> MATERIAL_ARROW = TYPES.register("material_arrow",
            () -> EntityType.Builder.<MaterialArrowEntity>of(MaterialArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).build("gregtech:material_arrow"));

    public static void register(IEventBus bus) { TYPES.register(bus); }
    public static void registerDispensers() {
        for (var holder : GTItems.allEntries())
            if (holder.get() instanceof MaterialArrowItem) DispenserBlock.registerProjectileBehavior(holder.get());
    }
}
