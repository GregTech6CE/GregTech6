package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.content.book.GTMaterialDictionary;
import com.gregtech.gregtech.content.book.GTMaterialEnchants;
import com.gregtech.gregtech.loaders.c.GTEnchantmentTable;
import com.gregtech.gregtech.loaders.c.GTMaterialFields;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;

/**
 * §55: the material dictionary's enchantment page — GT6's {@code UT.Books.addMaterialDictionary}
 * prints the enchantments a material grants per use, transcribed from {@code MT.java}'s
 * {@code addEnchantmentForTools/Damage/Weapons/Ammo/Ranged/Fishing/Armors} calls.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class EnchantmentDictionaryTests {

    /** The transcribed table and its 1.20.1 enchantment ids. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void enchantmentTableMatchesGt6(GameTestHelper helper) {
        helper.assertTrue(GTEnchantmentTable.MATERIALS.size() >= 150,
                "GT6 enchants well over a hundred materials, got " + GTEnchantmentTable.MATERIALS.size());
        int entries = GTEnchantmentTable.MATERIALS.stream().mapToInt(row -> row.entries().size()).sum();
        helper.assertTrue(entries >= 500, "MT.java has ~500 enchantment calls, got " + entries);
        // Every enchantment id must be a real 1.20.1 enchantment (the tool maps GT6's 1.7.10 names).
        for (GTEnchantmentTable.Material row : GTEnchantmentTable.MATERIALS) {
            for (GTEnchantmentTable.Entry entry : row.entries()) {
                ResourceLocation id = ResourceLocation.withDefaultNamespace(entry.enchantment());
                helper.assertTrue(ForgeRegistries.ENCHANTMENTS.containsKey(id),
                        entry.enchantment() + " (from " + row.material() + ") must be a real enchantment");
                // GT6 grants levels beyond vanilla's maximum (its own tables go past X).
                helper.assertTrue(entry.level() >= 1 && entry.level() <= 100,
                        "levels are positive, got " + entry.level());
            }
        }
        helper.assertTrue(GTMaterialEnchants.KINDS.equals(
                        List.of("Tools", "Weapons", "Ammo", "Ranged", "Fishing", "Armors")),
                "GT6's six enchantment lists, in its own order");
        helper.succeed();
    }

    /**
     * Every table row names a material through GT6's <em>field</em> names ({@code Ma} = Magic,
     * {@code PO4} = Phosphate, {@code Polycarbonate} = "Hard Plastic"), so the lookup has to go
     * through the generated field table. Only other mods' materials may stay unresolved.
     */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void everyTableRowResolvesToAPortMaterial(GameTestHelper helper) {
        helper.assertTrue(GTMaterialFields.size() >= 1000,
                "GT6's field table, got " + GTMaterialFields.size());
        java.util.TreeSet<String> unresolved = new java.util.TreeSet<>();
        for (GTEnchantmentTable.Material row : GTEnchantmentTable.MATERIALS) {
            if (GTMaterialEnchants.resolve(row.material()) == null) unresolved.add(row.material());
        }
        int resolved = GTMaterialEnchants.resolvedRows();
        helper.assertTrue(resolved >= GTEnchantmentTable.MATERIALS.size() - 4,
                "rows that name a port material: " + resolved + " of "
                        + GTEnchantmentTable.MATERIALS.size() + ", unresolved " + unresolved);
        // The only rows left are materials of other mods GT6 enchanted for compatibility, plus one
        // non-material receiver GT6's own table picked up.
        for (String allowed : new String[]{"Vinteum", "Pyrotheum", "Sunstone", "STONES"}) {
            unresolved.remove(allowed);
        }
        helper.assertTrue(unresolved.isEmpty(), "unexpected unresolved rows: " + unresolved);

        // GT6's field names, spelled differently from the material they name.
        helper.assertTrue(GTMaterialEnchants.resolve("Ma") == GTMaterialRegistry.get("Magic"),
                "Ma is Magic in MT.java, not magnesium");
        helper.assertTrue(GTMaterialEnchants.resolve("Fe") == GTMaterialRegistry.get("Iron"), "Fe is Iron");
        helper.assertTrue(GTMaterialEnchants.resolve("PO4") == GTMaterialRegistry.get("Phosphate"),
                "PO4 is Phosphate");
        helper.assertTrue(GTMaterialEnchants.resolve("Polycarbonate")
                        == GTMaterialRegistry.get("Hard Plastic"),
                "Polycarbonate is the material MT.java names Hard Plastic");
        helper.assertTrue(GTMaterialEnchants.resolve("HSLA") == GTMaterialRegistry.get("HSLA-Steel"),
                "HSLA is HSLA-Steel");
        helper.assertTrue(GTMaterialEnchants.resolve("Atl") == GTMaterialRegistry.get("Atlarus"),
                "Atl is Atlarus");
        helper.succeed();
    }

    /** The materials that only resolve through the field table really get their pages now. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void fieldNamedMaterialsGetTheirPage(GameTestHelper helper) {
        int checked = 0;
        for (String row : new String[]{"Ma", "Fe", "Pb", "Pt", "Ni", "Bi", "PO4", "Polycarbonate",
                "HSLA", "Atl", "Ke", "AmberGolden", "AmberDominican", "PhosphorusBlue",
                "PhosphorusRed", "PhosphorusWhite", "EnderAmethyst", "IronWood"}) {
            GTMaterial material = GTMaterialEnchants.resolve(row);
            helper.assertTrue(material != null, row + " resolves to a port material");
            Map<String, List<String>> enchants = GTMaterialEnchants.enchantmentsOf(material);
            helper.assertTrue(!enchants.isEmpty(),
                    material.getName() + " (row " + row + ") has enchantments in GT6's table");
            helper.assertTrue(GTMaterialDictionary.pages(material).stream()
                            .anyMatch(page -> page.startsWith("Enchantments of ")),
                    material.getName() + " has an enchantment page");
            checked++;
        }
        helper.assertTrue(checked == 18, "materials checked: " + checked);
        helper.succeed();
    }

    /** The lookup resolves GT6's short field names and merges GT6's "Damage" shorthand. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void lookupMatchesGt6Materials(GameTestHelper helper) {
        // Iron is `Fe` in GT6's table — the alias must find it.
        GTMaterial iron = GTMaterialRegistry.get("Iron");
        helper.assertTrue(iron != null && iron.isValid(), "the port has Iron");
        Map<String, List<String>> ironEnchants = GTMaterialEnchants.enchantmentsOf(iron);
        helper.assertTrue(!ironEnchants.isEmpty(), "iron has enchantments in GT6's table");
        helper.assertTrue(ironEnchants.containsKey("Weapons") || ironEnchants.containsKey("Tools"),
                "iron's entries land in the weapon/tool lists, got " + ironEnchants.keySet());
        helper.assertTrue(!ironEnchants.containsKey("Damage"),
                "GT6's Damage shorthand is merged into weapons and ammo");

        // A material GT6 never enchants yields nothing (no page).
        GTMaterial unobtainium = GTMaterialRegistry.get("Diamond");
        if (unobtainium != null && GTMaterialEnchants.enchantmentsOf(unobtainium).isEmpty()) {
            helper.assertTrue(GTMaterialDictionary.pages(unobtainium).stream()
                            .noneMatch(page -> page.startsWith("Enchantments of ")),
                    "no enchantment page for materials without enchantments");
        }
        helper.assertTrue(GTMaterialEnchants.roman(1).equals("I")
                        && GTMaterialEnchants.roman(4).equals("IV")
                        && GTMaterialEnchants.roman(10).equals("X"),
                "levels print as Roman numerals");
        helper.succeed();
    }

    /** A material with enchantments gets the page, with the kinds GT6 groups them by. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void dictionaryHasEnchantmentPage(GameTestHelper helper) {
        // Find any port material that the table enchants, so the test does not depend on one name.
        GTMaterial target = null;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.isValid() && !GTMaterialEnchants.enchantmentsOf(material).isEmpty()) {
                target = material;
                break;
            }
        }
        helper.assertTrue(target != null, "at least one port material has GT6 enchantments");
        String page = GTMaterialDictionary.pages(target).stream()
                .filter(entry -> entry.startsWith("Enchantments of ")).findFirst().orElse(null);
        helper.assertTrue(page != null,
                target.getName() + " must have an enchantment page");
        Map<String, List<String>> enchants = GTMaterialEnchants.enchantmentsOf(target);
        for (String kind : enchants.keySet()) {
            helper.assertTrue(page.contains(kind + ":"),
                    "the page lists the " + kind + " group, got: " + page);
        }
        helper.succeed();
    }
}
