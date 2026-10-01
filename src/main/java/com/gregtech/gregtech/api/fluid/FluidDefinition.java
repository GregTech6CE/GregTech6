package com.gregtech.gregtech.api.fluid;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.data.RegisteredFluids.FluidEntry;
import com.gregtech.gregtech.data.RegisteredFluids.FluidFlags;
import com.gregtech.gregtech.data.RegisteredFluids.FluidTextureMode;

import java.util.Objects;

/** Named fluid properties. Building is side-effect free; registration is explicit. */
public final class FluidDefinition {
    private FluidDefinition() {}
    public static Builder builder(String registryName) { return new Builder(registryName); }

    public static final class Builder {
        private final String name;
        private FluidTextureMode texture = FluidTextureMode.DEDICATED;
        private String materialKey;
        private int tint;
        private int density;
        private int viscosity;
        private int luminosity;
        private int temperature = RegisteredFluids.DEF_ENV_TEMP;
        private boolean gas;
        private boolean glint;
        private long flags = FluidFlags.LIQUID | FluidFlags.SIMPLE;

        private Builder(String name) {
            this.name = Objects.requireNonNull(name, "Fluid registry name");
            if (!name.matches("[a-z0-9/._-]+"))
                throw new IllegalArgumentException("Use a lowercase fluid registry name: " + name);
        }
        public Builder temperatureKelvin(int value) { temperature = value; return this; }
        public Builder density(int value) { density = value; return this; }
        public Builder viscosity(int value) { viscosity = value; return this; }
        public Builder luminosity(int value) { luminosity = value; return this; }
        public Builder color(int argb) { tint = argb; return this; }
        public Builder texture(FluidTextureMode value) { texture = Objects.requireNonNull(value); return this; }
        public Builder material(GTMaterial material) {
            if (material == null || !material.isValid()) throw new IllegalArgumentException("Invalid fluid material");
            materialKey = material.getName(); return this;
        }
        public Builder gas() { gas = true; flags = (flags | FluidFlags.GAS_FLAG) & ~FluidFlags.LIQUID; return this; }
        public Builder glint() { glint = true; flags |= FluidFlags.ENCHANTED_EFFECT; return this; }
        public Builder flags(long value) { flags |= value; return this; }
        public FluidEntry build() {
            if (temperature < 0 || viscosity < 0 || luminosity < 0 || luminosity > 15)
                throw new IllegalArgumentException("Invalid fluid physical properties: " + name);
            if (gas && (flags & FluidFlags.LIQUID) != 0)
                throw new IllegalArgumentException("Fluid cannot be marked both liquid and gas: " + name);
            return new FluidEntry(name, texture, materialKey, tint, true, glint, gas,
                    density, viscosity, luminosity, temperature, flags);
        }
        public FluidEntry register(String key) { return RegisteredFluids.registerDefinition(key, build()); }
    }
}
