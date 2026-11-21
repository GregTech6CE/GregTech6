package gregtech.content.block.machine;

import gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import gregtech.content.block.GTBlock;
/** Large Crucible multiblock controller. 3x3x3 hollow of crucible wall blocks, HU-powered.
 *  27x capacity of single-block crucible. */
public class LargeCrucibleControllerBlock extends GTBlock
        implements gregtech.engine.multiblock.StructurePreviewSource {
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        if (!level.isClientSide && !entity.isSpectator() && entity instanceof net.minecraft.world.entity.LivingEntity
                && level.getBlockEntity(pos) instanceof LargeCrucibleControllerBlockEntity crucible)
            gregtech.util.GTEntityHelper.applyTemperatureDamage(entity, crucible.getTemperature());
        super.stepOn(level, pos, state, entity);
    }
    private final gregtech.content.multiblock.LargeCrucibleSpecs.Variant variant;

    /** A formed vessel keeps a solid floor but stops supporting blocks on its open mouth. */
    private static final VoxelShape FORMED_SUPPORT_SHAPE =
            Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 15.0D / 16.0D, 1.0D);

    public LargeCrucibleControllerBlock() {
        this(gregtech.content.multiblock.LargeCrucibleSpecs.LEGACY,
                Properties.of().strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    }

    public LargeCrucibleControllerBlock(Properties properties) {
        this(gregtech.content.multiblock.LargeCrucibleSpecs.LEGACY, properties);
    }

    public LargeCrucibleControllerBlock(
            gregtech.content.multiblock.LargeCrucibleSpecs.Variant variant,
            Properties properties) {
        super(properties);
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(MultiblockPortBlock.CRUCIBLE_FORMED, false));
    }

    public gregtech.content.multiblock.LargeCrucibleSpecs.Variant variant() { return variant; }

    // GT6 MultiTileEntityCrucible disables every valid side, so no wrench rotation exists.
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MultiblockPortBlock.CRUCIBLE_FORMED);
    }

    @Override public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return state.getValue(MultiblockPortBlock.CRUCIBLE_FORMED)
                ? net.minecraft.world.level.block.RenderShape.INVISIBLE : net.minecraft.world.level.block.RenderShape.MODEL;
    }

    /** Vanilla fire asks this shape for the top support, so a formed mouth accepts no flame. */
    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(MultiblockPortBlock.CRUCIBLE_FORMED)
                ? FORMED_SUPPORT_SHAPE : super.getBlockSupportShape(state, level, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new LargeCrucibleControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof LargeCrucibleControllerBlockEntity ce)
                LargeCrucibleControllerBlockEntity.serverTick(lvl, pos, st, ce);
        };
    }

    @Override
    protected void appendTooltip(@NotNull ItemStack stack, @Nullable BlockGetter level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        gregtech.client.CrucibleTooltips.appendLargeCrucible(variant.crucible(), tooltip);
    }
    @Override public void onRemove(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
//? if >=1.21.1 {
                /*&& level.getBlockEntity(pos) instanceof LargeCrucibleControllerBlockEntity contents) {
*///?} else {
                && level.getBlockEntity(pos) instanceof gregtech.engine.inventory.BlockContents contents) {
//?}
            contents.dropContents();
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }
    @Override
    protected InteractionResult useBlock(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (gregtech.engine.tool.ToolInteractions.use(state, level, pos, player, hand, hit)) return InteractionResult.sidedSuccess(level.isClientSide);
        InteractionResult result = InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof LargeCrucibleControllerBlockEntity be)
            result = level.isClientSide
                    ? (be.canHandleUse(player, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS)
                    : be.tryUse(player, hand, hit);
        if (result != InteractionResult.PASS) return result;
        // GT6's crucible consumes an unclaimed click on the open mouth instead of passing it on to
        // the held item, which would otherwise put a block or a fire into the hollow.
        return state.getValue(MultiblockPortBlock.CRUCIBLE_FORMED) && hit.getDirection() == Direction.UP
                ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }
//? if >=1.21.1 {
    /*@Override public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder context) { return List.of(new ItemStack(this)); }
*///?} else {
//?}
}
