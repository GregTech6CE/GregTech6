/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@Isolated
class GregTechAddonsTest {
    private static final GregTechAddon.Context CONTEXT = new GregTechAddon.Context("forge", "1.20.1");

    @AfterEach
    void reset() throws ReflectiveOperationException {
        ((Map<?, ?>) accessibleField(GregTechAddons.class, "ADDONS").get(null)).clear();
        accessibleField(GregTechAddons.class, "phase").set(null, GregTechAddons.Phase.COLLECTING);
    }

    @Test
    void apiVersionCodeMatchesTheMajorAndMinorParts() {
        assertEquals(GregTechAddons.apiMajor() * 1000 + GregTechAddons.apiMinor(), GregTechAddons.apiVersion());
        assertEquals(1, GregTechAddons.apiMajor());
        assertEquals(10, GregTechAddons.apiMinor());
        assertEquals(1010, GregTechAddons.apiVersion());
    }

    @Test
    void registersAndDispatchesInOrder() {
        GregTechAddons.register(addon("first_addon", context -> {}));
        GregTechAddons.register(addon("second_addon", context -> {}));
        assertEquals(List.of("first_addon", "second_addon"), GregTechAddons.registeredIds());
        assertEquals(GregTechAddons.Phase.COLLECTING, GregTechAddons.phase());

        GregTechAddons.dispatchRecipesReady(CONTEXT);
        assertTrue(GregTechAddons.recipesReady());
        assertEquals(GregTechAddons.Phase.READY, GregTechAddons.phase());
    }

    @Test
    void duplicateAndLateRegistrationsFail() {
        GregTechAddons.register(addon("test_addon", context -> {}));
        assertThrows(IllegalStateException.class, () -> GregTechAddons.register(addon("test_addon", context -> {})));

        GregTechAddons.dispatchRecipesReady(CONTEXT);
        assertThrows(IllegalStateException.class, () -> GregTechAddons.register(addon("late_addon", context -> {})));
    }

    @Test
    void callbackFailureLeavesThePhaseFailed() {
        GregTechAddons.register(addon("test_addon", context -> {
            throw new IllegalStateException("boom");
        }));
        assertThrows(IllegalStateException.class, () -> GregTechAddons.dispatchRecipesReady(CONTEXT));
        assertEquals(GregTechAddons.Phase.FAILED, GregTechAddons.phase());
    }

    private static Field accessibleField(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static GregTechAddon addon(String id, Consumer<GregTechAddon.Context> callback) {
        return new GregTechAddon() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public void onRecipesReady(GregTechAddon.Context context) {
                callback.accept(context);
            }
        };
    }
}
