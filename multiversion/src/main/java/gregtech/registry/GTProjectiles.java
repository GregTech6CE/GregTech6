package gregtech.registry;

import gregtech.entity.MaterialArrowEntity;
import gregtech.item.MaterialArrowItem;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.Registries;
*///?} else {
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
//?}
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
//? if >=1.21.1 {
//?} else {
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
//?}
import net.minecraft.world.level.block.DispenserBlock;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?} else {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
//?}

public final class GTProjectiles {
    private GTProjectiles() {}
//? if >=1.21.1 {
    /*private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "gt6ce");
    public static final DeferredHolder<EntityType<?>, EntityType<MaterialArrowEntity>> MATERIAL_ARROW = TYPES.register("material_arrow",
*///?} else {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "gt6ce");
    public static final RegistryObject<EntityType<MaterialArrowEntity>> MATERIAL_ARROW = TYPES.register("material_arrow",
//?}
            () -> EntityType.Builder.<MaterialArrowEntity>of(MaterialArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).build("gt6ce:material_arrow"));

    public static void register(IEventBus bus) { TYPES.register(bus); }

    public static void registerDispensers() {
//? if >=1.21.1 {
        /*for (var holder : GTItems.allEntries())
            if (holder.get() instanceof MaterialArrowItem) DispenserBlock.registerProjectileBehavior(holder.get());
*///?} else {
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
//?}
    }
}
