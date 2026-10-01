package com.gregtech.gregtech.api.material;

/** GT6 atomic counts; for antimatter these mean antiparticles. Additional mass may be negative (Magic). */
public record AtomicProperties(long protons, long electrons, long neutrons, long additionalMass) {
    public static final AtomicProperties GT6_DEFAULT = new AtomicProperties(43, 43, 55, 0);
    public static final AtomicProperties ZERO = new AtomicProperties(0, 0, 0, 0);

    public AtomicProperties {
        if (protons < 0 || electrons < 0 || neutrons < 0)
            throw new IllegalArgumentException("Particle counts must be nonnegative");
        Math.addExact(Math.addExact(protons, neutrons), additionalMass);
    }

    public long mass() { return Math.addExact(Math.addExact(protons, neutrons), additionalMass); }
}
