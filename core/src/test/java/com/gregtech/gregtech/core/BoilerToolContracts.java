package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.multiblock.StructureGrid;
import com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerParameters;
import java.util.HashMap;

/** Source order and unchanged geometry across all four horizontal orientations. */
final class BoilerToolContracts {
    private static int assertions;
    static int verify() {
        assertions=0;
        var cells=OriginalLargeBoilerParameters.CHECK_ORDER;
        check(cells.size()==36,"Source boiler checks one air cell, nine heat cells and twenty-six wall/main cells");
        check(cells.get(0).role()==StructureGrid.Role.AIR,"Source checks the hollow centre before any placement");
        check(cells.get(1).equals(new OriginalLargeBoilerParameters.CheckCell(-1,-1,-1,StructureGrid.Role.HEAT_INPUT))
                &&cells.get(9).equals(new OriginalLargeBoilerParameters.CheckCell(1,-1,1,StructureGrid.Role.HEAT_INPUT)),
                "Source heat base is checked first from northwest to southeast");
        check(cells.get(19).equals(new OriginalLargeBoilerParameters.CheckCell(0,2,0,StructureGrid.Role.FLUID_OUTPUT))
                &&cells.get(20).equals(new OriginalLargeBoilerParameters.CheckCell(-1,1,-1,StructureGrid.Role.FLUID_OUTPUT)),
                "Source places the centre of the roof before either upper ring");
        for(int[] facing:new int[][]{{0,-1},{1,0},{0,1},{-1,0}}) {
            var prior=new HashMap<StructureGrid.Position,StructureGrid.Role>();
            for(var cell:OriginalLargeBoilerParameters.LAYOUT.cells())prior.put(
                    StructureGrid.offset(facing[0],0,facing[1],cell.right(),cell.up(),cell.back()),cell.role());
            var source=new HashMap<StructureGrid.Position,StructureGrid.Role>();
            for(var cell:cells)source.put(new StructureGrid.Position(cell.x()-facing[0],cell.y(),cell.z()-facing[1]),cell.role());
            check(source.size()==36&&source.equals(prior),"World-coordinate builder preserves the existing role map for front "+facing[0]+","+facing[1]);
        }
        check(OriginalLargeBoilerParameters.calcificationMessage(10000).equals("No Calcification in this Boiler"),"Source explicitly reports absence of scale");
        check(OriginalLargeBoilerParameters.calcificationMessage(9999).equals("Calcification: 0.01%")
                &&OriginalLargeBoilerParameters.calcificationMessage(9876).equals("Calcification: 1.24%"),"Source LH.percent retains both fractional digits");
        check(OriginalLargeBoilerParameters.contains(-1,-1,1)&&OriginalLargeBoilerParameters.contains(1,2,-1)
                &&!OriginalLargeBoilerParameters.contains(0,3,0),"Bound tool forwarding includes heat base and roof, excludes above roof");
        return assertions;
    }
    private static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
}
