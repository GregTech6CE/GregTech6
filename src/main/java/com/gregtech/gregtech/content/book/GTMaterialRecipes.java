package com.gregtech.gregtech.content.book;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.loaders.c.GTAlloyTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reverse lookups over the port's alloying table, for the material dictionary's alloy page
 * (GT6's {@code UT.Books.addMaterialDictionary} lists "the alloys this material takes part in").
 */
public final class GTMaterialRecipes {
    private GTMaterialRecipes() {}

    /**
     * The alloys a material is an <em>input</em> of, one line each — GT6's dictionary lists them as
     * "what this material can be alloyed into".
     */
    public static List<String> alloyedInto(GTMaterial material) {
        String name = material.getName();
        List<String> lines = new ArrayList<>();
        for (GTAlloyTable.Alloy alloy : GTAlloyTable.ALLOYS) {
            boolean used = false;
            for (GTAlloyTable.Alloy.Input input : alloy.inputs()) {
                if (input.material().equals(name)) {
                    used = true;
                    break;
                }
            }
            if (used) lines.add(describe(alloy));
        }
        return lines;
    }

    /** The alloys this material is the <em>result</em> of (GT6's "how to make it" note). */
    public static List<String> madeFrom(GTMaterial material) {
        String name = material.getName();
        List<String> lines = new ArrayList<>();
        for (GTAlloyTable.Alloy alloy : GTAlloyTable.ALLOYS) {
            if (alloy.output().equals(name)) lines.add(describe(alloy));
        }
        return lines;
    }

    /** {@code "Bronze x4 <- Copper x3 + Tin x1"}. */
    public static String describe(GTAlloyTable.Alloy alloy) {
        StringBuilder builder = new StringBuilder(alloy.output()).append(" x").append(alloy.units())
                .append(" <- ");
        for (int i = 0; i < alloy.inputs().size(); i++) {
            GTAlloyTable.Alloy.Input input = alloy.inputs().get(i);
            if (i > 0) builder.append(" + ");
            builder.append(input.material()).append(" x").append(input.units());
        }
        return builder.toString();
    }
}
