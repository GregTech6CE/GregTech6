package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.content.book.GTMaterialDictionary;
import com.gregtech.gregtech.content.book.GTMaterialRecipes;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.loaders.c.GTAlloyTable;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * §54: the material dictionary's alloy page (GT6's {@code UT.Books.addMaterialDictionary} lists the
 * alloys a material takes part in) — built from the port's own alloying table
 * ({@link GTAlloyTable}, transcribed from {@code Loader_Recipes_Alloys}).
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class AlloyDictionaryTests {

    /** The transcribed alloy table covers the port's alloying recipes. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void alloyTableMatchesLoader(GameTestHelper helper) {
        helper.assertTrue(GTAlloyTable.ALLOYS.size() >= 50,
                "the port's alloying loader has dozens of recipes, got " + GTAlloyTable.ALLOYS.size());
        // GT6's best known alloys: bronze from copper + tin, brass from copper + zinc, invar from
        // iron + nickel, stainless steel from iron + nickel + chromium + manganese.
        helper.assertTrue(hasAlloy("Bronze", "Copper", "Tin"), "bronze is copper + tin");
        helper.assertTrue(hasAlloy("Brass", "Copper", "Zinc"), "brass is copper + zinc");
        helper.assertTrue(hasAlloy("Invar", "Iron", "Nickel"), "invar is iron + nickel");
        helper.assertTrue(hasAlloy("StainlessSteel", "Iron", "Nickel", "Chromium", "Manganese"),
                "stainless steel is iron + nickel + chromium + manganese");
        // Loop variables in the loader (iron/copper variants) must be expanded, not left as names.
        for (GTAlloyTable.Alloy alloy : GTAlloyTable.ALLOYS) {
            for (GTAlloyTable.Alloy.Input input : alloy.inputs()) {
                helper.assertTrue(GTMaterialRegistry.get(input.material()) != null,
                        "alloy input " + input.material() + " must be a real material");
            }
        }
        helper.succeed();
    }

    /** The dictionary of an alloy lists how it is made, and its inputs list what they alloy into. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void dictionaryListsAlloys(GameTestHelper helper) {
        GTMaterial bronze = GTMaterialRegistry.get("Bronze");
        helper.assertTrue(bronze != null && bronze.isValid(), "the port has a Bronze material");
        List<String> made = GTMaterialRecipes.madeFrom(bronze);
        helper.assertTrue(!made.isEmpty(), "bronze is made by alloying");
        String line = made.get(0);
        helper.assertTrue(line.contains("Copper") && line.contains("Tin"),
                "the bronze line names its inputs, got: " + line);

        List<String> pages = GTMaterialDictionary.pages(bronze);
        String alloyPage = pages.stream().filter(page -> page.startsWith("Alloys of Bronze"))
                .findFirst().orElse(null);
        helper.assertTrue(alloyPage != null, "bronze's dictionary has an alloy page");
        helper.assertTrue(alloyPage.contains("Is an alloy of:") && alloyPage.contains("Copper x3"),
                "the page shows the recipe, got: " + alloyPage);

        // Copper participates in many alloys, so it gets the "alloys into" half too.
        List<String> into = GTMaterialRecipes.alloyedInto(GTMaterialRegistry.get("Copper"));
        helper.assertTrue(into.size() >= 5, "copper is part of many alloys, got " + into.size());
        String copperPage = String.join("\n", pages(GTMaterialRegistry.get("Copper")));
        helper.assertTrue(copperPage.contains("Alloys into:") && copperPage.contains("Bronze"),
                "copper's dictionary lists what it alloys into");
        helper.succeed();
    }

    /** Materials without any alloy recipe get no alloy page (GT6 omits empty sections). */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void materialsWithoutAlloysHaveNoPage(GameTestHelper helper) {
        GTMaterial silver = Materials.Silver;
        List<String> pages = GTMaterialDictionary.pages(silver);
        boolean hasAlloyPage = pages.stream().anyMatch(page -> page.startsWith("Alloys of Silver"));
        helper.assertTrue(hasAlloyPage || !GTMaterialRecipes.alloyedInto(silver).isEmpty(),
                "silver's alloy page appears exactly when it takes part in an alloy");
        // A material the alloy table never mentions has neither half.
        GTMaterial unobtainium = GTMaterialRegistry.get("Diamond");
        if (unobtainium != null && GTMaterialRecipes.alloyedInto(unobtainium).isEmpty()
                && GTMaterialRecipes.madeFrom(unobtainium).isEmpty()) {
            helper.assertTrue(GTMaterialDictionary.pages(unobtainium).stream()
                            .noneMatch(page -> page.startsWith("Alloys of ")),
                    "no alloy page is generated for materials without alloys");
        }
        helper.succeed();
    }

    private static List<String> pages(GTMaterial material) {
        return GTMaterialDictionary.pages(material);
    }

    /** True when the table has an alloy of {@code output} whose inputs contain all the names. */
    private static boolean hasAlloy(String output, String... inputs) {
        for (GTAlloyTable.Alloy alloy : GTAlloyTable.ALLOYS) {
            if (!alloy.output().equals(output)) continue;
            boolean all = true;
            for (String input : inputs) {
                boolean found = false;
                for (GTAlloyTable.Alloy.Input entry : alloy.inputs()) {
                    if (entry.material().equals(input)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    all = false;
                    break;
                }
            }
            if (all) return true;
        }
        return false;
    }
}
