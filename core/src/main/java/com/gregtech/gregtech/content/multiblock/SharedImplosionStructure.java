package com.gregtech.gregtech.content.multiblock;
import com.gregtech.gregtech.api.multiblock.StructureGrid;import java.util.*;
/** Original 25-wall hollow structure; one air cell and controller omitted. */
public final class SharedImplosionStructure {private SharedImplosionStructure(){}public static final StructureGrid LAYOUT=create();private static StructureGrid create(){var cells=new ArrayList<StructureGrid.Cell>();for(int x=-1;x<=1;x++)for(int y=0;y<=2;y++)for(int z=0;z<=2;z++){if(x==0&&y==0&&z==0)continue;cells.add(new StructureGrid.Cell(x,y,z,x==0&&y==1&&z==1?StructureGrid.Role.AIR:StructureGrid.Role.ITEM_IO));}return new StructureGrid(cells);}}
