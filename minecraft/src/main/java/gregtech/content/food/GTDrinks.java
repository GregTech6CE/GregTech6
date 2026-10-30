package gregtech.content.food;

import gregtech.content.nuclear.PlayerRadiation;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.data.generated.GTDrinksGen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;

*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
//?}

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * GT6 {@code CS.DrinksGT}: what a drink does to you.
 *
 * <p>GT6 registers every drinkable fluid in {@code Loader_Fluids} as a {@code FoodStatDrink}, which puts the
 * entry into {@code DrinksGT.REGISTER} keyed by the fluid's name ({@code FoodStatDrink.java:46-52}); a
 * GT6 bottle (250&nbsp;mB of that fluid, {@code MultiItemBottles}) reads it through {@code FoodStatFluid}.
 * The port keys the same table by {@link Fluid}, generated from {@code Loader_Fluids.java} into
 * {@link GTDrinksGen} (345 rows; the fluid is resolved through {@link RegisteredFluids#get(String)}, which
 * carries GT6's own {@code FL} field names).
 *
 * <p>Drinking applies GT6's {@code FoodStat.onEaten}: the food level and saturation go through vanilla's food
 * data, the six statistics into {@link PlayerFoodStats} (and {@link PlayerRadiation} for the radiation one).
 */
public final class GTDrinks {
    /** One {@code FoodStatDrink} row: the food values and the six statistics GT6 tracks. */
    public record Row(String field, int foodLevel, float saturation, int alcohol, int caffeine,
                      int dehydration, int sugar, int fat, int radiation, boolean alwaysEdible, boolean invisibleParticles, String useAction, boolean milk, boolean extinguish, boolean explosive, List<DrinkNutritionRows.Effect> effects) {}

    private static final Map<Fluid, Row> BY_FLUID = new IdentityHashMap<>();
    private static final List<String> UNRESOLVED = new java.util.ArrayList<>();
    private static boolean built;

    private GTDrinks() {}

    private static void ensureBuilt() {
        if (built) {
            return;
        }
        built = true;
        for (DrinkNutritionRows.Row source : GTDrinksGen.ROWS) {
            Row row=new Row(source.field(),source.foodLevel(),source.saturation(),source.alcohol(),source.caffeine(),source.dehydration(),source.sugar(),source.fat(),source.radiation(),source.alwaysEdible(),source.invisibleParticles(),source.useAction(),source.milk(),source.extinguish(),source.explosive(),source.effects());
            // GT6 keys drinks by FL field name for most fluids and by the FL.create("...") name for the
            // brews; the port keys its own table by field, so try both and finally match on the registry
            // path with sanitizePath applied to both sides (which is what the port itself registers).
            Fluid fluid = stillFluid(row.field());
            if (fluid == null) {
                fluid = fluidOf(RegisteredFluids.get(row.field()));
            }
            if (fluid == null) {
                fluid = byRegistryPath(row.field());
            }
            if (fluid != null) {
                BY_FLUID.put(fluid, row);
            } else {
                // Never dropped silently: the port has no fluid for this GT6 drink (another mod's fluid or
                // a row the extractor could not key); it is reported through unresolved() instead.
                UNRESOLVED.add(row.field());
            }
        }
    }

    /** GT6 drink rows whose fluid is not in the port (never silently dropped). */
    public static List<String> unresolved() {
        ensureBuilt();
        return List.copyOf(UNRESOLVED);
    }

    private static Fluid byRegistryPath(String key) {
        String wanted = RegisteredFluids.sanitizePath(key);
        for (RegisteredFluids.FluidEntry entry : RegisteredFluids.all().values()) {
            if (RegisteredFluids.sanitizePath(entry.registryName()).equals(wanted)) {
                Fluid fluid = fluidOf(entry);
                if (fluid != null) {
                    return fluid;
                }
            }
        }
        return null;
    }

    private static Fluid stillFluid(String field) {
//? if >=1.21.1 {
        /*var ro = gregtech.content.registry.GTFluids.still(field);
        return ro != null && ro.isBound() ? ro.get() : null;
*///?} else {
        net.minecraftforge.registries.RegistryObject<Fluid> ro = gregtech.content.registry.GTFluids.still(field);
        return ro != null && ro.isPresent() ? ro.get() : null;
//?}
    }

    /**
     * The registered still fluid of a GT6 fluid entry. Resolved through {@code GTFluids.entryForFluid} rather
     * than by name, so the three world waters and every other renamed fluid resolve correctly.
     */
    public static Fluid fluidOf(RegisteredFluids.FluidEntry entry) {
        if (entry == null) {
            return null;
        }
        // The catalog binds these entries to vanilla fluids rather than GT deferred holders.
        if (entry.isVanillaWater())
            return net.minecraft.world.level.material.Fluids.WATER;
        if (entry.isVanillaLava())
            return net.minecraft.world.level.material.Fluids.LAVA;
//? if >=1.21.1 {
        /*for (var ro
*///?} else {
        for (net.minecraftforge.registries.RegistryObject<Fluid> ro
//?}
                : gregtech.content.registry.GTFluids.FLUIDS.getEntries()) {
//? if >=1.21.1 {
            /*if (ro.isBound() && gregtech.content.registry.GTFluids.entryForFluid(ro.get()) == entry) {
*///?} else {
            if (ro.isPresent() && gregtech.content.registry.GTFluids.entryForFluid(ro.get()) == entry) {
//?}
                return ro.get();
            }
        }
        return null;
    }

    /** GT6 {@code DrinksGT.REGISTER.get(fluidName)}, keyed by the fluid instead of its name. */
    public static Row rowFor(Fluid fluid) {
        ensureBuilt();
        return fluid == null ? null : BY_FLUID.get(fluid);
    }

    /** Convenience for a stack. */
    public static Row rowFor(FluidStack stack) {
        return stack == null || stack.isEmpty() ? null : rowFor(stack.getFluid());
    }

    /** How many of GT6's drink fluids exist in the port (the rest are other mods' / potion brews). */
    public static int registered() {
        ensureBuilt();
        return BY_FLUID.size();
    }

    /** The port fluid a GT6 drink row resolved to, by GT6 field name (case-insensitive); for tests/tooling. */
    public static Fluid fluidForField(String field) {
        ensureBuilt();
        for (Map.Entry<Fluid, Row> entry : BY_FLUID.entrySet()) {
            if (entry.getValue().field().equalsIgnoreCase(field)) {
                return entry.getKey();
            }
        }
        return fluidOf(BottleFluidIdentity.resolve(field));
    }

    /** GT6 drink rows whose fluid is not in the port (other mods' fluids, vanilla potion brews). */
    public static List<String> skipped() {
        return GTDrinksGen.SKIPPED;
    }

    /** Canonical potions and the original drink registry only; never arbitrary tank fluids. */
    public static boolean canDrink(Player player, FluidStack stack) {
        if (stack == null || stack.getAmount() < 250) return false;
        Row row = rowFor(stack);
        boolean potion = !gregtech.engine.fluid.PotionBottleHandler.filled(stack).isEmpty();
        return (row != null || potion) && player.canEat(potion || gregtech.GregTechConfig.Rule.ALWAYS_DRINK.enabled()
                || row != null && row.alwaysEdible());
    }

    /** External-mod effects remain in the source table but are unavailable without their owning mod. */
//? if >=1.21.1 {
    /*private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectType(String source) {
*///?} else {
    private static net.minecraft.world.effect.MobEffect effectType(String source) {
//?}
        return switch (source) {
            case "Potion.blindness.id" -> net.minecraft.world.effect.MobEffects.BLINDNESS;
            case "Potion.confusion.id" -> net.minecraft.world.effect.MobEffects.CONFUSION;
            case "Potion.damageBoost.id" -> net.minecraft.world.effect.MobEffects.DAMAGE_BOOST;
            case "Potion.digSlowdown.id" -> net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN;
            case "Potion.digSpeed.id" -> net.minecraft.world.effect.MobEffects.DIG_SPEED;
            case "Potion.field_76444_x.id" -> net.minecraft.world.effect.MobEffects.ABSORPTION;
            case "Potion.fireResistance.id" -> net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE;
            case "Potion.harm.id" -> net.minecraft.world.effect.MobEffects.HARM;
            case "Potion.heal.id" -> net.minecraft.world.effect.MobEffects.HEAL;
            case "Potion.hunger.id" -> net.minecraft.world.effect.MobEffects.HUNGER;
            case "Potion.invisibility.id" -> net.minecraft.world.effect.MobEffects.INVISIBILITY;
            case "Potion.jump.id" -> net.minecraft.world.effect.MobEffects.JUMP;
            case "Potion.moveSlowdown.id" -> net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN;
            case "Potion.moveSpeed.id" -> net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED;
            case "Potion.nightVision.id" -> net.minecraft.world.effect.MobEffects.NIGHT_VISION;
            case "Potion.poison.id" -> net.minecraft.world.effect.MobEffects.POISON;
            case "Potion.regeneration.id" -> net.minecraft.world.effect.MobEffects.REGENERATION;
            case "Potion.resistance.id" -> net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE;
            case "Potion.waterBreathing.id" -> net.minecraft.world.effect.MobEffects.WATER_BREATHING;
            case "Potion.weakness.id" -> net.minecraft.world.effect.MobEffects.WEAKNESS;
            default -> null;
        };
    }

    private static void potionEffects(Player player, net.minecraft.world.item.ItemStack potion) {
//? if >=1.21.1 {
        /*var effects = potion.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                net.minecraft.world.item.alchemy.PotionContents.EMPTY).getAllEffects();
*///?} else {
        var effects = net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(potion);
//?}
        for (var effect : effects) {
//? if >=1.21.1 {
            /*var type = effect.getEffect().value();
*///?} else {
            var type = effect.getEffect();
//?}
            if (type.isInstantenous()) type.applyInstantenousEffect(player, player, player, effect.getAmplifier(), 1.0);
            else player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect));
        }
    }

    /**
     * GT6 {@code FoodStat.onEaten} for a drink: food level, saturation, the six statistics and the drink
     * sound. Returns false when the fluid is not a drink, so callers can pass.
     */
    public static boolean drink(Player player, FluidStack stack) {
        Row row = rowFor(stack);
        if (player.level().isClientSide || !canDrink(player, stack)) {
            return false;
        }
        // Source effects have GT6 durations (including splash/lingering), so never apply both tables.
        if (row == null) potionEffects(player, gregtech.engine.fluid.PotionBottleHandler.filled(stack));
        if (row != null) {
        if (row.extinguish()) player.clearFire();
        if (row.milk()) {
//? if >=1.21.1 {
            /*player.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
*///?} else {
            player.curePotionEffects(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MILK_BUCKET));
//?}
        }
        for (var effect : row.effects()) {
            var type = effectType(effect.sourceId());
            if (type != null && player.getRandom().nextInt(100) < effect.chance()) {
//? if >=1.21.1 {
                /*if (type.value().isInstantenous()) type.value().applyInstantenousEffect(player, player, player, effect.amplifier(), 1.0);
*///?} else {
                if (type.isInstantenous()) type.applyInstantenousEffect(player, player, player, effect.amplifier(), 1.0);
//?}
                else player.addEffect(new net.minecraft.world.effect.MobEffectInstance(type, effect.duration(), effect.amplifier(), false, !row.invisibleParticles()));
            }
        }
        if (row.explosive()) {
            player.level().explode(player, player.getX(), player.getY(), player.getZ(), 4, true,
                    net.minecraft.world.level.Level.ExplosionInteraction.TNT);
            player.hurt(player.damageSources().explosion(player, player), Float.MAX_VALUE);
        }
        player.getFoodData().eat(row.foodLevel(), row.saturation());
        if (row.alcohol() != 0) {
            PlayerFoodStats.change(player, row.alcohol(), GTFoodStats.ALCOHOL);
        }
        if (row.caffeine() != 0) {
            PlayerFoodStats.change(player, row.caffeine(), GTFoodStats.CAFFEINE);
        }
        if (row.dehydration() != 0) {
            PlayerFoodStats.change(player, row.dehydration(), GTFoodStats.DEHYDRATION);
        }
        if (row.sugar() != 0) {
            PlayerFoodStats.change(player, row.sugar(), GTFoodStats.SUGAR);
        }
        if (row.fat() != 0) {
            PlayerFoodStats.change(player, row.fat(), GTFoodStats.FAT);
        }
        if (row.radiation() != 0) {
            PlayerRadiation.change(player, row.radiation());
        }
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS,
                0.5F, 0.4F / (player.level().random.nextFloat() * 0.4F + 0.8F));
        return true;
    }
}
