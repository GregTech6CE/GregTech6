package com.gregtech.gregtech.content.multiblock;
import com.gregtech.gregtech.api.multiblock.StructureGrid;import java.util.*;
/** Original coke/cryo shell geometry, preserving unconstrained internal cells. */
public final class SharedHollowControllerGeometry {private SharedHollowControllerGeometry(){}public static List<StructureGrid.Position> cells(int radius,int minY,int maxY){var cells=new ArrayList<StructureGrid.Position>();for(int x=-radius;x<=radius;x++)for(int y=minY;y<=maxY;y++)for(int z=-radius;z<=radius;z++){if(Math.abs(x)<radius&&Math.abs(z)<radius&&y>minY&&y<maxY)continue;if(x!=0||y!=0||z+1!=0)cells.add(new StructureGrid.Position(x,y,z+1));}return List.copyOf(cells);}}
