/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

/**
 * One addon sensor kind. GregTech registers a block and a block item for it as
 * {@code <addon>:sensor_<id>}; the addon supplies the blockstate, model and name.
 * The display reads {@link #read(SensorContext)} every ten game ticks and uses
 * {@link #maximum(SensorContext)} for percentage, full and gauge modes.
 * {@link #unit()} is the glyph name shown in the sixth cell; it must be one of
 * the GregTech unit glyphs ({@code liter}, {@code eu}, {@code scale}, {@code kelvin},
 * {@code ru}, {@code ton}, {@code gramm}, {@code kilogramm}, {@code kiloton}, {@code clock},
 * {@code cubicmeter}, {@code cubicdecameter}, {@code gibbl}, {@code lumin}, {@code greg},
 * {@code neutron} or {@code lu}).
 */
public interface AddonSensor {
    /** Registry path suffix, without the namespace or the {@code sensor_} prefix. */
    String id();

    /** Translation key for the tooltip line. The addon supplies the block name separately. */
    String titleKey();

    /** Unit glyph name. */
    String unit();

    /** Display tint, {@code 0xRRGGBB}. */
    int color();

    /** Declared scale, used when the target does not supply one. */
    long maximum();

    /** Current value. The context target may be null. */
    long read(SensorContext context);

    /** Scale for this target. The default keeps {@link #maximum()}. */
    default long maximum(SensorContext context) { return maximum(); }
}
