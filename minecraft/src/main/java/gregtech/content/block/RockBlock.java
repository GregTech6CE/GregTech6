package gregtech.content.block;

import gregtech.api.material.GTMaterial;
import gregtech.content.blockentity.RockBlockEntity;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.GTWaterloggable;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialStackItemHelper;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Ground pebble (GT6 surface rock) — drops/picks up the {@code rockGt} item of its
 * material. Its stored material chooses the surface texture, while
 * ordinary pebbles retain their position-dependent shape.
 */
public class RockBlock extends GTBlock implements EntityBlock, SimpleWaterloggedBlock {
    /** Right-click picks the rock up (rockGt item, or only raw ore when marking a bedrock deposit). */
    @Override
    protected InteractionResult useBlock(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            List<ItemStack> stacks = yields(level.getBlockEntity(pos));
            level.removeBlock(pos, false);
            for (ItemStack stack : stacks) {
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static final VoxelShape SHAPE = box(3.0, 0.0, 3.0, 13.0, 4.0, 13.0);

    /** The state is sent before block entity data, including to other players watching placement. */
    public enum Appearance implements StringRepresentable {
        MATERIAL,
        INDICATOR,
        FLINT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Appearance> APPEARANCE = EnumProperty.create("appearance", Appearance.class);

    public RockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition
                .any()
                .setValue(APPEARANCE, Appearance.MATERIAL)
                .setValue(GTWaterloggable.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(APPEARANCE, GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return GTWaterloggable.getStateForPlacement(super.getStateForPlacement(context), context);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RockBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState neighbour,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos) {
        if (!canSurvive(state, level, pos)) return getFluidState(state).createLegacyBlock();
        GTWaterloggable.tickWater(state, level, pos);
        return super.updateShape(state, side, neighbour, level, pos, neighbourPos);
    }

    /** rockGt of the material, or only oreRaw when the rock marks a bedrock deposit. */
    public static List<ItemStack> yields(@Nullable BlockEntity be) {
        // GT6's item-on-the-ground mode: the rock carries the exact item it hands out.
        if (be instanceof RockBlockEntity rock && !rock.itemId().isEmpty()) {
            ItemStack explicit = rock.itemStack();
            // The original lying-item mode represents one exact item. Indicator bonuses belong
            // to the separate material-rock mode; otherwise meteoric raw ore is duplicated.
            return explicit.isEmpty() ? List.of() : List.of(explicit);
        }
        String materialName =
                be instanceof RockBlockEntity rock ? rock.getMaterial() : RockBlockEntity.DEFAULT_MATERIAL;
        GTMaterial material = GTMaterialRegistry.get(materialName);
        if (!material.resolve().isValid()) return List.of();
        int amount = be instanceof RockBlockEntity rock ? rock.count() : 1;
        // Original bedrock markers carry only oreRaw, not an extra rockGt stack. Also repairs
        // legacy markers that stored the old rawOre flag instead of an explicit item identity.
        MaterialPrefix prefix =
                be instanceof RockBlockEntity rock && rock.hasRawOre() ? MaterialPrefix.oreRaw : MaterialPrefix.rockGt;
        ItemStack result = MaterialStackItemHelper.mat(prefix, material.resolve(), amount);
        return result.isEmpty() ? List.of() : List.of(result);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return yields(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY));
    }
}
