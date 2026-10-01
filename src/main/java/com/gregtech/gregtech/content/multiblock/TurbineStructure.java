package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import java.util.ArrayList;

/** GT6 MultiTileEntityLargeTurbine: front-centered, solid 3x3x4 housing. */
public final class TurbineStructure {
    private TurbineStructure() {}
    public static final MultiblockLayout LAYOUT=create();
    public static MultiblockLayout.Role role(int right,int up,int back) {
        return AxialStructureTransform.role(net.minecraft.core.Direction.NORTH,right,up,back,true);
    }
    private static MultiblockLayout create(){
        var cells=new ArrayList<MultiblockLayout.Cell>();
        for(int back=0;back<4;back++)for(int up=-1;up<=1;up++)for(int right=-1;right<=1;right++)
            if(back!=0||up!=0||right!=0)cells.add(new MultiblockLayout.Cell(right,up,back,role(right,up,back)));
        return new MultiblockLayout(cells);
    }
}
