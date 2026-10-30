/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GregTechAddonsTest {
    private static final GregTechAddon.Context CONTEXT = new GregTechAddon.Context("forge", "1.20.1");

    @AfterEach
    void reset() {
        GregTechAddons.resetForTesting();
    }

    @Test
    void apiVersionCodeMatchesTheMajorAndMinorParts() {
        assertEquals(GregTechAddons.apiMajor() * 1000 + GregTechAddons.apiMinor(),
                GregTechAddons.apiVersion());
        assertEquals(1, GregTechAddons.apiMajor());
        assertEquals(0, GregTechAddons.apiMinor());
        assertEquals(1000, GregTechAddons.apiVersion());
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
        assertThrows(IllegalStateException.class,
                () -> GregTechAddons.register(addon("test_addon", context -> {})));

        GregTechAddons.dispatchRecipesReady(CONTEXT);
        assertThrows(IllegalStateException.class,
                () -> GregTechAddons.register(addon("late_addon", context -> {})));
    }

    @Test
    void callbackFailureLeavesThePhaseFailed() {
        GregTechAddons.register(addon("test_addon", context -> {
            throw new IllegalStateException("boom");
        }));
        assertThrows(IllegalStateException.class, () -> GregTechAddons.dispatchRecipesReady(CONTEXT));
        assertEquals(GregTechAddons.Phase.FAILED, GregTechAddons.phase());
    }

    @Test
    void resetReturnsAFreshLifecycle() {
        GregTechAddons.register(addon("test_addon", context -> {}));
        GregTechAddons.dispatchRecipesReady(CONTEXT);
        GregTechAddons.resetForTesting();
        assertEquals(List.of(), GregTechAddons.registeredIds());
        assertEquals(GregTechAddons.Phase.COLLECTING, GregTechAddons.phase());
        GregTechAddons.register(addon("test_addon", context -> {}));
        assertEquals(List.of("test_addon"), GregTechAddons.registeredIds());
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
