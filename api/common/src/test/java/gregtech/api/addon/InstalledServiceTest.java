/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstalledServiceTest {
    @Test
    void bindsOnceAndReadsBack() {
        InstalledService<String> service = new InstalledService<>();
        service.bind("first", "missing service", "already installed");
        assertEquals("first", service.get("missing service"));
    }

    @Test
    void aSecondBindNamesTheFirstBinder() {
        InstalledService<String> service = new InstalledService<>();
        service.bind("first", "missing service", "already installed");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.bind("second", "missing service", "already installed"));
        assertTrue(error.getMessage().contains("first bound by"));
        assertNotNull(error.getCause());
    }
}
