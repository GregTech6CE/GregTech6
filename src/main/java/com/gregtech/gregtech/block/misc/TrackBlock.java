package com.gregtech.gregtech.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jetbrains.annotations.Nullable;

/**
 * GT6's {@code BlockBaseRail} — the three rail families {@code Loader_Rails:41-72} registers.
 *
 * <p>GT6 gives every rail a material speed and turns it into the cart's maximum speed
 * ({@code getRailMaxSpeed}, {@code BlockBaseRail:277-289}): the full speed only when the cart is on
 * a straight run whose both neighbours are rails of the same axis, {@code min(speed, 0.4)} on a
 * curve or a dead end, and {@code min(speed, 1.0)} when the neighbouring chunks are not loaded.</p>
 *
 * <p>1.20.1 exposes exactly those hooks through Forge: {@code IForgeBaseRailBlock.getRailMaxSpeed}
 * and {@code IForgeBaseRailBlock.onMinecartPass} (called once per tick while a cart is on the rail,
 * which is where GT6's boost/brake logic lives, {@code BlockBaseRail:291-322}).</p>
 *
 * <p><b>Port difference (closed in §115):</b> Forge clamps every rail to the cart's own ceiling
 * ({@code IForgeAbstractMinecart.getMaxCartSpeedOnRail()}, default 1.2 — its setter is
 * {@code Math.min(value, getMaxCartSpeedOnRail())}, {@code AbstractMinecart:844}), so GT6's three
 * fastest rails — Tungstensteel 1.4, Tungstencarbide 1.6, Adamantium 4.0 — used to top out at 1.2.
 * {@code mixin/AbstractMinecartSpeedCapMixin} (the port's replacement for GT6's
 * {@code Minecraft_RemoveCartSpeedCap} ASM) makes the setter a plain assignment, and every rail here
 * hands its own speed to the cart in {@link #applySpeedToCart}. A cart that never meets a GT6 rail is
 * untouched and keeps Forge's 1.2.</p>
 */
public final class TrackBlock {

    /** GT6 {@code BlockBaseRail:287}: a rail that is not part of a straight run. */
    static final float CURVE_SPEED = 0.4F;
    /** GT6 {@code BlockBaseRail:282/285}: straight run with unloaded neighbours. */
    static final float UNLOADED_SPEED = 1.0F;

    private TrackBlock() {}

    /**
     * Hands the rail's speed to the cart the rail is carrying.
     *
     * <p>GT6 reads the speed straight off the rail it is standing on ({@code BlockBaseRail:277-289} is
     * the rail's own answer, and {@code TileEntityBase10ConnectorRendered} carts consult it directly).
     * 1.20.1 routes it through the cart instead: {@code getMaxSpeedWithRail} is
     * {@code min(railMaxSpeed, cart.getCurrentCartSpeedCapOnRail())} ({@code AbstractMinecart:861-862}),
     * so the rail has to leave its number on the cart — otherwise Forge's 1.2 ceiling decides. Called
     * from {@code onMinecartPass}, which vanilla invokes once per tick for the rail under the cart
     * ({@code AbstractMinecart:530}), so the value always belongs to the rail the cart is on right
     * now.</p>
     */
    static void applySpeedToCart(float speed, BlockState state, Level level, BlockPos pos,
                                 @Nullable AbstractMinecart cart) {
        if (cart == null) return;
        cart.setCurrentCartSpeedCapOnRail(railSpeed(speed, state, level, pos));
    }

    /** GT6's straight-run rule for one rail position. */
    static float railSpeed(float speed, BlockState state, Level level, BlockPos pos) {
        RailShape shape = state.getValue(((BaseRailBlock) state.getBlock()).getShapeProperty());
        boolean straight = switch (shape) {
            case NORTH_SOUTH -> sameAxis(level, pos.north(), RailShape.NORTH_SOUTH)
                    && sameAxis(level, pos.south(), RailShape.NORTH_SOUTH);
            case EAST_WEST -> sameAxis(level, pos.east(), RailShape.EAST_WEST)
                    && sameAxis(level, pos.west(), RailShape.EAST_WEST);
            default -> false;
        };
        if (!straight) return Math.min(speed, CURVE_SPEED);
        // GT6 asks `doChunksNearChunkExist(..., 17)`; a 17-block radius spans the neighbour chunks.
        return level.hasChunksAt(pos.offset(-16, 0, -16), pos.offset(16, 0, 16))
                ? speed : Math.min(speed, UNLOADED_SPEED);
    }

