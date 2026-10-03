package com.gregtech.gregtech.entity;

import com.gregtech.gregtech.api.material.MaterialFormItem;
import com.gregtech.gregtech.content.recipe.MaterialArrowRules;
import com.gregtech.gregtech.registry.GTProjectiles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Native ballistics with GT6 material quality, plastic speed and original-stack pickup. */
public final class MaterialArrowEntity extends AbstractArrow {
    private ItemStack ammo = new ItemStack(Items.ARROW);
    private int age;

    public MaterialArrowEntity(EntityType<? extends MaterialArrowEntity> type, Level level) { super(type, level); }

    public MaterialArrowEntity(Level level, LivingEntity shooter, ItemStack ammo) {
        super(GTProjectiles.MATERIAL_ARROW.get(), shooter, level);
        setAmmo(ammo);
    }

    public MaterialArrowEntity(Level level, double x, double y, double z, ItemStack ammo) {
        super(GTProjectiles.MATERIAL_ARROW.get(), x, y, z, level);
        setAmmo(ammo);
    }

    private void setAmmo(ItemStack stack) {
        ammo = stack.copy();
        ammo.setCount(1);
        if (ammo.getItem() instanceof MaterialFormItem form)
            setBaseDamage(MaterialArrowRules.baseDamage(form.getMaterial()));
    }

    @Override
    public void shoot(double x, double y, double z, float speed, float spread) {
        float multiplier = ammo.getItem() instanceof MaterialFormItem form
                ? MaterialArrowRules.speedMultiplier(form.getPrefix()) : 1.0F;
        super.shoot(x, y, z, speed * multiplier, spread);
    }

    @Override
    protected ItemStack getPickupItem() { return ammo.copy(); }

    @Override
    public void tick() {
        if (age++ == MaterialArrowRules.LIFETIME_TICKS) { discard(); return; }
        boolean wasInGround = inGround;
        super.tick();
        if (wasInGround && !inGround) age = 0;
    }

    @Override
    protected void tickDespawn() { /* GT6 expires after 3000 total ticks, handled above. */ }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("GTAmmo", ammo.save(new CompoundTag()));
        tag.putInt("GTArrowAge", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("GTAmmo", 10)) {
            ItemStack saved = ItemStack.of(tag.getCompound("GTAmmo"));
            if (!saved.isEmpty()) { ammo = saved.copy(); ammo.setCount(1); }
        }
        age = tag.getInt("GTArrowAge");
    }
}
