package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.multiblock.StructureGrid;
import java.util.ArrayList;

/** GT6 MultiTileEntityLargeTurbine: front-centered, solid 3x3x4 housing. */
public final class SharedTurbineStructure {
    private SharedTurbineStructure() {}
    public static final StructureGrid LAYOUT=create();
    public static StructureGrid.Role role(int right,int up,int back) {
        return StructureGrid.axialRole(0,right,up,back,true);
    }
    private static StructureGrid create(){
        var cells=new ArrayList<StructureGrid.Cell>();
        for(int back=0;back<4;back++)for(int up=-1;up<=1;up++)for(int right=-1;right<=1;right++)
            if(back!=0||up!=0||right!=0)cells.add(new StructureGrid.Cell(right,up,back,role(right,up,back)));
        return new StructureGrid(cells);
    }
}
