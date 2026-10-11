package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/**
 * One GT6 single-block Steam Boiler Tank ({@code MultiTileEntityBoilerTank}).
 *
 * <p>Sits on top of a burning box, accepts HU on every face, converts
 * {@code 1 L water + 80 HU -> 160 L steam} and pushes the steam upward. The
 * {@code strong} variants (dense-plate built) carry the high-output values.
 * {@code steamOutput} is the GT6 {@code NBT_OUTPUT_SU} value (steam L/t emitted),
 * i.e. {@code tableValue * STEAM_PER_EU(=2)}.</p>
 */
public record BoilerSpec(
        String id,
        GTMaterial material,
        boolean strong,
        long steamOutput,
        float hardness,
        String displayEn
) {
    /** Legacy constructor retained for addons; translations belong to language resources. */
    @Deprecated
    public BoilerSpec(String id, GTMaterial material, boolean strong, long steamOutput,
                      float hardness, String displayEn, String ignoredTranslation) {
        this(id, material, strong, steamOutput, hardness, displayEn);
    }

    /** The core only supplies the English fallback, never a second translation catalog. */
    @Deprecated
    public String displayZh() { return displayEn; }

    /** HU consumed per litre of water boiled (GT6 fixed value). */
    public static final long HU_PER_WATER = 80;
    /** Steam produced per litre of water at full efficiency (GT6 fixed value). */
    public static final long STEAM_PER_WATER = 160;

    /** Internal steam buffer; overflowing it is what makes the boiler explode.
     *  Sized so that at rated heat input the boiler reaches 50% (output threshold)
     *  in 250 seconds with distilled water, or 500 seconds at max calcification. */
    public long steamCapacity() {
        return steamOutput * 10000L;
    }

    /** Internal HU buffer; exceeding it (running dry) is what overheats the boiler. */
    public long heatCapacity() {
        return steamOutput * 10000L;
    }

    /** Recommended HU draw per tick from the burning box below. */
    public long heatInputRecommended() {
        return Math.max(1L, steamOutput / 2L);
    }
}
