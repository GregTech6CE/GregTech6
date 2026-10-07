/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original MultiTileEntityBasicMachine and 247 Basic Machines registrations. */
package com.gregtech.gregtech.content.machine;
import com.gregtech.gregtech.api.energy.MachineFaceMasks;
import com.gregtech.gregtech.data.BasicMachineOriginalParams;
import java.util.List;
import java.util.Set;
public final class OriginalBasicMachineRules {
    private OriginalBasicMachineRules() {}
    private static final Set<String> FAMILIES=Set.of("autoclave","autocrafter","bath","boxinator","bumblelyzer","burnmixer","buzzsaw","canner","catalyticcracker","centrifuge","clustermill","coagulator","compressor","crusher","cryomixer","crystallisationcrucible","debarker","distillery","dryer","electricloom","electricmixer","electricsifter","electrolyzer","extruder","fermenter","freezer","generifier","injector","laminator","laserengraver","laserwelder","lathe","lightning","loom","magneticseparator","massfab","melter","mixer","nanofab","oven","plantalyzer","polarizer","press","printer","replicator","roaster","rollbender","rollformer","rollingmill","sander","scannermolecular","scannervisuals","shredder","sifter","slicer","sluice","smelter","squeezer","steamcracker","unboxinator","wiremill");
    public static boolean handles(String name,int tier) {return FAMILIES.contains(name)&&BasicMachineOriginalParams.find(name,tier)!=null;}
    public static boolean cheapOverclocking(String name,int tier) {
        return handles(name,tier)&&Set.of("roaster","distillery","smelter","dryer","massfab","replicator","melter").contains(name);
    }
    public static int efficiency(String name,int tier) {
        if(!handles(name,tier))return 10000;
        return switch(name) {
            case "electricmixer","electricloom","electricsifter"->5000;
            case "massfab","replicator"->5000+1250*(tier-1);
            default->10000;
        };
    }
    public static boolean requiresIgnition(String name,int tier) {return handles(name,tier)&&name.equals("burnmixer");}
    public static boolean noConstantPower(String name,int tier) {return handles(name,tier)&&Set.of("massfab","replicator","autoclave","bath","generifier","coagulator").contains(name);}
    public static final int IGNITION_TICKS=40;
    public static final String IGNITION_NBT="gt.ignite";
    public static String faceKey(int side) {return switch(side) {
        case MachineFaceMasks.BOTTOM->"gt.lang.face.bottom";case MachineFaceMasks.TOP->"gt.lang.face.top";
        case MachineFaceMasks.LEFT->"gt.lang.face.left";case MachineFaceMasks.RIGHT->"gt.lang.face.right";
        case MachineFaceMasks.FRONT->"gt.lang.face.front";case MachineFaceMasks.BACK->"gt.lang.face.back";
        default->throw new IllegalArgumentException("Source face:"+side);
    };}
    public static final List<Integer> FACE_ORDER=List.of(MachineFaceMasks.BOTTOM,MachineFaceMasks.TOP,MachineFaceMasks.LEFT,MachineFaceMasks.FRONT,MachineFaceMasks.RIGHT,MachineFaceMasks.BACK);
    public static String unitKey(String type) {return "gt.td.short.energy."+switch(type) {
        case "HU"->"heat";case "RU"->"kinetic_rotation";case "KU"->"kinetic_push";
        case "EU"->"electricity";case "CU"->"cryo";case "LU"->"light";case "QU"->"quantum";case "MU"->"magnetic";
        case "TU"->"time";case "RF"->"redstone_flux";case "MJ"->"minecraft_joules";
        default->throw new IllegalArgumentException("Source energy:"+type);
    };}
    public record IoFace(int side,boolean automatic) {}
    public record IoRow(boolean any,List<IoFace> faces) {public IoRow {faces=List.copyOf(faces);}}
    /** Source 127 (including SIDE_ANY) corresponds to the port's six-bit63. */
    public static IoRow io(int slots,int mask,int auto) {
        if(slots<=0)return null;
        final int selected=mask&63;
        if(selected==63)return new IoRow(true,MachineFaceMasks.autoValid(auto)?List.of(new IoFace(auto,true)):List.of());
        var faces=FACE_ORDER.stream().filter(side->MachineFaceMasks.has(selected,side)).map(side->new IoFace(side,side==auto)).toList();
        return faces.isEmpty()?null:new IoRow(false,faces);
    }
}
