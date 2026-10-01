package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.item.behavior.ItemBehaviors.Consumable;
import com.gregtech.gregtech.item.behavior.ItemBehaviors.Outcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * GT6 {@code Behavior_Lighter} ({@code Behavior_Lighter.java:40-160}) — the matches, the match box,
 * the three lighters and the two fire starters.
 *
 * <h2>The two shapes</h2>
 *
 * <p>The behaviour has two constructors ({@code :44-58}) and therefore two completely different
 * click paths:</p>
 *
 * <ul>
 *   <li><b>Single use</b> ({@code new Behavior_Lighter(9000)}): the match and the two fire starters,
 *       where {@code mEmptyLighter}, {@code mUsedLighter} and {@code mFullLighter} are all
 *       {@code null} and {@code mFuelAmount} is {@code 1}. The click rolls {@code rng(10000) <
 *       mChance} ({@code :89}); a success ignites ({@code :91}) and consumes the item, a failure
 *       sets {@code tDamage = 10000} ({@code :94}) — which consumes the item just the same, because
 *       the {@code useUp} at {@code :97} is gated on {@code tDamage != 0}. Only a successful roll on
 *       a block that reports no ignition cost at all leaves the match intact.</li>
 *   <li><b>Multi use</b> ({@code :368}, {@code :377}, {@code :387}, {@code :358}): a full can turns
 *       into the used one and carries {@link LighterSpec#fuelAmount} in {@code gt.lighter}
 *       ({@code :122-128}); each attempt — successful or not — burns a fuel unit
 *       ({@code :112-114}), and a stack that is neither the used nor the full variant does
 *       nothing.</li>
 * </ul>
 *
 * <p>Both shapes refuse while the stack is stacked, unless they are single use
 * ({@code :62}, {@code :83}: {@code aStack.stackSize != 1 && (mFuelAmount != 1 || mEmptyLighter !=
 * null)} — a stackable single-use item is the one exception).</p>
 *
 * <h2>The port's items</h2>
 *
 * <p>All seven families exist in {@code GTMultiItemsGen.ENTRIES:72-86}; the full variant is the
 * registered-first id and the used variant the {@code _2}/bare one, matching
 * {@code MultiItemRandomTools:365-367} (Invar), {@code :374-376} (platinum),
 * {@code :383-386} (plastic, whose empty variant is the broken lighter), {@code :356-357} (match
 * box) and {@code :343} / {@code :394} / {@code :397} (the three single-use ones). The chances and
 * fuel amounts are the original's constructor arguments and live in the {@link LighterSpec}
 * factories.</p>
 *
 * <h2>Port notes</h2>
 *
 * <ul>
 *   <li>The fuel counter is {@code gt.lighter} ({@code UT.java:2087,2093}), not the tapes' and
 *       sprays' {@code gt.remaining}; {@link Consumable} takes the tag name as a parameter because
 *       of it.</li>
 *   <li>The original refuses client-side clicks at {@code :62}/{@code :83}; the port refuses them up
 *       front for the same reason {@code BehaviorFlintAndTinder} does.</li>
 *   <li>The ignition itself is the shared {@code TOOL_igniter} click
 *       ({@code Behavior_Lighter:91,105}) — {@link BehaviorFlintAndTinder#ignite}.</li>
 * </ul>
 */
public final class BehaviorLighter {

    /** GT6 {@code UT.Code.bind(0, 10000, aChance)} ({@code Behavior_Lighter:49,57}). */
    public static final long MAX_CHANCE = 10000L;

    /** GT6 {@code Behavior_Lighter:94,108}: a failed roll costs a full unit anyway. */
    public static final long FAILED_ROLL_COST = 10000L;

    /** GT6 {@code Behavior_Lighter:91,105}: {@code onToolClick(TOOL_igniter, MAX, 3, ...)}. */
    public static final long IGNITER_QUALITY = 3L;

    private BehaviorLighter() {}

    /**
     * One lighter family: GT6's constructor arguments ({@code Behavior_Lighter:44-58}) plus the
     * three items, which the port names instead of passing in.
     *
     * @param can        the empty/used/full triple, with {@link Consumable#LIGHTER_KEY} as the tag
     * @param fuelAmount GT6's {@code mFuelAmount}; {@code 1} together with an empty
     *                   {@link Consumable#empty()} is the single-use shape
     * @param chance     GT6's {@code mChance} out of {@link #MAX_CHANCE}; clamped like
     *                   {@code UT.Code.bind(0, 10000, aChance)}
     */
    public record LighterSpec(Consumable can, long fuelAmount, long chance) {

        /** GT6 {@code Behavior_Lighter:62,83}: only a single-use lighter may be stacked. */
        public boolean accepts(ItemStack stack) {
            return stack.getCount() == 1 || (fuelAmount == 1 && can.empty().isEmpty());
        }

        /** GT6 {@code ST.invalid(mUsedLighter)} ({@code :86}): the single-use shape. */
        public boolean singleUse() {
            return can.used().isEmpty();
        }
    }

    private static LighterSpec spec(String empty, String used, String full, long fuelAmount, long chance) {
        Consumable can = new Consumable(
                empty == null ? ItemStack.EMPTY : ItemBehaviors.stack(empty),
                used == null ? ItemStack.EMPTY : ItemBehaviors.stack(used),
                full == null ? ItemStack.EMPTY : ItemBehaviors.stack(full),
                fuelAmount, Consumable.LIGHTER_KEY);
        return new LighterSpec(can, fuelAmount, Math.max(0L, Math.min(MAX_CHANCE, chance)));
    }

    /** GT6 {@code MultiItemRandomTools:343}: {@code new Behavior_Lighter(9000)} — one match. */
    public static LighterSpec match() { return spec(null, null, null, 1, 9000); }

    /** GT6 {@code MultiItemRandomTools:358}: match box, 64 matches at 90%. */
    public static LighterSpec matchBox() {
        return spec(null, "gregtech:match_box", "gregtech:match_box_full", 64, 9000);
    }

    /** GT6 {@code MultiItemRandomTools:368}: Invar lighter, 100 uses, never fails. */
    public static LighterSpec invar() {
        return spec("gregtech:lighter_empty", "gregtech:lighter", "gregtech:lighter_full", 100, 10000);
    }

    /** GT6 {@code MultiItemRandomTools:377}: platinum lighter, 1000 uses, never fails. */
    public static LighterSpec platinum() {
        return spec("gregtech:shiny_lighter_empty", "gregtech:shiny_lighter", "gregtech:shiny_lighter_full",
                1000, 10000);
    }

    /** GT6 {@code MultiItemRandomTools:387}: plastic lighter, 100 uses at 90%, breaks when empty. */
    public static LighterSpec plastic() {
        return spec("gregtech:plastic_lighter_broken", "gregtech:plastic_lighter",
                "gregtech:plastic_lighter_full", 100, 9000);
    }

    /** GT6 {@code MultiItemRandomTools:394}: fire starter from dry grass, one use at 50%. */
    public static LighterSpec fireStarter() { return spec(null, null, null, 1, 5000); }

    /** GT6 {@code MultiItemRandomTools:397}: fire starter from dry bark, one use at 55%. */
    public static LighterSpec fireStarterBark() { return spec(null, null, null, 1, 5500); }

    /**
     * GT6 {@code Behavior_Lighter:82-120}: one block click.
     *
     * @param level  the level the clicked block stands in
     * @param pos    the clicked block
     * @param side   the clicked face
     * @param player the holder, or {@code null} for GT6's auto-tool case
     * @param stack  the lighter stack, whose fuel and item are updated
     * @param spec   the family this stack belongs to ({@link #invar()} and friends)
     * @param random the level's random source ({@code RNGSUS} in the original)
     * @return whether the click was consumed; a failed roll on the multi-use shape still returns
     *         {@code true} because it burns a fuel unit ({@code :110-117})
     */
    public static Outcome useOn(Level level, BlockPos pos, Direction side, @Nullable Player player, ItemStack stack,
                                LighterSpec spec, RandomSource random, float hitX, float hitY, float hitZ) {
        if (level.isClientSide) return Outcome.refused(stack);                    // :83
        if (!spec.accepts(stack)) return Outcome.refused(stack);                  // :83
        ItemStack prepared = spec.can().prepared(stack);                          // :85
        boolean successful = random.nextInt((int) MAX_CHANCE) < spec.chance();    // :89,103

        if (spec.singleUse()) {                                                   // :86-99
            if (!successful) return Outcome.acted(spec.can().usedUp(prepared));    // :94-98
            long cost = BehaviorFlintAndTinder.ignite(level, pos, side, player, prepared, hitX, hitY, hitZ);
            // :96 - a roll that fired but found nothing to ignite costs nothing at all.
            if (cost == 0L) return Outcome.refused(prepared);
            return Outcome.acted(spec.can().usedUp(prepared));
        }

        if (!spec.can().isUsed(prepared)) return Outcome.refused(prepared);        // :100
        long cost = successful ? BehaviorFlintAndTinder.ignite(level, pos, side, player, prepared, hitX, hitY, hitZ)
                : FAILED_ROLL_COST;                                               // :103-109
        if (cost == 0L) return Outcome.refused(prepared);
        return Outcome.acted(consumeFuel(prepared, spec, cost, player));           // :110-117
    }

    /**
     * GT6 {@code Behavior_Lighter:61-79}: left-clicking a creeper with a multi-use lighter lights it
     * for one fuel unit.
     *
     * @return whether the entity was a creeper and got lit
     */
    public static Outcome useOnEntity(Entity entity, @Nullable Player player, ItemStack stack, LighterSpec spec) {
        if (!spec.accepts(stack)) return Outcome.refused(stack);                   // :62
        ItemStack prepared = spec.can().prepared(stack);                           // :67
        if (!(entity instanceof Creeper creeper)) return Outcome.refused(prepared);
        if (!spec.can().isUsed(prepared)) return Outcome.refused(prepared);        // :69
        creeper.ignite();                                                          // :71
        if (ItemBehaviors.creative(player)) return Outcome.acted(prepared);        // :72 skips the decrement
        return Outcome.acted(spec.can().spent(prepared, 1L));                      // :72-76
    }

    /**
     * GT6 {@code Behavior_Lighter:112-114}: {@code units(tDamage, 10000, 1, T)} fuel units, rounded
     * up, and {@code useUp} once none are left.
     */
    public static ItemStack consumeFuel(ItemStack stack, LighterSpec spec, long cost, @Nullable Player player) {
        if (ItemBehaviors.creative(player)) return stack;                          // :111
        long units = com.gregtech.gregtech.content.tool.ConsumableRules.ignitionUnits(cost);                         // UT.Code.units(..., T)
        return spec.can().spent(stack, Math.max(1L, units));
    }
}
