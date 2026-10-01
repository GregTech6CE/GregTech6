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
        return com.gregtech.gregtech.content.transport.TrackCatalog.speed(speed,straight,
                level.hasChunksAt(pos.offset(-16,0,-16),pos.offset(16,0,16)));
    }

    private static boolean sameAxis(Level level, BlockPos pos, RailShape shape) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BaseRailBlock rail
                && state.getValue(rail.getShapeProperty()) == shape;
    }

    static void moveCart(AbstractMinecart cart,BlockState state,Level level,BlockPos pos,boolean powered){
        var motion=cart.getDeltaMovement();var shape=state.getValue(((BaseRailBlock)state.getBlock()).getShapeProperty());
        int axis=shape==RailShape.EAST_WEST?1:shape==RailShape.NORTH_SOUTH?2:0;
        boolean launch=powered && !(Math.sqrt(motion.x*motion.x+motion.z*motion.z)>.01D);
        boolean negative=launch && (axis==1?solidWall(level,pos.west()):axis==2&&solidWall(level,pos.north()));
        boolean positive=launch && (axis==1?solidWall(level,pos.east()):axis==2&&solidWall(level,pos.south()));
        var next=com.gregtech.gregtech.content.transport.TrackMotionRules.apply(motion.x,motion.y,motion.z,powered,axis,negative,positive);
        cart.setDeltaMovement(next.x(),next.y(),next.z());
    }
    private static boolean solidWall(Level level,BlockPos pos){return level.getBlockState(pos).isRedstoneConductor(level,pos);}

    /** GT6's plain track ({@code Loader_Rails:41-50}). */
    public static final class Straight extends RailBlock {
        @Override public com.mojang.serialization.MapCodec<RailBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
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
        @Override public com.mojang.serialization.MapCodec<PoweredRailBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
        private final float speed;

        public Booster(Properties properties, float speed) {
            super(properties,true);
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
            moveCart(cart,state,level,pos,state.getValue(PoweredRailBlock.POWERED));
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
        @Override public com.mojang.serialization.MapCodec<DetectorRailBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
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
