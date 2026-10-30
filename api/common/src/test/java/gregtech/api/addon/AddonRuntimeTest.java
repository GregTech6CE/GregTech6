/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddonRuntimeTest {
    @Test
    void hostQueriesFailBeforeTheRuntimeIsBound() {
        IllegalStateException error = assertThrows(IllegalStateException.class, AddonRuntime::services);
        assertTrue(error.getMessage().contains("not installed"));
    }
}
