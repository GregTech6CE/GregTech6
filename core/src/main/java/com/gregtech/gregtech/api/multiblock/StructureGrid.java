package com.gregtech.gregtech.api.multiblock;
import java.util.*;
/** Loader-free source coordinates and roles. Minecraft world/registry access is a boundary. */
public final class StructureGrid {
 public enum Role { AIR, CASING, LOGISTICS, HEAT_INPUT, CRUCIBLE, FLUID_INPUT, FLUID_OUTPUT, FLUID_IO, ITEM_IO, ITEM_FLUID_IO, ITEM_FLUID_INPUT, ITEM_FLUID_OUTPUT, ENERGY_INPUT, ITEM_FLUID_ENERGY_INPUT, ITEM_FLUID_ENERGY, ENERGY_OUTPUT }
 public record Position(int x,int y,int z){}public record Cell(int right,int up,int back,Role role){}
 private final List<Cell> cells;public StructureGrid(List<Cell> cells){var positions=new HashSet<Position>();for(var cell:cells)if(!positions.add(new Position(cell.right(),cell.up(),cell.back())))throw new IllegalArgumentException("Duplicate structure cell");this.cells=List.copyOf(cells);}public List<Cell> cells(){return cells;}
 public static Position offset(int fx,int fy,int fz,int right,int up,int back){return fy==0?new Position(-fz*right-fx*back,up,fx*right-fz*back):new Position(right,-fy*back,-up);}
 public static boolean lowest(int fy,int up,int back){return fy>0?back==3:fy<0?back==0:up==-1;}
 public static Role fluidRole(int fy,int up,int back){return back==0?(lowest(fy,up,back)?Role.FLUID_IO:Role.FLUID_INPUT):(lowest(fy,up,back)?Role.FLUID_OUTPUT:Role.CASING);}
 public static Role axialRole(int fy,int right,int up,int back,boolean fluids){return right==0&&up==0&&back==3?Role.ENERGY_OUTPUT:fluids?fluidRole(fy,up,back):Role.CASING;}
 public static boolean canAutoOutput(int faceY,boolean gas,int density){return faceY==0||gas||faceY==(density<0?1:-1);}
 public static boolean hollowTank(int cx,int cy,int cz,int fx,int fy,int fz,int size,java.util.function.Predicate<Position> loaded,java.util.function.Predicate<Position> wall,java.util.function.Predicate<Position> air){if(size!=3&&size!=5)throw new IllegalArgumentException("Tank size: "+size);int radius=size/2;int ox=cx-fx*radius,oy=cy-fy*radius,oz=cz-fz*radius;for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++){var p=new Position(ox+x,oy+y,oz+z);if(!loaded.test(p))return false;if(p.x()==cx&&p.y()==cy&&p.z()==cz)continue;boolean shell=Math.abs(x)==radius||Math.abs(y)==radius||Math.abs(z)==radius;if(!(shell?wall.test(p):air.test(p)))return false;}return true;}
}
