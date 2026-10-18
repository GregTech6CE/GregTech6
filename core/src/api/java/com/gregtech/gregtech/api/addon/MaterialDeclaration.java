package com.gregtech.gregtech.api.addon;

import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/** Pure data for one addon material. GregTech turns it into a generated {@code gregtech:} material at startup. */
public record MaterialDeclaration(
        int id,
        String name,
        String displayName,
        int color,
        int meltingPoint,
        int boilingPoint,
        float density,
        String formula,
        boolean dust,
        boolean gas,
        boolean liquid
) {
    public MaterialDeclaration {
        if (id <= 0 || id >= 10_000) throw new IllegalArgumentException("Material ID must be 1..9999: " + id);
        Objects.requireNonNull(name, "Material name");
        if (!name.matches("[A-Za-z][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Use a stable, readable material name: " + name);
        Objects.requireNonNull(displayName, "Material display name");
        if (displayName.isBlank() || (color & 0xFF00_0000) != 0)
            throw new IllegalArgumentException("Material requires a display name and RGB color: " + name);
        if (meltingPoint < 0 || boilingPoint < 0 || !Float.isFinite(density) || density < 0)
            throw new IllegalArgumentException("Invalid physical properties for " + name);
        formula = formula == null ? "" : formula;
    }

    public static Builder builder(int id, String name) { return new Builder(id, name); }

    public static final class Builder {
        private final int id;
        private final String name;
        private String displayName;
        private int color = 0xFFFFFF;
        private int meltingPoint = 300;
        private int boilingPoint = 1000;
        private float density = 1;
        private String formula = "";
        private boolean dust;
        private boolean gas;
        private boolean liquid;

        private Builder(int id, String name) {
            this.id = id;
            this.name = name;
            this.displayName = name;
        }

        public Builder displayName(String value) { displayName = value; return this; }
        public Builder color(int rgb) { color = rgb; return this; }
        public Builder meltingPointKelvin(int value) { meltingPoint = value; return this; }
        public Builder boilingPointKelvin(int value) { boilingPoint = value; return this; }
        public Builder density(float value) { density = value; return this; }
        public Builder chemicalFormula(String value) { formula = value; return this; }

        /** Dust, the small and tiny dusts, and the unit flag. This is what generates dust items. */
        public Builder dust() { dust = true; return this; }

        public Builder gas() { gas = true; return this; }
        public Builder liquid() { liquid = true; return this; }

        public MaterialDeclaration build() {
            return new MaterialDeclaration(id, name, displayName, color, meltingPoint, boilingPoint, density,
                    formula, dust, gas, liquid);
        }
    }
}
