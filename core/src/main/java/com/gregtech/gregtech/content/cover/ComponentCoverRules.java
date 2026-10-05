/**
 * Copyright (c) 2021 GregTech-6 Team
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

/* Adapted from Gregorius Techneticies' CoverPump/Conveyor/RobotArm, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.cover;

/** Loader-independent timing, placement, mode, slot and face rules of the component covers. */
public final class ComponentCoverRules {
    private ComponentCoverRules() {}
    public enum Kind { PUMP, CONVEYOR, ROBOT_ARM }
    public static Kind kind(String behavior) {
        if (behavior == null) return null;
        return switch (behavior) {
            case "cover_pump" -> Kind.PUMP;
            case "cover_conveyor" -> Kind.CONVEYOR;
            case "cover_robot_arm" -> Kind.ROBOT_ARM;
            default -> null;
        };
    }
    public static boolean canAttach(Kind kind, boolean ticks, boolean fluids, boolean items) {
        return kind == null || ticks && (kind == Kind.PUMP ? fluids : items);
    }
    public static boolean due(Kind kind, int tier, long serverTime, boolean stopped) {
        return !stopped && (kind == Kind.PUMP ? Math.floorMod(serverTime, 20) == 5
                : Math.floorMod(serverTime, itemInterval(tier)) == 0);
    }
    public static int pumpThroughput(int tier) { return tier < 0 ? 1000 : 250 << (2 * tier); }
    public static int itemInterval(int tier) { return tier < 0 ? 20 : Math.max(1, 512 >> tier); }
    public static int toggle(int visual, boolean pipe) { return pipe || visual == 0 ? 1 : 0; }
    public static int slotStep(int slot, boolean sneaking, boolean pipe) {
        return (int) Math.max(Short.MIN_VALUE, Math.min(pipe ? -1 : Short.MAX_VALUE,
                (long) slot + (sneaking ? -1 : 1)));
    }
    public static int sourceSlot(int value) { return value < 0 ? -1 - value : -1; }
    public static int targetSlot(int value) { return value < 0 ? -1 : value; }
    public static boolean allowsInsert(int visual) { return visual != 0; }
    public static boolean allowsExtract(int visual) { return visual == 0; }
    public static String texture(Kind kind, int visual) {
        return switch (kind) { case PUMP -> "pump"; case CONVEYOR -> "conveyor"; case ROBOT_ARM -> "robotarm"; }
                + (visual == 0 ? "/out" : "/in");
    }
}
