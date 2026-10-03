package com.gregtech.gregtech.block.inventory;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;
import java.util.List;

/** GT6 metal chest (MultiTileEntityChest): 54 slots, material-tinted chest shell. */
public class MetalChestBlock extends HorizontalDirectionalBlock implements EntityBlock, SimpleWaterloggedBlock, ToolInteractionTarget {

    // GT6: 1px inset, 14px tall (0.0625..0.9375 / 0..0.875)
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

    private final GTMaterial material;
    public enum Shell {
        METAL("metalchest"), REINFORCED_WOOD("woodchest"), LOOT("lootchest");
        private final String texture;
        Shell(String texture) { this.texture = texture; }
        public String texture() { return texture; }
    }
    private final Shell shell;

    @Override protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    public MetalChestBlock(GTMaterial material, Properties properties) {
        this(material, Shell.METAL, properties);
    }

    public MetalChestBlock(GTMaterial material, Shell shell, Properties properties) {
        super(properties);
        this.material = material;
        this.shell = shell;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(GTWaterloggable.WATERLOGGED, false));
    }

    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.facing(FACING, com.gregtech.gregtech.block.machine.MachineRotationType.HORIZONTAL) : null;
    }

    public GTMaterial material() { return material; }
    public Shell shell() { return shell; }

    @Override public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return shell == Shell.REINFORCED_WOOD && !material.has(com.gregtech.gregtech.api.material.MaterialProperty.UNBURNABLE) ? 100 : 0;
    }

    @Override public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return getFlammability(state, world, pos, face);
    }

    @Override public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }
    @Override public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int value) {
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.triggerEvent(id, value);
    }
    @Override public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof MetalChestBlockEntity chest) chest.recheckOpeners();
    }
    @Override @SuppressWarnings("unchecked")
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (type != com.gregtech.gregtech.registry.GTBlockEntities.METAL_CHEST.get()) return null;
        if (level.isClientSide) {
            return (world, pos, blockState, be) ->
                    MetalChestBlockEntity.clientTick(world, pos, blockState, (MetalChestBlockEntity) be);
        }
        // MultiTileEntityChest.onBlockActivated2 generates loot when opening, never on placement.
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState base = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        return GTWaterloggable.getStateForPlacement(base, ctx);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MetalChestBlockEntity(pos, state);
    }

    @Override public net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME,CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME;case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override public InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
    public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (ToolInteractions.use(state, level, pos, player, hand, hit))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (!(level.getBlockEntity(pos) instanceof MetalChestBlockEntity chest)) return InteractionResult.PASS;
        if (level.getBlockState(pos.above()).isRedstoneConductor(level, pos.above())) return InteractionResult.sidedSuccess(level.isClientSide);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            sp.openMenu(chest);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof MetalChestBlockEntity chest) {
            chest.generateLootIfNeeded();
            var inv = chest.inventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                inv.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        ItemStack result = new ItemStack(this);
        if (builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY)
                instanceof MetalChestBlockEntity chest) {
            chest.generateLootIfNeeded();
            if (chest.lootGenerated()) {
                var data = new net.minecraft.nbt.CompoundTag();
                data.putBoolean("GTLootGenerated", true);
                data.putString("id","gregtech:metal_chest");
                result.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data));
            }
        }
        return java.util.List.of(result);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.metal_chest.1")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
