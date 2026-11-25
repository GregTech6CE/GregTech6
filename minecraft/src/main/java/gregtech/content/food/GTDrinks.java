package gregtech.content.food;

import gregtech.content.nuclear.PlayerRadiation;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.data.generated.GTDrinksGen;
import gregtech.content.registry.GTFluids;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;

*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * GT6 {@code CS.DrinksGT}: what a drink does to you.
 *
 * <p>GT6 registers every drinkable fluid in {@code Loader_Fluids} as a {@code FoodStatDrink}, which puts the
 * entry into {@code DrinksGT.REGISTER} keyed by the fluid's name ({@code FoodStatDrink}); a
 * GT6 bottle (its source portion, {@code MultiItemBottles}) reads it through {@code FoodStatFluid}.
 * The port keys the same table by {@link Fluid}, generated from {@code Loader_Fluids.java} into
 * {@link GTDrinksGen} (131 rows; the fluid is resolved through {@link RegisteredFluids#get(String)}, which
 * carries GT6's own {@code FL} field names).
 *
 * <p>Drinking applies GT6's {@code FoodStat.onEaten}: the food level and saturation go through vanilla's food
 * data; the five nutrition statistics are removed, and drinks add no radiation.
 */
public final class GTDrinks {
    /** One {@code FoodStatDrink} row: the food values and the one statistic the port tracks. */
    public record Row(String field, int foodLevel, float saturation, int radiation, boolean alwaysEdible,
                      boolean invisibleParticles, String useAction, boolean milk, boolean extinguish,
                      boolean explosive, List<DrinkNutritionRows.Effect> effects) {}

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
            Row row=new Row(source.field(),source.foodLevel(),source.saturation(),source.radiation(),source.alwaysEdible(),source.invisibleParticles(),source.useAction(),source.milk(),source.extinguish(),source.explosive(),source.effects());
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
     * than by name, so every renamed fluid resolves correctly.
     */
    public static Fluid fluidOf(RegisteredFluids.FluidEntry entry) {
        if (entry == null) {
            return null;
        }
        // The catalog binds Lava to the vanilla fluid rather than a GT deferred holder.
        if (entry.isVanillaLava())
            return net.minecraft.world.level.material.Fluids.LAVA;
//? if >=1.21.1 {
        /*for (var ro
*///?} else {
        for (net.minecraftforge.registries.RegistryObject<Fluid> ro
//?}
                : gregtech.content.registry.GTFluids.FLUIDS.getEntries()) {
            if (GTFluids.isPresent(ro) && gregtech.content.registry.GTFluids.entryForFluid(ro.get()) == entry) {
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
        // Water is vanilla water; the catalog keeps no separate entry for it.
        if ("Water".equalsIgnoreCase(field)) return net.minecraft.world.level.material.Fluids.WATER;
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

    /**
     * GT6's always-edible gate. {@code canonicalBottle} is the port's {@code potion} flag: the fluid has a
     * canonical bottle item, so the drink is treated like a potion and ignores hunger. Item-level fallbacks
     * are canonical bottles too and pass {@code true}, which keeps them on the same gate as a live fluid.
     */
    public static boolean canEat(Player player, boolean canonicalBottle, boolean alwaysEdible) {
        return player.canEat(canonicalBottle || gregtech.GregTechConfig.Rule.ALWAYS_DRINK.enabled() || alwaysEdible);
    }

    /** Canonical potions and the original drink registry only; never arbitrary tank fluids. */
    public static boolean canDrink(Player player, FluidStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var bottle = gregtech.engine.fluid.PotionBottleHandler.filled(stack);
        int portion = bottle.getItem() instanceof gregtech.content.item.BottleItem item
                ? item.amount() : 250;
        if (stack.getAmount() < portion) return false;
        Row row = rowFor(stack);
        boolean potion = !bottle.isEmpty();
        return (row != null || potion) && canEat(player, potion, row != null && row.alwaysEdible());
    }

    /** External-mod effects remain in the source table but are unavailable without their owning mod. */
    private static net.minecraft.resources.ResourceLocation effectId(String source) {
        return switch (source) {
            case "Potion.blindness.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("blindness");
            case "Potion.confusion.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("nausea");
            case "Potion.damageBoost.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("strength");
            case "Potion.digSlowdown.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("mining_fatigue");
            case "Potion.digSpeed.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("haste");
            case "Potion.field_76444_x.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("absorption");
            case "Potion.fireResistance.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("fire_resistance");
            case "Potion.harm.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("instant_damage");
            case "Potion.heal.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("instant_health");
            case "Potion.hunger.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("hunger");
            case "Potion.invisibility.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("invisibility");
            case "Potion.jump.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("jump_boost");
            case "Potion.moveSlowdown.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("slowness");
            case "Potion.moveSpeed.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("speed");
            case "Potion.nightVision.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("night_vision");
            case "Potion.poison.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("poison");
            case "Potion.regeneration.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("regeneration");
            case "Potion.resistance.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("resistance");
            case "Potion.waterBreathing.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("water_breathing");
            case "Potion.weakness.id" -> gregtech.engine.effect.MobEffectCompat.vanillaId("weakness");
            default -> null;
        };
    }

    private static void potionEffects(Player player, net.minecraft.world.item.ItemStack potion) {
        for (var effect : gregtech.engine.effect.MobEffectCompat.potionEffects(potion)) {
            gregtech.engine.effect.MobEffectCompat.applyInstance(player, effect);
        }
    }

    /**
     * GT6 {@code FoodStat.onEaten} for a drink: food level, saturation, the drink sound and the source
     * effects. Returns false when the fluid is not a drink, so callers can pass.
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
            gregtech.engine.effect.MobEffectCompat.cureWithMilk(player);
        }
        for (var effect : row.effects()) {
            var id = effectId(effect.sourceId());
            if (id != null && player.getRandom().nextInt(100) < effect.chance()) {
                gregtech.engine.effect.MobEffectCompat.applyOrTrigger(
                        player, id, effect.duration(), effect.amplifier(), false, !row.invisibleParticles());
            }
        }
        if (row.explosive()) {
            player.level().explode(player, player.getX(), player.getY(), player.getZ(), 4, true,
                    net.minecraft.world.level.Level.ExplosionInteraction.TNT);
            player.hurt(player.damageSources().explosion(player, player), Float.MAX_VALUE);
        }
        player.getFoodData().eat(row.foodLevel(), row.saturation());
        if (row.radiation() != 0) {
            PlayerRadiation.change(player, row.radiation());
        }
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS,
                0.5F, 0.4F / (player.level().random.nextFloat() * 0.4F + 0.8F));
        return true;
    }
}
