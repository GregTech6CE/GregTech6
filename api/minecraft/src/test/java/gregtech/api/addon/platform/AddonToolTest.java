/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddonToolTest {
    @Test
    void namesMatchTheHostToolTable() {
        assertEquals(List.of(
                        "SPADE",
                        "UNIVERSAL_SPADE",
                        "MAGNIFYING_GLASS",
                        "SCREWDRIVER",
                        "SOFT_HAMMER",
                        "WRENCH",
                        "MONKEY_WRENCH",
                        "HARD_HAMMER",
                        "WIRE_CUTTER",
                        "SAW"),
                Arrays.stream(AddonTool.values()).map(Enum::name).toList());
    }
}
