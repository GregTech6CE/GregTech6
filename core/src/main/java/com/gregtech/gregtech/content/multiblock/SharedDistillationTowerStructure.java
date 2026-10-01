package com.gregtech.gregtech.content.multiblock;
import com.gregtech.gregtech.api.multiblock.StructureGrid.Role;import java.util.*;
/** Original 17101 front-bottom tower and heat-transmitter base, preserving role gates. */
public final class SharedDistillationTowerStructure {private SharedDistillationTowerStructure(){}public static final List<SharedLargeMachineLayouts.Cell> CELLS=create();private static List<SharedLargeMachineLayouts.Cell> create(){var cells=new ArrayList<SharedLargeMachineLayouts.Cell>();for(int x=-1;x<=1;x++)for(int y=-1;y<8;y++)for(int z=0;z<3;z++){if(x==0&&y==0&&z==0)continue;cells.add(new SharedLargeMachineLayouts.Cell(x,y,z,y<0?18101:18102,y<0?Role.ENERGY_INPUT:y==0?Role.ITEM_FLUID_IO:Role.FLUID_OUTPUT));}return List.copyOf(cells);}}
