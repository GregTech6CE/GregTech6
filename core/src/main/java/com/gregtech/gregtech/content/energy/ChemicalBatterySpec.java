package com.gregtech.gregtech.content.energy;

import java.util.*;

/** GT6 Loader_MultiTileEntities 14000..14044: five chemistries, ULV through EV. */
public record ChemicalBatterySpec(Chemistry chemistry,int tier) {
    public enum Chemistry {
        LEAD_ACID("lead_acid",2000,0xFF8000,false),
        ALKALINE("alkaline",4000,0x0000FF,false),
        NICKEL_CADMIUM("nickel_cadmium",4000,0x80FF80,false),
        LITHIUM_COBALT("lithium_cobalt",64000,0x0000FF,true),
        LITHIUM_MANGANESE("lithium_manganese",128000,0x00FF00,true);
        public final String id;
        public final long multiplier;
        public final int color;
        public final boolean advanced;
        Chemistry(String id,long multiplier,int color,boolean advanced) {
            this.id=id;this.multiplier=multiplier;this.color=color;this.advanced=advanced;
        }
    }
    public ChemicalBatterySpec {
        Objects.requireNonNull(chemistry);
        if(tier<0||tier>4) throw new IllegalArgumentException("Battery tier must be ULV..EV");
    }
    public String id(){return "battery_"+chemistry.id+"_"+new String[]{"ulv","lv","mv","hv","ev"}[tier];}
    public long voltage(){return 8L<<(tier*2);}
    public long capacity(){return voltage()*chemistry.multiplier;}
    public long minimumPacket(){return tier==0?1:voltage()/2;}
    public long maximumPacket(){return voltage()*2;}
    public int inset(){return new int[]{5,5,4,3,2}[tier];}
    public int height(){return tier==0?8:tier==4?13:11;}
    public int scale(){return tier==0?4:tier==4?9:7;}
    public String texture(){return "block/machines/batteries/eu/"+(chemistry.advanced?"advanced/":"standard/")+voltage();}
    /** GT6 UT.Code.scale: zero only when empty, last mark only when completely full. */
    public int display(long energy){return energy<=0?0:energy>=capacity()?scale():1+(int)(energy*(scale()-1)/capacity());}
    public static List<ChemicalBatterySpec> all() {
        var specs=new ArrayList<ChemicalBatterySpec>();
        for(var chemistry:Chemistry.values()) for(int tier=0;tier<5;tier++) specs.add(new ChemicalBatterySpec(chemistry,tier));
        return List.copyOf(specs);
    }
}
