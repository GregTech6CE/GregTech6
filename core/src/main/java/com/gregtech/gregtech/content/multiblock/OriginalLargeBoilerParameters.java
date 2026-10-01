package com.gregtech.gregtech.content.multiblock;
import com.gregtech.gregtech.api.multiblock.StructureGrid;import java.util.*;
/** Original five boiler variants, rated capacities and geometry. */
public final class OriginalLargeBoilerParameters {private OriginalLargeBoilerParameters(){}
 public static final int WATER_CAPACITY=128_000,HU_PER_WATER=80,STEAM_PER_WATER=160;
 public record Variant(int originalId,String path,int denseWallId,long steamOutput,float hardness){public long heatCapacity(){return steamOutput*10_000L;}public long steamCapacity(){return steamOutput*10_000L;}public long heatInputRecommended(){return steamOutput/2;}}
    private static final List<Variant> VARIANTS = List.of(
            new Variant(17201, "stainless_steel_boiler_main_barometer", 18022,   8_192,   6),
            new Variant(17202, "titanium_boiler_main_barometer",       18026,  16_384,   9),
            new Variant(17203, "tungstensteel_boiler_main_barometer",   18023,  32_768, 12.5f),
            new Variant(17204, "adamantium_boiler_main_barometer",     18025, 262_144, 100),
            new Variant(17205, "invar_boiler_main_barometer",          18027,   8_192,   6));

 public static final StructureGrid LAYOUT=layout();public static List<Variant> all(){return VARIANTS;}public static Variant byOriginalId(int id){return VARIANTS.stream().filter(v->v.originalId()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown GT6 large boiler "+id));}
    private static StructureGrid layout() {
        var cells = new ArrayList<StructureGrid.Cell>();
        for (int y = -1; y <= 2; y++) for (int back = 0; back <= 2; back++)
            for (int right = -1; right <= 1; right++) {
                StructureGrid.Role role;
                if (y == -1) role = StructureGrid.Role.HEAT_INPUT;
                else if (y == 0) role = StructureGrid.Role.FLUID_INPUT;
                else if (y == 1 && right == 0 && back == 1) role = StructureGrid.Role.AIR;
                else role = StructureGrid.Role.FLUID_OUTPUT;
                cells.add(new StructureGrid.Cell(right, y, back, role));
            }
        return new StructureGrid(cells);
    }
}
