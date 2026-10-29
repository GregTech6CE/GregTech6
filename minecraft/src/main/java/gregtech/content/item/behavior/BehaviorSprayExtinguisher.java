package gregtech.content.item.behavior;

import gregtech.content.item.behavior.ItemBehaviors.Consumable;
import gregtech.content.item.behavior.ItemBehaviors.Outcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/**
 * GT6 {@code Behavior_Spray_Extinguisher} ({@code Behavior_Spray_Extinguisher.java:45-147}) — the
 * CO2 fire extinguisher.
 *
 * <h2>The original</h2>
 *
 * <p>{@code extinguish(...)} has two halves.</p>
 *
 * <ol>
 *   <li>The block hook: when the clicked face is free — GT6's
 *       {@code aPlayer == null || SIDES_INVALID[aSide] || FakePlayer || !WD.obstructed(...)} — the
 *       click goes to {@code onToolClick(TOOL_extinguisher, aUses * 1000, 1,...)} and its answer is
 *       converted with {@code min(10, tDamage / 1000)}. That tool kind really is implemented in GT6:
 *       the dynamite is defused ({@code MultiTileEntityDynamite:101}), the three generators stop
 *       burning ({@code MultiTileEntityGeneratorSolid:227}, {@code MultiTileEntityGeneratorLiquid:181},
 *       {@code MultiTileEntityGeneratorFluidBed:203}) and the seven mini portals close at line 119 of
 *       their class ({@code MultiTileEntityMiniPortal*}).</li>
 *   <li>The area: with at least {@code 10} uses left, a {@code 3x3x3} volume in
 *       front of the clicked face has its fire blocks removed, then every entity in
 *       the {@code 5x5x5} box around that volume that is a {@link Blaze} takes 10 damage or that is
 *       burning is extinguished. Every one of those steps costs {@code 10} uses
 *       and the loop stops the moment the budget would be exceeded.</li>
 * </ol>
 *
 * <h2>Port notes</h2>
 *
 * <ul>
 *   <li>The block hook is {@link Extinguishable} — the port's half of
 *       {@code IBlockToolable.onToolClick(TOOL_extinguisher,...)}. Dynamite and other implementors
 *       can handle the tool before the area-extinguishing fallback runs.</li>
 *   <li>{@code tEntity.getClass() == EntityBlaze.class} is an exact class test in the
 *       original, so the port keeps {@code getClass() == Blaze.class} instead of
 *       {@code instanceof}.</li>
 *   <li>The damage source is GT6's {@code DamageSources.getCombatDamage("player", aPlayer, null, F)};
 *       1.20.1's player-attack source is its equivalent.</li>
 *   <li>{@code SIDES_INVALID} and {@code FakePlayer} have no port counterpart, as
 *       everywhere else in this package.</li>
 * </ul>
 */
public final class BehaviorSprayExtinguisher {

    /** GT6 {@code MultiItemRandomTools:298}: {@code (Spray_Empty, Spray_Extinguisher_Used, Spray_Extinguisher, 256)}. */
    public static final long USES = 256L;

    /** GT6 {@code Behavior_Spray_Extinguisher:53}: {@code mUses = aUses * 10}. */
    public static final int USES_MULTIPLIER = 10;

    /** GT6's: the area half needs at least this many uses. */
    public static final int AREA_MIN_USES = 10;

    /** GT6's: one fire block, one blaze hit, one extinguished entity. */
    public static final int EXTINGUISH_COST = 10;

    /** GT6's: {@code min(10, tDamage / 1000)} for the block hook's answer. */
    public static final int HOOK_MAX_COST = 10;

    /** GT6's: {@code attackEntityFrom(..., 10)}. */
    public static final float BLAZE_DAMAGE = 10.0F;

    private BehaviorSprayExtinguisher() {}

    /**
     * A block (or block entity) that reacts to GT6's {@code TOOL_extinguisher} ({@code CS.java:1036}):
     * the port's half of {@code IBlockToolable.onToolClick}.
     *
     * <p>GT6's implementors answer {@code 10000} — one full unit — when they defused themselves or
     * stopped burning; the extinguisher turns that into {@link #HOOK_MAX_COST} uses
     * ({@code Behavior_Spray_Extinguisher:100}).</p>
     */

//? if >=1.21.1 {
//?} else {
    public interface Extinguishable {
        /**
         * @return GT6's tool-click return value in 1/10000 units, {@code 0} when nothing was
         *         extinguished
         */
        long onExtinguish(Level level, BlockPos pos, Direction side, @Nullable Player player, ItemStack can,
                          boolean sneaking, float hitX, float hitY, float hitZ);
    }
//?}

