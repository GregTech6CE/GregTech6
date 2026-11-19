/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MachineRecipeDocumentsTest {
    @Test
    void exactIntegerValuesSurviveDifferentNumericRepresentations() {
        assertEquals(Long.MAX_VALUE, priority(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, priority(new BigInteger("9223372036854775807")));
        assertEquals(Long.MAX_VALUE, priority(new BigDecimal("9223372036854775807.00")));
        assertEquals(Long.MIN_VALUE, priority(-0x1.0p63));
        assertEquals(43L, priority(43.0f));
        assertEquals(9_007_199_254_740_993L, priority(new JsonNumber("9007199254740993.0")));
    }

    @Test
    void outOfRangeAndFractionalNumbersDoNotWrapRoundOrSaturate() {
        for (Number value : List.of(
                new BigInteger("9223372036854775808"),
                new BigDecimal("9007199254740992.5"),
                0x1.0p63,
                Double.NaN,
                Float.POSITIVE_INFINITY,
                new JsonNumber("9007199254740992.5"),
                new JsonNumber("-9223372036854775809"))) {
            var failure = assertThrows(IllegalArgumentException.class, () -> priority(value), value::toString);
            assertTrue(failure.getMessage().contains("priority"));
        }
    }

    private static long priority(Number value) {
        return MachineRecipeDocuments.addition(Map.of(
                "map", "gt.recipe.crusher",
                "item_inputs", List.of(Map.of("item", "minecraft:stone")),
                "item_outputs", List.of(Map.of("item", "minecraft:gravel")),
                "eu_per_tick", 16,
                "duration", 20,
                "priority", value)).priority();
    }

    /** Emulates JSON libraries that expose a token-backed Number. */
    private static final class JsonNumber extends Number {
        private final String token;

        private JsonNumber(String token) {
            this.token = token;
        }

        @Override public int intValue() { return (int) longValue(); }
        @Override public long longValue() { return new BigDecimal(token).longValue(); }
        @Override public float floatValue() { return Float.parseFloat(token); }
        @Override public double doubleValue() { return Double.parseDouble(token); }
        @Override public String toString() { return token; }
    }
}
