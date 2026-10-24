/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

// Bounded adaptation of PrefixBlock physics and explicit Loader_Rocks/Loader_Ores tuples.
// The paired source ledger and mapping provenance accompany this candidate.
package gregtech.data;

import java.util.Optional;

/** Immutable, state-independent calculation for explicitly reviewed native host identities.
 * Unknown identity/mode is NOT default stone. Caller retains its existing fallback.
 * This does not decide material ownership or special explosive/flammable resistance.
 */
public final class SourceOreHostPhysics {
    private SourceOreHostPhysics() {}
    public enum Mode { NORMAL, SMALL, BROKEN }
    public record Params(float hardness, float resistance, int harvest, String tool) {
        /** Only this reviewed table: its six SHOVEL rows use source Material.sand (no required tool).
         * Not a generic inference about arbitrary shovel-tagged blocks or tooltip adventure flags. */
        public boolean requiresToolForDrops() { return !tool.equals("SHOVEL"); }
    }
    private record Tuple(float hardness, float resistance, int offset, int minimum, String tool) {}

    public static Optional<Params> lookup(String host, Mode mode, int quality) {
        if (host == null || mode == null) return Optional.empty();
        Tuple tuple = tuple(host, mode);
        if (tuple == null) return Optional.empty();
        int boundedQuality = Math.max(0, Math.min(15, quality));
        int harvest = Math.max(tuple.minimum(), Math.min(999, tuple.offset() + boundedQuality));
        float base = tuple.hardness();
        float hardness = base < 0 ? -1 : base == 0 ? 0 : Math.max(1, base * (1 + harvest));
        return Optional.of(new Params(hardness, tuple.resistance() * (1 + harvest), harvest, tuple.tool()));
    }

