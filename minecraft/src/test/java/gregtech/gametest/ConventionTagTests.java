package gregtech.gametest;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** GT6's ore-dictionary identities stay reachable through the loaders' convention tags. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ConventionTagTests {
    private static final String BATCH = "convention_tags";

    private ConventionTagTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void dyeAndCraftingIdentitiesJoinConventionTags(GameTestHelper helper) {
        assertTagContains(helper, "forge:dyes/black", "gt6ce:dust_carbon");
        assertTagContains(helper, "forge:dyes/gray", "gt6ce:dust_graphite");
        assertTagContains(helper, "forge:dyes/gray", "gt6ce:dust_darkashes");
        assertTagContains(helper, "forge:dyes/light_gray", "gt6ce:dust_ashes");
        assertTagContains(helper, "forge:dyes/brown", "gt6ce:dust_cocoa");
        assertTagContains(helper, "forge:dyes/yellow", "gt6ce:dust_yellowlimonite");
        assertTagContains(helper, "gt6ce:glass_colorless", "minecraft:glass");
        assertTagContains(helper, "gt6ce:leathers", "minecraft:leather");
        assertTagContains(helper, "gt6ce:wooden_chests", "minecraft:chest");
        assertTagContains(helper, "gt6ce:crafting_hardened_clay", "minecraft:terracotta");
        assertTagContains(helper, "gt6ce:crafting_hardened_clay", "minecraft:red_terracotta");

        var stick = GTItems.getStack(MaterialPrefix.stick, GTMaterialRegistry.get("Wood"), 1);
        helper.assertFalse(stick.isEmpty(), "missing GT6 wooden stick");
        helper.assertTrue(stick.is(tag("forge:rods/wooden")), "GT6 wooden stick is not a wooden rod");
        helper.succeed();
    }

    private static void assertTagContains(GameTestHelper helper, String tagId, String itemId) {
        var id = ResourceLocation.tryParse(itemId);
        var item = id == null ? java.util.Optional.<Item>empty() : BuiltInRegistries.ITEM.getOptional(id);
        helper.assertTrue(item.isPresent(), "missing item " + itemId);
        helper.assertTrue(new ItemStack(item.orElseThrow()).is(tag(tagId)), itemId + " is missing from " + tagId);
    }

    private static TagKey<Item> tag(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.tryParse(id));
    }
}
