package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.multiblock.StructureGrid.Role;
import java.util.*;

/** GT6 MultiTileEntityMatterFabricator: 97 walls, 26 coils, 16 vents and 9 processors. */
public final class SharedMatterFabricatorStructure {
    private SharedMatterFabricatorStructure() {}
    public static final List<SharedLargeMachineLayouts.Cell> CELLS=create();
    private static List<SharedLargeMachineLayouts.Cell> create() {
        var cells=new ArrayList<SharedLargeMachineLayouts.Cell>();
        for(int x=-2;x<=2;x++)for(int y=0;y<5;y++)for(int z=0;z<5;z++) {
            if(x==0&&y==0&&z==0)continue;
            boolean air=x==0&&y==2&&z==2;
            int part=air?0:Math.abs(x)==2||y==0||y==4||z==0||z==4?18031:18044;
            cells.add(new SharedLargeMachineLayouts.Cell(x,y,z,part,air?Role.AIR:Role.ITEM_FLUID_ENERGY));
        }
        int processor=0;
        for(int x=-2;x<=2;x++)for(int z=0;z<5;z++) {
            int part=Math.abs(x)==2||z==0||z==4?18299:x==0&&z==2?18200:processor++<4?18202:18204;
            cells.add(new SharedLargeMachineLayouts.Cell(x,5,z,part,Role.CASING));
        }
        return List.copyOf(cells);
    }
}
