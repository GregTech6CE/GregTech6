package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.multiblock.StructureGrid;
import java.util.ArrayList;
import static com.gregtech.gregtech.api.multiblock.StructureGrid.Role.*;

/** GT6: nine heat transmitters under a hollow 3x3x3 boiler, controller at front-bottom centre. */
public final class SharedBoilerStructure {
    public static final StructureGrid LAYOUT=create();
    private SharedBoilerStructure() {}
    private static StructureGrid create() {
        var cells=new ArrayList<StructureGrid.Cell>();
        for(int x=-1;x<=1;x++) for(int z=0;z<=2;z++) for(int y=-1;y<=2;y++) {
            var role=y==-1?HEAT_INPUT:y==0?FLUID_INPUT:x==0&&z==1&&y==1?AIR:FLUID_OUTPUT;
            cells.add(new StructureGrid.Cell(x,y,z,role));
        }
        return new StructureGrid(cells);
    }
}
