package com.gregtech.gregtech.block.inventory;

import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.api.inventory.MassStorageFace;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import java.util.ArrayList;
import java.util.List;

/** Original bulk-storage controls and harvest rules at the Neo block/components boundary. */
public class MassStorageBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final GTMaterial material;
    public MassStorageBlock(GTMaterial material, Properties properties) {
        super(properties); this.material = material;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(GTWaterloggable.WATERLOGGED, false));
    }
    public GTMaterial material() { return material; }
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING, GTWaterloggable.WATERLOGGED); }
    public BlockState getStateForPlacement(BlockPlaceContext context) { return GTWaterloggable.getStateForPlacement(defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()), context); }
    protected FluidState getFluidState(BlockState state) { return GTWaterloggable.getFluidState(state); }
    protected BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) { GTWaterloggable.updateShape(state, level, pos); return state; }
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MassStorageBlockEntity(pos, state); }
    private ItemStack shell(BlockState state, MassStorageBlockEntity storage) {
        ItemStack shell = new ItemStack(this); CompoundTag tag = new CompoundTag();
        if (storage.isPacked()) {
            tag = storage.getUpdateTag(storage.getLevel().registryAccess());
            tag.remove("gt.partial_units");
        } else if ((storage.mode() & 7) != 0) tag.putInt("gt.mode", storage.mode() & 7);
        if (!tag.isEmpty()) {
            tag.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(storage.getType()).toString());
            shell.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        }
        return shell;
    }
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(entity instanceof MassStorageBlockEntity storage)) return List.of(new ItemStack(this));
        var drops = new ArrayList<ItemStack>(); drops.add(shell(state, storage)); drops.addAll(storage.looseDrops()); return drops;
    }
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(state, level, pos, player, hand, hit) ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(state, level, pos, player, InteractionHand.MAIN_HAND, hit) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }
    private boolean interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.OFF_HAND || !(level.getBlockEntity(pos) instanceof MassStorageBlockEntity store)) return false;
        ItemStack held = player.getItemInHand(hand);
        if (store.isPacked() && (com.gregtech.gregtech.platform.neoforge.NeoToolBindings.matches(held,"scissors") || com.gregtech.gregtech.platform.neoforge.NeoToolBindings.matches(held,"knife"))) {
            if (!level.isClientSide && store.unseal()) com.gregtech.gregtech.api.tool.GTToolHelper.damageForToolClickReturn(held, 1000, player); return true;
        }
        if (store.isPacked()) return false;
        if (com.gregtech.gregtech.platform.neoforge.NeoToolBindings.isScrewdriver(held)) {
            if (!level.isClientSide) {
                store.toggleFilterReset();
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(store.resetsFilter()
                        ? "message.gregtech.storage.filter_reset" : "message.gregtech.storage.filter_keep"), true);
                com.gregtech.gregtech.api.tool.GTToolHelper.damageForUse(held, 1, player);
            }
            return true;
        }
        if (hit.getDirection() != state.getValue(FACING)) return false;
        double x = MassStorageFace.x(state.getValue(FACING), hit.getLocation().x-pos.getX(), hit.getLocation().z-pos.getZ());
        double y = 16*(1-(hit.getLocation().y-pos.getY()));
        if (!MassStorageFace.active(x,y)) return false;
        int requested = MassStorageFace.withdrawal(x,y);
        if (requested > 0 && store.stored() > 0) {
            if (!level.isClientSide) {
                Direction front = state.getValue(FACING); int left=requested;
                while(left>0) { ItemStack out=store.extractAmount(left); if(out.isEmpty())break; left-=out.getCount();
                    com.gregtech.gregtech.util.GTItemDrops.dropItemStack(level,pos.getX()+.5+front.getStepX(),pos.getY()+.5,pos.getZ()+.5+front.getStepZ(),out); }
            }
            return true;
        }
        if (!held.isEmpty()) { if (!level.isClientSide) { int count=store.insert(held); if(count>0&&!player.getAbilities().instabuild)held.shrink(count); } return true; }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.gregtech.mass_storage.status",store.stored(),store.template().isEmpty()?net.minecraft.network.chat.Component.translatable("gui.gregtech.empty"):store.template().getHoverName()),true); return true;
        }
        if (!level.isClientSide && !store.template().isEmpty() && MassStorageFace.depositInventory(x,y)) {
            for(var stack:player.getInventory().items) { int inserted=store.insert(stack); if(inserted>0&&!player.getAbilities().instabuild)stack.shrink(inserted); }
            player.getInventory().setChanged();
        }
        return true;
    }
    /** Source creative pick retains the complete state, including untaped inventory and fractions. */
    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult hit,
            net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        var stack = new ItemStack(state.getBlock());
        if (level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage) {
            var data = storage.getUpdateTag(level.registryAccess());
            data.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(storage.getType()).toString());
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(data));
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
            List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("gt.tooltip.mass_storage.1"));
        tooltip.add(net.minecraft.network.chat.Component.translatable("gt.tooltip.mass_storage.2"));
        tooltip.add(net.minecraft.network.chat.Component.translatable("gt.tooltip.mass_storage.3"));
        var component = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (component == null || context.registries() == null) return;
        var stored = component.copyTag();
        if (stored.contains("gt.template") && stored.getLong("gt.stored") > 0) {
            var template = ItemStack.parseOptional(context.registries(), stored.getCompound("gt.template"));
            if (!template.isEmpty()) tooltip.add(net.minecraft.network.chat.Component.translatable(
                    "message.gregtech.mass_storage.status", stored.getLong("gt.stored"), template.getHoverName()));
        }
    }
}
