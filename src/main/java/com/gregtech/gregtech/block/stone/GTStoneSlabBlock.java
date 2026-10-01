package com.gregtech.gregtech.block.stone;
import com.gregtech.gregtech.api.material.MaterialPresentation;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.registry.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** GT6-style oriented stone half-block (any axis, including vertical). */
public final class GTStoneSlabBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.DOWN, box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D));
        SHAPES.put(Direction.UP, box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D));
        SHAPES.put(Direction.NORTH, box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 8.0D));
        SHAPES.put(Direction.SOUTH, box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D));
        SHAPES.put(Direction.WEST, box(0.0D, 0.0D, 0.0D, 8.0D, 16.0D, 16.0D));
        SHAPES.put(Direction.EAST, box(8.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D));
    }

    private final StoneType stoneType;
    private final StoneVariant variant;
    private final GTMaterial stoneMaterial;

    public GTStoneSlabBlock(StoneType stoneType, StoneVariant variant) {
        super(StoneBlockProperties.properties(stoneType, variant));
        this.stoneType = stoneType;
        this.variant = variant;
        this.stoneMaterial = stoneType.material();
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN)
                .setValue(GTWaterloggable.WATERLOGGED, false));
    }

    public StoneType stoneType() {
        return stoneType;
    }

    public StoneVariant variant() {
        return variant;
    }

    public GTMaterial stoneMaterial() {
        return stoneMaterial;
    }

    public String registryId() {
        return stoneType.registryId() + "_" + variant.registrySuffix() + "_slab";
    }

    public String modelId() {
        return "block/stones/" + stoneType.registryId() + "_" + variant.registrySuffix() + "_slab";
    }

    /** Full-block model used for inventory / hand display. */
    public String fullBlockModelId() {
        return "block/stones/" + stoneType.registryId() + "_" + variant.registrySuffix();
    }

    public boolean matchesFullBlock(GTStoneBlock block) {
        return block.stoneType() == stoneType && block.variant() == variant;
    }

    @Override
    public String getDescriptionId() {
        return "block." + GregTech.NAMESPACE + ".stone_slab";
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable(getDescriptionId(), MaterialPresentation.name(stoneMaterial), variant.displayName());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getNearestLookingDirection().getOpposite();
        return GTWaterloggable.getStateForPlacement(
                defaultBlockState().setValue(FACING, facing.getAxis() == Direction.Axis.Y ? facing : facing.getOpposite()), ctx);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        boolean silkTouch = tool != null && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0;
        if (variant == StoneVariant.STONE && !silkTouch) {
            Block cobble = GTBlocks.getStoneSlab(stoneType, StoneVariant.COBBLE);
            if (cobble != null) {
                return List.of(new ItemStack(cobble));
            }
        }
        return List.of(new ItemStack(this));
    }



    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        if (adjacent.getBlock() instanceof GTStoneSlabBlock other
                && state.getValue(FACING) == adjacent.getValue(FACING)
                && other.stoneType == stoneType
                && other.variant == variant) {
            return direction == state.getValue(FACING);
        }
        return super.skipRendering(state, adjacent, direction);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof GTStoneSlabBlockItem slabItem)) {
            return InteractionResult.PASS;
        }
        if (!slabItem.getBlock().equals(this)) {
            return InteractionResult.PASS;
        }
        Direction clicked = hit.getDirection();
        if (clicked != state.getValue(FACING).getOpposite()) {
            return InteractionResult.PASS;
        }
        Block full = GTBlocks.getStone(stoneType, variant);
        if (full == null) {
            return InteractionResult.PASS;
        }
        if (!level.isUnobstructed(full.defaultBlockState(), pos, CollisionContext.of(player))) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, full.defaultBlockState());
            level.playSound(null, pos, full.defaultBlockState().getSoundType(level, pos, player).getPlaceSound(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
