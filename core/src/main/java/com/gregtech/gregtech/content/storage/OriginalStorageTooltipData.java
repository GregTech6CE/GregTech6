/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from original storage inventories and TileEntityBase09FacingSingle.addToolTips. */
package com.gregtech.gregtech.content.storage;

import java.util.ArrayList;
import java.util.List;

/** Original row choices, independent of Minecraft item/NBT and loader APIs. */
public final class OriginalStorageTooltipData {
    private OriginalStorageTooltipData() {}
    public static final long MASS_CAPACITY = 1_000_000;
    public static final String SLOT_COUNT = "gt.multitileentity.hopper.tooltip.1";
    public static final String STACK_SIZE = "gt.multitileentity.hopper.tooltip.2";
    public static final String EXACT = "gt.multitileentity.hopper.tooltip.3";
    public static final String MASS_SIZE = "gt.multitileentity.massstorage.tooltip.1";
    public static final String MASS_TABLE = "gt.multitileentity.massstorage.tooltip.2";
    public static final String SCREWDRIVER = "gt.lang.use.screwdriver.to.toggle";
    public static final String MONKEY = "gt.lang.use.monkey.wrench.to.toggle";
    public static final String INPUTS = "gt.lang.use.monkey.wrench.to.toggle.inputs";
    public static final String OUTPUTS = "gt.lang.use.monkey.wrench.to.toggle.auto.outputs";
    public static final String MAGNIFIER = "gt.lang.use.magnifyingglass.to.detail";
    public static final String RESET = "gt.lang.use.soft.hammer.to.reset";
    public static final String TAPE = "gt.lang.use.tape";
    public static final String UNTAPE = "gt.lang.use.untape";
    public static final String PINCERS = "gt.lang.use.pincers.to.take";

    public record Hopper(int slots, int stackSize, boolean showStackSize, boolean exact, List<String> tools) {}
    public static Hopper hopper(int sourceSlots, boolean queue, Integer savedMode, boolean savedExact) {
        int mode = savedMode == null ? queue ? 64 : 0 : savedMode;
        return new Hopper(Math.max(queue ? 2 : 1, sourceSlots), mode, queue || mode > 0,
                !queue && savedExact, queue ? List.of(SCREWDRIVER, MAGNIFIER, RESET)
                                          : List.of(SCREWDRIVER, MONKEY, MAGNIFIER, RESET));
    }
    public static List<String> massTools(int mode) {
        var tools = new ArrayList<>(List.of(PINCERS, "gt.lang.use.cutter.to.toggle", SCREWDRIVER, OUTPUTS));
        if ((mode & 8) == 0) { tools.add(RESET); tools.add(TAPE); }
        else tools.add(UNTAPE);
        tools.add(MAGNIFIER);
        return List.copyOf(tools);
    }
    public static List<String> drawerTools() { return List.of(INPUTS); }
    public static List<String> craftingTableTools() { return List.of(INPUTS, SCREWDRIVER); }

    /** Only source ChestGenHooks names with an explicit adopted modern table identity. */
    public static String lootKey(String table) {
        String name = switch (table) {
            case "minecraft:chests/simple_dungeon" -> "dungeonChest";
            case "minecraft:chests/abandoned_mineshaft" -> "mineshaftCorridor";
            case "minecraft:chests/stronghold_library" -> "strongholdLibrary";
            case "minecraft:chests/stronghold_crossing" -> "strongholdCrossing";
            case "minecraft:chests/stronghold_corridor" -> "strongholdCorridor";
            case "minecraft:chests/desert_pyramid" -> "pyramidDesertyChest";
            case "minecraft:chests/jungle_temple" -> "pyramidJungleChest";
            case "minecraft:chests/jungle_temple_dispenser" -> "pyramidJungleDispenser";
            case "minecraft:chests/village/village_weaponsmith" -> "villageBlacksmith";
            case "minecraft:chests/spawn_bonus_chest" -> "bonusChest";
            default -> null;
        };
        return name == null ? null : "loot." + name;
    }
}
