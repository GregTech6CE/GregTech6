package com.gregtech.gregtech.content.cover;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
/** Cover placement and tools share the native connector connection bits. */
public final class CoverConnections {
    private CoverConnections(){}
    public static boolean blocks(PanelCoverHost host,Direction side){
        var cover=host.getCover(side);if(cover.isEmpty())return false;var id=CoverItems.behavior(cover);
        if(host.panels().shuttered(side))return true;
        if(CoverItems.REDSTONE_TORCH.equals(id)||CoverItems.REDSTONE_REPEATER.equals(id))return true;
        var owner=host.coverOwner();var level=owner.getLevel();
        return CoverUtilityBehaviors.FILTER_ITEM.equals(id)&&owner instanceof com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity
                &&level!=null&&level.getBlockEntity(owner.getBlockPos().relative(side)) instanceof com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity;
    }
    public static boolean canConnect(Level level,BlockPos pos,Direction side){
        return !(level.getBlockEntity(pos) instanceof PanelCoverHost host&&blocks(host,side))
                &&!(level.getBlockEntity(pos.relative(side)) instanceof PanelCoverHost other&&blocks(other,side.getOpposite()));
    }
    private static net.minecraft.world.level.block.state.properties.BooleanProperty property(BlockState state,Direction side){
        String name=side.getName();
        for(var p:state.getProperties())if(p instanceof net.minecraft.world.level.block.state.properties.BooleanProperty bit&&p.getName().equals(name))return bit;
        return null;
    }
    public static void update(PanelCoverHost host,Direction side,boolean connected){
        var owner=host.coverOwner();var level=owner.getLevel();if(level==null||level.isClientSide)return;
        var state=owner.getBlockState();var bit=property(state,side);if(bit==null)return;
        if(state.getValue(bit)!=connected)level.setBlockAndUpdate(owner.getBlockPos(),state.setValue(bit,connected));
        var adjacent=owner.getBlockPos().relative(side);if(!level.hasChunkAt(adjacent))return;
        var other=level.getBlockState(adjacent);var opposite=property(other,side.getOpposite());
        if(opposite!=null&&other.getBlock().getClass()==state.getBlock().getClass()&&other.getValue(opposite)!=connected)
            level.setBlockAndUpdate(adjacent,other.setValue(opposite,connected));
    }
    public static void attached(PanelCoverHost host,Direction side){
        var id=CoverItems.behavior(host.getCover(side));
        if(PanelCover.of(host.getCover(side))==PanelCover.SHUTTER)update(host,side,!host.panels().shuttered(side));
        else if(CoverItems.REDSTONE_TORCH.equals(id)||CoverItems.REDSTONE_REPEATER.equals(id)||CoverUtilityBehaviors.FILTER_ITEM.equals(id)&&blocks(host,side))update(host,side,false);
    }
}