    private static boolean sameAxis(Level level, BlockPos pos, RailShape shape) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BaseRailBlock rail
                && state.getValue(rail.getShapeProperty()) == shape;
    }

    /** GT6's plain track ({@code Loader_Rails:41-50}). */
    public static final class Straight extends RailBlock {
        private final float speed;

        public Straight(Properties properties, float speed) {
            super(properties);
            this.speed = speed;
        }

        @Override
        public float getRailMaxSpeed(BlockState state, Level level, BlockPos pos,
                                     @Nullable AbstractMinecart cart) {
            return railSpeed(speed, state, level, pos);
        }

        @Override
        public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
            applySpeedToCart(speed, state, level, pos, cart);
        }
    }

    /**
     * GT6's booster track ({@code Loader_Rails:52-61}, {@code mPowerRail = true}). Powered: doubles
     * a moving cart, or pushes a standing one away from the solid block it faces. Unpowered: brakes
     * the cart to half speed and stops it below 0.03.
     */
    public static final class Booster extends PoweredRailBlock {
        private final float speed;

        public Booster(Properties properties, float speed) {
            super(properties);
            this.speed = speed;
        }

        @Override
        public float getRailMaxSpeed(BlockState state, Level level, BlockPos pos,
                                     @Nullable AbstractMinecart cart) {
            return railSpeed(speed, state, level, pos);
        }

        @Override
        public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
            if (cart == null) return;
            applySpeedToCart(speed, state, level, pos, cart);
            var motion = cart.getDeltaMovement();
            double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            if (state.getValue(PoweredRailBlock.POWERED)) {
                if (horizontal > 0.01D) {
                    cart.setDeltaMovement(motion.x * 2, motion.y, motion.z * 2);
                    return;
                }
                // GT6 launches a standing cart away from the wall it touches.
                RailShape shape = state.getValue(getShapeProperty());
                if (shape == RailShape.EAST_WEST) {
                    if (solid(level, pos.west())) cart.setDeltaMovement(0.02D, motion.y, motion.z);
                    else if (solid(level, pos.east())) cart.setDeltaMovement(-0.02D, motion.y, motion.z);
                } else if (shape == RailShape.NORTH_SOUTH) {
                    if (solid(level, pos.north())) cart.setDeltaMovement(motion.x, motion.y, 0.02D);
                    else if (solid(level, pos.south())) cart.setDeltaMovement(motion.x, motion.y, -0.02D);
                }
            } else if (horizontal < 0.03D) {
                cart.setDeltaMovement(0, 0, 0);
            } else {
                cart.setDeltaMovement(motion.x / 2, 0, motion.z / 2);
            }
        }

        private static boolean solid(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return state.isRedstoneConductor(level, pos);
        }
    }

    /**
     * GT6's detector track ({@code Loader_Rails:63-72}). GT6 keeps the cart's momentum and only
     * reports the cart through redstone/comparator, which is what vanilla's
     * {@link DetectorRailBlock} does, so only the speed is added here.
     */
    public static final class Detector extends DetectorRailBlock {
        private final float speed;

        public Detector(Properties properties, float speed) {
            super(properties);
            this.speed = speed;
        }

        @Override
        public float getRailMaxSpeed(BlockState state, Level level, BlockPos pos,
                                     @Nullable AbstractMinecart cart) {
            return railSpeed(speed, state, level, pos);
        }

        @Override
        public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
            applySpeedToCart(speed, state, level, pos, cart);
        }
    }
}
