package gregtech.content.tool;

import gregtech.content.nuclear.ReactorRadiation;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Slime;

/** GT6's target predicates and debuffs; entity tags also allow modern addons to opt in. */
public final class GTEnchantmentEffects {
    private static final TagKey<EntityType<?>> ENDER = tag("ender_creatures");
    private static final TagKey<EntityType<?>> SLIME = tag("slime_creatures");
    private static final TagKey<EntityType<?>> WERE = tag("were_creatures");
    private static final String[] WERE_NAMES = {
        "wwolf", "yeti", "villagerwere", "wolfman", "werewolf", "alphawolf", "tamewere", "minotaur", "minoshroom"
    };

    private GTEnchantmentEffects() {}

    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("gt6ce", name));
    }

    public static boolean matches(GTEnchantmentKind kind, Entity target) {
        if (target instanceof EnderDragonPart part) target = part.parentMob;
        if (!(target instanceof LivingEntity)) return false;
        String name = target.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        return switch (kind) {
            case DISJUNCTION -> target instanceof EnderMan || target.getType().is(ENDER) || name.contains("ender");
            case DISSOLVING -> target instanceof Slime || target.getType().is(SLIME) || name.contains("slime");
            case WEREBANE ->
                target.getType().is(WERE) || java.util.Arrays.stream(WERE_NAMES).anyMatch(name::contains);
            case RADIOACTIVITY -> true;
        };
    }

    public static void apply(GTEnchantmentKind kind, int level, Entity target) {
        if (target instanceof EnderDragonPart part) target = part.parentMob;
        if (level <= 0 || target.level().isClientSide || !matches(kind, target)) return;
        var living = (LivingEntity) target;
        if (kind == GTEnchantmentKind.RADIOACTIVITY) {
            ReactorRadiation.apply(living, level, 1);
        } else {
            int duration = kind.duration(level), amplifier = kind.amplifier(level);
            living.addEffect(new MobEffectInstance(
                    kind == GTEnchantmentKind.WEREBANE ? MobEffects.WITHER : MobEffects.WEAKNESS, duration, amplifier));
            living.addEffect(new MobEffectInstance(MobEffects.POISON, duration, amplifier));
        }
    }
}
