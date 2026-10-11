package com.gregtech.gregtech.content.cover;
import net.minecraft.core.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
public final class CoverWorldInteraction {
    private CoverWorldInteraction(){}
    /** Whether the underlying host already supplies a full face at the block boundary. */
    public static boolean isCoverSurface(net.minecraft.world.level.block.entity.BlockEntity owner, Direction side) {
        var level=owner.getLevel();
        return level!=null && net.minecraft.world.level.block.Block.isFaceFull(
                owner.getBlockState().getShape(level,owner.getBlockPos(),CollisionContext.empty()),side);
    }
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
            var box=torch?CoverPlateGeometry.holder(side.ordinal()):CoverPlateGeometry.plate(side.ordinal());
            shape=Shapes.or(shape,Shapes.box(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ()));
        }
        return shape;
    }
}
