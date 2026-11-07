package gregtech.gametest;

import gregtech.bootstrap.registration.OriginCreativeContents;
import gregtech.content.item.CatalogVisibility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Creative pages follow the original GT6 tab split instead of the port's former grab-bag pages. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class CreativeTabClassificationTests {
    private static final String BATCH = "creative_tabs";

    private CreativeTabClassificationTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void decorationsHoldPlantsLeavesAndBales(GameTestHelper helper) {
        for (String id : new String[] {
            "flower_alpine_catchfly", "glowtus_red", "leaves_rubber", "bale_rye", "bale_grass", "clay_blue"
        })
            expect(helper, id, "iconsets");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void woodBlocksStayOnTheWoodPage(GameTestHelper helper) {
        for (String id : new String[] {
            "log_rubber", "planks_rubber", "stripped_log_rubber", "planks_compressed", "log_hole_maple"
        })
            expect(helper, id, "woods");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void stoneAndBuildingBlocksKeepTheirPages(GameTestHelper helper) {
        for (String id : new String[] {"crystal_ore_pyrite", "block_ore_amber", "sand_magnetite"})
            expect(helper, id, "stones");
        for (String id : new String[] {"grassblock_normal", "concrete_black", "asphalt_black"})
            expect(helper, id, "construction");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void materialVariantsFollowTheirBasePage(GameTestHelper helper) {
        expect(helper, "bottle_crate_steel", "storage");
        expect(helper, "bookshelf_metal_steel", "storage");
        expect(helper, "charging_crafting_table_steel_galvanized", "crafting_tables");
        expect(helper, "anvil_bronze", "tool_blocks");
        expect(helper, "fluid_cell_tin", "fluid_containers");
        expect(helper, "fluid_barometer_gas_cylinder", "fluid_containers");
        expect(helper, "rotation_transformer_steel", "axles");
        expect(helper, "laser_absorber_lv", "laser_absorbers");
        expect(helper, "co2_laser_lv", "lasers");
        expect(helper, "auto_hammer_steel", "automatic_tools");
        expect(helper, "zpm_discharger_basic", "zpm");
        expect(helper, "coin_pile", "coins");
        expect(helper, "bars_steel", "tool_blocks");
        expect(helper, "spike_ball_falling_fancy_gold", "tool_blocks");
        expect(helper, "filter_oredict", "sorting");
        expect(helper, "sap_bag", "tool_blocks");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void cratesAreUntypedAndVisible(GameTestHelper helper) {
        for (String id : new String[] {"crate", "loot_crate", "material_crate"}) {
            Item crate = expect(helper, id, "untyped");
            helper.assertTrue(
                    CatalogVisibility.visible(new ItemStack(crate)),
                    id + " should be visible in creative and EMI");
        }
        helper.succeed();
    }

    private static Item expect(GameTestHelper helper, String id, String family) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
        helper.assertTrue(item != Items.AIR, "missing item gt6ce:" + id);
        helper.assertTrue(
                family.equals(OriginCreativeContents.family(item)),
                id + " should be on the " + family + " page");
        return item;
    }
}
