package gregtech.content.entity;

import gregtech.engine.material.MaterialFormItem;
import gregtech.content.recipe.MaterialArrowRules;
import gregtech.content.registry.GTProjectiles;
import gregtech.engine.tool.MaterialEnchantmentStacks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Native ballistics with GT6 material quality, plastic speed and original-stack pickup. */
public final class MaterialArrowEntity extends AbstractArrow {
//? if >=1.21.1 {
//?} else {
    private ItemStack ammo = new ItemStack(Items.ARROW);
//?}
    private int age;

    public MaterialArrowEntity(EntityType<? extends MaterialArrowEntity> type, Level level) { super(type, level); }

//? if >=1.21.1 {
    /*public MaterialArrowEntity(Level level, LivingEntity shooter, ItemStack ammo, @org.jetbrains.annotations.Nullable ItemStack weapon) {
        super(GTProjectiles.MATERIAL_ARROW.get(), shooter, level, ammo.copyWithCount(1), weapon);
        applyMaterialDamage(ammo);
*///?} else {
    public MaterialArrowEntity(Level level, LivingEntity shooter, ItemStack ammo) {
        super(GTProjectiles.MATERIAL_ARROW.get(), shooter, level);
        setAmmo(ammo);
//?}
    }

    public MaterialArrowEntity(Level level, double x, double y, double z, ItemStack ammo) {
//? if >=1.21.1 {
        /*super(GTProjectiles.MATERIAL_ARROW.get(), x, y, z, level, ammo.copyWithCount(1), null);
        applyMaterialDamage(ammo);
*///?} else {
        super(GTProjectiles.MATERIAL_ARROW.get(), x, y, z, level);
        setAmmo(ammo);
//?}
    }

//? if >=1.21.1 {
    /*private void applyMaterialDamage(ItemStack ammo) {
        ammo = ammo.copyWithCount(1);
        MaterialEnchantmentStacks.applyAmmo(ammo, level());
        setPickupItemStack(ammo);
*///?} else {
    private void setAmmo(ItemStack stack) {
        ammo = stack.copy();
        ammo.setCount(1);
        MaterialEnchantmentStacks.applyAmmo(ammo, level());
//?}
        if (ammo.getItem() instanceof MaterialFormItem form)
            setBaseDamage(MaterialArrowRules.baseDamage(form.getMaterial()));
    }

    public ItemStack ammunition() {
//? if >=1.21.1 {
        /*return getPickupItemStackOrigin();
*///?} else {
        return ammo;
//?}
    }

    /** Called at the native damage boundary so vanilla piercing, criticals and pickup stay intact. */
    public boolean hurtTarget(net.minecraft.world.entity.Entity target, net.minecraft.world.damagesource.DamageSource source, float damage) {
        var ammunition = ammunition();
        MaterialEnchantmentStacks.applyAmmo(ammunition, level());
        int fire = MaterialEnchantmentStacks.level(ammunition, "fire_aspect", level());
        int previousFire = target.getRemainingFireTicks();
        if (fire > 0 && !(target instanceof net.minecraft.world.entity.monster.EnderMan)) ignite(target, fire * 4);
        var looting = MaterialEnchantmentStacks.level(ammunition, "looting", level());
        net.minecraft.world.entity.player.Player lootOwner = null;
        ItemStack saved = ItemStack.EMPTY;
        if (looting > 0 && level() instanceof net.minecraft.server.level.ServerLevel server
                && !(target instanceof net.minecraft.world.entity.player.Player)) {
//? if >=1.21.1 {
            /*lootOwner = net.neoforged.neoforge.common.util.FakePlayerFactory.get(server, LOOT_OWNER);
*///?} else {
            lootOwner = net.minecraftforge.common.util.FakePlayerFactory.get(server, LOOT_OWNER);
//?}
            saved = lootOwner.getMainHandItem();
            lootOwner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ammunition);
        }
        source = new gregtech.content.damage.MaterialProjectileDamageSource(
                source.typeHolder(), this, lootOwner == null ? source.getEntity() : lootOwner, ammunition);
        try {
            boolean hit = target.hurt(source, damage + MaterialEnchantmentStacks.damageBonus(ammunition, target, source));
            if (!hit) { target.setRemainingFireTicks(previousFire); return false; }
            MaterialEnchantmentStacks.postAttack(ammunition, target, source);
            int knockback = MaterialEnchantmentStacks.level(ammunition, "knockback", level());
            if (knockback > 0) {
                var velocity = getDeltaMovement().multiply(1, 0, 1).normalize().scale(knockback * .6);
                target.push(velocity.x, .1, velocity.z);
            }
            if (fire > 0) {
                ignite(target, fire * 4);
                if (target instanceof net.minecraft.world.entity.monster.Creeper creeper) creeper.ignite();
            }
            // AbstractArrow leaves an Enderman-hit projectile alive even when GT's weakness lets it hit.
            if (target instanceof net.minecraft.world.entity.monster.EnderMan enderman && enderman.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)) discard();
            return true;
        } finally {
            if (lootOwner != null) lootOwner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, saved);
        }
    }

    private static final com.mojang.authlib.GameProfile LOOT_OWNER = new com.mojang.authlib.GameProfile(
            java.util.UUID.fromString("f838047e-69fa-4c9a-b365-895a0ce05d48"), "[GT6 Arrow]");

    private static void ignite(net.minecraft.world.entity.Entity target, int seconds) {
//? if >=1.21.1 {
        /*target.igniteForSeconds(seconds);
*///?} else {
        target.setSecondsOnFire(seconds);
//?}
    }

    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide || MaterialEnchantmentStacks.level(ammunition(), "fire_aspect", level()) < 3) return;
        // GT6's WD.burn(..., true, false) includes the impacted block and all six neighbors.
        var pos = hit.getBlockPos();
        gregtech.content.blockentity.machine.SmelteryFireHelper.igniteAt(level(), pos, false);
        for (var side : net.minecraft.core.Direction.values())
            gregtech.content.blockentity.machine.SmelteryFireHelper.igniteAt(level(), pos.relative(side), false);
    }

    @Override
    public void shoot(double x, double y, double z, float speed, float spread) {
//? if >=1.21.1 {
        /*float multiplier = getPickupItem().getItem() instanceof MaterialFormItem form
*///?} else {
        float multiplier = ammo.getItem() instanceof MaterialFormItem form
//?}
                ? MaterialArrowRules.speedMultiplier(form.getPrefix()) : 1.0F;
        super.shoot(x, y, z, speed * multiplier, spread);
    }

    @Override
//? if >=1.21.1 {
    /*protected ItemStack getDefaultPickupItem() { return new ItemStack(Items.ARROW); }
*///?} else {
    protected ItemStack getPickupItem() { return ammo.copy(); }
//?}

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
//? if >=1.21.1 {
//?} else {
        tag.put("GTAmmo", ammo.save(new CompoundTag()));
//?}
        tag.putInt("GTArrowAge", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
//? if >=1.21.1 {
//?} else {
        if (tag.contains("GTAmmo", 10)) {
            ItemStack saved = ItemStack.of(tag.getCompound("GTAmmo"));
            if (!saved.isEmpty()) { ammo = saved.copy(); ammo.setCount(1); }
        }
//?}
        age = tag.getInt("GTArrowAge");
    }
}
