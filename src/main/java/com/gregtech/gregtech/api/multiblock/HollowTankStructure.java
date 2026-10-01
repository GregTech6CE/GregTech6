package com.gregtech.gregtech.api.multiblock;
import net.minecraft.core.*;import java.util.function.Predicate;
/** Exact original hollow-cube rules, with platform loaded/block predicates. */
public final class HollowTankStructure {private HollowTankStructure(){}
 public static boolean validate(BlockPos controller,Direction facing,int size,Predicate<BlockPos> loaded,Predicate<BlockPos> wall,Predicate<BlockPos> air){return StructureGrid.hollowTank(controller.getX(),controller.getY(),controller.getZ(),facing.getStepX(),facing.getStepY(),facing.getStepZ(),size,p->loaded.test(new BlockPos(p.x(),p.y(),p.z())),p->wall.test(new BlockPos(p.x(),p.y(),p.z())),p->air.test(new BlockPos(p.x(),p.y(),p.z())));}
}
