package com.gregtech.gregtech.content.cover;
import net.minecraft.core.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
public final class CoverWorldInteraction {
    private CoverWorldInteraction(){}
    public static void walk(Level level,BlockPos pos,Entity entity){
        if(!(level.getBlockEntity(pos) instanceof PanelCoverHost host)||!ComponentCoverFallback.ownClass(host.coverOwner()))return;
        var id=CoverItems.behavior(host.getCover(Direction.UP));
        if(CoverUtilityBehaviors.ASPHALT_PANEL.equals(id))CoverUtilityBehaviors.walkOverAsphalt(entity);
        if(CoverAttachmentBehaviors.DRAIN.equals(id)&&!host.panels().stopped())CoverAttachmentBehaviors.walkOverDrain(level,pos,host.componentFluids(Direction.UP),entity);
    }
    /** Original ICover.BOXES_COVERS occupies two pixels inside the block boundary; art stays flush. */
    public static VoxelShape collision(BlockGetter level,BlockPos pos,VoxelShape shape){
        if(!(level.getBlockEntity(pos) instanceof PanelCoverHost host)||!ComponentCoverFallback.ownClass(host.coverOwner()))return shape;
        for(var side:Direction.values()){
            var stack=host.getCover(side);
            if(stack.isEmpty()&&host.coverOwner() instanceof com.gregtech.gregtech.content.logistics.LogisticsCoverHost logistics)stack=logistics.logisticsCovers().get(side);
            if(stack.isEmpty())continue;
            var id=CoverItems.behavior(stack);
            boolean torch=CoverItems.REDSTONE_TORCH.equals(id)||CoverItems.REDSTONE_REPEATER.equals(id);
            double minX=torch?7/16.0:0,minY=minX,minZ=minX,maxX=torch?9/16.0:1,maxY=maxX,maxZ=maxX;
            double inner=torch?.5:.125;
            switch(side){case DOWN->{minY=0;maxY=inner;}case UP->{minY=1-inner;maxY=1;}case NORTH->{minZ=0;maxZ=inner;}case SOUTH->{minZ=1-inner;maxZ=1;}case WEST->{minX=0;maxX=inner;}case EAST->{minX=1-inner;maxX=1;}}
            shape=Shapes.or(shape,Shapes.box(minX,minY,minZ,maxX,maxY,maxZ));
        }
        return shape;
    }
}