    /** The port's can triple: the full id is registered first, the {@code _2} one is the used can. */
    public static Consumable fireExtinguisher() {
        return new Consumable(ItemBehaviors.stack("gt6ce:empty_spray_can"),
                ItemBehaviors.stack("gt6ce:fire_extinguisher_co2_2"),
                ItemBehaviors.stack("gt6ce:fire_extinguisher_co2"),
                USES * USES_MULTIPLIER);
    }

    /** GT6 {@code Behavior_Spray_Extinguisher:57-93}: one click. */
    public static Outcome useOn(Level level, BlockPos pos, Direction side, @Nullable Player player, ItemStack stack,
                                float hitX, float hitY, float hitZ) {
        return ItemBehaviors.useSpray(level, pos, side, player, stack, fireExtinguisher(),
                BehaviorSprayExtinguisher::extinguish, hitX, hitY, hitZ);
    }

    /**
     * GT6's: the block hook first, then the {@code 3x3x3} of fire and the burning
     * entities around it.
     *
     * @return the uses consumed, {@code 0} when nothing was extinguished
     */
    public static long extinguish(Level level, BlockPos pos, Direction side, long uses, @Nullable Player player,
                                  ItemStack can, float hitX, float hitY, float hitZ) {
        boolean sneaking = player != null && player.isShiftKeyDown();

        // - the block's own TOOL_extinguisher reaction, only when the face is reachable.
        if (!ItemBehaviors.obstructed(level, pos, side)) {
            Extinguishable target = level.getBlockEntity(pos) instanceof Extinguishable be ? be
                    : level.getBlockState(pos).getBlock() instanceof Extinguishable block ? block : null;
            if (target != null) {
                long damage = target.onExtinguish(level, pos, side, player, can, sneaking, hitX, hitY, hitZ);
                if (damage > 0) return Math.min(HOOK_MAX_COST, damage / 1000L);
            }
        }

        if (uses < AREA_MIN_USES) return 0L;

        long spent = 0L;
        BlockPos front = pos.relative(side);
        for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) for (int k = -1; k < 2; k++) {
            if (spent + EXTINGUISH_COST > uses) return spent;
            BlockPos fire = front.offset(i, j, k);
            if (level.getBlockState(fire).is(Blocks.FIRE)) {
                if (level.setBlock(fire, Blocks.AIR.defaultBlockState(), 3)) spent += EXTINGUISH_COST;
            }
        }

        List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AABB(
                front.getX() - 2, front.getY() - 2, front.getZ() - 2,
                front.getX() + 3, front.getY() + 3, front.getZ() + 3));
        for (Entity entity : entities) {
            if (spent + EXTINGUISH_COST > uses) return spent;
            spent += extinguishEntity(level, entity, player);
        }
        return spent;
    }

    /**
     * GT6's for a single entity, which is the whole per-entity rule: a blaze takes
     * {@link #BLAZE_DAMAGE}, any other burning entity is put out, and either one costs
     * {@link #EXTINGUISH_COST} uses. {@link #extinguish} hands its query result through here, so there
     * is one implementation of the rule.
     *
     * <p>It is public so it can be driven for an explicit entity: {@code Level.getEntitiesOfClass}
     * does not see entities in a chunk that was merely {@code setBlock}ed into
     * ({@code PersistentEntitySectionManager}, {@code EntitySectionStorage}), so the area query
     * cannot reach them either.</p>
     *
     * @return the uses this entity costs, {@code 0} when it costs nothing
     */
    public static long extinguishEntity(Level level, Entity entity, @Nullable Player player) {
        if (level == null || entity == null) return 0L;
        if (entity.getClass() == Blaze.class) {
            if (player != null) entity.hurt(level.damageSources().playerAttack(player), BLAZE_DAMAGE);
            else entity.hurt(level.damageSources().generic(), BLAZE_DAMAGE);
            return EXTINGUISH_COST;
        }
        if (entity.isOnFire()) {
            entity.clearFire();
            return EXTINGUISH_COST;
        }
        return 0L;
    }
}
