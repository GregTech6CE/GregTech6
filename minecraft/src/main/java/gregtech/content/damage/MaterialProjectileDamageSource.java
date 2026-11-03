package gregtech.content.damage;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Carries the impacting ammunition, including hitscan bullets which have no projectile entity. */
public final class MaterialProjectileDamageSource extends DamageSource {
    private final ItemStack ammunition;

    public MaterialProjectileDamageSource(Holder<DamageType> type, Entity direct, Entity owner, ItemStack ammunition) {
        super(type, direct, owner);
        this.ammunition = ammunition;
    }

    public ItemStack ammunition() {
        return ammunition;
    }
}
