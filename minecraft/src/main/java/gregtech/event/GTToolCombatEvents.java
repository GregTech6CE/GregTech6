package gregtech.event;

import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
//?}

/**
 * GT6 wrench combat rules that lived in {@code GT_Tool_Wrench}:
 * doubled damage against golems, doubled hurt cooldown and the higher swing exhaustion.
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class GTToolCombatEvents {
    private GTToolCombatEvents() {}

//? if >=1.21.1 {
/*    @SubscribeEvent
    public static void wrenchHit(LivingIncomingDamageEvent event) {
        GTToolType type = wrenchType(event.getSource());
        if (type == null) return;
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getAmount() <= 0.0F) return;
        if (isGolem(target)) event.setAmount(event.getAmount() * 2.0F);
        event.setInvulnerabilityTicks(20 * type.hurtResistanceMultiplier());
        ((Player) event.getSource().getEntity()).causeFoodExhaustion(type.attackExhaustion());
    }
*///?} else {
    @SubscribeEvent
    public static void wrenchHit(LivingHurtEvent event) {
        GTToolType type = wrenchType(event.getSource());
        if (type == null) return;
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getAmount() <= 0.0F) return;
        if (isGolem(target)) event.setAmount(event.getAmount() * 2.0F);
        int base = Math.max(20, target.invulnerableTime);
        target.invulnerableTime = Math.max(1, base * type.hurtResistanceMultiplier());
        ((Player) event.getSource().getEntity()).causeFoodExhaustion(type.attackExhaustion());
    }
//?}

    private static GTToolType wrenchType(DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player player) || source.getDirectEntity() != attacker) {
            return null;
        }
        ItemStack held = player.getMainHandItem();
        return GTToolHelper.isTool(held) && GTToolHelper.isMachineWrench(held)
                ? GTToolHelper.getType(held) : null;
    }

    /** GT6 {@code GT_Tool_Wrench.getNormalDamageAgainstEntity}. */
    private static boolean isGolem(Entity entity) {
        if (entity instanceof IronGolem) return true;
        String name = entity.getClass().getSimpleName();
        return name.contains("Golem") || name.equals("EntityTowerGuardian") || name.equals("TowerGuardian");
    }
}
