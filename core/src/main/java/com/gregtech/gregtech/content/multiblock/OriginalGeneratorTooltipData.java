/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original 17211..17234, MultiBlockConverter / Energy_Stats / LH.addToolTipsEfficiency. */
package com.gregtech.gregtech.content.multiblock;

import java.util.*;

/** Source controller identities and inherited presentation, shared by both native loaders. */
public final class OriginalGeneratorTooltipData {
    private OriginalGeneratorTooltipData() {}
    public enum Kind { STEAM, DYNAMO, GAS }
    public record Converter(int originalId, String legacyPath, String sourcePath, Kind kind,
                            long input, long output, int wallId) {
        public boolean showsInput() { return kind != Kind.GAS; }
        public String inputUnitKey() { return "gt.td.short.energy." + (kind == Kind.STEAM ? "steam" : kind == Kind.GAS ? "heat" : "kinetic_rotation"); }
        public String outputUnitKey() { return "gt.td.short.energy." + (kind == Kind.DYNAMO ? "electricity" : "kinetic_rotation"); }
        public long inputMinimum() { return input <= 16 ? 1 : input / 2; }
        public long inputMaximum() { return input * 2; }
        public long outputMinimum() { return output / 2; }
        public long outputMaximum() { return output * 2; }
        public int efficiency() { return (int)(10000L * output * (kind == Kind.STEAM ? 2 : 1) / input); }
        public List<String> structureKeys() {
            String family = switch(kind) {case STEAM -> "steamturbine";case DYNAMO -> "dynamo";case GAS -> "gasturbine";};
            int count = kind == Kind.DYNAMO ? 3 : 4;
            return java.util.stream.IntStream.rangeClosed(1,count).mapToObj(i->"gt.tooltip.multiblock."+family+"."+i).toList();
        }
    }
    public static final List<Converter> ALL = create();
    private static final Map<String,String> ALIASES = createAliases();
    private static List<Converter> create() {
        var list = new ArrayList<Converter>();
        for (int i=0;i<4;i++) {
            var steam=OriginalGeneratorParameters.STEAM.get(i);
            var dynamo=OriginalGeneratorParameters.DYNAMO.get(i);
            var gas=OriginalGeneratorParameters.GRADES.get(i);
            list.add(new Converter(17211+i,steam.id(),sourcePath(17211+i),Kind.STEAM,steam.input(),steam.output(),gas.wallId()));
            list.add(new Converter(17221+i,dynamo.id(),sourcePath(17221+i),Kind.DYNAMO,dynamo.input(),dynamo.output(),gas.wallId()));
            list.add(new Converter(17231+i,gas.id(),sourcePath(17231+i),Kind.GAS,gas.input(),gas.output(),gas.wallId()));
        }
        return List.copyOf(list);
    }
    private static String sourcePath(int id) {
        return SharedLargeMachineParts.DEFINITIONS.stream().filter(p->p.originalId()==id).findFirst().orElseThrow().name();
    }
    public static Converter find(String path) {
        return ALL.stream().filter(p->p.legacyPath().equals(path)||p.sourcePath().equals(path)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown original generator:"+path));
    }
    /** The existing one-item legacy conversion preserves the source controller composition. */
    private static Map<String,String> createAliases() {
        var result=new LinkedHashMap<String,String>();
        for(var p:ALL)result.put(p.legacyPath(),p.sourcePath());
        return Collections.unmodifiableMap(result);
    }
    public static Map<String,String> aliases() { return ALIASES; }
}
