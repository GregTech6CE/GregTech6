package com.gregtech.gregtech.block.tool;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.block.machine.MachineRotationType;
import com.gregtech.gregtech.blockentity.tool.AdvancedButtonBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** GT6's six-face advanced button with pulse, switch and indicator-lamp modes. */
public final class AdvancedButtonBlock extends DirectionalBlock implements EntityBlock, ToolInteractionTarget {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    @Override public com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    @Override public net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME,CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME;case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override public InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
    public AdvancedButtonBlock(Properties properties) {
        super(properties.noOcclusion().noCollission());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH)
                .setValue(ACTIVE, false).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof AdvancedButtonBlockEntity button) {
            CompoundTag config = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getCompound("GT6Button");
            if (config != null) button.loadItemConfig(config);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // FACING points away from the support: GT6 ButtonAdvanced:264,278-279.
        return switch (state.getValue(FACING)) {
            case NORTH -> box(4, 4, 14, 12, 12, 16);
            case EAST -> box(0, 4, 4, 2, 12, 12);
            case SOUTH -> box(4, 4, 0, 12, 12, 2);
            case WEST -> box(14, 4, 4, 16, 12, 12);
            case UP -> box(4, 0, 4, 12, 2, 12);
            case DOWN -> box(4, 14, 4, 12, 16, 12);
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedButtonBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        return !level.isClientSide && type == GTBlockEntities.ADVANCED_BUTTON.get()
                ? (world, pos, blockState, entity) -> ((AdvancedButtonBlockEntity) entity).serverTick() : null;
    }

    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool) && !GTToolHelper.isMonkeyWrench(tool)
                ? ToolInteractionSpec.facing(FACING, MachineRotationType.ALL) : null;
    }
    public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        boolean screwdriver = GTToolHelper.isScrewdriver(held);
        boolean cutter = GTToolHelper.isWireCutter(held);
        boolean hammer = GTToolHelper.isSoftHammer(held);
        boolean monkey = GTToolHelper.isMonkeyWrench(held);
        boolean magnifier = GTToolHelper.isMagnifyingGlass(held);
        if (!(level.getBlockEntity(pos) instanceof AdvancedButtonBlockEntity button)) return InteractionResult.PASS;
        if (screwdriver || cutter || hammer || monkey || magnifier) {
            if (!level.isClientSide && player.mayBuild() && level.mayInteract(player, pos)) {
                if (screwdriver) button.adjustLength(player.isShiftKeyDown() ? 20 : 1);
                else if (cutter) button.adjustStrength(player.isShiftKeyDown() ? -1 : 1);
                else if (hammer) button.toggleInversion(player.isShiftKeyDown());
                else if (monkey) button.cycleMode(player.isShiftKeyDown());
                player.displayClientMessage(button.status(), true);
                if (!magnifier) GTToolHelper.damageForToolClickReturn(held,
                        hammer || monkey ? 1000 : 100, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (ToolInteractions.use(state, level, pos, player, hand, hit))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (!level.isClientSide && button.press()) {
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.5F);
        }
        return button.isLampMode() ? InteractionResult.PASS : InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, fromPos, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AdvancedButtonBlockEntity button)
            button.updateLampInput();
    }

    @Override
    public boolean isSignalSource(BlockState state) { return true; }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof AdvancedButtonBlockEntity button && !button.isLampMode()
                && state.getValue(ACTIVE) ? button.strength() : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction == state.getValue(FACING) ? getSignal(state, level, pos, direction) : 0;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack item = new ItemStack(this);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof AdvancedButtonBlockEntity button)
            {var data=new CompoundTag();data.put("GT6Button",button.saveItemConfig());item.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));}
        return List.of(item);
    }

    @Override
    public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        for (String operation : new String[]{"screwdriver", "cutter", "monkey_wrench",
                "soft_hammer", "magnifying_glass", "sneak"})
            tooltip.add(Component.translatable("tooltip.gregtech.advanced_button." + operation));
    }
}
