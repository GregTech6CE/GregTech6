package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.tool.GTToolHelper;
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
 * GT6 {@code Behavior_FlintAndTinder} ({@code Behavior_FlintAndTinder.java:43-95}) — the flint and
 * tinder meta-tool, i.e. {@code GTToolType.FLINT_AND_TINDER} in the port
 * ({@code GTToolType.java:41}, GT6 id 50).
 *
 * <h2>The three chances ({@code :49-55})</h2>
 *
 * <ol>
 *   <li>a tool whose material has a tool durability of {@code <= 1} — flint — always ignites;</li>
 *   <li>a material tagged {@code FLAMMABLE} or {@code BURNING} ignites at
 *       {@code flintChance + (100 - flintChance) / 2}, i.e. half again as often as a plain tool;</li>
 *   <li>everything else at the configured {@code flintChance}.</li>
 * </ol>
 *
 * <p>{@code mFlintChance} is GT6's own client config value,
 * {@code "general.FlintAndSteelChance"} ({@code GT6_Main:111}), whose default is {@code 30}
 * ({@code GT_Proxy:95}); {@link #DEFAULT_FLINT_CHANCE} carries it over because the port has no such
 * config entry. All three are clamped into {@code 1..100} by {@code UT.Code.bind(1, 100, ...)}, which
 * {@link #ignitionChance} keeps.</p>
 *
 * <h2>What the click costs ({@code :48,56-60})</h2>
 *
 * <p>{@code tDamage} starts at {@code 5000} and is replaced by the block's tool-click return when a
 * branch fires. The tool is then damaged by {@code units(max(10000, tDamage), 10000, 100, T)} — so
 * <b>every</b> click that gets past the gate costs at least 100 durability, including the ones whose
 * roll failed, and the click reports {@code T} either way. Only the obstructed-face gate at
 * {@code :46} makes it return {@code F} without spending anything.</p>
 *
 * <h2>Port notes</h2>
 *
 * <ul>
 *   <li>{@code TD.Properties.BURNING} has no counterpart in the port's material table
 *       ({@code MaterialProperty:23} carries {@code FLAMMABLE}, {@code :27} carries
 *       {@code UNBURNABLE}); the second branch therefore tests {@code FLAMMABLE} alone.</li>
 *   <li>The original checks {@code aWorld.isRemote} only <em>after</em> the roll and the tool click
 *       ({@code :57}). The port refuses client-side clicks up front instead: the roll would run twice
 *       and the durability write would be undone by the next server packet.</li>
 *   <li>{@code SIDES_VALID} and {@code aPlayer instanceof FakePlayer} from {@code :46} have no port
 *       counterpart — {@link Direction} cannot be invalid and no port item click comes from a
 *       {@code FakePlayer}. {@link ItemBehaviors#obstructed} keeps the half that matters.</li>
 *   <li>The ignition itself is GT6's {@code TOOL_igniter} click, i.e. the block's own reaction first
 *       and the vanilla fire/TNT fallback second ({@code IBlockToolable.java:77-80} dispatches to
 *       {@code ToolCompat} only when the block is not {@code IBlockToolable}) — see
 *       {@link ItemBehaviors#igniteToolClick}, {@link ItemBehaviors#primeTnt} and
 *       {@link ItemBehaviors#lightVanillaFire}.</li>
 * </ul>
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
     * GT6 {@code Behavior_FlintAndTinder:49-55}: how many of the 100 roll values ignite.
     *
     * <p>The order is the original's {@code if}/{@code else if}/{@code else} chain, and each result
     * is clamped into {@code 1..100} exactly like {@code UT.Code.bind(1, 100, ...)}. A flammable tool
     * gets {@code chance + (100 - chance) / 2} — integer division, so {@code 30} becomes {@code 65}.</p>
     *
     * @param toolDurability the tool material's GT6 {@code mToolDurability}; {@code <= 1} means flint
     *                       and ignites unconditionally
     * @param flammable      whether the tool material is {@code FLAMMABLE} (or {@code BURNING}, which
     *                       the port's material table does not carry)
     * @param flintChance    GT6's configured chance, {@link #DEFAULT_FLINT_CHANCE} by default
     * @return the chance out of 100, never below 1 and never above 100
     */
    public static int ignitionChance(long toolDurability, boolean flammable, int flintChance) {
        int bound = (int) Math.max(1L, Math.min(100L, flintChance));              // UT.Code.bind(1,100,·)
        if (toolDurability <= 1) return 100;                                      // :49-50
        if (flammable) return (int) Math.max(1L, Math.min(100L, bound + (100L - bound) / 2)); // :51-52
        return bound;                                                             // :53-54
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
        if (player != null && ItemBehaviors.obstructed(level, pos, side)) return Outcome.refused(stack); // :46

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
     * GT6 {@code IBlockToolable.Util.onToolClick(TOOL_igniter, Long.MAX_VALUE, 1, ...)} followed by
     * the vanilla fallback of {@code ToolCompat} ({@code IBlockToolable.java:77-80}).
     *
     * @return the ignition cost in GT6's 1/10000 units, {@code 0} when nothing reacted
     */
    public static long ignite(Level level, BlockPos pos, Direction side, @Nullable Player player, ItemStack stack,
                              float hitX, float hitY, float hitZ) {
        boolean sneaking = player != null && player.isShiftKeyDown();
        long cost = ItemBehaviors.igniteToolClick(level, pos, side, player, stack, sneaking, hitX, hitY, hitZ);
        if (cost > 0) return cost;
        if (ItemBehaviors.primeTnt(level, pos, player)) return ItemBehaviors.TNT_COST;   // ToolCompat:203-207
        if (ItemBehaviors.lightVanillaFire(level, player, pos, side, stack)) return ItemBehaviors.TNT_COST;
        return 0L;
    }

    /**
     * GT6 {@code Behavior_FlintAndTinder:69-78}: left-clicking a creeper lights its fuse for 100
     * durability.
     *
     * <p>{@code EntityCreeper.func_146079_cb()} is 1.20.1's {@link Creeper#ignite()}. The original
     * refuses on the client ({@code :70}); the port's caller is server-side, and
     * {@link Creeper#ignite()} is a synced entity-data write either way, so no extra guard is
     * added.</p>
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
