package gregtech.block.sensor;

import gregtech.blockentity.sensor.SensorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.Nullable;
import java.util.List;

/** GT6 sensor panel block (fluid/item/energy/progress meters). */
public class SensorBlock extends DirectionalBlock implements EntityBlock, gregtech.internal.tool.ToolInteractionTarget {

    @Nullable private final SensorBlockEntity.Kind kind;
    @Nullable private final gregtech.api.addon.platform.AddonSensor addon;

//? if >=1.21.1 {
    /*@Override public com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME,CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME;case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
*///?} else {
//?}
    public SensorBlock(SensorBlockEntity.Kind kind, Properties properties) {
        super(properties.noOcclusion());
        this.kind = kind;
        this.addon = null;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    public SensorBlock(gregtech.api.addon.platform.AddonSensor addon, Properties properties) {
        super(properties.noOcclusion());
        if (addon == null) throw new IllegalArgumentException("Missing addon sensor");
        this.kind = null;
        this.addon = addon;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    /** Null for an addon sensor. */
    @Nullable public SensorBlockEntity.Kind kind() { return kind; }

    /** Null for a built-in sensor. */
    @Nullable public gregtech.api.addon.platform.AddonSensor addon() { return addon; }

    public String kindName() { return addon != null ? addon.id() : kind.name(); }

    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        int[] b = gregtech.internal.sensor.SensorPanelGeometry.bounds(state.getValue(FACING));
        return Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
    }
    @Override public gregtech.internal.tool.ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return gregtech.internal.tool.GTToolHelper.matchesTool(tool, gregtech.internal.tool.GTToolType.WRENCH)
                ? gregtech.internal.tool.ToolInteractionSpec.facing(FACING, gregtech.block.machine.MachineRotationType.ALL) : null;
    }
//? if >=1.21.1 {
    /*public net.minecraft.world.InteractionResult interact(BlockState state, Level level, BlockPos pos,
*///?} else {
    @Override public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
//?}
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        var tool = player.getItemInHand(hand);
        if (gregtech.internal.tool.GTToolHelper.isMonkeyWrench(tool)) {
            var side = gregtech.internal.tool.ToolInteractions.selectedFace(hit);
            if (!player.mayBuild() || !level.mayInteract(player, pos)
                    || side == state.getValue(FACING)) return net.minecraft.world.InteractionResult.PASS;
            if (tool.getItem() instanceof gregtech.item.ElectricToolItem electric
                    && !electric.canInteract(tool)) return net.minecraft.world.InteractionResult.PASS;
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof SensorBlockEntity sensor) {
                sensor.setInputSide(side);
                gregtech.internal.tool.GTToolHelper.damageForToolClickReturn(tool, 10000, player);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (gregtech.internal.tool.ToolInteractions.use(state, level, pos, player, hand, hit))
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        if (hit.getDirection() != state.getValue(FACING) || !player.mayBuild() || !level.mayInteract(player,pos)) return net.minecraft.world.InteractionResult.PASS;
        var point = gregtech.internal.block.FaceCoordinates.pixels(hit.getDirection(),
                hit.getLocation().x-pos.getX(), hit.getLocation().y-pos.getY(), hit.getLocation().z-pos.getZ());
        var held = player.getItemInHand(hand);
        boolean screwdriver = gregtech.internal.tool.GTToolHelper.matchesTool(held, gregtech.internal.tool.GTToolType.SCREWDRIVER);
        boolean reset = gregtech.internal.tool.GTToolHelper.matchesTool(held, gregtech.internal.tool.GTToolType.SOFT_HAMMER);
        if (level.isClientSide) {
            // Predict the click so the panel redraws in the same frame; the server's block-entity
            // update then overwrites this state.
            if (level.getBlockEntity(pos) instanceof SensorBlockEntity sensor)
                sensor.control(point.x(), point.y(), screwdriver, reset);
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof SensorBlockEntity sensor)) return net.minecraft.world.InteractionResult.PASS;
        if (!sensor.control(point.x(), point.y(), screwdriver, reset)) return net.minecraft.world.InteractionResult.PASS;
        if (screwdriver || reset) gregtech.internal.tool.GTToolHelper.damageForUse(held,1,player);
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override public void toolStateChanged(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof SensorBlockEntity sensor) sensor.resetInputSide();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // display faces the clicked surface's outward normal (panel on the machine wall)
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SensorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != gregtech.registry.GTBlockEntities.SENSOR.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<SensorBlockEntity>) SensorBlockEntity::serverTick;
    }

    // ── Redstone: analog signal proportional to the measured fill ratio ─────

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof SensorBlockEntity sensor
                && direction != state.getValue(FACING).getOpposite()) {
            return sensor.signal();
        }
        return 0;
    }

    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,List<Component> tooltip, TooltipFlag flag) {
*///?} else {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
//?}
        gregtech.client.SensorTooltips.add(tooltip, this);
    }
}
