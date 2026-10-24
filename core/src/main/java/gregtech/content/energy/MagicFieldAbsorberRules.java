/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntityMagicFieldAbsorber: trophy selection and alternating KU packets. */
package gregtech.content.energy;

import gregtech.data.GregTechTags;

public final class MagicFieldAbsorberRules {
    private MagicFieldAbsorberRules() {}
    public record Output(GregTechTags.Tag type, long amount) {
        public long packetSize(long tick) {
            if (GregTechTags.Energy.isSizeIrrelevant(type)) return 1;
            return type == GregTechTags.Energy.KU && tick % 128 < 64 ? -amount : amount;
        }
        public long packetCount() { return GregTechTags.Energy.isSizeIrrelevant(type) ? amount : 1; }
    }
    public static Output trophy(String id, boolean skull) {
        if (id.equals("minecraft:dragon_egg")) return new Output(GregTechTags.Energy.QU, 64);
        if (skull) return new Output(GregTechTags.Energy.TU, 1);
        return switch (id) {
            case "twilightforest:naga_trophy", "twilightforest:naga_wall_trophy" -> new Output(GregTechTags.Energy.KU, 64);
            case "twilightforest:lich_trophy", "twilightforest:lich_wall_trophy" -> new Output(GregTechTags.Energy.QU, 64);
            case "twilightforest:hydra_trophy", "twilightforest:hydra_wall_trophy" -> new Output(GregTechTags.Energy.HU, 64);
            case "twilightforest:ur_ghast_trophy", "twilightforest:ur_ghast_wall_trophy" -> new Output(GregTechTags.Energy.LU, 64);
            case "twilightforest:snow_queen_trophy", "twilightforest:snow_queen_wall_trophy" -> new Output(GregTechTags.Energy.CU, 64);
            default -> null;
        };
    }
}
