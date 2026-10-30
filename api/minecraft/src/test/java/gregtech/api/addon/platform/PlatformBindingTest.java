/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformBindingTest {
    @Test
    void platformServicesFailBeforeBinding() {
        IllegalStateException error = assertThrows(IllegalStateException.class, AddonPlatform::services);
        assertTrue(error.getMessage().contains("not installed"));
    }

    @Test
    void loaderServicesFailBeforeBinding() {
        IllegalStateException error = assertThrows(IllegalStateException.class, AddonLoader::services);
        assertTrue(error.getMessage().contains("not installed"));
    }

    @Test
    void aNullPlatformBindingIsRejectedWithoutBinding() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AddonPlatform.bind(null));
        assertTrue(error.getMessage().contains("addon platform services"));
        assertThrows(IllegalStateException.class, AddonPlatform::services);
    }

    @Test
    void aNullLoaderBindingIsRejectedWithoutBinding() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AddonLoader.bind(null));
        assertTrue(error.getMessage().contains("addon loader services"));
        assertThrows(IllegalStateException.class, AddonLoader::services);
    }

    @Test
    void recipesMapFailsBeforeBinding() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> AddonRecipes.map("ElectricBlastFurnace"));
        assertTrue(error.getMessage().contains("not installed"));
    }
}
