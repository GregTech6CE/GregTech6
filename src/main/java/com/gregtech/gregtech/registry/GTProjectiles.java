package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.entity.MaterialArrowEntity;
import com.gregtech.gregtech.item.MaterialArrowItem;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GTProjectiles {
    private GTProjectiles() {}
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "gregtech");
    public static final RegistryObject<EntityType<MaterialArrowEntity>> MATERIAL_ARROW = TYPES.register("material_arrow",
            () -> EntityType.Builder.<MaterialArrowEntity>of(MaterialArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).build("gregtech:material_arrow"));

    public static void register(IEventBus bus) { TYPES.register(bus); }

    public static void registerDispensers() {
        for (var holder : GTItems.allEntries()) {
            if (!(holder.get() instanceof MaterialArrowItem)) continue;
            DispenserBlock.registerBehavior(holder.get(), new AbstractProjectileDispenseBehavior() {
                @Override
                protected Projectile getProjectile(Level level, Position pos, ItemStack stack) {
                    var arrow = new MaterialArrowEntity(level, pos.x(), pos.y(), pos.z(), stack);
                    arrow.pickup = AbstractArrow.Pickup.ALLOWED;
                    return arrow;
                }
            });
        }
    }
}
