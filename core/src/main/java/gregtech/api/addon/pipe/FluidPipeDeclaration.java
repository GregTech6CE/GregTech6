/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.pipe;




import gregtech.api.addon.*;

/**
 * One addon fluid-pipe material. GregTech expands it into the seven built-in sizes,
 * from tiny through nonuple. {@code maxTemperature} of {@code 0} uses the material
 * melting point times 1.25, the same rule as a built-in row that leaves the limit unset.
 */
public record FluidPipeDeclaration(String idSuffix, String material, long baseCapacity,
                                   boolean gasProof, boolean acidProof, boolean plasmaProof,
                                   boolean magicProof, long maxTemperature, int flammability) {
    public FluidPipeDeclaration {
        if (!AddonNames.matches(AddonNames.LOWER_PATH, idSuffix))
            throw new IllegalArgumentException("Fluid pipe id must be a lower-case suffix: " + idSuffix);
        if (material == null || material.isBlank())
            throw new IllegalArgumentException("Fluid pipe '" + idSuffix + "' needs a material");
        if (baseCapacity <= 0)
            throw new IllegalArgumentException("Fluid pipe '" + idSuffix + "' capacity must be positive");
        try {
            Math.multiplyExact(baseCapacity, 24);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Fluid pipe '" + idSuffix + "' capacity overflows", overflow);
        }
        if (maxTemperature < 0)
            throw new IllegalArgumentException("Fluid pipe '" + idSuffix + "' temperature must be zero or positive");
        if (flammability < 0)
            throw new IllegalArgumentException("Fluid pipe '" + idSuffix + "' flammability must be zero or positive");
    }
}
