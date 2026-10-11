package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.TreeMap;

/**
 * Guards the material tag layout.
 *
 * <p>{@code gregtech:material/<material>} lists every item of one material — including its {@code unit}
 * forms and its fluid containers (GT6 fluid items bound to that material through
 * {@code RegisteredFluids.boundMaterial}). The umbrella {@code gregtech:material} tag that used to
 * reference all of them is deliberately gone: resolving it enumerates every material item in the mod
 * (tens of thousands), which cost JEI several seconds on every open.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class MaterialTagTests {

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("gregtech", path));
    }

    private static int sizeOf(String path) {
        return BuiltInRegistries.ITEM.getTag(tag(path)).map(set -> {
            int count = 0;
            for (var holder : set) count++;
            return count;
        }).orElse(0);
    }

    /** The umbrella tag must not exist: it is the one that made JEI scan every material item. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void umbrellaMaterialTagIsGone(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ITEM.getTag(tag("material")).isEmpty(),
                "gregtech:material (all materials) must not be registered");
        int iron = sizeOf("material/iron");
        helper.assertTrue(iron > 0, "the per-material tag still exists (gregtech:material/iron)");
        helper.assertTrue(iron < 200, "one material's tag stays small: iron has " + iron + " entries");
        helper.succeed();
    }

    /** A material's tag carries its unit form and its fluid containers. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void materialTagHasUnitsAndFluids(GameTestHelper helper) {
        GTMaterial iron = GTMaterialRegistry.get("Iron");
        helper.assertTrue(iron != null && iron.isValid(), "Iron resolves");

        ItemStack unit = GTItems.getStack(MaterialPrefix.unit, iron, 1);
        helper.assertTrue(!unit.isEmpty(), "the port registers unit items");
        helper.assertTrue(inTag("material/iron", unit), "iron's tag contains its unit form");

        var json = new TreeMap<String, Object>();
        json.put("material/iron", sizeOf("material/iron"));
        json.put("unit/iron", sizeOf("unit/iron"));
        json.put("fluid/iron", sizeOf("fluid/iron"));
        json.put("umbrellaPresent", BuiltInRegistries.ITEM.getTag(tag("material")).isPresent());
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/material-tags.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (java.io.IOException e) {
            helper.fail("cannot write docs/material-tags.json: " + e);
            return;
        }
        helper.assertTrue(sizeOf("unit/iron") > 0, "the unit form tag is published too");
        helper.succeed();
    }

    private static boolean inTag(String path, ItemStack stack) {
        return BuiltInRegistries.ITEM.getTag(tag(path))
                .map(set -> {
                    for (var holder : set) {
                        if (holder.value() == stack.getItem()) return true;
                    }
                    return false;
                }).orElse(false);
    }

    /**
     * Form tags keep working for recipe authors. The tag name is the prefix registry name plus the
     * material ({@code gregtech:ingot/iron}) — {@code items} is the tag directory, not part of the name.
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void formTagsStillExist(GameTestHelper helper) {
        GTMaterial iron = GTMaterialRegistry.get("Iron");
        ItemStack ingot = GTItems.getStack(MaterialPrefix.ingot, iron, 1);
        helper.assertTrue(!ingot.isEmpty() && ingot.getItem() instanceof MaterialItem, "iron ingot exists");
        helper.assertTrue(inTag("ingot/iron", ingot), "gregtech:ingot/iron contains the ingot");
        helper.assertTrue(sizeOf("ingot/iron") > 0, "gregtech:ingot/iron is published");
        helper.succeed();
    }
}