    private static Tuple tuple(String host, Mode mode) {
        return switch (host) {
            // Loader_Rocks
            case "stone_granite_black" -> switch (mode) {
                case NORMAL -> new Tuple(3.0F, 6.0F, 0, 3, "PICKAXE");
                case BROKEN -> new Tuple(1.5F, 3.0F, -1, 2, "PICKAXE");
                case SMALL -> new Tuple(3.0F, 6.0F, -1, 3, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_granite_red" -> switch (mode) {
                case NORMAL -> new Tuple(3.0F, 6.0F, 0, 3, "PICKAXE");
                case BROKEN -> new Tuple(1.5F, 3.0F, -1, 2, "PICKAXE");
                case SMALL -> new Tuple(3.0F, 6.0F, -1, 3, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_basalt" -> switch (mode) {
                case NORMAL -> new Tuple(2.0F, 3.0F, 0, 2, "PICKAXE");
                case BROKEN -> new Tuple(1.0F, 1.5F, -1, 1, "PICKAXE");
                case SMALL -> new Tuple(2.0F, 3.0F, -1, 2, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_marble" -> switch (mode) {
                case NORMAL -> new Tuple(0.5F, 0.75F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.25F, 0.37F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(0.5F, 0.75F, -1, 0, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_limestone" -> switch (mode) {
                case NORMAL -> new Tuple(0.5F, 0.75F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.25F, 0.37F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(0.5F, 0.75F, -1, 0, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_granite" -> switch (mode) {
                case NORMAL -> new Tuple(1.0F, 1.5F, 0, 1, "PICKAXE");
                case BROKEN -> new Tuple(0.5F, 0.75F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(1.0F, 1.5F, -1, 1, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_diorite" -> switch (mode) {
                case NORMAL -> new Tuple(0.5F, 0.75F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.25F, 0.37F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(0.5F, 0.75F, -1, 0, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_andesite" -> switch (mode) {
                case NORMAL -> new Tuple(0.5F, 0.75F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.25F, 0.37F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(0.5F, 0.75F, -1, 0, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_komatiite" -> switch (mode) {
                case NORMAL -> new Tuple(2.0F, 3.0F, 0, 2, "PICKAXE");
                case BROKEN -> new Tuple(1.0F, 1.5F, -1, 1, "PICKAXE");
                case SMALL -> new Tuple(2.0F, 3.0F, -1, 2, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_greenschist" -> switch (mode) {
                case NORMAL -> new Tuple(0.5F, 0.75F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.25F, 0.37F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(0.5F, 0.75F, -1, 0, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_blueschist" -> switch (mode) {
                case NORMAL -> new Tuple(0.5F, 0.75F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.25F, 0.37F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(0.5F, 0.75F, -1, 0, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_kimberlite" -> switch (mode) {
                case NORMAL -> new Tuple(2.0F, 3.0F, 0, 2, "PICKAXE");
                case BROKEN -> new Tuple(1.0F, 1.5F, -1, 1, "PICKAXE");
                case SMALL -> new Tuple(2.0F, 3.0F, -1, 2, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_quartzite" -> switch (mode) {
                case NORMAL -> new Tuple(0.5F, 0.75F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.25F, 0.37F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(0.5F, 0.75F, -1, 0, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_prismarine_light" -> switch (mode) {
                case NORMAL -> new Tuple(0.5F, 0.75F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.25F, 0.37F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(0.5F, 0.75F, -1, 0, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_prismarine_dark" -> switch (mode) {
                case NORMAL -> new Tuple(0.5F, 0.75F, 0, 1, "PICKAXE");
                case BROKEN -> new Tuple(0.25F, 0.37F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(0.5F, 0.75F, -1, 1, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_slate" -> switch (mode) {
                case NORMAL -> new Tuple(0.5F, 0.75F, 0, 1, "PICKAXE");
                case BROKEN -> new Tuple(0.25F, 0.37F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(0.5F, 0.75F, -1, 1, "PICKAXE");
            };
            // Loader_Rocks
            case "stone_shale" -> switch (mode) {
                case NORMAL -> new Tuple(0.5F, 0.75F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.25F, 0.37F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(0.5F, 0.75F, -1, 0, "PICKAXE");
            };
            // Loader_Ores direct
            case "stone" -> switch (mode) {
                case NORMAL -> new Tuple(1F, 1F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.5F, 0.5F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(1F, 1F, -1, 0, "PICKAXE");
            };
            // Loader_Ores direct
            case "netherrack" -> switch (mode) {
                case NORMAL -> new Tuple(0.5F, 0.5F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.2F, 0.2F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(0.5F, 0.5F, -1, 0, "PICKAXE");
            };
            // Loader_Ores direct
            case "end_stone" -> switch (mode) {
                case NORMAL -> new Tuple(1F, 2F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.5F, 1F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(1F, 2F, -1, 0, "PICKAXE");
            };
            // Loader_Ores direct; no distinct broken source block
            case "sand" -> switch (mode) {
                case NORMAL -> new Tuple(0.4F, 0.6F, 0, 0, "SHOVEL");
                case SMALL -> new Tuple(0.4F, 0.6F, -1, 0, "SHOVEL");
                default -> null;
            };
            // Loader_Ores direct; no distinct broken source block
            case "red_sand" -> switch (mode) {
                case NORMAL -> new Tuple(0.4F, 0.6F, 0, 0, "SHOVEL");
                case SMALL -> new Tuple(0.4F, 0.6F, -1, 0, "SHOVEL");
                default -> null;
            };
            // Loader_Ores direct; no distinct broken source block
            case "gravel" -> switch (mode) {
                case NORMAL -> new Tuple(0.6F, 0.8F, 0, 0, "SHOVEL");
                case SMALL -> new Tuple(0.6F, 0.8F, -1, 0, "SHOVEL");
                default -> null;
            };
            // Loader_Ores -w206–208 native blocks; original EtFu209–211 default rockset
            case "granite" -> switch (mode) {
                case NORMAL -> new Tuple(1F, 1F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.5F, 0.5F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(1F, 1F, -1, 0, "PICKAXE");
            };
            // Loader_Ores -w206–208 native blocks; original EtFu209–211 default rockset
            case "diorite" -> switch (mode) {
                case NORMAL -> new Tuple(1F, 1F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.5F, 0.5F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(1F, 1F, -1, 0, "PICKAXE");
            };
            // Loader_Ores -w206–208 native blocks; original EtFu209–211 default rockset
            case "andesite" -> switch (mode) {
                case NORMAL -> new Tuple(1F, 1F, 0, 0, "PICKAXE");
                case BROKEN -> new Tuple(0.5F, 0.5F, -1, 0, "PICKAXE");
                case SMALL -> new Tuple(1F, 1F, -1, 0, "PICKAXE");
            };
            default -> null;
        };
    }
}
