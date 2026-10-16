package com.gregtech.gregtech.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * GT6 {@code BlockBaseBars}: four independent railing segments in one block.
 * The bits are north=1, south=2, west=4 and east=8, exactly as in GT6 metadata.
 */
public class BarsBlock extends Block {
    public static final IntegerProperty MASK = IntegerProperty.create("mask", 0, 15);
    private static final VoxelShape[] COLLISION = new VoxelShape[16];
    private static final VoxelShape[] OUTLINE = new VoxelShape[16];

    static {
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape collision = Shapes.empty();
            if ((mask & 1) != 0) collision = Shapes.or(collision, box(0, 0, 0, 16, 16, 2));
            if ((mask & 2) != 0) collision = Shapes.or(collision, box(0, 0, 14, 16, 16, 16));
            if ((mask & 4) != 0) collision = Shapes.or(collision, box(0, 0, 0, 2, 16, 16));
            if ((mask & 8) != 0) collision = Shapes.or(collision, box(14, 0, 0, 16, 16, 16));
            COLLISION[mask] = collision;
            OUTLINE[mask] = switch (mask) {
                case 1 -> box(0, 0, 0, 16, 16, 1);
                case 2 -> box(0, 0, 15, 16, 16, 16);
                case 4 -> box(0, 0, 0, 1, 16, 16);
                case 8 -> box(15, 0, 0, 16, 16, 16);
                default -> Shapes.block();
            };
        }
    }

    private final String materialName;
    private final int tintRgb;
    private final int harvestLevel;

    public BarsBlock(String materialName, int tintRgb, int harvestLevel, Properties properties) {
        super(properties);
        this.materialName = materialName;
        this.tintRgb = tintRgb;
        this.harvestLevel = harvestLevel;
        registerDefaultState(stateDefinition.any().setValue(MASK, 1));
    }

    public String materialName() { return materialName; }
    public int tintRgb() { return tintRgb; }
    public int harvestLevel() { return harvestLevel; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MASK);
    }

    /** GT6 chooses a segment by the clicked face and the quarter of the face hit. */
    public static int placementBit(Direction face, double hitX, double hitZ) {
        return com.gregtech.gregtech.block.BarsRules.placement(face.getAxis().name().charAt(0),hitX,hitZ);
    }

    public static int faceBit(Direction face) {
        return switch (face) {
            case NORTH -> 1;
            case SOUTH -> 2;
            case WEST -> 4;
            case EAST -> 8;
            default -> 0;
        };
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        double x = Mth.frac(context.getClickLocation().x);
        double z = Mth.frac(context.getClickLocation().z);
        return defaultBlockState().setValue(MASK, placementBit(context.getClickedFace(), x, z));
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Player player
                && player.getMainHandItem().is(asItem())) return Shapes.block();
        return OUTLINE[state.getValue(MASK)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext
                && (entityContext.getEntity() instanceof ItemEntity
                    || entityContext.getEntity() instanceof ExperienceOrb
                    || entityContext.getEntity() instanceof Projectile)) return Shapes.empty();
        return COLLISION[state.getValue(MASK)];
    }

    @Override public boolean useShapeForLightOcclusion(BlockState state) { return false; }

    /** One dropped item for every segment, as GT6's connection-count drop rule. */
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this, com.gregtech.gregtech.block.BarsRules.dropCount(state.getValue(MASK))));
    }
}
