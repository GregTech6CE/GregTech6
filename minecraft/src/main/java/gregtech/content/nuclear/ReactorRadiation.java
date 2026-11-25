package gregtech.content.nuclear;
import gregtech.engine.effect.MobEffectCompat;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

/** GT6 UT.Entities.applyRadioactivity: player tracker, potion fallback for other living entities. */
public final class ReactorRadiation {
    public static final TagKey<Item> PROTECTION = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("gt6ce",
            "radiation_protection"));

    private ReactorRadiation() {
    }

    public static boolean protectedByArmor(LivingEntity entity) {
        for (var slot : new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) if (entity.getItemBySlot(slot).isEmpty() || !entity.getItemBySlot(slot).is(PROTECTION)) return false;
        return true;
    }

    public static boolean apply(LivingEntity entity, int level, int dose) {
//? if >=1.21.1 {
        /*if (level <= 0 || dose <= 0 || !entity.isAlive() || entity.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)
                || entity.getType().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD) || gregtech.content.damage.GTHazmat.isRadioProtected(entity)) return false;
*///?} else {
        if (level <= 0 || dose <= 0 || !entity.isAlive() || entity.getMobType() == MobType.UNDEAD || entity.getMobType() == MobType.ARTHROPOD
                || gregtech.content.damage.GTHazmat.isRadioProtected(entity)) return false;
//?}
        if (entity.level().isClientSide) return false;
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            PlayerRadiation.change(player, (long) level * dose);
            return true;
        }
        int amplifier = (int) Math.min(5, 5L * level / 7);
        add(entity, MobEffectCompat.vanillaId("slowness"), 140, level, dose, amplifier);
        add(entity, MobEffectCompat.vanillaId("mining_fatigue"), 150, level, dose, amplifier);
        add(entity, MobEffectCompat.vanillaId("nausea"), 130, level, dose, amplifier);
        add(entity, MobEffectCompat.vanillaId("weakness"), 150, level, dose, amplifier);
        add(entity, MobEffectCompat.vanillaId("hunger"), 130, level, dose, amplifier);
        add(entity, MobEffectCompat.vanillaId("wither"), 130, level, dose, amplifier);
        return true;
    }

    private static void add(LivingEntity entity, ResourceLocation effect, int factor, int level, int dose, int amplifier) {
        long duration = (long) Math.min(Integer.MAX_VALUE, (double) factor * level * dose);
        duration = Math.min(Integer.MAX_VALUE, duration + MobEffectCompat.remainingDuration(entity, effect));
        MobEffectCompat.apply(entity, effect, (int) duration, amplifier, false, true);
    }
}
