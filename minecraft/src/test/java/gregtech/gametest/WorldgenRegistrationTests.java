package gregtech.gametest;

import gregtech.content.worldgen.GTBedrockOres;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

/** Registry-level worldgen checks that do not need a populated world. */
//? if >=1.21.1 {
/*@net.neoforged.neoforge.gametest.GameTestHolder("gt6ce")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
@net.minecraftforge.gametest.GameTestHolder("gt6ce")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
public final class WorldgenRegistrationTests {
    @GameTest(template = "test_empty", batch = "worldgen_registration", timeoutTicks = 600)
    public static void retainedNetherOresAreRegistered(GameTestHelper helper) {
        helper.assertTrue(GTBedrockOres.NETHER.size() == 5, "nether bedrock rows: " + GTBedrockOres.NETHER.size());
        for (String id : List.of(
                "ore_glowstone", "ore_netherquartz", "ore_ancientdebris", "ore_efrine", "ore_firestone")) {
            helper.assertTrue(
                    BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath("gt6ce", id)),
                    "missing block " + id);
        }
        for (String id : List.of("ore_voidquartz", "ore_gloomstone")) {
            helper.assertFalse(
                    BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath("gt6ce", id)),
                    "unexpected block " + id);
        }
        helper.assertFalse(
                BuiltInRegistries.FLUID.containsKey(ResourceLocation.fromNamespaceAndPath("gt6ce", "waterdirty")),
                "dirty water is still registered");
        helper.succeed();
    }
}
