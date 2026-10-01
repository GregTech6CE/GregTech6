package com.gregtech.gregtech.content.multiblock;
import com.gregtech.gregtech.api.multiblock.*;import net.minecraft.core.*;
/** Six-axis Minecraft coordinate boundary over shared source geometry/port roles. */
public final class AxialStructureTransform {private AxialStructureTransform(){}
 public static BlockPos at(BlockPos origin,Direction front,int right,int up,int back){var p=StructureGrid.offset(front.getStepX(),front.getStepY(),front.getStepZ(),right,up,back);return origin.offset(p.x(),p.y(),p.z());}
 public static boolean isLowestLayer(Direction front,int up,int back){return StructureGrid.lowest(front.getStepY(),up,back);}
 public static MultiblockLayout.Role fluidRole(Direction front,int up,int back){return MultiblockLayout.Role.valueOf(StructureGrid.fluidRole(front.getStepY(),up,back).name());}
 public static MultiblockLayout.Role role(Direction front,int right,int up,int back,boolean fluids){return MultiblockLayout.Role.valueOf(StructureGrid.axialRole(front.getStepY(),right,up,back,fluids).name());}
}
