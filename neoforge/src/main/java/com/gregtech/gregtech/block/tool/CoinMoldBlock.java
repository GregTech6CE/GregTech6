package com.gregtech.gregtech.block.tool;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.blockentity.tool.CoinMoldBlockEntity;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** GT6 coinage mold: one tiny plate in, top-face hard-hammer strike, one metal coin out. */
public final class CoinMoldBlock extends ShapedToolBlock implements EntityBlock {
    public CoinMoldBlock(Properties properties) {
        super("coin_mold", properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoinMoldBlockEntity(pos, state);
    }

    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return null; // The original mold is not a facing machine.
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0,0,0,16,12,16);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, 12, 16);
    }

    @Override
    public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CoinMoldBlockEntity mold)) return InteractionResult.PASS;
        if (!player.mayBuild() || !level.mayInteract(player,pos)) return InteractionResult.FAIL;
        ItemStack held = player.getItemInHand(hand);
        if (GTToolHelper.matchesTool(held, GTToolType.HARD_HAMMER)) {
            if (hit.getDirection() != Direction.UP) return InteractionResult.PASS;
            if (!level.isClientSide && mold.strike()) {
                GTToolHelper.damageForToolClickReturn(held, 2000, player);
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.2F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hit.getDirection() == Direction.DOWN) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (mold.insertPlate(held, player.getAbilities().instabuild)) {
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, .5F, 1.0F);
            return InteractionResult.CONSUME;
        }
        ItemStack coin = mold.contents();
        if (!coin.isEmpty() && player.getInventory().add(coin.copy())) {
            mold.takeContents();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        CompoundTag config = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getCompound("GT6CoinMold");
        if (config != null && level.getBlockEntity(pos) instanceof CoinMoldBlockEntity mold)
            mold.loadItemConfig(config);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CoinMoldBlockEntity mold)
            mold.dropContents();
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof CoinMoldBlockEntity mold)
            mold.dropContents();
        super.onRemove(state, level, pos, replacement, moved);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack item = new ItemStack(this);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CoinMoldBlockEntity mold)
            {var data=new CompoundTag();data.put("GT6CoinMold",mold.saveItemConfig());item.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));}
        return List.of(item);
    }

    @Override
    public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gregtech.coin_mold.1"));
        tooltip.add(Component.translatable("tooltip.gregtech.coin_mold.2"));
    }
}
