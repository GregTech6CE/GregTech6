package com.gregtech.gregtech.content.bumble;

import com.gregtech.gregtech.damage.GTDamageTypes;
import com.gregtech.gregtech.damage.GTHazmat;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** GT6 MultiItemBumbles:440-456. Species damage is independent of the aggression roll. */
public final class BumbleSting {
    private static final ResourceKey<DamageType> FIRE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("gregtech", "bumble_fire"));

    private BumbleSting() {}

    public static boolean attack(ItemStack bee, LivingEntity target) {
        if (target.level().isClientSide || GTHazmat.isInsectProtected(target)) return false;
        var species = BumbleBeeType.speciesOf(bee);
        if (species == null) return false;
        int family = species.id() / 100;
        int strength = 1 + (species.id() / 10) % 10;
        boolean skeleton = target instanceof AbstractSkeleton || target instanceof SkeletonHorse;
        boolean snow = target instanceof SnowGolem;
        if (family == 8) return false;
        if (family == 105 || family >= 200 && family <= 203) {
            return !(target instanceof Player) && target.hurt(GTDamageTypes.bumble(target.level()), strength * 10);
        }
        if (skeleton || family != 3 && snow) return false;
        if (family == 3) {
            if (!target.hurt(GTDamageTypes.source(target.level(), FIRE), strength * 2)) return false;
            target.igniteForSeconds(strength * 10);
            return true;
        }
        int multiplier = family == 6 ? 4 : family == 9 ? 2 : 1;
        return target.hurt(GTDamageTypes.bumble(target.level()), strength * multiplier);
    }
}
