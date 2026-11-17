/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Controller ids and inherited tooltip values of GT6's generators (17211..17234,
 * MultiBlockConverter / Energy_Stats / LH.addToolTipsEfficiency). */
package gregtech.content.multiblock;

import gregtech.content.multiblock.OriginalGeneratorParameters.Ratings;
import java.util.*;

/** Source controller identities and inherited presentation, shared by both native loaders. */
public final class OriginalGeneratorTooltipData {
    private OriginalGeneratorTooltipData() {}

    public enum Kind {
        STEAM,
        DYNAMO,
        GAS
    }

    public record Converter(int originalId, String sourcePath, Kind kind, Ratings ratings, int wallId) {
        public boolean showsInput() {
            return kind != Kind.GAS;
        }

        public String inputUnitKey() {
            return "gt.td.short.energy."
                    + (kind == Kind.STEAM ? "steam" : kind == Kind.GAS ? "heat" : "kinetic_rotation");
        }

        public String outputUnitKey() {
            return "gt.td.short.energy." + (kind == Kind.DYNAMO ? "electricity" : "kinetic_rotation");
        }

        public List<String> structureKeys() {
            String family = switch (kind) {
                case STEAM -> "steamturbine";
                case DYNAMO -> "dynamo";
                case GAS -> "gasturbine";
            };
            int count = kind == Kind.DYNAMO ? 3 : 4;
            return java.util.stream.IntStream.rangeClosed(1, count)
                    .mapToObj(i -> "gt.tooltip.multiblock." + family + "." + i)
                    .toList();
        }
    }

    public static final List<Converter> ALL = create();

    private static List<Converter> create() {
        var list = new ArrayList<Converter>();
        for (int i = 0; i < 4; i++) {
            var steam = OriginalGeneratorParameters.STEAM.get(i);
            var dynamo = OriginalGeneratorParameters.DYNAMO.get(i);
            var gas = OriginalGeneratorParameters.GRADES.get(i);
            list.add(new Converter(17211 + i, sourcePath(17211 + i), Kind.STEAM, steam, gas.wallId()));
            list.add(new Converter(17221 + i, sourcePath(17221 + i), Kind.DYNAMO, dynamo, gas.wallId()));
            list.add(new Converter(17231 + i, sourcePath(17231 + i), Kind.GAS, gas, gas.wallId()));
        }
        return List.copyOf(list);
    }

    private static String sourcePath(int id) {
        return SharedLargeMachineParts.DEFINITIONS.stream()
                .filter(p -> p.originalId() == id)
                .findFirst()
                .orElseThrow()
                .name();
    }

    public static Converter find(String path) {
        return ALL.stream()
                .filter(p -> p.sourcePath().equals(path))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown original generator:" + path));
    }
}
