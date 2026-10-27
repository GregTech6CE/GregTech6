package gregtech.content.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** GT6 BlockMetaType six directional half blocks, used by street edges and bridge parapets. */
public class CFoamSlabBlock extends ColoredConstructionBlock implements gregtech.engine.block.FaceGridSlab {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape[] SHAPES = {
        Block.box(0,0,0,16,8,16), Block.box(0,8,0,16,16,16),
        Block.box(0,0,0,16,16,8), Block.box(0,0,8,16,16,16),
        Block.box(0,0,0,8,16,16), Block.box(8,0,0,16,16,16)
    };
    public CFoamSlabBlock(Properties properties) {
        super(properties, DyeColor.WHITE);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, gregtech.util.GTPlacementCode.resolveSlabFace(context));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get3DDataValue()];
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }

    /**
     * GT6 {@code BlockMetaType.onBlockActivated}: clicking the open face of a half block with the
     * same half fills the cell into the whole block. Without it two halves can never be joined,
     * because the occupied cell is not replaceable and placement always lands beside it.
     */
//? if >=1.21.1 {
    /*@Override public net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
*///?} else {
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
//?}
                                 InteractionHand hand, BlockHitResult hit) {
        Block whole = wholeBlock(state);
        if (whole == null || !player.getItemInHand(hand).is(asItem())
                || hit.getDirection() != state.getValue(FACING).getOpposite()) {
//? if >=1.21.1 {
            /*return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
*///?} else {
            return InteractionResult.PASS;
//?}
        }
        if (!level.isUnobstructed(whole.defaultBlockState(), pos, CollisionContext.of(player))) {
//? if >=1.21.1 {
            /*return net.minecraft.world.ItemInteractionResult.FAIL;
*///?} else {
            return InteractionResult.FAIL;
//?}
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, whole.defaultBlockState());
            level.playSound(null, pos, whole.defaultBlockState().getSoundType(level, pos, player).getPlaceSound(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
        }
//? if >=1.21.1 {
        /*return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
*///?} else {
        return InteractionResult.sidedSuccess(level.isClientSide);
//?}
    }

    /** The whole block of this slab's family and colour, or {@code null} when it has none. */
    private static Block wholeBlock(BlockState state) {
        String family = gregtech.content.registry.GTConstructionVariants.family(state.getBlock());
        if (family == null || !family.endsWith("_slab")) return null;
        return gregtech.content.registry.GTConstructionVariants.coloredBlock(
                family.substring(0, family.length() - "_slab".length()), FixedConstructionColor.colorOf(state));
    }
}
