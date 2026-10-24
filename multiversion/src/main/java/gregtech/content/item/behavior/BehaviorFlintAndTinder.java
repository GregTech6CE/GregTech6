package gregtech.content.item.behavior;

import gregtech.internal.material.MaterialProperty;
import gregtech.internal.tool.GTToolHelper;
//? if >=1.21.1 {
/*import gregtech.content.item.behavior.ManualToolBehaviorAccess.Outcome;
*///?} else {
import gregtech.content.item.behavior.ItemBehaviors.Outcome;
//?}
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
 * Flint-and-tinder attempts and shared native ignition dispatch.
 *
 * <p>The current implementation uses one probability roll. GT6
 * {@code Behavior_FlintAndTinder.onItemUseFirst} can make a second, ordinary-chance
 * roll after a flammable-material roll fails; its BURNING material flag is also
 * absent here. These differences are tracked as ALIGN-006 and ALIGN-127.</p>
 *
 * <p>Once the face gate passes, the tool costs at least 100 durability even if
 * ignition fails. Native hooks run before the vanilla TNT/fire fallback, but a
 * zero hook result currently enables that fallback as well (ALIGN-131).</p>
 *
 * <p>The server check precedes the roll in this adaptation. The caller supplies
 * the default chance of 30; no matching configuration entry is implemented.</p>
 */
public final class BehaviorFlintAndTinder {

    /** GT6 {@code GT_Proxy:95} / {@code GT6_Main:111}: default {@code "general.FlintAndSteelChance"}. */
    public static final int DEFAULT_FLINT_CHANCE = 30;

    /** GT6 {@code Behavior_FlintAndTinder:48}: {@code long tDamage = 5000} when no branch fired. */
    public static final long NO_IGNITION_COST = 5000L;

    /** GT6 {@code :58}: {@code units(max(10000, tDamage), 10000, 100, T)}. */
    public static final long MIN_TOOL_COST = 10000L;

    private BehaviorFlintAndTinder() {}

    /**
     * Current single-roll threshold, clamped to 1..100.
     *
     * <p>A durability of at most one returns 100; FLAMMABLE uses the enhanced
     * threshold. This helper does not model GT6's second attempt on a failed
     * enhanced roll, so its result is not the original combined probability.</p>
     */
    public static int ignitionChance(long toolDurability, boolean flammable, int flintChance) {
        int bound = (int) Math.max(1L, Math.min(100L, flintChance));
        if (toolDurability <= 1)
            return 100;
        if (flammable)
            return (int) Math.max(1L, Math.min(100L, bound + (100L - bound) / 2));
        return bound;
    }

    /**
     * GT6 {@code Behavior_FlintAndTinder:45-61}: one click of the flint and tinder.
     *
     * @param level       the level the clicked block stands in
     * @param pos         the clicked block
     * @param side        the clicked face
     * @param player      the holder, or {@code null} for GT6's auto-tool case
     * @param stack       the flint and tinder stack, damaged by the click
     * @param flintChance GT6's configured chance; use {@link #DEFAULT_FLINT_CHANCE}
     * @param random      the level's random source ({@code RNGSUS} in the original)
     * @return GT6's boolean return — after the gate this is {@code true} even when the roll failed
     */
    public static Outcome useOn(Level level, BlockPos pos, Direction side, @Nullable Player player, ItemStack stack,
                                int flintChance, RandomSource random, float hitX, float hitY, float hitZ) {
        if (level.isClientSide) return Outcome.refused(stack);                    // :57, moved up
//? if >=1.21.1 {
        /*if (player != null && ManualToolBehaviorAccess.obstructed(level, pos, side)) return Outcome.refused(stack); // :46
*///?} else {
        if (player != null && ItemBehaviors.obstructed(level, pos, side)) return Outcome.refused(stack); // :46
//?}

        boolean flammable = GTToolHelper.getStatMaterial(stack).has(MaterialProperty.FLAMMABLE);
        int chance = ignitionChance(GTToolHelper.getStatMaterial(stack).getToolDurability(), flammable, flintChance);

        long clickCost = 0L;
        if (random.nextInt(100) < chance) {                                       // :49-55
            clickCost = ignite(level, pos, side, player, stack, hitX, hitY, hitZ);
        }
        // :58 - always at least 100 durability, whether the roll fired or not.
        GTToolHelper.damageForToolClickReturn(stack, Math.max(MIN_TOOL_COST, clickCost), player);
        return Outcome.acted(stack);                                              // :60
    }

    /**
     * Ask a native ignition target, then try TNT/fire if its cost is zero.
     * GT6's IBlockToolable dispatch instead selects the native or compatibility
     * path by target type; ALIGN-131 tracks the difference.
     *
     * @return the ignition cost in GT6's 1/10000 units, {@code 0} when nothing reacted
     */
    public static long ignite(Level level, BlockPos pos, Direction side, @Nullable Player player, ItemStack stack,
                              float hitX, float hitY, float hitZ) {
        boolean sneaking = player != null && player.isShiftKeyDown();
//? if >=1.21.1 {
        /*long cost = ManualToolBehaviorAccess.igniteToolClick(level, pos, side, player, stack, sneaking, hitX, hitY, hitZ);
*///?} else {
        long cost = ItemBehaviors.igniteToolClick(level, pos, side, player, stack, sneaking, hitX, hitY, hitZ);
//?}
        if (cost > 0) return cost;
//? if >=1.21.1 {
        /*if (ManualToolBehaviorAccess.primeTnt(level, pos, player)) return ManualToolBehaviorAccess.TNT_COST;   // ToolCompat:203-207
        if (ManualToolBehaviorAccess.lightVanillaFire(level, player, pos, side, stack)) return ManualToolBehaviorAccess.TNT_COST;
*///?} else {
        if (ItemBehaviors.primeTnt(level, pos, player)) return ItemBehaviors.TNT_COST;   // ToolCompat:203-207
        if (ItemBehaviors.lightVanillaFire(level, player, pos, side, stack)) return ItemBehaviors.TNT_COST;
//?}
        return 0L;
    }

    /**
     * GT6 {@code Behavior_FlintAndTinder:69-78}: left-clicking a creeper lights its fuse for 100
     * durability.
     *
     * <p>The current helper has no side guard; GTToolItem invokes it from hurtEnemy.
     * This is a different callback from the original left-click interception
     * (ALIGN-129).</p>
     *
     * @return whether the entity was a creeper and got ignited
     */
    public static boolean igniteCreeper(Entity entity, ItemStack stack, @Nullable Player player) {
        if (!(entity instanceof Creeper creeper)) return false;                   // :71-76
        GTToolHelper.damageForToolClickReturn(stack, MIN_TOOL_COST, player);
        creeper.ignite();                                                         // :74
        return true;
    }
}
