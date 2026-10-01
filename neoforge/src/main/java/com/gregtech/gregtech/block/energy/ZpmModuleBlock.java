package com.gregtech.gregtech.block.energy;
import com.gregtech.gregtech.content.energy.ZpmEnergy;
import com.gregtech.gregtech.blockentity.energy.ZpmModuleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.item.*;
public final class ZpmModuleBlock extends Block implements EntityBlock {
    public static final IntegerProperty CHARGE=IntegerProperty.create("charge",0,15);
    private static final VoxelShape SHAPE=Shapes.or(Block.box(4,0,4,12,4,12),Block.box(5,4,5,11,12,11));
    @Override public com.mojang.serialization.MapCodec<? extends Block> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    public ZpmModuleBlock(Properties properties){super(properties.noOcclusion().lightLevel(s->s.getValue(CHARGE)));registerDefaultState(defaultBlockState().setValue(CHARGE,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(CHARGE);}
    @Override public VoxelShape getShape(BlockState s,BlockGetter w,BlockPos p,CollisionContext c){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ZpmModuleBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:(w,p,state,e)->{if(e instanceof ZpmModuleBlockEntity z)z.updateLight();};}
    @Override public java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        var stack=new ItemStack(this);
        if(builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof ZpmModuleBlockEntity z)ZpmEnergy.set(stack,z.energy());
        return java.util.List.of(stack);
    }
    @Override public void appendHoverText(ItemStack s,net.minecraft.world.item.Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag f){
        lines.add(net.minecraft.network.chat.Component.translatable("gregtech.zpm.charge",ZpmEnergy.stored(s),ZpmEnergy.CAPACITY));
        lines.add(net.minecraft.network.chat.Component.translatable("gregtech.zpm.non_rechargeable"));
    }
}
