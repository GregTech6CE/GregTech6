package gregtech.content.block.machine;

import gregtech.content.blockentity.machine.VonDaGraaggControllerBlockEntity;
import gregtech.content.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/** The original GT6 17996 spawn inhibitor controller, not a passive structure port. */
public final class VonDaGraaggControllerBlock extends Block implements EntityBlock {

    public VonDaGraaggControllerBlock(Properties properties) {
        super(properties);
    }

//? if >=1.21.1 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends Block> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }
*///?} else {
//?}
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VonDaGraaggControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide || type != GTBlockEntities.VON_DA_GRAAGG.get()) return null;
        return (tickLevel, tickPos, tickState, machine) -> VonDaGraaggControllerBlockEntity.serverTick(
                tickLevel, tickPos, tickState, (VonDaGraaggControllerBlockEntity) machine);
    }

//? if >=1.21.1 {
    /*@Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
*///?} else {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
//?}
            List<Component> tooltip, TooltipFlag flag) {
        gregtech.client.UtilityControllerTooltips.vonDaGraagg(tooltip);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder context) {
        return List.of(new ItemStack(this));
    }
}
