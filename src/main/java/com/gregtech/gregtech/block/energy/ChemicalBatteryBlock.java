package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.content.energy.ChemicalBatterySpec;
import com.gregtech.gregtech.blockentity.energy.ChemicalBatteryBlockEntity;
import com.gregtech.gregtech.item.ChemicalBatteryItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.List;

public final class ChemicalBatteryBlock extends Block implements EntityBlock {
    private final ChemicalBatterySpec spec;
    private final VoxelShape shape;
    public ChemicalBatteryBlock(ChemicalBatterySpec spec){
        super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(0.5f,3f).sound(SoundType.METAL).noOcclusion());
        this.spec=spec;shape=Block.box(spec.inset(),0,spec.inset(),16-spec.inset(),spec.height(),16-spec.inset());
    }
    public ChemicalBatterySpec spec(){return spec;}
    @Override public net.minecraft.world.InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,
            net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        var tool=player.getItemInHand(hand);
        if(!com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(tool,com.gregtech.gregtech.api.tool.GTToolType.MAGNIFYING_GLASS))
            return net.minecraft.world.InteractionResult.PASS;
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof ChemicalBatteryBlockEntity be) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.electric_tool.energy",be.stored(),spec.capacity()),true);
            com.gregtech.gregtech.api.tool.GTToolHelper.damageForToolClickReturn(tool,1,player,
                    hand==net.minecraft.world.InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return shape;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ChemicalBatteryBlockEntity(p,s);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,@javax.annotation.Nullable LivingEntity player,ItemStack stack){
        if(l.getBlockEntity(p) instanceof ChemicalBatteryBlockEntity be&&stack.getItem() instanceof ChemicalBatteryItem item)be.setCharge(item.stored(stack));
    }
    @Override public List<ItemStack> getDrops(BlockState s,LootParams.Builder loot){
        var result=super.getDrops(s,loot);
        if(loot.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ChemicalBatteryBlockEntity be)
            for(var drop:result)if(drop.is(asItem()))((ChemicalBatteryItem)drop.getItem()).setCharge(drop,be.stored());
        return result;
    }
    @Override public ItemStack getCloneItemStack(BlockState s,HitResult target,BlockGetter l,BlockPos p,Player player){
        return l.getBlockEntity(p) instanceof ChemicalBatteryBlockEntity be?be.asItem():new ItemStack(this);
    }
}
