package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import java.util.ArrayList;
import static com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.*;

/** GT6: nine heat transmitters under a hollow 3x3x3 boiler, controller at front-bottom centre. */
public final class BoilerStructure {
    public static final MultiblockLayout LAYOUT=create();
    private BoilerStructure() {}
    private static MultiblockLayout create() {
        var cells=new ArrayList<MultiblockLayout.Cell>();
        for(int x=-1;x<=1;x++) for(int z=0;z<=2;z++) for(int y=-1;y<=2;y++) {
            var role=y==-1?HEAT_INPUT:y==0?FLUID_INPUT:x==0&&z==1&&y==1?AIR:FLUID_OUTPUT;
            cells.add(new MultiblockLayout.Cell(x,y,z,role));
        }
        return new MultiblockLayout(cells);
    }
}
