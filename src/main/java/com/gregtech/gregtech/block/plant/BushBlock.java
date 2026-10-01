package com.gregtech.gregtech.block.plant;

import com.gregtech.gregtech.blockentity.BushBlockEntity;
import com.gregtech.gregtech.content.plant.GTBerryBushes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * GT6's berry bush ({@code MultiTileEntityBush}, multi-tile 32759).
 *
 * <p>GT6's bush is a small plant block that grows a chosen berry type over four stages: the
 * {@link BushBlockEntity} advances one stage per 256 growth increments, GT6's rain and light bonuses
 * included. Right-clicking a ripe bush hands out 1-2 berries and resets the stage, and right-clicking
 * a bush that has no berry yet with a GT6 berry sets its type.
 *
 * <p>The block's {@code stage} property drives both the model and the tint:
 * {@link GTBerryBushes#stageColour} gives GT6's exact colour per stage, applied by the client colour
 * handler.
 */
public class BushBlock extends Block implements EntityBlock {
    /** GT6's {@code mStage}: 0 = bare bush, 1 = bloom, 2 = immature berries, 3 = ripe. */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);

    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
    private static final VoxelShape COLLISION = box(2.0, 0.0, 2.0, 14.0, 2.0, 14.0);

    public BushBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    /** GT6 {@code canPlace}: the bush needs the ground below to be plantable greens. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isPlantableGround(level.getBlockState(pos.below()));
    }

    /** GT6 {@code BlocksGT.plantableGreens}: grass/dirt-like ground the bush grows on. */
    public static boolean isPlantableGround(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BushBlockEntity(pos, state);
    }

    /** GT6 {@code MultiTileEntityBush.writeItemNBT2} carries the chosen berry, not its growth. */
    private ItemStack packed(@Nullable BlockEntity entity) {
        ItemStack stack = new ItemStack(this);
        if (entity instanceof BushBlockEntity bush && !bush.berryId().isEmpty()) {
            CompoundTag data = new CompoundTag();
            data.putString("berry", bush.berryId());
            stack.addTagElement("BlockEntityTag", data);
        }
        return stack;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(packed(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return packed(level.getBlockEntity(pos));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        CompoundTag data = stack.getTagElement("BlockEntityTag");
        if (data == null || !data.contains("berry", Tag.TAG_STRING)) return;
        String berryId = data.getString("berry");
        if (GTBerryBushes.byId(berryId) == null) return;
        if (level.getBlockEntity(pos) instanceof BushBlockEntity bush) bush.setBerry(berryId);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> {
            if (be instanceof BushBlockEntity bush) bush.tick();
        };
    }

    /** GT6 {@code onBlockActivated3}: harvest when ripe, otherwise take a berry as the bush's type. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BushBlockEntity bush)) return InteractionResult.PASS;
        if (!bush.berryId().isEmpty()) {
            if (state.getValue(STAGE) < 3) return InteractionResult.PASS;
            int count = 1 + level.random.nextInt(2); // GT6: ST.amount(1+rng(2), mBerry)
            ItemStack berries = bush.berryStack(count);
            if (!berries.isEmpty()) {
                if (!player.addItem(berries)) player.drop(berries, false);
                level.setBlock(pos, state.setValue(STAGE, 0), 3);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
        // A berryless bush adopts the berry the player is holding (GT6 accepts any plantGtBerry).
        BerryLookup lookup = BerryLookup.current(player.getItemInHand(hand));
        if (lookup == null) return InteractionResult.PASS;
        bush.setBerry(lookup.id());
        return InteractionResult.CONSUME;
    }

    /** Small holder so the interaction can report the berry id without exposing the table type. */
    private record BerryLookup(String id) {
        static BerryLookup current(ItemStack stack) {
            GTBerryBushes.BerryType type = GTBerryBushes.of(stack);
            return type == null ? null : new BerryLookup(type.id());
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(level, pos, state, player);
    }

    /** GT6's bush faces the ground it stands on; exposed for the block entity's growth check. */
    public static Direction groundSide() {
        return Direction.UP;
    }
}
