/**
 * Copyright (c) 2023 GregTech-6 Team
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

package com.gregtech.gregtech.content.logistics;

import java.util.List;

/** Original metadata is flattened to stable block identities, shared by both loaders. */
public final class LongDistanceCatalog {
    private LongDistanceCatalog() {}
    public record Spec(String id, String kind, long voltage, float hardness, float resistance,
                       int sourceMeta, String material, long maximumTemperature, String texture) {
        public boolean sourceLine() { return sourceMeta >= 0; }
        public String networkIdentity() { return sourceLine() ? kind + ":" + sourceMeta : id; }
    }
    private static Spec wire(String id, int meta, String material, int tier) {
        String texture = new String[]{"ev", "iv", "luv", "zpm", "uv"}[tier - 4];
        return new Spec(id, "WIRE", 4096L << (2 * (tier - 4)), 5, 15, meta, material, 0,
                "long_dist_wire_" + texture);
    }
    private static Spec pipe(String id, int meta, String material, long temperature) {
        return new Spec(id, meta == 0 ? "ITEM_PIPE" : "FLUID_PIPE", 0, 5, 20, meta, material,
                temperature, meta == 0 ? "long_dist_pipe_item" : "long_dist_pipe_fluid");
    }
    private static Spec endpoint(String id, String kind, long voltage) {
        return new Spec(id, kind, voltage, kind.equals("TRANSFORMER") ? 4 : 16,
                kind.equals("TRANSFORMER") ? 4 : 16, -1, "", 0, "");
    }
    public static final List<Spec> LINES = List.of(
            wire("long_dist_wire_ev", 0, "Tin", 4),
            wire("long_dist_wire_lead", 1, "Lead", 4),
            wire("long_dist_wire_iv", 2, "Copper", 5),
            wire("long_dist_wire_luv", 3, "Silver", 6),
            wire("long_dist_wire_gold", 4, "Gold", 6),
            wire("long_dist_wire_electrum", 5, "Electrum", 6),
            wire("long_dist_wire_blue_alloy", 6, "BlueAlloy", 6),
            wire("long_dist_wire_electrotine_alloy", 7, "ElectrotineAlloy", 6),
            wire("long_dist_wire_zpm", 8, "Steel", 7),
            wire("long_dist_wire_aluminium", 9, "Aluminium", 7),
            wire("long_dist_wire_tungsten", 10, "Tungsten", 7),
            wire("long_dist_wire_tungsten_steel", 11, "TungstenSteel", 7),
            wire("long_dist_wire_uv", 12, "Osmium", 8),
            wire("long_dist_wire_platinum", 13, "Platinum", 8),
            wire("long_dist_wire_naquadah", 14, "Naquadah", 8),
            wire("long_dist_wire_graphene", 15, "Graphene", 8),
            pipe("long_dist_pipe_item", 0, "Electrum", -1),
            pipe("long_dist_pipe_fluid", 1, "StainlessSteel", 1943),
            pipe("long_dist_pipe_tungsten", 2, "Tungsten", 3695),
            pipe("long_dist_pipe_adamantium", 3, "Adamantium", 5225),
            pipe("long_dist_pipe_draconium", 4, "Draconium", 4500));
    public static final List<Spec> ALL;
    static {
        var entries = new java.util.ArrayList<Spec>();
        // Keep the old generic IDs, without introducing new survival recipes for them.
        entries.add(new Spec("long_dist_pipe", "LEGACY_PIPE", 0, 5, 20, -1, "", Long.MAX_VALUE, "long_dist_pipe_item"));
        entries.add(wire("long_dist_wire", 0, "Tin", 4));
        entries.add(endpoint("long_dist_endpoint_item", "ITEM_ENDPOINT", 0));
        entries.add(endpoint("long_dist_endpoint_fluid", "FLUID_ENDPOINT", 0));
        entries.add(endpoint("long_dist_transformer_ulv", "TRANSFORMER", 2048));
        entries.add(endpoint("long_dist_transformer_lv", "TRANSFORMER", 8192));
        entries.add(endpoint("long_dist_transformer_mv", "TRANSFORMER", 32768));
        entries.add(endpoint("long_dist_transformer_zpm", "TRANSFORMER", 131072));
        entries.add(endpoint("long_dist_transformer_uv", "TRANSFORMER", 524288));
        entries.addAll(LINES);
        ALL = List.copyOf(entries);
    }
    public static Spec find(String id) { return ALL.stream().filter(s -> s.id().equals(id)).findFirst().orElse(null); }
    public static Spec get(String id) {
        var result = find(id);
        if (result == null) throw new IllegalArgumentException("Unknown long-distance block: " + id);
        return result;
    }
    public static long voltage(String id) { return get(id).voltage(); }
}
