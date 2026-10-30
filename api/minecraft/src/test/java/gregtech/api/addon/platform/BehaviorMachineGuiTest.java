/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorMachineGuiTest {
    @Test
    void publishedGeometryMatchesTheHostTexture() {
        assertEquals(18, BehaviorMachineGui.SLOT);
        assertEquals(18, BehaviorMachineGui.TANK_WIDTH);
        assertEquals(54, BehaviorMachineGui.TANK_HEIGHT);
        assertEquals(10, BehaviorMachineGui.ENERGY_WIDTH);
        assertEquals(54, BehaviorMachineGui.ENERGY_HEIGHT);
        assertEquals(24, BehaviorMachineGui.PROGRESS_WIDTH);
        assertEquals(17, BehaviorMachineGui.PROGRESS_HEIGHT);
        assertEquals(82, BehaviorMachineGui.PLAYER_INVENTORY_GAP);
        assertEquals(176, BehaviorMachineGui.UV_SLOT_U);
        assertEquals(0, BehaviorMachineGui.UV_SLOT_V);
        assertEquals(194, BehaviorMachineGui.UV_ENERGY_U);
        assertEquals(0, BehaviorMachineGui.UV_ENERGY_V);
        assertEquals(204, BehaviorMachineGui.UV_ENERGY_FULL_U);
        assertEquals(214, BehaviorMachineGui.UV_TANK_U);
        assertEquals(0, BehaviorMachineGui.UV_TANK_V);
        assertEquals(232, BehaviorMachineGui.UV_TANK_MARKS_U);
        assertEquals(176, BehaviorMachineGui.UV_ARROW_U);
        assertEquals(54, BehaviorMachineGui.UV_ARROW_V);
        assertEquals(71, BehaviorMachineGui.UV_ARROW_FULL_V);
        assertEquals(176, BehaviorMachineGui.UV_TOGGLE_OFF_U);
        assertEquals(194, BehaviorMachineGui.UV_TOGGLE_ON_U);
        assertEquals(88, BehaviorMachineGui.UV_TOGGLE_V);
    }

    @Test
    void widgetSizesStayPositive() {
        assertTrue(BehaviorMachineGui.SLOT > 0);
        assertTrue(BehaviorMachineGui.TANK_WIDTH > 0);
        assertTrue(BehaviorMachineGui.TANK_HEIGHT > 0);
        assertTrue(BehaviorMachineGui.ENERGY_WIDTH > 0);
        assertTrue(BehaviorMachineGui.ENERGY_HEIGHT > 0);
        assertTrue(BehaviorMachineGui.PROGRESS_WIDTH > 0);
        assertTrue(BehaviorMachineGui.PROGRESS_HEIGHT > 0);
        assertTrue(BehaviorMachineGui.PLAYER_INVENTORY_GAP > 0);
    }
}
