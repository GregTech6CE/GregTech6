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

package com.gregtech.gregtech.content.plant;

import com.gregtech.gregtech.api.material.MaterialItemDefinitions;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.List;

/** Original MultiTileEntityBush plantGtBerry branch, with independent modern block identities.
 * Call variants only after the platform has finished material definitions and roles.
 * This catalog does not add material bushes to the original food/cotton world-generation pool.
 */
public final class MaterialBerryBushCatalog {
    private MaterialBerryBushCatalog() {}

    public record Variant(String blockPath, String berryItemId, int materialId, int colour) {}

    public static List<Variant> variants() { return Ready.VARIANTS; }

    private static final class Ready {
        private static final List<Variant> VARIANTS = MaterialItemDefinitions.all().stream()
                .filter(d -> d.prefix() == MaterialPrefix.plantGtBerry)
                .map(d -> new Variant("bush_" + d.itemId(), "gregtech:" + d.itemId(),
                        d.material().getId(), d.material().getColor() & 0xffffff)).toList();
    }

    /** Material berries use fixed foliage/bloom/immature colors and the material's solid RGB. */
    public static BerryBushCatalog.BerryType colours(String itemId, int solidRgb) {
        return new BerryBushCatalog.BerryType(itemId, 0x009000, 0xff9090, 0x80ff80, solidRgb & 0xffffff);
    }

    /** Same 28 source geometries as food bushes, supplied by the overrideable built-in pack. */
    public static String blockstateJson() {
        var json = new StringBuilder("{\"variants\":{");
        for (int stage = 0; stage < 4; stage++) for (int support = 0; support < 7; support++) {
            if (stage != 0 || support != 0) json.append(',');
            json.append("\"stage=").append(stage).append(",support=").append(support)
                    .append("\":{\"model\":\"gregtech:block/plants/bush_stage").append(stage);
            if (support != 6) json.append("_support").append(support);
            json.append("\"}");
        }
        return json.append("}}").toString();
    }
}
