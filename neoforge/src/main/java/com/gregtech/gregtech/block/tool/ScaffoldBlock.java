package com.gregtech.gregtech.block.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
 * GT6 {@code MultiTileEntityScaffold}: unsupported ledges fall, while a ledge joined to a
 * vertically supported scaffold along its facing axis remains in place. Design 0 is the
 * two-pixel ledge, 1 the lower hatch, 2 the open upper segment, and 3 the solid top plate.
 */
public final class ScaffoldBlock extends ShapedToolBlock {
    public static final IntegerProperty DESIGN = IntegerProperty.create("design", 0, 3);
    private static final VoxelShape LEDGE = Block.box(0, 14, 0, 16, 16, 16);
    private static final VoxelShape POSTS = Shapes.or(
            Block.box(0, 0, 0, 2, 16, 2), Block.box(0, 0, 14, 2, 16, 16),
            Block.box(14, 0, 0, 16, 16, 2), Block.box(14, 0, 14, 16, 16, 16)).optimize();
    private static final VoxelShape NORTH_FRAME = Shapes.or(POSTS, Block.box(0, 0, 0, 16, 16, 2)).optimize();
    private static final VoxelShape SOUTH_FRAME = Shapes.or(POSTS, Block.box(0, 0, 14, 16, 16, 16)).optimize();
    private static final VoxelShape EAST_FRAME = Shapes.or(POSTS, Block.box(14, 0, 0, 16, 16, 16)).optimize();
    private static final VoxelShape WEST_FRAME = Shapes.or(POSTS, Block.box(0, 0, 0, 2, 16, 16)).optimize();

    private final com.gregtech.gregtech.api.material.GTMaterial material;

    public ScaffoldBlock(Properties properties) {
        this(com.gregtech.gregtech.content.material.Materials.Steel, properties);
    }

    public ScaffoldBlock(com.gregtech.gregtech.api.material.GTMaterial material, Properties properties) {
        // Design 1 collision depends on the walking entity (sneaking/height), so Forge must not
        // cache the no-entity shape during BlockState baking and reuse it for every player.
        super("scaffold", properties.dynamicShape());
        this.material = material;
        registerDefaultState(defaultBlockState().setValue(DESIGN, 1));
    }

    public com.gregtech.gregtech.api.material.GTMaterial material() { return material; }
    /** Original scaffold inherits the facing-wrench row without extra instructions. */
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        com.gregtech.gregtech.client.StorageBlockTooltips.facing(tooltip);
    }
    @Override public int tintRgb() { return material.getColor(); }
    private static boolean isScaffold(BlockState state) { return state.getBlock() instanceof ScaffoldBlock; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DESIGN);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide && !oldState.is(this)) level.scheduleTick(pos, this, 1);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, movedByPiston);
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                  net.minecraft.world.level.LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        // Also called by chunk postprocessing for naturally generated scaffolds.
        level.scheduleTick(pos, this, 1);
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.is(this)) return;
        boolean vertical = verticalSupport(level, pos);
        if (!vertical && !groundConnection(level, pos, state.getValue(FACING))) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
            return;
        }
        int design = com.gregtech.gregtech.content.tool.UtilityToolRules.scaffoldDesign(vertical,isScaffold(level.getBlockState(pos.above())),isScaffold(level.getBlockState(pos.below())));
        if (state.getValue(DESIGN) != design) level.setBlock(pos, state.setValue(DESIGN, design), Block.UPDATE_ALL);
    }

    private boolean verticalSupport(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState support = level.getBlockState(below);
        return isScaffold(support) || support.isSolidRender(level, below);
    }

    private boolean groundConnection(LevelReader level, BlockPos pos, Direction facing) {
        Direction first = facing.getAxis() == Direction.Axis.X ? Direction.EAST : Direction.NORTH;
        for (Direction direction : new Direction[]{first, first.getOpposite()}) {
            for (int distance = 1; distance < com.gregtech.gregtech.content.tool.UtilityToolRules.SCAFFOLD_SEARCH; distance++) {
                BlockPos next = pos.relative(direction, distance);
                if (!isScaffold(level.getBlockState(next))) break;
                if (verticalSupport(level, next)) return true;
            }
        }
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // GT6 selects the whole cube for the three structural designs, even though their
        // entity collision is hollow or passable; only a cantilever ledge selects as a slab.
        return state.getValue(DESIGN) == 0 ? LEDGE : Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape frame = collisionFrame(state.getValue(FACING));
        return switch (state.getValue(DESIGN)) {
            case 0 -> LEDGE;
            case 1 -> context instanceof EntityCollisionContext collision
                    && collision.getEntity() != null
                    && (collision.getEntity().isShiftKeyDown()
                    || collision.getEntity().getY() < pos.getY() + 1)
                    ? frame : Shapes.or(LEDGE, frame).optimize();
            case 2 -> frame;
            default -> Shapes.block();
        };
    }

    private static VoxelShape collisionFrame(Direction facing) {
        return switch (facing) {
            case NORTH -> NORTH_FRAME;
            case SOUTH -> SOUTH_FRAME;
            case EAST -> EAST_FRAME;
            case WEST -> WEST_FRAME;
            default -> Shapes.empty();
        };
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return state.getValue(DESIGN) != 3;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this));
    }
}
