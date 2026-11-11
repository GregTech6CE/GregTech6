package gregtech.content.block.tool;

import gregtech.content.blockentity.tool.DustFunnelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/** GT6 Dust Funnel: combines dust sizes and sends the selected form downward. */
public class DustFunnelBlock extends Block implements EntityBlock {

    /** GT6 output forms in toggle order: blockDust, dust, dustSmall, dustTiny, dustDiv72. */
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 4);
    private static final String MODE_TAG = "gt.mode";

    // GT6 bounds: full top half plus four corner legs; the spout narrows one pixel per mode.
    private static final VoxelShape[] SHAPES = new VoxelShape[5];
    static {
        for (int mode = 0; mode < SHAPES.length; mode++) {
            int inset = 2 + mode;
            SHAPES[mode] = Shapes.or(
                    Block.box(0, 8, 0, 16, 16, 16),
                    Block.box(inset, 0, inset, 16 - inset, 8, 16 - inset),
                    Block.box(0, 0, 0, 2, 8, 2),
                    Block.box(0, 0, 14, 2, 8, 16),
                    Block.box(14, 0, 0, 16, 8, 2),
                    Block.box(14, 0, 14, 16, 8, 16));
        }
    }

//? if >=1.21.1 {
    /*@Override public net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME,CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME;case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override public InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
*///?} else {
//?}
    public DustFunnelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(MODE, itemMode(context.getItemInHand()));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(MODE)];
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DustFunnelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != gregtech.content.registry.GTBlockEntities.DUST_FUNNEL.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<DustFunnelBlockEntity>) DustFunnelBlockEntity::serverTick;
    }
//? if >=1.21.1 {
    /*public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
*///?} else {

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
//?}
                                 InteractionHand hand, BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof DustFunnelBlockEntity funnel)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (gregtech.engine.tool.GTToolHelper.isMonkeyWrench(held)) {
                funnel.cycleMode(player.isShiftKeyDown());
                player.displayClientMessage(funnel.modeIcon().getHoverName(), true);
                gregtech.engine.tool.GTToolHelper.damageForUse(held, 1, player);
            } else if (gregtech.engine.tool.GTToolHelper.matchesTool(held, gregtech.engine.tool.GTToolType.PINCERS)) {
                ItemStack out = funnel.retrieveFraction();
                if (!out.isEmpty() && !player.addItem(out)) player.drop(out, false);
                gregtech.engine.tool.GTToolHelper.damageForToolClickReturn(held, 10, player);
            } else if (player.isShiftKeyDown() && held.isEmpty()) {
                ItemStack out = funnel.retrieve();
                if (!out.isEmpty() && !player.addItem(out)) player.drop(out, false);
            } else if (!held.isEmpty() && hit.getDirection() == net.minecraft.core.Direction.UP) {
                int taken = funnel.insert(held);
                if (taken > 0 && !player.getAbilities().instabuild) held.shrink(taken);
                else if (taken == 0) player.displayClientMessage(
                        Component.translatable("message.gt6ce.dust_funnel.only_dusts"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof DustFunnelBlockEntity funnel) {
            funnel.dropContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // GT6 writeItemNBT2 keeps the selected size in the dropped item so a layout can be rebuilt.
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(withMode(asItem(), state.getValue(MODE)));
    }

    @Override
//? if >=1.21.1 {
    /*public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult hit,
                                      net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
*///?} else {
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
//?}
        return withMode(asItem(), state.getValue(MODE));
    }

    private static int itemMode(ItemStack stack) {
        CompoundTag data = blockEntityTag(stack);
        return data == null ? 0 : Math.floorMod(data.getInt(MODE_TAG), 5);
    }

    private static ItemStack withMode(Item item, int mode) {
        ItemStack stack = new ItemStack(item);
        // Mode 0 is the item's default; leaving it untagged keeps recovered funnels
        // stackable with crafted ones.
        if (mode == 0) return stack;
        CompoundTag data = new CompoundTag();
        data.putInt(MODE_TAG, Math.floorMod(mode, 5));
//? if >=1.21.1 {
        /*stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.CustomData.of(data));
*///?} else {
        stack.addTagElement("BlockEntityTag", data);
//?}
        return stack;
    }

    @Nullable
    private static CompoundTag blockEntityTag(ItemStack stack) {
//? if >=1.21.1 {
        /*var data = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTag();
*///?} else {
        return stack.getTagElement("BlockEntityTag");
//?}
    }

    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
*///?} else {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
//?}
        tooltip.add(Component.translatable("gt.tooltip.dust_funnel.1").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gt.lang.nogui.rightclick.interact").append(" (")
                .append(Component.translatable("gt.lang.face.top")).append(")").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gt.lang.use.pincers.to.take").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("gt.lang.use.monkey.wrench.to.toggle.outputs").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
