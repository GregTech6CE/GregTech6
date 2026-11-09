/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

/** JSON names shared by the Forge and NeoForge machine-recipe datapack readers. */
public final class MachineRecipeFields {
    public static final String TYPE_ADDITION = MachineRecipeType.ADDITION;
    public static final String TYPE_REMOVAL = MachineRecipeType.REMOVAL;
    public static final String TYPE = "type";
    public static final String MAP = "map";
    public static final String ITEM_INPUTS = "item_inputs";
    public static final String FLUID_INPUTS = "fluid_inputs";
    public static final String ITEM_OUTPUTS = "item_outputs";
    public static final String FLUID_OUTPUTS = "fluid_outputs";
    public static final String EU_PER_TICK = "eu_per_tick";
    public static final String DURATION = "duration";
    public static final String SPECIAL = "special";
    /** Explicit lookup priority; omitted or zero for ordinary rows. */
    public static final String PRIORITY = "priority";
    public static final String MATERIAL_INPUT = "material_input";

    public static final String CLEAR = "clear";
    public static final String ITEM = "item";
    public static final String TAG = "tag";
    public static final String FLUID = "fluid";
    public static final String MATERIAL = "material";
    public static final String FORM = "form";
    public static final String COUNT = "count";
    public static final String AMOUNT = "amount";
    public static final String CHANCE = "chance";
    /** GT6 output chances are parts per 10_000. 10_000 is certain. */
    public static final int CHANCE_CERTAIN = 10_000;

    public static final long MAX_AMOUNT = Integer.MAX_VALUE;

    private MachineRecipeFields() {}
}
